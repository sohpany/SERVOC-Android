package com.servoc.app.control;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

/** Press-and-hold eight-direction driving pad. Release/cancel always emits STOP. */
public final class DirectionPadView extends GridLayout {
    /** 50% alpha backgrounds keep the camera feed visible under the controls. */
    private static final int BASE=0x80263238;
    private static final int ACTIVE=0x80FFC107;
    private static final int STOP=0x80C62828;
    private static final int TEXT=0xFFF5F7F8;
    public interface Listener{void onDrive(int left,int right,boolean active);}
    private Listener listener;
    public DirectionPadView(Context c){
        super(c);
        // The app shell is RTL, but directional geometry must remain physical/screen LTR:
        // ↖ stays top-left, ↗ top-right, ↙ bottom-left, ↘ bottom-right.
        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        setColumnCount(3);setRowCount(3);setAlignmentMode(GridLayout.ALIGN_BOUNDS);setUseDefaultMargins(true);
        add(DiscreteDrive.Intent.FORWARD_LEFT,"↖",0,0);
        add(DiscreteDrive.Intent.FORWARD,"↑",0,1);
        add(DiscreteDrive.Intent.FORWARD_RIGHT,"↗",0,2);
        add(DiscreteDrive.Intent.PIVOT_LEFT,"↺",1,0);
        Button stop=button("■");stop.setBackgroundColor(STOP);stop.setTextColor(Color.WHITE);stop.setOnClickListener(v->emit(DiscreteDrive.Intent.STOP,false));addView(stop,cell(1,1));
        add(DiscreteDrive.Intent.PIVOT_RIGHT,"↻",1,2);
        add(DiscreteDrive.Intent.REVERSE_LEFT,"↙",2,0);
        add(DiscreteDrive.Intent.REVERSE,"↓",2,1);
        add(DiscreteDrive.Intent.REVERSE_RIGHT,"↘",2,2);
    }
    public void setListener(Listener l){listener=l;}
    public void reset(){emit(DiscreteDrive.Intent.STOP,false);}
    private void add(DiscreteDrive.Intent i,String label,int row,int col){
        Button b=button(label);
        b.setOnTouchListener((v,e)->{
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN){b.setBackgroundColor(ACTIVE);b.setTextColor(Color.WHITE);emit(i,true);return true;}
            if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){b.setBackgroundColor(BASE);b.setTextColor(TEXT);emit(DiscreteDrive.Intent.STOP,false);if(a==MotionEvent.ACTION_UP)v.performClick();return true;}
            return true;
        });
        b.setOnClickListener(v->{});addView(b,cell(row,col));
    }
    private Button button(String s){Button b=new Button(getContext());b.setText(s);b.setTextSize(26);b.setGravity(Gravity.CENTER);b.setTextColor(TEXT);b.setBackgroundColor(BASE);b.setAllCaps(false);b.setMinWidth(dp(48));b.setMinHeight(dp(48));b.setPadding(dp(2),0,dp(2),0);return b;}
    private LayoutParams cell(int row,int col){Spec rs=GridLayout.spec(row,1,1f),cs=GridLayout.spec(col,1,1f);LayoutParams p=new LayoutParams(rs,cs);p.width=0;p.height=0;p.setGravity(Gravity.FILL);return p;}
    private void emit(DiscreteDrive.Intent i,boolean active){if(listener==null)return;DriveMixer.Command c=DiscreteDrive.command(i);listener.onDrive(c.left,c.right,active);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
