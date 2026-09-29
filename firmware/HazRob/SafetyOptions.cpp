#include "SafetyOptions.h"
#include <Preferences.h>

namespace{
Preferences safetyNv;
bool loaded=false;
bool autoStop=false;

void ensureLoaded(){
  if(loaded)return;
  // Separate namespace avoids interfering with the main settings Preferences handle.
  safetyNv.begin("hazsafe",false);
  autoStop=safetyNv.getBool("autoStop",false); // build-stage default: warnings ON, automatic stop OFF
  loaded=true;
}
}

bool safetyAutoStopEnabled(){ensureLoaded();return autoStop;}

void safetyAutoStopSet(bool enabled,bool persist){
  ensureLoaded();
  autoStop=enabled;
  if(persist)safetyNv.putBool("autoStop",enabled);
}
