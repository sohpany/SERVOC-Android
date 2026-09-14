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
- Browser-side drive tests were performed while the Android application could still own the 20 Hz drive command stream. Therefore those browser results are not authoritative for L298N channel mapping.

### Software review and remediation

The joystick differential mixer is mathematically standard (`left=y+x`, `right=y-x`). Firmware applies the accepted left/right command directly to the two L298N channels with no acceleration/reversal shaping. No left/right inversion or channel swap is being changed until isolated physical channel mapping is measured.

A separate Android ownership defect was fixed in HR048: Direct mode previously retransmitted idle `0,0` every 50 ms, allowing an open but idle app to overwrite independent browser/diagnostic drive commands. HR048 now sends the zero transition once, then suppresses repeated idle-zero Direct commands until Android itself sends a non-zero command. Non-zero operator commands retain the 20 Hz continuity refresh.

### Required field gate

After installing an HR048 verified build, or after Force Stop of the old Android app, repeat wheels-up:

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

Unit tests were updated to require warning operation after warm-up even when the app baseline has not yet been captured.

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

The ultrasonic implementation must also be re-evaluated after correct wiring because the present non-blocking ECHO polling shares the main loop; short ECHO pulses require a field timing check before this channel can be declared PASS.

## 6. Remote-control finding and diagnostic remediation

Render/HiveMQ infrastructure diagnostics passed externally: stored app/robot MQTT credentials, TLS, ACL subscriptions and command/ACK/telemetry round-trips were accepted. The field failure is therefore not currently assigned to Render or HiveMQ.

HR048 expands the developer connection screen to show the complete robot-side chain and the first failing stage:

`configured -> wifi -> ip_ok -> dns -> time -> internet -> tls -> mqtt -> command_ack -> telemetry -> remote_ready`

It also displays the firmware `detail` field, so the next field test can distinguish Wi-Fi/DHCP/DNS/NTP/bootstrap/TLS/MQTT/ACK/telemetry without guessing.

No transport behavior has been changed before the first failing stage is captured.

## 7. Analysis UI / M4 — PRESENTATION REMEDIATED

PPM withholding remains scientifically intentional until complete M4 evidence exists. HR048 now labels the two layers explicitly:

- `Screening`: Rs/R0 fingerprint, ranked candidates, posterior, residuals and screening fit — available after a valid controlled session.
- `Quantitative PPM`: reportable only after the independent M4 gate.

The UI now states that a withheld PPM does not mean the screening analysis is unavailable. No M4 safety gate is bypassed and no screening estimate is relabeled as reportable concentration.

## 8. Verification infrastructure state

GitHub Actions push runs were created for the remediation commits, but the hosted job terminated before any workflow step started (`steps=[]`, zero runner execution). This is treated as CI infrastructure unavailability, not as evidence that the source passed or failed compilation.

A separate Render verification service was created to run Android unit tests plus main ESP32 and ESP32-CAM compilation. Its result must be checked before an HR048 APK/firmware artifact is released.

## 9. Acceptance state

Current state:

- Source baseline: PASS as HR047 source baseline only.
- HR048 MQ warning patch: code changed; verification pending.
- HR048 alert-output logic: code changed; verification pending; hardware output intentionally disabled by default.
- HR048 Direct idle-zero ownership fix: code changed; verification and field retest pending.
- Drive side mapping/inversion: field gate pending; no speculative inversion committed.
- IR/ECHO/sound: physical rewiring pending; ultrasonic timing review remains open.
- Remote diagnostic visibility: code changed; field `/remote/status` capture pending.
- Analysis Screening/M4 presentation: code changed; verification pending.

No HR048 artifact is approved for field use until Android tests, firmware compilation and the listed physical gates pass.
