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
    private final Context c;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final TextView[] values=new TextView[PhysicalCatalog.COUNT];
    private final TextView[] states=new TextView[PhysicalCatalog.COUNT];

    public PhysicalTab(Context c,HazRobHost host){
        this.c=c;this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"PHYSICAL / ENVIRONMENTAL MEASUREMENTS"),UiKit.full(c));
        root.addView(UiKit.small(c,"All 15 catalogue measurements remain visible. A value is shown only when a hardware source exists in the current ESP32 telemetry."),UiKit.full(c));

        LinearLayout head=UiKit.hbox(c);head.setBackgroundColor(0xFF263238);cell(head,"Measurement",2f);cell(head,"Value",1.2f);cell(head,"Source / status",1.5f);root.addView(head,UiKit.full(c));
        for(int i=0;i<PhysicalCatalog.COUNT;i++){
            LinearLayout row=UiKit.hbox(c);row.setBackgroundColor((i&1)==0?0xFF151B22:0xFF1B222A);
            cell(row,String.format(Locale.US,"%02d  %s",i+1,PhysicalCatalog.NAMES[i]),2f);
            values[i]=cell(row,"— "+PhysicalCatalog.UNITS[i],1.2f);
            states[i]=cell(row,"NOT INSTALLED",1.5f);states[i].setTextColor(0xFF78909C);
            root.addView(row,UiKit.full(c));
        }
    }

    private TextView cell(LinearLayout r,String s,float w){TextView t=UiKit.text(c,s);t.setTextSize(12);r.addView(t,new LinearLayout.LayoutParams(0,-2,w));return t;}
    public View view(){return scroll;}

    public void refresh(){
        for(int i=0;i<values.length;i++){values[i].setText("— "+PhysicalCatalog.UNITS[i]);states[i].setText("NOT INSTALLED");states[i].setTextColor(0xFF78909C);}
        JSONObject d=host.latestDiagnostics();if(d==null)return;
        JSONObject b=d.optJSONObject("bme");if(b==null||!b.optBoolean("ok",false))return;
        set(0,b.optDouble("temperature",Double.NaN),"BME280");
        set(1,b.optDouble("humidity",Double.NaN),"BME280");
        set(2,b.optDouble("pressure",Double.NaN),"BME280");
        double p=b.optDouble("pressure",Double.NaN);
        if(Double.isFinite(p)&&p>0){double alt=44330.0*(1.0-Math.pow(p/1013.25,0.1903));set(14,alt,"DERIVED from BME280 pressure @ 1013.25 hPa reference");}
    }

    private void set(int i,double v,String source){if(!Double.isFinite(v))return;values[i].setText(String.format(Locale.US,"%.2f %s",v,PhysicalCatalog.UNITS[i]));states[i].setText(source);states[i].setTextColor(0xFF81C784);}
}
