#include "RemoteLink.h"
#include "Config.h"
#include <WiFi.h>
#include <Preferences.h>
#include <time.h>

namespace{
Preferences wan;
String ssid,password,cloudDetail="idle";
bool configured=false,transportEnabled=false,dnsOk=false,timeOk=false;
bool cloudTls=false,cloudMqtt=false,cloudAck=false,cloudTelemetry=false,cloudCamera=false;
bool retryHold=false,everConnected=false;
uint8_t connectAttempts=0;
uint32_t connectAt=0,healthAt=0;
constexpr uint32_t RETRY_MS=7000;
constexpr uint32_t HEALTH_MS=5000;
constexpr uint8_t MAX_CONNECT_ATTEMPTS=2;

void resetRuntime(const char* detail){
  dnsOk=timeOk=false;retryHold=false;everConnected=false;connectAttempts=0;
  cloudTls=cloudMqtt=cloudAck=cloudTelemetry=cloudCamera=false;
  cloudDetail=detail;
}
void startSta(){
  if(!transportEnabled||!configured||ssid.isEmpty()||retryHold)return;
  WiFi.begin(ssid.c_str(),password.c_str());
  connectAttempts++;
  connectAt=millis();
  dnsOk=timeOk=false;
  cloudDetail="wifi_connecting";
}
void enterLocalSafeHold(){
  retryHold=true;dnsOk=timeOk=false;
  WiFi.disconnect(false,false);
  cloudDetail="wifi_local_safe";
}
void internetHealth(){
  if(!transportEnabled||WiFi.status()!=WL_CONNECTED){dnsOk=timeOk=false;return;}
  IPAddress ip;dnsOk=WiFi.hostByName("pool.ntp.org",ip)==1;
  if(dnsOk){configTime(0,0,"pool.ntp.org","time.cloudflare.com");time_t now=time(nullptr);timeOk=now>1700000000;}else timeOk=false;
}
String esc(const String& s){String o;o.reserve(s.length()+8);for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='\\'||c=='\"'){o+='\\';o+=c;}else if((uint8_t)c>=0x20)o+=c;}return o;}
}

void remoteBegin(){
  wan.begin("hazwan",false);
  ssid=wan.getString("ssid","");password=wan.getString("pass","");configured=!ssid.isEmpty();
  transportEnabled=wan.getBool("enabled",false);
  WiFi.setSleep(false);WiFi.setAutoReconnect(false);
  resetRuntime(transportEnabled?(configured?"remote_waiting":"wifi_not_configured"):"direct_local");
}
void remoteSetTransportEnabled(bool enabled,bool persistMode){
  transportEnabled=enabled;
  if(persistMode)wan.putBool("enabled",enabled);
  if(!enabled){
    WiFi.disconnect(false,false);
    resetRuntime("direct_local");
    return;
  }
  resetRuntime(configured?"wifi_connecting":"wifi_not_configured");
  if(configured)startSta();
}
bool remoteTransportEnabled(){return transportEnabled;}
bool remoteConfigured(){return configured;}

void remoteUpdate(){
  if(!transportEnabled){dnsOk=timeOk=false;return;}
  uint32_t now=millis();
  if(configured){
    if(WiFi.status()==WL_CONNECTED){
      if(!everConnected){everConnected=true;cloudDetail="wifi_connected";}
      connectAttempts=0;retryHold=false;
    }else if(!retryHold&&now-connectAt>=RETRY_MS){
      if(connectAttempts<MAX_CONNECT_ATTEMPTS)startSta();
      else enterLocalSafeHold();
    }
  }
  if(now-healthAt>=HEALTH_MS){
    healthAt=now;internetHealth();
    if(configured&&retryHold)cloudDetail="wifi_local_safe";
    else if(configured&&WiFi.status()==WL_CONNECTED&&!dnsOk)cloudDetail="dns_wait";
    else if(configured&&WiFi.status()==WL_CONNECTED&&dnsOk&&!timeOk)cloudDetail="time_wait";
  }
}

bool remoteProvision(const String& newSsid,const String& newPassword){
  String s=newSsid;s.trim();if(s.isEmpty()||s.length()>32||newPassword.length()>63)return false;
  ssid=s;password=newPassword;configured=true;wan.putString("ssid",ssid);wan.putString("pass",password);
  retryHold=false;everConnected=false;connectAttempts=0;dnsOk=timeOk=false;
  if(transportEnabled){WiFi.disconnect(false,false);startSta();}else cloudDetail="remote_configured_direct_local";
  return true;
}
bool remoteRetry(){
  if(!transportEnabled||!configured||ssid.isEmpty())return false;
  WiFi.disconnect(false,false);retryHold=false;everConnected=false;connectAttempts=0;dnsOk=timeOk=false;startSta();return true;
}
void remoteClear(){
  configured=false;transportEnabled=false;ssid="";password="";wan.remove("ssid");wan.remove("pass");wan.putBool("enabled",false);
  WiFi.disconnect(false,true);resetRuntime("wifi_not_configured");
}
void remoteSetCloudState(bool tls,bool mqtt,bool commandAck,bool telemetry,bool camera){cloudTls=tls;cloudMqtt=mqtt;cloudAck=commandAck;cloudTelemetry=telemetry;cloudCamera=camera;}
void remoteSetCloudDetail(const String& detail){cloudDetail=detail.length()?detail:"unknown";}

String remoteStatusJson(){
  bool connected=transportEnabled&&WiFi.status()==WL_CONNECTED;
  String ip=connected?WiFi.localIP().toString():"0.0.0.0";
  bool internet=transportEnabled&&connected&&dnsOk&&timeOk;
  bool ready=internet&&cloudTls&&cloudMqtt&&cloudAck&&cloudTelemetry;
  String j="{\"configured\":";j+=configured?"true":"false";
  j+=",\"transport_enabled\":";j+=transportEnabled?"true":"false";
  j+=",\"ssid\":\""+esc(ssid)+"\"";
  j+=",\"wifi\":";j+=connected?"true":"false";
  j+=",\"ip\":\""+ip+"\",\"ip_ok\":";j+=connected?"true":"false";
  j+=",\"rssi\":"+String(connected?WiFi.RSSI():0);
  j+=",\"dns\":";j+=dnsOk?"true":"false";
  j+=",\"time\":";j+=timeOk?"true":"false";
  j+=",\"internet\":";j+=internet?"true":"false";
  j+=",\"tls\":";j+=cloudTls?"true":"false";
  j+=",\"mqtt\":";j+=cloudMqtt?"true":"false";
  j+=",\"command_ack\":";j+=cloudAck?"true":"false";
  j+=",\"telemetry\":";j+=cloudTelemetry?"true":"false";
  j+=",\"remote_ready\":";j+=ready?"true":"false";
  j+=",\"retry_hold\":";j+=retryHold?"true":"false";
  j+=",\"connect_attempts\":"+String(connectAttempts);
  j+=",\"ap_clients\":"+String(WiFi.softAPgetStationNum());
  j+=",\"wifi_channel\":"+String(WiFi.channel());
  j+=",\"detail\":\""+esc(cloudDetail)+"\"}";
  return j;
}
String remoteSsid(){return ssid;}
bool remoteInternetReady(){return transportEnabled&&configured&&WiFi.status()==WL_CONNECTED&&dnsOk&&timeOk;}
