package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Typed analytical evidence emitted by ChemCore engines before P6 fusion/confidence scoring.
 *
 * This object deliberately separates sensor breadth from evidentiary independence. Multiple
 * sensors may support one observation family while still sharing the same independenceGroup.
 * P6 must de-duplicate confidence by independenceGroup rather than count sensor channels as
 * independent proofs.
 */
public final class ChemCoreEvidenceObservation {
    public static final String SCHEMA="chemcore-evidence/1";

    public enum Type {
        HR069_CALIBRATION,
        HR069_EXPERIMENTAL_FINGERPRINT,
        REFERENCE_FINGERPRINT,
        LITERATURE_CURVE,
        MANUFACTURER_CURVE,
        RESEARCH_CURVE,
        QSPR,
        SENSOR_PATTERN,
        LEGACY_CURVE,
        REFERENCE_CURVE,
        UNKNOWN
    }

    public final Type type;
    public final long chemicalUid;
    public final String label;
    public final List<String> sensorKeys;
    public final String familyId;
    public final String modelId;
    public final String sourceId;
    public final String independenceGroup;
    public final ChemCoreDecision.EvidenceTier tier;
    public final boolean insideDomain;
    /** Model can contribute a concentration estimate for fitting/screening. */
    public final boolean quantitativeEstimateSupported;
    /** This single observation is device/reference validated for reportable quantitative use. */
    public final boolean reportableQuantitative;
    public final double fittedPpm;
    public final double observedRatio;
    public final double predictedLog10Ratio;
    public final double residualLog10;
    public final double standardizedResidual;
    public final String note;

    public ChemCoreEvidenceObservation(Type type,long chemicalUid,String label,List<String> sensorKeys,
                                       String familyId,String modelId,String sourceId,String independenceGroup,
                                       ChemCoreDecision.EvidenceTier tier,boolean insideDomain,
                                       boolean quantitativeEstimateSupported,boolean reportableQuantitative,
                                       double fittedPpm,double observedRatio,double predictedLog10Ratio,
                                       double residualLog10,double standardizedResidual,String note){
        this.type=type==null?Type.UNKNOWN:type;this.chemicalUid=chemicalUid;this.label=label==null?"":label;
        List<String> clean=new ArrayList<>();if(sensorKeys!=null)for(String s:sensorKeys)if(s!=null&&!s.trim().isEmpty())clean.add(s.trim().toLowerCase());
        this.sensorKeys=Collections.unmodifiableList(clean);
        this.familyId=clean(familyId,this.type.name());this.modelId=clean(modelId,"");this.sourceId=clean(sourceId,"");
        this.independenceGroup=clean(independenceGroup,this.familyId+":"+this.sourceId);
        this.tier=tier==null?ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY:tier;this.insideDomain=insideDomain;
        this.quantitativeEstimateSupported=quantitativeEstimateSupported;this.reportableQuantitative=reportableQuantitative;
        this.fittedPpm=fittedPpm;this.observedRatio=observedRatio;this.predictedLog10Ratio=predictedLog10Ratio;
        this.residualLog10=residualLog10;this.standardizedResidual=standardizedResidual;this.note=note==null?"":note;
    }

    public static ChemCoreEvidenceObservation fromResponseModel(long chemicalUid,String label,ChemCoreResponseModel model,
                                                                 ChemCoreResponseModel.Estimate prediction,double fittedPpm,
                                                                 double observedRatio,double residualLog10,double standardizedResidual){
        if(model==null)throw new IllegalArgumentException("response model required");
        ChemCoreResponseModel.Estimate p=prediction;
        Type type=p!=null&&p.evidenceType!=null?p.evidenceType:model.evidenceType();
        String family=p!=null&&!blank(p.familyId)?p.familyId:model.evidenceFamilyId();
        String modelId=p!=null&&!blank(p.modelId)?p.modelId:model.modelId();
        String source=p!=null&&!blank(p.sourceId)?p.sourceId:model.sourceId();
        String independence=p!=null&&!blank(p.independenceGroup)?p.independenceGroup:model.independenceGroup();
        boolean inside=p!=null&&p.insideDomain;
        boolean estimate=p!=null&&p.available&&model.quantitativeEstimateSupported();
        boolean reportable=estimate&&inside&&model.reportableQuantitative();
        return new ChemCoreEvidenceObservation(type,chemicalUid,label,Collections.singletonList(model.sensorKey()),
                family,modelId,source,independence,model.evidenceTier(),inside,estimate,reportable,
                fittedPpm,observedRatio,p==null?Double.NaN:p.meanLog10Ratio,residualLog10,standardizedResidual,
                p==null?"":p.note);
    }

    public static int uniqueIndependenceGroups(List<ChemCoreEvidenceObservation> rows){
        Set<String> groups=new LinkedHashSet<>();if(rows!=null)for(ChemCoreEvidenceObservation r:rows)if(r!=null&&!blank(r.independenceGroup))groups.add(r.independenceGroup);
        return groups.size();
    }

    public static int uniqueSensors(List<ChemCoreEvidenceObservation> rows){
        Set<String> sensors=new LinkedHashSet<>();if(rows!=null)for(ChemCoreEvidenceObservation r:rows)if(r!=null)sensors.addAll(r.sensorKeys);
        return sensors.size();
    }

    private static String clean(String value,String fallback){return blank(value)?(fallback==null?"":fallback):value.trim();}
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
}
