#pragma once
#include "Config.h"

extern Settings settings;

void hwBegin();
void hwUpdate();

void driveSet(int left,int right);
void motionStop();
void emergencyStop();

bool relaySetMask(uint8_t mask);
void sensorPowerOff();
void measureStart();
void measureCancel();

void linkHeartbeat();
void linkSetArmed(bool armed);

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
