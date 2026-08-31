#include "Sensors.h"
#include "Pins.h"
#include "Relays.h"
#include "Config.h"
#include "ExposureLimits.h"

#include <Wire.h>
#include <Adafruit_BME280.h>
#include <Adafruit_BMP280.h>
#include <math.h>

namespace {

constexpr uint32_t BME_MS=1000,SAMPLE_MS=200;
constexpr uint8_t MAX_GASES=13;

constexpr MQSensor MQS[]={
  MQSensor::MQ2,MQSensor::MQ3,MQSensor::MQ4,MQSensor::MQ5,
  MQSensor::MQ7,MQSensor::MQ8,MQSensor::MQ9,MQSensor::MQ135
};

Adafruit_BME280 bme;
Adafruit_BMP280 bmp(&Wire);

enum class EnvType:uint8_t{None,BME280,BMP280,Unknown};
EnvType envType=EnvType::None;
bool envOK=false,envI2cFound=false;
uint8_t envAddress=0,envChipId=0;
float temp=NAN,hum=NAN,press=NAN;
uint32_t lastBme=0;

enum class Stage:uint8_t{Idle,Warm,Measure,Ready,Error};
Stage stage=Stage::Idle;

uint8_t row=0;
uint32_t stageStart=0,lastSample=0,warmMs=0,measureMs=0;
uint16_t samples[3]={};

float logSum[MQ_SENSOR_COUNT]={};
float adcSum[MQ_SENSOR_COUNT]={};
float aoSum[MQ_SENSOR_COUNT]={};
float rsSum[MQ_SENSOR_COUNT]={};

uint32_t rawSum[MQ_SENSOR_COUNT]={};
uint16_t ratioN[MQ_SENSOR_COUNT]={};
uint16_t valueN[MQ_SENSOR_COUNT]={};

MQRatios ratios;

GasEstimate results[MAX_GASES];
uint8_t resultN=0;


// =====================================================
// HELPERS
// =====================================================

uint8_t idx(MQSensor s){
  return static_cast<uint8_t>(s);
}

String num(float v,uint8_t d=2){
  return isfinite(v)?String(v,(unsigned)d):"null";
}

const char* mqKey(MQSensor s){
  switch(s){
    case MQSensor::MQ2:return "mq2";
    case MQSensor::MQ3:return "mq3";
    case MQSensor::MQ4:return "mq4";
    case MQSensor::MQ5:return "mq5";
    case MQSensor::MQ7:return "mq7";
    case MQSensor::MQ8:return "mq8";
    case MQSensor::MQ9:return "mq9";
    case MQSensor::MQ135:return "mq135";
    default:return "mq";
  }
}

const char* stateName(){
  if(stage==Stage::Idle)return "idle";
  if(stage==Stage::Ready)return "ready";
  if(stage==Stage::Error)return "error";

  if(stage==Stage::Warm)
    return row==1?"warm1":row==2?"warm2":"warm3";

  return row==1?"measure1":row==2?"measure2":"measure3";
}

uint32_t remainingMs(){
  uint32_t lim=
    stage==Stage::Warm?warmMs:
    stage==Stage::Measure?measureMs:0;

  if(!lim)return 0;

  uint32_t e=millis()-stageStart;
  return e>=lim?0:lim-e;
}

bool rowInstalled(uint8_t r){
  for(uint8_t i=0;i<mqArrayChannelCount();i++){
    const MQChannelConfig* c=mqArrayChannelConfig(i);
    if(c&&c->enabled&&c->row==r)return true;
  }
  return false;
}

void powerRow(uint8_t r){
  relaysSet(
    r==1?RelayGroup::Group1:
    r==2?RelayGroup::Group2:
    r==3?RelayGroup::Group3:
         RelayGroup::Off
  );
}

uint8_t riskOf(const GasEstimate& e){
  RiskLevel r;

  return ExposureLimits::screeningRisk(
    e.gas,
    e.ppmEquivalent,
    r
  ) ? static_cast<uint8_t>(r) : 0;
}

uint8_t confidenceOf(const GasEstimate& e){
  return static_cast<uint8_t>(
    gasConfidence(e.contributors,e.agreement)
  );
}

void clearSession(){
  memset(logSum,0,sizeof(logSum));
  memset(adcSum,0,sizeof(adcSum));
  memset(aoSum,0,sizeof(aoSum));
  memset(rsSum,0,sizeof(rsSum));
  memset(rawSum,0,sizeof(rawSum));
  memset(ratioN,0,sizeof(ratioN));
  memset(valueN,0,sizeof(valueN));
  memset(samples,0,sizeof(samples));

  resultN=0;
  row=0;
  gasAnalyzerClear(ratios);
}


// =====================================================
// BME280
// =====================================================

uint8_t readI2cReg(uint8_t address,uint8_t reg){
  Wire.beginTransmission(address);
  Wire.write(reg);
  if(Wire.endTransmission(false)!=0)return 0;
  if(Wire.requestFrom((int)address,1)!=1)return 0;
  return Wire.read();
}

bool probeAddress(uint8_t address){
  Wire.beginTransmission(address);
  return Wire.endTransmission()==0;
}

const char* envTypeName(){
  switch(envType){
    case EnvType::BME280:return "BME280";
    case EnvType::BMP280:return "BMP280";
    case EnvType::Unknown:return "UNKNOWN";
    default:return "NONE";
  }
}

void detectEnvironmentSensor(){
  envOK=false;envI2cFound=false;envAddress=0;envChipId=0;envType=EnvType::None;
  temp=hum=press=NAN;

  const uint8_t addresses[]={0x76,0x77};
  for(uint8_t address:addresses){
    if(!probeAddress(address))continue;
    envI2cFound=true;envAddress=address;envChipId=readI2cReg(address,0xD0);

    if(envChipId==0x60){
      envType=EnvType::BME280;
      envOK=bme.begin(address,&Wire);
    }else if(envChipId==0x58){
      envType=EnvType::BMP280;
      envOK=bmp.begin(address,envChipId);
    }else{
      envType=EnvType::Unknown;
      envOK=false;
    }
    break;
  }

  if(!envOK)return;
  if(envType==EnvType::BME280){
    temp=bme.readTemperature();
    hum=bme.readHumidity();
    press=bme.readPressure()/100.0f;
  }else if(envType==EnvType::BMP280){
    temp=bmp.readTemperature();
    hum=NAN;
    press=bmp.readPressure()/100.0f;
  }
}

void updateBme(){
  uint32_t now=millis();
  if(!envOK||now-lastBme<BME_MS)return;
  lastBme=now;
  if(envType==EnvType::BME280){
    temp=bme.readTemperature();
    hum=bme.readHumidity();
    press=bme.readPressure()/100.0f;
  }else if(envType==EnvType::BMP280){
    temp=bmp.readTemperature();
    hum=NAN;
    press=bmp.readPressure()/100.0f;
  }
}


// =====================================================
// MQ SAMPLING
// =====================================================

void collect(){
  for(MQSensor s:MQS){
    if(!mqArrayInstalled(s)||!mqArrayPowered(s))
      continue;

    const MQReading* r=mqArrayReading(s);

    if(!r||!r->valid)
      continue;

    uint8_t i=idx(s);

    if(i>=MQ_SENSOR_COUNT)
      continue;

    rawSum[i]+=r->raw;
    adcSum[i]+=r->adcVoltage;
    aoSum[i]+=r->sensorVoltage;
    rsSum[i]+=r->rs;
    valueN[i]++;

    if(isfinite(r->rsR0)&&r->rsR0>0){
      logSum[i]+=logf(r->rsR0);
      ratioN[i]++;
    }
  }

  if(row>=1&&row<=3)
    samples[row-1]++;
}

void buildRatios(){
  gasAnalyzerClear(ratios);

  for(MQSensor s:MQS){
    uint8_t i=idx(s);

    if(i<MQ_SENSOR_COUNT&&ratioN[i])
      ratios.rsR0[i]=
        expf(logSum[i]/float(ratioN[i]));
  }
}


// =====================================================
// RESULTS
// =====================================================

void finish(){
  relaysOff();
  row=0;

  buildRatios();

  GasEstimate all[MAX_GASES];

  size_t n=gasAnalyzeAll(
    ratios,
    all,
    MAX_GASES
  );

  resultN=0;

  for(size_t i=0;i<n&&resultN<MAX_GASES;i++){
    if(all[i].valid&&all[i].contributors)
      results[resultN++]=all[i];
  }

  stage=resultN?Stage::Ready:Stage::Error;
  stageStart=millis();
}


// =====================================================
// GENERIC 3-ROW STATE MACHINE
// =====================================================

void startNextRow(uint8_t first){
  relaysOff();

  for(uint8_t r=first;r<=3;r++){
    if(!rowInstalled(r))
      continue;

    row=r;
    powerRow(row);

    stage=Stage::Warm;
    stageStart=millis();
    lastSample=0;
    return;
  }

  finish();
}

void runSession(){
  if(stage!=Stage::Warm&&
     stage!=Stage::Measure)
    return;

  uint32_t now=millis();
  uint32_t elapsed=now-stageStart;

  if(stage==Stage::Warm){
    if(elapsed>=warmMs){
      stage=Stage::Measure;
      stageStart=now;
      lastSample=0;
    }
    return;
  }

  if(now-lastSample>=SAMPLE_MS){
    lastSample=now;
    collect();
  }

  if(elapsed>=measureMs)
    startNextRow(row+1);
}


// =====================================================
// JSON HELPERS
// =====================================================

String mqLive(MQSensor s){
  const MQReading* r=mqArrayReading(s);

  String j="{\"status\":\"";
  j+=mqArrayStatusText(s);
  j+="\"";

  if(r&&r->valid){
    j+=",\"raw\":"+String(r->raw);
    j+=",\"adc\":"+num(r->adcVoltage,3);
    j+=",\"ao\":"+num(r->sensorVoltage,3);
    j+=",\"rs\":"+num(r->rs,3);
    j+=",\"ratio\":"+num(r->rsR0,3);
  }else{
    j+=",\"raw\":0,\"adc\":null,\"ao\":null,"
       "\"rs\":null,\"ratio\":null";
  }

  return j+"}";
}

String mqAverage(MQSensor s){
  uint8_t i=idx(s);

  if(i>=MQ_SENSOR_COUNT||!valueN[i])
    return "{\"valid\":false}";

  float n=valueN[i];

  String j="{\"valid\":true";

  j+=",\"raw\":"+num(rawSum[i]/n,1);
  j+=",\"adc\":"+num(adcSum[i]/n,3);
  j+=",\"ao\":"+num(aoSum[i]/n,3);
  j+=",\"rs\":"+num(rsSum[i]/n,3);
  j+=",\"ratio\":";

  j+=ratioN[i]
    ?num(expf(logSum[i]/float(ratioN[i])),3)
    :"null";

  return j+"}";
}

String estimateJson(const GasEstimate& e){
  String j="{\"gas\":\"";

  j+=::gasName(e.gas);

  j+="\",\"chem\":";
  j+=String(
    static_cast<uint8_t>(
      gasChemId(e.gas)
    )
  );

  j+=",\"ppm\":";
  j+=num(e.ppmEquivalent,2);

  // 0 = no numerical reference
  // 1..5 = agreed risk colours
  j+=",\"risk\":";
  j+=String(riskOf(e));

  // 0 none, 1 *, 2 **, 3 ***
  j+=",\"confidence\":";
  j+=String(confidenceOf(e));

  j+=",\"agreement\":";
  j+=num(e.agreement,1);

  j+=",\"contributors\":";
  j+=String(e.contributors);

  return j+"}";
}

void currentRatios(MQRatios& out){
  if(stage==Stage::Ready){
    out=ratios;
    return;
  }

  gasAnalyzerClear(out);

  for(MQSensor s:MQS){
    uint8_t i=idx(s);

    if(i>=MQ_SENSOR_COUNT)
      continue;

    const MQReading* r=mqArrayReading(s);

    if(r&&r->valid)
      out.rsR0[i]=r->rsR0;
  }
}

} // namespace


