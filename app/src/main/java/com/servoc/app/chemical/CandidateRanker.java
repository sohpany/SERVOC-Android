package com.servoc.app.chemical;

import com.servoc.app.exposure.ExposureLimits;
import java.util.Comparator;

/**
 * Stable priority ordering for live chemical candidates.
 * Identity ranking is driven by analytical evidence quality, not by hazard severity.
 * Quantitative hazard interpretation remains available separately after calibration gates pass.
 */
public final class CandidateRanker {
    private CandidateRanker(){}

    public static final Comparator<ValidatedGasEngine.Result> COMPARATOR=(a,b)->{
        if(a==b)return 0;if(a==null)return 1;if(b==null)return -1;
        int c=Double.compare(b.score,a.score);if(c!=0)return c;
        c=Boolean.compare(b.quantitative,a.quantitative);if(c!=0)return c;
        c=Double.compare(b.evidenceQuality,a.evidenceQuality);if(c!=0)return c;
        c=Integer.compare(b.calibratedContributors,a.calibratedContributors);if(c!=0)return c;
        c=Integer.compare(b.primaryContributors,a.primaryContributors);if(c!=0)return c;
        c=Double.compare(b.agreement,a.agreement);if(c!=0)return c;
        c=Double.compare(b.coverage,a.coverage);if(c!=0)return c;
        c=Integer.compare(b.totalContributors,a.totalContributors);if(c!=0)return c;
        c=Integer.compare(a.communityCrossContributors,b.communityCrossContributors);if(c!=0)return c;
        c=Integer.compare(a.rejectedContributors,b.rejectedContributors);if(c!=0)return c;
        return Integer.compare(a.chemical,b.chemical);
    };

    /**
     * Hazard severity is intentionally separate from chemical identity ranking.
     * It is available only when the result is quantitatively valid.
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
