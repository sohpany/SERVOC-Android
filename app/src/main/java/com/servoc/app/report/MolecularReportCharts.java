package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Molecular/QSPR visuals for SmartReport.
 *
 * The renderer reads only persisted ChemCore evidence_observations. It does not run QSPR,
 * query PubChem, infer descriptors, or create new analytical evidence while reporting.
 */
public final class MolecularReportCharts {
    private MolecularReportCharts(){}

    static final class Row {
        final String label;
        int observations;
        int insideDomain;
        double supportSum;
        int supportN;
        String family="";
        String source="";
        Row(String label){this.label=label==null?"":label;}
        double meanSupport(){return supportN>0?supportSum/supportN:Double.NaN;}
    }

    public static boolean hasQsprEvidence(JSONObject chemcore){return !rows(chemcore,1).isEmpty();}

    /**
     * Shows persisted PubChem/QSPR evidence for the leading candidates.
     * Green segment = QSPR observations accepted inside their recorded applicability domain.
     * Gray segment = recorded QSPR observations not accepted inside domain.
     * Support weight is an evidence-fusion weight, not a probability of chemical presence.
     */
    public static void qsprEvidence(Canvas c,RectF box,JSONObject chemcore){
        frame(c,box,"Molecular / PubChem-QSPR evidence");
        List<Row> rows=rows(chemcore,6);
        if(rows.isEmpty()){
            small(c,"No persisted QSPR evidence for this session",box.centerX(),box.centerY()-5,Color.DKGRAY,Paint.Align.CENTER,8.5f,false);
            small(c,"Report does not synthesize molecular evidence",box.centerX(),box.centerY()+14,Color.DKGRAY,Paint.Align.CENTER,7.2f,false);
            return;
        }
        int max=1;for(Row r:rows)max=Math.max(max,r.observations);
        RectF p=new RectF(box.left+112,box.top+44,box.right-22,box.bottom-44);
        float gh=p.height()/Math.max(1,rows.size());
        for(int i=0;i<rows.size();i++){
            Row r=rows.get(i);float y=p.top+i*gh+gh*.18f,h=Math.max(7,gh*.38f);
            small(c,(i+1)+". "+shorten(r.label,15),p.left-7,y+h*.72f,Color.DKGRAY,Paint.Align.RIGHT,6.8f,false);
            float total=(r.observations/(float)max)*p.width();
            float in=(r.insideDomain/(float)max)*p.width();
            c.drawRoundRect(new RectF(p.left,y,p.left+total,y+h),3,3,fill(0xFFB0BEC5));
            if(in>0)c.drawRoundRect(new RectF(p.left,y,p.left+in,y+h),3,3,fill(0xFF2E7D32));
            String w=Double.isFinite(r.meanSupport())?String.format(Locale.US,"w %.2f",r.meanSupport()):"w —";
            small(c,"n="+r.observations+" • in="+r.insideDomain+" • "+w,p.right,y+h*.72f,Color.DKGRAY,Paint.Align.RIGHT,6.4f,false);
        }
        Row first=rows.get(0);
        String lineage=!first.family.isEmpty()?shorten(first.family,28):"QSPR";
        if(!first.source.isEmpty())lineage+=" • "+shorten(first.source,24);
        small(c,lineage,box.centerX(),box.bottom-25,Color.DKGRAY,Paint.Align.CENTER,6.7f,false);
        small(c,"M1 molecular support only • never unlocks reportable PPM",box.centerX(),box.bottom-10,0xFF8D3B00,Paint.Align.CENTER,7.0f,true);
    }

    private static List<Row> rows(JSONObject chemcore,int limit){
        List<Row> out=new ArrayList<>();JSONArray candidates=chemcore==null?null:chemcore.optJSONArray("candidates");if(candidates==null)return out;
        for(int i=0;i<candidates.length()&&out.size()<limit;i++){
            JSONObject q=candidates.optJSONObject(i);if(q==null)continue;JSONArray ev=q.optJSONArray("evidence_observations");if(ev==null)continue;
            Row r=new Row(q.optString("label","#"+(i+1)));
            for(int j=0;j<ev.length();j++){
                JSONObject e=ev.optJSONObject(j);if(e==null||!isQspr(e))continue;r.observations++;
                if(e.optBoolean("inside_domain",false))r.insideDomain++;
                double w=e.optDouble("support_weight",Double.NaN);if(Double.isFinite(w)){r.supportSum+=w;r.supportN++;}
                if(r.family.isEmpty())r.family=e.optString("family_id","");if(r.source.isEmpty())r.source=e.optString("source_id","");
            }
            if(r.observations>0)out.add(r);
        }
        return out;
    }

    private static boolean isQspr(JSONObject e){String type=e.optString("type","");String family=e.optString("family_id","");return "QSPR".equalsIgnoreCase(type)||family.toUpperCase(Locale.US).contains("PUBCHEM_QSPR");}
    private static void frame(Canvas c,RectF b,String title){c.drawRect(b,fill(Color.WHITE));Paint s=new Paint(Paint.ANTI_ALIAS_FLAG);s.setColor(0xFFCFD8DC);s.setStyle(Paint.Style.STROKE);s.setStrokeWidth(1);c.drawRect(b,s);small(c,title,b.left+10,b.top+21,Color.BLACK,Paint.Align.LEFT,10.5f,true);}
    private static Paint fill(int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);return p;}
    private static void small(Canvas c,String text,float x,float y,int color,Paint.Align align,float size,boolean bold){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setFakeBoldText(bold);c.drawText(text==null?"":text,x,y,p);}
    private static String shorten(String s,int n){String q=s==null?"":s.trim();return q.length()<=n?q:q.substring(0,Math.max(1,n-1))+"…";}
}
