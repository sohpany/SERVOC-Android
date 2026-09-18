package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.servoc.app.calibration.SqliteCalibrationProvider;
import com.servoc.app.chemcore.ChemCoreFeaturePipeline;
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
        String[] labels=new String[HazRobHardwareProfile.MQ.length];
        double[] response=new double[labels.length];
        if(s==null||s.samples.isEmpty()){barChart(c,box,"Sensor fingerprint","canonical |signed delta|",labels,response);return;}
        double[] time=new double[s.samples.size()];
        double[][] ratios=new double[s.samples.size()][HazRobHardwareProfile.MQ.length];
        for(int n=0;n<s.samples.size();n++){
            SessionSeries.Sample sample=s.samples.get(n);time[n]=sample.tMs/1000.0;
            for(int i=0;i<labels.length;i++){
                HazRobHardwareProfile.Channel hw=HazRobHardwareProfile.MQ[i];labels[i]=hw.key.toUpperCase(Locale.US);
                JSONObject m=SessionSeries.sensor(sample,hw.key);
                ratios[n][i]=m==null?Double.NaN:m.optDouble("ratio",Double.NaN);
            }
        }
        ChemCoreFeaturePipeline.Result canonical=ChemCoreFeaturePipeline.extract(time,ratios);
        double max=0;
        for(int i=0;i<labels.length;i++){double v=canonical.feature(i,4);response[i]=v;if(Double.isFinite(v))max=Math.max(max,v);}
        if(max>0)for(int i=0;i<response.length;i++)if(Double.isFinite(response[i]))response[i]/=max;
        barChart(c,box,"Sensor fingerprint","canonical |signed delta|",labels,response);
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

    /** Donut composition of the persisted 144-panel screening states. */
    public static void screeningDonut(Canvas c,RectF box,JSONObject chemcore){JSONObject p=chemcore==null?null:chemcore.optJSONObject("screening_144");JSONArray rows=p==null?null:p.optJSONArray("rows");String[] st={"SUPPORTED","WEAK_MATCH","REJECTED","OUT_OF_DOMAIN","INSUFFICIENT_EVIDENCE","UNSUPPORTED_BY_CURRENT_ARRAY"};int[] n=new int[st.length];int total=0;if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject q=rows.optJSONObject(i);if(q==null)continue;String s=q.optString("status","");for(int k=0;k<st.length;k++)if(st[k].equals(s)){n[k]++;total++;break;}}if(total==0){empty(c,box,"144 screening composition","No evaluated panel");return;}frame(c,box,"144 screening composition");float size=Math.min(box.width()*.52f,box.height()*.68f),cx=box.left+box.width()*.34f,cy=box.centerY()+8;RectF oval=new RectF(cx-size/2,cy-size/2,cx+size/2,cy+size/2);float start=-90;for(int i=0;i<n.length;i++){if(n[i]==0)continue;float sweep=360f*n[i]/total;Paint pnt=paint(PALETTE[i%PALETTE.length],1);pnt.setStyle(Paint.Style.FILL);c.drawArc(oval,start,sweep,true,pnt);start+=sweep;}Paint hole=paint(Color.WHITE,1);hole.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,size*.25f,hole);small(c,String.valueOf(total),cx,cy+4,Color.DKGRAY,Paint.Align.CENTER);String[] lab={"Supported","Weak","Rejected","OOD","Insufficient","Unsupported"};for(int i=0;i<lab.length;i++){float y=box.top+52+i*27,x=box.left+box.width()*.66f;Paint pnt=paint(PALETTE[i%PALETTE.length],1);pnt.setStyle(Paint.Style.FILL);c.drawCircle(x,y-4,6,pnt);small(c,lab[i]+"  "+n[i],x+12,y,Color.DKGRAY,Paint.Align.LEFT);}}

    /** Scatter relationship: relative candidate posterior vs joint-array residual RMS. */
    public static void candidateScatter(Canvas c,RectF box,JSONObject chemcore){JSONArray a=chemcore==null?null:chemcore.optJSONArray("candidates");if(a==null||a.length()==0){empty(c,box,"Candidate fit map","No ChemCore candidates");return;}frame(c,box,"Candidate fit map — posterior vs residual");RectF p=new RectF(box.left+58,box.top+40,box.right-22,box.bottom-48);double maxR=0;for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q!=null)maxR=Math.max(maxR,q.optDouble("residual_rms",0));}if(maxR<=0)maxR=1;Paint ax=paint(0xFF90A4AE,1);c.drawLine(p.left,p.bottom,p.right,p.bottom,ax);c.drawLine(p.left,p.top,p.left,p.bottom,ax);for(int i=0;i<a.length();i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;double post=q.optDouble("posterior",Double.NaN),res=q.optDouble("residual_rms",Double.NaN);if(!Double.isFinite(post)||!Double.isFinite(res))continue;float x=p.left+(float)Math.max(0,Math.min(1,post))*p.width(),y=p.bottom-(float)Math.max(0,Math.min(1,res/maxR))*p.height();Paint dot=paint(PALETTE[i%PALETTE.length],1);dot.setStyle(Paint.Style.FILL);c.drawCircle(x,y,i<5?8:5,dot);if(i<5)small(c,"#"+(i+1),x+9,y-5,Color.DKGRAY,Paint.Align.LEFT);}small(c,"relative posterior →",p.centerX(),box.bottom-8,Color.DKGRAY,Paint.Align.CENTER);small(c,"residual ↑",box.left+6,p.top+8,Color.DKGRAY,Paint.Align.LEFT);}

    /** Radar/spider view of the canonical 8-channel chemical fingerprint. */
    public static void fingerprintRadar(Canvas c,RectF box,SessionSeries.Series s){if(s==null||!s.usable()){empty(c,box,"Chemical fingerprint radar","No usable time-series");return;}double[] v=fingerprintValues(s);if(v==null){empty(c,box,"Chemical fingerprint radar","Insufficient canonical MQ features");return;}frame(c,box,"Chemical fingerprint radar");float cx=box.centerX(),cy=box.centerY()+8,r=Math.min(box.width(),box.height())*.34f;Paint grid=paint(0xFFB0BEC5,1),line=paint(PALETTE[0],3),fill=paint(PALETTE[0],1);fill.setStyle(Paint.Style.FILL);fill.setAlpha(45);Path area=new Path();for(int ring=1;ring<=4;ring++){Path gp=new Path();for(int i=0;i<8;i++){double a=-Math.PI/2+i*Math.PI/4;float x=cx+(float)Math.cos(a)*r*ring/4,y=cy+(float)Math.sin(a)*r*ring/4;if(i==0)gp.moveTo(x,y);else gp.lineTo(x,y);}gp.close();c.drawPath(gp,grid);}for(int i=0;i<8;i++){double a=-Math.PI/2+i*Math.PI/4;float ex=cx+(float)Math.cos(a)*r,ey=cy+(float)Math.sin(a)*r;c.drawLine(cx,cy,ex,ey,grid);small(c,com.servoc.app.hardware.HazRobHardwareProfile.MQ[i].key,cx+(float)Math.cos(a)*(r+18),cy+(float)Math.sin(a)*(r+18),Color.DKGRAY,Paint.Align.CENTER);float x=cx+(float)Math.cos(a)*r*(float)v[i],y=cy+(float)Math.sin(a)*r*(float)v[i];if(i==0)area.moveTo(x,y);else area.lineTo(x,y);}area.close();c.drawPath(area,fill);c.drawPath(area,line);}

    private static double[] fingerprintValues(SessionSeries.Series s){try{java.util.List<com.servoc.app.chemcore.ChemCoreFeaturePipeline.Sample> samples=new java.util.ArrayList<>();for(SessionSeries.Sample row:s.samples){double[] ratios=new double[8];for(int i=0;i<8;i++){JSONObject q=SessionSeries.sensor(row,com.servoc.app.hardware.HazRobHardwareProfile.MQ[i].key);ratios[i]=q==null?Double.NaN:q.optDouble("ratio",Double.NaN);}samples.add(new com.servoc.app.chemcore.ChemCoreFeaturePipeline.Sample(row.tMs,ratios));}com.servoc.app.chemcore.ChemCoreFeaturePipeline.Result r=com.servoc.app.chemcore.ChemCoreFeaturePipeline.extract(samples);double[] out=new double[8];double max=0;for(int i=0;i<8;i++){double x=Math.abs(r.features[i][4]);out[i]=Double.isFinite(x)?x:0;max=Math.max(max,out[i]);}if(max<=0)return null;for(int i=0;i<8;i++)out[i]/=max;return out;}catch(Exception e){return null;}}

    /** ChemCore candidate residual RMS; lower is a better joint-array fit. */
    public static void candidateResiduals(Canvas c,RectF box,JSONObject chemcore){JSONArray a=chemcore==null?null:chemcore.optJSONArray("candidates");if(a==null||a.length()==0){empty(c,box,"Candidate residual RMS","No ChemCore candidates");return;}int n=Math.min(8,a.length());String[] labels=new String[n];double[] v=new double[n];for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);labels[i]=shortName(q==null?"—":q.optString("label","#"+(i+1)),16);v[i]=q==null?Double.NaN:q.optDouble("residual_rms",Double.NaN);}barRawChart(c,box,"Candidate residual RMS","residual",labels,v);}

    /** Evidence-tier distribution across the persisted 144-panel result. */
    public static void evidenceTiers(Canvas c,RectF box,JSONObject chemcore){JSONObject p=chemcore==null?null:chemcore.optJSONObject("screening_144");JSONArray rows=p==null?null:p.optJSONArray("rows");String[] labels={"M0","M1","M2","M3","M4"};double[] v=new double[5];if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject q=rows.optJSONObject(i);if(q==null)continue;String e=q.optString("evidence_level","");for(int k=0;k<5;k++)if(e.contains("M"+k)){v[k]++;break;}}barCountChart(c,box,"Evidence-tier distribution","chemicals",labels,v);}

    private static void barRawChart(Canvas c,RectF box,String title,String axis,String[] labels,double[] values){frame(c,box,title);RectF p=new RectF(box.left+45,box.top+34,box.right-12,box.bottom-46);double max=0;for(double v:values)if(Double.isFinite(v))max=Math.max(max,v);if(max<=0)max=1;int n=labels.length;float gap=Math.max(3,p.width()/(Math.max(1,n)*6f)),bw=(p.width()-gap*(n+1))/Math.max(1,n);for(int i=0;i<n;i++){double v=values[i];float h=Double.isFinite(v)?(float)(Math.max(0,v)/max*p.height()):0,l=p.left+gap+i*(bw+gap);Paint b=paint(PALETTE[i%PALETTE.length],1);b.setStyle(Paint.Style.FILL);c.drawRect(l,p.bottom-h,l+bw,p.bottom,b);small(c,labels[i],l+bw/2,p.bottom+14,Color.DKGRAY,Paint.Align.CENTER);small(c,fmt(v),l+bw/2,p.bottom-h-4,Color.DKGRAY,Paint.Align.CENTER);}small(c,axis,p.centerX(),box.bottom-7,Color.DKGRAY,Paint.Align.CENTER);}

    /** Distribution of the authoritative 144-panel screening states. */
    public static void screening144(Canvas c,RectF box,JSONObject chemcore){
        JSONObject panel=chemcore==null?null:chemcore.optJSONObject("screening_144");JSONArray rows=panel==null?null:panel.optJSONArray("rows");
        String[] labels={"Supported","Weak","Rejected","OOD","Insufficient","Unsupported"};String[] states={"SUPPORTED","WEAK_MATCH","REJECTED","OUT_OF_DOMAIN","INSUFFICIENT_EVIDENCE","UNSUPPORTED_BY_CURRENT_ARRAY"};double[] v=new double[states.length];
        if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;String s=r.optString("status","");for(int k=0;k<states.length;k++)if(states[k].equals(s)){v[k]++;break;}}
        barCountChart(c,box,"144-panel screening status","chemicals",labels,v);
    }

    /** Toxicity and flammability remain separate scientific dimensions. */
    public static void exposureRatios(Canvas c,RectF box,JSONObject chemcore){
        JSONObject g=chemcore==null?null:chemcore.optJSONObject("gas_exposure_ppm");String[] labels={"Toxic ref","%LFL"};double[] v={Double.NaN,Double.NaN};
        if(g!=null&&g.optBoolean("input_gate_passed",false)){v[0]=g.optDouble("toxicity_priority_percent",Double.NaN);v[1]=g.optDouble("percent_lfl",Double.NaN);}
        barPercentChart(c,box,"Exposure / flammability reference ratios","% of reference",labels,v);
    }

    private static void barCountChart(Canvas c,RectF box,String title,String axis,String[] labels,double[] values){
        frame(c,box,title);RectF p=new RectF(box.left+45,box.top+34,box.right-12,box.bottom-46);double max=1;for(double v:values)if(Double.isFinite(v))max=Math.max(max,v);int n=labels.length;float gap=Math.max(3,p.width()/(n*6f)),bw=(p.width()-gap*(n+1))/Math.max(1,n);for(int i=0;i<n;i++){double v=values[i];float h=Double.isFinite(v)?(float)(Math.max(0,v)/max*p.height()):0,l=p.left+gap+i*(bw+gap);Paint b=paint(PALETTE[i%PALETTE.length],1);b.setStyle(Paint.Style.FILL);c.drawRect(l,p.bottom-h,l+bw,p.bottom,b);small(c,labels[i],l+bw/2,p.bottom+14,Color.DKGRAY,Paint.Align.CENTER);small(c,String.valueOf((int)v),l+bw/2,p.bottom-h-4,Color.DKGRAY,Paint.Align.CENTER);}small(c,axis,p.centerX(),box.bottom-7,Color.DKGRAY,Paint.Align.CENTER);
    }

    private static void barPercentChart(Canvas c,RectF box,String title,String axis,String[] labels,double[] values){
        frame(c,box,title);RectF p=new RectF(box.left+45,box.top+34,box.right-12,box.bottom-46);double max=100;for(double v:values)if(Double.isFinite(v))max=Math.max(max,v);int n=labels.length;float gap=24,bw=(p.width()-gap*(n+1))/Math.max(1,n);for(int i=0;i<n;i++){double v=values[i];float h=Double.isFinite(v)?(float)(Math.max(0,v)/max*p.height()):0,l=p.left+gap+i*(bw+gap);Paint b=paint(PALETTE[i%PALETTE.length],1);b.setStyle(Paint.Style.FILL);c.drawRect(l,p.bottom-h,l+bw,p.bottom,b);small(c,labels[i],l+bw/2,p.bottom+14,Color.DKGRAY,Paint.Align.CENTER);small(c,Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—",l+bw/2,p.bottom-h-4,Color.DKGRAY,Paint.Align.CENTER);}small(c,axis,p.centerX(),box.bottom-7,Color.DKGRAY,Paint.Align.CENTER);
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
