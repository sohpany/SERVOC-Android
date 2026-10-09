package com.servoc.app.calibration;

import org.json.JSONObject;

/**
 * Single application-side normalization authority for the accepted HazRob device R0 state.
 *
 * The active value is resolved by ManualR0Calibration: a persisted positive device-specific
 * override wins; otherwise the accepted field clean-air baseline is used. No synthetic/default R0
 * is manufactured when neither source exists.
 *
 * Two deliberately separate paths are exposed:
 *  - ratio(): strict analytical/quantitative path. A channel must be marked valid.
 *  - availableRatio()+evidenceWeight(): corroborative-evidence path. Any physically interpretable
 *    ratio is retained even when quality is degraded, but its evidentiary weight is reduced.
 *
 * Missing power/ADC/source data never manufacture chemical evidence; those states remain useful as
 * measurement-health evidence only. This preserves all available information without allowing a
 * weak channel to silently become reportable quantitative data.
 */
public final class AcceptedR0Normalizer {
    public static final String SCHEMA="hazrob-r0-normalizer/2";
    public static final String SOURCE="app_accepted_device_r0";
    private AcceptedR0Normalizer(){}

    /** Active device-specific R0: manual override when present, accepted field baseline otherwise. */
    public static double acceptedR0(String sensorKey){return ManualR0Calibration.get(sensorKey);}

    /** Strict path used by canonical features and reportable quantitative calculations. */
    public static double ratio(String sensorKey,JSONObject sensor){
        if(sensor==null||noChemicalInput(sensor)||!sensor.optBoolean("valid",sensor.optBoolean("ok",false)))return Double.NaN;
        return physicalRatio(sensorKey,sensor);
    }

    /**
     * Corroborative path: returns a finite physical ratio whenever the telemetry contains one,
     * irrespective of the binary valid flag. The caller MUST apply evidenceWeight(sensor).
     */
    public static double availableRatio(String sensorKey,JSONObject sensor){
        if(sensor==null)return Double.NaN;
        return physicalRatio(sensorKey,sensor);
    }

    /**
     * Archived samples must be replayed against the R0 captured WITH that sample,
     * never against today's device override. Otherwise updating R0 silently rewrites
     * the chemical fingerprint and score of an older stored session.
     *
     * Legacy samples without saved R0 may only use their previously recorded ratio;
     * no current or synthetic R0 is substituted during history re-analysis.
     */
    public static double recordedRatio(String sensorKey,JSONObject recorded){
        if(recorded==null||noChemicalInput(recorded)||!recorded.optBoolean("valid",recorded.optBoolean("ok",false)))
            return Double.NaN;
        return recordedPhysicalRatio(recorded);
    }

    /** Quality-weighted historical corroboration uses the same frozen R0 evidence. */
    public static double availableRecordedRatio(String sensorKey,JSONObject recorded){
        return recordedPhysicalRatio(recorded);
    }

    /** Did this saved sample actually carry the active device R0 with provenance? */
    public static boolean usesRecordedDeviceR0(String sensorKey,JSONObject recorded){
        if(recorded==null||noChemicalInput(recorded)||!recorded.optBoolean("valid",recorded.optBoolean("ok",false)))
            return false;
        double rs=recorded.optDouble("rs",Double.NaN),r0=recorded.optDouble("r0",Double.NaN);
        if(!(Double.isFinite(rs)&&rs>0&&Double.isFinite(r0)&&r0>0))return false;
        String source=recorded.optString("r0_source","");
        boolean known="manual_device_r0".equals(source)||FieldR0Baseline.SOURCE.equals(source);
        boolean provenance=recorded.has("r0_provenance_complete")
                ?recorded.optBoolean("r0_provenance_complete",false)
                :!recorded.optString("r0_session","").isEmpty()&&!recorded.optString("r0_utc","").isEmpty();
        return known&&provenance;
    }

