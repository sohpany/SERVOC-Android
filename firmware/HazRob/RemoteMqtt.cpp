#include "RemoteMqtt.h"
#include "RemoteCloudConfig.h"
#include "RemoteLink.h"
#include "Hardware.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <time.h>

namespace {
WiFiClientSecure tls;
PubSubClient mqttClient(tls);
bool mqtt=false,acked=false,telemetrySent=false,tlsHandshakeOk=false;
int mqttConnAck=-1;
uint32_t retryAt=0,lastTelemetry=0,lastStatus=0;
String root,cmdTopic,ackTopic,telemetryTopic,statusTopic,lastSession;
uint64_t lastSeq=0;

String u64(uint64_t v){char b[24];snprintf(b,sizeof(b),"%llu",(unsigned long long)v);return String(b);}
String esc(const String& s){String o;o.reserve(s.length()+8);for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='\\'||c=='\"'){o+='\\';o+=c;}else if(c=='\n')o+="\\n";else if((uint8_t)c>=0x20)o+=c;}return o;}
int keyPos(const String& j,const char* key){return j.indexOf("\""+String(key)+"\":");}
String jstr(const String& j,const char* key,const String& def=""){int p=keyPos(j,key);if(p<0)return def;p=j.indexOf('"',p+strlen(key)+3);if(p<0)return def;String o;for(int i=p+1;i<(int)j.length();i++){char c=j[i];if(c=='"')break;if(c=='\\'&&i+1<(int)j.length()){char n=j[++i];o+=(n=='n'?'\n':n);}else o+=c;}return o;}
int64_t jint(const String& j,const char* key,int64_t def=0){int p=keyPos(j,key);if(p<0)return def;p+=strlen(key)+3;while(p<(int)j.length()&&(j[p]==' '||j[p]=='\t'))p++;char* end=nullptr;long long v=strtoll(j.c_str()+p,&end,10);return end==j.c_str()+p?def:v;}
bool fresh(const String& j){uint64_t sent=(uint64_t)jint(j,"sent_ms",0),ttl=(uint64_t)jint(j,"ttl_ms",0);if(!sent||!ttl)return false;time_t now=time(nullptr);if(now<1700000000)return false;uint64_t ms=(uint64_t)now*1000ULL;return sent<=ms+60000ULL&&ms<=sent+ttl;}

bool mqttPublish(const String& topic,const String& payload,bool retain=false){if(!mqtt||!mqttClient.connected())return false;return mqttClient.publish(topic.c_str(),payload.c_str(),retain);}

bool setCameraFlash(const String& mode){
  if(mode!="on"&&mode!="off")return false;
  String host=settings.cam;host.trim();if(host.isEmpty())host=Cfg::CAM;
  WiFiClient c;c.setTimeout(2);if(!c.connect(host.c_str(),80))return false;
  c.print("GET /flash?mode="+mode+" HTTP/1.1\r\nHost: "+host+"\r\nConnection: close\r\n\r\n");c.flush();
  uint32_t until=millis()+2000;while((int32_t)(until-millis())>0&&!c.available()&&c.connected())delay(2);
  String line=c.available()?c.readStringUntil('\n'):"";c.stop();return line.indexOf(" 200 ")>=0;
}

void ack(const String& session,uint64_t seq,const String& nonce,bool ok,const String& payload="{}",const String& error=""){
  String j="{\"session\":\""+esc(session)+"\",\"seq\":"+u64(seq)+",\"nonce\":\""+esc(nonce)+"\",\"ok\":"+(ok?"true":"false");
  if(!error.isEmpty())j+=",\"error\":\""+esc(error)+"\"";
  j+=",\"payload\":"+payload+"}";
  if(mqttPublish(ackTopic,j,false))acked=true;
}

void applyCommand(const String& j){
  String session=jstr(j,"session"),nonce=jstr(j,"nonce"),type=jstr(j,"type");uint64_t seq=(uint64_t)jint(j,"seq",0);
  if(session.isEmpty()||nonce.isEmpty()||type.isEmpty()||!seq){ack(session,seq,nonce,false,"{}","invalid");return;}
  if(!fresh(j)){ack(session,seq,nonce,false,"{}","expired");return;}
  if(lastSession.isEmpty()){lastSession=session;lastSeq=0;}
  else if(session!=lastSession){if(type!="probe"){ack(session,seq,nonce,false,"{}","session");return;}lastSession=session;lastSeq=0;}
  if(seq<=lastSeq){ack(session,seq,nonce,false,"{}","sequence");return;}lastSeq=seq;

  bool ok=true;String payload="{}";
  if(type=="probe")linkHeartbeat();
  else if(type=="drive"){linkHeartbeat();driveSet(constrain((int)jint(j,"left",0),-100,100),constrain((int)jint(j,"right",0),-100,100));}
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
  else if(type=="config_set"){
    String cam=jstr(j,"cam",settings.cam);if(!cam.isEmpty())settings.cam=cam;
    settings.speed=constrain((int)jint(j,"speed",settings.speed),20,100);
    settings.nL=constrain((int)jint(j,"nl",settings.nL),1400,1600);
    settings.nR=constrain((int)jint(j,"nr",settings.nR),1400,1600);
    settings.warm=constrain((int)jint(j,"warm",settings.warm),0,3600);
    settings.measure=constrain((int)jint(j,"measure",settings.measure),5,600);
    saveSettings();payload=settingsJson();
  }else ok=false;
  ack(session,seq,nonce,ok,payload,ok?"":"unknown_command");
}

