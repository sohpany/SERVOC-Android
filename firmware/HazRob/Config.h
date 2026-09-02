#pragma once
#include <Arduino.h>

namespace Cfg {

// WiFi / camera
constexpr char SSID[]="HazRob";
constexpr char NVS[]="servoc";
constexpr char CAM[]="192.168.4.2";

// Drive
constexpr uint8_t SERVO_L=18,SERVO_R=19;
constexpr uint32_t FAILSAFE=1000;
constexpr float ACCEL=500.0f,DECEL=800.0f,REVERSE=1000.0f;
constexpr uint32_t RETRACE_MS=60000;
constexpr int RETRACE_MAX_SPEED=40;

// I2C
constexpr uint8_t SDA=21,SCL=22;
constexpr uint8_t ADS1_ADDR=0x48,ADS2_ADDR=0x49;

// 4-channel active-LOW relay board
// R1 = MQ bank 1, R2 = MQ bank 2, R3/R4 reserved for future hardware.
constexpr uint8_t RELAY1=25,RELAY2=26,RELAY3=27,RELAY4=32;
constexpr uint8_t RELAY_ON=LOW,RELAY_OFF=HIGH;
constexpr uint8_t MQ_ROWS=2;

// Local sensors
constexpr uint8_t SOUND_PIN=36; // MAX4466 OUT, ADC1
constexpr uint8_t IR_PIN=35;    // digital warning only
constexpr uint8_t AUX1=33,AUX2=34,AUX3=39;
constexpr uint8_t IR_ACTIVE=LOW;

// MQ via 2 x ADS1115. ads: 0=0x48, 1=0x49; ch: A0..A3; row: relay bank.
struct MQ{
  const char* name;
  uint8_t ads,ch,row;
  float r0;
};

constexpr MQ mq[]={
  {"mq2",   0,0,1,10.0f},
  {"mq4",   0,1,1,10.0f},
  {"mq5",   0,2,1,10.0f},
  {"mq8",   0,3,1,10.0f},
  {"mq3",   1,0,2,10.0f},
  {"mq7",   1,1,2,10.0f},
  {"mq9",   1,2,2,10.0f},
  {"mq135", 1,3,2,10.0f}
};
constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

// Acquisition
constexpr float MQ_VCC=5.0f,RL=10.0f;
constexpr uint16_t MQ_PERIOD=100,SAMPLE_PERIOD=200;
constexpr uint8_t ADC_SAMPLES=4;
constexpr uint16_t ENV_PERIOD=1000;
constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=128;
constexpr uint16_t IR_PERIOD=50;

}

struct Settings{
  String cam;
  int speed=100,nL=1500,nR=1500;
  uint16_t warm=60,measure=30;
};
