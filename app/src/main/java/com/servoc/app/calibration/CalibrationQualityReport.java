package com.servoc.app.calibration;

import com.servoc.app.chemical.ManufacturerEvidenceMatrix;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Quality-gate result for one device-specific MQ calibration session. */
public final class CalibrationQualityReport {
    public static final String SCHEMA="hazrob-calibration-qc/1";
    public final boolean accepted;
    public final int distinctConcentrations;
    public final double concentrationSpanFactor;
    public final double logPowerR2;
    public final double worstReplicateCvPercent;
    public final List<String> failures;
    public final List<String> warnings;

    private CalibrationQualityReport(boolean accepted,int distinctConcentrations,
                                     double concentrationSpanFactor,double logPowerR2,
                                     double worstReplicateCvPercent,
                                     List<String> failures,List<String> warnings){
        this.accepted=accepted;
        this.distinctConcentrations=distinctConcentrations;
        this.concentrationSpanFactor=concentrationSpanFactor;
        this.logPowerR2=logPowerR2;
        this.worstReplicateCvPercent=worstReplicateCvPercent;
        this.failures=Collections.unmodifiableList(failures);
        this.warnings=Collections.unmodifiableList(warnings);
    }

    public static CalibrationQualityReport evaluate(CalibrationSession session,CalibrationQualityPolicy policy){
        if(session==null||policy==null)throw new IllegalArgumentException("session/policy required");
        List<String> fail=new ArrayList<>(),warn=new ArrayList<>();
        List<CalibrationSession.Level> levels=session.exposureLevels();

        if(policy.requireBaseline&&!session.hasPhase(CalibrationSession.BASELINE)) fail.add("Missing baseline phase.");
        if(policy.requireRecovery&&!session.hasPhase(CalibrationSession.RECOVERY)) fail.add("Missing recovery phase.");
        if(policy.requireHeaterProtocolMetadata&&!session.allHeaterMetadataValid()) fail.add("Invalid or missing heater protocol metadata in an exposure capture.");
        if(levels.size()<policy.minDistinctConcentrations)
            fail.add("Insufficient distinct reference concentrations: "+levels.size()+" < "+policy.minDistinctConcentrations+".");

        // Manufacturer ranges are evidence boundaries, not a substitute for device-specific calibration.
        // A controlled research calibration may intentionally probe outside them, so this is a warning
        // rather than an automatic rejection. The resulting model must carry its own validated domain.
        ManufacturerEvidenceMatrix.Evidence manufacturer=ManufacturerEvidenceMatrix.get(session.sensorId,session.chemicalId);
        if(manufacturer.level==ManufacturerEvidenceMatrix.NONE){
            warn.add("No registered manufacturer target/graph evidence for this sensor/analyte pair; treat calibration as experimental evidence requiring independent validation.");
        }else{
            for(CalibrationSession.Level l:levels){
                if(l.ppm<manufacturer.minPpm||l.ppm>manufacturer.maxPpm)
                    warn.add("Reference level "+l.ppm+" ppm is outside the registered manufacturer evidence range "+manufacturer.minPpm+"-"+manufacturer.maxPpm+" ppm; do not label this point manufacturer-supported.");
            }
        }

        double worstCv=0;
        for(CalibrationSession.Level l:levels){
            if(l.replicates<policy.minReplicatesPerConcentration)
                fail.add("Insufficient replicates at "+l.ppm+" ppm: "+l.replicates+" < "+policy.minReplicatesPerConcentration+".");
            if(Double.isFinite(l.cvPercent)){
                worstCv=Math.max(worstCv,l.cvPercent);
                if(l.cvPercent>policy.maxReplicateCvPercent)
                    fail.add("Replicate CV too high at "+l.ppm+" ppm: "+round(l.cvPercent)+"% > "+policy.maxReplicateCvPercent+"%.");
            }else fail.add("Replicate dispersion could not be evaluated at "+l.ppm+" ppm.");
        }

        double span=Double.NaN;
        if(levels.size()>=2){
            double min=levels.get(0).ppm,max=levels.get(levels.size()-1).ppm;
            span=max/min;
            if(!Double.isFinite(span)||span<policy.minConcentrationSpanFactor)
                fail.add("Reference concentration span too narrow: "+round(span)+"x < "+policy.minConcentrationSpanFactor+"x.");
        }

        double r2=Double.NaN;
        if(levels.size()>=2){
            try{
                EmpiricalMqCurve curve=session.buildCurve("HazRob calibration QC candidate");
                r2=curve.logPowerR2();
                if(Double.isFinite(r2)&&r2<policy.minLogPowerR2)
                    fail.add("Global log-power diagnostic R² too low: "+round(r2)+" < "+policy.minLogPowerR2+".");
                if(!Double.isFinite(r2))warn.add("R² diagnostic unavailable; inspect calibration manually.");
            }catch(IllegalArgumentException e){
                fail.add("Calibration response is not usable/monotonic: "+e.getMessage());
            }
        }

        // Temperature/RH are recorded for later compensation; large variation is a warning,
        // not automatically a rejection until a device-specific compensation model exists.
        double minT=Double.POSITIVE_INFINITY,maxT=Double.NEGATIVE_INFINITY;
        double minH=Double.POSITIVE_INFINITY,maxH=Double.NEGATIVE_INFINITY;
        for(CalibrationSession.Level l:levels){
            if(Double.isFinite(l.medianTempC)){minT=Math.min(minT,l.medianTempC);maxT=Math.max(maxT,l.medianTempC);}
            if(Double.isFinite(l.medianRh)){minH=Math.min(minH,l.medianRh);maxH=Math.max(maxH,l.medianRh);}
        }
        if(maxT>=minT&&maxT-minT>5)warn.add("Temperature varied by "+round(maxT-minT)+" °C across calibration levels.");
        if(maxH>=minH&&maxH-minH>15)warn.add("Relative humidity varied by "+round(maxH-minH)+" percentage points across calibration levels.");

        return new CalibrationQualityReport(fail.isEmpty(),levels.size(),span,r2,worstCv,fail,warn);
    }

    public JSONObject toJson(){
        JSONObject o=new JSONObject();try{o.put("schema",SCHEMA);o.put("accepted",accepted);o.put("distinct_concentrations",distinctConcentrations);put(o,"concentration_span_factor",concentrationSpanFactor);put(o,"log_power_r2",logPowerR2);put(o,"worst_replicate_cv_pct",worstReplicateCvPercent);JSONArray f=new JSONArray();for(String s:failures)f.put(s);o.put("failures",f);JSONArray w=new JSONArray();for(String s:warnings)w.put(s);o.put("warnings",w);}catch(Exception ignored){}return o;
    }

    public String summary(){
        StringBuilder s=new StringBuilder(accepted?"ACCEPTED":"REJECTED");
        s.append(" | levels=").append(distinctConcentrations)
         .append(" | span=").append(round(concentrationSpanFactor)).append("x")
         .append(" | R2=").append(round(logPowerR2))
         .append(" | worst CV=").append(round(worstReplicateCvPercent)).append("%");
        for(String f:failures)s.append("\nFAIL: ").append(f);
        for(String w:warnings)s.append("\nWARN: ").append(w);
        return s.toString();
    }

    private static void put(JSONObject o,String key,double x)throws Exception{o.put(key,Double.isFinite(x)?x:JSONObject.NULL);}
    private static String round(double x){
        if(!Double.isFinite(x))return "NA";
        return String.format(java.util.Locale.US,"%.3f",x);
    }
}
