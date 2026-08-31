#include "Hardware.h"
#include <Preferences.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <WiFi.h>
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
bool fs=false;

float lim(float v){
  float m=constrain(settings.speed,20,100);
  return constrain(v,-m,m);
}

int pulse(float v,int n,int dir){
  v=constrain(v*dir,-100.0f,100.0f);
  return v>=0 ? n+(2000-n)*v/100 : n+(n-1000)*v/100;
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

  if(rev){
    if(c>0)return max(0.0f,c-d);
    return min(0.0f,c+d);
  }

  return c<t?min(t,c+d):max(t,c-d);
}

void driveUpdate(){
  uint32_t now=millis();

  if(now-cmdAt>Cfg::FAILSAFE){
    if(!fs){
      tarL=tarR=curL=curR=0;
      driveWrite();
      fs=true;
    }
    return;
  }

  if(now-driveAt<10)return;
  float dt=min(now-driveAt,50UL)/1000.0f;
  driveAt=now;

  curL=ramp(curL,tarL,dt);
  curR=ramp(curR,tarR,dt);
  driveWrite();
}

/* RELAYS */
uint8_t relay=0;

void relayRaw(uint8_t g){
  digitalWrite(Cfg::RELAY1,HIGH);
  digitalWrite(Cfg::RELAY2,HIGH);
  digitalWrite(Cfg::RELAY3,HIGH);

  if(g==1)digitalWrite(Cfg::RELAY1,LOW);
  else if(g==2)digitalWrite(Cfg::RELAY2,LOW);
  else if(g==3)digitalWrite(Cfg::RELAY3,LOW);
  else g=0;

  relay=g;
}

/* MQ */
struct Reading{
  uint16_t raw=0;
  float adc=NAN,ao=NAN,rs=NAN,ratio=NAN;
  bool ok=false;
};

Reading mq[Cfg::MQ_N];
uint32_t mqAt=0;

void readMQ(uint8_t i){
  const auto &c=Cfg::mq[i];
  Reading &r=mq[i];

  if(relay!=c.row){r=Reading();return;}

  uint32_t raw=0,mv=0;

  for(uint8_t n=0;n<Cfg::ADC_SAMPLES;n++){
    raw+=analogRead(c.pin);
    mv+=analogReadMilliVolts(c.pin);
  }

  r.raw=raw/Cfg::ADC_SAMPLES;
  r.adc=(mv/(float)Cfg::ADC_SAMPLES)/1000.0f;
  r.ao=r.adc*c.div;

  if(r.ao<=.01f||r.ao>=Cfg::MQ_VCC){r.ok=false;return;}

  r.rs=Cfg::RL*(Cfg::MQ_VCC-r.ao)/r.ao;
  r.ratio=r.rs/c.r0;
  r.ok=isfinite(r.ratio)&&r.ratio>0;
}

void mqUpdate(){
  uint32_t now=millis();
  if(now-mqAt<Cfg::MQ_PERIOD)return;
  mqAt=now;

  for(uint8_t i=0;i<Cfg::MQ_N;i++)readMQ(i);
}

/* ENV */
Adafruit_BME280 bme;
Adafruit_BMP280 bmp(&Wire);

uint8_t envType=0,envAddr=0,envId=0;
float temp=NAN,hum=NAN,press=NAN;
uint32_t envAt=0;

bool probe(uint8_t a){
  Wire.beginTransmission(a);
  return Wire.endTransmission()==0;
}

uint8_t chip(uint8_t a){
  Wire.beginTransmission(a);
  Wire.write(0xD0);
  if(Wire.endTransmission(false))return 0;
  if(Wire.requestFrom((int)a,1)!=1)return 0;
  return Wire.read();
}

void envRead(){
  if(envType==1){
    temp=bme.readTemperature();
    hum=bme.readHumidity();
    press=bme.readPressure()/100.0f;
  }else if(envType==2){
    temp=bmp.readTemperature();
    hum=NAN;
    press=bmp.readPressure()/100.0f;
  }
}

