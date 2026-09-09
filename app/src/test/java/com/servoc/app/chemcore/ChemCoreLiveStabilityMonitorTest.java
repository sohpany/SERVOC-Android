package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChemCoreLiveStabilityMonitorTest {
    private static final String[] KEYS={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};

    @Test public void stableFlatArrayPassesResearchGate() throws Exception {
        ChemCoreLiveStabilityMonitor m=new ChemCoreLiveStabilityMonitor();
        long t=1_000_000L;
        for(int i=0;i<48;i++){
            JSONObject mq=new JSONObject();
            for(int k=0;k<KEYS.length;k++){
                double ratio=1.20+k*0.03+(i%3-1)*0.0004;
                mq.put(KEYS[k],new JSONObject().put("valid",true).put("ratio",ratio));
            }
            m.accept(new JSONObject().put("mq",mq),t+i*250L);
        }
        ChemCoreLiveStabilityMonitor.Assessment a=m.assess();
        assertEquals(8,a.validChannels);assertTrue(a.stableChannels>=6);assertTrue(a.stable);
    }

    @Test public void sustainedTrendDoesNotPass() throws Exception {
        ChemCoreLiveStabilityMonitor m=new ChemCoreLiveStabilityMonitor();
        long t=2_000_000L;
        for(int i=0;i<48;i++){
            JSONObject mq=new JSONObject();
            for(String key:KEYS)mq.put(key,new JSONObject().put("valid",true).put("ratio",1.0+i*0.003));
            m.accept(new JSONObject().put("mq",mq),t+i*250L);
        }
        ChemCoreLiveStabilityMonitor.Assessment a=m.assess();
        assertEquals(8,a.validChannels);assertFalse(a.stable);
    }
}
