package com.servoc.app.models;

/**
 * Integration status for the full MQDataScience 76-model catalogue.
 *
 * All 76 upstream models are ADOPTED as part of the HazRob analytical library.
 * Runtime status is deliberately separate from adoption:
 *  - VERIFIED: Android implementation is mathematically equivalent to the pinned upstream form for the current 1-D calibration dataset.
 *  - APPROXIMATION: executable Android implementation exists, but still differs from the pinned upstream implementation or tuning procedure.
 *  - PORT PENDING: adopted upstream model has not yet been ported for Android execution.
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
            CalibrationRegressionModels.LOGARITHMIC
    };

    private static final int[] APPROXIMATION={
            CalibrationRegressionModels.HUBER,
            CalibrationRegressionModels.THEIL_SEN,
            CalibrationRegressionModels.EXPONENTIAL,
            CalibrationRegressionModels.POWER
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

    public static int adoptedCount(){return MqDataScience76Catalog.COUNT;}
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

    public static String status(int i){return runtimeStatus(i);}
}
