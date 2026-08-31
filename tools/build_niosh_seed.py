#!/usr/bin/env python3
"""Build a versioned HazRob chemical identity seed derived from the NIOSH Pocket Guide.

Priority 1 is the official CDC/NIOSH CAS index. Some CI networks are blocked by CDC with
HTTP 403; in that case only, this tool falls back to the immutable Version 3 companion
dataset for Lucas, Whittaker & Bailer (2024), DOI 10.1080/15459624.2023.2267098.

The fallback is explicitly tagged derived-research/non-authoritative. This tool imports
identity/CAS only. Exposure values are handled separately and require authority-aware
records with official-source verification.
"""
from __future__ import annotations

import csv
import hashlib
import html
import io
import json
import re
import sys
import zipfile
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import Request, urlopen
from xml.etree import ElementTree as ET

OFFICIAL_SOURCE = "https://www.cdc.gov/niosh/npg/npgdcas.html"
FIGSHARE_VERSION_API = "https://api.figshare.com/v2/articles/24431042/versions/3"
FIGSHARE_LANDING = "https://tandf.figshare.com/articles/dataset/24431042"
FIGSHARE_DOI = "10.6084/m9.figshare.24431042.v3"
ARTICLE_DOI = "10.1080/15459624.2023.2267098"
CAS_RE = re.compile(r"\b(\d{2,7}-\d{2}-\d)\b")
UA = "HazRob-scientific-library/0.6 (+research prototype; provenance audit)"


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
            self.in_row = True; self.cells = []; self.detail_href = None
        elif tag in ("td", "th") and self.in_row:
            self.in_cell = True; self.cell_parts = []
        elif tag == "a" and self.in_row:
            href = next((v for k, v in attrs if k.lower() == "href"), None)
            if href and "npgd" in href.lower(): self.detail_href = href

    def handle_data(self, data):
        if self.in_row and self.in_cell: self.cell_parts.append(data)

    def handle_endtag(self, tag):
        tag = tag.lower()
        if tag in ("td", "th") and self.in_cell:
            self.cells.append(" ".join("".join(self.cell_parts).split())); self.in_cell = False
        elif tag == "tr" and self.in_row:
            self.in_row = False
            if len(self.cells) < 2: return
            joined = " | ".join(self.cells); match = CAS_RE.search(joined)
            if not match: return
            cas = match.group(1); name = ""
            for cell in self.cells:
                if not CAS_RE.fullmatch(cell.strip()) and cell.strip():
                    name = html.unescape(cell).strip(); break
            if name:
                href = urljoin(OFFICIAL_SOURCE, self.detail_href) if self.detail_href else OFFICIAL_SOURCE
                self.rows.append((cas, name, href))


def fetch(url: str) -> bytes:
    req = Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    with urlopen(req, timeout=45) as response: return response.read()


def clean_header(value: object) -> str:
    return re.sub(r"[^a-z0-9]+", " ", str(value or "").lower()).strip()


def clean_text(value: object) -> str:
    return " ".join(str(value or "").replace("\xa0", " ").split()).strip()


def rows_to_records(rows: list[list[object]], source_url: str) -> list[dict]:
    if not rows: return []
    best = None
    for ri, row in enumerate(rows[:25]):
        headers = [clean_header(x) for x in row]
        cas_idx = next((i for i,h in enumerate(headers) if h == "cas" or h.startswith("cas ") or "cas no" in h or "cas number" in h), None)
        name_idx = next((i for i,h in enumerate(headers) if h in ("chemical","chemical name","chem name") or h.startswith("chemical name")), None)
        if cas_idx is not None and name_idx is not None:
            best = (ri, cas_idx, name_idx); break
    if best is None: return []
    ri, cas_idx, name_idx = best; out = []
    for row in rows[ri+1:]:
        if cas_idx >= len(row) or name_idx >= len(row): continue
        cas_match = CAS_RE.search(clean_text(row[cas_idx])); name = clean_text(row[name_idx])
        if cas_match and name:
            out.append({"cas":cas_match.group(1),"name_en":name,"source_url":source_url})
    return out


def parse_csv(data: bytes, source_url: str) -> list[dict]:
    text = data.decode("utf-8-sig", errors="replace")
    sample = text[:8192]
    try: dialect = csv.Sniffer().sniff(sample, delimiters=",\t;|")
    except csv.Error: dialect = csv.excel
    return rows_to_records(list(csv.reader(io.StringIO(text), dialect)), source_url)


def col_index(ref: str) -> int:
    letters = re.match(r"[A-Z]+", ref.upper())
    if not letters: return 0
    n = 0
    for ch in letters.group(0): n = n*26 + ord(ch)-64
    return n-1


