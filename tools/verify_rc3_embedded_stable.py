#!/usr/bin/env python3
"""RC3 build-time and post-APK gate for deployable, not research-reference, Stable."""
import argparse,hashlib,json,pathlib,sys,zipfile

EXPECTED_VERSION="1.0.0-hr069r1"
EXPECTED_SCHEMA="chemcore-stable-snapshot-v1"
EXPECTED_PROFILE="hazrob-chemcore-chamber-v2"
ASSET_ROOT="assets/generated/chemcore/embedded_stable/"

def verified(manifest_bytes,payload):
    manifest=json.loads(manifest_bytes)
    snap=json.loads(payload)
    assert manifest["library_version"]==EXPECTED_VERSION,manifest.get("library_version")
    assert manifest["channel"]=="stable"
    assert manifest["payload_schema"]==EXPECTED_SCHEMA
    assert manifest["min_app_version"]=="0.10.2"
    assert manifest["max_app_version"]=="0.10.2"
    assert EXPECTED_PROFILE in manifest["sampling_profile_compatibility"]
    assert manifest["payload_file"]=="chemcore-stable-snapshot.json"
    assert manifest["chemical_count"]==652 and manifest["calibration_count"]==0
    assert len(payload)==manifest["payload_size_bytes"]
    assert hashlib.sha256(payload).hexdigest()==manifest["payload_sha256"]
    assert snap["content_counts"]["chemical_identities"]==652
    assert snap["content_counts"]["response_priors"]==49
    assert snap.get("embedded_never_promotes_experiment_to_m4") is True
    assert snap["knowledge"]["response_priors"]["policy"]["external_m4"] is False
    records=snap["knowledge"]["response_priors"]["records"]
    assert len(records)==49
    assert all(x.get("evidence_tier") in ("M1","M2") and not x.get("device_validated",False) for x in records)
    return manifest

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--asset-dir",type=pathlib.Path,required=True)
    parser.add_argument("--apk",type=pathlib.Path)
    opts=parser.parse_args()
    folder=opts.asset_dir
    a=(folder/"manifest.json").read_bytes()
    b=(folder/"chemcore-stable-snapshot.json").read_bytes()
    manifest=verified(a,b)
    if opts.apk:
        with zipfile.ZipFile(opts.apk) as z:
            embedded_manifest=z.read(ASSET_ROOT+"manifest.json")
            embedded_payload=z.read(ASSET_ROOT+"chemcore-stable-snapshot.json")
        assert embedded_manifest==a,"APK manifest differs from verified generated asset"
        assert embedded_payload==b,"APK payload differs from verified generated asset"
        verified(embedded_manifest,embedded_payload)
    print(json.dumps({"stable_version":manifest["library_version"],"payload_sha256":manifest["payload_sha256"],
        "identities":652,"screening_priors":49,"reference_gas_m4":0,
        "in_apk":bool(opts.apk),"result":"PASS"},sort_keys=True))

if __name__=="__main__":
    main()
