#!/usr/bin/env python3
"""Build the HazRob gas flammability seed from official NIOSH Pocket Guide entries.

The output is deliberately source-faithful. Numeric LFL/UEL values are copied only when an
explicit percent-by-volume value is present on the official chemical page. NA/missing/ambiguous
values remain null; the builder never infers an LFL from another property or from an IDLH.
"""
from __future__ import annotations

import hashlib
import html
import json
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

UA = "HazRob-scientific-library/0.10.1 (+NIOSH flammability provenance build)"
LABELS = ("Lower Explosive Limit", "Lower Flammable Limit", "Upper Explosive Limit", "Upper Flammable Limit")
PCT_RE = re.compile(r"(?<![\d.])(\d+(?:\.\d+)?)\s*%")
OFFICIAL_PREFIXES = (
    "https://www.cdc.gov/niosh/npg/",
    "https://www.cdc.gov/NIOSH/NPG/",
)


class TextParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.skip = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() in ("script", "style", "noscript"):
            self.skip += 1

    def handle_endtag(self, tag):
        if tag.lower() in ("script", "style", "noscript") and self.skip:
            self.skip -= 1

    def handle_data(self, data):
        if self.skip:
            return
        s = " ".join(html.unescape(data).replace("\xa0", " ").split())
        if s:
            self.parts.append(s)


def fetch(url: str, retries: int = 3) -> bytes:
    last = None
    for attempt in range(retries):
        try:
            req = Request(url, headers={"User-Agent": UA, "Accept": "text/html,application/xhtml+xml"})
            with urlopen(req, timeout=35) as response:
                return response.read()
        except (HTTPError, URLError, TimeoutError) as exc:
            last = exc
            time.sleep(0.35 * (attempt + 1))
    raise RuntimeError(f"fetch failed for {url}: {last}")


def official_detail_url(url: str) -> bool:
    low = str(url or "").lower()
    return low.startswith("https://www.cdc.gov/niosh/npg/") and "/npgd" in low


def field_after(parts: list[str], names: tuple[str, ...]) -> str | None:
    wanted = {x.casefold() for x in names}
    for i, part in enumerate(parts):
        if part.strip().casefold() not in wanted:
            continue
        for candidate in parts[i + 1:i + 7]:
            c = candidate.strip()
            if not c:
                continue
            if c.casefold() in {x.casefold() for x in LABELS}:
                break
            return c
    return None


def pct(text: str | None) -> float | None:
    if not text:
        return None
    m = PCT_RE.search(text)
    if not m:
        return None
    value = float(m.group(1))
    return value if value > 0 else None


def parse_record(rec: dict) -> dict:
    url = str(rec.get("source_url") or "")
    base = {
        "cas": rec.get("cas"),
        "name_en": rec.get("name_en"),
        "source_url": url,
        "lfl_pct": None,
        "ufl_pct": None,
        "lfl_text": None,
        "ufl_text": None,
        "page_sha256": None,
        "status": "not_official_detail_url",
    }
    if not official_detail_url(url):
        return base
    try:
        raw = fetch(url)
        parser = TextParser(); parser.feed(raw.decode("utf-8", errors="replace"))
        lfl_text = field_after(parser.parts, ("Lower Explosive Limit", "Lower Flammable Limit"))
        ufl_text = field_after(parser.parts, ("Upper Explosive Limit", "Upper Flammable Limit"))
        base.update({
            "lfl_pct": pct(lfl_text),
            "ufl_pct": pct(ufl_text),
            "lfl_text": lfl_text,
            "ufl_text": ufl_text,
            "page_sha256": hashlib.sha256(raw).hexdigest(),
            "status": "ok",
        })
    except Exception as exc:
        base["status"] = "fetch_or_parse_error:" + exc.__class__.__name__
    return base


def main() -> int:
    identity_path = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/generated/niosh/niosh_chemicals.json")
    out_dir = Path(sys.argv[2] if len(sys.argv) > 2 else "generated/flammability")
    out_dir.mkdir(parents=True, exist_ok=True)
    identity_bytes = identity_path.read_bytes()
    identity = json.loads(identity_bytes.decode("utf-8"))
    records = identity.get("records") or []
    if len(records) < 500:
        raise RuntimeError(f"identity seed unexpectedly small: {len(records)}")

    results: list[dict] = []
    with ThreadPoolExecutor(max_workers=4) as pool:
        jobs = {pool.submit(parse_record, rec): rec for rec in records}
        for idx, future in enumerate(as_completed(jobs), 1):
            results.append(future.result())
            if idx % 50 == 0:
                print(f"processed {idx}/{len(records)}", file=sys.stderr)
    results.sort(key=lambda r: (str(r.get("cas") or ""), str(r.get("name_en") or "").casefold()))

    ok = sum(1 for r in results if r.get("status") == "ok")
    lfl = sum(1 for r in results if isinstance(r.get("lfl_pct"), (int, float)))
    ufl = sum(1 for r in results if isinstance(r.get("ufl_pct"), (int, float)))
    errors = sum(1 for r in results if str(r.get("status", "")).startswith("fetch_or_parse_error"))
    if ok < 500:
        raise RuntimeError(f"official NIOSH detail-page coverage too low: ok={ok}, errors={errors}")
    if lfl < 50:
        raise RuntimeError(f"numeric LFL coverage unexpectedly low: {lfl}")

    generated = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    payload = {
        "schema": "hazrob.flammability-seed.v1",
        "authority": "NIOSH",
        "dataset": "NPG_FLAMMABILITY",
        "authoritative": True,
        "generated_utc": generated,
        "source_identity_sha256": hashlib.sha256(identity_bytes).hexdigest(),
        "source": "https://www.cdc.gov/niosh/npg/",
        "record_count": len(results),
        "official_page_ok_count": ok,
        "numeric_lfl_count": lfl,
        "numeric_ufl_count": ufl,
        "error_count": errors,
        "records": results,
    }
    out = out_dir / "niosh_npg_flammability.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    manifest = {
        "file": out.name,
        "sha256": hashlib.sha256(out.read_bytes()).hexdigest(),
        "schema": payload["schema"],
        "authority": payload["authority"],
        "dataset": payload["dataset"],
        "authoritative": True,
        "record_count": len(results),
        "official_page_ok_count": ok,
        "numeric_lfl_count": lfl,
        "numeric_ufl_count": ufl,
        "error_count": errors,
        "generated_utc": generated,
        "source_identity_sha256": payload["source_identity_sha256"],
    }
    (out_dir / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
