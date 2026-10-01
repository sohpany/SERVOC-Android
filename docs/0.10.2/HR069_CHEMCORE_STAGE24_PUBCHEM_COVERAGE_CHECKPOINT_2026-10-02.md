# HazRob 0.10.2 / HR069 — ChemCore Stage 24 PubChem Molecular-Coverage Checkpoint

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Status: **IN PROGRESS**
Scope: molecular/PubChem coverage required for the 144-entry analytical catalogue.

## Verified seed-level facts

The embedded ChemCore molecular seed manifest declares:

- source: PubChem (NIH/NLM/NCBI)
- schema: `chemcore-molecular-seed-manifest-v1`
- records: 652
- resolved: 615
- unresolved: 37
- generation error count: 0
- generated UTC: 2026-09-09T02:03:09Z

The seed includes structural/text properties and numeric molecular descriptors such as molecular weight, XLogP, TPSA, complexity, H-bond donor/acceptor counts, rotatable bonds, exact/monoisotopic mass, heavy atom count, charge and stereochemical counts.

## Import semantics

`ChemCoreMolecularSeedImporter` imports only records whose seed status is `RESOLVED`, matches them to the local chemicals table by CAS, and stores PubChem evidence with authority `NIH/NLM/NCBI` and evidence tier `MOLECULAR_REFERENCE`.

The importer explicitly documents that molecular/structural knowledge **does not imply MQ detectability or calibration**.

Source precedence is:

1. activated cryptographically verified ChemCore Stable payload containing a supported molecular seed;
2. immutable seed embedded in the APK.

## Descriptor contract

Default QSPR eligibility uses the eight CORE descriptors from `chemcore-molecular-descriptors/1`:

- MolecularWeight
- TPSA
- HBondDonorCount
- HBondAcceptorCount
- RotatableBondCount
- HeavyAtomCount
- Charge
- Complexity

Optional descriptors must not silently remove compounds from the default complete matrix.

A molecular record and a QSPR-complete record are therefore distinct states.

## Unresolved-curation policy

The unresolved curation set explicitly prevents mixtures, materials, occupational categories and isomer groups from being coerced into one molecular descriptor vector. Curation alone does not make such records QSPR-eligible.

This is consistent with the HazRob 144-entry object-type rule: mixture/aggregate/source objects must remain separate from pure molecular analytes.

## Why Stage 24 is not yet closed

The 652-record manifest is broader than the frozen 144-entry analytical catalogue. A seed-wide resolved count (615/652) cannot be substituted for the required **144/144 catalogue join**.

The exact Stage 24 closeout requires a database-derived or deterministic CAS join over the imported local `chemicals` table, reporting for every one of the 144 entries:

- PubChem structure present / absent;
- active descriptor set present / absent;
- CORE descriptor completeness;
- optional descriptor completeness;
- molecular-QSPR eligibility;
- object-type exclusion reason when applicable.

`MolecularCoverageReport` already provides database-derived counts for structures, active descriptor sets, and property coverage; the Stage 24 closeout should use that imported database state rather than infer coverage from the large seed manifest.

## Current scientific decision

Until the exact 144-entry join is recorded:

- PubChem coverage is considered **seed available, catalogue coverage pending exact audit**;
- no missing catalogue record is silently treated as resolved;
- no mixture/aggregate is coerced to one molecule;
- no PubChem record is allowed to imply sensor detectability;
- no QSPR candidate is accepted solely because a CID/structure exists.

## Stage accounting checkpoint

- Required denominator: 144 catalogue entries.
- Seed-level records verified: 652 total / 615 resolved / 37 unresolved.
- Exact 144 catalogue structure coverage: pending database/CAS join.
- Exact 144 CORE-complete QSPR coverage: pending database/CAS join.
- Stage state: **IN PROGRESS**, not falsely marked complete.

## Next action

Produce the catalogue-specific molecular coverage export from the imported ChemCore database, then close Stage 24. Stage 25 will subsequently evaluate QSPR applicability domain rather than assuming descriptor completeness alone is sufficient.