package com.servoc.app.report;

import com.servoc.app.data.SessionSeries;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Shared scoring policy for selecting the primary analytical measurement inside a Field Session.
 *
 * This is a report-selection policy only; it does not alter ChemCore inference or candidate ranking.
 * The score intentionally mirrors the current SmartFieldSessionPdfWriter policy so PDF and XLSX
 * select the same analytical measurement. Any future scoring change must update both usages/tests
 * under the SmartReport/ChemCore Change Record.
 */
final class SmartReportPrimarySelector {
    private SmartReportPrimarySelector(){}

    static int score(SessionSeries.Series s, JSONObject d, int records){
        int x=Math.min(12,Math.max(0,records));
        if(s!=null&&s.usable()){
            x+=70;
            x+=Math.min(20,s.samples.size()/5);
        }
        if(d!=null){
            x+=100;
            JSONArray a=d.optJSONArray("candidates");
            if(a!=null)x+=Math.min(20,a.length()*3);
            double p=topPosterior(d);
            if(Double.isFinite(p))x+=(int)Math.round(Math.max(0,Math.min(1,p))*20);
            int v=validMq(d);
            if(v>0)x+=Math.min(8,v);
            String o=d.optString("outcome","");
            if("IDENTIFIED".equals(o))x+=20;
            if("QUANTIFIED".equals(o)||d.optBoolean("ppm_reportable",false))x+=40;
        }
        return x;
    }

    private static double topPosterior(JSONObject d){
        if(d==null)return Double.NaN;
        double p=d.optDouble("top_posterior",Double.NaN);
        if(Double.isFinite(p))return p;
        JSONArray a=d.optJSONArray("candidates");
        if(a!=null&&a.length()>0){JSONObject q=a.optJSONObject(0);if(q!=null)return q.optDouble("posterior",Double.NaN);}
        return Double.NaN;
    }

    private static int validMq(JSONObject d){
        if(d==null)return -1;
        JSONObject q=d.optJSONObject("measurement_quality");
        if(q==null)q=d.optJSONObject("lab_quality");
        if(q!=null){int v=q.optInt("valid_channels",-1);if(v>=0)return v;}
        return d.optInt("valid_mq_channels",-1);
    }
}
