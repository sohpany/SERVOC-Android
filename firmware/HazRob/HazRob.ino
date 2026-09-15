#include <WiFi.h>
#include <WebServer.h>
#include "Hardware.h"
#include "RemoteLink.h"
#include "RemoteMqtt.h"
#include "SafetyInterlock.h"

// HR-0101-056 connection authority:
// DIRECT = AP-only + HTTP control/telemetry + local camera network.
// REMOTE = AP+STA + MQTT/Render gateway. Local drive is rejected to prevent dual authority.
WebServer web(80);

enum class RadioProfile:uint8_t{DIRECT=0,REMOTE=1};
RadioProfile radioProfile=RadioProfile::DIRECT;
int8_t pendingProfile=-1;
uint32_t pendingProfileAt=0,wifiAt=0;
bool naptEnabled=false;

void reply(int code,const char* type,const String& body){web.send(code,type,body);}
void reply(int code,const char* body){reply(code,"text/plain",body);}
const char* profileName(){return radioProfile==RadioProfile::DIRECT?"direct":"remote";}

void configureSoftAp(wifi_mode_t mode){
  WiFi.persistent(false);
  WiFi.mode(mode);
  WiFi.setSleep(false);
  WiFi.setAutoReconnect(false);
  IPAddress ip(192,168,4,1),mask(255,255,255,0),dhcp(192,168,4,100);
  WiFi.softAPConfig(ip,ip,mask,dhcp);
  WiFi.softAP(Cfg::SSID);
  wifiAt=millis();
}

void applyRadioProfile(RadioProfile next,bool persistMode){
  motionStop();
  if(next==RadioProfile::DIRECT){
    remoteMqttDisconnect();
    if(naptEnabled)WiFi.AP.enableNAPT(false);
    naptEnabled=false;
    remoteSetTransportEnabled(false,persistMode);
    delay(20);
    configureSoftAp(WIFI_AP);
    radioProfile=RadioProfile::DIRECT;
  }else{
    configureSoftAp(WIFI_AP_STA);
    radioProfile=RadioProfile::REMOTE;
    remoteSetTransportEnabled(true,persistMode);
  }
}
void requestRadioProfile(RadioProfile next){pendingProfile=(int8_t)next;pendingProfileAt=millis();}
void profileUpdate(){
  if(pendingProfile<0||millis()-pendingProfileAt<180)return;
  RadioProfile next=(RadioProfile)pendingProfile;pendingProfile=-1;
  applyRadioProfile(next,true);
}

void gatewayUpdate(){
  if(radioProfile!=RadioProfile::REMOTE){if(naptEnabled){WiFi.AP.enableNAPT(false);naptEnabled=false;}return;}
  bool ready=remoteInternetReady();
  if(ready!=naptEnabled&&WiFi.AP.enableNAPT(ready))naptEnabled=ready;
}

void wifiWatch(){
  if(millis()-wifiAt<5000)return;wifiAt=millis();
  wifi_mode_t expected=radioProfile==RadioProfile::DIRECT?WIFI_AP:WIFI_AP_STA;
  if(WiFi.getMode()!=expected||WiFi.softAPIP()!=IPAddress(192,168,4,1)){
    applyRadioProfile(radioProfile,false);
  }
}

String esc(const String&s){String o;o.reserve(s.length()+4);for(char c:s){if(c=='\\'||c=='\"')o+='\\';if((uint8_t)c>=0x20)o+=c;}return o;}
String scanJson(){
  if(radioProfile!=RadioProfile::REMOTE)return "{\"ok\":false,\"error\":\"direct_ap_locked\"}";
  motionStop();
  constexpr uint32_t LIMIT_MS=2000;uint32_t started=millis();WiFi.scanDelete();
  int n=WiFi.scanNetworks(true,true,false,120);
  while(n==WIFI_SCAN_RUNNING&&millis()-started<LIMIT_MS){hwUpdate();remoteUpdate();gatewayUpdate();remoteMqttUpdate();delay(5);n=WiFi.scanComplete();}
  bool timed=n==WIFI_SCAN_RUNNING;if(timed||n<0)n=0;uint32_t elapsed=min(millis()-started,LIMIT_MS);
  String j="{\"ok\":true,\"duration_ms\":"+String(elapsed)+",\"timed_out\":"+(timed?"true":"false")+",\"networks\":[";
  for(int i=0;i<n;i++){if(i)j+=',';j+="{\"ssid\":\""+esc(WiFi.SSID(i))+"\",\"rssi\":"+String(WiFi.RSSI(i))+",\"secure\":"+(WiFi.encryptionType(i)==WIFI_AUTH_OPEN?"false":"true")+",\"channel\":"+String(WiFi.channel(i))+"}";}
  WiFi.scanDelete();return j+"]}";
}

