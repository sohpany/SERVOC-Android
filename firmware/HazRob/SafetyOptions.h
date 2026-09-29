#pragma once

// Operator-selectable automatic forward-stop policy for cliff/collision hazards.
// Warnings, E-STOP, explicit stop and command-loss watchdog remain active independently.
bool safetyAutoStopEnabled();
void safetyAutoStopSet(bool enabled,bool persist=true);
