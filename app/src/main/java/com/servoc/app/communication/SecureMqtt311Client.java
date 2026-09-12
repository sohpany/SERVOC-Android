package com.servoc.app.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/** Minimal MQTT 3.1.1 QoS0 client over verified TLS. No external dependency and no insecure fallback. */
public final class SecureMqtt311Client implements AutoCloseable {
    public interface Listener { void onMessage(String topic,String payload); void onDisconnected(Exception error); }
    private final String host,clientId,username,password;private final int port,keepAliveSeconds;
    private SSLSocket socket;private InputStream in;private OutputStream out;private Thread reader;private volatile Listener listener;private final AtomicBoolean running=new AtomicBoolean(false);private int packetId=1;private long lastWriteMs;
    private final ConcurrentHashMap<Integer,PendingSub> pendingSubs=new ConcurrentHashMap<>();
    private static final class PendingSub{final CountDownLatch latch=new CountDownLatch(1);volatile int code=-1;}

    public SecureMqtt311Client(String host,int port,String clientId,String username,String password){this(host,port,clientId,username,password,30);}
    public SecureMqtt311Client(String host,int port,String clientId,String username,String password,int keepAliveSeconds){
        if(host==null||host.trim().isEmpty())throw new IllegalArgumentException("broker host required");this.host=host.trim();this.port=port<=0?8883:port;this.clientId=clientId==null||clientId.trim().isEmpty()?"hazrob-android":clientId.trim();this.username=username==null?"":username;this.password=password==null?"":password;this.keepAliveSeconds=Math.max(15,Math.min(300,keepAliveSeconds));
    }

    public synchronized void connect(Listener listener)throws Exception{
        close();this.listener=listener;
        SSLSocket s=(SSLSocket)SSLSocketFactory.getDefault().createSocket(host,port);s.setSoTimeout(1200);s.startHandshake();SSLSession session=s.getSession();if(!HttpsURLConnection.getDefaultHostnameVerifier().verify(host,session)){try{s.close();}catch(Exception ignored){}throw new SSLHandshakeException("broker hostname verification failed");}
        socket=s;in=s.getInputStream();out=s.getOutputStream();sendConnect();Packet p=readPacket();int code=p.type==2&&p.body.length>=2?p.body[1]&255:-1;if(code!=0){close();throw new IOException("MQTT CONNACK rejected: "+connackText(code)+" ("+code+")");}running.set(true);reader=new Thread(this::readerLoop,"HazRob-MQTT");reader.setDaemon(true);reader.start();
    }

    public boolean isConnected(){return running.get()&&socket!=null&&socket.isConnected()&&!socket.isClosed();}

    public void subscribe(String topic)throws Exception{
        if(!isConnected())throw new IOException("MQTT not connected");int id;PendingSub p=new PendingSub();synchronized(this){id=nextPacketId();pendingSubs.put(id,p);ByteArrayOutputStream body=new ByteArrayOutputStream();body.write((id>>8)&255);body.write(id&255);writeUtf(body,topic);body.write(0);writePacket(0x82,body.toByteArray());}
        try{if(!p.latch.await(2500,TimeUnit.MILLISECONDS))throw new IOException("MQTT SUBACK timeout: "+topic);if(p.code<0)throw new IOException("MQTT disconnected before SUBACK: "+topic);if(p.code==0x80)throw new IOException("MQTT SUBACK rejected by ACL: "+topic);if(p.code!=0)throw new IOException("MQTT SUBACK unexpected QoS "+p.code+": "+topic);}finally{pendingSubs.remove(id);}
    }

    public synchronized void publish(String topic,String payload)throws Exception{publish(topic,payload,false);}
    public synchronized void publish(String topic,String payload,boolean retain)throws Exception{
        if(!isConnected())throw new IOException("MQTT not connected");ByteArrayOutputStream body=new ByteArrayOutputStream();writeUtf(body,topic);byte[] p=(payload==null?"":payload).getBytes(StandardCharsets.UTF_8);body.write(p);writePacket(retain?0x31:0x30,body.toByteArray());
    }

