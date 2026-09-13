# HazRob 0.10.1 — Controlled Change Log Addendum HR-0101-047

**Change ID:** HR-0101-047  
**Title:** Gas-relative safety prioritization, ppm-only exposure interpretation, flammability library, and whole-system compatibility closeout  
**Version:** HazRob 0.10.1 / API 4  
**Executable source baseline:** `7877fb372a84c8bce56be887b864fee166f27fa7`  
**Distribution/test-gate source:** `5aa65eca113969cb8ec49dc5850076c327fdea5f`  
**Status:** **SOURCE/INTEGRATION/DISTRIBUTION BUILD PASS — GAS-SAFETY SOFTWARE LOOP CLOSED — PHYSICAL ENV/M4 VALIDATION STILL CONTROLLED SEPARATELY**

## 1. Objective

Close the remaining analytical safety loop after HR-0101-046 without changing the HazRob 0.10.1 release identity, GPIO authority, direct/remote transport contract, drive behavior, or camera behavior.

The controlled path is now:

`MQ acquisition → accepted R0 normalization → ChemCore identity/open-set → complete M4 quantitative gate → reportable ppm → chemical_uid/CAS-bound exposure references → ppm-only toxicological comparison → LFL/UFL comparison → relative hazard priority → operator/report presentation`.

Absolute ppm is always retained as a measured result when the quantitative gate passes, but it is not the default safety-priority key. Safety order is based on the substance-specific ratio to its controlling health or flammability reference.

## 2. Scientific safety policy

### 2.1 Toxicological limits

The gas-safety interpreter accepts only occupational toxicological limits explicitly expressed in `ppm`. Mass-concentration limits such as `mg/m3`, `ug/m3`, dust and fibre units remain in the scientific database but are excluded from gas-phase hazard prioritization. No silent mass-to-ppm conversion is permitted.

This prevents a gas-focused HazRob report from mixing particulate limits with gas/vapour ppm interpretation.

### 2.2 Flammability limits

LFL/LEL and UFL/UEL are represented as percent by volume and converted only for the dimensionless ratio calculation using the identity:

`1 vol% = 10,000 ppmv`

Therefore:

`%LFL = measured_ppm / (LFL_vol_percent × 10,000) × 100`

The flame indicator is raised only when the accepted quantitative concentration reaches or exceeds `100% LFL`.

### 2.3 Relative hazard priority

For an accepted quantitative result:

`toxicity_ratio = measured_ppm / controlling_health_reference_ppm`

`flammability_ratio = measured_ppm / LFL_ppm`

`hazard_priority_ratio = max(toxicity_ratio, flammability_ratio)`

Default operator sorting is **Hazard**, descending by `hazard_priority_ratio`. A second operator sorting mode, **Concentration**, sorts descending by measured ppm. Changing the display sort never changes chemical identity, thresholds, symbols, evidence or the scientific result.

The required regression example is locked by test: a 6 ppm gas against a 5 ppm reference (ratio 1.2) sorts ahead of a 300 ppm gas against a 1000 ppm reference (ratio 0.3) in Hazard mode, while Concentration mode reverses only the display order.

### 2.4 Hazard symbols

- `☠` means the measured reportable ppm has reached/exceeded at least one available ppm health reference for that chemical.
- `🔥` means the reportable concentration has reached/exceeded `100% LFL`.
- A TWA/STEL concentration crossing in a short capture is a health-reference context warning, not a legal compliance determination.
- `IDLH` and ceiling references retain their immediate-reference semantics.

No symbol is generated from a withheld/non-M4 ppm hypothesis.

## 3. Authoritative data and provenance

The existing identity/exposure layer remains keyed to `chemical_uid` / CAS rather than display name matching. NIOSH/OSHA/IDLH records therefore remain bound to the resolved material identity.

HR-0101-047 adds a controlled NIOSH Pocket Guide flammability seed generated from all 652 pinned NIOSH identities. Generation verification completed with:

- official pages processed: **652 / 652**;
- page errors: **0**;
- records with numeric LFL: **213**;
- records with numeric UFL: **179**;
- controlled seed SHA-256: `457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434`.

The Android build downloads or uses the controlled seed, verifies this exact SHA-256, and fails closed on mismatch before packaging it into APK assets. Runtime gas safety therefore does not require Internet access to obtain the flammability seed.

## 4. Analytical and reporting integration

`ChemCoreDecisionRepository` enriches the same session-bound ChemCore decision payload with `gas_exposure_ppm`. The enrichment occurs only after the result passes the existing complete M4/reportable-ppm/identity gate.

The operator analysis surface exposes:

- accepted chemical identity;
- measured ppm when reportable;
- toxicity-reference ratio;
- LFL percentage;
- hazard-priority ratio and controlling basis;
- `☠` and `🔥` indicators;
- safety findings sorted by **Hazard** or **Concentration**.

