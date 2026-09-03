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
volatile bool running=false,connected=false;
volatile uint32_t sentFrames=0,connectErrors=0,sendErrors=0,lastFrameBytes=0;
uint32_t lastConnectAt=0;
constexpr char WS_GUID[]="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

String b64(const uint8_t* data,size_t len){size_t outLen=0;size_t cap=((len+2)/3)*4+4;char* out=(char*)malloc(cap);if(!out)return"";if(mbedtls_base64_encode((unsigned char*)out,cap,&outLen,data,len)!=0){free(out);return"";}String s;for(size_t i=0;i<outLen;i++)s+=(char)out[i];free(out);return s;}
String expectedAccept(const String& key){String in=key+WS_GUID;uint8_t digest[20];mbedtls_sha1_context c;mbedtls_sha1_init(&c);mbedtls_sha1_starts(&c);mbedtls_sha1_update(&c,(const unsigned char*)in.c_str(),in.length());mbedtls_sha1_finish(&c,digest);mbedtls_sha1_free(&c);return b64(digest,sizeof(digest));}

bool readLine(String& line,uint32_t timeoutMs){line="";uint32_t until=millis()+timeoutMs;while((int32_t)(until-millis())>0&&ws.connected()){while(ws.available()){char c=(char)ws.read();if(c=='\n'){line.trim();return true;}if(c!='\r')line+=c;if(line.length()>2048)return false;}delay(1);}return false;}

bool handshake(){
  if(!RemoteVideoCfg::configured()||WiFi.status()!=WL_CONNECTED)return false;
  ws.stop();ws.setCACert(RemoteVideoCfg::ROOT_CA);ws.setTimeout(5);
  if(!ws.connect(RemoteVideoCfg::HOST,RemoteVideoCfg::PORT)){connectErrors++;return false;}
  uint8_t nonce[16];esp_fill_random(nonce,sizeof(nonce));String key=b64(nonce,sizeof(nonce));if(key.isEmpty()){ws.stop();connectErrors++;return false;}
  String path=String(RemoteVideoCfg::BASE_PATH)+"/publish/"+RemoteVideoCfg::ROBOT_ID;
  String req="GET "+path+" HTTP/1.1\r\nHost: "+String(RemoteVideoCfg::HOST)+"\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: "+key+"\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Protocol: hazrob-jpeg-v1\r\nAuthorization: Bearer "+String(RemoteVideoCfg::PUBLISH_TOKEN)+"\r\n\r\n";
  ws.print(req);ws.flush();String line;if(!readLine(line,5000)||line.indexOf(" 101 ")<0){ws.stop();connectErrors++;return false;}String accept="";bool upgrade=false;
  while(readLine(line,2500)){if(line.isEmpty())break;int p=line.indexOf(':');if(p<=0)continue;String k=line.substring(0,p),v=line.substring(p+1);k.trim();v.trim();k.toLowerCase();if(k=="sec-websocket-accept")accept=v;else if(k=="upgrade"){v.toLowerCase();upgrade=v=="websocket";}}
  if(!upgrade||accept!=expectedAccept(key)){ws.stop();connectErrors++;return false;}connected=true;return true;
}

void writeLength(size_t len){if(len<=125)ws.write((uint8_t)(0x80|len));else if(len<=65535){ws.write((uint8_t)(0x80|126));ws.write((uint8_t)(len>>8));ws.write((uint8_t)len);}else{ws.write((uint8_t)(0x80|127));for(int i=7;i>=0;i--)ws.write((uint8_t)(((uint64_t)len>>(8*i))&0xFF));}}

bool sendMaskedFrame(uint8_t opcode,const uint8_t* data,size_t len){
  if(!ws.connected())return false;ws.write((uint8_t)(0x80|(opcode&0x0F)));writeLength(len);uint8_t mask[4];esp_fill_random(mask,4);ws.write(mask,4);uint8_t temp[768];size_t pos=0;while(pos<len){size_t n=min(sizeof(temp),len-pos);for(size_t i=0;i<n;i++)temp[i]=data[pos+i]^mask[(pos+i)&3];size_t w=ws.write(temp,n);if(w!=n)return false;pos+=n;}ws.flush();return ws.connected();
}

bool readServerFrame(){
  if(!ws.available())return true;int b0=ws.read();if(b0<0)return false;uint32_t until=millis()+250;while(!ws.available()&&(int32_t)(until-millis())>0)delay(1);if(!ws.available())return false;int b1=ws.read();uint8_t opcode=b0&0x0F;uint64_t len=b1&0x7F;if(len==126){int a=ws.read(),b=ws.read();if(a<0||b<0)return false;len=((uint16_t)a<<8)|(uint16_t)b;}else if(len==127){len=0;for(int i=0;i<8;i++){int x=ws.read();if(x<0)return false;len=(len<<8)|(uint8_t)x;}}if(len>125&&opcode>=8)return false;uint8_t payload[125];size_t got=0;while(got<len){if(!ws.available()){if((int32_t)(until-millis())<=0)return false;delay(1);continue;}payload[got++]=(uint8_t)ws.read();}
  if(opcode==8)return false;if(opcode==9)return sendMaskedFrame(10,payload,(size_t)len);return true;
}

void remoteTask(void*){
  uint32_t nextFrame=0;
  while(running){
    if(!RemoteVideoCfg::configured()||WiFi.status()!=WL_CONNECTED){if(ws.connected())ws.stop();connected=false;vTaskDelay(pdMS_TO_TICKS(500));continue;}
    if(!ws.connected()||!connected){connected=false;uint32_t now=millis();if(now-lastConnectAt<RemoteVideoCfg::RETRY_MS){vTaskDelay(pdMS_TO_TICKS(100));continue;}lastConnectAt=now;if(!handshake()){vTaskDelay(pdMS_TO_TICKS(250));continue;}nextFrame=millis();}
    while(ws.available()){if(!readServerFrame()){ws.stop();connected=false;break;}}
    if(!connected){continue;}
    uint32_t now=millis();if((int32_t)(now-nextFrame)<0){vTaskDelay(pdMS_TO_TICKS(5));continue;}nextFrame=now+RemoteVideoCfg::FRAME_INTERVAL_MS;
    size_t len=0;if(!captureFn||!captureFn(frameBuf,RemoteVideoCfg::MAX_JPEG,len)||!len){vTaskDelay(pdMS_TO_TICKS(10));continue;}
    lastFrameBytes=(uint32_t)len;if(!sendMaskedFrame(2,frameBuf,len)){sendErrors++;ws.stop();connected=false;continue;}sentFrames++;
  }
  ws.stop();connected=false;vTaskDelete(nullptr);
}
}

void remoteVideoBegin(RemoteCaptureFn capture){captureFn=capture;if(!RemoteVideoCfg::configured()||taskHandle)return;frameBuf=(uint8_t*)ps_malloc(RemoteVideoCfg::MAX_JPEG);if(!frameBuf)return;running=true;xTaskCreatePinnedToCore(remoteTask,"remote-video",8192,nullptr,1,&taskHandle,0);}
void remoteVideoStop(){running=false;ws.stop();connected=false;taskHandle=nullptr;}
bool remoteVideoConnected(){return connected&&ws.connected();}
String remoteVideoJson(){String j="{\"configured\":"+String(RemoteVideoCfg::configured()?"true":"false")+",\"connected\":"+String(remoteVideoConnected()?"true":"false")+",\"frames\":"+String(sentFrames)+",\"last_jpeg_bytes\":"+String(lastFrameBytes)+",\"connect_errors\":"+String(connectErrors)+",\"send_errors\":"+String(sendErrors)+"}";return j;}