    private static double recordedPhysicalRatio(JSONObject recorded){
        if(recorded==null)return Double.NaN;
        double r0=recorded.optDouble("r0",Double.NaN),rs=recorded.optDouble("rs",Double.NaN);
        if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0)return rs/r0;
        double frozen=recorded.optDouble("ratio",Double.NaN);
        return Double.isFinite(frozen)&&frozen>0?frozen:Double.NaN;
    }

    /**
     * Support weight for chemically interpretable but degraded measurements.
     * 1.0 = full-quality signal; 0 < w < 1 = support-only; 0 = no chemical signal available.
     */
    public static double evidenceWeight(JSONObject sensor){
        if(sensor==null)return 0.0;
        JSONObject quality=sensor.optJSONObject("quality");
        String status=telemetryStatus(sensor);
        boolean valid=sensor.optBoolean("valid",sensor.optBoolean("ok",false));
        double w;
        // A stale/contradictory valid flag must never override an explicit absent-input state.
        if(noChemicalInput(sensor))w=0.0;
        else if(valid)w=1.0;
        else if("VALID".equals(status))w=0.35; // inconsistent status: support only
        else if("STALE".equals(status))w=0.55;
        else if("INVALID".equals(status)||"NORMALIZATION_INVALID".equals(status))w=0.35;
        else if("RAIL_HIGH_OR_SATURATED".equals(status))w=0.25;
        else if("OPEN_OR_LOW_OUTPUT".equals(status))w=0.15;
        else if("POWER_OFF".equals(status)||"ADC_UNAVAILABLE".equals(status)||"MISSING_SOURCE".equals(status))w=0.0;
        else w=physicalRatio("",sensor)>0?0.30:0.0;

        if(quality!=null){
            if(quality.optBoolean("noise_evaluable",false)&&quality.optBoolean("noise_flag",false))w*=0.60;
            String temporal=quality.optString("temporal_state","").trim().toUpperCase();
            if("UNSTABLE".equals(temporal))w*=0.65;
            if("SATURATED".equals(temporal))w=Math.min(w,0.25);
            if("STUCK".equals(temporal))w=Math.min(w,0.20);
        }
        return clamp(w,0.0,1.0);
    }

    public static String evidenceQualityClass(JSONObject sensor){
        double w=evidenceWeight(sensor);if(w>=0.85)return"FULL";if(w>=0.45)return"DEGRADED";if(w>0)return"WEAK";return"DIAGNOSTIC_ONLY";
    }

    /** True when a valid Rs value is normalized with any accepted device R0 source. */
    public static boolean usesAcceptedDeviceR0(String sensorKey,JSONObject sensor){
        if(sensor==null||noChemicalInput(sensor)||!sensor.optBoolean("valid",sensor.optBoolean("ok",false)))return false;
        double r0=acceptedR0(sensorKey),rs=sensor.optDouble("rs",Double.NaN);
        return Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0;
    }

    /** Backward-compatible field-baseline predicate; manual overrides intentionally return false. */
    public static boolean usesAcceptedFieldR0(String sensorKey,JSONObject sensor){
        return usesAcceptedDeviceR0(sensorKey,sensor)&&FieldR0Baseline.SOURCE.equals(ManualR0Calibration.getSource(sensorKey));
    }

    public static String provenance(String sensorKey,JSONObject sensor){
        if(usesAcceptedDeviceR0(sensorKey,sensor)){
            String source=ManualR0Calibration.getSource(sensorKey),session=ManualR0Calibration.getSession(sensorKey);
            if(source==null||source.isEmpty())source="accepted_device_r0";
            return session==null||session.isEmpty()?source:source+":"+session;
        }
        double v=availableRatio(sensorKey,sensor);return Double.isFinite(v)&&v>0?"telemetry_or_rs_ratio_corroborative":"no_chemical_ratio_available";
    }

    /** Explicit source/ADC/power loss has priority over contradictory stale validity bits. */
    private static boolean noChemicalInput(JSONObject sensor){
        String status=telemetryStatus(sensor);
        return "POWER_OFF".equals(status)||"ADC_UNAVAILABLE".equals(status)
                ||"MISSING_SOURCE".equals(status);
    }
    private static String telemetryStatus(JSONObject sensor){
        if(sensor==null)return "";
        String status=sensor.optString("status","").trim();
        if(status.isEmpty()){
            JSONObject quality=sensor.optJSONObject("quality");
            status=quality==null?"":quality.optString("status","").trim();
        }
        return status.toUpperCase(java.util.Locale.US);
    }

    private static double physicalRatio(String sensorKey,JSONObject sensor){
        if(sensor==null)return Double.NaN;
        double r0=acceptedR0(sensorKey),rs=sensor.optDouble("rs",Double.NaN);
        if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0)return rs/r0;
        double telemetry=sensor.optDouble("ratio",Double.NaN);
        return Double.isFinite(telemetry)&&telemetry>0?telemetry:Double.NaN;
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
