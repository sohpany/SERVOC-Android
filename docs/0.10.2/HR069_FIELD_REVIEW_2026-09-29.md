# HazRob 0.10.2 / HR069 — Field Review Change Log

Date: 2026-09-29
Branch: `release/hazrob-0.10.2`
Baseline at review start: `dde559906a4efae1e11a8e2e54556a8366ba8efb`

## Release rule for this review

- All changes remain on HazRob `0.10.2 / HR069` until the review is complete.
- Each accepted field note is implemented, refetched and source-verified before moving on.
- No APK, firmware ZIP, source ZIP, or user release artifact is produced until all requested modifications are complete.
- Final artifacts must be built from one final verified source SHA after the review closes.
- Physical behavior is not marked PASS merely because the source contract is correct; phone/robot acceptance remains required where noted.

## MOD-01 — Landscape driving view

Status: **IMPLEMENTED / SOURCE-VERIFIED — DEVICE UI ACCEPTANCE PENDING**

User requirement:

- In landscape orientation the app presents the driving workspace rather than the normal tabbed application interface.
- Camera video is the main working surface.
- Application navigation tabs are hidden.
- Drive controls remain usable with the video.
- Alerts/status remain visible.

Implemented behavior:

- `HazRobApplication` is registered as the Android `Application` lifecycle guard.
- On `MainActivity` resume in landscape:
  - the application navigation tabs are hidden;
  - the footer is hidden;
  - the generic manual-refresh row is hidden;
  - the global safety/connection/gas status header remains visible;
  - the existing `ControlTab` remains the driving surface with camera, Joystick/Dual Tank controls, speed control, E-STOP, IR/cliff badge, collision warning, camera state, tracking state and flame evidence overlays.
- MainActivity starts the operator workspace on the driving tab, so an orientation recreation enters the driving workspace rather than exposing another tab without navigation.
- On Activity pause/rotation/background transition, the application lifecycle guard explicitly sends drive command `(0,0)` so a non-zero touch command cannot be replayed across recreation.
- Portrait retains the normal tabbed application shell.

Implementation commits:

- `e1b99fb60d2365371a236b52aaf5bd699eaff009` — add landscape driving shell/lifecycle guard.
- `21a33bd0cfd04ad69b9c646ff8ec6054aa7b2cb6` — register `HazRobApplication` in AndroidManifest.
- `8526dcf22a8bce234a0f899fbdd4ac6e6b99e51e` — keep global safety/gas/connection status visible in landscape while hiding navigation/footer/refresh UI.

Regression guard:

The canonical release audit now requires:

- `HazRobApplication` registration in AndroidManifest;
- navigation tabs hidden in landscape;
- global safety status retained;
- footer and generic refresh row hidden;
- drive command neutralization on pause/rotation.

### Required device acceptance before final release

1. Open HazRob in portrait and verify the normal tabs are present.
2. Rotate to landscape and confirm the tabs disappear.
3. Confirm camera video is the principal surface and drive controls remain usable.
4. Confirm E-STOP, IR/cliff, proximity, gas/global status, camera status and tracking/flame indicators remain visible when applicable.
5. Hold a non-zero drive input, rotate/background the app in a controlled wheels-off-ground test, and confirm command is neutralized rather than replayed.
6. Rotate back to portrait and confirm the normal tabbed shell returns.
7. Confirm orientation changes do not corrupt the active connection, stored field-session state, tracking preference or robot-side measurement state.

## MOD-02 — IR cliff logic and fail-safe startup

Status: **IMPLEMENTED / SOURCE-VERIFIED — PHYSICAL POLARITY TEST PENDING**

Required safety behavior:

- Ground present under the downward IR sensor = forward movement may be allowed.
- Ground not detected = cliff condition = forward movement must be blocked.
- Reverse and zero-net pivot remain available as escape motions.
- At boot, forward motion remains blocked until ground detection has been positively confirmed.

Current polarity and fail-safe policy:

- `IR_GROUND_ACTIVE=LOW`
- `IR_FAIL_SAFE_STARTUP=true`
- Current firmware therefore interprets LOW as ground present and HIGH as no ground/cliff.
- `Hardware.cpp` initializes `irWarning` from `Cfg::IR_FAIL_SAFE_STARTUP` while `irGroundDetected=false`.
- The existing drive interlock remains `forwardBlocked=irWarning||ultraStop`.

Debounce behavior retained:

- Ground must be confirmed for `IR_CLIFF_CLEAR_SAMPLES=2` before the startup block clears.
- After confirmed ground, later ground loss requires `IR_CLIFF_ASSERT_SAMPLES=3` before declaring a cliff.
- This preserves operational noise filtering while making startup fail-safe.

Implementation history:

- `e5cf903db9e4ac3285969f9cb9ee8d40a1103d06` — initial fail-safe startup state.
- `9cf8479a05f308f28a6fd309bc0a5b72008a9ed8` — initial canonical guard.
- `04b8413bc4a40a45c7f4f00f3892caa63f7c7dca` — centralize the policy as `IR_FAIL_SAFE_STARTUP` and retain fail-safe initialization on the latest HR069 source.
- `2dbb03653b00a7ce6ef1f4ff1f87e82c6677f7b7` — canonical field-review guard for both the landscape contract and current IR fail-safe contract.

### Required physical acceptance before final release

1. Place the assembled robot on normal ground at the installed IR reference height.
2. Read `ir.raw_level`, `ir.ground_detected`, `ir.cliff_warning`, and `drive.forward_blocked`.
3. With the current polarity contract, after two confirming samples expect: `raw_level=0`, `ground_detected=true`, `cliff_warning=false`, and `forward_blocked=false` unless ultrasonic stop is active.
4. Safely restrain the robot and remove ground from beneath the downward sensor.
5. After the configured cliff debounce expect: `raw_level=1`, `ground_detected=false`, `cliff_warning=true`, `forward_blocked=true`.
6. Confirm forward commands are blocked while reverse and zero-net pivot escape commands remain available.
7. If the assembled sensor produces the opposite electrical levels, change only `IR_GROUND_ACTIVE` to HIGH, then repeat the complete acceptance test before final build.

## Current review status

- MOD-01: source implementation complete; device UI/rotation acceptance pending.
- MOD-02: source implementation complete; assembled IR electrical-polarity and motion-interlock acceptance pending.
- Final APK/firmware artifacts: **intentionally not produced yet**.
