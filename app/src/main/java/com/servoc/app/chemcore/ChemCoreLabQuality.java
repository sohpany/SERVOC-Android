package com.servoc.app.chemcore;

import android.database.Cursor;

import com.servoc.app.calibration.CalibrationValidationMarker;
import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Session-independent readiness snapshot for the HazRob analytical laboratory.
 *
 * The gates are deliberately separated:
 *  - configurationReady: hardware + accepted device R0 normalization are available for a controlled run;
 *  - captureReady: the powered array currently has enough valid channels to collect evidence;
 *  - environmentReady: chamber T/RH/P is actually available for environmental models;
 *  - referenceValidatedChannels: sensors having at least one active QC-accepted device/reference-gas curve.
 *
 * A clean-air R0 baseline is sufficient for Rs/R0 normalization, but never by itself permits
 * quantitative ppm interpretation.
 */
public final class ChemCoreLabQuality {
    public static final String SCHEMA="chemcore-lab-quality/1";
    public static final int REQUIRED_R0_CHANNELS=8;
    public static final int MIN_VALID_CHANNELS=6;

    public final boolean connected,mqPower,ads1,ads2,stationary;
    public final int validMqChannels,r0NormalizedChannels,fieldR0Channels,referenceValidatedChannels;
    public final boolean configurationReady,captureReady,environmentReady;
    public final String environmentSensor,environmentStatus,environmentAddress,environmentChipId;
    public final double temperatureC,humidityRh,pressureHpa;
    public final List<String> blockers,advisories;

    private ChemCoreLabQuality(boolean connected,boolean mqPower,boolean ads1,boolean ads2,boolean stationary,
            int validMqChannels,int r0NormalizedChannels,int fieldR0Channels,int referenceValidatedChannels,
            boolean configurationReady,boolean captureReady,boolean environmentReady,String environmentSensor,
            String environmentStatus,String environmentAddress,String environmentChipId,double temperatureC,
            double humidityRh,double pressureHpa,List<String> blockers,List<String> advisories){
        this.connected=connected;this.mqPower=mqPower;this.ads1=ads1;this.ads2=ads2;this.stationary=stationary;
        this.validMqChannels=validMqChannels;this.r0NormalizedChannels=r0NormalizedChannels;
        this.fieldR0Channels=fieldR0Channels;this.referenceValidatedChannels=referenceValidatedChannels;
        this.configurationReady=configurationReady;this.captureReady=captureReady;this.environmentReady=environmentReady;
        this.environmentSensor=nz(environmentSensor);this.environmentStatus=nz(environmentStatus);
        this.environmentAddress=nz(environmentAddress);this.environmentChipId=nz(environmentChipId);
        this.temperatureC=temperatureC;this.humidityRh=humidityRh;this.pressureHpa=pressureHpa;
        this.blockers=Collections.unmodifiableList(new ArrayList<>(blockers==null?Collections.<String>emptyList():blockers));
        this.advisories=Collections.unmodifiableList(new ArrayList<>(advisories==null?Collections.<String>emptyList():advisories));
    }

    /** Compatibility overload used by pure diagnostic tests and callers without database access. */
    public static ChemCoreLabQuality evaluate(JSONObject diagnostics,int driveLeft,int driveRight){
        return evaluateInternal(diagnostics,driveLeft,driveRight,-1);
    }

    /** Preferred runtime path: M4 availability is read from active QC-accepted calibration records. */
    public static ChemCoreLabQuality evaluate(JSONObject diagnostics,int driveLeft,int driveRight,ExperimentsDb db){
        return evaluateInternal(diagnostics,driveLeft,driveRight,referenceValidatedSensorCount(db));
    }

