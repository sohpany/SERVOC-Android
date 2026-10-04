# CR-20261004 — ChemCore UID / M3 / OOD hardening

## Scope

This change set hardens three analytical-system concerns identified during the 0.10.2 / HR069 system review:

1. **Chemical identity namespace consistency** between the zero-based legacy catalogue (`legacy_id`), ChemCore result `chemical_uid`, and database-backed QSPR candidates.
2. **M3 fingerprint identity promotion** so source-backed multi-channel fingerprints continue to promote only through the existing conservative identity gates and never create M4/PPM authority.
3. **Open-set / OOD provenance** so reports and audit traces can distinguish an empirical training-quantile threshold from an independently validated threshold or an explicit runtime policy.

Base revision: `release/hazrob-0.10.2-rc2` at `43a01d86c260353a77f1a45f808f22cf46997e06`.

---

## 1. Problem statement: `chemical_uid` ambiguity

### Before

Two identity paths used different assumptions:

- `ChemCoreLegacyCandidateRegistry` and `AcceptedFingerprintIdentityEngine` converted a zero-based legacy catalogue index to a UID with `legacy_id + 1`.
- `ChemCoreQsprRuntimeRegistry` builds candidates from the live `chemicals.uid` database value.

Fresh HazRob databases currently seed the original 144 rows in the same numeric order, so the values normally coincide. However, they are **different namespaces** and the database schema does not define `uid = legacy_id + 1` as a permanent invariant. A future migration/import/reseed could therefore produce two candidate objects for the same chemical with different UIDs.

### Change

`ChemCoreLegacyCandidateRegistry` is now the single compatibility bridge:

- If a database-backed QSPR candidate exists for the same normalized catalogue label, its `chemical_uid` is authoritative.
- The historical `legacy_id + 1` mapping is retained only when no database-backed QSPR candidate for that compound exists.
- If two different QSPR UIDs resolve to the same normalized catalogue label, resolution returns `-1` and the direct/reference candidate fails closed instead of creating a duplicate identity.
- `AcceptedFingerprintIdentityEngine` no longer performs its own `chemicalId + 1` conversion. It calls the shared UID bridge.
- Fingerprint candidate merging suppresses a stale same-label/different-UID candidate so a single result cannot report the same chemical twice under two IDs.

Policy identifier: `chemcore-legacy-uid-bridge/2`.

### Residual limitation

The compatibility fallback remains necessary for legacy-only candidates when the QSPR/database-backed layer has not produced a candidate for that material. A future schema hardening phase should pass an explicit database-backed `legacy_id -> uid` resolver into all ChemCore entry points and remove the arithmetic fallback entirely.

---

## 2. M3 multi-channel fingerprint promotion

The scientific rule remains unchanged and is now documented more explicitly:

- Research/source-backed fingerprint references are identity evidence only.
- At least **2 installed HR069 MQ channels** must overlap the fingerprint.
- Fingerprint distance must be within the accepted identity gate.
- The top fingerprint must be sufficiently separated from the runner-up using the existing score/distance margins.
- An explicit accepted Open-Set/OOD result is required.
- Promotion target is **`IDENTIFIED / M3_ARRAY_CONSISTENT`** only.
- Fingerprint identity never creates M4 evidence and never makes PPM reportable.
- Existing `UNKNOWN`, mixture/conflict, or non-promotable outcomes cannot be overridden by the fingerprint layer.

Fingerprint identity implementation version is now `accepted-id-fp-v2-uid-safe`.

### Important semantic distinction

`M3_ARRAY_CONSISTENT` means multi-channel identity consistency. It is not device/reference-gas quantitative calibration. Occupational quantitative interpretation remains gated by `QUANTIFIED + M4_HAZROB_VALIDATED`.

---

## 3. OOD / Open-Set provenance

`ChemCoreOpenSetGate` is upgraded to `chemcore-open-set/2-provenance`.

Each `Result` now exposes:

- `gateKind`
- `independentlyValidatedThreshold`

Supported gate kinds:

- `EMPIRICAL_TRAINING_QUANTILE`
- `INDEPENDENT_VALIDATION_THRESHOLD`
- `EXTERNAL_RUNTIME_POLICY`

The OOD reason string also carries both provenance values. This preserves the current accepted/rejected behaviour while making the scientific status auditable.

### Why this matters

An OOD threshold estimated from the training distribution is operationally useful, but it must not be described as independently validated. Conversely, `fromValidatedThreshold(...)` now records the stronger provenance explicitly.

No numerical OOD threshold was changed by this change request. No new validation claim was manufactured.

---

## 4. Safety properties preserved

The following fail-closed properties remain in force:

- missing/rejected Open-Set state cannot be overridden by an exact fingerprint match;
- a one-channel fingerprint cannot become a named M3 identity;
- M3 does not create reportable PPM;
- ambiguous UID resolution fails closed;
- QSPR remains M1 transfer support and cannot independently create M3/M4;
- M4 remains a separate HR069 device/reference-gas validation gate.

---

## 5. Tests added/extended

### `ChemCoreLegacyCandidateRegistryTest`

Covers:

1. database-backed/QSPR UID overrides the historical offset for the same chemical;
2. compatibility offset is retained only when no database-backed candidate exists;
3. two conflicting database UIDs for one normalized label fail closed;
4. invalid legacy catalogue IDs fail closed.

### `ChemCoreOpenSetGateTest`

Extended to verify:

1. empirical gates are tagged `EMPIRICAL_TRAINING_QUANTILE`;
2. empirical thresholds do not claim independent validation;
3. validated-threshold gates are tagged `INDEPENDENT_VALIDATION_THRESHOLD`;
4. validated threshold provenance is retained in the result/reason;
5. insufficient feature coverage still fails closed;
6. explicit live/runtime gate results are tagged `EXTERNAL_RUNTIME_POLICY`.

### Existing fingerprint tests retained

`AcceptedFingerprintIdentityEngineTest` already verifies:

- exact multi-channel ethanol reference can reach M3 only after accepted OOD;
- missing OOD fails closed;
- rejected OOD fails closed;
- a single-channel acetone prior does not become named identity;
- `UNKNOWN` cannot be overridden;
- fingerprint-only M3 never produces reportable PPM.

---

## 6. Acceptance criteria

This change is acceptable only if CI confirms:

- JVM unit tests compile and pass;
- Android application compilation succeeds;
- existing ChemCore result/report contract tests remain green;
- no test shows a fingerprint M3 result with finite/reportable PPM;
- UID bridge tests pass for non-default database UID values;
- OOD provenance tests pass without changing the numeric gate thresholds.

---

## 7. Recommended next steps

1. Replace the residual `legacy_id + 1` fallback with an explicit database-backed UID resolver at every production ChemCore entry point.
2. Add a database integrity audit reporting any legacy row whose `uid` differs from the historical offset, without rewriting IDs automatically.
3. Persist OOD `gateKind` and validation provenance as first-class fields in `ChemCoreResultEnvelope` instead of relying only on the note/reason chain.
4. Validate Open-Set thresholds on held-out known and unknown HR069 sessions and publish confusion/rejection metrics in the model card.
5. Validate fingerprint M3 thresholds (`distance`, score margin, distance margin) on controlled pure-gas and interference datasets.
6. Build a dedicated mixture dataset before interpreting the current residual guard as anything stronger than a mixture/interference warning.

---

## Non-goals

This change does **not**:

- claim M4 calibration for any new chemical;
- alter existing PPM response curves;
- change the uncertainty proxy values;
- infer mixture components;
- add generic T/RH compensation coefficients;
- claim that softmax ranking weights are calibrated probabilities of chemical presence.
