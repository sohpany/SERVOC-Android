package com.servoc.app.chemcore;

import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONObject;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class CompletedSessionChemCoreAnalyzerTest {
    @Test public void completedEightChannelAverageProducesAcceptedR0Ratios()throws Exception{
        ManualR0Calibration.load(null);
        JSONObject root=new JSONObject(),measurement=new JSONObject(),mq=new JSONObject();
        measurement.put("state","ready");measurement.put("valid_channels",8);
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject q=new JSONObject();q.put("ok",true);q.put("valid",true);q.put("rs",FieldR0Baseline.value(hw.key)*1.25);mq.put(hw.key,q);
        }
        measurement.put("mq",mq);root.put("measurement",measurement);
        Map<String,Double> ratios=CompletedSessionChemCoreAnalyzer.observedRatios(root);
        assertEquals(8,ratios.size());
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ)assertEquals(1.25,ratios.get(hw.key),1e-9);
    }

    @Test public void acceptedReferencePatternProducesRankedScreeningCandidates()throws Exception{
        List<InverseMolecularSolver.Candidate> library=ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates();
        assertFalse("accepted reference candidate registry must not be empty",library.isEmpty());
        SamplingProfile profile=SamplingProfile.hazRobStandardChamberV2();
        Map<String,Double> observed=null;
        for(InverseMolecularSolver.Candidate candidate:library){
            Map<String,Double> attempt=new LinkedHashMap<>();double ppm=Math.sqrt(candidate.models.get(0).minPpm()*candidate.models.get(0).maxPpm());
            for(ChemCoreResponseModel model:candidate.models){
                ChemCoreResponseModel.Estimate p=model.predict(ppm,25.0,50.0,profile);
                if(p!=null&&p.available&&Double.isFinite(p.meanLog10Ratio))attempt.put(model.sensorKey(),Math.pow(10.0,p.meanLog10Ratio));
            }
            if(attempt.size()>=candidate.minimumContributors){observed=attempt;break;}
        }
        assertNotNull("at least one accepted candidate must yield a usable reference pattern",observed);
        ChemCoreInferenceEngine.Context context=new ChemCoreInferenceEngine.Context(
                "FIELD-TEST",profile.profileId,CompletedSessionChemCoreAnalyzer.MODEL_VERSION,25.0,50.0,null,false);
        ChemCoreResultEnvelope result=ChemCoreInferenceEngine.analyze(observed,library,profile,context);
        assertNotNull(result);assertFalse("screening candidate list must not be empty",result.candidates.isEmpty());
        JSONObject json=result.toJson();assertTrue(json.getJSONArray("candidates").length()>0);
        assertFalse("ordinary screening must not report ppm without M4/open-set gates",json.getBoolean("ppm_reportable"));
        assertNotEquals(ChemCoreDecision.Outcome.QUANTIFIED,result.outcome);
    }
}
