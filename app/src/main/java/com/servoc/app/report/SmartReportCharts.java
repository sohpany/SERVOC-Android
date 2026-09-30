package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * High-information visuals used by the HR069 smart report planner.
 * All values are read from persisted session/ChemCore evidence; no values are synthesized.
 */
public final class SmartReportCharts {
    private SmartReportCharts(){}

    /** 12x12 view of the operational 144-material panel, separating session evidence from array capability. */
    public static void screening144ReliabilityMatrix(Canvas c,RectF box,JSONObject chemcore){
        JSONObject panel=chemcore==null?null:chemcore.optJSONObject("screening_144");JSONArray rows=panel==null?null:panel.optJSONArray("rows");
        frame(c,box,"144-material session evidence map");
        if(rows==null||rows.length()==0){emptyMessage(c,box,"No persisted 144-panel result");return;}
        RectF grid=new RectF(box.left+24,box.top+42,box.right-24,box.bottom-55);float cw=grid.width()/12f,ch=grid.height()/12f;
        Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);fill.setStyle(Paint.Style.FILL);Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(.6f);stroke.setColor(0xFFFFFFFF);
        for(int i=0;i<144;i++){
            JSONObject r=i<rows.length()?rows.optJSONObject(i):null;int color=colorFor144(r);int row=i/12,col=i%12;float l=grid.left+col*cw,t=grid.top+row*ch;fill.setColor(color);c.drawRect(l,t,l+cw,t+ch,fill);c.drawRect(l,t,l+cw,t+ch,stroke);
            if(cw>=18&&ch>=14)small(c,String.valueOf(i+1),l+cw/2,t+ch*.66f,contrast(color),Paint.Align.CENTER,6.5f);
        }
        float y=box.bottom-38;legend(c,box.left+24,y,0xFF1B5E20,"Accepted identity");legend(c,box.left+142,y,0xFF2E7D32,"Strong session support");legend(c,box.left+302,y,0xFFF9A825,"Moderate/limited");legend(c,box.left+452,y,0xFFE0E0E0,"No positive session evidence");legend(c,box.left+650,y,0xFF616161,"Array unsupported");
        small(c,"Cell number = operational-panel index + 1. Session support is an evidence index, not probability of presence.",box.centerX(),box.bottom-12,Color.DKGRAY,Paint.Align.CENTER,7.5f);
    }

    /** Ranks only materials that have positive session candidate evidence. */
    public static void top144SessionSupport(Canvas c,RectF box,JSONObject chemcore){
        JSONObject panel=chemcore==null?null:chemcore.optJSONObject("screening_144");List<JSONObject> rows=sessionSupported(panel,12);frame(c,box,"Top 144-panel session evidence");
        if(rows.isEmpty()){emptyMessage(c,box,"No positive session candidate evidence");return;}
        RectF p=new RectF(box.left+165,box.top+42,box.right-28,box.bottom-35);float gh=p.height()/rows.size();
        for(int i=0;i<rows.size();i++){
            JSONObject r=rows.get(i);double score=r.optDouble("session_support_index_pct",Double.NaN);float y=p.top+i*gh+gh*.18f,h=gh*.56f,w=Double.isFinite(score)?(float)(Math.max(0,Math.min(100,score))/100.0*p.width()):0;
            Paint bg=fill(0xFFECEFF1);c.drawRoundRect(new RectF(p.left,y,p.right,y+h),4,4,bg);Paint fg=fill(supportColor(score,r.optBoolean("detected",false)));c.drawRoundRect(new RectF(p.left,y,p.left+w,y+h),4,4,fg);
            String label=(i+1)+". "+r.optString("name","#"+(r.optInt("index",-1)+1));small(c,shorten(label,25),p.left-8,y+h*.72f,Color.DKGRAY,Paint.Align.RIGHT,7.5f);small(c,Double.isFinite(score)?String.format(Locale.US,"%.1f",score):"unscored",p.left+Math.max(26,w-5),y+h*.72f,contrast(supportColor(score,r.optBoolean("detected",false))),Paint.Align.RIGHT,7f);
        }
        small(c,"Analytical support index (0–100 when available); not a calibrated detection probability.",box.centerX(),box.bottom-10,Color.DKGRAY,Paint.Align.CENTER,7.5f);
    }

    /** Candidate evidence matrix: confidence, channel coverage, agreement, evidence quality. */
    public static void candidateEvidenceMatrix(Canvas c,RectF box,JSONObject chemcore){
        JSONArray a=chemcore==null?null:chemcore.optJSONArray("candidates");frame(c,box,"Candidate evidence matrix");if(a==null||a.length()==0){emptyMessage(c,box,"No candidate evidence");return;}
        int n=Math.min(8,a.length());String[] headers={"Support","Coverage","Agreement","Evidence"};float left=box.left+145,top=box.top+48,rowH=(box.height()-82)/Math.max(1,n),colW=(box.width()-175)/headers.length;
        for(int k=0;k<headers.length;k++)small(c,headers[k],left+k*colW+colW/2,box.top+35,Color.DKGRAY,Paint.Align.CENTER,7.5f);
        for(int i=0;i<n;i++){
            JSONObject q=a.optJSONObject(i);if(q==null)continue;float y=top+i*rowH;small(c,(i+1)+". "+shorten(q.optString("label","#"+(i+1)),18),left-8,y+rowH*.62f,Color.DKGRAY,Paint.Align.RIGHT,7.3f);
            double[] v={q.optDouble("analytical_confidence_pct",Double.NaN),q.optDouble("coverage_pct",Double.NaN),q.optDouble("agreement_pct",Double.NaN),q.optDouble("evidence_quality_pct",Double.NaN)};
            for(int k=0;k<v.length;k++){int color=metricColor(v[k]);Paint p=fill(color);RectF cell=new RectF(left+k*colW+2,y+2,left+(k+1)*colW-2,y+rowH-2);c.drawRoundRect(cell,4,4,p);small(c,Double.isFinite(v[k])?String.format(Locale.US,"%.0f",v[k]):"—",cell.centerX(),cell.centerY()+3,contrast(color),Paint.Align.CENTER,8f);}
        }
        small(c,"Positive-evidence dimensions are displayed separately; they are not multiplied into a new probability.",box.centerX(),box.bottom-9,Color.DKGRAY,Paint.Align.CENTER,7.3f);
    }

    /** Decision-quality dashboard: keeps incomparable dimensions separate rather than forcing one composite score. */
    public static void decisionQuality(Canvas c,RectF box,JSONObject chemcore){
        frame(c,box,"Decision quality / uncertainty");if(chemcore==null){emptyMessage(c,box,"No ChemCore result");return;}
        double post=chemcore.optDouble("top_posterior",Double.NaN),margin=chemcore.optDouble("posterior_separation_margin",Double.NaN),entropy=chemcore.optDouble("normalized_entropy",Double.NaN),ood=chemcore.optDouble("ood_score",Double.NaN),confidence=chemcore.optDouble("analytical_confidence_pct",Double.NaN);
        String[] lab={"Ranking weight","Separation margin","Entropy","OOD distance","Evidence support"};double[] val={post,margin,entropy,ood,confidence};String[] semantics={"relative model rank","top − runner-up","normalized ambiguity","open-set distance","positive-evidence index %"};
        float gap=8,w=(box.width()-40-gap*4)/5f,x=box.left+20,y=box.top+55,h=box.height()-90;
        for(int i=0;i<5;i++){RectF r=new RectF(x+i*(w+gap),y,x+i*(w+gap)+w,y+h);c.drawRoundRect(r,7,7,fill(0xFFF4F6F8));small(c,lab[i],r.centerX(),r.top+22,Color.DKGRAY,Paint.Align.CENTER,7.2f);String value=i==4?(Double.isFinite(val[i])?String.format(Locale.US,"%.1f%%",val[i]):"—"):(Double.isFinite(val[i])?String.format(Locale.US,"%.3f",val[i]):"—");small(c,value,r.centerX(),r.centerY(),Color.BLACK,Paint.Align.CENTER,13f);small(c,semantics[i],r.centerX(),r.bottom-18,Color.DKGRAY,Paint.Align.CENTER,6.5f);}
    }

    /** M4 quantitative result visual. It never invents a confidence interval. */
    public static void quantitativeInterval(Canvas c,RectF box,JSONObject chemcore){
        frame(c,box,"Quantitative result / uncertainty interval");if(chemcore==null||!chemcore.optBoolean("ppm_reportable",false)){emptyMessage(c,box,"PPM withheld by analytical/M4 gate");return;}
        double ppm=chemcore.optDouble("ppm",Double.NaN),lo=chemcore.optDouble("prediction_low",Double.NaN),hi=chemcore.optDouble("prediction_high",Double.NaN);if(!Double.isFinite(ppm)){emptyMessage(c,box,"No finite reportable concentration");return;}
        boolean interval=Double.isFinite(lo)&&Double.isFinite(hi)&&lo<=ppm&&hi>=ppm&&hi>lo;double min=interval?Math.min(0,lo):0,max=interval?hi:Math.max(ppm*1.35,ppm+1);if(max<=min)max=min+1;
        float l=box.left+70,r=box.right-45,y=box.centerY()+5;Paint axis=stroke(0xFF78909C,2);c.drawLine(l,y,r,y,axis);
        if(interval){float xl=scale(lo,min,max,l,r),xh=scale(hi,min,max,l,r);Paint band=stroke(0xFF1565C0,7);c.drawLine(xl,y,xh,y,band);small(c,String.format(Locale.US,"%.3f",lo),xl,y+24,Color.DKGRAY,Paint.Align.CENTER,7.5f);small(c,String.format(Locale.US,"%.3f",hi),xh,y+24,Color.DKGRAY,Paint.Align.CENTER,7.5f);}
        float xp=scale(ppm,min,max,l,r);Paint dot=fill(0xFFC62828);c.drawCircle(xp,y,7,dot);small(c,String.format(Locale.US,"%.3f ppm",ppm),xp,y-18,Color.BLACK,Paint.Align.CENTER,10f);
        small(c,interval?"Recorded prediction interval shown":"No prediction interval recorded; point estimate only",box.centerX(),box.bottom-15,Color.DKGRAY,Paint.Align.CENTER,7.5f);
    }

    /** Sensor/evidence breadth for top candidates, without collapsing independence into posterior. */
    public static void evidenceBreadth(Canvas c,RectF box,JSONObject chemcore){
        JSONArray a=chemcore==null?null:chemcore.optJSONArray("candidates");frame(c,box,"Independent evidence breadth");if(a==null||a.length()==0){emptyMessage(c,box,"No candidate evidence");return;}int n=Math.min(8,a.length());
        String[] names=new String[n];double max=1;for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q!=null){max=Math.max(max,Math.max(q.optInt("evidence_group_count",0),q.optInt("evidence_sensor_breadth",0)));}}
        RectF p=new RectF(box.left+125,box.top+45,box.right-24,box.bottom-36);float gh=p.height()/n;
        for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;names[i]=q.optString("label","#"+(i+1));int groups=q.optInt("evidence_group_count",0),sensors=q.optInt("evidence_sensor_breadth",0);float y=p.top+i*gh+gh*.15f,h=gh*.28f;small(c,(i+1)+". "+shorten(names[i],16),p.left-7,y+h*1.7f,Color.DKGRAY,Paint.Align.RIGHT,7f);c.drawRect(p.left,y,p.left+(float)(groups/max)*p.width(),y+h,fill(0xFF1565C0));c.drawRect(p.left,y+h+3,p.left+(float)(sensors/max)*p.width(),y+2*h+3,fill(0xFF2E7D32));small(c,"G"+groups+" / S"+sensors,p.right,y+2*h,Color.DKGRAY,Paint.Align.RIGHT,6.5f);}
        small(c,"Blue = independent evidence groups • Green = sensor breadth",box.centerX(),box.bottom-10,Color.DKGRAY,Paint.Align.CENTER,7.2f);
    }

    private static List<JSONObject> sessionSupported(JSONObject panel,int limit){List<JSONObject> out=new ArrayList<>();JSONArray rows=panel==null?null:panel.optJSONArray("rows");if(rows==null)return out;for(int i=0;i<rows.length();i++){JSONObject q=rows.optJSONObject(i);if(q!=null&&q.optBoolean("session_candidate_evidence",false))out.add(q);}Collections.sort(out,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){double x=a.optDouble("session_support_index_pct",Double.NaN),y=b.optDouble("session_support_index_pct",Double.NaN);if(Double.isFinite(x)&&Double.isFinite(y)){int n=Double.compare(y,x);if(n!=0)return n;}else if(Double.isFinite(x))return-1;else if(Double.isFinite(y))return 1;return Integer.compare(a.optInt("candidate_rank",999),b.optInt("candidate_rank",999));}});return out.size()>limit?new ArrayList<>(out.subList(0,limit)):out;}
    private static int colorFor144(JSONObject r){if(r==null)return 0xFFF5F5F5;if(r.optBoolean("detected",false))return 0xFF1B5E20;if(!r.optBoolean("catalogue_array_supported",!"UNSUPPORTED_BY_CURRENT_ARRAY".equals(r.optString("status",""))))return 0xFF616161;if(!r.optBoolean("session_candidate_evidence",false))return 0xFFE0E0E0;double s=r.optDouble("session_support_index_pct",Double.NaN);return supportColor(s,false);}
    private static int supportColor(double v,boolean accepted){if(accepted)return 0xFF1B5E20;if(!Double.isFinite(v))return 0xFF90A4AE;if(v>=85)return 0xFF2E7D32;if(v>=70)return 0xFF558B2F;if(v>=50)return 0xFFF9A825;if(v>=30)return 0xFFEF6C00;return 0xFFB71C1C;}
    private static int metricColor(double v){if(!Double.isFinite(v))return 0xFFE0E0E0;if(v>=85)return 0xFF2E7D32;if(v>=70)return 0xFF7CB342;if(v>=50)return 0xFFF9A825;if(v>=30)return 0xFFEF6C00;return 0xFFC62828;}
    private static int contrast(int color){double y=(0.299*Color.red(color)+0.587*Color.green(color)+0.114*Color.blue(color))/255.0;return y>.62?Color.BLACK:Color.WHITE;}
    private static float scale(double v,double min,double max,float l,float r){return (float)(l+(v-min)/(max-min)*(r-l));}
    private static void frame(Canvas c,RectF b,String title){c.drawRect(b,fill(Color.WHITE));c.drawRect(b,stroke(0xFFCFD8DC,1));small(c,title,b.left+10,b.top+21,Color.BLACK,Paint.Align.LEFT,12f,true);}
    private static void emptyMessage(Canvas c,RectF b,String text){small(c,text,b.centerX(),b.centerY(),Color.DKGRAY,Paint.Align.CENTER,9f);}
    private static void legend(Canvas c,float x,float y,int color,String label){c.drawRect(x,y-8,x+12,y+4,fill(color));small(c,label,x+16,y+2,Color.DKGRAY,Paint.Align.LEFT,6.7f);}
    private static Paint fill(int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);return p;}
    private static Paint stroke(int color,float width){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);return p;}
    private static void small(Canvas c,String text,float x,float y,int color,Paint.Align align,float size){small(c,text,x,y,color,align,size,false);}
    private static void small(Canvas c,String text,float x,float y,int color,Paint.Align align,float size,boolean bold){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setFakeBoldText(bold);c.drawText(text==null?"":text,x,y,p);}
    private static String shorten(String s,int n){String q=s==null?"":s.trim();return q.length()<=n?q:q.substring(0,Math.max(1,n-1))+"…";}
}
