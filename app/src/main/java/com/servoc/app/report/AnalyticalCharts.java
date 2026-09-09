package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.servoc.app.calibration.SqliteCalibrationProvider;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.sensors.RobotTelemetry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Canvas charts used by HazRob analytical PDF/XLSX reports. Every plotted point comes from recorded session data. */
public final class AnalyticalCharts {
    private AnalyticalCharts(){}
    private static final int[] PALETTE={0xFF1565C0,0xFFC62828,0xFF2E7D32,0xFF6A1B9A,0xFFEF6C00,0xFF00838F,0xFFAD1457,0xFF455A64};
    private static final boolean[] HEATER_VALID={true,true,true,true,false,true,false,true};

    private static final class Line{final String name;final double[] x,y;final int color;Line(String n,double[] x,double[] y,int c){name=n;this.x=x;this.y=y;color=c;}}

    public static void mqRsR0(Canvas c,RectF box,SessionSeries.Series s){
        List<Line> lines=new ArrayList<>();int n=0;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){Line l=sensorLine(s,hw.key,"ratio",hw.key.toUpperCase(Locale.US),PALETTE[n++%PALETTE.length]);if(l!=null)lines.add(l);}
        lineChart(c,box,"MQ Rs/R0 vs time","time (s)","Rs/R0",lines);
    }

    public static void fingerprint(Canvas c,RectF box,SessionSeries.Series s){
        String[] labels=new String[HazRobHardwareProfile.MQ.length];double[] response=new double[labels.length];double max=0;
        for(int i=0;i<labels.length;i++){
            HazRobHardwareProfile.Channel hw=HazRobHardwareProfile.MQ[i];labels[i]=hw.key.toUpperCase(Locale.US);double a=medianWindow(s,hw.key,0.00,0.25),b=medianWindow(s,hw.key,0.75,1.00);
            double v=Double.isFinite(a)&&Double.isFinite(b)&&a>0&&b>0?Math.abs(Math.log(b/a)):Double.NaN;response[i]=v;if(Double.isFinite(v))max=Math.max(max,v);
        }
        if(max>0)for(int i=0;i<response.length;i++)if(Double.isFinite(response[i]))response[i]/=max;
        barChart(c,box,"Sensor fingerprint","normalized response",labels,response);
    }

    /** Returns the number of quantitative chemical curves actually plotted. */
    public static int ppm(Canvas c,RectF box,SessionSeries.Series s,ExperimentsDb db,JSONObject analytics){
        if(s==null||!s.usable()||db==null){empty(c,box,"Quantitative concentration vs time","No quantitative time-series");return 0;}
        JSONArray a=analytics==null?null:analytics.optJSONArray("results");if(a==null||a.length()==0){empty(c,box,"Quantitative concentration vs time","No analytical candidates");return 0;}
        List<JSONObject> candidates=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null)candidates.add(x);}candidates.sort(Comparator.comparingDouble((JSONObject x)->x.optDouble("agreement",0)).reversed());if(candidates.size()>6)candidates=candidates.subList(0,6);
        SqliteCalibrationProvider calibration=new SqliteCalibrationProvider(db);List<Line> lines=new ArrayList<>();int color=0;
        for(JSONObject candidate:candidates){int chemical=candidate.optInt("chemical_id",candidate.optInt("legacy_id",-1));if(chemical<0)continue;List<Double> xx=new ArrayList<>(),yy=new ArrayList<>();for(SessionSeries.Sample q:s.samples){JSONObject root=new JSONObject();try{root.put("mq",q.mq);}catch(Exception ignored){}double[] ratios=RobotTelemetry.extractRatios(root);ValidatedGasEngine.Result r=ValidatedGasEngine.analyzeOne(ratios,chemical,HEATER_VALID,calibration,false);if(r!=null&&r.quantitative&&Double.isFinite(r.ppm)&&r.ppm>0){xx.add(q.tMs/1000.0);yy.add(r.ppm);}}if(yy.size()>=2){String name=candidate.optString("name_ar",candidate.optString("name",candidate.optString("name_en","#"+chemical)));lines.add(new Line(shortName(name,15),arr(xx),arr(yy),PALETTE[color++%PALETTE.length]));}}
        lineChart(c,box,"Quantitative concentration vs time","time (s)","ppm",lines);return lines.size();
    }

    public static void temperature(Canvas c,RectF box,SessionSeries.Series s){lineChart(c,box,"Temperature vs time","time (s)","C",singleEnv(s,"temperature","Temperature",0xFFB71C1C));}
    public static void humidity(Canvas c,RectF box,SessionSeries.Series s){lineChart(c,box,"Humidity vs time","time (s)","%RH",singleEnv(s,"humidity","Humidity",0xFF0277BD));}
    public static void pressure(Canvas c,RectF box,SessionSeries.Series s){lineChart(c,box,"Pressure vs time","time (s)","hPa",singleEnv(s,"pressure","Pressure",0xFF455A64));}

    public static void sound(Canvas c,RectF box,SessionSeries.Series s){
        List<Double> xx=new ArrayList<>(),rms=new ArrayList<>(),pp=new ArrayList<>();
        for(SessionSeries.Sample q:s.samples){JSONObject sound=q.state.optJSONObject("sound");if(sound==null)continue;double a=sound.optDouble("rms_v",Double.NaN),b=sound.optDouble("p2p_v",Double.NaN);if(!Double.isFinite(a)&&!Double.isFinite(b))continue;xx.add(q.tMs/1000.0);rms.add(a);pp.add(b);}
        List<Line> lines=new ArrayList<>();if(valid(rms)>=2)lines.add(new Line("RMS",arr(xx),arr(rms),0xFF1565C0));if(valid(pp)>=2)lines.add(new Line("Peak-Peak",arr(xx),arr(pp),0xFFEF6C00));lineChart(c,box,"Sound vs time","time (s)","V",lines);
    }

    /** Supports both legacy agreement results and ChemCore posterior candidates. */
    public static void candidates(Canvas c,RectF box,JSONObject analytics){
        if(analytics==null){empty(c,box,"Chemical candidates","No analytical results");return;}
        JSONArray cc=analytics.optJSONArray("candidates");
        if(cc!=null){
            List<JSONObject> rows=new ArrayList<>();for(int i=0;i<cc.length();i++){JSONObject x=cc.optJSONObject(i);if(x!=null&&Double.isFinite(x.optDouble("posterior",Double.NaN)))rows.add(x);}rows.sort(Comparator.comparingDouble((JSONObject x)->x.optDouble("posterior",0)).reversed());if(rows.size()>8)rows=rows.subList(0,8);
            String[] names=new String[rows.size()];double[] values=new double[rows.size()];for(int i=0;i<rows.size();i++){JSONObject x=rows.get(i);names[i]=shortName(x.optString("label","#"+x.optLong("chemical_uid",-1)),18);values[i]=Math.max(0,Math.min(1,x.optDouble("posterior",Double.NaN)));}
            barChart(c,box,"ChemCore candidate posterior","posterior",names,values);return;
        }
        JSONArray a=analytics.optJSONArray("results");if(a==null){empty(c,box,"Chemical candidates","No analytical results");return;}
        List<JSONObject> rows=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optBoolean("evidence_present",true))rows.add(x);}rows.sort(Comparator.comparingDouble((JSONObject x)->x.optDouble("agreement",0)).reversed());if(rows.size()>8)rows=rows.subList(0,8);
        String[] names=new String[rows.size()];double[] values=new double[rows.size()];for(int i=0;i<rows.size();i++){JSONObject x=rows.get(i);String name=x.optString("name_ar",x.optString("name",x.optString("name_en","#"+x.optInt("chemical_id",-1))));names[i]=shortName(name,18);values[i]=x.optDouble("agreement",Double.NaN)/100.0;}
        barChart(c,box,"Chemical candidate agreement","agreement",names,values);
    }

    private static List<Line> singleEnv(SessionSeries.Series s,String key,String name,int color){
        List<Double>x=new ArrayList<>(),y=new ArrayList<>();for(SessionSeries.Sample q:s.samples){double v=SessionSeries.value(q.env,key);if(Double.isFinite(v)){x.add(q.tMs/1000.0);y.add(v);}}List<Line> out=new ArrayList<>();if(y.size()>=2)out.add(new Line(name,arr(x),arr(y),color));return out;
    }

    private static Line sensorLine(SessionSeries.Series s,String sensor,String value,String label,int color){
        List<Double>x=new ArrayList<>(),y=new ArrayList<>();for(SessionSeries.Sample q:s.samples){JSONObject m=SessionSeries.sensor(q,sensor);double v=m==null?Double.NaN:m.optDouble(value,Double.NaN);if(Double.isFinite(v)&&v>0){x.add(q.tMs/1000.0);y.add(v);}}return y.size()<2?null:new Line(label,arr(x),arr(y),color);
    }

    private static double medianWindow(SessionSeries.Series s,String sensor,double f0,double f1){
        if(s.samples.isEmpty())return Double.NaN;long duration=Math.max(1,s.durationMs());List<Double> v=new ArrayList<>();for(SessionSeries.Sample q:s.samples){double f=(double)q.tMs/duration;if(f<f0||f>f1)continue;JSONObject m=SessionSeries.sensor(q,sensor);double r=m==null?Double.NaN:m.optDouble("ratio",Double.NaN);if(Double.isFinite(r)&&r>0)v.add(r);}if(v.isEmpty())return Double.NaN;Double[] a=v.toArray(new Double[0]);Arrays.sort(a);int n=a.length;return n%2==1?a[n/2]:(a[n/2-1]+a[n/2])/2.0;
    }

    private static void lineChart(Canvas c,RectF box,String title,String xLabel,String yLabel,List<Line> lines){
        frame(c,box,title);if(lines==null||lines.isEmpty()){small(c,"No recorded/validated series",box.centerX(),box.centerY(),Color.DKGRAY,Paint.Align.CENTER);return;}
        RectF p=new RectF(box.left+54,box.top+34,box.right-14,box.bottom-42);double xmin=Double.POSITIVE_INFINITY,xmax=Double.NEGATIVE_INFINITY,ymin=Double.POSITIVE_INFINITY,ymax=Double.NEGATIVE_INFINITY;
        for(Line l:lines)for(int i=0;i<l.x.length;i++)if(Double.isFinite(l.x[i])&&Double.isFinite(l.y[i])){xmin=Math.min(xmin,l.x[i]);xmax=Math.max(xmax,l.x[i]);ymin=Math.min(ymin,l.y[i]);ymax=Math.max(ymax,l.y[i]);}
        if(!Double.isFinite(xmin)||!Double.isFinite(ymin)){empty(c,box,title,"No valid samples");return;}if(xmax<=xmin)xmax=xmin+1;if(ymax<=ymin){double e=Math.max(.01,Math.abs(ymin)*.05);ymin-=e;ymax+=e;}double pad=(ymax-ymin)*.08;ymin-=pad;ymax+=pad;
        Paint g=paint(0xFFE0E0E0,1);for(int i=0;i<=4;i++){float y=p.top+p.height()*i/4f;c.drawLine(p.left,y,p.right,y,g);double v=ymax-(ymax-ymin)*i/4.0;small(c,fmt(v),p.left-5,y+4,Color.DKGRAY,Paint.Align.RIGHT);}for(int i=0;i<=4;i++){float x=p.left+p.width()*i/4f;c.drawLine(x,p.top,x,p.bottom,g);double v=xmin+(xmax-xmin)*i/4.0;small(c,fmt(v),x,p.bottom+15,Color.DKGRAY,Paint.Align.CENTER);}
        for(Line l:lines){Paint q=paint(l.color,2.2f);float px=0,py=0;boolean prev=false;for(int i=0;i<l.x.length;i++){double xv=l.x[i],yv=l.y[i];if(!Double.isFinite(xv)||!Double.isFinite(yv)){prev=false;continue;}float x=(float)(p.left+(xv-xmin)/(xmax-xmin)*p.width()),y=(float)(p.bottom-(yv-ymin)/(ymax-ymin)*p.height());if(prev)c.drawLine(px,py,x,y,q);px=x;py=y;prev=true;}}
        small(c,xLabel,p.centerX(),box.bottom-7,Color.DKGRAY,Paint.Align.CENTER);small(c,yLabel,box.left+5,p.top-4,Color.DKGRAY,Paint.Align.LEFT);float lx=p.left;for(Line l:lines){Paint q=paint(l.color,3);c.drawLine(lx,p.top-10,lx+15,p.top-10,q);small(c,l.name,lx+19,p.top-6,Color.DKGRAY,Paint.Align.LEFT);lx+=68;if(lx>p.right-70){lx=p.left;}}
    }

    private static void barChart(Canvas c,RectF box,String title,String axis,String[] labels,double[] values){
        frame(c,box,title);if(labels==null||labels.length==0){empty(c,box,title,"No data");return;}RectF p=new RectF(box.left+45,box.top+34,box.right-12,box.bottom-46);double max=0;for(double v:values)if(Double.isFinite(v))max=Math.max(max,v);if(max<=0)max=1;int n=labels.length;float gap=Math.max(3,p.width()/(n*6f)),bw=(p.width()-gap*(n+1))/Math.max(1,n);Paint axisPaint=paint(0xFFE0E0E0,1);c.drawLine(p.left,p.bottom,p.right,p.bottom,axisPaint);for(int i=0;i<n;i++){double v=values[i];float h=Double.isFinite(v)?(float)(Math.max(0,v)/max*p.height()):0;float l=p.left+gap+i*(bw+gap);Paint b=paint(PALETTE[i%PALETTE.length],1);b.setStyle(Paint.Style.FILL);c.drawRect(l,p.bottom-h,l+bw,p.bottom,b);small(c,labels[i],l+bw/2,p.bottom+14,Color.DKGRAY,Paint.Align.CENTER);if(Double.isFinite(v))small(c,String.format(Locale.US,"%.0f%%",100*v),l+bw/2,p.bottom-h-4,Color.DKGRAY,Paint.Align.CENTER);}small(c,axis,p.centerX(),box.bottom-7,Color.DKGRAY,Paint.Align.CENTER);
    }

    private static void frame(Canvas c,RectF b,String title){Paint bg=paint(Color.WHITE,1);bg.setStyle(Paint.Style.FILL);c.drawRect(b,bg);Paint border=paint(0xFFCFD8DC,1);c.drawRect(b,border);Paint t=paint(Color.BLACK,1);t.setTextSize(13);t.setFakeBoldText(true);c.drawText(title,b.left+8,b.top+20,t);}
    private static void empty(Canvas c,RectF b,String title,String text){frame(c,b,title);small(c,text,b.centerX(),b.centerY(),Color.DKGRAY,Paint.Align.CENTER);}
    private static void small(Canvas c,String text,float x,float y,int color,Paint.Align align){Paint p=paint(color,1);p.setTextSize(8);p.setTextAlign(align);c.drawText(text==null?"":text,x,y,p);}
    private static Paint paint(int color,float width){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStrokeWidth(width);p.setStyle(Paint.Style.STROKE);return p;}
    private static String fmt(double v){if(!Double.isFinite(v))return"—";double a=Math.abs(v);return a>=100?String.format(Locale.US,"%.0f",v):a>=10?String.format(Locale.US,"%.1f",v):String.format(Locale.US,"%.2f",v);}
    private static String shortName(String s,int n){if(s==null)return"";s=s.trim();return s.length()<=n?s:s.substring(0,n-1)+"…";}
    private static double[] arr(List<Double> x){double[] a=new double[x.size()];for(int i=0;i<a.length;i++)a[i]=x.get(i)==null?Double.NaN:x.get(i);return a;}
    private static int valid(List<Double> x){int n=0;for(Double v:x)if(v!=null&&Double.isFinite(v))n++;return n;}
}
