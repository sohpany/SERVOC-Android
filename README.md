# HazRob 0.10.2

HazRob is a low-cost mobile robotic platform for remote hazardous-environment reconnaissance, multi-gas **screening**, controlled chemical analysis, environmental sensing, video inspection, evidence capture and research-grade traceability.

**Release:** Android `0.10.2` · Master ESP32 `HazRob 0.10.2` / API v4 · ESP32-CAM `HazRob-CAM 0.10.2`  
**Physical hardware profile:** `hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2`  
**Analytical platform:** ChemCore / AMCI  
**Application ID:** `com.servoc.app`

> HazRob 0.10.2 is a research prototype. The current MQ/ESP32 architecture is **not Ex-certified or intrinsically safe** and must not be represented as suitable for classified explosive atmospheres. Generic/manufacturer MQ response curves are not device calibration certificates. Quantitative occupational interpretation requires analyte-specific validated calibration and the applicable measurement domain.

## What changed in 0.10.2

0.10.2 is the controlled continuation of the HR067/HR066 integration baseline for the ChemCore-generation HazRob platform. It keeps the analytical foundation introduced in 0.10 while unifying the Android application, main ESP32, ESP32-CAM, canonical GPIO allocation, Direct transport, Remote MQTT/WSS transport, Render/HiveMQ contracts, scientific datasets, build workflows and release artifacts under one version-controlled baseline.

The software separates:

`Robot + acquisition firmware → controlled measurement → ChemCore features → evidence/model inference → OOD/UNKNOWN gate → gated quantification → AMCI/re-measurement → report`

The scientific library is versioned independently from the APK so approved experimental knowledge can evolve without rebuilding the application.

## Hardware source of truth

The authoritative human-readable pin map is [`docs/0.10.1/GPIO_AUTHORITY.md`](docs/0.10.1/GPIO_AUTHORITY.md). Firmware source remains the executable authority. Any active documentation that disagrees with those sources is a release defect.

### 4WD motion — no servo drive

The current robot uses an **L298N differential 4WD chassis**, not servo propulsion:

- logical LEFT speed enable = GPIO17 (L298N channel B);
- logical LEFT direction = GPIO19 / GPIO32;
- logical RIGHT speed enable = GPIO16 (L298N channel A);
- logical RIGHT direction = GPIO18 / GPIO27;
- two motors per side are wired in parallel to the corresponding bridge outputs;
- **ENA and ENB jumpers are removed** when GPIO16/GPIO17 control the enable inputs;
- speed PWM is applied to ENA/ENB, while IN1..IN4 are direction-only control lines;
- drive lease, ramping, STOP/E-STOP, link-loss hold and retrace remain firmware safety functions.

`SERVO_DRIVE=false` is locked by the Android release audit.

### Eight-channel MQ array

All eight installed MQ modules share **one active-LOW power relay on GPIO25**:

- `0` = all MQ OFF;
- `1` = all MQ ON;
- other masks are invalid.

ADS1115 acquisition is independent of the common power relay:

| ADC | Address | Channels |
|---|---|---|
| ADS1 | `0x48` | MQ2=A0, MQ4=A1, MQ5=A2, MQ8=A3 |
| ADS2 | `0x49` | MQ3=A0, MQ7=A1, MQ9=A2, MQ135=A3 |

Both ADS1115 boards are **fixed at the regulated 5 V HazRob rail**. Firmware uses a ±6.144 V PGA full-scale, `860 SPS`, and four conversions averaged per channel. The eight channels are scanned sequentially, and the acquisition path records scan-cycle/channel timing provenance so downstream analysis does not falsely assume physical simultaneity.

`R0=10.0` and `RL=10.0` defaults are placeholders/engineering assumptions until the actual sensor modules are characterized. They must not authorize M4 quantitative output.

### Other master-ESP32 channels

- alert output: GPIO26 (reserved; alert hardware not installed in this hardware profile);
- MAX4466 electrical sound channel: GPIO36 / ADC1;
- IR digital warning: GPIO35;
- LDR/LM393 digital threshold: GPIO34;
- battery divider ADC: GPIO33 / ADC1;
- ultrasonic TRIG/ECHO: GPIO23 / GPIO39;
- I2C SDA/SCL: GPIO21/GPIO22 at 400 kHz;
- BME280/BMP280 auto-detection: `0x76/0x77`;
- GPIO14: reserved for a future internal-temperature device only.