// =====================================================
// PUBLIC API
// =====================================================

void sensorsBegin(){
  relaysBegin();
  mqArrayBegin();

  Wire.begin(
    Pins::I2C_SDA,
    Pins::I2C_SCL
  );

  detectEnvironmentSensor();

  clearSession();
}

void sensorsUpdate(){
  updateBme();
  mqArrayUpdate();
  runSession();
}


// =====================================================
// BME API
// =====================================================

bool sensorsBmePresent(){return envOK;}
bool sensorsEnvPresent(){return envOK;}
bool sensorsEnvI2cFound(){return envI2cFound;}
const char* sensorsEnvType(){return envTypeName();}
uint8_t sensorsEnvAddress(){return envAddress;}
uint8_t sensorsEnvChipId(){return envChipId;}
bool sensorsEnvHumidityAvailable(){return envType==EnvType::BME280;}
float sensorsTemperature(){return temp;}
float sensorsHumidity(){return hum;}
float sensorsPressure(){return press;}


// =====================================================
// MQ API
// =====================================================

const MQReading* sensorsMq(MQSensor s){
  return mqArrayReading(s);
}


// =====================================================
// GAS API
// =====================================================

bool sensorsBestGas(GasEstimate& out){
  MQRatios r;
  currentRatios(r);

  out=gasBestConsensus(r,2);
  return out.valid;
}

