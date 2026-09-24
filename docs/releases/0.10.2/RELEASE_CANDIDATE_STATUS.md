# 0.10.2 HR068 — Release Candidate Status

## Source state
Source integration is complete for the controlled 0.10.2 candidate.

- Branch: `release/hazrob-0.10.2`
- Parent baseline: `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9` (HR067 app / HR066-equivalent Main+CAM source)
- Android: `0.10.2`, versionCode `26`
- Main firmware: `HazRob 0.10.2`, API 4
- CAM firmware: `HazRob-CAM 0.10.2`
- Remote service compatibility contract: `0.10.1 / protocol 2` (intentionally unchanged until coordinated backend migration)
- Candidate artifact label: HR068

## Preserved runtime/scientific contracts
No product behavior was intentionally changed merely to create the version bump. The candidate preserves:
- Direct-first boot/default behavior;
- local robot AP and camera topology;
- E-stop and 500 ms stale-command stop;
- authoritative L298N mapping;
- HR067 field R0 metadata;
- DB schema 8 and non-destructive persistence;
- ChemCore feature schema and M0–M4 evidence semantics;
- exact-session report lineage;
- quantitative/reportability gates;
- scientific-library provenance and authority separation;
- PDF/XLSX analytical provenance.

## Selective migration result
No wholesale merge from divergent later branches was performed. Later scientific seed snapshots and divergent database implementations remain held pending explicit provenance/migration validation. Existing HR067 analytical functionality is retained.

## Automated validation state
GitHub Actions was enabled for `release/**`, but the current 0.10.2 runs failed before any workflow step started and before a runner was allocated (`runner_id=0`). Therefore those runs do not constitute a source/test failure and do not constitute a passing build.

A separate Railway validation service could not be provisioned because the current Railway plan reported its resource-provision limit reached.

Accordingly:
- source candidate: COMPLETE;
- automated binary build: BLOCKED BY CURRENT CI/RESOURCE AVAILABILITY;
- APK/firmware artifact hashes: NOT YET ISSUED;
- physical field acceptance: NOT YET PERFORMED;
- immutable `v0.10.2` tag: MUST NOT be created yet.

## Required acceptance before final tag
1. Restore CI runner/build capacity and execute the complete audit/unit/lint/release build.
2. Compile Main ESP32 and ESP32-CAM using the pinned ESP32 toolchain.
3. Record APK/Main/CAM SHA-256 and signing verification.
4. Flash the physical robot and verify Direct control first.
5. Verify local camera, command-loss stop and E-stop.
6. Only then test optional Remote, MQTT reconnect, remote video, mode switching and hotspot loss/recovery.
7. Record results and create immutable `v0.10.2` only after acceptance.
