package com.servoc.app.communication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** One active bidirectional remote HazRob session. Commands and telemetry share the same RobotApi contract as Direct mode. */
public final class RemoteConnection implements SecureMqtt311Client.Listener,AutoCloseable {
    private static volatile RemoteConnection active;private static volatile boolean transportOwned=false;
    private final SecureMqtt311Client mqtt;private final String robotId,root,sessionId;private final AtomicLong sequence=new AtomicLong();private final SecureRandom random=new SecureRandom();
    private final ConcurrentHashMap<String,Pending> pending=new ConcurrentHashMap<>();
    private final ScheduledExecutorService drivePump=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"HazRob-RemoteDrive");t.setDaemon(true);return t;});
    private final AtomicLong driveRevision=new AtomicLong();
    private volatile JSONObject latestTelemetry,latestStatus;private volatile long telemetryAt;private volatile boolean disconnected;
    private volatile int desiredDriveLeft=0,desiredDriveRight=0;private volatile long publishedDriveRevision=-1,lastDrivePublishAt=0,lastAckRttMs=-1;
    private static final class Pending{final CountDownLatch latch=new CountDownLatch(1);volatile JSONObject ack;}

    private RemoteConnection(SecureMqtt311Client mqtt,String robotId){
        this.mqtt=mqtt;this.robotId=robotId;this.root="hazrob/"+robotId+"/";this.sessionId=UUID.randomUUID().toString();
        drivePump.scheduleAtFixedRate(this::pumpDrive,0,RemoteDrivePolicy.PUMP_MS,TimeUnit.MILLISECONDS);
    }

    public static synchronized RemoteConnection connect(Context context,SharedPreferences prefs)throws Exception{
        transportOwned=true;RemoteConnection previous=active;
        String host=prefs.getString("remote_broker_host","").trim(),robot=prefs.getString("remote_robot_id","").trim();int port=prefs.getInt("remote_broker_port",8883);if(host.isEmpty()||robot.isEmpty())throw new IllegalStateException("remote service profile incomplete");
        RemoteCredentialStore.Credentials c=new RemoteCredentialStore(context,prefs).load();if(!c.present())throw new IllegalStateException("remote MQTT credentials unavailable");
        String client="hazrob-app-"+robot+"-"+Long.toHexString(System.currentTimeMillis());SecureMqtt311Client m=new SecureMqtt311Client(host,port,client,c.username,c.password);RemoteConnection r=new RemoteConnection(m,robot);
        try{m.connect(r);m.subscribe(r.root+"ack");m.subscribe(r.root+"telemetry");m.subscribe(r.root+"status");active=r;if(previous!=null&&previous!=r)previous.close();return r;}
        catch(Exception e){try{r.close();}catch(Exception ignored){}throw e;}
    }

    public static RemoteConnection active(){RemoteConnection r=active;return r!=null&&r.isConnected()?r:null;}
    public static RemoteConnection current(){return active;}
    public static boolean transportOwned(){return transportOwned;}
    public static synchronized void releaseTransport(){transportOwned=false;RemoteConnection r=active;active=null;if(r!=null)r.close();}
    public boolean isConnected(){return mqtt.isConnected()&&!disconnected;}
    public String robotId(){return robotId;}public String sessionId(){return sessionId;}public String operatorRole(){return "operator";}public long lastAckRttMs(){return lastAckRttMs;}

    public RemoteReadiness probe(long timeoutMs){
        boolean tls=isConnected(),mqttOk=tls,ack=false,telemetry=false;JSONObject status=latestStatus;
        try{JSONObject a=command("probe",new JSONObject(),2500,true,Math.max(1000,timeoutMs));ack=a!=null&&a.optBoolean("ok",false);}catch(Exception ignored){}
        long until=SystemClock.elapsedRealtime()+Math.max(500,timeoutMs);while(SystemClock.elapsedRealtime()<until){telemetry=latestTelemetry!=null&&SystemClock.elapsedRealtime()-telemetryAt<5000;status=latestStatus;if(ack&&telemetry)break;try{Thread.sleep(50);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}
        boolean configured=true,wifi=status!=null&&status.optBoolean("wifi",ack),ip=status!=null&&status.optBoolean("ip_ok",wifi),dns=status!=null&&status.optBoolean("dns",ack),time=status!=null&&status.optBoolean("time",ack),internet=status!=null&&status.optBoolean("internet",ack);boolean ready=tls&&mqttOk&&ack&&telemetry;long age=telemetryAt==0?-1:SystemClock.elapsedRealtime()-telemetryAt;
        String detail=ready?"remote ready • ack "+lastAckRttMs+" ms • telemetry age "+age+" ms":"remote handshake incomplete";
        return new RemoteReadiness(configured,wifi,ip,dns,time,internet,tls,mqttOk,ack,telemetry,false,ready,detail);
    }

    public JSONObject command(String type,JSONObject payload,int ttlMs,boolean waitAck,long timeoutMs)throws Exception{
        if(!isConnected())throw new IllegalStateException("remote MQTT disconnected");
        long seq=sequence.incrementAndGet();String nonce=Long.toHexString(random.nextLong())+Long.toHexString(System.nanoTime());JSONObject cmd=RemoteCommandEnvelope.build(sessionId,seq,nonce,type,ttlMs,System.currentTimeMillis(),payload);
        Pending p=null;long started=SystemClock.elapsedRealtime();if(waitAck){p=new Pending();pending.put(nonce,p);}try{mqtt.publish(root+"cmd",cmd.toString(),false);if(!waitAck)return null;if(!p.latch.await(Math.max(250,timeoutMs),TimeUnit.MILLISECONDS))throw new IOException("remote ACK timeout");JSONObject a=p.ack;if(a==null)throw new IOException("remote disconnected before ACK");if(!a.optBoolean("ok",false))throw new IOException("remote command rejected: "+a.optString("error","unknown"));lastAckRttMs=Math.max(0,SystemClock.elapsedRealtime()-started);return a;}finally{if(waitAck)pending.remove(nonce);}
    }

    private void setDesiredDrive(int left,int right){desiredDriveLeft=Math.max(-100,Math.min(100,left));desiredDriveRight=Math.max(-100,Math.min(100,right));driveRevision.incrementAndGet();}
    private void pumpDrive(){
        if(!isConnected())return;long now=SystemClock.elapsedRealtime(),revision=driveRevision.get();int left=desiredDriveLeft,right=desiredDriveRight;boolean changed=revision!=publishedDriveRevision;
        if(!RemoteDrivePolicy.due(now,lastDrivePublishAt,changed,left,right))return;
        try{JSONObject p=new JSONObject();p.put("left",left);p.put("right",right);command("drive",p,RemoteDrivePolicy.COMMAND_TTL_MS,false,0);publishedDriveRevision=revision;lastDrivePublishAt=SystemClock.elapsedRealtime();}catch(Exception ignored){}
    }

    /** Updates the desired remote command immediately; the dedicated pump publishes only the newest value, never a backlog. */
    public void drive(int left,int right)throws Exception{if(!isConnected())throw new IllegalStateException("remote MQTT disconnected");setDesiredDrive(left,right);}
    public void motionStop()throws Exception{setDesiredDrive(0,0);command("motion_stop",new JSONObject(),1500,true,1500);}
    public void emergencyStop()throws Exception{setDesiredDrive(0,0);command("estop",new JSONObject(),3000,true,2500);}
    public void emergencyReset()throws Exception{command("estop_reset",new JSONObject(),3000,true,2500);}
    public void sensorOff()throws Exception{command("sensor_off",new JSONObject(),3000,true,2500);}
    public void heartbeat()throws Exception{command("heartbeat",new JSONObject(),2500,false,0);}
    public void relayMask(int mask)throws Exception{if(mask!=0&&mask!=1)throw new IllegalArgumentException("MQ power state must be 0 or 1");JSONObject p=new JSONObject();p.put("mask",mask);command("relay",p,3000,true,2500);}
    public void startMeasurement()throws Exception{command("measure_start",new JSONObject(),4000,true,3000);}
    public void cancelMeasurement()throws Exception{command("measure_cancel",new JSONObject(),3000,true,2500);}
    /** Alert refreshes are intentionally fire-and-forget; firmware TTL clears a stale gas alert. */
    public void setAlertLevel(int severity,int ttlMs)throws Exception{if(severity<0||severity>2)throw new IllegalArgumentException("Alert severity must be 0..2");JSONObject p=new JSONObject();p.put("severity",severity);p.put("ttl",Math.max(500,Math.min(10000,ttlMs)));command("alert",p,3500,false,0);}
    public void alertTest()throws Exception{command("alert_test",new JSONObject(),2000,true,1500);}
    public void cameraFlash(String mode)throws Exception{JSONObject p=new JSONObject();p.put("mode",mode==null?"off":mode);command("camera_flash",p,3000,true,2500);}

    public JSONObject snapshot(int left,int right){JSONObject t=latestTelemetry;if(t==null)return null;JSONObject s=t.optJSONObject("snapshot");if(s!=null)return s;JSONObject diag=t.optJSONObject("diag"),data=t.optJSONObject("data");if(diag!=null||data!=null)return RobotProtocolV4.normalize(diag,data,left,right);return t;}
    public JSONObject status(){return latestStatus;}

    @Override public void onMessage(String topic,String payload){try{JSONObject j=new JSONObject(payload);if(topic.equals(root+"ack")){String nonce=j.optString("nonce","");Pending p=pending.get(nonce);if(p!=null){p.ack=j;p.latch.countDown();}}else if(topic.equals(root+"telemetry")){latestTelemetry=j;telemetryAt=SystemClock.elapsedRealtime();}else if(topic.equals(root+"status")){latestStatus=j;}}catch(Exception ignored){}}
    @Override public void onDisconnected(Exception error){disconnected=true;for(Pending p:pending.values())p.latch.countDown();}

    @Override public synchronized void close(){if(active==this)active=null;disconnected=true;drivePump.shutdownNow();mqtt.close();for(Pending p:pending.values())p.latch.countDown();pending.clear();}
}
