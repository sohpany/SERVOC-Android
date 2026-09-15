#include "RemoteVideo.h"
#include "RemoteVideoConfig.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <esp_system.h>
#include <esp32-hal-psram.h>
#include <mbedtls/base64.h>
#include <mbedtls/sha1.h>

namespace {
WiFiClientSecure ws;
RemoteCaptureFn captureFn=nullptr;
TaskHandle_t taskHandle=nullptr;
uint8_t* frameBuf=nullptr;
volatile bool running=false,connected=false,localActive=false;
volatile uint32_t sentFrames=0,connectErrors=0,sendErrors=0,lastFrameBytes=0,retryDelayMs=RemoteVideoCfg::RETRY_MIN_MS;
volatile uint32_t frameIntervalMs=RemoteVideoCfg::FRAME_INTERVAL_MIN_MS,lastFrameWorkMs=0;
uint32_t nextConnectAt=0;
String publishToken=RemoteVideoCfg::PUBLISH_TOKEN;
bool credentialReady=false;
constexpr char WS_GUID[]="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

String compact(const String&s){String o;o.reserve(s.length());for(char c:s)if(c!=' '&&c!='\t'&&c!='\r'&&c!='\n')o+=c;return o;}
int keyPos(const String&j,const char*k){String x="\""+String(k)+"\":";return j.indexOf(x);}
String jsonString(const String&j,const char*k){int p=keyPos(j,k);if(p<0)return"";p=j.indexOf('"',p+strlen(k)+3);if(p<0)return"";String o;for(++p;p<(int)j.length()&&j[p]!='"';p++){if(j[p]=='\\'&&p+1<(int)j.length())p++;o+=j[p];}return o;}
int64_t jsonInt(const String&j,const char*k,int64_t d=-1){int p=keyPos(j,k);if(p<0)return d;p+=strlen(k)+3;while(p<(int)j.length()&&(j[p]==' '||j[p]=='\t'))p++;bool neg=p<(int)j.length()&&j[p]=='-';if(neg)p++;if(p>=(int)j.length()||j[p]<'0'||j[p]>'9')return d;int64_t v=0;while(p<(int)j.length()&&j[p]>='0'&&j[p]<='9')v=v*10+(j[p++]-'0');return neg?-v:v;}
String b64(const uint8_t* data,size_t len){size_t outLen=0;size_t cap=((len+2)/3)*4+4;char* out=(char*)malloc(cap);if(!out)return"";if(mbedtls_base64_encode((unsigned char*)out,cap,&outLen,data,len)!=0){free(out);return"";}String s;for(size_t i=0;i<outLen;i++)s+=(char)out[i];free(out);return s;}
String expectedAccept(const String& key){String in=key+WS_GUID;uint8_t digest[20];mbedtls_sha1_context c;mbedtls_sha1_init(&c);mbedtls_sha1_starts(&c);mbedtls_sha1_update(&c,(const unsigned char*)in.c_str(),in.length());mbedtls_sha1_finish(&c,digest);mbedtls_sha1_free(&c);return b64(digest,sizeof(digest));}

bool bootstrapPublishToken(bool force=false){
  if(!force&&credentialReady&&publishToken.length()>=8)return true;
  if(WiFi.status()!=WL_CONNECTED||!RemoteVideoCfg::configured()||localActive)return false;
  WiFiClientSecure c;c.setCACert(RemoteVideoCfg::ROOT_CA);c.setTimeout(4);
  if(!c.connect(RemoteVideoCfg::HOST,RemoteVideoCfg::PORT))return publishToken.length()>=8;
  c.print("GET "+String(RemoteVideoCfg::BOOTSTRAP_PATH)+" HTTP/1.1\r\nHost: "+RemoteVideoCfg::HOST+"\r\nAuthorization: Bearer "+RemoteVideoCfg::BOOTSTRAP_TOKEN+"\r\nX-HazRob-Client: esp32-cam\r\nAccept: application/json\r\nConnection: close\r\n\r\n");
  uint32_t until=millis()+4000;String status=c.readStringUntil('\n');bool ok=status.indexOf(" 200 ")>=0;
  while((int32_t)(until-millis())>0&&c.connected()){String h=c.readStringUntil('\n');if(h=="\r"||h.length()<=1)break;}
  String body;body.reserve(512);while((int32_t)(until-millis())>0&&(c.connected()||c.available())){while(c.available()&&body.length()<2048)body+=(char)c.read();if(body.length()>=2048)break;delay(1);}c.stop();
  if(!ok)return publishToken.length()>=8;
  String version=jsonString(body,"version"),role=jsonString(body,"client"),robot=jsonString(body,"robot"),token=compact(jsonString(body,"video_publish_token"));
  int64_t protocol=jsonInt(body,"protocol",-1);
  if(version!=RemoteVideoCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteVideoCfg::BOOTSTRAP_PROTOCOL||role!="esp32-cam"||robot!=RemoteVideoCfg::ROBOT_ID||token.length()<8)return publishToken.length()>=8;
  publishToken=token;credentialReady=true;return true;
}

bool readLine(String& line,uint32_t timeoutMs){line="";uint32_t until=millis()+timeoutMs;while((int32_t)(until-millis())>0&&ws.connected()){while(ws.available()){char c=(char)ws.read();if(c=='\n'){line.trim();return true;}if(c!='\r')line+=c;if(line.length()>2048)return false;}delay(1);}return false;}

bool handshake(){
  if(localActive||!RemoteVideoCfg::configured()||WiFi.status()!=WL_CONNECTED||!bootstrapPublishToken(false))return false;
  ws.stop();ws.setCACert(RemoteVideoCfg::ROOT_CA);ws.setTimeout(5);
  if(!ws.connect(RemoteVideoCfg::HOST,RemoteVideoCfg::PORT)){connectErrors++;return false;}
  uint8_t nonce[16];esp_fill_random(nonce,sizeof(nonce));String key=b64(nonce,sizeof(nonce));if(key.isEmpty()){ws.stop();connectErrors++;return false;}
  String path=String(RemoteVideoCfg::BASE_PATH)+"/publish/"+RemoteVideoCfg::ROBOT_ID;
  String req="GET "+path+" HTTP/1.1\r\nHost: "+String(RemoteVideoCfg::HOST)+"\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: "+key+"\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Protocol: hazrob-jpeg-v1\r\nAuthorization: Bearer "+publishToken+"\r\n\r\n";
  ws.print(req);ws.flush();String line;if(!readLine(line,5000)){ws.stop();connectErrors++;return false;}if(line.indexOf(" 401 ")>=0){credentialReady=false;publishToken="";}if(line.indexOf(" 101 ")<0){ws.stop();connectErrors++;return false;}String accept="";bool upgrade=false;
  while(readLine(line,2500)){if(line.isEmpty())break;int p=line.indexOf(':');if(p<=0)continue;String k=line.substring(0,p),v=line.substring(p+1);k.trim();v.trim();k.toLowerCase();if(k=="sec-websocket-accept")accept=v;else if(k=="upgrade"){v.toLowerCase();upgrade=v=="websocket";}}
  if(!upgrade||accept!=expectedAccept(key)){ws.stop();connectErrors++;return false;}connected=true;return true;
}

void scheduleRetry(){uint32_t now=millis(),wait=retryDelayMs;nextConnectAt=now+wait;uint64_t doubled=(uint64_t)retryDelayMs*2ULL;retryDelayMs=(uint32_t)min((uint64_t)RemoteVideoCfg::RETRY_MAX_MS,doubled);}
void resetRetry(){retryDelayMs=RemoteVideoCfg::RETRY_MIN_MS;nextConnectAt=0;}
void writeLength(size_t len){if(len<=125)ws.write((uint8_t)(0x80|len));else if(len<=65535){ws.write((uint8_t)(0x80|126));ws.write((uint8_t)(len>>8));ws.write((uint8_t)len);}else{ws.write((uint8_t)(0x80|127));for(int i=7;i>=0;i--)ws.write((uint8_t)(((uint64_t)len>>(8*i))&0xFF));}}
bool sendMaskedFrame(uint8_t opcode,const uint8_t* data,size_t len){if(!ws.connected())return false;ws.write((uint8_t)(0x80|(opcode&0x0F)));writeLength(len);uint8_t mask[4];esp_fill_random(mask,4);ws.write(mask,4);uint8_t temp[768];size_t pos=0;while(pos<len){size_t n=min(sizeof(temp),len-pos);for(size_t i=0;i<n;i++)temp[i]=data[pos+i]^mask[(pos+i)&3];size_t w=ws.write(temp,n);if(w!=n)return false;pos+=n;}ws.flush();return ws.connected();}
bool readServerFrame(){if(!ws.available())return true;int b0=ws.read();if(b0<0)return false;uint32_t until=millis()+250;while(!ws.available()&&(int32_t)(until-millis())>0)delay(1);if(!ws.available())return false;int b1=ws.read();uint8_t opcode=b0&0x0F;uint64_t len=b1&0x7F;if(len==126){int a=ws.read(),b=ws.read();if(a<0||b<0)return false;len=((uint16_t)a<<8)|(uint16_t)b;}else if(len==127){len=0;for(int i=0;i<8;i++){int x=ws.read();if(x<0)return false;len=(len<<8)|(uint8_t)x;}}if(len>125&&opcode>=8)return false;uint8_t payload[125];size_t got=0;while(got<len){if(!ws.available()){if((int32_t)(until-millis())<=0)return false;delay(1);continue;}payload[got++]=(uint8_t)ws.read();}if(opcode==8)return false;if(opcode==9)return sendMaskedFrame(10,payload,(size_t)len);return true;}
void adaptPacing(uint32_t workMs){lastFrameWorkMs=workMs;uint32_t cur=frameIntervalMs;if(workMs+6>=cur)frameIntervalMs=min(RemoteVideoCfg::FRAME_INTERVAL_MAX_MS,cur+RemoteVideoCfg::FRAME_INTERVAL_STEP_MS);else if(cur>RemoteVideoCfg::FRAME_INTERVAL_MIN_MS&&workMs+20<cur){uint32_t step=min(RemoteVideoCfg::FRAME_INTERVAL_STEP_MS,cur-RemoteVideoCfg::FRAME_INTERVAL_MIN_MS);frameIntervalMs=cur-step;}}

void remoteTask(void*){
  uint32_t nextFrame=0;
  while(running){
    if(!RemoteVideoCfg::configured()||WiFi.status()!=WL_CONNECTED){if(ws.connected())ws.stop();connected=false;vTaskDelay(pdMS_TO_TICKS(500));continue;}
    if(localActive&&!connected){vTaskDelay(pdMS_TO_TICKS(50));continue;}
    if(!ws.connected()||!connected){
      connected=false;uint32_t now=millis();if(nextConnectAt&&(int32_t)(now-nextConnectAt)<0){vTaskDelay(pdMS_TO_TICKS(100));continue;}
      if(localActive){vTaskDelay(pdMS_TO_TICKS(50));continue;}
      if(!handshake()){scheduleRetry();vTaskDelay(pdMS_TO_TICKS(100));continue;}resetRetry();frameIntervalMs=RemoteVideoCfg::FRAME_INTERVAL_MIN_MS;nextFrame=millis();
    }
    while(ws.available()){if(!readServerFrame()){ws.stop();connected=false;scheduleRetry();break;}}
    if(!connected)continue;
    if(localActive){vTaskDelay(pdMS_TO_TICKS(20));continue;}
    uint32_t now=millis();if((int32_t)(now-nextFrame)<0){vTaskDelay(pdMS_TO_TICKS(3));continue;}uint32_t interval=frameIntervalMs;nextFrame=now+interval;
    uint32_t workStart=millis();size_t len=0;if(!captureFn||!captureFn(frameBuf,RemoteVideoCfg::MAX_JPEG,len)||!len){vTaskDelay(pdMS_TO_TICKS(5));continue;}
    lastFrameBytes=(uint32_t)len;if(!sendMaskedFrame(2,frameBuf,len)){sendErrors++;ws.stop();connected=false;scheduleRetry();continue;}sentFrames++;adaptPacing(millis()-workStart);
  }
  ws.stop();connected=false;taskHandle=nullptr;vTaskDelete(nullptr);
}
}

