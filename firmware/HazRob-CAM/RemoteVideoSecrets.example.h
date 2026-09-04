#pragma once

// Copy this file to RemoteVideoSecrets.h before field flashing.
// RemoteVideoSecrets.h is ignored by Git and must never be committed.
#define HAZROB_VIDEO_PUBLISH_TOKEN "PASTE_RENDER_PUBLISH_TOKEN_HERE"
#define HAZROB_VIDEO_ROOT_CA R"PEM(
-----BEGIN CERTIFICATE-----
PASTE_TRUSTED_ROOT_CA_FOR_hazrob-video-relay.onrender.com_HERE
-----END CERTIFICATE-----
)PEM"
