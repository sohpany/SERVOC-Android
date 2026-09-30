package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cross-session visuals for Site reports. */
public final class SiteReportCharts {
    private SiteReportCharts(){}

    /** 12x12 recurrence map: intensity = sessions with positive candidate evidence. */
    public static void recurrence144(Canvas c,RectF box,SiteReportIntelligence.SitePlan plan){
        frame(c,box,"144-material recurrence across site sessions");if(plan==null||plan.totalSessions<=0){empty(c,box,"No site sessions");return;}
        Map<Integer,SiteReportIntelligence.MaterialRecurrence> byIndex=new HashMap<>();for(SiteReportIntelligence.MaterialRecurrence m:plan.materials)byIndex.put(m.index,m);
        RectF grid=new RectF(box.left+24,box.top+42,box.right-24,box.bottom-58);float cw=grid.width()/12f,ch=grid.height()/12f;Paint border=stroke(0xFFFFFFFF,0.6f);
        for(int i=0;i<144;i++){SiteReportIntelligence.MaterialRecurrence m=byIndex.get(i);int n=m==null?0:m.supportedSessions;float ratio=plan.totalSessions>0?n/(float)plan.totalSessions:0;int color=recurrenceColor(ratio,m!=null&&m.acceptedSessions>0);int row=i/12,col=i%12;float l=grid.left+col*cw,t=grid.top+row*ch;c.drawRect(l,t,l+cw,t+ch,fill(color));c.drawRect(l,t,l+cw,t+ch,border);if(cw>=18&&ch>=14)small(c,String.valueOf(i+1),l+cw/2,t+ch*.66f,contrast(color),Paint.Align.CENTER,6.5f,false);}
        float y=box.bottom-38;legend(c,box.left+25,y,0xFFE0E0E0,"No supported session");legend(c,box.left+185,y,0xFF90CAF9,"Low recurrence");legend(c,box.left+315,y,0xFF42A5F5,"Repeated");legend(c,box.left+420,y,0xFF1565C0,"High recurrence");legend(c,box.left+555,y,0xFF1B5E20,"Accepted in ≥1 session");
        small(c,"Intensity = fraction of site sessions with positive candidate evidence; green marks at least one accepted identity.",box.centerX(),box.bottom-12,Color.DKGRAY,Paint.Align.CENTER,7.2f,false);
    }

    /** Top recurring material evidence, preserving candidate/accepted/quantified counts separately. */
    public static void topRecurrence(Canvas c,RectF box,SiteReportIntelligence.SitePlan plan){
        frame(c,box,"Most recurrent material evidence");List<SiteReportIntelligence.MaterialRecurrence> rows=plan==null?null:plan.topRecurring;if(rows==null||rows.isEmpty()){empty(c,box,"No positive recurring material evidence");return;}int n=Math.min(10,rows.size());
        float left=box.left+165,top=box.top+47,rowH=(box.height()-78)/n,max=Math.max(1,plan.totalSessions);
        for(int i=0;i<n;i++){SiteReportIntelligence.MaterialRecurrence m=rows.get(i);float y=top+i*rowH,h=rowH*.18f;small(c,(i+1)+". "+shorten(m.name,22),left-8,y+rowH*.55f,Color.DKGRAY,Paint.Align.RIGHT,7.2f,false);c.drawRect(left,y,left+(m.supportedSessions/(float)max)*(box.right-left-35),y+h,fill(0xFF42A5F5));c.drawRect(left,y+h+2,left+(m.acceptedSessions/(float)max)*(box.right-left-35),y+2*h+2,fill(0xFF2E7D32));c.drawRect(left,y+2*h+4,left+(m.quantifiedSessions/(float)max)*(box.right-left-35),y+3*h+4,fill(0xFF6A1B9A));small(c,"S"+m.supportedSessions+" A"+m.acceptedSessions+" Q"+m.quantifiedSessions+" • max "+pct(m.maxSupportPct)+" • med "+pct(m.medianSupportPct),box.right-12,y+rowH*.58f,Color.DKGRAY,Paint.Align.RIGHT,6.5f,false);}
        small(c,"Blue=session support • Green=accepted identity • Purple=M4 quantitative session",box.centerX(),box.bottom-10,Color.DKGRAY,Paint.Align.CENTER,7f,false);
    }

    public static void sessionOutcomeBars(Canvas c,RectF box,SiteReportIntelligence.SitePlan plan){
        frame(c,box,"Site analytical-state distribution");if(plan==null||plan.totalSessions<=0){empty(c,box,"No site sessions");return;}String[] labels={"Analytical","Identified","Quantified","Open/Mix","Invalid","Critical"};int[] v={plan.analyticalSessions,plan.identifiedSessions,plan.quantifiedSessions,plan.openSetOrMixtureSessions,plan.invalidSessions,plan.criticalSessions};float pLeft=box.left+40,pRight=box.right-20,pTop=box.top+48,pBottom=box.bottom-38;float gap=8,bw=(pRight-pLeft-gap*(v.length+1))/v.length;for(int i=0;i<v.length;i++){float h=(pBottom-pTop)*(v[i]/(float)Math.max(1,plan.totalSessions)),l=pLeft+gap+i*(bw+gap);c.drawRect(l,pBottom-h,l+bw,pBottom,fill(i==5&&v[i]>0?0xFFC62828:0xFF546E7A));small(c,labels[i],l+bw/2,pBottom+14,Color.DKGRAY,Paint.Align.CENTER,6.7f,false);small(c,String.valueOf(v[i]),l+bw/2,pBottom-h-4,Color.DKGRAY,Paint.Align.CENTER,7.2f,true);}small(c,"Counts are independent field sessions; no concentration pooling.",box.centerX(),box.bottom-9,Color.DKGRAY,Paint.Align.CENTER,7f,false);
    }

    private static int recurrenceColor(float ratio,boolean accepted){if(accepted)return 0xFF1B5E20;if(ratio<=0)return 0xFFE0E0E0;if(ratio<.20)return 0xFFBBDEFB;if(ratio<.50)return 0xFF64B5F6;if(ratio<.80)return 0xFF42A5F5;return 0xFF1565C0;}
    private static int contrast(int color){double y=(.299*Color.red(color)+.587*Color.green(color)+.114*Color.blue(color))/255.0;return y>.62?Color.BLACK:Color.WHITE;}
    private static void frame(Canvas c,RectF r,String title){c.drawRect(r,fill(Color.WHITE));c.drawRect(r,stroke(0xFFCFD8DC,1));small(c,title,r.left+9,r.top+20,Color.BLACK,Paint.Align.LEFT,11.5f,true);}
    private static void empty(Canvas c,RectF r,String msg){small(c,msg,r.centerX(),r.centerY(),Color.DKGRAY,Paint.Align.CENTER,9f,false);}
    private static void legend(Canvas c,float x,float y,int color,String label){c.drawRect(x,y-8,x+12,y+4,fill(color));small(c,label,x+16,y+2,Color.DKGRAY,Paint.Align.LEFT,6.5f,false);}
    private static Paint fill(int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.FILL);p.setColor(color);return p;}
    private static Paint stroke(int color,float width){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);return p;}
    private static void small(Canvas c,String s,float x,float y,int color,Paint.Align a,float size,boolean bold){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setTextAlign(a);p.setFakeBoldText(bold);c.drawText(s==null?"":s,x,y,p);}
    private static String shorten(String s,int n){String q=s==null?"":s;return q.length()<=n?q:q.substring(0,Math.max(1,n-1))+"…";}
    private static String pct(double v){return Double.isFinite(v)?String.format(Locale.US,"%.0f%%",v):"—";}
}
