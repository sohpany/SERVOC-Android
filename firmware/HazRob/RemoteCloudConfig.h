#pragma once
#include <Arduino.h>

/**
 * HazRob 0.9 remote cloud profile.
 * Fill service credentials locally before flashing when the MQTT service is provisioned.
 * Never commit production credentials. Remote mode remains disabled while any required value is empty.
 */
namespace RemoteCfg {
constexpr char BROKER_HOST[]="";
constexpr uint16_t BROKER_PORT=8883;
constexpr char ROBOT_ID[]="hazrob-001";
constexpr char USERNAME[]="";
constexpr char PASSWORD[]="";

// PEM root CA that validates BROKER_HOST. setInsecure() is intentionally never used.
constexpr char ROOT_CA[]="";
constexpr uint16_t KEEPALIVE_SEC=30;
constexpr uint32_t RETRY_MS=7000;
constexpr uint32_t TELEMETRY_MS=1000;
constexpr uint32_t STATUS_MS=5000;

inline bool configured(){return BROKER_HOST[0]&&ROBOT_ID[0]&&USERNAME[0]&&PASSWORD[0]&&ROOT_CA[0];}
}
