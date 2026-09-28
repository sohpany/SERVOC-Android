package com.servoc.app.models;

/**
 * Scientific-use contract for the complete MQDataScience 76-model catalogue.
 *
 * The pinned upstream implementation uses t/time as the primary independent variable and
 * adds temperature/RH to selected models. HazRob may re-fit some mathematical families
 * against Rs/R0 -> ppm, but that is a HazRob adaptation and must never be reported as
 * upstream-equivalent unless separately verified.
 */
public final class Mq76ModelRoleRegistry {
    private Mq76ModelRoleRegistry(){}

    public enum Purpose {
        ENVIRONMENTAL_RESPONSE,
        CURVE_AND_ROBUST_REGRESSION,
        TEMPORAL_DYNAMICS,
        MACHINE_LEARNING_REGRESSION
    }

    public enum Deployment {
        PINNED_UPSTREAM_LAB,
        ANDROID_NATIVE_AND_PINNED_LAB
    }

    public static final class Spec {
        public final int index;
        public final String name;
        public final Purpose purpose;
        public final Deployment deployment;
        public final boolean requiresTime;
        public final boolean usesTemperature;
        public final boolean usesHumidity;
        public final boolean directCalibrationEligible;

        Spec(int index,String name,Purpose purpose,Deployment deployment,
             boolean requiresTime,boolean usesTemperature,boolean usesHumidity,
             boolean directCalibrationEligible){
            this.index=index;this.name=name;this.purpose=purpose;this.deployment=deployment;
            this.requiresTime=requiresTime;this.usesTemperature=usesTemperature;
            this.usesHumidity=usesHumidity;this.directCalibrationEligible=directCalibrationEligible;
        }
    }

    public static int count(){return MqDataScience76Catalog.COUNT;}

    public static Spec get(int i){
        if(i<0||i>=MqDataScience76Catalog.COUNT)throw new IllegalArgumentException("model index out of range: "+i);
        Purpose purpose;
        if(i<=10)purpose=Purpose.ENVIRONMENTAL_RESPONSE;
        else if(i<=48)purpose=Purpose.CURVE_AND_ROBUST_REGRESSION;
        else if(i<=62)purpose=Purpose.TEMPORAL_DYNAMICS;
        else purpose=Purpose.MACHINE_LEARNING_REGRESSION;

        boolean temp=usesTemperature(i),rh=usesHumidity(i),time=usesTime(i);
        // Safe research eligibility only. Final quantitative use still requires the HazRob
        // calibration/reference-gas and M4 gates. Periodic/time-series families are kept out
        // of direct concentration calibration by default.
        boolean directCalibration=(i>=11&&i<=48)||(i>=63&&i<=73);
        Deployment deployment=isAndroidNative(i)?Deployment.ANDROID_NATIVE_AND_PINNED_LAB:Deployment.PINNED_UPSTREAM_LAB;
        return new Spec(i,MqDataScience76Catalog.NAMES[i],purpose,deployment,time,temp,rh,directCalibration);
    }

    public static boolean allIntegrated(){
        if(MqDataScience76Catalog.COUNT!=76)return false;
        for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(get(i)==null)return false;
        return true;
    }

    /** All catalogue entries are executable in the pinned developer/lab bridge. */
    public static boolean pinnedLabExecutable(int i){return i>=0&&i<MqDataScience76Catalog.COUNT;}

    /** Current dependency-free Android-native calibration ports. */
    public static boolean isAndroidNative(int i){
        switch(i){
            case 11: case 12: case 13: case 14:
            case 16: case 17: case 18: case 23:
            case 24: case 25: case 26: case 27:
            case 28: case 29:
                return true;
            default:return false;
        }
    }

    private static boolean usesTemperature(int i){
        switch(i){
            case 0: case 1: case 2: case 4: case 5: case 6: case 7:
            case 61: case 62: case 74: case 75:
                return true;
            default:return false;
        }
    }

    private static boolean usesHumidity(int i){
        switch(i){
            case 0: case 1: case 3: case 4: case 6: case 8: case 10:
            case 61: case 62: case 74: case 75:
                return true;
            default:return false;
        }
    }

    private static boolean usesTime(int i){
        switch(i){
            case 1: case 6: case 7: case 8: case 9: case 10:
                return false;
            default:return true;
        }
    }

    public static String scientificUse(int i){
        Spec s=get(i);
        switch(s.purpose){
            case ENVIRONMENTAL_RESPONSE:
                return "Environmental/time nuisance modelling; use for compensation or longitudinal prediction, not standalone analyte identity.";
            case CURVE_AND_ROBUST_REGRESSION:
                return "Curve-family research. HazRob may fit Rs/R0-to-ppm only with controlled reference-gas data and independent validation.";
            case TEMPORAL_DYNAMICS:
                return "Transient/drift/forecast modelling. Do not reinterpret a time-series forecast as a concentration calibration curve.";
            default:
                return "Data-driven regression candidate. Requires held-out HazRob validation, domain checks and uncertainty before activation.";
        }
    }
}
