package com.servoc.app.measurement;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;

/**
 * Final Android-side measurement normalization boundary for HazRob 0.10.2 / HR069.
 *
 * Firmware owns acquisition and publishes raw ADC/voltage/Rs plus its own field-baseline ratio.
 * Android owns the active analytical R0 normalization. This class preserves the firmware values
 * as audit provenance, verifies the active Rs/R0 relation, annotates transport/time provenance,
 * and exposes one quality state consumed by Live, Tracking and persisted Session data.
 */
public final class CanonicalMeasurementNormalizer {
    private CanonicalMeasurementNormalizer() {}

    public static final String SCHEMA="hazrob-measurement/1";
    public static final String NORMALIZATION_OWNER="ANDROID_ACTIVE_R0";
    private static final long STALE_AGE_US=500_000L;
    private static final double LOW_OUTPUT_V=0.010;
    private static final double HIGH_OUTPUT_V=4.999;

    public static JSONObject enrich(JSONObject snapshot,String transportMode){
        if(snapshot==null)snapshot=new JSONObject();
        try{
            long utc=System.currentTimeMillis(),mono=System.nanoTime()/1_000_000L;
            JSONObject transport=new JSONObject();
            String mode="REMOTE".equalsIgnoreCase(transportMode)?"REMOTE":"DIRECT";
            transport.put("mode",mode);transport.put("received_utc_ms",utc);transport.put("received_monotonic_ms",mono);
            transport.put("offline_capable","DIRECT".equals(mode));
            snapshot.put("_transport",transport);snapshot.put("measurement_schema",SCHEMA);

            JSONObject rawData=snapshot.optJSONObject("_data_api");
            JSONObject rawMq=rawData==null?null:rawData.optJSONObject("mq");
            JSONObject normalizedMq=snapshot.optJSONObject("mq");
            JSONObject adc=snapshot.optJSONObject("adc");
            if(normalizedMq!=null)enrichMq(normalizedMq,rawMq,adc);
            enrichEnvironment(snapshot.optJSONObject("env"),rawData==null?null:rawData.optJSONObject("env"));
        }catch(Exception ignored){}
        return snapshot;
    }

