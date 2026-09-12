package com.servoc.app.communication;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.measurement.LiveSensorDiagnostic;
import org.json.JSONObject;
import java.util.Locale;

/** Normalizes HazRob API4 while tolerating staged/legacy field names. */
public final class RobotProtocolV4 {
    private RobotProtocolV4(){}

    public static JSONObject normalize(JSONObject diag,JSONObject data,int commandedLeft,int commandedRight){
        JSONObject out=new JSONObject();
        try{
            if(diag==null)diag=new JSONObject();if(data==null)data=new JSONObject();
            out.put("firmware",diag.optString("fw",diag.optString("firmware","HazRob")));
            out.put("api_version",diag.has("api")?diag.optInt("api",4):diag.optInt("api_version",4));
            out.put("heap",diag.optLong("heap",0));out.put("clients",diag.optInt("clients",0));
            out.put("hardware_profile",diag.optString("hardware_profile",HazRobHardwareProfile.ID));
            out.put("mq_groups",HazRobHardwareProfile.MQ_GROUPS);
            out.put("installed_relay_mask",HazRobHardwareProfile.INSTALLED_RELAY_MASK);
            out.put("relay_channels",HazRobHardwareProfile.RELAY_CHANNELS);

            JSONObject dd=first(data.optJSONObject("drive"),diag.optJSONObject("drive"));
            boolean retrace=dd!=null?dd.optBoolean("retrace",false):diag.optBoolean("retrace",data.optBoolean("retrace",false));
            boolean failsafe=dd!=null?dd.optBoolean("failsafe",false):diag.optBoolean("failsafe",false);
            JSONObject drive=new JSONObject();drive.put("left",commandedLeft);drive.put("right",commandedRight);drive.put("failsafe",failsafe);drive.put("retrace",retrace);if(dd!=null&&dd.has("history"))drive.put("history",dd.optInt("history",0));out.put("drive",drive);out.put("retrace",retrace);

            JSONObject relayNorm=relay(diag,data),adcNorm=adc(diag,data),mqNorm=mq(data.optJSONObject("mq"),false);
            out.put("env",environment(diag,data.optJSONObject("env")));
            out.put("device",device(diag,data));
            out.put("relay",relayNorm);
            out.put("sound",sound(first(data.optJSONObject("sound"),diag.optJSONObject("sound"))));
            out.put("ir",ir(diag,first(data.optJSONObject("ir"),diag.optJSONObject("ir"))));
            out.put("ldr",ldr(diag,first(data.optJSONObject("ldr"),diag.optJSONObject("ldr"))));
            out.put("alert",alert(first(data.optJSONObject("alert"),diag.optJSONObject("alert"))));
            out.put("adc",adcNorm);
            out.put("mq",mqNorm);
            out.put("sensor_power",LiveSensorDiagnostic.evaluate(relayNorm,adcNorm,mqNorm).toJson());
            out.put("measurement",measurement(data.optJSONObject("measurement")));
            out.put("link",link(diag));
            out.put("_diag_api",diag);out.put("_data_api",data);
        }catch(Exception ignored){}
        return out;
    }

    private static JSONObject first(JSONObject a,JSONObject b){return a!=null?a:b;}

    private static JSONObject environment(JSONObject diag,JSONObject e){
        JSONObject x=new JSONObject();try{
            if(e==null)e=new JSONObject();boolean ok=e.optBoolean("ok",false);
            int type=diag.optInt("envType",0),addr=diag.optInt("envAddr",0),chip=diag.optInt("envId",0);
            String name=type==1?"BME280":type==2?"BMP280":"NONE";
            x.put("ok",ok);x.put("i2c_found",type!=0||addr!=0||chip!=0);x.put("type",name);x.put("sensor",name);
            x.put("address",addr>0?String.format(Locale.US,"0x%02X",addr):JSONObject.NULL);x.put("chip_id",chip>0?String.format(Locale.US,"0x%02X",chip):JSONObject.NULL);
            x.put("temperature",finiteAny(e,"t","temperature"));x.put("humidity",finiteAny(e,"h","humidity"));x.put("pressure",finiteAny(e,"p","pressure"));x.put("humidity_available",type!=2&&!e.isNull(e.has("h")?"h":"humidity"));
        }catch(Exception ignored){}return x;
    }

