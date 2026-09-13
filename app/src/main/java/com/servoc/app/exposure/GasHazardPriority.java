package com.servoc.app.exposure;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Display-order policy for already accepted gas detections.
 *
 * Analytical identity ranking remains independent. HAZARD is the default safety view and orders by
 * the dimensionless substance-specific hazard ratio. CONCENTRATION is an operator display option
 * that orders by measured ppm only. Neither mode changes identity, thresholds, symbols or evidence.
 */
public final class GasHazardPriority {
    public enum Mode { HAZARD, CONCENTRATION }
    private GasHazardPriority(){}

    public static String labelAr(Mode mode){return mode==Mode.CONCENTRATION?"التركيز":"الخطر";}
    public static Mode parse(String value){return "concentration".equalsIgnoreCase(value)||"ppm".equalsIgnoreCase(value)?Mode.CONCENTRATION:Mode.HAZARD;}

    public static double hazardScore(JSONObject assessment){
        if(assessment==null||!assessment.optBoolean("input_gate_passed",false))return Double.NaN;
        double x=assessment.optDouble("hazard_priority_ratio",Double.NaN);return Double.isFinite(x)&&x>=0?x:Double.NaN;
    }
    public static double concentrationPpm(JSONObject assessment){
        if(assessment==null||!assessment.optBoolean("input_gate_passed",false))return Double.NaN;
        double x=assessment.optDouble("ppm",Double.NaN);return Double.isFinite(x)&&x>=0?x:Double.NaN;
    }
    public static double score(JSONObject assessment){return hazardScore(assessment);}

    public static int compare(JSONObject a,JSONObject b,Mode mode){
        Mode m=mode==null?Mode.HAZARD:mode;
        double x=m==Mode.CONCENTRATION?concentrationPpm(a):hazardScore(a),y=m==Mode.CONCENTRATION?concentrationPpm(b):hazardScore(b);
        boolean xv=Double.isFinite(x),yv=Double.isFinite(y);
        if(xv&&yv){int c=Double.compare(y,x);if(c!=0)return c;}
        else if(xv)return -1;else if(yv)return 1;
        // The non-selected dimension is only a tie-breaker, never the primary sort key.
        double tx=m==Mode.CONCENTRATION?hazardScore(a):concentrationPpm(a),ty=m==Mode.CONCENTRATION?hazardScore(b):concentrationPpm(b);
        boolean txv=Double.isFinite(tx),tyv=Double.isFinite(ty);if(txv&&tyv){int c=Double.compare(ty,tx);if(c!=0)return c;}else if(txv)return -1;else if(tyv)return 1;
        return Long.compare(a==null?Long.MAX_VALUE:a.optLong("chemical_uid",Long.MAX_VALUE),b==null?Long.MAX_VALUE:b.optLong("chemical_uid",Long.MAX_VALUE));
    }

    public static int compareDescending(JSONObject a,JSONObject b){return compare(a,b,Mode.HAZARD);}
    public static JSONArray sort(JSONArray input){return sort(input,Mode.HAZARD);}
    public static JSONArray sort(JSONArray input,Mode mode){
        List<JSONObject> rows=new ArrayList<>();if(input!=null)for(int i=0;i<input.length();i++){JSONObject q=input.optJSONObject(i);if(q!=null)rows.add(q);}
        rows.sort((a,b)->compare(a,b,mode));JSONArray out=new JSONArray();for(JSONObject q:rows)out.put(q);return out;
    }

    public static String sortDescriptor(Mode mode){Mode m=mode==null?Mode.HAZARD:mode;return m==Mode.HAZARD?"hazard_priority_ratio_desc":"ppm_desc";}
}
