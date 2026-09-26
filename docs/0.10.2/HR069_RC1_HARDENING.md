# HazRob 0.10.2 / HR069 — RC1 Hardening Record

Date: 2026-09-26

## Scope

This record covers the source-hardening pass performed before producing the first field-test APK/Main/CAM artifacts for HazRob 0.10.2 / HR069. The personal repository `sohpany/SERVOC-Android` remains the source of truth during this phase. Organization migration is intentionally deferred until after physical field acceptance.

## Frozen starting point

- Branch: `release/hazrob-0.10.2`
- Starting commit: `2916835430ee9096e303306f397a5b89adac3050`
- Starting message: `0.10.2 HR069: lock Wi-Fi TX power and revision authority in firmware audit`

## Applied corrections

### Database authority

- Corrected `ReleaseAudit.DATABASE_SCHEMA` from 8 to 9.
- Kept the actual `ExperimentsDb.DB_VERSION=9` implementation and existing `oldVersion<9` migration path unchanged.
- Updated the release audit unit test to lock schema 9.

### Canonical 0.10.2 release audit

- Repaired malformed `tools/audit_0102_release_contract.py` source.
- Added explicit 0.10.2 / HR069 identity checks.
- Added release-schema vs database-schema checks.
- Preserved user-library isolation, exact-session report lineage and physical safety contract checks.
- Added the canonical audit to `build-apk.yml` before historical compatibility audits.

### APK CI identity

- Build identity now records:
  - release `HazRob-0.10.2`
  - engineering revision `HR069`
  - database schema `9`
  - source SHA/ref/repository/run
- Artifact retention reduced to 14 days.
- Public update publication remains fail-closed on the historical release gate and is not enabled for the HR069 qualification branch during RC1 testing.

### Firmware baseline and packaging

- Added `firmware/FIRMWARE_BASELINE_0.10.2.md` from executable source authority.
- Removed firmware-package fallback to the legacy 0.10.1 baseline document.
- Firmware package now contains `SOURCE-COMMIT.txt` and SHA-256 sums.
- Firmware artifact retention reduced to 14 days.
- ESP32 core remains pinned to `esp32:esp32@3.3.11`.

### Reference Bank publisher identity

- Preserved `sohpany/SERVOC-Android` and `reference-bank` as the current publication target during the personal-repository phase.
- Updated request User-Agent identity from `HazRob-Android/0.9.2` to `HazRob-Android/0.10.2`.

## Confirmed active remote coordinates

- Main/Android service version: 0.10.2
- Wire/bootstrap protocol: 2
- Robot ID: `hazrob-001`
- MQTT primary: EMQX Cloud Serverless, TLS 8883
- Bootstrap/video authority: Railway `hazrob-video-relay-0102-branchfix-production.up.railway.app`
- HiveMQ: controlled contingency, not automatic hot failover in HR069

No remote platform rebinding is performed during RC1 source hardening.

## Intentionally deferred items

### Flammability asset pinning

The release build currently verifies a fixed SHA-256 for the accepted NIOSH flammability asset and, if the source asset is absent, recovers the exact asset from the previously accepted 0.10.1 APK. The asset does not exist in Git history at its intended source path. Therefore this hardening pass does **not** regenerate or substitute scientific data. The fallback remains temporarily in place until the exact accepted asset can be extracted and pinned byte-for-byte.

### APK signing secret migration

The established prototype signing certificate must remain unchanged for Android upgrade continuity. Migration of the keystore/password material to GitHub Secrets is deferred until the build harness/organization secret store is available. RC1 must continue to verify the expected signing certificate SHA-256 before artifacts are accepted.

### Organization migration

Repository ownership, Railway/Render source rebinding, HiveMQ/EMQX reconfiguration and Cloudflare integration are deferred until physical robot acceptance.

## Qualification gates before RC1 is accepted

1. Canonical source audit passes.
2. Historical compatibility audits required by current CI pass.
3. Pinned scientific dataset SHA/provenance validation passes.
4. ChemCore Stable snapshot packaging validation passes.
5. Android unit tests pass.
6. Signed release APK builds and matches the established signer certificate.
7. Main ESP32 compiles with pinned ESP32 core.
8. ESP32-CAM compiles with pinned ESP32 core.
9. APK/Main/CAM artifacts are tied to one exact source commit SHA.
10. Physical field acceptance is performed before any clean migration or platform rebinding.

## Field-test rule

Every physical test result must record the artifact source commit SHA. If a functional or protocol-level code change is introduced after RC1, evaluate whether the engineering revision must advance beyond HR069 rather than silently reusing the same revision identity.
