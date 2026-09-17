#pragma once
#include <Arduino.h>

void remoteBegin();
void remoteUpdate();
bool remoteProvision(const String& ssid,const String& password);
bool remoteRetry();
void remoteClear();
void remoteSetTransportEnabled(bool enabled,bool persistMode);
bool remoteTransportEnabled();
bool remoteConfigured();
bool remoteDeveloperAuthorized();
void remoteSetDeveloperAuthorized(bool enabled);
String remoteStatusJson();
String remoteSsid();
bool remoteInternetReady();
void remoteSetCloudState(bool tls,bool mqtt,bool commandAck,bool telemetry,bool camera);
void remoteSetCloudDetail(const String& detail);
