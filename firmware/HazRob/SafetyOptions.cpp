#include "SafetyOptions.h"
#include <Preferences.h>

namespace {
Preferences safetyNv;
bool loaded=false;
bool autoStop=false;
void ensureLoaded(){
  if(loaded)return;
  safetyNv.begin("hazsafe",false);
  // Explicit operator option. Default remains OFF for first-time installs.
  autoStop=safetyNv.getBool("autoStop",false);
  loaded=true;
}
}

bool safetyAutoStopEnabled(){
  ensureLoaded();
  return autoStop;
}

void safetyAutoStopSet(bool enabled,bool persist){
  ensureLoaded();
  autoStop=enabled;
  if(persist)safetyNv.putBool("autoStop",enabled);
}
