#include <WiFi.h>
#include <WebServer.h>
#include "Hardware.h"

WebServer web(80);
uint32_t wifiAt=0;

void reply(int c,const char* s){web.send(c,"text/plain",s);}

void startAP(){
  WiFi.persistent(false);
  WiFi.mode(WIFI_AP);
  WiFi.setSleep(false);

  IPAddress ip(192,168,4,1);
  IPAddress subnet(255,255,255,0);
  IPAddress dhcpStart(192,168,4,100);

  // 192.168.4.2 is permanently reserved for ESP-CAM.
  WiFi.softAPConfig(ip,ip,subnet,dhcpStart);
  WiFi.softAP(Cfg::SSID);
}

void wifiWatch(){
  if(millis()-wifiAt<5000)return;
  wifiAt=millis();

  if(!(WiFi.getMode()&WIFI_AP)||WiFi.softAPIP()!=IPAddress(192,168,4,1)){
    WiFi.softAPdisconnect(true);
    delay(20);
    startAP();
  }
}

void setup(){
  hwBegin();
  startAP();

  web.on("/",[]{reply(200,"HazRob 0.8.0");});

  web.on("/drive",[]{
    if(!web.hasArg("l")||!web.hasArg("r"))return reply(400,"BAD");
    driveSet(
      constrain(web.arg("l").toInt(),-100L,100L),
      constrain(web.arg("r").toInt(),-100L,100L)
    );
    reply(200,"OK");
  });

  web.on("/stop",[]{
    stopAll();
    reply(200,"OK");
  });

  // Manual relay control. R1/R2 are MQ groups; R3/R4 are reserved.
  web.on("/relay",[]{
    uint8_t mask=0;

    if(web.hasArg("mask")){
      int v=web.arg("mask").toInt();
      if(v<0||v>15)return reply(400,"BAD");
      mask=(uint8_t)v;
    }else if(web.hasArg("g")){
      int g=web.arg("g").toInt();
      if(g<0||g>4)return reply(400,"BAD");
      mask=g==0?0:(uint8_t)(1U<<(g-1));
    }else{
      return reply(400,"BAD");
    }

    if(!relaySetMask(mask))return reply(409,"RELAY MASK REJECTED");
    reply(200,"OK");
  });

  // Measurement never changes relay power in HazRob 0.8.
  // The operator selects R1/R2 first, then starts acquisition.
  web.on("/measure",[]{
    String c=web.arg("cmd");
    if(c=="start")measureStart();
    else if(c=="cancel")measureCancel();
    else return reply(400,"BAD");
    reply(200,"OK");
  });

  web.on("/data",[]{
    web.send(200,"application/json",dataJson());
  });

  web.on("/diag",[]{
    web.send(200,"application/json",diagJson());
  });

  web.on("/cfg",[]{
    bool save=false;

    if(web.hasArg("cam")){
      settings.cam=web.arg("cam");
      save=true;
    }
    if(web.hasArg("speed")){
      settings.speed=constrain(web.arg("speed").toInt(),20L,100L);
      save=true;
    }
    if(web.hasArg("nl")){
      settings.nL=constrain(web.arg("nl").toInt(),1400L,1600L);
      save=true;
    }
    if(web.hasArg("nr")){
      settings.nR=constrain(web.arg("nr").toInt(),1400L,1600L);
      save=true;
    }
    if(web.hasArg("warm")){
      settings.warm=constrain(web.arg("warm").toInt(),0L,3600L);
      save=true;
    }
    if(web.hasArg("measure")){
      settings.measure=constrain(web.arg("measure").toInt(),5L,600L);
      save=true;
    }

    if(save)saveSettings();
    web.send(200,"application/json",settingsJson());
  });

  web.onNotFound([]{
    reply(404,"NOT FOUND");
  });

  web.begin();
}

void loop(){
  hwUpdate();
  web.handleClient();
  wifiWatch();
  delay(1);
}
