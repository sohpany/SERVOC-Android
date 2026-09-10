#include <WiFi.h>
#include <WebServer.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>

const char* AP_SSID = "4W-TEMP";
const char* UPLINK_SSID = "SHEHAB-LAPTOP";
const char* UPLINK_PASS = "11661166";
const char* RELAY_HOST = "https://hazrob-mini-remote.onrender.com";
const char* CHANNEL = "0Jo6HgI-45pkxeEeiA3fs1rb";

const int ENA = 16;
const int IN1 = 18;
const int IN2 = 27;
const int ENB = 17;
const int IN3 = 19;
const int IN4 = 32;

WebServer server(80);
int leftCmd = 0, rightCmd = 0;
uint32_t lastRemotePoll = 0;

int clamp100(int v){ return constrain(v, -100, 100); }

void motorWrite(int en, int a, int b, int value){
  value = clamp100(value);
  if(value > 0){ digitalWrite(a,HIGH); digitalWrite(b,LOW); }
  else if(value < 0){ digitalWrite(a,LOW); digitalWrite(b,HIGH); }
  else { digitalWrite(a,LOW); digitalWrite(b,LOW); }
  analogWrite(en, map(abs(value),0,100,0,255));
}

void applyDrive(){
  motorWrite(ENA,IN1,IN2,leftCmd);
  motorWrite(ENB,IN3,IN4,rightCmd);
}

void stopDrive(){ leftCmd = rightCmd = 0; applyDrive(); }

void handleDrive(){
  if(!server.hasArg("l") || !server.hasArg("r")){
    server.send(400,"text/plain","BAD"); return;
  }
  leftCmd = clamp100(server.arg("l").toInt());
  rightCmd = clamp100(server.arg("r").toInt());
  applyDrive();
  server.send(200,"application/json","{\"ok\":true}");
}

void handleStatus(){
  String j = "{\"ok\":true,\"l\":"+String(leftCmd)+",\"r\":"+String(rightCmd)+",\"internet\":"+(WiFi.status()==WL_CONNECTED?"true":"false")+"}";
  server.send(200,"application/json",j);
}

int jsonInt(const String& s,const char* key){
  String k = String("\"")+key+"\":";
  int p=s.indexOf(k); if(p<0) return 0; p += k.length();
  while(p<(int)s.length() && s[p]==' ') p++;
  bool neg=false; if(p<(int)s.length() && s[p]=='-'){neg=true;p++;}
  int v=0; while(p<(int)s.length() && isDigit(s[p])) v=v*10+(s[p++]-'0');
  return neg?-v:v;
}

void pollRemote(){
  if(WiFi.status()!=WL_CONNECTED) return;
  uint32_t now=millis(); if(now-lastRemotePoll<120) return; lastRemotePoll=now;
  WiFiClientSecure tls; tls.setInsecure();
  HTTPClient http;
  String url = String(RELAY_HOST)+"/cmd/"+CHANNEL;
  if(!http.begin(tls,url)) return;
  http.setTimeout(800);
  int code=http.GET();
  if(code==200){
    String body=http.getString();
    leftCmd=clamp100(jsonInt(body,"l"));
    rightCmd=clamp100(jsonInt(body,"r"));
    applyDrive();
  }
  http.end();
}

void setup(){
  pinMode(ENA,OUTPUT); pinMode(IN1,OUTPUT); pinMode(IN2,OUTPUT);
  pinMode(ENB,OUTPUT); pinMode(IN3,OUTPUT); pinMode(IN4,OUTPUT);
  stopDrive();

  WiFi.persistent(false);
  WiFi.mode(WIFI_AP_STA);
  WiFi.setSleep(false);
  IPAddress ap(192,168,4,1), mask(255,255,255,0), firstDhcp(192,168,4,100);
  WiFi.softAPConfig(ap,ap,mask,firstDhcp);
  WiFi.softAP(AP_SSID);
  WiFi.begin(UPLINK_SSID,UPLINK_PASS);

  server.on("/",[](){server.send(200,"text/plain","ESP32-4W-TEMP");});
  server.on("/drive",handleDrive);
  server.on("/stop",[](){stopDrive();server.send(200,"text/plain","OK");});
  server.on("/status",handleStatus);
  server.begin();
}

void loop(){
  server.handleClient();
  static bool napt=false;
  bool online = WiFi.status()==WL_CONNECTED;
  if(online!=napt){ if(WiFi.AP.enableNAPT(online)) napt=online; }
  pollRemote();
  delay(2);
}
