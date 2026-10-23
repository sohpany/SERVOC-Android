# RC3 RELEASE CANDIDATE CHECKPOINT — 2026-10-24

## Identity and boundaries

- Product: ORIGINAL wheeled HazRob Android; **not** HazRob Tank.
- Repository: sohpany/SERVOC-Android.
- Branch: release/hazrob-0.10.2-rc3.
- Base: RC2 commit 698a3c9e98449c0235f762a7194964ef5a48fb1f.
- Android versionName 0.10.2; Android versionCode 31; engineering HR069-R1; RC label RC3.
- Keep `com.servoc.app`, HR069-R1 logo bytes, original ESP32/ESP32-CAM firmware and the operator/developer ChemCore separation unchanged.

## Change history imported from RC2

1. Sessions crash containment: non-reentrant refresh, stable spinner snapshots, grouped counts, guarded navigation, SQLite non-destructive user table recovery. Root cause not proven without Logcat.
2. Tank-inspired, independently implemented two-pane field sessions [الإدارة / التقارير]; explicit NEW/APPROVED records and deliberate approvals; protected retry and approved-only reports. Does NOT import Tank's legacy crashing SessionTab.
3. Site/session PDFs and Excel remain tied to exact archived measurements and qualified analytical evidence; no pooled PPM and no reporting of unapproved sessions.
4. MAX4466 sound display refresh = 1000 ms, display smoothing; raw sensor sampling preserved.
5. Echo 5 V-to-3.3 V translator affects voltage amplitude, not time-of-flight. Valid readings allow display offset; invalid pulses show diagnostics only; safety stop parameters remain in firmware.
6. Drive landscape MQ switch relocated; R0 clean-air baseline 30 one-second samples with stability gate and explicit review; light physical table; dark legible tracking search rows; visible direction-reversal checkboxes.
7. Tank-inspired light color palette for non-driving tabs; preserve operator control visual safety and the unchanged original logo.

## ChemCore Stable version policy

- Latest source-verified **deployable compatible** embedded Stable available in wheeled source: `1.0.0-hr069r1`, 652 molecular identities and 49 research screening priors M1/M2, **zero M4 reference-gas calibration records**.
- Source build procedure: `tools/build_hr069_embedded_stable.py` and Gradle task `prepareHr069EmbeddedStable`.
- RC3 independent verification: `tools/verify_rc3_embedded_stable.py` checks manifest identity, compatibility, count, M4 policy, payload SHA-256/length and **identical bytes packaged in final APK**.
- The HazRob/build-harness 2026-10-21 `0.1.0-research-reference-snapshot` is declared `PUBLISHED_RESEARCH_REFERENCE_SNAPSHOT_NOT_ACTIVATED` and explicitly has `runtime_activation:false` and `original_HR069_app: RESEARCH_REFERENCE_ONLY_NOT_ACTIVE`. It **must not** be substituted for approved operational Stable or presented as M4.
- Preserve a verified newer locally installed Stable instead of downgrading to APK seed. Separate central stable publication needs a formal controlled approval and compatible publication manifest.

## Required validation and release gate

Run:
```sh
python3 tools/audit_rc3_release_candidate.py
python3 tools/audit_0102_rc2_runtime_contract.py
python3 tools/audit_0102_release_contract.py
python3 tools/build_hr069_embedded_stable.py --out-dir app/build/generated/hazrobAssets/generated/chemcore/embedded_stable
python3 tools/verify_rc3_embedded_stable.py --asset-dir app/build/generated/hazrobAssets/generated/chemcore/embedded_stable
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
python3 tools/verify_rc3_embedded_stable.py --asset-dir app/build/generated/hazrobAssets/generated/chemcore/embedded_stable --apk app/build/outputs/apk/debug/app-debug.apk
apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
```

Acceptance on handset: open Sessions repeatedly including sites with 0/1/multiple entries, restore backup and verify approval, unapproved PDF gate, document PDF/XLSX export, track list black labels, drive reverse checkboxes, landscape relay/E-STOP separation, Echo invalid timeouts/offset, MAX4466 1 Hz, auto-R0 with stable clean air and failure invalid samples, physical rows, preserved prior sessions.

## Status and blocked release

- GitHub Actions on late RC2 commits returned failed jobs with **zero steps** (runner unavailable before checkout), not a source-unit failure or a passing build.
- CI state of RC3 must be checked after push. If workflow still cannot allocate runners, **no APK is created or approved**.
- Manual promotion through HazRob/build-harness controlled-release policy remains required; do not present a candidate as approved by CI alone. An installable device-verified standalone APK needs the same signing certificate for in-place upgrade.
- Preserve prior user app data; do not uninstall to test a new release.
