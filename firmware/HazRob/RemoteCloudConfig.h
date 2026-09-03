#pragma once
#include <Arduino.h>

/**
 * HazRob 0.9 remote cloud profile.
 * Public service coordinates are pinned here; production secrets remain local and must never be committed.
 */
namespace RemoteCfg {
constexpr char BROKER_HOST[]="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud";
constexpr uint16_t BROKER_PORT=8883;
constexpr char ROBOT_ID[]="hazrob-001";
constexpr char USERNAME[]="hazrob_robot";
constexpr char PASSWORD[]="";

// PEM trust anchor for BROKER_HOST. setInsecure() is intentionally never used.
// Keep empty until the production trust anchor is installed locally / through the secure build path.
constexpr char ROOT_CA[]="";
constexpr uint16_t KEEPALIVE_SEC=30;
constexpr uint32_t RETRY_MS=7000;
constexpr uint32_t TELEMETRY_MS=1000;
constexpr uint32_t STATUS_MS=5000;

inline bool configured(){return BROKER_HOST[0]&&ROBOT_ID[0]&&USERNAME[0]&&PASSWORD[0]&&ROOT_CA[0];}
}
