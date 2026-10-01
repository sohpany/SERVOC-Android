package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChemCoreEnvironmentDomainPolicyTest {
    @Test public void declaredTemperatureDomainRequiresMeasuredTemperature(){
        ChemCoreEnvironmentDomainPolicy.Result r=ChemCoreEnvironmentDomainPolicy.evaluate(
                10,40,Double.NaN,Double.NaN,Double.NaN,50);
        assertFalse(r.insideDomain);
        assertTrue(r.temperatureRequired);
        assertTrue(r.missingRequiredMeasurement);
    }

    @Test public void declaredHumidityDomainRequiresMeasuredHumidity(){
        ChemCoreEnvironmentDomainPolicy.Result r=ChemCoreEnvironmentDomainPolicy.evaluate(
                Double.NaN,Double.NaN,10,90,25,Double.NaN);
        assertFalse(r.insideDomain);
        assertTrue(r.rhRequired);
        assertTrue(r.missingRequiredMeasurement);
    }

    @Test public void declaredDomainAcceptsOnlyMeasuredValuesInsideRange(){
        assertTrue(ChemCoreEnvironmentDomainPolicy.evaluate(10,40,10,90,25,50).insideDomain);
        assertFalse(ChemCoreEnvironmentDomainPolicy.evaluate(10,40,10,90,45,50).insideDomain);
        assertFalse(ChemCoreEnvironmentDomainPolicy.evaluate(10,40,10,90,25,95).insideDomain);
    }

    @Test public void undeclaredEnvironmentDoesNotInventAConstraint(){
        ChemCoreEnvironmentDomainPolicy.Result r=ChemCoreEnvironmentDomainPolicy.evaluate(
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        assertTrue(r.insideDomain);
        assertFalse(r.temperatureRequired);
        assertFalse(r.rhRequired);
        assertFalse(r.missingRequiredMeasurement);
    }

    @Test public void powerLawModelFailsClosedWhenRequiredEnvironmentIsMissing(){
        PowerLawResponseModel model=new PowerLawResponseModel("mq2","env-test",2.0,-0.4,5,500,0.20,
                10,40,10,90,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"synthetic");
        ChemCoreResponseModel.Estimate missing=model.predict(50,Double.NaN,50,SamplingProfile.hazRobPrototypeV1());
        ChemCoreResponseModel.Estimate valid=model.predict(50,25,50,SamplingProfile.hazRobPrototypeV1());
        assertTrue(missing.available);
        assertFalse(missing.insideDomain);
        assertTrue(valid.insideDomain);
    }
}
