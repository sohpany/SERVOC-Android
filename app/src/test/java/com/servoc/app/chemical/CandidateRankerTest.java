package com.servoc.app.chemical;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CandidateRankerTest {
    private static ValidatedGasEngine.Result r(int chemical,double ppm,int calibrated,int primary,int confidence,double agreement,boolean quantitative,int risk,int manufacturerCross,int communityCross){
        int total=calibrated+primary+manufacturerCross+communityCross;
        return new ValidatedGasEngine.Result(chemical,ppm,agreement,total,primary,calibrated,manufacturerCross,communityCross,0,confidence,quantitative,risk,"test");
    }

    @Test public void validatedQuantitativeHazardRanksAheadOfNonQuantitativeEvidence(){
        ValidatedGasEngine.Result hazard=r(1,100.0,1,1,GasEngine.STRONG,70,true,3,0,0);
        ValidatedGasEngine.Result identity=r(2,Double.NaN,2,2,GasEngine.HIGH,95,false,-1,0,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(identity);rows.add(hazard);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(hazard,rows.get(0));
    }

    @Test public void higherValidatedConcentrationBreaksEqualDangerTie(){
        // Chemicals without a legacy exposure entry both have hazard tier 0; valid ppm then orders them.
        ValidatedGasEngine.Result low=r(70,50.0,1,1,GasEngine.STRONG,80,true,-1,0,0);
        ValidatedGasEngine.Result high=r(71,250.0,1,1,GasEngine.STRONG,80,true,-1,0,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(low);rows.add(high);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(high,rows.get(0));
        assertEquals(250.0,CandidateRanker.concentrationPpm(high),1e-9);
    }

    @Test public void calibrationAndManufacturerTargetOutrankWeakCrossResponse(){
        ValidatedGasEngine.Result target=r(1,Double.NaN,0,1,GasEngine.STRONG,70,false,-1,0,0);
        ValidatedGasEngine.Result cross=r(2,Double.NaN,0,0,GasEngine.HIGH,98,false,-1,4,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(cross);rows.add(target);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(target,rows.get(0));
    }

    @Test public void agreementBreaksTieAfterEvidenceAndConfidence(){
        ValidatedGasEngine.Result low=r(1,Double.NaN,0,1,GasEngine.STRONG,55,false,-1,0,0);
        ValidatedGasEngine.Result high=r(2,Double.NaN,0,1,GasEngine.STRONG,88,false,-1,0,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(low);rows.add(high);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(high,rows.get(0));
    }

    @Test public void unvalidatedRiskAndPpmCannotInfluenceOrdering(){
        ValidatedGasEngine.Result fakeRisk=r(1,99999.0,0,0,GasEngine.SUSPECTED,60,false,3,1,0);
        ValidatedGasEngine.Result target=r(2,Double.NaN,0,1,GasEngine.STRONG,70,false,-1,0,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(fakeRisk);rows.add(target);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(target,rows.get(0));
        assertEquals(0,CandidateRanker.hazardTier(fakeRisk));
        assertFalse(Double.isFinite(CandidateRanker.concentrationPpm(fakeRisk)));
    }
}
