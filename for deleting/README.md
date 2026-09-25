# SERVOC / HazRob — for deleting

Audit date: 2026-09-25
Authority branch: `release/hazrob-0.10.2`

## Purpose

This file is the controlled manual-deletion manifest produced after comparing legacy and temporary branches against HazRob 0.10.2.

**Important:** deleting this folder does NOT delete Git branches. Branches are Git refs and must be deleted manually from the GitHub Branches interface (or with an explicit ref-deletion operation). This manifest is only the audited checklist.

## A. Legacy branches — safe to delete

These branches are either ancestors of 0.10.2 or were separately reviewed and their relevant functionality is superseded/preserved in 0.10.2.

- dev/v0.2-analytics
- dev/v0.3-gas-models
- fix/v0.3.1-hardware-integration
- dev/v0.4-full-ui
- dev/v0.5-arabic-integration
- dev/hazrob-0.6
- dev/hazrob-0.6-core
- dev/hazrob-0.6-final
- dev/hazrob-0.6-hardware-sync
- dev/hazrob-0.6-impl
- dev/hazrob-0.6-protocol
- dev/hazrob-0.6-work
- dev/hazrob-0.7
- dev/hazrob-0.7.1
- dev/hazrob-0.8
- archive/main-pre-hazrob-0.8
- reference-bank
- dev/hazrob-0.9
- fix/hazrob-0.9.1-unified-remote
- fix/hazrob-0.9.2-integration-hardening
- dev/hazrob-0.9.3-alerts-ldr
- dev/hazrob-0.9.3-rc2-l298n-drive
- feat/hazrob-0.9.3-alerts-ldr

## B. 0.10 / 0.10.1 / HR branches — safe to delete

The reviewed content is already incorporated, superseded, or preserved by the 0.10.2 implementation/documentation.

- dev/hazrob-0.10-chemcore-foundation
- dev/hazrob-0.10.1-hr037-staging
- dev/hazrob-0.10.1-hr044-release-candidate
- dev/hazrob-0.10.1
- docs/hazrob-0.10.1-hr037-temp
- hr0101037-final
- hr0101037
- baseline/hazrob-0.10.1-software
- release/hazrob-0.10.1-software-baseline
- release/hazrob-0.10.1-final-hr063
- release/hazrob-0.10.1-hr064
- fix/hr063-wifi-field-repair
- fix/hr065-field-network
- fix/hr-0101-048-field-remediation
- tmp-hr063-doc
- tmp-hr063
- archive/hr054-pre-baseline-a
- build/railway-apk-v23
- dev/0101-analytical-v4
- build/analytical-v4-stage2-validation
- build/analytical-v4-stage3-validation
- build/analytical-v4-stage4-validation
- build/analytical-v4-validation

## C. Optional prototype — delete if no longer needed as a historical prototype

- temp/hazrob-mini-remote

This is a standalone temporary MiniRemote prototype (temporary Android app, ESP32/ESP32-CAM firmware and relay). It is not part of the current HazRob 0.10.2 production architecture.

## D. KEEP — do not delete in this cleanup

- main
- release/hazrob-0.10.2
- app-update-distribution

Any newer active operational/distribution branch not explicitly listed above must be reviewed before deletion.

## Preservation findings

Before classifying branches for deletion, the audit checked the areas most likely to contain unique value:

- chemical fingerprint/reference construction;
- R0 normalization and calibration curves;
- exact measurement-session lineage;
- ChemCore model artifacts and open-set/quality gates;
- PDF/XLSX analytical reporting;
- direct/remote connectivity, MQTT and video relay;
- gas alert/LDR behavior;
- L298N drive behavior;
- GPIO/hardware authority;
- controlled HR documentation.

Relevant legacy behavior is already represented by newer 0.10.2 code or controlled documentation. The old HR044 firmware compatibility note is also covered in substance by the retained HR044 controlled-change documentation.

## Acceptance boundary

This cleanup classification is a source/history audit. It does not replace physical acceptance. BME280/environmental hardware validation and final robot physical/E2E acceptance remain separate gates.
