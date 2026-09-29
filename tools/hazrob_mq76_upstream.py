#!/usr/bin/env python3
"""
HazRob MQ76 pinned research bridge.

Runs the complete 76-model MQDataScience research catalogue without embedding
third-party code into the Android runtime. The exact upstream PredictData.py is
loaded from the pinned commit and verified against its Git blob SHA-1 before use.

This is a developer/research tool. Its output is model-development evidence only
and must not bypass HazRob M4 calibration, open-set, domain, or reportability gates.

The catalogue is role-aware. Models are retained according to their scientific use:
  1-11  environmental/time nuisance-response research
 12-49  curve/robust-regression research
 50-63  temporal-dynamics research
 64-76  machine-learning regression research

Cross-model spread emitted by this tool is an UNCALIBRATED disagreement diagnostic.
It is not a prediction interval and must never be promoted to measurement uncertainty
without independent HazRob coverage validation.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import urllib.request

UPSTREAM_REPOSITORY = "abcdaaaaaaaaa/MQDataScience"
UPSTREAM_COMMIT = "e88f876140587a708ee84a4d4d3acbd5720274ee"
UPSTREAM_PATH = "DataScience/Requirements/PredictData.py"
UPSTREAM_BLOB_SHA1 = "bacf95948714dbe76f09c57e3db14cbc60616580"
UPSTREAM_URL = (
    f"https://raw.githubusercontent.com/{UPSTREAM_REPOSITORY}/"
    f"{UPSTREAM_COMMIT}/{UPSTREAM_PATH}"
)
EXPECTED_MODELS = 76

ROLE_RANGES = (
    (1, 11, "ENVIRONMENTAL_NUISANCE", "time/temperature/RH nuisance-response and compensation research"),
    (12, 49, "CURVE_ROBUST_REGRESSION", "calibration-curve and robust/nonlinear response research"),
    (50, 63, "TEMPORAL_DYNAMICS", "response/recovery, stabilization and temporal-dynamics research"),
    (64, 76, "ML_REGRESSION", "machine-learning regression research"),
)


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def load_pinned_source(cache_dir: Path, offline_source: Path | None) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)
    target = cache_dir / "PredictData.py"
    if offline_source is not None:
        data = offline_source.read_bytes()
    elif target.exists():
        data = target.read_bytes()
        if git_blob_sha1(data) == UPSTREAM_BLOB_SHA1:
            return target
        target.unlink()
        data = b""
    else:
        data = b""

    if not data:
        with urllib.request.urlopen(UPSTREAM_URL, timeout=30) as r:
            data = r.read()

    actual = git_blob_sha1(data)
    if actual != UPSTREAM_BLOB_SHA1:
        raise RuntimeError(
            f"Pinned upstream blob mismatch: expected {UPSTREAM_BLOB_SHA1}, got {actual}"
        )
    target.write_bytes(data)
    return target


def import_predictdata(path: Path):
    spec = importlib.util.spec_from_file_location("hazrob_mq76_predictdata", path)
    if spec is None or spec.loader is None:
        raise RuntimeError("Cannot load pinned PredictData.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_csv(path: Path):
    t, y, temp, rh = [], [], [], []
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        fields = set(reader.fieldnames or [])
        required = {"time_s", "response"}
        if not required.issubset(fields):
            raise ValueError(
                "CSV requires columns: time_s,response; optional: temperature_c,rh_pct"
            )
        has_temp = "temperature_c" in fields
        has_rh = "rh_pct" in fields
        for row in reader:
            try:
                tv = float(row["time_s"])
                yv = float(row["response"])
            except (TypeError, ValueError):
                continue
            if not (math.isfinite(tv) and math.isfinite(yv)):
                continue
            t.append(tv)
            y.append(yv)
            if has_temp:
                try:
                    temp.append(float(row["temperature_c"]))
                except (TypeError, ValueError):
                    temp.append(float("nan"))
            if has_rh:
                try:
                    rh.append(float(row["rh_pct"]))
                except (TypeError, ValueError):
                    rh.append(float("nan"))
    if len(t) < 6:
        raise ValueError("At least 6 valid samples are required for the MQ76 research bridge")
    return t, y, (temp if has_temp else None), (rh if has_rh else None)


def role_for_priority(priority: int):
    for lo, hi, key, description in ROLE_RANGES:
        if lo <= priority <= hi:
            return key, description
    raise ValueError(f"Unexpected MQ76 priority: {priority}")


def finite_float(value):
    try:
        x = float(value)
    except (TypeError, ValueError):
        return None
    return x if math.isfinite(x) else None


def model_role_rankings(runner, statuses):
    roles = {
        key: {
            "priority_range": [lo, hi],
            "description": description,
            "successful_models": 0,
            "failed_models": 0,
            "ranked": [],
        }
        for lo, hi, key, description in ROLE_RANGES
    }
    for model in runner.models:
        priority = int(model.priority)
        key, _ = role_for_priority(priority)
        status = statuses.get(model.name, {})
        cv = finite_float(status.get("CV_R2"))
        if status.get("Status") == "Success" and cv is not None:
            roles[key]["successful_models"] += 1
            roles[key]["ranked"].append(
                {"priority": priority, "model": model.name, "cv_r2": cv}
            )
        else:
            roles[key]["failed_models"] += 1
    for value in roles.values():
        value["ranked"].sort(key=lambda x: (-x["cv_r2"], x["priority"]))
        value["best"] = value["ranked"][0] if value["ranked"] else None
    return roles


def _spread_summary(np, matrix):
    if matrix is None or len(matrix) < 2:
        return {
            "available": False,
            "successful_surfaces": 0 if matrix is None else len(matrix),
            "reason": "at least two finite model surfaces are required",
        }
    a = np.vstack(matrix)
    p10 = np.percentile(a, 10, axis=0)
    p50 = np.percentile(a, 50, axis=0)
    p90 = np.percentile(a, 90, axis=0)
    width = p90 - p10
    return {
        "available": True,
        "successful_surfaces": int(a.shape[0]),
        "p10": [float(v) for v in p10],
        "p50": [float(v) for v in p50],
        "p90": [float(v) for v in p90],
        "median_p90_minus_p10": float(np.median(width)),
        "max_p90_minus_p10": float(np.max(width)),
    }


def disagreement_diagnostic(np, runner, data, statuses):
    per_role = {key: [] for _, _, key, _ in ROLE_RANGES}
    all_surfaces = []
    failures = []
    for model in runner.models:
        status = statuses.get(model.name, {})
        if status.get("Status") != "Success":
            continue
        priority = int(model.priority)
        role, _ = role_for_priority(priority)
        try:
            # ModelRunner.run() ends each successful model fitted on the full dataset.
            pred = np.asarray(model.predict_surface(data), dtype=float).reshape(-1)
            if pred.size != data.t_surface.size or not np.all(np.isfinite(pred)):
                raise ValueError("non-finite or wrong-length prediction surface")
            per_role[role].append(pred)
            all_surfaces.append(pred)
        except Exception as exc:
            failures.append({"priority": priority, "model": model.name, "reason": str(exc)[:120]})

    roles = {key: _spread_summary(np, rows) for key, rows in per_role.items()}
    return {
        "kind": "UNCALIBRATED_MODEL_DISAGREEMENT",
        "reportable_prediction_interval": False,
        "m4_evidence": False,
        "warning": (
            "Percentile spread describes disagreement among successful research models on this trace. "
            "It is not a calibrated uncertainty or coverage interval and must not be used for occupational reporting."
        ),
        "all_successful_models": _spread_summary(np, all_surfaces),
        "by_role": roles,
        "surface_failures": failures,
    }


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("csv", type=Path)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--offline-source", type=Path)
    p.add_argument("--cache-dir", type=Path, default=Path(".cache/hazrob-mq76"))
    p.add_argument("--surface-points", type=int, default=200)
    args = p.parse_args()

    source = load_pinned_source(args.cache_dir, args.offline_source)
    mq = import_predictdata(source)

    import numpy as np

    t, y, temp, rh = read_csv(args.csv)
    t_np = np.asarray(t, dtype=float)
    y_np = np.asarray(y, dtype=float)
    temp_np = None if temp is None else np.asarray(temp, dtype=float)
    rh_np = None if rh is None else np.asarray(rh, dtype=float)

    # Fill sparse environmental values only for research continuity; record this fact.
    env_imputed = False
    for arr in (temp_np, rh_np):
        if arr is not None and np.any(~np.isfinite(arr)):
            finite = np.isfinite(arr)
            if not np.any(finite):
                arr[:] = 0.0
            else:
                idx = np.arange(arr.size)
                arr[~finite] = np.interp(idx[~finite], idx[finite], arr[finite])
            env_imputed = True

    surface_n = max(20, int(args.surface_points))
    surface = np.linspace(float(np.min(t_np)), float(np.max(t_np)), surface_n)
    data = mq.RegressionData(t_np, y_np, surface, temp_np, rh_np)
    runner = mq.ModelRunner(data)
    if len(runner.models) != EXPECTED_MODELS:
        raise RuntimeError(
            f"Pinned upstream catalogue changed unexpectedly: expected {EXPECTED_MODELS}, got {len(runner.models)}"
        )
    priorities = sorted(int(m.priority) for m in runner.models)
    if priorities != list(range(1, EXPECTED_MODELS + 1)):
        raise RuntimeError("Pinned upstream catalogue priorities are not the complete ordered 1..76 set")

    best_cv_r2, best_surface, best_name, statuses = runner.run()
    if len(statuses) != EXPECTED_MODELS:
        raise RuntimeError(
            f"Incomplete MQ76 execution status: expected {EXPECTED_MODELS}, got {len(statuses)}"
        )

    success = sum(1 for v in statuses.values() if v.get("Status") == "Success")
    role_rankings = model_role_rankings(runner, statuses)
    disagreement = disagreement_diagnostic(np, runner, data, statuses)

    result = {
        "schema": "hazrob-mq76-research/1",
        "source": {
            "repository": UPSTREAM_REPOSITORY,
            "commit": UPSTREAM_COMMIT,
            "path": UPSTREAM_PATH,
            "git_blob_sha1": UPSTREAM_BLOB_SHA1,
        },
        "input": {
            "samples": len(t),
            "temperature_supplied": temp is not None,
            "humidity_supplied": rh is not None,
            "environment_imputed": env_imputed,
        },
        "catalogue": {
            "expected_models": EXPECTED_MODELS,
            "reported_models": len(statuses),
            "successful_models": success,
            "failed_models": len(statuses) - success,
            "complete_priority_set": priorities == list(range(1, EXPECTED_MODELS + 1)),
        },
        "selection": {
            "best_model": best_name,
            "best_cv_r2": float(best_cv_r2),
            "warning": (
                "Research ranking only. Highest CV R2 is not sufficient for M4 calibration, "
                "identity, uncertainty, or occupational quantitative reporting."
            ),
        },
        "roles": role_rankings,
        "model_status": statuses,
        "surface": {
            "x": [float(v) for v in surface],
            "prediction": [float(v) for v in best_surface],
        },
        "model_disagreement_diagnostic": disagreement,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(json.dumps(result["catalogue"], ensure_ascii=False))
    print(f"Best research model: {best_name} | CV_R2={best_cv_r2}")
    for role, info in role_rankings.items():
        best = info.get("best")
        if best:
            print(f"{role}: {best['model']} | CV_R2={best['cv_r2']}")
        else:
            print(f"{role}: no successful model")
    print(f"Wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
