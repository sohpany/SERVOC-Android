# CR-20261008-012 — BME280 operational status and 4D analytics research clarification

**Status:** REGISTERED / DOCUMENTATION-ONLY / NON-AUTHORITATIVE SCIENTIFIC OBSERVATION  
**Date:** 2026-10-08  
**Repository:** `sohpany/SERVOC-Android`  
**Existing branch (no new branch):** `release/hazrob-0.10.2-rc2`  
**Starting HEAD:** `5250ea769615ec82c5dc944576ea3042736b3db2`

## Change reason
The project owner explicitly reported on 2026-10-08 that the **installed BME280 is operating very well**, superseding earlier discussions of possible malfunction. The owner asked to document this and review HazRob's pre-existing multivariable / 4D MQDataScience library.

## Authorized documentation-only scope
1. Add a dated BME280 field-operator confirmation with its evidence/verification limits.
2. Describe the separate definitions of four-dimensional time-environment-response models and four-coordinate concentration-response calibration manifolds.
3. Link pinned existing 76-model catalogue, 43 source-derived 1D curves, environmental correction surfaces and research qualification restrictions.

## Exclusions
No changes to ESP32, ESP32-CAM, power supplies, hardware status flags, app, ChemCore, scientific calibration, 136-feature v1 schema, response curves, M3/M4 decisions, quantitative outputs or active releases. Do not claim live digital telemetry was directly captured or compared with a reference instrument by this documentation action.

## Risk / controls
- User-confirmed operational health is **not** metrological qualification of temperature, RH or pressure accuracy.
- Four plotted variables do **not** imply analyte identity, independent concentration axis or laboratory calibration data.
- MQDataScience models originally model a response given time and optional temperature and RH; chemical concentration calibration must be an independently labeled and validated adaptation.
- Preserve the versioned provenance, fail-closed heater gating, environmental domain controls and M4 quantitative gate.
- Retain earlier malfunction notes as historical, rather than silently changing audit history.

## Planned artifacts
- `docs/0.10.2/CR-20261008-012_BME280_AND_FOUR_DIMENSIONAL_RESEARCH.md` (this change record).
- `docs/0.10.2/HR069_BME280_USER_CONFIRMED_STATUS_AND_4D_RESEARCH_2026-10-08.md` (evidence and proposed research usage).

## Documentation verification
- Check that both new files exist on the current RC2 branch.
- Cross-reference existing MQ76 catalog and research-domain source files.
- Review Git compare to ensure only the above documentation files changed.
- Confirm no authority/promotion claim, operational-test claim or M4 calibration inference.

**Approval boundary:** User's instruction is to record facts and analyze current methods, not to deploy or certify a new algorithm.
