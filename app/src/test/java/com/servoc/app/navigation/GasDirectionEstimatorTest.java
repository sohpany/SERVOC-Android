package com.servoc.app.navigation;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class GasDirectionEstimatorTest {
    private static JSONObject sample(double ratio)throws Exception{
        JSONObject root=new JSONObject(),mq=new JSONObject();
        for(String k:new String[]{"mq2","mq3","mq4","mq7","mq8","mq135"}){
            JSONObject r=new JSONObject();r.put("valid",true);r.put("ratio",ratio);mq.put(k,r);
        }
        root.put("mq",mq);return root;
    }

    @Test public void risingGasEvidenceWhileDrivingForwardPointsForward()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),100,100,1000);
        e.update(sample(0.90),100,100,2000);
        e.update(sample(0.80),100,100,3000);
        GasDirectionEstimator.Result r=e.update(sample(0.70),100,100,4000);
        assertTrue(r.valid);assertTrue(r.confidence>=0.18);assertEquals(0.0,r.angleRad,0.35);
        assertTrue(r.voteCount>=2);assertTrue(r.coherence>0&&r.coherence<=1);assertTrue(r.evidenceStrength>0&&r.evidenceStrength<=1);
        assertEquals(r.confidence,r.coherence*r.evidenceStrength,1e-12);
    }

    @Test public void noMotionDoesNotInventDirection()throws Exception{
        GasDirectionEstimator e=new GasDirectionEstimator();
        e.update(sample(1.00),0,0,1000);e.update(sample(0.80),0,0,2000);GasDirectionEstimator.Result r=e.update(sample(0.60),0,0,3000);
        assertFalse(r.valid);assertEquals(0,r.voteCount);assertEquals(0,r.confidence,0);
    }
}
