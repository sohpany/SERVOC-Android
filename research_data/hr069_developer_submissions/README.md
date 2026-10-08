# HR069 Developer Reference Submission Staging — NOT operational ChemCore knowledge

**Source line:** HazRob Android 0.10.2 / HR069, engineering increment HR069-R1
**Repository/branch:** `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2`.
**Status:** SCIENTIFIC REVIEW INBOX / UNPROMOTED, *not* an accepted scientific reference library.
**Change-control:** `docs/0.10.2/CR-20261008-006_DEV_REFERENCE_CHEMCORE_SYNC_AUDIT.md`, `HZR-GOV-CC-001`.

A developer-only laboratory experiment that was averaged on the HR069 device and approved locally may be submitted to `approved_experiments/` for scientific review, accompanied by any separately derived artifact under `derived_references/`. These filenames incorporate the originating session identity and payload SHA-256; immutable create-only GitHub PUT is required. All submission attempts must be auditable from the device.

**Submission is NOT canonical promotion.** These unreviewed records must **not** be directly loaded by ChemCore, interpreted as a calibrated ppm model, assigned M4 status, or automatically included in Stable packages. A distinct controlled scientific admission stage must:
1. Confirm record schema `hazrob-approved-experiment/3`, original device/session/capture and experiment UID/provenance, SHA-256 source hashes, valid reference gas/concentration method, chamber/sampling profile, and no cross-device duplicate.
2. Recompute and review derived artifacts and compare to existing source records in `research_data/hr069_analytical_library/`; preserve source evidence tier.
3. Obtain the project owner's recorded scientific acceptance, plus independent tests and qualitative/quantitative qualification appropriate to the evidence tier.
4. Only after approval, write normalized accepted records to the canonical `research_data/hr069_analytical_library/` under its own change record.
5. Independently build, test, approve and distribute a **new immutable** `chemcore-stable-snapshot-v1` with `manifest.json` schema v2, consistent feature/sampling profile, revision/source SHA, size, SHA-256, data/evidence revisions, compatibility and model-policy gates.
6. Permit normal Android 0.10.2 users to download it via a separately approved **read-only** HTTPS source without repository write credentials, and preserve their previous known-good Stable package.

`HazRob/build-harness/library/` is historical/qualification support, *not* the canonical HR069 analytical corpus. Never resurrect the retired publication workflows from `.github/workflows/chemcore-library-publish*.yml` as a shortcut.

User Field Sessions/Sites are local operational records and are **NEVER** accepted into this staging lane by role substitution. Developer submission permission cannot be inferred merely from seeing the Developer tab; the exact database session role, capture lineage, averaging and human approval gates must pass.

Nothing in this directory grants a published APK, Main ESP32, ESP32-CAM, calibration curve or scientific Stable snapshot a released/authoritative status.
