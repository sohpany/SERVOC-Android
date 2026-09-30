package com.servoc.app.measurement;

import android.content.ContentValues;
import android.os.SystemClock;
import com.servoc.app.chemcore.ChemCoreSchema;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.field.FieldSessionRepository;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Automatically records normalized telemetry across ESP32 MQ cycles and controlled ChemCore captures. */
public final class TelemetryRecorder {
    private final ExperimentsDb db;private final FieldSessionRepository fieldSessions;private String sessionId,lastCompletedSessionId="";private long startedElapsed,lastSampleElapsed,sampleSeq;private boolean chemCoreCaptureSession=false;
    public TelemetryRecorder(ExperimentsDb db){this.db=db;this.fieldSessions=new FieldSessionRepository(db);ChemCoreSchema.ensure(db.getWritableDatabase());lastCompletedSessionId=db.getMeta("last_measurement_session_id","");}

    public synchronized void accept(JSONObject snapshot,int left,int right,boolean developer){
        if(snapshot==null)return;JSONObject m=snapshot.optJSONObject("measurement");String state=m==null?"idle":m.optString("state","idle");long now=SystemClock.elapsedRealtime();
        boolean firmwareActive="warmup".equals(state)||"measuring".equals(state);boolean forced=MeasurementSessionRegistry.chemCoreCaptureRequested();
        boolean activeState=firmwareActive||forced;
        if(sessionId==null&&activeState)start(forced?MeasurementSessionRegistry.chemCoreDeveloper():developer,now,forced);

        boolean terminalFirmware="ready".equals(state)||"error".equals(state)||"idle".equals(state);
        long samplePeriod=chemCoreCaptureSession?200:250;
        if(sessionId!=null&&(now-lastSampleElapsed>=samplePeriod||(!chemCoreCaptureSession&&terminalFirmware))){sample(snapshot,left,right,now);lastSampleElapsed=now;}

        String forcedStop=MeasurementSessionRegistry.chemCoreStopReason();
        if(sessionId!=null&&chemCoreCaptureSession&&!forcedStop.isEmpty())finish(snapshot,forcedStop);
        else if(sessionId!=null&&!chemCoreCaptureSession&&terminalFirmware)finish(snapshot,state);
    }

    public synchronized String activeSessionId(){return sessionId==null?"":sessionId;}
    public synchronized String lastCompletedSessionId(){return lastCompletedSessionId==null?"":lastCompletedSessionId;}

    private void start(boolean developer,long now,boolean chemCore){
        chemCoreCaptureSession=chemCore;sessionId=(developer?"DEV":"FIELD")+(chemCore?"-CHEMCORE-":"-")+System.currentTimeMillis();startedElapsed=lastSampleElapsed=now;sampleSeq=0;
        ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("started_ts",isoNow());v.put("kind",chemCore?"developer_chemcore_controlled_capture":(developer?"developer_mq_cycle":"field_mq_cycle"));v.put("operator_role",developer?"DEVELOPER":"USER");
        try{JSONObject meta=new JSONObject();meta.put("chemcore_controlled_capture",chemCore);meta.put("sample_period_ms",chemCore?200:250);meta.put("mq_scan_timing_capture",chemCore);meta.put("measurement_schema",CanonicalMeasurementNormalizer.SCHEMA);v.put("analytics",meta.toString());}catch(Exception ignored){}
        db.getWritableDatabase().insertOrThrow("measurement_sessions",null,v);
        // Bind only ordinary USER field acquisitions. Developer/ChemCore controlled captures stay outside the user domain.
        if(!developer&&!chemCore){
            FieldSessionRepository.FieldSession active=fieldSessions.activeFieldSession();
            if(active!=null)fieldSessions.linkMeasurement(active.id,sessionId);
        }
        MeasurementSessionRegistry.started(sessionId);
    }