    /** Device-health telemetry is intentionally separate from the ChemCore chamber environment. */
    private static JSONObject device(JSONObject diag,JSONObject data){
        JSONObject out=new JSONObject();try{
            JSONObject d=first(data==null?null:data.optJSONObject("device"),diag==null?null:diag.optJSONObject("device"));if(d==null)d=new JSONObject();
            JSONObject b=d.optJSONObject("battery"),p=d.optJSONObject("proximity"),t=d.optJSONObject("internal_temp"),e=d.optJSONObject("energy_basis");
            JSONObject battery=new JSONObject();if(b==null)b=new JSONObject();battery.put("voltage_v",finiteAny(b,"v","voltage_v"));battery.put("raw",b.has("raw")?b.optLong("raw",0):JSONObject.NULL);battery.put("estimated",b.optBoolean("estimated",true));out.put("battery",battery);
            JSONObject proximity=new JSONObject();if(p==null)p=new JSONObject();proximity.put("distance_cm",finiteAny(p,"cm","distance_cm"));proximity.put("warning",p.optBoolean("warning",false));proximity.put("trig_gpio",p.has("trig")?p.optInt("trig",23):23);proximity.put("echo_gpio",p.has("echo")?p.optInt("echo",39):39);out.put("proximity",proximity);
            JSONObject internal=new JSONObject();if(t==null)t=new JSONObject();internal.put("installed",t.optBoolean("installed",false));internal.put("temperature_c",finiteAny(t,"c","temperature_c"));internal.put("gpio",t.has("gpio")?t.optInt("gpio",14):14);out.put("internal_temp",internal);
            JSONObject energy=new JSONObject();if(e==null)e=new JSONObject();energy.put("uptime_ms",Math.max(0,e.optLong("uptime_ms",0)));energy.put("drive_eq_ms",Math.max(0,e.optLong("drive_eq_ms",0)));energy.put("mq_on_ms",Math.max(0,e.optLong("mq_on_ms",0)));out.put("energy_basis",energy);
        }catch(Exception ignored){}return out;
    }

    private static JSONObject relay(JSONObject diag,JSONObject data){
        JSONObject x=new JSONObject();try{
            JSONObject r=first(data==null?null:data.optJSONObject("relay"),diag==null?null:diag.optJSONObject("relay"));
            int raw=0;long powerMs=0;boolean on=false;
            if(r!=null){raw=r.has("mask")?r.optInt("mask",0):(r.optBoolean("on",false)?1:0);on=r.has("on")?r.optBoolean("on",false):(raw!=0);powerMs=r.optLong("power_ms",0);}
            else if(data!=null&&data.has("relayMask")){raw=data.optInt("relayMask",0);on=raw!=0;}
            else if(diag!=null&&diag.has("relayMask")){raw=diag.optInt("relayMask",0);on=raw!=0;}
            else if(data!=null){int g=data.optInt("relay",0);raw=g>0?1:0;on=raw!=0;}
            int mask=on||raw!=0?1:0;
            x.put("mask",mask);x.put("raw_mask",raw);x.put("on",mask==1);x.put("power_ms",powerMs);x.put("channels",1);x.put("manual_control",true);
        }catch(Exception ignored){}return x;
    }

