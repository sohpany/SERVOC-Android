# HazRob 0.10.2 / HR069 — Intelligent operator UI, analytics and reporting workplan

Created: 2026-09-30
Branch: `release/hazrob-0.10.2`
Starting documented HEAD: `c4956b3e37148cc96e2dbe9539e1bebfc7e82c10`
Scientific baseline: P11/P12 external analytical enrichment and organization qualification are complete.

## Objective

Upgrade the operator-facing HazRob application so driving, live measurement, analysis, results and reports expose the capabilities of the HR069 analytical stack without presenting internal implementation prose as end-user output. Changes must preserve release identity, fail-closed safety behaviour, non-M4 external-evidence semantics, Direct/Remote transport compatibility and the already-qualified scientific engine.

## Fixed operator requirements

- Landscape driving is a dedicated full-screen video workspace. Video occupies the full usable screen; warnings and controls float on top.
- Driving modes are additive, not replacements. Four modes must remain independently selectable from the same drive-mode selector:
  1. Dual/Tank sliders.
  2. Steering wheel.
  3. Two-axis Joystick.
  4. Direction buttons: forward, forward-right, forward-left, reverse, reverse-right, reverse-left, pivot-right, pivot-left.
- Differential-drive semantics are common across every mode:
  - forward = both sides positive;
  - reverse = both sides negative;
  - pivot right = left positive / right negative;
  - pivot left = left negative / right positive;
  - a right turn while translating slows the physical right side;
  - a left turn while translating slows the physical left side;
  - steering geometry is travel-relative in reverse as well.
- Live gas-response warnings appear in the driving video workspace during live measurement.
- MQ relay power has a floating ON/OFF control in the driving workspace.
- Wi-Fi signal has a floating RSSI/quality badge.
- Echo/proximity displays front distance in centimetres.
- IR cliff semantics are explicit: ground signal present = ground detected/no cliff warning; no ground signal = cliff warning.
- User-facing analysis/result/report screens must use cards, rankings, confidence/evidence, curves, distributions and actionable controls. Internal formula/prose diagnostics are developer-only.
- Text/background contrast must be audited across all operator screens.

## Phases

| Phase | Scope | State | Acceptance gate |
|---|---|---|---|
| UI-00 | Freeze plan and current contracts | DONE | This file committed before implementation. |
| UI-01 | Landscape full-screen driving workspace | ACTIVE | In landscape, video fills the usable screen and every driving/safety control is an overlay; portrait remains tabbed. |
| UI-02 | Four-mode differential driving | PENDING | Tank, Wheel, Joystick and Buttons all emit the same audited left/right command semantics; pivot and diagonal tests pass. |
| UI-03 | Driving telemetry overlays | PENDING | Live gas state, MQ relay, Wi-Fi RSSI, Echo cm, IR ground/cliff and E-STOP state are visible and refresh without blocking drive control. |
| UI-04 | Measurement workspace redesign | PENDING | Live sensor table/cards, baseline/warm-up/relay/session state, response deltas and measurement controls replace narrative-only output. |
| UI-05 | Analysis/results workspace redesign | PENDING | Ranked candidates, confidence/evidence, PPM/reference state, fingerprint fit/residuals and evidence drill-down are interactive. |
| UI-06 | Reports and chart-engine review | PENDING | Smart report sections and chart catalogue are aligned with ChemCore/fingerprint/exposure evidence; exports remain lossless and scientifically labelled. |
| UI-07 | Contrast/readability audit | PENDING | No white/near-white text on white/near-white background; minimum operator contrast rules are applied consistently. |
| UI-08 | Integrated regression/qualification | PENDING | Source contracts + Android unit tests + new UI/control tests pass; no full RC/APK publication unless separately requested. |

## UI-02 control matrix

Normalized examples before speed limiting:

| Intent | left | right |
|---|---:|---:|
| Forward | +100 | +100 |
| Reverse | -100 | -100 |
| Pivot right (3 o'clock) | +100 | -100 |
| Pivot left (9 o'clock) | -100 | +100 |
| Forward-right | +100 | +50 |
| Forward-left | +50 | +100 |
| Reverse-right | -100 | -50 |
| Reverse-left | -50 | -100 |

The Tank mode is direct per-side control and may emit any independent pair within -100..+100. Wheel, Joystick and Buttons use the same differential-drive semantic contract.

## UI-03 sensor semantics

- Echo front distance: show numeric `cm` when valid. Warning threshold comes from firmware diagnostics; UI does not invent an independent safety threshold.
- IR: `ground_detected=true` means surface/ground present and no cliff warning. `cliff_warning=true` means no safe ground is detected; display `خطر السقوط`.
- Gas live warning: operational response state only unless the analytical engine has an accepted identity/quantitative result. Do not relabel a generic MQ response as a named gas.
- External research priors remain non-M4; UI must distinguish identity/reference hypotheses from M4-validated occupational quantitative results.

## Reporting principles

1. Separate measurement quality, identification, quantification/reference estimate and occupational interpretation.
2. Show evidence provenance and confidence/residuals rather than opaque scores alone.
3. Preserve raw sensor/channel data and session metadata in XLSX/PDF source sections.
4. Charts are selected by data availability; never draw a quantitative chart for missing/invalid dimensions.
5. Tracking is an operator navigation aid and is not inserted into the formal analytical report unless explicitly requested later.

## Resume protocol

Read this file first, then the P11/P12 analytical workplan. Continue from the first non-DONE phase. Each phase must update this ledger with the commit/test evidence before moving to the next phase.
