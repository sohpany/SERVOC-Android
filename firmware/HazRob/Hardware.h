#pragma once
#include "Config.h"

extern Settings settings;

void hwBegin();
void hwUpdate();

void driveSet(int left,int right);
void stopAll();

void relaySet(uint8_t group);
void measureStart();
void measureCancel();

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
