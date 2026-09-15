#include <WiFi.h>
#include <WebServer.h>
#include "Hardware.h"
#include "RemoteLink.h"
#include "RemoteMqtt.h"
#include "SafetyInterlock.h"

// HR-0101-055
// Direct path is restored to the proven local model: SoftAP + HTTP.
// Remote remains available as an independent secondary path over STA/MQTT.
WebServer web(80);
uint32_t wifiAt=0;
bool naptEnabled=false;

void reply(int code,const char* type,const String& body){web.send(code,type,body);}
void reply(int code,const char* body){reply(code,"text/plain",body);}

void startAP(){
  WiFi.persistent(false);
  WiFi.mode(WIFI_AP_STA);
  WiFi.setSleep(false);
  WiFi.setAutoReconnect(false);
  IPAddress ip(192,168,4,1),mask(255,255,255,0),dhcp(192,168,4,100);
  WiFi.softAPConfig(ip,ip,mask,dhcp);
  WiFi.softAP(Cfg::SSID);
  naptEnabled=false;
  wifiAt=millis();
}

void gatewayUpdate(){
  bool ready=remoteInternetReady();
  if(ready!=naptEnabled&&WiFi.AP.enableNAPT(ready))naptEnabled=ready;
}

void wifiWatch(){
  if(millis()-wifiAt<5000)return;
  wifiAt=millis();
  if(!(WiFi.getMode()&WIFI_AP)||WiFi.softAPIP()!=IPAddress(192,168,4,1)){
    motionStop();
    if(naptEnabled)WiFi.AP.enableNAPT(false);
    WiFi.softAPdisconnect(true);
    startAP();
  }
}

String esc(const String&s){String o;o.reserve(s.length()+4);for(char c:s){if(c=='\\'||c=='\"')o+='\\';if((uint8_t)c>=0x20)o+=c;}return o;}
String scanJson(){
  motionStop();
  constexpr uint32_t LIMIT_MS=2000;
  uint32_t started=millis();
  WiFi.scanDelete();
  int n=WiFi.scanNetworks(true,true,false,120);
  while(n==WIFI_SCAN_RUNNING&&millis()-started<LIMIT_MS){
    hwUpdate();
    remoteUpdate();
    gatewayUpdate();
    remoteMqttUpdate();
    delay(5);
    n=WiFi.scanComplete();
  }
  bool timed=n==WIFI_SCAN_RUNNING;
  if(timed||n<0)n=0;
  uint32_t elapsed=min(millis()-started,LIMIT_MS);
  String j="{\"duration_ms\":"+String(elapsed)+",\"timed_out\":"+(timed?"true":"false")+",\"networks\":[";
  for(int i=0;i<n;i++){
    if(i)j+=',';
    j+="{\"ssid\":\""+esc(WiFi.SSID(i))+"\",\"rssi\":"+String(WiFi.RSSI(i))+",\"secure\":"+(WiFi.encryptionType(i)==WIFI_AUTH_OPEN?"false":"true")+",\"channel\":"+String(WiFi.channel(i))+"}";
  }
  WiFi.scanDelete();
  return j+"]}";
}

