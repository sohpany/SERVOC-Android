#pragma once
#include <Arduino.h>

void remoteMqttBegin();
void remoteMqttUpdate();
void remoteMqttDisconnect();
bool remoteMqttConnected();
