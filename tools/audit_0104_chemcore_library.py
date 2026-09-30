#!/usr/bin/env python3
"""HazRob 0.10.2 / HR069 P4 ChemCore analytical-library updater contract."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def txt(p): return (ROOT/p).read_text(encoding='utf-8')
def req(s,n,label):
    if n not in s: raise AssertionError(f'{label}: missing {n!r}')
def forbid(s,n,label):
    if n in s: raise AssertionError(f'{label}: forbidden {n!r}')

def main():
    manifest=txt('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManifest.java')
    verifier=txt('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryVerifier.java')
    client=txt('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryUpdateClient.java')
    manager=txt('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManager.java')
    store=txt('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryStore.java')
    reader=txt('app/src/main/java/com/servoc/app/chemcore/library/ChemCoreStableKnowledgeReader.java')
    settings=txt('app/src/main/java/com/servoc/app/ui/SettingsTab.java')
    dev=txt('app/src/main/java/com/servoc/app/ui/ChemCoreLibraryTab.java')
    builder=txt('tools/build_chemcore_stable_snapshot.py')
    publisher=txt('tools/publish_chemcore_library.py')
    workflow=txt('.github/workflows/chemcore-library-publish-0102.yml')
    gradle=txt('app/build.gradle')

    for n in ['MANIFEST_SCHEMA = 2','payloadSchema','dataRevision','evidenceRevision','chemcore-manifest-v2','STABLE_PAYLOAD_SCHEMA']:
        req(manifest,n,'manifest v2')
    for n in ['payload_schema','data_revision','evidence_revision']:
        req(publisher,n.replace('_','-') if False else n,'publisher manifest fields')
    req(builder,'EVIDENCE_REVISION = "hazrob-evidence-policy/1"','evidence revision')
    req(builder,'data_revision(source_files)','data revision fingerprint')

    # Remote Stable metadata is authenticated before use.
    req(manager,'ChemCorePublicationKeyProvider.load(context)','publication key loading')
    req(manager,'verifyManifestOnly(m, appVersion, REQUIRED_SAMPLING_PROFILE, key)','manifest-first authentication')
    req(manager,'REQUIRED_SAMPLING_PROFILE = SamplingProfile.hazRobStandardChamberV2().profileId','sampling profile compatibility')

    # HTTPS-only bounded transport and no redirect downgrade surface.
    req(client,'requireHttps(manifestUrl)','HTTPS manifest')
    req(client,'requireHttps(payload)','HTTPS payload')
    req(client,'setInstanceFollowRedirects(false)','redirect policy')
    req(client,'MAX_PAYLOAD_BYTES','payload bound')
    forbid(client,'http://','no plaintext updater endpoint')

    # Hash + signature + semantic payload contract.
    for n in ['payload SHA-256 mismatch','verifyManifestSignature','validateStablePayloadSemantics','payload schema does not match signed manifest','payload data revision does not match signed manifest','payload evidence revision does not match signed manifest']:
        req(verifier,n,'verifier')
    req(verifier,'m4_requires_device_specific_validation','M4 safety policy')
    req(verifier,'external_research_packaging_does_not_promote_m4','external evidence safety policy')

    # Staging, durable writes, atomic directory activation and pointer commit.
    req(store,'copyAndSync(downloadedPayload, payloadOut)','staged payload copy')
    req(store,'verifyPayload(payloadOut, manifest, appVersion, publicationKey)','staged re-verification')
    req(store,'tempDir.renameTo(finalDir)','atomic package activation')
    req(store,'active library pointer commit failed','durable active pointer')
    req(store,'rollbackToPrevious(boolean measurementActive, String appVersion, PublicKey publicationKey)','verified rollback API')
    req(store,'verifyPayload(payload, previous, appVersion, publicationKey)','rollback full re-verification')
    req(store,'getFilesDir(), "chemcore-library"','library isolated from servoc.db')

    # Update/rollback interlock and offline runtime.
    req(store,'update blocked while measurement/analysis is active','update interlock')
    req(store,'rollback blocked while measurement/analysis is active','rollback interlock')
    req(manager,'measurementOrAnalysisActive','analysis interlock')
    req(reader,'Read-only adapter from a verified ChemCore Stable JSON payload','offline reader')
    forbid(reader,'HttpURLConnection','runtime must not require network')
    forbid(reader,'new URL(','runtime must not require URL')

    # User update is independent from APK update/reinstall.
    req(settings,'تحديثات منظومة التحليل','user analytical update surface')
    req(settings,'checkAnalysisUpdate()','user analytical update action')
    req(settings,'libraryManager.install','user installs library directly')
    forbid(settings,'AppUpdateManager','library update must not reinstall APK')
    req(dev,'manager.rollback(active, ReleaseAudit.VERSION)','developer verified rollback')

    # Build-pinned endpoint and publication workflow must agree on v2.
    req(gradle,'CHEMCORE_LIBRARY_MANIFEST_URL','build-pinned Stable endpoint')
    req(workflow,"assert m['schema_version']==2",'publication workflow schema')
    req(workflow,"text='chemcore-manifest-v2\\n'",'publication signature contract')
    req(workflow,'--data-revision "$DATA_REVISION" --evidence-revision "$EVIDENCE_REVISION"','publication revisions')
    req(workflow,'CHEMCORE_PUBLICATION_PRIVATE_KEY_PEM','private signing key remains secret-backed')

    print('HazRob 0.10.2 / HR069 P4 ChemCore library updater contract: PASS')
    key=(ROOT/'app/src/main/assets/chemcore/publication_key.pem').read_text(encoding='utf-8').strip()
    if 'NOT_PROVISIONED' in key:
        print('P4 CRYPTO PROVISIONING: BLOCKED — publication public key is still NOT_PROVISIONED (expected fail-closed runtime).')
    else:
        print('P4 CRYPTO PROVISIONING: public key asset present; publication workflow still verifies it against the secret private key.')
    return 0

if __name__=='__main__':
    try: raise SystemExit(main())
    except AssertionError as exc:
        print(f'HazRob 0.10.2 / HR069 P4 ChemCore library updater contract: FAIL: {exc}')
        raise SystemExit(1)