    private static JSONObject sound(JSONObject s){JSONObject x=new JSONObject();try{if(s==null)s=new JSONObject();Object rms=finiteAny(s,"rms_v","rms"),p2p=finiteAny(s,"p2p_v","p2p");x.put("rms_v",rms);x.put("p2p_v",p2p);x.put("ok",rms!=JSONObject.NULL||p2p!=JSONObject.NULL);x.put("calibrated_db_spl",false);}catch(Exception ignored){}return x;}
    private static JSONObject ir(JSONObject diag,JSONObject i){JSONObject x=new JSONObject();try{boolean warning=i!=null?i.optBoolean("warning",diag.optBoolean("ir",false)):diag.optBoolean("ir",false);x.put("installed",true);x.put("warning",warning);x.put("level",i==null?JSONObject.NULL:i.optInt("level",-1));x.put("drive_interlock",false);}catch(Exception ignored){}return x;}
    private static JSONObject ldr(JSONObject diag,JSONObject i){JSONObject x=new JSONObject();try{boolean installed=i!=null&&i.optBoolean("installed",i.has("active")||i.has("level"));x.put("installed",installed);x.put("active",installed&&i.optBoolean("active",diag.optBoolean("ldr",false)));x.put("level",installed&&i.has("level")?i.optInt("level",-1):JSONObject.NULL);x.put("gpio",installed&&i.has("gpio")?i.optInt("gpio",34):34);x.put("quantitative_lux",false);}catch(Exception ignored){}return x;}
    private static JSONObject alert(JSONObject a){JSONObject x=new JSONObject();try{if(a==null)a=new JSONObject();x.put("active",a.optBoolean("active",false));x.put("level",a.optInt("level",0));x.put("name",a.optString("name","none"));x.put("gpio",a.optInt("gpio",26));x.put("gas",Math.max(0,Math.min(2,a.optInt("gas",0))));x.put("ir",a.optBoolean("ir",false));x.put("link",a.optBoolean("link",false));}catch(Exception ignored){}return x;}

    private static JSONObject adc(JSONObject diag,JSONObject data){
        JSONObject x=new JSONObject();try{
            JSONObject a=first(data==null?null:data.optJSONObject("adc"),diag==null?null:diag.optJSONObject("adc"));boolean a1=a!=null?a.optBoolean("ads1",false):diag.optBoolean("ads1",false);boolean a2=a!=null?a.optBoolean("ads2",false):diag.optBoolean("ads2",false);x.put("ads1",a1);x.put("ads2",a2);x.put("ads1_address","0x48");x.put("ads2_address","0x49");x.put("any_ready",a1||a2);x.put("all_ready",a1&&a2);
            JSONObject provenance=a==null?null:a.optJSONObject("acquisition");if(provenance==null){JSONObject da=diag==null?null:diag.optJSONObject("adc");provenance=da==null?null:da.optJSONObject("acquisition");}
            if(provenance!=null)x.put("acquisition",new JSONObject(provenance.toString()));
        }catch(Exception ignored){}return x;
    }

    private static JSONObject link(JSONObject diag){JSONObject x=new JSONObject();try{JSONObject l=diag==null?null:diag.optJSONObject("link");x.put("armed",l!=null?l.optBoolean("armed",false):diag.optBoolean("linkArmed",false));x.put("lost",l!=null?l.optBoolean("lost",false):diag.optBoolean("linkLost",false));x.put("heartbeat_age_ms",l!=null?l.optLong("heartbeat_age_ms",0):diag.optLong("heartbeatAgeMs",0));}catch(Exception ignored){}return x;}

    private static JSONObject measurement(JSONObject m){
        JSONObject x=new JSONObject();try{
            if(m==null)m=new JSONObject();int code;String state;
            Object raw=m.opt("state");
            if(raw instanceof Number){code=((Number)raw).intValue();state=code==1?"warmup":code==2?"measuring":code==3?"ready":code==4?"error":"idle";}
            else{state=m.optString("state","idle").toLowerCase(Locale.US);code="warmup".equals(state)?1:"measuring".equals(state)?2:"ready".equals(state)?3:"error".equals(state)?4:0;}
            int duration=Math.max(0,m.optInt("duration_s",0)),elapsed=Math.max(0,m.optInt("elapsed_s",0)),remaining=m.has("remaining_s")?Math.max(0,m.optInt("remaining_s",0)):Math.max(0,duration-elapsed);
            x.put("state",state);x.put("state_code",code);x.put("mask",m.optInt("mask",0)!=0?1:0);x.put("power",m.optBoolean("power",m.optInt("mask",0)!=0));x.put("manual_power",m.optBoolean("manual_power",true));x.put("manual_warmup",m.optBoolean("manual_warmup",true));x.put("duration_s",duration);x.put("elapsed_s",elapsed);x.put("remaining_s",remaining);x.put("error",m.optString("error",""));x.put("valid_channels",Math.max(0,m.optInt("valid_channels",0)));x.put("mq",mq(m.optJSONObject("mq"),true));
        }catch(Exception ignored){}return x;
    }

