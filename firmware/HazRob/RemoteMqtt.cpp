#include "RemoteMqtt.h"
#include "RemoteCloudConfig.h"
#include "RemoteLink.h"
#include "Hardware.h"
#include "Alerts.h"
#include "SafetyInterlock.h"
#include "SafetyOptions.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <time.h>

namespace{
constexpr uint32_t CONTROL_TELEMETRY_MS=150;
constexpr uint32_t TLS_CONNECT_TIMEOUT_MS=5000;
constexpr uint32_t TLS_RUNTIME_TIMEOUT_MS=1000;
constexpr uint32_t LOCAL_HTTP_TIMEOUT_MS=1000;
WiFiClientSecure tls;bool mqtt=false,acked=false,telemetrySent=false;uint32_t retryAt=0,lastIo=0,lastTelemetry=0,lastStatus=0;uint16_t packetId=1;int lastConnack=-1;String root,cmdTopic,ackTopic,telemetryTopic,statusTopic,lastSession;uint64_t lastSeq=0;String cloudUser=RemoteCfg::USERNAME,cloudPass=RemoteCfg::PASSWORD;bool credsReady=false;
String compact(const String&s){String o;o.reserve(s.length());for(char c:s)if(c!=' '&&c!='\t'&&c!='\r'&&c!='\n')o+=c;return o;}
String esc(const String&s){String o;o.reserve(s.length()+4);for(char c:s){if(c=='\\'||c=='\"')o+='\\';if((uint8_t)c>=0x20)o+=c;}return o;}
int kp(const String&j,const char*k){String x="\""+String(k)+"\":";return j.indexOf(x);}String js(const String&j,const char*k,const String&d=""){int p=kp(j,k);if(p<0)return d;p=j.indexOf('"',p+strlen(k)+3);if(p<0)return d;String o;for(++p;p<(int)j.length()&&j[p]!='"';p++){if(j[p]=='\\'&&p+1<(int)j.length())p++;o+=j[p];}return o;}
int64_t ji(const String&j,const char*k,int64_t d=0){int p=kp(j,k);if(p<0)return d;p+=strlen(k)+3;while(p<(int)j.length()&&(j[p]==' '||j[p]=='\t'))p++;bool neg=p<(int)j.length()&&j[p]=='-';if(neg)p++;if(p>=(int)j.length()||j[p]<'0'||j[p]>'9')return d;int64_t v=0;while(p<(int)j.length()&&j[p]>='0'&&j[p]<='9')v=v*10+(j[p++]-'0');return neg?-v:v;}
void writeUtf(const String&s){uint16_t n=s.length();tls.write((uint8_t)(n>>8));tls.write((uint8_t)n);tls.write((const uint8_t*)s.c_str(),n);}void writeRemaining(size_t n){do{uint8_t d=n%128;n/=128;if(n)d|=0x80;tls.write(d);}while(n);}bool readRemaining(size_t&v){v=0;size_t m=1;for(uint8_t i=0;i<4;i++){int d=tls.read();if(d<0)return false;v+=(d&127)*m;if(!(d&128))return true;m*=128;}return false;}
String u64s(uint64_t v){char b[21];uint8_t p=20;b[p]=0;do{b[--p]='0'+v%10;v/=10;}while(v&&p);return String(b+p);}
String safetyAutoStopPayload(){return String("{\"enabled\":")+(safetyAutoStopEnabled()?"true":"false")+",\"warnings_active\":true,\"estop_independent\":true,\"watchdog_independent\":true}";}

bool bootstrapCredentials(bool force=false){
  if(!force&&credsReady&&cloudUser.length()&&cloudPass.length())return true;
  remoteSetCloudDetail(force?"bootstrap_refresh":"bootstrap_start");
  WiFiClientSecure c;c.setCACert(RemoteCfg::ROOT_CA);c.setTimeout(TLS_CONNECT_TIMEOUT_MS);
  if(!c.connect(RemoteCfg::BOOTSTRAP_HOST,RemoteCfg::BOOTSTRAP_PORT)){remoteSetCloudDetail("bootstrap_tls_failed");return cloudUser.length()&&cloudPass.length();}
  c.print("GET "+String(RemoteCfg::BOOTSTRAP_PATH)+" HTTP/1.1\r\nHost: "+RemoteCfg::BOOTSTRAP_HOST+"\r\nAuthorization: Bearer "+RemoteCfg::BOOTSTRAP_TOKEN+"\r\nX-HazRob-Client: esp32\r\nAccept: application/json\r\nConnection: close\r\n\r\n");
  uint32_t until=millis()+TLS_CONNECT_TIMEOUT_MS;String status=c.readStringUntil('\n');bool ok=status.indexOf(" 200 ")>=0;while((int32_t)(until-millis())>0&&c.connected()){String h=c.readStringUntil('\n');if(h=="\r"||h.length()<=1)break;}
  String body;body.reserve(512);while((int32_t)(until-millis())>0&&(c.connected()||c.available())){while(c.available()&&body.length()<2048)body+=(char)c.read();if(body.length()>=2048)break;delay(1);}c.stop();
  if(!ok){remoteSetCloudDetail(status.length()?"bootstrap_http_failed":"bootstrap_timeout");return cloudUser.length()&&cloudPass.length();}
  String version=js(body,"version"),role=js(body,"client"),robot=js(body,"robot"),u=js(body,"mqtt_user"),p=compact(js(body,"mqtt_password"));int64_t protocol=ji(body,"protocol",-1);
  if(version!=RemoteCfg::BOOTSTRAP_SERVICE_VERSION||protocol!=RemoteCfg::BOOTSTRAP_PROTOCOL||role!="esp32"||robot!=RemoteCfg::ROBOT_ID||u.isEmpty()||p.length()<8){remoteSetCloudDetail("bootstrap_contract_mismatch");return false;}
  cloudUser=u;cloudPass=p;credsReady=true;remoteSetCloudDetail("bootstrap_ok");return true;
}

bool mqttConnectPacket(){lastConnack=-1;uint8_t body[1024];size_t p=0;auto put=[&](uint8_t v){if(p<sizeof(body))body[p++]=v;};auto utf=[&](const char*s){size_t n=strlen(s);if(p+2+n>sizeof(body))return false;put(n>>8);put(n);memcpy(body+p,s,n);p+=n;return true;};if(!utf("MQTT"))return false;put(4);put(0xE6);put(RemoteCfg::KEEPALIVE_SEC>>8);put(RemoteCfg::KEEPALIVE_SEC);String clientId="hazrob-"+String(RemoteCfg::ROBOT_ID),willTopic="hazrob/"+String(RemoteCfg::ROBOT_ID)+"/status";if(!utf(clientId.c_str())||!utf(willTopic.c_str())||!utf("{\"mqtt\":false,\"tls\":false,\"remote_ready\":false,\"reason\":\"lwt\"}")||!utf(cloudUser.c_str())||!utf(cloudPass.c_str()))return false;tls.write((uint8_t)0x10);writeRemaining(p);tls.write(body,p);tls.flush();lastIo=millis();uint32_t u=millis()+4000;while((int32_t)(u-millis())>0&&!tls.available())delay(5);if(!tls.available()){remoteSetCloudDetail("mqtt_connack_timeout");return false;}int h=tls.read();size_t len=0;if(h!=0x20||!readRemaining(len)||len!=2){remoteSetCloudDetail("mqtt_connack_malformed");return false;}int f=tls.read(),code=tls.read();lastConnack=code;if(f<0||code<0){remoteSetCloudDetail("mqtt_connack_malformed");return false;}if(code!=0){remoteSetCloudDetail("mqtt_connack_"+String(code));return false;}remoteSetCloudDetail("mqtt_connected");return true;}
bool mqttSubscribe(const String&t){uint16_t id=++packetId;if(!id)id=++packetId;tls.write((uint8_t)0x82);writeRemaining(5+t.length());tls.write(id>>8);tls.write(id);writeUtf(t);tls.write((uint8_t)0);tls.flush();lastIo=millis();uint32_t until=millis()+4000;while((int32_t)(until-millis())>0&&!tls.available())delay(2);if(!tls.available()){remoteSetCloudDetail("mqtt_suback_timeout");return false;}int h=tls.read();size_t len=0;if(((uint8_t)h>>4)!=9||!readRemaining(len)||len<3){remoteSetCloudDetail("mqtt_suback_malformed");return false;}int hi=tls.read(),lo=tls.read(),code=tls.read();for(size_t n=3;n<len&&tls.available();n++)tls.read();if(hi<0||lo<0||code<0||(((uint16_t)hi<<8)|(uint16_t)lo)!=id){remoteSetCloudDetail("mqtt_suback_mismatch");return false;}if(code==0x80){remoteSetCloudDetail("mqtt_suback_rejected");return false;}remoteSetCloudDetail("mqtt_subscribed");return true;}
bool mqttPublish(const String&t,const String&p,bool retain=false){if(!mqtt||!tls.connected())return false;size_t rem=2+t.length()+p.length();tls.write((uint8_t)(retain?0x31:0x30));writeRemaining(rem);writeUtf(t);tls.write((const uint8_t*)p.c_str(),p.length());tls.flush();lastIo=millis();return tls.connected();}
bool fresh(const String&j){uint64_t s=ji(j,"sent_ms"),ttl=ji(j,"ttl_ms");if(!s||!ttl)return false;time_t t=time(nullptr);if(t<1700000000)return false;uint64_t n=(uint64_t)t*1000ULL;return s<=n+60000ULL&&n<=s+ttl;}
bool setCameraFlash(const String&m){if(m!="on"&&m!="off")return false;String h=settings.cam;h.trim();if(h.isEmpty())h=Cfg::CAM;WiFiClient c;c.setTimeout(LOCAL_HTTP_TIMEOUT_MS);if(!c.connect(h.c_str(),80))return false;c.print("GET /flash?mode="+m+" HTTP/1.1\r\nHost: "+h+"\r\nConnection: close\r\n\r\n");c.flush();uint32_t u=millis()+2000;while((int32_t)(u-millis())>0&&!c.available()&&c.connected())delay(2);String l=c.available()?c.readStringUntil('\n'):"";c.stop();return l.indexOf(" 200 ")>=0;}
void ack(const String&s,uint64_t q,const String&n,bool ok,const String&p="{}",const String&e=""){String j="{\"session\":\""+esc(s)+"\",\"seq\":"+u64s(q)+",\"nonce\":\""+esc(n)+"\",\"ok\":"+(ok?"true":"false");if(!e.isEmpty())j+=",\"error\":\""+esc(e)+"\"";j+=",\"payload\":"+p+"}";if(mqttPublish(ackTopic,j))acked=true;}
void applyCommand(const String&j){String s=js(j,"session"),n=js(j,"nonce"),t=js(j,"type");uint64_t q=ji(j,"seq");if(s.isEmpty()||n.isEmpty()||t.isEmpty()||!q){ack(s,q,n,false,"{}","invalid");return;}if(!fresh(j)){ack(s,q,n,false,"{}","expired");return;}if(lastSession.isEmpty()){lastSession=s;lastSeq=0;}else if(s!=lastSession){if(t!="probe"){ack(s,q,n,false,"{}","session");return;}lastSession=s;lastSeq=0;}if(q<=lastSeq){ack(s,q,n,false,"{}","sequence");return;}lastSeq=q;bool ok=true;String p="{}",err="";
if(t=="probe"){linkHeartbeat();p=String("{\"estop\":")+(safetyEstopLatched()?"true":"false")+"}";}
else if(t=="drive"){if(safetyEstopLatched()){ok=false;err="estop_latched";}else{linkHeartbeat();driveSet(constrain((int)ji(j,"left"),-100,100),constrain((int)ji(j,"right"),-100,100));}}
else if(t=="motion_stop")motionStop();
else if(t=="estop"){safetyLatchEstop();emergencyStop();p="{\"estop\":true}";}
else if(t=="estop_reset"){motionStop();safetyResetEstop();p="{\"estop\":false}";}
else if(t=="sensor_off")sensorPowerOff();
else if(t=="heartbeat")linkHeartbeat();
else if(t=="heartbeat_disarm")linkSetArmed(false);
else if(t=="relay"){linkHeartbeat();int v=ji(j,"mask",-1);if(safetyEstopLatched()&&v==1){ok=false;err="estop_latched";}else ok=(v==0||v==1)&&relaySetMask(v);}
else if(t=="measure_start"){if(safetyEstopLatched()){ok=false;err="estop_latched";}else{linkHeartbeat();ok=measureStart();if(!ok)err="measurement_precheck";}}
else if(t=="measure_cancel")measureCancel();
else if(t=="alert"){int severity=(int)ji(j,"severity",-1);uint32_t ttlMs=(uint32_t)constrain((long)ji(j,"ttl",3000),500L,10000L);if(severity<0||severity>2)ok=false;else Alerts::setGas((uint8_t)severity,ttlMs);}
else if(t=="alert_test")Alerts::test();
else if(t=="camera_flash"){String m=js(j,"mode","off");ok=setCameraFlash(m);p="{\"mode\":\""+m+"\"}";}
else if(t=="safety_auto_stop_get")p=safetyAutoStopPayload();
else if(t=="safety_auto_stop_set"){motionStop();bool enabled=ji(j,"enabled",0)!=0;safetyAutoStopSet(enabled,true);p=safetyAutoStopPayload();}
else if(t=="config_get")p=settingsJson();
else if(t=="config_set"){SettingsPatch patch;String cam=js(j,"cam");if(kp(j,"cam")>=0&&cam.length()){patch.hasCam=true;patch.cam=cam;}if(kp(j,"speed")>=0){patch.hasSpeed=true;patch.speed=(int)ji(j,"speed",settings.speed);}if(kp(j,"warm")>=0){patch.hasWarm=true;patch.warm=(int)ji(j,"warm",settings.warm);}if(kp(j,"measure")>=0){patch.hasMeasure=true;patch.measure=(int)ji(j,"measure",settings.measure);}applySettings(patch,true);p=settingsJson();}
else{ok=false;err="unknown_command";}
if(!ok&&err.isEmpty())err="command_failed";bool quietSuccess=ok&&(t=="drive"||t=="heartbeat"||t=="alert");if(!quietSuccess)ack(s,q,n,ok,p,err);}
void processPacket(){int h=tls.read();if(h<0)return;size_t rem=0;if(!readRemaining(rem))return;if(((uint8_t)h>>4)!=3){while(rem--&&tls.connected()){while(!tls.available())delay(1);tls.read();}return;}if(rem<2)return;int hi=tls.read(),lo=tls.read();if(hi<0||lo<0)return;uint16_t tn=(hi<<8)|lo;rem-=2;if(tn>rem||tn>256)return;String topic;topic.reserve(tn);for(uint16_t i=0;i<tn;i++){while(!tls.available()&&tls.connected())delay(1);topic+=(char)tls.read();}rem-=tn;if(rem>4096){tls.stop();mqtt=false;remoteSetCloudDetail("mqtt_payload_oversize");return;}String p;p.reserve(rem);while(rem--&&tls.connected()){while(!tls.available()&&tls.connected())delay(1);if(tls.connected())p+=(char)tls.read();}if(topic==cmdTopic)applyCommand(p);lastIo=millis();}
void setCloudState(){remoteSetCloudState(mqtt&&tls.connected(),mqtt&&tls.connected(),acked,telemetrySent,false);}
bool cloudReady(){mqtt=true;root="hazrob/"+String(RemoteCfg::ROBOT_ID)+"/";cmdTopic=root+"cmd";ackTopic=root+"ack";telemetryTopic=root+"telemetry";statusTopic=root+"status";if(!mqttSubscribe(cmdTopic)){mqtt=false;setCloudState();return false;}lastIo=millis();acked=telemetrySent=false;setCloudState();mqttPublish(statusTopic,remoteStatusJson(),true);lastStatus=millis();remoteSetCloudDetail("mqtt_ready");return true;}
bool connectCloud(){if(!RemoteCfg::configured()){remoteSetCloudDetail("cloud_config_invalid");return false;}if(!remoteInternetReady())return false;if(!bootstrapCredentials(false))return false;for(uint8_t attempt=0;attempt<2;attempt++){tls.stop();tls.setCACert(RemoteCfg::ROOT_CA);tls.setTimeout(TLS_CONNECT_TIMEOUT_MS);tls.setNoDelay(true);remoteSetCloudDetail("broker_tls_connect");if(tls.connect(RemoteCfg::BROKER_HOST,RemoteCfg::BROKER_PORT)){remoteSetCloudDetail("broker_tls_ok");if(mqttConnectPacket()&&cloudReady()){tls.setTimeout(TLS_RUNTIME_TIMEOUT_MS);return true;}}else remoteSetCloudDetail("broker_tls_failed");tls.stop();mqtt=false;setCloudState();if(attempt==0&&!bootstrapCredentials(true))break;}return false;}
void publishTelemetry(){String j="{\"diag\":"+diagJson()+",\"data\":"+dataJson()+",\"safety\":{\"estop_latched\":"+(safetyEstopLatched()?String("true"):String("false"))+"}}";if(mqttPublish(telemetryTopic,j))telemetrySent=true;setCloudState();}void publishStatus(){setCloudState();String s=remoteStatusJson();if(s.endsWith("}")){s.remove(s.length()-1);s+=",\"estop_latched\":";s+=safetyEstopLatched()?"true":"false";s+='}';}mqttPublish(statusTopic,s,true);}
}
void remoteMqttBegin(){root="hazrob/"+String(RemoteCfg::ROBOT_ID)+"/";cmdTopic=root+"cmd";ackTopic=root+"ack";telemetryTopic=root+"telemetry";statusTopic=root+"status";remoteSetCloudDetail("mqtt_idle");}
void remoteMqttUpdate(){uint32_t n=millis();if(!RemoteCfg::configured()){mqtt=false;remoteSetCloudDetail("cloud_config_invalid");setCloudState();return;}if(!remoteInternetReady()){if(mqtt||tls.connected()){tls.stop();mqtt=acked=telemetrySent=false;setCloudState();}return;}if(!mqtt||!tls.connected()){if(mqtt)remoteSetCloudDetail("mqtt_disconnected");mqtt=false;setCloudState();if(n-retryAt>=RemoteCfg::RETRY_MS){retryAt=n;connectCloud();}return;}while(tls.available())processPacket();if(n-lastTelemetry>=CONTROL_TELEMETRY_MS){lastTelemetry=n;publishTelemetry();}if(n-lastStatus>=RemoteCfg::STATUS_MS){lastStatus=n;publishStatus();}if(n-lastIo>=RemoteCfg::KEEPALIVE_SEC*500UL){tls.write((uint8_t)0xC0);tls.write((uint8_t)0);tls.flush();lastIo=n;}}
void remoteMqttDisconnect(){if(mqtt&&tls.connected()){remoteSetCloudState(false,false,false,false,false);mqttPublish(statusTopic,remoteStatusJson(),true);tls.write((uint8_t)0xE0);tls.write((uint8_t)0);tls.flush();}tls.stop();mqtt=acked=telemetrySent=false;lastSession="";lastSeq=0;remoteSetCloudDetail("mqtt_disconnected");setCloudState();}bool remoteMqttConnected(){return mqtt&&tls.connected();}