Identity candidate ranking remains independent and continues to use ChemCore posterior/residual/evidence logic. A highly hazardous material cannot become the top identity candidate merely because its hazard threshold is low.

PDF output now contains a dedicated gas-safety page. XLSX output contains a dedicated `سلامة الغاز` worksheet. Both remain bound to the same persisted ChemCore decision/session and include ppm, controlling references, ratios, LFL/UFL information, symbols and source provenance where available.

## 5. Whole-system compatibility matrix

| Interface / subsystem | Authority checked | Result |
|---|---|---|
| Release identity | Android `0.10.1` / versionCode 21; master `HazRob 0.10.1`; CAM `HazRob-CAM 0.10.1`; relay `0.10.1` | PASS |
| API | Main ESP32 API 4 ↔ Android 0.10.1 contract | PASS |
| Direct network | Robot `192.168.4.1`; CAM `192.168.4.2`; gateway `192.168.4.1` | PASS |
| Drive GPIO | left EN16/IN18/27; right EN17/IN19/32 | PASS — unchanged |
| I2C | SDA21/SCL22; ADS1 `0x48`; ADS2 `0x49`; ENV probe `0x76/0x77` | PASS at source contract; physical ENV still pending |
| MQ power | GPIO25 active-low relay | PASS — unchanged |
| MQ mapping | ADS1: MQ2/MQ4/MQ5/MQ8; ADS2: MQ3/MQ7/MQ9/MQ135 | PASS |
| R0 authority | HR-0101-045 three-session baseline; 216 samples | PASS / synchronized in app and firmware source |
| Android ↔ master remote commands | drive, stop, estop/reset, relay, measure, alerts, config, heartbeat, camera flash and probes | PASS — every emitted command has a firmware handler |
| E-stop | Direct + Remote latch/reset + UI drive inhibition | PASS |
| Local/remote configuration | shared firmware `SettingsPatch/applySettings` | PASS |
| HiveMQ | host/port/robot-id identical across Android/master; role-scoped bootstrap | PASS |
| Camera direct | CAM static IP, `/status`, `/stream`, flash and MJPEG | PASS |
| Camera remote | `esp32-cam` bootstrap role, publish token, Render/WSS authority | PASS |
| ChemCore identity | single authoritative ChemCore v3 path; legacy competing UI paths removed | PASS |
| PPM gate | complete M4 + reportable ppm + accepted identity required | PASS / fail-closed |
| Exposure units | toxicological safety uses explicit ppm only | PASS / fail-closed |
| Flammability | pinned NIOSH LFL/UFL seed, SHA-256 verified | PASS |
| Safety sorting | Hazard default + Concentration operator option | PASS |
| PDF/XLSX lineage | exact session-bound ChemCore payload and gas-safety enrichment | PASS at source/build |

## 6. Firmware compatibility determination

Git comparison from the HR-0101-044 executable baseline `69d17f52b2096cc303f005db78a6a2c7bfcd3ab9` to HR-0101-047 executable baseline `7877fb372a84c8bce56be887b864fee166f27fa7` shows only one firmware file changed: `firmware/HazRob/Config.h`. That change synchronizes the accepted HR-0101-045 R0 constants/provenance. No master drive, networking, MQTT, ADC, relay, ENV driver or command-handler source was changed by HR-0101-046/047.

No file under `firmware/HazRob-CAM/` changed in that comparison. Camera behavior is therefore source-identical to the previously verified HazRob-CAM 0.10.1 baseline.

The final distribution build nevertheless compiled both ESP32 targets from the same distribution/test-gate source `5aa65eca113969cb8ec49dc5850076c327fdea5f` to prove that repository-level changes did not introduce integration drift.

## 7. Android pre-closeout verification at executable baseline

Controlled builder: `hazrob-0101-build`  
Deploy: `dep-daj18b3m8hqs73eja1c0`  
Source: `7877fb372a84c8bce56be887b864fee166f27fa7`

Verified before documentation closeout:

- cross-component integration contract: **PASS**;
- controlled flammability seed SHA-256 verification: **PASS**;
- `:app:testDebugUnitTest`: **PASS**;
- `:app:assembleRelease`: **PASS**;
- `lintVitalRelease`: **PASS**;
- signing continuity: **PASS**;
- APK SHA-256: `27bd82ba796dfa57fbebe19364774e93467780664c5583898f3cfeffb418d086`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

## 8. Controlled limitations — not compatibility failures

The software/distribution compatibility closeout does not change the scientific status of the following physical work:

