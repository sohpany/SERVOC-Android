# Chemical Library and 144-Panel Contract — 0.10.2

Reviewed against HR067 commit `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.

## Two distinct coverage domains
HazRob must preserve the distinction between:

1. **Chemical knowledge coverage** — identity, identifiers, occupational limits, hazard metadata and provenance.
2. **Analytical coverage** — whether the installed MQ array has defensible evidence to identify and/or quantify that substance.

Presence in the scientific database MUST NOT be interpreted as sensor detectability.

## Legacy 144 panel
The legacy analytical-priority catalogue remains versioned as:
`HAZROB-CHEM-144-v3`.

It is a compatibility/analytical-priority panel, not the structural size of the scientific database.

`ChemicalCoverage` explicitly separates:
- target-backed direct curve candidates;
- manufacturer-graph cross-response evidence;
- community/research-only curves;
- empirical fingerprint/calibration candidates;
- aggregates/mixtures;
- weak or unsupported entries.

`MqSensorAffinity` retains unsupported substances in the catalogue rather than fabricating a PPM model.

## Dynamic scientific database
The HR067 architecture removes the structural 144-entry limit. Dynamic chemical records use database UID/CAS/name/synonym resolution while retaining legacy IDs only where required for compatibility.

Pinned HR067 scientific snapshots documented in the integration audit:
- NIOSH chemical identity records: 652
- NIOSH IDLH records: 402
- OSHA Table Z-1 source substance rows: 432
- normalized OSHA limit records: 638

The OSHA snapshot records SHA-256:
`4dc62e78ef823071c9a998b35730acfe14d11a6a77a2975f3b762f2301c588fd`.

## Authority separation
OSHA, NIOSH and other future authorities must remain separate provenance records. Values from different authorities/jurisdictions are not collapsed into one anonymous threshold.

The OSHA importer intentionally excludes comparison columns belonging to other authorities rather than mixing them into OSHA records.

## MQDataScience
The scientific registry retains MQDataScience v6.0.0 with 76 registered algorithms/models pinned to commit:
`e88f876140587a708ee84a4d4d3acbd5720274ee`.

Registration, Android implementation and physical validation are distinct states. A registered model is not automatically an accepted quantitative model.

## 0.10.2 invariants
- Do not impose a 144-record ceiling on the knowledge database.
- Do not claim that all knowledge-base chemicals are detectable by the installed array.
- Do not promote cross-response/community evidence to quantitative PPM without accepted calibration/domain evidence.
- Do not remove unsupported chemicals merely because the current array cannot measure them.
- Do not silently replace pinned scientific datasets; updates require provenance, row-count/schema validation, hashes and release documentation.
- Preserve historical legacy IDs for model/report compatibility while using stable dynamic chemical UIDs for new records.