void onMessage(char* topic,byte* payload,unsigned int len){
  if(String(topic)!=cmdTopic||len>4096)return;
  String j;j.reserve(len);for(unsigned int i=0;i<len;i++)j+=(char)payload[i];applyCommand(j);
}

void setCloudState(){remoteSetCloudState(tlsHandshakeOk,mqtt&&mqttClient.connected(),acked,telemetrySent,false);}

bool connectCloud(){
  if(!RemoteCfg::configured()||!remoteInternetReady())return false;
  if(mqttClient.connected())mqttClient.disconnect();tls.stop();
  mqtt=false;acked=false;telemetrySent=false;mqttConnAck=-1;setCloudState();

  tls.setCACert(RemoteCfg::ROOT_CA);tls.setTimeout(4);
  tlsHandshakeOk=false;
  if(!tls.connect(RemoteCfg::BROKER_HOST,RemoteCfg::BROKER_PORT)){setCloudState();return false;}
  tlsHandshakeOk=true;setCloudState();

  String clientId="hazrob-"+String(RemoteCfg::ROBOT_ID);
  if(!mqttClient.connect(clientId.c_str(),RemoteCfg::USERNAME,RemoteCfg::PASSWORD)){
    mqttConnAck=mqttClient.state();mqtt=false;setCloudState();tls.stop();return false;
  }

  mqtt=true;mqttConnAck=0;
  if(!mqttClient.subscribe(cmdTopic.c_str())){mqttConnAck=mqttClient.state();}
  acked=false;telemetrySent=false;setCloudState();return true;
}

void publishTelemetry(){String j="{\"diag\":"+diagJson()+",\"data\":"+dataJson()+"}";if(mqttPublish(telemetryTopic,j,false))telemetrySent=true;setCloudState();}
void publishStatus(){setCloudState();mqttPublish(statusTopic,remoteStatusJson(),true);}
}

void remoteMqttBegin(){
  root="hazrob/"+String(RemoteCfg::ROBOT_ID)+"/";cmdTopic=root+"cmd";ackTopic=root+"ack";telemetryTopic=root+"telemetry";statusTopic=root+"status";
  mqttClient.setServer(RemoteCfg::BROKER_HOST,RemoteCfg::BROKER_PORT);
  mqttClient.setCallback(onMessage);
  mqttClient.setKeepAlive(RemoteCfg::KEEPALIVE_SEC);
  mqttClient.setSocketTimeout(4);
  mqttClient.setBufferSize(4096);
}

void remoteMqttUpdate(){
  uint32_t now=millis();
  if(!RemoteCfg::configured()){
    if(mqttClient.connected())mqttClient.disconnect();tls.stop();mqtt=false;tlsHandshakeOk=false;mqttConnAck=-1;setCloudState();return;
  }
  if(!remoteInternetReady()){
    if(mqttClient.connected())mqttClient.disconnect();tls.stop();mqtt=false;acked=false;telemetrySent=false;tlsHandshakeOk=false;mqttConnAck=-1;setCloudState();return;
  }
  if(!mqttClient.connected()){
    int s=mqttClient.state();if(s!=0)mqttConnAck=s;mqtt=false;acked=false;telemetrySent=false;setCloudState();
    if(now-retryAt>=RemoteCfg::RETRY_MS){retryAt=now;connectCloud();}
    return;
  }

  mqtt=true;mqttConnAck=0;
  if(!mqttClient.loop()){mqttConnAck=mqttClient.state();mqtt=false;setCloudState();return;}
  if(now-lastTelemetry>=RemoteCfg::TELEMETRY_MS){lastTelemetry=now;publishTelemetry();}
  if(now-lastStatus>=RemoteCfg::STATUS_MS){lastStatus=now;publishStatus();}
}

void remoteMqttDisconnect(){
  if(mqttClient.connected())mqttClient.disconnect();tls.stop();mqtt=false;acked=false;telemetrySent=false;tlsHandshakeOk=false;mqttConnAck=-1;lastSession="";lastSeq=0;setCloudState();
}

bool remoteMqttConnected(){return mqtt&&mqttClient.connected();}
int remoteMqttConnAck(){return mqttConnAck;}
const char* remoteMqttError(){
  switch(mqttConnAck){
    case 0:return "ok";
    case 1:return "protocol_rejected";
    case 2:return "client_id_rejected";
    case 3:return "server_unavailable";
    case 4:return "bad_credentials";
    case 5:return "not_authorized";
    case -4:return "mqtt_timeout";
    case -3:return "connection_lost";
    case -2:return "tcp_connect_failed";
    case -1:return "disconnected";
    default:return "mqtt_error";
  }
}
