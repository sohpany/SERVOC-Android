#!/usr/bin/env python3
"""Build a provenance-rich ChemCore molecular descriptor seed from HazRob NIOSH identities.

The script uses CAS numbers already pinned in the HazRob NIOSH identity dataset and resolves
basic molecular/structural descriptors through the public PubChem PUG REST service. It is
intentionally separate from exposure-limit and sensor-response evidence.

Design goals:
- resumable local cache so a 600+ compound build does not restart after interruption;
- conservative retry/backoff and request pacing (PubChem asks clients to stay <=5 req/s);
- explicit unresolved records instead of silently dropping compounds;
- deterministic output ordering and SHA-256 manifest;
- current PubChem property names (SMILES, ConnectivitySMILES and Charge);
- curated name fallback only for a small, explicitly reviewed set of discrete pure compounds;
- no claim that a PubChem descriptor implies MQ detectability.

Typical use from repository root:
    python3 tools/build_chemcore_molecular_seed.py

For a smoke test:
    python3 tools/build_chemcore_molecular_seed.py --limit 10
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = ROOT / "app/src/main/assets/generated/niosh/niosh_chemicals.json"
DEFAULT_OUTPUT = ROOT / "app/src/main/assets/generated/chemcore/molecular_descriptors.json"
DEFAULT_MANIFEST = ROOT / "app/src/main/assets/generated/chemcore/manifest.json"
DEFAULT_CACHE = ROOT / ".cache/chemcore_pubchem"

PUBCHEM_BASE = "https://pubchem.ncbi.nlm.nih.gov/rest/pug"
PUBCHEM_SOURCE = "https://pubchem.ncbi.nlm.nih.gov/"
UA = "HazRob-ChemCore-molecular-seed/0.10.2 (+scientific research; provenance-preserving)"

# PubChem occasionally resolves a reviewed discrete compound reliably by canonical/common name
# even when the occupational CAS/name string does not produce a PUG REST property row. Fallbacks
# are deliberately explicit and limited to records already curated as PURE_COMPOUND. Never apply
# this mechanism to mixtures, materials, isomer groups, fumes, mists or occupational categories.
CURATED_NAME_FALLBACKS: dict[str, tuple[str, ...]] = {
    "10210-68-1": ("Dicobalt octacarbonyl", "Cobalt carbonyl"),
    "12079-65-1": ("Cyclopentadienyl manganese tricarbonyl",),
    "12108-13-3": ("Methylcyclopentadienylmanganese tricarbonyl", "Methyl cyclopentadienyl manganese tricarbonyl"),
    "1304-82-1": ("Bismuth telluride",),
    "17702-41-9": ("Decaborane",),
}

# Names follow the current PubChem PUG REST specification. PubChem calls net/formal
# molecular charge "Charge"; "FormalCharge" is not a valid PUG REST property tag.
PROPERTIES = [
    "MolecularFormula",
    "MolecularWeight",
    "SMILES",
    "ConnectivitySMILES",
    "InChI",
    "InChIKey",
    "IUPACName",
    "Title",
    "XLogP",
    "TPSA",
    "Complexity",
    "HBondDonorCount",
    "HBondAcceptorCount",
    "RotatableBondCount",
    "ExactMass",
    "MonoisotopicMass",
    "HeavyAtomCount",
    "Charge",
    "CovalentUnitCount",
    "DefinedAtomStereoCount",
    "UndefinedAtomStereoCount",
    "DefinedBondStereoCount",
    "UndefinedBondStereoCount",
]

NUMERIC_KEYS = {
    "MolecularWeight", "XLogP", "TPSA", "Complexity", "HBondDonorCount",
    "HBondAcceptorCount", "RotatableBondCount", "ExactMass", "MonoisotopicMass",
    "HeavyAtomCount", "Charge", "CovalentUnitCount", "DefinedAtomStereoCount",
    "UndefinedAtomStereoCount", "DefinedBondStereoCount", "UndefinedBondStereoCount",
}

UNITS = {
    "MolecularWeight": "g/mol",
    "ExactMass": "Da",
    "MonoisotopicMass": "Da",
    "TPSA": "A^2",
}


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_bytes(data)
    tmp.replace(path)


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def cache_path(cache_dir: Path, cas: str) -> Path:
    return cache_dir / (cas.replace("/", "_") + ".json")


def request_json(url: str, retries: int, backoff_s: float) -> dict[str, Any]:
    last: Exception | None = None
    for attempt in range(retries + 1):
        try:
            req = Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
            with urlopen(req, timeout=45) as response:
                return json.loads(response.read().decode("utf-8"))
        except HTTPError as e:
            last = e
            if e.code == 404:
                raise
            if e.code not in (408, 429, 500, 502, 503, 504) or attempt >= retries:
                raise
        except (URLError, TimeoutError, json.JSONDecodeError) as e:
            last = e
            if attempt >= retries:
                raise
        time.sleep(backoff_s * (2 ** attempt))
    assert last is not None
    raise last


def property_url(identifier: str) -> str:
    prop_path = ",".join(PROPERTIES)
    return f"{PUBCHEM_BASE}/compound/name/{quote(identifier, safe='')}/property/{prop_path}/JSON"


def query_identifier(identifier: str, retries: int, backoff_s: float) -> tuple[str, dict[str, Any] | None, str | None, str]:
    url = property_url(identifier)
    try:
        payload = request_json(url, retries=retries, backoff_s=backoff_s)
        rows = payload.get("PropertyTable", {}).get("Properties", [])
        if rows:
            return "RESOLVED", rows[0], None, url
        return "UNRESOLVED", None, "PubChem returned no property row", url
    except HTTPError as e:
        status = "UNRESOLVED" if e.code == 404 else "ERROR"
        return status, None, f"HTTP {e.code}: {e.reason}", url
    except Exception as e:
        return "ERROR", None, f"{type(e).__name__}: {e}", url


def resolve_pubchem(cas: str, cache_dir: Path, refresh: bool, retries: int,
                    backoff_s: float, delay_s: float) -> tuple[str, dict[str, Any] | None, str | None, str, str]:
    """Resolve by CAS first, then curated pure-compound names when explicitly configured.

    Returns status, property, error, resolution_method, resolution_identifier.
    """
    cp = cache_path(cache_dir, cas)
    fallbacks = CURATED_NAME_FALLBACKS.get(cas, ())
    if cp.exists() and not refresh:
        cached = load_json(cp)
        cached_status = cached.get("status", "ERROR")
        # A previously unresolved CAS cache must not suppress a newly added curated fallback.
        # Resolved/error-free records remain fully resumable.
        if cached_status == "RESOLVED" or not fallbacks:
            return (cached_status, cached.get("property"), cached.get("error"),
                    cached.get("resolution_method", "CAS"), cached.get("resolution_identifier", cas))

    attempts: list[dict[str, Any]] = []
    status, prop, error, url = query_identifier(cas, retries, backoff_s)
    attempts.append({"method": "CAS", "identifier": cas, "status": status, "error": error, "url": url})
    method = "CAS"; identifier = cas

    if status == "UNRESOLVED" and fallbacks:
        for fallback in fallbacks:
            s2, p2, e2, u2 = query_identifier(fallback, retries, backoff_s)
            attempts.append({"method": "CURATED_NAME_FALLBACK", "identifier": fallback, "status": s2, "error": e2, "url": u2})
            if s2 == "RESOLVED":
                status, prop, error = s2, p2, None
                method, identifier = "CURATED_NAME_FALLBACK", fallback
                break
            # A transient server/network failure should remain visible rather than silently becoming unresolved.
            if s2 == "ERROR":
                status, prop, error = s2, None, e2
                method, identifier = "CURATED_NAME_FALLBACK", fallback
                break

    cache_dir.mkdir(parents=True, exist_ok=True)
    cp.write_text(json.dumps({
        "cas": cas,
        "status": status,
        "property": prop,
        "error": error,
        "resolution_method": method,
        "resolution_identifier": identifier,
        "attempts": attempts,
        "retrieved_at_utc": utc_now(),
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    if delay_s > 0:
        time.sleep(delay_s)
    return status, prop, error, method, identifier


def finite_number(value: Any) -> float | int | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, float)) and math.isfinite(float(value)):
        return value
    try:
        x = float(value)
        return x if math.isfinite(x) else None
    except (TypeError, ValueError):
        return None


def normalize_property(prop: dict[str, Any] | None) -> dict[str, Any] | None:
    if not prop:
        return None
    out: dict[str, Any] = {}
    cid = prop.get("CID")
    if isinstance(cid, int):
        out["CID"] = cid
    for key in PROPERTIES:
        if key not in prop:
            continue
        value = prop[key]
        if key in NUMERIC_KEYS:
            n = finite_number(value)
            if n is None:
                continue
            out[key] = {"value": n, "unit": UNITS.get(key), "source_id": "PUBCHEM",
                        "method": "PubChem PUG REST property endpoint"}
        else:
            out[key] = {"value": str(value), "source_id": "PUBCHEM",
                        "method": "PubChem PUG REST property endpoint"}
    return out


def build(args: argparse.Namespace) -> tuple[dict[str, Any], dict[str, Any]]:
    source = load_json(args.input)
    records = source.get("records") or []
    if args.limit is not None:
        records = records[:max(0, args.limit)]

    result_records: list[dict[str, Any]] = []
    resolved = unresolved = errors = fallback_resolved = 0
    for i, row in enumerate(records, 1):
        cas = str(row.get("cas") or "").strip()
        name = str(row.get("name_en") or "").strip()
        method = "NONE"; identifier = ""
        if not cas:
            status, prop, error = "UNRESOLVED", None, "missing CAS in source identity record"
        else:
            status, prop, error, method, identifier = resolve_pubchem(
                cas, args.cache_dir, args.refresh, args.retries, args.backoff, args.delay)
        if status == "RESOLVED":
            resolved += 1
            if method == "CURATED_NAME_FALLBACK": fallback_resolved += 1
        elif status == "UNRESOLVED": unresolved += 1
        else: errors += 1
        result_records.append({
            "name_en": name,
            "cas": cas or None,
            "identity_source_url": row.get("source_url"),
            "status": status,
            "error": error,
            "resolution_method": method,
            "resolution_identifier": identifier or None,
            "pubchem": normalize_property(prop),
        })
        suffix = f" via {method}:{identifier}" if status == "RESOLVED" and identifier else ""
        print(f"[{i}/{len(records)}] {cas or 'NO-CAS'} {name}: {status}{suffix}")

    root = {
        "schema": "chemcore-molecular-seed-v1",
        "generated_at_utc": utc_now(),
        "record_count": len(result_records),
        "resolved_count": resolved,
        "unresolved_count": unresolved,
        "error_count": errors,
        "curated_name_fallback_resolved_count": fallback_resolved,
        "interpretation": "Molecular/structural knowledge only; these records do not imply MQ detectability, calibration, or quantitative validity.",
        "identity_provenance": source.get("provenance"),
        "descriptor_provenance": {
            "source_id": "PUBCHEM",
            "authority": "NIH/NLM/NCBI",
            "source_url": PUBCHEM_SOURCE,
            "interface": "PubChem PUG REST property endpoint",
            "properties_requested": PROPERTIES,
            "resolution_order": "source CAS first; explicit curated-name fallback only for reviewed PURE_COMPOUND records",
            "curated_name_fallbacks": CURATED_NAME_FALLBACKS,
            "usage_policy_note": "PUG REST clients should remain at or below five requests per second; bulk data should use an optimized workflow.",
        },
        "records": result_records,
    }
    payload = (json.dumps(root, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    manifest = {
        "schema": "chemcore-molecular-seed-manifest-v1",
        "generated_at_utc": root["generated_at_utc"],
        "record_count": len(result_records),
        "resolved_count": resolved,
        "unresolved_count": unresolved,
        "error_count": errors,
        "curated_name_fallback_resolved_count": fallback_resolved,
        "sha256": sha256_bytes(payload),
        "source_identity_sha256": sha256_bytes(args.input.read_bytes()),
        "descriptor_source": PUBCHEM_SOURCE,
        "property_names": PROPERTIES,
    }
    return root, manifest


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    p.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    p.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    p.add_argument("--cache-dir", type=Path, default=DEFAULT_CACHE)
    p.add_argument("--limit", type=int, default=None)
    p.add_argument("--refresh", action="store_true")
    p.add_argument("--retries", type=int, default=4)
    p.add_argument("--backoff", type=float, default=1.0)
    p.add_argument("--delay", type=float, default=0.25,
                   help="minimum sleep after uncached requests; keep >=0.20 s to respect PubChem's <=5 requests/s guidance")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    root, manifest = build(args)
    payload = (json.dumps(root, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    atomic_write(args.output, payload)
    atomic_write(args.manifest, (json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8"))
    print(f"Wrote {args.output}: {manifest['record_count']} records; resolved={manifest['resolved_count']}; unresolved={manifest['unresolved_count']}; errors={manifest['error_count']}; curated-name-fallback-resolved={manifest['curated_name_fallback_resolved_count']}")
    print(f"SHA-256 {manifest['sha256']}")
    return 0 if manifest["error_count"] == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())
