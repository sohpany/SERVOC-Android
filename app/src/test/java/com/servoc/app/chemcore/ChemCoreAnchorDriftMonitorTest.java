package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class ChemCoreAnchorDriftMonitorTest {
    private static ChemCoreAnchorDriftMonitor.Policy policy(){
        return new ChemCoreAnchorDriftMonitor.Policy(3,6,0.03,0.08,3,"unit-test thresholds only");
    }

    @Test public void stableIndependentAnchorsRemainStable(){
        Map<String,Double> ref=reference();List<Map<String,Double>> sessions=new ArrayList<>();
        sessions.add(session(1.01));sessions.add(session(0.99));sessions.add(session(1.00));
        ChemCoreAnchorDriftMonitor.Result r=ChemCoreAnchorDriftMonitor.assess(ref,sessions,policy());
        assertEquals(ChemCoreAnchorDriftMonitor.Verdict.STABLE,r.verdict);
        assertEquals(8,r.eligibleChannels);assertFalse(r.automaticBaselineUpdatePermitted());
    }

    @Test public void oneModeratePersistentShiftYieldsWatch(){
        Map<String,Double> ref=reference();List<Map<String,Double>> sessions=new ArrayList<>();
        for(int i=0;i<3;i++){Map<String,Double>s=session(1.0);s.put("mq3",1.10);sessions.add(s);}
        ChemCoreAnchorDriftMonitor.Result r=ChemCoreAnchorDriftMonitor.assess(ref,sessions,policy());
        assertEquals(ChemCoreAnchorDriftMonitor.Verdict.WATCH,r.verdict);assertEquals(1,r.watchChannels);
    }

    @Test public void largePersistentShiftRequiresNewCalibrationRevision(){
        Map<String,Double> ref=reference();List<Map<String,Double>> sessions=new ArrayList<>();
        for(int i=0;i<3;i++){Map<String,Double>s=session(1.0);s.put("mq8",1.25);sessions.add(s);}
        ChemCoreAnchorDriftMonitor.Result r=ChemCoreAnchorDriftMonitor.assess(ref,sessions,policy());
        assertEquals(ChemCoreAnchorDriftMonitor.Verdict.RECALIBRATION_REQUIRED,r.verdict);assertEquals(1,r.recalibrationChannels);
    }

    @Test public void incompleteLongitudinalEvidenceCannotDeclareStability(){
        List<Map<String,Double>> sessions=new ArrayList<>();sessions.add(session(1.0));
        ChemCoreAnchorDriftMonitor.Result r=ChemCoreAnchorDriftMonitor.assess(reference(),sessions,policy());
        assertEquals(ChemCoreAnchorDriftMonitor.Verdict.INSUFFICIENT_EVIDENCE,r.verdict);
    }

    private static Map<String,Double> reference(){return session(1.0);}
    private static Map<String,Double> session(double v){
        Map<String,Double> m=new LinkedHashMap<>();
        for(String k:new String[]{"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"})m.put(k,v);
        return m;
    }
}
