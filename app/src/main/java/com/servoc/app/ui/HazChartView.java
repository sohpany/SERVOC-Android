package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import java.util.Arrays;
import java.util.Locale;

/** Lightweight dependency-free chart used by the operational dashboard. */
public final class HazChartView extends View {
    private static final int BAR=1,LINE=2;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] palette={0xFF42A5F5,0xFF66BB6A,0xFFFFCA28,0xFFAB47BC,0xFF26C6DA,0xFFFF7043,0xFF8D6E63,0xFF78909C};
    private int mode=BAR;
    private String title="",subtitle="",suffix="";
    private String[] labels=new String[0];
    private double[] bars=new double[0];
    private double[][] lines=new double[0][];
    private double fixedMax=Double.NaN;

    public HazChartView(Context c){super(c);setBackgroundColor(0xFF151A20);setMinimumHeight(UiKit.dp(c,190));}

    public void setBars(String title,String subtitle,String[] labels,double[] values,String suffix,double max){
        this.mode=BAR;this.title=s(title);this.subtitle=s(subtitle);this.labels=labels==null?new String[0]:Arrays.copyOf(labels,labels.length);this.bars=values==null?new double[0]:Arrays.copyOf(values,values.length);this.suffix=s(suffix);this.fixedMax=max;invalidate();
    }

    public void setLines(String title,String subtitle,String[] labels,double[][] values,String suffix){
        this.mode=LINE;this.title=s(title);this.subtitle=s(subtitle);this.labels=labels==null?new String[0]:Arrays.copyOf(labels,labels.length);this.lines=copy(values);this.suffix=s(suffix);this.fixedMax=Double.NaN;invalidate();
    }

    public void clear(String title,String message){setBars(title,message,new String[0],new double[0],"",Double.NaN);}

    @Override protected void onMeasure(int wSpec,int hSpec){int w=MeasureSpec.getSize(wSpec);int desired=mode==BAR?UiKit.dp(getContext(),Math.max(190,100+Math.max(1,bars.length)*32)):UiKit.dp(getContext(),245);int h=resolveSize(desired,hSpec);setMeasuredDimension(w,h);}

    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextSize(15*d);p.setColor(Color.WHITE);c.drawText(title,12*d,23*d,p);
        p.setTypeface(android.graphics.Typeface.DEFAULT);p.setTextSize(11*d);p.setColor(0xFF9FB0BE);c.drawText(subtitle,12*d,42*d,p);
        if(mode==BAR)drawBars(c,w,h,d);else drawLines(c,w,h,d);
    }

    private void drawBars(Canvas c,float w,float h,float d){
        if(bars.length==0){p.setTextSize(13*d);p.setColor(0xFF78909C);c.drawText("—",12*d,78*d,p);return;}
        double max=Double.isFinite(fixedMax)&&fixedMax>0?fixedMax:0;for(double v:bars)if(Double.isFinite(v))max=Math.max(max,v);if(!(max>0))max=1;max*=Double.isFinite(fixedMax)?1:1.08;
        float labelW=Math.min(92*d,w*.28f),x0=labelW+12*d,x1=w-14*d,rowH=(h-58*d)/Math.max(1,bars.length);
        for(int i=0;i<bars.length;i++){
            float cy=58*d+rowH*i+rowH*.52f;String label=i<labels.length?s(labels[i]):("#"+(i+1));double v=bars[i];
            p.setTextSize(11*d);p.setColor(0xFFCFD8DC);p.setTextAlign(Paint.Align.LEFT);c.drawText(ellipsize(label,p,labelW-8*d),10*d,cy+4*d,p);
            p.setColor(0xFF263238);c.drawRoundRect(new RectF(x0,cy-8*d,x1,cy+8*d),5*d,5*d,p);
            if(Double.isFinite(v)&&v>=0){float bx=(float)(x0+(x1-x0)*Math.min(1,v/max));p.setColor(palette[i%palette.length]);c.drawRoundRect(new RectF(x0,cy-8*d,bx,cy+8*d),5*d,5*d,p);p.setTextAlign(Paint.Align.RIGHT);p.setTextSize(10*d);p.setColor(Color.WHITE);c.drawText(fmt(v)+suffix,x1,cy-11*d,p);}
        }
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawLines(Canvas c,float w,float h,float d){
        int n=0;for(double[] a:lines)if(a!=null)n=Math.max(n,a.length);if(n<2){p.setTextSize(13*d);p.setColor(0xFF78909C);c.drawText("بانتظار بيانات زمنية كافية...",12*d,78*d,p);return;}
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(double[] a:lines)if(a!=null)for(double v:a)if(Double.isFinite(v)){min=Math.min(min,v);max=Math.max(max,v);}if(!Double.isFinite(min)||!Double.isFinite(max)){return;}if(Math.abs(max-min)<1e-9){min-=1;max+=1;}double pad=(max-min)*.10;min-=pad;max+=pad;
        float left=42*d,right=w-12*d,top=66*d,bottom=h-34*d;
        p.setStrokeWidth(1*d);p.setColor(0xFF37474F);for(int i=0;i<=4;i++){float y=top+(bottom-top)*i/4f;c.drawLine(left,y,right,y,p);double val=max-(max-min)*i/4.0;p.setTextSize(9*d);p.setColor(0xFF90A4AE);p.setTextAlign(Paint.Align.RIGHT);c.drawText(fmt(val)+suffix,left-5*d,y+3*d,p);p.setColor(0xFF37474F);}
        if(min<0&&max>0){float y=(float)(bottom-(0-min)/(max-min)*(bottom-top));p.setColor(0xFF78909C);p.setStrokeWidth(1.4f*d);c.drawLine(left,y,right,y,p);}
        for(int s=0;s<lines.length;s++){double[] a=lines[s];if(a==null||a.length<2)continue;Path path=new Path();boolean started=false;for(int i=0;i<a.length;i++){double v=a[i];if(!Double.isFinite(v)){started=false;continue;}float x=left+(right-left)*i/(n-1f);float y=(float)(bottom-(v-min)/(max-min)*(bottom-top));if(!started){path.moveTo(x,y);started=true;}else path.lineTo(x,y);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setColor(palette[s%palette.length]);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
        float lx=left;float ly=h-13*d;p.setTextAlign(Paint.Align.LEFT);p.setTextSize(9*d);for(int i=0;i<labels.length&&i<lines.length;i++){p.setColor(palette[i%palette.length]);c.drawRect(lx,ly-7*d,lx+8*d,ly+1*d,p);p.setColor(0xFFCFD8DC);String t=s(labels[i]);c.drawText(t,lx+11*d,ly,p);lx+=11*d+p.measureText(t)+10*d;if(lx>w-55*d){break;}}
        p.setTextAlign(Paint.Align.LEFT);
    }

    private static String fmt(double v){double a=Math.abs(v);if(a>=100)return String.format(Locale.US,"%.0f",v);if(a>=10)return String.format(Locale.US,"%.1f",v);return String.format(Locale.US,"%.2f",v);}
    private static String ellipsize(String s,Paint p,float max){if(p.measureText(s)<=max)return s;String e="…";for(int n=s.length()-1;n>0;n--){String x=s.substring(0,n)+e;if(p.measureText(x)<=max)return x;}return e;}
    private static String s(String x){return x==null?"":x;}
    private static double[][] copy(double[][] in){if(in==null)return new double[0][];double[][] o=new double[in.length][];for(int i=0;i<in.length;i++)o[i]=in[i]==null?null:Arrays.copyOf(in[i],in[i].length);return o;}
}
