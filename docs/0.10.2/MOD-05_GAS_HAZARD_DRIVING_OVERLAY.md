# MOD-05 — Gas Hazard Driving Overlay

Release line: HazRob 0.10.2 / HR069
Branch: `release/hazrob-0.10.2`

## Requested behavior
- While driving in landscape mode, HazRob shall show a dedicated gas-warning box over the camera view when the operational gas alert becomes active.
- The gas warning is independent from the cliff/collision banner and independent from the ECHO distance box.
- The overlay must not claim calibrated ppm or chemical identity merely from the operational MQ-response alert.

## Implemented behavior
- `GasHazardBadge` reads `alert.gas` from the unified live diagnostics/telemetry snapshot.
- `gas=0`: badge hidden.
- `gas=1`: amber/yellow warning box: `⚠ تحذير غاز`.
- `gas=2`: red danger box: `⚠ خطر غاز مرتفع`.
- Refresh period: 250 ms.
- `HazRobApplication` injects the badge into the same landscape camera `FrameLayout` used by the driving workspace.
- The ECHO box remains independently installed and functional.
- The overlay has no authority over movement, E-STOP, optional cliff/collision auto-stop, calibration, identification or ppm estimation.

## Source guards
- `DrivingHazardOverlayContractTest` verifies installation of both ECHO and gas overlays, ECHO 10 cm color threshold, gas level semantics, hidden-clear state, and the no-ppm/no-identity claim.

## Physical/UI acceptance pending
1. With no gas alert, verify the gas box is absent.
2. Trigger a controlled operational WARNING state and verify the amber/yellow box appears.
3. Trigger a controlled operational DANGER state and verify the red box appears.
4. Verify the box clears after the gas alert returns to zero after the normal downgrade/clear hold.
5. Verify gas warning and ECHO distance can be visible simultaneously without overlap that obscures driving controls.
6. Verify cliff/collision warning can coexist with the gas overlay.
7. Verify behavior in Direct and Remote modes.
8. Confirm no numeric ppm or chemical name is shown by this overlay unless a separate validated analytical result exists elsewhere in the application.

## Release hold
No APK or firmware ZIP is to be produced for user delivery until the requested field-modification list is declared complete and integrated regression/build gates pass.
