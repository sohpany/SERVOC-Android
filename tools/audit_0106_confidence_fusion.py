#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MATRIX=ROOT/'app/src/main/java/com/servoc/app/chemcore/ChemCoreConfidenceMatrix.java'
TEST=ROOT/'app/src/test/java/com/servoc/app/chemcore/ChemCoreConfidenceMatrixEvidenceTest.java'


def req(text,needle,label):
    if needle not in text:
        raise SystemExit(f'FAIL: missing {label}: {needle}')


def main():
    matrix=MATRIX.read_text(encoding='utf-8')
    test=TEST.read_text(encoding='utf-8')

    req(matrix,'score(long chemicalUid,List<ChemCoreEvidenceObservation> evidence)','typed P6 evidence-fusion entry point')
    req(matrix,'strongestByGroup','independence-group de-duplication')
    req(matrix,'r.chemicalUid!=chemicalUid','candidate isolation')
    req(matrix,'!r.insideDomain','out-of-domain evidence exclusion')
    req(matrix,'tierRank(r.tier)>tierRank(prior)','strongest-tier selection per independent group')
    req(matrix,'sensors.addAll(r.sensorKeys)','sensor breadth kept separate from evidence independence')
    req(matrix,'M3_ARRAY_CONSISTENT','M3 explicit non-M4 treatment')
    req(matrix,'return score(q,referenceOnly+m4,m4,sensors.size())','typed evidence maps into bounded positive-evidence schedule')

    req(test,'correlatedQsprChannelsCountAsOneIndependentEvidenceGroup','correlated-QSPR regression')
    req(test,'strongestTierWinsInsideOneIndependenceGroup','strongest-tier regression')
    req(test,'independentReferenceGroupsRemainIndependent','independent-reference regression')
    req(test,'outOfDomainAndOtherChemicalDoNotBoostCandidateConfidence','domain and candidate isolation regression')
    req(test,'m3IdentityEvidenceIsNonM4SupportedEvidenceUntilWeightCalibration','M3 conservative weighting regression')

    print('HazRob 0.10.2 / HR069 P6 confidence fusion contract: PASS')
    print('Rule: confidence de-duplicates by independenceGroup; sensor breadth is supportive, not independent proof.')


if __name__=='__main__':
    main()
