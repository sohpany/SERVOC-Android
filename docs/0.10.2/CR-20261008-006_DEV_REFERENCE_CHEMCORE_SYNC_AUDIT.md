# CR-20261008-006 — HR069-R1 developer-reference publication / ChemCore Stable update compatibility audit

**Status:** REGISTERED — AUDIT COMPLETE / REMEDIATION DESIGN PENDING / NO SCIENTIFIC PROMOTION
**Registered:** 2026-10-08
**Release line:** Android 0.10.2, engineering HR069-R1 (HR069 controlled baseline), Main ESP32 API v4, ESP32-CAM unchanged.
**Only development branch:** `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2`
**Starting audit source HEAD:** `9eeeab979c4c1bc350c789a67d7dd85cdd1b3d68`
**Currently approved controlled APK/source:** SHA `43a01d86c260353a77f1a45f808f22cf46997e06`, per `HazRob/build-harness/CURRENT_RELEASE_AUTHORITY.md`.
**Scientific source authority:** `sohpany/SERVOC-Android:release/hazrob-0.10.2-rc2/research_data/hr069_analytical_library/`.
**Rules:** `HZR-GOV-CC-001`. This audit does not authorize promotion or publication.

## Request and required behavior

User-domain Field Sessions/Sites must stay local. Only explicitly developer-initiated laboratory sessions may produce candidate reference experiments eligible for review/approval/upload. Uploading a reference must not itself confer M4 or modify a runtime ChemCore Stable library. Stable updates must be independently approved, packaged, versioned, verified and delivered over read-only HTTPS without exposing developer GitHub credentials to users. There is no requirement or permission to touch versions older than 0.10.2/HR069 or to create another development branch.

## Findings verified directly in current RC2/HR069-R1 sources

1. `TelemetryRecorder`, `MeasurementSessionRegistry`, `ExperimentQueries.recentDeveloper`, `isDeveloperReference` and `ReferenceLineageGuard.valid` segregate role by actual persisted `measurement_sessions.operator_role`; the reference lineage guard also checks `developer_*` capture origin, `ready_average`, session/capture equality, at least two telemetry samples, chemical, reference kind, ppm/feature schema and hardware profile.
2. `DeveloperExperimentsTab` now guards reference capture, display and publication entry against USER-role sessions. `FieldSessionsTab` does not contain central publication or user-library sync buttons.
3. `GitHubReferencePublisher` nevertheless hardcodes `HazRob/build-harness`, branch `main`, and `library/reference-bank/approved-experiments`/`derived-references`. This is **NOT** the current canonical scientific authority. Current governance explicitly supersedes this old ownership model. Publication cannot be accepted as valid RC2 canonical ingestion.
4. The publisher's `validateApprovedRecord` checks identity fields but does not itself independently verify persisted developer role; the UI guard exists, yet `publish` accepts a detached Record object. A production publication gate must bind to persisted valid `ReferenceLineageGuard` and source hashes. The derived publisher likewise does not check `artifactSourcesValid` itself.
5. Publisher uses local numerical experiment IDs/timestamp to name approved files. That is not a globally unique producer/session identity across devices; immutable content-addressed naming/source provenance is needed before multi-device ingestion.
6. `ReferenceBankExporter` schema `hazrob-approved-experiment/3` and the old `reference_bank/SCHEMA.md` still documents `/1`; reconcile the current release schema before packaging.
7. Android `BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL` is an empty string. `ChemCoreLibraryManager.check()` therefore fails closed without a Developer override. Ordinary users cannot yet get automatic online updates.
8. Both legacy `.github/workflows/chemcore-library-publish*.yml` are **retired/DO NOT PUBLISH** by change control. No active `developer-approved-experiment -> scientifically qualified canonical RC2 library source -> immutable Stable snapshot -> approved public read-only distribution` chain was verified.
9. The ChemCore Stable reader requires `chemcore-stable-snapshot-v1`; it reads `knowledge.response_priors` and `knowledge.model_artifacts` (and optional molecular seed). Uploading `hazrob-approved-experiment/3` or `hazrob-derived-reference-publication/1` files cannot directly update these runtime records without a deliberate schema-mapping/quality gate.
10. Updater already has important defensive controls: HTTPS, app version, sampling profile, `Stable` only, payload size+SHA256, semantic revision/count/provenance checks, staging and rollback without replacement of `servoc.db`. This supports safe updates when valid compatible snapshots become available, but **not** publisher authenticity by signature or completed end-to-end delivery.
11. Unreviewed candidate upload is NOT a final scientific publication. HR069-specific M4 evidence still requires reference-gas/device validation and the manufacturer heater protocol where applicable.

## Corrective design (non-authoritative until approved)

- P-C1: Define the **single RC2 canonical reference-ingestion path** and write permission boundary. Candidate developer uploads should enter a clearly named *non-runtime, unpromoted staging lane* within the currently authorized production/scientific repository (never historical HazRob-Lab, never an independent canonical build-harness library).
- P-C2: On device, require `DEVELOPER` persisted role, valid reference lineage, reviewed approval state, immutable session/capture IDs, sampling profile, 0.10.2/HR069 hardware contract, source hash and typed evidence tier. Fail closed before any GitHub PUT if authority or lineage is unknown.
- P-C3: Generate a content-addressed publication package and signed/approved intake review. Do not use only local experiment_id + timestamp as a global ID. No automatic M4 promotion.
- P-C4: Scientific change-control process validates submitted material and deliberately derives `knowledge`-schema models/priors from approved canonical corpus, then builds `chemcore-stable-snapshot-v1` JSON with matching manifest schema v2, schema version, sampling profile, source commit, evidence/data revisions, SHA256 and counts. Incorporate fixed regression/challenge tests.
- P-C5: Publish only approved immutable Stable snapshot + manifest to a verified read-only HTTPS distribution channel for ordinary users. No PAT in APK, no unauthorized GitHub bypass, and no inactive historical publisher workflow revival.
- P-C6: Add end-to-end acceptance tests: USER uploads denied; DEV captured/correctly averaged/approved accepted as staging only; bad capture/tier/schema/source denied; stale/historical endpoint rejected; Stable package compatible with app 0.10.2 and HR069 sample profile; wrong SHA/version/feature schema denied; rollback preserves local sites/sessions; independently tested reader incorporates approved response priors; PDF/XLSX provenance survives update.
- P-C7: Preserve old approved APK baseline until CR test, real publication trial, trial download+rollback, review and explicit approval. No auto-promoting source HEAD.

## Current blocking decisions / next implementation checkpoint

**BLOCKED:** user-facing ChemCore Stable online update is not currently provisioned; developer uploads via existing hardcoded build-harness endpoint are inconsistent with governance. It is incorrect to report complete upload/download compatibility.

**NO CHANGE executed by this audit:** no stable scientific payload or manifest published, no remote endpoint changed, no user sessions uploaded, no previous versions reintroduced. New code for re-routing publication must be reviewed against this CR and the current change-control policy before adoption.

**Resume on the same branch** with these files: `GitHubReferencePublisher.java`, `DeveloperExperimentsTab.java`, `ReferenceLineageGuard.java`, `ReferenceArtifactBuilder.java`, `ReferenceBankExporter.java`, `ChemCoreLibraryManager.java`, `ChemCoreStableKnowledgeReader.java`, `app/build.gradle` and retired workflow status.
