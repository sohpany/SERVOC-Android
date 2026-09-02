#include "Hardware.h"
#include <Preferences.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <WiFi.h>
#include <Adafruit_ADS1X15.h>
#include <Adafruit_BME280.h>
#include <Adafruit_BMP280.h>
#include <math.h>

Settings settings;

namespace{

Preferences nv;

/* DRIVE */
Servo sL,sR;
float tarL=0,tarR=0,curL=0,curR=0;
uint32_t cmdAt=0,driveAt=0;
bool fs=false,retracing=false,lossHandled=false;

float lim(float v){
  float m=constrain(settings.speed,20,100);
  return constrain(v,-m,m);
}

int pulse(float v,int n,int dir){
  v=constrain(v*dir,-100.0f,100.0f);
  return v>=0?n+(2000-n)*v/100:n+(n-1000)*v/100;
}

void driveWrite(){
  sL.writeMicroseconds(pulse(curL,settings.nL,1));
  sR.writeMicroseconds(pulse(curR,settings.nR,-1));
}

float ramp(float c,float t,float dt){
  if(fabsf(t-c)<.1f)return t;
  bool rev=(c>0&&t<0)||(c<0&&t>0);
  float rate=rev?Cfg::REVERSE:(fabsf(t)<fabsf(c)?Cfg::DECEL:Cfg::ACCEL);
  float d=rate*dt;
  if(rev)return c>0?max(0.0f,c-d):min(0.0f,c+d);
  return c<t?min(t,c+d):max(t,c-d);
}

constexpr uint16_t HIST_STEP=250;
constexpr uint16_t HIST_N=Cfg::RETRACE_MS/HIST_STEP;
struct Hist{int8_t l,r;};
Hist hist[HIST_N];
uint16_t histHead=0,histCount=0,retrIdx=0,retrRemain=0;
uint32_t histAt=0,retrAt=0;

void clearHistory(){histHead=histCount=retrIdx=retrRemain=0;}

void historyUpdate(uint32_t now){
  if(retracing||fs||now-histAt<HIST_STEP)return;
  histAt=now;
  hist[histHead]={(int8_t)constrain((int)roundf(curL),-100,100),(int8_t)constrain((int)roundf(curR),-100,100)};
  histHead=(histHead+1)%HIST_N;
  if(histCount<HIST_N)histCount++;
}

void beginRetrace(uint32_t now){
  tarL=tarR=curL=curR=0;
  driveWrite();
  fs=true;lossHandled=true;retracing=false;
  if(!histCount)return;

  uint16_t i=(histHead+HIST_N-1)%HIST_N,n=histCount;
  while(n&&!hist[i].l&&!hist[i].r){i=(i+HIST_N-1)%HIST_N;n--;}
  if(!n)return;

  retrIdx=i;retrRemain=n;retrAt=now;retracing=true;
}

void retraceUpdate(uint32_t now){
  if(!retracing||now-retrAt<HIST_STEP)return;
  retrAt=now;
  if(!retrRemain){tarL=tarR=0;retracing=false;return;}

  Hist h=hist[retrIdx];
  tarL=constrain(-(int)h.l,-Cfg::RETRACE_MAX_SPEED,Cfg::RETRACE_MAX_SPEED);
  tarR=constrain(-(int)h.r,-Cfg::RETRACE_MAX_SPEED,Cfg::RETRACE_MAX_SPEED);
  retrIdx=(retrIdx+HIST_N-1)%HIST_N;
  retrRemain--;
}

void driveUpdate(){
  uint32_t now=millis();
  if(!retracing&&!lossHandled&&now-cmdAt>Cfg::FAILSAFE)beginRetrace(now);
  retraceUpdate(now);
  historyUpdate(now);
  if(now-driveAt<10)return;
  float dt=min(now-driveAt,50UL)/1000.0f;
  driveAt=now;
  curL=ramp(curL,tarL,dt);
  curR=ramp(curR,tarR,dt);
  driveWrite();
}

/* RELAYS */
const uint8_t rp[]={Cfg::RELAY1,Cfg::RELAY2,Cfg::RELAY3,Cfg::RELAY4};
uint8_t relayMask=0;

void relayRaw(uint8_t mask){
  mask&=0x0F;
  for(uint8_t i=0;i<4;i++)digitalWrite(rp[i],mask&(1<<i)?Cfg::RELAY_ON:Cfg::RELAY_OFF);
  relayMask=mask;
}

bool rowOn(uint8_t row){return row>=1&&row<=4&&(relayMask&(1<<(row-1)));}

uint8_t legacyRelayGroup(){
  for(uint8_t i=0;i<4;i++)if(relayMask==(1U<<i))return i+1;
  return 0;
}

/* ADS1115 + MQ */
Adafruit_ADS1115 ads1,ads2;
bool ads1ok=false,ads2ok=false;

struct Reading{
  uint16_t raw=0;
  float adc=NAN,ao=NAN,rs=NAN,ratio=NAN;
  bool ok=false;
};

Reading mq[Cfg::MQ_N];
uint32_t mqAt=0;
uint8_t mqIndex=0;

Adafruit_ADS1115* getAds(uint8_t n){return n?&ads2:&ads1;}
bool adsOK(uint8_t n){return n?ads2ok:ads1ok;}

void readMQ(uint8_t i){
  const auto &c=Cfg::mq[i];
  Reading &r=mq[i];
  if(!rowOn(c.row)||!adsOK(c.ads)){r=Reading();return;}

  Adafruit_ADS1115 *a=getAds(c.ads);
  int32_t sum=0;
  float volts=0;
  for(uint8_t n=0;n<Cfg::ADC_SAMPLES;n++){
    int16_t x=a->readADC_SingleEnded(c.ch);
    sum+=x;
    volts+=a->computeVolts(x);
  }

  int32_t av=sum/Cfg::ADC_SAMPLES;
  if(av<0)av=0;
  r.raw=(uint16_t)av;
  r.adc=volts/Cfg::ADC_SAMPLES;
  r.ao=r.adc;

  if(r.ao<=.01f||r.ao>=Cfg::MQ_VCC){r.ok=false;return;}
  r.rs=Cfg::RL*(Cfg::MQ_VCC-r.ao)/r.ao;
  r.ratio=r.rs/c.r0;
  r.ok=isfinite(r.ratio)&&r.ratio>0;
}

void mqUpdate(){
  uint32_t now=millis();
  uint16_t slot=Cfg::MQ_PERIOD/Cfg::MQ_N;
  if(slot<2)slot=2;
  if(now-mqAt<slot)return;
  mqAt=now;
  readMQ(mqIndex);
  if(++mqIndex>=Cfg::MQ_N)mqIndex=0;
}

/* ENV */
Adafruit_BME280 bme;
Adafruit_BMP280 bmp(&Wire);
uint8_t envType=0,envAddr=0,envId=0;
float temp=NAN,hum=NAN,press=NAN;
uint32_t envAt=0;

bool probe(uint8_t a){Wire.beginTransmission(a);return Wire.endTransmission()==0;}
uint8_t chip(uint8_t a){
  Wire.beginTransmission(a);Wire.write(0xD0);
  if(Wire.endTransmission(false))return 0;
  if(Wire.requestFrom((int)a,1)!=1)return 0;
  return Wire.read();
}

void envRead(){
  if(envType==1){temp=bme.readTemperature();hum=bme.readHumidity();press=bme.readPressure()/100.0f;}
  else if(envType==2){temp=bmp.readTemperature();hum=NAN;press=bmp.readPressure()/100.0f;}
}

void envBegin(){
  const uint8_t addr[]={0x76,0x77};
  for(uint8_t a:addr){
    if(!probe(a))continue;
    envAddr=a;envId=chip(a);
    if(envId==0x60&&bme.begin(a,&Wire)){envType=1;break;}
    if((envId==0x56||envId==0x57||envId==0x58)&&bmp.begin(a,envId)){envType=2;break;}
  }
  envRead();
}

void envUpdate(){
  if(!envType||millis()-envAt<Cfg::ENV_PERIOD)return;
  envAt=millis();envRead();
}

/* SOUND */
float soundRms=NAN,soundP2P=NAN;
uint32_t soundAt=0;

void soundUpdate(){
  uint32_t now=millis();
  if(now-soundAt<Cfg::SOUND_PERIOD)return;
  soundAt=now;

  float mean=0,m2=0;
  uint16_t lo=4095,hi=0;
  for(uint16_t n=1;n<=Cfg::SOUND_SAMPLES;n++){
    uint16_t x=analogRead(Cfg::SOUND_PIN);
    if(x<lo)lo=x;if(x>hi)hi=x;
    float d=x-mean;mean+=d/n;m2+=d*(x-mean);
  }

  const float V=3.3f/4095.0f;
  soundRms=sqrtf(m2/Cfg::SOUND_SAMPLES)*V;
  soundP2P=(hi-lo)*V;
}

/* IR WARNING ONLY */
bool irWarning=false;
uint32_t irAt=0;
void irUpdate(){
  uint32_t now=millis();
  if(now-irAt<Cfg::IR_PERIOD)return;
  irAt=now;
  irWarning=digitalRead(Cfg::IR_PIN)==Cfg::IR_ACTIVE;
}

/* MEASUREMENT */
enum Stage:uint8_t{IDLE,WARM,MEASURE,READY,ERROR};
Stage stage=IDLE;
uint8_t row=0;
uint32_t stageAt=0,sampleAt=0;
uint32_t rawSum[Cfg::MQ_N]={};
float adcSum[Cfg::MQ_N]={},aoSum[Cfg::MQ_N]={},rsSum[Cfg::MQ_N]={},logSum[Cfg::MQ_N]={};
uint16_t cnt[Cfg::MQ_N]={},rcnt[Cfg::MQ_N]={};

void clearMeasure(){
  memset(rawSum,0,sizeof(rawSum));memset(adcSum,0,sizeof(adcSum));memset(aoSum,0,sizeof(aoSum));
  memset(rsSum,0,sizeof(rsSum));memset(logSum,0,sizeof(logSum));memset(cnt,0,sizeof(cnt));memset(rcnt,0,sizeof(rcnt));row=0;
}

bool hasRow(uint8_t r){for(uint8_t i=0;i<Cfg::MQ_N;i++)if(Cfg::mq[i].row==r)return true;return false;}

void nextRow(uint8_t r){
  relayRaw(0);
  while(r<=Cfg::MQ_ROWS&&!hasRow(r))r++;
  if(r>Cfg::MQ_ROWS){
    bool ok=false;for(uint8_t i=0;i<Cfg::MQ_N;i++)if(cnt[i])ok=true;
    row=0;stage=ok?READY:ERROR;return;
  }
  row=r;relayRaw(1<<(row-1));stage=WARM;stageAt=millis();
}

void collect(){
  for(uint8_t i=0;i<Cfg::MQ_N;i++){
    if(Cfg::mq[i].row!=row||!mq[i].ok)continue;
    rawSum[i]+=mq[i].raw;adcSum[i]+=mq[i].adc;aoSum[i]+=mq[i].ao;rsSum[i]+=mq[i].rs;cnt[i]++;
    if(mq[i].ratio>0){logSum[i]+=logf(mq[i].ratio);rcnt[i]++;}
  }
}

void measureUpdate(){
  if(stage!=WARM&&stage!=MEASURE)return;
  uint32_t now=millis();
  if(stage==WARM){
    if(now-stageAt>=(uint32_t)settings.warm*1000UL){stage=MEASURE;stageAt=sampleAt=now;}
    return;
  }
  if(now-sampleAt>=Cfg::SAMPLE_PERIOD){sampleAt=now;collect();}
  if(now-stageAt>=(uint32_t)settings.measure*1000UL)nextRow(row+1);
}

/* SETTINGS + JSON */
String num(float v,uint8_t d=2){return isfinite(v)?String(v,(unsigned)d):"null";}

void loadSettings(){
  nv.begin(Cfg::NVS,false);
  settings.cam=nv.getString("camIP",Cfg::CAM);
  settings.speed=constrain(nv.getInt("speed",100),20,100);
  settings.nL=constrain(nv.getInt("neutralL",1500),1400,1600);
  settings.nR=constrain(nv.getInt("neutralR",1500),1400,1600);
  settings.warm=constrain(nv.getUInt("warmup",60),0UL,3600UL);
  settings.measure=constrain(nv.getUInt("measure",30),5UL,600UL);
}

String envJson(){
  String j="{\"ok\":";j+=envType?"true":"false";
  j+=",\"t\":"+num(temp,2);j+=",\"h\":"+num(hum,2);j+=",\"p\":"+num(press,1);return j+"}";
}

String mqJson(bool avg){
  String j="{";
  for(uint8_t i=0;i<Cfg::MQ_N;i++){
    if(i)j+=",";j+="\"";j+=Cfg::mq[i].name;j+="\":{";
    if(avg){
      if(!cnt[i])j+="\"ok\":false";
      else{
        float n=cnt[i];j+="\"ok\":true";j+=",\"raw\":"+num(rawSum[i]/n,1);j+=",\"adc\":"+num(adcSum[i]/n,3);
        j+=",\"ao\":"+num(aoSum[i]/n,3);j+=",\"rs\":"+num(rsSum[i]/n,3);j+=",\"ratio\":";
        j+=rcnt[i]?num(expf(logSum[i]/rcnt[i]),3):"null";
      }
    }else{
      j+="\"ok\":";j+=mq[i].ok?"true":"false";j+=",\"raw\":"+String(mq[i].raw);j+=",\"adc\":"+num(mq[i].adc,3);
      j+=",\"ao\":"+num(mq[i].ao,3);j+=",\"rs\":"+num(mq[i].rs,3);j+=",\"ratio\":"+num(mq[i].ratio,3);
    }
    j+="}";
  }
  return j+"}";
}

}

