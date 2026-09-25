# HazRob 0.10.2 — Master Implementation Plan

Status: APPROVED DEVELOPMENT PLAN
Branch: `release/hazrob-0.10.2`
Scope: 0.10.2 only. No version bump is implied by this plan.
Baseline rule: preserve Direct-first operation, safety interlocks, scientific provenance, calibration/reference data, and existing ChemCore evidence gates.

## 1. Architectural boundary

HazRob has two product domains with separate workflows.

### Developer domain
Developer-only activities:
- R0 and calibration workflows, including controlled M4 work.
- Laboratory experiments and controlled captures.
- Chemical definition, fingerprint creation, validation and approval.
- Scientific/reference-library development and publication.
- Robot configuration and Direct/Remote connection configuration.

Developer experiments and calibration records MUST NOT appear as user sites or field sessions.

### User domain
User-facing activities:
- Sites.
- Field sessions.
- Site/session library management.
- Live measurement and analysis.
- Chemical findings, tracking and exposure/property review.
- Session reports and site aggregate reports.
- Analysis-library updates from the approved Stable publication channel.

### Scientific dependency
Knowledge flows one way for normal operation:

Developer validation -> approved scientific library -> analysis engine -> user field operation.

A user field result MUST NOT automatically become a calibration point, approved fingerprint, reference artifact, or developer-library record.

## 2. Data model

Do not repurpose `measurement_sessions` as the user-visible field-session entity. It remains the technical/scientific acquisition record.

Add a user-domain layer:
- `sites(site_id, name, notes, created_ts, updated_ts)`
- `field_sessions(field_session_id, site_id, name, started_ts, ended_ts, status, created_ts, updated_ts)`
- `field_session_measurements(field_session_id, measurement_session_id, linked_ts)`

Rules:
- IDs are immutable; display names are mutable.
- One active user field session at a time.
- A field session may contain multiple technical `measurement_sessions`.
- Developer technical sessions are never attached to user field sessions.
- ChemCore decisions remain bound to exact technical session IDs.
- Field/session reports aggregate only technical sessions explicitly mapped to that field session.
- Deleting user sites/sessions must not delete calibration curves, approved fingerprints/reference artifacts, chemical catalog, exposure library, scientific sources, or developer experiments.
- Referential deletion must be explicit and transactional unless foreign-key enforcement is verified at runtime.

Database migration must be additive and non-destructive.

## 3. Persistent HazRob User Library

User field data must not depend exclusively on app-private storage.

Implement a user-selected persistent HazRob library using Android Storage Access Framework (SAF) with persistable URI permission.

Logical contents:
- library manifest/schema version;
- sites and field-session metadata;
- source/raw field-session data required to reconstruct reports;
- analytical results and provenance;
- optional generated PDF/XLSX/ZIP artifacts.

Rules:
- Internal technical IDs, not display names, define storage identity.
- Reinstall flow: Open existing HazRob Library / Create new HazRob Library.
- App update must preserve compatibility through explicit library schema migration.
- PDF/XLSX are derived artifacts; source session data remains authoritative.
- Backup/restore is supported at library level.
- Every stored analysis records app version, analysis-library version/hash, chemical catalog/version, fingerprint/model provenance and exposure-library provenance.
- Reanalysis with a newer library creates a new revision and never silently rewrites the historical result.

## 4. User sites and sessions UI

Add a user-facing Sites/Sessions surface independent of Developer.

Required operations:
- create/open/rename/delete site;
- create/start/end/open/rename/delete field session;
- browse previous sites and sessions through "مكتبة المواقع والجلسات";
- generate session PDF/XLSX;
- generate site aggregate report;
- generate ZIP containing aggregate and per-session reports;
- open existing external HazRob library;
- backup/restore library.

No responsible-user/person field is required.

## 5. User analysis-library updates

Expose a normal-user updater in Settings.

Existing ChemCore update infrastructure is authoritative:
- `ChemCoreLibraryManager`
- `ChemCoreLibraryUpdateClient`
- `ChemCoreLibraryVerifier`
- `ChemCoreLibraryStore`

User UI must show:
- installed analysis-library version;
- last update/check state;
- compatibility state;
- Check for updates action;
- install action only for compatible Stable snapshots.

Preserve:
- signed/verified package path;
- SHA-256 verification;
- Stable-only normal updater;
- compatibility gate;
- rollback/fail-safe behavior;
- no update installation during active measurement/analysis.

Developer manifest/source overrides remain developer-only.

## 6. Analysis integration

Field sessions consume approved knowledge but do not mutate it.

Required:
- exact technical-session -> field-session binding;
- aggregate ChemCore decisions across mapped technical sessions;
- preserve M0-M4 and quantitative/reportability gates;
- never infer PPM from identity evidence alone;
- preserve approved single-specialist-sensor policy when implemented;
- distinguish scientific evidence/coverage from field finding strength.

Before changing colors, inspect the exact existing four-level color implementation and preserve its semantics. Do not relabel `ChemicalCoverage` evidence availability as detection strength.

## 7. Session report contract

Each user field session gets an independent report.

PDF target:
- A4 landscape;
- approximately 7-8 mm margins;
- target 3 pages when data density permits.

Page 1:
- site/session identity and time;
- acquisition summary;
- key analytical findings;
- recognized/candidate compound matrix.

