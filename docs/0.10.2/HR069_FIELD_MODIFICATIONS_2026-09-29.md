# HazRob 0.10.2 / HR069 — Field Modification Log

Baseline branch: `release/hazrob-0.10.2`
Baseline SHA before this field-change series: `dde559906a4efae1e11a8e2e54556a8366ba8efb`
Engineering revision: `HR069`

This log records field-requested corrections one by one. No APK or firmware ZIP is to be released until the requested modification list is declared complete and the integrated regression/build gates pass.

## MOD-01 — Landscape driving view

**Requested behavior**
- When Android is in landscape orientation, HazRob must present the driving workspace instead of the normal tabbed application shell.
- The camera stream remains the primary visual surface.
- Driving controls and operational/safety alerts remain visible.
- Main application tabs/header/footer and the generic per-tab refresh bar are hidden in landscape.
- Pausing/leaving the activity must neutralize the drive command so an orientation/lifecycle transition cannot preserve a non-zero drive request.
- Portrait keeps the normal tabbed application shell.

**Implementation status:** PLANNED

**Verification requirements**
- Contract test must confirm the landscape shell suppression and lifecycle neutral-drive guard.
- Physical/UI acceptance remains required on the target Android phone in portrait and landscape.

## MOD-02 — IR cliff fail-safe semantics

**Requested behavior**
- The downward IR sensor represents `ground present`, not `obstacle ahead`.
- Ground present permits forward motion.
- Loss of ground detection means cliff/fall risk and must block any command with a net forward component.
- Reverse and zero-net pivot remain available for escape.
- At boot, forward motion must remain blocked until ground presence is positively confirmed; the firmware must not begin in an optimistic/unverified-safe state.

**Current electrical assumption**
- `IR_GROUND_ACTIVE=LOW`: raw LOW means ground detected; raw HIGH means no ground/cliff.
- This polarity must be confirmed physically on the assembled sensor after flashing. If the actual module is opposite, only the active-level configuration is changed; the safety semantics remain the same.

**Debounce contract**
- Ground confirmation / cliff clear: 2 consecutive ground samples.
- Cliff assertion after previously confirmed ground: 3 consecutive lost-ground samples.
- IR sample period: 50 ms.

**Implementation status:** PLANNED

**Verification requirements**
- Static/contract guard for fail-safe startup and forward-block semantics.
- Physical acceptance must verify `raw_level` on floor versus lifted/no-floor condition before final release acceptance.
