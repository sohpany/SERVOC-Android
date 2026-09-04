package com.servoc.app.communication;

import org.json.JSONObject;
import java.util.Iterator;

/** Pure-Java builder for the MQTT command contract shared with the allocation-free ESP32 parser. */
final class RemoteCommandEnvelope {
    private RemoteCommandEnvelope(){}

    static JSONObject build(String session,long seq,String nonce,String type,int ttlMs,long sentMs,JSONObject payload)throws Exception{
        return build(session,seq,nonce,type,ttlMs,sentMs,payload,"developer","");
    }

    static JSONObject build(String session,long seq,String nonce,String type,int ttlMs,long sentMs,JSONObject payload,String role,String access)throws Exception{
        JSONObject body=payload==null?new JSONObject():payload;
        JSONObject cmd=new JSONObject();
        cmd.put("session",session);cmd.put("seq",seq);cmd.put("nonce",nonce);cmd.put("type",type);
        cmd.put("ttl_ms",Math.max(100,Math.min(10000,ttlMs)));cmd.put("sent_ms",sentMs);cmd.put("payload",body);
        String r="guest".equalsIgnoreCase(role)?"guest":"developer";cmd.put("role",r);if("guest".equals(r))cmd.put("access",access==null?"":access);
        Iterator<String> keys=body.keys();
        while(keys.hasNext()){String k=keys.next();if(!reserved(k))cmd.put(k,body.opt(k));}
        return cmd;
    }

    private static boolean reserved(String k){
        return "session".equals(k)||"seq".equals(k)||"nonce".equals(k)||"type".equals(k)||"ttl_ms".equals(k)||"sent_ms".equals(k)||"payload".equals(k)||"role".equals(k)||"access".equals(k);
    }
}
