#include <WiFi.h>
#include <WebServer.h>
#include "Hardware.h"

WebServer web(80);
uint32_t wifiAt=0;

void reply(int c,const char* s){
  web.send(c,"text/plain",s);
}

void startAP(){
  WiFi.mode(WIFI_AP);
  IPAddress ip(192,168,4,1);
  WiFi.softAPConfig(ip,ip,IPAddress(255,255,255,0));
  WiFi.softAP(Cfg::SSID);
}

void wifiWatch(){
  if(millis()-wifiAt<5000)return;
  wifiAt=millis();

  if(!(WiFi.getMode()&WIFI_AP) ||
     WiFi.softAPIP()!=IPAddress(192,168,4,1)){
    WiFi.softAPdisconnect(true);
    delay(20);
    startAP();
  }
}

void setup(){
  hwBegin();
  startAP();

  web.on("/",[]{
    reply(200,"HazRob");
  });

  web.on("/drive",[]{
    if(!web.hasArg("l")||!web.hasArg("r"))
      return reply(400,"BAD");

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

  web.on("/relay",[]{
    if(!web.hasArg("g"))
      return reply(400,"BAD");

    int g=web.arg("g").toInt();

    if(g<0||g>3)
      return reply(400,"BAD");

    relaySet(g);
    reply(200,"OK");
  });

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
}
