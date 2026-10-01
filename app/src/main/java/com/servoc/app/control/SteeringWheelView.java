package com.servoc.app.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Steering-wheel presentation requested for HazRob.
 *
 * Gesture model:
 * - touch-down is neutral at the user's finger position;
 * - drag up/down controls forward/reverse magnitude;
 * - drag right/left controls steering curvature;
 * - horizontal-only drag becomes a 50% capped pivot through DriveMixer;
 * - release/cancel always returns 0/0.
 *
 * X and Y are deliberately clamped independently rather than normalized as a circle, so the
 * operator can request full travel speed and full steering lock at the same time.
 */
public final class SteeringWheelView extends View {
    public interface Listener { void onDrive(int left,int right,boolean active); }

    private static final int INVALID_POINTER=-1;
    private final Paint rim=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spoke=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hub=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint indicator=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private int pointerId=INVALID_POINTER;
    private boolean active=false;
    private float startX,startY;
    private float x=0f,y=0f;

    public SteeringWheelView(Context c){
        super(c);setClickable(true);setFocusable(true);setBackgroundColor(Color.TRANSPARENT);
        rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(dp(10));rim.setColor(0xDDF5F7F8);
        spoke.setStyle(Paint.Style.STROKE);spoke.setStrokeWidth(dp(6));spoke.setStrokeCap(Paint.Cap.ROUND);spoke.setColor(0xDDFFC107);
        hub.setStyle(Paint.Style.FILL);hub.setColor(0xFFF5F7F8);
        guide.setStyle(Paint.Style.STROKE);guide.setStrokeWidth(dp(2));guide.setColor(0x66FFC107);
        indicator.setStyle(Paint.Style.FILL);indicator.setColor(0xFFFFC107);
    }

    public void setListener(Listener l){listener=l;}
    public void reset(){resetInternal(true);}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float cx=getWidth()/2f,cy=getHeight()/2f;
        float r=Math.max(dp(20),Math.min(cx,cy)-dp(12));
        c.drawCircle(cx,cy,r+dp(4),guide);
        c.save();
        c.rotate(x*105f,cx,cy);
        c.drawCircle(cx,cy,r,rim);
        float inner=r*0.72f;
        double[] angles={-90,30,150};
        for(double a:angles){double rad=Math.toRadians(a);float ex=cx+(float)Math.cos(rad)*inner,ey=cy+(float)Math.sin(rad)*inner;c.drawLine(cx,cy,ex,ey,spoke);}
        c.drawCircle(cx,cy,dp(15),hub);
        c.restore();

        // Small gesture marker: horizontal = steering, vertical = travel command.
        float ix=cx+x*r*0.72f,iy=cy-y*r*0.72f;
        c.drawCircle(ix,iy,dp(6),indicator);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                pointerId=e.getPointerId(0);startX=e.getX(0);startY=e.getY(0);active=true;x=0;y=0;invalidate();emit();return true;
            case MotionEvent.ACTION_MOVE:{
                int index=e.findPointerIndex(pointerId);if(index<0){resetInternal(true);return true;}update(e.getX(index),e.getY(index));return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                if(e.getPointerId(e.getActionIndex())==pointerId)resetInternal(true);return true;
            case MotionEvent.ACTION_UP:
                performClick();resetInternal(true);return true;
            case MotionEvent.ACTION_CANCEL:
                resetInternal(true);return true;
            default:return true;
        }
    }

    private void update(float px,float py){
        float travel=Math.max(dp(48),Math.min(getWidth(),getHeight())*0.38f);
        x=clamp((px-startX)/travel);
        y=clamp((startY-py)/travel);
        if(Math.abs(x)<0.04f)x=0f;
        if(Math.abs(y)<0.04f)y=0f;
        invalidate();emit();
    }

    private void emit(){if(listener==null)return;DriveMixer.Command c=DriveMixer.mix(x,y);listener.onDrive(c.left,c.right,active);}
    private void resetInternal(boolean notify){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);pointerId=INVALID_POINTER;active=false;x=0;y=0;invalidate();if(notify)emit();}
    private static float clamp(float v){return Math.max(-1f,Math.min(1f,v));}
    @Override public boolean performClick(){super.performClick();return true;}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
}
