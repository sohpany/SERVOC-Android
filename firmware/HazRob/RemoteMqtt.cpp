#include "RemoteMqtt.h"
#include "RemoteCloudConfig.h"
#include "RemoteLink.h"
#include "Hardware.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <time.h>

namespace {
WiFiClientSecure tls;
bool mqtt=false,acked=false,telemetrySent=false;
uint32_t retryAt=0,lastIo=0,lastTelemetry=0,lastStatus=0;
uint16_t packetId=1;
String root,cmdTopic,ackTopic,telemetryTopic,statusTopic,lastSession;
uint64_t lastSeq=0;

void writeUtf(const String& s){uint16_t n=(uint16_t)s.length();tls.write((uint8_t)(n>>8));tls.write((uint8_t)n);tls.write((const uint8_t*)s.c_str(),n);}
void writeRemaining(size_t n){do{uint8_t d=n%128;n/=128;if(n)d|=0x80;tls.write(d);}while(n);}
bool readRemaining(size_t& value){value=0;size_t mult=1;for(int i=0;i<4;i++){int d=tls.read();if(d<0)return false;value+=(d&127)*mult;if(!(d&128))return true;mult*=128;}return false;}
String u64(uint64_t v){char b[24];snprintf(b,sizeof(b),"%llu",(unsigned long long)v);return String(b);}

bool mqttConnectPacket(){uint8_t body[768];size_t p=0;auto put=[&](uint8_t v){if(p<sizeof(body))body[p++]=v;};auto utf=[&](const char* s){size_t n=strlen(s);if(p+2+n>sizeof(body))return false;put((uint8_t)(n>>8));put((uint8_t)n);memcpy(body+p,s,n);p+=n;return true;};if(!utf("MQTT"))return false;put(4);uint8_t flags=0xC2;put(flags);put((uint8_t)(RemoteCfg::KEEPALIVE_SEC>>8));put((uint8_t)RemoteCfg::KEEPALIVE_SEC);String clientId="hazrob-"+String(RemoteCfg::ROBOT_ID);if(!utf(clientId.c_str())||!utf(RemoteCfg::USERNAME)||!utf(RemoteCfg::PASSWORD))return false;tls.write((uint8_t)0x10);writeRemaining(p);tls.write(body,p);tls.flush();lastIo=millis();uint32_t until=millis()+4000;while((int32_t)(until-millis())>0&&!tls.available())delay(5);if(!tls.available())return false;int h=tls.read();size_t len=0;if(h!=0x20||!readRemaining(len)||len!=2)return false;int flagsAck=tls.read(),code=tls.read();return flagsAck>=0&&code==0;}

bool mqttSubscribe(const String& topic){uint16_t id=++packetId;if(!id)id=++packetId;size_t rem=2+2+topic.length()+1;tls.write((uint8_t)0x82);writeRemaining(rem);tls.write((uint8_t)(id>>8));tls.write((uint8_t)id);writeUtf(topic);tls.write((uint8_t)0);tls.flush();lastIo=millis();return true;}
bool mqttPublish(const String& topic,const String& payload,bool retain=false){if(!mqtt||!tls.connected())return false;size_t rem=2+topic.length()+payload.length();tls.write((uint8_t)(retain?0x31:0x30));writeRemaining(rem);writeUtf(topic);tls.write((const uint8_t*)payload.c_str(),payload.length());tls.flush();lastIo=millis();return tls.connected();}

String esc(const String& s){String o;o.reserve(s.length()+8);for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='\\'||c=='\"'){o+='\\';o+=c;}else if(c=='\n')o+="\\n";else if((uint8_t)c>=0x20)o+=c;}return o;}
int keyPos(const String& j,const char* key){String k="\""+String(key)+"\":";return j.indexOf(k);}
String jstr(const String& j,const char* key,const String& def=""){int p=keyPos(j,key);if(p<0)return def;p=j.indexOf('"',p+strlen(key)+3);if(p<0)return def;String o;for(int i=p+1;i<(int)j.length();i++){char c=j[i];if(c=='"')break;if(c=='\\'&&i+1<(int)j.length()){char n=j[++i];o+=(n=='n'?'\n':n);}else o+=c;}return o;}
int64_t jint(const String& j,const char* key,int64_t def=0){int p=keyPos(j,key);if(p<0)return def;p+=strlen(key)+3;while(p<(int)j.length()&&(j[p]==' '||j[p]=='\t'))p++;char* end=nullptr;long long v=strtoll(j.c_str()+p,&end,10);return end==j.c_str()+p?def:v;}