GPIO34/35/36/39 are used only as inputs, as required by the ESP32 hardware. ESP32 GPIO is a 3.3 V logic domain: 5 V ECHO or 5 V I2C pull-ups must not be presented directly to GPIO.

## Standard ChemCore sampling chamber

Sampling profile `hazrob-chemcore-chamber-v2` is the controlled analytical reference profile:

- chamber: 90 × 40 × 150 mm;
- gross internal volume: 0.540 L;
- estimated effective air volume after MQ domes: ≈0.517 L;
- centered inlet: 40 × 20 mm;
- centered rear fan opening: ≈28 × 28 mm;
- 40 × 40 mm, 5 V / 0.28 A extraction fan downstream of the array;
- engineering flow prior: ≈1.10 ± 0.40 L/s;
- estimated mean chamber velocity: ≈0.31 m/s;
- flow status remains `FLOW_ESTIMATED` until a physical measurement upgrades the evidence state.

Changing chamber geometry, fan exposure or sensor layout requires a new Sampling Profile rather than silently mixing reference data.

## ChemCore / AMCI

ChemCore is the analytical/metrological layer. AMCI is the adaptive decision layer that determines whether evidence is sufficient or more exposure, recovery, repetition or spatial confirmation is needed.

### Canonical feature pipeline

`chemcore-features/1` produces 17 features per MQ channel × 8 channels = **136 features**, including baseline, steady-state, signed/absolute response change, extrema, slope, T10/T50/T90, signed/absolute AUC, baseline noise and SNR. Signed response is retained; response direction is not discarded.

### Evidence states

- `M0` catalogued only;
- `M1` molecular/QSPR inferred;
- `M2` manufacturer/literature supported;
- `M3` array-consistent identification;
- `M4` HazRob device-validated.

Normal results may be `SCREENING`, `IDENTIFIED`, `QUANTIFIED`, `UNKNOWN`, `INSUFFICIENT_EVIDENCE`, `OUT_OF_VALIDATED_DOMAIN`, `POSSIBLE_MIXTURE_OR_INTERFERENCE`, or `INVALID_MEASUREMENT`.

Occupational-limit interpretation is gated from quantitative use unless the required device-specific validation state is present.

### Molecular intelligence

The embedded ChemCore seed contains **652 occupational/chemical identities**. The current molecular enrichment resolves **615** to molecular records and explicitly curates **37** unresolved/material/mixture/category records rather than inventing single-molecule identities for them.

The knowledge layer stores molecular descriptors with provenance and supports physical feasibility checks, QSPR-MOX research models, hierarchical candidate reduction and inverse full-array consistency solving. Molecular properties are priors/features—not proof that an MQ sensor measures constituent atoms or uniquely identifies a molecule.

## Dynamic scientific-library updates

Application releases and scientific-library releases are independent.

- Research/candidate records remain developer-controlled.
- Only approved **Stable** snapshots are distributed to normal users.
- The Status screen exposes the normal-user **Update** action.
- The same action checks the Android application first and then the ChemCore Stable library.
- Button state reports checking, updating, completed, or no update.
- Library installation is blocked during active measurement/analysis.
- Packages are staged, SHA-256 checked, signature verified, compatibility checked and atomically activated with rollback support.
- GitHub credentials/private signing keys are never embedded in the APK.

Developer mode retains source override, research workflow and rollback controls.

## Direct and remote communications

### Direct mode

- robot AP: `HazRob`;
- master ESP32: `192.168.4.1`;
- ESP32-CAM: `192.168.4.2`;
- master HTTP API v4 provides drive, STOP/E-STOP, telemetry, MQ power, measurement and configuration endpoints;
- local video is MJPEG from ESP32-CAM (`:81` stream, HTTP/status on port 80);
- Android binds Direct traffic to the HazRob `192.168.4.0/24` Wi-Fi path rather than relying on the default cellular route.

