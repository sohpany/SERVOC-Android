#pragma once
#include <Arduino.h>

namespace Cfg{
constexpr char FW[]="HazRob 0.10.0";
constexpr uint8_t API=4;
constexpr char HW_PROFILE[]="hazrob-0.9.3-1r8mq-alert-ldr-l298n-v1"; // physical wiring revision is unchanged
constexpr char SSID[]="HazRob",NVS[]="servoc",CAM[]="192.168.4.2";

// L298N 4WD: two motors per side in parallel; ENA/ENB enabled, PWM on direction inputs.
constexpr uint8_t DRIVE_L_IN1=18,DRIVE_L_IN2=27,DRIVE_R_IN1=19,DRIVE_R_IN2=32;
constexpr bool DRIVE_L_INVERT=false,DRIVE_R_INVERT=false;
constexpr uint32_t DRIVE_LEASE_MS=450,LINK_DETECT_MS=900,LINK_HOLD_MS=3000,RETRACE_MS=60000,ADS_REPROBE_MS=3000;
constexpr uint32_t FAILSAFE=DRIVE_LEASE_MS,LINK_LOSS_MS=LINK_DETECT_MS;
constexpr float ACCEL=500.0f,DECEL=800.0f,REVERSE=1000.0f;
constexpr int RETRACE_MAX_SPEED=40;

constexpr uint8_t SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49;
constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;
constexpr uint8_t ALERT_PIN=26,SOUND_PIN=36,IR_PIN=35,LDR_PIN=34,AUX1=33,AUX3=39,AUX2=LDR_PIN;
constexpr uint8_t IR_ACTIVE=LOW;

struct MQ{const char* name;uint8_t ads,ch;float r0;};
constexpr MQ mq[]={
  {"mq2",0,0,10.0f},{"mq4",0,1,10.0f},{"mq5",0,2,10.0f},{"mq8",0,3,10.0f},
  {"mq3",1,0,10.0f},{"mq7",1,1,10.0f},{"mq9",1,2,10.0f},{"mq135",1,3,10.0f}
};
constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

// ADS1115 is fixed at 5 V; PGA ±6.144 V, 860 SPS, four conversions/channel.
constexpr uint16_t ADS_RATE_SPS=860;
constexpr float ADS_FULL_SCALE_V=6.144f,ADS_SUPPLY_V=5.0f,MQ_VCC=5.0f,RL=10.0f;
constexpr uint16_t MQ_PERIOD=100,SAMPLE_PERIOD=200,OPERATIONAL_WARMUP_SEC=60,ANALYSIS_WINDOW_SEC=30;
constexpr uint8_t ADC_SAMPLES=4;
constexpr bool TRACKING_CONTINUOUS=true;
constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=128,ENV_PERIOD=1000,IR_PERIOD=50,LDR_PERIOD=100;
}

struct Settings{String cam;int speed=100;uint16_t warm=Cfg::OPERATIONAL_WARMUP_SEC,measure=Cfg::ANALYSIS_WINDOW_SEC;};
