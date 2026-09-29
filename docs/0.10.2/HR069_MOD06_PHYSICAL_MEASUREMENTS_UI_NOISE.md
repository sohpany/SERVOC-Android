# HazRob 0.10.2 / HR069 — MOD-06 Physical Measurements UI + MAX4466 Noise

## User request

The physical-measurements tab must be readable on a narrow Android phone. The existing four-column table allowed columns to shrink until units and Arabic labels wrapped character-by-character. The MAX4466 microphone must also show the noise level in dB after the hardware gain was set to 25.

## Implemented behavior

### Responsive physical-measurements table

- The physical table no longer uses uncontrolled `stretchAllColumns` / `shrinkAllColumns` behavior at runtime.
- Runtime column proportions are fixed to `30% / 18% / 16% / 36%` for measurement / value / unit / status-source.
- Numeric values are emphasized at 14sp and bold.
- Engineering units are kept on one line, centered, bold, and given a minimum width of 44dp to prevent `µg/m³`, `m/s²`, `µSv/h`, etc. from breaking character-by-character.
- The status/source column remains multiline and is limited to three lines.
- The enhancement is installed from `HazRobApplication` and is applied when the physical-measurements table becomes visible.

### MAX4466 noise level

- Main ESP32 firmware already computes an estimated sound-pressure level using `AMP_GAIN=25.0f`.
- Firmware publishes `sound.db_spl`, `sound.db_estimated=true`, and `sound.gain_reference=25`.
- `RobotProtocolV4` now preserves `db_spl` and `gain_reference` in the normalized diagnostics snapshot instead of dropping them.
- The physical `الضوضاء` row displays the live reading to one decimal place in `dB`.
- The source/status text explicitly reports `MAX4466 • dB SPL تقديري • Gain 25×`.
- The UI enhancer retains a raw `_data_api.sound` fallback so the reading remains visible if a staged normalizer ever omits the normalized field.

## Scientific limitation

The displayed dB value is **estimated dB SPL**, not a traceably calibrated sound-level-meter result. Setting the MAX4466 gain to 25× aligns the firmware calculation with the current hardware setting, but absolute SPL accuracy still requires acoustic calibration against a reference calibrator or calibrated sound level meter. The application therefore keeps `calibrated_db_spl=false`.

## Regression protection

`PhysicalMeasurementsUiContractTest` protects:

- installation of the UI enhancer,
- responsive column weights,
- disabled automatic column shrinking,
- single-line engineering units and minimum unit width,
- emphasized value text,
- normalized `db_spl` and `gain_reference`,
- firmware `AMP_GAIN=25.0f`,
- firmware publication of `db_spl` and gain reference,
- estimated/not-calibrated declaration.

The canonical `tools/audit_0102_release_contract.py` also contains cross-layer guards for MOD-04, MOD-05 and MOD-06.

## Physical acceptance pending

1. Open `القياسات > الفيزيائية` on the target phone in portrait mode and verify that units no longer stack one character per line.
2. Verify that measurement name, value, unit and source remain readable without horizontal clipping.
3. Produce quiet and louder sound near the MAX4466 and confirm that the `الضوضاء` row changes and displays a finite value in dB.
4. Confirm RMS and P-P diagnostic rows continue to update.
5. Compare the displayed dB against a calibrated sound level meter at several steady levels before treating it as quantitative SPL.
6. Verify the same normalized sound data in Direct and Remote operation.

**Status:** IMPLEMENTED IN SOURCE — INTEGRATED BUILD/PHYSICAL ACCEPTANCE PENDING.

No APK or firmware ZIP is released at this stage; artifact production remains held until the requested modification list is declared complete.
