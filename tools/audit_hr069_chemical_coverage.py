#!/usr/bin/env python3
"""Offline integrity audit for HazRob 0.10.2 / HR069 chemical evidence coverage.

No network access is required. The audit ties the 144-entry legacy catalogue, the pinned
652-record NIOSH identity seed, and the external MQ/MOS evidence registry together while
asserting that no external source is allowed to claim HazRob M4 status.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/servoc/app/chemical/ChemicalCatalog.java"
NIOSH = ROOT / "app/src/main/assets/generated/niosh/niosh_chemicals.json"
NIOSH_MANIFEST = ROOT / "app/src/main/assets/generated/niosh/manifest.json"
REGISTRY = ROOT / "research_data/chemical_coverage_hr069/evidence_registry.json"


def fail(msg: str) -> None:
    raise SystemExit("HR069 CHEMICAL COVERAGE AUDIT FAILED: " + msg)


def legacy_names() -> list[str]:
    text = CATALOG.read_text(encoding="utf-8")
    m = re.search(r"public\s+static\s+final\s+String\[\]\s+NAMES\s*=\s*\{(.*?)\};", text, re.S)
    if not m:
        fail("cannot parse ChemicalCatalog.NAMES")
    # Catalogue strings are simple Java string literals; decode the minimal escape set used here.
    vals = re.findall(r'"((?:\\.|[^"\\])*)"', m.group(1))
    return [bytes(v, "utf-8").decode("unicode_escape") for v in vals]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="fail on any contract violation")
    args = ap.parse_args()

    names = legacy_names()
    niosh = json.loads(NIOSH.read_text(encoding="utf-8"))
    niosh_manifest = json.loads(NIOSH_MANIFEST.read_text(encoding="utf-8"))
    reg = json.loads(REGISTRY.read_text(encoding="utf-8"))

    records = niosh.get("records", [])
    cas_set = {str(r.get("cas", "")).strip() for r in records if str(r.get("cas", "")).strip()}
    cross = reg.get("chemical_crosswalk", [])
    curves = reg.get("preferred_mq135_curves", [])
    sources = reg.get("sources", [])

    source_ids = {s.get("id") for s in sources}
    cross_ids = {int(x["legacy_id"]) for x in cross}
    niosh_matches = [x for x in cross if str(x.get("cas", "")).strip() in cas_set]

    # Contract checks.
    problems: list[str] = []
    if len(names) != 144:
        problems.append(f"legacy catalogue count {len(names)} != 144")
    if int(niosh.get("record_count", -1)) != len(records):
        problems.append("NIOSH declared record_count does not equal records length")
    if len(records) != 652:
        problems.append(f"NIOSH seed count {len(records)} != pinned 652")
    if int(niosh_manifest.get("record_count", -1)) != 652:
        problems.append("NIOSH manifest does not declare 652 records")
    if reg.get("policy", {}).get("external_m4_allowed") is not False:
        problems.append("external_m4_allowed must remain false")
    if any(bool(s.get("m4")) for s in sources):
        problems.append("an external source claims m4=true")
    if len(curves) != 6:
        problems.append(f"preferred peer-reviewed MQ135 curve count {len(curves)} != 6")

    for row in cross:
        idx = int(row["legacy_id"])
        if not (0 <= idx < len(names)):
            problems.append(f"crosswalk legacy_id {idx} out of range")
            continue
        if names[idx] != row.get("name"):
            problems.append(f"crosswalk name mismatch for {idx}: {row.get('name')!r} != {names[idx]!r}")
        for sid in row.get("sources", []):
            if sid not in source_ids:
                problems.append(f"crosswalk {idx} references unknown source {sid}")

    seen_curve_ids: set[int] = set()
    for c in curves:
        idx = int(c["legacy_id"])
        seen_curve_ids.add(idx)
        if c.get("source") != "UMBU_MQ135_2023":
            problems.append(f"preferred curve {idx} is not pinned to UMBU_MQ135_2023")
        lo, hi = c.get("domain_ppm", [None, None])
        if lo != 10 or hi != 100:
            problems.append(f"preferred curve {idx} has unexpected published domain {c.get('domain_ppm')}")
        if not (float(c.get("a", 0)) > 0 and float(c.get("b", 0)) < 0):
            problems.append(f"preferred curve {idx} has invalid power-law coefficients")
        if idx not in cross_ids:
            problems.append(f"preferred curve {idx} is missing from external crosswalk")

    expected_curve_ids = {20, 21, 23, 95, 103, 110}
    if seen_curve_ids != expected_curve_ids:
        problems.append(f"preferred MQ135 curve IDs {sorted(seen_curve_ids)} != {sorted(expected_curve_ids)}")

    report = {
        "release": reg.get("release"),
        "hardware_revision": reg.get("hardware_revision"),
        "legacy_catalogue_records": len(names),
        "niosh_identity_records": len(records),
        "external_sources_registered": len(sources),
        "legacy_chemicals_with_verified_external_evidence": len(cross_ids),
        "external_crosswalk_entries_found_in_niosh_by_cas": len(niosh_matches),
        "peer_reviewed_preferred_mq135_curves": len(curves),
        "preferred_curve_legacy_ids": sorted(seen_curve_ids),
        "external_m4_allowed": reg.get("policy", {}).get("external_m4_allowed"),
        "problems": problems,
    }
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))

    if args.check and problems:
        fail("; ".join(problems))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
