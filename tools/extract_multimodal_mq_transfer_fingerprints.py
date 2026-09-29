#!/usr/bin/env python3
"""Derive source-normalized transfer fingerprints from the seven-MQ gas-class dataset.

Input columns: MQ2, MQ3, MQ5, MQ6, MQ7, MQ8, MQ135 plus class. HR069 overlaps six
channels (MQ2, MQ3, MQ5, MQ7, MQ8, MQ135); MQ4/MQ9 are deliberately left missing.
The output keeps raw-source statistics and two dimensionless source-normalized shapes.
It is transfer/class evidence only, not Rs/R0, pure-gas identity, PPM, or M4.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,math,pathlib,statistics
from datetime import datetime,timezone

RELEASE="0.10.2";HW="HR069";SOURCE_ID="MQDS-001"
SOURCE_SENSORS=["MQ2","MQ3","MQ5","MQ6","MQ7","MQ8","MQ135"]
HR069_ORDER=["MQ2","MQ3","MQ4","MQ5","MQ7","MQ8","MQ9","MQ135"]
OVERLAP=["MQ2","MQ3","MQ5","MQ7","MQ8","MQ135"]

def dig(path,kind):
    h=hashlib.new(kind)
    with open(path,'rb') as f:
        for b in iter(lambda:f.read(1<<20),b''):h.update(b)
    return h.hexdigest()

def mean(xs):return statistics.fmean(xs) if xs else math.nan
def sd(xs):return statistics.stdev(xs) if len(xs)>1 else (0.0 if xs else math.nan)
def geomean(xs):return math.exp(sum(math.log(x) for x in xs)/len(xs))
def norm_class(s):return str(s).strip().replace(' ','')

def main():
    ap=argparse.ArgumentParser();ap.add_argument('csv');ap.add_argument('--out',required=True);args=ap.parse_args()
    src=pathlib.Path(args.csv);out=pathlib.Path(args.out);by_class={}
    with src.open('r',encoding='utf-8-sig',newline='') as f:
        r=csv.DictReader(f)
        for row in r:
            cls=norm_class(row.get('Gas',''))
            if not cls:continue
            values={s:float(row[s]) for s in SOURCE_SENSORS}
            by_class.setdefault(cls,[]).append(values)
    if 'NoGas' not in by_class:raise ValueError('NoGas baseline class missing')
    baseline={s:mean([r[s] for r in by_class['NoGas']]) for s in OVERLAP}
    records=[]
    for cls,rows in sorted(by_class.items()):
        means={s:mean([r[s] for r in rows]) for s in OVERLAP}
        sds={s:sd([r[s] for r in rows]) for s in OVERLAP}
        gm=geomean([means[s] for s in OVERLAP])
        cross={s:math.log10(means[s]/gm) for s in OVERLAP}
        rel={s:math.log10(means[s]/baseline[s]) for s in OVERLAP}
        hr069_cross=[];hr069_rel=[]
        for s in HR069_ORDER:
            hr069_cross.append(cross.get(s,None));hr069_rel.append(rel.get(s,None))
        records.append({"class":cls,"sample_count":len(rows),"source_mean_raw":means,"source_sd_raw":sds,
                        "log10_cross_sensor_geomean_shape_hr069_order":hr069_cross,
                        "log10_class_mean_over_nogas_mean_hr069_order":hr069_rel})
    payload={
      "schema":"hazrob.external-mq-class-transfer-fingerprints/1","release":RELEASE,"hardware_revision":HW,
      "source_id":SOURCE_ID,"source_file":src.name,"source_file_md5":dig(src,'md5'),"source_file_sha256":dig(src,'sha256'),
      "source_sensor_order":SOURCE_SENSORS,"hr069_sensor_order":HR069_ORDER,"overlap_sensors":OVERLAP,"overlap_count":len(OVERLAP),
      "missing_hr069_channels":["MQ4","MQ9"],"class_count":len(records),"total_samples":sum(x['sample_count'] for x in records),
      "records":records,"acceptance_state":"ACCEPTED_TRANSFER_CLASS_FINGERPRINT",
      "identity_reference_accepted":True,"direct_hr069_named_identity":False,
      "reason_direct_identity_withheld":"Source uses a different instrument/ADC chain and lacks MQ4/MQ9; no missing channels are synthesized. A local transfer transform is required before direct HR069 classification.",
      "vector_semantics":"source ADC class means normalized within-source by cross-sensor geomean and by NoGas class means; not Rs/R0",
      "class_semantics":{"NoGas":"neutral/source baseline","Perfume":"perfume source; not a pure chemical label","Smoke":"composite smoke class","Mixture":"perfume + smoke composite class"},
      "quantitative_ppm":False,"m4":False,"generated_utc":datetime.now(timezone.utc).isoformat()
    }
    out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(payload,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps({"classes":{x['class']:x['sample_count'] for x in records},"overlap":OVERLAP,"m4":False},ensure_ascii=False))
if __name__=='__main__':main()
