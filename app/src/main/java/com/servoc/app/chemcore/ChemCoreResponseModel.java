package com.servoc.app.chemcore;

/** Forward sensor-response model used by the ChemCore inverse solver. */
public interface ChemCoreResponseModel {
    String sensorKey();
    String version();
    ChemCoreDecision.EvidenceTier evidenceTier();
    double minPpm();
    double maxPpm();

    /** Typed provenance defaults keep legacy models source-compatible while P5 migrates engines. */
    default ChemCoreEvidenceObservation.Type evidenceType(){
        ChemCoreDecision.EvidenceTier t=evidenceTier();
        if(t==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED)return ChemCoreEvidenceObservation.Type.HR069_CALIBRATION;
        if(t==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED)return ChemCoreEvidenceObservation.Type.QSPR;
        return ChemCoreEvidenceObservation.Type.REFERENCE_CURVE;
    }
    default String evidenceFamilyId(){return evidenceType().name();}
    default String modelId(){return version();}
    default String sourceId(){return version();}
    /** P6 will de-duplicate confidence by this key; it is intentionally not the sensor key. */
    default String independenceGroup(){return evidenceFamilyId()+":"+sourceId();}
    /** Any forward response model may assist a bounded screening/fitted concentration estimate. */
    default boolean quantitativeEstimateSupported(){return true;}
    /** Only M4 device/reference validated models may individually support reportable quantitation. */
    default boolean reportableQuantitative(){return evidenceTier()==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;}

    final class Estimate {
        public final boolean available;
        public final double meanLog10Ratio;
        public final double sigmaLog10;
        public final boolean insideDomain;
        public final String note;
        /** Optional prediction-specific provenance. Blank/null values fall back to model metadata. */
        public final ChemCoreEvidenceObservation.Type evidenceType;
        public final String familyId,modelId,sourceId,independenceGroup;

        public Estimate(boolean available,double meanLog10Ratio,double sigmaLog10,boolean insideDomain,String note){
            this(available,meanLog10Ratio,sigmaLog10,insideDomain,note,null,"","","","");
        }
        public Estimate(boolean available,double meanLog10Ratio,double sigmaLog10,boolean insideDomain,String note,
                        ChemCoreEvidenceObservation.Type evidenceType,String familyId,String modelId,String sourceId,String independenceGroup){
            this.available=available;this.meanLog10Ratio=meanLog10Ratio;this.sigmaLog10=sigmaLog10;
            this.insideDomain=insideDomain;this.note=note==null?"":note;this.evidenceType=evidenceType;
            this.familyId=familyId==null?"":familyId;this.modelId=modelId==null?"":modelId;
            this.sourceId=sourceId==null?"":sourceId;this.independenceGroup=independenceGroup==null?"":independenceGroup;
        }
    }

    Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile);
}
