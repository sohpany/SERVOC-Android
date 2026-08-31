#!/usr/bin/env python3
"""Build an auditable OSHA Table Z-1 PEL seed from the official OSHA page.

Only OSHA regulatory PEL columns are parsed. Cal/OSHA, NIOSH REL and ACGIH TLV
columns are deliberately ignored. ACGIH values are copyrighted and are never
copied into HazRob by this tool.
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
from urllib.request import Request, urlopen

SOURCE = "https://www.osha.gov/annotated-pels/table-z-1"
CAS_RE = re.compile(r"\b(\d{2,7}-\d{2}-\d)\b")
NUM_RE = re.compile(r"(?<![\d.])([0-9][0-9,]*(?:\.[0-9]+)?)")
EXPLICIT_RE = re.compile(r"([0-9][0-9,]*(?:\.[0-9]+)?)\s*(ppm|mg\s*/?\s*m(?:\^?3|³|\{3\}))", re.I)
UA = "HazRob-scientific-library/0.6 (+research prototype; provenance audit)"
FRACTIONS = {
    "total dust", "respirable fraction", "respirable dust", "respirable fraction;",
    "respirable", "total particulate", "total", "inhalable fraction"
}

class TableParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.depth = 0; self.in_row = False; self.in_cell = False
        self.parts: list[str] = []; self.cells: list[str] = []; self.rows: list[list[str]] = []
    def handle_starttag(self, tag, attrs):
        t = tag.lower()
        if t == "table": self.depth += 1
        elif t == "tr" and self.depth:
            self.in_row = True; self.cells = []
        elif t in ("td", "th") and self.in_row:
            self.in_cell = True; self.parts = []
        elif t == "br" and self.in_cell: self.parts.append("\n")
    def handle_data(self, data):
        if self.in_cell: self.parts.append(data)
    def handle_endtag(self, tag):
        t = tag.lower()
        if t in ("td", "th") and self.in_cell:
            text = html.unescape("".join(self.parts)).replace("\xa0", " ")
            text = "\n".join(" ".join(x.split()) for x in text.splitlines() if " ".join(x.split()))
            self.cells.append(text.strip()); self.in_cell = False
        elif t == "tr" and self.in_row:
            if self.cells: self.rows.append(self.cells[:])
            self.in_row = False
        elif t == "table" and self.depth: self.depth -= 1

def fetch(url: str) -> bytes:
    req = Request(url, headers={"User-Agent": UA, "Accept": "text/html,*/*"})
    with urlopen(req, timeout=45) as r: return r.read()

def norm_unit(unit: str) -> str:
    x = unit.lower().replace(" ", "").replace("m³", "m3").replace("m^3", "m3").replace("m{3}", "m3")
    return "ppm" if "ppm" in x else "mg/m3"

def default_kind(text: str) -> str:
    u = text.upper()
    if "STEL" in u or "(ST)" in u: return "STEL"
    if "CEILING" in u or "(C)" in u: return "CEILING"
    return "TWA"

def parse_cell(raw: str, context_unit: str, descriptor: str | None = None) -> list[dict]:
    text = " ".join((raw or "").replace("m³", "m3").replace("m^{3}", "m3").replace("m^3", "m3").split())
    if not text or text in {"—", "-"}: return []
    out: list[dict] = []
    # Explicit-unit entries can contain more than one limit, e.g. "1 ppm / 5 ppm STEL".
    matches = list(EXPLICIT_RE.finditer(text))
    if matches:
        for idx, m in enumerate(matches):
            value = float(m.group(1).replace(",", "")); unit = norm_unit(m.group(2))
            before = text[max(0, m.start()-12):m.start()].upper(); after = text[m.end():min(len(text), m.end()+18)].upper()
            kind = "CEILING" if "(C)" in before or "CEILING" in before else "STEL" if "STEL" in after or "(ST)" in before else "TWA"
            note = text if descriptor is None else f"{descriptor} | {text}"
            out.append({"limit_type":kind,"value":value,"unit":unit,"duration_min":15.0 if kind=="STEL" else 480.0 if kind=="TWA" else None,"notation":note})
        return out
    # Column-context numeric entries: OSHA page separates ppm and mg/m3 columns.
    if re.fullmatch(r"\s*(?:\(C\)|\(ST\))?\s*[0-9][0-9,]*(?:\.[0-9]+)?(?:\s*[a-zA-Z*()]+)?\s*", text):
        m = NUM_RE.search(text)
        if m:
            kind = default_kind(text); note = text if descriptor is None else f"{descriptor} | {text}"
            out.append({"limit_type":kind,"value":float(m.group(1).replace(",","")),"unit":context_unit,"duration_min":15.0 if kind=="STEL" else 480.0 if kind=="TWA" else None,"notation":note})
    return out

def main() -> int:
    out_dir = Path(sys.argv[1] if len(sys.argv)>1 else "generated/osha_z1"); out_dir.mkdir(parents=True, exist_ok=True)
    raw = fetch(SOURCE); p = TableParser(); p.feed(raw.decode("utf-8", errors="replace"))
    records: list[dict] = []; substance_rows = 0; parent_name = None; parent_cas = None
    for cells in p.rows:
        joined = " | ".join(cells); cas_match = CAS_RE.search(joined)
        if cas_match:
            cas = cas_match.group(1); cas_idx = next((i for i,c in enumerate(cells) if CAS_RE.search(c)), None)
            if cas_idx is None or cas_idx < 1: continue
            name = cells[0].strip(); parent_name, parent_cas = name, cas; substance_rows += 1
            # OSHA regulatory columns are the two cells immediately after CAS: ppm and mg/m3.
            ppm_cell = cells[cas_idx+1] if cas_idx+1 < len(cells) else ""
            mg_cell = cells[cas_idx+2] if cas_idx+2 < len(cells) else ""
            for r in parse_cell(ppm_cell, "ppm") + parse_cell(mg_cell, "mg/m3"):
                r.update({"name_en":name,"cas":cas,"authority":"OSHA","jurisdiction":"US","source_id":"OSHA_Z1","revision":"29 CFR 1910.1000 Table Z-1","source_url":SOURCE})
                records.append(r)
            continue
        # Preserve common fraction subrows under the immediately preceding CAS substance.
        if parent_cas and cells:
            descriptor = cells[0].strip(); key = descriptor.casefold().rstrip(";")
            if key in FRACTIONS:
                # Typical layout keeps a blank CAS column before OSHA ppm/mg columns.
                start = 2 if len(cells) >= 4 and not cells[1].strip() else 1
                ppm_cell = cells[start] if start < len(cells) else ""
                mg_cell = cells[start+1] if start+1 < len(cells) else ""
                for r in parse_cell(ppm_cell,"ppm",descriptor)+parse_cell(mg_cell,"mg/m3",descriptor):
                    r.update({"name_en":parent_name,"cas":parent_cas,"authority":"OSHA","jurisdiction":"US","source_id":"OSHA_Z1","revision":"29 CFR 1910.1000 Table Z-1","source_url":SOURCE})
                    records.append(r)
    # De-duplicate exact alternate parser repeats only; ppm and mg/m3 remain distinct records.
    seen=set(); unique=[]
    for r in records:
        key=(r["cas"],r["limit_type"],r["value"],r["unit"],r.get("notation", ""))
        if key in seen: continue
        seen.add(key); unique.append(r)
    if substance_rows < 300 or len(unique) < 300:
        raise RuntimeError(f"OSHA Z-1 parser produced {substance_rows} CAS rows/{len(unique)} PEL records; source layout may have changed")
    generated = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00","Z")
    source_hash = hashlib.sha256(raw).hexdigest()
    payload={"schema":"hazrob.exposure-seed.v1","generated_utc":generated,"authority":"OSHA","dataset":"PEL_Z1","source":SOURCE,"source_sha256":source_hash,"substance_rows":substance_rows,"record_count":len(unique),"records":unique}
    jp=out_dir/"osha_z1.json"; jp.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    manifest={"file":jp.name,"sha256":hashlib.sha256(jp.read_bytes()).hexdigest(),"source_sha256":source_hash,"authority":"OSHA","dataset":"PEL_Z1","substance_rows":substance_rows,"record_count":len(unique),"generated_utc":generated,"authoritative":True,"excluded_columns":["Cal/OSHA PEL","NIOSH REL","ACGIH TLV"]}
    (out_dir/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(manifest,indent=2)); return 0

if __name__ == "__main__": raise SystemExit(main())
