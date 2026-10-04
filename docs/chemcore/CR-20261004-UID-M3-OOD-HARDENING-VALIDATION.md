# Validation record — CR-20261004 ChemCore UID / M3 / OOD hardening

## Change under validation

- Base branch: `release/hazrob-0.10.2-rc2`
- Base SHA: `43a01d86c260353a77f1a45f808f22cf46997e06`
- Change branch: `fix/chemcore-uid-m3-ood-hardening`
- Pull request: `#7`
- Scope: canonical UID consistency, M3 fingerprint identity hardening, Open-Set/OOD provenance, regression tests, and focused CI.

## Completed CI evidence

GitHub Actions workflow `ChemCore Hardening PR Validation` completed successfully on the proposed PR merge for code head `f82103f7927e3da0dbaf1a40612c3e342f9be68c`.

- Workflow run: `37220918671`
- Job: `chemcore-unit-and-contract`
- Job id: `111490909085`
- Conclusion: `success`
- Proposed merge SHA tested: `b4d4d6e1ca5907ab8a06c8219bb62a29bf1e6781`

### Source audits

All focused analytical audits passed:

1. `tools/audit_0104_chemcore_library.py` — PASS
2. `tools/audit_0105_analytical_engines.py` — PASS
3. `tools/audit_0106_confidence_fusion.py` — PASS

The P6 audit additionally confirmed that confidence remains de-duplicated by immediate independence group and upstream scientific lineage; degraded/correlated evidence can only add capped corroboration; posterior ranking and PPM remain separate semantics.

### JVM/unit/contract tests

Command:

```text
gradle --no-daemon :app:testDebugUnitTest
```

Result: `BUILD SUCCESSFUL`.

The build executed the full debug JVM test task after compiling the app and unit-test sources. This includes the newly added/extended hardening regression tests together with the pre-existing ChemCore contract tests.

Hardening-specific test coverage includes:

- database/QSPR UID overrides historical `legacy_id + 1` when authoritative;
- fallback is retained only for legacy-only candidates;
- conflicting canonical UIDs for the same normalized chemical label fail closed;
- invalid legacy IDs fail closed;
- accepted fingerprint evidence UID equals promoted M3 result UID;
- fingerprint M3 remains non-quantitative and has no reportable PPM;
- empirical OOD gates are labeled as empirical training-quantile thresholds;
- independently supplied validated thresholds retain that provenance;
- low feature coverage fails closed;
- external runtime/live OOD policy is not mislabeled as independent validation.

### Android compilation

Command:

```text
gradle --no-daemon :app:assembleDebug
```

Result: `BUILD SUCCESSFUL`.

The debug APK compiled and packaged successfully. The CI restored the existing prototype signing material only for build verification; no release-signing or production-release claim is made by this test.

## Scientific acceptance checks

The tested change preserves the following safety properties:

- missing or rejected Open-Set/OOD state cannot be overridden by an exact fingerprint match;
- a single-channel fingerprint cannot create a named M3 identity;
- M3 remains `IDENTIFIED / M3_ARRAY_CONSISTENT` and cannot create reportable PPM;
- M4 remains the separate HR069 device/reference-gas quantitative gate;
- ambiguous UID mapping fails closed;
- fingerprint evidence and promoted result use the same canonical UID policy;
- QSPR remains M1 transfer support and cannot independently create M3/M4;
- no numerical OOD threshold was changed or newly claimed as validated.

## Residual limitations / follow-up validation

This software verification does **not** replace the required experimental validation campaigns. The following remain open scientific tasks:

1. replace the residual compatibility `legacy_id + 1` fallback with explicit database-backed UID resolution at all production entry points;
2. validate OOD thresholds using held-out known/unknown HR069 sessions and publish rejection/acceptance metrics;
3. validate fingerprint M3 thresholds using controlled pure-gas and interference datasets;
4. validate mixture/interference policies with a dedicated mixture dataset;
5. preserve M4 as unavailable for any chemical/channel until HR069 reference-gas calibration requirements are actually satisfied.

## CI authority

The pull-request workflow is configured to rerun on application, tooling, ChemCore documentation, Gradle, or workflow changes. The GitHub Actions check attached to the **current PR head** is the authoritative merge gate; this document records the completed code validation evidence and does not override a later failed check.
