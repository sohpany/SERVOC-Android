package com.servoc.app.control;

import android.content.Context;
import android.content.SharedPreferences;
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
    private final SharedPreferences drivePreferences;
    private Listener listener;
    public DirectionPadView(Context c){
        super(c);
        drivePreferences=c.getSharedPreferences(DriveDirectionOption.PREF_FILE,Context.MODE_PRIVATE);
        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        setColumnCount(3);setRowCount(3);setAlignmentMode(GridLayout.ALIGN_BOUNDS);setUseDefaultMargins(true);
        add(DiscreteDrive.Intent.FORWARD_LEFT,"↖",0,0);
        add(DiscreteDrive.Intent.FORWARD,"↑",0,1);
        add(DiscreteDrive.Intent.FORWARD_RIGHT,"↗",0,2);
        add(DiscreteDrive.Intent.PIVOT_RIGHT,"↺",1,0);
        Button stop=button("■");stop.setBackgroundColor(STOP);stop.setTextColor(Color.WHITE);stop.setOnClickListener(v->emit(DiscreteDrive.Intent.STOP,false));addView(stop,cell(1,1));
        add(DiscreteDrive.Intent.PIVOT_LEFT,"↻",1,2);
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
    private void emit(DiscreteDrive.Intent i,boolean active){if(listener==null)return;int turn=drivePreferences.getInt(DriveTuning.PREF_TURN_PERCENT,DriveTuning.DEFAULT_TURN_PERCENT);int pivot=drivePreferences.getInt(DriveTuning.PREF_PIVOT_PERCENT,DriveTuning.DEFAULT_PIVOT_PERCENT);DriveMixer.Command c=DiscreteDrive.command(i,turn,pivot);boolean reverse=drivePreferences.getBoolean(DriveDirectionOption.PREF_REVERSE_BUTTONS,false);boolean reverseSpin=drivePreferences.getBoolean(DriveDirectionOption.PREF_REVERSE_PIVOT_ALL,false);
        boolean pivot=i==DiscreteDrive.Intent.PIVOT_LEFT||i==DiscreteDrive.Intent.PIVOT_RIGHT;
        DriveMixer.Command mapped=DriveDirectionOption.applyPivot(DriveMode.BUTTONS,reverse,reverseSpin,pivot,c.left,c.right);listener.onDrive(mapped.left,mapped.right,active);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
