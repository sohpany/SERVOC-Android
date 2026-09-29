#!/usr/bin/env python3
"""Validate a controlled HazRob binary/ternary gas-mixture research campaign.

The validator is intentionally research-only. Published MOS-array work shows that
mixture responses can depart materially from single-gas/power-law behavior, so this
tool requires pure-component controls and replicated mixture observations instead
of assuming additive pure-gas responses.

It validates experimental design/provenance only; it does not infer components,
concentrations, M4 status, or occupational reportability.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import hashlib
import json
import math
from pathlib import Path

SCHEMA = "hazrob-mixture-campaign/1"
OUTPUT_SCHEMA = "hazrob-mixture-campaign-validation/1"
ALLOWED_KINDS = {"blank", "pure", "mixture", "recovery"}


def finite_pos(v):
    return isinstance(v, (int, float)) and math.isfinite(float(v)) and float(v) > 0


def finite(v):
    return isinstance(v, (int, float)) and math.isfinite(float(v))


def sha256(data: bytes):
    return hashlib.sha256(data).hexdigest()


def component_id(row):
    if not isinstance(row, dict):
        return ""
    return str(row.get("chemical_uid") or row.get("cas") or row.get("chemical") or "").strip()


def composition_signature(components):
    pairs = []
    for c in components or []:
        cid = component_id(c)
        ppm = c.get("ppm") if isinstance(c, dict) else None
        if cid and finite_pos(ppm):
            pairs.append((cid, round(float(ppm), 9)))
    return tuple(sorted(pairs))


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("campaign", type=Path)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--min-pure-replicates", type=int, default=3)
    p.add_argument("--min-mixture-replicates", type=int, default=3)
    p.add_argument("--min-blanks", type=int, default=3)
    p.add_argument("--require-environment", action="store_true")
    args = p.parse_args()

    raw = args.campaign.read_bytes()
    doc = json.loads(raw.decode("utf-8"))
    failures, warnings = [], []

    if doc.get("schema") != SCHEMA:
        failures.append(f"schema must be {SCHEMA}")
    if not str(doc.get("campaign_id") or "").strip():
        failures.append("campaign_id is required")
    if not str(doc.get("sampling_profile_id") or "").strip():
        failures.append("sampling_profile_id is required")
    if not str(doc.get("hardware_profile") or "").strip():
        failures.append("hardware_profile is required")

    declared = {component_id(c) for c in (doc.get("components") or [])}
    declared.discard("")
    if len(declared) < 2:
        failures.append("at least two declared chemical components are required")

    runs = doc.get("runs") or []
    if not isinstance(runs, list) or not runs:
        failures.append("runs must be a non-empty list")
        runs = []

    run_ids = set()
    kinds = Counter()
    pure_counts = Counter()
    mixture_counts = Counter()
    environment_complete = 0
    referenced_components = set()
    session_ids = set()

    for i, run in enumerate(runs):
        if not isinstance(run, dict):
            failures.append(f"run[{i}] is not an object")
            continue
        rid = str(run.get("run_id") or "").strip()
        if not rid:
            failures.append(f"run[{i}] missing run_id")
        elif rid in run_ids:
            failures.append(f"duplicate run_id: {rid}")
        else:
            run_ids.add(rid)

        sid = str(run.get("session_id") or "").strip()
        if not sid:
            failures.append(f"run[{i}] missing session_id")
        elif sid in session_ids:
            failures.append(f"session_id reused across runs: {sid}")
        else:
            session_ids.add(sid)

        kind = str(run.get("kind") or "").strip().lower()
        if kind not in ALLOWED_KINDS:
            failures.append(f"run[{i}] invalid kind: {kind}")
            continue
        kinds[kind] += 1

        comps = run.get("components") or []
        sig = composition_signature(comps)
        for cid, _ in sig:
            referenced_components.add(cid)
            if declared and cid not in declared:
                failures.append(f"run[{i}] uses undeclared component: {cid}")

        if kind in {"blank", "recovery"}:
            if sig:
                failures.append(f"run[{i}] {kind} must not declare positive-ppm components")
        elif kind == "pure":
            if len(sig) != 1:
                failures.append(f"run[{i}] pure run must contain exactly one positive-ppm component")
            else:
                pure_counts[sig[0][0]] += 1
        elif kind == "mixture":
            if len(sig) not in {2, 3}:
                failures.append(f"run[{i}] mixture must contain exactly two or three positive-ppm components")
            else:
                mixture_counts[sig] += 1

        t, rh = run.get("temperature_c"), run.get("rh_pct")
        if finite(t) and finite(rh):
            environment_complete += 1
        elif args.require_environment:
            failures.append(f"run[{i}] requires finite temperature_c and rh_pct")

        if not isinstance(run.get("replicate"), int) or run.get("replicate") < 1:
            failures.append(f"run[{i}] replicate must be an integer >=1")

    if kinds["blank"] < max(1, args.min_blanks):
        failures.append(f"at least {max(1,args.min_blanks)} independent blank runs are required")
    if kinds["recovery"] == 0:
        warnings.append("no recovery run is present; recovery/interference persistence cannot be characterized")

    for cid in sorted(declared):
        n = pure_counts[cid]
        if n < max(1, args.min_pure_replicates):
            failures.append(
                f"component {cid} has {n} pure controls; require >= {max(1,args.min_pure_replicates)}"
            )
    if not mixture_counts:
        failures.append("at least one replicated binary/ternary mixture composition is required")
    for sig, n in mixture_counts.items():
        if n < max(1, args.min_mixture_replicates):
            label = "+".join(f"{c}@{ppm:g}ppm" for c, ppm in sig)
            failures.append(
                f"mixture {label} has {n} replicates; require >= {max(1,args.min_mixture_replicates)}"
            )

    if referenced_components and declared and referenced_components != declared:
        missing = sorted(declared - referenced_components)
        if missing:
            failures.append("declared components never used in runs: " + ", ".join(missing))

    env_fraction = environment_complete / len(runs) if runs else 0.0
    if env_fraction < 1.0:
        warnings.append(
            f"environment metadata complete for {environment_complete}/{len(runs)} runs; temperature/RH domain analysis is incomplete"
        )

    ready = not failures
    out = {
        "schema": OUTPUT_SCHEMA,
        "state": "MIXTURE_RESEARCH_READY" if ready else "INVALID_CAMPAIGN",
        "training_eligible": ready,
        "runtime_activation_eligible": False,
        "m4_granted": False,
        "campaign_id": doc.get("campaign_id"),
        "sampling_profile_id": doc.get("sampling_profile_id"),
        "hardware_profile": doc.get("hardware_profile"),
        "source_sha256": sha256(raw),
        "counts": {
            "runs": len(runs),
            "blank": kinds["blank"],
            "pure": kinds["pure"],
            "mixture": kinds["mixture"],
            "recovery": kinds["recovery"],
            "declared_components": len(declared),
            "unique_mixture_compositions": len(mixture_counts),
            "environment_complete_fraction": env_fraction,
        },
        "pure_replicates": dict(sorted(pure_counts.items())),
        "mixture_replicates": [
            {
                "components": [{"chemical": c, "ppm": ppm} for c, ppm in sig],
                "replicates": n,
            }
            for sig, n in sorted(mixture_counts.items())
        ],
        "scientific_contract": {
            "assume_pure_gas_additivity": False,
            "mixture_models_are_research_only": True,
            "pure_component_controls_required": True,
            "independent_replicates_required": True,
            "note": (
                "MOS-array mixture responses may be non-additive. This design supports empirical mixture/interference modelling; "
                "it does not authorize decomposition or concentration reporting without independent HazRob validation."
            ),
        },
        "failures": failures,
        "warnings": warnings,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps({"state": out["state"], "failures": len(failures), "warnings": len(warnings)}, ensure_ascii=False))
    return 0 if ready else 2


if __name__ == "__main__":
    raise SystemExit(main())
