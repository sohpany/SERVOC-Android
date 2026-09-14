# HazRob 0.10.1 — HR-0101-048 Field Integration Remediation

Status: **IN PROGRESS — FIELD FINDINGS CONFIRMED; REMEDIATION BUILD NOT YET FIELD-ACCEPTED**

Branch: `fix/hr-0101-048-field-remediation`
Baseline: `dev/hazrob-0.10.1` at `702ddcf9cef20f1212719902acbc275bda7f3f58`

## 1. Trigger

Physical testing after loading the HR047 distribution artifacts exposed integration failures that were not closed by source/build gates alone:

1. Forward and reverse motion work, but steering/pivot behavior is not correct.
2. MQ response appears weak operationally and immediate gas alerts are absent.
3. Analysis UI is perceived as withholding all results.
4. Remote control over the Internet does not work in the field.
5. IR, ultrasonic ECHO and sound channels do not work in the current physical wiring; the user reported routing their signals through an I2C level shifter while powering modules from 5 V.

These findings reopen physical integration acceptance. HR047 remains a source/build baseline, not a field-acceptance claim.

## 2. Drive finding

### Confirmed field observations

- Forward command: correct.
- Reverse command: correct.
- Right steering command: vehicle turns left.
- Left steering command: vehicle barely turns and wheel behavior is not the expected differential/pivot motion.
- Browser-side drive tests were performed while the Android application may still have owned the 20 Hz drive command stream. Therefore those browser results are not yet authoritative for L298N channel mapping.

### Software review

The joystick differential mixer is mathematically standard (`left=y+x`, `right=y-x`). Firmware applies the accepted left/right command directly to the two L298N channels with no acceleration/reversal shaping. No left/right inversion or channel swap is being changed in HR048 until an isolated wheels-up channel test is repeated with the Android process fully stopped.

### Required field gate

Repeat with Android Force Stop:

- `L=+50, R=0`
- `L=0, R=+50`
- `L=-50, R=0`
- `L=0, R=-50`
- `L=+50, R=-50`
- `L=-50, R=+50`

Record which physical side moves and its direction. Only then may logical side-swap or inversion be committed.

## 3. MQ immediate-warning defect — FIXED IN REMEDIATION BRANCH

The Android instant-warning layer previously required a separately captured/persisted clean-air baseline. If that baseline was absent, a valid `Rs/R0` response was silently treated as NORMAL, so a fresh installation could never produce a gas warning after warm-up.

HR048 changes the operational warning rule:

- A captured Android clean-air baseline still takes precedence.
- If it is absent, the accepted firmware normalization `Rs/R0` uses `1.0` as the operational fallback baseline.
- Existing 20% ELEVATED and 40% HIGH response thresholds are retained for this first remediation build to avoid changing two variables at once.
- This remains an operational response alarm only. It is not quantitative ppm, IDLH, LFL or chemical-identification validation.

Unit tests are updated to require warning operation after warm-up even when the app baseline has not yet been captured.

## 4. Alert GPIO defect — LOGIC FIXED / HARDWARE ENABLE STILL CONTROLLED

The prior firmware calculated alert levels but forced GPIO26 LOW on every update. HR048 corrects the logic so the pin can follow an active alert when a safe external alert interface is declared installed.

Safety rule:

- GPIO26 is a logic output only.
- An 8-ohm speaker must never be connected directly to the ESP32 GPIO.
- `ALERT_OUTPUT_INSTALLED=false` remains the default until a transistor/MOSFET/amplifier or suitable active-buzzer interface is physically confirmed.
- Telemetry now distinguishes logical alert activity from actual asserted alert output.

Phone-side audible warning remains available independently and benefits from the MQ fallback-baseline fix.

## 5. IR / ECHO / sound physical wiring finding

These are not I2C devices in the authoritative HazRob profile:

- Sound/MAX4466 OUT -> GPIO36 (ADC1 analog).
- IR digital output -> GPIO35.
- Ultrasonic TRIG -> GPIO23.
- Ultrasonic ECHO -> GPIO39 (input only; 5 V ECHO must be reduced to <=3.3 V).

A bidirectional BSS138-style I2C level shifter is not an appropriate analog signal path for MAX4466 and is not the preferred path for HC-SR04 ECHO. Physical rewiring is required before these channels can be evaluated as firmware defects.

## 6. Remote-control finding

Render/HiveMQ infrastructure diagnostics passed externally: stored app/robot MQTT credentials, TLS, ACL subscriptions and command/ACK/telemetry round-trips were accepted. The field failure is therefore not currently assigned to Render or HiveMQ.

The next authoritative field datum is the robot's `/remote/status` chain:

`configured -> wifi -> ip_ok -> dns -> time -> internet -> tls -> mqtt -> command_ack -> telemetry -> remote_ready`

No remote transport code will be changed until the first failing stage and `detail` value are captured.

## 7. Analysis UI / M4

PPM withholding remains scientifically intentional until complete M4 evidence exists. HR048 must improve presentation so screening candidate results are visibly separated from validated quantitative ppm; it must not bypass M4 or relabel screening estimates as reportable concentration.

## 8. Acceptance state

Current state:

- Source baseline: PASS.
- Previous synchronized builds: PASS for HR047 source, not field acceptance.
- HR048 MQ warning patch: code changed, build/test pending.
- HR048 alert-output logic: code changed, build/firmware compile pending.
- Drive mapping: field gate pending; no speculative inversion committed.
- IR/ECHO/sound: physical rewiring pending.
- Remote: robot status capture pending.
- Analysis UX: remediation pending.

No HR048 artifact is approved for field use until Android tests, firmware compilation and the listed physical gates pass.
