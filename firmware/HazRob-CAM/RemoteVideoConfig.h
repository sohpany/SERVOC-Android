#pragma once
#include <Arduino.h>

/**
 * Remote video relay provisioning template.
 * Keep real production credentials out of the repository; inject them in the local flashing copy.
 */
namespace RemoteVideoCfg {
constexpr char HOST[]="";
constexpr uint16_t PORT=443;
constexpr char BASE_PATH[]="/hazrob/video";
constexpr char ROBOT_ID[]="hazrob-001";
constexpr char PUBLISH_TOKEN[]="";
constexpr char ROOT_CA[] = R"PEM(
)PEM";
constexpr uint32_t FRAME_INTERVAL_MS=250; // ~4 FPS over cellular by default.
constexpr uint32_t RETRY_MS=5000;
constexpr size_t MAX_JPEG=160*1024;
inline bool configured(){return HOST[0]&&ROBOT_ID[0]&&PUBLISH_TOKEN[0]&&strstr(ROOT_CA,"BEGIN CERTIFICATE")!=nullptr;}
}
