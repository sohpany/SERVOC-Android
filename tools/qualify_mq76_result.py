#!/usr/bin/env python3
"""HazRob MQ76 research-result qualification gate.

Consumes ``hazrob-mq76-research/1`` output and emits an auditable candidate record.
The tool is deliberately fail-closed: it never activates a model and never upgrades
research evidence to M4. Independent/session-held-out evidence and scientific review
remain separate release gates.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path

EXPECTED_SCHEMA = "hazrob-mq76-research/1"
EXPECTED_REPOSITORY = "abcdaaaaaaaaa/MQDataScience"
EXPECTED_COMMIT = "e88f876140587a708ee84a4d4d3acbd5720274ee"
EXPECTED_BLOB = "bacf95948714dbe76f09c57e3db14cbc60616580"
EXPECTED_MODELS = 76
OUTPUT_SCHEMA = "hazrob-mq76-candidate/1"


def finite_number(v):
    return isinstance(v, (int, float)) and math.isfinite(float(v))


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def validate_research(doc: dict) -> list[str]:
    failures: list[str] = []
    if doc.get("schema") != EXPECTED_SCHEMA:
        failures.append("unexpected research schema")
    src = doc.get("source") or {}
    if src.get("repository") != EXPECTED_REPOSITORY:
        failures.append("upstream repository mismatch")
    if src.get("commit") != EXPECTED_COMMIT:
        failures.append("upstream commit mismatch")
    if src.get("git_blob_sha1") != EXPECTED_BLOB:
        failures.append("upstream PredictData.py blob mismatch")
    cat = doc.get("catalogue") or {}
    if cat.get("expected_models") != EXPECTED_MODELS:
        failures.append("expected model count is not 76")
    if cat.get("reported_models") != EXPECTED_MODELS:
        failures.append("research output does not report all 76 models")
    statuses = doc.get("model_status") or {}
    if len(statuses) != EXPECTED_MODELS:
        failures.append("model_status does not contain 76 entries")
    sel = doc.get("selection") or {}
    if not sel.get("best_model"):
        failures.append("best research model is missing")
    if not finite_number(sel.get("best_cv_r2")):
        failures.append("best_cv_r2 is missing or non-finite")
    return failures


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("research_json", type=Path)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--sensor-key", required=True)
    p.add_argument("--chemical", required=True, help="Stable chemical id/CAS/name used by the lab campaign")
    p.add_argument("--sampling-profile-id", default="")
    p.add_argument("--feature-schema", default="mq76-upstream-time-response/1")
    p.add_argument("--validity-domain", default="UNSPECIFIED_RESEARCH_DOMAIN")
    p.add_argument("--training-experiments", type=int, default=0)
    p.add_argument("--validation-experiments", type=int, default=0)
    p.add_argument("--blind-test-experiments", type=int, default=0)
    p.add_argument("--heldout-r2", type=float)
    p.add_argument("--heldout-log-rmse", type=float)
    p.add_argument("--heldout-mae", type=float)
    args = p.parse_args()

    raw = args.research_json.read_bytes()
    doc = json.loads(raw.decode("utf-8"))
    failures = validate_research(doc)
    selection = doc.get("selection") or {}
    inp = doc.get("input") or {}

    heldout = {
        "r2": args.heldout_r2,
        "log_rmse": args.heldout_log_rmse,
        "mae": args.heldout_mae,
    }
    heldout = {k: float(v) for k, v in heldout.items() if v is not None and math.isfinite(v)}

    warnings: list[str] = []
    if inp.get("environment_imputed"):
        warnings.append("temperature/RH contained imputed values; do not use as environmental validation")
    if not heldout:
        warnings.append("no independent held-out metrics supplied")
    if args.validation_experiments <= 0:
        warnings.append("no independent validation experiments declared")
    if args.blind_test_experiments <= 0:
        warnings.append("no blind-test experiments declared")
    if not args.sampling_profile_id:
        warnings.append("sampling profile is not bound")
    if args.validity_domain == "UNSPECIFIED_RESEARCH_DOMAIN":
        warnings.append("validity domain is not specified")

    structurally_valid = not failures
    qualification_ready = (
        structurally_valid
        and bool(heldout)
        and args.validation_experiments > 0
        and bool(args.sampling_profile_id)
        and args.validity_domain != "UNSPECIFIED_RESEARCH_DOMAIN"
    )

    # Even a qualification-ready candidate is intentionally inactive. M4 requires
    # accepted HazRob reference-gas calibration and independent release review.
    out = {
        "schema": OUTPUT_SCHEMA,
        "state": "QUALIFICATION_CANDIDATE" if qualification_ready else "RESEARCH_ONLY",
        "activation_eligible": False,
        "m4_granted": False,
        "sensor_key": args.sensor_key.lower(),
        "chemical": args.chemical,
        "model": {
            "name": selection.get("best_model", ""),
            "upstream_cv_r2": selection.get("best_cv_r2"),
            "feature_schema": args.feature_schema,
            "sampling_profile_id": args.sampling_profile_id,
            "validity_domain": args.validity_domain,
        },
        "experiment_counts": {
            "training": max(0, args.training_experiments),
            "validation": max(0, args.validation_experiments),
            "blind_test": max(0, args.blind_test_experiments),
        },
        "heldout_metrics": heldout,
        "source": {
            "research_artifact_sha256": sha256_bytes(raw),
            "repository": EXPECTED_REPOSITORY,
            "commit": EXPECTED_COMMIT,
            "git_blob_sha1": EXPECTED_BLOB,
            "all_76_reported": structurally_valid,
        },
        "gates": {
            "research_structure_valid": structurally_valid,
            "heldout_metrics_present": bool(heldout),
            "independent_validation_declared": args.validation_experiments > 0,
            "sampling_profile_bound": bool(args.sampling_profile_id),
            "validity_domain_declared": args.validity_domain != "UNSPECIFIED_RESEARCH_DOMAIN",
            "reference_gas_m4_gate": False,
            "scientific_review_gate": False,
        },
        "failures": failures,
        "warnings": warnings,
        "note": (
            "This artifact records MQ76 model-development evidence only. It cannot activate a runtime model, "
            "establish analyte identity, or authorize occupational ppm reporting."
        ),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps({"state": out["state"], "activation_eligible": False, "failures": len(failures)}, ensure_ascii=False))
    return 0 if structurally_valid else 2


if __name__ == "__main__":
    raise SystemExit(main())
