#pragma once
#include <Arduino.h>

/** HazRob 0.9 HiveMQ profile. Public service coordinates are pinned; secrets stay in a local ignored header. */
#if __has_include("RemoteCloudSecrets.h")
#include "RemoteCloudSecrets.h"
#endif
#ifndef HAZROB_MQTT_PASSWORD
#define HAZROB_MQTT_PASSWORD ""
#endif
#ifndef HAZROB_MQTT_ROOT_CA
#define HAZROB_MQTT_ROOT_CA ""
#endif

namespace RemoteCfg {
constexpr char BROKER_HOST[]="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud";
constexpr uint16_t BROKER_PORT=8883;
constexpr char ROBOT_ID[]="hazrob-001";
constexpr char USERNAME[]="hazrob_robot";
constexpr char PASSWORD[]=HAZROB_MQTT_PASSWORD;
constexpr char ROOT_CA[]=HAZROB_MQTT_ROOT_CA;
constexpr uint16_t KEEPALIVE_SEC=30;
constexpr uint32_t RETRY_MS=7000;
constexpr uint32_t TELEMETRY_MS=1000;
constexpr uint32_t STATUS_MS=5000;

inline bool configured(){return BROKER_HOST[0]&&ROBOT_ID[0]&&USERNAME[0]&&PASSWORD[0]&&strstr(ROOT_CA,"BEGIN CERTIFICATE")!=nullptr;}
}
