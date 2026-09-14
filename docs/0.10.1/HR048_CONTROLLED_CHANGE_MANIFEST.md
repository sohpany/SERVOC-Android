# HazRob 0.10.1 — HR-0101-048 Controlled Change Manifest

Status: **CONTROLLED REMEDIATION / NOT YET FIELD-ACCEPTED**

- Product/version: `HazRob 0.10.1`
- Change request: `HR-0101-048`
- Target branch: `dev/hazrob-0.10.1`
- Isolated remediation branch: `fix/hr-0101-048-field-remediation`
- Baseline commit: `702ddcf9cef20f1212719902acbc275bda7f3f58`
- Candidate branch head immediately before this manifest commit: `5b6686a559f4d4b98c65e32efa0d2fe578260c7b`
- Version bump: **NONE**
- Release rule: no merge and no new field baseline until software/build gates and applicable physical gates pass.

## 1. Approved remediation scope

| Area | Controlled change | Status |
|---|---|---|
| Drive | Keep canonical differential mixer; map logical LEFT to L298N B and logical RIGHT to L298N A as HR048 remediation candidate | Implemented; physical re-acceptance pending |
| Direct drive ownership | Suppress repeated idle `0,0` while retaining non-zero 20 Hz refresh | Implemented; verification pending |
| MQ warning | Use firmware-normalized `Rs/R0=1.0` as operational fallback when separate Android baseline is absent | Implemented; unit/build/field verification pending |
| Alert GPIO26 | Correct logical alert assertion but keep `ALERT_OUTPUT_INSTALLED=false` until safe external driver is confirmed | Implemented; hardware output intentionally fail-closed |
| Remote diagnosis | Show staged Config/Wi-Fi/IP/DNS/Time/Internet/TLS/MQTT/ACK/Telemetry readiness and firmware detail | Implemented; field first-failure capture pending |
| Analysis UI | Separate Screening outputs from M4-gated reportable PPM | Implemented; no M4 bypass |
| IR/ECHO/Sound | No speculative firmware fix before correct electrical interfacing; document authoritative pins and voltage rules | Physical rewiring/test pending |
| ADS VDD | Keep software acquisition assumption at 5 V but reclassify it as requiring physical confirmation | Measurement pending |
| LFL/UFL offline seed | Preserve pinned Render fallback until exact SHA-pinned asset is bundled locally | Pending; primary seed service retained |

## 2. Files intentionally changed from the controlled baseline

Android/application:

- `app/src/main/java/com/servoc/app/communication/RobotApi.java`
- `app/src/main/java/com/servoc/app/hardware/HazRobHardwareProfile.java`
- `app/src/main/java/com/servoc/app/measurement/InstantMqWarningEngine.java`
- `app/src/main/java/com/servoc/app/ui/AnalyticalLabTab.java`
- `app/src/main/java/com/servoc/app/ui/RobotConnectionSetupTab.java`
- `app/src/test/java/com/servoc/app/measurement/GasAlertCoordinatorTest.java`

Main ESP32 firmware:

- `firmware/HazRob/Config.h`
- `firmware/HazRob/Alerts.cpp`

Controlled documentation/audits/build:

- `docs/0.10.1/FIELD_INTEGRATION_REMEDIATION_HR048.md`
- `docs/0.10.1/GPIO_AUTHORITY.md`
- `docs/0.10.1/HR048_CONTROLLED_CHANGE_MANIFEST.md`
- `tools/audit_hr048_drive_contract.py`
- `tools/audit_hr048_integration_contract.py`
- `tools/build_hr048_validation.sh`

No ESP32-CAM runtime source change is authorized by HR048 unless a later field finding explicitly requires it.

## 3. Invariants that must not change in HR048

- Version remains `0.10.1`; versionCode remains 21 unless a separate distribution decision explicitly changes it.
- Direct robot address remains `192.168.4.1`; ESP32-CAM direct address remains `192.168.4.2`.
- I2C remains SDA GPIO21 / SCL GPIO22.
- ADS addresses remain `0x48` and `0x49`.
- MQ relay remains GPIO25 active LOW.
- Accepted field R0 campaign and values are not recalibrated by this remediation.
- M4 quantitative gate is not weakened or bypassed.
- Remote MQTT command payload preserves logical `left/right` semantics; no UI-only steering inversion is permitted.
- GPIO26 never directly drives the measured low-impedance speaker.
- IR/ECHO/Sound are not reclassified as I2C devices.

## 4. Software/build verification gate

One immutable commit must produce all of the following PASS results:

1. `tools/audit_hr048_drive_contract.py`
2. `tools/audit_hr048_integration_contract.py`
3. `tools/audit_0101_motion_contract.py`
4. `tools/audit_hr047_gas_safety_contract.py`
5. Android `:app:testDebugUnitTest`
6. Android `:app:assembleRelease`
7. APK signer SHA-256 = `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`
8. main ESP32 compile using `esp32:esp32@3.3.11` and FQBN `esp32:esp32:esp32:PartitionScheme=huge_app`
9. ESP32-CAM compile using `esp32:esp32@3.3.11` and FQBN `esp32:esp32:esp32cam`
10. SHA-256 manifest for generated test artifacts.

A software/build PASS does not satisfy the physical field gate.

## 5. Physical field gate

### Drive

Wheels-up at low speed:

- `L=+50,R=0`
- `L=0,R=+50`
- `L=-50,R=0`
- `L=0,R=-50`
- `L=+50,R=-50`
- `L=-50,R=+50`

Then verify forward, reverse, forward-right arc, forward-left arc, pivot-right and pivot-left. Any weak/non-reversing side is treated as electrical/mechanical until proven otherwise.

### Sensors

- MAX4466 OUT direct to the approved analog path at GPIO36 and within ESP32-safe voltage.
- IR output to GPIO35 at <=3.3 V logic.
- Ultrasonic TRIG GPIO23; ECHO GPIO39 reduced to <=3.3 V with appropriate interface.
- ENV/BME/BMP and ADS I2C device ACKs tested separately.
- ADS1115 assembled VDD measured and recorded before analytical physical acceptance.

### MQ/alerts

- Heater warm-up completed.
- Controlled clean-air response recorded.
- Controlled smoke/gas challenge confirms live response and phone-side alert behavior.
- External GPIO26 alert hardware remains disabled until safe driver topology is physically approved.

### Remote

Capture `/remote/status` and identify the first failing stage, then validate MQTT command -> ACK -> telemetry round-trip before declaring Remote PASS.

## 6. LFL/UFL service-retirement gate

The primary `hazrob-0101-flammability-seed` Render service remains required until the exact pinned file
`generated/flammability/niosh_npg_flammability.json` is bundled in the APK and verified with SHA-256:

`457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`

A fresh-install offline bootstrap test must pass before that primary service can be retired. Duplicate no-op seed services are not scientific runtime authorities, but their deletion is an infrastructure housekeeping action and is not part of source acceptance.

## 7. Acceptance labels

Use only these states in HR048 records:

- **IMPLEMENTED** — source change exists.
- **SOFTWARE PASS** — static/unit/build gate passed for one immutable commit.
- **PHYSICAL PASS** — measured field test passed.
- **PENDING** — evidence not yet collected.
- **BLOCKED** — a prerequisite is missing.

Do not label a subsystem PASS merely because it compiled.
