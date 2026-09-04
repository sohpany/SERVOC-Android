#include <WiFi.h>
#include <WebServer.h>
#include "Hardware.h"
#include "RemoteLink.h"
#include "RemoteMqtt.h"

WebServer web(80);
uint32_t wifiAt=0;
bool naptEnabled=false;

void reply(int c,const char* s){web.send(c,"text/plain",s);}
String jsonEsc(const String& s){String o;o.reserve(s.length()+8);for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='\\'||c=='\"'){o+='\\';o+=c;}else if((uint8_t)c>=0x20)o+=c;}return o;}
String wifiScanJson(){
  int n=WiFi.scanNetworks(false,true,false,300);
  String j="{\"networks\":[";
  if(n>0){for(int i=0;i<n;i++){if(i)j+=",";j+="{\"ssid\":\""+jsonEsc(WiFi.SSID(i))+"\",\"rssi\":"+String(WiFi.RSSI(i))+",\"secure\":"+(WiFi.encryptionType(i)==WIFI_AUTH_OPEN?"false":"true")+",\"channel\":"+String(WiFi.channel(i))+"}";}}
  j+="]}";WiFi.scanDelete();return j;
}
void startAP(){
  WiFi.persistent(false);WiFi.mode(WIFI_AP_STA);WiFi.setSleep(false);
  IPAddress ip(192,168,4,1),subnet(255,255,255,0),dhcpStart(192,168,4,100);
  WiFi.softAPConfig(ip,ip,subnet,dhcpStart);WiFi.softAP(Cfg::SSID);naptEnabled=false;
}
void gatewayUpdate(){bool want=remoteInternetReady();if(want==naptEnabled)return;if(WiFi.AP.enableNAPT(want))naptEnabled=want;}
void wifiWatch(){if(millis()-wifiAt<5000)return;wifiAt=millis();if(!(WiFi.getMode()&WIFI_AP)||WiFi.softAPIP()!=IPAddress(192,168,4,1)){if(naptEnabled)WiFi.AP.enableNAPT(false);naptEnabled=false;WiFi.softAPdisconnect(true);delay(20);startAP();}}

void setup(){
  hwBegin();startAP();remoteBegin();remoteMqttBegin();
  web.on("/",[]{reply(200,Cfg::FW);});

  // Direct and Remote use the same safety heartbeat semantics.
  web.on("/drive",[]{if(!web.hasArg("l")||!web.hasArg("r"))return reply(400,"BAD");linkHeartbeat();driveSet(constrain(web.arg("l").toInt(),-100L,100L),constrain(web.arg("r").toInt(),-100L,100L));reply(200,"OK");});
  web.on("/stop",[]{motionStop();reply(200,"OK");});
  web.on("/motion/stop",[]{motionStop();reply(200,"OK");});
  web.on("/estop",[]{emergencyStop();reply(200,"OK");});
  web.on("/sensor/off",[]{sensorPowerOff();reply(200,"OK");});
  web.on("/heartbeat",[]{if(web.hasArg("arm")&&web.arg("arm")=="0"){linkSetArmed(false);reply(200,"DISARMED");return;}linkHeartbeat();reply(200,"OK");});

  web.on("/remote/status",HTTP_GET,[]{String j=remoteStatusJson();if(j.endsWith("}")){j.remove(j.length()-1);j+=",\"napt\":";j+=naptEnabled?"true":"false";j+="}";}web.send(200,"application/json",j);});
  web.on("/remote/scan",HTTP_GET,[]{web.send(200,"application/json",wifiScanJson());});
  web.on("/remote/provision",HTTP_POST,[]{if(!web.hasArg("ssid")||!web.hasArg("password")){web.send(400,"application/json","{\"ok\":false,\"error\":\"missing_fields\"}");return;}bool ok=remoteProvision(web.arg("ssid"),web.arg("password"));web.send(ok?200:400,"application/json",ok?"{\"ok\":true}":"{\"ok\":false,\"error\":\"invalid_credentials_format\"}");});
  web.on("/remote/clear",HTTP_POST,[]{if(!web.hasArg("confirm")||web.arg("confirm")!="1"){web.send(400,"application/json","{\"ok\":false}");return;}remoteMqttDisconnect();if(naptEnabled)WiFi.AP.enableNAPT(false);naptEnabled=false;remoteClear();web.send(200,"application/json","{\"ok\":true}");});

  web.on("/relay",[]{if(!web.hasArg("mask"))return reply(400,"BAD");String v=web.arg("mask");if(v!="0"&&v!="1")return reply(409,"MQ POWER MUST BE 0 OR 1");linkHeartbeat();if(!relaySetMask(v=="1"?1:0))return reply(409,"MQ POWER REJECTED");reply(200,"OK");});
  web.on("/measure",[]{String c=web.arg("cmd");if(c=="start"){linkHeartbeat();measureStart();}else if(c=="cancel")measureCancel();else return reply(400,"BAD");reply(200,"OK");});
  web.on("/data",[]{web.send(200,"application/json",dataJson());});
  web.on("/diag",[]{web.send(200,"application/json",diagJson());});
  web.on("/cfg",[]{bool save=false;if(web.hasArg("cam")){settings.cam=web.arg("cam");save=true;}if(web.hasArg("speed")){settings.speed=constrain(web.arg("speed").toInt(),20L,100L);save=true;}if(web.hasArg("nl")){settings.nL=constrain(web.arg("nl").toInt(),1400L,1600L);save=true;}if(web.hasArg("nr")){settings.nR=constrain(web.arg("nr").toInt(),1400L,1600L);save=true;}if(web.hasArg("warm")){settings.warm=constrain(web.arg("warm").toInt(),0L,3600L);save=true;}if(web.hasArg("measure")){settings.measure=constrain(web.arg("measure").toInt(),5L,600L);save=true;}if(save)saveSettings();web.send(200,"application/json",settingsJson());});
  web.onNotFound([]{reply(404,"NOT FOUND");});web.begin();
}

void loop(){hwUpdate();remoteUpdate();gatewayUpdate();remoteMqttUpdate();web.handleClient();wifiWatch();delay(1);}
