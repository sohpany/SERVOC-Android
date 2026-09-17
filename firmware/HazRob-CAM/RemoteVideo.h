#pragma once
#include <Arduino.h>

using RemoteCaptureFn=bool(*)(uint8_t*,size_t,size_t&);

void remoteVideoBegin(RemoteCaptureFn);
void remoteVideoStop();
void remoteVideoSetLocalActive(bool);
bool remoteVideoConnected();
String remoteVideoJson();