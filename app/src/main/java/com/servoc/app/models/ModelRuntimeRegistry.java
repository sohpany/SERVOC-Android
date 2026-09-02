package com.servoc.app.models;

/**
 * Integration status for the full MQDataScience 76-model catalogue.
 *
 * All 76 upstream models are ADOPTED as part of the HazRob analytical library.
 * Runtime status only describes whether a faithful Android-side implementation
 * has already been ported and regression-tested; PORT PENDING never means that
 * the upstream model has been rejected or deprecated.
 */
public final class ModelRuntimeRegistry {
    private ModelRuntimeRegistry(){}

    public static boolean isAdopted(int zeroBasedIndex){
        return zeroBasedIndex>=0 && zeroBasedIndex<MqDataScience76Catalog.COUNT;
    }

    public static boolean isAndroidNative(int zeroBasedIndex){
        for(int i:CalibrationRegressionModels.NATIVE)if(i==zeroBasedIndex)return true;
        return false;
    }

    public static int adoptedCount(){return MqDataScience76Catalog.COUNT;}
    public static int nativeCount(){int n=0;for(int i=0;i<MqDataScience76Catalog.COUNT;i++)if(isAndroidNative(i))n++;return n;}
    public static int pendingPortCount(){return adoptedCount()-nativeCount();}

    public static String adoptionStatus(int i){return isAdopted(i)?"ADOPTED":"INVALID INDEX";}
    public static String runtimeStatus(int i){return isAndroidNative(i)?"ANDROID NATIVE / TESTED":"ADOPTED / PORT PENDING";}
    public static String status(int i){return runtimeStatus(i);}
}
