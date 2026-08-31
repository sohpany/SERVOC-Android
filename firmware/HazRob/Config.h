#pragma once
#include <Arduino.h>

namespace Cfg {

// WiFi / camera
constexpr char SSID[]="Servoc";
constexpr char NVS[]="servoc";
constexpr char CAM[]="192.168.4.2";

// Drive
constexpr uint8_t SERVO_L=18,SERVO_R=19;
constexpr uint32_t FAILSAFE=1000;
constexpr float ACCEL=500,DECEL=800,REVERSE=1000;

// I2C
constexpr uint8_t SDA=21,SCL=22;

// MQ relay groups
constexpr uint8_t RELAY1=25,RELAY2=26,RELAY3=23;

// Future 16-channel analog MUX
constexpr uint8_t MUX_S0=13,MUX_S1=14,MUX_S2=16,MUX_S3=17;

// MQ
struct MQ{
  const char* name;
  uint8_t pin,row;
  float div,r0;
};

constexpr MQ mq[]={
  {"mq2",34,1,1.000,10.0},
  {"mq3",39,2,1.000,10.0},
  {"mq4",33,1,1.000,10.0},
  {"mq7",32,2,1.000,10.0},
  {"mq8",36,2,1.000,10.0},
  {"mq135",35,1,1.729,10.0}
};

constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

// Measurement
constexpr float MQ_VCC=5.0,RL=10.0;
constexpr uint16_t MQ_PERIOD=100,SAMPLE_PERIOD=200;
constexpr uint8_t ADC_SAMPLES=8;

}

struct Settings{
  String cam;
  int speed=100,nL=1500,nR=1500;
  uint16_t warm=60,measure=30;
};
