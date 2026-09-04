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

bool guestAccessSet(const String& token);
void guestAccessRevoke();
bool guestAccessValid(const String& token);
bool guestAccessEnabled();

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
