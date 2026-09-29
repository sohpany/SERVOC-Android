#!/usr/bin/env python3
"""Derive source-preserving MQ135 transfer features from UCI dataset 1081.

The UCI source contains 10 MOS channels and only MQ135 overlaps HR069. The source records
50/100/200 ppb exposures, explicitly below nominal sensor detection limits. Therefore this tool
never converts the response into HR069 Rs/R0, never fills missing channels, and never marks M4.
It produces a single-channel transfer fingerprint/shape bank useful for corroboration and future
transfer calibration.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,math,pathlib,statistics,zipfile
from datetime import datetime,timezone

RELEASE="0.10.2"; HW="HR069"
SOURCE_ID="MQDS-003"; DOI="10.24432/C5CK6F"
SENSOR_NAMES=["TGS2603","TGS2630","TGS813","TGS822","MQ135","MQ137","MQ138","2M012","VOCS-P","2SH12"]
POINTS_PER_SENSOR=900; BASELINE_N=300; EXPOSURE_N=600; MQ135_INDEX=4
LEGACY={"ethanol":103,"acetone":110,"toluene":95,"ethyl_acetate":115,"ethyl acetate":115,
        "isopropanol":105,"n-hexane":84,"hexane":84}

def finite_mean(xs):
    q=[x for x in xs if math.isfinite(x)]
    return statistics.fmean(q) if q else math.nan

def finite_sd(xs):
    q=[x for x in xs if math.isfinite(x)]
    return statistics.stdev(q) if len(q)>1 else (0.0 if q else math.nan)

def md5(path):
    h=hashlib.md5()
    with open(path,'rb') as f:
        for b in iter(lambda:f.read(1<<20),b''):h.update(b)
    return h.hexdigest()

def sha256(path):
    h=hashlib.sha256()
    with open(path,'rb') as f:
        for b in iter(lambda:f.read(1<<20),b''):h.update(b)
    return h.hexdigest()

def norm_label(s):return str(s).strip().lower().replace('-','_').replace(' ','_')

def concentration_ppm(x):
    s=str(x).strip().lower().replace(' ','')
    if s.endswith('ppb'):return float(s[:-3])/1000.0
    if s.endswith('ppm'):return float(s[:-3])
    v=float(s)
    # UCI labels are ppb; plain 50/100/200 values therefore map to ppm.
    return v/1000.0 if v>=10 else v

def sample_features(series):
    if len(series)!=POINTS_PER_SENSOR:raise ValueError(f'expected {POINTS_PER_SENSOR} MQ135 points, got {len(series)}')
    baseline=series[:BASELINE_N]; exposure=series[BASELINE_N:]
    b=finite_mean(baseline); bsd=finite_sd(baseline)
    early=finite_mean(exposure[:60]); late=finite_mean(exposure[-60:]); emean=finite_mean(exposure)
    if not math.isfinite(b) or abs(b)<1e-15: raise ValueError('invalid baseline')
    rel=[(x-b)/abs(b) for x in exposure if math.isfinite(x)]
    signed_peak=max(rel,key=lambda x:abs(x)) if rel else math.nan
    ratio=late/b if math.isfinite(late) and b!=0 else math.nan
    # Linear trend over the final minute, in baseline fractions per second.
    tail=exposure[-60:]; n=len(tail); sx=n*(n-1)/2; sxx=(n-1)*n*(2*n-1)/6
    sy=sum(tail); sxy=sum(i*y for i,y in enumerate(tail)); den=n*sxx-sx*sx
    slope=(n*sxy-sx*sy)/den/abs(b) if den and b else math.nan
    return {"baseline_mean_raw":b,"baseline_sd_raw":bsd,"early_60s_mean_raw":early,
            "exposure_mean_raw":emean,"endpoint_60s_mean_raw":late,
            "endpoint_over_baseline":ratio,"endpoint_delta_fraction":(late-b)/abs(b),
            "max_abs_delta_fraction_signed":signed_peak,"endpoint_slope_fraction_per_s":slope}

def locate_csv(z):
    names=[n for n in z.namelist() if pathlib.PurePosixPath(n).name.lower()=='gsalc.csv']
    if not names:raise FileNotFoundError('gsalc.csv not found in archive')
    return names[0]

def main():
    ap=argparse.ArgumentParser();ap.add_argument('archive');ap.add_argument('--out',required=True);args=ap.parse_args()
    archive=pathlib.Path(args.archive);out=pathlib.Path(args.out); groups={}
    with zipfile.ZipFile(archive) as z:
        member=locate_csv(z)
        text=z.read(member).decode('utf-8-sig',errors='strict').splitlines()
    for row in csv.reader(text):
        if not row:continue
        try:
            gas=norm_label(row[0]); ppm=concentration_ppm(row[1]); values=[float(x) for x in row[2:]]
        except (ValueError,IndexError):
            # tolerate an optional header row only
            continue
        if len(values)!=len(SENSOR_NAMES)*POINTS_PER_SENSOR:
            raise ValueError(f'{gas} {ppm}: expected 9000 response points, got {len(values)}')
        off=MQ135_INDEX*POINTS_PER_SENSOR; series=values[off:off+POINTS_PER_SENSOR]
        key=(gas,ppm);groups.setdefault(key,[]).append(sample_features(series))
    records=[]
    feature_names=["baseline_mean_raw","baseline_sd_raw","early_60s_mean_raw","exposure_mean_raw","endpoint_60s_mean_raw",
                   "endpoint_over_baseline","endpoint_delta_fraction","max_abs_delta_fraction_signed","endpoint_slope_fraction_per_s"]
    for (gas,ppm),rows in sorted(groups.items(),key=lambda x:(x[0][0],x[0][1])):
        legacy=LEGACY.get(gas)
        rec={"gas":gas,"legacy_id":legacy,"concentration_ppm":ppm,"concentration_ppb":ppm*1000.0,
             "replicate_count":len(rows),"features":{}}
        for name in feature_names:
            vals=[r[name] for r in rows]
            rec["features"][name]={"mean":finite_mean(vals),"sd":finite_sd(vals)}
        records.append(rec)
    gas_names=sorted({r['gas'] for r in records})
    payload={
      "schema":"hazrob.external-single-channel-transfer-fingerprints/1",
      "release":RELEASE,"hardware_revision":HW,"source_id":SOURCE_ID,"doi":DOI,
      "archive":{"filename":archive.name,"md5":md5(archive),"sha256":sha256(archive)},
      "source_sensor_order":SENSOR_NAMES,"source_points_per_sensor":POINTS_PER_SENSOR,
      "overlap_with_hr069":["MQ135"],"overlap_count":1,"mq135_source_index":MQ135_INDEX,
      "sampling":{"baseline_seconds":BASELINE_N,"exposure_seconds":EXPOSURE_N,"sample_hz":1},
      "concentrations_ppm":[0.05,0.1,0.2],"gas_count":len(gas_names),"record_count":len(records),
      "gases":gas_names,"records":records,
      "acceptance_state":"ACCEPTED_TRANSFER_REFERENCE",
      "identity_reference_accepted":True,
      "direct_hr069_named_identity":False,
      "reason_direct_identity_withheld":"Only MQ135 overlaps HR069; the >=2 independent-channel named-identity gate is intentionally retained.",
      "vector_semantics":"source MQ135 raw-response dynamics normalized to each sample baseline; not HR069 Rs/R0",
      "source_limitation":"UCI states 50/100/200 ppb are below nominal sensor detection limits; use as low-concentration transfer/shape evidence, not authoritative ppm calibration.",
      "quantitative_ppm":False,"m4":False,
      "generated_utc":datetime.now(timezone.utc).isoformat()
    }
    out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(payload,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps({"records":len(records),"gases":gas_names,"overlap":1,"m4":False},ensure_ascii=False))
if __name__=='__main__':main()
