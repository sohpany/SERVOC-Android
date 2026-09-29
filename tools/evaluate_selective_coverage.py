#!/usr/bin/env python3
"""HazRob selective-classification coverage evaluator.

Calibrates a confidence threshold on an explicitly labelled CALIBRATION split and
reports accepted-sample accuracy together with coverage on a separate VALIDATION
split. This is a research/qualification aid; it never changes runtime thresholds.

CSV columns:
  split,session_id,confidence,correct
where split is calibration or validation, confidence is 0..1, and correct is
0/1, true/false, yes/no.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
from pathlib import Path

SCHEMA = "hazrob-selective-coverage/1"


def truth(v: str):
    s = str(v).strip().lower()
    if s in {"1", "true", "yes", "y", "correct"}:
        return True
    if s in {"0", "false", "no", "n", "incorrect"}:
        return False
    raise ValueError(f"invalid correct value: {v}")


def wilson(k: int, n: int, z: float = 1.959963984540054):
    if n <= 0:
        return float("nan"), float("nan")
    p = k / n
    den = 1 + z * z / n
    center = (p + z * z / (2 * n)) / den
    half = z * math.sqrt((p * (1 - p) + z * z / (4 * n)) / n) / den
    return max(0.0, center - half), min(1.0, center + half)


def metrics(rows, threshold):
    accepted = [r for r in rows if r["confidence"] >= threshold]
    n, a = len(rows), len(accepted)
    correct = sum(1 for r in accepted if r["correct"])
    lo, hi = wilson(correct, a)
    return {
        "total": n,
        "accepted": a,
        "coverage": a / n if n else 0.0,
        "accepted_accuracy": correct / a if a else None,
        "accuracy_wilson95_low": lo if a else None,
        "accuracy_wilson95_high": hi if a else None,
        "independent_sessions_total": len({r["session_id"] for r in rows}),
        "independent_sessions_accepted": len({r["session_id"] for r in accepted}),
    }


def load(path: Path):
    out = {"calibration": [], "validation": []}
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        rd = csv.DictReader(f)
        required = {"split", "session_id", "confidence", "correct"}
        if not required.issubset(set(rd.fieldnames or [])):
            raise ValueError("CSV requires split,session_id,confidence,correct")
        for i, row in enumerate(rd, start=2):
            split = str(row["split"]).strip().lower()
            if split not in out:
                raise ValueError(f"line {i}: split must be calibration or validation")
            sid = str(row["session_id"]).strip()
            if not sid:
                raise ValueError(f"line {i}: session_id required")
            try:
                c = float(row["confidence"])
            except Exception as exc:
                raise ValueError(f"line {i}: invalid confidence") from exc
            if not math.isfinite(c) or c < 0 or c > 1:
                raise ValueError(f"line {i}: confidence must be within 0..1")
            out[split].append({"session_id": sid, "confidence": c, "correct": truth(row["correct"])})
    return out


def choose_threshold(rows, min_accuracy_lower_bound, min_coverage):
    candidates = sorted({0.0, 1.0, *(r["confidence"] for r in rows)})
    qualified = []
    curve = []
    for t in candidates:
        m = metrics(rows, t)
        m["threshold"] = t
        curve.append(m)
        lo = m["accuracy_wilson95_low"]
        if lo is not None and lo >= min_accuracy_lower_bound and m["coverage"] >= min_coverage:
            qualified.append(m)
    # Maximize coverage first; for ties choose the stricter threshold.
    if not qualified:
        return None, curve
    qualified.sort(key=lambda m: (-m["coverage"], -m["threshold"]))
    return qualified[0], curve


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("csv", type=Path)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--min-accuracy-lower-bound", type=float, default=0.80)
    p.add_argument("--min-coverage", type=float, default=0.25)
    p.add_argument("--min-calibration-sessions", type=int, default=3)
    p.add_argument("--min-validation-sessions", type=int, default=3)
    args = p.parse_args()

    if not (0 < args.min_accuracy_lower_bound <= 1):
        raise ValueError("--min-accuracy-lower-bound must be in (0,1]")
    if not (0 < args.min_coverage <= 1):
        raise ValueError("--min-coverage must be in (0,1]")

    ds = load(args.csv)
    cal, val = ds["calibration"], ds["validation"]
    failures, warnings = [], []
    cal_sessions = len({r["session_id"] for r in cal})
    val_sessions = len({r["session_id"] for r in val})
    if cal_sessions < max(1, args.min_calibration_sessions):
        failures.append(f"calibration split has {cal_sessions} independent sessions")
    if val_sessions < max(1, args.min_validation_sessions):
        failures.append(f"validation split has {val_sessions} independent sessions")
    if not cal:
        failures.append("calibration split is empty")
    if not val:
        failures.append("validation split is empty")

    selected, curve = choose_threshold(cal, args.min_accuracy_lower_bound, args.min_coverage) if cal else (None, [])
    if selected is None:
        failures.append("no calibration threshold satisfies the requested lower-bound accuracy and coverage")
        validation = None
        threshold = None
    else:
        threshold = selected["threshold"]
        validation = metrics(val, threshold) if val else None
        if validation is not None:
            lo = validation["accuracy_wilson95_low"]
            if lo is None or lo < args.min_accuracy_lower_bound:
                warnings.append("held-out validation accuracy lower bound does not meet the requested target")
            if validation["coverage"] < args.min_coverage:
                warnings.append("held-out validation coverage is below the requested minimum")

    ready = not failures and validation is not None
    validation_pass = bool(
        ready
        and validation["accuracy_wilson95_low"] is not None
        and validation["accuracy_wilson95_low"] >= args.min_accuracy_lower_bound
        and validation["coverage"] >= args.min_coverage
    )
    out = {
        "schema": SCHEMA,
        "state": "HELDOUT_CRITERIA_MET" if validation_pass else ("EVALUATED_NOT_MET" if ready else "INSUFFICIENT_EVIDENCE"),
        "runtime_activation_eligible": False,
        "threshold": threshold,
        "criteria": {
            "min_accuracy_wilson95_low": args.min_accuracy_lower_bound,
            "min_coverage": args.min_coverage,
        },
        "calibration_selection": selected,
        "heldout_validation": validation,
        "calibration_curve": curve,
        "scientific_contract": {
            "threshold_selected_only_on_calibration_split": True,
            "validation_split_is_held_out": True,
            "accuracy_reported_with_coverage": True,
            "confidence_is_assumed_precomputed_not_recalibrated_here": True,
            "note": "Passing this evaluator is validation evidence only; it does not grant M4 or change runtime thresholds automatically.",
        },
        "failures": failures,
        "warnings": warnings,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps({"state": out["state"], "threshold": threshold, "failures": len(failures)}, ensure_ascii=False))
    return 0 if validation_pass else 2


if __name__ == "__main__":
    raise SystemExit(main())
