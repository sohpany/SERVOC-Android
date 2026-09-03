#pragma once
#include <Arduino.h>

using RemoteCaptureFn=bool (*)(uint8_t* out,size_t cap,size_t& len);

void remoteVideoBegin(RemoteCaptureFn capture);
void remoteVideoStop();
bool remoteVideoConnected();
String remoteVideoJson();
