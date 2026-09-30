#include "SafetyOptions.h"
#include <Preferences.h>

namespace{
Preferences safetyNv;
bool loaded=false;
bool autoStop=true;

void ensureLoaded(){
  if(loaded)return;
  // Separate namespace avoids interfering with the main settings Preferences handle.
  // Safe boot default: warnings remain visible and forward motion is blocked by
  // confirmed cliff/no-ground or critical front-obstacle conditions. A developer
  // may explicitly disable auto-stop for controlled maintenance/testing and that
  // choice is persisted, but an unconfigured robot boots fail-safe.
  safetyNv.begin("hazsafe",false);
  autoStop=safetyNv.getBool("autoStop",true);
  loaded=true;
}
}

bool safetyAutoStopEnabled(){ensureLoaded();return autoStop;}

void safetyAutoStopSet(bool enabled,bool persist){
  ensureLoaded();
  autoStop=enabled;
  if(persist)safetyNv.putBool("autoStop",enabled);
}
