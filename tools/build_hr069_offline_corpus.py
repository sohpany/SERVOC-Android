#!/usr/bin/env python3
"""Bundle the *current approved repository* HR069 research sources into Android assets.

This packages authentic pinned scientific records only. The offline SQLite import
is a provenance catalog and cannot promote literature priors to calibrated M4.
No network calls, no mutation of the source corpus, deterministic output.
"""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "research_data" / "hr069_analytical_library"
EXPECTED_FILES = (
    "1_propanol_sjtu_mq_transfer_evidence_v1.json",
    "ethylene_mq3_transfer_evidence_v1.json",
    "food_freshness_headspace_fingerprints_v1.json",
    "identification_fingerprint_priors_v1.json",
    "kononov_mox_numeric_transfer_batch_v1.json",
    "methanol_mq_transfer_evidence_v1.json",
    "mq_experiment_source_index_v1.json",
    "mq_models_76.json",
    "mq_research_curves_43.json",
    "multimodal_mq_transfer_fingerprints_v1.json",
    "pubchem_resolution_delta_v1.json",
    "robofusion_hr069_gasleak_operational_transfer_v1.json",
    "same_sensor_peer_reviewed_identity_evidence_v1.json",
    "uci_mq135_lowconcentration_transfer_fingerprints_v1.json",
)
SCHEMA = "hazrob-chemcore-offline-research-corpus/1"


def build(out_dir: Path):
    actual = set(p.name for p in SOURCE.glob("*.json"))
    if actual != set(EXPECTED_FILES):
        raise ValueError("HR069 research corpus changed; review each addition/removal before rebuilding: " + repr(sorted(actual ^ set(EXPECTED_FILES))))
    out_dir.mkdir(parents=True, exist_ok=True)
    files = []
    for name in EXPECTED_FILES:
        raw = (SOURCE / name).read_bytes()
        if not raw or len(raw) > 512_000:
            raise ValueError(f"invalid source size: {name}")
        decoded = json.loads(raw)
        if not isinstance(decoded, dict) or not str(decoded.get("schema", "")):
            raise ValueError(f"missing scientific source schema: {name}")
        (out_dir / name).write_bytes(raw)
        files.append(dict(name=name, sha256=hashlib.sha256(raw).hexdigest(), bytes=len(raw), source_schema=decoded["schema"]))
    manifest = dict(schema=SCHEMA, release="0.10.2", hardware_revision="HR069",
                    evidence_scope="EXTERNAL_RESEARCH_NON_M4", m4_promotion=False,
                    runtime_calibration_override=False,
                    authoritative_repository="sohpany/SERVOC-Android",
                    authoritative_branch="release/hazrob-0.10.2-rc2",
                    authoritative_root="research_data/hr069_analytical_library",
                    source_count=len(files), sources=files)
    data = (json.dumps(manifest, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")
    (out_dir / "manifest.json").write_bytes(data)
    print(json.dumps({"schema": SCHEMA, "sources": len(files), "sha256_manifest": hashlib.sha256(data).hexdigest(), "total_bytes": sum(x["bytes"] for x in files)}))
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()
    build(args.out_dir)
