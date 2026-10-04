package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Advanced SmartReport diagnostics derived only from persisted HR069 session/ChemCore evidence.
 * No solver, QSPR model or response curve is rerun here; this class is presentation-only.
 */
public final class SmartReportAdvancedCharts {
    private SmartReportAdvancedCharts(){}

    /** Pearson correlation of recorded log10(Rs/R0) trajectories. Descriptive only. */
    public static void mqCorrelationMatrix(Canvas c, RectF box, SessionSeries.Series s){
        panel(c,box,"ترابط MQ • Pearson r للـ log10(Rs/R0)");
        if(s==null||!s.usable()){empty(c,box,"السلسلة الزمنية غير متاحة");return;}
        final int n=HazRobHardwareProfile.MQ.length;
        float left=box.left+44,top=box.top+40;
        float size=Math.min(box.width()-58,box.height()-72);
        float cell=size/n;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        for(int i=0;i<n;i++){
            String ki=HazRobHardwareProfile.MQ[i].key;
            text(c,ki.toUpperCase(Locale.US),left+i*cell+cell*.5f,top-7,5.2f,Color.DKGRAY,true,Paint.Align.CENTER);
            text(c,ki.toUpperCase(Locale.US),left-5,top+i*cell+cell*.67f,5.2f,Color.DKGRAY,true,Paint.Align.RIGHT);
            for(int j=0;j<n;j++){
                double r=i==j?1.0:pearsonLogRatio(s,ki,HazRobHardwareProfile.MQ[j].key);
                p.setStyle(Paint.Style.FILL);p.setColor(corrColor(r));
                RectF q=new RectF(left+j*cell,top+i*cell,left+(j+1)*cell,top+(i+1)*cell);c.drawRect(q,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.45f);p.setColor(0x66FFFFFF);c.drawRect(q,p);
                if(Double.isFinite(r))text(c,String.format(Locale.US,"%.2f",r),q.centerX(),q.centerY()+2,4.7f,Math.abs(r)>.55?Color.WHITE:Color.DKGRAY,false,Paint.Align.CENTER);
            }
        }
        text(c,"ارتباط وصفي للسلوك الزمني فقط • لا يثبت الهوية ولا الاستقلال الإحصائي",box.centerX(),box.bottom-8,5.6f,Color.DKGRAY,false,Paint.Align.CENTER);
    }

    /**
     * Stage-17 common-concentration consistency diagnostic for the top candidate.
     * X = model-predicted log10 ratio at stored C*, Y = observed log10 ratio. Points close to
     * y=x indicate cross-channel curve agreement at the already-computed common concentration.
     */
    public static void curveConsistency(Canvas c,RectF box,JSONObject decision){
        panel(c,box,"اتساق المنحنيات عند C* • Stage17 common-C");
        JSONObject cand=topCandidate(decision);if(cand==null){empty(c,box,"لا يوجد مرشح ChemCore محفوظ");return;}
        JSONArray a=cand.optJSONArray("evidence_observations");List<Point> pts=new ArrayList<>();
        if(a!=null)for(int i=0;i<a.length();i++){
            JSONObject e=a.optJSONObject(i);if(e==null)continue;
            double obs=e.optDouble("observed_ratio",Double.NaN),pred=e.optDouble("predicted_log10_ratio",Double.NaN),z=e.optDouble("standardized_residual",Double.NaN);
            if(!(obs>0)||!Double.isFinite(obs)||!Double.isFinite(pred))continue;
            String sensor=sensorLabel(e);pts.add(new Point(sensor,pred,Math.log10(obs),z,e.optBoolean("inside_domain",false)));
        }
        if(pts.size()<2){empty(c,box,"يتطلب ≥2 قنوات رقمية محفوظة • single-channel لا يمثل تقاطعًا");return;}
        double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY,sumZ2=0;int zN=0;
        for(Point p:pts){lo=Math.min(lo,Math.min(p.pred,p.obs));hi=Math.max(hi,Math.max(p.pred,p.obs));if(Double.isFinite(p.z)){sumZ2+=p.z*p.z;zN++;}}
        if(!(hi>lo)){hi=lo+1;}double pad=Math.max(.08,(hi-lo)*.12);lo-=pad;hi+=pad;
        float l=box.left+34,r=box.right-12,t=box.top+43,b=box.bottom-35;
        Paint ax=new Paint(Paint.ANTI_ALIAS_FLAG);ax.setColor(0xFFB0BEC5);ax.setStrokeWidth(.7f);c.drawLine(l,b,r,b,ax);c.drawLine(l,t,l,b,ax);
        float x0=map(lo,lo,hi,l,r),y0=map(lo,lo,hi,b,t),x1=map(hi,lo,hi,l,r),y1=map(hi,lo,hi,b,t);Paint diag=new Paint(Paint.ANTI_ALIAS_FLAG);diag.setColor(0xFF78909C);diag.setStrokeWidth(1f);c.drawLine(x0,y0,x1,y1,diag);
        for(Point p:pts){float x=map(p.pred,lo,hi,l,r),y=map(p.obs,lo,hi,b,t);Paint dot=new Paint(Paint.ANTI_ALIAS_FLAG);dot.setStyle(Paint.Style.FILL);dot.setColor(p.inside?0xFF2E7D32:0xFFEF6C00);c.drawCircle(x,y,4.5f,dot);text(c,p.sensor,x+5,y-4,5.2f,Color.DKGRAY,true,Paint.Align.LEFT);}
        text(c,"predicted log10 ratio",(l+r)/2,b+14,5.4f,Color.DKGRAY,false,Paint.Align.CENTER);text(c,"observed",l-4,t-5,5.4f,Color.DKGRAY,false,Paint.Align.RIGHT);
        double fit=firstFinite(cand,"screening_fit_ppm","best_ppm");double rms=zN>0?Math.sqrt(sumZ2/zN):Double.NaN;
        String note="C* screening="+(Double.isFinite(fit)?String.format(Locale.US,"%.3g",fit):"—")+" ppm • z-RMS="+(Double.isFinite(rms)?String.format(Locale.US,"%.2f",rms):"—")+" • "+pts.size()+" ch";
        text(c,note,box.centerX(),box.bottom-19,5.6f,Color.DKGRAY,true,Paint.Align.CENTER);
        text(c,"تشخيص توافق منحنيات محفوظ؛ C* ليست PPM تقريرية إلا بعد بوابة M4",box.centerX(),box.bottom-7,5.3f,Color.DKGRAY,false,Paint.Align.CENTER);
    }