/* PUBLIC */
void hwBegin(){
  loadSettings();
  for(uint8_t p:rp){pinMode(p,OUTPUT);digitalWrite(p,Cfg::RELAY_OFF);}
  relayRaw(0);

  pinMode(Cfg::IR_PIN,INPUT);pinMode(Cfg::SOUND_PIN,INPUT);
  analogReadResolution(12);analogSetPinAttenuation(Cfg::SOUND_PIN,ADC_11db);

  sL.setPeriodHertz(50);sR.setPeriodHertz(50);
  sL.attach(Cfg::SERVO_L,1000,2000);sR.attach(Cfg::SERVO_R,1000,2000);
  cmdAt=driveAt=histAt=millis();driveWrite();

  Wire.begin(Cfg::SDA,Cfg::SCL);Wire.setClock(100000);
  ads1ok=ads1.begin(Cfg::ADS1_ADDR,&Wire);ads2ok=ads2.begin(Cfg::ADS2_ADDR,&Wire);
  if(ads1ok){ads1.setGain(GAIN_TWOTHIRDS);ads1.setDataRate(RATE_ADS1115_860SPS);}
  if(ads2ok){ads2.setGain(GAIN_TWOTHIRDS);ads2.setDataRate(RATE_ADS1115_860SPS);}
  envBegin();
}

