# SUPERSEDED — HR069 ChemCore Central Library ↔ Organization Research Sync Plan

**Original plan date:** 2026-10-02  
**Superseded:** 2026-10-04  
**Change record:** `HazRob/build-harness/docs/CR-20261004-007_RETIRE_OBSOLETE_CENTRAL_LIBRARY_SYNC_PLAN.md`  
**Status:** HISTORICAL REFERENCE ONLY — DO NOT EXECUTE

## Why this document is superseded

The original version of this file proposed an authority model in which `HazRob/build-harness/library/` would become the canonical scientific-library owner and `sohpany/SERVOC-Android` would act mainly as an application consumer.

That model is withdrawn and is **not** the current HazRob authority model.

The original plan remains available through Git history for provenance, including commit `bea62203bfe509c794aada0fb74dba38ef7f6ba6`. It must not be resumed, completed, or used as authority for any present or future library synchronization.

## Current authoritative model

For HazRob RC2 / Android 0.10.2 / HR069:

1. **Production/executable repository:** `sohpany/SERVOC-Android`.
2. **Active branch:** `release/hazrob-0.10.2-rc2`.
3. **Production ChemCore:** the ChemCore implementation in `sohpany/SERVOC-Android`.
4. **Canonical active analytical corpus:**
   `research_data/hr069_analytical_library/`
   in `sohpany/SERVOC-Android`.
5. **HazRob/build-harness:** governance, CI/build, test, qualification and release-evidence support only. It is not a second production source tree and it is not a second canonical scientific library.
6. **sohpany/HazRob-Lab:** FROZEN / HISTORICAL REFERENCE ONLY since 2026-10-01.

## Explicitly withdrawn assumptions from the original plan

The following former statements/paths are non-authoritative and must not be used as current instructions:

- `HazRob/build-harness/library/` as canonical scientific-library owner;
- `HazRob/build-harness/library/chemcore/stable/` as current Central Stable authority;
- `HazRob/build-harness/library/research/hr069/` as the current primary research library;
- `HazRob/build-harness/library/reference-bank/` as a current reference-bank authority;
- the former `CLSYNC-0001` checkpoint as an active resume point;
- the proposed `HR069_CHEMCORE_CENTRAL_LIBRARY_SYNC_LEDGER_2026-10-02.csv` as a required current ledger.

The referenced CSV was not present at the expected active RC2 path during the 2026-10-04 governance audit and must not be recreated under the withdrawn authority model.

## Current scientific admission rule

Any new experiment, calibration, fingerprint, curve, dataset, model, transfer evidence or analytical record must enter the current canonical RC2/HR069 corpus through the active change-control process and preserve provenance, hashes, conditions, evidence tier and scientific limitations.

External or transfer evidence does not become M4 by source alone. HR069 reportable quantitative ppm remains subject to device/reference calibration and validation.

## Current governance references

Use the current records in `HazRob/build-harness`:

- `CURRENT_RELEASE_AUTHORITY.md`
- `CURRENT_EXECUTION_STATUS_0.10.2_HR069.md`
- `EXECUTION_RULES_HR069.md`
- `docs/HAZROB_MANDATORY_CHANGE_CONTROL_AND_RELEASE_POLICY_2026-10-04.md`
- `docs/HR069_CURRENT_CONTROLLED_BASELINE_APPROVAL_2026-10-04.md`
- `docs/HR069_RETROSPECTIVE_APK_AND_FIRMWARE_RELEASE_LEDGER_2026-10-04.md`

## Final instruction

**Do not execute or resume the original organization-library synchronization plan.**

This file now exists only to make the historical provenance visible and to prevent the superseded authority model from being mistaken for the current one.
