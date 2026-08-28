package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.sensors.RobotTelemetry;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class SensorsTab {
    private final Context c;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout content;
    private final TextView cycle;
    private final TextView bme;
    private final TextView[] mqRows=new TextView[RobotTelemetry.MQ_KEYS.length];

    public SensorsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        content=UiKit.vbox(c);content.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));content.setBackgroundColor(0xFF0E1116);
        scroll.addView(content);

        content.addView(UiKit.title(c,"SENSORS / MEASUREMENT"),UiKit.full(c));
        cycle=UiKit.text(c,"Measurement: idle");cycle.setBackgroundColor(0xFF151A20);content.addView(cycle,UiKit.full(c));

        LinearLayout run=UiKit.hbox(c);
        Button start=UiKit.button(c,"START");start.setOnClickListener(v->measurement(true));run.addView(start,UiKit.weight(1));
        Button cancel=UiKit.button(c,"CANCEL");cancel.setOnClickListener(v->measurement(false));run.addView(cancel,UiKit.weight(1));
        Button accept=UiKit.button(c,"ACCEPT");accept.setOnClickListener(v->accept());run.addView(accept,UiKit.weight(1));
        Button repeat=UiKit.button(c,"REPEAT");repeat.setOnClickListener(v->measurement(true));run.addView(repeat,UiKit.weight(1));
        content.addView(run,UiKit.full(c));

        content.addView(UiKit.title(c,"BME280"),UiKit.full(c));
        bme=UiKit.text(c,"Temperature —   Humidity —   Pressure —");bme.setBackgroundColor(0xFF151A20);content.addView(bme,UiKit.full(c));

        content.addView(UiKit.title(c,"MQ CHANNELS"),UiKit.full(c));
        for(int i=0;i<mqRows.length;i++){
            mqRows[i]=UiKit.text(c,RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)+"   not reported");
            mqRows[i].setBackgroundColor((i&1)==0?0xFF161C23:0xFF1B222B);mqRows[i].setTextIsSelectable(true);content.addView(mqRows[i],UiKit.full(c));
        }

        content.addView(UiKit.title(c,"RELAY ROWS"),UiKit.full(c));
        content.addView(UiKit.small(c,"Manual relay operation cancels the automatic measurement state machine in the current ESP32 firmware."),UiKit.full(c));
        LinearLayout rel=UiKit.hbox(c);
        for(int g=1;g<=3;g++){final int x=g;Button b=UiKit.button(c,"ROW "+g);b.setOnClickListener(v->relay(x));rel.addView(b,UiKit.weight(1));}
        Button off=UiKit.button(c,"ALL OFF");off.setOnClickListener(v->relay(0));rel.addView(off,UiKit.weight(1));
        content.addView(rel,UiKit.full(c));

        content.addView(UiKit.small(c,"Raw, ADC voltage, sensor AO, Rs and Rs/R0 are acquisition values. Quantitative PPM remains provisional until R0/reference-gas calibration is validated."),UiKit.full(c));
    }

    public View view(){return scroll;}

    private void measurement(boolean start){
        host.io().execute(()->{
            try{if(start)host.robotApi().startMeasurement();else host.robotApi().cancelMeasurement();
                host.showToast(start?"Measurement cycle started":"Measurement cancelled");}
            catch(Exception e){host.showToast("Measurement command failed");}
        });
    }

    private void relay(int group){
        host.io().execute(()->{try{host.robotApi().relay(group);host.showToast(group==0?"All relays OFF":"Relay row "+group+" ON");}
        catch(Exception e){host.showToast("Relay command failed");}});
    }

    private void accept(){
        JSONObject d=host.latestDiagnostics();
        if(d==null){host.showToast("No live measurement to accept");return;}
        JSONObject m=d.optJSONObject("measurement");
        if(m==null||!"ready".equalsIgnoreCase(m.optString("state"))){host.showToast("Complete the automatic measurement before Accept");return;}
        final String ts=isoNow();final String payload=d.toString();final String analytics= AnalyticsSnapshot.toJson(d);
        host.io().execute(()->{
            try{long id=host.database().save(ts,payload,"ESP32 automatic MQ row cycle",analytics);
                host.showToast("Measurement accepted • #"+id);host.refreshAllTabs();}
            catch(Exception e){host.showToast("Could not save measurement");}
        });
    }

    public void refresh(){
        JSONObject d=host.latestDiagnostics();
        if(d==null){cycle.setText("Measurement: robot offline");bme.setText("Temperature —   Humidity —   Pressure —");for(int i=0;i<mqRows.length;i++)mqRows[i].setText(RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)+"   not reported");return;}
        JSONObject m=d.optJSONObject("measurement");
        if(m!=null){
            String s=m.optString("state","unknown");int g=m.optInt("group",0);double rem=m.optDouble("remaining",Double.NaN);
            String x="Measurement: "+s+(g>0?" • row "+g:"")+(Double.isFinite(rem)&&rem>0?String.format(Locale.US," • %.1f s",rem):"")+
                    " • samples "+m.optInt("samples1",0)+"/"+m.optInt("samples2",0)+"/"+m.optInt("samples3",0);
            cycle.setText(x);cycle.setTextColor("ready".equalsIgnoreCase(s)?0xFF81C784:Color.WHITE);
        }
        JSONObject env=d.optJSONObject("bme");
        if(env==null||!env.optBoolean("ok",false))bme.setText("BME280: N/A");
        else bme.setText(String.format(Locale.US,"Temperature %.1f °C   Humidity %.1f %%RH   Pressure %.1f hPa",env.optDouble("temperature"),env.optDouble("humidity"),env.optDouble("pressure")));

        JSONObject source=d.optJSONObject("mq");
        if(m!=null&&"ready".equalsIgnoreCase(m.optString("state"))&&m.optJSONObject("mq")!=null)source=m.optJSONObject("mq");
        for(int i=0;i<mqRows.length;i++){
            String key=RobotTelemetry.MQ_KEYS[i];JSONObject r=source==null?null:source.optJSONObject(key);
            if(r==null){mqRows[i].setText(key.toUpperCase(Locale.US)+"   not present in current ESP32 acquisition map");mqRows[i].setTextColor(0xFF78909C);continue;}
            StringBuilder s=new StringBuilder(key.toUpperCase(Locale.US));
            if(r.has("status"))s.append("  ").append(r.optString("status"));
            if(r.has("valid")&&!r.optBoolean("valid"))s.append("  invalid");
            if(!r.isNull("raw"))s.append("  raw ").append(r.optInt("raw"));
            add(s,r,"adc","Vadc");add(s,r,"ao","Vao");add(s,r,"rs","Rs");add(s,r,"ratio","Rs/R0");
            mqRows[i].setText(s.toString());mqRows[i].setTextColor(Color.WHITE);
        }
    }

    private static void add(StringBuilder s,JSONObject r,String k,String label){double v=r.optDouble(k,Double.NaN);if(Double.isFinite(v))s.append(String.format(Locale.US,"  %s %.3f",label,v));}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
