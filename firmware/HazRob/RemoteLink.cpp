#include "RemoteLink.h"
#include "Config.h"
#include <WiFi.h>
#include <Preferences.h>
#include <time.h>

namespace{
Preferences wan;
String ssid,password,cloudDetail="idle";
bool configured=false,developerAuthorized=false,transportEnabled=false,dnsOk=false,timeOk=false;
bool cloudTls=false,cloudMqtt=false,cloudAck=false,cloudTelemetry=false,cloudCamera=false;
bool retryHold=false,everConnected=false;
uint8_t connectAttempts=0,dnsFailStreak=0;
uint32_t connectAt=0,healthAt=0,retryDelayMs=7000;
constexpr uint32_t RETRY_MIN_MS=7000;
constexpr uint32_t RETRY_MAX_MS=60000;
constexpr uint32_t HEALTH_MS=5000;
constexpr uint8_t DNS_FAIL_LIMIT=3;
constexpr time_t VALID_TIME_MIN=1700000000;

void resetRuntime(const char* detail){
  dnsOk=false;
  timeOk=time(nullptr)>VALID_TIME_MIN;
  retryHold=false;
  everConnected=false;
  connectAttempts=0;
  dnsFailStreak=0;
  retryDelayMs=RETRY_MIN_MS;
  cloudTls=cloudMqtt=cloudAck=cloudTelemetry=cloudCamera=false;
  cloudDetail=detail;
}

void startSta(){
  if(!transportEnabled||!configured||ssid.isEmpty())return;
  WiFi.begin(ssid.c_str(),password.c_str());
  if(connectAttempts<255)connectAttempts++;
  connectAt=millis();
  dnsOk=false;
  dnsFailStreak=0;
  cloudDetail=everConnected?"wifi_reconnecting":"wifi_connecting";
}

void internetHealth(){
  if(!transportEnabled||WiFi.status()!=WL_CONNECTED){
    dnsOk=false;
    dnsFailStreak=0;
    timeOk=time(nullptr)>VALID_TIME_MIN;
    return;
  }

  IPAddress ip;
  bool resolved=WiFi.hostByName("pool.ntp.org",ip)==1;
  if(resolved){
    dnsOk=true;
    dnsFailStreak=0;
    if(!timeOk)configTime(0,0,"pool.ntp.org","time.cloudflare.com");
  }else{
    if(dnsFailStreak<DNS_FAIL_LIMIT)dnsFailStreak++;
    if(dnsFailStreak>=DNS_FAIL_LIMIT)dnsOk=false;
  }
  timeOk=time(nullptr)>VALID_TIME_MIN;
}

String esc(const String& s){String o;o.reserve(s.length()+8);for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='\\'||c=='\"'){o+='\\';o+=c;}else if((uint8_t)c>=0x20)o+=c;}return o;}
}

void remoteBegin(){
  wan.begin("hazwan",false);
  ssid=wan.getString("ssid","");password=wan.getString("pass","");configured=!ssid.isEmpty();developerAuthorized=wan.getBool("authorized",false);
  transportEnabled=false;
  wan.putBool("enabled",false);
  WiFi.setSleep(false);WiFi.setAutoReconnect(false);
  resetRuntime(configured?"direct_saved_wan":"direct_local");
}

void remoteSetTransportEnabled(bool enabled,bool persistMode){
  (void)persistMode;
  wan.putBool("enabled",false);
  if(enabled&&!developerAuthorized){
    transportEnabled=false;
    WiFi.disconnect(false,false);
    resetRuntime("remote_not_developer_authorized");
    return;
  }
  transportEnabled=enabled;
  if(!enabled){
    WiFi.disconnect(false,false);
    resetRuntime(configured?"direct_saved_wan":"direct_local");
    return;
  }
  resetRuntime(configured?"wifi_connecting":"wifi_not_configured");
  if(configured)startSta();
}

bool remoteTransportEnabled(){return transportEnabled;}
bool remoteConfigured(){return configured;}
bool remoteDeveloperAuthorized(){return developerAuthorized;}
void remoteSetDeveloperAuthorized(bool enabled){developerAuthorized=enabled;wan.putBool("authorized",enabled);if(!enabled&&transportEnabled){transportEnabled=false;WiFi.disconnect(false,false);resetRuntime(configured?"direct_saved_wan":"direct_local");}}

