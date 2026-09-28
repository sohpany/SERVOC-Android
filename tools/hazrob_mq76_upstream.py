#!/usr/bin/env python3
"""
HazRob MQ76 pinned research bridge.

Runs the complete 76-model MQDataScience research catalogue without embedding
third-party code into the Android runtime. The exact upstream PredictData.py is
loaded from the pinned commit and verified against its Git blob SHA-1 before use.

This is a developer/research tool. Its output is model-development evidence only
and must not bypass HazRob M4 calibration, open-set, domain, or reportability gates.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
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
            if not (tv == tv and yv == yv):
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
    best_cv_r2, best_surface, best_name, statuses = runner.run()

    success = sum(1 for v in statuses.values() if v.get("Status") == "Success")
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
            "expected_models": 76,
            "reported_models": len(statuses),
            "successful_models": success,
            "failed_models": len(statuses) - success,
        },
        "selection": {
            "best_model": best_name,
            "best_cv_r2": float(best_cv_r2),
            "warning": (
                "Research ranking only. Highest CV R2 is not sufficient for M4 calibration, "
                "identity, uncertainty, or occupational quantitative reporting."
            ),
        },
        "model_status": statuses,
        "surface": {
            "x": [float(v) for v in surface],
            "prediction": [float(v) for v in best_surface],
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(json.dumps(result["catalogue"], ensure_ascii=False))
    print(f"Best research model: {best_name} | CV_R2={best_cv_r2}")
    print(f"Wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
