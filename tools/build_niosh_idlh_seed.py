#!/usr/bin/env python3
"""Build an auditable HazRob NIOSH IDLH seed from the official CDC table.

Both the 1994 value and a 2016-present/CIB-66 value are retained when present.
The newer value is tagged current_policy=true, matching NIOSH's published guidance.
No fallback to a non-authoritative dataset is allowed for exposure-limit data.
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

SOURCE="https://www.cdc.gov/niosh/idlh/intridl4.html"
CAS_RE=re.compile(r"\b(\d{2,7}-\d{2}-\d)\b")
NUM_RE=re.compile(r"(?<![\d.])([0-9][0-9,]*(?:\.[0-9]+)?)\s*(ppm|mg(?:\s+[A-Za-z()\[\]0-9,+-]+)?\s*/\s*m(?:\^?\{?3\}?|3))",re.I)

class TableParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True);self.in_table=False;self.in_row=False;self.in_cell=False;self.parts=[];self.cells=[];self.rows=[]
    def handle_starttag(self,tag,attrs):
        t=tag.lower()
        if t=="table":self.in_table=True
        elif t=="tr" and self.in_table:self.in_row=True;self.cells=[]
        elif t in ("td","th") and self.in_row:self.in_cell=True;self.parts=[]
    def handle_data(self,data):
        if self.in_cell:self.parts.append(data)
    def handle_endtag(self,tag):
        t=tag.lower()
        if t in ("td","th") and self.in_cell:
            self.cells.append(" ".join(html.unescape("".join(self.parts)).replace("\xa0"," ").split()));self.in_cell=False
        elif t=="tr" and self.in_row:
            if len(self.cells)>=4:self.rows.append(self.cells[:4])
            self.in_row=False
        elif t=="table" and self.in_table:self.in_table=False

def fetch(url):
    req=Request(url,headers={"User-Agent":"HazRob-scientific-library/0.6 (+research prototype; provenance audit)","Accept":"text/html,*/*"})
    with urlopen(req,timeout=45) as r:return r.read()

def parse_limit(raw):
    text=" ".join((raw or "").replace("m^{3}","m3").replace("m^3","m3").replace("m³","m3").split())
    m=NUM_RE.search(text)
    if not m:return None
    value=float(m.group(1).replace(",",""));unit=re.sub(r"\s+"," ",m.group(2)).replace(" / ","/").strip()
    unit=re.sub(r"m\{?3\}?$","m3",unit,flags=re.I)
    return value,unit,text

def main():
    out=Path(sys.argv[1] if len(sys.argv)>1 else "generated/niosh_idlh");out.mkdir(parents=True,exist_ok=True)
    raw=fetch(SOURCE);p=TableParser();p.feed(raw.decode("utf-8",errors="replace"));records=[];substances=0
    for cells in p.rows:
        name,cas_cell,old_cell,new_cell=cells
        cas_match=CAS_RE.search(cas_cell);cas=cas_match.group(1) if cas_match else None
        if not name or (not old_cell and not new_cell):continue
        substances+=1
        new=parse_limit(new_cell);old=parse_limit(old_cell)
        if old:
            value,unit,notation=old;records.append({"name_en":name,"cas":cas,"authority":"NIOSH","jurisdiction":"US","limit_type":"IDLH","value":value,"unit":unit,"duration_min":None,"notation":notation,"revision":"1994","current_policy":False,"source_id":"NIOSH_IDLH"})
        if new:
            value,unit,notation=new;records.append({"name_en":name,"cas":cas,"authority":"NIOSH","jurisdiction":"US","limit_type":"IDLH","value":value,"unit":unit,"duration_min":None,"notation":notation,"revision":"CIB66/current","current_policy":True,"source_id":"NIOSH_IDLH"})
    if substances<300 or len(records)<300:raise RuntimeError(f"IDLH parser produced {substances} substances/{len(records)} numeric records; source layout may have changed")
    generated=datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00","Z");source_hash=hashlib.sha256(raw).hexdigest()
    payload={"schema":"hazrob.exposure-seed.v1","generated_utc":generated,"authority":"NIOSH","dataset":"IDLH","source":SOURCE,"source_sha256":source_hash,"substance_rows":substances,"record_count":len(records),"records":records}
    jp=out/"niosh_idlh.json";jp.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    manifest={"file":jp.name,"sha256":hashlib.sha256(jp.read_bytes()).hexdigest(),"source_sha256":source_hash,"authority":"NIOSH","dataset":"IDLH","substance_rows":substances,"record_count":len(records),"generated_utc":generated,"authoritative":True}
    (out/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8");print(json.dumps(manifest,indent=2));return 0
if __name__=="__main__":raise SystemExit(main())
