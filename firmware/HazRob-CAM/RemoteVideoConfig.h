#pragma once
#include <Arduino.h>

/** HazRob 0.10 Render video profile. Public coordinates are pinned; secrets stay in a local ignored header. */
#if __has_include("RemoteVideoSecrets.h")
#include "RemoteVideoSecrets.h"
#endif
#ifndef HAZROB_VIDEO_PUBLISH_TOKEN
#define HAZROB_VIDEO_PUBLISH_TOKEN ""
#endif
#ifndef HAZROB_VIDEO_ROOT_CA
#define HAZROB_VIDEO_ROOT_CA ""
#endif

namespace RemoteVideoCfg {
constexpr char HOST[]="hazrob-video-relay.onrender.com";
constexpr uint16_t PORT=443;
constexpr char BASE_PATH[]="/hazrob/video";
constexpr char ROBOT_ID[]="hazrob-001";
constexpr char PUBLISH_TOKEN[]=HAZROB_VIDEO_PUBLISH_TOKEN;
constexpr char ROOT_CA[]=HAZROB_VIDEO_ROOT_CA;
constexpr uint32_t FRAME_INTERVAL_MS=100; // target ~10 FPS; actual rate adapts to capture/network throughput.
constexpr uint32_t RETRY_MIN_MS=2000,RETRY_MAX_MS=30000;
constexpr size_t MAX_JPEG=160*1024;
inline bool configured(){return HOST[0]&&ROBOT_ID[0]&&PUBLISH_TOKEN[0]&&strstr(ROOT_CA,"BEGIN CERTIFICATE")!=nullptr;}
}
