# ChemCore Stable Library Publication Key Provisioning

**Status:** REQUIRED BEFORE FIRST REMOTE STABLE INSTALL  
**Trust contract:** `SHA256withRSA` over `chemcore-manifest-v1` canonical bytes

## Purpose

ChemCore Stable Library updates are independently versioned scientific artifacts. HTTPS protects transport, but the application additionally requires a publication signature so a compromised mirror, accidental file replacement, or unsigned Research bundle cannot silently become operational scientific knowledge.

## Key separation

Use a dedicated ChemCore publication key pair. Do **not** reuse:

- the Android APK signing key;
- the prototype signing key;
- GitHub credentials/PATs;
- MQTT/TLS credentials;
- any robot provisioning secret.

The **private key** signs Stable publication manifests and must never be committed to GitHub, placed in the APK, uploaded to a public distribution endpoint, or entered into the Android UI.

The **public key** is safe to distribute and is bundled into the APK at:

`app/src/main/assets/chemcore/publication_key.pem`

`ChemCorePublicationKeyProvider` fails closed when this key is absent or invalid; Stable remote installation remains disabled while embedded/local scientific knowledge continues to work.

## Recommended provisioning procedure

1. Generate a dedicated RSA key pair in a controlled environment.
2. Store the private PEM in the GitHub Actions secret `CHEMCORE_PUBLICATION_PRIVATE_KEY_PEM` or an equivalent protected CI signing service.
3. Export only the public PEM and commit it to `app/src/main/assets/chemcore/publication_key.pem`.
4. Record the public-key SHA-256 fingerprint in the release audit documentation.
5. Run `.github/workflows/chemcore-library-publish.yml` on an approved payload.
6. Confirm the Android verifier accepts the signed manifest and rejects a one-byte payload or manifest modification.
7. Only then configure the Stable HTTPS manifest endpoint in the app.

## Rotation

A compromised or intentionally rotated publication key requires an **application update** because the trust anchor lives in the APK. Existing Stable library snapshots remain traceable by their manifest, payload SHA-256 and source commit. A key rotation must be documented as a trust-policy change in the ChemCore master plan and research freeze record.

## Recovery rule

The updater keeps the previous verified snapshot for developer rollback. Rollback never bypasses signature/hash checks for a newly downloaded package and is blocked during active measurement/analysis.
