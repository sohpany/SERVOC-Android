package com.servoc.app.calibration;

import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * One-point field transfer calibration for the MAX4466 microphone.
 *
 * HR069 keeps firmware telemetry at the electrically defensible level (RMS/P-P voltage) and does
 * not assume a fixed MAX4466 gain. Android derives a repeatable ADC-relative level from RMS voltage
 * and maps it to a reference sound-level meter with one additive dB offset:
 *
 *   raw_dbfs = 20 * log10(rms_v / 3.3 V)
 *   compensated_db = raw_dbfs + offset_db
 *   offset_db = reference_meter_db - raw_dbfs
 *
 * The offset is valid only while the MAX4466 trimmer/gain and hardware placement remain unchanged.
 */
public final class MicrophoneDbCalibration {
    public static final String PREF_OFFSET_DB="microphone_db_offset_db";
    public static final String PREF_SET_AT="microphone_db_offset_set_at";
    public static final double ADC_FULL_SCALE_V=3.3;
    public static final double MIN_OFFSET_DB=-160.0;
    public static final double MAX_OFFSET_DB=160.0;

    private MicrophoneDbCalibration(){}

    public static double rawDbFs(double rmsV){
        if(!Double.isFinite(rmsV)||rmsV<=0.0)return Double.NaN;
        double ratio=rmsV/ADC_FULL_SCALE_V;
        if(ratio<=0.0)return Double.NaN;
        return 20.0*Math.log10(ratio);
    }

    public static double rawDbFs(JSONObject sound){
        if(sound==null)return Double.NaN;
        double rms=sound.optDouble("rms_v",sound.optDouble("rms",Double.NaN));
        return rawDbFs(rms);
    }

    public static boolean configured(SharedPreferences p){return p!=null&&p.contains(PREF_OFFSET_DB);}

    public static double offsetDb(SharedPreferences p){
        if(p==null)return 0.0;
        return clamp(p.getFloat(PREF_OFFSET_DB,0f));
    }

    public static double compensatedDb(JSONObject sound,SharedPreferences p){
        double raw=rawDbFs(sound);if(!Double.isFinite(raw))return Double.NaN;
        return raw+offsetDb(p);
    }

    public static double compensate(double rawDb,double offsetDb){
        if(!Double.isFinite(rawDb))return Double.NaN;
        return rawDb+clamp(offsetDb);
    }

    public static double requiredOffset(double hazRobRawDb,double referenceDb){
        if(!Double.isFinite(hazRobRawDb)||!Double.isFinite(referenceDb))return Double.NaN;
        return clamp(referenceDb-hazRobRawDb);
    }

    public static double clamp(double v){
        if(!Double.isFinite(v))return 0.0;
        return Math.max(MIN_OFFSET_DB,Math.min(MAX_OFFSET_DB,v));
    }

    public static void save(SharedPreferences p,double offsetDb){
        if(p==null)return;
        p.edit().putFloat(PREF_OFFSET_DB,(float)clamp(offsetDb)).putLong(PREF_SET_AT,System.currentTimeMillis()).apply();
    }

    public static void clear(SharedPreferences p){
        if(p==null)return;
        p.edit().remove(PREF_OFFSET_DB).remove(PREF_SET_AT).apply();
    }
}