void envBegin(){
  Wire.begin(Cfg::SDA,Cfg::SCL);
  const uint8_t a[]={0x76,0x77};

  for(uint8_t x:a){
    if(!probe(x))continue;

    envAddr=x; envId=chip(x);

    if(envId==0x60 && bme.begin(x,&Wire)){envType=1;break;}

    if((envId==0x56||envId==0x57||envId==0x58) &&
       bmp.begin(x,envId)){envType=2;break;}
  }

  envRead();
}

void envUpdate(){
  if(!envType||millis()-envAt<1000)return;
  envAt=millis();
  envRead();
}

/* MEASUREMENT */
enum Stage:uint8_t{IDLE,WARM,MEASURE,READY,ERROR};
Stage stage=IDLE;

uint8_t row=0;
uint32_t stageAt=0,sampleAt=0;

uint32_t rawSum[Cfg::MQ_N]={};
float adcSum[Cfg::MQ_N]={},aoSum[Cfg::MQ_N]={};
float rsSum[Cfg::MQ_N]={},logSum[Cfg::MQ_N]={};
uint16_t cnt[Cfg::MQ_N]={},rcnt[Cfg::MQ_N]={};

void clearMeasure(){
  memset(rawSum,0,sizeof(rawSum));
  memset(adcSum,0,sizeof(adcSum));
  memset(aoSum,0,sizeof(aoSum));
  memset(rsSum,0,sizeof(rsSum));
  memset(logSum,0,sizeof(logSum));
  memset(cnt,0,sizeof(cnt));
  memset(rcnt,0,sizeof(rcnt));
  row=0;
}

bool hasRow(uint8_t r){
  for(uint8_t i=0;i<Cfg::MQ_N;i++)
    if(Cfg::mq[i].row==r)return true;
  return false;
}

void nextRow(uint8_t r){
  relayRaw(0);

  while(r<=3&&!hasRow(r))r++;

  if(r>3){
    row=0;
    bool ok=false;
    for(uint8_t i=0;i<Cfg::MQ_N;i++)if(cnt[i])ok=true;
    stage=ok?READY:ERROR;
    return;
  }

  row=r;
  relayRaw(row);
  stage=WARM;
  stageAt=millis();
}

void collect(){
  for(uint8_t i=0;i<Cfg::MQ_N;i++){
    if(Cfg::mq[i].row!=row||!mq[i].ok)continue;

    rawSum[i]+=mq[i].raw;
    adcSum[i]+=mq[i].adc;
    aoSum[i]+=mq[i].ao;
    rsSum[i]+=mq[i].rs;
    cnt[i]++;

    if(mq[i].ratio>0){
      logSum[i]+=logf(mq[i].ratio);
      rcnt[i]++;
    }
  }
}

void measureUpdate(){
  if(stage!=WARM&&stage!=MEASURE)return;

  uint32_t now=millis();

  if(stage==WARM){
    if(now-stageAt >= (uint32_t)settings.warm*1000UL){
      stage=MEASURE;
      stageAt=sampleAt=now;
    }
    return;
  }

  if(now-sampleAt>=Cfg::SAMPLE_PERIOD){
    sampleAt=now;
    collect();
  }

  if(now-stageAt >= (uint32_t)settings.measure*1000UL)
    nextRow(row+1);
}

/* SETTINGS + JSON */
String num(float v,uint8_t d=2){
  return isfinite(v)?String(v,(unsigned)d):"null";
}

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
  String j="{\"ok\":";
  j+=envType?"true":"false";
  j+=",\"t\":"+num(temp,2);
  j+=",\"h\":"+num(hum,2);
  j+=",\"p\":"+num(press,1);
  return j+"}";
}

