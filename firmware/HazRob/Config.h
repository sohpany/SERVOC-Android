#pragma once
#include <Arduino.h>

namespace Cfg{
constexpr char FW[]="HazRob 0.10.0";
constexpr uint8_t API=4;
constexpr char HW_PROFILE[]="hazrob-0.10-1r8mq-l298n6-batt-ultra-temp-v1";
constexpr char SSID[]="HazRob",NVS[]="servoc",CAM[]="192.168.4.2";

// L298N 4WD differential drive: two motors per side in parallel.
// ENA/ENB are dedicated PWM speed pins; IN1..IN4 are direction only.
constexpr uint8_t DRIVE_L_EN=16,DRIVE_L_IN1=18,DRIVE_L_IN2=27;
constexpr uint8_t DRIVE_R_EN=17,DRIVE_R_IN1=19,DRIVE_R_IN2=32;
constexpr bool DRIVE_L_INVERT=false,DRIVE_R_INVERT=false;
constexpr uint32_t DRIVE_LEASE_MS=450,LINK_DETECT_MS=900,LINK_HOLD_MS=3000,RETRACE_MS=60000,ADS_REPROBE_MS=3000;
constexpr float ACCEL=500.0f,DECEL=800.0f,REVERSE=1000.0f;
constexpr int RETRACE_MAX_SPEED=40;

// Shared analytical I2C bus: ADS1 + ADS2 + chamber BME/BMP.
constexpr uint8_t SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49;
constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;

// Discrete / device-health channels.
constexpr uint8_t ALERT_PIN=26;          // reserved 8-ohm speaker alert signal; safe interface required before installation
constexpr uint8_t SOUND_PIN=36;          // MAX4466 ADC1
constexpr uint8_t IR_PIN=35;             // lower-front fall-edge warning
constexpr uint8_t LDR_PIN=34;            // LM393 digital threshold
constexpr uint8_t BATTERY_PIN=33;        // ADC1, divider required
constexpr uint8_t ULTRA_TRIG_PIN=23;      // front ultrasonic trigger
constexpr uint8_t ULTRA_ECHO_PIN=39;      // input only; reduce 5 V echo to <=3.3 V
constexpr uint8_t INTERNAL_TEMP_PIN=14;   // compact one-wire internal/battery temperature input (DS18B20 default)
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

// Compact device-health timing / conversion constants.
constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=128,ENV_PERIOD=1000,IR_PERIOD=50,LDR_PERIOD=100;
constexpr uint16_t BATTERY_PERIOD=1000,ULTRA_PERIOD=150,INTERNAL_TEMP_PERIOD=1500;
constexpr uint32_t ULTRA_TIMEOUT_US=25000;
constexpr float ULTRA_WARN_CM=35.0f;
// 47k/10k divider reference. Final assembled divider and ADC correction are calibrated in device settings later.
constexpr float BAT_R_TOP=47000.0f,BAT_R_BOTTOM=10000.0f,BAT_DIV=(BAT_R_TOP+BAT_R_BOTTOM)/BAT_R_BOTTOM;
}

struct Settings{String cam;int speed=100;uint16_t warm=Cfg::OPERATIONAL_WARMUP_SEC,measure=Cfg::ANALYSIS_WINDOW_SEC;};
