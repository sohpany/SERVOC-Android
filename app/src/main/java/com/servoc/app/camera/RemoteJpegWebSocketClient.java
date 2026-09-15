package com.servoc.app.camera;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/** Minimal RFC6455 WSS JPEG consumer. TLS uses the Android trust store and HTTPS hostname verification. */
final class RemoteJpegWebSocketClient {
    interface Sink{boolean active();void frame(byte[] jpeg);void status(String state,String detail);}
    private static final int MAX_JPEG=1024*1024,MAX_HEADERS=16384;
    private static final int CONNECT_TIMEOUT_MS=4000,IO_TIMEOUT_MS=8000;
    private static final String GUID="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final SecureRandom random=new SecureRandom();
    private volatile SSLSocket socket;

    void close(){SSLSocket s=socket;socket=null;if(s!=null)try{s.close();}catch(Exception ignored){}}

    void stream(String url,String bearer,Sink sink)throws Exception{
        URI u=new URI(url);if(!"wss".equalsIgnoreCase(u.getScheme()))throw new IllegalArgumentException("wss required");String host=u.getHost();if(host==null||host.isEmpty())throw new IllegalArgumentException("host");int port=u.getPort()>0?u.getPort():443;String path=u.getRawPath();if(path==null||path.isEmpty())path="/";if(u.getRawQuery()!=null&&!u.getRawQuery().isEmpty())path+="?"+u.getRawQuery();
        Socket plain=new Socket();SSLSocket s=null;
        try{
            plain.connect(new InetSocketAddress(host,port),CONNECT_TIMEOUT_MS);plain.setTcpNoDelay(true);plain.setKeepAlive(true);
            SSLSocketFactory factory=(SSLSocketFactory)SSLSocketFactory.getDefault();s=(SSLSocket)factory.createSocket(plain,host,port,true);socket=s;s.setSoTimeout(IO_TIMEOUT_MS);SSLParameters p=s.getSSLParameters();p.setEndpointIdentificationAlgorithm("HTTPS");s.setSSLParameters(p);s.startHandshake();
            InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();byte[] nonce=new byte[16];random.nextBytes(nonce);String key=Base64.encodeToString(nonce,Base64.NO_WRAP);String hostHeader=port==443?host:host+":"+port;
            StringBuilder q=new StringBuilder("GET ").append(path).append(" HTTP/1.1\r\nHost: ").append(hostHeader).append("\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: ").append(key).append("\r\nSec-WebSocket-Version: 13\r\n");if(bearer!=null&&!bearer.isEmpty())q.append("Authorization: Bearer ").append(bearer).append("\r\n");q.append("\r\n");out.write(q.toString().getBytes(StandardCharsets.US_ASCII));out.flush();
            String headers=readHeaders(in);String first=headers.substring(0,headers.indexOf("\r\n"));if(!first.contains(" 101 "))throw new IllegalStateException("WSS "+first);String expected=accept(key),actual=header(headers,"Sec-WebSocket-Accept");if(!expected.equals(actual))throw new SecurityException("WebSocket accept mismatch");
            sink.status("streaming",hostHeader+path);ByteArrayOutputStream fragmented=null;int fragmentedOpcode=0;
            while(sink.active()&&s==socket&&!s.isClosed()){
                int b0=in.read();if(b0<0)throw new EOFException();int b1=read1(in);boolean fin=(b0&0x80)!=0,masked=(b1&0x80)!=0;int opcode=b0&0x0F;long len=b1&0x7F;if(len==126)len=((long)read1(in)<<8)|read1(in);else if(len==127){len=0;for(int i=0;i<8;i++)len=(len<<8)|read1(in);}
                if(len>MAX_JPEG)throw new IllegalStateException("WSS frame too large");byte[] mask=masked?readN(in,4):null,payload=readN(in,(int)len);if(mask!=null)for(int i=0;i<payload.length;i++)payload[i]^=mask[i&3];
                if(opcode==8){sendControl(out,8,new byte[0]);return;}if(opcode==9){sendControl(out,10,payload);continue;}if(opcode==10)continue;
                if(opcode==2){if(fin){deliver(payload,sink);}else{fragmented=new ByteArrayOutputStream(Math.min(MAX_JPEG,Math.max(4096,payload.length*2)));fragmented.write(payload);fragmentedOpcode=2;}continue;}
                if(opcode==0&&fragmented!=null){if(fragmented.size()+payload.length>MAX_JPEG)throw new IllegalStateException("fragmented JPEG too large");fragmented.write(payload);if(fin){if(fragmentedOpcode==2)deliver(fragmented.toByteArray(),sink);fragmented=null;fragmentedOpcode=0;}}
            }
        }finally{
            if(socket==s)socket=null;
            if(s!=null)try{s.close();}catch(Exception ignored){}else try{plain.close();}catch(Exception ignored){}
        }
    }

    private void deliver(byte[] p,Sink sink){if(p.length<4||(p[0]&255)!=0xFF||(p[1]&255)!=0xD8||(p[p.length-2]&255)!=0xFF||(p[p.length-1]&255)!=0xD9)return;sink.frame(p);}
    private void sendControl(OutputStream out,int opcode,byte[] payload)throws Exception{if(payload.length>125)return;byte[] mask=new byte[4];random.nextBytes(mask);out.write(0x80|opcode);out.write(0x80|payload.length);out.write(mask);for(int i=0;i<payload.length;i++)out.write(payload[i]^mask[i&3]);out.flush();}
    private static String readHeaders(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();int state=0;while(b.size()<MAX_HEADERS){int x=in.read();if(x<0)throw new EOFException();b.write(x);state=state==0?(x=='\r'?1:0):state==1?(x=='\n'?2:0):state==2?(x=='\r'?3:0):(x=='\n'?4:0);if(state==4)return b.toString("ISO-8859-1");}throw new IllegalStateException("headers too large");}
    private static String header(String headers,String name){String[] lines=headers.split("\r\n");for(String line:lines){int p=line.indexOf(':');if(p>0&&line.substring(0,p).trim().equalsIgnoreCase(name))return line.substring(p+1).trim();}return"";}
    private static String accept(String key)throws Exception{byte[] d=MessageDigest.getInstance("SHA-1").digest((key+GUID).getBytes(StandardCharsets.US_ASCII));return Base64.encodeToString(d,Base64.NO_WRAP);}
    private static int read1(InputStream in)throws Exception{int x=in.read();if(x<0)throw new EOFException();return x;}
    private static byte[] readN(InputStream in,int n)throws Exception{byte[] b=new byte[n];int p=0;while(p<n){int r=in.read(b,p,n-p);if(r<0)throw new EOFException();p+=r;}return b;}
}