Page 2:
- chemical-analysis charts;
- ranking/fingerprint/comparison;
- ChemCore evidence state.

Page 3:
- temperature/humidity/pressure and physical/safety context;
- alerts/events;
- exposure interpretation;
- concise scientific provenance.

Excel retains detailed/raw tables and analytical sheets.

Reports must identify exact field session and the exact analysis/library versions used.

## 8. Site aggregate report

Do not create a 144-compound-wide matrix.

Layout:
- field sessions are horizontal columns;
- under each session list only HazRob-144 compounds actually observed/reported for that session;
- preserve the exact existing four color-level semantics after source verification;
- display scientific state separately: reportable PPM, ID, or Candidate;
- repeated compound within one field session appears once using the strongest valid session state;
- sort by existing level then evidence/confidence;
- paginate session columns rather than shrinking text excessively.

Header includes site name, date range, session count, total measurement duration, alerts and distinct observed compounds.
Footer includes legend and analysis/library version.

## 9. ZIP export

On-demand site package:
- site aggregate PDF;
- aggregate XLSX if implemented;
- per-session PDF;
- per-session XLSX.

Generated reports are replaceable artifacts; source session data is retained independently.

## 10. XLSX remediation

Current `XlsxReportWriter.validate()` validates ZIP membership only and is not sufficient proof of workbook validity.

Required:
- validate worksheet XML and OOXML ordering;
- sanitize invalid XML 1.0 characters;
- correct worksheet child ordering where necessary;
- generate a real workbook in regression tests;
- reopen/parse the generated workbook with an independent OOXML-capable test path;
- retain all required analytical/provenance sheets.

Excel corruption is not considered fixed until a generated workbook passes reopen/parse validation.

## 11. Control and alert work retained in 0.10.2

Complete previously approved control work:
- landscape Control tab uses full operational screen;
- orientation transition sends/maintains zero drive until controls are safely rebuilt;
- ultrasonic warning at <=10 cm;
- forward stop at <=5 cm;
- cliff IR forward interlock;
- reverse/pivot remain available under those hazards;
- video overlays: "خطر الاصطدام" and "خطر السقوط";
- tight flame bounding box;
- small blinking live gas finding overlay;
- correct Direct/Remote drive status label;
- audible phone hazard alert verification/fallback;
- verify joystick extreme-left/right pivot through the complete UI -> mixer -> logical side -> GPIO mapping.

Do not alter DriveMixer merely because of the reported symptom; its current pure-pivot unit contract must be traced downstream first.

## 12. UI audit

Audit every visible user control:
UI control -> handler -> backend/data action -> success/error state.

Required cleanup:
- remove stale HR/0.10.1 visible identity from active 0.10.2 UI;
- remove non-executive instructional clutter where it adds no operational value;
- replace long static lists with functional views/actions;
- keep compact safety/scientific caveats where needed;
- add chemical properties/exposure dialog beside tracking where candidate UI supports it.

## 13. Implementation sequence

A. Architecture and contracts
B. Database migration and field-session repositories
C. Persistent external HazRob User Library
D. Sites/Sessions UI and lifecycle
E. Settings analysis-library updater
F. Field-session/ChemCore integration
G. Finding/evidence semantics verification
H. Session PDF/XLSX
I. Site aggregate report and ZIP
J. XLSX remediation and independent reopen test
K. Control/alerts/landscape/audio cleanup
L. Full UI functional audit
M. Cross-system integration qualification
N. Release build/deployment verification
O. Physical robot acceptance

Each stage uses focused commits and reruns affected tests before proceeding.

## 14. Automated/internal acceptance

Programmatically verify:
- DB upgrade is non-destructive;
- developer records never enter user site/session queries;
- user deletion cannot delete scientific reference/calibration data;
- exact technical-session binding;
- no nearest-time fallback for new field-session reports;
- Stable updater compatibility/signature/hash/failure rollback;
- historical analysis version preservation;
- ChemCore reportability gates;
- site aggregation isolation;
- PDF generation;
- XLSX generation + reopen/parse;
- ZIP contents;
- Direct/Remote protocol contracts;
- drive mixer/pivot contract;
- collision/cliff interlock contract;
- app lifecycle zero-drive behavior;
- missing/invalid data behavior.

## 15. Physical acceptance gates

Hardware-dependent checks remain explicitly separate:
1. Direct control with no WAN.
2. Direct camera.
3. 500 ms command-loss stop.
4. E-stop.
5. Remote control via WAN/hotspot.
6. MQTT reconnect.
7. Remote CAM WSS video.
8. Direct/Remote switching.
9. Hotspot loss/recovery.
10. Ultrasonic 10 cm warning and 5 cm forward stop.
11. IR cliff warning and forward stop.
12. Reverse/pivot availability under collision/cliff interlock.
13. Exact joystick extreme-left/right physical wheel behavior.
14. Audible phone warning on target handset.
15. BME280 physical T/RH/P acquisition.
16. Generated Excel opens on target phone/desktop application.

## 16. Release discipline

- Development remains on `release/hazrob-0.10.2`.
- No immutable `v0.10.2` tag before physical acceptance.
- No silent migration of developer data into user data.
- No destructive migration.
- No scientific-library mutation from ordinary user field sessions.
- Direct operation without WAN remains mandatory.
