#!/usr/bin/env python3
"""Fetch pinned/authoritative external MQ research datasets for HazRob 0.10.2 / HR069.

This utility is intentionally separate from the Android runtime. It downloads only research data and
never promotes third-party measurements to HazRob quantitative/M4 evidence.

Default behavior downloads sources whose redistribution/license status was explicitly verified in the
registry. MQDS-002 (Zenodo Food Freshness E-Nose) requires --include-unverified-license because the
verified Zenodo record rendered a blank license field at review time, even though the archive is public.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import shutil
import sys
import urllib.request
from datetime import datetime, timezone

ROOT = pathlib.Path(__file__).resolve().parent
RAW = ROOT / "raw"

SOURCES = {
    "MQDS-001": {
        "url": "https://raw.githubusercontent.com/TakMashhido/Gas-Sensors-Measurements-Dataset/a4f36dbcdcc650b9f7c7d105846ea1b6d69f76d4/Gas_Sensors_Measurements.csv",
        "filename": "Gas_Sensors_Measurements.csv",
        "license_verified": True,
        "expected_md5": None,
        "provenance": {
            "upstream_commit": "a4f36dbcdcc650b9f7c7d105846ea1b6d69f76d4",
            "doi": "10.5281/zenodo.13283720",
            "license": "MIT",
        },
    },
    "MQDS-002": {
        "url": "https://zenodo.org/records/17285312/files/Food-Freshness-E-Nose-Dataset.zip?download=1",
        "filename": "Food-Freshness-E-Nose-Dataset.zip",
        "license_verified": False,
        "expected_md5": "008b8ccb62d7a7d51ff9ffcfd698a1e5",
        "provenance": {
            "doi": "10.5281/zenodo.17285312",
            "version": "v1",
            "license": "UNVERIFIED_IN_REVIEWED_RECORD",
        },
    },
    "MQDS-003": {
        "url": "https://archive.ics.uci.edu/static/public/1081/gas%2Bsensor%2Barray%2Blow-concentration.zip",
        "filename": "gas_sensor_array_low_concentration.zip",
        "license_verified": True,
        "expected_md5": None,
        "provenance": {
            "doi": "10.24432/C5CK6F",
            "license": "CC BY 4.0",
        },
    },
}


def digest(path: pathlib.Path, name: str) -> str:
    h = hashlib.new(name)
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def fetch(source_id: str, spec: dict, include_unverified_license: bool, force: bool) -> None:
    if not spec["license_verified"] and not include_unverified_license:
        print(f"SKIP {source_id}: license/redistribution status is not yet explicitly verified")
        return

    dest_dir = RAW / source_id
    dest_dir.mkdir(parents=True, exist_ok=True)
    dest = dest_dir / spec["filename"]
    tmp = dest.with_suffix(dest.suffix + ".tmp")

    if dest.exists() and not force:
        print(f"KEEP {source_id}: {dest} already exists; use --force to refetch")
    else:
        req = urllib.request.Request(
            spec["url"],
            headers={"User-Agent": "HazRob-HR069-research-source-fetcher/1.0"},
        )
        print(f"FETCH {source_id}: {spec['url']}")
        try:
            with urllib.request.urlopen(req, timeout=120) as response, tmp.open("wb") as out:
                shutil.copyfileobj(response, out)
            tmp.replace(dest)
        finally:
            if tmp.exists():
                tmp.unlink()

    md5 = digest(dest, "md5")
    sha256 = digest(dest, "sha256")
    expected = spec.get("expected_md5")
    if expected and md5.lower() != expected.lower():
        raise RuntimeError(
            f"{source_id}: MD5 mismatch; expected {expected}, got {md5}. "
            "Do not use this download until provenance is resolved."
        )

    record = {
        "source_id": source_id,
        "source_url": spec["url"],
        "filename": spec["filename"],
        "retrieved_utc": datetime.now(timezone.utc).isoformat(),
        "bytes": dest.stat().st_size,
        "md5": md5,
        "sha256": sha256,
        "license_verified": spec["license_verified"],
        "provenance": spec["provenance"],
        "hazrob_evidence_class": "RESEARCH_EXTERNAL",
        "authoritative_quantification_allowed": False,
    }
    (dest_dir / "FETCHED_SOURCE.json").write_text(
        json.dumps(record, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    print(f"OK   {source_id}: sha256={sha256}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", nargs="*", choices=sorted(SOURCES), help="source IDs; default: all")
    parser.add_argument(
        "--include-unverified-license",
        action="store_true",
        help="allow fetching public records whose redistribution/license field was not explicitly verified",
    )
    parser.add_argument("--force", action="store_true", help="replace an existing local raw download")
    args = parser.parse_args()

    selected = args.source or list(SOURCES)
    for source_id in selected:
        fetch(source_id, SOURCES[source_id], args.include_unverified_license, args.force)
    return 0


if __name__ == "__main__":
    sys.exit(main())
