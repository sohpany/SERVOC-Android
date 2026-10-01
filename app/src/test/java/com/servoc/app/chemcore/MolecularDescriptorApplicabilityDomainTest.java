package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MolecularDescriptorApplicabilityDomainTest {
    @Test public void acceptsInteriorDescriptorVectorAndRejectsExtrapolation(){
        String[] names={"MolecularWeight","TPSA"};
        List<Map<String,Double>> train=new ArrayList<>();
        train.add(v(30,10));train.add(v(50,20));train.add(v(70,30));train.add(v(90,40));train.add(v(110,50));
        MolecularDescriptorApplicabilityDomain ad=MolecularDescriptorApplicabilityDomain.fit(names,train);

        MolecularDescriptorApplicabilityDomain.Result inside=ad.evaluate(v(60,25));
        assertTrue(inside.complete);
        assertTrue(inside.insideMinMaxEnvelope);
        assertTrue(inside.insideDomain);
        assertTrue(Double.isFinite(inside.rmsZ));

        MolecularDescriptorApplicabilityDomain.Result outside=ad.evaluate(v(140,25));
        assertTrue(outside.complete);
        assertFalse(outside.insideMinMaxEnvelope);
        assertFalse(outside.insideDomain);
    }

    @Test public void missingDescriptorFailsClosed(){
        String[] names={"MolecularWeight","TPSA"};
        List<Map<String,Double>> train=List.of(v(30,10),v(50,20));
        MolecularDescriptorApplicabilityDomain ad=MolecularDescriptorApplicabilityDomain.fit(names,train);
        MolecularDescriptorApplicabilityDomain.Result r=ad.evaluate(Map.of("MolecularWeight",40.0));
        assertFalse(r.complete);
        assertFalse(r.insideDomain);
    }

    private static Map<String,Double> v(double mw,double tpsa){
        Map<String,Double> out=new LinkedHashMap<>();out.put("MolecularWeight",mw);out.put("TPSA",tpsa);return out;
    }
}
