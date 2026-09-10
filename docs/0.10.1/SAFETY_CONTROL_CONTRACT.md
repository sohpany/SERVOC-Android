# HazRob 0.10.1 — Safety Control Contract

Status: **AUTHORITATIVE SOFTWARE SAFETY CONTRACT**  
Change control: `HR-0101-015`

## Scope and limitation

HazRob 0.10.1 implements a **software-latched emergency stop interlock**. This improves deterministic command safety but is **not** a certified machinery emergency-stop function, safety PLC, safety relay, intrinsically safe system, or substitute for a physical power-isolating emergency stop.

## Required state model

- Normal state: `estop_latched=false`.
- `estop` command: immediately force both drive channels to zero, stop/reset active measurement, remove MQ power, clear active motion/retrace, and set `estop_latched=true`.
- While latched: every drive request remains inhibited regardless of Direct/Remote heartbeat or joystick state.
- Heartbeat/link recovery must never clear the E-stop latch.
- E-stop reset is explicit and separate from ordinary STOP.
- Reset clears the software latch but must leave commanded and actual drive output at zero. Movement requires a subsequent new operator drive command.

## Transport parity

Direct HTTP:

- activate: `/estop`
- reset: `/estop/reset`

Remote MQTT command envelope:

- activate: `estop`
- reset: `estop_reset`

Both transports call the same firmware safety implementation.

## Android operator rules

- Pressing **إيقاف طارئ** immediately sets the local operator command to L=0/R=0 before sending the E-stop command.
- While the E-stop is locally or telemetrically latched, non-zero joystick commands are discarded rather than queued.
- Ordinary tap while latched must not silently reset the E-stop.
- Reset requires an intentional long-press action and leaves L/R at zero.
- Telemetry remains the authority for displayed latch state after communication resumes.

## Telemetry

`drive.estop` shall expose the current software latch as a boolean in normalized Direct and Remote telemetry. This state is also available through `/diag` because the `drive` object is embedded in diagnostics.

## Failure behavior

- Loss of transport while E-stop is latched keeps the latch set.
- Reconnect/heartbeat does not clear it.
- Application restart does not itself authorize motion. The firmware latch remains authoritative until explicit reset or controller reboot.
- A controller reboot inherently resets RAM state; therefore a physical emergency-stop/power-isolation mechanism remains required for a true hardware safety function.

## Verification gates

Before release:

1. static cross-component contract confirms `estop_reset` exists in Android and firmware;
2. firmware compile succeeds;
3. Android unit/build succeeds;
4. Direct test: activate -> repeated drive commands remain zero -> reset -> still zero -> new drive command moves;
5. Remote test repeats the same sequence through MQTT;
6. telemetry reports `drive.estop=true/false` correctly;
7. link-loss/reconnect does not clear an active latch.
