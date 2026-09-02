#pragma once
#include "Config.h"

extern Settings settings;

void hwBegin();
void hwUpdate();

void driveSet(int left,int right);
void stopAll();

void relaySetMask(uint8_t mask);
void measureStart();
void measureCancel();

void saveSettings();

String dataJson();
String diagJson();
String settingsJson();
