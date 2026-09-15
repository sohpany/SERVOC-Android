#include <WiFi.h>
#include <WiFiUdp.h>
#include "Hardware.h"
#include "RemoteLink.h"
#include "RemoteMqtt.h"
#include "SafetyInterlock.h"

WiFiServer web(80);WiFiUDP driveUdp;uint32_t wifiAt=0;bool naptEnabled=false;
constexpr uint16_t DRIVE_UDP_PORT=4210;constexpr uint8_t DRIVE_UDP_VERSION=1;
uint32_t driveUdpPackets=0,driveUdpRejected=0,driveUdpAt=0;uint16_t driveUdpSeq=0;bool driveUdpSeqValid=false,driveUdpPeerValid=false;IPAddress driveUdpPeerIp;uint16_t driveUdpPeerPort=0;
void driveUdpUpdate();
uint8_t hx(char c){return c>='0'&&c<='9'?c-'0':c>='A'&&c<='F'?c-'A'+10:c>='a'&&c<='f'?c-'a'+10:0;}
String dec(const String&s){String o;o.reserve(s.length());for(size_t i=0;i<s.length();i++){char c=s[i];if(c=='+')o+=' ';else if(c=='%'&&i+2<s.length()){o+=(char)((hx(s[i+1])<<4)|hx(s[i+2]));i+=2;}else o+=c;}return o;}
String esc(const String&s){String o;o.reserve(s.length()+4);for(char c:s){if(c=='\\'||c=='\"')o+='\\';if((uint8_t)c>=0x20)o+=c;}return o;}
String arg(const String&q,const String&b,const char*k){String key=String(k)+"=";auto scan=[&](const String&s)->String{int p=0;while(p<(int)s.length()){int e=s.indexOf('&',p);if(e<0)e=s.length();if(s.substring(p,p+key.length())==key)return dec(s.substring(p+key.length(),e));p=e+1;}return "";};String v=scan(q);return v.length()?v:scan(b);}
void send(WiFiClient&c,int code,const char*type,const String&body){const char*msg=code==200?"OK":code==400?"Bad Request":code==404?"Not Found":"Conflict";c.print("HTTP/1.1 ");c.print(code);c.print(' ');c.print(msg);c.print("\r\nContent-Type: ");c.print(type);c.print("\r\nContent-Length: ");c.print(body.length());c.print("\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n");c.print(body);}
String scanJson(){
  constexpr uint32_t SCAN_LIMIT_MS=2000;uint32_t started=millis();WiFi.scanDelete();
  int n=WiFi.scanNetworks(true,true,false,120);while(n==WIFI_SCAN_RUNNING&&millis()-started<SCAN_LIMIT_MS){driveUdpUpdate();hwUpdate();delay(5);n=WiFi.scanComplete();}
  bool timed=n==WIFI_SCAN_RUNNING;if(timed||n<0)n=0;uint32_t elapsed=min(millis()-started,SCAN_LIMIT_MS);
  String j="{\"duration_ms\":"+String(elapsed)+",\"timed_out\":"+(timed?"true":"false")+",\"networks\":[";
  for(int i=0;i<n;i++){if(i)j+=',';j+="{\"ssid\":\""+esc(WiFi.SSID(i))+"\",\"rssi\":"+String(WiFi.RSSI(i))+",\"secure\":"+(WiFi.encryptionType(i)==WIFI_AUTH_OPEN?"false":"true")+",\"channel\":"+String(WiFi.channel(i))+"}";}
  WiFi.scanDelete();return j+"]}";
}
void startAP(){WiFi.persistent(false);WiFi.mode(WIFI_AP_STA);WiFi.setSleep(false);IPAddress ip(192,168,4,1),mask(255,255,255,0),dhcp(192,168,4,100);WiFi.softAPConfig(ip,ip,mask,dhcp);WiFi.softAP(Cfg::SSID);naptEnabled=false;}
void gatewayUpdate(){bool w=remoteInternetReady();if(w!=naptEnabled&&WiFi.AP.enableNAPT(w))naptEnabled=w;}
void wifiWatch(){if(millis()-wifiAt<5000)return;wifiAt=millis();if(!(WiFi.getMode()&WIFI_AP)||WiFi.softAPIP()!=IPAddress(192,168,4,1)){if(naptEnabled)WiFi.AP.enableNAPT(false);WiFi.softAPdisconnect(true);startAP();}}

