package com.servoc.app.exposure;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Safety-ordering policy for accepted gas detections.
 *
 * Analytical identity ranking remains independent. This class sorts only already-accepted hazard
 * assessments by their dimensionless relative hazard ratio, never by absolute ppm.
 */
public final class GasHazardPriority {
    private GasHazardPriority(){}

    public static double score(JSONObject assessment){
        if(assessment==null||!assessment.optBoolean("input_gate_passed",false))return Double.NaN;
        double x=assessment.optDouble("hazard_priority_ratio",Double.NaN);return Double.isFinite(x)&&x>=0?x:Double.NaN;
    }

    public static int compareDescending(JSONObject a,JSONObject b){
        double x=score(a),y=score(b);boolean xv=Double.isFinite(x),yv=Double.isFinite(y);
        if(xv&&yv){int c=Double.compare(y,x);if(c!=0)return c;}
        else if(xv)return -1;else if(yv)return 1;
        // Stable deterministic tie-breaker; concentration is deliberately not used as the primary key.
        return Long.compare(a==null?Long.MAX_VALUE:a.optLong("chemical_uid",Long.MAX_VALUE),b==null?Long.MAX_VALUE:b.optLong("chemical_uid",Long.MAX_VALUE));
    }

    public static JSONArray sort(JSONArray input){
        List<JSONObject> rows=new ArrayList<>();if(input!=null)for(int i=0;i<input.length();i++){JSONObject q=input.optJSONObject(i);if(q!=null)rows.add(q);}
        rows.sort(GasHazardPriority::compareDescending);JSONArray out=new JSONArray();for(JSONObject q:rows)out.put(q);return out;
    }
}
