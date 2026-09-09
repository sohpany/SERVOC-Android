#!/usr/bin/env python3
"""Strict structural gate for ChemCore experiment/reference promotion candidates."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

CAS_RE = re.compile(r"^\d{2,7}-\d{2}-\d$")
ALLOWED_STATUS = {"CANDIDATE", "REVIEWED", "QC_PASSED", "APPROVED_FOR_PUBLISHING", "REJECTED"}
ALLOWED_EVIDENCE = {"M0", "M1", "M2", "M3", "M4"}
REQUIRED_TOP = {
    "candidate_schema_version", "candidate_id", "created_utc", "experiment_id", "session_id",
    "chemical", "reference", "instrument", "sampling", "analytical", "review"
}


def require(cond: bool, msg: str, errors: list[str]) -> None:
    if not cond:
        errors.append(msg)


def validate(path: Path) -> list[str]:
    errors: list[str] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        return [f"invalid JSON: {exc}"]
    require(isinstance(data, dict), "root must be an object", errors)
    if not isinstance(data, dict):
        return errors
    missing = sorted(REQUIRED_TOP - set(data))
    require(not missing, f"missing top-level fields: {', '.join(missing)}", errors)
    require(data.get("candidate_schema_version") == 1, "candidate_schema_version must be 1", errors)
    require(bool(str(data.get("candidate_id", "")).strip()), "candidate_id required", errors)
    require(bool(str(data.get("session_id", "")).strip()), "session_id required", errors)
    require(isinstance(data.get("experiment_id"), int) and data.get("experiment_id", 0) > 0, "experiment_id must be positive integer", errors)

    chemical = data.get("chemical") if isinstance(data.get("chemical"), dict) else {}
    cas = str(chemical.get("cas", "")).strip()
    require(bool(str(chemical.get("name", "")).strip()), "chemical.name required", errors)
    require(CAS_RE.match(cas) is not None, "chemical.cas must be a CAS-formatted identifier", errors)
    require(isinstance(chemical.get("uid"), int) and chemical.get("uid", 0) > 0, "chemical.uid must be positive integer", errors)

    reference = data.get("reference") if isinstance(data.get("reference"), dict) else {}
    ppm = reference.get("known_ppm")
    if ppm is not None:
        require(isinstance(ppm, (int, float)) and ppm >= 0, "reference.known_ppm must be non-negative numeric or null", errors)
    require(bool(str(reference.get("kind", "")).strip()), "reference.kind required", errors)
    require(bool(str(reference.get("source", "")).strip()), "reference.source required", errors)

    instrument = data.get("instrument") if isinstance(data.get("instrument"), dict) else {}
    for key in ("app_version", "firmware", "hardware_profile", "feature_schema"):
        require(bool(str(instrument.get(key, "")).strip()), f"instrument.{key} required", errors)

    sampling = data.get("sampling") if isinstance(data.get("sampling"), dict) else {}
    require(bool(str(sampling.get("profile_id", "")).strip()), "sampling.profile_id required", errors)
    require(bool(str(sampling.get("flow_status", "")).strip()), "sampling.flow_status required", errors)

    analytical = data.get("analytical") if isinstance(data.get("analytical"), dict) else {}
    tier = str(analytical.get("evidence_tier", "")).strip().upper()
    require(tier in ALLOWED_EVIDENCE, "analytical.evidence_tier must be M0..M4", errors)
    require(bool(str(analytical.get("feature_payload_sha256", "")).strip()), "analytical.feature_payload_sha256 required", errors)
    feature_sha = str(analytical.get("feature_payload_sha256", "")).lower()
    require(re.fullmatch(r"[0-9a-f]{64}", feature_sha) is not None, "feature_payload_sha256 must be SHA-256 hex", errors)

    review = data.get("review") if isinstance(data.get("review"), dict) else {}
    status = str(review.get("status", "")).strip().upper()
    require(status in ALLOWED_STATUS, "review.status invalid", errors)
    if status == "APPROVED_FOR_PUBLISHING":
        require(bool(str(review.get("reviewed_utc", "")).strip()), "approved candidate requires review.reviewed_utc", errors)
        require(bool(str(review.get("decision_note", "")).strip()), "approved candidate requires review.decision_note", errors)
        require(bool(str(review.get("review_session", "")).strip()), "approved candidate requires review.review_session", errors)
        require(tier in {"M1", "M2", "M3", "M4"}, "approved candidate cannot remain M0", errors)

    attachments = data.get("attachments", [])
    require(isinstance(attachments, list), "attachments must be an array", errors)
    if isinstance(attachments, list):
        for i, item in enumerate(attachments):
            require(isinstance(item, dict), f"attachments[{i}] must be an object", errors)
            if not isinstance(item, dict):
                continue
            sha = str(item.get("sha256", "")).lower()
            require(re.fullmatch(r"[0-9a-f]{64}", sha) is not None, f"attachments[{i}].sha256 invalid", errors)
            require(bool(str(item.get("kind", "")).strip()), f"attachments[{i}].kind required", errors)

    return errors


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("candidate", type=Path)
    args = p.parse_args()
    errors = validate(args.candidate)
    if errors:
        for e in errors:
            print("ERROR:", e)
        return 1
    raw = args.candidate.read_bytes()
    print("VALID", args.candidate)
    print("SHA-256", hashlib.sha256(raw).hexdigest())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
