# HazRob 0.10.2 / HR069 — Operator UI, analytics and reporting workplan

Status: ACTIVE / RESUMABLE
Created: 2026-09-30
Last updated: 2026-09-30
Scope: `release/hazrob-0.10.2` / HR069 only
Starting HEAD: `a29101b3c3abda8ab7127087b2d403b7e606cdf4`
Current pre-qualification checkpoint: `65a723a023b2437c3e8fbd6057d5ae4696a50ce8`

## Scope rule

This workplan is intentionally limited to the current HazRob 0.10.2 / HR069 implementation. Historical HR047/HR050/HR061/HR067 UI assumptions are not used as design authority. Current code, current ChemCore payloads, current HR069 telemetry and current operator requirements are authoritative.

## Operator requirements captured on 2026-09-30

1. Landscape driving must present the live camera full-screen with only driving controls, live hazard overlays and essential status indicators floating over video.
2. Driving modes are independent and must all remain available:
   - dual independent tank sliders;
   - steering wheel;
   - two-axis joystick;
   - eight-direction buttons plus STOP.
3. Differential-drive semantics:
   - tank right up + tank left down => right forward / left reverse;
   - tank right down + tank left up => right reverse / left forward;
   - wheel/joystick 3 o'clock => pivot right (left forward / right reverse);
   - wheel/joystick 9 o'clock => pivot left (left reverse / right forward);
   - diagonals produce progressive differential steering;
   - forward steering slows the inside physical side rather than sending identical speeds.
4. Driving overlay must include live gas response, MQ relay ON/OFF, Wi-Fi strength, forward Echo distance in cm, and IR ground/cliff state.
5. IR semantics: ground signal present => no cliff warning; ground signal absent => cliff warning.
6. Measurement, analysis and results UI must be operational dashboards, not long explanatory paragraphs.
7. Reports must expose analytical evidence, confidence, residuals, fingerprint/curve outputs, safety gates and traceability without pretending screening hypotheses are M4 reportable ppm.
8. Text/background contrast must remain readable in all operator views.

## Execution ledger

| Phase | Scope | State | Acceptance gate |
|---|---|---|---|
| UI-01 | Landscape full-screen driving | **DONE — SOURCE VERIFIED** | camera occupies full content area; system chrome hidden; controls/status float over video |
| UI-02 | Four independent drive modes | **DONE — SOFTWARE MATRIX LOCKED** | TANK/WHEEL/JOYSTICK/BUTTONS all selectable; pivot/progressive matrix locked by `HR069OperatorDriveTest`; physical wheel acceptance still requires robot |
| UI-03 | Drive hazard/status overlays | **DONE — SOURCE VERIFIED** | gas, relay, Wi-Fi, Echo cm and IR ground/cliff visible on driving screen; protocol normalizes raw Echo `cm` to UI `distance_cm` |
| UI-04 | Measurement workspace redesign | **DONE — PRE-QUALIFICATION** | four operational KPI cards, progress, power/start/cancel/save actions, concise per-channel diagnostics |
| UI-05 | Analysis/results dashboard redesign | **DONE — PRE-QUALIFICATION** | top hypothesis, posterior/margin, OOD/entropy, evidence tier, quantitative gate, dynamic candidates, on-demand 144 panel, expandable residuals |
| UI-06 | Smart reports and charts | **DONE — PRE-QUALIFICATION** | smart report dashboard; 20 existing analytical charts retained; evidence-aware chart availability; `ReportIntelligence` state/recommendation policy; PDF/XLSX remain session-bound |
| UI-07 | Contrast/readability audit | **DONE — PRE-QUALIFICATION** | explicit dark/light foreground/background primitives; luminance-aware candidate text; dark sensor details; no dependence on phone theme for button contrast |
| UI-08 | HR069 current-source qualification | **ACTIVE** | current HR069 source contract + selected current tests + Android compile + firmware compile; no APK/release artifact |

## Implementation checkpoints

- `6170600a...` — full-screen drive, four-mode presentation and live overlays introduced.
- `60413200...` — current HR069 drive acceptance matrix added.
- `ed9f9877...` — measurement and ChemCore operator dashboards rebuilt.
- `986bd1e8...` — smart report workspace rebuilt.
- `ed1289e1...` — `ReportIntelligence` evidence/readiness policy added.
- `167079b0...` — report intelligence unit tests added.
- `7d57844d...` — theme-independent contrast primitives added.
- `65a723a0...` — current HR069 operator UI contract extended through UI-07.

## Current implementation details

### UI-01 / Driving landscape

`ControlTab` detects landscape, makes `cameraBox` occupy the content area, hides surrounding application chrome/system bars while driving and places controls as floating overlays over live video.

### UI-02 / Four independent drive modes

`DriveMode` contains exactly `TANK`, `WHEEL`, `JOYSTICK`, `BUTTONS`. The Joystick is an additional mode and does not replace the steering wheel.

The canonical differential matrix is:

| Demand | Left | Right |
|---|---:|---:|
| Forward | +100 | +100 |
| Reverse | -100 | -100 |
| Pivot right / 3 o'clock | +100 | -100 |
| Pivot left / 9 o'clock | -100 | +100 |
| Forward-right | +100 | +50 |
| Forward-left | +50 | +100 |
| Reverse-right | -100 | -50 |
| Reverse-left | -50 | -100 |

