# CR-20261008-007 — Correct HR069-R1 diagnostic artifact provenance

**State:** REGISTERED / WORKING CHANGE / NO RELEASE APPROVAL
**Date:** 2026-10-08
**Production branch only:** `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2`
**Starting branch HEAD:** `b7e3ba18224258fe3574003861ae348e5cadab9b`
**Latest successfully qualified functional source:** `ac01a9c3c6b5671e036e4c24abdc484ff8cdb0c2`
**Current owner-approved APK baseline:** `43a01d86c260353a77f1a45f808f22cf46997e06`, Android versionCode 26, engineering HR069. This is NOT superseded.
**Working increment:** 0.10.2 / HR069-R1, Android versionCode 27, main ESP32 API4 with `build_revision` HR069-R1, camera HR069 unchanged.

## Problem
The successful WORKING/UNAPPROVED APK artifact for workflow run 37848396297 carried a build manifest `engineering_revision=HR069` and stale `change_record=CR-20261004-003`, despite being built from HR069-R1 code. It also labeled the APK itself HR069. The binary SHA-256 was correct, but these metadata fields misstate the actual working revision. The discrepancy must not be silently ignored or repaired by manually renaming a binary.

## Authorized scope
Modify ONLY `.github/workflows/build-rc2-test-apk.yml` and `.github/workflows/build-apk.yml` to use explicit `HR069-R1` for working artifacts, log the relevant current CR/checkpoint, keep exact `GITHUB_SHA`, run number, APK hash, versionName/versionCode and signing fingerprint. Do not change build/runtime logic, release authority, P13 upload prohibitions or physical acceptance requirements. Do not create a branch.

## Risks/rollback
A malformed YAML string or mistaken artifact name could interrupt qualification or incorrectly imply approval. Rollback is git revert of the exact metadata-only change on the same controlled branch; preserve action logs. Current known-good approved APK remains versionCode26 HR069. Never release any new artifact unless tested and explicitly owner approved.

## Acceptance criteria
1. The new run builds `app` successfully with `versionCode 27`, `versionName 0.10.2`, `ReleaseAudit.ENGINEERING_REVISION HR069-R1`.
2. The diagnostic artifact is named `HazRob-0.10.2-HR069-R1-RC2-WORKING-...` with `authority_status=WORKING_UNAPPROVED_NON_AUTHORITATIVE` and exact source SHA.
3. Manifest includes `engineering_revision=HR069-R1`, correct CR/checkpoint reference, calculated APK SHA-256, versionCode, signer.
4. `build-apk.yml` retains explicit P13 denial of final APK release/export; change does not confer approval.
5. Android unit/integration checks and Main/CAM firmware source builds must pass for the exact test SHA. Actual IR/Echo, motor stop, joystick and PDF/XLSX acceptance on physical hardware remain pending and cannot be inferred from CI success.

## Preliminary evidence
- 37848396209: Android internal validation PASS (source `ac01a9c3`).
- 37848396238: Android/source tests + Main/CAM compile PASS (source `ac01a9c3`).
- 37848396297: WORKING Diagnostic APK artifact build PASS (source `ac01a9c3`). Existing artifact APK hash is `51f06803fcf43b3beb8e5c63facba07bf9dcd11693df1c9620a243d315e0c1ec`; **not** to be rewritten in retrospect.
- CR owner instruction: maintain one branch, document every revision, never revert to <0.10.2/HR069. This CR is a source-level diagnostic metadata correction, not promotion.

## To complete
- Implement workflow metadata correction.
- Re-run tests/build at exact updated SHA, record artifact ID/hash and signer, review.
- Leave result WORKING/UNAPPROVED until owner/physical acceptance.
