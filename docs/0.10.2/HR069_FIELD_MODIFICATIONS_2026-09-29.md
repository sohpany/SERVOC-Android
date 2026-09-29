# HazRob 0.10.2 / HR069 — Field Modification Log

Baseline branch: `release/hazrob-0.10.2`
Baseline SHA before this field-change series: `dde559906a4efae1e11a8e2e54556a8366ba8efb`
Engineering revision: `HR069`

This log records field-requested corrections one by one. No APK or firmware ZIP is to be released until the requested modification list is declared complete and the integrated regression/build gates pass.

## MOD-01 — Landscape driving view

**Requested behavior**
- When Android is in landscape orientation, HazRob presents the driving workspace instead of the normal tabbed application shell.
- The camera stream remains the primary visual surface.
- Driving controls, E-STOP and operational/safety alerts remain visible.
- Application navigation tabs, footer and the generic per-tab refresh row are hidden in landscape.
- The global safety/connection/gas status header is deliberately retained in landscape so critical alerts are not suppressed.
- Pausing/leaving MainActivity neutralizes the drive command so orientation/background transitions cannot preserve a non-zero drive request.
- Portrait keeps the normal tabbed shell.

**Implementation status:** IMPLEMENTED IN SOURCE — PHYSICAL/UI ACCEPTANCE PENDING

**Implementation**
- `HazRobApplication` registers Activity lifecycle callbacks.
- Landscape suppresses navigation tabs/footer/generic refresh while retaining the safety header and existing ControlTab driving/camera surface.
- MainActivity pause is guarded by `setDriveCommand(0,0)` through `HazRobHost`.
- `AndroidManifest.xml` registers `.HazRobApplication`.
- `LandscapeDrivingContractTest` and the canonical release audit protect the contract.

**Verification requirements still pending**
- Rotate the target phone Portrait → Landscape while stationary: driving camera view must appear without navigation tabs.
- Rotate while a non-zero command is being issued only in a raised/safe test: motion must neutralize during the lifecycle transition and must not resume by itself.
- Confirm camera, E-STOP, gas/cliff/proximity/connection alerts remain visible and usable in landscape.

## MOD-02 — IR ground/cliff recognition semantics

**Requested behavior**
- The downward IR sensor represents `ground present`, not `obstacle ahead`.
- Ground present is the normal state.
- Loss of ground detection means cliff/fall risk and must generate the cliff warning state.
- At boot, IR hazard state starts conservatively as warning until ground presence is positively confirmed; firmware must not report an unverified safe-ground state.
- Actual automatic blocking of Forward is governed separately by MOD-03 and is no longer unconditional.

**Electrical assumption to verify physically**
- `IR_GROUND_ACTIVE=LOW`: raw LOW means ground detected; raw HIGH means no ground/cliff.
- If the assembled module proves opposite, only the active-level configuration changes; the warning semantics remain unchanged.

**Debounce contract**
- Ground confirmation / cliff clear: 2 consecutive ground samples.
- Cliff assertion after ground loss: 3 consecutive lost-ground samples.
- IR sample period: 50 ms.

**Implementation status:** IMPLEMENTED IN SOURCE — PHYSICAL POLARITY ACCEPTANCE PENDING

**Implementation**
- `Config.h` explicitly defines `IR_FAIL_SAFE_STARTUP=true` and documents ground-present semantics.
- `Hardware.cpp` initializes `irWarning` from the fail-safe startup policy.
- `hazardAlert()` continues to publish IR/proximity warning state independently from the optional drive block.
- `IRCliffFailSafeContractTest` and the canonical release audit protect polarity, startup warning and debounce behavior.

**Verification requirements still pending**
- With robot over floor: verify `raw_level=0`, `ground_detected=true`, `cliff_warning=false` under the present LOW-active assumption.
- Lift the front/downward sensor away from floor: verify `raw_level=1`, then `cliff_warning=true` after debounce.
- Confirm the cliff warning remains visible whether MOD-03 automatic stopping is OFF or ON.
- If raw electrical polarity is opposite, change `IR_GROUND_ACTIVE` only and rerun these tests.

## MOD-03 — Optional automatic stop for cliff/collision hazards

**Requested behavior**
- During the current robot-build phase, IR cliff and ultrasonic collision hazards must continue to generate warnings but must not automatically stop/block Forward by default.
- Automatic stopping is an operator-selectable setting in the application Settings tab.
- Default after first use/clean firmware storage is **OFF**.
- When OFF: cliff/collision warnings remain active, including ultrasonic stop-zone detection, but the robot does not automatically suppress a Forward drive command because of those two hazards.
- When ON: a cliff warning or ultrasonic stop-zone condition blocks only commands with a net Forward component; Reverse and zero-net Pivot remain available for escape.
- E-STOP, explicit Stop and command-loss watchdog remain active and independent regardless of this setting.
- The setting must be authoritative in the robot, persist across reboot, and work identically in Direct and Remote modes.

**Implementation status:** IMPLEMENTED IN SOURCE — INTEGRATED/PHYSICAL ACCEPTANCE PENDING

