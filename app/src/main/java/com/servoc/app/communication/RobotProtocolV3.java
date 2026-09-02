package com.servoc.app.communication;

import org.json.JSONObject;
import java.util.Locale;

/** Normalizes the compact HazRob ESP32 API v3 payload and additive 0.7.1 hardware fields. */
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
            out.put("mq_rows",diag.optInt("mqRows",4));
            out.put("max_active_rows",diag.optInt("maxActiveRows",2));

            boolean retrace=diag.optBoolean("retrace",data.optBoolean("retrace",false));
            JSONObject drive=new JSONObject();
            drive.put("left",commandedLeft);
            drive.put("right",commandedRight);
            drive.put("failsafe",diag.optBoolean("failsafe",false));
            drive.put("retrace",retrace);
            out.put("drive",drive);
            out.put("retrace",retrace);

            out.put("env",environment(diag,data.optJSONObject("env")));
            out.put("relay",relay(diag,data));
            out.put("sound",sound(data.optJSONObject("sound")));
            out.put("ir",ir(diag,data.optJSONObject("ir")));
            out.put("ldr",ldr(diag,data.optJSONObject("ldr")));
            out.put("adc",adc(diag));
            out.put("mq",mq(data.optJSONObject("mq"),false));
            out.put("measurement",measurement(data.optJSONObject("measurement")));

            out.put("_diag_v3",diag);
            out.put("_data_v3",data);
        }catch(Exception ignored){}
        return out;
    }

    private static JSONObject environment(JSONObject diag,JSONObject e){
        JSONObject x=new JSONObject();
        try{
            if(e==null)e=new JSONObject();
            boolean ok=e.optBoolean("ok",false);
            int type=diag.optInt("envType",0),addr=diag.optInt("envAddr",0),chip=diag.optInt("envId",0);
            String name=type==1?"BME280":type==2?"BMP280":"NONE";
            x.put("ok",ok);
            x.put("i2c_found",type!=0||addr!=0||chip!=0);
            x.put("type",name);
            x.put("sensor",name);
            x.put("address",addr>0?String.format(Locale.US,"0x%02X",addr):JSONObject.NULL);
            x.put("chip_id",chip>0?String.format(Locale.US,"0x%02X",chip):JSONObject.NULL);
            x.put("temperature",finite(e,"t"));
            x.put("humidity",finite(e,"h"));
            x.put("pressure",finite(e,"p"));
            x.put("humidity_available",type!=2&&!e.isNull("h"));
        }catch(Exception ignored){}
        return x;
    }

    private static JSONObject relay(JSONObject diag,JSONObject data){
        JSONObject x=new JSONObject();
        try{
            int mask;
            if(data!=null&&data.has("relayMask"))mask=data.optInt("relayMask",0)&15;
            else if(diag!=null&&diag.has("relayMask"))mask=diag.optInt("relayMask",0)&15;
            else{
                int g=data==null?0:data.optInt("relay",0);
                mask=g>=1&&g<=4?1<<(g-1):0;
            }
            int group=Integer.bitCount(mask)==1?Integer.numberOfTrailingZeros(mask)+1:0;
            x.put("mask",mask);
            x.put("group",group);
            x.put("active_rows",Integer.bitCount(mask));
            x.put("max_active_rows",diag==null?2:diag.optInt("maxActiveRows",2));
            x.put("group1",(mask&1)!=0);
            x.put("group2",(mask&2)!=0);
            x.put("group3",(mask&4)!=0);
            x.put("group4",(mask&8)!=0);
        }catch(Exception ignored){}
        return x;
    }

    private static JSONObject sound(JSONObject s){
        JSONObject x=new JSONObject();
        try{
            if(s==null)s=new JSONObject();
            Object rms=finite(s,"rms"),p2p=finite(s,"p2p");
            x.put("rms_v",rms);x.put("p2p_v",p2p);
            x.put("ok",rms!=JSONObject.NULL||p2p!=JSONObject.NULL);
            x.put("calibrated_db_spl",false);
        }catch(Exception ignored){}
        return x;
    }

    private static JSONObject ir(JSONObject diag,JSONObject i){
        JSONObject x=new JSONObject();
        try{
            boolean warning=i!=null?i.optBoolean("warning",diag.optBoolean("ir",false)):diag.optBoolean("ir",false);
            x.put("warning",warning);
            x.put("level",i==null?JSONObject.NULL:i.optInt("level",-1));
            x.put("drive_interlock",false);
        }catch(Exception ignored){}
        return x;
    }

    private static JSONObject ldr(JSONObject diag,JSONObject i){
        JSONObject x=new JSONObject();
        try{
            boolean active=i!=null?i.optBoolean("active",diag.optBoolean("ldr",false)):diag.optBoolean("ldr",false);
            x.put("active",active);
            x.put("level",i==null?JSONObject.NULL:i.optInt("level",-1));
            x.put("quantitative_lux",false);
        }catch(Exception ignored){}
        return x;
    }

    private static JSONObject adc(JSONObject diag){
        JSONObject x=new JSONObject();
        try{x.put("ads1",diag.optBoolean("ads1",false));x.put("ads2",diag.optBoolean("ads2",false));}catch(Exception ignored){}
        return x;
    }

    private static JSONObject measurement(JSONObject m){
        JSONObject x=new JSONObject();
        try{
            if(m==null)m=new JSONObject();
            int code=m.optInt("state",0);
            String state=code==1?"warmup":code==2?"measuring":code==3?"ready":code==4?"error":"idle";
            x.put("state",state);
            x.put("state_code",code);
            x.put("group",m.optInt("group",0));
            x.put("phase",m.optInt("phase",0));
            x.put("mask",m.optInt("mask",0)&15);
            x.put("mq",mq(m.optJSONObject("mq"),true));
        }catch(Exception ignored){}
        return x;
    }

    private static JSONObject mq(JSONObject source,boolean averaged){
        JSONObject out=new JSONObject();
        if(source==null)return out;
        String[] keys={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
        for(String key:keys){
            JSONObject r=source.optJSONObject(key);if(r==null)continue;
            JSONObject x=new JSONObject();
            try{
                boolean ok=r.optBoolean("ok",r.optBoolean("valid",false));
                x.put("ok",ok);x.put("valid",ok);x.put("status",ok?(averaged?"AVERAGED":"OK"):"INACTIVE");
                copy(r,x,"raw");copy(r,x,"adc");copy(r,x,"ao");copy(r,x,"rs");copy(r,x,"ratio");
                if(r.has("row"))x.put("row",r.optInt("row",0));
                if(r.has("ads"))x.put("ads",r.optInt("ads",0));
                if(r.has("ch"))x.put("ch",r.optInt("ch",0));
                x.put("r0_calibrated",r.optBoolean("r0_calibrated",false));
                out.put(key,x);
            }catch(Exception ignored){}
        }
        return out;
    }

    private static Object finite(JSONObject o,String key){
        if(o==null||o.isNull(key))return JSONObject.NULL;
        double v=o.optDouble(key,Double.NaN);
        return Double.isFinite(v)?v:JSONObject.NULL;
    }

    private static void copy(JSONObject from,JSONObject to,String key)throws Exception{
        if(from.has(key)&&!from.isNull(key)){
            double v=from.optDouble(key,Double.NaN);
            if(Double.isFinite(v))to.put(key,v);
        }
    }
}
