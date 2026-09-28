package com.servoc.app.calibration;

import com.servoc.app.chemical.GasEngine;
import org.junit.Test;

import static org.junit.Assert.*;

public class CalibrationManufacturerDomainWarningTest {
    @Test public void mq3EthanolLevelBelowManufacturerRangeIsExplicitlyFlagged(){
        CalibrationSession s=new CalibrationSession("domain-warning",GasEngine.MQ3,103,"ethanol reference",1L);
        s.add(new CalibrationSession.Capture(CalibrationSession.BASELINE,Double.NaN,1.0,20,55,true,1L));
        long ts=2L;
        for(double ppm:new double[]{5,50,500}){
            for(int r=0;r<3;r++)s.add(new CalibrationSession.Capture(
                    CalibrationSession.EXPOSURE,ppm,Math.max(0.05,1.0/(1.0+ppm/100.0)),20,55,true,ts++));
        }
        s.add(new CalibrationSession.Capture(CalibrationSession.RECOVERY,Double.NaN,1.0,20,55,true,ts));
        CalibrationQualityReport q=CalibrationQualityReport.evaluate(s,CalibrationQualityPolicy.hazRobDefault());
        boolean found=false;
        for(String w:q.warnings)if(w.contains("5.0 ppm")&&w.contains("25.0-500.0 ppm"))found=true;
        assertTrue(found);
    }
}
