#pragma once
#include <Arduino.h>

namespace Cfg {

// WiFi / camera
constexpr char SSID[]="HazRob";
constexpr char NVS[]="servoc";          // keep previous settings compatible
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

// 4-channel active-LOW relay board. Each relay powers one MQ row (2 sensors).
constexpr uint8_t RELAY1=25,RELAY2=26,RELAY3=27,RELAY4=32;
constexpr uint8_t RELAY_ON=LOW,RELAY_OFF=HIGH;
constexpr uint8_t MQ_ROWS=4;
constexpr uint8_t MAX_ACTIVE_ROWS=2;     // 5 V / 4 A temporary power budget

// Measurement scans four rows as two power phases (4 MQ sensors powered per phase).
constexpr uint8_t MEASURE_PHASES=2;
constexpr uint8_t MEASURE_MASKS[MEASURE_PHASES]={0x03,0x0C}; // R1+R2, then R3+R4

// Local sensors
constexpr uint8_t SOUND_PIN=36; // MAX4466 OUT, ADC1
constexpr uint8_t IR_PIN=35;    // digital warning only
constexpr uint8_t LDR_PIN=34;   // optional digital LDR module
constexpr uint8_t AUX1=33,AUX2=39;
constexpr uint8_t IR_ACTIVE=LOW;
constexpr uint8_t LDR_ACTIVE=LOW;

// MQ via 2 x ADS1115. ads: 0=0x48, 1=0x49; ch: A0..A3; row: relay row.
// Physical mapping:
// R1: MQ4/MQ5 | R2: MQ7/MQ9 | R3: MQ2/MQ8 | R4: MQ3/MQ135.
struct MQ{
  const char* name;
  uint8_t ads,ch,row;
  float r0;
};

constexpr MQ mq[]={
  {"mq4",   0,0,1,10.0f},
  {"mq5",   0,1,1,10.0f},
  {"mq7",   0,2,2,10.0f},
  {"mq9",   0,3,2,10.0f},
  {"mq2",   1,0,3,10.0f},
  {"mq8",   1,1,3,10.0f},
  {"mq3",   1,2,4,10.0f},
  {"mq135", 1,3,4,10.0f}
};
constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

// Acquisition. MQ signals are slow; one ADS conversion per round-robin slot is sufficient
// and leaves the WebServer/servo loop responsive.
constexpr float MQ_VCC=5.0f,RL=10.0f;
constexpr uint16_t MQ_PERIOD=160,SAMPLE_PERIOD=200;
constexpr uint8_t ADC_SAMPLES=1;
constexpr uint16_t ENV_PERIOD=1000;
constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=96;
constexpr uint16_t IR_PERIOD=50,LDR_PERIOD=100;

}

struct Settings{
  String cam;
  int speed=100,nL=1500,nR=1500;
  uint16_t warm=60,measure=30;
};
