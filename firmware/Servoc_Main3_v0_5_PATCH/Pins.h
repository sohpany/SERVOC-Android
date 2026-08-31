#pragma once
#include <Arduino.h>

namespace Pins {

// =====================================================
// DRIVE
// =====================================================

constexpr uint8_t SERVO_LEFT  = 18;
constexpr uint8_t SERVO_RIGHT = 19;


// =====================================================
// I2C
// =====================================================

constexpr uint8_t I2C_SDA = 21;
constexpr uint8_t I2C_SCL = 22;


// =====================================================
// SENSOR POWER ROWS
// صف واحد فقط يعمل في كل لحظة
// =====================================================

constexpr uint8_t RELAY_GROUP1 = 25;
constexpr uint8_t RELAY_GROUP2 = 26;
constexpr uint8_t RELAY_GROUP3 = 23;


// =====================================================
// CURRENT DIRECT MQ INPUTS
//
// مؤقتة حتى الانتقال إلى Analog MUX.
// =====================================================

constexpr uint8_t MQ7_ADC   = 32;
constexpr uint8_t MQ4_ADC   = 33;
constexpr uint8_t MQ2_ADC   = 34;
constexpr uint8_t MQ135_ADC = 35;
constexpr uint8_t MQ8_ADC   = 36;
constexpr uint8_t MQ3_ADC   = 39;


// =====================================================
// RESERVED FOR FUTURE ANALOG MUX
//
// لا تستخدم الآن.
// المقترح:
// S0 = 13
// S1 = 14
// S2 = 16
// S3 = 17
//
// SIG سيستخدم ADC1 بعد تحويل الحساسات إلى MUX.
// =====================================================

constexpr uint8_t MUX_S0 = 13;
constexpr uint8_t MUX_S1 = 14;
constexpr uint8_t MUX_S2 = 16;
constexpr uint8_t MUX_S3 = 17;

}