    private static final class Point{final String sensor;final double pred,obs,z;final boolean inside;Point(String s,double p,double o,double z,boolean in){sensor=s;pred=p;obs=o;this.z=z;inside=in;}}

    private static JSONObject topCandidate(JSONObject q){if(q==null)return null;JSONArray a=q.optJSONArray("candidates");return a!=null&&a.length()>0?a.optJSONObject(0):null;}
    private static String sensorLabel(JSONObject e){JSONArray a=e.optJSONArray("sensor_keys");if(a!=null&&a.length()>0)return a.optString(0,"MQ").toUpperCase(Locale.US);String s=e.optString("sensor_key","");return s.isEmpty()?"MQ":s.toUpperCase(Locale.US);}
    private static double firstFinite(JSONObject q,String... keys){if(q==null)return Double.NaN;for(String k:keys){double v=q.optDouble(k,Double.NaN);if(Double.isFinite(v))return v;}return Double.NaN;}

    private static double pearsonLogRatio(SessionSeries.Series s,String a,String b){double sx=0,sy=0,sxx=0,syy=0,sxy=0;int n=0;for(SessionSeries.Sample row:s.samples){JSONObject qa=SessionSeries.sensor(row,a),qb=SessionSeries.sensor(row,b);double x=SessionSeries.value(qa,"ratio"),y=SessionSeries.value(qb,"ratio");if(!(x>0)||!(y>0)||!Double.isFinite(x)||!Double.isFinite(y))continue;x=Math.log10(x);y=Math.log10(y);sx+=x;sy+=y;sxx+=x*x;syy+=y*y;sxy+=x*y;n++;}if(n<4)return Double.NaN;double vx=n*sxx-sx*sx,vy=n*syy-sy*sy;if(!(vx>1e-15)||!(vy>1e-15))return Double.NaN;return Math.max(-1,Math.min(1,(n*sxy-sx*sy)/Math.sqrt(vx*vy)));}
    private static int corrColor(double v){if(!Double.isFinite(v))return 0xFFECEFF1;double x=Math.max(-1,Math.min(1,v));if(x>=0){int t=(int)Math.round(255*x);return Color.rgb(255,245-t/2,245-t);}int t=(int)Math.round(255*(-x));return Color.rgb(245-t,245-t/2,255);}
    private static float map(double v,double lo,double hi,float a,float b){return (float)(a+(v-lo)/(hi-lo)*(b-a));}
    private static void panel(Canvas c,RectF r,String title){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.FILL);p.setColor(0xFFF8FAFC);c.drawRoundRect(r,6,6,p);text(c,title,r.left+8,r.top+18,7.5f,Color.BLACK,true,Paint.Align.LEFT);}
    private static void empty(Canvas c,RectF r,String msg){text(c,msg,r.centerX(),r.centerY(),6.5f,Color.DKGRAY,false,Paint.Align.CENTER);}
    private static void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);p.setTextAlign(align);c.drawText(s==null?"":s,x,y,p);}
}