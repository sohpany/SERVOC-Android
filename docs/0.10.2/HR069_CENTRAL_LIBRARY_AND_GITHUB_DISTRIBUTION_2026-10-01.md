# HazRob HR069 — Central analytical library and direct GitHub distribution

Date: 2026-10-01  
Release authority: **RC2 / 0.10.2 / HR069**  
Status: **ACTIVE / CANONICAL**

## 1. Decision

HazRob-Lab is frozen as of 2026-10-01. It is historical/scientific provenance only and is no longer an active research, staging, calibration, publication or distribution workspace.

All new analytical and experimental data must be admitted to one central scientific library:

- repository: `sohpany/SERVOC-Android`
- branch: `release/hazrob-0.10.2-rc2`
- canonical evidence corpus: `research_data/hr069_analytical_library/`

This record supersedes any prior workflow that required new research to be staged in `sohpany/HazRob-Lab` before entering production.

## 2. Single-authority analytical flow

The approved path is:

`experiment / reference gas / external evidence`

→ provenance and source preservation

→ `research_data/hr069_analytical_library/`

→ schema / identity / sensor-channel / domain checks

→ scientific qualification and ChemCore runtime eligibility decision

→ signed/hashed ChemCore publication package

→ public read-only GitHub distribution channel

→ HazRob user application verifies and installs the update.

No parallel scientific-library path is authoritative.

## 3. What must be uploaded to the central library

All future analytical additions belong in the RC2 central library or its controlled adjacent provenance/index directories, including:

- raw or derived experiment evidence;
- HR069 device/reference M4 calibration evidence;
- R0/reference records where appropriate;
- response curves;
- pure-analyte fingerprints;
- mixture/headspace/operational fingerprints;
- transfer datasets;
- same-sensor peer-reviewed identity evidence;
- environmental compensation evidence;
- drift/anchor datasets;
- model cards and qualified model artifacts;
- source DOI/URL/license/access state;
- source and derived hashes;
- concentration, temperature, RH, pressure, chamber/flow and heater conditions;
- analyte/class identity mapping and unresolved queues.

Raw observations are never overwritten by derived artifacts.

## 4. Runtime promotion remains controlled

Being present in the central library does not automatically make an artifact runtime-authoritative.

Evidence remains classified separately from runtime eligibility:

- M0: catalogue/identity only;
- M1: molecular/QSPR/research inference;
- M2: manufacturer/literature/known-concentration support;
- M3: accepted array-consistent/fingerprint identity evidence;
- M4: HR069 device-specific reference-gas validation suitable for quantitative reportability when all other gates pass.

External evidence cannot self-promote to M4.

## 5. User update distribution

HazRob applications must obtain library updates **directly from GitHub**.

The end-user update channel must satisfy all of the following:

1. GitHub-hosted.
2. Public read-only access for ordinary users.
3. No GitHub token or account credential stored in the APK.
4. Manifest-driven version discovery.
5. Content hashes for payload integrity.
6. Cryptographic signature/publication-key verification before activation.
7. App-version/schema/sampling-profile compatibility checks.
8. Atomic install with rollback to the last verified library.
9. Fail closed if manifest, signature, hash or compatibility verification fails.
10. No Render/Railway/Cloudflare/HiveMQ fallback for scientific-library distribution.

Private source repositories may continue to hold development material. Only the qualified publication payload and manifest need to be exposed through the public GitHub distribution channel.

## 6. Current activation state

RC2 already implements a GitHub-only/fail-closed update policy, but the configured user-facing manifest endpoint remains blank until a **public read-only GitHub publication repository/channel** is provisioned.

Do not point the application at a private GitHub raw URL: ordinary users would require authentication and the update would fail.

Until the public endpoint exists, the app must remain fail-closed rather than fall back to a non-GitHub scientific source.

## 7. Repository roles after this decision

### `sohpany/SERVOC-Android`

**ACTIVE authority.**

- application and firmware source;
- RC2 ChemCore runtime;
- central analytical evidence corpus;
- experiment/reference evidence intake;
- scientific packaging source.

### `sohpany/HazRob-Lab`

**FROZEN.**

- historical scientific provenance only;
- no new experiments or library updates;
- never an end-user distribution source.

### `HazRob/build-harness`

**QUALIFICATION ONLY.**

- pinned source checkout;
- audits, tests, builds and evidence;
- not the scientific source of truth;
- not the central library;
- not the end-user update origin.

### Public GitHub publication channel

**END-USER DISTRIBUTION ONLY.**

- read-only manifests and qualified signed/hashed payloads;
- no private source credentials;
- no raw confidential research requirement.

## 8. Existing canonical corpus retained

The adopted RC2 corpus retains, among other assets:

- 144-entry operational catalogue context;
- 652-record molecular/NIOSH identity layer;
- 43 MQ research curves;
- 76 MQDataScience research models;
- 13 numeric fingerprint families;
- Food Freshness 59-session headspace fingerprints;
- Multimodal MQ transfer data;
- UCI MQ135 low-concentration transfer data;
- RoboFusion operational gas-leak transfer prototypes;
- Methanol, Ethylene and 1-Propanol transfer evidence;
- Kononov MOX numeric transfer data;
- same-sensor peer-reviewed identity evidence;
- source/provenance indexes and molecular-resolution deltas.

## 9. Change-control rule

Every new central-library addition must preserve at minimum:

- source and provenance;
- source/derived hash where applicable;
- chemical or class identity;
- CAS where available;
- sensor models and exact HR069 channel overlap;
- raw/derived distinction;
- concentration units/domain;
- environmental and sampling conditions where available;
- evidence tier;
- M4 status;
- runtime eligibility;
- transformation version;
- validation result;
- revision date.

No missing sensor value may be fabricated to make a fingerprint appear complete.

## 10. Authority statement

From this checkpoint forward, the phrase **HazRob central analytical library** means the RC2/HR069 canonical corpus in `research_data/hr069_analytical_library/` plus its controlled provenance/coverage inputs in the active `release/hazrob-0.10.2-rc2` line.

HazRob-Lab is not part of the active write/update path.
