package com.servoc.app.communication;

/** Immutable stage-based remote readiness result used by the entry/provisioning UI. */
public final class RemoteReadiness {
    public final boolean configured,wifi,ip,dns,time,internet,tls,mqtt,commandAck,telemetry,camera,ready;
    public final String detail;
    public RemoteReadiness(boolean configured,boolean wifi,boolean ip,boolean dns,boolean time,boolean internet,boolean tls,boolean mqtt,boolean commandAck,boolean telemetry,boolean camera,boolean ready,String detail){
        this.configured=configured;this.wifi=wifi;this.ip=ip;this.dns=dns;this.time=time;this.internet=internet;this.tls=tls;this.mqtt=mqtt;this.commandAck=commandAck;this.telemetry=telemetry;this.camera=camera;this.ready=ready;this.detail=detail==null?"":detail;
    }
    /** Core admission stages intentionally exclude camera/video, which is an independent path. */
    public int completedCoreStages(){int n=0;if(configured)n++;if(wifi)n++;if(ip)n++;if(dns)n++;if(time)n++;if(internet)n++;if(tls)n++;if(mqtt)n++;if(commandAck)n++;if(telemetry)n++;return n;}
    public static RemoteReadiness fromRobot(org.json.JSONObject j){
        if(j==null)return new RemoteReadiness(false,false,false,false,false,false,false,false,false,false,false,false,"no status");
        boolean configured=j.optBoolean("configured",false),wifi=j.optBoolean("wifi",false);String ip=j.optString("ip","0.0.0.0");boolean hasIp=wifi&&!ip.isEmpty()&&!"0.0.0.0".equals(ip);boolean dns=j.optBoolean("dns",false),time=j.optBoolean("time",false),internet=j.optBoolean("internet",false),tls=j.optBoolean("tls",false),mqtt=j.optBoolean("mqtt",false),ack=j.optBoolean("command_ack",false),telemetry=j.optBoolean("telemetry",false),camera=j.optBoolean("camera",false),ready=j.optBoolean("remote_ready",false)&&ack&&telemetry;
        return new RemoteReadiness(configured,wifi,hasIp,dns,time,internet,tls,mqtt,ack,telemetry,camera,ready,j.optString("detail",""));
    }
}
