# CR-20261004-010 — SmartReport Analytical Expansion

**Project:** HazRob 0.10.2 / HR069 / RC2  
**Date:** 2026-10-04  
**Status:** AUTOMATED ACCEPTANCE PASSED — FIELD/VISUAL ACCEPTANCE PENDING  
**Production source:** `sohpany/SERVOC-Android`  
**Authoritative branch:** `release/hazrob-0.10.2-rc2`  
**Accepted runtime-code SHA:** `3f506ee7edd2103b07f2f89c798499342e0ebb5e`

## 1. Purpose

Develop the existing SmartReport reporting engine rather than create a parallel analytical-report engine. SmartReport shall consume ChemCore outputs through one versioned analytical contract and render professional, dynamic, evidence-aware PDF and XLSX reports.

The change addresses the prior failure mode in which a ChemCore-capable application could still produce a weak report with only one useful chart, could allow PDF/XLSX to drift from ChemCore semantics, or could expose a SmartReport user interface while still exporting through the legacy single-measurement report path.

## 2. Mandatory architecture

1. ChemCore remains the analytical authority. SmartReport does not re-infer chemical identity or independently calculate reportable ppm.
2. `SmartReportAnalyticalContract` is the explicit bridge between ChemCore and reporting.
3. PDF and XLSX must consume the same Field Session bundle, the same primary Measurement Session, and the same contract decision/series.
4. `SmartReportPrimarySelector` is the only primary-session scoring policy.
5. Any ChemCore/SmartReport contract-version mismatch must fail closed in tests/runtime binding.
6. Quantitative occupational/safety interpretation remains gated by ChemCore M4/reportable-ppm status.
7. Excel is the detailed auditable companion to the compact PDF, not a separate analytical authority.
8. A measurement mapped to a user Field Session must export through the Field Session SmartReport entry point; the legacy single-measurement report is only a fallback for records without a Field Session mapping.

## 3. PDF acceptance design

The Field Session PDF is landscape A4 (`842 × 595`) and remains compact:

- 4 pages for a single Measurement Session.
- 5 pages when cross-session comparison is applicable.
- 6 analytical charts per analytical page using a 3 × 2 grid.

### Page 1 — Executive view

- site / field-session identity;
- primary analytical measurement;
- top candidate and evidence tier;
- cross-session stability where available;
- MQ/quality summary;
- 144-material panel status;
- candidate hazard-context strip;
- explicit screening/M4 interpretation guard.

### Page 2 — Sensor and fingerprint analytics

Six distinct views:

1. Rs/R0 dynamics;
2. fingerprint view;
3. MQ signal quality;
4. fingerprint radar;
5. response kinetics;
6. persistence map.

### Page 3 — ChemCore inference

Six distinct views:

1. candidate ranking;
2. candidate scatter;
3. candidate bubble map;
4. residual/fit diagnostics;
5. persisted molecular/QSPR evidence when such evidence exists in the ChemCore decision; otherwise evidence-tier fallback;
6. decision quality.

The QSPR view is report-only visualization of persisted `evidence_observations`. It does not run a second QSPR engine inside the report and does not create evidence that ChemCore did not store.

### Page 4 — Evidence, 144 panel, environment and safety context

Six slots selected from session-supported analytics, including:

- candidate evidence matrix;
- 144-material reliability matrix;
- top session support within the 144 panel;
- screening distribution/donut;
- environmental/evidence-breadth context;
- exposure-ratio chart only when `ppmReportable == true`; otherwise a non-quantitative fallback is used.

### Page 5 — Cross-session stability and provenance

Generated when more than one Measurement Session is associated with the Field Session. It shows candidate/ranking-weight stability plus model/library/session lineage. Cross-session consistency is not presented as proof of identity.

## 4. XLSX acceptance design

`SmartFieldSessionXlsxWriter` consumes the same `SmartReportAnalyticalContract` and produces 11 auditable sheets:

1. الملخص التنفيذي
2. بيانات الجلسة
3. المرشحون والمخاطر
4. مصفوفة 144
5. الأدلة التحليلية
6. حدود التعرض
7. الجودة وعدم اليقين
8. السلسلة الزمنية
9. القياسات الفنية
10. عقد التحليل
11. المنهجية والمصدر

