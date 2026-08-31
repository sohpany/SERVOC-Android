#!/usr/bin/env python3
"""Build a versioned HazRob chemical identity seed from the official NIOSH NPG CAS index.

This imports identity/CAS/source URL only. Exposure values are intentionally handled by
separate authority-aware importers so regulatory/recommendation semantics are preserved.
"""
from __future__ import annotations

import hashlib
import html
import json
import re
import sys
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urljoin
from urllib.request import Request, urlopen

SOURCE = "https://www.cdc.gov/niosh/npg/npgdcas.html"
CAS_RE = re.compile(r"\b(\d{2,7}-\d{2}-\d)\b")


class NpgTableParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.in_row = False
        self.in_cell = False
        self.cell_parts: list[str] = []
        self.cells: list[str] = []
        self.detail_href: str | None = None
        self.rows: list[tuple[str, str, str]] = []

    def handle_starttag(self, tag, attrs):
        tag = tag.lower()
        if tag == "tr":
            self.in_row = True
            self.cells = []
            self.detail_href = None
        elif tag in ("td", "th") and self.in_row:
            self.in_cell = True
            self.cell_parts = []
        elif tag == "a" and self.in_row:
            href = next((v for k, v in attrs if k.lower() == "href"), None)
            if href and "npgd" in href.lower():
                self.detail_href = href

    def handle_data(self, data):
        if self.in_row and self.in_cell:
            self.cell_parts.append(data)

    def handle_endtag(self, tag):
        tag = tag.lower()
        if tag in ("td", "th") and self.in_cell:
            self.cells.append(" ".join("".join(self.cell_parts).split()))
            self.in_cell = False
        elif tag == "tr" and self.in_row:
            self.in_row = False
            if len(self.cells) < 2 or not self.detail_href:
                return
            joined = " | ".join(self.cells)
            match = CAS_RE.search(joined)
            if not match:
                return
            cas = match.group(1)
            # NPG tables normally present CAS first and chemical name next. Prefer the
            # first non-CAS textual cell to remain robust to minor table-layout changes.
            name = ""
            for cell in self.cells:
                if not CAS_RE.fullmatch(cell.strip()) and cell.strip():
                    name = html.unescape(cell).strip()
                    break
            if name:
                self.rows.append((cas, name, urljoin(SOURCE, self.detail_href)))


def fetch(url: str) -> bytes:
    req = Request(url, headers={"User-Agent": "HazRob-scientific-library/0.6"})
    with urlopen(req, timeout=45) as response:
        return response.read()


def main() -> int:
    out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "generated/niosh")
    out_dir.mkdir(parents=True, exist_ok=True)
    raw = fetch(SOURCE)
    parser = NpgTableParser()
    parser.feed(raw.decode("utf-8", errors="replace"))

    seen: set[tuple[str, str]] = set()
    records = []
    for cas, name, detail_url in parser.rows:
        key = (cas, name.casefold())
        if key in seen:
            continue
        seen.add(key)
        records.append({
            "cas": cas,
            "name_en": name,
            "source_id": "NIOSH_NPG",
            "source_url": detail_url,
        })
    records.sort(key=lambda r: (r["cas"], r["name_en"].casefold()))

    if len(records) < 500:
        raise RuntimeError(f"NIOSH parse produced only {len(records)} records; source layout may have changed")

    generated = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    source_sha256 = hashlib.sha256(raw).hexdigest()
    payload = {
        "schema": "hazrob.chemical-seed.v1",
        "generated_utc": generated,
        "source": SOURCE,
        "source_sha256": source_sha256,
        "record_count": len(records),
        "records": records,
    }

    json_path = out_dir / "niosh_chemicals.json"
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    manifest = {
        "file": json_path.name,
        "sha256": hashlib.sha256(json_path.read_bytes()).hexdigest(),
        "source_sha256": source_sha256,
        "record_count": len(records),
        "generated_utc": generated,
    }
    (out_dir / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
