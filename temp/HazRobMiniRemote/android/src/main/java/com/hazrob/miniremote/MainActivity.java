package com.hazrob.miniremote;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String CHANNEL = "0Jo6HgI-45pkxeEeiA3fs1rb";
    private static final String RELAY = "https://hazrob-mini-remote.onrender.com";
    private static final String DIRECT_ROBOT = "http://192.168.4.1";
    private static final String DIRECT_VIDEO = "http://192.168.4.2/stream";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private WebView video;
    private TextView status, leftValue, rightValue;
    private DriveSlider leftSlider, rightSlider;
    private boolean remote = false;
    private volatile int left = 0, right = 0;
    private volatile long lastSendAt = 0;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        buildUi();
        setMode(false);
    }

    private TextView text(String s, int sp){
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private Button button(String s){
        Button b = new Button(this); b.setText(s); return b;
    }

    private void buildUi(){
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18,18,18));
        root.setPadding(12,12,12,12);

        LinearLayout top = new LinearLayout(this);
        Button direct = button("قريب");
        Button far = button("بعيد");
        Button stop = button("STOP");
        top.addView(direct,new LinearLayout.LayoutParams(0,-2,1));
        top.addView(far,new LinearLayout.LayoutParams(0,-2,1));
        top.addView(stop,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(top,new LinearLayout.LayoutParams(-1,-2));

        status = text("—",14);
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        video = new WebView(this);
        WebSettings ws = video.getSettings();
        ws.setJavaScriptEnabled(false);
        ws.setLoadsImagesAutomatically(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        video.setBackgroundColor(Color.BLACK);
        root.addView(video,new LinearLayout.LayoutParams(-1,0,1.25f));

        LinearLayout labels = new LinearLayout(this);
        leftValue = text("L 0",18);
        rightValue = text("R 0",18);
        labels.addView(leftValue,new LinearLayout.LayoutParams(0,-2,1));
        labels.addView(rightValue,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(labels,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout sliders = new LinearLayout(this);
        sliders.setGravity(Gravity.CENTER);
        leftSlider = new DriveSlider(this, v -> { left=v; leftValue.setText("L "+v); sendDrive(false); });
        rightSlider = new DriveSlider(this, v -> { right=v; rightValue.setText("R "+v); sendDrive(false); });
        sliders.addView(leftSlider,new LinearLayout.LayoutParams(0,0,1));
        sliders.addView(rightSlider,new LinearLayout.LayoutParams(0,0,1));
        root.addView(sliders,new LinearLayout.LayoutParams(-1,0,1));

        direct.setOnClickListener(v -> setMode(false));
        far.setOnClickListener(v -> setMode(true));
        stop.setOnClickListener(v -> {
            left=0; right=0;
            leftSlider.setValue(0); rightSlider.setValue(0);
            leftValue.setText("L 0"); rightValue.setText("R 0");
            sendDrive(true);
        });

        setContentView(root);
    }

    private void setMode(boolean far){
        remote = far;
        status.setText(far ? "REMOTE — Render" : "DIRECT — 4W-TEMP");
        String src = far ? RELAY+"/video/"+CHANNEL : DIRECT_VIDEO;
        String html = "<html><body style='margin:0;background:#000;display:flex;align-items:center;justify-content:center'>"+
                "<img src='"+src+"' style='width:100%;height:auto;max-height:100%;object-fit:contain'/></body></html>";
        video.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
        sendDrive(true);
    }

    private void sendDrive(boolean force){
        long now = System.currentTimeMillis();
        if(!force && now-lastSendAt<60) return;
        lastSendAt = now;
        final int l = left, r = right;
        final boolean far = remote;
        io.execute(() -> {
            String base = far ? RELAY+"/drive/"+CHANNEL : DIRECT_ROBOT+"/drive";
            String u = String.format(Locale.US,"%s?l=%d&r=%d",base,l,r);
            HttpURLConnection c = null;
            try{
                c=(HttpURLConnection)new URL(u).openConnection();
                c.setConnectTimeout(1000);
                c.setReadTimeout(1000);
                c.setUseCaches(false);
                c.setRequestMethod("GET");
                int code=c.getResponseCode();
                runOnUiThread(() -> status.setText((far?"REMOTE":"DIRECT")+" — "+(code==200?"OK":"HTTP "+code)));
            }catch(Exception e){
                runOnUiThread(() -> status.setText((far?"REMOTE":"DIRECT")+" — NO LINK"));
            }finally{ if(c!=null)c.disconnect(); }
        });
    }

    @Override protected void onDestroy(){
        try{video.stopLoading();video.destroy();}catch(Exception ignored){}
        io.shutdownNow();
        super.onDestroy();
    }

    private interface SliderListener { void onValue(int v); }

    private static final class DriveSlider extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SliderListener listener;
        private int value = 0;

        DriveSlider(Activity a, SliderListener l){
            super(a); listener=l;
            setBackgroundColor(Color.rgb(28,28,28));
            setMinimumWidth(120); setMinimumHeight(300);
        }

        void setValue(int v){ value=Math.max(-100,Math.min(100,v)); invalidate(); }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float cx=getWidth()/2f, top=30, bottom=getHeight()-30;
            p.setStrokeWidth(8); p.setColor(Color.DKGRAY); c.drawLine(cx,top,cx,bottom,p);
            p.setStrokeWidth(2); p.setColor(Color.GRAY); c.drawLine(cx-35,getHeight()/2f,cx+35,getHeight()/2f,p);
            float y = bottom - ((value+100)/200f)*(bottom-top);
            p.setColor(Color.WHITE); c.drawCircle(cx,y,24,p);
            p.setTextSize(30); p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.LTGRAY);
            c.drawText("+100",cx,25,p); c.drawText("-100",cx,getHeight()-5,p);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE){
                float top=30, bottom=getHeight()-30;
                float y=Math.max(top,Math.min(bottom,e.getY()));
                int v=Math.round(((bottom-y)/(bottom-top))*200f-100f);
                if(v!=value){ value=v; invalidate(); listener.onValue(value); }
                return true;
            }
            return e.getAction()==MotionEvent.ACTION_UP;
        }
    }
}