bool sensorsGasAnalysisReady(){
  GasEstimate e;
  return sensorsBestGas(e);
}

String sensorsGasJson(){
  GasEstimate e;

  if(!sensorsBestGas(e))
    return "{\"ready\":false}";

  String j="{\"ready\":true,\"result\":";
  j+=estimateJson(e);
  return j+"}";
}


// =====================================================
// MEASUREMENT CONTROL
// =====================================================

void sensorsMeasurementStart(){
  clearSession();

  warmMs=
    uint32_t(gConfig.warmupSec)*1000UL;

  measureMs=
    uint32_t(gConfig.measureSec)*1000UL;

  startNextRow(1);
}

void sensorsMeasurementCancel(){
  relaysOff();
  clearSession();

  stage=Stage::Idle;
  stageStart=millis();
}

bool sensorsMeasurementRunning(){
  return
    stage==Stage::Warm||
    stage==Stage::Measure;
}

bool sensorsMeasurementReady(){
  return stage==Stage::Ready;
}


// =====================================================
// MEASUREMENT JSON
// =====================================================

String sensorsMeasurementJson(){
  String j="{\"state\":\"";

  j+=stateName();

  j+="\",\"group\":";
  j+=String(row);

  j+=",\"remaining\":";
  j+=num(remainingMs()/1000.0f,1);

  j+=",\"warmup\":";
  j+=String(warmMs/1000UL);

  j+=",\"measure\":";
  j+=String(measureMs/1000UL);

  j+=",\"samples1\":";
  j+=String(samples[0]);

  j+=",\"samples2\":";
  j+=String(samples[1]);

  j+=",\"samples3\":";
  j+=String(samples[2]);

  j+=",\"environment\":{\"ok\":";
  j+=envOK?"true":"false";

  j+=",\"temperature\":";
  j+=num(temp,2);

  j+=",\"humidity\":";
  j+=num(hum,2);

  j+=",\"pressure\":";
  j+=num(press,1);

  j+="},\"mq\":{";

  bool comma=false;

  for(MQSensor s:MQS){
    if(!mqArrayInstalled(s))
      continue;

    if(comma)j+=",";
    comma=true;

    j+="\"";
    j+=mqKey(s);
    j+="\":";
    j+=mqAverage(s);
  }

  j+="},\"candidates\":[";

  for(uint8_t i=0;i<resultN;i++){
    if(i)j+=",";
    j+=estimateJson(results[i]);
  }

  return j+"]}";
}


// =====================================================
// MAIN JSON
// =====================================================

String sensorsJson(){
  String j="{\"bme\":{\"ok\":";

  j+=envOK?"true":"false";

  j+=",\"temperature\":";
  j+=num(temp,2);

  j+=",\"humidity\":";
  j+=num(hum,2);

  j+=",\"pressure\":";
  j+=num(press,1);

  j+="},\"relay\":{\"group1\":";
  j+=relaysGroup1On()?"true":"false";

  j+=",\"group2\":";
  j+=relaysGroup2On()?"true":"false";

  j+=",\"group3\":";
  j+=relaysGroup3On()?"true":"false";

  j+="},\"mq\":{";

  bool comma=false;

  for(MQSensor s:MQS){
    if(!mqArrayInstalled(s))
      continue;

    if(comma)j+=",";
    comma=true;

    j+="\"";
    j+=mqKey(s);
    j+="\":";
    j+=mqLive(s);
  }

  j+="},\"gas\":";
  j+=sensorsGasJson();

  j+=",\"measurement\":";
  j+=sensorsMeasurementJson();

  return j+"}";
}