#pragma once
#include "Config.h"

extern Settings settings;

void hwBegin(),hwUpdate();

void driveSet(int left,int right);
void motionStop(),emergencyStop();

bool relaySetMask(uint8_t on);
void sensorPowerOff();
void measureStart(),measureCancel();

void linkHeartbeat();
void linkSetArmed(bool armed);

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
