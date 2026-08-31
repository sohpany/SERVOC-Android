#pragma once
#include <Arduino.h>
#include "GasAnalyzer.h"
#include "MQArray.h"

// Core
void sensorsBegin();
void sensorsUpdate();
String sensorsJson();

// Environmental I2C sensor (BME280/BMP280)
bool sensorsBmePresent(); // compatibility alias: true for a recognized working BME280 or BMP280
bool sensorsEnvPresent();
bool sensorsEnvI2cFound();
const char* sensorsEnvType();
uint8_t sensorsEnvAddress();
uint8_t sensorsEnvChipId();
bool sensorsEnvHumidityAvailable();
float sensorsTemperature();
float sensorsHumidity();
float sensorsPressure();

// MQ
const MQReading* sensorsMq(MQSensor sensor);

// Gas analysis
bool sensorsGasAnalysisReady();
bool sensorsBestGas(GasEstimate& out);
String sensorsGasJson();

// Measurement session
void sensorsMeasurementStart();
void sensorsMeasurementCancel();
bool sensorsMeasurementRunning();
bool sensorsMeasurementReady();
String sensorsMeasurementJson();