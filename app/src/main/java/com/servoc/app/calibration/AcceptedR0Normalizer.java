package com.servoc.app.calibration;

import org.json.JSONObject;

/**
 * Single application-side normalization authority for the accepted HazRob field R0 campaign.
 *
 * The preferred path deliberately reconstructs Rs/R0 from the physical sensor resistance Rs and
 * the controlled HR-0101-045 baseline. This keeps analysis synchronized with the accepted R0
 * standard even when the installed ESP32 has not yet been reflashed with the latest constants.
 * Telemetry ratio is retained only as a compatibility fallback when Rs is unavailable.
 */
public final class AcceptedR0Normalizer {
    public static final String SCHEMA="hazrob-r0-normalizer/1";
    public static final String SOURCE="app_accepted_field_r0";
    private AcceptedR0Normalizer(){}

    public static double acceptedR0(String sensorKey){return FieldR0Baseline.value(sensorKey);}

    public static double ratio(String sensorKey,JSONObject sensor){
        if(sensor==null||!sensor.optBoolean("valid",sensor.optBoolean("ok",false)))return Double.NaN;
        double r0=acceptedR0(sensorKey),rs=sensor.optDouble("rs",Double.NaN);
        if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0)return rs/r0;
        double telemetry=sensor.optDouble("ratio",Double.NaN);
        return Double.isFinite(telemetry)&&telemetry>0?telemetry:Double.NaN;
    }

    public static boolean usesAcceptedFieldR0(String sensorKey,JSONObject sensor){
        if(sensor==null||!sensor.optBoolean("valid",sensor.optBoolean("ok",false)))return false;
        double r0=acceptedR0(sensorKey),rs=sensor.optDouble("rs",Double.NaN);
        return Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0;
    }

    public static String provenance(String sensorKey,JSONObject sensor){
        if(usesAcceptedFieldR0(sensorKey,sensor))return SOURCE+":"+FieldR0Baseline.SESSION;
        return "telemetry_ratio_fallback";
    }
}