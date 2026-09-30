package com.servoc.app.vision;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Foreground HUD drawn directly above the camera image.
 *
 * Flame evidence may carry a text label. Gas tracking is intentionally arrow-only: the overlay
 * never owns drive/safety authority and never presents directional coherence as a probability.
 * Camera frames are QVGA 4:3 and are rendered by MjpegView using FIT_CENTER, so normalized flame
 * coordinates are mapped into the same fitted image rectangle rather than the whole screen.
 */
public final class HazardOverlayView extends View {
    private static final float SOURCE_ASPECT=4f/3f;

    private final Paint flameFill=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flameStroke=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gasPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gasShadow=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textBg=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowHead=new Path();
    private final RectF imageRect=new RectF();

    private boolean flameVisible=false,gasVisible=false;
    private float left,top,right,bottom;
    private double gasAngleRad;

    public HazardOverlayView(Context c){
        super(c);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);

        flameFill.setStyle(Paint.Style.FILL);flameFill.setColor(0x36FF0000);
        flameStroke.setStyle(Paint.Style.STROKE);flameStroke.setStrokeWidth(dp(3));flameStroke.setColor(0xE6FF3030);

        gasShadow.setStyle(Paint.Style.STROKE);gasShadow.setStrokeCap(Paint.Cap.ROUND);gasShadow.setStrokeJoin(Paint.Join.ROUND);gasShadow.setStrokeWidth(dp(11));gasShadow.setColor(0xB0000000);
        gasPaint.setStyle(Paint.Style.STROKE);gasPaint.setStrokeCap(Paint.Cap.ROUND);gasPaint.setStrokeJoin(Paint.Join.ROUND);gasPaint.setStrokeWidth(dp(7));gasPaint.setColor(0xE600E676);

        textPaint.setStyle(Paint.Style.FILL);textPaint.setTextSize(dp(13));textPaint.setFakeBoldText(true);textPaint.setColor(0xFFFFFFFF);
        textBg.setStyle(Paint.Style.FILL);textBg.setColor(0xB0000000);
        hudPaint.setStyle(Paint.Style.STROKE);hudPaint.setStrokeWidth(dp(1.5f));hudPaint.setColor(0x70FFFFFF);
    }

    public void setFlameRegion(double l,double t,double r,double b,boolean visible){
        flameVisible=visible&&finite(l)&&finite(t)&&finite(r)&&finite(b)&&r>l&&b>t;
        if(flameVisible){left=clamp((float)l);top=clamp((float)t);right=clamp((float)r);bottom=clamp((float)b);}invalidate();
    }
    public void clearFlame(){flameVisible=false;invalidate();}

    /** angleRad is relative to robot forward: 0=up, +PI/2=right, -PI/2=left. */
    public void setGasDirection(double angleRad,double confidence){
        gasVisible=Double.isFinite(angleRad)&&Double.isFinite(confidence)&&confidence>=0.18;
        gasAngleRad=angleRad;invalidate();
    }
    public void clearGas(){gasVisible=false;invalidate();}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        computeImageRect(w,h,imageRect);
        drawHudCorners(c,imageRect);
        if(flameVisible)drawFlameRegion(c,imageRect);
        if(gasVisible)drawGasArrow(c,imageRect);
    }

    private void drawFlameRegion(Canvas c,RectF frame){
        RectF box=new RectF(
                frame.left+left*frame.width(),
                frame.top+top*frame.height(),
                frame.left+right*frame.width(),
                frame.top+bottom*frame.height());
        float pad=dp(4);box.inset(-pad,-pad);
        box.left=Math.max(frame.left,box.left);box.top=Math.max(frame.top,box.top);
        box.right=Math.min(frame.right,box.right);box.bottom=Math.min(frame.bottom,box.bottom);
        c.drawRect(box,flameFill);c.drawRect(box,flameStroke);
        drawLabel(c,"لهب محتمل",box.left,Math.max(frame.top,box.top-dp(22)),0xD9B71C1C);
    }

    private void drawGasArrow(Canvas c,RectF frame){
        float cx=frame.centerX(),cy=frame.top+frame.height()*0.48f;
        float len=Math.min(frame.width(),frame.height())*0.24f;
        float dx=(float)Math.sin(gasAngleRad),dy=(float)-Math.cos(gasAngleRad);
        float ex=cx+dx*len,ey=cy+dy*len;

        c.drawLine(cx,cy,ex,ey,gasShadow);
        c.drawLine(cx,cy,ex,ey,gasPaint);
        c.drawCircle(cx,cy,dp(5),gasPaint);

        float head=Math.max(dp(16),len*0.25f),wing=head*0.52f;
        float bx=ex-dx*head,by=ey-dy*head;float px=-dy,py=dx;
        arrowHead.reset();arrowHead.moveTo(ex,ey);arrowHead.lineTo(bx+px*wing,by+py*wing);arrowHead.lineTo(bx-px*wing,by-py*wing);arrowHead.close();
        Paint shadowFill=new Paint(gasShadow);shadowFill.setStyle(Paint.Style.FILL);c.drawPath(arrowHead,shadowFill);
        Paint fill=new Paint(gasPaint);fill.setStyle(Paint.Style.FILL);c.drawPath(arrowHead,fill);
    }

    private void drawHudCorners(Canvas c,RectF frame){
        float s=Math.min(frame.width(),frame.height())*0.055f;
        float inset=dp(8);
        float l=frame.left+inset,t=frame.top+inset,r=frame.right-inset,b=frame.bottom-inset;
        c.drawLine(l,t,l+s,t,hudPaint);c.drawLine(l,t,l,t+s,hudPaint);
        c.drawLine(r-s,t,r,t,hudPaint);c.drawLine(r,t,r,t+s,hudPaint);
        c.drawLine(l,b,l+s,b,hudPaint);c.drawLine(l,b-s,l,b,hudPaint);
        c.drawLine(r-s,b,r,b,hudPaint);c.drawLine(r,b-s,r,b,hudPaint);
    }

    private void drawLabel(Canvas c,String text,float x,float y,int bgColor){
        float padX=dp(7),padY=dp(5);float tw=textPaint.measureText(text);Paint.FontMetrics fm=textPaint.getFontMetrics();float th=fm.bottom-fm.top;
        textBg.setColor(bgColor);RectF bg=new RectF(x,y,x+tw+padX*2,y+th+padY*2);c.drawRoundRect(bg,dp(5),dp(5),textBg);
        c.drawText(text,x+padX,y+padY-fm.top,textPaint);
    }

    /** MjpegView uses FIT_CENTER; mirror that geometry for overlay alignment. */
    private static void computeImageRect(float viewW,float viewH,RectF out){
        float viewAspect=viewW/viewH;
        if(viewAspect>SOURCE_ASPECT){
            float imageW=viewH*SOURCE_ASPECT;float left=(viewW-imageW)/2f;out.set(left,0,left+imageW,viewH);
        }else{
            float imageH=viewW/SOURCE_ASPECT;float top=(viewH-imageH)/2f;out.set(0,top,viewW,top+imageH);
        }
    }

    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static boolean finite(double v){return Double.isFinite(v);}
    private static float clamp(float v){return v<0?0:v>1?1:v;}
}
