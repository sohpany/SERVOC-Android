# HazRob 0.10.2 / HR069 — ChemCore Knowledge-Base Governance

Date: 2026-10-02
Branch: `release/hazrob-0.10.2-rc2`
Status: ACTIVE GOVERNANCE RULE

## Purpose

ChemCore development is knowledge-first and experimentally revisable. The 144-entry analytical knowledge base is built now from the best available chemical identity, manufacturer evidence, literature/research relations, existing fingerprints, PubChem/QSPR descriptors and current HR069 software artifacts. Controlled HR069 experiments subsequently validate, correct, promote, demote or replace individual records and model relations.

The knowledge base is therefore neither a frozen truth table nor a speculative runtime rule set. It is a versioned evidence graph used to construct and test the analytical engine.

## Core rule

`Build knowledge -> derive analytical hypotheses/routes -> test experimentally -> revise evidence/model -> revalidate regressions -> publish new knowledge/model version.`

A knowledge record may be useful before experimental validation provided its evidence tier and limitations remain explicit. Lack of M4 evidence does not block cataloguing a manufacturer/literature relation; it only blocks promotion to reportable quantitative evidence.

## Record immutability and revision

Every material-level or sensor-analyte value must carry:
- stable catalogue key/index;
- value and controlled vocabulary;
- source/provenance identifier;
- evidence class/tier;
- review state;
- applicability/domain fields where relevant;
- revision/version identifier;
- supersedes/superseded-by relation when changed;
- experimental validation state;
- rationale for promotion, demotion or replacement.

Experimental correction never silently overwrites history. A new accepted relation supersedes the prior relation while retaining its provenance for audit and regression comparison.

## Evidence states

- `KNOWLEDGE_PRIOR` — chemically/source-supported information useful for routing but not HR069 validated.
- `SOURCE_BACKED_M1_M2` — PubChem/QSPR or manufacturer/literature/research response evidence with its declared tier.
- `HR069_OBSERVED` — repeatable HR069 observation not yet independently validated for identity/quantitation.
- `HR069_M3_VALIDATED` — independently validated identity evidence.
- `HR069_M4_VALIDATED` — independently validated quantitative evidence with domain and uncertainty.
- `CONFLICTED` — credible sources/experiments disagree; both are retained pending resolution.
- `REJECTED_OR_SUPERSEDED` — historical value kept for audit but excluded from active inference.

## Flexibility rule

The 38-stage plan is a control structure, not a rigid waterfall. Later stages may reveal a correction needed in an earlier knowledge layer. Such corrections are allowed when:
1. the affected records are versioned;
2. the reason and evidence are documented;
3. dependent artifacts are invalidated/recomputed where necessary;
4. regression tests are rerun for affected chemical families;
5. progress counts are updated rather than silently preserved.

Stages may therefore iterate, but no stage may silently change definitions used by another stage.

## Runtime promotion rule

Knowledge-base construction and runtime inference changes are separated. A newly catalogued relation may inform analysis design immediately, but runtime promotion must declare:
- intended analytical role;
- evidence lineage/independence group;
- valid domain;
- conflict/interference behavior;
- identity ceiling;
- regression impact.

No experiment is tuned solely to make one known challenge pass. Blind/held-out cases are used to decide whether a change generalizes.

## Experimental feedback loop

For each controlled experiment:
1. preserve raw session data and provenance;
2. run the current knowledge/model version without revealing ground truth where possible;
3. record SIGNAL/AFFINITY/CANDIDATE/IDENTIFIED/QUANTIFIED and rejected hypotheses;
4. reveal ground truth only after the blind result is locked;
5. attribute error to sensor suitability, source curve, transfer, fingerprint, temporal model, QSPR, correlation, OOD, mixture/interference or data quality;
6. modify the smallest justified knowledge/model component;
7. rerun family-level and global regression suites;
8. publish a new version only when the change improves evidence-consistent performance without unacceptable regressions.

## Final objective

The finished system must use the knowledge base to choose the appropriate analytical path for each material rather than applying one universal classifier. It should preserve uncertainty, identify chemical families when exact identity is not defensible, exploit specialist sensors, broad-array patterns, curves, fingerprints, temporal behavior and molecular priors without double counting, reject unsupported/OOD hypotheses, retain rejected live signals for audit, recognize mixtures/interferences, and produce reportable ppm only under HR069 M4 validation.
