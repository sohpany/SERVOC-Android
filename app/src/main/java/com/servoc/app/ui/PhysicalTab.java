package com.servoc.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.measurements.PhysicalCatalog;
import org.json.JSONObject;
import java.util.Locale;

public final class PhysicalTab {
    private final HazRobHost host;
    private final ScrollView scroll;
    private final TextView[] values=new TextView[PhysicalCatalog.COUNT];
    private final TextView[] states=new TextView[PhysicalCatalog.COUNT];

    public PhysicalTab(Context c,HazRobHost host){
        this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);root.addView(UiKit.title(c,"القياسات الفيزيائية والبيئية"),UiKit.full(c));root.addView(UiKit.small(c,"تبقى القياسات الخمسة عشر ظاهرة. تُعرض قيمة فقط عند وجود مصدر عتادي أو قيمة مشتقة موثقة."),UiKit.full(c));LinearLayout head=UiKit.hbox(c);head.setBackgroundColor(0xFF263238);cell(head,"القياس",2f);cell(head,"القيمة",1.2f);cell(head,"المصدر / الحالة",1.5f);root.addView(head,UiKit.full(c));for(int i=0;i<PhysicalCatalog.COUNT;i++){LinearLayout row=UiKit.hbox(c);row.setBackgroundColor((i&1)==0?0xFF151B22:0xFF1B222A);cell(row,String.format(Locale.US,"%02d  %s",i+1,PhysicalCatalog.NAMES_AR[i]),2f);values[i]=cell(row,"— "+PhysicalCatalog.UNITS[i],1.2f);states[i]=cell(row,"غير مركب",1.5f);states[i].setTextColor(0xFF78909C);root.addView(row,UiKit.full(c));}}
    private TextView cell(LinearLayout r,String s,float w){TextView t=UiKit.text(r.getContext(),s);t.setTextSize(12);r.addView(t,new LinearLayout.LayoutParams(0,-2,w));return t;}public View view(){return scroll;}
    public void refresh(){for(int i=0;i<values.length;i++){values[i].setText("— "+PhysicalCatalog.UNITS[i]);states[i].setText("غير مركب");states[i].setTextColor(0xFF78909C);}JSONObject d=host.latestDiagnostics();if(d==null){for(int i=0;i<3;i++)states[i].setText("لا يوجد اتصال");return;}JSONObject e=d.optJSONObject("env");if(e==null)e=d.optJSONObject("bme");if(e==null||!e.optBoolean("ok",false)){for(int i=0;i<3;i++){states[i].setText("الحساس البيئي غير مكتشف");states[i].setTextColor(0xFFFFB74D);}return;}String type=e.optString("type",e.optString("sensor","BME280"));set(0,e.optDouble("temperature",Double.NaN),type);double h=e.optDouble("humidity",Double.NaN);if(Double.isFinite(h))set(1,h,type);else{states[1].setText("الرطوبة غير متاحة في "+type);states[1].setTextColor(0xFFFFB74D);}set(2,e.optDouble("pressure",Double.NaN),type);double p=e.optDouble("pressure",Double.NaN);if(Double.isFinite(p)&&p>0){double alt=44330.0*(1.0-Math.pow(p/1013.25,0.1903));set(14,alt,"مشتق من الضغط عند مرجع 1013.25 hPa");}}
    private void set(int i,double v,String source){if(!Double.isFinite(v))return;values[i].setText(String.format(Locale.US,"%.2f %s",v,PhysicalCatalog.UNITS[i]));states[i].setText(source);states[i].setTextColor(0xFF81C784);}
}
