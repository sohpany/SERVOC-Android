# HazRob 0.10.2 / HR069 — Stage 28 Unified Evidence-Weighting Policy

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Scope: Analytical Confidence Index evidence fusion only. Candidate posterior ranking remains a separate quantity.

## Objective
Prevent correlated derivatives from appearing as independent scientific confirmations while retaining useful positive corroboration.

## Core rule
ChemCore confidence is now de-duplicated on two levels:

1. **Immediate evidence identity** (`independenceGroup`) — prevents repeated channels/copies of the same immediate model/source from being counted repeatedly.
2. **Scientific lineage** (`ChemCoreEvidenceLineagePolicy.confidenceLineageKey`) — prevents different algorithms derived from the same upstream information from being counted as independent proofs.

## Full-strength lineages
The main confidence score can receive independent groups from:

- distinct direct external reference sources;
- independent HR069 calibration campaigns;
- independent HR069 experimental fingerprint campaigns;
- external reference fingerprints when they are not declared model-derived;
- one accepted-reference-derived model lineage when no direct reference evidence for the candidate is present.

Distinct direct reference sources retain their own source-specific lineage keys.

## Accepted-reference-derived models
All current QSPR model-transfer evidence and model-derived `ResearchIdentificationFingerprintRegistry` fingerprints share:

`UPSTREAM:ACCEPTED_REFERENCE_RESPONSE_LIBRARY`

Therefore:

- multiple QSPR sensor models do not create multiple independent confidence groups;
- QSPR + model-derived fingerprint do not create two independent confidence groups;
- when direct reference evidence is present for the candidate, the derived-library lineage does not create an additional independent proof.

## Correlated-derivative corroboration
A useful correlated derivative is retained as positive corroboration instead of being discarded.

The combined contribution from reference-library-derived evidence is capped at **3.0 percentage points** in the Analytical Confidence Index when direct reference evidence already exists.

This bonus is not counted in `uniqueEvidenceGroups`.

Examples under the current policy:

- one direct M2 reference alone: 45 base points before sensor breadth;
- direct M2 + compatible QSPR derivative: QSPR adds at most 1.5 corroborative points rather than a second 20-point M1 proof;
- direct M2 + compatible model-derived M3 fingerprint: the fingerprint adds at most 3 corroborative points rather than a second independent reference group.

## Weak / degraded evidence
Out-of-domain, poor-fit or corroborative measurements retain the existing quality multipliers and global corroboration cap. A degraded copy sharing the same immediate `independenceGroup` as a full-strength observation is suppressed and cannot re-enter through a different lineage label.

## Sensor breadth
Sensor breadth remains reported separately from evidentiary independence. Multiple responsive MQ channels may increase breadth without manufacturing extra source lineages.

## Semantics
The Analytical Confidence Index remains an operational score, not a calibrated probability. Candidate posterior/model weights remain separate and must not be presented as confidence percentages.

## Regression expectations
Tests now require:

- multiple QSPR groups from the same upstream model-transfer library to collapse to one M1 lineage;
- QSPR with a direct reference to become correlated corroboration, not an independent QSPR group;
- model-derived reference fingerprint with a direct reference to become correlated corroboration;
- repeated weak copies of a strong immediate evidence group not to double count;
- independent direct sources to retain independent source lineages.

## Status
Stage 28: IMPLEMENTED. Final COMPLETE status is recorded after the RC2 unit-test workflow passes the lineage-aware fusion regression suite.