bool fresh(const String& j){uint64_t sent=(uint64_t)jint(j,"sent_ms",0),ttlMs=(uint64_t)jint(j,"ttl_ms",0);if(!sent||!ttlMs)return false;time_t now=time(nullptr);if(now<1700000000)return false;uint64_t nowMs=(uint64_t)now*1000ULL;return sent<=nowMs+60000ULL&&nowMs<=sent+ttlMs;}

bool setCameraFlash(const String& mode){if(mode!="on"&&mode!="off")return false;String host=settings.cam;host.trim();if(host.isEmpty())host=Cfg::CAM;WiFiClient c;c.setTimeout(2);if(!c.connect(host.c_str(),80))return false;String req="GET /flash?mode="+mode+" HTTP/1.1\r\nHost: "+host+"\r\nConnection: close\r\n\r\n";c.print(req);c.flush();uint32_t until=millis()+2000;while((int32_t)(until-millis())>0&&!c.available()&&c.connected())delay(2);String line=c.available()?c.readStringUntil('\n'):"";c.stop();return line.indexOf(" 200 ")>=0;}

void ack(const String& session,uint64_t seq,const String& nonce,bool ok,const String& payload="{}",const String& error=""){String j="{\"session\":\""+esc(session)+"\",\"seq\":"+u64(seq)+",\"nonce\":\""+esc(nonce)+"\",\"ok\":"+(ok?"true":"false");if(!error.isEmpty())j+=",\"error\":\""+esc(error)+"\"";j+=",\"payload\":"+payload+"}";if(mqttPublish(ackTopic,j,false))acked=true;}

/** One remote operator command surface. Transport security remains MQTT/TLS; no per-user authorization token exists. */
void applyCommand(const String& j){
  String session=jstr(j,"session"),nonce=jstr(j,"nonce"),type=jstr(j,"type");uint64_t seq=(uint64_t)jint(j,"seq",0);
  if(session.isEmpty()||nonce.isEmpty()||type.isEmpty()||!seq){ack(session,seq,nonce,false,"{}","invalid");return;}
  if(!fresh(j)){ack(session,seq,nonce,false,"{}","expired");return;}
  if(lastSession.isEmpty()){lastSession=session;lastSeq=0;}
  else if(session!=lastSession){if(type!="probe"){ack(session,seq,nonce,false,"{}","session");return;}lastSession=session;lastSeq=0;}
  if(seq<=lastSeq){ack(session,seq,nonce,false,"{}","sequence");return;}lastSeq=seq;

  bool ok=true;String payload="{}";
  if(type=="probe"){linkHeartbeat();}
  else if(type=="drive"){int l=constrain((int)jint(j,"left",0),-100,100),r=constrain((int)jint(j,"right",0),-100,100);linkHeartbeat();driveSet(l,r);}
  else if(type=="motion_stop")motionStop();
  else if(type=="estop")emergencyStop();
  else if(type=="sensor_off")sensorPowerOff();
  else if(type=="heartbeat")linkHeartbeat();
  else if(type=="heartbeat_disarm")linkSetArmed(false);
  else if(type=="relay"){linkHeartbeat();int64_t v=jint(j,"mask",-1);ok=(v==0||v==1)&&relaySetMask((uint8_t)v);}
  else if(type=="measure_start"){linkHeartbeat();measureStart();}
  else if(type=="measure_cancel")measureCancel();
  else if(type=="camera_flash"){String mode=jstr(j,"mode","off");ok=setCameraFlash(mode);payload="{\"mode\":\""+esc(mode)+"\"}";}
  else if(type=="config_get")payload=settingsJson();
  else if(type=="config_set"){String cam=jstr(j,"cam",settings.cam);if(!cam.isEmpty())settings.cam=cam;settings.speed=constrain((int)jint(j,"speed",settings.speed),20,100);settings.nL=constrain((int)jint(j,"nl",settings.nL),1400,1600);settings.nR=constrain((int)jint(j,"nr",settings.nR),1400,1600);settings.warm=constrain((int)jint(j,"warm",settings.warm),0,3600);settings.measure=constrain((int)jint(j,"measure",settings.measure),5,600);saveSettings();payload=settingsJson();}
  else ok=false;
  ack(session,seq,nonce,ok,payload,ok?"":"unknown_command");
}