bool driveUdpNewer(uint16_t seq,uint32_t now,const IPAddress&ip,uint16_t port){
  if(!driveUdpSeqValid||!driveUdpPeerValid||!(ip==driveUdpPeerIp)||port!=driveUdpPeerPort||now-driveUdpAt>1000)return true;
  return (int16_t)(seq-driveUdpSeq)>0;
}
void driveUdpUpdate(){
  int n=0;while((n=driveUdp.parsePacket())>0){
    IPAddress peerIp=driveUdp.remoteIP();uint16_t peerPort=driveUdp.remotePort();
    uint8_t b[8];int got=0;while(driveUdp.available()&&got<8)b[got++]=(uint8_t)driveUdp.read();while(driveUdp.available())driveUdp.read();
    if(n!=8||got!=8){driveUdpRejected++;continue;}
    uint8_t check=0;for(uint8_t i=0;i<7;i++)check^=b[i];
    if(b[0]!='H'||b[1]!='R'||b[2]!=DRIVE_UDP_VERSION||check!=b[7]){driveUdpRejected++;continue;}
    uint16_t seq=(uint16_t)b[3]|((uint16_t)b[4]<<8);int l=(int8_t)b[5],r=(int8_t)b[6];uint32_t now=millis();
    if(l<-100||l>100||r<-100||r>100||!driveUdpNewer(seq,now,peerIp,peerPort)||safetyEstopLatched()){driveUdpRejected++;continue;}
    linkHeartbeat();driveSet(l,r);driveUdpSeq=seq;driveUdpSeqValid=true;driveUdpPeerValid=true;driveUdpPeerIp=peerIp;driveUdpPeerPort=peerPort;driveUdpAt=now;driveUdpPackets++;
  }
}