    private static JSONObject mq(JSONObject source,boolean averaged){
        JSONObject out=new JSONObject();if(source==null)return out;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject r=source.optJSONObject(hw.key);if(r==null)continue;JSONObject x=new JSONObject();try{
                boolean ok=r.optBoolean("ok",r.optBoolean("valid",false));x.put("ok",ok);x.put("valid",ok);x.put("status",ok?(averaged?"AVERAGED":"OK"):"INACTIVE");
                copy(r,x,"raw");copy(r,x,"adc");copy(r,x,"ao");copy(r,x,"rs");copy(r,x,"ratio");copy(r,x,"r0");
                if(!averaged){copyLong(r,x,"sample_us");copyLong(r,x,"scan_cycle");copyLong(r,x,"channel_offset_us");copyLong(r,x,"age_us");}
                x.put("relay",1);x.put("row",1);x.put("powered",r.optBoolean("powered",false));x.put("ads",r.has("ads")?r.optInt("ads",hw.adsIndex):hw.adsIndex);x.put("ads_address",String.format(Locale.US,"0x%02X",hw.adsAddress));x.put("ch",r.has("ch")?r.optInt("ch",hw.adcChannel):hw.adcChannel);
                boolean firmwareCalibrated=r.optBoolean("r0_calibrated",false);String firmwareSource=r.optString("r0_source",firmwareCalibrated?"firmware_calibrated":"firmware_placeholder_or_unknown");
                x.put("firmware_r0_calibrated",firmwareCalibrated);x.put("manual_calibration_confirmed",false);x.put("calibration_status",firmwareCalibrated?"CALIBRATED_FIRMWARE":"UNCALIBRATED");
                ManualR0Calibration.apply(hw.key,x);
                boolean manual="manual_device_r0".equals(x.optString("r0_source",""))&&x.optBoolean("r0_calibrated",false);
                if(manual){x.put("manual_calibration_confirmed",true);x.put("calibration_status","CALIBRATED_MANUAL");}
                else if(firmwareCalibrated){x.put("r0_calibrated",true);x.put("r0_source",firmwareSource);x.put("manual_calibration_confirmed",false);x.put("calibration_status","CALIBRATED_FIRMWARE");}
                else{x.put("r0_calibrated",false);x.put("manual_calibration_confirmed",false);x.put("calibration_status","UNCALIBRATED");}
                x.put("r0_loaded",x.optBoolean("r0_calibrated",false));out.put(hw.key,x);
            }catch(Exception ignored){}
        }
        return out;
    }

    private static Object finiteAny(JSONObject o,String a,String b){if(o==null)return JSONObject.NULL;if(o.has(a)&&!o.isNull(a))return finite(o,a);return finite(o,b);}
    private static Object finite(JSONObject o,String key){if(o==null||o.isNull(key))return JSONObject.NULL;double v=o.optDouble(key,Double.NaN);return Double.isFinite(v)?v:JSONObject.NULL;}
    private static void copy(JSONObject from,JSONObject to,String key)throws Exception{if(from.has(key)&&!from.isNull(key)){double v=from.optDouble(key,Double.NaN);if(Double.isFinite(v))to.put(key,v);}}
    private static void copyLong(JSONObject from,JSONObject to,String key)throws Exception{if(from.has(key)&&!from.isNull(key))to.put(key,from.optLong(key));}
}
