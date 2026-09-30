package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Typed analytical evidence emitted by ChemCore engines before fusion/confidence scoring.
 *
 * Sensor breadth and evidentiary independence remain separate. measurementQualityWeight adds a
 * third axis: a degraded measurement can still corroborate other evidence, but it cannot silently
 * become full-strength or reportable quantitative evidence.
 */
public final class ChemCoreEvidenceObservation {
    public static final String SCHEMA="chemcore-evidence/2";

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
    /** 0..1 measurement-quality multiplier. 1=full; 0<w<1=support-only; 0=diagnostic-only. */
    public final double measurementQualityWeight;
    /** PRIMARY or CORROBORATIVE; role is auditable and never upgrades evidence tier. */
    public final String supportRole;
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
        this(type,chemicalUid,label,sensorKeys,familyId,modelId,sourceId,independenceGroup,tier,insideDomain,
                quantitativeEstimateSupported,reportableQuantitative,fittedPpm,observedRatio,predictedLog10Ratio,
                residualLog10,standardizedResidual,note,1.0,"PRIMARY");
    }

    public ChemCoreEvidenceObservation(Type type,long chemicalUid,String label,List<String> sensorKeys,
                                       String familyId,String modelId,String sourceId,String independenceGroup,
                                       ChemCoreDecision.EvidenceTier tier,boolean insideDomain,
                                       boolean quantitativeEstimateSupported,boolean reportableQuantitative,
                                       double fittedPpm,double observedRatio,double predictedLog10Ratio,
                                       double residualLog10,double standardizedResidual,String note,
                                       double measurementQualityWeight,String supportRole){
        this.type=type==null?Type.UNKNOWN:type;this.chemicalUid=chemicalUid;this.label=label==null?"":label;
        List<String> cleanSensors=new ArrayList<>();if(sensorKeys!=null)for(String s:sensorKeys)if(s!=null&&!s.trim().isEmpty())cleanSensors.add(s.trim().toLowerCase());
        this.sensorKeys=Collections.unmodifiableList(cleanSensors);
        this.familyId=clean(familyId,this.type.name());this.modelId=clean(modelId,"");this.sourceId=clean(sourceId,"");
        this.independenceGroup=clean(independenceGroup,this.familyId+":"+this.sourceId);
        this.tier=tier==null?ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY:tier;this.insideDomain=insideDomain;
        this.quantitativeEstimateSupported=quantitativeEstimateSupported;
        this.measurementQualityWeight=clamp(Double.isFinite(measurementQualityWeight)?measurementQualityWeight:0,0,1);
        this.supportRole=clean(supportRole,"PRIMARY").toUpperCase();
        // A degraded/corroborative observation can support identity/confidence but is never reportable quantitative evidence.
        this.reportableQuantitative=reportableQuantitative&&this.measurementQualityWeight>=0.999&&"PRIMARY".equals(this.supportRole);
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
                p==null?"":p.note,1.0,"PRIMARY");
    }

    /** Support-only observation generated from a physically available but degraded MQ measurement. */
    public static ChemCoreEvidenceObservation corroborativeFromResponseModel(long chemicalUid,String label,ChemCoreResponseModel model,
                                                                              ChemCoreResponseModel.Estimate prediction,double fittedPpm,
                                                                              double observedRatio,double residualLog10,double standardizedResidual,
                                                                              double measurementQualityWeight,String qualityNote){
        if(model==null)throw new IllegalArgumentException("response model required");
        ChemCoreResponseModel.Estimate p=prediction;
        String family=(p!=null&&!blank(p.familyId)?p.familyId:model.evidenceFamilyId())+":corroborative";
        String modelId=p!=null&&!blank(p.modelId)?p.modelId:model.modelId();
        String source=p!=null&&!blank(p.sourceId)?p.sourceId:model.sourceId();
        // Keep the original independence group so weak data from the same underlying model cannot double count.
        String independence=p!=null&&!blank(p.independenceGroup)?p.independenceGroup:model.independenceGroup();
        boolean inside=p!=null&&p.insideDomain;
        String note=(qualityNote==null?"":qualityNote)+(p!=null&&!blank(p.note)?"; "+p.note:"");
        return new ChemCoreEvidenceObservation(Type.SENSOR_PATTERN,chemicalUid,label,Collections.singletonList(model.sensorKey()),
                family,modelId,source,independence,model.evidenceTier(),inside,false,false,
                fittedPpm,observedRatio,p==null?Double.NaN:p.meanLog10Ratio,residualLog10,standardizedResidual,note,
                measurementQualityWeight,"CORROBORATIVE");
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
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
