package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

/** Landscape-only operational gas warning overlay. It does not imply calibrated ppm or chemical identity. */
public final class GasHazardBadge extends TextView implements Runnable {
    private static final long REFRESH_MS=250L;
    private final HazRobHost host;
    private boolean running=false;

    public GasHazardBadge(Context context,HazRobHost host){
        super(context);
        this.host=host;
        setGravity(Gravity.CENTER);
        setTextSize(17);
        setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        int h=dp(12),v=dp(6);
        setPadding(h,v,h,v);
        setVisibility(View.GONE);
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
        JSONObject alert=d==null?null:d.optJSONObject("alert");
        int gas=alert==null?0:Math.max(0,Math.min(2,alert.optInt("gas",0)));
        if(gas<=0){
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);
        setTextColor(0xFFFFFFFF);
        if(gas>=2){
            setText("⚠ خطر غاز مرتفع");
            setBackgroundColor(0xE0C62828);
        }else{
            setText("⚠ تحذير غاز");
            setBackgroundColor(0xE0F9A825);
        }
    }
}
