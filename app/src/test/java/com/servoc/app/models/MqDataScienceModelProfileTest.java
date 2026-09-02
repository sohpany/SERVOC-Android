package com.servoc.app.models;

import org.junit.Test;

import static org.junit.Assert.*;

public class MqDataScienceModelProfileTest {

    @Test public void everyAdoptedModelHasExactlyOneDataDomain(){
        int total=0;
        for(MqDataScienceModelProfile.Domain d:MqDataScienceModelProfile.Domain.values())
            total+=MqDataScienceModelProfile.count(d);
        assertEquals(MqDataScience76Catalog.COUNT,total);
        assertEquals(11,MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.MULTIVARIATE_ENV));
        assertEquals(40,MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.CURVE_1D));
        assertEquals(12,MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.TIME_SERIES));
        assertEquals(13,MqDataScienceModelProfile.count(MqDataScienceModelProfile.Domain.MACHINE_LEARNING));
    }

    @Test public void representativeRequirementsMatchCatalogueSemantics(){
        assertEquals(MqDataScienceModelProfile.Domain.MULTIVARIATE_ENV,MqDataScienceModelProfile.domain(0));
        assertTrue(MqDataScienceModelProfile.needsTemperature(0));
        assertTrue(MqDataScienceModelProfile.needsHumidity(0));
        assertTrue(MqDataScienceModelProfile.needsOrderedTime(0));

        assertEquals(MqDataScienceModelProfile.Domain.CURVE_1D,
                MqDataScienceModelProfile.domain(CalibrationRegressionModels.POWER));
        assertEquals(MqDataScienceModelProfile.Domain.TIME_SERIES,MqDataScienceModelProfile.domain(57)); // ARIMA
        assertEquals(MqDataScienceModelProfile.Domain.MACHINE_LEARNING,MqDataScienceModelProfile.domain(65)); // RF
    }
}
