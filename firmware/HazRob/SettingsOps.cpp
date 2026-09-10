#include "Hardware.h"

void applySettings(const SettingsPatch& patch,bool persist){
  if(patch.hasCam&&patch.cam.length())settings.cam=patch.cam;
  if(patch.hasSpeed)settings.speed=constrain((long)patch.speed,20L,100L);
  if(patch.hasWarm)settings.warm=(uint16_t)constrain((long)patch.warm,0L,3600L);
  if(patch.hasMeasure)settings.measure=(uint16_t)constrain((long)patch.measure,5L,600L);
  if(persist)saveSettings();
}
