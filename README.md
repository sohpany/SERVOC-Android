# HazRob 0.10.2 / HR069

## Latest paired APK and Main ESP32 firmware — 2026-10-09

**CURRENT LATEST VERIFIED DIAGNOSTIC PAIR: HazRob 0.10.2 / HR069-R1, WORKING / UNAPPROVED.** Use the **same exact source commit** `6281d3eb7c1c961ce63b0b5e4ae0792874626720` for the app and Main ESP32. Active engineering branch: `release/hazrob-0.10.2-rc2`. **Do not roll back to HR068/HR067/HR050.** No field/production release promotion has taken place; the historical controlled Android baseline remains versionCode 26.

| Component | Latest tested build | Origin and evidence | SHA-256 |
|---|---|---|---|
| **Android APK** | **0.10.2 / HR069-R1 · versionCode 30 · `com.servoc.app`** | [Signed diagnostic APK artifact 11590871098](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815/artifacts/11590871098), [successful build](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815) | `87226e1749d632e2016332a2a204fec09705eb27ebf378f500a9f5a3b8886039` (**APK bytes**) |
| **Main ESP32** | **0.10.2 / HR069-R1 · ESP32 API v4** | [Arduino source and compiled firmware ZIP artifact 11590647043](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034/artifacts/11590647043), [successful Main and CAM compilation](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034) | `28474851e87ea431f5beb38f03ff0c016625960ac9dd129132128d53043f55d6` (**inner firmware ZIP**) |
| **ESP32-CAM** | **0.10.2 / HR069 · unchanged** | Existing camera firmware remains valid; new firmware compiled in [same CI run](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034) for compatibility checking. **Reflashing ESP32-CAM is not required for this update.** | Separate diagnostic artifact; not part of Main ESP32 ZIP |

**Download these two files separately:** (1) [latest APK build](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815/artifacts/11590871098); (2) [latest Main ESP32 firmware](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034/artifacts/11590647043). **GitHub Actions wraps APK artifacts in a ZIP**; unzip the APK artifact to obtain `HazRob-0.10.2-HR069-R1-RC2-WORKING-359-6281d3eb7c1c961ce63b0b5e4ae0792874626720.apk`. The ESP32 artifact contains a ZIP with `HazRob/HazRob.ino`, companion source and compiled firmware. Download may require GitHub login; workflow artifacts have limited retention (configured to 14 days). **Do not confuse ZIP SHA-256 with extracted APK SHA-256.**

**Verification gates passed on exact commit `6281d3eb`:** [Android/firmware integration CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37870937034), [Android internal CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936746), and [signed diagnostic APK CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37870936815). Validation includes Android unit/source contracts, ChemCore and developer/report smoke checks, Arduino-ESP32 core `3.3.11` Main and ESP32-CAM compilations, and **35 host-C++ firmware-replay assertions** covering Echo, IR, direct BME bus recovery and differential drive. APK `apksigner verify` passed. Tests are internal/software-only; they do **not** establish real sensor calibration, on-phone PDF/XLSX rendering, cloud transport reliability or robot safety acceptance.

**Changes since the former vCode28 diagnostic pair:** firmware Echo fault classifications (`no_rising_edge`, `no_falling_edge`, `echo_high_before_trigger`, `invalid_pulse`), non-stale invalid distance and developer-visible diagnostic counts; a recoverable environmental sensor interface with 1 s cadence and communication error counters; Android status/measurement screens expose those counters. **BME280 connects directly to ESP32 GPIO13 (SDA) and GPIO14 (SCL)** on the ESP32's *native secondary I²C controller*, with **no ADS1115, intermediary I²C converter/level shifter or multiplexer in its path**. The MQ ADS1115 devices remain on an independent GPIO21/22 bus. IR semantics remain HIGH = ground present / LOW = no ground; Echo TRIG23/ECHO39 updates every nominal 150 ms, warns at ≤10 cm and blocks forward at ≤5 cm **only if optional operator auto-stop is enabled** (default OFF). The ECHO input must be ≤3.3 V.

