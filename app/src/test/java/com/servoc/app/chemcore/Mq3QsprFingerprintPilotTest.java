package com.servoc.app.chemcore;

import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ResearchCurveRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Exploratory MQ3 fingerprint <-> PubChem/QSPR pilot.
 *
 * It intentionally uses only molecular analytes with a numeric MQ3 reference relation and complete
 * PubChem descriptors. LPG is excluded because it is a mixture rather than one molecular entity.
 * Leave-compound-out validation is used; the ethanol diagnostic trains with ethanol fully removed.
 * Results are research diagnostics only and never M3/M4 evidence.
 */
public final class Mq3QsprFingerprintPilotTest {
    private static final String[] CORE3={"MolecularWeight","TPSA","Complexity"};
    private static final String[] CORE8={"MolecularWeight","TPSA","HBondDonorCount","HBondAcceptorCount","RotatableBondCount","HeavyAtomCount","Charge","Complexity"};
    private static final Map<Integer,String> CAS=new LinkedHashMap<>();
    static {
        CAS.put(103,"64-17-5"); // ethanol
        CAS.put(34,"71-43-2");  // benzene
        CAS.put(84,"110-54-3"); // n-hexane
        CAS.put(20,"630-08-0"); // carbon monoxide
        CAS.put(76,"74-82-8");  // methane
    }

    @Test public void generateMq3QsprFingerprintPilot() throws Exception {
        JSONObject asset=new JSONObject(new String(Files.readAllBytes(assetFile().toPath()),StandardCharsets.UTF_8));
        Map<Integer,Map<String,Double>> descriptor8=new LinkedHashMap<>();
        for(Map.Entry<Integer,String> e:CAS.entrySet()){
            Map<String,Double> d=descriptorsForCas(asset,e.getValue(),CORE8);if(d!=null)descriptor8.put(e.getKey(),d);
        }
        assertTrue("five MQ3 molecular analytes with PubChem descriptors required",descriptor8.size()>=5);

        List<QsprValidationProtocol.LabeledObservation> rows3=observations(descriptor8,CORE3);
        List<QsprValidationProtocol.LabeledObservation> rows8=observations(descriptor8,CORE8);
        QsprValidationProtocol.Result lco3=QsprValidationProtocol.leaveCompoundOut("mq3","mq3-qspr-core3-pilot",CORE3,rows3,0.10);
        QsprValidationProtocol.Result lco8=QsprValidationProtocol.leaveCompoundOut("mq3","mq3-qspr-core8-pilot",CORE8,rows8,0.10);

        JSONObject correlations=new JSONObject();
        for(String name:CORE8){
            List<Double> x=new ArrayList<>(),y=new ArrayList<>();
            for(Map.Entry<Integer,Map<String,Double>> e:descriptor8.entrySet()){
                ResearchCurveRegistry.Curve c=mq3Curve(e.getKey());if(c==null||!c.inDeclaredRange(100.0))continue;
                Double v=e.getValue().get(name);if(v==null||!Double.isFinite(v))continue;
                x.add(v);y.add(Math.log10(c.ratioFromPpm(100.0)));
            }
            correlations.put(name,pearson(x,y));
        }

        // Ethanol is removed completely from training, then predicted as a held-out molecule.
        JSONObject ethanol=ethanolHoldout(descriptor8.get(103),rows3,rows8);

        JSONObject root=new JSONObject();root.put("schema","hazrob-mq3-qspr-fingerprint-pilot/1");root.put("sensor","MQ3");
        root.put("molecular_compounds",descriptor8.size());root.put("training_source","MQ3 ResearchCurveRegistry responses + PubChem molecular descriptors");
        root.put("validation","leave-compound-out; ethanol diagnostic removes every ethanol response row from training");
        root.put("scientific_scope","exploratory relationship test only; small-N molecular panel; not independent laboratory validation and not M4");
        root.put("core3_descriptors",new JSONArray(CORE3));root.put("core8_descriptors",new JSONArray(CORE8));
        root.put("lco_core3",validationJson(lco3));root.put("lco_core8",validationJson(lco8));root.put("descriptor_vs_log10_mq3_ratio_at_100ppm",correlations);root.put("ethanol_holdout",ethanol);

        File build=new File("build");if(!build.isDirectory()&&!build.mkdirs())throw new IllegalStateException("cannot create build directory");
        File report=new File(build,"mq3-qspr-fingerprint-pilot.json");Files.write(report.toPath(),root.toString(2).getBytes(StandardCharsets.UTF_8));
        System.out.println("MQ3_QSPR_FINGERPRINT_PILOT_REPORT="+report.getAbsolutePath());
        System.out.println("MQ3_QSPR_CORE3_RMSE="+lco3.rmseLog10+";CORE8_RMSE="+lco8.rmseLog10);
        assertTrue(lco3.predictions>0);assertTrue(lco8.predictions>0);
    }

