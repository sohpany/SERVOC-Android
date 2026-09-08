#pragma once
#include <Arduino.h>

namespace Cfg{
constexpr char FW[]="HazRob 0.9.3-rc2";
constexpr uint8_t API=4;
constexpr char HW_PROFILE[]="hazrob-0.9.3-1r8mq-alert-ldr-l298n-v1";

constexpr char SSID[]="HazRob";
constexpr char NVS[]="servoc";
constexpr char CAM[]="192.168.4.2";

// L298N 4WD differential drive.
// ENA/ENB remain enabled on the L298N module; speed PWM is applied directly
// to the direction inputs so only four ESP32 GPIO outputs are required.
// Two motors on each side are wired in parallel to the corresponding bridge.
constexpr uint8_t DRIVE_L_IN1=18,DRIVE_L_IN2=27;
constexpr uint8_t DRIVE_R_IN1=19,DRIVE_R_IN2=32;
// If one side runs opposite to the intended command after final wiring,
// change only the corresponding inversion flag instead of remapping the API.
constexpr bool DRIVE_L_INVERT=false,DRIVE_R_INVERT=false;

// Motion/link safety: stale drive command stops motion quickly. If the whole link
// is absent, hold stopped for 3 s to permit recovery; only then power MQ off and retrace.
constexpr uint32_t DRIVE_LEASE_MS=450,LINK_DETECT_MS=900,LINK_HOLD_MS=3000;
constexpr uint32_t RETRACE_MS=60000,ADS_REPROBE_MS=3000;
// Compatibility aliases used by older modules.
constexpr uint32_t FAILSAFE=DRIVE_LEASE_MS,LINK_LOSS_MS=LINK_DETECT_MS;
constexpr float ACCEL=500.0f,DECEL=800.0f,REVERSE=1000.0f;
constexpr int RETRACE_MAX_SPEED=40;

constexpr uint8_t SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49;

// One active-LOW relay powers all eight MQ heaters/sensors.
constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;

// GPIO26 drives only the alert signal. A dynamic low-impedance speaker must use
// an external transistor/driver stage; do not connect a 4/8-ohm speaker directly.
// Alert patterns are non-blocking at the application state-machine level.
constexpr uint8_t ALERT_PIN=26;
constexpr uint8_t SOUND_PIN=36,IR_PIN=35,LDR_PIN=34,AUX1=33,AUX3=39;
constexpr uint8_t AUX2=LDR_PIN; // compatibility alias for the previous profile.
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

constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=128,ENV_PERIOD=1000,IR_PERIOD=50,LDR_PERIOD=100;
}

struct Settings{
  String cam;
  int speed=100;
  uint16_t warm=Cfg::OPERATIONAL_WARMUP_SEC,measure=Cfg::ANALYSIS_WINDOW_SEC;
};
