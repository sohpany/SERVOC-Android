package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UiKit {
    private UiKit(){}

    public static LinearLayout vbox(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);rtl(l);return l;}
    public static LinearLayout hbox(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);rtl(l);return l;}
    public static TextView text(Context c,String s){TextView t=new TextView(c);t.setText(s);t.setTextColor(Color.WHITE);t.setPadding(dp(c,8),dp(c,7),dp(c,8),dp(c,7));t.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return t;}
    public static TextView title(Context c,String s){TextView t=text(c,s);t.setTextSize(18);t.setTypeface(Typeface.DEFAULT_BOLD);t.setTextColor(0xFF90CAF9);return t;}
    public static TextView small(Context c,String s){TextView t=text(c,s);t.setTextSize(12);t.setTextColor(0xFFB8C1CC);return t;}
    public static Button button(Context c,String s){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return b;}
    public static EditText input(Context c,String s){EditText e=new EditText(c);e.setText(s);e.setTextColor(Color.WHITE);e.setSingleLine(true);e.setBackgroundColor(0xFF232A33);e.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return e;}
    public static LinearLayout card(Context c){LinearLayout l=vbox(c);l.setPadding(dp(c,8),dp(c,8),dp(c,8),dp(c,8));l.setBackgroundColor(0xFF181D24);return l;}
    public static LinearLayout.LayoutParams full(Context c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,3),0,dp(c,3));return p;}
    public static LinearLayout.LayoutParams weight(float w){return new LinearLayout.LayoutParams(0,-2,w);}
    public static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    public static void rtl(View v){v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);}
    public static void riskColor(TextView t,int risk){
        t.setTextColor(Color.WHITE);
        switch(risk){
            case 1:t.setBackgroundColor(0xFF1B5E20);break;
            case 2:t.setBackgroundColor(0xFF43A047);break;
            case 3:t.setBackgroundColor(0xFFF9A825);t.setTextColor(Color.BLACK);break;
            case 4:t.setBackgroundColor(0xFFEF6C00);break;
            case 5:t.setBackgroundColor(0xFFC62828);break;
            default:t.setBackgroundColor(0xFF263238);break;
        }
    }
    public static String stars(int confidence){return confidence<=0?"—":confidence==1?"★☆☆":confidence==2?"★★☆":"★★★";}
    public static String riskName(int risk){switch(risk){case 1:return"آمن جدًا";case 2:return"آمن";case 3:return"تنبيه";case 4:return"خطر";case 5:return"حرج";default:return"غير متاح";}}
    public static View divider(Context c){View v=new View(c);v.setBackgroundColor(0xFF30363D);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}
}
