# HazRob 0.10.2 / HR069 — Operator UI, analytics and reporting workplan

Status: ACTIVE / RESUMABLE
Created: 2026-09-30
Scope: `release/hazrob-0.10.2` / HR069 only
Starting HEAD: `a29101b3c3abda8ab7127087b2d403b7e606cdf4`

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
| UI-01 | Landscape full-screen driving | DONE / VERIFYING | camera occupies full content area; system chrome hidden; controls/status float over video |
| UI-02 | Four independent drive modes | DONE / VERIFYING | TANK/WHEEL/JOYSTICK/BUTTONS all selectable; canonical left/right commands; pivot and progressive-turn matrix tested |
| UI-03 | Drive hazard/status overlays | DONE / VERIFYING | gas, relay, Wi-Fi, Echo cm and IR ground/cliff visible on driving screen |
| UI-04 | Measurement workspace redesign | ACTIVE | operator cards for power, acquisition readiness, live gas state, channel validity, session progress and actions; reduce narrative text |
| UI-05 | Analysis/results dashboard redesign | PENDING | top hypothesis, posterior/margin, OOD/entropy, evidence tier, quantitative gate, residuals and dynamic candidate cards |
| UI-06 | Smart reports and charts | PENDING | report KPIs, evidence provenance, safety state and chart capability matrix mapped to recorded data |
| UI-07 | Contrast/readability audit | PENDING | no white-on-white or low-contrast operator text; candidate cards choose foreground by background luminance |
| UI-08 | HR069 current-source qualification | PENDING | current HR069 contract + Android tests + firmware compile; no release artifact unless explicitly requested |

## Current implementation checkpoint

At starting HEAD `a29101b3...`:

- `ControlTab` detects landscape, expands `cameraBox`, hides surrounding app chrome and applies immersive system UI flags.
- Four drive presentations exist through `DriveMode`: `TANK`, `WHEEL`, `JOYSTICK`, `BUTTONS`.
- `DualTankDriveControl` directly emits one signed speed per physical side.
- `SteeringWheelView` and `JoystickView` both use the current `DriveMixer`.
- `DirectionPadView` uses fixed `DiscreteDrive` commands including progressive forward/reverse turns and in-place pivots.
- Driving overlay contains Wi-Fi RSSI/quality, live gas state, MQ relay toggle, Echo front distance, IR ground/cliff state, flame state, camera state, tracking state and E-STOP.
- Echo distance is displayed in centimetres when valid.
- Current IR UI treats `cliff_warning=true` as "no ground" and `ground_detected=true` as safe ground.

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

## Validation policy

Only current 0.10.2 / HR069 source contracts are authoritative for UI-01..UI-08. A dedicated `audit_hr069_operator_ui_contract.py` is the UI source gate for this workstream. Historical revision-specific UI audits may remain in repository history but are not called by the HR069 UI qualification path.
