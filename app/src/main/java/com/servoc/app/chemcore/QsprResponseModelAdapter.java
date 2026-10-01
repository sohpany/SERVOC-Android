package com.servoc.app.chemcore;

import java.util.Map;

/** Adapts a trained QSPR-MOX model and one compound descriptor vector to the forward-model contract. */
public final class QsprResponseModelAdapter implements ChemCoreResponseModel {
    private final QsprMoxModel model;
    private final Map<String,Double> descriptors;

    public QsprResponseModelAdapter(QsprMoxModel model,Map<String,Double> descriptors){
        if(model==null)throw new IllegalArgumentException("model required");
        this.model=model;this.descriptors=descriptors;
    }

    @Override public String sensorKey(){return model.sensorKey;}
    @Override public String version(){return model.version;}
    @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED;}
    @Override public double minPpm(){return model.minPpm;}
    @Override public double maxPpm(){return model.maxPpm;}
    @Override public ChemCoreEvidenceObservation.Type evidenceType(){return ChemCoreEvidenceObservation.Type.QSPR;}
    @Override public String evidenceFamilyId(){return "PUBCHEM_QSPR_MOX";}
    @Override public String modelId(){return model.version;}
    @Override public String sourceId(){return ChemCoreQsprRuntimeRegistry.VERSION;}
    /** Sensor-specific QSPR fits share descriptors/training lineage and are one evidentiary family for P6. */
    @Override public String independenceGroup(){return "QSPR:"+ChemCoreQsprRuntimeRegistry.VERSION;}
    @Override public boolean reportableQuantitative(){return false;}

    @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
        QsprMoxModel.Prediction p=model.predict(descriptors,ppm,temperatureC,rh);
        /*
         * Fail closed at runtime. QsprMoxModel may still return an out-of-domain numerical
         * prediction for validation diagnostics, but the production response-model adapter must
         * not feed that extrapolation into candidate ranking. This is especially important while
         * the source-backed synthetic QSPR bench is trained at a single reference T/RH condition.
         */
        boolean usable=p.available&&p.insideTrainingEnvelope;
        String note=usable?p.note:"QSPR rejected outside validated training envelope; "+p.note;
        return new Estimate(usable,p.meanLog10Ratio,p.uncertaintyProxyLog10,usable,note,
                evidenceType(),evidenceFamilyId(),modelId(),sourceId(),independenceGroup());
    }
}
