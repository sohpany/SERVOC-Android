#pragma once

// HazRob-CAM 0.10 normally obtains the Render publisher credential at runtime.
// DO NOT copy this file for the normal field build.
// This file is retained only as an optional controlled fallback for diagnostics.
#define HAZROB_VIDEO_PUBLISH_TOKEN ""

// The trusted public root CA is already embedded in RemoteVideoConfig.h.
// Leave HAZROB_VIDEO_ROOT_CA undefined unless intentionally testing another CA.
  