**R0 and Rs units verified in the shipped Android vCode30 (already implemented; no APK recompile required):** Developer → `معايرة R0` accepts and displays the eight MQ clean-air `R0` resistances in **kΩ**, and displays the corresponding live `Rs` as **kΩ**, not Ω. The Main ESP32 firmware computes `Rs = RL × (5 V − AO) / AO` with numerical `RL=10.0` **assumed to mean 10 kΩ**; it transfers the resulting `rs` and `r0` numerically in the **same kΩ unit**. Android `ManualR0Calibration.save/saveAll` stores the user's numeric **kΩ** input unchanged in local SharedPreferences with source/session/UTC lineage; `ManualR0Calibration.apply` recomputes the dimensionless `Rs/R0` ratio for **new live** readings. There is **no ×1000 or ÷1000 conversion** on save or on ratio calculation. The firmware field baseline is the fallback (e.g., MQ3 ≈20.686 kΩ, MQ135 ≈213.239 kΩ), not an M4-certified reference-gas calibration; previously recorded session ratios remain tied to the stored historical R0. Single-channel save/reset and atomic eight-channel save are blocked during active capture/analysis. **Required field check:** measure the actual load resistor `RL` of each ready-made MQ breakout board, as the assumed 10 kΩ may not match the physical board and all inferred `Rs`/`R0` magnitudes would then be rescaled. Scientific PPM must not be claimed from a clean-air baseline alone.

**Install safely:** install signed vCode30 APK **over** the existing app without uninstalling (provided signature matches) to preserve local site/session/R0 data; flash **Main ESP32** using `firmware/HazRob/HazRob.ino` (ESP32 Dev Module, Huge APP, ESP32 core `3.3.11`) to activate firmware-side Echo/BME fixes. **Do not reflash ESP32-CAM** unless its separate firmware needs replacing. Lift wheels during motor checks; verify sensor inputs and motor STOP before free driving. Current E-STOP UI activation requires a 2-second hold; use the device only for supervised tests and **never assume it is certified safe for explosive atmospheres or occupational ppm decisions**.

**Checkpoint and limitations:** [2026-10-09 internal hardware/developer test record](docs/0.10.2/HR069-R1_INTERNAL_HARDWARE_AND_DEVELOPER_TESTS_20261009.md). Field verification, actual echo voltage/edge timing, IR floor calibration, BME physical I²C signal, drive current, direct/remote connectivity and genuine Android PDF/XLSX exports are **PENDING**.

---

## Historical engineering checkpoint — HR069-R1 (2026-10-08; superseded by 2026-10-09 diagnostic pair)


**Development branch:** `release/hazrob-0.10.2-rc2` (single branch; do not create another).
**Android:** 0.10.2 / HR069-R1 WORKING diagnostic, `versionCode 28` (validated in CI; controlled production APK authority still remains versionCode 26); **Main ESP32:** firmware API4 / engineering revision `HR069-R1`, identified by `/diag.build_revision`. **ESP32-CAM:** 0.10.2 / HR069, unchanged.
**Current status: WORKING DIAGNOSTIC / NOT DEVICE-ACCEPTED.** CI verified APK/firmware compilation and Android unit contracts for functional SHA `8b89e4c2`; physical IR/Echo, R0 measurement and on-phone PDF/XLSX acceptance remain pending. This does **not** authorize production promotion.

