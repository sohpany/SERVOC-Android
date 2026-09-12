#pragma once
#include <Arduino.h>

void remoteBegin();
void remoteUpdate();
bool remoteProvision(const String& ssid,const String& password);
void remoteClear();
String remoteStatusJson();
String remoteSsid();
bool remoteInternetReady();
void remoteSetCloudState(bool tls,bool mqtt,bool commandAck,bool telemetry,bool camera);
void remoteSetCloudDetail(const String& detail);
