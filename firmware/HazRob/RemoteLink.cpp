#include "RemoteLink.h"
#include "Config.h"
#include <WiFi.h>
#include <Preferences.h>
#include <time.h>

namespace{
Preferences wan;
String ssid,password;
bool configured=false,dnsOk=false,timeOk=false;
bool cloudTls=false,cloudMqtt=false,cloudAck=false,cloudTelemetry=false,cloudCamera=false;
uint32_t connectAt=0,healthAt=0;
constexpr uint32_t RETRY_MS=7000;
constexpr uint32_t HEALTH_MS=5000;

void startSta(){if(!configured||ssid.isEmpty())return;WiFi.disconnect(false,false);delay(20);WiFi.begin(ssid.c_str(),password.c_str());connectAt=millis();dnsOk=timeOk=false;}
void internetHealth(){if(WiFi.status()!=WL_CONNECTED){dnsOk=timeOk=false;return;}IPAddress ip;dnsOk=WiFi.hostByName("pool.ntp.org",ip)==1;if(dnsOk){configTime(0,0,"pool.ntp.org","time.cloudflare.com");time_t now=time(nullptr);timeOk=now>1700000000;}else timeOk=false;}
String esc(const String& s){String o;o.reserve(s.length()+8);for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='\\'||c=='\"'){o+='\\';o+=c;}else if((uint8_t)c>=0x20)o+=c;}return o;}
}

void remoteBegin(){wan.begin("hazwan",false);ssid=wan.getString("ssid","");password=wan.getString("pass","");configured=!ssid.isEmpty();WiFi.mode(WIFI_AP_STA);WiFi.setSleep(false);if(configured)startSta();}
void remoteUpdate(){uint32_t now=millis();if(configured&&WiFi.status()!=WL_CONNECTED&&now-connectAt>=RETRY_MS)startSta();if(now-healthAt>=HEALTH_MS){healthAt=now;internetHealth();}}

bool remoteProvision(const String& newSsid,const String& newPassword){String s=newSsid;s.trim();if(s.isEmpty()||s.length()>32||newPassword.length()>63)return false;ssid=s;password=newPassword;configured=true;wan.putString("ssid",ssid);wan.putString("pass",password);startSta();return true;}
void remoteClear(){configured=false;ssid="";password="";dnsOk=timeOk=false;cloudTls=cloudMqtt=cloudAck=cloudTelemetry=cloudCamera=false;wan.remove("ssid");wan.remove("pass");WiFi.disconnect(false,true);}
void remoteSetCloudState(bool tls,bool mqtt,bool commandAck,bool telemetry,bool camera){cloudTls=tls;cloudMqtt=mqtt;cloudAck=commandAck;cloudTelemetry=telemetry;cloudCamera=camera;}

String remoteStatusJson(){
  bool connected=WiFi.status()==WL_CONNECTED;String ip=connected?WiFi.localIP().toString():"0.0.0.0";bool internet=connected&&dnsOk&&timeOk;bool ready=internet&&cloudTls&&cloudMqtt&&cloudAck&&cloudTelemetry;
  String j="{\"configured\":";j+=configured?"true":"false";j+=",\"ssid\":\""+esc(ssid)+"\"";j+=",\"wifi\":";j+=connected?"true":"false";j+=",\"ip\":\""+ip+"\",\"ip_ok\":";j+=connected?"true":"false";j+=",\"rssi\":"+String(connected?WiFi.RSSI():0);j+=",\"dns\":";j+=dnsOk?"true":"false";j+=",\"time\":";j+=timeOk?"true":"false";j+=",\"internet\":";j+=internet?"true":"false";j+=",\"tls\":";j+=cloudTls?"true":"false";j+=",\"mqtt\":";j+=cloudMqtt?"true":"false";j+=",\"command_ack\":";j+=cloudAck?"true":"false";j+=",\"telemetry\":";j+=cloudTelemetry?"true":"false";j+=",\"camera\":";j+=cloudCamera?"true":"false";j+=",\"remote_ready\":";j+=ready?"true":"false";j+="}";return j;
}
String remoteSsid(){return ssid;}
bool remoteInternetReady(){return configured&&WiFi.status()==WL_CONNECTED&&dnsOk&&timeOk;}
