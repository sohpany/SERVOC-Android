# HazRob 0.10.2 / HR069 — Field Review Change Log

Date: 2026-09-29
Branch: `release/hazrob-0.10.2`
Baseline at review start: `dde559906a4efae1e11a8e2e54556a8366ba8efb`

## Release rule for this review

- All changes remain on HazRob `0.10.2 / HR069` until the review is complete.
- Each accepted field note is implemented and verified before moving on.
- No APK, firmware ZIP, source ZIP, or release artifact is produced for the user until all requested modifications are complete.
- Final artifacts must be built from one final verified source SHA after the review closes.

## MOD-01 — Landscape driving view

Status: **ACCEPTED — IMPLEMENTATION PENDING**

Requirement:

- In landscape orientation the app shall present a dedicated full-screen driving view.
- Camera video is the main surface.
- Application navigation tabs are hidden in landscape driving mode.
- Drive controls remain overlaid and usable.
- Safety/status overlays remain visible, including E-STOP, IR/cliff, ultrasonic proximity, gas warning, connection/camera state, and tracking state when active.
- Rotating the phone must not create or replay a non-zero drive command.
- Rotation must not stop an active measurement, field session, tracking state, or connection solely because the UI was rebuilt.
- Returning to portrait restores the normal tabbed application shell and prior user context.

Implementation is intentionally deferred until the remaining requested field notes are collected/reviewed, unless explicitly prioritized earlier.

## MOD-02 — IR cliff logic and fail-safe startup

Status: **IMPLEMENTED / SOURCE-VERIFIED — PHYSICAL POLARITY TEST PENDING**

Required safety behavior:

- Ground present under the downward IR sensor = forward movement may be allowed.
- Ground not detected = cliff condition = forward movement must be blocked.
- Reverse and zero-net pivot remain available as escape motions.
- At boot, forward motion must remain blocked until ground detection has been positively confirmed.

Existing polarity contract retained:

- `IR_GROUND_ACTIVE=LOW`
- Therefore the current firmware interprets LOW as ground detected and HIGH as no ground/cliff.
- This electrical polarity must still be confirmed on the assembled physical sensor before final release. If the real module is opposite, only the polarity constant is changed after that measurement.

Implementation:

- `firmware/HazRob/Hardware.cpp` now initializes `irWarning=true` while `irGroundDetected=false`.
- This makes the existing forward interlock fail-safe immediately after boot.
- Once the sensor confirms ground for `IR_CLIFF_CLEAR_SAMPLES=2`, `irWarning` clears and forward motion becomes eligible.
- After ground has been confirmed, later ground loss still uses the existing debounce `IR_CLIFF_ASSERT_SAMPLES=3` before declaring a cliff, preserving the established operational noise filtering.
- The existing drive safety gate remains `forwardBlocked=irWarning||ultraStop`.

Implementation commit:

- `e5cf903db9e4ac3285969f9cb9ee8d40a1103d06` — `0.10.2 HR069: make IR cliff gate fail-safe at boot`

Regression guard:

- The canonical `tools/audit_0102_release_contract.py` now requires:
  - `IR_GROUND_ACTIVE=LOW`
  - `bool irWarning=true,irGroundDetected=false`
  - existing forward block contract `forwardBlocked=irWarning||ultraStop`

Guard commit:

- `9cf8479a05f308f28a6fd309bc0a5b72008a9ed8` — `0.10.2 HR069: guard IR fail-safe startup contract`

Source verification completed after both writes.

### Required physical acceptance test before final artifacts are accepted

1. Place the assembled robot on normal ground at the installed IR reference height.
2. Read `ir.raw_level`, `ir.ground_detected`, `ir.cliff_warning`, and `drive.forward_blocked`.
3. Expected with the current polarity contract after two confirming samples: `raw_level=0`, `ground_detected=true`, `cliff_warning=false`, `forward_blocked=false` unless ultrasonic stop is active.
4. Lift the front/downward sensor away from the ground while the robot is safely restrained.
5. Expected after the configured cliff debounce: `raw_level=1`, `ground_detected=false`, `cliff_warning=true`, `forward_blocked=true`.
6. Confirm forward commands are blocked while reverse and zero-net pivot escape commands remain available.
7. If the observed electrical levels are reversed, change `IR_GROUND_ACTIVE` to HIGH and repeat the complete acceptance test before release.
