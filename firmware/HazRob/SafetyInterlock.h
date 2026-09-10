#pragma once

// HazRob 0.10.1 software safety interlock.
// This is an application-level latch and is NOT a certified hardware emergency-stop circuit.
void safetyLatchEstop();
void safetyResetEstop();
bool safetyEstopLatched();
