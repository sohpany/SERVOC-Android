#pragma once
#include "Config.h"

extern Settings settings;

struct SettingsPatch{
  bool hasCam=false,hasSpeed=false,hasWarm=false,hasMeasure=false;
  String cam;
  int speed=0,warm=0,measure=0;
};

// Earliest setup-stage relay guard: preload OFF before enabling GPIO25 output.
// Hardware level translation/pull-up remains responsible for the pre-setup reset/ROM window.
void relayBootSafeInit();
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
void applySettings(const SettingsPatch& patch,bool persist=true);

String dataJson();
String diagJson();
String settingsJson();