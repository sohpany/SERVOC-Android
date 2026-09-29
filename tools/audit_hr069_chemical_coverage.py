#!/usr/bin/env python3
"""Offline integrity audit for HazRob 0.10.2 / HR069 analytical evidence coverage.

The audit ties together the 144-entry legacy catalogue, 652-record NIOSH identity seed,
external MQ/MOS evidence, fingerprint datasets, the canonical 43 research curves and the
full 76-model MQDataScience catalogue. External evidence is never allowed to claim M4.
"""
from __future__ import annotations
import argparse,json,re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CATALOG=ROOT/'app/src/main/java/com/servoc/app/chemical/ChemicalCatalog.java'
GAS_ENGINE=ROOT/'app/src/main/java/com/servoc/app/chemical/GasEngine.java'
DB=ROOT/'app/src/main/java/com/servoc/app/data/ExperimentsDb.java'
NIOSH=ROOT/'app/src/main/assets/generated/niosh/niosh_chemicals.json'
NIOSH_MANIFEST=ROOT/'app/src/main/assets/generated/niosh/manifest.json'
REGISTRY=ROOT/'research_data/chemical_coverage_hr069/evidence_registry.json'
DATASET_REGISTRY=ROOT/'research_data/external_mq_fingerprints/manifest.json'
CURVE_REGISTRY=ROOT/'research_data/hr069_analytical_library/mq_research_curves_43.json'
MODEL_REGISTRY=ROOT/'research_data/hr069_analytical_library/mq_models_76.json'

def fail(msg:str)->None: raise SystemExit('HR069 CHEMICAL COVERAGE AUDIT FAILED: '+msg)
def load(p:Path)->dict: return json.loads(p.read_text(encoding='utf-8'))

def java_string_array(text:str,symbol:str)->list[str]:
    m=re.search(r"public\s+static\s+final\s+String\[\]\s+"+re.escape(symbol)+r"\s*=\s*\{(.*?)\};",text,re.S)
    if not m: fail(f'cannot parse ChemicalCatalog.{symbol}')
    vals=re.findall(r'"((?:\\.|[^"\\])*)"',m.group(1)); return [v.replace('\\"','"').replace('\\\\','\\') for v in vals]

def gas_engine_supported_ids()->set[int]:
    return {int(x) for x in re.findall(r"new\s+Curve\(MQ(?:2|3|4|5|7|8|9|135)\s*,\s*(\d+)\s*,",GAS_ENGINE.read_text(encoding='utf-8'))}

