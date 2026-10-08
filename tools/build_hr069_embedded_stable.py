#!/usr/bin/env python3
"""Construct a self-contained PRE-RELEASE ChemCore Stable-channel snapshot for HR069-R1.

This is *not* a published scientific Stable approval or promotion of reference-gas M4
evidence. Existing pinned molecular/curation data and non-M4 43 MQ research curves
with six peer-reviewed overlays are assembled in the current Stable format.
The application installs it only as its initial offline knowledge if no verified
library is already installed. Remote signed/approved future Stable can supersede it.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SCHEMA="chemcore-stable-snapshot-v1"
CREATED="2026-10-08T00:00:00Z"
# Original verified source commit of HR069-R1 at creation of the science-compatible build.
SOURCE_COMMIT="b17e37ade80d79bcdfd5f320e2eb096093b6d2bb"
LIBRARY_VERSION="1.0.0-hr069r1"
PROFILE="hazrob-chemcore-chamber-v2"
FEATURE_SCHEMA="chemcore-features/1"


def write(staging: Path):
    staging.mkdir(parents=True,exist_ok=True)
    priors=staging/"hr069_research_response_priors.json"
    subprocess.run([sys.executable,str(ROOT/"tools/build_hr069_research_priors.py"),"--out",str(priors)],cwd=ROOT,check=True)
    rows=json.loads(priors.read_text(encoding="utf-8"))
    if rows["schema"]!="hazrob.chemcore-response-priors.v1" or rows["record_count"]!=49:
        raise ValueError("unexpected HR069 response prior count or schema")
    if any(r.get("evidence_tier") not in {"M1","M2"} or r.get("device_validated") for r in rows["records"]):
        raise ValueError("no unapproved M4 evidence may enter embedded Stable")
    snapshot=staging/"chemcore-stable-snapshot.json"
    subprocess.run([sys.executable,str(ROOT/"tools/build_chemcore_stable_snapshot.py"),
        "--molecular",str(ROOT/"app/src/main/assets/generated/chemcore/molecular_descriptors.json"),
        "--curation",str(ROOT/"app/src/main/assets/generated/chemcore/unresolved_curation.json"),
        "--response-priors",str(priors),
        "--source-commit",SOURCE_COMMIT,
        "--out",str(snapshot)],cwd=ROOT,check=True)
    root=json.loads(snapshot.read_text(encoding="utf-8"))
    if root.get("schema")!=SCHEMA or root["content_counts"]["chemical_identities"]!=652 or root["content_counts"]["response_priors"]!=49:
        raise ValueError("Stable snapshot expected content counts missing")
    if root["knowledge"]["response_priors"]["policy"]["external_m4"]:
        raise ValueError("research prior must be non-M4")
    root["created_at_utc"]=CREATED
    root["embedded_build_state"]="WORKING_PRE_RELEASE_NO_SCIENTIFIC_PUBLICATION"
    root["embedded_research_priors_may_support_screening_only"]=True
    root["embedded_never_promotes_experiment_to_m4"]=True
    snapshot.write_text(json.dumps(root,ensure_ascii=False,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    raw=snapshot.read_bytes()
    manifest={
        "library_version":LIBRARY_VERSION,
        "schema_version":2,
        "channel":"stable",
        "created_utc":CREATED,
        "source_commit_sha":SOURCE_COMMIT,
        "payload_file":"chemcore-stable-snapshot.json",
        "payload_format":"json",
        "payload_schema":SCHEMA,
        "integrity_mode":"sha256",
        "payload_sha256":hashlib.sha256(raw).hexdigest(),
        "payload_size_bytes":len(raw),
        "chemical_count":652,
        "source_count":0,
        "model_count":0,
        "calibration_count":0,
        "min_app_version":"0.10.2",
        "max_app_version":"0.10.2",
        "feature_schema_version":FEATURE_SCHEMA,
        "sampling_profile_compatibility":[PROFILE],
        "data_revision":root["data_revision"],
        "evidence_revision":root["evidence_revision"],
    }
    (staging/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    priors.unlink()
    print(json.dumps({"version":LIBRARY_VERSION,"chemical_identities":652,"response_priors":49,"m4":False,"manifest_sha256":hashlib.sha256((staging/"manifest.json").read_bytes()).hexdigest(),"payload_sha256":manifest["payload_sha256"],"payload_bytes":len(raw)}))


if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--out-dir",required=True,type=Path)
    args=p.parse_args()
    write(args.out_dir)
