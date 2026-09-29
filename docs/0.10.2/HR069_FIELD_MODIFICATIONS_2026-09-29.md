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
- Portrait keeps the normal tabbed application shell.

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

## MOD-02 — IR cliff fail-safe semantics

**Requested behavior**
- The downward IR sensor represents `ground present`, not `obstacle ahead`.
- Ground present permits forward motion.
- Loss of ground detection means cliff/fall risk and blocks any command with a net forward component.
- Reverse and zero-net pivot remain available for escape.
- At boot, forward motion remains blocked until ground presence is positively confirmed; firmware must not begin in an optimistic/unverified-safe state.

**Electrical assumption to verify physically**
- `IR_GROUND_ACTIVE=LOW`: raw LOW means ground detected; raw HIGH means no ground/cliff.
- If the assembled module proves opposite, only the active-level configuration changes; the safety semantics remain unchanged.

**Debounce contract**
- Ground confirmation / cliff clear: 2 consecutive ground samples.
- Cliff assertion after ground loss: 3 consecutive lost-ground samples.
- IR sample period: 50 ms.

**Implementation status:** IMPLEMENTED IN SOURCE — PHYSICAL POLARITY ACCEPTANCE PENDING

**Implementation**
- `Config.h` explicitly defines `IR_FAIL_SAFE_STARTUP=true` and documents ground-present semantics.
- `Hardware.cpp` initializes `irWarning` from the fail-safe startup policy.
- `driveSet()` continues to block only commands with a net forward component when `irWarning || ultraStop`, preserving reverse/pivot escape.
- `IRCliffFailSafeContractTest` and the canonical release audit protect polarity, startup, debounce and forward-block contracts.

**Verification requirements still pending**
- With robot over floor: verify `raw_level=0`, `ground_detected=true`, `cliff_warning=false` under the present LOW-active assumption.
- Lift the front/downward sensor away from floor: verify `raw_level=1`, then `cliff_warning=true` and `forward_blocked=true` after debounce.
- Verify Forward is blocked while Reverse and zero-net Pivot remain available.
- If raw electrical polarity is opposite, change `IR_GROUND_ACTIVE` only and rerun these tests.

## DISC-01 — Environmental I2C read-bus defect discovered during diff verification

**Status:** CORRECTED IN SOURCE — INTEGRATED REGRESSION PENDING

The existing ENV helper issued `EnvWire.requestFrom(...)` on the dedicated GPIO13/14 environmental bus but then consumed bytes using `Wire.read()` from the analytical GPIO21/22 bus. The corrected implementation consumes the requested bytes using `EnvWire.read()`.

This is a correctness defect in the pre-existing ENV path and is kept separate from MOD-02 in this log. It can materially affect BME/BMP communication and must be covered by the final regression/physical ENV tests before release.

## DISC-02 — Measurement JSON state quoting defect discovered during diff verification

**Status:** CORRECTED IN SOURCE — INTEGRATED REGRESSION PENDING

The previous `measurementJson()` serialization did not close the quoted `state` value before emitting the `mask` field. The corrected serializer closes the quote so the embedded measurement object is valid JSON.

This is a pre-existing data-path defect and is kept separate from MOD-02. Final regression must verify `/data` and `/diag` JSON parsing in all measurement states (`idle`, `measuring`, `ready`, `error`).

## Release hold

No APK, Main ESP32 ZIP, CAM ZIP or combined firmware bundle is to be produced for user delivery until the requested field-modification list is complete. At that point the final source HEAD will be pinned, all canonical/unit/firmware gates will be run, and all artifacts will be built from that one exact SHA.
