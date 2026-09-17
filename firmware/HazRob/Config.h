#pragma once
#include <Arduino.h>

namespace Cfg{
constexpr char FW[]="HazRob 0.10.1 HR065";
constexpr char BUILD_ID[]="HR-0101-065-MAIN";
constexpr uint8_t API=4;
constexpr char HW_PROFILE[]="hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2";
constexpr char SSID[]="HazRob",NVS[]="servoc",CAM[]="192.168.4.2";

// L298N 4WD differential drive: two motors per physical side in parallel.
// HR048 controlled field correction: logical LEFT -> L298N channel B, logical RIGHT -> channel A.
constexpr uint8_t DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32;
constexpr uint8_t DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27;
constexpr bool DRIVE_L_INVERT=false,DRIVE_R_INVERT=false;
constexpr uint32_t CONTROL_COMMAND_TIMEOUT_MS=500,ADS_REPROBE_MS=3000;

constexpr uint8_t SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49;
constexpr uint8_t ENV_SDA=13,ENV_SCL=14;
constexpr uint32_t ADS_I2C_HZ=400000,ENV_I2C_HZ=100000;
constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;
constexpr uint8_t ALERT_PIN=26;
constexpr bool ALERT_OUTPUT_INSTALLED=false;
constexpr uint8_t ALERT_ACTIVE=HIGH,ALERT_INACTIVE=LOW;
constexpr uint8_t SOUND_PIN=36;
constexpr uint8_t IR_PIN=35;
constexpr uint8_t LDR_PIN=34;
constexpr uint8_t BATTERY_PIN=33;
constexpr uint8_t ULTRA_TRIG_PIN=23,ULTRA_ECHO_PIN=39;
constexpr float BATTERY_DIVIDER_RATIO=3.00f;
constexpr uint16_t PWM_FREQ=20000,PWM_BITS=8;
constexpr uint8_t PWM_MAX=255;
}
