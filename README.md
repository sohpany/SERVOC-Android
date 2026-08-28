# SERVOC Android

Android control, telemetry, video and analytical application for the SERVOC remote hazard-inspection robot.

## Architecture principle

- ESP32 retains real-time hardware control, acquisition and failsafe functions.
- Android handles the user interface, chemical analytics, PPM/exposure calculations, experiment storage and future AI/cloud functions.
- ESP32-CAM firmware remains independent; the app consumes its video stream.

## Development model

The application is developed as independent functional packages/modules so control, communications, camera, chemical analytics, exposure limits, experiments and future AI features can evolve without tightly coupling the codebase.

## Status

Initial repository baseline. Android source and CI APK build workflow will be added incrementally.
