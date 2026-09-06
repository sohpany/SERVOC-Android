#pragma once
#include "Config.h"

extern Settings settings;

void hwBegin(),hwUpdate();

void driveSet(int left,int right);
void motionStop(),emergencyStop();

bool relaySetMask(uint8_t on);
void sensorPowerOff();
void measureStart(),measureCancel();

// App-originated gas-response alerts: 0=clear, 1=warning, 2=danger.
bool alertSet(uint8_t severity,uint32_t ttlMs=3000);
void alertTest();

void linkHeartbeat();
void linkSetArmed(bool armed);

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