void httpUpdate(){WiFiClient c=web.accept();if(!c)return;c.setTimeout(80);String line=c.readStringUntil('\n');line.trim();int a=line.indexOf(' '),z=line.indexOf(' ',a+1);if(a<1||z<a){c.stop();return;}String method=line.substring(0,a),target=line.substring(a+1,z),body;int qi=target.indexOf('?');String path=qi<0?target:target.substring(0,qi),q=qi<0?"":target.substring(qi+1);int len=0;while(c.connected()){line=c.readStringUntil('\n');line.trim();if(!line.length())break;if(line.startsWith("Content-Length:"))len=line.substring(15).toInt();}if(len>0&&len<2048){uint32_t until=millis()+150;while((int)body.length()<len&&c.connected()&&(int32_t)(until-millis())>0){while(c.available()&&(int)body.length()<len)body+=(char)c.read();driveUdpUpdate();delay(1);}}auto A=[&](const char*k){return arg(q,body,k);};
if(path=="/")send(c,200,"text/plain",Cfg::FW);else if(path=="/drive"){String l=A("l"),r=A("r");if(!l.length()||!r.length())send(c,400,"text/plain","BAD");else if(safetyEstopLatched())send(c,409,"text/plain","ESTOP LATCHED");else{linkHeartbeat();driveSet(constrain(l.toInt(),-100L,100L),constrain(r.toInt(),-100L,100L));send(c,200,"text/plain","OK");}}else if(path=="/stop"||path=="/motion/stop"){motionStop();send(c,200,"text/plain","OK");}else if(path=="/estop"){safetyLatchEstop();emergencyStop();send(c,200,"text/plain","ESTOP LATCHED");}else if(path=="/estop/reset"){motionStop();safetyResetEstop();send(c,200,"text/plain","ESTOP RESET");}else if(path=="/sensor/off"){sensorPowerOff();send(c,200,"text/plain","OK");}else if(path=="/heartbeat"){if(A("arm")=="0"){linkSetArmed(false);send(c,200,"text/plain","DISARMED");}else{linkHeartbeat();send(c,200,"text/plain","OK");}}else if(path=="/remote/status"){String j=remoteStatusJson();if(j.endsWith("}")){j.remove(j.length()-1);j+=",\"napt\":";j+=naptEnabled?"true":"false";j+=",\"estop_latched\":";j+=safetyEstopLatched()?"true":"false";j+='}';}send(c,200,"application/json",j);}else if(path=="/remote/scan")send(c,200,"application/json",scanJson());else if(path=="/remote/provision"&&method=="POST"){String s=A("ssid"),p=A("password");bool ok=s.length()&&remoteProvision(s,p);send(c,ok?200:400,"application/json",ok?"{\"ok\":true}":"{\"ok\":false}");}else if(path=="/remote/clear"&&method=="POST"){if(A("confirm")!="1")send(c,400,"application/json","{\"ok\":false}");else{remoteMqttDisconnect();if(naptEnabled)WiFi.AP.enableNAPT(false);naptEnabled=false;remoteClear();send(c,200,"application/json","{\"ok\":true}");}}else if(path=="/relay"){String v=A("mask");if(v!="0"&&v!="1")send(c,409,"text/plain","MQ POWER MUST BE 0 OR 1");else{linkHeartbeat();send(c,relaySetMask(v=="1")?200:409,"text/plain","OK");}}else if(path=="/measure"){String x=A("cmd");if(x=="start"){linkHeartbeat();bool ok=measureStart();send(c,ok?200:409,"text/plain",ok?"OK":"MEASURE PRECHECK FAILED");}else if(x=="cancel"){measureCancel();send(c,200,"text/plain","OK");}else send(c,400,"text/plain","BAD");}else if(path=="/alert"){if(A("cmd")=="test"){alertTest();send(c,200,"text/plain","OK");}else{String sv=A("severity");long s=sv.toInt();if(!sv.length()||s<0||s>2)send(c,409,"text/plain","BAD");else{String tv=A("ttl");uint32_t ttl=tv.length()?constrain(tv.toInt(),500L,10000L):3000;send(c,alertSet((uint8_t)s,ttl)?200:409,"text/plain","OK");}}}else if(path=="/data")send(c,200,"application/json",dataJson());else if(path=="/diag"){String j=diagJson();if(j.endsWith("}")){j.remove(j.length()-1);j+=",\"estop_latched\":";j+=safetyEstopLatched()?"true":"false";j+=",\"drive_udp\":{\"installed\":true,\"port\":"+String(DRIVE_UDP_PORT)+",\"version\":"+String(DRIVE_UDP_VERSION)+",\"packets\":"+String(driveUdpPackets)+",\"rejected\":"+String(driveUdpRejected)+",\"last_age_ms\":"+String(driveUdpAt?millis()-driveUdpAt:0)+"}";j+='}';}send(c,200,"application/json",j);}else if(path=="/cfg"){SettingsPatch patch;String v;if((v=A("cam")).length()){patch.hasCam=true;patch.cam=v;}if((v=A("speed")).length()){patch.hasSpeed=true;patch.speed=v.toInt();}if((v=A("warm")).length()){patch.hasWarm=true;patch.warm=v.toInt();}if((v=A("measure")).length()){patch.hasMeasure=true;patch.measure=v.toInt();}if(q.length()||body.length())applySettings(patch,true);send(c,200,"application/json",settingsJson());}else send(c,404,"text/plain","NOT FOUND");c.stop();}
void setup(){relayBootSafeInit();hwBegin();startAP();remoteBegin();remoteMqttBegin();driveUdp.begin(DRIVE_UDP_PORT);web.begin();}
void loop(){driveUdpUpdate();hwUpdate();driveUdpUpdate();remoteUpdate();gatewayUpdate();remoteMqttUpdate();httpUpdate();driveUdpUpdate();wifiWatch();delay(1);}
