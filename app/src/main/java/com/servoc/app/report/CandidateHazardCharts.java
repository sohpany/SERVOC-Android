package com.servoc.app.report;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Report visuals for candidate-level toxicity/flammability context. */
public final class CandidateHazardCharts {
    private CandidateHazardCharts(){}

    public static void table(Canvas c,RectF box,JSONObject context){
        frame(c,box,"Candidate hazard context");JSONArray rows=context==null?null:context.optJSONArray("candidates");if(rows==null||rows.length()==0){empty(c,box,"No candidate hazard context");return;}
        int n=Math.min(6,rows.length());float y=box.top+45,rowH=(box.height()-76)/n;
        small(c,"Candidate",box.left+8,box.top+33,Color.DKGRAY,Paint.Align.LEFT,6.6f,true);small(c,"Basis / ppm",box.left+145,box.top+33,Color.DKGRAY,Paint.Align.LEFT,6.6f,true);small(c,"Toxicity",box.left+245,box.top+33,Color.DKGRAY,Paint.Align.LEFT,6.6f,true);small(c,"Flammability",box.left+330,box.top+33,Color.DKGRAY,Paint.Align.LEFT,6.6f,true);
        for(int i=0;i<n;i++){
            JSONObject r=rows.optJSONObject(i);if(r==null)continue;int bg=(i&1)==0?0xFFF7F8FA:Color.WHITE;c.drawRect(new RectF(box.left+4,y-12,box.right-4,y+rowH-3),fill(bg));
            String label=(i+1)+". "+r.optString("label","");String meta=meta(r);small(c,shorten(label,22),box.left+8,y,Color.BLACK,Paint.Align.LEFT,7.2f,true);small(c,shorten(meta,27),box.left+8,y+11,Color.DKGRAY,Paint.Align.LEFT,5.7f,false);
            double ppm=r.optDouble("concentration_ppm",Double.NaN);String basis="M4_MEASURED_REPORTABLE".equals(r.optString("concentration_basis",""))?"M4":"EST";small(c,basis+" • "+(Double.isFinite(ppm)?fmt(ppm)+" ppm":"—"),box.left+145,y+4,basis.equals("M4")?0xFF1B5E20:0xFFEF6C00,Paint.Align.LEFT,6.9f,true);
            JSONObject toxic=r.optJSONObject("toxicity"),flame=r.optJSONObject("flammability");double tr=toxic==null?Double.NaN:toxic.optDouble("max_context_percent",Double.NaN),lfl=flame==null?Double.NaN:flame.optDouble("percent_lfl_context",Double.NaN);JSONObject trRef=toxic==null?null:toxic.optJSONObject("controlling_reference");
            String tox=toxic!=null&&toxic.optBoolean("reference_available",false)?"☠ "+(Double.isFinite(tr)?fmt1(tr)+"% "+refShort(trRef):"ref available"):"—";String fla=flame!=null&&flame.optBoolean("reference_available",false)?"🔥 "+(Double.isFinite(lfl)?fmt1(lfl)+"% LFL":"LFL available"):"—";
            small(c,shorten(tox,18),box.left+245,y+4,hazardColor(tr),Paint.Align.LEFT,6.8f,true);small(c,shorten(fla,18),box.left+330,y+4,hazardColor(lfl),Paint.Align.LEFT,6.8f,true);y+=rowH;
        }
        small(c,"EST = screening-fit ppm: hazard context only, not an occupational exposure result or alarm.",box.centerX(),box.bottom-10,Color.DKGRAY,Paint.Align.CENTER,6.5f,false);
    }

