#!/usr/bin/env python3
"""Offline integrity audit for HazRob 0.10.2 / HR069 chemical evidence coverage.

No network access is required. The audit ties the 144-entry legacy catalogue, the pinned
652-record NIOSH identity seed, the external MQ/MOS chemical-evidence registry, and the
external fingerprint-dataset provenance registry together while asserting that no external
source is allowed to claim HazRob M4 status or to be double-counted as independent evidence.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/servoc/app/chemical/ChemicalCatalog.java"
GAS_ENGINE = ROOT / "app/src/main/java/com/servoc/app/chemical/GasEngine.java"
NIOSH = ROOT / "app/src/main/assets/generated/niosh/niosh_chemicals.json"
NIOSH_MANIFEST = ROOT / "app/src/main/assets/generated/niosh/manifest.json"
REGISTRY = ROOT / "research_data/chemical_coverage_hr069/evidence_registry.json"
DATASET_REGISTRY = ROOT / "research_data/external_mq_fingerprints/manifest.json"


def fail(msg: str) -> None:
    raise SystemExit("HR069 CHEMICAL COVERAGE AUDIT FAILED: " + msg)


def java_string_array(text: str, symbol: str) -> list[str]:
    m = re.search(r"public\s+static\s+final\s+String\[\]\s+"+re.escape(symbol)+r"\s*=\s*\{(.*?)\};", text, re.S)
    if not m:
        fail(f"cannot parse ChemicalCatalog.{symbol}")
    vals = re.findall(r'"((?:\\.|[^"\\])*)"', m.group(1))
    # The current legacy catalogue uses ASCII formula/name strings. Avoid unicode_escape
    # because it can silently corrupt future non-ASCII labels.
    return [v.replace('\\"','"').replace('\\\\','\\') for v in vals]


def legacy_names() -> list[str]:
    return java_string_array(CATALOG.read_text(encoding="utf-8"), "NAMES")


def gas_engine_supported_ids() -> set[int]:
    text = GAS_ENGINE.read_text(encoding="utf-8")
    return {int(x) for x in re.findall(r"new\s+Curve\(MQ(?:2|3|4|5|7|8|9|135)\s*,\s*(\d+)\s*,", text)}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="fail on any contract violation")
    args = ap.parse_args()

    names = legacy_names()
    niosh = json.loads(NIOSH.read_text(encoding="utf-8"))
    niosh_manifest = json.loads(NIOSH_MANIFEST.read_text(encoding="utf-8"))
    reg = json.loads(REGISTRY.read_text(encoding="utf-8"))
    datasets = json.loads(DATASET_REGISTRY.read_text(encoding="utf-8"))

    records = niosh.get("records", [])
    cas_set = {str(r.get("cas", "")).strip() for r in records if str(r.get("cas", "")).strip()}
    cross = reg.get("chemical_crosswalk", [])
    pure_cross = reg.get("legacy_sensor_crosswalk", [])
    curves = reg.get("preferred_mq135_curves", [])
    sources = reg.get("sources", [])
    dataset_sources = datasets.get("sources", [])

    source_ids = {s.get("id") for s in sources}
    cross_ids = {int(x["legacy_id"]) for x in cross}
    pure_ids = {int(x["legacy_id"]) for x in pure_cross}
    niosh_matches = [x for x in cross if str(x.get("cas", "")).strip() in cas_set]
    pure_niosh_matches = [x for x in pure_cross if str(x.get("cas", "")).strip() in cas_set]
    gas_ids = gas_engine_supported_ids()

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
    if len(gas_ids) != 13:
        problems.append(f"GasEngine supported chemical count {len(gas_ids)} != pinned 13")

    def validate_crosswalk(rows: list[dict], label: str, require_cas: bool) -> None:
        seen: set[int] = set()
        for row in rows:
            idx = int(row["legacy_id"])
            if idx in seen:
                problems.append(f"duplicate {label} legacy_id {idx}")
            seen.add(idx)
            if not (0 <= idx < len(names)):
                problems.append(f"{label} legacy_id {idx} out of range")
                continue
            if names[idx] != row.get("name"):
                problems.append(f"{label} name mismatch for {idx}: {row.get('name')!r} != {names[idx]!r}")
            cas = str(row.get("cas", "")).strip()
            if require_cas and not cas:
                problems.append(f"{label} {idx} missing CAS")
            if cas and cas not in cas_set:
                problems.append(f"{label} {idx} CAS {cas} absent from pinned NIOSH seed")

    validate_crosswalk(cross, "external crosswalk", False)
    validate_crosswalk(pure_cross, "legacy sensor crosswalk", True)

    for row in cross:
        for sid in row.get("sources", []):
            if sid not in source_ids:
                problems.append(f"crosswalk {row['legacy_id']} references unknown source {sid}")

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

    expected_gas_ids = {20,21,23,34,75,76,78,84,95,103,110,135,139}
    if gas_ids != expected_gas_ids:
        problems.append(f"GasEngine supported IDs {sorted(gas_ids)} != pinned {sorted(expected_gas_ids)}")

    expected_pure_ids = {20,21,23,34,75,76,78,79,84,95,102,103,105,110,115}
    if pure_ids != expected_pure_ids:
        problems.append(f"legacy sensor crosswalk IDs {sorted(pure_ids)} != expected {sorted(expected_pure_ids)}")

    # Duplicate fingerprint provenance contract: original MultimodalGasData counts once;
    # the later GitHub/Zenodo copy is retained for audit but contributes zero independent weight.
    ds = {d.get("id"): d for d in dataset_sources}
    canonical = ds.get("MQDS-004", {})
    probable_copy = ds.get("MQDS-001", {})
    if canonical.get("independent_evidence_weight") != 1:
        problems.append("MQDS-004 canonical MultimodalGasData must have independent_evidence_weight=1")
    if probable_copy.get("independent_evidence_weight") != 0:
        problems.append("MQDS-001 probable derivative must have independent_evidence_weight=0")
    if probable_copy.get("possible_parent_source") != "MQDS-004":
        problems.append("MQDS-001 probable derivative must reference MQDS-004 as possible parent")

    report = {
        "release": reg.get("release"),
        "hardware_revision": reg.get("hardware_revision"),
        "legacy_catalogue_records": len(names),
        "niosh_identity_records": len(records),
        "gas_engine_curve_chemicals": len(gas_ids),
        "gas_engine_curve_ids": sorted(gas_ids),
        "external_sources_registered": len(sources),
        "legacy_chemicals_with_verified_external_evidence": len(cross_ids),
        "external_crosswalk_entries_found_in_niosh_by_cas": len(niosh_matches),
        "pure_sensor_relevant_legacy_chemicals_crosslinked_to_niosh": len(pure_niosh_matches),
        "pure_sensor_relevant_crosswalk_size": len(pure_cross),
        "peer_reviewed_preferred_mq135_curves": len(curves),
        "preferred_curve_legacy_ids": sorted(seen_curve_ids),
        "canonical_multimodal_dataset_weight": canonical.get("independent_evidence_weight"),
        "probable_derivative_dataset_weight": probable_copy.get("independent_evidence_weight"),
        "external_m4_allowed": reg.get("policy", {}).get("external_m4_allowed"),
        "problems": problems,
    }
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))

    if args.check and problems:
        fail("; ".join(problems))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
