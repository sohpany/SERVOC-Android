#pragma once
#include <Arduino.h>

namespace Cfg {

// WiFi / camera
constexpr char SSID[]="HazRob";
constexpr char NVS[]="servoc";
constexpr char CAM[]="192.168.4.2";

// Drive - 2 x MG995 continuous rotation
constexpr uint8_t SERVO_L=18;
constexpr uint8_t SERVO_R=19;
constexpr uint32_t FAILSAFE=1000;
constexpr float ACCEL=500.0f;
constexpr float DECEL=800.0f;
constexpr float REVERSE=1000.0f;
constexpr uint32_t RETRACE_MS=60000;
constexpr int RETRACE_MAX_SPEED=40;

// I2C
constexpr uint8_t SDA=21;
constexpr uint8_t SCL=22;
constexpr uint8_t ADS1_ADDR=0x48;
constexpr uint8_t ADS2_ADDR=0x49;

// 4-channel relay board - active LOW
// R1 = installed MQ group 1
// R2 = installed MQ group 2
// R3/R4 = reserved
constexpr uint8_t RELAY1=25;
constexpr uint8_t RELAY2=26;
constexpr uint8_t RELAY3=27;
constexpr uint8_t RELAY4=32;
constexpr uint8_t RELAY_ON=LOW;
constexpr uint8_t RELAY_OFF=HIGH;
constexpr uint8_t MQ_ROWS=2;
constexpr uint8_t MQ_INSTALLED_MASK=0x03;
constexpr uint8_t MAX_ACTIVE_MQ_GROUPS=2;

// Local sensors
constexpr uint8_t SOUND_PIN=36; // MAX4466 analog microphone
constexpr uint8_t IR_PIN=35;    // proximity/obstacle warning only
constexpr uint8_t AUX1=33;
constexpr uint8_t AUX2=34;
constexpr uint8_t AUX3=39;
constexpr uint8_t IR_ACTIVE=LOW;

// MQ via 2 x ADS1115
// ads: 0 = ADS1115 #1 @ 0x48, 1 = ADS1115 #2 @ 0x49
// ch : 0..3 = A0..A3
// row: 1 = Relay R1, 2 = Relay R2
struct MQ{
  const char* name;
  uint8_t ads;
  uint8_t ch;
  uint8_t row;
  float r0;
};

constexpr MQ mq[]={
  // R1 / ADS1115 #1 @ 0x48
  {"mq2",   0,0,1,10.0f},
  {"mq4",   0,1,1,10.0f},
  {"mq5",   0,2,1,10.0f},
  {"mq8",   0,3,1,10.0f},

  // R2 / ADS1115 #2 @ 0x49
  {"mq3",   1,0,2,10.0f},
  {"mq7",   1,1,2,10.0f},
  {"mq9",   1,2,2,10.0f},
  {"mq135", 1,3,2,10.0f}
};
constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

// MQ / ADC
constexpr float MQ_VCC=5.0f;
constexpr float RL=10.0f;
constexpr uint16_t MQ_PERIOD=100;
constexpr uint16_t SAMPLE_PERIOD=200;
constexpr uint8_t ADC_SAMPLES=4;

// Sound
constexpr uint16_t SOUND_PERIOD=100;
constexpr uint16_t SOUND_SAMPLES=128;

// Environment
constexpr uint16_t ENV_PERIOD=1000;

// IR
constexpr uint16_t IR_PERIOD=50;

}

struct Settings{
  String cam;
  int speed=100;
  int nL=1500;
  int nR=1500;
  uint16_t warm=60;
  uint16_t measure=30;
};
