package com.servoc.app.report;

import com.servoc.app.data.SessionSeries;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HazRob 0.10.2 / HR069 report-readiness policy.
 * Decides what can be shown from recorded evidence; it never creates missing evidence.
 */
public final class ReportIntelligence {
    private ReportIntelligence(){}

    public static final int CHART_COUNT=20;
    public enum State{EMPTY,SIGNAL_ONLY,ANALYTICAL_ONLY,SCREENING,IDENTIFIED,QUANTIFIED}

    public static final class ChartAvailability{
        public final int index;public final boolean available;public final String source,reason;
        ChartAvailability(int index,boolean available,String source,String reason){this.index=index;this.available=available;this.source=source;this.reason=reason;}
    }

    public static final class Snapshot{
        public final State state;public final boolean timeSeriesAvailable,chemCoreAvailable,ppmReportable,safetyInterpretable;
        public final int candidateCount,screenedCount,screeningTarget,availableChartCount;
        public final List<Integer> recommendedCharts;
        Snapshot(State state,boolean series,boolean chem,boolean ppm,boolean safety,int candidates,int screened,int target,int available,List<Integer> recommended){
            this.state=state;timeSeriesAvailable=series;chemCoreAvailable=chem;ppmReportable=ppm;safetyInterpretable=safety;candidateCount=candidates;screenedCount=screened;screeningTarget=target;availableChartCount=available;recommendedCharts=Collections.unmodifiableList(new ArrayList<>(recommended));
        }
    }

    public static Snapshot evaluate(SessionSeries.Series series,JSONObject chemcore){
        boolean seriesOk=series!=null&&series.usable(),chemOk=chemcore!=null;
        JSONArray candidates=chemOk?chemcore.optJSONArray("candidates"):null;int candidateCount=candidates==null?0:candidates.length();
        JSONObject screening=chemOk?chemcore.optJSONObject("screening_144"):null;int screened=screening==null?0:screening.optInt("evaluated_count",0),target=screening==null?0:screening.optInt("target_count",144);
        boolean ppm=chemOk&&chemcore.optBoolean("ppm_reportable",false)&&Double.isFinite(chemcore.optDouble("ppm",Double.NaN));
        JSONObject gas=chemOk?chemcore.optJSONObject("gas_exposure_ppm"):null;boolean safety=gas!=null&&gas.optBoolean("input_gate_passed",false);
        String outcome=chemOk?chemcore.optString("outcome",""):"";State state;
        if(!seriesOk&&!chemOk)state=State.EMPTY;
        else if(!chemOk)state=State.SIGNAL_ONLY;
        else if(ppm||"QUANTIFIED".equals(outcome))state=State.QUANTIFIED;
        else if("IDENTIFIED".equals(outcome))state=State.IDENTIFIED;
        else if(candidateCount>0||screened>0)state=State.SCREENING;
        else state=State.ANALYTICAL_ONLY;
        int available=0;for(int i=0;i<CHART_COUNT;i++)if(chart(i,series,chemcore).available)available++;
        List<Integer> rec=new ArrayList<>();
        if(chemOk){add(rec,3);add(rec,6);if(screened>0){add(rec,7);add(rec,8);}add(rec,9);if(safety)add(rec,10);}
        if(seriesOk){add(rec,0);add(rec,1);add(rec,12);add(rec,13);add(rec,14);add(rec,15);}
        if(rec.size()>8)rec=new ArrayList<>(rec.subList(0,8));
        return new Snapshot(state,seriesOk,chemOk,ppm,safety,candidateCount,screened,target,available,rec);
    }

    public static ChartAvailability chart(int index,SessionSeries.Series series,JSONObject chemcore){
        boolean seriesOk=series!=null&&series.usable(),chemOk=chemcore!=null;
        if(index<0||index>=CHART_COUNT)return new ChartAvailability(index,false,"NONE","unknown_chart");
        boolean needsChem=index>=3&&index<=10,needsSeries=index<=2||index>=11;
        if(needsChem&&!chemOk)return new ChartAvailability(index,false,"CHEMCORE","chemcore_result_missing");
        if(needsSeries&&!seriesOk)return new ChartAvailability(index,false,"RECORDED_SERIES","recorded_time_series_missing");
        if(index==7||index==8){JSONObject s=chemcore.optJSONObject("screening_144");if(s==null||s.optInt("evaluated_count",0)<=0)return new ChartAvailability(index,false,"CHEMCORE","screening_144_missing");}
        if(index==10){JSONObject g=chemcore.optJSONObject("gas_exposure_ppm");if(g==null)return new ChartAvailability(index,false,"CHEMCORE","gas_safety_payload_missing");}
        return new ChartAvailability(index,true,needsChem?"CHEMCORE":"RECORDED_SERIES","available");
    }

    private static void add(List<Integer> a,int v){if(!a.contains(v))a.add(v);}
}
