#!/usr/bin/env python3
"""Build ChemCore-compatible HR069 research response priors from the pinned 43 MQ curves.

The output is deliberately non-M4. It also overlays six higher-provenance peer-reviewed
MQ135 graph fits from Umbu 2023 in 10..100 ppm and keeps the corresponding pinned
MQDataScience curve only above 100 ppm, so the same physical sensor/analyte is never
double-counted in an overlapping concentration domain.
"""
from __future__ import annotations
import argparse, json, math
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DEFAULT_CURVES=ROOT/'research_data/hr069_analytical_library/mq_research_curves_43.json'

LABELS={20:'Carbon Monoxide',21:'Ammonia',23:'Carbon Dioxide',34:'Benzene',75:'Hydrogen',76:'Methane',78:'Propane',84:'n-Hexane',95:'Toluene',103:'Ethanol',110:'Acetone',135:'LPG',139:'Smoke'}
# legacy_id: (a,b,min_ppm,max_ppm,r2,label)
UMBU={
  110:(2.7295,-0.2822,10.0,100.0,0.9996,'Acetone'),
  95 :(2.9630,-0.2838,10.0,100.0,0.9998,'Toluene'),
  103:(3.8659,-0.3091,10.0,100.0,0.9996,'Alcohol / ethanol mapping'),
  23 :(4.9999,-0.3387,10.0,100.0,0.9998,'Carbon dioxide'),
  21 :(6.4835,-0.4060,10.0,100.0,0.9996,'Ammonia'),
  20 :(5.2629,-0.2668,10.0,100.0,0.9992,'Carbon monoxide'),
}

def prior(*,legacy_id:int,label:str,sensor:str,intercept:float,slope:float,sigma:float,lo:float,hi:float,tier:str,version:str,source:str,note:str,minimum:int=1,weight:float=-0.5,doi:str='')->dict:
    # The legacy catalogue is seeded first into an empty chemicals table in ascending legacy_id;
    # therefore the initial SQLite INTEGER PRIMARY KEY uid is legacy_id+1. This contract is
    # separately audited and the legacy_id is preserved in every record for reconciliation.
    return {
      'chemical_uid':legacy_id+1,'legacy_id':legacy_id,'label':label,'sensor_key':sensor.lower(),
      'form':'LOG10_LINEAR','intercept':intercept,'slope':slope,'sigma_log10':sigma,
      'min_ppm':lo,'max_ppm':hi,'evidence_tier':tier,'device_validated':False,
      'model_version':version,'source_id':source,'minimum_contributors':minimum,
      'prior_log_weight':weight,'doi':doi,'note':note,
    }

def main()->int:
    ap=argparse.ArgumentParser();ap.add_argument('--curves',type=Path,default=DEFAULT_CURVES);ap.add_argument('--out',type=Path,required=True);args=ap.parse_args()
    root=json.loads(args.curves.read_text(encoding='utf-8'));curves=root.get('curves',[])
    if root.get('curve_count')!=43 or len(curves)!=43: raise SystemExit('expected exactly 43 pinned research curves')
    rows=[]
    # Higher-provenance non-M4 literature priors first.
    for cid,(a,b,lo,hi,r2,glabel) in UMBU.items():
        rows.append(prior(legacy_id=cid,label=LABELS[cid],sensor='mq135',intercept=math.log10(a),slope=b,sigma=.12,lo=lo,hi=hi,tier='M2',version='UMBU_MQ135_2023',source='UMBU_MQ135_2023',doi='10.23960/jtaf.v11i2.314',weight=-.1,note=f'Peer-reviewed datasheet-graph fit ({glabel}); R2={r2}; external prior, not M4.'))
    community=0
    for c in curves:
        cid=int(c['legacy_id']);sensor=str(c['sensor']).lower();a=float(c['ratio_a']);b=float(c['ratio_b']);scale=float(c.get('output_scale',1));lo=float(c['min_ppm']);hi=float(c['max_ppm'])
        if sensor=='mq135' and cid in UMBU:
            lo=max(lo,100.000001)  # no overlap with the preferred literature interval
            if lo>=hi: continue
        # Source semantics: ppm = ((ratio/a)^(1/b))*scale => ratio=a*(ppm/scale)^b.
        intercept=math.log10(a)-b*math.log10(scale)
        rows.append(prior(legacy_id=cid,label=LABELS.get(cid,str(c.get('gas_label','Chemical'))),sensor=sensor,intercept=intercept,slope=b,sigma=.25,lo=lo,hi=hi,tier='M1',version='MQDataScience-6.0.0-e88f876',source='MQDATASCIENCE_6_0_0',minimum=2 if cid in (135,139) else 1,note=str(c.get('note') or 'Pinned digitized response-graph prior; research-only until HR069 validation.')))
        community+=1
    if community!=43: raise SystemExit(f'expected all 43 pinned research curves, got {community}')
    out={
      'schema':'hazrob.chemcore-response-priors.v1','release':'0.10.2','hardware_revision':'HR069',
      'policy':{'external_m4':False,'same_sensor_overlapping_domain_allowed':False,'legacy_uid_contract':'chemical_uid=legacy_id+1 for the 144 legacy bootstrap rows; legacy_id retained for reconciliation'},
      'source_curve_count':43,'preferred_literature_overlay_count':6,'record_count':len(rows),'records':rows,
    }
    args.out.parent.mkdir(parents=True,exist_ok=True);args.out.write_text(json.dumps(out,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
    print(json.dumps({'records':len(rows),'source_curves':43,'literature_overlays':6,'m4':False},indent=2));return 0
if __name__=='__main__': raise SystemExit(main())