    private void sendConnect()throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();writeUtf(b,"MQTT");b.write(4);int flags=0x02;if(!username.isEmpty())flags|=0x80;if(!password.isEmpty())flags|=0x40;b.write(flags);b.write((keepAliveSeconds>>8)&255);b.write(keepAliveSeconds&255);writeUtf(b,clientId);if(!username.isEmpty())writeUtf(b,username);if(!password.isEmpty())writeUtf(b,password);writePacket(0x10,b.toByteArray());
    }

    private void readerLoop(){Exception failure=null;try{long lastPing=System.currentTimeMillis();while(running.get()){try{Packet p=readPacket();if(p.type==3)dispatchPublish(p);else if(p.type==9)dispatchSubAck(p);else if(p.type==13){/* PINGRESP */}}catch(SocketTimeoutException timeout){long now=System.currentTimeMillis();if(now-lastWriteMs>keepAliveSeconds*500L&&now-lastPing>keepAliveSeconds*500L){synchronized(this){if(running.get())writePacket(0xC0,new byte[0]);}lastPing=now;}}}}catch(Exception e){failure=e;}finally{boolean was=running.getAndSet(false);try{if(socket!=null)socket.close();}catch(Exception ignored){}for(PendingSub p:pendingSubs.values()){p.code=-1;p.latch.countDown();}if(was&&listener!=null)listener.onDisconnected(failure);}}

    private void dispatchPublish(Packet p)throws Exception{
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(p.body));int n=d.readUnsignedShort();if(n<0||n>p.body.length-2)throw new IOException("invalid MQTT topic");byte[] tb=new byte[n];d.readFully(tb);byte[] payload=new byte[p.body.length-2-n];d.readFully(payload);Listener l=listener;if(l!=null)l.onMessage(new String(tb,StandardCharsets.UTF_8),new String(payload,StandardCharsets.UTF_8));
    }
    private void dispatchSubAck(Packet p)throws Exception{if(p.body.length<3)throw new IOException("malformed MQTT SUBACK");int id=((p.body[0]&255)<<8)|(p.body[1]&255),code=p.body[2]&255;PendingSub s=pendingSubs.get(id);if(s!=null){s.code=code;s.latch.countDown();}}

    private synchronized void writePacket(int header,byte[] body)throws Exception{if(out==null)throw new IOException("MQTT output unavailable");out.write(header);writeRemainingLength(out,body.length);out.write(body);out.flush();lastWriteMs=System.currentTimeMillis();}
    private Packet readPacket()throws Exception{if(in==null)throw new EOFException();int h=in.read();if(h<0)throw new EOFException();int len=readRemainingLength(in);byte[] body=new byte[len];int p=0;while(p<len){int n=in.read(body,p,len-p);if(n<0)throw new EOFException();p+=n;}return new Packet((h>>4)&15,h&15,body);}
    private static final class Packet{final int type,flags;final byte[] body;Packet(int t,int f,byte[] b){type=t;flags=f;body=b;}}

    private int nextPacketId(){packetId=(packetId%65535)+1;return packetId;}
    private static String connackText(int code){switch(code){case 0:return"accepted";case 1:return"unacceptable protocol";case 2:return"identifier rejected";case 3:return"server unavailable";case 4:return"bad username/password";case 5:return"not authorized";default:return"malformed/unknown";}}
    private static void writeUtf(OutputStream out,String s)throws Exception{byte[] b=(s==null?"":s).getBytes(StandardCharsets.UTF_8);if(b.length>65535)throw new IOException("MQTT string too long");out.write((b.length>>8)&255);out.write(b.length&255);out.write(b);}
    private static void writeRemainingLength(OutputStream out,int value)throws Exception{do{int digit=value%128;value/=128;if(value>0)digit|=0x80;out.write(digit);}while(value>0);}
    private static int readRemainingLength(InputStream in)throws Exception{int multiplier=1,value=0,digit,count=0;do{digit=in.read();if(digit<0)throw new EOFException();value+=(digit&127)*multiplier;multiplier*=128;if(++count>4)throw new IOException("malformed MQTT remaining length");}while((digit&128)!=0);return value;}

    @Override public synchronized void close(){running.set(false);try{if(out!=null&&socket!=null&&!socket.isClosed()){out.write(0xE0);out.write(0);out.flush();}}catch(Exception ignored){}try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;in=null;out=null;reader=null;for(PendingSub p:pendingSubs.values()){p.code=-1;p.latch.countDown();}pendingSubs.clear();}
}
