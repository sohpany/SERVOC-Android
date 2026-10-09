#!/usr/bin/env python3
"""Host C++ simulation of actual HR069-R1 firmware Echo/IR/ENV/drive functions.

Extracts the C++ function bodies verbatim from firmware/HazRob/Hardware.cpp and
compiles them with deterministic Arduino/ESP32 peripheral stubs on CI.
Tests here are SOFTWARE ONLY, not physical voltage, sensor accuracy, timing on
an ESP32, BLE/Wi-Fi performance, or field safety acceptance.
"""
from __future__ import annotations

import pathlib
import re
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "firmware/HazRob/Hardware.cpp").read_text(encoding="utf-8")
CONFIG = (ROOT / "firmware/HazRob/Config.h").read_text(encoding="utf-8")


def firmware_function(name: str) -> str:
    match = re.search(r"(?m)^void(?:\s+IRAM_ATTR)?\s+" + re.escape(name) + r"\s*\([^)]*\)\s*\{", SOURCE)
    if not match:
        raise AssertionError(f"Firmware function not found: {name}")
    start = match.start()
    begin = SOURCE.index("{", match.start())
    depth = 0
    for index in range(begin, len(SOURCE)):
        ch = SOURCE[index]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return SOURCE[start:index + 1]
    raise AssertionError(f"Unbalanced braces: {name}")


def must_have(text: str, required: str, label: str) -> None:
    if required not in text:
        raise AssertionError(f"{label}: missing {required!r}")