The analytical-evidence sheet records, where present, evidence types, QSPR observation count, QSPR in-domain count, mean support weight, and QSPR lineage/source identifiers from persisted ChemCore evidence. QSPR remains M1 molecular support and never unlocks M4/reportable ppm.

`SmartXlsxPackage` self-validates required XLSX container entries and worksheet presence before writing the final stream.

## 5. Scientific reporting gates

- Relative ChemCore ranking weights are not relabelled as calibrated probabilities.
- M1/M2/M3 evidence may be shown as screening/candidate evidence but may not unlock reportable ppm.
- Molecular/QSPR evidence is support evidence only and may not be promoted to M4 by the report layer.
- Reportable ppm requires the ChemCore M4 quantitative gate.
- Gas-safety quantitative interpretation may not pass when ppm is withheld.
- Screening-fit/estimated ppm used for diagnostic candidate context is not an occupational exposure determination.
- Out-of-domain/open-set/mixture conditions remain explicitly reportable as uncertainty/limitations rather than being converted into a false identity.

## 6. Anti-drift and export-path changes implemented

During acceptance review two release-integrity risks were found and corrected.

### 6.1 PDF/XLSX primary-session drift

`SmartFieldSessionPdfWriter` had re-selected its own primary Measurement Session with a duplicate private scoring implementation even though `FieldSessionReportExporter.resolve()` already provided the authoritative primary session and shared contract used by XLSX.

The duplicate PDF scorer was removed. PDF and XLSX are now bound to the same resolved Field Session bundle and the same `SmartReportAnalyticalContract`.

### 6.2 SmartReport UI exporting through the legacy path

The user-facing `ReportsTab` displayed SmartReport analytics but its PDF/Excel buttons still called `requestExport(...)`, the legacy single-measurement exporter. This could reproduce a weak/old report even though the new SmartReport engine itself was correct.

The routing is now fail-safe and explicit:

- `FieldSessionRepository.fieldSessionIdForMeasurement(...)` performs an exact reverse lookup from Measurement Session to Field Session.
- If the selected measurement belongs to a Field Session, `ReportsTab` calls `requestFieldSessionExport(...)`.
- `FieldSessionsTab` uses the same `requestFieldSessionExport(...)` entry point.
- `requestExport(...)` remains only as a fallback for records that do not belong to a user Field Session.

### Corrective commits

- `f442e2ba556ece74d0822cb94d1a333e63be04ee` — strengthened channel-sync tests so PDF cannot retain a second scoring implementation or independently select another primary session.
- `2d967932715f35a4cfd2eddac4c68b1f1ca0c303` — bound PDF to `b.analyticalContract`, `b.primaryMeasurementSessionId`, `contract.series()` and `contract.decision()`; removed the duplicate PDF scorer.
- `831d643e52a5c0bfda9332255a0492e4373886e9` — added SmartReport PDF/XLSX acceptance suite, including XLSX package-container validation.
- `b3f46ed39e6cac8b71b3727af66e61a0059f952f` — completed the persisted molecular/QSPR reporting evidence and XLSX QSPR-lineage acceptance state.
- `c0aa7728af2de4a064541afa27b134d009635515` — added exact Measurement Session → Field Session reverse lookup for report routing.
- `a1782dd62d0d1fef508eaaf6a1a7ea077b436965` — routed SmartReport PDF/XLSX buttons through the Field Session exporter whenever the selected measurement is Field Session-mapped.
- `3f506ee7edd2103b07f2f89c798499342e0ebb5e` — added the UI-routing acceptance lock preventing regression to the legacy export path.

## 7. Validation evidence

### 7.1 Earlier QSPR/report integration checkpoint

Code SHA `b3f46ed39e6cac8b71b3727af66e61a0059f952f` passed both relevant workflows:

- Internal validation run `37177070310`: SUCCESS.
- Diagnostic run `37177070271`: SUCCESS.

This included Android unit/contract tests, signed APK build, APK integrity/signing verification, ChemCore diagnostic challenges, MQ3 digital-twin diagnostics, and MQ3 QSPR fingerprint diagnostics.

### 7.2 Final automated acceptance of runtime-code SHA

Accepted runtime-code SHA: `3f506ee7edd2103b07f2f89c798499342e0ebb5e`.