String mqJson(bool average){
  String j="{";

  for(uint8_t i=0;i<Cfg::MQ_N;i++){
    if(i)j+=",";
    j+="\""; j+=Cfg::mq[i].name; j+="\":{";

    if(average){
      if(!cnt[i])j+="\"ok\":false";
      else{
        float n=cnt[i];
        j+="\"ok\":true,\"raw\":"+num(rawSum[i]/n,1);
        j+=",\"adc\":"+num(adcSum[i]/n,3);
        j+=",\"ao\":"+num(aoSum[i]/n,3);
        j+=",\"rs\":"+num(rsSum[i]/n,3);
        j+=",\"ratio\":";
        j+=rcnt[i]?num(expf(logSum[i]/rcnt[i]),3):"null";
      }
    }else{
      j+="\"ok\":";
      j+=mq[i].ok?"true":"false";
      j+=",\"raw\":"+String(mq[i].raw);
      j+=",\"adc\":"+num(mq[i].adc,3);
      j+=",\"ao\":"+num(mq[i].ao,3);
      j+=",\"rs\":"+num(mq[i].rs,3);
      j+=",\"ratio\":"+num(mq[i].ratio,3);
    }

    j+="}";
  }

  return j+"}";
}

}

/* PUBLIC */

void hwBegin(){
  loadSettings();

  digitalWrite(Cfg::RELAY1,HIGH);
  digitalWrite(Cfg::RELAY2,HIGH);
  digitalWrite(Cfg::RELAY3,HIGH);

  pinMode(Cfg::RELAY1,OUTPUT);
  pinMode(Cfg::RELAY2,OUTPUT);
  pinMode(Cfg::RELAY3,OUTPUT);

  relayRaw(0);

  for(uint8_t i=0;i<Cfg::MQ_N;i++){
    pinMode(Cfg::mq[i].pin,INPUT);
    analogSetPinAttenuation(Cfg::mq[i].pin,ADC_11db);
  }

  analogReadResolution(12);

  sL.setPeriodHertz(50); sR.setPeriodHertz(50);
  sL.attach(Cfg::SERVO_L,1000,2000);
  sR.attach(Cfg::SERVO_R,1000,2000);

  cmdAt=driveAt=millis();
  driveWrite();

  envBegin();
}

void hwUpdate(){
  driveUpdate();
  envUpdate();
  mqUpdate();
  measureUpdate();
}

void driveSet(int l,int r){
  tarL=lim(l);
  tarR=lim(r);
  cmdAt=millis();
  fs=false;
}

void stopAll(){
  tarL=tarR=curL=curR=0;
  driveWrite();
  measureCancel();
}

void relaySet(uint8_t g){
  measureCancel();
  relayRaw(g<=3?g:0);
}

void measureStart(){
  clearMeasure();
  nextRow(1);
}

void measureCancel(){
  relayRaw(0);
  clearMeasure();
  stage=IDLE;
}

void saveSettings(){
  nv.putString("camIP",settings.cam);
  nv.putInt("speed",settings.speed);
  nv.putInt("neutralL",settings.nL);
  nv.putInt("neutralR",settings.nR);
  nv.putUInt("warmup",settings.warm);
  nv.putUInt("measure",settings.measure);
}

String settingsJson(){
  String j="{\"cam\":\""+settings.cam+"\"";
  j+=",\"speed\":"+String(settings.speed);
  j+=",\"nl\":"+String(settings.nL);
  j+=",\"nr\":"+String(settings.nR);
  j+=",\"warm\":"+String(settings.warm);
  j+=",\"measure\":"+String(settings.measure);
  return j+"}";
}

String dataJson(){
  String j="{\"env\":";
  j+=envJson();
  j+=",\"relay\":"+String(relay);
  j+=",\"mq\":"+mqJson(false);
  j+=",\"measurement\":{\"state\":"+String((uint8_t)stage);
  j+=",\"group\":"+String(row);
  j+=",\"mq\":"+mqJson(true)+"}}";
  return j;
}

String diagJson(){
  String j="{\"fw\":\"HazRob\",\"api\":3";
  j+=",\"heap\":"+String(ESP.getFreeHeap());
  j+=",\"clients\":"+String(WiFi.softAPgetStationNum());
  j+=",\"failsafe\":";
  j+=fs?"true":"false";
  j+=",\"envType\":"+String(envType);
  j+=",\"envAddr\":"+String(envAddr);
  j+=",\"envId\":"+String(envId);
  return j+"}";
}
