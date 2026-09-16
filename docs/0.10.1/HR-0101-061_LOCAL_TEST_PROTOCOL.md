# HR-0101-061 — Local Experimental Qualification Protocol

Date: 2026-09-16
Release: HazRob 0.10.1
Status: ACTIVE

## Architecture acceptance contract

1. Boot profile is DIRECT.
2. Main ESP32 is the only SoftAP authority.
3. Visible robot SSID is exactly `HazRob`.
4. Main local endpoint is `192.168.4.1`.
5. ESP32-CAM is STA-only in the controlled production path.
6. Camera local endpoint is `192.168.4.2`.
7. Retained WAN credentials never authorize automatic Remote activation.
8. Remote is secondary and requires an explicit operator transition.
9. Android Direct traffic is pinned to the HazRob local Wi-Fi route.
10. Main, CAM and APK candidates must be traceable to one controlled source SHA.

## Qualification stages

### L1 — Static source contract
Audit production entrypoints and shared constants. Reject any CAM AP/AP+STA fallback/test path in the controlled release path.

### L2 — Automated contract tests
Run integration, motion and consolidated release-gate audits. Any failure blocks release.

### L3 — Reproducible build
Compile master ESP32 and ESP32-CAM with pinned ESP32 core 3.3.11. Build Android candidate from the same SHA. Capture SHA-256 hashes and build identity.

### L4 — Artifact integrity
Verify exact source SHA, source directories, hashes and release identity. Mixed-SHA or unidentified artifacts are rejected.

### L5 — Software integration
Verify Android canonical SSID/hosts, Direct routing, camera endpoint, explicit Remote transition and diagnostics against the same network contract.

### P1 — Physical cold boot
Flash both exact controlled firmware artifacts. Cold boot. PASS only if `HazRob` is visible and no CAM-created robot AP is visible.

### P2 — Local addressing
PASS only if main responds at 192.168.4.1 and CAM joins as STA at 192.168.4.2.

### P3 — Direct operation
Verify control, telemetry, camera stream, reconnect and Android route persistence without Internet dependency.

### P4 — Remote transition
From an already healthy Direct state, explicitly request Remote. Verify AP continuity while STA/cloud path is added. Reboot must return to Direct.

## Current evidence

The previous physical T01 observation failed P1: the expected HazRob AP was not visible and a camera AP was visible. Current production source inspection does not authorize a CAM AP path, so artifact/flash provenance remains a primary hypothesis until exact controlled artifacts are rebuilt and reflashed.

GitHub Actions runner allocation is currently a separate qualification blocker: the observed firmware jobs terminate before any step executes and therefore do not constitute a compilation result.

## Release rule

No APK/ZIP is labelled FINAL, FIELD-QUALIFIED or RELEASE-READY until L1-L5 and P1-P4 have objective evidence. A software-only pass may be labelled SOFTWARE-QUALIFIED / PHYSICAL ACCEPTANCE PENDING.
