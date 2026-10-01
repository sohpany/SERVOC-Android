package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared HR069 UI primitives with explicit HazRob palette and high-contrast colors. */
public final class UiKit {
    private UiKit(){}

    // HazRob identity palette: yellow / black / white / gray.
    public static final int HAZ_BLACK=0xFF0A0D11;
    public static final int HAZ_SURFACE=0xFF151A20;
    public static final int HAZ_SURFACE_ALT=0xFF232A33;
    public static final int HAZ_GRAY_DARK=0xFF263238;
    public static final int HAZ_GRAY=0xFF455A64;
    public static final int HAZ_MUTED=0xFFB0BEC5;
    public static final int HAZ_WHITE=0xFFF5F7F8;
    public static final int HAZ_YELLOW=0xFFFFC107;
    public static final int NON_DRIVING_BG=0xFF0E1116;
    public static final double MIN_TEXT_CONTRAST=4.5;

    // Functional state colors are intentionally separate from brand colors.
    public static final int STATE_GOOD=0xFF81C784;
    public static final int STATE_WARN=0xFFFFC107;
    public static final int STATE_BAD=0xFFFF7043;
    public static final int STATE_CRITICAL=0xFFC62828;

    // Compatibility aliases used by existing screens.
    public static final int DARK_BG=HAZ_SURFACE,DARK_TEXT=HAZ_WHITE,MUTED_TEXT=HAZ_MUTED,LIGHT_BG=HAZ_GRAY_DARK,LIGHT_TEXT=HAZ_WHITE;

