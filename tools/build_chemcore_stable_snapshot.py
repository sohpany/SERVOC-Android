#!/usr/bin/env python3
"""Build the canonical ChemCore Stable knowledge payload from approved repository assets.

This is a packaging step, not an approval step. It fails closed if the pinned molecular identity
layer is incomplete or internally inconsistent. Research/candidate material is included only when
an explicit approved input path is supplied by the release workflow; evidence tiers are preserved
and external material cannot become M4 merely by being packaged.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

MOLECULAR_SCHEMA = "chemcore-molecular-seed-v1"
CURATION_SCHEMA = "chemcore-unresolved-curation-v1"
SNAPSHOT_SCHEMA = "chemcore-stable-snapshot-v1"


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        value = json.load(f)
    if not isinstance(value, dict):
        raise SystemExit(f"expected JSON object: {path}")
    return value


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def validate_molecular(seed: dict) -> None:
    if seed.get("schema") != MOLECULAR_SCHEMA:
        raise SystemExit(f"unexpected molecular schema: {seed.get('schema')}")
    records = seed.get("records")
    if not isinstance(records, list): raise SystemExit("molecular seed records array missing")
    declared = int(seed.get("record_count", len(records))); resolved = int(seed.get("resolved_count", 0)); unresolved = int(seed.get("unresolved_count", 0)); errors = int(seed.get("error_count", 0))
    if declared != len(records): raise SystemExit(f"molecular record_count mismatch: {declared} != {len(records)}")
    if declared != 652: raise SystemExit(f"Stable HazRob identity layer must contain 652 records, got {declared}")
    if resolved + unresolved != declared: raise SystemExit("resolved + unresolved does not equal molecular record_count")
    if errors != 0: raise SystemExit(f"molecular seed has retrieval errors: {errors}")
    for i, row in enumerate(records):
        if not isinstance(row, dict): raise SystemExit(f"molecular record {i} is not an object")
        if not str(row.get("cas", "")).strip(): raise SystemExit(f"molecular record {i} missing CAS")
        if row.get("status") not in {"RESOLVED", "UNRESOLVED"}: raise SystemExit(f"molecular record {i} has invalid status")


def validate_curation(curation: dict, molecular: dict) -> None:
    if curation.get("schema") != CURATION_SCHEMA: raise SystemExit(f"unexpected curation schema: {curation.get('schema')}")
    rows = curation.get("records")
    if not isinstance(rows, list): raise SystemExit("curation records array missing")
    declared = int(curation.get("record_count", len(rows)))
    if declared != len(rows): raise SystemExit("curation record_count mismatch")
    if declared != int(molecular.get("unresolved_count", -1)): raise SystemExit("curation count does not match molecular unresolved_count")
    if any(bool(r.get("molecular_qspr_eligible", False)) for r in rows if isinstance(r, dict)): raise SystemExit("unresolved curation must not silently promote QSPR eligibility")


def optional_json(path: str) -> object:
    if not path: return []
    p = Path(path)
    if not p.is_file(): raise SystemExit(f"optional approved input missing: {p}")
    with p.open("r", encoding="utf-8") as f: return json.load(f)


def count_items(value: object) -> int:
    if isinstance(value, list): return len(value)
    if isinstance(value, dict):
        for key in ("records", "items", "artifacts", "models", "calibrations", "priors", "sources", "curves"):
            q = value.get(key)
            if isinstance(q, list): return len(q)
    return 0


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--molecular", default="app/src/main/assets/generated/chemcore/molecular_descriptors.json")
    p.add_argument("--curation", default="app/src/main/assets/generated/chemcore/unresolved_curation.json")
    p.add_argument("--response-priors", default=""); p.add_argument("--model-artifacts", default=""); p.add_argument("--calibrations", default=""); p.add_argument("--reference-artifacts", default="")
    p.add_argument("--source-commit", default=""); p.add_argument("--out", required=True); args = p.parse_args()

    molecular_path = Path(args.molecular); curation_path = Path(args.curation)
    molecular = load_json(molecular_path); curation = load_json(curation_path); validate_molecular(molecular); validate_curation(curation, molecular)
    response_priors = optional_json(args.response_priors); model_artifacts = optional_json(args.model_artifacts); calibrations = optional_json(args.calibrations); reference_artifacts = optional_json(args.reference_artifacts)

    snapshot = {
        "schema": SNAPSHOT_SCHEMA,
        "created_at_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source_commit_sha": args.source_commit,
        "policy": {"channel":"stable","molecular_presence_does_not_imply_detectability":True,"unresolved_entities_are_not_forced_to_single_molecules":True,"m4_requires_device_specific_validation":True,"external_research_packaging_does_not_promote_m4":True},
        "content_counts": {"chemical_identities":int(molecular.get("record_count",0)),"molecularly_resolved":int(molecular.get("resolved_count",0)),"identity_curation_records":int(curation.get("record_count",0)),"response_priors":count_items(response_priors),"model_artifacts":count_items(model_artifacts),"calibrations":count_items(calibrations),"reference_artifacts":count_items(reference_artifacts)},
        "source_files": {"molecular_descriptors_sha256":sha256(molecular_path),"unresolved_curation_sha256":sha256(curation_path)},
        "knowledge": {"molecular_seed":molecular,"identity_curation":curation,"response_priors":response_priors,"model_artifacts":model_artifacts,"calibrations":calibrations,"reference_artifacts":reference_artifacts},
    }
    for key,path in (("response_priors_sha256",args.response_priors),("model_artifacts_sha256",args.model_artifacts),("calibrations_sha256",args.calibrations),("reference_artifacts_sha256",args.reference_artifacts)):
        if path: snapshot["source_files"][key]=sha256(Path(path))
    out=Path(args.out); out.parent.mkdir(parents=True,exist_ok=True); out.write_text(json.dumps(snapshot,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")
    print(json.dumps({"schema":SNAPSHOT_SCHEMA,"output":str(out),"sha256":sha256(out),"bytes":out.stat().st_size,**snapshot["content_counts"]},indent=2)); return 0

if __name__ == "__main__": raise SystemExit(main())
