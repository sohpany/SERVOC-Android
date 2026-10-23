package com.servoc.app.measurement;

import android.content.SharedPreferences;
import org.json.JSONObject;

/**
 * Read-only Echo diagnostic/presentation. 5 V to 3.3 V level translation changes
 * logic amplitude, NOT time-of-flight; no 5/3.3 distance multiplier is valid.
 * User offset never feeds firmware collision thresholds or safety interlocks.
 */
public final class EchoReading {
    public static final String PREF_OFFSET_CM="echo_display_offset_cm";
    public static final double MAX_OFFSET_CM=200.0;
    private static final double CM_PER_MICROSECOND=0.0343/2.0;
    public final boolean valid;
    public final double rawCm,displayCm,diagnosticCm;
    public final long pulseUs,timeouts;
    public final String status;

    private EchoReading(boolean valid,double rawCm,double displayCm,double diagnosticCm,long pulseUs,long timeouts,String status){
        this.valid=valid;this.rawCm=rawCm;this.displayCm=displayCm;this.diagnosticCm=diagnosticCm;
        this.pulseUs=pulseUs;this.timeouts=timeouts;this.status=status;
    }
    public static double offsetCm(SharedPreferences prefs){
        return prefs==null?0.0:prefs.getFloat(PREF_OFFSET_CM,0.0f);
    }
    public static boolean saveOffset(SharedPreferences prefs,double cm){
        return prefs!=null&&Double.isFinite(cm)&&Math.abs(cm)<=MAX_OFFSET_CM
            &&prefs.edit().putFloat(PREF_OFFSET_CM,(float)cm).commit();
    }
    public static EchoReading from(JSONObject telemetry,SharedPreferences prefs){
        JSONObject device=telemetry==null?null:telemetry.optJSONObject("device");
        JSONObject p=device==null?null:device.optJSONObject("proximity");
        JSONObject raw=telemetry==null?null:telemetry.optJSONObject("_data_api");
        JSONObject rawDevice=raw==null?null:raw.optJSONObject("device");
        JSONObject rawP=rawDevice==null?null:rawDevice.optJSONObject("proximity");
        if(p==null)p=rawP;
        if(p==null)return new EchoReading(false,Double.NaN,Double.NaN,Double.NaN,0,0,"no_telemetry");
        long pulse=p.optLong("pulse_us",0);
        if(pulse<=0&&rawP!=null)pulse=rawP.optLong("pulse_us",0);
        long timeouts=p.optLong("timeouts",0);
        if(timeouts<=0&&rawP!=null)timeouts=rawP.optLong("timeouts",0);
        String status=p.optString("status","unknown");
        double rawCm=p.optDouble("distance_cm",p.optDouble("cm",Double.NaN));
        // A long diagnostic pulse is not a legitimate range result.
        double diagnostic=pulse>0?pulse*CM_PER_MICROSECOND:Double.NaN;
        boolean valid=p.optBoolean("valid",false)&&Double.isFinite(rawCm)&&rawCm>=2.0&&rawCm<=400.0;
        double display=valid?rawCm+offsetCm(prefs):Double.NaN;
        // An offset that would turn a valid reading negative is rejected for display.
        if(!Double.isFinite(display)||display<0.0)valid=false;
        return new EchoReading(valid,rawCm,valid?display:Double.NaN,diagnostic,pulse,timeouts,status);
    }
}
