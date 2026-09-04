#pragma once
#include <Arduino.h>

namespace Cfg{
constexpr char FW[]="HazRob 0.9.0";
constexpr uint8_t API=4;
constexpr char HW_PROFILE[]="hazrob-0.9-1r8mq-v1";

constexpr char SSID[]="HazRob";
constexpr char NVS[]="servoc";
constexpr char CAM[]="192.168.4.2";

constexpr uint8_t SERVO_L=18,SERVO_R=19;
constexpr uint32_t FAILSAFE=1000,RETRACE_MS=60000,LINK_LOSS_MS=15000,ADS_REPROBE_MS=3000;
constexpr float ACCEL=500.0f,DECEL=800.0f,REVERSE=1000.0f;
constexpr int RETRACE_MAX_SPEED=40;

constexpr uint8_t SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49;

// One active-LOW relay powers all eight MQ heaters/sensors.
constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;

constexpr uint8_t SOUND_PIN=36,IR_PIN=35,AUX1=33,AUX2=34,AUX3=39;
constexpr uint8_t IR_ACTIVE=LOW;

struct MQ{const char* name;uint8_t ads,ch;float r0;};
constexpr MQ mq[]={
  {"mq2",0,0,10.0f},{"mq4",0,1,10.0f},{"mq5",0,2,10.0f},{"mq8",0,3,10.0f},
  {"mq3",1,0,10.0f},{"mq7",1,1,10.0f},{"mq9",1,2,10.0f},{"mq135",1,3,10.0f}
};
constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

constexpr float MQ_VCC=5.0f,RL=10.0f;
constexpr uint16_t MQ_PERIOD=100,SAMPLE_PERIOD=200;
constexpr uint8_t ADC_SAMPLES=4;
constexpr uint16_t OPERATIONAL_WARMUP_SEC=60,ANALYSIS_WINDOW_SEC=30;
constexpr bool TRACKING_CONTINUOUS=true;

constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=128,ENV_PERIOD=1000,IR_PERIOD=50;
}

struct Settings{
  String cam;
  int speed=100,nL=1500,nR=1500;
  uint16_t warm=Cfg::OPERATIONAL_WARMUP_SEC,measure=Cfg::ANALYSIS_WINDOW_SEC;
};