void setup(){
  relayBootSafeInit();
  hwBegin();
  startAP();

  remoteBegin();
  remoteMqttBegin();

  web.on("/",[]{reply(200,Cfg::FW);});
  web.on("/drive",[]{
    if(!web.hasArg("l")||!web.hasArg("r"))return reply(400,"BAD");
    if(safetyEstopLatched())return reply(409,"ESTOP LATCHED");
    linkHeartbeat();
    driveSet(constrain(web.arg("l").toInt(),-100L,100L),constrain(web.arg("r").toInt(),-100L,100L));
    reply(200,"OK");
  });
  web.on("/stop",[]{motionStop();reply(200,"OK");});
  web.on("/motion/stop",[]{motionStop();reply(200,"OK");});
  web.on("/estop",[]{safetyLatchEstop();emergencyStop();reply(200,"ESTOP LATCHED");});
  web.on("/estop/reset",[]{motionStop();safetyResetEstop();reply(200,"ESTOP RESET");});
  web.on("/sensor/off",[]{sensorPowerOff();reply(200,"OK");});
  web.on("/heartbeat",[]{
    if(web.hasArg("arm")&&web.arg("arm")=="0"){linkSetArmed(false);return reply(200,"DISARMED");}
    linkHeartbeat();reply(200,"OK");
  });

  web.on("/remote/status",[]{
    String j=remoteStatusJson();
    if(j.endsWith("}")){
      j.remove(j.length()-1);
      j+=",\"napt\":";j+=naptEnabled?"true":"false";
      j+=",\"estop_latched\":";j+=safetyEstopLatched()?"true":"false";
      j+=",\"direct_protocol\":\"HTTP\"}";
    }
    reply(200,"application/json",j);
  });
  web.on("/remote/scan",[]{reply(200,"application/json",scanJson());});
  web.on("/remote/provision",HTTP_POST,[]{
    motionStop();
    String s=web.arg("ssid"),p=web.arg("password");
    bool ok=s.length()&&remoteProvision(s,p);
    reply(ok?200:400,"application/json",ok?"{\"ok\":true}":"{\"ok\":false}");
  });
  web.on("/remote/retry",HTTP_POST,[]{
    motionStop();
    bool ok=remoteRetry();
    reply(ok?200:409,"application/json",ok?"{\"ok\":true}":"{\"ok\":false,\"error\":\"not_configured\"}");
  });
  web.on("/remote/clear",HTTP_POST,[]{
    if(web.arg("confirm")!="1")return reply(400,"application/json","{\"ok\":false}");
    motionStop();
    remoteMqttDisconnect();
    if(naptEnabled)WiFi.AP.enableNAPT(false);
    naptEnabled=false;
    remoteClear();
    reply(200,"application/json","{\"ok\":true}");
  });

  web.on("/relay",[]{
    if(!web.hasArg("mask"))return reply(400,"BAD");
    int mask=web.arg("mask").toInt();
    if(mask!=0&&mask!=1)return reply(409,"MQ POWER MUST BE 0 OR 1");
    linkHeartbeat();
    if(!relaySetMask((uint8_t)mask))return reply(409,"RELAY REJECTED");
    reply(200,"OK");
  });
  web.on("/measure",[]{
    String cmd=web.arg("cmd");
    if(cmd=="start"){linkHeartbeat();bool ok=measureStart();return reply(ok?200:409,ok?"OK":"MEASURE PRECHECK FAILED");}
    if(cmd=="cancel"){measureCancel();return reply(200,"OK");}
    reply(400,"BAD");
  });
  web.on("/alert",[]{
    if(web.arg("cmd")=="test"){alertTest();return reply(200,"OK");}
    if(!web.hasArg("severity"))return reply(400,"BAD");
    long severity=web.arg("severity").toInt();
    if(severity<0||severity>2)return reply(409,"BAD");
    uint32_t ttl=web.hasArg("ttl")?(uint32_t)constrain(web.arg("ttl").toInt(),500L,10000L):3000U;
    alertSet((uint8_t)severity,ttl);reply(200,"OK");
  });
  web.on("/data",[]{reply(200,"application/json",dataJson());});
  web.on("/diag",[]{
    String j=diagJson();
    if(j.endsWith("}")){
      j.remove(j.length()-1);
      j+=",\"estop_latched\":";j+=safetyEstopLatched()?"true":"false";
      j+=",\"drive_udp\":{\"installed\":false,\"port\":4210,\"version\":1}";
      j+=",\"direct\":{\"protocol\":\"HTTP\",\"ap_ip\":\"192.168.4.1\",\"camera_ip\":\"192.168.4.2\",\"ap_clients\":"+String(WiFi.softAPgetStationNum())+"}";
      j+='}';
    }
    reply(200,"application/json",j);
  });
  web.on("/cfg",[]{
    SettingsPatch patch;
    if(web.hasArg("cam")){patch.hasCam=true;patch.cam=web.arg("cam");}
    if(web.hasArg("speed")){patch.hasSpeed=true;patch.speed=web.arg("speed").toInt();}
    if(web.hasArg("warm")){patch.hasWarm=true;patch.warm=web.arg("warm").toInt();}
    if(web.hasArg("measure")){patch.hasMeasure=true;patch.measure=web.arg("measure").toInt();}
    if(patch.hasCam||patch.hasSpeed||patch.hasWarm||patch.hasMeasure)applySettings(patch,true);
    reply(200,"application/json",settingsJson());
  });

  web.onNotFound([]{reply(404,"NOT FOUND");});
  web.begin();
}

void loop(){
  hwUpdate();
  web.handleClient();
  remoteUpdate();
  gatewayUpdate();
  remoteMqttUpdate();
  wifiWatch();
  delay(1);
}
