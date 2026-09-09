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

    @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
        QsprMoxModel.Prediction p=model.predict(descriptors,ppm,temperatureC,rh);
        return new Estimate(p.available,p.meanLog10Ratio,p.uncertaintyProxyLog10,p.insideTrainingEnvelope,p.note);
    }
}
