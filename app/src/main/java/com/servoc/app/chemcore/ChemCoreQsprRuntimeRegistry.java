package com.servoc.app.chemcore;

import android.database.Cursor;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.data.ExperimentsDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime PubChem/QSPR bridge for HazRob 0.10.2 / HR069.
 *
 * Scientific operating rule:
 *  - direct/fingerprint/reference evidence wins on the same sensor;
 *  - QSPR may support a molecularly eligible target only on sensor models that passed
 *    leave-compound-out validation AND whose training-compound descriptor domain contains the
 *    target molecule;
 *  - QSPR-only identity remains M1/estimated and never creates reportable ppm;
 *  - mixtures/materials/isomer groups/occupational categories curated as non-molecular are
 *    excluded;
 *  - concentration/environment limits are still enforced by the response-model domain.
 *
 * QSPR training rows are deterministic points sampled from accepted source-backed M2 response
 * relations. They are model-transfer observations, not independent laboratory sessions.
 */
public final class ChemCoreQsprRuntimeRegistry {
    public static final String VERSION="pubchem-qspr-mox-runtime/3-ad";
    private static final int TRAINING_POINTS_PER_COMPOUND=8;
    private static final int MIN_TRAINING_COMPOUNDS=5;
    /** At least one independently validated sensor-QSPR model is enough to keep the compound readable. */
    private static final int MIN_QSPR_CONTRIBUTORS=1;
    private static final double RIDGE_LAMBDA=0.10;
    private static final double MAX_LCO_RMSE_LOG10=0.60;
    private static final double MAX_LCO_ABS_BIAS_LOG10=0.40;
    private static final double MIN_LCO_INSIDE_ENVELOPE_FRACTION=0.50;
    private static final double QSPR_ONLY_PRIOR_LOG_WEIGHT=-1.0;
    private static final double REFERENCE_TEMP_C=25.0;
    private static final double REFERENCE_RH=50.0;
    private static final String[] SENSOR_KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};

    public static final class Status {
        public final int descriptorCompleteLiveTargets;
        public final int enabledSensorModels;
        public final int liveCandidates;
        public final String enabledSensors;
        public final String note;
        Status(int descriptorCompleteLiveTargets,int enabledSensorModels,int liveCandidates,String enabledSensors,String note){
            this.descriptorCompleteLiveTargets=descriptorCompleteLiveTargets;
            this.enabledSensorModels=enabledSensorModels;
            this.liveCandidates=liveCandidates;
            this.enabledSensors=enabledSensors==null?"":enabledSensors;
            this.note=note==null?"":note;
        }
        public String summary(){
            return VERSION+":targets="+descriptorCompleteLiveTargets+":sensor_models="+enabledSensorModels+
                    ":candidates="+liveCandidates+":sensors="+enabledSensors+":"+note;
        }
    }

    private static final class SensorModel {
        final QsprMoxModel model;
        final MolecularDescriptorApplicabilityDomain descriptorDomain;
        SensorModel(QsprMoxModel model,MolecularDescriptorApplicabilityDomain descriptorDomain){
            this.model=model;this.descriptorDomain=descriptorDomain;
        }
    }

    private static volatile List<InverseMolecularSolver.Candidate> cached=Collections.emptyList();
    private static volatile Status lastStatus=new Status(0,0,0,"","not built");

    private ChemCoreQsprRuntimeRegistry(){}

    /** Rebuilds the deterministic in-memory QSPR layer after PubChem/curation seeds are loaded. */
    public static synchronized Status rebuild(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        MolecularIntelligenceRepository molecular=new MolecularIntelligenceRepository(db);
        ChemCoreIdentityCurationRepository curation=new ChemCoreIdentityCurationRepository(db);
        Map<Integer,Long> legacyUid=legacyUidMap(db);
        Map<Long,Target> liveTargets=liveTargets(db,curation,molecular);
        String[] descriptors=MolecularDescriptorContract.defaultQsprDescriptors();

        List<SensorModel> sensorModels=new ArrayList<>();
        List<String> enabledSensorNames=new ArrayList<>();
        for(int sensor=0;sensor<SENSOR_KEYS.length;sensor++){
            List<QsprValidationProtocol.LabeledObservation> labeled=new ArrayList<>();
            Set<Long> distinctCompounds=new LinkedHashSet<>();
            Map<Long,Map<String,Double>> distinctDescriptorVectors=new LinkedHashMap<>();
            for(int chem=0;chem<ChemicalCatalog.COUNT;chem++){
                Long uid=legacyUid.get(chem);if(uid==null)continue;
                Map<String,Double> vector=descriptorVector(molecular,uid,descriptors);if(vector==null)continue;
                if(!AcceptedReferenceResponseModel.eligible(sensor,chem))continue;
                AcceptedReferenceResponseModel direct;
                try{direct=new AcceptedReferenceResponseModel(sensor,chem);}catch(IllegalArgumentException e){continue;}
                int before=labeled.size();
                double lo=direct.minPpm(),hi=direct.maxPpm();
                if(!(lo>0)||!(hi>lo))continue;
                for(int j=0;j<TRAINING_POINTS_PER_COMPOUND;j++){
                    double f=TRAINING_POINTS_PER_COMPOUND==1?0.5:(double)j/(TRAINING_POINTS_PER_COMPOUND-1);
                    double ppm=Math.pow(10.0,Math.log10(lo)+f*(Math.log10(hi)-Math.log10(lo)));
                    ChemCoreResponseModel.Estimate p=direct.predict(ppm,REFERENCE_TEMP_C,REFERENCE_RH,null);
                    if(p==null||!p.available||!Double.isFinite(p.meanLog10Ratio))continue;
                    QsprMoxModel.Observation o=new QsprMoxModel.Observation(vector,ppm,REFERENCE_TEMP_C,REFERENCE_RH,p.meanLog10Ratio,1.0);
                    labeled.add(new QsprValidationProtocol.LabeledObservation(uid,"source-model-M2",o));
                }
                if(labeled.size()>before){
                    distinctCompounds.add(uid);
                    distinctDescriptorVectors.put(uid,vector);
                }
            }
            if(distinctCompounds.size()<MIN_TRAINING_COMPOUNDS)continue;
            List<QsprMoxModel.Observation> observations=new ArrayList<>();
            for(QsprValidationProtocol.LabeledObservation x:labeled)observations.add(x.observation);
            try{
                String version=VERSION+"/"+SENSOR_KEYS[sensor];
                QsprMoxModel model=QsprMoxModel.fit(SENSOR_KEYS[sensor],version,descriptors,observations,RIDGE_LAMBDA);
                QsprValidationProtocol.Result validation=QsprValidationProtocol.leaveCompoundOut(SENSOR_KEYS[sensor],version,descriptors,labeled,RIDGE_LAMBDA);
                if(!passesValidation(validation))continue;
                MolecularDescriptorApplicabilityDomain ad=MolecularDescriptorApplicabilityDomain.fit(
                        descriptors,new ArrayList<>(distinctDescriptorVectors.values()));
                sensorModels.add(new SensorModel(model,ad));
                enabledSensorNames.add(SENSOR_KEYS[sensor]);
            }catch(IllegalArgumentException ignored){}
        }

        List<InverseMolecularSolver.Candidate> next=new ArrayList<>();
        if(sensorModels.size()>=MIN_QSPR_CONTRIBUTORS){
            for(Target target:liveTargets.values()){
                List<ChemCoreResponseModel> models=new ArrayList<>();
                for(SensorModel s:sensorModels){
                    MolecularDescriptorApplicabilityDomain.Result ad=s.descriptorDomain.evaluate(target.descriptors);
                    if(!ad.insideDomain)continue;
                    models.add(new QsprResponseModelAdapter(s.model,target.descriptors));
                }
                if(models.size()>=MIN_QSPR_CONTRIBUTORS)
                    next.add(new InverseMolecularSolver.Candidate(target.uid,target.name,models,QSPR_ONLY_PRIOR_LOG_WEIGHT,MIN_QSPR_CONTRIBUTORS));
            }
        }
        cached=Collections.unmodifiableList(next);
        lastStatus=new Status(liveTargets.size(),sensorModels.size(),next.size(),String.join(",",enabledSensorNames),
                sensorModels.size()>=MIN_QSPR_CONTRIBUTORS
                        ?"QSPR M1 candidates are restricted by sensor-specific descriptor applicability domains"
                        :"no sensor-QSPR model passed compound-level validation/applicability requirements");
        return lastStatus;
    }

    public static Status status(){return lastStatus;}
    public static List<InverseMolecularSolver.Candidate> candidates(){return cached;}

    /**
     * Adds QSPR support to a direct/stable candidate set. For the same chemical+sensor, the
     * higher-tier direct model wins. QSPR-only candidates remain readable only on validated,
     * descriptor-domain-compatible QSPR channels.
     */
    public static List<InverseMolecularSolver.Candidate> supplement(List<InverseMolecularSolver.Candidate> base){
        List<InverseMolecularSolver.Candidate> molecular=cached;
        if(molecular.isEmpty())return base==null?Collections.emptyList():base;
        Map<Long,InverseMolecularSolver.Candidate> qByUid=new LinkedHashMap<>();for(InverseMolecularSolver.Candidate q:molecular)qByUid.put(q.chemicalUid,q);
        Map<Long,InverseMolecularSolver.Candidate> out=new LinkedHashMap<>();
        if(base!=null)for(InverseMolecularSolver.Candidate b:base){
            if(b==null)continue;InverseMolecularSolver.Candidate q=qByUid.remove(b.chemicalUid);
            if(q==null){out.put(b.chemicalUid,b);continue;}
            Map<String,ChemCoreResponseModel> models=new LinkedHashMap<>();
            for(ChemCoreResponseModel m:q.models)putPreferred(models,m);
            for(ChemCoreResponseModel m:b.models)putPreferred(models,m);
            out.put(b.chemicalUid,new InverseMolecularSolver.Candidate(b.chemicalUid,b.label,
                    new ArrayList<>(models.values()),b.priorLogWeight,b.minimumContributors));
        }
        for(InverseMolecularSolver.Candidate q:qByUid.values())out.putIfAbsent(q.chemicalUid,q);
        return new ArrayList<>(out.values());
    }

    private static boolean passesValidation(QsprValidationProtocol.Result v){
        return v!=null&&v.compoundsAttempted>=MIN_TRAINING_COMPOUNDS&&
                v.compoundsEvaluated==v.compoundsAttempted&&
                v.predictions>=v.compoundsEvaluated*4&&
                Double.isFinite(v.rmseLog10)&&v.rmseLog10<=MAX_LCO_RMSE_LOG10&&
                Double.isFinite(v.biasLog10)&&Math.abs(v.biasLog10)<=MAX_LCO_ABS_BIAS_LOG10&&
                Double.isFinite(v.insideEnvelopeFraction)&&v.insideEnvelopeFraction>=MIN_LCO_INSIDE_ENVELOPE_FRACTION;
    }

    private static void putPreferred(Map<String,ChemCoreResponseModel> models,ChemCoreResponseModel incoming){
        if(incoming==null||incoming.sensorKey()==null)return;String key=incoming.sensorKey().toLowerCase();ChemCoreResponseModel current=models.get(key);
        if(current==null||rank(incoming.evidenceTier())>=rank(current.evidenceTier()))models.put(key,incoming);
    }
    private static int rank(ChemCoreDecision.EvidenceTier t){return t==null?0:t.ordinal();}

    private static Map<Integer,Long> legacyUidMap(ExperimentsDb db){
        Map<Integer,Long> out=new LinkedHashMap<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT legacy_id,uid FROM chemicals WHERE legacy_id IS NOT NULL AND active=1",null)){
            while(c.moveToNext())out.put(c.getInt(0),c.getLong(1));
        }
        return out;
    }

    private static final class Target {
        final long uid;final String name;final Map<String,Double> descriptors;
        Target(long uid,String name,Map<String,Double> descriptors){this.uid=uid;this.name=name==null?"":name;this.descriptors=descriptors;}
    }

    private static Map<Long,Target> liveTargets(ExperimentsDb db,ChemCoreIdentityCurationRepository curation,MolecularIntelligenceRepository molecular){
        Map<Long,Target> out=new LinkedHashMap<>();String[] requested=MolecularDescriptorContract.defaultQsprDescriptors();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid,legacy_id,cas,name_en FROM chemicals WHERE active=1 AND legacy_id IS NOT NULL ORDER BY legacy_id",null)){
            while(c.moveToNext()){
                long uid=c.getLong(0);int legacy=c.getInt(1);String cas=c.getString(2),name=c.getString(3);
                if(legacy<0||legacy>=ChemicalCatalog.COUNT)continue;
                int level=ChemicalCoverage.forChemical(legacy).level;
                if(level!=ChemicalCoverage.DIRECT_CURVE&&level!=ChemicalCoverage.FINGERPRINT_CANDIDATE)continue;
                if(!curation.molecularQsprEligible(cas,name))continue;
                Map<String,Double> vector=descriptorVector(molecular,uid,requested);if(vector==null)continue;
                out.put(uid,new Target(uid,name,vector));
            }
        }
        return out;
    }

    private static Map<String,Double> descriptorVector(MolecularIntelligenceRepository molecular,long uid,String[] names){
        Map<String,Double> out=new LinkedHashMap<>();
        for(String n:names){Double v=molecular.latestNumeric(uid,n);if(v==null||!Double.isFinite(v))return null;out.put(n,v);}
        return out;
    }
}