- IR polarity: **HIGH = ground present; LOW = no ground**. Operator manually adjusts the sensor. Automatic verification of state transitions was removed by explicit request; a stuck HIGH sensor can falsely report ground.
- Automatic forward-stop on IR cliff or Echo ≤5 cm is a **persisted, operator-selectable setting in Settings**, default OFF on first install; hazard warnings, E-STOP and watchdog are independent. Do not enable near edges until physical calibration passes.
- Echo acquisition, Direct polling and MQTT telemetry have a **150 ms nominal cadence**, without a guarantee of one new data sample per screen frame. Echo validity and timeout fields remain explicit.
- **Editable device R0 (new):** Developer → **معايرة R0** exposes MQ2/MQ3/MQ4/MQ5/MQ7/MQ8/MQ9/MQ135, with live Rs readback, single-channel save/reset and atomic eight-value revision. Positive finite R0 overrides are synchronously committed to Android SharedPreferences `servoc`, with UTC and calibration-session provenance; `ManualR0Calibration.apply` updates `Rs/R0` in **subsequent** normalized readings. ChemCore Stable updates do not replace local R0; installing a matching package in-place preserves it, but **uninstalling the app deletes its internal preferences unless independently backed up**. Field clean-air 2026-09-13 values remain fallback, not reference-gas M4.
- **Arduino deliverable:** RC2 workflow `.github/workflows/package-hr069-r1-arduino-source.yml` compiles the **exact current Main ESP32** with ESP32 core 3.3.11 and produces conventional `HazRob/HazRob.ino` plus all `.h/.cpp` in a ZIP identified by Git SHA, with Huge APP board instructions. Diagnostic APK and Arduino source are working/unapproved until physical acceptance.
- Android lifecycle dead-man: leaving the app/focus (including SAF picker) clears both motor commands, resets the on-screen control and sends an independent motion-stop request; firmware watchdog remains a final fail-safe.
- Four controls: spring-return dual motor sliders; corrected in-place pivot buttons; independent invert-pivot setting for Joystick/Wheel/Buttons (tank excluded); retain moving-turn steering and STOP.
- Tracking is target-specific for the selected chemical, uses fresh unique MQ sensor sample timestamps and does not invent direction without supporting motion/gradient evidence.
- **User sites and Field Sessions are LOCAL on the phone.** No central upload or reference-bank promotion in user interface. Developer laboratory/reference captures are separated by persisted role and only approved laboratory developer references may be **submitted for scientific review** into the non-operational RC2 staging path; this is not ChemCore promotion. The existing phone-folder SAF archive is a local backup, **not a central upload**.
- Reports: one selected Field Session or **the entire selected Site** (all its sessions); PDF/XLSX use SmartReport; no arbitrarily selected subset report. Site export is asynchronous, with SAF lifecycle protection.
- Site/session deletion removes the closed user's measurement data, excludes developer/reference records and records deletion tombstones for local restore safety.
- New 1024×1024 yellow/white/black targeting-hexagon PNG with transparent **outer** canvas is generated reproducibly by `tools/hazrob_hr069_r1_png.py`; launcher and first connection screen use this asset. Visual/device acceptance remains pending.

**Developer reference publication / ChemCore Stable compatibility:** [CR-20261008-006 audit](docs/0.10.2/CR-20261008-006_DEV_REFERENCE_CHEMCORE_SYNC_AUDIT.md) found an obsolete publisher target (`HazRob/build-harness`), a missing approved Stable publication/distribution pipeline, and an empty default manifest endpoint. Upload does not equal a ChemCore update. Developer staging now targets `research_data/hr069_developer_submissions/`, *not* `HazRob/build-harness`; a separate science-admission and Stable distribution pipeline is still missing. **NOT E2E VERIFIED**; no direct canonical/M4 publication authorized. HR069-R1 adds a local developer-session-aware update interlock, including controlled captures when the ESP32 firmware is idle.

**Current corrective build verified in CI (device validation pending):** Android 0.10.2 / HR069-R1 `versionCode 28` (subsequent to the previously delivered vCode27 diagnostic APK). Historical session replay no longer binds saved MQ readings to a newly edited R0: `ChemCoreSessionFeatureAdapter`, `SessionSeries` and `ChemCoreQuantitativeTimeSeries` use the stored sample's R0/ratio, while current direct measurement still uses the editable device R0. Developer R0 changes are blocked during a live recording/analysis. SmartReport XLSX is permitted for existing USER field sessions even with zero derived ChemCore experiments; missing evidence is reported as missing, not invented. **This vCode28 diagnostic APK passed the three CI workflows at SHA `8b89e4c2`; it supersedes vCode27 only for diagnostic testing, not as an approved production release.**

