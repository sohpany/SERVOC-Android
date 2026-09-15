#include <WiFi.h>
#include <WebServer.h>
#include "Hardware.h"
#include "SafetyInterlock.h"

// HR-0101-055 / Baseline A
// Direct local authority only: WIFI_AP + HTTP control/telemetry.
// No STA, WAN provisioning, NAPT, MQTT, TLS, scan or UDP drive is started here.
WebServer web(80);
uint32_t wifiAt=0;

void reply(int code,const char* text){web.send(code,"text/plain",text);}

void startAP(){
  WiFi.persistent(false);
  WiFi.mode(WIFI_AP);
  WiFi.setSleep(false);

  IPAddress ip(192,168,4,1);
  IPAddress subnet(255,255,255,0);
  IPAddress dhcpStart(192,168,4,100);

  // Historical proven local topology: main ESP32=.1, ESP32-CAM=.2.
  WiFi.softAPConfig(ip,ip,subnet,dhcpStart);
  WiFi.softAP(Cfg::SSID);
  wifiAt=millis();
}

void wifiWatch(){
  if(millis()-wifiAt<5000)return;
  wifiAt=millis();
  if(WiFi.getMode()!=WIFI_AP || WiFi.softAPIP()!=IPAddress(192,168,4,1)){
    motionStop();
    WiFi.softAPdisconnect(true);
    delay(20);
    startAP();
  }
}

void setup(){
  relayBootSafeInit();
  hwBegin();
  startAP();

  web.on("/",[]{reply(200,Cfg::FW);});

  // Baseline A drive authority: HTTP only.
  web.on("/drive",[]{
    if(!web.hasArg("l")||!web.hasArg("r"))return reply(400,"BAD");
    if(safetyEstopLatched())return reply(409,"ESTOP LATCHED");
    linkHeartbeat();
    driveSet(
      constrain(web.arg("l").toInt(),-100L,100L),
      constrain(web.arg("r").toInt(),-100L,100L)
    );
    reply(200,"OK");
  });

  web.on("/stop",[]{motionStop();reply(200,"OK");});
  web.on("/motion/stop",[]{motionStop();reply(200,"OK");});
  web.on("/estop",[]{safetyLatchEstop();emergencyStop();reply(200,"ESTOP LATCHED");});
  web.on("/estop/reset",[]{motionStop();safetyResetEstop();reply(200,"ESTOP RESET");});
  web.on("/sensor/off",[]{sensorPowerOff();reply(200,"OK");});

  web.on("/heartbeat",[]{
    if(web.hasArg("arm")&&web.arg("arm")=="0"){
      linkSetArmed(false);reply(200,"DISARMED");return;
    }
    linkHeartbeat();reply(200,"OK");
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
    if(cmd=="start"){
      linkHeartbeat();
      bool ok=measureStart();
      return reply(ok?200:409,ok?"OK":"MEASURE PRECHECK FAILED");
    }
    if(cmd=="cancel"){measureCancel();return reply(200,"OK");}
    reply(400,"BAD");
  });

  web.on("/alert",[]{
    if(web.hasArg("cmd")&&web.arg("cmd")=="test"){
      alertTest();return reply(200,"OK");
    }
    if(!web.hasArg("severity"))return reply(400,"BAD");
    long severity=web.arg("severity").toInt();
    if(severity<0||severity>2)return reply(409,"BAD");
    uint32_t ttl=web.hasArg("ttl")?(uint32_t)constrain(web.arg("ttl").toInt(),500L,10000L):3000U;
    reply(alertSet((uint8_t)severity,ttl)?200:409,"OK");
  });

  web.on("/data",[]{web.send(200,"application/json",dataJson());});
  web.on("/diag",[]{
    String j=diagJson();
    if(j.endsWith("}")){
      j.remove(j.length()-1);
      j+=",\"estop_latched\":";
      j+=safetyEstopLatched()?"true":"false";
      j+=",\"connection\":{\"mode\":\"direct_ap_http\",\"ap_only\":true,\"udp_drive\":false,\"remote_runtime\":false,\"ap_ip\":\"192.168.4.1\",\"camera_ip\":\"192.168.4.2\",\"ap_clients\":"+String(WiFi.softAPgetStationNum())+"}";
      j+='}';
    }
    web.send(200,"application/json",j);
  });

  web.on("/cfg",[]{
    SettingsPatch patch;
    if(web.hasArg("cam")){patch.hasCam=true;patch.cam=web.arg("cam");}
    if(web.hasArg("speed")){patch.hasSpeed=true;patch.speed=web.arg("speed").toInt();}
    if(web.hasArg("warm")){patch.hasWarm=true;patch.warm=web.arg("warm").toInt();}
    if(web.hasArg("measure")){patch.hasMeasure=true;patch.measure=web.arg("measure").toInt();}
    if(patch.hasCam||patch.hasSpeed||patch.hasWarm||patch.hasMeasure)applySettings(patch,true);
    web.send(200,"application/json",settingsJson());
  });

  // Explicitly report that remote runtime is intentionally absent in Baseline A.
  web.on("/remote/status",[]{web.send(200,"application/json","{\"ok\":true,\"baseline\":\"A\",\"remote_runtime\":false,\"wifi_mode\":\"AP_ONLY\"}");});

  web.onNotFound([]{reply(404,"NOT FOUND");});
  web.begin();
}

void loop(){
  hwUpdate();
  web.handleClient();
  wifiWatch();
  delay(1);
}
