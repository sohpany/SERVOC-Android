package com.servoc.app.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Steering-wheel presentation for the differential drive.
 * The touch vector is interpreted as a clock-face drive demand:
 * 12=forward, 6=reverse, 3=pivot-right, 9=pivot-left and diagonals=progressive turns.
 */
public final class SteeringWheelView extends View {
    public interface Listener{void onDrive(int left,int right,boolean active);}
    private final Paint outer=new Paint(Paint.ANTI_ALIAS_FLAG),inner=new Paint(Paint.ANTI_ALIAS_FLAG),spoke=new Paint(Paint.ANTI_ALIAS_FLAG),marker=new Paint(Paint.ANTI_ALIAS_FLAG),label=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x=0,y=0;private boolean active=false;private int pointerId=-1;private Listener listener;

    public SteeringWheelView(Context c){
        super(c);setClickable(true);setFocusable(true);
        outer.setStyle(Paint.Style.STROKE);outer.setStrokeWidth(dp(8));outer.setColor(0xDDFFFFFF);
        inner.setStyle(Paint.Style.STROKE);inner.setStrokeWidth(dp(3));inner.setColor(0xAA90CAF9);
        spoke.setStrokeWidth(dp(5));spoke.setStrokeCap(Paint.Cap.ROUND);spoke.setColor(0xAAFFFFFF);
        marker.setStyle(Paint.Style.FILL);marker.setColor(0xFFFFC107);
        label.setColor(0xFFECEFF1);label.setTextAlign(Paint.Align.CENTER);label.setTextSize(dp(11));
    }
    public void setListener(Listener l){listener=l;}
    public void reset(){resetInternal(true);}

    @Override protected void onMeasure(int w,int h){int d=resolveSize(dp(180),Math.min(w,h));setMeasuredDimension(d,d);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.max(1f,Math.min(cx,cy)-dp(10));
        c.drawCircle(cx,cy,r,outer);c.drawCircle(cx,cy,r*.42f,inner);
        double rotation=Math.atan2(x,y);for(int i=0;i<3;i++){double a=rotation+i*2*Math.PI/3;c.drawLine(cx,cy,cx+(float)Math.sin(a)*r*.72f,cy-(float)Math.cos(a)*r*.72f,spoke);}
        float mr=r*.86f;c.drawCircle(cx+x*mr,cy-y*mr,dp(7),marker);
        c.drawText("12",cx,cy-r+dp(14),label);c.drawText("3",cx+r-dp(8),cy+dp(4),label);c.drawText("6",cx,cy+r-dp(4),label);c.drawText("9",cx-r+dp(8),cy+dp(4),label);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);pointerId=e.getPointerId(0);active=true;update(e,0);return true;
            case MotionEvent.ACTION_MOVE:int idx=e.findPointerIndex(pointerId);if(idx<0){resetInternal(true);return true;}update(e,idx);return true;
            case MotionEvent.ACTION_POINTER_UP:if(e.getPointerId(e.getActionIndex())==pointerId)resetInternal(true);return true;
            case MotionEvent.ACTION_UP:performClick();resetInternal(true);return true;
            case MotionEvent.ACTION_CANCEL:resetInternal(true);return true;
            default:return true;
        }
    }
    private void update(MotionEvent e,int idx){
        float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.max(1f,Math.min(cx,cy)-dp(10));float nx=(e.getX(idx)-cx)/r,ny=-(e.getY(idx)-cy)/r;double mag=Math.hypot(nx,ny);if(mag>1){nx/=mag;ny/=mag;}if(mag<=1e-6){nx=ny=0;}x=nx;y=ny;invalidate();emit();
    }
    private void emit(){if(listener==null)return;DriveMixer.Command c=DriveMixer.mix(x,y);listener.onDrive(c.left,c.right,active);}
    private void resetInternal(boolean emit){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);pointerId=-1;active=false;x=y=0;invalidate();if(emit)emit();}
    @Override public boolean performClick(){super.performClick();return true;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
