package com.servoc.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.servoc.app.data.ExperimentQueries;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/** User-facing saved measurement history. Developer approval/reference-bank controls are intentionally absent. */
public final class ReportsTab {
    private final HazRobHost host;private final ScrollView scroll;private final LinearLayout list;private final TextView summary,details;private final Button pdf,xlsx;private long selectedId=-1;
    public ReportsTab(Context c,HazRobHost host){this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);root.addView(UiKit.title(c,"السجل والتقارير"),UiKit.full(c));summary=UiKit.text(c,"لا توجد قياسات محفوظة");summary.setBackgroundColor(0xFF151A20);root.addView(summary,UiKit.full(c));details=UiKit.text(c,"اختر قياسًا محفوظًا لعرض ملخصه أو تصديره.");details.setBackgroundColor(0xFF11171D);details.setTextIsSelectable(true);root.addView(details,UiKit.full(c));LinearLayout actions=UiKit.hbox(c);pdf=UiKit.button(c,"تصدير PDF");pdf.setOnClickListener(v->export("pdf"));actions.addView(pdf,UiKit.weight(1));xlsx=UiKit.button(c,"تصدير Excel");xlsx.setOnClickListener(v->export("xlsx"));actions.addView(xlsx,UiKit.weight(1));root.addView(actions,UiKit.full(c));list=UiKit.vbox(c);root.addView(list,UiKit.full(c));setEnabled(false);}
    public View view(){return scroll;}
    public void refresh(){list.removeAllViews();List<ExperimentQueries.Record> rows=ExperimentQueries.recent(host.database(),100);summary.setText("القياسات المحفوظة: "+host.database().count()+" • المعروض: "+rows.size());if(rows.isEmpty()){selectedId=-1;setEnabled(false);list.addView(UiKit.small(list.getContext(),"احفظ نتيجة مكتملة من تبويب القياس والتحليل."),UiKit.full(list.getContext()));details.setText("لا توجد قياسات محفوظة.");return;}if(selectedId<0)selectedId=rows.get(0).id;boolean found=false;for(ExperimentQueries.Record r:rows){if(r.id==selectedId)found=true;Button b=UiKit.button(list.getContext(),"#"+r.id+"   "+safe(r.ts)+"   "+safe(r.protocol));b.setOnClickListener(v->select(r.id));list.addView(b,UiKit.full(list.getContext()));}if(!found)selectedId=rows.get(0).id;showSelected();}
    private void select(long id){selectedId=id;showSelected();}
    private void showSelected(){ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null){setEnabled(false);return;}setEnabled(true);JSONObject d=json(r.payload),m=d.optJSONObject("measurement"),e=d.optJSONObject("env"),relay=d.optJSONObject("relay"),a=json(r.analytics);int results=a.optJSONArray("results")==null?0:a.optJSONArray("results").length();boolean mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);long powerMs=relay==null?0:relay.optLong("power_ms",0);StringBuilder s=new StringBuilder("القياس #").append(r.id).append("\nUTC: ").append(safe(r.ts)).append("\nالنوع: ").append(safe(r.protocol)).append("\nMQ Power: ").append(mqOn?"ON":"OFF").append(" • GPIO25 • 8 sensors");if(mqOn)s.append(" • ").append(powerMs).append(" ms");if(m!=null)s.append("\nدورة MQ: ").append(m.optString("state","—"));if(e!=null&&e.optBoolean("ok",false)){s.append("\nالحرارة: ").append(fmt(e.optDouble("temperature",Double.NaN))).append(" °C");double h=e.optDouble("humidity",Double.NaN);if(Double.isFinite(h))s.append(" • الرطوبة: ").append(fmt(h)).append(" %RH");s.append(" • الضغط: ").append(fmt(e.optDouble("pressure",Double.NaN))).append(" hPa");}s.append("\nنتائج الأدلة الكيميائية المسجلة: ").append(results);details.setText(s.toString());}
    private void setEnabled(boolean v){pdf.setEnabled(v);xlsx.setEnabled(v);}private void export(String f){if(selectedId<0){host.showToast("اختر قياسًا أولًا");return;}host.requestExport(f,selectedId);}private static JSONObject json(String s){try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}private static String safe(String s){return s==null?"":s;}private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"—";}
}
