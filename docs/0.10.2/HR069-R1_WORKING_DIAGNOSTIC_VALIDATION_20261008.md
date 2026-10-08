# HazRob 0.10.2 / HR069-R1 — Working Diagnostic Validation Manifest

**Date:** 2026-10-08
**STATE:** WORKING / UNAPPROVED / NON-AUTHORITATIVE. **DO NOT PROMOTE OR PRESENT AS APPROVED.**
**Branch:** `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2` (the ONLY development branch).
**Exact compiled and tested source SHA:** `b17e37ade80d79bcdfd5f320e2eb096093b6d2bb`.
**Android release:** `versionName=0.10.2`, **WORKING** `versionCode=27`, `ReleaseAudit.ENGINEERING_REVISION=HR069-R1`, package `com.servoc.app`.
**Main ESP32:** firmware banner `HazRob 0.10.2`, API v4, independent `/diag.build_revision=HR069-R1`. **ESP32-CAM:** HR069 unchanged, jointly compile-tested.
**Owner-approved APK baseline:** prior `versionCode=26` / HR069, source `43a01d86c260353a77f1a45f808f22cf46997e06`; it remains the authoritative baseline until a new approval/promotion manifest exists.

## Exact working APK evidence

- GitHub Actions run: `37849238447`, WORKING Diagnostic APK, **SUCCESS**.
- Artifact ID: `11580847969`; workflow run number: `291`.
- Artifact name: `HazRob-0.10.2-HR069-R1-RC2-WORKING-UNAPPROVED-291-b17e37ade80d79bcdfd5f320e2eb096093b6d2bb`.
- APK size: `3,567,297` bytes.
- APK SHA-256: `51f06803fcf43b3beb8e5c63facba07bf9dcd11693df1c9620a243d315e0c1ec` (validated against `SHA256SUMS.txt`).
- APK signer certificate SHA-256: `d6b5b69be382f2afa6df4b46bade5b6f864f4060622e949075293ac62003d3f0`.
- `ARTIFACT-MANIFEST.txt`: `authority_status=WORKING_UNAPPROVED_NON_AUTHORITATIVE`, `engineering_revision=HR069-R1`, `change_record=CR-20261008-007`, `source_sha=b17e37...`, `runtime_contract=PASS`, `android_unit_tests=PASS`, `apk_build=PASS`, `apk_signer_verification=PASS`.
- **No APK release authority implied** by this source, artifact filename, versionCode or CI pass.

## Automated test evidence — all on the exact b17e37 source SHA

| CI Workflow | Run ID | Result | Coverage |
| --- | --- | --- | --- |
| Validate HazRob 0.10.2 HR069 Development | `37849238516` | **SUCCESS** | HR069 source/Android contracts, Android tests, Main ESP32 compile, ESP32-CAM compile |
| Build HazRob APK — Internal Validation | `37849238454` | **SUCCESS** | source contracts, Android tests, internal signed APK build and verifier; no authoritative artifact export |
| Build HazRob RC2 WORKING Diagnostic APK | `37849238447` | **SUCCESS** | runtime/source/Android/ChemCore tests, diagnostic APK build/hash/cert validation, unapproved archive upload |

## HR069-R1 engineering scope qualified at code/CI level

- Echo acquisition 150ms nominal + application Direct 150ms polling + MQTT Remote 150ms publish, freshness and status diagnostics. **Physical echo range and collision reaction NOT validated**.
- IR `HIGH = ground`, `LOW = no ground` on GPIO35 at 3.3V, no extra software-transition verification (per owner). Persisted selectable forward auto-stop default OFF. **Sensor was previously stuck HIGH on floor and elevated; owner must adjust/test physically.**
- Independent spring-return dual tank sliders, corrected pivot buttons, separate invert-pivot option excluding tank, joystick directional mixing, and Android onPause motor-zero/explicit stop. **Real drive safety trial NOT performed**.
- Target-specific tracking uses fresh unique MQ readings, not repeated/stale telemetry; arrow needs spatial evidence.
- Local USER sites/sessions; protected atomic deletion of own measurements; role-separated DEVELOPER controlled lab sessions; user report SmartField/SmartSite scope; no arbitrary partial multi-session aggregate.
- SmartReport PDF/XLSX background writers and disk-backed XLSX zip; removal of UI-thread bundle resolution and old one-chart operator fallback. **Real exported PDF/XLSX files still require on-phone open/inspection.**
- 1024px transparent PNG logo in launcher and first app page, with underlying black hexagon opaque and outside transparent. **Owner visual confirmation pending**.
- Developer approved experiment uploads can only STAGE into `research_data/hr069_developer_submissions/` on this RC2 branch, with role/lineage and SHA checks; they CANNOT automatically update canonical ChemCore or confer M4.
- ChemCore read-only Stable distribution remains **NOT E2E VERIFIED**; build default manifest URL is unconfigured, legacy publishers retired. No GitHub credentials embedded in client package. No live scientific uploads performed.

## Open, BLOCKING release acceptance gates

1. Robot hardware: physically validate IR toggles when moved between ground (<4cm) and no-ground (>70cm), calibration/trimpot and correct HIGH/LOW, 3.3V DO to GPIO35, then optionally enable auto-stop safely.
2. Echo physical targets and ECHO 5V-to-3.3V level conversion, zero/invalid/timeout reporting, 5/10cm thresholds, 150ms acquisition/network behavior.
3. Physical differential drive: dual tank finger lift, joystick diagonals, pivot direction, STOP on Android backgrounding, lost link/E-STOP watchdog, both Direct and Remote.
4. Validate the actual generated PDF and XLSX for full Site and single Session on the target Android device, and confirm no crash/content corruption.
5. Validate developer lab session role vs USER local privacy, CRUD deletion+SAF tombstones, actual central staging permission and controlled stable publication separately (without promoting user measurements).
6. Owner visual acceptance of transparent logo in launcher/connection screen and preservation of local app data on in-place upgrade; no uninstall as part of a routine update.
7. Collect actual Main ESP32 & ESP32-CAM firmware binary/ZIP hashes and joint field trial with APK; final owner approval/promotion and immutable release manifest per `HZR-GOV-CC-001`.

## Related logs

- Scope and resumable execution: `docs/0.10.2/HR069-R1_EXECUTION_CHECKPOINT_20261008.md`
- Developer source/Stable dependency: `docs/0.10.2/CR-20261008-006_DEV_REFERENCE_CHEMCORE_SYNC_AUDIT.md`
- Working artifact identity correction: `docs/0.10.2/CR-20261008-007_HR069_R1_DIAGNOSTIC_ARTIFACT_IDENTITY.md`

This is a **working diagnostic record**, not the new `CURRENT_RELEASE_AUTHORITY.md` or proof of complete field validation. Later documentation-only commits need not change the tested functional SHA; preserve its exact APK hash and all existing workflow run IDs.