    public static void executiveStrip(Canvas c,RectF box,JSONObject context){
        JSONArray rows=context==null?null:context.optJSONArray("candidates");frame(c,box,"Top candidate hazard profile");if(rows==null||rows.length()==0){empty(c,box,"No candidate context");return;}JSONObject r=rows.optJSONObject(0);if(r==null){empty(c,box,"No candidate context");return;}
        String symbols=r.optString("hazard_symbols","");String basis="M4_MEASURED_REPORTABLE".equals(r.optString("concentration_basis",""))?"M4 measured":"screening estimate";double ppm=r.optDouble("concentration_ppm",Double.NaN);JSONObject t=r.optJSONObject("toxicity"),f=r.optJSONObject("flammability");
        small(c,shorten(r.optString("label",""),28)+"  "+symbols,box.left+12,box.top+42,Color.BLACK,Paint.Align.LEFT,10.5f,true);small(c,shorten(meta(r),45),box.left+250,box.top+42,Color.DKGRAY,Paint.Align.LEFT,6.5f,false);small(c,basis+" • "+(Double.isFinite(ppm)?fmt(ppm)+" ppm":"ppm unavailable"),box.left+12,box.top+67,"M4 measured".equals(basis)?0xFF1B5E20:0xFFEF6C00,Paint.Align.LEFT,7.2f,true);
        double tox=t==null?Double.NaN:t.optDouble("max_context_percent",Double.NaN),lfl=f==null?Double.NaN:f.optDouble("percent_lfl_context",Double.NaN);JSONObject cr=t==null?null:t.optJSONObject("controlling_reference");small(c,"Toxicity: "+(t!=null&&t.optBoolean("reference_available",false)?(Double.isFinite(tox)?fmt1(tox)+"% of "+refShort(cr):"reference available"):"no comparable ref"),box.left+250,box.top+67,hazardColor(tox),Paint.Align.LEFT,7f,false);small(c,"Flammability: "+(f!=null&&f.optBoolean("reference_available",false)?(Double.isFinite(lfl)?fmt1(lfl)+"% LFL":"LFL available"):"no LFL ref"),box.left+500,box.top+67,hazardColor(lfl),Paint.Align.LEFT,7f,false);
    }

    private static String refShort(JSONObject r){if(r==null)return"ref";String a=r.optString("authority",""),t=r.optString("limit_type","");String s=(a+" "+t).trim();return s.isEmpty()?"ref":s;}
    private static String meta(JSONObject r){String f=r.optString("formula",""),cas=r.optString("cas",""),cl=r.optString("chemical_class","");StringBuilder s=new StringBuilder();if(!f.isEmpty())s.append(f);if(!cl.isEmpty()){if(s.length()>0)s.append(" • ");s.append(cl);}if(!cas.isEmpty()){if(s.length()>0)s.append(" • ");s.append("CAS ").append(cas);}return s.toString();}
    private static int hazardColor(double p){if(!Double.isFinite(p))return Color.DKGRAY;if(p>=100)return 0xFFC62828;if(p>=50)return 0xFFD84315;if(p>=20)return 0xFFEF6C00;if(p>=10)return 0xFFF9A825;return 0xFF2E7D32;}
    private static void frame(Canvas c,RectF r,String title){c.drawRect(r,fill(Color.WHITE));Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xFFCFD8DC);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawRect(r,p);small(c,title,r.left+9,r.top+20,Color.BLACK,Paint.Align.LEFT,11.2f,true);}
    private static void empty(Canvas c,RectF r,String s){small(c,s,r.centerX(),r.centerY(),Color.DKGRAY,Paint.Align.CENTER,8.5f,false);}
    private static Paint fill(int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);return p;}
    private static void small(Canvas c,String s,float x,float y,int color,Paint.Align a,float size,boolean bold){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setTextAlign(a);p.setFakeBoldText(bold);c.drawText(s==null?"":s,x,y,p);}
    private static String shorten(String s,int n){String q=s==null?"":s;return q.length()<=n?q:q.substring(0,Math.max(1,n-1))+"…";}
    private static String fmt(double v){return String.format(Locale.US,"%.3f",v);}
    private static String fmt1(double v){return String.format(Locale.US,"%.1f",v);}
}
