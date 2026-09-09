package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;

/** Minimal machine-readable model card for scientific audit and release freeze. */
public final class ChemCoreModelCard {
    public static final String SCHEMA="chemcore-model-card/1";
    public final String modelType,version,featureSchema,trainingScope,validationProtocol,samplingProfileId,validityDomain,sha256;
    public final int trainingExperiments,validationExperiments,blindTestExperiments;
    public final Map<String,Double> metrics;
    public final List<String> sourceIds;

    public ChemCoreModelCard(String modelType,String version,String featureSchema,String trainingScope,String validationProtocol,
                             String samplingProfileId,String validityDomain,int trainingExperiments,int validationExperiments,
                             int blindTestExperiments,Map<String,Double> metrics,List<String> sourceIds,String sha256){
        this.modelType=modelType==null?"":modelType;this.version=version==null?"":version;this.featureSchema=featureSchema==null?"":featureSchema;
        this.trainingScope=trainingScope==null?"":trainingScope;this.validationProtocol=validationProtocol==null?"":validationProtocol;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;
        this.validityDomain=validityDomain==null?"":validityDomain;this.trainingExperiments=trainingExperiments;this.validationExperiments=validationExperiments;
        this.blindTestExperiments=blindTestExperiments;this.metrics=metrics;this.sourceIds=sourceIds;this.sha256=sha256==null?"":sha256;
    }

    public JSONObject toJson(){JSONObject x=new JSONObject();x.put("schema",SCHEMA);x.put("model_type",modelType);x.put("version",version);x.put("feature_schema",featureSchema);x.put("training_scope",trainingScope);x.put("validation_protocol",validationProtocol);x.put("sampling_profile_id",samplingProfileId);x.put("validity_domain",validityDomain);x.put("training_experiments",trainingExperiments);x.put("validation_experiments",validationExperiments);x.put("blind_test_experiments",blindTestExperiments);JSONObject m=new JSONObject();if(metrics!=null)for(Map.Entry<String,Double> e:metrics.entrySet())if(e.getValue()!=null&&Double.isFinite(e.getValue()))m.put(e.getKey(),e.getValue());x.put("metrics",m);JSONArray s=new JSONArray();if(sourceIds!=null)for(String id:sourceIds)s.put(id);x.put("source_ids",s);x.put("sha256",sha256);return x;}
}
