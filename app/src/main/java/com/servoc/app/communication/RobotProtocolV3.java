package com.servoc.app.communication;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import java.util.Locale;

/** Normalizes compact ESP32 API v3 payloads into the HazRob 0.8 hardware contract. */
public final class RobotProtocolV3 {
    private RobotProtocolV3(){}

    public static JSONObject normalize(JSONObject diag, JSONObject data, int commandedLeft, int commandedRight){
        JSONObject out=new JSONObject();
        try{
            if(diag==null)diag=new JSONObject();
            if(data==null)data=new JSONObject();

            out.put("firmware",diag.optString("fw",diag.optString("firmware","HazRob")));
            out.put("api_version",diag.has("api")?diag.optInt("api",3):diag.optInt("api_version",3));
            out.put("heap",diag.optLong("heap",0));
            out.put("clients",diag.optInt("clients",0));
            out.put("hardware_profile",HazRobHardwareProfile.ID);
            out.put("mq_groups",HazRobHardwareProfile.MQ_GROUPS);
            out.put("mq_rows",HazRobHardwareProfile.MQ_GROUPS);
            out.put("installed_relay_mask",HazRobHardwareProfile.INSTALLED_RELAY_MASK);
            out.put("relay_channels",HazRobHardwareProfile.RELAY_CHANNELS);

            boolean retrace=diag.optBoolean("retrace",data.optBoolean("retrace",false));
            JSONObject drive=new JSONObject();
            drive.put("left",commandedLeft);drive.put("right",commandedRight);
            drive.put("failsafe",diag.optBoolean("failsafe",false));drive.put("retrace",retrace);
            out.put("drive",drive);out.put("retrace",retrace);

            out.put("env",environment(diag,data.optJSONObject("env")));
            out.put("relay",relay(diag,data));out.put("sound",sound(data.optJSONObject("sound")));
            out.put("ir",ir(diag,data.optJSONObject("ir")));out.put("ldr",ldr(diag,data.optJSONObject("ldr")));
            out.put("adc",adc(diag));out.put("mq",mq(data.optJSONObject("mq"),false));out.put("measurement",measurement(data.optJSONObject("measurement")));
            out.put("_diag_v3",diag);out.put("_data_v3",data);
        }catch(Exception ignored){}
        return out;
    }

    private static JSONObject environment(JSONObject diag,JSONObject e){
        JSONObject x=new JSONObject();try{if(e==null)e=new JSONObject();boolean ok=e.optBoolean("ok",false);int type=diag.optInt("envType",0),addr=diag.optInt("envAddr",0),chip=diag.optInt("envId",0);String name=type==1?"BME280":type==2?"BMP280":"NONE";x.put("ok",ok);x.put("i2c_found",type!=0||addr!=0||chip!=0);x.put("type",name);x.put("sensor",name);x.put("address",addr>0?String.format(Locale.US,"0x%02X",addr):JSONObject.NULL);x.put("chip_id",chip>0?String.format(Locale.US,"0x%02X",chip):JSONObject.NULL);x.put("temperature",finite(e,"t"));x.put("humidity",finite(e,"h"));x.put("pressure",finite(e,"p"));x.put("humidity_available",type!=2&&!e.isNull("h"));}catch(Exception ignored){}return x;
    }

    private static JSONObject relay(JSONObject diag,JSONObject data){
        JSONObject x=new JSONObject();try{int raw;if(data!=null&&data.has("relayMask"))raw=data.optInt("relayMask",0);else if(diag!=null&&diag.has("relayMask"))raw=diag.optInt("relayMask",0);else{int g=data==null?0:data.optInt("relay",0);raw=g>=1&&g<=4?1<<(g-1):0;}int mask=raw&HazRobHardwareProfile.INSTALLED_RELAY_MASK;int group=Integer.bitCount(mask)==1?Integer.numberOfTrailingZeros(mask)+1:0;x.put("mask",mask);x.put("raw_mask",raw&15);x.put("group",group);x.put("active_groups",Integer.bitCount(mask));x.put("active_rows",Integer.bitCount(mask));x.put("manual_control",true);x.put("group1",(mask&1)!=0);x.put("group2",(mask&2)!=0);x.put("group3",false);x.put("group4",false);x.put("reserved_mask",raw&0x0C);}catch(Exception ignored){}return x;
    }

