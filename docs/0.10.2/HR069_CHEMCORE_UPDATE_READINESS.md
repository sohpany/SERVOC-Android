# HazRob 0.10.2 / HR069 — ChemCore in-app update readiness

Date: 2026-09-29
Target: `release/hazrob-0.10.2` / `HR069`

## Executive status

The Android application already contains the complete **runtime update mechanism** required for independently updating the ChemCore scientific library without replacing the APK:

- HTTPS-only manifest discovery and payload download;
- size bounds and staged download;
- signed-manifest / signed-payload verification;
- compatibility checks against app version and feature/sampling schema;
- atomic activation and rollback;
- fail-closed blocking while warm-up, measurement or analysis is active;
- Arabic developer UI to check, download/install and roll back the library;
- runtime reader for Stable response priors and validated model artifacts.

However, the current 0.10.2 source still carries the placeholder `NOT_PROVISIONED` in `app/src/main/assets/chemcore/publication_key.pem`. Therefore **a currently installed APK built with that placeholder cannot activate a remote signed ChemCore update**. `ChemCorePublicationKeyProvider` intentionally returns no key and the installer fails closed.

## Required one-time action

A one-time APK rebuild/update is required to provision the public verification key. After that APK is installed, subsequent compatible ChemCore Stable scientific-library updates can be delivered independently through the in-app updater.

Required secure steps:

1. Generate/choose the long-lived ChemCore publication RSA key pair outside the repository.
2. Commit **only the public key** to `app/src/main/assets/chemcore/publication_key.pem`.
3. Store the matching private key only as the GitHub Actions secret `CHEMCORE_PUBLICATION_PRIVATE_KEY_PEM`; never commit or paste it into source, logs, issues or documentation.
4. Build/sign/qualify the 0.10.2 HR069 APK once with the provisioned public key.
5. Install that APK on the field phone.
6. Publish future Stable ChemCore bundles with `.github/workflows/chemcore-library-publish-0102.yml`.
7. The installed app then checks the build-pinned endpoint `https://hazrob-app-updates.onrender.com/chemcore/manifest.json`, verifies signatures, stages and activates compatible updates.

## HR069 scientific payload currently prepared

The 0.10.2 branch now has a reproducible candidate packaging path containing:

- 652 NIOSH-backed chemical identity records;
- 43 pinned MQDataScience sensor-response curves, preserved as non-M4 research priors;
- six higher-provenance peer-reviewed MQ135 response-curve overlays in their published 10–100 ppm domain;
- the complete 76-model MQDataScience research catalogue with Android/runtime status preserved;
- external MQ/MOS dataset/source provenance as reference artifacts;
- explicit policy preventing external research data from self-promoting to M4.

`tools/build_hr069_research_priors.py` produces the response-prior layer, and the 0.10.2 publication workflow packages it into the signed ChemCore Stable snapshot.

## Security / scientific acceptance gates

No workaround that disables signature verification is permitted. The updater remains fail closed until the real publication key is provisioned.

External evidence remains M1/M2/M3/RESEARCH_EXTERNAL as applicable. Only physical HR069 device-specific accepted reference-gas calibration/validation may create M4 quantitative evidence.

## Resume checkpoint

If work is interrupted, continue from `docs/0.10.2/HR069_EXTERNAL_DATA_ENRICHMENT_WORKPLAN.md`, then this document. The publication-key provisioning step is a **manual/secure-account prerequisite**, not a scientific-data blocker: laboratory ingestion and candidate ChemCore packaging can continue before the key is provisioned, but distribution to the already-installed app cannot complete until the one-time keyed APK is installed.
