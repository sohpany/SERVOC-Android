package com.servoc.app.models;

/**
 * Integration status for the full MQDataScience 76-model catalogue.
 *
 * All 76 upstream models are ADOPTED as part of the HazRob analytical library.
 * Runtime status is deliberately separate from adoption:
 *  - VERIFIED: Android implementation is mathematically equivalent to the pinned upstream form for the current 1-D calibration dataset.
 *  - APPROXIMATION: executable Android implementation exists, but still differs from the pinned upstream implementation or tuning procedure.
 *  - PORT PENDING: adopted upstream model has not yet been ported for Android execution.
 *
 * Independently of Android-native execution, all 76 entries are integrated into the
 * pinned developer/lab research bridge. This preserves the full upstream research value
 * without pretending that SciPy/scikit-learn/statsmodels/XGBoost/LightGBM/CatBoost/pyGAM
 * are natively executing inside the field Android process.
 */
public final class ModelRuntimeRegistry {
    private ModelRuntimeRegistry(){}

    // Current fidelity audit against MQDataScience v6.0.0 / pinned commit.
    private static final int[] VERIFIED={
            CalibrationRegressionModels.LINEAR,
            CalibrationRegressionModels.WEIGHTED_LINEAR,
            CalibrationRegressionModels.RIDGE,
            CalibrationRegressionModels.LASSO,
            CalibrationRegressionModels.ELASTIC_NET,
            CalibrationRegressionModels.DEMING,
            CalibrationRegressionModels.POLY2,
            CalibrationRegressionModels.POLY3,
            CalibrationRegressionModels.POLY4,
            CalibrationRegressionModels.EXPONENTIAL,
            CalibrationRegressionModels.LOGARITHMIC,
            CalibrationRegressionModels.POWER
    };

    private static final int[] APPROXIMATION={
            CalibrationRegressionModels.HUBER,
            CalibrationRegressionModels.THEIL_SEN
    };

    public static boolean isAdopted(int zeroBasedIndex){
        return zeroBasedIndex>=0 && zeroBasedIndex<MqDataScience76Catalog.COUNT;
    }

    public static boolean isAndroidNative(int zeroBasedIndex){
        for(int i:CalibrationRegressionModels.NATIVE)if(i==zeroBasedIndex)return true;
        return false;
    }

    public static boolean isUpstreamVerified(int zeroBasedIndex){
        for(int i:VERIFIED)if(i==zeroBasedIndex)return true;
        return false;
    }

    public static boolean isApproximation(int zeroBasedIndex){
        for(int i:APPROXIMATION)if(i==zeroBasedIndex)return true;
        return false;
    }

    /** Full research-layer integration through the pinned upstream lab bridge. */
    public static boolean isResearchIntegrated(int zeroBasedIndex){
        return Mq76ModelRoleRegistry.pinnedLabExecutable(zeroBasedIndex);
    }

    public static int adoptedCount(){return MqDataScience76Catalog.COUNT;}
    public static int integratedResearchCount(){return Mq76ModelRoleRegistry.count();}
    public static int nativeCount(){int n=0;for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(isAndroidNative(i))n++;return n;}
    public static int verifiedCount(){int n=0;for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(isUpstreamVerified(i))n++;return n;}
    public static int approximationCount(){int n=0;for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(isApproximation(i))n++;return n;}
    public static int pendingPortCount(){return adoptedCount()-nativeCount();}

    public static String adoptionStatus(int i){return isAdopted(i)?"ADOPTED":"INVALID INDEX";}

    public static String runtimeStatus(int i){
        if(!isAdopted(i))return "INVALID INDEX";
        if(isUpstreamVerified(i))return "ANDROID NATIVE / UPSTREAM-VERIFIED";
        if(isApproximation(i))return "ANDROID NATIVE / APPROXIMATION";
        return "ADOPTED / PORT PENDING";
    }

    /**
     * Whole-system status: distinguishes field-device execution from research integration.
     * A non-native model can still be actively useful in model development, drift analysis,
     * environmental modelling or candidate selection in the pinned lab workflow.
     */
    public static String systemIntegrationStatus(int i){
        if(!isAdopted(i))return "INVALID INDEX";
        if(isUpstreamVerified(i))return "INTEGRATED / ANDROID-VERIFIED + PINNED-LAB";
        if(isApproximation(i))return "INTEGRATED / ANDROID-APPROXIMATION + PINNED-LAB";
        return "INTEGRATED / PINNED-UPSTREAM-LAB; ANDROID PORT NOT REQUIRED FOR RESEARCH USE";
    }

    public static String status(int i){return runtimeStatus(i);}
}
