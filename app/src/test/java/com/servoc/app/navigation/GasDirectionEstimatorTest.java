package com.servoc.app.navigation;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class GasDirectionEstimatorTest {
    private static JSONObject sample(double ratio)throws Exception{return sample(ratio,true);}
    private static JSONObject sample(double ratio,boolean valid)throws Exception{
        JSONObject root=new JSONObject(),mq=new JSONObject();
        for(String k:new String[]{"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"}){
            JSONObject r=new JSONObject();r.put("valid",valid);r.put("ratio",ratio);
            JSONObject q=new JSONObject();q.put("valid",valid);q.put("status",valid?"VALID":"INVALID");q.put("manufacturer_heater_protocol_valid",true);r.put("quality",q);mq.put(k,r);
        }
        root.put("mq",mq);return root;
    }

    @Test public void risingGasEvidenceWhileDrivingForwardPointsForward()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),100,100,1000);
        e.update(sample(0.90),100,100,2000);
        e.update(sample(0.80),100,100,3000);
        GasDirectionEstimator.Result r=e.update(sample(0.70),100,100,4000);
        assertTrue(r.valid);assertTrue(r.confidence>=GasDirectionEstimator.MIN_DIRECTION_CONFIDENCE);assertEquals(0.0,r.angleRad,0.35);
        assertTrue(r.voteCount>=2);assertTrue(r.supportSensors>0);assertEquals("DIRECTION_HYPOTHESIS",r.reason);
        assertTrue(r.coherence>0&&r.coherence<=1);assertTrue(r.evidenceStrength>0&&r.evidenceStrength<=1);
        assertEquals(r.confidence,r.coherence*r.evidenceStrength,1e-12);
    }

    @Test public void noMotionDoesNotInventDirection()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),0,0,1000);e.update(sample(0.80),0,0,2000);GasDirectionEstimator.Result r=e.update(sample(0.60),0,0,3000);
        assertFalse(r.valid);assertEquals(0,r.voteCount);assertEquals(0,r.confidence,0);assertEquals("INSUFFICIENT_TRANSLATIONAL_EVIDENCE",r.reason);
    }

    @Test public void spinInPlaceDoesNotInventDirection()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),100,-100,1000);e.update(sample(0.80),100,-100,2000);GasDirectionEstimator.Result r=e.update(sample(0.60),100,-100,3000);
        assertFalse(r.valid);assertEquals(0,r.voteCount);assertEquals("INSUFFICIENT_TRANSLATIONAL_EVIDENCE",r.reason);
    }

    @Test public void telemetryGapClearsDirectionalVotes()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),100,100,1000);e.update(sample(0.90),100,100,1500);e.update(sample(0.80),100,100,2000);
        GasDirectionEstimator.Result before=e.update(sample(0.70),100,100,2500);assertTrue(before.valid);
        GasDirectionEstimator.Result after=e.update(sample(0.65),100,100,6000);
        assertFalse(after.valid);assertEquals(0,after.voteCount);assertEquals("INSUFFICIENT_TRANSLATIONAL_EVIDENCE",after.reason);
    }

    @Test public void targetChangeClearsPreviousDirection()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),100,100,1000,103);e.update(sample(0.90),100,100,1500,103);e.update(sample(0.80),100,100,2000,103);
        assertTrue(e.update(sample(0.70),100,100,2500,103).valid);
        GasDirectionEstimator.Result changed=e.update(sample(0.69),100,100,3000,95);
        assertFalse(changed.valid);assertEquals(0,changed.voteCount);assertEquals(95,changed.targetChemical);
    }

    @Test public void invalidCanonicalChannelsNeverCreateArrow()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.0,false),100,100,1000,103);e.update(sample(0.7,false),100,100,2000,103);
        GasDirectionEstimator.Result r=e.update(sample(0.5,false),100,100,3000,103);
        assertFalse(r.valid);assertEquals(0,r.supportSensors);assertEquals("NO_VALID_TARGET_CHANNELS",r.reason);
    }

    @Test public void unsupportedTargetFailsClosed()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        GasDirectionEstimator.Result r=e.update(sample(0.7),100,100,1000,0);
        assertFalse(r.valid);assertEquals("TARGET_UNSUPPORTED_BY_CURRENT_ARRAY",r.reason);
    }
}
