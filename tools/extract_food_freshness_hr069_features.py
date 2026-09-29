#!/usr/bin/env python3
"""Derive non-M4 HazRob fingerprint features from the Food Freshness E-Nose archive.

Input is the original `Food-Freshness-E-Nose-Dataset.zip`. The script preserves source
provenance and emits only reproducible RESEARCH_EXTERNAL session-level features. It does
not infer gas identity, concentration, PPM, exposure, or M4 calibration from food-headspace
labels.
"""
from __future__ import annotations

import argparse, csv, hashlib, io, json, math, statistics, zipfile
from pathlib import Path

EXPECTED_MD5 = "008b8ccb62d7a7d51ff9ffcfd698a1e5"
SOURCE_ID = "FOOD_FRESHNESS_2025"
SOURCE_DOI = "10.5281/zenodo.17285312"
SENSORS = ["MQ2","MQ3","MQ4","MQ5","MQ6","MQ7","MQ8","MQ9","MQ135"]
HR069 = ["MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"]


def digest(path: Path, algo: str) -> str:
    h=hashlib.new(algo)
    with path.open("rb") as f:
        for block in iter(lambda:f.read(1024*1024),b""): h.update(block)
    return h.hexdigest()


def clean_float(v: str):
    try:
        x=float(v)
        return x if math.isfinite(x) else None
    except Exception:
        return None


def q(values, p):
    if not values: return None
    s=sorted(values)
    if len(s)==1:return s[0]
    k=(len(s)-1)*p; a=math.floor(k); b=math.ceil(k)
    if a==b:return s[a]
    return s[a]*(b-k)+s[b]*(k-a)


def geometric_shape(means: dict[str,float], sensors: list[str]) -> dict[str,float]:
    valid=[means[s] for s in sensors if means.get(s) is not None and means[s]>0]
    if not valid:return {}
    center=math.exp(sum(math.log(x) for x in valid)/len(valid))
    return {s:means[s]/center for s in sensors if means.get(s) is not None and means[s]>0}


def analyze_csv(zf: zipfile.ZipFile, info: zipfile.ZipInfo) -> dict:
    with zf.open(info) as raw:
        text=io.TextIOWrapper(raw,encoding="utf-8-sig",errors="replace",newline="")
        r=csv.DictReader(text)
        cols={c.strip():c for c in (r.fieldnames or [])}
        missing=[s for s in SENSORS if s not in cols]
        if missing: raise ValueError(f"{info.filename}: missing sensors {missing}")
        ticks=[]; data={s:[] for s in SENSORS}
        for row in r:
            t=clean_float(row.get(cols.get("Ticks",""),"")) if "Ticks" in cols else None
            if t is not None:ticks.append(t)
            for s in SENSORS:
                v=clean_float(row.get(cols[s],""))
                if v is not None:data[s].append(v)
    means={s:(statistics.fmean(data[s]) if data[s] else None) for s in SENSORS}
    stats={}
    for s in SENSORS:
        v=data[s]
        stats[s]={
            "n":len(v),"mean":statistics.fmean(v) if v else None,"median":statistics.median(v) if v else None,
            "stdev":statistics.stdev(v) if len(v)>1 else 0.0 if v else None,
            "p05":q(v,.05),"p95":q(v,.95),"min":min(v) if v else None,"max":max(v) if v else None,
        }
    dt=[]
    if len(ticks)>1:
        dt=[b-a for a,b in zip(ticks,ticks[1:]) if b>a]
    duration=(ticks[-1]-ticks[0]) if len(ticks)>1 else None
    return {
        "session":info.filename,"rows":max((len(x) for x in data.values()),default=0),
        "duration_ticks":duration,"median_tick_interval":statistics.median(dt) if dt else None,
        "sensor_stats":stats,
        "shape_all9_geomean_normalized":geometric_shape(means,SENSORS),
        "shape_hr069_8_geomean_normalized":geometric_shape(means,HR069),
    }


def main() -> int:
    ap=argparse.ArgumentParser()
    ap.add_argument("archive",type=Path)
    ap.add_argument("--out",type=Path,required=True)
    ap.add_argument("--allow-md5-mismatch",action="store_true")
    args=ap.parse_args()
    if not args.archive.is_file():raise SystemExit(f"missing archive: {args.archive}")
    md5=digest(args.archive,"md5")
    if md5.lower()!=EXPECTED_MD5 and not args.allow_md5_mismatch:
        raise SystemExit(f"Food Freshness MD5 mismatch: {md5} != {EXPECTED_MD5}")
    sessions=[]
    with zipfile.ZipFile(args.archive) as zf:
        infos=[x for x in zf.infolist() if not x.is_dir() and x.filename.lower().endswith('.csv')]
        for info in sorted(infos,key=lambda x:x.filename.lower()):sessions.append(analyze_csv(zf,info))
    payload={
        "schema":"hazrob.external-fingerprint-features.v1",
        "project":"HazRob","release":"0.10.2","hardware_revision":"HR069",
        "source":{"id":SOURCE_ID,"doi":SOURCE_DOI,"archive":args.archive.name,"md5":md5,"sha256":digest(args.archive,"sha256")},
        "evidence_class":"RESEARCH_EXTERNAL","m4":False,"quantitative_ppm":False,
        "semantics":"food/headspace session fingerprints; not pure-gas calibration",
        "sensor_order":SENSORS,"hr069_sensor_order":HR069,"session_count":len(sessions),"sessions":sessions,
    }
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(payload,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")
    print(json.dumps({"output":str(args.out),"sessions":len(sessions),"archive_md5":md5,"archive_sha256":payload['source']['sha256'],"output_sha256":digest(args.out,'sha256')},indent=2))
    return 0

if __name__=="__main__": raise SystemExit(main())
