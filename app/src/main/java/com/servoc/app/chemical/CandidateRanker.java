package com.servoc.app.chemical;

import com.servoc.app.exposure.ExposureLimits;
import java.util.Comparator;

/**
 * Stable priority ordering for live chemical candidates.
 * Quantitative concentration participates only when device/reference calibration has
 * made the result quantitative. Non-quantitative candidates are ranked by evidence only.
 */
public final class CandidateRanker {
    private CandidateRanker(){}

    public static final Comparator<ValidatedGasEngine.Result> COMPARATOR=(a,b)->{
        int c=Integer.compare(hazardTier(b),hazardTier(a));if(c!=0)return c;
        c=Boolean.compare(b!=null&&b.quantitative,a!=null&&a.quantitative);if(c!=0)return c;
        if(a!=null&&b!=null&&a.quantitative&&b.quantitative){
            c=Double.compare(validPpm(b),validPpm(a));if(c!=0)return c;
        }
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

    /**
     * Danger is allowed to influence ordering only for a quantitative result.
     * Prefer a caller-provided validated risk if present; otherwise use the legacy
     * screening table as a compatibility fallback for the current 144 analytical IDs.
     */
    public static int hazardTier(ValidatedGasEngine.Result r){
        if(r==null||!r.quantitative||!Double.isFinite(r.ppm)||r.ppm<0)return 0;
        int explicit=r.risk>=0?r.risk+1:0;
        int screening=ExposureLimits.screeningRisk(r.chemical,r.ppm);
        return Math.max(explicit,screening);
    }

    public static double concentrationPpm(ValidatedGasEngine.Result r){
        return r!=null&&r.quantitative&&Double.isFinite(r.ppm)&&r.ppm>=0?r.ppm:Double.NaN;
    }

    private static double validPpm(ValidatedGasEngine.Result r){double p=concentrationPpm(r);return Double.isFinite(p)?p:-1;}

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
