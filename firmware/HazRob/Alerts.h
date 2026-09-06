#pragma once
#include <Arduino.h>

namespace Alerts {
enum Level : uint8_t {
  NONE=0,
  TEST=1,
  LINK=2,
  IR=3,
  GAS_WARNING=4,
  GAS_DANGER=5,
  ESTOP=6
};

void begin();
void update();
void setIr(bool active);
void setLink(bool active);
void setGas(uint8_t severity,uint32_t ttlMs=3000);
void emergencyStop();
void test();
void clearTransient();
Level level();
const char* name();
String json();
}
