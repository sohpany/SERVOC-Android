#!/usr/bin/env python3
"""Classify PubChem-unresolved occupational identities without inventing molecular structures.

This curation layer intentionally separates a pure molecular identity from mixtures, materials,
isomer groups and occupational categories. It does not fabricate PubChem descriptors and does
not make any unresolved entry QSPR-eligible.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

PURE="PURE_COMPOUND"
MIXTURE="MIXTURE"
MATERIAL="MATERIAL"
ISOMER="ISOMER_GROUP"
OCC="OCCUPATIONAL_CATEGORY"

# Exact keys are used because one CAS can legitimately occur in more than one NIOSH identity row.
CURATION={
("10210-68-1","Cobalt carbonyl (as Co)"):(PURE,"Discrete compound identity appears intended; resolve structure by authoritative name/CAS source before molecular modelling. Exposure basis is expressed as Co."),
("11107-01-0","Tungsten carbide (cemented)"):(MATERIAL,"Cemented tungsten carbide is a composite/material identity; do not force a single molecular descriptor vector."),
("12079-65-1","Manganese cyclopentadienyl tricarbonyl (as Mn)"):(PURE,"Discrete organometallic identity appears intended; authoritative structure resolution required before QSPR. Exposure basis is expressed as Mn."),
("12108-13-3","Methyl cyclopentadienyl manganese tricarbonyl (as Mn)"):(PURE,"Discrete organometallic identity appears intended; authoritative structure resolution required before QSPR. Exposure basis is expressed as Mn."),
("12718-69-3","Tungsten carbide (cemented)"):(MATERIAL,"Cemented tungsten carbide is a composite/material identity; do not force a single molecular descriptor vector."),
("1304-82-1","Bismuth telluride, undoped"):(PURE,"Stoichiometric compound identity appears intended, but it is a low-volatility solid and must not be treated as a gas-phase QSPR target without a relevant exposure mechanism."),
("1317-65-3","1317-65-3 (natural)"):(MATERIAL,"Natural calcium-carbonate-bearing material category; composition/mineral form is not represented by a unique small-molecule record."),
("1317-65-3","Limestone"):(MATERIAL,"Limestone is a natural material with variable mineral composition; do not collapse it to one molecular identity."),
("1317-65-3","Marble"):(MATERIAL,"Marble is a natural material/mineral category; do not collapse it to one molecular identity."),
("1321-74-0","1321-74-0 (mixed isomers)"):(ISOMER,"NIOSH identity explicitly denotes mixed isomers; component-resolved records are required for molecular modelling."),
("1332-21-4","Asbestos"):(MATERIAL,"Asbestos is a mineral-fiber category, not a single volatile molecular analyte."),
("1332-58-7","Kaolin"):(MATERIAL,"Kaolin is a mineral material category and is not represented by a single volatile molecular structure."),
("1335-87-1","Hexachloronaphthalene"):(ISOMER,"Technical hexachloronaphthalene can represent an isomeric composition; component/isomer resolution is required before molecular modelling."),
("1395-21-7","1395-21-7 (BPN)"):(MATERIAL,"Biological/protein preparation identity; not a small volatile molecule for MOX molecular QSPR."),
("17702-41-9","Decaborane"):(PURE,"Discrete compound identity appears intended; resolve authoritative structure record before molecular modelling."),
("25013-15-4","25013-15-4 (inhibited)"):(ISOMER,"Industrial inhibited product/isomer composition; resolve actual component identity rather than assigning one molecule."),
("25321-14-6","Dinitrotoluene"):(ISOMER,"Generic dinitrotoluene CAS represents an isomeric/technical composition; component-resolved identities are required for molecular modelling."),
("25376-45-8","Toluenediamine"):(ISOMER,"Generic toluenediamine CAS represents an isomeric/technical composition; component-resolved identities are required for molecular modelling."),
("37329-49-0","Tungsten carbide (cemented)"):(MATERIAL,"Cemented tungsten carbide is a composite/material identity; do not force a single molecular descriptor vector."),
("53469-21-9","Chlorodiphenyl (42% chlorine)"):(MIXTURE,"Technical chlorinated biphenyl mixture defined by chlorine content; not a unique molecule."),
("61788-32-7","Hydrogenated terphenyls"):(MIXTURE,"Hydrogenated terphenyl commercial/technical mixture; component-resolved composition is required for molecular modelling."),
("65996-93-2","Coal tar pitch volatiles"):(OCC,"Occupational exposure category comprising a complex set of volatile/semi-volatile constituents; no single molecular descriptor vector is valid."),
("65997-15-1","Portland cement"):(MATERIAL,"Portland cement is a multicomponent inorganic material; not a unique molecular analyte."),
("68476-85-7","L.P.G."):(MIXTURE,"Liquefied petroleum gas is a composition-dependent hydrocarbon mixture; use component/mixture models, not a single molecule."),
("68956-68-3","Vegetable oil mist"):(OCC,"Occupational aerosol/mist category with composition depending on the source oil; not a unique volatile molecule."),
("8002-05-9","Petroleum distillates (naphtha)"):(MIXTURE,"Petroleum distillate/naphtha is a variable hydrocarbon mixture; component or mixture-specific models are required."),
("8002-74-2","Paraffin wax fume"):(OCC,"Fume from a hydrocarbon material mixture; not a unique molecular analyte."),
("8006-61-9","Gasoline"):(MIXTURE,"Gasoline is a composition-variable multicomponent fuel; retain as mixture and model constituents/mixture fingerprints separately."),
("8006-64-2","Turpentine"):(MIXTURE,"Turpentine is a natural/technical terpene mixture; composition varies and must not be represented by one molecule."),
("8008-20-6","Kerosene"):(MIXTURE,"Kerosene is a composition-variable petroleum fraction; mixture-specific modelling is required."),
("8012-95-1","Oil mist (mineral)"):(OCC,"Occupational mineral-oil aerosol category; not a unique gas-phase molecular analyte."),
("8030-30-6","Naphtha (coal tar)"):(MIXTURE,"Coal-tar naphtha is a multicomponent fraction; not a unique molecular analyte."),
("8032-32-4","VM & P Naphtha"):(MIXTURE,"VM&P naphtha is a variable petroleum solvent mixture; not a unique molecular analyte."),
("8052-41-3","Stoddard solvent"):(MIXTURE,"Stoddard solvent is a petroleum hydrocarbon mixture; mixture-specific composition/model is required."),
("8052-42-4","Asphalt fumes"):(OCC,"Asphalt fume is an occupational emission category from a complex material; no unique molecule represents it."),
("9004-34-6","Cellulose"):(MATERIAL,"Polymeric material; not a unique volatile small molecule for current MOX molecular QSPR."),
("9005-25-8","Starch"):(MATERIAL,"Polymeric/biological material; not a unique volatile small molecule for current MOX molecular QSPR."),
}


def main()->int:
    p=argparse.ArgumentParser()
    p.add_argument("--seed",default="app/src/main/assets/generated/chemcore/molecular_descriptors.json")
    p.add_argument("--output",default="app/src/main/assets/generated/chemcore/unresolved_curation.json")
    args=p.parse_args();seed_path=Path(args.seed);out_path=Path(args.output)
    seed=json.loads(seed_path.read_text(encoding="utf-8"));unresolved=[r for r in seed.get("records",[]) if r.get("status")=="UNRESOLVED"]
    observed={(r.get("cas",""),r.get("name_en","")) for r in unresolved};expected=set(CURATION)
    missing=sorted(observed-expected);stale=sorted(expected-observed)
    if missing or stale:
        raise SystemExit("curation coverage mismatch; unclassified=%r stale=%r"%(missing,stale))
    rows=[];counts={}
    for r in unresolved:
        key=(r["cas"],r["name_en"]);kind,note=CURATION[key];counts[kind]=counts.get(kind,0)+1
        rows.append({
            "cas":r["cas"],"name_en":r["name_en"],"entity_type":kind,
            "molecular_qspr_eligible":False,
            "status":"CURATED_NONRESOLVED_IDENTITY",
            "resolution_policy":"AUTHORITATIVE_STRUCTURE_REQUIRED" if kind==PURE else "DO_NOT_FORCE_SINGLE_MOLECULE",
            "note":note,"identity_source_url":r.get("identity_source_url","")
        })
    payload={
        "schema":"chemcore-unresolved-curation-v1","generated_at_utc":datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00","Z"),
        "source_seed_sha256":hashlib.sha256(seed_path.read_bytes()).hexdigest(),"record_count":len(rows),"entity_type_counts":dict(sorted(counts.items())),
        "policy":"Classification prevents ambiguous occupational/material entries from being coerced into single-molecule QSPR. No record in this file is made QSPR-eligible by curation alone.",
        "records":rows
    }
    out_path.parent.mkdir(parents=True,exist_ok=True);out_path.write_text(json.dumps(payload,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps(payload["entity_type_counts"],indent=2));print("records",len(rows));return 0

if __name__=="__main__":raise SystemExit(main())