void hwUpdate(){driveUpdate();mqUpdate();envUpdate();soundUpdate();irUpdate();measureUpdate();}

void driveSet(int l,int r){
  if(retracing||fs)clearHistory();
  retracing=false;lossHandled=false;fs=false;
  tarL=lim(l);tarR=lim(r);cmdAt=millis();
}

void stopAll(){
  tarL=tarR=curL=curR=0;retracing=false;lossHandled=true;fs=false;
  clearHistory();driveWrite();measureCancel();
}

void relaySetMask(uint8_t mask){measureCancel();relayRaw(mask&0x0F);}
void measureStart(){clearMeasure();nextRow(1);}
void measureCancel(){relayRaw(0);clearMeasure();stage=IDLE;}

void saveSettings(){
  nv.putString("camIP",settings.cam);nv.putInt("speed",settings.speed);nv.putInt("neutralL",settings.nL);nv.putInt("neutralR",settings.nR);
  nv.putUInt("warmup",settings.warm);nv.putUInt("measure",settings.measure);
}

String settingsJson(){
  String j="{\"cam\":\""+settings.cam+"\"";j+=",\"speed\":"+String(settings.speed);j+=",\"nl\":"+String(settings.nL);
  j+=",\"nr\":"+String(settings.nR);j+=",\"warm\":"+String(settings.warm);j+=",\"measure\":"+String(settings.measure);return j+"}";
}

