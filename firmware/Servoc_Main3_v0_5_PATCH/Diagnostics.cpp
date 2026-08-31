#include "Diagnostics.h"

#include <Arduino.h>
#include <WiFi.h>
#include <math.h>

#include "Drive.h"
#include "Sensors.h"
#include "Relays.h"
#include "MQArray.h"

namespace {

constexpr MQSensor MQS[]={
  MQSensor::MQ2,
  MQSensor::MQ3,
  MQSensor::MQ4,
  MQSensor::MQ7,
  MQSensor::MQ8,
  MQSensor::MQ135
};

const char* key(MQSensor s){
  switch(s){
    case MQSensor::MQ2:return "mq2";
    case MQSensor::MQ3:return "mq3";
    case MQSensor::MQ4:return "mq4";
    case MQSensor::MQ7:return "mq7";
    case MQSensor::MQ8:return "mq8";
    case MQSensor::MQ135:return "mq135";
    default:return "mq";
  }
}

String num(float v,uint8_t d=2){
  return isfinite(v)?String(v,(unsigned)d):"null";
}

String mqJson(MQSensor s){
  const MQReading* r=sensorsMq(s);

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
    j+=",\"raw\":0,\"adc\":null,\"ao\":null,\"rs\":null,\"ratio\":null";
  }

  return j+"}";
}

} // namespace


String diagnosticsJson(){
  String j;
  j.reserve(1800);

  j="{\"firmware\":\"Servoc_Main3_v0.5\",\"api_version\":2,\"uptime\":";
  j+=String(millis()/1000UL);

  j+=",\"heap\":";
  j+=String(ESP.getFreeHeap());

  j+=",\"clients\":";
  j+=String(WiFi.softAPgetStationNum());

  // Drive
  j+=",\"drive\":{\"left\":";
  j+=String(driveLeftOutput());

  j+=",\"right\":";
  j+=String(driveRightOutput());

  j+=",\"failsafe\":";
  j+=driveFailsafeActive()?"true":"false";
  j+="}";

  // Environmental sensor diagnostics. Keep legacy "bme" for app compatibility.
  j+=",\"env\":{\"ok\":";
  j+=sensorsEnvPresent()?"true":"false";
  j+=",\"i2c_found\":";
  j+=sensorsEnvI2cFound()?"true":"false";
  j+=",\"type\":\"";j+=sensorsEnvType();j+="\"";
  j+=",\"address\":";
  if(sensorsEnvAddress()){j+="\"0x";if(sensorsEnvAddress()<16)j+="0";j+=String(sensorsEnvAddress(),HEX);j+="\"";}else j+="null";
  j+=",\"chip_id\":";
  if(sensorsEnvChipId()){j+="\"0x";if(sensorsEnvChipId()<16)j+="0";j+=String(sensorsEnvChipId(),HEX);j+="\"";}else j+="null";
  j+=",\"humidity_available\":";j+=sensorsEnvHumidityAvailable()?"true":"false";
  j+=",\"temperature\":";j+=num(sensorsTemperature(),2);
  j+=",\"humidity\":";j+=num(sensorsHumidity(),2);
  j+=",\"pressure\":";j+=num(sensorsPressure(),1);
  j+="}";

  j+=",\"bme\":{\"ok\":";
  j+=sensorsBmePresent()?"true":"false";
  j+=",\"sensor\":\"";j+=sensorsEnvType();j+="\"";
  j+=",\"temperature\":";j+=num(sensorsTemperature(),2);
  j+=",\"humidity\":";j+=num(sensorsHumidity(),2);
  j+=",\"pressure\":";j+=num(sensorsPressure(),1);
  j+="}";

  // Relay rows
  j+=",\"relay\":{\"group1\":";
  j+=relaysGroup1On()?"true":"false";

  j+=",\"group2\":";
  j+=relaysGroup2On()?"true":"false";

  j+=",\"group3\":";
  j+=relaysGroup3On()?"true":"false";

  j+="}";

  // MQ
  j+=",\"mq\":{";

  for(uint8_t i=0;i<sizeof(MQS)/sizeof(MQS[0]);i++){
    if(i)j+=",";

    j+="\"";
    j+=key(MQS[i]);
    j+="\":";
    j+=mqJson(MQS[i]);
  }

  j+="}";

  // Analysis + measurement
  j+=",\"gas\":";
  j+=sensorsGasJson();

  j+=",\"measurement\":";
  j+=sensorsMeasurementJson();

  return j+"}";
}