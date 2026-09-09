#include "Hardware.h"
#include "Alerts.h"
#include <Preferences.h>
#include <Wire.h>
#include <WiFi.h>
#include <Adafruit_ADS1X15.h>
#include <Adafruit_BME280.h>
#include <Adafruit_BMP280.h>
#include <math.h>

Settings settings;

namespace{
Preferences nv;

/* ---------- L298N DRIVE / LINK LOSS STATE MACHINE ---------- */
float tarL=0,tarR=0,curL=0,curR=0;
uint32_t cmdAt=0,driveAt=0;
bool fs=false,retracing=false;

float lim(float v){float m=constrain(settings.speed,20,100);return constrain(v,-m,m);}
void motorWrite(uint8_t in1,uint8_t in2,float value,bool invert){
  int cmd=constrain((int)roundf(value),-100,100);if(invert)cmd=-cmd;
  int duty=map(abs(cmd),0,100,0,255);
  // Always remove drive from the opposite leg before applying PWM. Zero is coast.
  if(cmd>0){analogWrite(in2,0);analogWrite(in1,duty);}
  else if(cmd<0){analogWrite(in1,0);analogWrite(in2,duty);}
  else{analogWrite(in1,0);analogWrite(in2,0);}
}
void driveWrite(){
  motorWrite(Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT);
  motorWrite(Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT);
}
float ramp(float c,float t,float dt){
  if(fabsf(t-c)<.1f)return t;
  bool rev=(c>0&&t<0)||(c<0&&t>0);
  float rate=rev?Cfg::REVERSE:(fabsf(t)<fabsf(c)?Cfg::DECEL:Cfg::ACCEL),d=rate*dt;
  if(rev)return c>0?max(0.0f,c-d):min(0.0f,c+d);
  return c<t?min(t,c+d):max(t,c-d);
}
constexpr uint16_t HIST_STEP=250,HIST_N=Cfg::RETRACE_MS/HIST_STEP;
struct Hist{int8_t l,r;};Hist hist[HIST_N];
uint16_t histHead=0,histCount=0,retrIdx=0,retrRemain=0;
uint32_t histAt=0,retrAt=0;
void clearHistory(){histHead=histCount=retrIdx=retrRemain=0;}
void historyUpdate(uint32_t now){if(retracing||fs||now-histAt<HIST_STEP)return;histAt=now;hist[histHead]={(int8_t)constrain((int)roundf(curL),-100,100),(int8_t)constrain((int)roundf(curR),-100,100)};histHead=(histHead+1)%HIST_N;if(histCount<HIST_N)histCount++;}
void zeroMotionPreserveHistory(){tarL=tarR=curL=curR=0;driveWrite();}
void beginRetrace(uint32_t now){
  zeroMotionPreserveHistory();fs=true;retracing=false;
  if(!histCount)return;
  uint16_t i=(histHead+HIST_N-1)%HIST_N,n=histCount;
  while(n&&!hist[i].l&&!hist[i].r){i=(i+HIST_N-1)%HIST_N;n--;}
  if(!n)return;
  retrIdx=i;retrRemain=n;retrAt=now;retracing=true;
}
void retraceUpdate(uint32_t now){
  if(!retracing||now-retrAt<HIST_STEP)return;
  retrAt=now;
  if(!retrRemain){tarL=tarR=curL=curR=0;driveWrite();retracing=false;return;}
  Hist h=hist[retrIdx];
  tarL=constrain(-(int)h.l,-Cfg::RETRACE_MAX_SPEED,Cfg::RETRACE_MAX_SPEED);
  tarR=constrain(-(int)h.r,-Cfg::RETRACE_MAX_SPEED,Cfg::RETRACE_MAX_SPEED);
  retrIdx=(retrIdx+HIST_N-1)%HIST_N;retrRemain--;
}
void driveUpdate(){
  uint32_t now=millis();
  // A stale non-zero drive command is never allowed to remain latched, even if
  // a separate heartbeat is still alive.
  if(!retracing&&!fs&&now-cmdAt>Cfg::DRIVE_LEASE_MS&&(fabsf(tarL)>.1f||fabsf(tarR)>.1f))tarL=tarR=0;
  retraceUpdate(now);historyUpdate(now);
  if(now-driveAt<10)return;
  float dt=min(now-driveAt,50UL)/1000.0f;driveAt=now;curL=ramp(curL,tarL,dt);curR=ramp(curR,tarR,dt);driveWrite();
}
void motionStopInternal(){tarL=tarR=curL=curR=0;retracing=false;fs=false;clearHistory();driveWrite();}

/* ---------- ONE MQ POWER RELAY ---------- */
bool mqPowerOn=false;
uint32_t mqPowerAt=0;
void relayRaw(bool on){if(on&&!mqPowerOn)mqPowerAt=millis();else if(!on)mqPowerAt=0;digitalWrite(Cfg::RELAY_MQ,on?Cfg::RELAY_ON:Cfg::RELAY_OFF);mqPowerOn=on;if(!on)Alerts::setGas(0);}
uint32_t mqPowerMs(){return mqPowerOn&&mqPowerAt?millis()-mqPowerAt:0;}

/* ---------- I2C / ADS1115 + MQ ---------- */
bool i2cProbe(uint8_t a){Wire.beginTransmission(a);return Wire.endTransmission()==0;}
Adafruit_ADS1115 ads1,ads2;
bool ads1ok=false,ads2ok=false;uint32_t adsHealthAt=0;
struct Reading{
  uint16_t raw=0;float adc=NAN,ao=NAN,rs=NAN,ratio=NAN;bool ok=false;
  uint32_t sampleUs=0,scanCycle=0,channelOffsetUs=0;
};
Reading mq[Cfg::MQ_N];uint32_t mqAt=0,mqScanCycle=0,mqCycleStartUs=0;uint8_t mqIndex=0;
Adafruit_ADS1115* getAds(uint8_t n){return n?&ads2:&ads1;}
bool adsOK(uint8_t n){return n?ads2ok:ads1ok;}
bool adsBegin(uint8_t n){Adafruit_ADS1115 *a=getAds(n);uint8_t addr=n?Cfg::ADS2_ADDR:Cfg::ADS1_ADDR;bool ok=a->begin(addr,&Wire);if(ok){a->setGain(GAIN_TWOTHIRDS);a->setDataRate(RATE_ADS1115_860SPS);}if(n)ads2ok=ok;else ads1ok=ok;return ok;}
void adsHealthUpdate(){uint32_t now=millis();if(now-adsHealthAt<Cfg::ADS_REPROBE_MS)return;adsHealthAt=now;bool p1=i2cProbe(Cfg::ADS1_ADDR),p2=i2cProbe(Cfg::ADS2_ADDR);if(!p1)ads1ok=false;else if(!ads1ok)adsBegin(0);if(!p2)ads2ok=false;else if(!ads2ok)adsBegin(1);}
void readMQ(uint8_t i){
  const auto &c=Cfg::mq[i];Reading &r=mq[i];
  if(!mqPowerOn||!adsOK(c.ads)){r=Reading();return;}
  Adafruit_ADS1115 *a=getAds(c.ads);int32_t sum=0;float volts=0;
  for(uint8_t n=0;n<Cfg::ADC_SAMPLES;n++){int16_t x=a->readADC_SingleEnded(c.ch);sum+=x;volts+=a->computeVolts(x);}
  uint32_t sampled=micros();int32_t av=sum/Cfg::ADC_SAMPLES;if(av<0)av=0;r.raw=(uint16_t)av;r.adc=volts/Cfg::ADC_SAMPLES;r.ao=r.adc;
  r.sampleUs=sampled;r.scanCycle=mqScanCycle;r.channelOffsetUs=sampled-mqCycleStartUs;
  if(r.ao<=.01f||r.ao>=Cfg::MQ_VCC){r.ok=false;return;}
  r.rs=Cfg::RL*(Cfg::MQ_VCC-r.ao)/r.ao;r.ratio=r.rs/c.r0;r.ok=isfinite(r.rs)&&r.rs>0;
}
void mqUpdate(){
  uint32_t now=millis();uint16_t slot=Cfg::MQ_PERIOD/Cfg::MQ_N;if(slot<2)slot=2;if(now-mqAt<slot)return;mqAt=now;
  if(mqIndex==0){mqScanCycle++;if(!mqScanCycle)mqScanCycle=1;mqCycleStartUs=micros();}
  readMQ(mqIndex);if(++mqIndex>=Cfg::MQ_N)mqIndex=0;
}

/* ---------- BME / BMP ---------- */
Adafruit_BME280 bme;Adafruit_BMP280 bmp(&Wire);
uint8_t envType=0,envAddr=0,envId=0;float temp=NAN,hum=NAN,press=NAN;uint32_t envAt=0;
uint8_t chip(uint8_t a){Wire.beginTransmission(a);Wire.write(0xD0);if(Wire.endTransmission(false))return 0;if(Wire.requestFrom((int)a,1)!=1)return 0;return Wire.read();}
void envRead(){if(envType==1){temp=bme.readTemperature();hum=bme.readHumidity();press=bme.readPressure()/100.0f;}else if(envType==2){temp=bmp.readTemperature();hum=NAN;press=bmp.readPressure()/100.0f;}}
void envBegin(){const uint8_t addr[]={0x76,0x77};for(uint8_t a:addr){if(!i2cProbe(a))continue;envAddr=a;envId=chip(a);if(envId==0x60&&bme.begin(a,&Wire)){envType=1;break;}if((envId==0x56||envId==0x57||envId==0x58)&&bmp.begin(a,envId)){envType=2;break;}}envRead();}
void envUpdate(){if(!envType||millis()-envAt<Cfg::ENV_PERIOD)return;envAt=millis();envRead();}

/* ---------- SOUND / MAX4466 ---------- */
float soundRms=NAN,soundP2P=NAN;uint32_t soundAt=0;
void soundUpdate(){uint32_t now=millis();if(now-soundAt<Cfg::SOUND_PERIOD)return;soundAt=now;float mean=0,m2=0;uint16_t lo=4095,hi=0;for(uint16_t n=1;n<=Cfg::SOUND_SAMPLES;n++){uint16_t x=analogRead(Cfg::SOUND_PIN);if(x<lo)lo=x;if(x>hi)hi=x;float d=x-mean;mean+=d/n;m2+=d*(x-mean);}const float V=3.3f/4095.0f;soundRms=sqrtf(m2/Cfg::SOUND_SAMPLES)*V;soundP2P=(hi-lo)*V;}

/* ---------- IR ---------- */
bool irWarning=false;uint8_t irLevel=HIGH;uint32_t irAt=0;
void irUpdate(){uint32_t now=millis();if(now-irAt<Cfg::IR_PERIOD)return;irAt=now;irLevel=digitalRead(Cfg::IR_PIN);irWarning=irLevel==Cfg::IR_ACTIVE;Alerts::setIr(irWarning);}

/* ---------- LDR / LM393 DIGITAL THRESHOLD ---------- */
uint8_t ldrLevel=HIGH;uint32_t ldrAt=0;
void ldrUpdate(){uint32_t now=millis();if(now-ldrAt<Cfg::LDR_PERIOD)return;ldrAt=now;ldrLevel=digitalRead(Cfg::LDR_PIN);}

/* ---------- MEASUREMENT ---------- */
enum Stage:uint8_t{IDLE,WARM,MEASURE,READY,ERROR};Stage stage=IDLE;
bool measurePower=false;uint32_t stageAt=0,sampleAt=0;
uint32_t rawSum[Cfg::MQ_N]={};float adcSum[Cfg::MQ_N]={},aoSum[Cfg::MQ_N]={},rsSum[Cfg::MQ_N]={},logSum[Cfg::MQ_N]={};uint16_t cnt[Cfg::MQ_N]={},rcnt[Cfg::MQ_N]={};
void clearSums(){memset(rawSum,0,sizeof(rawSum));memset(adcSum,0,sizeof(adcSum));memset(aoSum,0,sizeof(aoSum));memset(rsSum,0,sizeof(rsSum));memset(logSum,0,sizeof(logSum));memset(cnt,0,sizeof(cnt));memset(rcnt,0,sizeof(rcnt));}
void resetMeasurement(){clearSums();measurePower=false;stageAt=sampleAt=0;stage=IDLE;}
void collect(){if(!measurePower)return;for(uint8_t i=0;i<Cfg::MQ_N;i++){if(!mq[i].ok)continue;rawSum[i]+=mq[i].raw;adcSum[i]+=mq[i].adc;aoSum[i]+=mq[i].ao;rsSum[i]+=mq[i].rs;cnt[i]++;if(mq[i].ratio>0&&isfinite(mq[i].ratio)){logSum[i]+=logf(mq[i].ratio);rcnt[i]++;}}}
void finishMeasure(){bool ok=false;for(uint8_t i=0;i<Cfg::MQ_N;i++)if(cnt[i]){ok=true;break;}stage=ok?READY:ERROR;}
void measureUpdate(){if(stage!=MEASURE)return;if(mqPowerOn!=measurePower){resetMeasurement();return;}uint32_t now=millis();if(now-sampleAt>=Cfg::SAMPLE_PERIOD){sampleAt=now;collect();}if(now-stageAt>=(uint32_t)settings.measure*1000UL)finishMeasure();}

/* ---------- LINK WATCHDOG: STOP -> 3 s HOLD -> MQ OFF + RETRACE ---------- */
bool linkArmed=false,linkLost=false,linkHold=false;uint32_t linkAt=0,holdAt=0;
void recoverLink(uint32_t now){
  if(linkHold||linkLost||retracing){zeroMotionPreserveHistory();retracing=false;fs=false;linkHold=false;linkLost=false;clearHistory();Alerts::setLink(false);}
  linkAt=now;
}
void beginLinkHold(uint32_t now){
  if(linkHold||retracing)return;
  zeroMotionPreserveHistory();fs=true;linkHold=true;linkLost=true;holdAt=now;Alerts::setLink(true);
}
void linkWatchUpdate(){
  if(!linkArmed)return;
  uint32_t now=millis();
  if(retracing)return;
  if(!linkHold){if(now-linkAt>Cfg::LINK_DETECT_MS)beginLinkHold(now);return;}
  if(now-linkAt<=Cfg::LINK_DETECT_MS){recoverLink(now);return;}
  if(now-holdAt<Cfg::LINK_HOLD_MS)return;
  // The recovery window expired. Sensor power is removed before autonomous retrace.
  linkHold=false;resetMeasurement();relayRaw(false);beginRetrace(now);
}

/* ---------- SETTINGS / JSON ---------- */
String num(float v,uint8_t d=2){return isfinite(v)?String(v,(unsigned)d):"null";}
const char* boolJson(bool v){return v?"true":"false";}
void loadSettings(){nv.begin(Cfg::NVS,false);settings.cam=nv.getString("camIP",Cfg::CAM);settings.speed=constrain(nv.getInt("speed",100),20,100);settings.warm=constrain(nv.getUInt("warmup",Cfg::OPERATIONAL_WARMUP_SEC),0UL,3600UL);settings.measure=constrain(nv.getUInt("measure",Cfg::ANALYSIS_WINDOW_SEC),5UL,600UL);}
String envJson(){String j="{\"ok\":"+String(boolJson(envType!=0))+",\"t\":"+num(temp,2)+",\"h\":"+num(hum,2)+",\"p\":"+num(press,1)+"}";return j;}
String acquisitionJson(){
  return "{\"model\":\"ADS1115\",\"rate_sps\":"+String(Cfg::ADS_RATE_SPS)+",\"gain\":\"GAIN_TWOTHIRDS\",\"full_scale_v\":"+num(Cfg::ADS_FULL_SCALE_V,3)+",\"supply_v\":"+num(Cfg::ADS_SUPPLY_V,3)+",\"conversions_per_channel\":"+String(Cfg::ADC_SAMPLES)+",\"scan_period_ms\":"+String(Cfg::MQ_PERIOD)+",\"channels\":"+String(Cfg::MQ_N)+",\"sequential\":true}";
}
String mqJson(bool avg){
  bool powered=avg?measurePower:mqPowerOn;String j="{";uint32_t nowUs=micros();
  for(uint8_t i=0;i<Cfg::MQ_N;i++){
    if(i)j+=",";const auto &c=Cfg::mq[i];j+="\"";j+=c.name;j+="\":{";
    if(avg){if(!cnt[i])j+="\"ok\":false";else{float n=cnt[i];j+="\"ok\":true";j+=",\"raw\":"+num(rawSum[i]/n,1)+",\"adc\":"+num(adcSum[i]/n,3)+",\"ao\":"+num(aoSum[i]/n,3)+",\"rs\":"+num(rsSum[i]/n,3)+",\"ratio\":";j+=rcnt[i]?num(expf(logSum[i]/rcnt[i]),3):"null";}}
    else{
      j+="\"ok\":"+String(boolJson(mq[i].ok))+",\"raw\":"+String(mq[i].raw)+",\"adc\":"+num(mq[i].adc,3)+",\"ao\":"+num(mq[i].ao,3)+",\"rs\":"+num(mq[i].rs,3)+",\"ratio\":"+num(mq[i].ratio,3);
      if(mq[i].sampleUs){j+=",\"sample_us\":"+String(mq[i].sampleUs)+",\"scan_cycle\":"+String(mq[i].scanCycle)+",\"channel_offset_us\":"+String(mq[i].channelOffsetUs)+",\"age_us\":"+String(nowUs-mq[i].sampleUs);}
    }
    j+=",\"powered\":"+String(boolJson(powered))+",\"relay\":1,\"row\":1,\"ads\":"+String(c.ads)+",\"ch\":"+String(c.ch)+",\"r0\":"+num(c.r0,3)+",\"r0_calibrated\":false,\"r0_source\":\"firmware_default\"}";
  }
  return j+"}";
}
String driveJson(){
  return "{\"driver\":\"L298N\",\"left\":"+String((int)roundf(curL))+",\"right\":"+String((int)roundf(curR))+",\"failsafe\":"+String(boolJson(fs))+",\"retrace\":"+String(boolJson(retracing))+",\"hold\":"+String(boolJson(linkHold))+",\"history\":"+String(histCount)+",\"pins\":{\"l_in1\":"+String(Cfg::DRIVE_L_IN1)+",\"l_in2\":"+String(Cfg::DRIVE_L_IN2)+",\"r_in1\":"+String(Cfg::DRIVE_R_IN1)+",\"r_in2\":"+String(Cfg::DRIVE_R_IN2)+"}}";
}
const char* linkState(){return retracing?"retrace":linkHold?"hold":linkLost?"lost":"ok";}
uint32_t holdRemaining(){if(!linkHold)return 0;uint32_t elapsed=millis()-holdAt;return elapsed>=Cfg::LINK_HOLD_MS?0:Cfg::LINK_HOLD_MS-elapsed;}
} // namespace

