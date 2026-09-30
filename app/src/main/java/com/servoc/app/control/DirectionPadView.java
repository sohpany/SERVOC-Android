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
    public interface Listener{void onDrive(int left,int right,boolean active);}
    private Listener listener;
    public DirectionPadView(Context c){
        super(c);setColumnCount(3);setRowCount(3);setAlignmentMode(GridLayout.ALIGN_BOUNDS);setUseDefaultMargins(true);
        add(DiscreteDrive.Intent.FORWARD_LEFT,"↖\nأمام يسار",0,0);
        add(DiscreteDrive.Intent.FORWARD,"↑\nأمام",0,1);
        add(DiscreteDrive.Intent.FORWARD_RIGHT,"↗\nأمام يمين",0,2);
        add(DiscreteDrive.Intent.PIVOT_LEFT,"↺\nدوران يسار",1,0);
        Button stop=button("■\nتوقف");stop.setBackgroundColor(0xFFB71C1C);stop.setTextColor(Color.WHITE);stop.setOnClickListener(v->emit(DiscreteDrive.Intent.STOP,false));addView(stop,cell(1,1));
        add(DiscreteDrive.Intent.PIVOT_RIGHT,"↻\nدوران يمين",1,2);
        add(DiscreteDrive.Intent.REVERSE_LEFT,"↙\nخلف يسار",2,0);
        add(DiscreteDrive.Intent.REVERSE,"↓\nخلف",2,1);
        add(DiscreteDrive.Intent.REVERSE_RIGHT,"↘\nخلف يمين",2,2);
    }
    public void setListener(Listener l){listener=l;}
    public void reset(){emit(DiscreteDrive.Intent.STOP,false);}
    private void add(DiscreteDrive.Intent i,String label,int row,int col){Button b=button(label);b.setOnTouchListener((v,e)->{int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){emit(i,true);return true;}if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){emit(DiscreteDrive.Intent.STOP,false);if(a==MotionEvent.ACTION_UP)v.performClick();return true;}return true;});b.setOnClickListener(v->{});addView(b,cell(row,col));}
    private Button button(String s){Button b=new Button(getContext());b.setText(s);b.setTextSize(11);b.setGravity(Gravity.CENTER);b.setTextColor(0xFFECEFF1);b.setBackgroundColor(0xCC263238);b.setAllCaps(false);b.setPadding(dp(3),dp(2),dp(3),dp(2));return b;}
    private LayoutParams cell(int row,int col){Spec rs=GridLayout.spec(row,1,1f),cs=GridLayout.spec(col,1,1f);LayoutParams p=new LayoutParams(rs,cs);p.width=0;p.height=0;p.setGravity(Gravity.FILL);return p;}
    private void emit(DiscreteDrive.Intent i,boolean active){if(listener==null)return;DriveMixer.Command c=DiscreteDrive.command(i);listener.onDrive(c.left,c.right,active);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
