# HazRob release and analytical authority — 2026-10-01

## Decision status

**ACTIVE / CANONICAL — revised after HazRob-Lab freeze**

This record establishes the authoritative HazRob release and analytical baseline.

## 1. Authoritative release

The latest and only active release line is:

- RC: **RC2**
- Android: **0.10.2**
- Engineering revision: **HR069**
- versionCode: **26**
- source repository: `sohpany/SERVOC-Android`
- source branch: `release/hazrob-0.10.2-rc2`
- RC2 application baseline SHA before authority-document commits: `22820dc365ef634e9b9e5e0d685ffdb53dcbbe35`
- database schema: **9**
- Main ESP32 API: **v4 / firmware 0.10.2**

All production, analytical, calibration, report, APK and firmware work must be reconciled against this line unless a later release is explicitly approved.

## 2. HR070 withdrawal

**0.10.3 / HR070 is CANCELLED, WITHDRAWN and NON-AUTHORITATIVE.**

Its branch/artifacts are historical/recovery material only. HR070 must not be used as a release source, merge authority, ChemCore authority, calibration authority, analytical-library authority or report-methodology authority.

Any useful HR070-only change requires explicit review and reintroduction into the active RC2/HR069 line or a later formally approved successor.

## 3. Production analytical authority

The production analytical inference engine is the **ChemCore implementation inside RC2/HR069**:

`app/src/main/java/com/servoc/app/chemcore/`

This includes controlled inference, open-set/OOD rejection, evidence fusion, accepted-fingerprint identity, reference-response fitting, mixture/interference guards, session dynamics, confidence handling and M4 quantitative gating.

## 4. Central analytical library — single write authority

The canonical HazRob analytical evidence corpus is:

`research_data/hr069_analytical_library/`

inside:

`sohpany/SERVOC-Android : release/hazrob-0.10.2-rc2`

**This is the single active scientific-library write authority.**

All new experimental data, fingerprints, response curves, transfer evidence, reference-gas results, model evidence and scientific-source additions are admitted here with provenance and scientific qualification controls.

The active corpus currently includes the major evidence layers documented in `HR069_CENTRAL_LIBRARY_AND_GITHUB_DISTRIBUTION_2026-10-01.md`, including 43 MQ research curves, 76 MQDataScience models, 13 numeric fingerprint families, 59 Food Freshness headspace fingerprints, Multimodal/UCI/RoboFusion transfer evidence and the newer Methanol/Ethylene/1-Propanol/Kononov/same-sensor evidence sets.

## 5. HazRob-Lab freeze

`sohpany/HazRob-Lab` is **FROZEN effective 2026-10-01**.

It is retained as historical/scientific provenance only.

After the freeze:

- no new experiments are written there;
- no new calibration data are written there;
- no new fingerprints/curves/models are staged there;
- no ChemCore publication is sourced there;
- no user update is distributed from there.

Existing data remain preserved and are not deleted.

## 6. Build harness role

`HazRob/build-harness` is **qualification/build evidence only**.

It may check out an explicitly pinned RC2 source SHA, run audits/tests/builds and preserve evidence. It is not:

- the application source tree;
- the central scientific library;
- the experiment-data destination;
- the end-user distribution origin.

## 7. End-user library distribution

HazRob applications will receive qualified scientific-library updates **directly from GitHub**.

The distribution channel must be:

- public and read-only for ordinary users;
- manifest-driven;
- hash verified;
- cryptographically signed/verified before activation;
- compatible with app/schema/sampling-profile requirements;
- atomic with rollback;
- fail-closed on verification failure;
- free of GitHub credentials/tokens in the APK.

A private raw GitHub URL must not be used as the user endpoint because ordinary users would require authentication.

Until the public GitHub publication endpoint is provisioned, RC2 remains intentionally fail-closed rather than falling back to Render or another scientific-library host.

## 8. Scientific rules preserved

1. External/source-backed evidence remains non-M4 unless separately qualified.
2. External evidence never self-promotes to `M4_HAZROB_VALIDATED`.
3. Accepted fingerprint identity may support M3 only after open-set/OOD and required separation/channel gates.
4. Fingerprint identity does not create reportable occupational/safety PPM.
5. Source-backed PPM relations remain limited to their declared domains and provisional/reference use unless M4 exists.
6. Reportable quantitative PPM requires HR069 device/reference M4 validation and existing ChemCore gates.
7. MQ7/MQ9 manufacturer-curve use remains fail-closed while their required heater cycle is not physically valid.
8. Missing sensor channels remain missing; no source-to-HR069 channel is fabricated.
9. Mixture/headspace/smoke/operational classes are not silently converted to pure-chemical identities.
10. Duplicate/mirror datasets do not count as independent evidence.

## 9. Authority order

When records conflict, use this order:

1. **Release identity:** RC2 / 0.10.2 / HR069.
2. **Production analytical runtime:** RC2 ChemCore.
3. **Central analytical library:** RC2 `research_data/hr069_analytical_library/`.
4. **Qualified user distribution:** public read-only GitHub publication channel.
5. **Build/qualification evidence:** `HazRob/build-harness`.
6. **Historical provenance:** frozen `sohpany/HazRob-Lab` and withdrawn branches including HR070.

## 10. Change control

No dataset, fingerprint, response curve, model or calibration may replace the baseline silently. Every addition must preserve source/provenance, content hash where applicable, analyte/class mapping, sensor/channel mapping, concentration/environment domain, evidence tier, M4 status, runtime eligibility, validation result and revision/date.

The detailed central-library and GitHub-distribution architecture is authoritative in:

`docs/0.10.2/HR069_CENTRAL_LIBRARY_AND_GITHUB_DISTRIBUTION_2026-10-01.md`
