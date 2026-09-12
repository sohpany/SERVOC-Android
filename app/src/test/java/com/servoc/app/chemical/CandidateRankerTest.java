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

    @Test public void strongerAnalyticalIdentityRanksAheadOfHazardSeverity(){
        ValidatedGasEngine.Result hazard=r(1,100.0,1,1,GasEngine.STRONG,70,true,3,0,0);
        ValidatedGasEngine.Result identity=r(2,Double.NaN,2,2,GasEngine.HIGH,95,false,-1,0,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(hazard);rows.add(identity);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(identity,rows.get(0));
        assertTrue(CandidateRanker.hazardTier(hazard)>0);
    }

    @Test public void concentrationDoesNotBreakIdentityRankingTie(){
        ValidatedGasEngine.Result low=r(70,50.0,1,1,GasEngine.STRONG,80,true,-1,0,0);
        ValidatedGasEngine.Result high=r(71,250.0,1,1,GasEngine.STRONG,80,true,-1,0,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(high);rows.add(low);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(low,rows.get(0));
        assertEquals(250.0,CandidateRanker.concentrationPpm(high),1e-9);
    }

    @Test public void broaderDocumentedFingerprintMayOutrankSingleSensorTarget(){
        ValidatedGasEngine.Result target=r(1,Double.NaN,0,1,GasEngine.SUSPECTED,0,false,-1,0,0);
        ValidatedGasEngine.Result cross=r(2,Double.NaN,0,0,GasEngine.STRONG,75,false,-1,4,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(target);rows.add(cross);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(cross,rows.get(0));
        assertTrue(cross.score>target.score);
    }

    @Test public void agreementCannotDominateWhenEvidenceCoverageIsWeak(){
        ValidatedGasEngine.Result weak=r(1,Double.NaN,0,0,GasEngine.SUSPECTED,100,false,-1,0,1);
        ValidatedGasEngine.Result multi=r(2,Double.NaN,0,1,GasEngine.STRONG,65,false,-1,2,0);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();rows.add(weak);rows.add(multi);rows.sort(CandidateRanker.COMPARATOR);
        assertSame(multi,rows.get(0));
    }

    @Test public void unvalidatedRiskAndPpmCannotBecomeQuantitative(){
        ValidatedGasEngine.Result fakeRisk=r(1,99999.0,0,0,GasEngine.SUSPECTED,60,false,3,1,0);
        assertEquals(0,CandidateRanker.hazardTier(fakeRisk));
        assertFalse(Double.isFinite(CandidateRanker.concentrationPpm(fakeRisk)));
    }
}
