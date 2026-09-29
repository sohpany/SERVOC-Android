package com.servoc.app.fingerprint;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ResearchIdentificationFingerprintRegistryTest {
    @Test public void sourceBackedLibraryCoversAllDeclaredNumericFamiliesWithoutM4(){
        assertFalse(ResearchIdentificationFingerprintRegistry.M4);
        assertTrue(ResearchIdentificationFingerprintRegistry.REFERENCE_PPM_ACCEPTED);
        List<ChemicalFingerprint> library=ResearchIdentificationFingerprintRegistry.library();
        assertFalse(library.isEmpty());
        Set<Integer> found=new HashSet<>();
        for(ChemicalFingerprint f:library){
            found.add(f.chemicalId);
            assertEquals(8,f.meanLogRatio.length);
            assertEquals(8,f.sigmaLogRatio.length);
            assertEquals(0,f.sampleCount); // deterministic source-backed reference, not HR069 empirical captures
            assertTrue(f.label.startsWith("RESEARCH_PRIOR|"));
            boolean finite=false;
            for(int i=0;i<8;i++){
                if(Double.isFinite(f.meanLogRatio[i])){
                    finite=true;
                    assertTrue(Double.isFinite(f.sigmaLogRatio[i]));
                    assertTrue(f.sigmaLogRatio[i]>=0.20);
                }
            }
            assertTrue(finite);
        }
        int[] expected=ResearchIdentificationFingerprintRegistry.allNumericFingerprintIds();
        for(int id:expected){
            assertTrue("missing research fingerprint for chemical/class "+id,found.contains(id));
            assertTrue(ResearchIdentificationFingerprintRegistry.templateCountForChemical(id)>0);
        }
        assertEquals(expected.length,found.size());
        assertEquals(13,found.size());
        assertTrue(found.contains(135)); // LPG operational class
        assertTrue(found.contains(139)); // Smoke operational class
        assertTrue(ResearchIdentificationFingerprintRegistry.isOperationalClass(135));
        assertTrue(ResearchIdentificationFingerprintRegistry.isOperationalClass(139));
    }

    @Test public void foodSpoilageTargetsDoNotInventUnsupportedNumericFingerprints(){
        Set<Integer> numeric=new HashSet<>();
        Set<Integer> nonNumeric=new HashSet<>();
        for(ResearchIdentificationFingerprintRegistry.TargetStatus s:ResearchIdentificationFingerprintRegistry.foodSpoilageTargets()){
            if(s.numericFingerprintAvailable)numeric.add(s.chemicalId);else nonNumeric.add(s.chemicalId);
        }
        assertEquals(setOf(21,23,76,103,110),numeric);
        assertTrue(nonNumeric.contains(4));   // H2S: affinity only
        assertTrue(nonNumeric.contains(54));  // acetaldehyde
        assertTrue(nonNumeric.contains(89));  // ethylene
        assertTrue(nonNumeric.contains(102)); // methanol transfer evidence
        assertTrue(nonNumeric.contains(115)); // ethyl acetate transfer evidence
        for(int id:nonNumeric)assertFalse(ResearchIdentificationFingerprintRegistry.hasNumericPrior(id));
    }

    @Test public void researchClassifierReturnsOneBestTemplatePerChemical(){
        ChemicalFingerprint ethanol=null;
        for(ChemicalFingerprint f:ResearchIdentificationFingerprintRegistry.library()){
            if(f.chemicalId==103&&f.label.contains("|300ppm|")){ethanol=f;break;}
        }
        assertNotNull(ethanol);
        double[] ratios=new double[8];
        for(int i=0;i<8;i++)ratios[i]=Double.isFinite(ethanol.meanLogRatio[i])?Math.pow(10.0,ethanol.meanLogRatio[i]):Double.NaN;
        boolean[] heater={true,true,true,true,true,true,true,true};
        List<FingerprintClassifier.Match> matches=FingerprintClassifier.classifyResearchPriors(ratios,heater);
        assertFalse(matches.isEmpty());
        assertEquals(103,matches.get(0).fingerprint.chemicalId);
        Set<Integer> unique=new HashSet<>();
        for(FingerprintClassifier.Match m:matches)assertTrue(unique.add(m.fingerprint.chemicalId));
    }

    @Test public void heaterCycledChannelsFailClosedWhenValidityUnknown(){
        ChemicalFingerprint co=null;
        for(ChemicalFingerprint f:ResearchIdentificationFingerprintRegistry.library()){
            if(f.chemicalId==20&&f.label.contains("|200ppm|")){co=f;break;}
        }
        assertNotNull(co);
        double[] ratios=new double[8];
        for(int i=0;i<8;i++)ratios[i]=Double.isFinite(co.meanLogRatio[i])?Math.pow(10.0,co.meanLogRatio[i]):Double.NaN;
        List<FingerprintClassifier.Match> matches=FingerprintClassifier.classifyResearchPriors(ratios);
        assertFalse(matches.isEmpty());
        for(FingerprintClassifier.Match m:matches){
            // MQ7 and MQ9 are excluded without an explicitly valid heater cycle; max contributors is six.
            assertTrue(m.contributors<=6);
        }
    }

    private static Set<Integer> setOf(int... ids){Set<Integer>s=new HashSet<>();for(int id:ids)s.add(id);return s;}
}
