package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Locale;

/** Landscape-only driving overlay for the forward ultrasonic distance. */
public final class EchoDistanceBadge extends TextView implements Runnable {
    private static final long REFRESH_MS=250L;
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
        // Raw API4 fallback protects the overlay if an intermediate normalizer omits/renames the field.
        if((Double.isNaN(cm)||Double.isInfinite(cm)||cm<=0)&&d!=null){
            JSONObject raw=d.optJSONObject("_data_api");
            JSONObject rawDevice=raw==null?null:raw.optJSONObject("device");
            JSONObject rawProximity=rawDevice==null?null:rawDevice.optJSONObject("proximity");
            if(rawProximity!=null)cm=rawProximity.optDouble("cm",rawProximity.optDouble("distance_cm",Double.NaN));
        }
        if(Double.isNaN(cm)||Double.isInfinite(cm)||cm<=0){
            setText("ECHO — cm");
            setTextColor(0xFF90A4AE);
            return;
        }
        setText(String.format(Locale.US,"ECHO %.1f cm",cm));
        setTextColor(cm<=WARNING_CM?0xFFFF5252:0xFF66BB6A);
    }
}
