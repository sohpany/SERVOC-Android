package com.servoc.app.chemical;

import java.util.Comparator;

/**
 * Stable priority ordering for live chemical candidates.
 * Safety hazard is allowed to dominate only when quantitative interpretation is valid;
 * otherwise ordering is based strictly on analytical evidence quality.
 */
public final class CandidateRanker {
    private CandidateRanker(){}

    public static final Comparator<ValidatedGasEngine.Result> COMPARATOR=(a,b)->{
        int c=Integer.compare(hazardTier(b),hazardTier(a));if(c!=0)return c;
        c=Integer.compare(b.calibratedContributors,a.calibratedContributors);if(c!=0)return c;
        c=Integer.compare(b.primaryContributors,a.primaryContributors);if(c!=0)return c;
        c=Integer.compare(b.confidence,a.confidence);if(c!=0)return c;
        c=Double.compare(b.agreement,a.agreement);if(c!=0)return c;
        c=Integer.compare(b.manufacturerCrossContributors,a.manufacturerCrossContributors);if(c!=0)return c;
        c=Integer.compare(b.totalContributors,a.totalContributors);if(c!=0)return c;
        c=Integer.compare(a.communityCrossContributors,b.communityCrossContributors);if(c!=0)return c;
        c=Integer.compare(a.rejectedContributors,b.rejectedContributors);if(c!=0)return c;
        return Integer.compare(a.chemical,b.chemical);
    };

    public static int hazardTier(ValidatedGasEngine.Result r){
        return r!=null&&r.quantitative&&r.risk>=0?r.risk+1:0;
    }

    public static String evidenceLabel(ValidatedGasEngine.Result r){
        if(r==null)return "لا دليل";
        if(r.quantitative&&r.calibratedContributors>0)return "قياس كمي معاير";
        if(r.calibratedContributors>0)return "معايرة مرجعية/تعريف";
        if(r.primaryContributors>0&&r.confidence>=GasEngine.HIGH)return "دليل هدف مرتفع";
        if(r.primaryContributors>0)return "دليل هدف";
        if(r.manufacturerCrossContributors>0)return "استجابة متقاطعة موثقة";
        return "بصمة بحثية";
    }
}