def parse_xlsx(data: bytes, source_url: str) -> list[dict]:
    out: list[dict] = []
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        shared: list[str] = []
        if "xl/sharedStrings.xml" in z.namelist():
            root = ET.fromstring(z.read("xl/sharedStrings.xml"))
            for si in root.iter():
                if si.tag.endswith("}si"):
                    shared.append("".join(t.text or "" for t in si.iter() if t.tag.endswith("}t")))
        wb = ET.fromstring(z.read("xl/workbook.xml")); relroot = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        rels = {r.attrib.get("Id"):r.attrib.get("Target") for r in relroot}
        for sheet in wb.iter():
            if not sheet.tag.endswith("}sheet"): continue
            rid = next((v for k,v in sheet.attrib.items() if k.endswith("}id")), None); target = rels.get(rid)
            if not target: continue
            path = target.lstrip("/")
            if not path.startswith("xl/"): path = "xl/"+path
            if path not in z.namelist(): continue
            sroot = ET.fromstring(z.read(path)); rows: list[list[object]] = []
            for row in (e for e in sroot.iter() if e.tag.endswith("}row")):
                vals: dict[int,str] = {}
                for c in (e for e in row if e.tag.endswith("}c")):
                    idx = col_index(c.attrib.get("r","A1")); typ = c.attrib.get("t",""); value = ""
                    if typ == "inlineStr": value = "".join(t.text or "" for t in c.iter() if t.tag.endswith("}t"))
                    else:
                        v = next((x for x in c if x.tag.endswith("}v")), None)
                        if v is not None and v.text is not None:
                            value = v.text
                            if typ == "s":
                                try: value = shared[int(value)]
                                except (ValueError,IndexError): pass
                    vals[idx] = value
                if vals:
                    width=max(vals)+1; rows.append([vals.get(i,"") for i in range(width)])
            out.extend(rows_to_records(rows, source_url))
    return out


def official_records() -> tuple[list[dict], dict]:
    raw = fetch(OFFICIAL_SOURCE); parser = NpgTableParser(); parser.feed(raw.decode("utf-8", errors="replace"))
    records=[{"cas":cas,"name_en":name,"source_url":url} for cas,name,url in parser.rows]
    if len(records) < 500: raise RuntimeError(f"official NIOSH parse produced only {len(records)} records")
    return records,{"source_kind":"official","authority":"NIOSH","authoritative":True,"source":OFFICIAL_SOURCE,"source_sha256":hashlib.sha256(raw).hexdigest()}


def derived_records() -> tuple[list[dict], dict]:
    meta_raw=fetch(FIGSHARE_VERSION_API); meta=json.loads(meta_raw.decode("utf-8")); files=meta.get("files") or []
    candidates=[f for f in files if str(f.get("name","")).lower().endswith((".csv",".xlsx"))]
    if not candidates: raise RuntimeError("pinned Figshare dataset contains no CSV/XLSX source")
    best: list[dict]=[]; best_file=None; best_raw=b""
    for f in candidates:
        url=f.get("download_url") or f.get("download_url_private")
        if not url: continue
        raw=fetch(url); name=str(f.get("name","")); low=name.lower()
        try: rows=parse_csv(raw,url) if low.endswith(".csv") else parse_xlsx(raw,url)
        except Exception as exc:
            print(f"warning: could not parse {name}: {exc}",file=sys.stderr); continue
        if len(rows)>len(best): best,best_file,best_raw=rows,f,raw
    if len(best)<500: raise RuntimeError(f"pinned Figshare parse produced only {len(best)} CAS/name records")
    return best,{
        "source_kind":"derived-research","authority":"NIOSH-derived research dataset","authoritative":False,
        "source":FIGSHARE_LANDING,"source_api":FIGSHARE_VERSION_API,"source_file":best_file.get("name"),"source_file_id":best_file.get("id"),
        "source_supplied_md5":best_file.get("supplied_md5"),"source_sha256":hashlib.sha256(best_raw).hexdigest(),
        "figshare_doi":FIGSHARE_DOI,"article_doi":ARTICLE_DOI,"version":3,
    }


def dedupe(records: list[dict], provenance: dict) -> list[dict]:
    seen:set[tuple[str,str]]=set(); out=[]
    sid="NIOSH_NPG" if provenance["source_kind"]=="official" else "NIOSH_NPG_DERIVED_2024"
    for r in records:
        cas=clean_text(r.get("cas")); name=clean_text(r.get("name_en")); m=CAS_RE.search(cas)
        if not m or not name: continue
        cas=m.group(1); key=(cas,name.casefold())
        if key in seen: continue
        seen.add(key); out.append({"cas":cas,"name_en":name,"source_id":sid,"source_url":r.get("source_url") or provenance["source"],"source_kind":provenance["source_kind"],"authoritative":bool(provenance["authoritative"])})
    out.sort(key=lambda r:(r["cas"],r["name_en"].casefold())); return out


def main() -> int:
    out_dir=Path(sys.argv[1] if len(sys.argv)>1 else "generated/niosh"); out_dir.mkdir(parents=True,exist_ok=True)
    try:
        records, provenance=official_records(); print("source: official CDC/NIOSH",file=sys.stderr)
    except (HTTPError,URLError) as exc:
        print(f"warning: official CDC/NIOSH source unreachable from this network ({exc}); using pinned research snapshot",file=sys.stderr)
        records, provenance=derived_records()
    records=dedupe(records,provenance)
    if len(records)<500: raise RuntimeError(f"chemical seed contains only {len(records)} records")
    generated=datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00","Z")
    payload={"schema":"hazrob.chemical-seed.v1","generated_utc":generated,"record_count":len(records),"provenance":provenance,"source_sha256":provenance["source_sha256"],"records":records}
    json_path=out_dir/"niosh_chemicals.json"; json_path.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    manifest={"file":json_path.name,"sha256":hashlib.sha256(json_path.read_bytes()).hexdigest(),"source_sha256":provenance["source_sha256"],"source_kind":provenance["source_kind"],"authoritative":provenance["authoritative"],"record_count":len(records),"generated_utc":generated,"provenance":provenance}
    (out_dir/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(manifest,ensure_ascii=False,indent=2)); return 0

if __name__=="__main__": raise SystemExit(main())