    private void sample(JSONObject snapshot,int left,int right,long now){
        long tMs=Math.max(0,now-startedElapsed);ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("t_ms",tMs);
        JSONObject e=snapshot.optJSONObject("env"),mq=snapshot.optJSONObject("mq");v.put("env_payload",e==null?"{}":e.toString());v.put("mq_payload",mq==null?"{}":mq.toString());
        JSONObject state=new JSONObject();
        try{
            JSONObject transport=snapshot.optJSONObject("_transport");long utc=transport==null?System.currentTimeMillis():transport.optLong("received_utc_ms",System.currentTimeMillis());
            long monotonic=transport==null?now:transport.optLong("received_monotonic_ms",now);
            state.put("sample_seq",++sampleSeq);state.put("sample_utc_ms",utc);state.put("sample_monotonic_ms",monotonic);state.put("session_t_ms",tMs);
            state.put("measurement_schema",snapshot.optString("measurement_schema",CanonicalMeasurementNormalizer.SCHEMA));
            if(transport!=null)state.put("transport",new JSONObject(transport.toString()));
            state.put("left",left);state.put("right",right);state.put("hardware_profile",HazRobHardwareProfile.ID);state.put("chemcore_controlled_capture",chemCoreCaptureSession);
            JSONObject d=snapshot.optJSONObject("drive");if(d!=null){
                state.put("failsafe",d.optBoolean("failsafe",false));state.put("retrace",d.optBoolean("retrace",false));state.put("hold",d.optBoolean("hold",false));
                state.put("safety_auto_stop",d.optBoolean("safety_auto_stop",false));state.put("forward_hazard",d.optBoolean("forward_hazard",false));state.put("forward_blocked",d.optBoolean("forward_blocked",false));state.put("forward_block_reason",d.optString("forward_block_reason",""));
            }
            JSONObject sound=snapshot.optJSONObject("sound");if(sound!=null)state.put("sound",sound);
            JSONObject ir=snapshot.optJSONObject("ir");if(ir!=null)state.put("ir",ir);
            JSONObject ldr=snapshot.optJSONObject("ldr");if(ldr!=null&&ldr.optBoolean("installed",false))state.put("ldr",ldr);
            JSONObject adc=snapshot.optJSONObject("adc");if(adc!=null)state.put("adc",adc);
            JSONObject relay=snapshot.optJSONObject("relay");if(relay!=null)state.put("relay",relay);
            JSONObject sensorPower=snapshot.optJSONObject("sensor_power");if(sensorPower!=null)state.put("sensor_power",sensorPower);
            JSONObject measurement=snapshot.optJSONObject("measurement");if(measurement!=null){
                String measurementState=measurement.optString("state","idle");int stateCode=measurement.optInt("state_code",0);
                state.put("measurement_state",measurementState);state.put("measurement_state_code",stateCode);state.put("measurement_phase",stateCode);state.put("measurement_mask",measurement.optInt("mask",0));
                state.put("measurement_valid_channels",measurement.optInt("valid_channels",0));state.put("measurement_error",measurement.optString("error",""));
            }
        }catch(Exception ignored){}
        v.put("motion_payload",state.toString());db.getWritableDatabase().insert("telemetry_samples",null,v);
        if(chemCoreCaptureSession)persistMqTiming(mq,tMs);
    }

    private void persistMqTiming(JSONObject mq,long sampleTMs){
        if(mq==null||sessionId==null)return;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject r=mq.optJSONObject(hw.key);if(r==null||!r.has("scan_cycle")||!r.has("channel_offset_us"))continue;
            long cycle=r.optLong("scan_cycle",-1),offset=r.optLong("channel_offset_us",-1);if(cycle<0||offset<0)continue;
            ContentValues t=new ContentValues();t.put("session_id",sessionId);t.put("sample_t_ms",sampleTMs);t.put("scan_cycle_id",cycle);t.put("sensor_key",hw.key);t.put("channel_offset_us",offset);
            db.getWritableDatabase().insert("chemcore_channel_timing",null,t);
        }
    }

    private void finish(JSONObject snapshot,String state){
        final String completed=sessionId;final boolean wasChemCore=chemCoreCaptureSession;ContentValues v=new ContentValues();v.put("ended_ts",isoNow());v.put("payload",snapshot.toString());
        try{
            JSONObject meta=new JSONObject();meta.put("terminal_state",state);meta.put("contains_aux_sensors",true);meta.put("chemcore_controlled_capture",wasChemCore);
            meta.put("hardware_profile",HazRobHardwareProfile.ID);meta.put("measurement_schema",CanonicalMeasurementNormalizer.SCHEMA);meta.put("sample_count",sampleSeq);
            meta.put("mq_power_architecture","single_relay_8_sensors");meta.put("installed_relays","R1");meta.put("relay_gpio",25);meta.put("mq_sensor_count",8);meta.put("manual_warmup",true);meta.put("sample_period_ms",wasChemCore?200:250);meta.put("mq_scan_timing_capture",wasChemCore);
            JSONObject transport=snapshot.optJSONObject("_transport");if(transport!=null)meta.put("terminal_transport",transport.optString("mode","UNKNOWN"));
            JSONObject adc=snapshot.optJSONObject("adc");if(adc!=null&&adc.optJSONObject("acquisition")!=null)meta.put("adc_acquisition",adc.optJSONObject("acquisition"));
            v.put("analytics",meta.toString());
        }catch(Exception ignored){}
        db.getWritableDatabase().update("measurement_sessions",v,"session_id=?",new String[]{completed});
        lastCompletedSessionId=completed;db.putMeta("last_measurement_session_id",completed);db.putMeta("last_measurement_session_terminal_state",state);
        MeasurementSessionRegistry.completed(completed);if(wasChemCore)MeasurementSessionRegistry.clearChemCoreCapture();sessionId=null;startedElapsed=lastSampleElapsed=sampleSeq=0;chemCoreCaptureSession=false;
    }

    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
