# HazRob 0.10.1 — Motion Control Contract

Status: **AUTHORITATIVE INTEGRATION CONTRACT / FIELD VALIDATION PENDING**  
Change control: `HR-0101-034`

## Objective

HazRob manual drive shall follow operator commands with the minimum intentional software shaping practical for the existing ESP32 + L298N prototype. Connection-health diagnostics, analytical measurements and MQ power are independent from normal drive dynamics.

## Operator input

- Joystick radial dead zone: 3%.
- Outside the dead zone, joystick magnitude is mapped linearly; quadratic response shaping is retired.
- Differential mix remains `left=y+x`, `right=y-x`, normalized to the -100..100 command domain.
- Operator speed control remains available, but its default is 100%.
- A changed joystick command is dispatched immediately; a 20 Hz refresh stream republishes the latest command for continuity.

## Main ESP32 drive path

- `driveSet(left,right)` applies the current validated command directly to L298N PWM/direction output.
- Acceleration ramp, deceleration ramp and software reversal ramp are retired and removed from source.
- Automatic movement history, Retrace and link-hold motion state are retired and removed from source.
- Link heartbeat is diagnostic only and does not zero motion, cancel measurement, remove MQ power or initiate autonomous movement.
- Ordinary explicit `motion_stop`/`/motion/stop` remains available to the operator/application.

## Stale-command boundary

- A single `CONTROL_COMMAND_TIMEOUT_MS=1500` boundary replaces the previous drive-lease/link-detect/link-hold/retrace chain.
- The timeout does not delay, ramp or rate-limit a valid new command.
- If non-zero output remains active and no new drive command is received for the full timeout, the main ESP32 neutralizes both drive outputs and records `stale_stop=true`.
- A subsequent valid drive command clears `stale_stop` and is applied immediately unless the independent E-stop latch is active.

## Android lifecycle

- Leaving the Drive tab does not issue an automatic motion STOP.
- `Activity.onPause()` does not issue an automatic motion STOP.
- Camera streaming may stop independently when the Drive tab is hidden or the Activity is paused.
- Destruction/loss of the command producer eventually falls through to the firmware stale-command boundary; no Retrace or analytical side effect is authorized.

## Remote transport

- Remote drive remains MQTT QoS0/fire-and-forget; it does not wait for command ACK in the normal steering loop.
- Session, sequence, nonce and TTL freshness checks remain mandatory. They protect against delayed/replayed commands and are not motor-speed shaping mechanisms.
- Render remains bootstrap/video infrastructure and is not part of the per-command motor path.
- HiveMQ remains the authenticated TLS broker and does not implement acceleration, reversal or stopping policy.

## Independent emergency stop

The software-latched E-stop defined by `SAFETY_CONTROL_CONTRACT.md` remains independent from this responsiveness policy. It is not used for acceleration shaping and does not add intentional delay to accepted normal drive commands. It may inhibit drive only after it has been explicitly latched.

## Telemetry

The drive object shall expose at least:

- `left`
- `right`
- `command_age_ms`
- `stale_stop`
- canonical L298N pin mapping

Retired drive telemetry (`retrace`, `hold`, motion-history count and acceleration/failsafe state tied to the old policy) shall not be emitted.

## Verification gates

Before HR-0101-034 can be closed:

1. fail-closed source audit passes;
2. Android unit tests and release build pass;
3. main ESP32 compiles at the same source SHA;
4. ESP32-CAM baseline also compiles at that SHA to preserve release consistency;
5. wheels-up Direct test measures 0->command, command->0, forward->reverse and pivot-turn response;
6. Remote test measures command latency/jitter and validates reconnect behavior;
7. loss of control does not initiate Retrace or modify MQ/measurement state;
8. stale-command neutralization occurs only after the configured bounded timeout;
9. explicit E-stop remains latched across repeated drive commands until explicit reset.

No physical-response or reversal behavior is declared PASS until the wheels-up tests are performed on the actual HazRob hardware.
