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

/** Shared HR069 UI primitives with explicit high-contrast colors independent of phone theme. */
public final class UiKit {
    private UiKit(){}
    public static final int DARK_BG=0xFF181D24,DARK_TEXT=0xFFECEFF1,MUTED_TEXT=0xFFB8C1CC,LIGHT_BG=0xFFECEFF1,LIGHT_TEXT=0xFF1B1F23;

    public static LinearLayout vbox(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);rtl(l);return l;}
    public static LinearLayout hbox(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);rtl(l);return l;}
    public static TextView text(Context c,String s){TextView t=new TextView(c);t.setText(s);t.setTextColor(DARK_TEXT);t.setPadding(dp(c,8),dp(c,7),dp(c,8),dp(c,7));t.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return t;}
    public static TextView title(Context c,String s){TextView t=text(c,s);t.setTextSize(18);t.setTypeface(Typeface.DEFAULT_BOLD);t.setTextColor(0xFF90CAF9);return t;}
    public static TextView small(Context c,String s){TextView t=text(c,s);t.setTextSize(12);t.setTextColor(MUTED_TEXT);return t;}
    public static Button button(Context c,String s){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextColor(LIGHT_TEXT);b.setBackgroundColor(LIGHT_BG);b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return b;}
    public static EditText input(Context c,String s){EditText e=new EditText(c);e.setText(s);e.setTextColor(DARK_TEXT);e.setHintTextColor(0xFF90A4AE);e.setSingleLine(true);e.setBackgroundColor(0xFF232A33);e.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);return e;}
    public static LinearLayout card(Context c){LinearLayout l=vbox(c);l.setPadding(dp(c,8),dp(c,8),dp(c,8),dp(c,8));l.setBackgroundColor(DARK_BG);return l;}
    public static LinearLayout.LayoutParams full(Context c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,3),0,dp(c,3));return p;}
    public static LinearLayout.LayoutParams weight(float w){return new LinearLayout.LayoutParams(0,-2,w);}
    public static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    public static void rtl(View v){v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);}
    public static int foregroundFor(int background){int r=Color.red(background),g=Color.green(background),b=Color.blue(background);double luminance=.2126*r+.7152*g+.0722*b;return luminance>150?Color.BLACK:Color.WHITE;}
    public static void contrast(TextView t,int background){t.setBackgroundColor(background);t.setTextColor(foregroundFor(background));}
    public static void riskColor(TextView t,int risk){
        switch(risk){
            case 1:contrast(t,0xFF1B5E20);break;
            case 2:contrast(t,0xFF43A047);break;
            case 3:contrast(t,0xFFF9A825);break;
            case 4:contrast(t,0xFFEF6C00);break;
            case 5:contrast(t,0xFFC62828);break;
            default:contrast(t,0xFF263238);break;
        }
    }
    public static String stars(int confidence){return confidence<=0?"—":confidence==1?"★☆☆":confidence==2?"★★☆":"★★★";}
    public static String riskName(int risk){switch(risk){case 1:return"آمن جدًا";case 2:return"آمن";case 3:return"تنبيه";case 4:return"خطر";case 5:return"حرج";default:return"غير متاح";}}
    public static View divider(Context c){View v=new View(c);v.setBackgroundColor(0xFF30363D);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}
}
