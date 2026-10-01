package com.servoc.app.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Wide spring-centred vertical control for one physical side of the HazRob differential drive. */
public final class DualTankDriveControl extends View {
    public interface Listener { void onCommand(int value); }
    private final Paint track=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG),knob=new Paint(Paint.ANTI_ALIAS_FLAG),text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int value=0; private Listener listener;

    public DualTankDriveControl(Context c){
        super(c);setClickable(true);setFocusable(true);
        track.setColor(0xAA455A64);track.setStrokeCap(Paint.Cap.ROUND);
        fill.setColor(0xFFFFC107);fill.setStrokeCap(Paint.Cap.ROUND);
        knob.setColor(0xFFF5F7F8);text.setColor(0xFFF5F7F8);text.setTextAlign(Paint.Align.CENTER);text.setTextSize(dp(12));
        updateDescription();
    }
    public void setListener(Listener l){listener=l;}
    public int value(){return value;}
    public void reset(){setValue(0,true);}
    private void setValue(int v,boolean notify){v=Math.max(-100,Math.min(100,v));if(v==value){if(notify&&listener!=null)listener.onCommand(value);return;}value=v;invalidate();updateDescription();if(notify&&listener!=null)listener.onCommand(value);}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(resolveSize(dp(92),w),resolveSize(dp(220),h));}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float cx=getWidth()/2f,top=dp(25),bottom=getHeight()-dp(25),mid=(top+bottom)/2f;
        track.setStrokeWidth(dp(20));fill.setStrokeWidth(dp(16));c.drawLine(cx,top,cx,bottom,track);
        float y=mid-(value/100f)*(bottom-top)/2f;c.drawLine(cx,mid,cx,y,fill);c.drawCircle(cx,y,dp(18),knob);
        c.drawText("+100",cx,dp(15),text);c.drawText("0",cx,mid+dp(4),text);c.drawText("-100",cx,getHeight()-dp(5),text);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);update(e.getY());return true;}
        if(a==MotionEvent.ACTION_MOVE){update(e.getY());return true;}
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){if(a==MotionEvent.ACTION_UP)performClick();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);setValue(0,true);return true;}return true;
    }
    private void update(float py){float top=dp(25),bottom=getHeight()-dp(25),mid=(top+bottom)/2f,half=(bottom-top)/2f;float y=Math.max(top,Math.min(bottom,py));setValue(Math.round((mid-y)*100f/half),true);}
    @Override public boolean performClick(){super.performClick();return true;}
    private void updateDescription(){setContentDescription("تحكم جهة العجلات "+value);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
