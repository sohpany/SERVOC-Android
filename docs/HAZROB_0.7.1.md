# HazRob 0.7.1

## Purpose

0.7.1 is the camera/hardware recovery release after field testing 0.7.0 showed no ESP32-CAM video and flash control failure.

## Root-cause boundary

The master ESP32 network is now `HazRob`, while an older ESP32-CAM image may still search only for `Servoc`. If the camera never associates with the same AP, both `192.168.4.2:81/stream` and port-80 flash controls are unreachable. The Android app cannot rewrite an SSID compiled into an old camera firmware.

## Camera changes

- ESP32-CAM 0.7.1 alternates between open `HazRob` and legacy `Servoc` until connected.
- Static camera IP remains 192.168.4.2; master remains 192.168.4.1.
- Canonical MJPEG endpoint remains port 81 `/stream`; port 80 `/stream` redirects to it.
- `/status` reports firmware, SSID, IP, PID, resolution, FPS, RSSI and flash state.
- Multiple flash-control aliases are supported for migration compatibility.
- Android tries multiple MJPEG paths and shows camera connection/stream/retry states instead of a silent black view.
- Flash UI is ON/OFF only and distinguishes camera-unreachable from unsupported-control errors.

## Video policy

MP4 recording is independent from HazRob measurement sessions and reports. The app converts incoming MJPEG frames to H.264/MP4 using actual incoming timestamps and exports the result to `Movies/HazRob`. It does not create a video measurement session, attach the MP4 to a report, or save the MP4 as a report/evidence asset.

Flame-analysis events are separate analytical events and may retain their own still-image evidence; they do not own or link the MP4 recording.

## Sensor integration

- Eight MQ channels are normalized in Android: MQ2, MQ3, MQ4, MQ5, MQ7, MQ8, MQ9 and MQ135.
- Relay-bank planning is performed in Android for gas-source tracking.
- MAX4466 is recorded and displayed as RMS/P-P voltage, not calibrated dB SPL.
- IR is a warning-only signal and never commands an automatic stop.
- ADS1115 #1/#2 health, relay mask and ESP32 retrace state are exposed in UI, telemetry and reports.
- PDF/Excel reports include an auxiliary hardware/sensor section but deliberately exclude MP4 files.

## Version

- Android versionName: 0.7.1
- Android versionCode: 11
- ESP32 API: v3 plus relayMask/ADS1115/IR/sound/retrace extensions
- ESP32-CAM firmware identity: HazRob-CAM 0.7.1

## Required field sequence

1. Program the master ESP32 with the HazRob master firmware.
2. Program ESP32-CAM with `firmware/HazRob-CAM/HazRob-CAM.ino` from this release.
3. Power the master ESP32 first and connect the phone to `HazRob`.
4. Power ESP32-CAM and wait for it to join at 192.168.4.2.
5. In the app, run the robot/camera connection test.
6. Confirm camera status changes to live streaming before testing recording and flash.
7. Only after video is stable, test sensor banks and gas-source tracking.