### Remote mode

- the operator is not asked for a HazRob remote password;
- passwordless operator UX does **not** mean anonymous transport: service credentials are provisioned automatically and role-scoped;
- robot control/telemetry: MQTT 3.1.1 over TLS through the configured HiveMQ endpoint;
- command envelopes use session/sequence/nonce/TTL and application ACK semantics;
- retained MQTT Last Will marks an unexpectedly disconnected robot unavailable;
- remote video: binary JPEG over WSS through the Render relay;
- ESP32-CAM uses bounded reconnect backoff to avoid reconnect storms;
- Render relay validates role-scoped bootstrap credentials, frame bounds and publisher/viewer freshness.

Secrets are not committed to source control. Required deployment variable names and contracts are version-controlled; live values remain in their deployment environments.

## Measurement lifecycle

Focused ChemCore analysis requires the robot stationary and follows a controlled state path:

`CONDITION → BASELINE → EXPOSURE → STABILITY → FEATURES → IDENTITY → OPEN-SET → QUANTIFICATION GATE → CONSISTENCY/MIXTURE CHECK → REPORT → RECOVERY`

Survey measurements may be collected while moving, but they are not promoted to controlled quantitative analysis.

## Scientific data and traceability

Pinned project datasets include:

- 652 NIOSH-indexed chemical identities;
- 402 NIOSH IDLH records;
- 638 normalized OSHA Table Z-1 rows covering 432 substances;
- a 144-compound analytical-priority/compatibility catalogue;
- the adopted/pinned MQDataScience 76-model research catalogue.

These counts describe knowledge/research scope—not 652 gases that the current hardware can quantitatively measure.

Every ChemCore decision/report can carry session ID, hardware profile, Sampling Profile, feature-schema version, model/version/hash, library version/SHA/source commit, evidence tier, OOD/uncertainty state and candidate ranking.

## Reports

PDF and XLSX outputs are tied to the exact measurement session and include ChemCore provenance when available. Reports deliberately distinguish screening/model support from validated quantitative measurement.

## Firmware layout and capacity policy

Master firmware: `firmware/HazRob/`  
Camera firmware: `firmware/HazRob-CAM/`

The firmware is modular but deliberately compact. ChemCore, molecular databases and heavy inference remain on Android rather than the ESP32. GitHub Actions compiles both targets and enforces a release flash-headroom budget; comments/source formatting are not treated as Flash savings—the compiled binary is the metric that matters.

## Build and release control

Authoritative integration branch: `dev/hazrob-0.10.1`. Public application/firmware publication remains gated during integration and is enabled only from the explicit `release/hazrob-0.10.1-ready` release branch after the audit gates pass.

GitHub Actions validates pinned scientific datasets, executes Android regression tests, builds/signs the release APK, verifies the signing certificate and creates the application-update manifest. A separate firmware workflow compiles both ESP32 targets and records Flash usage. ChemCore Stable publication has an independent signed fail-closed workflow.

Controlled changes are recorded in [`docs/0.10.1/CONTROLLED_CHANGE_LOG.md`](docs/0.10.1/CONTROLLED_CHANGE_LOG.md).

## Research references

The canonical standards/manufacturer/software-provenance bibliography is [`REFERENCES_APA.md`](REFERENCES_APA.md). The recent peer-reviewed evidence specifically used to design ChemCore preprocessing, molecular-descriptor response modeling, drift handling, open-set recognition, uncertainty/selective classification, calibration transfer and mixture analysis is maintained in [`docs/CHEMCORE_RESEARCH_REFERENCES_2024_2026.md`](docs/CHEMCORE_RESEARCH_REFERENCES_2024_2026.md).

## Current scientific limitations

The 0.10.1 software integration can be released independently of unfinished physical validation. Still required before strong M4 quantitative claims are: actual module `RL` characterization, device-specific `R0`, MQ7/MQ9 heater-cycle validation, reference-gas/analyte-specific calibration, interference/mixture validation, drift validation and independent/blind validation. ADS1115 supply is already a fixed **5 V design value**; it is not an unresolved requirement.
