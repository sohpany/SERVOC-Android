# Chemical Library Scope Contract — 0.10.2

Reviewed against HR067 commit `2ac75492ca3dc0eafcfe8328dd2c417e2f9aa2c9`.

## Three scopes must remain separate

### 1. Analytical-priority catalogue
`ChemicalCatalog` contains 144 entries and is versioned by `LibraryStatus` as `HAZROB-CHEM-144-v3`.

The 144 entries are NOT 144 validated PPM curves. Each entry has an analytical status/evidence path.

### 2. Scientific identity knowledge base
The generated NIOSH identity seed at the HR067 baseline contains 652 records and carries authoritative provenance plus source/content SHA-256.

Presence in this knowledge base means HazRob knows the chemical identity/reference record. It does NOT mean the installed MQ array can detect, identify or quantify it.

### 3. Occupational exposure/reference library
The HR067 OSHA Z-1 seed contains 638 limit records covering 432 substance rows. OSHA authority and source provenance are stored separately from other authorities.

The exposure library is downstream safety/reference knowledge; it does not create analytical detectability.

## 144 analytical coverage policy
`ChemicalCoverage` distinguishes:
- target-backed direct curve;
- fingerprint/calibration candidate;
- weak/unvalidated or unsupported;
- aggregate/mixture metric.

A target-backed curve still requires device-specific R0/reference-gas calibration, correct heater protocol and valid operating range before safety use.

Manufacturer graph cross-response and community/inherited equations remain lower-grade evidence and cannot independently justify quantitative PPM.

Aggregate entries such as mixtures/operational metrics must not be assigned a fabricated universal pure-gas equation.

## Knowledge ≠ detectability ≠ quantification
0.10.2 must preserve this separation:

`chemical exists in knowledge DB`
≠ `installed array can identify it`
≠ `device can report validated ppm`
≠ `an exposure limit can be interpreted quantitatively`.

## Library versions at HR067 baseline
- analytics: `HazRob-ANALYTICS-2026.09-v6.1`
- analytical catalogue: `HAZROB-CHEM-144-v3`
- exposure layer: `HAZROB-OEL-source-v1`
- calibration engine: `HAZROB-MQ-CAL-v2.1`

## Provenance rule
Scientific seed refreshes must be versioned and hash-validated. Updating NIOSH/OSHA/reference sources must not silently mutate historical experiment/report interpretation.

## 0.10.2 action
Preserve the HR067 144-catalog analytical semantics and retain the broader scientific knowledge/exposure datasets as separate layers. Later regenerated authoritative seeds may be imported only after deterministic diff, provenance validation, importer tests and historical-report compatibility checks.
