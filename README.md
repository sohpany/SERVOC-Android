# HazRob Android

HazRob is the Android operator, evidence and analytical application for the remote hazard-inspection robot.

## HazRob 0.6 core

- Android application: `com.servoc.app`, versionCode `7`, versionName `0.6.0`.
- Master ESP32 firmware API v3 is intentionally compact and deterministic: drive, STOP/failsafe, relay control, MQ acquisition, BME/BMP acquisition, measurement sequencing and settings.
- ESP32-CAM remains an independent camera/flash subsystem; Android receives one MJPEG stream and fans frames out internally to display, recording and experimental vision analysis.
- Session role gate: normal `USER` mode and PIN-protected `DEVELOPER` mode. Developer experiments/calibration/reference-bank functions are absent from the normal-user UI.
- One unified **Measurement & Analysis** workflow replaces the previous separate sensor/chemical/measurement tabs.
- Offline-first SQLite schema v7 is no longer structurally limited to 144 chemicals; legacy IDs are retained only where old analytical models still require them.
- Bundled pinned official datasets include 652 NIOSH CAS-index chemical identity records, 402 NIOSH IDLH records and 638 normalized OSHA Table Z-1 exposure-limit records, each with provenance and SHA-256 manifests.
- Developer reference captures accept dynamic database UID, CAS or exact chemical name rather than being limited to the legacy 1–144 set.
- MQ time-series, transient-feature extraction and sensor-health/drift support are available for validation workflows.
- Camera recording converts ESP32-CAM MJPEG frames to H.264/MP4 on the phone, retains frame timing evidence and exports the finalized video to `Movies/HazRob`.
- Experimental visual-flame evidence stores temporal confidence, region and event snapshots; it never commands the robot or validates gas PPM.
- A compound-specific gas-direction assistant combines movement history with relevant MQ response changes to estimate a relative source bearing. It is advisory only and does not autonomously drive.
- Approved developer experiments can be exported locally as immutable JSON records and optionally published directly to the dedicated GitHub `reference-bank` branch. The developer supplies a fine-grained token for the current session only; no GitHub credential is embedded or persisted by HazRob.

## Camera and evidence

Camera display and flash remain independent of the master ESP32 control path. A single MJPEG connection feeds display, optional H.264/MP4 recording and experimental visual-flame analysis.

When media capture occurs during an active MQ measurement cycle, video and visual events reuse that measurement `session_id`, allowing later correlation with environmental, MQ and motion telemetry. Outside a measurement cycle, the media subsystem creates a separate evidence session. Vision evidence is not used to calculate compound identity, PPM or exposure limits.

## Analytical validity

Chemical-knowledge coverage and MQ analytical coverage are separate. The presence of a chemical, exposure limit or hazard record in the database does not imply that the installed MQ array can identify or quantify it.

Legacy/default MQ `R0=10` values are not treated as physical reference-gas calibration. Quantitative PPM and exposure-risk interpretation require a suitable validated sensor/compound calibration path and domain checks. Otherwise HazRob reports identification/fingerprint/research evidence without fabricated PPM.

## Current robot sensors

Installed direct MQ channels: MQ-2, MQ-3, MQ-4, MQ-7, MQ-8 and MQ-135. MQ-5 and MQ-9 remain library expansion slots. BME280/BMP280 environmental acquisition is direct I²C and independent of MQ relay switching.

## Firmware source of truth

The current compact master firmware is stored under `firmware/HazRob/` as four Arduino tabs:

- `HazRob.ino`
- `Config.h`
- `Hardware.h`
- `Hardware.cpp`

The local Wi-Fi SSID remains `Servoc` for compatibility with the existing ESP32-CAM setup. The project/application identity remains HazRob.

## Scientific data and references

Scientific-library design, source provenance and the wider research bibliography are documented in `docs/SCIENTIFIC_LIBRARY_v0.6.md`, `docs/GAS_MODEL_RESEARCH.md` and `REFERENCES_APA.md`. Pinned dataset integrity is checked by CI before the APK build.

## Reference-bank publication

Only an already `APPROVED` developer experiment can be published. The GitHub destination is the `reference-bank` branch under `reference_bank/approved_experiments/`. Publication filenames are canonical and derived from immutable experiment identity/timestamp; the create request deliberately omits an existing-file SHA, so a duplicate path is rejected instead of silently overwriting an approved record. Repository credentials remain session-only input.

## Deferred beyond 0.6

4G/VPN transport, thermal sensing, physical MUX expansion, ESP-NOW nodes, autonomous plume-following and autonomous return-on-link-loss remain separate development layers. Motion history and gas-direction evidence are recorded in 0.6 as foundations for later navigation research, but the ESP32 local STOP/failsafe remains the authority on communication loss.
