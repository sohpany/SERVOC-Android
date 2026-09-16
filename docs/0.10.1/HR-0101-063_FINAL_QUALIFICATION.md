# HazRob 0.10.1 — HR-0101-063 Final Qualification Freeze

Date: 2026-09-16
Parent baseline: HR-0101-062 / aa1035780d2e6a84002fbe69aa50e2dc88757072

## Scope
This controlled record freezes the reviewed integrated HazRob 0.10.1 package: Android application, ESP32 Main firmware, ESP32-CAM firmware, Direct/Remote networking contract, Render video relay integration, MQTT/HiveMQ integration contract, branding/reporting changes inherited from HR062, and controlled build artifacts.

## Network authority contract
- Main ESP32 is the sole HazRob SoftAP authority.
- Direct boot is mandatory.
- HazRob AP address: 192.168.4.1.
- ESP32-CAM address: 192.168.4.2 and production camera firmware is STA-only.
- Remote mode is entered explicitly; it must not replace the local HazRob AP needed by the camera.
- Android Direct flow expects Main at 192.168.4.1 and CAM at 192.168.4.2.

## Qualification gates
The frozen baseline was reviewed for Android/Main/CAM contract consistency. Controlled Render builds for the parent baseline completed successfully for Android and firmware, including integration audit, Android unit tests/release assembly/signature verification, and Arduino compilation of Main and CAM. Render relay deployment is live on the same 0.10.1 branch.

## Field limitation
Software/build qualification does not substitute for physical hardware verification. Final field acceptance requires clean flashing of both controllers and verification that HazRob is the advertised AP, no ESP_* camera AP appears, Main is reachable at 192.168.4.1, CAM at 192.168.4.2, and Direct/Remote, motion, sensors and video operate on the physical robot. Live HiveMQ end-to-end verification is also a field/runtime acceptance item.

## Change-control rule
HR-0101-063 is a documentation/freeze revision. It intentionally introduces no runtime-code change beyond the reviewed HR062 source baseline. Any later runtime modification requires a new controlled revision and a complete regression/build cycle.