    public static LinearLayout vbox(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);rtl(l);return l;}
    public static LinearLayout hbox(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);rtl(l);return l;}
    public static TextView text(Context c,String s){TextView t=new TextView(c);t.setText(s);t.setTextColor(DARK_TEXT);t.setTextSize(14);t.setPadding(dp(c,8),dp(c,7),dp(c,8),dp(c,7));t.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);guardOnAttach(t);return t;}
    public static TextView title(Context c,String s){TextView t=text(c,s);t.setTextSize(18);t.setTypeface(Typeface.DEFAULT_BOLD);t.setTextColor(HAZ_YELLOW);return t;}
    public static TextView small(Context c,String s){TextView t=text(c,s);t.setTextSize(12);t.setTextColor(MUTED_TEXT);return t;}

    /** Default secondary action: gray surface with white text. */
    public static Button button(Context c,String s){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(HAZ_WHITE);b.setBackgroundColor(HAZ_GRAY_DARK);b.setMinHeight(dp(c,48));b.setMinWidth(dp(c,48));b.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);guardOnAttach(b);return b;}
    /** Primary/selected action: HazRob yellow with black text. */
    public static void primary(Button b){if(b==null)return;b.setBackgroundColor(HAZ_YELLOW);b.setTextColor(Color.BLACK);}
    public static void secondary(Button b){if(b==null)return;b.setBackgroundColor(HAZ_GRAY_DARK);b.setTextColor(HAZ_WHITE);}
    public static void selected(Button b,boolean on){if(on)primary(b);else secondary(b);}
    public static void danger(Button b){if(b==null)return;b.setBackgroundColor(STATE_CRITICAL);b.setTextColor(Color.WHITE);}

    public static EditText input(Context c,String s){EditText e=new EditText(c);e.setText(s);e.setTextColor(HAZ_WHITE);e.setHintTextColor(0xFF90A4AE);e.setSingleLine(true);e.setMinHeight(dp(c,48));e.setBackgroundColor(HAZ_SURFACE_ALT);e.setPadding(dp(c,10),dp(c,8),dp(c,10),dp(c,8));e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);guardOnAttach(e);return e;}
    public static LinearLayout card(Context c){LinearLayout l=vbox(c);l.setPadding(dp(c,8),dp(c,8),dp(c,8),dp(c,8));l.setBackgroundColor(HAZ_SURFACE);return l;}
    public static LinearLayout.LayoutParams full(Context c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,3),0,dp(c,3));return p;}
    public static LinearLayout.LayoutParams weight(float w){return new LinearLayout.LayoutParams(0,-2,w);}
    public static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    public static void rtl(View v){v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);}

    private static double channel(int value){double s=value/255.0;return s<=0.03928?s/12.92:Math.pow((s+0.055)/1.055,2.4);}
    public static double luminance(int color){return 0.2126*channel(Color.red(color))+0.7152*channel(Color.green(color))+0.0722*channel(Color.blue(color));}
    public static double contrastRatio(int a,int b){double la=luminance(a),lb=luminance(b),hi=Math.max(la,lb),lo=Math.min(la,lb);return (hi+0.05)/(lo+0.05);}
    public static int foregroundFor(int background){double black=contrastRatio(background,Color.BLACK),white=contrastRatio(background,Color.WHITE);return black>=white?Color.BLACK:Color.WHITE;}
    public static void contrast(TextView t,int background){t.setBackgroundColor(background);t.setTextColor(foregroundFor(background));}

    /**
     * Runtime contrast guard for every non-driving workspace. It is intentionally not used to
     * restructure ControlTab; the driving/video overlay keeps its own transparency/layout rules.
     * Explicit card/button colors are preserved. Text below 4.5:1 is corrected to black or white.
     */
    public static void applyNonDrivingContrast(View root){
        if(root==null)return;
        if(root.getBackground()==null)root.setBackgroundColor(NON_DRIVING_BG);
        auditContrast(root,backgroundColor(root,NON_DRIVING_BG));
    }

    private static void guardOnAttach(final TextView t){
        t.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(View v){t.post(()->ensureReadable(t));}
            @Override public void onViewDetachedFromWindow(View v){}
        });
    }

    private static void ensureReadable(TextView t){
        if(t==null)return;int bg=effectiveBackground(t);int current=t.getCurrentTextColor();
        if(contrastRatio(current,bg)<MIN_TEXT_CONTRAST)t.setTextColor(foregroundFor(bg));
        if(t instanceof EditText){EditText e=(EditText)t;int hint=e.getCurrentHintTextColor();if(contrastRatio(hint,bg)<MIN_TEXT_CONTRAST)e.setHintTextColor(foregroundFor(bg));}
    }

    private static int effectiveBackground(View v){
        View cur=v;while(cur!=null){Drawable d=cur.getBackground();if(d instanceof ColorDrawable){int c=((ColorDrawable)d).getColor();if(Color.alpha(c)>=230)return c;}ViewParent p=cur.getParent();cur=p instanceof View?(View)p:null;}return NON_DRIVING_BG;
    }

    private static void auditContrast(View v,int inheritedBackground){
        if(v==null)return;int background=backgroundColor(v,inheritedBackground);
        if(v instanceof TextView){TextView t=(TextView)v;int current=t.getCurrentTextColor();if(contrastRatio(current,background)<MIN_TEXT_CONTRAST)t.setTextColor(foregroundFor(background));if(t instanceof EditText){EditText e=(EditText)t;int hint=e.getCurrentHintTextColor();if(contrastRatio(hint,background)<MIN_TEXT_CONTRAST)e.setHintTextColor(foregroundFor(background));}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)auditContrast(g.getChildAt(i),background);}
    }

    private static int backgroundColor(View v,int fallback){Drawable d=v.getBackground();if(d instanceof ColorDrawable){int c=((ColorDrawable)d).getColor();if(Color.alpha(c)>=230)return c;}return fallback;}

    public static void riskColor(TextView t,int risk){
        switch(risk){
            case 1:contrast(t,0xFF1B5E20);break;
            case 2:contrast(t,0xFF43A047);break;
            case 3:contrast(t,HAZ_YELLOW);break;
            case 4:contrast(t,0xFFEF6C00);break;
            case 5:contrast(t,STATE_CRITICAL);break;
            default:contrast(t,HAZ_GRAY_DARK);break;
        }
    }
    public static String stars(int confidence){return confidence<=0?"—":confidence==1?"★☆☆":confidence==2?"★★☆":"★★★";}
    public static String riskName(int risk){switch(risk){case 1:return"آمن جدًا";case 2:return"آمن";case 3:return"تنبيه";case 4:return"خطر";case 5:return"حرج";default:return"غير متاح";}}
    public static View divider(Context c){View v=new View(c);v.setBackgroundColor(HAZ_GRAY);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return v;}
}
