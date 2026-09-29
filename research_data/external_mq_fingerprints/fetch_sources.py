#!/usr/bin/env python3
"""Fetch pinned/authoritative external MQ research datasets for HazRob 0.10.2 / HR069.

This utility is intentionally separate from the Android runtime. It downloads only research data and
never promotes third-party measurements to HazRob quantitative/M4 evidence.

Raw archives are research-cache material. A public download does not by itself authorize Git
redistribution; provenance and license state are recorded separately in manifest.json.
"""

from __future__ import annotations
import argparse,hashlib,json,pathlib,shutil,sys,time,urllib.error,urllib.request
from datetime import datetime, timezone

ROOT=pathlib.Path(__file__).resolve().parent
RAW=ROOT/'raw'

SOURCES={
    'MQDS-001':{
        'urls':['https://raw.githubusercontent.com/TakMashhido/Gas-Sensors-Measurements-Dataset/a4f36dbcdcc650b9f7c7d105846ea1b6d69f76d4/Gas_Sensors_Measurements.csv'],
        'filename':'Gas_Sensors_Measurements.csv','license_verified':True,'expected_md5':None,
        'provenance':{'upstream_commit':'a4f36dbcdcc650b9f7c7d105846ea1b6d69f76d4','doi':'10.5281/zenodo.13283720','license':'MIT'},
    },
    'MQDS-002':{
        'urls':[
            'https://zenodo.org/records/17285312/files/Food-Freshness-E-Nose-Dataset.zip?download=1',
            'https://www.kaggle.com/api/v1/datasets/download/mehrabmahdian/food-freshness-electronic-nose-data',
        ],
        'filename':'Food-Freshness-E-Nose-Dataset.zip','license_verified':False,
        'expected_md5':'008b8ccb62d7a7d51ff9ffcfd698a1e5',
        'provenance':{'doi':'10.5281/zenodo.17285312','version':'v1','creator':'Mehrab Mahdian','license':'EXACT_DATASET_LICENSE_PENDING_DIRECT_CONFIRMATION'},
    },
    'MQDS-003':{
        'urls':['https://archive.ics.uci.edu/static/public/1081/gas%2Bsensor%2Barray%2Blow-concentration.zip'],
        'filename':'gas_sensor_array_low_concentration.zip','license_verified':True,'expected_md5':None,
        'provenance':{'doi':'10.24432/C5CK6F','license':'CC BY 4.0'},
    },
}

def digest(path:pathlib.Path,name:str)->str:
    h=hashlib.new(name)
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''): h.update(chunk)
    return h.hexdigest()

def fetch_url(url:str,tmp:pathlib.Path)->None:
    req=urllib.request.Request(url,headers={'User-Agent':'HazRob-HR069-research-source-fetcher/1.1','Accept':'application/zip,text/csv,application/octet-stream;q=0.9,*/*;q=0.1'})
    with urllib.request.urlopen(req,timeout=180) as response,tmp.open('wb') as out: shutil.copyfileobj(response,out)

def fetch(source_id:str,spec:dict,include_unverified_license:bool,force:bool)->None:
    if not spec['license_verified'] and not include_unverified_license:
        print(f"SKIP {source_id}: exact redistribution/license state is not yet verified; use --include-unverified-license for research-cache acquisition only")
        return
    dest_dir=RAW/source_id;dest_dir.mkdir(parents=True,exist_ok=True);dest=dest_dir/spec['filename'];tmp=dest.with_suffix(dest.suffix+'.tmp')
    used_url=None;errors=[]
    if dest.exists() and not force:
        print(f'KEEP {source_id}: {dest} already exists; use --force to refetch')
    else:
        for attempt,url in enumerate(spec['urls'],1):
            print(f'FETCH {source_id} candidate {attempt}/{len(spec["urls"])}: {url}')
            try:
                if tmp.exists(): tmp.unlink()
                fetch_url(url,tmp);tmp.replace(dest);used_url=url;break
            except Exception as e:
                errors.append(f'{url} -> {type(e).__name__}: {e}')
                if tmp.exists(): tmp.unlink()
                time.sleep(min(2*attempt,5))
        if not dest.exists():
            raise RuntimeError(source_id+': all download candidates failed\n'+'\n'.join(errors))
    md5=digest(dest,'md5');sha256=digest(dest,'sha256');expected=spec.get('expected_md5')
    if expected and md5.lower()!=expected.lower():
        raise RuntimeError(f'{source_id}: MD5 mismatch; expected {expected}, got {md5}. Do not use this download until provenance is resolved.')
    record={'source_id':source_id,'source_url_used':used_url or 'existing-cache','candidate_urls':spec['urls'],'filename':spec['filename'],'retrieved_utc':datetime.now(timezone.utc).isoformat(),'bytes':dest.stat().st_size,'md5':md5,'sha256':sha256,'license_verified':spec['license_verified'],'provenance':spec['provenance'],'hazrob_evidence_class':'RESEARCH_EXTERNAL','authoritative_quantification_allowed':False,'raw_archive_git_redistribution_authorized':bool(spec['license_verified'])}
    (dest_dir/'FETCHED_SOURCE.json').write_text(json.dumps(record,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(f'OK   {source_id}: sha256={sha256}')

def main()->int:
    p=argparse.ArgumentParser();p.add_argument('source',nargs='*',choices=sorted(SOURCES));p.add_argument('--include-unverified-license',action='store_true',help='acquire public archive into local research cache even if exact redistribution terms are pending');p.add_argument('--force',action='store_true');args=p.parse_args()
    for source_id in (args.source or list(SOURCES)): fetch(source_id,SOURCES[source_id],args.include_unverified_license,args.force)
    return 0
if __name__=='__main__': sys.exit(main())
