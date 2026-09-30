#!/usr/bin/env python3
"""Build an immutable experimental ChemCore library publication bundle.

The prototype updater intentionally uses HTTPS + SHA-256 + compatibility/semantic validation.
Digital publication signing is deferred until production hardening.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--payload", required=True); p.add_argument("--out-dir", required=True)
    p.add_argument("--library-version", required=True); p.add_argument("--source-commit", required=True)
    p.add_argument("--channel", choices=["stable", "research"], default="stable")
    p.add_argument("--payload-format", choices=["json", "sqlite"], default="json")
    p.add_argument("--payload-schema", required=True); p.add_argument("--data-revision", required=True); p.add_argument("--evidence-revision", required=True)
    p.add_argument("--min-app-version", required=True); p.add_argument("--max-app-version", default="")
    p.add_argument("--feature-schema", required=True); p.add_argument("--sampling-profile", action="append", default=[])
    p.add_argument("--chemical-count", type=int, required=True); p.add_argument("--source-count", type=int, default=0)
    p.add_argument("--model-count", type=int, default=0); p.add_argument("--calibration-count", type=int, default=0)
    args = p.parse_args()

    payload = Path(args.payload).resolve()
    if not payload.is_file(): raise SystemExit(f"payload missing: {payload}")
    out = Path(args.out_dir).resolve(); out.mkdir(parents=True, exist_ok=True)
    payload_out = out / payload.name; shutil.copy2(payload, payload_out)
    if any(x < 0 for x in (args.chemical_count, args.source_count, args.model_count, args.calibration_count)):
        raise SystemExit("record counts must be non-negative")

    manifest = {
        "library_version": args.library_version, "schema_version": 2, "channel": args.channel,
        "created_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source_commit_sha": args.source_commit, "payload_file": payload_out.name,
        "payload_format": args.payload_format, "payload_schema": args.payload_schema,
        "integrity_mode": "sha256", "payload_sha256": sha256(payload_out),
        "payload_size_bytes": payload_out.stat().st_size,
        "chemical_count": args.chemical_count, "source_count": args.source_count,
        "model_count": args.model_count, "calibration_count": args.calibration_count,
        "min_app_version": args.min_app_version, "max_app_version": args.max_app_version,
        "feature_schema_version": args.feature_schema, "data_revision": args.data_revision,
        "evidence_revision": args.evidence_revision,
        "sampling_profile_compatibility": sorted(set(args.sampling_profile)),
    }

    manifest_path = out / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    audit = {"bundle_sha256": sha256(payload_out), "manifest_sha256": sha256(manifest_path),
             "payload": payload_out.name, "manifest": manifest_path.name,
             "integrity_mode": "sha256", "data_revision": args.data_revision, "evidence_revision": args.evidence_revision}
    (out / "publication_audit.json").write_text(json.dumps(audit, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2)); return 0


if __name__ == "__main__": raise SystemExit(main())