void remoteUpdate(){
  if(!transportEnabled){dnsOk=false;return;}
  uint32_t now=millis();

  if(configured){
    if(WiFi.status()==WL_CONNECTED){
      if(!everConnected){everConnected=true;cloudDetail="wifi_connected";}
      connectAttempts=0;
      retryDelayMs=RETRY_MIN_MS;
      retryHold=false;
    }else if(now-connectAt>=retryDelayMs){
      WiFi.disconnect(false,false);
      delay(10);
      startSta();
      retryDelayMs=min(RETRY_MAX_MS,retryDelayMs*2U);
    }
  }

  if(now-healthAt>=HEALTH_MS){
    healthAt=now;
    internetHealth();
    if(configured&&WiFi.status()==WL_CONNECTED&&!dnsOk)cloudDetail="dns_wait";
    else if(configured&&WiFi.status()==WL_CONNECTED&&dnsOk&&!timeOk)cloudDetail="time_wait";
  }
}

bool remoteProvision(const String& newSsid,const String& newPassword){
  String s=newSsid;s.trim();if(s.isEmpty()||s.length()>32||newPassword.length()>63)return false;
  ssid=s;password=newPassword;configured=true;wan.putString("ssid",ssid);wan.putString("pass",password);wan.putBool("enabled",false);
  retryHold=false;everConnected=false;connectAttempts=0;dnsOk=false;dnsFailStreak=0;retryDelayMs=RETRY_MIN_MS;timeOk=time(nullptr)>VALID_TIME_MIN;
  if(transportEnabled){WiFi.disconnect(false,false);startSta();}else cloudDetail="remote_configured_direct_local";
  return true;
}

bool remoteRetry(){
  if(!transportEnabled||!configured||ssid.isEmpty())return false;
  WiFi.disconnect(false,false);retryHold=false;everConnected=false;connectAttempts=0;dnsOk=false;dnsFailStreak=0;retryDelayMs=RETRY_MIN_MS;timeOk=time(nullptr)>VALID_TIME_MIN;startSta();return true;
}

void remoteClear(){
  configured=false;developerAuthorized=false;transportEnabled=false;ssid="";password="";wan.remove("ssid");wan.remove("pass");wan.putBool("authorized",false);wan.putBool("enabled",false);
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
  j+=",\"developer_authorized\":";j+=developerAuthorized?"true":"false";
  j+=",\"transport_enabled\":";j+=transportEnabled?"true":"false";
  j+=",\"boot_default\":\"direct\"";
  j+=",\"ssid\":\""+esc(ssid)+"\"";
  j+=",\"wifi\":";j+=connected?"true":"false";
  j+=",\"ip\":\""+ip+"\",\"ip_ok\":";j+=connected?"true":"false";
  j+=",\"rssi\":"+String(connected?WiFi.RSSI():0);
  j+=",\"dns\":";j+=dnsOk?"true":"false";
  j+=",\"dns_fail_streak\":"+String(dnsFailStreak);
  j+=",\"time\":";j+=timeOk?"true":"false";
  j+=",\"internet\":";j+=internet?"true":"false";
  j+=",\"tls\":";j+=cloudTls?"true":"false";
  j+=",\"mqtt\":";j+=cloudMqtt?"true":"false";
  j+=",\"command_ack\":";j+=cloudAck?"true":"false";
  j+=",\"telemetry\":";j+=cloudTelemetry?"true":"false";
  j+=",\"remote_ready\":";j+=ready?"true":"false";
  j+=",\"retry_hold\":";j+=retryHold?"true":"false";
  j+=",\"retry_delay_ms\":"+String(retryDelayMs);
  j+=",\"connect_attempts\":"+String(connectAttempts);
  j+=",\"ap_clients\":"+String(WiFi.softAPgetStationNum());
  j+=",\"wifi_channel\":"+String(WiFi.channel());
  j+=",\"detail\":\""+esc(cloudDetail)+"\"}";
  return j;
}

String remoteSsid(){return ssid;}
bool remoteInternetReady(){return transportEnabled&&configured&&WiFi.status()==WL_CONNECTED&&dnsOk&&timeOk;}
