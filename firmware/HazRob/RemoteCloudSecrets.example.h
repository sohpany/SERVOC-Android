#pragma once

// Copy this file to RemoteCloudSecrets.h before field flashing.
// RemoteCloudSecrets.h is ignored by Git and must never be committed.
#define HAZROB_MQTT_PASSWORD "PASTE_HIVEMQ_hazrob_robot_PASSWORD_HERE"
#define HAZROB_MQTT_ROOT_CA R"PEM(
-----BEGIN CERTIFICATE-----
PASTE_TRUSTED_ROOT_CA_FOR_HIVEMQ_HOST_HERE
-----END CERTIFICATE-----
)PEM"
