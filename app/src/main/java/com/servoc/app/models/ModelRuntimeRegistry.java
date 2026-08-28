package com.servoc.app.models;

/** Truthful runtime status for the 76 source-registered models. */
public final class ModelRuntimeRegistry {
    private ModelRuntimeRegistry(){}

    public static boolean isAndroidNative(int zeroBasedIndex){
        // Priority 17 Ridge -> HazRob RidgeConcentrationModel.
        // Priority 30 Power -> HazRob LogPowerCurveFitter / EmpiricalMqCurve power-law path.
        return zeroBasedIndex==16 || zeroBasedIndex==29;
    }

    public static int nativeCount(){int n=0;for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(isAndroidNative(i))n++;return n;}
    public static String status(int i){return isAndroidNative(i)?"ANDROID NATIVE":"REGISTERED / PORT PENDING";}
}
