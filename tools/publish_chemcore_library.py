#!/usr/bin/env python3
"""Build a signed immutable ChemCore library publication bundle.

The private publication key is read from a file supplied at runtime and is never written to the
repository. Stable manifests use SHA256withRSA over the same canonical byte contract implemented
by ChemCoreLibraryManifest.java.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
from datetime import datetime, timezone
from pathlib import Path


def canonical_bytes(m: dict) -> bytes:
    sampling = sorted(m.get("sampling_profile_compatibility", []))
    rows = [
        ("library_version", m["library_version"]),
        ("schema_version", str(m["schema_version"])),
        ("channel", m["channel"].lower()),
        ("created_utc", m["created_utc"]),
        ("source_commit_sha", m["source_commit_sha"]),
        ("payload_file", m["payload_file"]),
        ("payload_format", m["payload_format"].lower()),
        ("payload_sha256", m["payload_sha256"].lower()),
        ("payload_size_bytes", str(m["payload_size_bytes"])),
        ("chemical_count", str(m["chemical_count"])),
        ("source_count", str(m["source_count"])),
        ("model_count", str(m["model_count"])),
        ("calibration_count", str(m["calibration_count"])),
        ("min_app_version", m["min_app_version"]),
        ("max_app_version", m.get("max_app_version", "")),
        ("feature_schema_version", m["feature_schema_version"]),
        ("sampling_profile_compatibility", ",".join(sampling)),
        ("signature_algorithm", m["signature_algorithm"]),
    ]
    text = "chemcore-manifest-v1\n" + "".join(f"{k}={v}\n" for k, v in rows)
    return text.encode("utf-8")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def sign_rsa_sha256(data: bytes, key_path: Path) -> str:
    with tempfile.TemporaryDirectory(prefix="chemcore-sign-") as td:
        raw = Path(td) / "canonical.txt"
        sig = Path(td) / "signature.bin"
        raw.write_bytes(data)
        subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(key_path), "-out", str(sig), str(raw)],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        return base64.b64encode(sig.read_bytes()).decode("ascii")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--payload", required=True)
    p.add_argument("--out-dir", required=True)
    p.add_argument("--library-version", required=True)
    p.add_argument("--source-commit", required=True)
    p.add_argument("--channel", choices=["stable", "research"], default="stable")
    p.add_argument("--payload-format", choices=["json", "sqlite"], default="json")
    p.add_argument("--min-app-version", required=True)
    p.add_argument("--max-app-version", default="")
    p.add_argument("--feature-schema", required=True)
    p.add_argument("--sampling-profile", action="append", default=[])
    p.add_argument("--chemical-count", type=int, required=True)
    p.add_argument("--source-count", type=int, default=0)
    p.add_argument("--model-count", type=int, default=0)
    p.add_argument("--calibration-count", type=int, default=0)
    p.add_argument("--private-key", default="")
    args = p.parse_args()

    payload = Path(args.payload).resolve()
    if not payload.is_file():
        raise SystemExit(f"payload missing: {payload}")
    out = Path(args.out_dir).resolve()
    out.mkdir(parents=True, exist_ok=True)
    payload_out = out / payload.name
    shutil.copy2(payload, payload_out)

    if any(x < 0 for x in (args.chemical_count, args.source_count, args.model_count, args.calibration_count)):
        raise SystemExit("record counts must be non-negative")

    manifest = {
        "library_version": args.library_version,
        "schema_version": 1,
        "channel": args.channel,
        "created_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source_commit_sha": args.source_commit,
        "payload_file": payload_out.name,
        "payload_format": args.payload_format,
        "payload_sha256": sha256(payload_out),
        "payload_size_bytes": payload_out.stat().st_size,
        "chemical_count": args.chemical_count,
        "source_count": args.source_count,
        "model_count": args.model_count,
        "calibration_count": args.calibration_count,
        "min_app_version": args.min_app_version,
        "max_app_version": args.max_app_version,
        "feature_schema_version": args.feature_schema,
        "sampling_profile_compatibility": sorted(set(args.sampling_profile)),
        "signature_algorithm": "SHA256withRSA" if args.channel == "stable" else "",
        "signature": "",
    }

    if args.channel == "stable":
        key_value = args.private_key.strip()
        if not key_value:
            raise SystemExit("stable publication requires --private-key; use a CI secret-backed temporary file")
        key = Path(key_value).resolve()
        if not key.is_file():
            raise SystemExit(f"private key missing: {key}")
        manifest["signature"] = sign_rsa_sha256(canonical_bytes(manifest), key)

    manifest_path = out / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    audit = {
        "bundle_sha256": sha256(payload_out),
        "manifest_sha256": sha256(manifest_path),
        "payload": payload_out.name,
        "manifest": manifest_path.name,
    }
    (out / "publication_audit.json").write_text(json.dumps(audit, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
