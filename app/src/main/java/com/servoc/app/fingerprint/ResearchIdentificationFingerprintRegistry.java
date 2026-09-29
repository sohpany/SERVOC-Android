package com.servoc.app.fingerprint;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.LiteratureCurveRegistry;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.SensorOperatingEnvelope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Research-only identification fingerprint priors for the eight HR069 MQ channels.
 *
 * The vectors are deterministic model-derived expectations in log10(Rs/R0) space. They are
 * generated only where a source-backed response relation exists for the exact MQ channel and
 * analyte. They are NOT empirical HR069 fingerprints and can never be treated as M4.
 */
public final class ResearchIdentificationFingerprintRegistry {
    private ResearchIdentificationFingerprintRegistry() {}

    public static final String RELEASE="0.10.2";
    public static final String HARDWARE_REVISION="HR069";
    public static final String EVIDENCE_TIER="RESEARCH_EXTERNAL";
    public static final boolean M4=false;
    public static final int SENSOR_COUNT=8;

    public static final String MODEL_DERIVED_NUMERIC="MODEL_DERIVED_NUMERIC";
    public static final String TRANSFER_DATA_AVAILABLE="TRANSFER_DATA_AVAILABLE";
    public static final String MANUFACTURER_AFFINITY_ONLY="MANUFACTURER_AFFINITY_ONLY";
    public static final String SOURCE_PENDING="SOURCE_PENDING";

    /** Concentrations sampled to represent response-shape fingerprints without extrapolation. */
    public static final double[] CONCENTRATION_GRID_PPM={10,25,50,100,200,300,500,1000,3000,5000,10000};

    /** Pure analytes with source-backed numeric response relations on at least one installed MQ channel. */
    public static final int[] NUMERIC_ANALYTES={20,21,23,34,75,76,78,84,95,103,110};

    public static final class TargetStatus {
        public final int chemicalId;
        public final String name;
        public final String status;
        public final boolean numericFingerprintAvailable;
        public final String note;
        TargetStatus(int chemicalId,String status,boolean numericFingerprintAvailable,String note){
            this.chemicalId=chemicalId;
            this.name=chemicalId>=0&&chemicalId<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[chemicalId]:"";
            this.status=status;
            this.numericFingerprintAvailable=numericFingerprintAvailable;
            this.note=note;
        }
    }

    private static final List<ChemicalFingerprint> LIBRARY=Collections.unmodifiableList(buildLibrary());

    /**
     * Food/spoilage-related compounds discussed for HazRob. A status row is retained even when
     * an HR069-compatible numerical vector is not yet defensible, preventing a missing source
     * from being silently replaced by an invented fingerprint.
     */
    public static List<TargetStatus> foodSpoilageTargets(){
        List<TargetStatus> out=new ArrayList<>();
        out.add(status(4,MANUFACTURER_AFFINITY_ONLY,false,"MQ135 manufacturer documentation supports a sulfide response category, but no exact H2S Rs/R0 numerical fingerprint is registered for HR069."));
        out.add(status(54,SOURCE_PENDING,false,"Acetaldehyde is a relevant food/headspace VOC, but no exact HR069 MQ response relation has yet been pinned."));
        out.add(status(89,SOURCE_PENDING,false,"Ethylene is a key fruit-ripening gas, but no exact HR069 MQ response relation has yet been pinned."));
        out.add(status(102,TRANSFER_DATA_AVAILABLE,false,"Methanol has external MQ3 dynamic-heater discrimination data; transfer to the present HR069 acquisition protocol is still required."));
        out.add(status(103,MODEL_DERIVED_NUMERIC,true,"Ethanol/alcohol has multi-channel source-backed response relations including the MQ3 target channel and a peer-reviewed MQ135 curve."));
        out.add(status(110,MODEL_DERIVED_NUMERIC,true,"Acetone has an MQ135 peer-reviewed Rs/R0 prior and registered research relation."));
        out.add(status(115,TRANSFER_DATA_AVAILABLE,false,"Ethyl acetate has external known-concentration MOS-array evidence, but not an exact HR069 MQ response vector yet."));
        out.add(status(23,MODEL_DERIVED_NUMERIC,true,"CO2 has a peer-reviewed MQ135 cross-response prior; this is identification/screening evidence, not a selective CO2 meter."));
        out.add(status(21,MODEL_DERIVED_NUMERIC,true,"Ammonia has an MQ135 manufacturer target range and peer-reviewed response prior."));
        out.add(status(76,MODEL_DERIVED_NUMERIC,true,"Methane has multiple HR069 MQ response channels and manufacturer target channels."));
        return Collections.unmodifiableList(out);
    }

