package com.servoc.app.exposure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class GasHazardPriorityTest {
    private static JSONObject finding(long uid,double ppm,double hazard)throws Exception{return new JSONObject().put("input_gate_passed",true).put("chemical_uid",uid).put("ppm",ppm).put("hazard_priority_ratio",hazard);}

    @Test public void hazardSortPrioritizesSixPpmAtFivePpmLimitOverThreeHundredPpmAtOneThousandPpmLimit()throws Exception{
        JSONObject dangerousLowConcentration=finding(1,6,1.2);JSONObject benignHighConcentration=finding(2,300,0.3);
        JSONArray out=GasHazardPriority.sort(new JSONArray().put(benignHighConcentration).put(dangerousLowConcentration),GasHazardPriority.Mode.HAZARD);
        assertEquals(1,out.getJSONObject(0).getLong("chemical_uid"));assertEquals(2,out.getJSONObject(1).getLong("chemical_uid"));
    }

    @Test public void concentrationSortIsAvailableWithoutChangingHazardScores()throws Exception{
        JSONObject dangerousLowConcentration=finding(1,6,1.2);JSONObject benignHighConcentration=finding(2,300,0.3);
        JSONArray out=GasHazardPriority.sort(new JSONArray().put(dangerousLowConcentration).put(benignHighConcentration),GasHazardPriority.Mode.CONCENTRATION);
        assertEquals(2,out.getJSONObject(0).getLong("chemical_uid"));assertEquals(1,out.getJSONObject(1).getLong("chemical_uid"));assertEquals(1.2,GasHazardPriority.hazardScore(dangerousLowConcentration),1e-9);assertEquals("ppm_desc",GasHazardPriority.sortDescriptor(GasHazardPriority.Mode.CONCENTRATION));
    }

    @Test public void hazardIsDefaultAndWithheldRowsSortAfterValidRows()throws Exception{
        JSONObject valid=finding(3,1,0.1),withheld=new JSONObject().put("input_gate_passed",false).put("ppm",99999).put("hazard_priority_ratio",99999);
        JSONArray out=GasHazardPriority.sort(new JSONArray().put(withheld).put(valid));assertEquals(3,out.getJSONObject(0).getLong("chemical_uid"));assertFalse(out.getJSONObject(1).getBoolean("input_gate_passed"));assertEquals("الخطر",GasHazardPriority.labelAr(null));
    }
}
