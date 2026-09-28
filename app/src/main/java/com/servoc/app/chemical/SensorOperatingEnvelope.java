package com.servoc.app.chemical;

/**
 * Operating-envelope policy for the eight MQ sensors physically used by HazRob.
 * Manufacturer targets, manufacturer graph cross-responses and community-only
 * research fits are kept as distinct evidence tiers.
 */
public final class SensorOperatingEnvelope {
    private SensorOperatingEnvelope() {}

    public static final int UNKNOWN = 0;
    public static final int COMMUNITY_CROSS_RESPONSE = 1;
    public static final int MANUFACTURER_GRAPH_RESPONSE = 2;
    public static final int DATASHEET_CROSS_RESPONSE = MANUFACTURER_GRAPH_RESPONSE; // compatibility alias
    public static final int PRIMARY_TARGET = 3;
    public static final int OUT_OF_RANGE = -1;
    public static final int HEATER_PROTOCOL_REQUIRED = -2;
    public static final int HEATER_PROTOCOL_INVALID = HEATER_PROTOCOL_REQUIRED;

    public static final class Check {
        public final int status;
        public final double minPpm;
        public final double maxPpm;
        public final String note;

        Check(int status,double minPpm,double maxPpm,String note){
            this.status=status;this.minPpm=minPpm;this.maxPpm=maxPpm;this.note=note;
        }

        /** Only a manufacturer target is eligible without an accepted device-specific calibration. */
        public boolean usableForQuantitation(){return status==PRIMARY_TARGET;}
        public boolean usableAsFingerprintEvidence(){
            return status==PRIMARY_TARGET||status==MANUFACTURER_GRAPH_RESPONSE||status==COMMUNITY_CROSS_RESPONSE;
        }
    }

    public static Check check(int sensor,int chemical,double ppm,boolean heaterCycleValid){
        if(!Double.isFinite(ppm)||ppm<=0)
            return new Check(UNKNOWN,Double.NaN,Double.NaN,"Non-finite or non-positive concentration.");

        MqManufacturerOperatingContract.Spec operating=MqManufacturerOperatingContract.get(sensor);
        if(operating!=null&&operating.heaterCycleRequired&&!heaterCycleValid)
            return new Check(HEATER_PROTOCOL_REQUIRED,Double.NaN,Double.NaN,
                    operating.model+" use requires the manufacturer high/low heater phase/cycle for the exact sensor revision/module.");

        ManufacturerEvidenceMatrix.Evidence m=ManufacturerEvidenceMatrix.get(sensor,chemical);
        if(m.level!=ManufacturerEvidenceMatrix.NONE){
            if(ppm<m.minPpm||ppm>m.maxPpm)
                return new Check(OUT_OF_RANGE,m.minPpm,m.maxPpm,
                        "Outside the current manufacturer evidence range; do not extrapolate ppm. "+m.note);
            if(m.level==ManufacturerEvidenceMatrix.TARGET)
                return new Check(PRIMARY_TARGET,m.minPpm,m.maxPpm,m.source+": "+m.note);
            return new Check(MANUFACTURER_GRAPH_RESPONSE,m.minPpm,m.maxPpm,
                    m.source+": "+m.note+" Graph response is supporting evidence, not selectivity.");
        }

        // A community/digitized fit may still be useful for pattern analysis, but receives
        // a lower evidence tier if the current manufacturer registry does not show the pair.
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical)continue;
            if(ppm<c.minPpm||ppm>c.maxPpm)
                return new Check(OUT_OF_RANGE,c.minPpm,c.maxPpm,
                        "Outside the declared community/datasheet-fit research range.");
            return new Check(COMMUNITY_CROSS_RESPONSE,c.minPpm,c.maxPpm,
                    "Community/digitized graph fit only. Retain for fingerprint/research comparison; do not infer selective ppm without accepted device-specific calibration.");
        }

        return new Check(UNKNOWN,Double.NaN,Double.NaN,
                "No manufacturer or registered research envelope for this sensor/analyte pair.");
    }
}
