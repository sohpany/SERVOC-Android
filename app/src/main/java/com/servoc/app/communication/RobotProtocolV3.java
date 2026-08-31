package com.servoc.app.communication;

import org.json.JSONObject;
import java.util.Locale;

/** Normalizes the compact HazRob ESP32 API v3 payload into the application telemetry model. */
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

            JSONObject drive=new JSONObject();
            drive.put("left",commandedLeft);
            drive.put("right",commandedRight);
            drive.put("failsafe",diag.optBoolean("failsafe",false));
            out.put("drive",drive);

            out.put("env",environment(diag,data.optJSONObject("env")));
            out.put("relay",relay(data));
            out.put("mq",mq(data.optJSONObject("mq"),false));
            out.put("measurement",measurement(data.optJSONObject("measurement")));

            // Retain compact raw payloads for diagnostics/export without coupling UI code to them.
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

    private static JSONObject relay(JSONObject data){
        JSONObject x=new JSONObject();
        try{
            int g=data==null?0:data.optInt("relay",0);
            x.put("group",g);
            x.put("group1",g==1);
            x.put("group2",g==2);
            x.put("group3",g==3);
        }catch(Exception ignored){}
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
                x.put("ok",ok);
                x.put("valid",ok);
                x.put("status",ok?(averaged?"AVERAGED":"OK"):"INACTIVE");
                copy(r,x,"raw");copy(r,x,"adc");copy(r,x,"ao");copy(r,x,"rs");copy(r,x,"ratio");
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
