# 0.10.2 Selective Migration Audit

Baseline: HR067 / commit `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.

## Reviewed newer scientific lineage
The current repository tip was reviewed as a possible source of newer scientific data. It is a divergent lineage, not a linear successor of HR067.

### Already represented in HR067
The nonlinear MQDataScience ports, fidelity work, manual calibration lineage and related scientific engine work predate HR067 in its ancestry. They are therefore retained through the HR067 baseline rather than copied again.

### Scientific seed refresh
A later divergent tip contains regenerated NIOSH/OSHA seed snapshots with the same principal row counts:
- NIOSH chemical identities: 652
- NIOSH IDLH: 402
- OSHA Z-1: 638 records / 432 substances

These files have different generated timestamps/hashes. They are NOT copied blindly into 0.10.2. Dataset refresh requires provenance/hash validation and importer compatibility testing first.

### Database schema
The later divergent `ExperimentsDb.java` is NOT a drop-in replacement for HR067. HR067 contains schema/features that must not be lost, including the reference-artifact lineage. Database changes will be additive/migrated, never replaced wholesale.

## HOLD / do not import wholesale
- Main ESP32 WAN STA/AP+STA/NAPT changes.
- Divergent full `ExperimentsDb.java`.
- Regenerated scientific assets until content/provenance validation passes.
- Any older hardware-profile/manual-relay changes inconsistent with HR067 hardware.

## Release invariant
No migration may remove HR067 analysis, ChemCore, reports, calibration, fingerprints, scientific provenance, stored experiments, reference artifacts, Direct control, local camera, E-stop, or the 500 ms command watchdog.
