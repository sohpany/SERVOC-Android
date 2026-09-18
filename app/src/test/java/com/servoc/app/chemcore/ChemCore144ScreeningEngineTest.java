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
}
