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

bool guestAccessSet(const String& token);
void guestAccessRevoke();
bool guestAccessValid(const String& token);
bool guestAccessEnabled();

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