    private static JSONObject sound(JSONObject s){JSONObject x=new JSONObject();try{if(s==null)s=new JSONObject();Object rms=finite(s,"rms"),p2p=finite(s,"p2p");x.put("rms_v",rms);x.put("p2p_v",p2p);x.put("ok",rms!=JSONObject.NULL||p2p!=JSONObject.NULL);x.put("calibrated_db_spl",false);}catch(Exception ignored){}return x;}
    private static JSONObject ir(JSONObject diag,JSONObject i){JSONObject x=new JSONObject();try{boolean warning=i!=null?i.optBoolean("warning",diag.optBoolean("ir",false)):diag.optBoolean("ir",false);x.put("installed",true);x.put("warning",warning);x.put("level",i==null?JSONObject.NULL:i.optInt("level",-1));x.put("drive_interlock",false);}catch(Exception ignored){}return x;}
    private static JSONObject ldr(JSONObject diag,JSONObject i){JSONObject x=new JSONObject();try{boolean present=i!=null&&(i.has("active")||i.has("level"));x.put("installed",present);x.put("active",present&&i.optBoolean("active",diag.optBoolean("ldr",false)));x.put("level",present?i.optInt("level",-1):JSONObject.NULL);x.put("quantitative_lux",false);}catch(Exception ignored){}return x;}
    private static JSONObject adc(JSONObject diag){JSONObject x=new JSONObject();try{x.put("ads1",diag.optBoolean("ads1",false));x.put("ads2",diag.optBoolean("ads2",false));x.put("ads1_address","0x48");x.put("ads2_address","0x49");}catch(Exception ignored){}return x;}

    private static JSONObject measurement(JSONObject m){
        JSONObject x=new JSONObject();try{if(m==null)m=new JSONObject();Object rawState=m.opt("state");int code=0;String state;if(rawState instanceof Number){code=((Number)rawState).intValue();state=code==1?"warmup":code==2?"measuring":code==3?"ready":code==4?"error":"idle";}else{state=m.optString("state","idle").toLowerCase(Locale.US);code="warmup".equals(state)?1:"measuring".equals(state)?2:"ready".equals(state)?3:"error".equals(state)?4:0;}x.put("state",state);x.put("state_code",code);x.put("group",m.optInt("group",0));x.put("phase",m.optInt("phase",0));x.put("mask",m.optInt("mask",0)&HazRobHardwareProfile.INSTALLED_RELAY_MASK);x.put("manual_power",true);x.put("mq",mq(m.optJSONObject("mq"),true));}catch(Exception ignored){}return x;
    }

    private static JSONObject mq(JSONObject source,boolean averaged){
        JSONObject out=new JSONObject();if(source==null)return out;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){JSONObject r=source.optJSONObject(hw.key);if(r==null)continue;JSONObject x=new JSONObject();try{boolean ok=r.optBoolean("ok",r.optBoolean("valid",false));x.put("ok",ok);x.put("valid",ok);x.put("status",ok?(averaged?"AVERAGED":"OK"):"INACTIVE");copy(r,x,"raw");copy(r,x,"adc");copy(r,x,"ao");copy(r,x,"rs");copy(r,x,"ratio");x.put("row",r.has("row")?r.optInt("row",hw.relay):hw.relay);x.put("relay",hw.relay);x.put("ads",r.has("ads")?r.optInt("ads",hw.adsIndex):hw.adsIndex);x.put("ads_address",String.format(Locale.US,"0x%02X",hw.adsAddress));x.put("ch",r.has("ch")?r.optInt("ch",hw.adcChannel):hw.adcChannel);x.put("r0_calibrated",r.optBoolean("r0_calibrated",false));ManualR0Calibration.apply(hw.key,x);out.put(hw.key,x);}catch(Exception ignored){}}
        return out;
    }

    private static Object finite(JSONObject o,String key){if(o==null||o.isNull(key))return JSONObject.NULL;double v=o.optDouble(key,Double.NaN);return Double.isFinite(v)?v:JSONObject.NULL;}
    private static void copy(JSONObject from,JSONObject to,String key)throws Exception{if(from.has(key)&&!from.isNull(key)){double v=from.optDouble(key,Double.NaN);if(Double.isFinite(v))to.put(key,v);}}
}
