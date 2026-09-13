package com.servoc.app.calibration;

import org.json.JSONObject;

/** Machine-readable provenance marker embedded in calibration_curves.source_note without a DB migration. */
public final class CalibrationValidationMarker {
    public static final String SCHEMA="hazrob-calibration-publication/1";
    private CalibrationValidationMarker(){}

    public static String encode(CalibrationSession session,CalibrationQualityReport quality,String operatorNote){
        JSONObject o=new JSONObject();try{o.put("schema",SCHEMA);o.put("qc_accepted",quality!=null&&quality.accepted);o.put("session_id",session==null?"":session.sessionId);o.put("sensor_id",session==null?-1:session.sensorId);o.put("chemical_id",session==null?-1:session.chemicalId);o.put("reference_gas",session==null?"":session.referenceGas);o.put("quality",quality==null?JSONObject.NULL:quality.toJson());o.put("operator_note",operatorNote==null?"":operatorNote);}catch(Exception ignored){}return o.toString();
    }

    public static boolean accepted(String sourceNote){
        if(sourceNote==null||sourceNote.trim().isEmpty())return false;
        try{JSONObject o=new JSONObject(sourceNote);return SCHEMA.equals(o.optString("schema"))&&o.optBoolean("qc_accepted",false)&&o.optJSONObject("quality")!=null&&o.optJSONObject("quality").optBoolean("accepted",false);}catch(Exception ignored){return false;}
    }

    public static JSONObject parse(String sourceNote){try{return new JSONObject(sourceNote==null?"{}":sourceNote);}catch(Exception ignored){return new JSONObject();}}
}