**Latest verified WORKING diagnostics (NOT production-approved):** Android 0.10.2 / HR069-R1 `versionCode 28`, source SHA `8b89e4c2`, APK SHA-256 `022ba04594cd7b7735e2fd67a37586ee4b9e235eaf5e7e799e170b1e42d19618`, signing-certificate SHA-256 `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`. The separate Main ESP32 Arduino source ZIP remains source SHA `e6a14bc5`, ZIP SHA-256 `6532c3495de23b1d4917ad64882519d3cc9c0cb42fd66634dc92059ba560058e`; Main firmware bytes are unchanged by vCode28 edits. APK embeds ChemCore Stable `1.0.0-hr069r1` with 652 identities, 49 non-M4 priors and editable local 8-channel R0. [Build APK CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37854726140), [Android internal release CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37854726118), [Android/Firmware integration CI](https://github.com/sohpany/SERVOC-Android/actions/runs/37854726087), [verified artifact checkpoint](docs/0.10.2/HR069-R1_EXECUTION_CHECKPOINT_20261008.md). Physical acceptance and owner release approval remain pending.

**Resumable execution checklist:** [HR069-R1 checkpoint](docs/0.10.2/HR069-R1_EXECUTION_CHECKPOINT_20261008.md). The canonical stable 0.10.2 HR069 documentation below remains the historical baseline until HR069-R1 passes physical validation and controlled release approval.



HazRob is a mobile research robot for hazardous-environment reconnaissance, multi-sensor gas **screening**, controlled chemical experiments, environmental sensing, remote inspection, video, evidence capture and traceable analytical research.

## Previously controlled release authority (historical production baseline; latest diagnostic pair is documented above)

| Component | Controlled release |
|---|---|
| Android application | `0.10.2` (`versionCode 26`) |
| Main ESP32 firmware | `HazRob 0.10.2` / API v4 |
| Engineering firmware revision | **HR069** |
| ESP32-CAM firmware | `HazRob-CAM 0.10.2` |
| Hardware profile | `hazrob-0.10.2-hr069-1r8mq-l298n6-batt-ultra-ir-ldr-env2-v2` |
| Android application ID | `com.servoc.app` |
| Analytical platform | ChemCore / AMCI |
| Integration/release branch | `release/hazrob-0.10.2` |

**HR069 is the engineering authority for the final 0.10.2 qualification line.** HR066/HR067/HR068 remain historical integration references only and must not replace HR069 as the active firmware baseline.

> **Research-prototype limitation:** HazRob 0.10.2 is not Ex-certified or intrinsically safe. The current ESP32/MQ/electromechanical architecture must not be represented as approved for classified explosive atmospheres. MQ response curves and research models are not substitutes for traceable analyte-specific calibration. Occupational exposure interpretation is gated by the available validation/evidence state.

## 1. 0.10.2 architecture

The controlled system consists of five cooperating layers:

1. **Main ESP32 / HR069** — motion, safety interlocks, sensor acquisition, Direct-mode HTTP API, Remote-mode MQTT transport and local network routing.
2. **ESP32-CAM** — STA-only local MJPEG and authenticated remote JPEG/WSS publisher.
3. **Android 0.10.2** — operator UI, robot control, measurements, analytical processing, ChemCore/AMCI, reports, user library and update management.
4. **Railway** — current bootstrap authority and remote JPEG relay.
5. **MQTT service** — **EMQX Cloud Serverless is the primary broker**. **HiveMQ Cloud is retained as a controlled contingency/fallback**, not the canonical primary path.

The analytical path is:

`Robot/acquisition → controlled measurement → ChemCore features → evidence/model inference → OOD/UNKNOWN gate → quantification gate → AMCI/re-measurement → exact-session report`

The scientific library is versioned independently from the APK. Approved Stable library packages can therefore evolve without rebuilding the application.

## 2. Connection modes

### 2.1 Direct mode — default and safety baseline

HazRob starts Direct-first. Saved WAN credentials do not silently activate Remote mode after boot.

- robot AP SSID: `HazRob`;
- Main ESP32: `192.168.4.1`;
- ESP32-CAM: `192.168.4.2`;
- ESP32-CAM is **STA-only** and must not advertise its own `ESP_*` access point;
- Android binds robot traffic to the HazRob Wi-Fi path;
- Main HTTP API v4 carries drive, STOP/E-STOP, sensor data, measurement and configuration operations;
- local camera status is served on port 80 and MJPEG on port 81.

Direct mode must remain usable without Internet, Railway, EMQX or HiveMQ.

### 2.2 Remote mode

Remote activation is explicit. Switching transport must not bypass motion or safety interlocks.

**Control/telemetry path:**

`Android ↔ TLS/MQTT ↔ EMQX ↔ TLS/MQTT ↔ Main ESP32`

**Video path:**

`ESP32-CAM → TLS/WSS → Railway video relay → TLS/WSS → Android`

**Bootstrap path:**

`Android/Main/CAM → HTTPS/TLS → Railway bootstrap`

Current remote contract:

- service version: `0.10.2`;
- wire protocol: `2`;
- robot identity: `hazrob-001`;
- MQTT primary: EMQX Cloud Serverless, TLS port `8883`;
- MQTT contingency: HiveMQ Cloud, selected explicitly when needed;
- Railway provides bootstrap and video relay;
- credentials/tokens are role-scoped and are not committed to the repository;
- Internet reachability alone is not remote readiness.

Remote readiness requires TLS validation, MQTT authentication/authorization, fresh command/ACK exchange, telemetry, status and — when video is required — an authenticated decodable JPEG stream.

### 2.3 MQTT topics

Canonical namespace: `hazrob/hazrob-001/#`.

- commands: `hazrob/hazrob-001/cmd`;
- ACK: `hazrob/hazrob-001/ack`;
- telemetry: `hazrob/hazrob-001/telemetry`;
- retained status: `hazrob/hazrob-001/status`.

Command envelopes use session, sequence, nonce and TTL controls. Replayed/stale commands are rejected. MQTT connectivity never overrides E-STOP or firmware motion interlocks.

## 3. Main ESP32 HR069 hardware authority

Firmware source under `firmware/HazRob/` is the executable authority. Documentation must follow the firmware, not override it.

### 3.1 4WD / L298N

The robot uses differential 4WD with two motors per side. It does **not** use servo propulsion.

| Function | GPIO |
|---|---:|
| Logical LEFT PWM enable | 17 |
| Logical LEFT direction | 19 / 32 |
| Logical RIGHT PWM enable | 16 |
| Logical RIGHT direction | 18 / 27 |

The physical steering handedness is accounted for in the firmware mapping: logical LEFT maps to L298N channel B and logical RIGHT to channel A.

ENA/ENB jumpers are removed when GPIO16/GPIO17 provide PWM enable control. IN1..IN4 are direction lines. STOP/E-STOP, command lease/timeout, ramping, link-loss handling and sensor interlocks remain firmware responsibilities.

### 3.2 IR ground/cliff protection — HR069

- GPIO: **35**;
- input-only ESP32 pin;
- installed downward at the lower/front robot area;
- **latest HR069-R1 WORKING** ground-detected active level: **HIGH** (LOW means no ground);
- IR is treated as a **digital ground/cliff detector**, not a distance sensor;
- latest actual reported sensor installation height: **less than 4 cm** (the firmware reference is 4 cm);
- cliff assertion requires **3 consecutive samples**;
- clearing requires **2 consecutive ground samples**.

A persistent loss of ground sets a cliff warning and **blocks forward-component motion only when the optional automatic-stop setting is enabled** (the setting defaults OFF on a first install). Reverse and appropriate escape/pivot movement remain available so the operator can recover the robot.

### 3.3 Ultrasonic / ECHO collision protection — HR069

- TRIG: **GPIO23**;
- ECHO: **GPIO39**;
- ECHO is input-only;
- a 5 V ECHO signal **must be level-reduced to ≤3.3 V before GPIO39**;
- warning threshold: **≤10 cm**;
- forward-stop threshold: **≤5 cm**;
- timeout: `25,000 µs`;
- nominal sampling period: `150 ms`.

Ultrasonic acquisition is implemented as a non-blocking state machine rather than a long blocking `pulseIn()` path. The purpose is to preserve control-loop, MQTT and acquisition responsiveness.

At ≤5 cm, forward-component motion is blocked **only when the optional automatic-stop setting is enabled**. Reverse/pivot escape remains possible. The Android UI receives the warning/blocked state; enforcement remains in firmware so UI/network failure cannot remove the physical safety gate.

### 3.4 Other Main ESP32 channels

| Device/function | GPIO / bus |
|---|---|
| MQ common relay | GPIO25, active LOW |
| Alert control | GPIO26 — reserved logic output; alert hardware currently not installed |
| MAX4466 sound | GPIO36 / ADC1 |
| IR ground/cliff | GPIO35 |
| LDR/LM393 threshold | GPIO34 |
| Battery divider | GPIO33 / ADC1 |
| Ultrasonic TRIG | GPIO23 |
| Ultrasonic ECHO | GPIO39 |
| ADS1115 I2C | SDA21 / SCL22, 400 kHz |
| Environmental I2C | SDA13 / SCL14, 100 kHz |

GPIO34/35/36/39 are input-only pins. ESP32 GPIO is a 3.3 V logic domain; 5 V digital signals or 5 V I2C pull-ups must not be applied directly to ESP32 GPIO.

The environmental bus is deliberately separated from the analytical ADS1115 bus so an ENV-device fault does not share the same I2C path as the MQ ADC acquisition.

## 4. MQ analytical array

Eight MQ channels are installed and share one active-LOW power relay on GPIO25.

- relay mask `0`: all MQ OFF;
- relay mask `1`: all MQ ON;
- unsupported masks are rejected.

### ADS1115 map

| ADC | Address | A0 | A1 | A2 | A3 |
|---|---|---|---|---|---|
| ADS1 | `0x48` | MQ2 | MQ4 | MQ5 | MQ8 |
| ADS2 | `0x49` | MQ3 | MQ7 | MQ9 | MQ135 |

Controlled software acquisition assumption:

- ADS1115 supply domain: 5 V design assumption requiring physical verification on the assembled robot;
- PGA: ±6.144 V;
- data rate: 860 SPS;
- four conversions averaged per channel;
- channels are scanned sequentially, not physically simultaneously;
- timing/scan provenance is retained for downstream analysis.

### Field R0 baseline currently embedded in HR069

The current device-specific field clean-air normalization was accepted from three independent 30 s sessions on 2026-09-13, 216 samples total. Session Rs values were aggregated geometrically and sessions were weighted equally in log space.

This is a **field clean-air normalization baseline**, not a reference-gas certificate and not sufficient by itself for strong M4 quantitative ppm claims. The embedded metadata explicitly records that reference-gas validation and environmental compensation were not performed for that baseline.

## 5. Sampling chamber

Controlled analytical profile: `hazrob-chemcore-chamber-v2`.

- internal geometry: 90 × 40 × 150 mm;
- gross volume: 0.540 L;
- estimated effective air volume after MQ domes: ≈0.517 L;
- centered inlet: 40 × 20 mm;
- rear fan opening: ≈28 × 28 mm;
- extraction fan: 40 × 40 mm, 5 V / 0.28 A;
- engineering flow prior: ≈1.10 ± 0.40 L/s;
- estimated mean chamber velocity: ≈0.31 m/s;
- flow evidence remains `FLOW_ESTIMATED` until physically measured.

Changing chamber geometry, fan configuration or sensor placement requires a new Sampling Profile. Reference datasets from materially different profiles must not be silently mixed.

## 6. ESP32-CAM

Source: `firmware/HazRob-CAM/`.

The camera is STA-only and uses the Main ESP32 network. In Direct mode it provides local status/MJPEG. In Remote mode it waits for the Main ESP32 remote-status gate before starting the authenticated Railway WSS publisher.

Remote publishing requires, among other conditions:

- Main radio profile reports Remote;
- WAN/Internet/NAPT readiness is available;
- time is synchronized sufficiently for TLS validation;
- bootstrap service contract matches version/protocol/client/robot identity;
- publisher token is valid.

A local MJPEG client takes priority over unnecessary simultaneous remote publishing. Remote reconnect uses bounded backoff to avoid reconnect storms.

## 7. Android 0.10.2

Android is the operator and analytical platform. The application provides:

- Direct/Remote connection selection;
- live video and robot driving;
- floating drive controls over the video interface;
- STOP/E-STOP and motion-state feedback;
- IR/cliff and ultrasonic proximity warnings;
- live measurement acquisition;
- advanced analysis and candidate ranking;
- ChemCore/AMCI analytical workflows;
- developer-only experimental/calibration functions;
- exact-session reports;
- user-owned HazRob library backup/restore;
- application and Stable scientific-library updates.

The developer area remains access-controlled by the application design; normal users receive the operational interface without needing deployment credentials.

## 8. Measurement lifecycle

Focused analysis requires the controlled path:

`CONDITION → BASELINE → EXPOSURE → STABILITY → FEATURES → IDENTITY → OPEN-SET → QUANTIFICATION GATE → CONSISTENCY/MIXTURE CHECK → REPORT → RECOVERY`

Survey measurements collected while the robot is moving may be useful reconnaissance data, but they are not automatically promoted to controlled quantitative analysis.

## 9. ChemCore / AMCI

ChemCore is the analytical/metrological layer. AMCI is the adaptive decision layer that decides whether evidence is sufficient or whether additional exposure, recovery, repetition or spatial confirmation is needed.

### Feature schema

`chemcore-features/1` generates 17 features per MQ channel × 8 channels = **136 features**. Features include baseline, steady-state response, signed and absolute response changes, extrema, slope, T10/T50/T90, signed/absolute AUC, baseline noise and SNR. Signed response direction is retained.

### Evidence states

- `M0` — catalogued only;
- `M1` — molecular/QSPR inferred;
- `M2` — manufacturer/literature supported;
- `M3` — array-consistent identification;
- `M4` — HazRob device-validated.

Possible analytical outcomes include `SCREENING`, `IDENTIFIED`, `QUANTIFIED`, `UNKNOWN`, `INSUFFICIENT_EVIDENCE`, `OUT_OF_VALIDATED_DOMAIN`, `POSSIBLE_MIXTURE_OR_INTERFERENCE` and `INVALID_MEASUREMENT`.

Occupational exposure-limit interpretation is not promoted to quantitative use unless the required validation state exists.

## 10. Scientific knowledge and molecular layer

The embedded ChemCore identity seed contains 652 occupational/chemical identities. Current molecular enrichment resolves 615 molecular records and explicitly curates 37 unresolved/material/mixture/category records rather than fabricating single-molecule identities.

Molecular descriptors are priors/features. They do not prove that an MQ sensor uniquely detects a molecule or its constituent atoms.

Pinned/reference project datasets include:

- 652 NIOSH-indexed identities;
- 402 NIOSH IDLH records;
- 638 normalized OSHA Table Z-1 rows covering 432 substances;
- 144-compound analytical-priority/compatibility catalogue;
- adopted/pinned MQDataScience 76-model research catalogue.

These are knowledge-library counts, **not a claim that the current MQ array quantitatively measures every listed substance**.

## 11. Scientific-library and application updates

APK releases and ChemCore Stable releases are independent.

For normal users, the Update function can check the application and approved Stable scientific library. Candidate/research records remain developer-controlled.

Stable library installation is fail-closed:

1. download/stage;
2. verify SHA-256;
3. verify signature;
4. verify compatibility/schema;
5. block activation during an active measurement/analysis;
6. activate atomically;
7. retain rollback capability.

Private signing keys and GitHub/deployment credentials must never be embedded in the APK.

## 12. User data and exact-session reporting

0.10.2 maintains a HazRob user workspace/library on installation and separates user-owned field records from controlled reference-bank content.

Field-session persistence and report generation are session-specific. Reports must not silently mix measurements from unrelated sessions. Backup/restore must preserve user field sessions and measurement links while preventing field archives from being imported as controlled Reference Bank experiments.

PDF/XLSX reporting can carry session ID, hardware profile, Sampling Profile, feature schema, model/version/hash, library version/SHA/source commit, evidence tier, uncertainty/OOD state and candidate ranking when available.

## 13. Remote service deployment

### Railway

Railway is the current 0.10.2 production authority for bootstrap and remote video relay. The controlled relay source is under `remote/video-relay/` and is built from `release/hazrob-0.10.2`.

### EMQX / HiveMQ

- **EMQX:** canonical primary MQTT broker.
- **HiveMQ:** retained contingency broker; do not delete it from recovery planning.
- Broker selection in deployment uses provider-neutral configuration where possible.
- A contingency switch must be controlled; do not allow the robot and Android to silently migrate to different brokers.

### Legacy Render note

Render was used in earlier HazRob deployment paths and may still host legacy/update services. It is **not the HR069 bootstrap/video production authority**. Documentation or comments that describe Render as the current Remote video/bootstrap authority are stale and should be treated as defects.

## 14. Build and release control

### Android

The controlled Android build uses:

- compile/target SDK 36;
- min SDK 26;
- Java 17;
- `versionName 0.10.2`;
- `versionCode 26`;
- unit/regression tests before release assembly;
- signed release APK;
- signing-certificate verification;
- APK SHA-256 output;
- application update manifest generation.

### Firmware

Workflow `.github/workflows/build-firmware.yml` builds both targets from `release/**`:

- Main ESP32 HR069;
- ESP32-CAM HR069;
- ESP32 Arduino core pinned to `3.3.11`;
- warning-enabled compile;
- binary packaging;
- SHA-256 manifest.

Canonical artifact family:

`HazRob-0.10.2-HR069-FIRMWARE-<commit>`

### Release qualification

0.10.2 is not considered final merely because source files exist. Release acceptance requires successful build/test execution plus physical-device acceptance for hardware-dependent behavior.

Current qualification gates include:

- source/integration contracts;
- Android unit/regression tests;
- signed APK build and certificate verification;
- Main ESP32 compile;
- ESP32-CAM compile;
- Direct-mode integration;
- Remote Railway/EMQX integration;
- IR cliff behavior;
- ultrasonic 10 cm warning / 5 cm forward-stop behavior;
- STOP/E-STOP and command-timeout behavior;
- camera Direct/Remote operation;
- sensor and acquisition sanity checks;
- physical on-device acceptance.

A CI job that never receives a GitHub runner is an infrastructure blocker, not evidence that source compilation failed; it must nevertheless be rerun successfully before final release acceptance.

## 15. Flashing acceptance checklist

When replacing an unknown/old firmware build:

1. erase Main ESP32 flash;
2. erase ESP32-CAM flash;
3. flash the binaries produced from the same HR069 source commit;
4. verify Main advertises `HazRob` and responds at `192.168.4.1`;
5. verify camera does not advertise an independent ESP AP;
6. verify camera status at `192.168.4.2` and MJPEG at port 81;
7. verify motor direction and STOP before free driving;
8. physically verify IR active polarity and cliff response;
9. physically verify ultrasonic distance and 10/5 cm gates;
10. verify ADS/MQ/environmental channels;
11. verify Direct mode without Internet;
12. only then test Remote Railway/EMQX operation;
13. record firmware SHA/source commit in the test record.

## 16. Safety and scientific limitations

Before strong M4 quantitative claims, the following still require controlled physical/metrological validation as applicable:

- actual module load-resistance characterization;
- device-specific/reference-gas calibration;
- MQ7/MQ9 heater-cycle validation where the module/application requires it;
- analyte-specific concentration-response calibration;
- interference and mixture validation;
- drift/repeatability/reproducibility assessment;
- environmental compensation validation;
- independent/blind validation;
- physical chamber airflow characterization.

The current field R0 baseline improves device normalization but does not replace those requirements.

## 17. Repository map

- Android: `app/`
- Main ESP32: `firmware/HazRob/`
- ESP32-CAM: `firmware/HazRob-CAM/`
- Remote relay: `remote/video-relay/`
- MQTT contract: `remote/mqtt/`
- Release/audit tooling: `tools/`
- GitHub workflows: `.github/workflows/`
- controlled scientific references: `REFERENCES_APA.md` and `docs/CHEMCORE_RESEARCH_REFERENCES_2024_2026.md`
- change history: `CHANGELOG.md`

## 18. Release rule

For the 0.10.2 line, the release identity is:

**Android 0.10.2 + Main ESP32 HR069 + ESP32-CAM 0.10.2 + Railway bootstrap/video + EMQX primary MQTT + HiveMQ contingency.**

Do not regress the active release baseline to HR067/HR068 or to the old Render/HiveMQ-primary architecture. Any future architecture change must be explicit, tested and documented rather than inferred from legacy files.