void setup(){
  relayBootSafeInit();hwBegin();
  WiFi.persistent(false);WiFi.setSleep(false);WiFi.setAutoReconnect(false);
  remoteBegin();remoteMqttBegin();
  applyRadioProfile(remoteTransportEnabled()?RadioProfile::REMOTE:RadioProfile::DIRECT,false);

  web.on("/",[]{reply(200,Cfg::FW);});
  web.on("/drive",[]{
    if(radioProfile!=RadioProfile::DIRECT)return reply(409,"REMOTE MODE");
    if(!web.hasArg("l")||!web.hasArg("r"))return reply(400,"BAD");
    if(safetyEstopLatched())return reply(409,"ESTOP LATCHED");
    linkHeartbeat();driveSet(constrain(web.arg("l").toInt(),-100L,100L),constrain(web.arg("r").toInt(),-100L,100L));reply(200,"OK");
  });
  web.on("/stop",[]{motionStop();reply(200,"OK");});
  web.on("/motion/stop",[]{motionStop();reply(200,"OK");});
  web.on("/estop",[]{safetyLatchEstop();emergencyStop();reply(200,"ESTOP LATCHED");});
  web.on("/estop/reset",[]{motionStop();safetyResetEstop();reply(200,"ESTOP RESET");});
  web.on("/sensor/off",[]{sensorPowerOff();reply(200,"OK");});
  web.on("/heartbeat",[]{if(web.hasArg("arm")&&web.arg("arm")=="0"){linkSetArmed(false);return reply(200,"DISARMED");}linkHeartbeat();reply(200,"OK");});

  web.on("/connection/direct",HTTP_POST,[]{reply(200,"application/json","{\"ok\":true,\"switching_to\":\"direct\"}");requestRadioProfile(RadioProfile::DIRECT);});
  web.on("/connection/remote",HTTP_POST,[]{
    if(!remoteConfigured())return reply(409,"application/json","{\"ok\":false,\"error\":\"not_configured\"}");
    reply(200,"application/json","{\"ok\":true,\"switching_to\":\"remote\"}");requestRadioProfile(RadioProfile::REMOTE);
  });

  web.on("/remote/status",[]{
    String j=remoteStatusJson();
    if(j.endsWith("}")){j.remove(j.length()-1);j+=",\"napt\":";j+=naptEnabled?"true":"false";j+=",\"radio_profile\":\""+String(profileName())+"\"";j+=",\"estop_latched\":";j+=safetyEstopLatched()?"true":"false";j+=",\"direct_protocol\":\"HTTP\"}";}
    reply(200,"application/json",j);
  });
  web.on("/remote/scan",[]{if(radioProfile!=RadioProfile::REMOTE)return reply(409,"application/json",scanJson());reply(200,"application/json",scanJson());});
  web.on("/remote/provision",HTTP_POST,[]{
    motionStop();String s=web.arg("ssid"),p=web.arg("password");bool ok=s.length()&&remoteProvision(s,p);
    reply(ok?200:400,"application/json",ok?"{\"ok\":true,\"switching_to\":\"remote\"}":"{\"ok\":false}");if(ok)requestRadioProfile(RadioProfile::REMOTE);
  });
  web.on("/remote/retry",HTTP_POST,[]{
    if(!remoteConfigured())return reply(409,"application/json","{\"ok\":false,\"error\":\"not_configured\"}");
    if(radioProfile==RadioProfile::REMOTE){motionStop();bool ok=remoteRetry();return reply(ok?200:409,"application/json",ok?"{\"ok\":true}":"{\"ok\":false}");}
    reply(200,"application/json","{\"ok\":true,\"switching_to\":\"remote\"}");requestRadioProfile(RadioProfile::REMOTE);
  });
  web.on("/remote/clear",HTTP_POST,[]{
    if(web.arg("confirm")!="1")return reply(400,"application/json","{\"ok\":false}");
    motionStop();remoteMqttDisconnect();if(naptEnabled)WiFi.AP.enableNAPT(false);naptEnabled=false;remoteClear();
    reply(200,"application/json","{\"ok\":true,\"switching_to\":\"direct\"}");requestRadioProfile(RadioProfile::DIRECT);
  });

  web.on("/relay",[]{if(!web.hasArg("mask"))return reply(400,"BAD");int mask=web.arg("mask").toInt();if(mask!=0&&mask!=1)return reply(409,"MQ POWER MUST BE 0 OR 1");linkHeartbeat();if(!relaySetMask((uint8_t)mask))return reply(409,"RELAY REJECTED");reply(200,"OK");});
  web.on("/measure",[]{String cmd=web.arg("cmd");if(cmd=="start"){linkHeartbeat();bool ok=measureStart();return reply(ok?200:409,ok?"OK":"MEASURE PRECHECK FAILED");}if(cmd=="cancel"){measureCancel();return reply(200,"OK");}reply(400,"BAD");});
  web.on("/alert",[]{if(web.arg("cmd")=="test"){alertTest();return reply(200,"OK");}if(!web.hasArg("severity"))return reply(400,"BAD");long severity=web.arg("severity").toInt();if(severity<0||severity>2)return reply(409,"BAD");uint32_t ttl=web.hasArg("ttl")?(uint32_t)constrain(web.arg("ttl").toInt(),500L,10000L):3000U;alertSet((uint8_t)severity,ttl);reply(200,"OK");});
  web.on("/data",[]{reply(200,"application/json",dataJson());});
  web.on("/diag",[]{
    String j=diagJson();
    if(j.endsWith("}")){j.remove(j.length()-1);j+=",\"estop_latched\":";j+=safetyEstopLatched()?"true":"false";j+=",\"drive_udp\":{\"installed\":false,\"port\":4210,\"version\":1}";j+=",\"connection\":{\"radio_profile\":\""+String(profileName())+"\",\"direct_protocol\":\"HTTP\",\"ap_only\":"+(radioProfile==RadioProfile::DIRECT?String("true"):String("false"))+",\"ap_ip\":\"192.168.4.1\",\"camera_ip\":\"192.168.4.2\",\"ap_clients\":"+String(WiFi.softAPgetStationNum())+"}";j+='}';}
    reply(200,"application/json",j);
  });
  web.on("/cfg",[]{SettingsPatch patch;if(web.hasArg("cam")){patch.hasCam=true;patch.cam=web.arg("cam");}if(web.hasArg("speed")){patch.hasSpeed=true;patch.speed=web.arg("speed").toInt();}if(web.hasArg("warm")){patch.hasWarm=true;patch.warm=web.arg("warm").toInt();}if(web.hasArg("measure")){patch.hasMeasure=true;patch.measure=web.arg("measure").toInt();}if(patch.hasCam||patch.hasSpeed||patch.hasWarm||patch.hasMeasure)applySettings(patch,true);reply(200,"application/json",settingsJson());});
  web.onNotFound([]{reply(404,"NOT FOUND");});
  web.begin();
}

void loop(){
  hwUpdate();web.handleClient();profileUpdate();
  if(radioProfile==RadioProfile::REMOTE){remoteUpdate();gatewayUpdate();remoteMqttUpdate();}
  else gatewayUpdate();
  wifiWatch();delay(1);
}
