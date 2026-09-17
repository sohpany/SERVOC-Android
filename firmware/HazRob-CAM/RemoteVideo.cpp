#include "RemoteVideo.h"
#include "RemoteVideoConfig.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <esp_system.h>
#include <esp32-hal-psram.h>
#include <mbedtls/base64.h>
#include <mbedtls/sha1.h>

namespace{
WiFiClientSecure ws;
RemoteCaptureFn cap=nullptr;
TaskHandle_t task=nullptr;
uint8_t* buf=nullptr;
volatile bool run=false,conn=false,local=false;
uint32_t retry=RemoteVideoCfg::RETRY_MIN_MS,nextTry=0,interval=RemoteVideoCfg::FRAME_INTERVAL_MIN_MS;
String token=RemoteVideoCfg::PUBLISH_TOKEN;
bool cred=false;
const char GUID[]="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
constexpr uint32_t TLS_CONNECT_TIMEOUT_MS=5000;
constexpr uint32_t TLS_RUNTIME_TIMEOUT_MS=1500;

String b64(const uint8_t*d,size_t n){
  size_t l=0,c=((n+2)/3)*4+4;char*o=(char*)malloc(c);
  if(!o)return "";
  if(mbedtls_base64_encode((uint8_t*)o,c,&l,d,n)){free(o);return "";}
  String s;for(size_t i=0;i<l;i++)s+=(char)o[i];free(o);return s;
}

String acceptKey(const String&k){
  String x=k+GUID;uint8_t d[20];mbedtls_sha1_context c;
  mbedtls_sha1_init(&c);mbedtls_sha1_starts(&c);
  mbedtls_sha1_update(&c,(uint8_t*)x.c_str(),x.length());
  mbedtls_sha1_finish(&c,d);mbedtls_sha1_free(&c);return b64(d,20);
}

String js(const String&j,const char*k){
  String q="\""+String(k)+"\"";int p=j.indexOf(q);
  if(p<0||(p=j.indexOf(':',p))<0||(p=j.indexOf('"',p))<0)return "";
  int e=j.indexOf('"',++p);return e<0?"":j.substring(p,e);
}

long ji(const String&j,const char*k){
  String q="\""+String(k)+"\"";int p=j.indexOf(q);
  if(p<0||(p=j.indexOf(':',p))<0)return -1;
  return j.substring(p+1).toInt();
}

bool bootstrap(){
  if(cred&&token.length()>=8)return true;
  if(local||WiFi.status()!=WL_CONNECTED||!RemoteVideoCfg::configured())return false;

  WiFiClientSecure c;c.setCACert(RemoteVideoCfg::ROOT_CA);c.setTimeout(TLS_CONNECT_TIMEOUT_MS);
  if(!c.connect(RemoteVideoCfg::HOST,RemoteVideoCfg::PORT))return false;

  c.print("GET "+String(RemoteVideoCfg::BOOTSTRAP_PATH)+
    " HTTP/1.1\r\nHost: "+RemoteVideoCfg::HOST+
    "\r\nAuthorization: Bearer "+RemoteVideoCfg::BOOTSTRAP_TOKEN+
    "\r\nX-HazRob-Client: esp32-cam\r\nConnection: close\r\n\r\n");

  String r;uint32_t t=millis()+TLS_CONNECT_TIMEOUT_MS;
  while((int32_t)(t-millis())>0&&(c.connected()||c.available())){
    while(c.available()&&r.length()<2048)r+=(char)c.read();
    delay(1);
  }
  c.stop();

  if(r.indexOf(" 200 ")<0||
     js(r,"version")!=RemoteVideoCfg::BOOTSTRAP_SERVICE_VERSION||
     ji(r,"protocol")!=RemoteVideoCfg::BOOTSTRAP_PROTOCOL||
     js(r,"client")!="esp32-cam"||
     js(r,"robot")!=RemoteVideoCfg::ROBOT_ID)return false;

  token=js(r,"video_publish_token");
  return cred=token.length()>=8;
}

bool line(String&l,uint32_t ms){
  l="";uint32_t t=millis()+ms;
  while((int32_t)(t-millis())>0&&ws.connected()){
    while(ws.available()){
      char c=ws.read();
      if(c=='\n'){l.trim();return true;}
      if(c!='\r')l+=c;
    }
    delay(1);
  }
  return false;
}

bool connectWS(){
  if(local||!bootstrap())return false;
  ws.stop();ws.setCACert(RemoteVideoCfg::ROOT_CA);ws.setTimeout(TLS_CONNECT_TIMEOUT_MS);
  if(!ws.connect(RemoteVideoCfg::HOST,RemoteVideoCfg::PORT))return false;

  uint8_t n[16];esp_fill_random(n,16);String key=b64(n,16);
  ws.print("GET "+String(RemoteVideoCfg::BASE_PATH)+"/publish/"+RemoteVideoCfg::ROBOT_ID+
    " HTTP/1.1\r\nHost: "+RemoteVideoCfg::HOST+
    "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: "+key+
    "\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Protocol: hazrob-jpeg-v1"
    "\r\nAuthorization: Bearer "+token+"\r\n\r\n");

  String l;if(!line(l,5000))return false;
  if(l.indexOf(" 401 ")>=0){cred=false;token="";}
  if(l.indexOf(" 101 ")<0)return false;

  String a;bool u=false;
  while(line(l,2000)&&l.length()){
    int p=l.indexOf(':');if(p<1)continue;
    String k=l.substring(0,p),v=l.substring(p+1);k.toLowerCase();v.trim();
    if(k=="sec-websocket-accept")a=v;
    if(k=="upgrade"){v.toLowerCase();u=v=="websocket";}
  }
  if(!u||a!=acceptKey(key)){ws.stop();return false;}
  ws.setTimeout(TLS_RUNTIME_TIMEOUT_MS);
  return conn=true;
}

void again(){
  nextTry=millis()+retry;
  retry=min(RemoteVideoCfg::RETRY_MAX_MS,retry*2);
}

void len(size_t n){
  if(n<=125)ws.write((uint8_t)(0x80|n));
  else if(n<=65535){
    ws.write((uint8_t)0xFE);ws.write((uint8_t)(n>>8));ws.write((uint8_t)n);
  }else{
    ws.write((uint8_t)0xFF);
    for(int i=7;i>=0;i--)ws.write((uint8_t)((uint64_t)n>>(i*8)));
  }
}

bool frame(uint8_t op,const uint8_t*d,size_t n){
  if(!ws.connected())return false;
  ws.write((uint8_t)(0x80|op));len(n);
  uint8_t m[4],b[512];esp_fill_random(m,4);ws.write(m,4);
  for(size_t p=0;p<n;){
    size_t z=min(sizeof(b),n-p);
    for(size_t i=0;i<z;i++)b[i]=d[p+i]^m[(p+i)&3];
    if(ws.write(b,z)!=z)return false;p+=z;
  }
  return ws.connected();
}

bool incoming(){
  if(!ws.available())return true;
  int a=ws.read();uint32_t t=millis()+250;
  while(!ws.available()&&(int32_t)(t-millis())>0)delay(1);
  if(!ws.available())return false;

  int b=ws.read();uint8_t op=a&15;uint64_t n=b&127;
  if(n==126){int x=ws.read(),y=ws.read();if(x<0||y<0)return false;n=(x<<8)|y;}
  else if(n==127){n=0;for(int i=0;i<8;i++){int x=ws.read();if(x<0)return false;n=(n<<8)|x;}}

  if(op>=8&&n>125)return false;
  uint8_t p[125];size_t g=0;
  while(g<n){
    if(ws.available())p[g++]=ws.read();
    else if((int32_t)(t-millis())<=0)return false;
    else delay(1);
  }
  if(op==8)return false;
  return op!=9||frame(10,p,n);
}

void worker(void*){
  uint32_t next=0;
  for(;;){
    if(!run){
      if(conn||ws.connected())ws.stop();
      conn=false;
      vTaskDelay(pdMS_TO_TICKS(100));
      continue;
    }

    if(WiFi.status()!=WL_CONNECTED||local){
      if(conn||ws.connected())ws.stop();
      conn=false;
      vTaskDelay(pdMS_TO_TICKS(50));
      continue;
    }

    if(!conn||!ws.connected()){
      conn=false;
      if(nextTry&&(int32_t)(millis()-nextTry)<0){vTaskDelay(pdMS_TO_TICKS(100));continue;}
      if(!connectWS()){again();vTaskDelay(pdMS_TO_TICKS(50));continue;}
      retry=RemoteVideoCfg::RETRY_MIN_MS;nextTry=0;
      interval=RemoteVideoCfg::FRAME_INTERVAL_MIN_MS;next=millis();
    }

    while(ws.available())if(!incoming()){ws.stop();conn=false;again();break;}
    if(!conn)continue;

    uint32_t now=millis();
    if((int32_t)(now-next)<0){vTaskDelay(pdMS_TO_TICKS(3));continue;}
    next=now+interval;

    uint32_t t=millis();size_t n=0;
    if(!cap||!cap(buf,RemoteVideoCfg::MAX_JPEG,n)||!n){
      vTaskDelay(pdMS_TO_TICKS(3));
      continue;
    }

    if(!frame(2,buf,n)){ws.stop();conn=false;again();continue;}

    uint32_t w=millis()-t;
    if(w+6>=interval)interval=min(RemoteVideoCfg::FRAME_INTERVAL_MAX_MS,interval+RemoteVideoCfg::FRAME_INTERVAL_STEP_MS);
    else if(w+20<interval&&interval>RemoteVideoCfg::FRAME_INTERVAL_MIN_MS)
      interval=max(RemoteVideoCfg::FRAME_INTERVAL_MIN_MS,interval-RemoteVideoCfg::FRAME_INTERVAL_STEP_MS);
  }
}
}

void remoteVideoBegin(RemoteCaptureFn f){
  cap=f;
  if(!RemoteVideoCfg::configured())return;
  if(!buf)buf=(uint8_t*)ps_malloc(RemoteVideoCfg::MAX_JPEG);
  if(!buf)return;

  run=true;
  retry=RemoteVideoCfg::RETRY_MIN_MS;
  nextTry=0;

  if(task)return;
  BaseType_t ok=xTaskCreatePinnedToCore(worker,"rv",6144,nullptr,1,&task,0);
  if(ok!=pdPASS){task=nullptr;run=false;}
}

void remoteVideoStop(){run=false;ws.stop();conn=false;}
void remoteVideoSetLocalActive(bool a){local=a;if(a){ws.stop();conn=false;}}
bool remoteVideoConnected(){return conn&&ws.connected();}
String remoteVideoJson(){return String("{\"connected\":")+(remoteVideoConnected()?"true":"false")+"}";}