    private static void enrichMq(JSONObject mq,JSONObject rawMq,JSONObject adc){
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject n=mq.optJSONObject(hw.key);if(n==null)continue;
            JSONObject r=rawMq==null?null:rawMq.optJSONObject(hw.key);
            try{
                if(r!=null){
                    copyNumber(r,n,"r0","firmware_r0");copyNumber(r,n,"ratio","firmware_ratio");
                    copyString(r,n,"r0_source","firmware_r0_source");copyString(r,n,"r0_session","firmware_r0_session");copyString(r,n,"r0_utc","firmware_r0_utc");
                    copyNumber(r,n,"r0_samples","firmware_r0_samples");copyNumber(r,n,"r0_window_s","firmware_r0_window_s");
                    if(r.has("r0_reference_validated"))n.put("firmware_r0_reference_validated",r.optBoolean("r0_reference_validated",false));
                    if(r.has("r0_environment_compensated"))n.put("firmware_r0_environment_compensated",r.optBoolean("r0_environment_compensated",false));
                }

                double rs=n.optDouble("rs",Double.NaN),r0=n.optDouble("r0",Double.NaN),reported=n.optDouble("ratio",Double.NaN);
                boolean ratioPossible=finitePositive(rs)&&finitePositive(r0);double canonical=ratioPossible?rs/r0:Double.NaN;
                JSONObject normalization=new JSONObject();normalization.put("owner",NORMALIZATION_OWNER);normalization.put("formula","ratio=rs/r0");
                normalization.put("r0_source",n.optString("r0_source","unavailable"));normalization.put("r0_reference_validated",n.optBoolean("r0_reference_validated",false));
                normalization.put("r0_provenance_complete",hasR0Provenance(n));normalization.put("r0_age_known",n.optBoolean("r0_age_known",false));
                if(n.has("r0_age_ms"))normalization.put("r0_age_ms",n.optLong("r0_age_ms"));if(n.has("r0_session"))normalization.put("r0_session",n.optString("r0_session",""));if(n.has("r0_utc"))normalization.put("r0_utc",n.optString("r0_utc",""));
                if(ratioPossible){
                    normalization.put("input_ratio",Double.isFinite(reported)?reported:JSONObject.NULL);
                    normalization.put("ratio_consistent",Double.isFinite(reported)&&relativeError(reported,canonical)<=1e-6);
                    n.put("ratio",canonical);
                }else normalization.put("ratio_consistent",false);
                n.put("normalization",normalization);

                boolean powered=n.optBoolean("powered",false);boolean adsReady=adsReady(adc,hw.adsIndex);
                boolean incomingOk=n.optBoolean("ok",n.optBoolean("valid",false));
                double ao=n.optDouble("ao",Double.NaN);long age=r==null?n.optLong("age_us",-1):r.optLong("age_us",n.optLong("age_us",-1));
                boolean stale=age>STALE_AGE_US;
                String status;
                if(r==null)status="MISSING_SOURCE";
                else if(!powered)status="POWER_OFF";
                else if(!adsReady)status="ADC_UNAVAILABLE";
                else if(!incomingOk&&Double.isFinite(ao)&&ao<=LOW_OUTPUT_V)status="OPEN_OR_LOW_OUTPUT";
                else if(!incomingOk&&Double.isFinite(ao)&&ao>=HIGH_OUTPUT_V)status="RAIL_HIGH_OR_SATURATED";
                else if(!incomingOk)status="INVALID";
                else if(!ratioPossible)status="NORMALIZATION_INVALID";
                else if(stale)status="STALE";
                else status="VALID";
                boolean valid="VALID".equals(status);

                JSONObject quality=new JSONObject();quality.put("status",status);quality.put("valid",valid);quality.put("powered",powered);quality.put("adc_ready",adsReady);
                quality.put("stale",stale);quality.put("sample_age_us",age>=0?age:JSONObject.NULL);
                quality.put("low_output_threshold_v",LOW_OUTPUT_V);quality.put("high_output_threshold_v",HIGH_OUTPUT_V);
                quality.put("noise_evaluable",false);quality.put("noise_flag",JSONObject.NULL);quality.put("noise_status","NOT_EVALUATED_NO_PER_CONVERSION_VARIANCE");
                quality.put("manufacturer_heater_protocol_valid",HazRobHardwareProfile.manufacturerHeaterProtocolValid(hw.key));
                n.put("quality",quality);n.put("status",status);n.put("valid",valid);
            }catch(Exception ignored){}
        }
    }

    private static void enrichEnvironment(JSONObject env,JSONObject rawEnv){
        if(env==null)return;
        try{
            env.put("source","HR069_ENV_I2C");
            if(rawEnv!=null&&rawEnv.optJSONObject("bus")!=null)env.put("bus",new JSONObject(rawEnv.optJSONObject("bus").toString()));
            boolean ok=env.optBoolean("ok",false);String sensor=env.optString("sensor",env.optString("type","NONE"));
            env.put("provenance_complete",!"NONE".equalsIgnoreCase(sensor)&&(env.has("address")||env.has("last_address")));
            env.put("quality_status",ok?"VALID":"UNAVAILABLE");
        }catch(Exception ignored){}
    }

    private static boolean adsReady(JSONObject adc,int index){if(adc==null)return false;return index==0?adc.optBoolean("ads1",false):adc.optBoolean("ads2",false);}
    private static boolean finitePositive(double x){return Double.isFinite(x)&&x>0;}
    private static double relativeError(double a,double b){double d=Math.max(1e-12,Math.max(Math.abs(a),Math.abs(b)));return Math.abs(a-b)/d;}
    private static boolean hasR0Provenance(JSONObject n){if(n.has("r0_provenance_complete"))return n.optBoolean("r0_provenance_complete",false);String source=n.optString("r0_source","");return !source.isEmpty()&&!"unavailable".equals(source)&&n.has("r0_session")&&n.has("r0_utc");}
    private static void copyNumber(JSONObject from,JSONObject to,String source,String target)throws Exception{if(from.has(source)&&!from.isNull(source))to.put(target,from.optDouble(source));}
    private static void copyString(JSONObject from,JSONObject to,String source,String target)throws Exception{if(from.has(source)&&!from.isNull(source))to.put(target,from.optString(source,""));}
}