void processPacket(){int h=tls.read();if(h<0)return;size_t rem=0;if(!readRemaining(rem))return;uint8_t type=(uint8_t)h>>4;if(type!=3){while(rem--&&tls.connected()){while(!tls.available())delay(1);tls.read();}return;}if(rem<2)return;int hi=tls.read(),lo=tls.read();if(hi<0||lo<0)return;uint16_t tn=(uint16_t)((hi<<8)|lo);rem-=2;if(tn>rem||tn>256)return;String topic;topic.reserve(tn);for(uint16_t i=0;i<tn;i++){while(!tls.available()&&tls.connected())delay(1);topic+=(char)tls.read();}rem-=tn;if(rem>4096){while(rem--&&tls.connected()){while(!tls.available())delay(1);tls.read();}return;}String payload;payload.reserve(rem);while(rem--&&tls.connected()){while(!tls.available()&&tls.connected())delay(1);if(tls.connected())payload+=(char)tls.read();}if(topic==cmdTopic)applyCommand(payload);lastIo=millis();}

void setCloudState(){remoteSetCloudState(mqtt&&tls.connected(),mqtt&&tls.connected(),acked,telemetrySent,false);}
bool connectCloud(){if(!RemoteCfg::configured()||!remoteInternetReady())return false;tls.stop();tls.setCACert(RemoteCfg::ROOT_CA);tls.setTimeout(4);if(!tls.connect(RemoteCfg::BROKER_HOST,RemoteCfg::BROKER_PORT))return false;if(!mqttConnectPacket()){tls.stop();return false;}mqtt=true;root="hazrob/"+String(RemoteCfg::ROBOT_ID)+"/";cmdTopic=root+"cmd";ackTopic=root+"ack";telemetryTopic=root+"telemetry";statusTopic=root+"status";mqttSubscribe(cmdTopic);lastIo=millis();acked=false;telemetrySent=false;setCloudState();return true;}
void publishTelemetry(){String j="{\"diag\":"+diagJson()+",\"data\":"+dataJson()+"}";if(mqttPublish(telemetryTopic,j,false))telemetrySent=true;setCloudState();}
void publishStatus(){setCloudState();mqttPublish(statusTopic,remoteStatusJson(),true);}
}

void remoteMqttBegin(){root="hazrob/"+String(RemoteCfg::ROBOT_ID)+"/";cmdTopic=root+"cmd";ackTopic=root+"ack";telemetryTopic=root+"telemetry";statusTopic=root+"status";}
void remoteMqttUpdate(){uint32_t now=millis();if(!RemoteCfg::configured()){mqtt=false;setCloudState();return;}if(!remoteInternetReady()){if(mqtt||tls.connected()){tls.stop();mqtt=false;acked=false;telemetrySent=false;setCloudState();}return;}if(!mqtt||!tls.connected()){mqtt=false;setCloudState();if(now-retryAt>=RemoteCfg::RETRY_MS){retryAt=now;connectCloud();}return;}while(tls.available())processPacket();if(now-lastTelemetry>=RemoteCfg::TELEMETRY_MS){lastTelemetry=now;publishTelemetry();}if(now-lastStatus>=RemoteCfg::STATUS_MS){lastStatus=now;publishStatus();}if(now-lastIo>=RemoteCfg::KEEPALIVE_SEC*500UL){tls.write((uint8_t)0xC0);tls.write((uint8_t)0);tls.flush();lastIo=now;}}
void remoteMqttDisconnect(){if(mqtt&&tls.connected()){tls.write((uint8_t)0xE0);tls.write((uint8_t)0);tls.flush();}tls.stop();mqtt=false;acked=false;telemetrySent=false;lastSession="";lastSeq=0;setCloudState();}
bool remoteMqttConnected(){return mqtt&&tls.connected();}
