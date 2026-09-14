package com.servoc.app.communication;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** HazRob API facade: low-latency UDP drive locally, HTTP for local control/telemetry, MQTT remotely. */
public final class RobotApi {
    private volatile String robotHost;
    private volatile boolean directFastDrive=false;
    private DatagramSocket driveSocket;
    private InetAddress driveAddress;
    private int driveSequence=0;
    // Direct-mode ownership: keep refreshing non-zero operator commands, but do not let an idle
    // Android activity overwrite independent local diagnostic commands with 0/0 every 50 ms.
    private int lastDirectDriveLeft=Integer.MIN_VALUE,lastDirectDriveRight=Integer.MIN_VALUE;
    public RobotApi(String host){setRobotHost(host);}

    public synchronized void setRobotHost(String host){
        if(host==null||host.trim().isEmpty())host="192.168.4.1";
        host=host.trim().replace("http://","").replace("https://","");while(host.endsWith("/"))host=host.substring(0,host.length()-1);
        if(!host.equals(robotHost)){closeDriveSocket();directFastDrive=false;driveAddress=null;driveSequence=0;lastDirectDriveLeft=lastDirectDriveRight=Integer.MIN_VALUE;}
        robotHost=host;
    }
    public String getRobotHost(){return robotHost;}
    private String url(String path){return "http://"+robotHost+(path.startsWith("/")?path:"/"+path);}
    private static RemoteConnection remote(){RemoteConnection r=RemoteConnection.current();if(r==null&&RemoteConnection.transportOwned())throw new IllegalStateException("remote transport reconnecting");return r;}

