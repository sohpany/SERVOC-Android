package com.servoc.app.vision;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Lightweight live overlay: red flame-suspicion box + green gas-direction arrow. */
public final class HazardOverlayView extends View {
    private final Paint flameFill=new Paint(Paint.ANTI_ALIAS_FLAG),flameStroke=new Paint(Paint.ANTI_ALIAS_FLAG),gasPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowHead=new Path();
    private boolean flameVisible=false,gasVisible=false;
    private float left,top,right,bottom;
    private double gasAngleRad,gasConfidence;

    public HazardOverlayView(Context c){
        super(c);setWillNotDraw(false);setClickable(false);setFocusable(false);
        flameFill.setStyle(Paint.Style.FILL);flameFill.setColor(0x36FF0000);
        flameStroke.setStyle(Paint.Style.STROKE);flameStroke.setStrokeWidth(dp(3));flameStroke.setColor(0xD9FF3030);
        gasPaint.setStyle(Paint.Style.STROKE);gasPaint.setStrokeCap(Paint.Cap.ROUND);gasPaint.setStrokeJoin(Paint.Join.ROUND);gasPaint.setStrokeWidth(dp(7));gasPaint.setColor(0xA800E676);
    }

    public void setFlameRegion(double l,double t,double r,double b,boolean visible){
        flameVisible=visible&&finite(l)&&finite(t)&&finite(r)&&finite(b)&&r>l&&b>t;
        if(flameVisible){left=clamp((float)l);top=clamp((float)t);right=clamp((float)r);bottom=clamp((float)b);}invalidate();
    }
    public void clearFlame(){flameVisible=false;invalidate();}

    /** angleRad is relative to robot forward: 0=up, +PI/2=right, -PI/2=left. */
    public void setGasDirection(double angleRad,double confidence){
        gasVisible=Double.isFinite(angleRad)&&Double.isFinite(confidence)&&confidence>=0.18;
        gasAngleRad=angleRad;gasConfidence=Math.max(0,Math.min(1,confidence));invalidate();
    }
    public void clearGas(){gasVisible=false;invalidate();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        if(flameVisible){
            RectF box=new RectF(left*w,top*h,right*w,bottom*h);float pad=dp(4);box.inset(-pad,-pad);
            c.drawRect(box,flameFill);c.drawRect(box,flameStroke);
        }
        if(gasVisible)drawGasArrow(c,w,h);
    }

    private void drawGasArrow(Canvas c,float w,float h){
        // Place away from the bottom-right joystick and keep the arrow readable over video.
        float cx=w*0.50f,cy=h*0.22f;float len=Math.min(w,h)*(0.15f+0.08f*(float)gasConfidence);
        // Screen coordinates: 0 rad = up.
        float dx=(float)Math.sin(gasAngleRad),dy=(float)-Math.cos(gasAngleRad);
        float ex=cx+dx*len,ey=cy+dy*len;c.drawLine(cx,cy,ex,ey,gasPaint);
        float head=Math.max(dp(14),len*0.26f),wing=head*0.52f;
        float bx=ex-dx*head,by=ey-dy*head;float px=-dy,py=dx;
        arrowHead.reset();arrowHead.moveTo(ex,ey);arrowHead.lineTo(bx+px*wing,by+py*wing);arrowHead.lineTo(bx-px*wing,by-py*wing);arrowHead.close();
        Paint p=new Paint(gasPaint);p.setStyle(Paint.Style.FILL);c.drawPath(arrowHead,p);
    }

    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static boolean finite(double v){return Double.isFinite(v);}
    private static float clamp(float v){return v<0?0:v>1?1:v;}
}
