#pragma once
#include <Arduino.h>

namespace Cfg{
constexpr char FW[]="HazRob 0.10.2";
constexpr char ENGINEERING_REVISION[]="HR069-R1";
constexpr uint8_t API=4;
constexpr char HW_PROFILE[]="hazrob-0.10.2-hr069-1r8mq-l298n6-batt-ultra-ir-ldr-env2-v2";
constexpr char SSID[]="HazRob",NVS[]="servoc",CAM[]="192.168.4.2";

// L298N 4WD differential drive: two motors per physical side in parallel.
// Field behavior confirmed reversed steering handedness while straight forward/reverse remained correct.
// Logical LEFT therefore maps to L298N channel B and logical RIGHT to channel A, while both
// direction inversions remain false.
constexpr uint8_t DRIVE_L_EN=17,DRIVE_L_IN1=19,DRIVE_L_IN2=32;
constexpr uint8_t DRIVE_R_EN=16,DRIVE_R_IN1=18,DRIVE_R_IN2=27;
constexpr bool DRIVE_L_INVERT=false,DRIVE_R_INVERT=false;
constexpr uint32_t CONTROL_COMMAND_TIMEOUT_MS=500,ADS_REPROBE_MS=3000;

// The analytical ADC bus is separated from the environmental bus so BME/BMP faults do not
// electrically or logically share the ADS1/ADS2 I2C path. Both use ESP32 hardware I2C controllers.
constexpr uint8_t SDA=21,SCL=22,ADS1_ADDR=0x48,ADS2_ADDR=0x49;
constexpr uint8_t ENV_SDA=13,ENV_SCL=14;
constexpr uint32_t ADS_I2C_HZ=400000,ENV_I2C_HZ=100000;
constexpr uint8_t RELAY_MQ=25,RELAY_ON=LOW,RELAY_OFF=HIGH;

// Discrete / device-health channels.
// ALERT_PIN is a logic-level control output only. Never connect an 8-ohm speaker directly to ESP32.
constexpr uint8_t ALERT_PIN=26;
constexpr bool ALERT_OUTPUT_INSTALLED=false;
constexpr uint8_t ALERT_ACTIVE=HIGH,ALERT_INACTIVE=LOW;
constexpr uint8_t SOUND_PIN=36;          // MAX4466 ADC1
constexpr uint8_t IR_PIN=35;             // downward-looking lower-front ground/cliff sensor
constexpr uint8_t LDR_PIN=34;            // LM393 digital light-threshold output; NOT quantitative lux
constexpr uint8_t BATTERY_PIN=33;        // ADC1, divider required
constexpr uint8_t ULTRA_TRIG_PIN=23;      // front ultrasonic trigger
constexpr uint8_t ULTRA_ECHO_PIN=39;      // input only; reduce 5 V echo to <=3.3 V

// The configured active level represents GROUND PRESENT, not an obstacle-ahead event.
// Operator-calibrated MH-B output: HIGH = ground present; LOW = ground absent/cliff.
// There is no automatic validation of sensor wiring or calibration.
// Verify the detector manually before enabling automatic forward-stop.
constexpr uint8_t IR_GROUND_ACTIVE=HIGH;
constexpr bool IR_FAIL_SAFE_STARTUP=true;
constexpr uint8_t IR_CLIFF_ASSERT_SAMPLES=3;
constexpr uint8_t IR_CLIFF_CLEAR_SAMPLES=2;
// Digital IR is calibrated physically at the installed downward reference height; it does not measure distance.
constexpr float IR_REFERENCE_HEIGHT_CM=4.0f;

// Future quantitative illuminance: prefer BH1750 on an explicitly assigned bus after installation.
constexpr bool LUX_SENSOR_INSTALLED=false;

// Device-specific field clean-air normalization baseline accepted from three independent
// 30.0 s sessions on 2026-09-13 (73 + 70 + 73 = 216 samples). Each session is first
// aggregated as the geometric mean of Rs, then the three session means receive equal
// weight in log space. Field wind suitability is controlled protocol context; T/RH/P
// were unavailable from the onboard ENV channel and no environmental correction is claimed.
// This is NOT manufacturer/reference-gas validation for quantitative ppm curves.
constexpr char R0_SOURCE[]="field_clean_air_r0";
constexpr char R0_SESSION[]="FIELD-R0-3SESSION-20260913";
constexpr char R0_UTC[]="2026-09-13T01:00:47Z";
constexpr uint16_t R0_SAMPLES=216;
constexpr uint8_t R0_SESSIONS=3;
constexpr bool R0_REFERENCE_GAS_VALIDATED=false;
constexpr bool R0_ENVIRONMENT_COMPENSATED=false;

struct MQ{const char* name;uint8_t ads,ch;float r0;};
constexpr MQ mq[]={
  {"mq2",0,0,91.734024f},{"mq4",0,1,65.269585f},{"mq5",0,2,251.964722f},{"mq8",0,3,54.888683f},
  {"mq3",1,0,20.686031f},{"mq7",1,1,160.601349f},{"mq9",1,2,69.990082f},{"mq135",1,3,213.239044f}
};
constexpr uint8_t MQ_N=sizeof(mq)/sizeof(mq[0]);

// Software acquisition is configured for ADS1115 VDD=5 V, PGA +/-6.144 V, 860 SPS,
// and four conversions per channel. Physical acceptance must confirm the assembled ADS supply domain;
// a measured 3.3 V I2C logic level alone does not prove ADS VDD. Do not change this conversion
// assumption without a controlled physical measurement and corresponding recalculation review.
constexpr uint16_t ADS_RATE_SPS=860;
constexpr float ADS_FULL_SCALE_V=6.144f,ADS_SUPPLY_V=5.0f,MQ_VCC=5.0f,RL=10.0f;
constexpr uint16_t MQ_PERIOD=100,SAMPLE_PERIOD=200,OPERATIONAL_WARMUP_SEC=60,ANALYSIS_WINDOW_SEC=30;
constexpr uint8_t ADC_SAMPLES=4;
constexpr bool TRACKING_CONTINUOUS=true;

// Compact device-health timing / conversion constants.
constexpr uint16_t SOUND_PERIOD=100,SOUND_SAMPLES=128,ENV_PERIOD=1000,IR_PERIOD=50,LDR_PERIOD=100;
constexpr uint16_t BATTERY_PERIOD=1000,ULTRA_PERIOD=150;
constexpr uint32_t ULTRA_TIMEOUT_US=25000;
constexpr float ULTRA_WARN_CM=10.0f;
constexpr float ULTRA_STOP_CM=5.0f;
// 47k/10k divider reference. Final assembled divider and ADC correction are calibrated in device settings later.
constexpr float BAT_R_TOP=47000.0f,BAT_R_BOTTOM=10000.0f,BAT_DIV=(BAT_R_TOP+BAT_R_BOTTOM)/BAT_R_BOTTOM;
}

struct Settings{String cam;int speed=100;uint16_t warm=Cfg::OPERATIONAL_WARMUP_SEC,measure=Cfg::ANALYSIS_WINDOW_SEC;};
