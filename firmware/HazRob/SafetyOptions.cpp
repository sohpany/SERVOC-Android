#include "SafetyOptions.h"
#include <Preferences.h>

namespace{
Preferences safetyNv;
bool loaded=false;

void ensureLoaded(){
  if(loaded)return;
  // Automatic IR/ultrasonic motion stopping is intentionally disabled for HR069 RC2.
  // Hazard warnings remain active, while E-STOP and the stale-command watchdog remain independent.
  // Force the persisted state OFF as well so an older saved autoStop=true value cannot re-enable it
  // after flashing this firmware.
  safetyNv.begin("hazsafe",false);
  safetyNv.putBool("autoStop",false);
  loaded=true;
}
}

bool safetyAutoStopEnabled(){
  ensureLoaded();
  return false;
}

void safetyAutoStopSet(bool enabled,bool persist){
  (void)enabled;
  ensureLoaded();
  // Compatibility endpoint retained for the Android API, but automatic stopping stays disabled.
  if(persist)safetyNv.putBool("autoStop",false);
}