1. the onboard BME/BMP still requires physical identification/valid T/RH/P telemetry before environmental correction can be claimed;
2. HR-0101-045 R0 is a device field clean-air normalization baseline, not a reference-gas quantitative calibration;
3. exact material-specific reportable ppm still requires successful M4 reference-gas calibration for every contributing quantitative curve, accepted Open-Set identity and in-domain solution;
4. material discrimination performance must still be demonstrated with reference gases, blind tests, interference/mixture challenges and out-of-domain cases;
5. gas-source tracking remains a separate physical navigation validation task.

These limitations are intentionally represented as fail-closed gates. They do not create a source-level incompatibility between Android, ESP32, CAM, remote transport or reporting.

## 9. Non-regression constraints after HR-0101-047

- remain on HazRob `0.10.1` / API 4;
- no GPIO reassignment without explicit hardware change control;
- no reintroduction of legacy parallel chemical-analysis UI paths;
- no mass-concentration limit silently converted into ppm gas safety;
- no safety ranking by absolute ppm when Hazard sort is selected;
- no `☠`/`🔥` derived from screening-only or withheld ppm;
- no flame marker below 100% LFL;
- no legal TWA/STEL compliance claim from a capture that does not satisfy the required temporal/sampling context;
- no quantitative ppm outside complete M4/open-set/domain gates;
- no assumed T/RH/P when ENV telemetry is unavailable;
- no unpinned flammability seed accepted into a release build.

## 10. Final synchronized distribution closeout

**Distribution/test-gate source:** `5aa65eca113969cb8ec49dc5850076c327fdea5f`  
This source differs from executable baseline `7877fb...` only by controlled documentation/test-gate additions; application and firmware executable logic is unchanged by those later commits.

### 10.1 Independent release gates

Controlled release-gate service: `hazrob-0101-hr047-release-gate`  
Deploy: `dep-daj1cf95efls73f8bsbg`  
Result: **LIVE / PASS**

- HazRob 0.10.1 cross-component integration contract: **PASS**;
- HR-0101-047 gas-safety integration gate: **PASS**;
- M4/reportable-ppm gate binding: **PASS**;
- toxicological PPM-only policy: **PASS**;
- pinned LFL/UFL policy: **PASS**;
- Hazard/Concentration sort contract: **PASS**;
- R0 synchronization app ↔ ESP32 source: **PASS**.

### 10.2 Android final distribution build

Controlled builder: `hazrob-0101-build`  
Deploy: `dep-daj1ch89qbnc73fja6dg`  
Source: `5aa65eca113969cb8ec49dc5850076c327fdea5f`  
Result: **LIVE / PASS**

- integration contract: **PASS**;
- flammability seed SHA-256 verification: **PASS**;
- `:app:testDebugUnitTest`: **PASS**;
- `:app:assembleRelease`: **PASS**;
- `lintVitalRelease`: **PASS**;
- signer continuity: **PASS**;
- APK SHA-256: `4c9f21c5478335fce60ed237c2ac7c59fc99d0add54145e940c57141d7f902e2`;
- signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.

### 10.3 Main ESP32 final distribution build

Controlled builder: `hazrob-0101-firmware-build`  
Deploy: `dep-daj1cjiq185c73ajsojg`  
Source: `5aa65eca113969cb8ec49dc5850076c327fdea5f`  
Target: `esp32:esp32:esp32:PartitionScheme=huge_app`  
Arduino ESP32 core: `3.3.11`  
Result: **LIVE / PASS**

- program storage: **1,091,121 bytes / 34%**;
- global dynamic memory: **50,204 bytes / 15%**;
- free local-variable memory: **277,476 bytes**.

### 10.4 ESP32-CAM final distribution build

Same controlled firmware deploy and source as 10.3.  
Target: `esp32:esp32:esp32cam`  
Result: **PASS**

- program storage: **1,118,803 bytes / 35%**;
- global dynamic memory: **67,836 bytes / 20%**;
- free local-variable memory: **259,844 bytes**.

Compiler warnings about volatile-qualified increments and the existing certificate-pointer expression remain warnings only; no compile/link failure occurred. They are unchanged from the previously accepted CAM baseline and do not indicate Android/master/CAM contract incompatibility.

### 10.5 Infrastructure incident disposition

The first final firmware attempt failed before compilation because cached Arduino CLI state caused `arduino-cli config init` to report an existing configuration file under `set -e`. The source integration audit had already passed. The builder was corrected by assigning a fresh controlled `ARDUINO_CONFIG_FILE`/data directory and the exact-head rebuild then compiled both master and CAM successfully. This event is classified as **build-environment failure, resolved**, not a firmware regression.

**Final distribution determination:** Android + main ESP32 + ESP32-CAM + remote protocol + chemical-analysis/reporting safety contract are source/build compatible for HazRob 0.10.1. Physical ENV qualification and reference-gas M4 validation remain intentionally pending and must not be represented as completed metrological validation.
