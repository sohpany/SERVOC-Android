package com.servoc.app.calibration;

import org.junit.Test;

import static org.junit.Assert.*;

public class CalibrationValidationMarkerTest {
    private static CalibrationSession acceptedSession(){
        CalibrationSession s=new CalibrationSession("CAL-ACCEPT",0,0,"certified reference gas",1L);
        s.add(new CalibrationSession.Capture(CalibrationSession.BASELINE,Double.NaN,1.0,25,40,true,2));
        double[] ppm={10,40,160};
        for(double p:ppm){double r=5.0/Math.sqrt(p);for(int k=0;k<3;k++)s.add(new CalibrationSession.Capture(CalibrationSession.EXPOSURE,p,r,25,40,true,10+k));}
        s.add(new CalibrationSession.Capture(CalibrationSession.RECOVERY,Double.NaN,1.01,25,40,true,99));
        return s;
    }

    @Test public void acceptedQcProducesM4Marker(){
        CalibrationSession s=acceptedSession();CalibrationQualityReport q=CalibrationQualityReport.evaluate(s,CalibrationQualityPolicy.hazRobDefault());
        assertTrue(q.summary(),q.accepted);String marker=CalibrationValidationMarker.encode(s,q,"controlled laboratory run");
        assertTrue(CalibrationValidationMarker.accepted(marker));assertTrue(CalibrationValidationMarker.parse(marker).optJSONObject("quality").optBoolean("accepted"));
    }

    @Test public void legacySourceNoteNeverCountsAsM4Validation(){
        assertFalse(CalibrationValidationMarker.accepted("Derived only from APPROVED HazRob experiments: 1,2,3"));
        assertFalse(CalibrationValidationMarker.accepted(""));
    }

    @Test public void incompleteCampaignCannotProduceAcceptedMarker(){
        CalibrationSession s=new CalibrationSession("CAL-REJECT",0,0,"reference",1L);
        s.add(new CalibrationSession.Capture(CalibrationSession.BASELINE,Double.NaN,1.0,25,40,true,2));
        for(int k=0;k<2;k++)s.add(new CalibrationSession.Capture(CalibrationSession.EXPOSURE,10,1.5,25,40,true,10+k));
        CalibrationQualityReport q=CalibrationQualityReport.evaluate(s,CalibrationQualityPolicy.hazRobDefault());
        assertFalse(q.accepted);assertFalse(CalibrationValidationMarker.accepted(CalibrationValidationMarker.encode(s,q,"incomplete")));
    }
}
