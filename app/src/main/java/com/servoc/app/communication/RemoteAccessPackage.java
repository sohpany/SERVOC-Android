package com.servoc.app.communication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.servoc.app.camera.RemoteVideoCredentialStore;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/** Portable HazRob viewer/control access package. Possession of the code grants remote access. */
public final class RemoteAccessPackage {
    private static final String PREFIX="HZR1-";

    private RemoteAccessPackage(){}

    public static boolean present(Context context,SharedPreferences prefs){
        RemoteServiceProfile.ensureDefaults(prefs);
        RemoteCredentialStore.Credentials mqtt=new RemoteCredentialStore(context,prefs).load();
        String video=new RemoteVideoCredentialStore(prefs).load();
        return mqtt.present()&&!video.trim().isEmpty();
    }

    public static String exportCode(Context context,SharedPreferences prefs)throws Exception{
        RemoteServiceProfile.ensureDefaults(prefs);
        RemoteCredentialStore.Credentials mqtt=new RemoteCredentialStore(context,prefs).load();
        String video=new RemoteVideoCredentialStore(prefs).load();
        if(!mqtt.present()||video.trim().isEmpty())throw new IllegalStateException("remote credentials incomplete");
        JSONObject j=new JSONObject();
        j.put("v",1);
        j.put("robot",RemoteServiceProfile.ROBOT_ID);
        j.put("user",mqtt.username);
        j.put("pass",mqtt.password);
        j.put("video",video);
        String payload=Base64.encodeToString(j.toString().getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
        return PREFIX+payload;
    }

    public static void importCode(Context context,SharedPreferences prefs,String code)throws Exception{
        if(code==null)throw new IllegalArgumentException("empty access code");
        String s=code.replace("\n","").replace("\r","").replace(" ","").trim();
        if(!s.startsWith(PREFIX)||s.length()<=PREFIX.length())throw new IllegalArgumentException("invalid access code");
        byte[] raw=Base64.decode(s.substring(PREFIX.length()),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
        JSONObject j=new JSONObject(new String(raw,StandardCharsets.UTF_8));
        if(j.optInt("v",0)!=1)throw new IllegalArgumentException("unsupported access code");
        if(!RemoteServiceProfile.ROBOT_ID.equals(j.optString("robot","")))throw new IllegalArgumentException("wrong robot");
        String user=j.optString("user","").trim(),pass=j.optString("pass",""),video=j.optString("video","");
        if(user.isEmpty()||pass.length()<8||video.length()<12)throw new IllegalArgumentException("incomplete access code");
        RemoteServiceProfile.ensureDefaults(prefs);
        new RemoteCredentialStore(context,prefs).save(user,pass);
        new RemoteVideoCredentialStore(prefs).save(video);
        prefs.edit()
                .putString("remote_broker_host",RemoteServiceProfile.MQTT_HOST)
                .putInt("remote_broker_port",RemoteServiceProfile.MQTT_PORT)
                .putString("remote_robot_id",RemoteServiceProfile.ROBOT_ID)
                .putString("remote_video_url",RemoteServiceProfile.VIDEO_VIEW_WSS)
                .apply();
    }
}
