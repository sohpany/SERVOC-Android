package com.servoc.app.calibration;

import com.servoc.app.data.ExperimentsDb;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Single publication gate for device-specific quantitative MQ calibration curves.
 *
 * A curve may be stored inactive for audit even when QC is rejected. It is activated for
 * runtime M4 use only when the configured QC policy accepts the controlled session and a
 * reference-gas identity is present. Runtime still applies candidate/open-set/domain gates.
 */
public final class CalibrationCurvePublisher {
    private CalibrationCurvePublisher(){}

    public static final class Result {
        public final long curveId;
        public final boolean stored,activated;
        public final CalibrationQualityReport quality;
        public final String version,message;
        Result(long curveId,boolean stored,boolean activated,CalibrationQualityReport quality,String version,String message){
            this.curveId=curveId;this.stored=stored;this.activated=activated;this.quality=quality;
            this.version=version==null?"":version;this.message=message==null?"":message;
        }
    }

    public static Result publish(ExperimentsDb db,CalibrationSession session,CalibrationQualityPolicy policy,
                                 String version,String operatorNote,boolean requestActivation){
        if(db==null||session==null)throw new IllegalArgumentException("database/session required");
        if(policy==null)policy=CalibrationQualityPolicy.hazRobDefault();
        CalibrationQualityReport q=CalibrationQualityReport.evaluate(session,policy);
        List<CalibrationSession.Level> levels=session.exposureLevels();
        String v=version==null||version.trim().isEmpty()?defaultVersion(session):version.trim();
        boolean referenceKnown=session.referenceGas!=null&&!session.referenceGas.trim().isEmpty();
        boolean activate=requestActivation&&q.accepted&&referenceKnown;
        if(levels.size()<2){
            return new Result(-1,false,false,q,v,"Calibration retained as session evidence only; fewer than two concentration levels cannot form a curve.");
        }
        try{
            EmpiricalMqCurve curve=session.buildCurve("QC-gated HazRob device calibration");
            double[] ppm=new double[levels.size()],ratio=new double[levels.size()];
            for(int i=0;i<levels.size();i++){ppm[i]=levels.get(i).ppm;ratio[i]=levels.get(i).medianRatio;}
            String marker=CalibrationValidationMarker.encode(session,q,operatorNote);
            long id=db.saveCalibrationCurve(session.sensorId,session.chemicalId,v,isoNow(),session.referenceGas,marker,
                    curve.logPowerR2(),ppm,ratio,null,null,activate);
            String message;
            if(activate)message="Calibration stored and activated: QC accepted; runtime M4 remains candidate/open-set/domain gated.";
            else if(!q.accepted)message="Calibration stored INACTIVE: QC rejected. "+q.summary();
            else if(!referenceKnown)message="Calibration stored INACTIVE: QC passed but reference-gas identity is missing.";
            else message="Calibration stored INACTIVE by operator request despite accepted QC.";
            return new Result(id,id>0,activate,q,v,message);
        }catch(IllegalArgumentException ex){
            return new Result(-1,false,false,q,v,"Calibration curve rejected before storage: "+ex.getMessage());
        }
    }

    private static String defaultVersion(CalibrationSession s){return "m4-device-s"+s.sensorId+"-c"+s.chemicalId+"-"+System.currentTimeMillis();}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