def main() -> None:
    for marker in [
        "ENV_SDA=13,SCL=14",  # intentionally handled by explicit checks below
    ]:
        pass
    must_have(CONFIG, "ENV_SDA=13,ENV_SCL=14", "direct GPIO13/14 environmental bus")
    must_have(CONFIG, "ENV_PERIOD=1000", "one-second real BME sampling")
    must_have(CONFIG, "ULTRA_PERIOD=150", "150ms Echo cadence")
    must_have(CONFIG, "ULTRA_TIMEOUT_US=25000", "Echo timeout")
    must_have(CONFIG, "IR_GROUND_ACTIVE=HIGH", "HIGH means ground")
    must_have(SOURCE, "EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL)", "native ESP32 secondary I2C; no ADS intermediary")
    must_have(SOURCE, "Wire.begin(Cfg::SDA,Cfg::SCL)", "separate ADS bus")

    fragments = {name: firmware_function(name) for name in (
        "ultraEchoIsr", "ultraUpdate", "irUpdate",
        "envRecoverBus", "envUpdate", "driveSet"
    )}
    for name in ("envRecoverBus", "envUpdate"):
        # Comments correctly mention what must NOT be touched. Inspect executable statements only.
        executable = re.sub(r"//[^\n]*|/\*.*?\*/", "", fragments[name], flags=re.S)
        if re.search(r"\b(?:adsBegin|adsRead|Wire\.)", executable):
            raise AssertionError(f"Unexpected ADS bus access from {name}")

    cpp = r'''
#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <iostream>
#include <algorithm>
#define HIGH 1
#define LOW 0
#define IRAM_ATTR
namespace Cfg {
  constexpr uint8_t ULTRA_ECHO_PIN=39, ULTRA_TRIG_PIN=23, IR_PIN=35, IR_GROUND_ACTIVE=HIGH;
  constexpr uint8_t ENV_SDA=13, ENV_SCL=14;
  constexpr uint16_t ULTRA_PERIOD=150, IR_PERIOD=50, ENV_PERIOD=1000;
  constexpr uint32_t ULTRA_TIMEOUT_US=25000, ENV_I2C_HZ=100000;
  constexpr uint8_t IR_CLIFF_ASSERT_SAMPLES=3, IR_CLIFF_CLEAR_SAMPLES=2;
  constexpr float ULTRA_WARN_CM=10.0f, ULTRA_STOP_CM=5.0f;
}
uint32_t fakeUs=0; int echoLevel=LOW,irPhysical=HIGH,trigHighCount=0,alertCount=0,writeCount=0;
uint32_t micros(){return fakeUs;}
uint32_t millis(){return fakeUs/1000;}
int digitalRead(int pin){return pin==Cfg::ULTRA_ECHO_PIN?echoLevel:(pin==Cfg::IR_PIN?irPhysical:LOW);}
void digitalWrite(int pin,int value){if(pin==Cfg::ULTRA_TRIG_PIN&&value==HIGH)trigHighCount++;}
void delayMicroseconds(uint32_t us){fakeUs+=us;}
void noInterrupts(){}
void interrupts(){}
bool ff(float n){return std::isfinite(n);}
void hazardAlert(){alertCount++;}
bool ultraWarning=false,ultraStop=false;
bool irWarning=true,irGroundDetected=false;
uint8_t irLevel=HIGH,irLostCount=0,irGroundCount=0;
uint32_t irAt=0;

float ultraCm=NAN;
uint32_t ultraAt=0,ultraPulseAt=0,ultraLastPulseUs=0,ultraLastOkAt=0,ultraTimeoutCount=0;
uint32_t ultraNoRiseCount=0,ultraNoFallCount=0,ultraStuckHighCount=0,ultraInvalidPulseCount=0;
const char* ultraStatus="awaiting_first_echo";
volatile uint32_t ultraRiseUsIsr=0,ultraFallUsIsr=0;
volatile bool ultraAwaiting=false,ultraEchoReady=false;

struct FakeWire {
  int endCount=0,beginCount=0,clockCount=0,timeoutCount=0;
  int sda=-1,scl=-1;
  void end(){endCount++;}
  void begin(int a,int b){beginCount++;sda=a;scl=b;}
  void setClock(uint32_t){clockCount++;}
  void setTimeOut(uint32_t ms){if(ms==25)timeoutCount++;}
} EnvWire;
constexpr uint32_t ENV_REPROBE_MS=3000;
constexpr uint8_t ENV_FAIL_LIMIT=3;
uint8_t envType=1,envReadFails=0;
uint32_t envAt=0,envProbeAt=0,envReadErrorTotal=0,envBusRecoveries=0;
float temp=23.0f,hum=55.0f,press=1005.0f;
const char* envStatus="ok";
bool envShouldFail=false;
int envBeginCalls=0,envReadCalls=0;
void envBegin(){envBeginCalls++;envProbeAt=millis();envType=1;envReadFails=0;}
bool envRead(){
  envReadCalls++;
  if(envShouldFail){envReadFails++;envReadErrorTotal++;envStatus="read_failed";return false;}
  envReadFails=0;temp=24.0f;hum=56.0f;press=1006.0f;envStatus="ok";return true;
}
float curL=0,curR=0;uint32_t cmdAt=0;bool staleStop=false;
float lim(int v){return std::clamp(static_cast<float>(v),-100.0f,100.0f);}
bool safetyAutoStopEnabled(){return false;}
void driveWrite(){writeCount++;}
int assertions=0;
void check(bool ok,const char* msg){
  ++assertions;
  if(!ok){std::cerr<<"FAIL: "<<msg<<"\n";std::exit(2);}
}
void resetEcho(){
  fakeUs=150000;echoLevel=LOW;
  ultraAt=ultraPulseAt=ultraLastPulseUs=ultraLastOkAt=ultraTimeoutCount=0;
  ultraNoRiseCount=ultraNoFallCount=ultraStuckHighCount=ultraInvalidPulseCount=0;
  ultraRiseUsIsr=ultraFallUsIsr=0;ultraAwaiting=false;ultraEchoReady=false;
  ultraCm=NAN;ultraWarning=ultraStop=false;ultraStatus="awaiting_first_echo";
}
'''+ "\n".join(fragments[k] for k in ("ultraEchoIsr","ultraUpdate","irUpdate","envRecoverBus","envUpdate","driveSet")) + r'''

void runEcho(float centimeters){
  ultraUpdate();check(ultraAwaiting,"Echo trigger armed");
  fakeUs+=100;echoLevel=HIGH;ultraEchoIsr();
  fakeUs+=static_cast<uint32_t>(std::floor(centimeters/0.01715f));
  echoLevel=LOW;ultraEchoIsr();
  ultraUpdate();
}
int main(){
  resetEcho();runEcho(25.0f);
  check(std::fabs(ultraCm-25.0f)<0.05f,"25cm distance from real edge delta");
  check(std::strcmp(ultraStatus,"ok")==0,"valid Echo status");
  check(!ultraWarning&&!ultraStop,"far target never blocks forward");

  resetEcho();runEcho(4.0f);
  check(ultraWarning&&ultraStop,"4cm sets warning and stop zone");
  resetEcho();runEcho(10.0f);
  check(ultraWarning&&!ultraStop,"10cm warns without stop");
  resetEcho();runEcho(5.0f);
  check(ultraWarning&&ultraStop,"5cm warns and blocks if enabled");

  resetEcho();ultraUpdate();fakeUs+=26000;ultraUpdate();
  check(std::strcmp(ultraStatus,"no_rising_edge")==0,"timeout diagnoses no rising edge");
  check(ultraNoRiseCount==1&&ultraTimeoutCount==1,"no-rise counter");
  check(!std::isfinite(ultraCm)&&!ultraWarning&&!ultraStop,"timeout clears stale distance and thresholds");

  resetEcho();ultraUpdate();fakeUs+=100;echoLevel=HIGH;ultraEchoIsr();
  fakeUs+=26000;ultraUpdate();
  check(std::strcmp(ultraStatus,"no_falling_edge")==0,"rise-only pulse diagnosed");
  check(ultraNoFallCount==1&&ultraTimeoutCount==1,"no-fall counter");

  resetEcho();int triggerBefore=trigHighCount;echoLevel=HIGH;ultraUpdate();
  check(std::strcmp(ultraStatus,"echo_high_before_trigger")==0,"stuck HIGH diagnosis");
  check(ultraStuckHighCount==1&&trigHighCount==triggerBefore&&!std::isfinite(ultraCm),"stuck HIGH rejects trigger and distance");

  resetEcho();ultraUpdate();fakeUs+=100;echoLevel=HIGH;ultraEchoIsr();
  fakeUs+=26000;echoLevel=LOW;ultraEchoIsr();ultraUpdate();
  check(std::strcmp(ultraStatus,"invalid_pulse")==0,"too-long edge interval rejected");
  check(ultraInvalidPulseCount==1&&!std::isfinite(ultraCm),"invalid pulse counter and null distance");

  fakeUs=0;irWarning=true;irGroundDetected=false;irLostCount=irGroundCount=0;irAt=0;
  irPhysical=HIGH;
  fakeUs=50000;irUpdate();fakeUs=100000;irUpdate();
  check(irGroundDetected&&!irWarning,"IR HIGH ground after 2 samples");
  irPhysical=LOW;
  fakeUs=150000;irUpdate();fakeUs=200000;irUpdate();fakeUs=250000;irUpdate();
  check(!irGroundDetected&&irWarning,"IR LOW cliff after 3 samples");

  fakeUs=0;envType=1;envReadFails=0;envAt=0;envProbeAt=0;envShouldFail=false;
  fakeUs=1000000;envUpdate();
  check(envReadCalls==1&&temp==24.0f&&hum==56.0f&&press==1006.0f,"fresh direct BME measurements at 1Hz");
  envShouldFail=true;
  fakeUs=2000000;envUpdate();
  check(envReadFails==1&&envType==1,"one read failure not premature restart");
  fakeUs=3000000;envUpdate();
  check(envReadFails==2&&envType==1,"two read failures not premature restart");
  fakeUs=4000000;envUpdate();
  check(envType==0&&envBusRecoveries==1,"third read failure restarts BME bus");
  check(EnvWire.endCount==1&&EnvWire.beginCount==1&&EnvWire.sda==13&&EnvWire.scl==14,"direct GPIO13/14 bus recovery");
  check(EnvWire.timeoutCount==1&&EnvWire.clockCount==1,"BME I2C timeout re-applied");
  check(!std::isfinite(temp)&&!std::isfinite(hum)&&!std::isfinite(press),"failed BME readings not held");
  int prior=envBeginCalls;fakeUs=6500000;envUpdate();
  check(envBeginCalls==prior,"backoff before 3000ms");
  fakeUs=7000000;envUpdate();
  check(envBeginCalls==prior+1,"BME 3000ms retry");

  fakeUs=8000000;curL=curR=0;irWarning=false;ultraStop=false;
  driveSet(25,0);check(curL==25&&curR==0,"left only independent axis");
  driveSet(0,-67);check(curL==0&&curR==-67,"right only independent reverse");
  driveSet(40,95);check(curL==40&&curR==95,"independent asymmetric speed");
  driveSet(0,0);check(curL==0&&curR==0,"explicit neutral");
  check(writeCount>=4,"driveWrite triggered each independent command");
  std::cout<<"PASS HR069-R1 host firmware replay, assertions="<<assertions
           <<" (Echo distance/errors, IR polarity, direct BME bus/recovery, motor commands)\n";
}
'''
    with tempfile.TemporaryDirectory(prefix="hr069-host-") as root:
        base = pathlib.Path(root)
        test_file = base / "hardware_replay.cpp"
        exe = base / "hardware_replay"
        test_file.write_text(cpp, encoding="utf-8")
        subprocess.run(["g++", "-std=c++17", "-O0", "-Wall", "-Wextra", "-pedantic",
                        str(test_file), "-o", str(exe)], check=True)
        subprocess.run([str(exe)], check=True)


if __name__ == "__main__":
    main()
