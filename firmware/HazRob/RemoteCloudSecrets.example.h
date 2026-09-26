#pragma once

// HazRob 0.10 normally obtains MQTT credentials at runtime from the Railway bootstrap service.
// DO NOT copy this file to RemoteCloudSecrets.h for the normal 0.10 field build.
// This template is kept only as an optional offline/build-time fallback for controlled diagnostics.
// Never place a real password in this example file and never commit RemoteCloudSecrets.h.
#define HAZROB_MQTT_USERNAME "hazrob_robot"
#define HAZROB_MQTT_PASSWORD ""

// The current trusted root CA is already embedded in RemoteCloudConfig.h.
// Leave this undefined unless intentionally testing a different trusted CA.
