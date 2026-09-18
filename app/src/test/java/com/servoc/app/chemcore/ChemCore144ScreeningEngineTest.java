package com.servoc.app.chemcore;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ChemCore144ScreeningEngineTest {
    @Test public void evaluatesEntireOperationalPanel() {
        JSONObject p=ChemCore144ScreeningEngine.evaluate();
        assertEquals("EVALUATED",p.optString("state"));
        assertEquals(144,p.optInt("target_count"));
        assertEquals(144,p.optInt("evaluated_count"));
        JSONArray rows=p.optJSONArray("rows");
        assertNotNull(rows); assertEquals(144,rows.length());
        for(int i=0;i<rows.length();i++){
            JSONObject r=rows.optJSONObject(i);
            assertNotNull(r); assertEquals(i,r.optInt("index",-1));
            assertFalse(r.optString("name").isEmpty());
            assertFalse(r.optString("status").isEmpty());
            assertFalse(r.optString("reason").isEmpty());
            assertFalse(r.optBoolean("detected",true));
            assertFalse(r.optBoolean("ppm_reportable",true));
        }
    }
    @Test public void identifiedSessionMarksOnlyAcceptedRowDetected() {
        ChemCoreResultEnvelope e=new ChemCoreResultEnvelope("s",ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,
                20L,"Carbon Monoxide",Double.NaN,Double.NaN,Double.NaN,0.1,0.8,0.2,"p",ChemCoreFeaturePipeline.SCHEMA_VERSION,"m","",null);
        JSONArray rows=ChemCore144ScreeningEngine.evaluate(e).optJSONArray("rows");
        int detected=0;
        for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i).optBoolean("detected"))detected++;
        assertEquals(1,detected);
        assertTrue(rows.optJSONObject(20).optBoolean("detected"));
        assertFalse(rows.optJSONObject(20).optBoolean("ppm_reportable"));
    }
}
