package com.servoc.app.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Vertical 0..100% operator speed selector. Top is faster, bottom is slower. */
public final class VerticalSpeedControl extends View {
    public interface Listener { void onSpeedChanged(int percent); }

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int percent = DriveSpeedLimiter.DEFAULT_PERCENT;
    private Listener listener;

    public VerticalSpeedControl(Context context) {
        super(context);
        track.setColor(0x668FA3AD);track.setStrokeCap(Paint.Cap.ROUND);
        fill.setColor(0xFF42A5F5);fill.setStrokeCap(Paint.Cap.ROUND);
        knob.setColor(0xEEFFFFFF);
        tick.setColor(0x99FFFFFF);
        text.setColor(0xFFCFD8DC);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(dp(10));
        setClickable(true);setFocusable(true);updateDescription();
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public int getPercent() { return percent; }

    public void setPercent(int value, boolean notify) {
        int p = DriveSpeedLimiter.clampPercent(value);
        if (p == percent) { if (notify && listener != null) listener.onSpeedChanged(percent); return; }
        percent = p;invalidate();updateDescription();if (notify && listener != null) listener.onSpeedChanged(percent);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(dp(64), widthMeasureSpec), resolveSize(dp(160), heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx=getWidth()/2f,top=dp(24),bottom=Math.max(top+1,getHeight()-dp(22));
        float stroke=dp(8);track.setStrokeWidth(stroke);fill.setStrokeWidth(stroke);
        c.drawLine(cx,top,cx,bottom,track);
        float y=bottom-(bottom-top)*(percent/100f);
        c.drawLine(cx,y,cx,bottom,fill);
        tick.setStrokeWidth(dp(1));float mid=(top+bottom)/2f;c.drawLine(cx-dp(12),mid,cx+dp(12),mid,tick);
        c.drawCircle(cx,y,dp(10),knob);
        c.drawText("100%",cx,dp(12),text);
        c.drawText("0%",cx,getHeight()-dp(3),text);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int action=e.getActionMasked();
        if(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_MOVE&&action!=MotionEvent.ACTION_UP&&action!=MotionEvent.ACTION_CANCEL)return false;
        if(action==MotionEvent.ACTION_DOWN&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
        if(action!=MotionEvent.ACTION_CANCEL)updateFromY(e.getY());
        if(action==MotionEvent.ACTION_UP){performClick();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
        else if(action==MotionEvent.ACTION_CANCEL&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
        return true;
    }

    private void updateFromY(float touchY) {
        float top=dp(24),bottom=Math.max(top+1,getHeight()-dp(22));
        float y=Math.max(top,Math.min(bottom,touchY));
        int p=Math.round((bottom-y)*100f/(bottom-top));
        setPercent(p,true);
    }

    @Override public boolean performClick(){super.performClick();return true;}

    private void updateDescription(){setContentDescription("سرعة المركبة "+percent+" بالمئة");}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
