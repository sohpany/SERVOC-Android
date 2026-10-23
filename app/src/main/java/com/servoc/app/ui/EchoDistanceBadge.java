package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.TextView;

import org.json.JSONObject;
import com.servoc.app.measurement.EchoReading;

import java.util.Locale;

/** Landscape-only driving overlay for the forward ultrasonic distance. */
public final class EchoDistanceBadge extends TextView implements Runnable {
    private static final long REFRESH_MS=150L;
    private static final double WARNING_CM=10.0;
    private final HazRobHost host;
    private boolean running=false;

    public EchoDistanceBadge(Context context,HazRobHost host){
        super(context);
        this.host=host;
        setGravity(Gravity.CENTER);
        setTextSize(17);
        setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        setTextColor(0xFF90A4AE);
        setBackgroundColor(0xCC151A20);
        int h=dp(10),v=dp(5);
        setPadding(h,v,h,v);
        setText("ECHO — cm");
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}

    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        running=true;
        removeCallbacks(this);
        post(this);
    }

    @Override protected void onDetachedFromWindow(){
        running=false;
        removeCallbacks(this);
        super.onDetachedFromWindow();
    }

    @Override public void run(){
        render();
        if(running)postDelayed(this,REFRESH_MS);
    }

    private void render(){
        JSONObject d=host.latestDiagnostics();
        JSONObject device=d==null?null:d.optJSONObject("device");
        JSONObject proximity=device==null?null:device.optJSONObject("proximity");
        double cm=proximity==null?Double.NaN:proximity.optDouble("distance_cm",proximity.optDouble("cm",Double.NaN));
        EchoReading reading=EchoReading.from(d,host.preferences());
        long timeouts=0,pulseUs=0;

        // Raw API4 fallback protects the overlay if an intermediate normalizer omits/renames a field.
        if(d!=null){
            JSONObject raw=d.optJSONObject("_data_api");
            JSONObject rawDevice=raw==null?null:raw.optJSONObject("device");
            JSONObject rawProximity=rawDevice==null?null:rawDevice.optJSONObject("proximity");
            if(rawProximity!=null){
                if(Double.isNaN(cm)||Double.isInfinite(cm)||cm<=0)cm=rawProximity.optDouble("cm",rawProximity.optDouble("distance_cm",Double.NaN));
                timeouts=Math.max(0,rawProximity.optLong("timeouts",0));
                pulseUs=Math.max(0,rawProximity.optLong("pulse_us",0));
            }
        }

        if(!reading.valid){cm=Double.NaN;}
        else cm=reading.displayCm;
        if(Double.isNaN(cm)||Double.isInfinite(cm)||cm<=0){
            if(timeouts>0){
                setText("ECHO TIMEOUT ×"+timeouts);
                setTextColor(0xFFFFB74D);
            }else if(pulseUs>0){
                setText("ECHO pulse "+pulseUs+" µs");
                setTextColor(0xFFFFB74D);
            }else{
                setText("ECHO — cm");
                setTextColor(0xFF90A4AE);
            }
            return;
        }
        setText(String.format(Locale.US,"ECHO %.1f cm",cm));
        setTextColor(cm<=WARNING_CM?0xFFFF5252:0xFF66BB6A);
    }
}