    public static List<ChemicalFingerprint> library(){return LIBRARY;}

    public static boolean hasNumericPrior(int chemicalId){
        for(ChemicalFingerprint f:LIBRARY)if(f.chemicalId==chemicalId)return true;
        return false;
    }

    public static int templateCountForChemical(int chemicalId){
        int n=0;for(ChemicalFingerprint f:LIBRARY)if(f.chemicalId==chemicalId)n++;return n;
    }

    private static TargetStatus status(int id,String status,boolean numeric,String note){return new TargetStatus(id,status,numeric,note);}

    private static List<ChemicalFingerprint> buildLibrary(){
        List<ChemicalFingerprint> out=new ArrayList<>();
        for(int chemical:NUMERIC_ANALYTES){
            String name=chemical>=0&&chemical<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[chemical]:String.valueOf(chemical);
            for(double ppm:CONCENTRATION_GRID_PPM){
                ChemicalFingerprint f=buildTemplate(chemical,name,ppm);
                if(f!=null)out.add(f);
            }
        }
        return out;
    }

    private static ChemicalFingerprint buildTemplate(int chemical,String name,double ppm){
        double[] mean=new double[SENSOR_COUNT];
        double[] sigma=new double[SENSOR_COUNT];
        Arrays.fill(mean,Double.NaN);Arrays.fill(sigma,Double.NaN);
        int contributors=0;
        double[] legacyRatios=GasEngine.simulateRatios(chemical,ppm);

        for(int sensor=0;sensor<SENSOR_COUNT;sensor++){
            SourceValue v=fromLiterature(sensor,chemical,ppm);
            if(v==null)v=fromGasEngine(sensor,chemical,ppm,legacyRatios);
            if(v==null)v=fromResearchRegistry(sensor,chemical,ppm);
            if(v==null)continue;
            mean[sensor]=Math.log10(v.ratio);
            sigma[sensor]=v.sigma;
            contributors++;
        }
        if(contributors==0)return null;
        String label="RESEARCH_PRIOR|"+name+"|"+formatPpm(ppm)+"ppm|"+RELEASE+"-"+HARDWARE_REVISION;
        return new ChemicalFingerprint(chemical,label,0,mean,sigma);
    }

    private static SourceValue fromLiterature(int sensor,int chemical,double ppm){
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            double ratio=c.ratioFromPpm(ppm);
            if(Double.isFinite(ratio)&&ratio>0)return new SourceValue(ratio,0.20);
        }
        return null;
    }

    private static SourceValue fromGasEngine(int sensor,int chemical,double ppm,double[] ratios){
        if(!GasEngine.hasCurve(sensor,chemical)||ratios==null||sensor>=ratios.length)return null;
        SensorOperatingEnvelope.Check envelope=SensorOperatingEnvelope.check(sensor,chemical,ppm,true);
        if(!envelope.usableAsFingerprintEvidence())return null;
        double ratio=ratios[sensor];
        if(!Double.isFinite(ratio)||ratio<=0)return null;
        double sigma=envelope.status==SensorOperatingEnvelope.PRIMARY_TARGET?0.22:
                envelope.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE?0.27:0.32;
        return new SourceValue(ratio,sigma);
    }

    private static SourceValue fromResearchRegistry(int sensor,int chemical,double ppm){
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            SensorOperatingEnvelope.Check envelope=SensorOperatingEnvelope.check(sensor,chemical,ppm,true);
            if(!envelope.usableAsFingerprintEvidence())continue;
            double ratio=c.ratioFromPpm(ppm);
            if(Double.isFinite(ratio)&&ratio>0)return new SourceValue(ratio,0.32);
        }
        return null;
    }

    private static String formatPpm(double ppm){
        long rounded=Math.round(ppm);
        return Math.abs(ppm-rounded)<1e-9?String.valueOf(rounded):String.valueOf(ppm);
    }

    private static final class SourceValue{
        final double ratio,sigma;
        SourceValue(double ratio,double sigma){this.ratio=ratio;this.sigma=sigma;}
    }
}
