package com.servoc.app.chemical;

/**
 * Scientific coverage planner for the 144-entry chemical catalogue.
 * It separates direct quantitative curves, empirical fingerprint/calibration work,
 * composite metrics and compounds unsupported by the currently installed MQ array.
 */
public final class ChemicalCoverage {
    private ChemicalCoverage() {}

    public static final int DIRECT_CURVE = 3;
    public static final int FINGERPRINT_CANDIDATE = 2;
    public static final int UNVALIDATED_OR_WEAK = 1;
    public static final int AGGREGATE = 0;

    public static final class Status {
        public final int level;
        public final String method;
        public final String note;
        Status(int level, String method, String note) {
            this.level=level; this.method=method; this.note=note;
        }
    }

    public static Status forChemical(int chem) {
        if (chem < 0 || chem >= ChemicalCatalog.COUNT)
            return new Status(UNVALIDATED_OR_WEAK, "UNKNOWN", "Invalid catalogue index.");

        MqSensorAffinity.Plan plan=MqSensorAffinity.forChemical(chem);

        if (isDirectCurve(chem)) {
            return new Status(DIRECT_CURVE,
                    "Rs/R0 log-power curve + evidence-weighted consensus",
                    "A library curve exists. Recommended installed channels: "+sensorNames(plan.mask)+
                    ". Device-specific R0/calibration, valid heater protocol and in-range operation are still required before quantitative safety use.");
        }

        if (chem >= 135) {
            if(plan.mask==0){
                return new Status(UNVALIDATED_OR_WEAK,
                        "Unsupported aggregate with current MQ array",
                        plan.reason);
            }
            return new Status(AGGREGATE,
                    "Composite/mixture fingerprint • "+plan.className,
                    "Recommended installed channels: "+sensorNames(plan.mask)+". "+plan.reason+
                    " A mixture/operational metric must not be assigned a single universal pure-gas MQ equation.");
        }

        if(plan.mask==0){
            return new Status(UNVALIDATED_OR_WEAK,
                    plan.className,
                    plan.reason);
        }

        return new Status(FINGERPRINT_CANDIDATE,
                "Empirical MQ-array training • "+plan.className,
                "Recommended installed channels: "+sensorNames(plan.mask)+". "+plan.reason+
                " Identity classification and concentration calibration remain separate models.");
    }

    public static int directCurveCount() { return GasEngine.SUPPORTED.length; }

    public static int fingerprintCandidateCount() {
        int n=0;
        for(int i=0;i<ChemicalCatalog.COUNT;i++) if(forChemical(i).level==FINGERPRINT_CANDIDATE) n++;
        return n;
    }

    public static int aggregateCount(){
        int n=0;
        for(int i=0;i<ChemicalCatalog.COUNT;i++) if(forChemical(i).level==AGGREGATE) n++;
        return n;
    }

    public static int weakOrUnvalidatedCount() {
        int n=0;
        for(int i=0;i<ChemicalCatalog.COUNT;i++) if(forChemical(i).level==UNVALIDATED_OR_WEAK) n++;
        return n;
    }

    private static boolean isDirectCurve(int chem) {
        for (int c : GasEngine.SUPPORTED) if (c == chem) return true;
        return false;
    }

    public static String sensorNames(int mask){
        if(mask==0)return "none";
        String[] n={"MQ-2","MQ-3","MQ-4","MQ-5","MQ-7","MQ-8","MQ-9","MQ-135"};
        StringBuilder s=new StringBuilder();
        for(int i=0;i<n.length;i++) if((mask&(1<<i))!=0){
            if(s.length()>0)s.append(", ");
            s.append(n[i]);
        }
        return s.toString();
    }
}