**Internal validation workflow — run `37178457731`: SUCCESS**

- checkout exact RC2 source: PASS;
- canonical source contracts: PASS;
- canonical ChemCore Stable snapshot packaging: PASS;
- Android unit and contract tests: PASS;
- signed APK internal build: PASS;
- APK integrity and signing verification: PASS;
- workflow completed successfully.

**RC2 diagnostic workflow — run `37178457742`: SUCCESS**

- current RC2 runtime contract: PASS;
- Android unit tests and ChemCore reference challenges: PASS;
- ChemCore reference simulation diagnostic: PASS/uploaded;
- ChemCore Experiment 4 replay diagnostic: PASS/uploaded;
- MQ3 digital twin pilot diagnostic: PASS/uploaded;
- MQ3 QSPR fingerprint pilot diagnostic: PASS/uploaded;
- installable diagnostic debug APK build: PASS;
- diagnostic APK verification/fingerprinting: PASS;
- workflow completed successfully.

Therefore the source/unit/contract/build/signing/diagnostic acceptance for CR-010 is complete at SHA `3f506ee7...`.

This documentation update records that evidence only and does not alter runtime code.

## 8. Acceptance tests introduced/strengthened

- `SmartReportChannelSyncContractTest`
  - one shared analytical contract;
  - one primary selector;
  - PDF must consume the facade contract/primary session;
  - PDF may not define a private duplicate scoring implementation;
  - XLSX must consume the same contract and include contract metadata.

- `SmartReportAcceptanceContractTest`
  - landscape 4–5 page topology;
  - six-chart grid;
  - distinct sensor/fingerprint analytical views;
  - distinct ChemCore inference views;
  - dynamic persisted molecular/QSPR evidence with non-QSPR fallback;
  - QSPR must not promote reportable ppm;
  - M4 guard around quantitative exposure charting;
  - 11-sheet XLSX companion;
  - QSPR evidence/lineage columns in the auditable workbook;
  - valid inspectable XLSX ZIP/container structure.

- `SmartReportUiRoutingContractTest`
  - SmartReport UI must perform exact Field Session reverse lookup;
  - Field Session-mapped measurements must call `requestFieldSessionExport(...)`;
  - legacy `requestExport(...)` remains only as unmapped-measurement fallback;
  - FieldSessionsTab and ReportsTab converge on the same Field Session SmartReport entry point.

## 9. Automated acceptance decision

Automated acceptance is **PASSED** for runtime-code SHA `3f506ee7edd2103b07f2f89c798499342e0ebb5e`.

The following automated gates are satisfied:

1. current RC2 branch code passed synchronization and acceptance tests;
2. the user SmartReport export path no longer bypasses the Field Session SmartReport when a mapping exists;
3. PDF and XLSX remain bound to the same primary Measurement Session and analytical contract;
4. reportable ppm and gas-safety quantitative outputs remain M4-gated;
5. signed and diagnostic APK builds completed successfully.

This is an **automated engineering acceptance**, not yet a manual field/visual acceptance of rendered report files.

## 10. Remaining field/visual acceptance boundary

Automated source/unit/build validation can prove contract synchronization, page/chart topology, scientific gates, compile correctness, routing correctness and XLSX package integrity. It does **not** by itself constitute manual visual inspection of a PDF/XLSX generated from a new real robot Field Session on the target Android device.

The remaining step is therefore limited to field/visual acceptance:

1. create or use one representative HR069 user Field Session with recorded Measurement Session data;
2. export PDF from the SmartReport path;
3. export XLSX from the same Field Session;
4. verify PDF page count/layout, RTL readability, chart labels, chart density and absence of clipping;
5. verify that XLSX opens correctly and contains all 11 sheets with consistent primary-session identity and evidence lineage;
6. verify on the target Android phone and at least one desktop viewer;
7. append the generated-file evidence and observations to this CR.

Until this evidence exists, CR-010 remains **AUTOMATED ACCEPTANCE PASSED — FIELD/VISUAL ACCEPTANCE PENDING**, not fully field-accepted.

## 11. Final promotion rule

CR-010 may be promoted to **FULLY ACCEPTED** only when the field/visual acceptance evidence in Section 10 is appended to this record. No further runtime-code change is required unless that inspection exposes a real rendering/export defect.
