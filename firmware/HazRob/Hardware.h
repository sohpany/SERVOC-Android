#pragma once
#include "Config.h"

extern Settings settings;

struct SettingsPatch{
  bool hasCam=false,hasSpeed=false,hasWarm=false,hasMeasure=false;
  String cam;
  int speed=0,warm=0,measure=0;
};

// Earliest setup-stage relay guard: write the inactive level before GPIO25 becomes an output,
// then assert it again after direction change. The external 3.3V/5V level translator pull-up
// remains responsible for holding Relay IN inactive during the reset/ROM window before setup().
inline void relayBootSafeInit(){digitalWrite(Cfg::RELAY_MQ,Cfg::RELAY_OFF);pinMode(Cfg::RELAY_MQ,OUTPUT);digitalWrite(Cfg::RELAY_MQ,Cfg::RELAY_OFF);}
void hwBegin(),hwUpdate();

void driveSet(int left,int right);
void motionStop(),emergencyStop();

bool relaySetMask(uint8_t on);
void sensorPowerOff();
bool measureStart();
void measureCancel();

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
