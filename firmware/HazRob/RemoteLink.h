#pragma once
#include <Arduino.h>

void remoteBegin();
void remoteUpdate();
bool remoteProvision(const String& ssid,const String& password);
void remoteClear();
String remoteStatusJson();
String remoteSsid();
bool remoteInternetReady();
