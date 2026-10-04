# CR-20261004-010 — SmartReport Analytical Expansion

**Project:** HazRob 0.10.2 / HR069 / RC2  
**Date:** 2026-10-04  
**Status:** IN VALIDATION  
**Production source:** `sohpany/SERVOC-Android`  
**Authoritative branch:** `release/hazrob-0.10.2-rc2`

## 1. Purpose

Develop the existing SmartReport reporting engine rather than create a parallel analytical-report engine. SmartReport shall consume ChemCore outputs through one versioned analytical contract and render professional, dynamic, evidence-aware PDF and XLSX reports.

The change addresses the prior failure mode in which a ChemCore-capable application could still produce a weak report with only one useful chart or could allow PDF/XLSX to drift from ChemCore semantics.

## 2. Mandatory architecture

1. ChemCore remains the analytical authority. SmartReport does not re-infer chemical identity or independently calculate reportable ppm.
2. `SmartReportAnalyticalContract` is the explicit bridge between ChemCore and reporting.
3. PDF and XLSX must consume the same Field Session bundle, the same primary Measurement Session, and the same contract decision/series.
4. `SmartReportPrimarySelector` is the only primary-session scoring policy.
5. Any ChemCore/SmartReport contract-version mismatch must fail closed in tests/runtime binding.
6. Quantitative occupational/safety interpretation remains gated by ChemCore M4/reportable-ppm status.
7. Excel is the detailed auditable companion to the compact PDF, not a separate analytical authority.

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
5. evidence tiers;
6. decision quality.

### Page 4 — Evidence, 144 panel, environment and safety context

Six slots selected from session-supported analytics, including:

- candidate evidence matrix;
- 144-material reliability matrix;
- top session support within the 144 panel;
- screening distribution/donut;
- environmental/evidence-breadth context;
- exposure-ratio chart only when `ppmReportable == true`; otherwise a non-quantitative fallback is used.

### Page 5 — Cross-session stability and provenance

Generated when more than one Measurement Session is associated with the Field Session. It shows candidate/posterior stability plus model/library/session lineage. Cross-session consistency is not presented as proof of identity.

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

`SmartXlsxPackage` self-validates required XLSX container entries and worksheet presence before writing the final stream.

## 5. Scientific reporting gates

- Relative ChemCore ranking weights are not relabelled as calibrated probabilities.
- M1/M2/M3 evidence may be shown as screening/candidate evidence but may not unlock reportable ppm.
- Reportable ppm requires the ChemCore M4 quantitative gate.
- Gas-safety quantitative interpretation may not pass when ppm is withheld.
- Screening-fit/estimated ppm used for diagnostic candidate context is not an occupational exposure determination.
- Out-of-domain/open-set/mixture conditions remain explicitly reportable as uncertainty/limitations rather than being converted into a false identity.

## 6. Anti-drift change implemented

During acceptance review a latent drift risk was found: `SmartFieldSessionPdfWriter` re-selected its own primary Measurement Session with a duplicate private scoring implementation even though `FieldSessionReportExporter.resolve()` already provided the authoritative primary session and shared contract used by XLSX.

This was treated as a release-integrity issue, not cosmetic cleanup.

### Corrective commits

- `f442e2ba556ece74d0822cb94d1a333e63be04ee` — strengthened channel-sync tests so PDF cannot retain a second scoring implementation or independently select another primary session.
- `2d967932715f35a4cfd2eddac4c68b1f1ca0c303` — bound PDF to `b.analyticalContract`, `b.primaryMeasurementSessionId`, `contract.series()` and `contract.decision()`; removed the duplicate PDF scorer.
- `831d643e52a5c0bfda9332255a0492e4373886e9` — added SmartReport PDF/XLSX acceptance suite, including XLSX package-container validation.

## 7. Validation evidence

Pre-change integration baseline:

- SHA `e0ed125030def90e82408a2ef8be78e574e9bf79`
- Internal validation workflow run `37175490445`
- canonical source contracts: PASS
- ChemCore stable snapshot packaging: PASS
- Android unit/contract tests: PASS
- signed APK internal build: PASS
- APK signature/integrity verification: PASS

Current CR-010 head at this document creation is `831d643e52a5c0bfda9332255a0492e4373886e9`; new CI acceptance runs are pending and must pass before CR-010 is marked ACCEPTED.

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
  - M4 guard around quantitative exposure charting;
  - 11-sheet XLSX companion;
  - valid inspectable XLSX ZIP/container structure.

## 9. Remaining acceptance boundary

Automated source/unit/build validation can prove contract synchronization, page/chart topology, scientific gates, compile correctness and XLSX package integrity. It does **not** by itself constitute manual visual inspection of a PDF/XLSX generated from a new real robot Field Session on the target Android device.

Therefore the final field acceptance step remains: generate one representative Field Session PDF and XLSX from real/representative HR069 session data, inspect readability/RTL/chart density, and confirm the exported files open correctly on the target phone/desktop. This step must be recorded when performed; it must not be implied before evidence exists.

## 10. Promotion rule

CR-010 may be marked **ACCEPTED** only when:

1. current branch CI passes with the synchronization and acceptance tests;
2. no new report path bypasses `SmartReportAnalyticalContract`;
3. PDF and XLSX remain bound to the same primary Measurement Session;
4. reportable ppm and gas-safety quantitative outputs remain M4-gated;
5. any later field/visual acceptance evidence is appended to this record rather than kept only in chat.
