package com.servoc.app.models;

/** Truthful runtime status for the 76 source-registered models. */
public final class ModelRuntimeRegistry {
    private ModelRuntimeRegistry(){}

    public static boolean isAndroidNative(int zeroBasedIndex){
        for(int i:CalibrationRegressionModels.NATIVE)if(i==zeroBasedIndex)return true;
        return false;
    }

    public static int nativeCount(){int n=0;for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(isAndroidNative(i))n++;return n;}
    public static String status(int i){return isAndroidNative(i)?"ANDROID NATIVE":"REGISTERED / PORT PENDING";}
}
