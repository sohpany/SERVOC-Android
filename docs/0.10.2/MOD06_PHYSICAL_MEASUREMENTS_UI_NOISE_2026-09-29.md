# HazRob 0.10.2 / HR069 — MOD-06 Physical Measurements UI + Noise

## Requested behavior
- Improve the Physical Measurements tab on narrow Android screens so measurement names, values, units and source/status text remain readable instead of wrapping character-by-character.
- Show the live MAX4466 noise level in dB in the physical measurements list.
- Use the current microphone amplifier gain reference of **25×** as configured on the hardware.

## Implementation
- Added `PhysicalMeasurementsUiEnhancer` and install it from `HazRobApplication` while the operator workspace is active.
- The physical table is identified by its header contract (`القياس`, `القيمة`, `الوحدة`).
- Disabled `TableLayout` automatic stretch/shrink and applied explicit proportional widths:
  - measurement 30%
  - value 18%
  - unit 16%
  - status/source 36%
- Increased the primary numeric value text size for faster reading.
- Kept all 15 physical catalog rows and the MAX4466 RMS/P-P and IR diagnostic rows; no measurement was removed.

## Noise telemetry correction
The Main ESP32 firmware already calculates an estimated SPL using:
- `AMP_GAIN=25.0f`
- microphone sensitivity reference `MIC_V_PER_PA=0.00630957f`
- `soundDb = 94 + 20*log10(micVrms/MIC_V_PER_PA)`

Firmware already publishes:
- `sound.rms_v`
- `sound.p2p_v`
- `sound.db_spl`
- `sound.db_estimated=true`
- `sound.gain_reference=25`

A normalization defect was found: `RobotProtocolV4.sound()` previously retained RMS/P-P but dropped `db_spl` and `gain_reference`. MOD-06 now preserves both fields in the unified snapshot, so the Physical Measurements tab can show the noise level directly.

`PhysicalMeasurementsUiEnhancer` also contains a raw `_data_api.sound` fallback so the value remains visible during staged compatibility transitions.

## Display semantics
- Noise primary value: one decimal place, unit `dB` from `PhysicalCatalog`.
- Source/status: `MAX4466 • dB SPL تقديري • Gain 25×`.
- RMS and P-P remain diagnostic voltage values below the primary noise row.
- `dB SPL` is explicitly described as **estimated**, not a certified sound-level-meter reading. Setting amplifier gain to 25× aligns the firmware calculation with the current hardware setting but does not replace acoustic calibration against a traceable reference instrument.

## Related correction discovered during verification
MOD-04 ECHO overlay was updated to accept the normalized `device.proximity.distance_cm` field and retain a raw `device.proximity.cm` fallback. This prevents a valid ultrasonic reading from being rendered as `ECHO — cm` because of a field-name normalization mismatch.

## Automated guard
Added `PhysicalMeasurementsUiContractTest` to verify:
- responsive table installation and column policy;
- normalized `db_spl` and `gain_reference` preservation;
- display source text;
- firmware 25× gain reference and dB telemetry contract.

## Acceptance pending
No APK or firmware ZIP is released at this stage. Physical/UI acceptance after the final integrated build must verify:
1. Portrait Physical Measurements table is readable on the target phone without single-character column wrapping.
2. Temperature, humidity, pressure, altitude and unavailable/not-installed rows remain correctly aligned.
3. MAX4466 `dB` updates live when sound level changes.
4. RMS and P-P continue to update.
5. Gain reference is shown as 25×.
6. Compare the estimated dB reading with a reference SPL meter before treating it as quantitative occupational-noise data.
