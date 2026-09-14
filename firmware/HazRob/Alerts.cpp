#include "Alerts.h"
#include "Config.h"

namespace Alerts{
namespace{
bool irActive=false,linkActive=false;uint8_t gasSeverity=0;uint32_t gasUntil=0,estopUntil=0,testUntil=0;Level activeLevel=NONE;
bool alive(uint32_t n,uint32_t u){return u&&(int32_t)(u-n)>0;}
Level requested(uint32_t n){if(alive(n,estopUntil))return ESTOP;if(gasSeverity>=2&&alive(n,gasUntil))return GAS_DANGER;if(gasSeverity==1&&alive(n,gasUntil))return GAS_WARNING;if(irActive)return IR;if(linkActive)return LINK;if(alive(n,testUntil))return TEST;return NONE;}
void writeOutput(){digitalWrite(Cfg::ALERT_PIN,(Cfg::ALERT_OUTPUT_INSTALLED&&activeLevel!=NONE)?Cfg::ALERT_ACTIVE:Cfg::ALERT_INACTIVE);}
}
void begin(){pinMode(Cfg::ALERT_PIN,OUTPUT);digitalWrite(Cfg::ALERT_PIN,Cfg::ALERT_INACTIVE);}
void update(){uint32_t n=millis();if(gasSeverity&&!alive(n,gasUntil)){gasSeverity=0;gasUntil=0;}activeLevel=requested(n);writeOutput();}
void setIr(bool a){irActive=a;}void setLink(bool a){linkActive=a;}
void setGas(uint8_t s,uint32_t ttl){s=s>2?2:s;if(!s){gasSeverity=0;gasUntil=0;return;}gasSeverity=s;gasUntil=millis()+constrain(ttl,500UL,10000UL);}
void emergencyStop(){estopUntil=millis()+2600UL;}void test(){testUntil=millis()+650UL;}
void clearTransient(){gasSeverity=0;gasUntil=estopUntil=testUntil=0;activeLevel=NONE;writeOutput();}Level level(){return activeLevel;}
const char* name(){switch(activeLevel){case TEST:return"test";case LINK:return"link";case IR:return"ir";case GAS_WARNING:return"gas_warning";case GAS_DANGER:return"gas_danger";case ESTOP:return"estop";default:return"none";}}
String json(){return "{\"gpio\":"+String(Cfg::ALERT_PIN)+",\"installed\":"+(Cfg::ALERT_OUTPUT_INSTALLED?"true":"false")+",\"level\":"+String((uint8_t)activeLevel)+",\"name\":\""+String(name())+"\",\"active\":"+(activeLevel==NONE?"false":"true")+",\"output_asserted\":"+((Cfg::ALERT_OUTPUT_INSTALLED&&activeLevel!=NONE)?"true":"false")+",\"gas\":"+String(gasSeverity)+",\"ir\":"+(irActive?"true":"false")+",\"link\":"+(linkActive?"true":"false")+"}";}
}
