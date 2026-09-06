#include "Alerts.h"
#include "Config.h"

namespace Alerts {
namespace {
struct Segment{uint16_t hz,ms;};

constexpr Segment P_TEST[]={{1800,180},{0,420}};
constexpr Segment P_LINK[]={{900,180},{0,120},{700,180},{0,1520}};
constexpr Segment P_IR[]={{1300,120},{0,1880}};
constexpr Segment P_GAS_WARNING[]={{1550,220},{0,780}};
constexpr Segment P_GAS_DANGER[]={{2200,180},{0,120},{2200,180},{0,120},{2200,180},{0,620}};
constexpr Segment P_ESTOP[]={{520,450},{0,180},{520,450},{0,920}};

bool irActive=false,linkActive=false;
uint8_t gasSeverity=0;
uint32_t gasUntil=0,estopUntil=0,testUntil=0,segmentAt=0;
uint8_t segmentIndex=0;
Level activeLevel=NONE;

bool alive(uint32_t now,uint32_t until){return until!=0&&(int32_t)(until-now)>0;}

const Segment* pattern(Level l,size_t& n){
  switch(l){
    case ESTOP:n=sizeof(P_ESTOP)/sizeof(P_ESTOP[0]);return P_ESTOP;
    case GAS_DANGER:n=sizeof(P_GAS_DANGER)/sizeof(P_GAS_DANGER[0]);return P_GAS_DANGER;
    case GAS_WARNING:n=sizeof(P_GAS_WARNING)/sizeof(P_GAS_WARNING[0]);return P_GAS_WARNING;
    case IR:n=sizeof(P_IR)/sizeof(P_IR[0]);return P_IR;
    case LINK:n=sizeof(P_LINK)/sizeof(P_LINK[0]);return P_LINK;
    case TEST:n=sizeof(P_TEST)/sizeof(P_TEST[0]);return P_TEST;
    default:n=0;return nullptr;
  }
}

Level requested(uint32_t now){
  if(alive(now,estopUntil))return ESTOP;
  if(gasSeverity>=2&&alive(now,gasUntil))return GAS_DANGER;
  if(gasSeverity==1&&alive(now,gasUntil))return GAS_WARNING;
  if(irActive)return IR;
  if(linkActive)return LINK;
  if(alive(now,testUntil))return TEST;
  return NONE;
}

void output(uint16_t hz){if(hz)tone(Cfg::ALERT_PIN,hz);else noTone(Cfg::ALERT_PIN);}
void restart(Level l,uint32_t now){noTone(Cfg::ALERT_PIN);activeLevel=l;segmentIndex=0;segmentAt=0;if(l==NONE)return;size_t n=0;const Segment* p=pattern(l,n);if(p&&n){output(p[0].hz);segmentAt=now;}}
}

void begin(){pinMode(Cfg::ALERT_PIN,OUTPUT);digitalWrite(Cfg::ALERT_PIN,LOW);noTone(Cfg::ALERT_PIN);}

void update(){
  uint32_t now=millis();
  if(gasSeverity&& !alive(now,gasUntil)){gasSeverity=0;gasUntil=0;}
  Level want=requested(now);if(want!=activeLevel)restart(want,now);if(activeLevel==NONE)return;
  size_t n=0;const Segment* p=pattern(activeLevel,n);if(!p||!n)return;
  if(now-segmentAt<p[segmentIndex].ms)return;
  segmentIndex=(uint8_t)((segmentIndex+1)%n);output(p[segmentIndex].hz);segmentAt=now;
}

void setIr(bool active){irActive=active;}
void setLink(bool active){linkActive=active;}
void setGas(uint8_t severity,uint32_t ttlMs){
  severity=severity>2?2:severity;if(!severity){gasSeverity=0;gasUntil=0;return;}
  gasSeverity=severity;uint32_t ttl=constrain(ttlMs,500UL,10000UL);gasUntil=millis()+ttl;
}
void emergencyStop(){estopUntil=millis()+2600UL;}
void test(){testUntil=millis()+650UL;}
void clearTransient(){gasSeverity=0;gasUntil=estopUntil=testUntil=0;}
Level level(){return activeLevel;}
const char* name(){
  switch(activeLevel){case TEST:return"test";case LINK:return"link";case IR:return"ir";case GAS_WARNING:return"gas_warning";case GAS_DANGER:return"gas_danger";case ESTOP:return"estop";default:return"none";}
}
String json(){String j="{\"gpio\":"+String(Cfg::ALERT_PIN)+",\"level\":"+String((uint8_t)activeLevel)+",\"name\":\""+String(name())+"\",\"active\":"+(activeLevel==NONE?"false":"true")+",\"gas\":"+String(gasSeverity)+",\"ir\":"+(irActive?"true":"false")+",\"link\":"+(linkActive?"true":"false")+"}";return j;}
}