    private static JSONObject ethanolHoldout(Map<String,Double> ethanolDescriptors,List<QsprValidationProtocol.LabeledObservation> rows3,
                                              List<QsprValidationProtocol.LabeledObservation> rows8) throws Exception {
        JSONObject out=new JSONObject();if(ethanolDescriptors==null)return out;
        QsprMoxModel m3=fitExcluding(rows3,103L,CORE3,"mq3-ethanol-holdout-core3");
        QsprMoxModel m8=fitExcluding(rows8,103L,CORE8,"mq3-ethanol-holdout-core8");
        ResearchCurveRegistry.Curve research=mq3Curve(103);JSONArray pts=new JSONArray();
        for(double ppm:new double[]{25.0,100.0,300.0}){
            if(research==null||!research.inDeclaredRange(ppm))continue;
            MqVirtualSensorSimulator.Result accepted=MqVirtualSensorSimulator.simulate(MqVirtualSensorSimulator.Request.mq3(103,ppm));
            QsprMoxModel.Prediction p3=m3.predict(subset(ethanolDescriptors,CORE3),ppm,25.0,50.0);
            QsprMoxModel.Prediction p8=m8.predict(subset(ethanolDescriptors,CORE8),ppm,25.0,50.0);
            double rr=research.ratioFromPpm(ppm);JSONObject p=new JSONObject();p.put("ppm",ppm);p.put("research_ratio",rr);
            p.put("accepted_reference_ratio",accepted.available?accepted.ratio:JSONObject.NULL);
            p.put("qspr_core3_ratio",p3.available?p3.responseRatio:JSONObject.NULL);p.put("qspr_core8_ratio",p8.available?p8.responseRatio:JSONObject.NULL);
            if(p3.available){p.put("core3_abs_log_distance_to_research",Math.abs(Math.log10(p3.responseRatio/rr)));if(accepted.available)p.put("core3_abs_log_distance_to_accepted",Math.abs(Math.log10(p3.responseRatio/accepted.ratio)));}
            if(p8.available){p.put("core8_abs_log_distance_to_research",Math.abs(Math.log10(p8.responseRatio/rr)));if(accepted.available)p.put("core8_abs_log_distance_to_accepted",Math.abs(Math.log10(p8.responseRatio/accepted.ratio)));}
            pts.put(p);
        }
        out.put("note","ethanol excluded from QSPR training; comparison is against MQ3 research curve and current accepted/manufacturer-forward response");out.put("points",pts);return out;
    }

    private static QsprMoxModel fitExcluding(List<QsprValidationProtocol.LabeledObservation> rows,long uid,String[] descriptors,String version){
        List<QsprMoxModel.Observation> train=new ArrayList<>();for(QsprValidationProtocol.LabeledObservation x:rows)if(x.chemicalUid!=uid)train.add(x.observation);
        return QsprMoxModel.fit("mq3",version,descriptors,train,0.10);
    }