**Implementation**
- Added `SafetyOptions.h/.cpp` with dedicated NVS namespace `hazsafe` and key `autoStop`.
- Firmware default is `autoStop=false`.
- `Hardware.cpp` preserves `irWarning` / `ultraWarning` / ultrasonic stop-zone detection at all times, while actual `forwardBlocked` is calculated as `safetyAutoStopEnabled() && (irWarning || ultraStop)`.
- Drive telemetry now distinguishes `forward_hazard` from actual `forward_blocked` and reports `safety_auto_stop`.
- Proximity telemetry preserves warning/stop-zone information even when automatic blocking is disabled.
- Direct mode exposes `/safety/auto_stop` for read/set, persisting the robot-side value.
- Remote MQTT exposes `safety_auto_stop_get` and `safety_auto_stop_set` using the same robot-side persistent value.
- `RobotApi` exposes one Direct/Remote facade for reading and setting the option.
- Settings tab now contains the checkbox `تفعيل التوقف التلقائي عند خطر السقوط أو الاصطدام` and explicitly states that warnings remain active when automatic stop is disabled.
- Applying a changed safety option first neutralizes motion, then changes the policy.
- `SafetyAutoStopOptionContractTest`, updated `IRCliffFailSafeContractTest`, and the canonical release audit guard the cross-layer contract.

**Physical/UI acceptance still pending**
1. Auto-stop OFF + no floor/cliff: cliff warning must appear, `forward_hazard=true`, but `forward_blocked=false`; perform only with robot safely supported because Forward is intentionally permitted.
2. Auto-stop OFF + obstacle within ultrasonic stop zone (`<=5 cm`): collision warning/stop-zone must appear but `forward_blocked=false`.
3. Auto-stop ON + no floor/cliff: cliff warning remains visible and Forward is blocked.
4. Auto-stop ON + obstacle within stop zone: collision warning remains visible and Forward is blocked.
5. Auto-stop ON: Reverse and zero-net Pivot remain available for escape.
6. E-STOP must stop/lock movement with Auto-stop OFF and ON.
7. Command-loss watchdog must stop motion with Auto-stop OFF and ON.
8. Explicit Stop must work with Auto-stop OFF and ON.
9. Change the option, reboot Main ESP32, and verify the robot-side value persists.
10. Verify the same value and behavior from Direct and Remote modes.

## MOD-04 — Ultrasonic distance overlay in landscape driving view

**Requested behavior**
- The landscape driving view shall show a dedicated box with the live front ultrasonic/ECHO distance in centimeters.
- Valid distance values greater than `10 cm` are displayed in green.
- Valid distance values at or below `10 cm` are displayed in red.
- Missing, timed-out or invalid distance must display `ECHO — cm` in neutral gray rather than a fabricated zero.
- The distance display is informational and remains active whether MOD-03 automatic stopping is OFF or ON.

**Implementation status:** IMPLEMENTED IN SOURCE — PHYSICAL/UI ACCEPTANCE PENDING

**Implementation**
- Added `EchoDistanceBadge` as a self-refreshing `250 ms` landscape overlay bound to `device.proximity.cm` from the unified diagnostics/telemetry snapshot.
- `HazRobApplication` injects the badge into the landscape camera `FrameLayout`, centered near the top and below the primary safety-warning overlay.
- Display format is `ECHO %.1f cm`.
- Color policy is green for `>10.0 cm`, red for `<=10.0 cm`, and gray for unavailable/invalid data.
- The badge has no authority over drive commands and does not alter MOD-03 warning/auto-stop behavior.
- `EchoDistanceOverlayContractTest` protects the threshold, colors, unavailable state and landscape installation contract.

**Physical/UI acceptance still pending**
1. With a target farther than 10 cm, verify the displayed value tracks the measured distance and is green.
2. Move the target to exactly/approximately 10 cm and below; verify the displayed value becomes red.
3. Remove the echo/no valid return; verify `ECHO — cm` appears in gray rather than `0 cm`.
4. Verify the box remains readable in landscape without covering E-STOP, joystick/tank controls, or the primary warning banner.
5. Repeat with Auto-stop OFF and ON; the displayed distance and color must remain identical for the same measured distance.
6. Repeat in Direct and Remote; the badge must use the same unified proximity telemetry.

## DISC-01 — Environmental I2C read-bus defect discovered during diff verification

**Status:** CORRECTED IN SOURCE — INTEGRATED REGRESSION PENDING

The existing ENV helper issued `EnvWire.requestFrom(...)` on the dedicated GPIO13/14 environmental bus but then consumed bytes using `Wire.read()` from the analytical GPIO21/22 bus. The corrected implementation consumes the requested bytes using `EnvWire.read()`.

This is a correctness defect in the pre-existing ENV path and is kept separate from the requested field changes in this log. It can materially affect BME/BMP communication and must be covered by the final regression/physical ENV tests before release.

## DISC-02 — Measurement JSON state quoting defect discovered during diff verification

**Status:** CORRECTED IN SOURCE — INTEGRATED REGRESSION PENDING

The previous `measurementJson()` serialization did not close the quoted `state` value before emitting the `mask` field. The corrected serializer closes the quote so the embedded measurement object is valid JSON.

This is a pre-existing data-path defect and is kept separate from the requested field changes. Final regression must verify `/data` and `/diag` JSON parsing in all measurement states (`idle`, `measuring`, `ready`, `error`).

## Release hold

No APK, Main ESP32 ZIP, CAM ZIP or combined firmware bundle is to be produced for user delivery until the requested field-modification list is complete. At that point the final source HEAD will be pinned, all canonical/unit/firmware gates will be run, and all artifacts will be built from that one exact SHA.