    private static ChemCoreLabQuality evaluateInternal(JSONObject diagnostics,int driveLeft,int driveRight,int referenceFromDb){
        JSONObject d=diagnostics==null?new JSONObject():diagnostics;
        JSONObject relay=d.optJSONObject("relay"),adc=d.optJSONObject("adc"),mq=d.optJSONObject("mq"),env=d.optJSONObject("env");
        boolean connected=diagnostics!=null;
        boolean power=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
        boolean a1=adc!=null&&adc.optBoolean("ads1",false),a2=adc!=null&&adc.optBoolean("ads2",false);
        boolean stationary=driveLeft==0&&driveRight==0;
        int valid=0,r0=0,field=0,telemetryReference=0,firmwareR0Mismatch=0;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject s=mq==null?null:mq.optJSONObject(hw.key);
            if(s!=null&&s.optBoolean("ok",s.optBoolean("valid",false)))valid++;
            // HR-0101-045 is the application-side normalization authority. The ESP32 may be synchronized later.
            if(FieldR0Baseline.has(hw.key)){r0++;field++;}
            else if(s!=null&&s.optBoolean("r0_calibrated",s.optBoolean("r0_loaded",false))){r0++;if("field_clean_air_r0".equals(s.optString("r0_source","")))field++;}
            if(s!=null&&s.optBoolean("r0_reference_validated",false))telemetryReference++;
            if(s!=null&&FieldR0Baseline.has(hw.key)){
                double fw=s.optDouble("r0",Double.NaN),accepted=FieldR0Baseline.value(hw.key);
                if(Double.isFinite(fw)&&fw>0&&Math.abs(fw-accepted)/accepted>0.001)firmwareR0Mismatch++;
            }
        }
        int reference=referenceFromDb>=0?referenceFromDb:telemetryReference;
        boolean envOk=env!=null&&env.optBoolean("ok",false)&&finite(env,"temperature")&&finite(env,"pressure");
        String envType=env==null?"NONE":env.optString("type",env.optString("sensor","NONE"));
        boolean needsHumidity="BME280".equalsIgnoreCase(envType);
        if(envOk&&needsHumidity&&!finite(env,"humidity"))envOk=false;
        boolean config=connected&&a1&&a2&&r0>=REQUIRED_R0_CHANNELS&&stationary;
        boolean capture=config&&power&&valid>=MIN_VALID_CHANNELS;
        List<String> blockers=new ArrayList<>(),advisories=new ArrayList<>();
        if(!connected)blockers.add("telemetry_unavailable");
        if(connected&&!a1)blockers.add("ads1115_0x48_not_ready");
        if(connected&&!a2)blockers.add("ads1115_0x49_not_ready");
        if(connected&&r0<REQUIRED_R0_CHANNELS)blockers.add("r0_normalization_incomplete:"+r0+"/"+REQUIRED_R0_CHANNELS);
        if(connected&&!stationary)blockers.add("robot_not_stationary");
        if(connected&&!power)advisories.add("mq_power_off_until_protocol_start");
        if(connected&&power&&valid<MIN_VALID_CHANNELS)advisories.add("valid_mq_channels_below_capture_gate:"+valid+"/8");
        if(firmwareR0Mismatch>0)advisories.add("esp32_r0_sync_pending:"+firmwareR0Mismatch+"/8;app_normalizes_from_rs_with_"+FieldR0Baseline.SESSION);
        if(!envOk)advisories.add("environmental_compensation_unavailable:"+(env==null?"missing":env.optString("status_code","unknown")));
        if(reference==0)advisories.add("reference_gas_m4_calibration_not_available");
        else if(reference<REQUIRED_R0_CHANNELS)advisories.add("reference_gas_calibration_partial:"+reference+"/8");
        return new ChemCoreLabQuality(connected,power,a1,a2,stationary,valid,r0,field,reference,config,capture,envOk,
                envType,env==null?"missing":env.optString("status_code",envOk?"ok":"unknown"),
                env==null?"":env.optString("address",""),env==null?"":env.optString("chip_id",""),
                value(env,"temperature"),value(env,"humidity"),value(env,"pressure"),blockers,advisories);
    }

    /** True only for array normalization/readiness. It is not a ppm-calibration claim. */
    public boolean fingerprintNormalizationReady(){return r0NormalizedChannels>=REQUIRED_R0_CHANNELS;}

    /** Candidate-specific M4 checks still apply even when this returns true. */
    public boolean anyReferenceGasCalibrationAvailable(){return referenceValidatedChannels>0;}

    public String compact(){
        String env=environmentReady?(environmentSensor+" OK"):((environmentSensor.isEmpty()?"ENV":environmentSensor)+" "+environmentStatus);
        return "ADS "+(ads1?"1":"0")+"/"+(ads2?"1":"0")+" • MQ "+validMqChannels+"/8 • R0 "+r0NormalizedChannels+"/8 • M4-ref "+referenceValidatedChannels+"/8 • "+env;
    }

    public JSONObject toJson(){
        try{
            JSONObject o=new JSONObject();o.put("schema",SCHEMA);o.put("connected",connected);o.put("mq_power",mqPower);o.put("ads1_ready",ads1);o.put("ads2_ready",ads2);o.put("stationary",stationary);
            o.put("valid_mq_channels",validMqChannels);o.put("r0_normalized_channels",r0NormalizedChannels);o.put("field_r0_channels",fieldR0Channels);o.put("reference_validated_channels",referenceValidatedChannels);
            o.put("configuration_ready",configurationReady);o.put("capture_ready",captureReady);o.put("fingerprint_normalization_ready",fingerprintNormalizationReady());o.put("any_reference_gas_calibration_available",anyReferenceGasCalibrationAvailable());
            JSONObject r0=new JSONObject();r0.put("source",FieldR0Baseline.SOURCE);r0.put("campaign",FieldR0Baseline.SESSION);r0.put("samples",FieldR0Baseline.SAMPLE_COUNT);r0.put("session_count",FieldR0Baseline.SESSION_COUNT);r0.put("reference_gas_validated",FieldR0Baseline.REFERENCE_GAS_VALIDATED);r0.put("environment_compensated",FieldR0Baseline.ENVIRONMENT_COMPENSATED);o.put("r0_standard",r0);
            JSONObject e=new JSONObject();e.put("ready",environmentReady);e.put("sensor",environmentSensor);e.put("status",environmentStatus);e.put("address",environmentAddress);e.put("chip_id",environmentChipId);putFinite(e,"temperature_c",temperatureC);putFinite(e,"humidity_rh",humidityRh);putFinite(e,"pressure_hpa",pressureHpa);o.put("environment",e);
            o.put("blockers",array(blockers));o.put("advisories",array(advisories));
            JSONObject calibration=new JSONObject();calibration.put("field_r0_normalization_ready",fingerprintNormalizationReady());calibration.put("field_r0_channels",fieldR0Channels);calibration.put("reference_gas_validated_channels",referenceValidatedChannels);calibration.put("global_ppm_permission","candidate_specific_by_design");calibration.put("ppm_rule","candidate requires complete M4 device/reference-gas support for all contributing quantitative curves + accepted open-set identity + validated domain");o.put("calibration_gate",calibration);
            return o;
        }catch(JSONException ex){throw new IllegalStateException("Unable to serialize ChemCore lab quality",ex);}
    }

    private static int referenceValidatedSensorCount(ExperimentsDb db){
        if(db==null)return 0;Set<Integer> sensors=new HashSet<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT sensor_id,reference_gas,source_note FROM calibration_curves WHERE active=1",null)){
            while(c.moveToNext()){
                int sensor=c.getInt(0);String gas=c.isNull(1)?"":c.getString(1),source=c.isNull(2)?"":c.getString(2);
                if(gas!=null&&!gas.trim().isEmpty()&&CalibrationValidationMarker.accepted(source)&&HazRobHardwareProfile.forSensor(sensor)!=null)sensors.add(sensor);
            }
        }catch(Exception ignored){}
        return sensors.size();
    }

    private static JSONArray array(List<String> x){JSONArray a=new JSONArray();if(x!=null)for(String s:x)a.put(s);return a;}
    private static boolean finite(JSONObject o,String key){return o!=null&&Double.isFinite(o.optDouble(key,Double.NaN));}
    private static double value(JSONObject o,String key){return o==null?Double.NaN:o.optDouble(key,Double.NaN);}
    private static void putFinite(JSONObject o,String key,double v)throws JSONException{o.put(key,Double.isFinite(v)?v:JSONObject.NULL);}
    private static String nz(String s){return s==null?"":s;}
}