    private static List<QsprValidationProtocol.LabeledObservation> observations(Map<Integer,Map<String,Double>> all,String[] descriptorNames){
        List<QsprValidationProtocol.LabeledObservation> rows=new ArrayList<>();
        for(Map.Entry<Integer,Map<String,Double>> e:all.entrySet()){
            ResearchCurveRegistry.Curve c=mq3Curve(e.getKey());if(c==null)continue;Map<String,Double> d=subset(e.getValue(),descriptorNames);if(d==null)continue;
            for(int i=0;i<8;i++){
                double f=(double)i/7.0;double ppm=Math.pow(10.0,Math.log10(c.minPpm)+f*(Math.log10(c.maxPpm)-Math.log10(c.minPpm)));
                double ratio=c.ratioFromPpm(ppm);if(!(ratio>0)||!Double.isFinite(ratio))continue;
                QsprMoxModel.Observation o=new QsprMoxModel.Observation(d,ppm,25.0,50.0,Math.log10(ratio),1.0);
                rows.add(new QsprValidationProtocol.LabeledObservation(e.getKey(),"mq3-research",o));
            }
        }return rows;
    }

    private static Map<String,Double> descriptorsForCas(JSONObject root,String cas,String[] names) throws Exception {
        JSONArray records=root.getJSONArray("records");
        for(int i=0;i<records.length();i++){
            JSONObject r=records.getJSONObject(i);if(!cas.equals(r.optString("cas")))continue;JSONObject p=r.optJSONObject("pubchem");if(p==null)return null;
            Map<String,Double> out=new LinkedHashMap<>();for(String name:names){JSONObject q=p.optJSONObject(name);if(q==null)return null;double v=q.optDouble("value",Double.NaN);if(!Double.isFinite(v))return null;out.put(name,v);}return out;
        }return null;
    }

    private static Map<String,Double> subset(Map<String,Double> source,String[] names){if(source==null)return null;Map<String,Double> out=new LinkedHashMap<>();for(String n:names){Double v=source.get(n);if(v==null||!Double.isFinite(v))return null;out.put(n,v);}return out;}
    private static ResearchCurveRegistry.Curve mq3Curve(int chemical){for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)if(c.sensor==GasEngine.MQ3&&c.chemical==chemical)return c;return null;}

    private static JSONObject validationJson(QsprValidationProtocol.Result r) throws Exception {
        JSONObject x=new JSONObject();x.put("protocol",r.protocol);x.put("compounds_attempted",r.compoundsAttempted);x.put("compounds_evaluated",r.compoundsEvaluated);x.put("predictions",r.predictions);
        x.put("rmse_log10",r.rmseLog10);x.put("bias_log10",r.biasLog10);x.put("mean_compound_rmse_log10",r.meanCompoundRmseLog10);x.put("worst_compound_rmse_log10",r.worstCompoundRmseLog10);x.put("worst_compound_abs_bias_log10",r.worstCompoundAbsBiasLog10);x.put("inside_envelope_fraction",r.insideEnvelopeFraction);return x;
    }

    private static double pearson(List<Double> x,List<Double> y){
        int n=Math.min(x.size(),y.size());if(n<3)return Double.NaN;double mx=0,my=0;for(int i=0;i<n;i++){mx+=x.get(i);my+=y.get(i);}mx/=n;my/=n;double xy=0,xx=0,yy=0;for(int i=0;i<n;i++){double a=x.get(i)-mx,b=y.get(i)-my;xy+=a*b;xx+=a*a;yy+=b*b;}return xx>0&&yy>0?xy/Math.sqrt(xx*yy):Double.NaN;
    }

    private static File assetFile(){File a=new File("src/main/assets/generated/chemcore/molecular_descriptors.json");if(a.isFile())return a;File b=new File("app/src/main/assets/generated/chemcore/molecular_descriptors.json");if(b.isFile())return b;throw new IllegalStateException("molecular_descriptors.json not found");}
}