void remoteVideoBegin(RemoteCaptureFn capture){captureFn=capture;if(!RemoteVideoCfg::configured()||taskHandle)return;if(!frameBuf)frameBuf=(uint8_t*)ps_malloc(RemoteVideoCfg::MAX_JPEG);if(!frameBuf)return;running=true;frameIntervalMs=RemoteVideoCfg::FRAME_INTERVAL_MIN_MS;resetRetry();xTaskCreatePinnedToCore(remoteTask,"remote-video",8192,nullptr,1,&taskHandle,0);}
void remoteVideoStop(){running=false;ws.stop();connected=false;}
void remoteVideoSetLocalActive(bool active){localActive=active;}
bool remoteVideoConnected(){return connected&&ws.connected();}
String remoteVideoJson(){String j="{\"configured\":"+String(RemoteVideoCfg::configured()?"true":"false")+",\"credential_ready\":"+String(credentialReady||publishToken.length()>=8?"true":"false")+",\"connected\":"+String(remoteVideoConnected()?"true":"false")+",\"paused_local\":"+String(localActive?"true":"false")+",\"frames\":"+String(sentFrames)+",\"last_jpeg_bytes\":"+String(lastFrameBytes)+",\"connect_errors\":"+String(connectErrors)+",\"send_errors\":"+String(sendErrors)+",\"frame_interval_ms\":"+String(frameIntervalMs)+",\"target_fps\":"+String(1000.0f/(float)frameIntervalMs,1)+",\"last_frame_work_ms\":"+String(lastFrameWorkMs)+",\"retry_backoff_ms\":"+String(retryDelayMs)+"}";return j;}
