#include "SafetyInterlock.h"

namespace {
volatile bool estopLatched=false;
}

void safetyLatchEstop(){estopLatched=true;}
void safetyResetEstop(){estopLatched=false;}
bool safetyEstopLatched(){return estopLatched;}
