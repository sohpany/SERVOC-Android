package com.servoc.app.models;

import org.junit.Test;

import static org.junit.Assert.*;

public class ModelRuntimeRegistryTest {

    @Test public void allUpstreamModelsRemainAdopted(){
        assertEquals(76,MqDataScience76Catalog.COUNT);
        assertEquals(MqDataScience76Catalog.COUNT,ModelRuntimeRegistry.adoptedCount());
        for(int i=0;i<MqDataScience76Catalog.COUNT;i++)assertTrue(ModelRuntimeRegistry.isAdopted(i));
    }

    @Test public void runtimeBucketsAreInternallyConsistent(){
        assertEquals(CalibrationRegressionModels.NATIVE.length,ModelRuntimeRegistry.nativeCount());
        assertEquals(ModelRuntimeRegistry.nativeCount(),
                ModelRuntimeRegistry.verifiedCount()+ModelRuntimeRegistry.approximationCount());
        assertEquals(MqDataScience76Catalog.COUNT,
                ModelRuntimeRegistry.nativeCount()+ModelRuntimeRegistry.pendingPortCount());
    }

    @Test public void fidelityAuditMarksKnownCasesTruthfully(){
        assertTrue(ModelRuntimeRegistry.isUpstreamVerified(CalibrationRegressionModels.LINEAR));
        assertTrue(ModelRuntimeRegistry.isUpstreamVerified(CalibrationRegressionModels.POLY2));
        assertTrue(ModelRuntimeRegistry.isUpstreamVerified(CalibrationRegressionModels.POLY3));
        assertTrue(ModelRuntimeRegistry.isUpstreamVerified(CalibrationRegressionModels.LOGARITHMIC));

        assertTrue(ModelRuntimeRegistry.isApproximation(CalibrationRegressionModels.WEIGHTED_LINEAR));
        assertTrue(ModelRuntimeRegistry.isApproximation(CalibrationRegressionModels.RIDGE));
        assertTrue(ModelRuntimeRegistry.isApproximation(CalibrationRegressionModels.EXPONENTIAL));
        assertTrue(ModelRuntimeRegistry.isApproximation(CalibrationRegressionModels.POWER));

        assertEquals("ADOPTED / PORT PENDING",ModelRuntimeRegistry.runtimeStatus(15)); // RANSAC
    }
}