    public String getText(String path,int timeoutMs)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("GET");return finish(c);}
    public String postForm(String path,String body,int timeoutMs)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url(path)).openConnection();c.setUseCaches(false);c.setConnectTimeout(timeoutMs);c.setReadTimeout(timeoutMs);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.setRequestProperty("Cache-Control","no-store");byte[] b=(body==null?"":body).getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream out=c.getOutputStream()){out.write(b);out.flush();}return finish(c);}
    private static String finish(HttpURLConnection c)throws Exception{int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();StringBuilder s=new StringBuilder();if(in!=null){try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}}c.disconnect();if(code<200||code>=300)throw new IllegalStateException("HTTP "+code+" "+s);return s.toString();}
    public JSONObject getJson(String path,int timeoutMs)throws Exception{return new JSONObject(getText(path,timeoutMs));}

    private void observeDirectCapabilities(JSONObject diag){
        JSONObject u=diag==null?null:diag.optJSONObject("drive_udp");
        directFastDrive=u!=null&&u.optBoolean("installed",false)&&u.optInt("port",0)==DirectDriveDatagram.PORT&&u.optInt("version",0)==DirectDriveDatagram.VERSION;
        if(!directFastDrive)closeDriveSocket();
    }
    private void closeDriveSocket(){if(driveSocket!=null){try{driveSocket.close();}catch(Exception ignored){}driveSocket=null;}}
    private void sendDriveDatagram(int left,int right)throws Exception{
        if(driveSocket==null||driveSocket.isClosed())driveSocket=new DatagramSocket();
        if(driveAddress==null)driveAddress=InetAddress.getByName(robotHost);
        driveSequence=(driveSequence+1)&0xFFFF;
        byte[] b=DirectDriveDatagram.encode(driveSequence,left,right);
        driveSocket.send(new DatagramPacket(b,b.length,driveAddress,DirectDriveDatagram.PORT));
    }
    private void sendStopBurst(){for(int i=0;i<3;i++){try{sendDriveDatagram(0,0);}catch(Exception ignored){break;}}}

    public synchronized void drive(int left,int right)throws Exception{
        RemoteConnection r=remote();if(r!=null){r.drive(left,right);return;}
        if(left==0&&right==0&&lastDirectDriveLeft==0&&lastDirectDriveRight==0)return;
        if(directFastDrive){
            try{if(left==0&&right==0)sendStopBurst();else sendDriveDatagram(left,right);lastDirectDriveLeft=left;lastDirectDriveRight=right;return;}
            catch(Exception e){directFastDrive=false;closeDriveSocket();}
        }
        getText("/drive?l="+left+"&r="+right,450);lastDirectDriveLeft=left;lastDirectDriveRight=right;
    }
    public synchronized void motionStop()throws Exception{RemoteConnection r=remote();if(r!=null){r.motionStop();return;}if(directFastDrive)sendStopBurst();try{getText("/motion/stop",450);}catch(Exception e){getText("/stop",450);}lastDirectDriveLeft=lastDirectDriveRight=0;}
    public void stop()throws Exception{motionStop();}
    public synchronized void emergencyStop()throws Exception{RemoteConnection r=remote();if(r!=null){r.emergencyStop();return;}if(directFastDrive)sendStopBurst();getText("/estop",650);lastDirectDriveLeft=lastDirectDriveRight=0;}
    public void emergencyReset()throws Exception{RemoteConnection r=remote();if(r!=null){r.emergencyReset();return;}getText("/estop/reset",650);}
    public void sensorPowerOff()throws Exception{RemoteConnection r=remote();if(r!=null){r.sensorOff();return;}try{getText("/sensor/off",650);}catch(Exception e){mqPower(false);}}
    public void heartbeat()throws Exception{RemoteConnection r=remote();if(r!=null){r.heartbeat();return;}getText("/heartbeat",500);}
    public void disarmHeartbeat()throws Exception{RemoteConnection r=remote();if(r!=null){r.command("heartbeat_disarm",new JSONObject(),2000,false,0);return;}getText("/heartbeat?arm=0",500);}

    /** Raw endpoints intentionally remain local for provisioning and direct-mode compatibility checks. */
    public JSONObject diagnosticsRaw()throws Exception{JSONObject d=getJson("/diag",1000);observeDirectCapabilities(d);return d;}
    public JSONObject dataRaw()throws Exception{return getJson("/data",1200);}
    public JSONObject snapshot(int commandedLeft,int commandedRight)throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(commandedLeft,commandedRight);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return RobotProtocolV4.normalize(diagnosticsRaw(),dataRaw(),commandedLeft,commandedRight);}
    public JSONObject diagnostics()throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(0,0);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return diagnosticsRaw();}
    public JSONObject data()throws Exception{RemoteConnection r=remote();if(r!=null){if(!r.isConnected())throw new IllegalStateException("remote MQTT disconnected");JSONObject s=r.snapshot(0,0);if(s==null)throw new IllegalStateException("remote telemetry unavailable");return s;}return dataRaw();}

    public JSONObject config()throws Exception{RemoteConnection r=remote();if(r!=null){JSONObject a=r.command("config_get",new JSONObject(),3000,true,2500);JSONObject p=a.optJSONObject("payload");return p==null?new JSONObject():p;}return getJson("/cfg",1200);}

    /** These endpoints intentionally remain local because they bootstrap or reset the robot WAN connection. */
    public JSONObject remoteStatus()throws Exception{return getJson("/remote/status",1800);}
    public JSONObject remoteScan()throws Exception{return getJson("/remote/scan",2800);}
    public JSONObject remoteProvision(String ssid,String password)throws Exception{return new JSONObject(postForm("/remote/provision","ssid="+enc(ssid)+"&password="+enc(password),3000));}
    public JSONObject remoteClear()throws Exception{return new JSONObject(postForm("/remote/clear","confirm=1",1500));}

    /** L298N drive configuration. Direction mapping stays in firmware; operator sets only the speed limit. */
    public JSONObject setConfig(String cameraIp,int speed,int warmupSec,int measureSec)throws Exception{
        RemoteConnection r=remote();if(r!=null){JSONObject p=new JSONObject();p.put("cam",cameraIp==null?"":cameraIp);p.put("speed",speed);p.put("warm",warmupSec);p.put("measure",measureSec);JSONObject a=r.command("config_set",p,4000,true,3000);JSONObject q=a.optJSONObject("payload");return q==null?p:q;}
        String cam=URLEncoder.encode(cameraIp==null?"":cameraIp,StandardCharsets.UTF_8.name());return getJson("/cfg?cam="+cam+"&speed="+speed+"&warm="+warmupSec+"&measure="+measureSec,1500);
    }

    public void startMeasurement()throws Exception{RemoteConnection r=remote();if(r!=null){r.startMeasurement();return;}getText("/measure?cmd=start",800);}
    public void cancelMeasurement()throws Exception{RemoteConnection r=remote();if(r!=null){r.cancelMeasurement();return;}getText("/measure?cmd=cancel",800);}

    public void mqPower(boolean on)throws Exception{relayMask(on?1:0);}
    public void relay(int state)throws Exception{relayMask(state);}
    public void relayMask(int mask)throws Exception{
        if(mask!=0&&mask!=1)throw new IllegalArgumentException("MQ power state must be 0 or 1");
        RemoteConnection r=remote();if(r!=null){r.relayMask(mask);return;}
        getText("/relay?mask="+mask,800);
    }

    public void setAlertLevel(int severity)throws Exception{setAlertLevel(severity,3000);}
    public void setAlertLevel(int severity,int ttlMs)throws Exception{
        if(severity<0||severity>2)throw new IllegalArgumentException("Alert severity must be 0..2");
        int ttl=Math.max(500,Math.min(10000,ttlMs));RemoteConnection r=remote();if(r!=null){r.setAlertLevel(severity,ttl);return;}
        getText("/alert?severity="+severity+"&ttl="+ttl,800);
    }
    public void alertTest()throws Exception{RemoteConnection r=remote();if(r!=null){r.alertTest();return;}getText("/alert?cmd=test",800);}

    public void cameraFlash(String mode)throws Exception{RemoteConnection r=remote();if(r==null)throw new IllegalStateException("remote session unavailable");r.cameraFlash(mode);}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s==null?"":s,StandardCharsets.UTF_8.name());}
}
