# HazRob 0.10.2 Baseline Manifest

Status: development baseline; not a field-accepted release.

## Provenance
- Parent baseline: HR067 application / HR066 Main + CAM firmware.
- Exact source parent: `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.
- Android baseline was 0.10.1 versionCode 25; 0.10.2 uses versionCode 26.
- Protocol remains 2 unless a separately reviewed wire-contract change is approved.

## Preservation rule
0.10.2 is additive. HR067 analytical, reporting, database, calibration, ChemCore, fingerprint and scientific-library components are preserved. No component is deleted merely because a newer branch exists.

## Connection rule
Direct is the mandatory default and must remain functional without WAN. Saved WAN credentials must not auto-activate Main ESP32 STA at boot. Remote is optional and failure-isolated. ESP32-CAM STA to the HazRob AP is part of the Direct topology and is not equivalent to Main ESP32 WAN STA.

## Safety invariants
E-stop, safety interlock and the 500 ms motion command watchdog are release invariants. Remote transport must not bypass them.

## Scientific integrity
Identification/screening evidence is distinct from quantitative PPM validity. Quantitative output requires an accepted calibration/model path and valid operating domain. Exposure-limit comparison must not manufacture concentration from identification evidence.

## Migration policy
Changes from later divergent branches are ported selectively after compatibility review. Main-ESP32 WAN STA/AP+STA/NAPT changes are held for physical testing unless explicitly approved.

## Acceptance gates
Direct control, local camera, stale-command stop and E-stop are tested before optional Remote/MQTT/video tests. Final tag `v0.10.2` is prohibited until physical acceptance and artifact hashes are recorded.