String dataJson(){
  String j="{\"env\":";j+=envJson();
  j+=",\"sound\":{\"rms\":"+num(soundRms,3)+",\"p2p\":"+num(soundP2P,3)+"}";
  j+=",\"ir\":{\"warning\":";j+=irWarning?"true":"false";j+="}";
  j+=",\"relay\":"+String(legacyRelayGroup());j+=",\"relayMask\":"+String(relayMask);
  j+=",\"retrace\":";j+=retracing?"true":"false";
  j+=",\"mq\":"+mqJson(false);
  j+=",\"measurement\":{\"state\":"+String((uint8_t)stage);j+=",\"group\":"+String(row);j+=",\"mq\":"+mqJson(true)+"}}";
  return j;
}

String diagJson(){
  String j="{\"fw\":\"HazRob\",\"api\":3";j+=",\"heap\":"+String(ESP.getFreeHeap());j+=",\"clients\":"+String(WiFi.softAPgetStationNum());
  j+=",\"failsafe\":";j+=fs?"true":"false";j+=",\"retrace\":";j+=retracing?"true":"false";j+=",\"history\":"+String(histCount);
  j+=",\"relayMask\":"+String(relayMask);j+=",\"ads1\":";j+=ads1ok?"true":"false";j+=",\"ads2\":";j+=ads2ok?"true":"false";
  j+=",\"ir\":";j+=irWarning?"true":"false";j+=",\"envType\":"+String(envType);j+=",\"envAddr\":"+String(envAddr);j+=",\"envId\":"+String(envId);
  return j+"}";
}