`DualTankDriveControl` emits each physical side independently. `SteeringWheelView` and `JoystickView` use the same `DriveMixer`. `DirectionPadView` uses the equivalent fixed `DiscreteDrive` commands.

Software mapping is therefore internally consistent. If the robot does not physically pivot or slow the intended inside side after this source is installed, the next acceptance step is physical wiring/channel mapping verification rather than changing the mathematical mixer blindly.

### UI-03 / Situational overlays

Driving overlay contains:

- live gas-response state;
- floating MQ relay ON/OFF control;
- Wi-Fi RSSI/quality;
- Echo forward distance in cm;
- IR ground/cliff state;
- flame/camera/tracking state;
- E-STOP, speed and current left/right command.

Raw firmware Echo telemetry publishes `cm`; `RobotProtocolV4` normalizes it into canonical `device.proximity.distance_cm` before `ControlTab` displays it. IR uses `ground_detected` and `cliff_warning`; ground present is safe, ground absent is a cliff warning.

### UI-04 / Measurement workspace

The live measurement screen now answers at a glance:

1. MQ power and ADS readiness;
2. valid/available MQ channels and unstable channels;
3. array response state and responding channel count;
4. controlled session state, progress and remaining time;
5. direct operator actions for MQ power, start, cancel and save.

Per-sensor cards retain Raw/Vadc/Vao/Rs/Rs-R0, baseline delta, direction, noise CV, R0 synchronization and health state but no longer dominate the top of the screen with long narrative status text.

### UI-05 / ChemCore analysis and results

The analysis dashboard prioritizes:

- analytical readiness;
- top candidate;
- posterior and separation margin;
- evidence tier/outcome;
- quantitative PPM gate;
- MQ/ENV quality, OOD and normalized entropy;
- compact candidate preview.

Equations and residuals are hidden in an operator-selectable technical-details area. Results show dynamic candidate cards, optional full 144 screening, safety/exposure state and operator candidate tracking. Selecting/tracking a candidate is explicitly not treated as automatic identity acceptance.

### UI-06 / Smart reports and charts

The current chart engine already derives points from either recorded `SessionSeries` data or a saved ChemCore payload. It does not synthesize missing time-series or ChemCore points.

The report workspace now presents session, decision, quality, safety and candidate KPIs first. Each selected chart states its evidence source or the exact missing prerequisite.

`ReportIntelligence` provides report states:

- `EMPTY`;
- `SIGNAL_ONLY`;
- `ANALYTICAL_ONLY`;
- `SCREENING`;
- `IDENTIFIED`;
- `QUANTIFIED`.

It also identifies available charts and recommends the highest-value charts based on whether recorded signal data, ChemCore results, 144 screening and gas-safety payloads are actually present.

The existing XLSX exporter remains evidence-rich: summary, time-series, MQ, analysis, hardware/state, charts, gas safety, 144 screening, provenance and identification matrix. PDF/XLSX outputs remain bound to the exact saved session.

### UI-07 / Contrast

`UiKit` now defines explicit high-contrast dark-surface and light-button foreground/background colors. Generic buttons no longer rely on the phone Theme to choose text color. Candidate/risk surfaces use luminance-aware foreground selection. Sensor detail text was raised from low-contrast gray to a readable light gray on the dark workspace.

## UI architecture rules

### Driving

The driving screen is a situational-awareness screen, not an analytics report. It may show only information that can affect immediate operator decisions: video, drive mode, speed, left/right command, E-STOP, front range, cliff state, gas response, relay state, Wi-Fi, camera, flame and target tracking.

### Measurement

The measurement tab must answer five questions at a glance:

1. Is MQ power ON?
2. Are ADS/sensors available and valid?
3. Is the array warming, stable, degraded or responding?
4. Is a controlled session running and how much time remains?
5. Can the operator start/cancel/save the session now?

### Analysis

The analysis tab must answer:

1. Is the session analytically admissible?
2. What is the top chemical hypothesis?
3. How separated is it from the runner-up?
4. What evidence tier and sensor coverage support it?
5. Is quantitative ppm reportable, provisional, or withheld?
6. Are residual/OOD/entropy gates acceptable?

Mathematical equations belong in an expandable technical details area, not the primary operator dashboard.

### Reports

Reports must distinguish:

- measured raw/derived signals;
- screening hypotheses;
- accepted identification evidence;
- provisional source-backed ppm references;
- M4 device/reference-gas validated quantitative results;
- occupational/safety interpretation gates.

No chart may be rendered as factual if its required source series/payload is missing.

## UI-08 qualification policy

Only current 0.10.2 / HR069 source contracts are authoritative. The dedicated `audit_hr069_operator_ui_contract.py` is the UI source gate for this workstream. Historical revision-specific UI audits may remain in repository history but are not called by the current qualification path.

The application-repository Actions jobs observed during this workstream terminate before executing any step (`steps=[]`). That infrastructure condition is not classified as a source-code test failure. UI-08 therefore uses the independent `HazRob/build-harness` qualification path to check the exact current source SHA.

No UI-08 workflow may create/sign/publish an APK, create a firmware release ZIP or deploy a release unless that is separately requested after qualification.
