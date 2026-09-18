# HR-0102-067 — Analytical v4 unified release package

Date: 2026-09-18  
Release: **HazRob 0.10.2 / HR067**  
Predecessor: HazRob 0.10.1 / HR066

## Release definition

HR067 is a unified application + firmware package. The Android application advances to **0.10.2 (versionCode 26)** and incorporates the completed Analytical v4 work through Stage 13 and the Stage 14 release-candidate line.

## Firmware status

There is **no new firmware source change in HR067**. HR067 intentionally republishes the accepted HR066 Main ESP32 and ESP32-CAM sources unchanged so the application and both controllers have one synchronized release identifier and one distributable package.

Authoritative firmware directories remain:

- `firmware/HazRob`
- `firmware/HazRob-CAM`

The HR066 network/hardware contract remains authoritative: Main ESP32 is the sole `HazRob` AP at 192.168.4.1; ESP32-CAM is STA-only at 192.168.4.2; DIRECT is the boot/default authority; REMOTE requires explicit activation.

## Android 0.10.2 scope

This release consolidates Analytical Contract v4, canonical `chemcore-features/1`, 144-compound screening, open-set/mixture/OOD gates, M4 quantitative gating, exposure/flammability separation, live measurement quality, drift-aware calibration validity, analysis-results UI, dynamic reports, 20-chart analytical visualization suite, and PDF/XLSX v4 reporting.

Candidate tracking remains an operational UI function and is not persisted as an analytical conclusion in reports.

## Release identity

- Release family: HR067
- Android: HazRob 0.10.2
- Android versionCode: 26
- Firmware: HR066 source-equivalent, repackaged under HR067
- Firmware modification relative to HR066: **NONE**
- Physical robot acceptance: pending flash/install and field verification

## Package contents

The final HR067 distributable shall contain:

1. HazRob 0.10.2 APK.
2. Main ESP32 firmware from the frozen HR066 source.
3. ESP32-CAM firmware from the frozen HR066 source.
4. This HR067 manifest.
5. Firmware/source hashes and release commit identifiers.

## Release rule

No firmware behavior may be described as changed in HR067. Any later firmware modification requires a subsequent controlled change identifier rather than silently altering this package.