/* ================= PUBLIC ================= */
void hwBegin(){
  loadSettings();Alerts::begin();pinMode(Cfg::RELAY_MQ,OUTPUT);relayRaw(false);
  pinMode(Cfg::IR_PIN,INPUT);pinMode(Cfg::LDR_PIN,INPUT);pinMode(Cfg::SOUND_PIN,INPUT);analogReadResolution(12);analogSetPinAttenuation(Cfg::SOUND_PIN,ADC_11db);
  pinMode(Cfg::DRIVE_L_IN1,OUTPUT);pinMode(Cfg::DRIVE_L_IN2,OUTPUT);pinMode(Cfg::DRIVE_R_IN1,OUTPUT);pinMode(Cfg::DRIVE_R_IN2,OUTPUT);
  analogWrite(Cfg::DRIVE_L_IN1,0);analogWrite(Cfg::DRIVE_L_IN2,0);analogWrite(Cfg::DRIVE_R_IN1,0);analogWrite(Cfg::DRIVE_R_IN2,0);
  cmdAt=driveAt=histAt=millis();driveWrite();
  Wire.begin(Cfg::SDA,Cfg::SCL);adsBegin(0);adsBegin(1);adsHealthAt=millis();envBegin();
}
void hwUpdate(){driveUpdate();adsHealthUpdate();mqUpdate();envUpdate();soundUpdate();irUpdate();ldrUpdate();measureUpdate();linkWatchUpdate();Alerts::update();}
void driveSet(int l,int r){
  // linkHeartbeat() is invoked by both Direct HTTP and Remote MQTT before driveSet().
  if(retracing||linkHold||linkLost)recoverLink(millis());
  fs=false;tarL=lim(l);tarR=lim(r);cmdAt=millis();
}
void motionStop(){motionStopInternal();}
void emergencyStop(){motionStopInternal();resetMeasurement();relayRaw(false);Alerts::emergencyStop();}
bool relaySetMask(uint8_t on){if(on>1)return false;if(stage==MEASURE||stage==WARM)resetMeasurement();relayRaw(on==1);return true;}
void sensorPowerOff(){resetMeasurement();relayRaw(false);}
void measureStart(){clearSums();measurePower=mqPowerOn;stageAt=sampleAt=millis();stage=measurePower?MEASURE:ERROR;}
void measureCancel(){resetMeasurement();Alerts::setGas(0);}
bool alertSet(uint8_t severity,uint32_t ttlMs){if(severity>2)return false;Alerts::setGas(severity,ttlMs);return true;}
void alertTest(){Alerts::test();}
void linkHeartbeat(){uint32_t now=millis();linkArmed=true;if(linkHold||linkLost||retracing)recoverLink(now);else linkAt=now;}
void linkSetArmed(bool armed){linkArmed=armed;linkHold=false;linkLost=false;Alerts::setLink(false);if(!armed){fs=false;linkAt=holdAt=0;}else linkAt=millis();}
void saveSettings(){nv.putString("camIP",settings.cam);nv.putInt("speed",settings.speed);nv.putUInt("warmup",settings.warm);nv.putUInt("measure",settings.measure);}
String settingsJson(){return "{\"cam\":\""+settings.cam+"\",\"speed\":"+String(settings.speed)+",\"warm\":"+String(settings.warm)+",\"measure\":"+String(settings.measure)+",\"drive_driver\":\"L298N\",\"warm_manual\":true,\"mq_power_manual\":true}";}
String dataJson(){
  String j="{\"env\":"+envJson();
  j+=",\"sound\":{\"rms\":"+num(soundRms,3)+",\"p2p\":"+num(soundP2P,3)+"}";
  j+=",\"ir\":{\"warning\":"+String(boolJson(irWarning))+",\"level\":"+String(irLevel)+"}";
  j+=",\"ldr\":{\"installed\":true,\"gpio\":"+String(Cfg::LDR_PIN)+",\"level\":"+String(ldrLevel)+",\"quantitative_lux\":false}";
  j+=",\"alert\":"+Alerts::json();
  j+=",\"relay\":{\"mask\":"+String(mqPowerOn?1:0)+",\"on\":"+String(boolJson(mqPowerOn))+",\"gpio\":"+String(Cfg::RELAY_MQ)+",\"power_ms\":"+String(mqPowerMs())+",\"channels\":1}";
  j+=",\"adc\":{\"ads1\":"+String(boolJson(ads1ok))+",\"ads2\":"+String(boolJson(ads2ok))+",\"acquisition\":"+acquisitionJson()+"}";
  j+=",\"drive\":"+driveJson();
  j+=",\"mq\":"+mqJson(false);
  j+=",\"measurement\":{\"state\":"+String((uint8_t)stage)+",\"mask\":"+String(measurePower?1:0)+",\"power\":"+String(boolJson(measurePower))+",\"manual_power\":true,\"manual_warmup\":true,\"duration_s\":"+String(settings.measure)+",\"mq\":"+mqJson(true)+"}}";
  return j;
}
String diagJson(){
  String j="{\"fw\":\""+String(Cfg::FW)+"\",\"api\":"+String(Cfg::API)+",\"hardware_profile\":\""+String(Cfg::HW_PROFILE)+"\"";
  j+=",\"heap\":"+String(ESP.getFreeHeap())+",\"clients\":"+String(WiFi.softAPgetStationNum());
  j+=",\"failsafe\":"+String(boolJson(fs))+",\"retrace\":"+String(boolJson(retracing));
  j+=",\"drive\":"+driveJson();
  j+=",\"relay\":{\"mask\":"+String(mqPowerOn?1:0)+",\"on\":"+String(boolJson(mqPowerOn))+",\"gpio\":"+String(Cfg::RELAY_MQ)+",\"power_ms\":"+String(mqPowerMs())+",\"channels\":1}";
  j+=",\"adc\":{\"ads1\":"+String(boolJson(ads1ok))+",\"ads2\":"+String(boolJson(ads2ok))+",\"acquisition\":"+acquisitionJson()+"}";
  j+=",\"ads1\":"+String(boolJson(ads1ok))+",\"ads2\":"+String(boolJson(ads2ok));
  j+=",\"link\":{\"armed\":"+String(boolJson(linkArmed))+",\"lost\":"+String(boolJson(linkLost))+",\"hold\":"+String(boolJson(linkHold))+",\"state\":\""+String(linkState())+"\",\"hold_remaining_ms\":"+String(holdRemaining())+",\"heartbeat_age_ms\":"+String(linkArmed?millis()-linkAt:0)+"}";
  j+=",\"envType\":"+String(envType)+",\"envAddr\":"+String(envAddr)+",\"envId\":"+String(envId);
  j+=",\"ldr\":{\"installed\":true,\"gpio\":"+String(Cfg::LDR_PIN)+",\"level\":"+String(ldrLevel)+"}";
  j+=",\"alert\":"+Alerts::json();
  j+=",\"ir\":{\"warning\":"+String(boolJson(irWarning))+",\"level\":"+String(irLevel)+"},\"mqPowerManual\":true,\"warmupManual\":true}";
  return j;
}