def main()->int:
    ap=argparse.ArgumentParser();ap.add_argument('--check',action='store_true');args=ap.parse_args();problems=[]
    names=java_string_array(CATALOG.read_text(encoding='utf-8'),'NAMES'); niosh=load(NIOSH); nmanifest=load(NIOSH_MANIFEST); reg=load(REGISTRY); datasets=load(DATASET_REGISTRY); curvepack=load(CURVE_REGISTRY); modelpack=load(MODEL_REGISTRY)
    nrecords=niosh.get('records',[]); cas_set={str(r.get('cas','')).strip() for r in nrecords if str(r.get('cas','')).strip()}; cross=reg.get('chemical_crosswalk',[]); pure=reg.get('legacy_sensor_crosswalk',[]); preferred=reg.get('preferred_mq135_curves',[]); sources=reg.get('sources',[]); dataset_sources=datasets.get('sources',[])
    source_ids={s.get('id') for s in sources}; cross_ids={int(x['legacy_id']) for x in cross}; pure_ids={int(x['legacy_id']) for x in pure}; gas_ids=gas_engine_supported_ids()

    if len(names)!=144: problems.append(f'legacy catalogue count {len(names)} != 144')
    if len(nrecords)!=652 or int(niosh.get('record_count',-1))!=652 or int(nmanifest.get('record_count',-1))!=652: problems.append('NIOSH identity layer must remain exactly 652 records')
    if reg.get('policy',{}).get('external_m4_allowed') is not False or any(bool(s.get('m4')) for s in sources): problems.append('external evidence must never claim M4')
    if len(preferred)!=6: problems.append(f'preferred peer-reviewed MQ135 curve count {len(preferred)} != 6')
    if gas_ids!={20,21,23,34,75,76,78,84,95,103,110,135,139}: problems.append(f'unexpected GasEngine chemical IDs: {sorted(gas_ids)}')

    # Portable analytical-library contracts.
    curves=curvepack.get('curves',[])
    if curvepack.get('release')!='0.10.2' or curvepack.get('hardware_revision')!='HR069': problems.append('43-curve registry release identity mismatch')
    if curvepack.get('m4') is not False or int(curvepack.get('curve_count',-1))!=43 or len(curves)!=43: problems.append('canonical research curve registry must contain exactly 43 non-M4 curves')
    sensors={c.get('sensor') for c in curves}
    if sensors!={'MQ2','MQ3','MQ4','MQ5','MQ7','MQ8','MQ9','MQ135'}: problems.append(f'43-curve registry sensor set mismatch: {sorted(sensors)}')
    for i,c in enumerate(curves):
        if not (float(c.get('ratio_a',0))>0 and float(c.get('ratio_b',0))<0 and float(c.get('max_ppm',0))>float(c.get('min_ppm',0))>0): problems.append(f'invalid research curve at index {i}')

    models=modelpack.get('models',[])
    expected_counts=(76,14,12,2,62); actual=(int(modelpack.get('model_count',-1)),int(modelpack.get('android_native_count',-1)),int(modelpack.get('upstream_verified_count',-1)),int(modelpack.get('approximation_count',-1)),int(modelpack.get('android_port_pending_count',-1)))
    if actual!=expected_counts or len(models)!=76 or [m.get('index') for m in models]!=list(range(76)): problems.append(f'76-model registry contract mismatch: {actual}, rows={len(models)}')
    if not all(bool(m.get('adopted')) and bool(m.get('research_integrated')) for m in models): problems.append('all 76 models must remain adopted/research-integrated')

    # Stable legacy UID mapping contract used by generated ChemCore response priors.
    dbtext=DB.read_text(encoding='utf-8')
    if 'for(int i=0;i<ChemicalCatalog.COUNT;i++)' not in dbtext or 'v.put("legacy_id",i)' not in dbtext or 'db.insertOrThrow("chemicals",null,v)' not in dbtext: problems.append('legacy chemical bootstrap order changed; chemical_uid=legacy_id+1 response-prior mapping must be reworked')

    def validate_crosswalk(rows,label,require_cas):
        seen=set()
        for row in rows:
            idx=int(row['legacy_id'])
            if idx in seen: problems.append(f'duplicate {label} legacy_id {idx}')
            seen.add(idx)
            if not 0<=idx<len(names): problems.append(f'{label} id {idx} out of range'); continue
            if names[idx]!=row.get('name'): problems.append(f'{label} name mismatch {idx}')
            cas=str(row.get('cas','')).strip()
            if require_cas and not cas: problems.append(f'{label} {idx} missing CAS')
            if cas and cas not in cas_set: problems.append(f'{label} {idx} CAS absent from NIOSH seed')
    validate_crosswalk(cross,'external crosswalk',False);validate_crosswalk(pure,'legacy sensor crosswalk',True)
    for row in cross:
        for sid in row.get('sources',[]):
            if sid not in source_ids: problems.append(f"crosswalk {row['legacy_id']} references unknown source {sid}")

    seen=set()
    for c in preferred:
        idx=int(c['legacy_id']);seen.add(idx);lo,hi=c.get('domain_ppm',[None,None])
        if c.get('source')!='UMBU_MQ135_2023' or lo!=10 or hi!=100 or not(float(c.get('a',0))>0 and float(c.get('b',0))<0): problems.append(f'invalid preferred MQ135 curve {idx}')
        if idx not in cross_ids: problems.append(f'preferred curve {idx} missing external crosswalk')
    if seen!={20,21,23,95,103,110}: problems.append(f'preferred curve IDs mismatch: {sorted(seen)}')
    expected_pure={20,21,23,34,75,76,78,79,84,95,102,103,105,110,115}
    if pure_ids!=expected_pure: problems.append(f'legacy sensor crosswalk mismatch: {sorted(pure_ids)}')

    # Dataset provenance / duplicate / Food Freshness contracts.
    ds={d.get('id'):d for d in dataset_sources}; canonical=ds.get('MQDS-004',{}); derivative=ds.get('MQDS-001',{}); food=ds.get('MQDS-002',{})
    if canonical.get('independent_evidence_weight')!=1 or derivative.get('independent_evidence_weight')!=0 or derivative.get('possible_parent_source')!='MQDS-004': problems.append('canonical/derivative fingerprint weighting contract violated')
    expected_array={'MQ2','MQ3','MQ4','MQ5','MQ7','MQ8','MQ9','MQ135'}
    if set(food.get('hr069_overlap',[]))!=expected_array or food.get('published_md5')!='008b8ccb62d7a7d51ff9ffcfd698a1e5': problems.append('Food Freshness HR069 overlap/hash contract mismatch')
    if food.get('quantitative_ppm_eligible') is not False: problems.append('Food Freshness must not be direct quantitative PPM evidence')

    report={'release':reg.get('release'),'hardware_revision':reg.get('hardware_revision'),'legacy_catalogue_records':len(names),'niosh_identity_records':len(nrecords),'gas_engine_curve_chemicals':len(gas_ids),'research_curve_count':len(curves),'mq_model_count':len(models),'android_native_models':modelpack.get('android_native_count'),'external_sources_registered':len(sources),'external_dataset_sources':len(dataset_sources),'legacy_chemicals_with_verified_external_evidence':len(cross_ids),'peer_reviewed_preferred_mq135_curves':len(preferred),'food_freshness_hr069_overlap':len(food.get('hr069_overlap',[])),'external_m4_allowed':reg.get('policy',{}).get('external_m4_allowed'),'problems':problems}
    print(json.dumps(report,ensure_ascii=False,indent=2,sort_keys=True))
    if args.check and problems: fail('; '.join(problems))
    return 0
if __name__=='__main__': raise SystemExit(main())
