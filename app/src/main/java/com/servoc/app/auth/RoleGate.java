package com.servoc.app.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Session-only UI role gate. The developer PIN is a workflow barrier, not a secret store. */
public final class RoleGate {
    private RoleGate(){}
    public interface Listener{void onUser();void onDeveloper();}
    public static final String DEVELOPER_PIN="1166";

    public static View build(Activity a,Listener listener){
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(a,28),dp(a,28),dp(a,28),dp(a,28));root.setBackgroundColor(0xFF0A0D11);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=new TextView(a);title.setText("HazRob");title.setTextSize(31);title.setTextColor(Color.WHITE);title.setGravity(Gravity.CENTER);title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);root.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView sub=new TextView(a);sub.setText("روبوت فحص وتحليل المخاطر عن بُعد\nاختر نمط الدخول");sub.setTextSize(15);sub.setTextColor(0xFFB0BEC5);sub.setGravity(Gravity.CENTER);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,dp(a,8),0,dp(a,28));root.addView(sub,sp);
        Button user=button(a,"مستخدم عادي");user.setOnClickListener(v->listener.onUser());root.addView(user,new LinearLayout.LayoutParams(-1,dp(a,54)));
        Button developer=button(a,"مطور");LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,dp(a,54));dp.setMargins(0,dp(a,12),0,0);root.addView(developer,dp);developer.setOnClickListener(v->askPin(a,listener));
        TextView note=new TextView(a);note.setText("أدوات التجارب والمعايرة والبنك المرجعي متاحة في وضع المطور فقط.");note.setTextColor(0xFF78909C);note.setTextSize(12);note.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.setMargins(0,dp(a,18),0,0);root.addView(note,np);
        return root;
    }

    private static void askPin(Activity a,Listener listener){
        EditText pin=new EditText(a);pin.setHint("PIN");pin.setGravity(Gravity.CENTER);pin.setTextColor(Color.WHITE);pin.setHintTextColor(0xFF78909C);pin.setSingleLine(true);pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);pin.setBackgroundColor(0xFF1B222B);pin.setPadding(dp(a,12),dp(a,10),dp(a,12),dp(a,10));
        LinearLayout box=new LinearLayout(a);box.setPadding(dp(a,22),dp(a,8),dp(a,22),0);box.addView(pin,new LinearLayout.LayoutParams(-1,-2));
        AlertDialog d=new AlertDialog.Builder(a).setTitle("دخول المطور").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("دخول",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(DEVELOPER_PIN.equals(pin.getText().toString().trim())){d.dismiss();listener.onDeveloper();}else{pin.setText("");Toast.makeText(a,"رمز المطور غير صحيح",Toast.LENGTH_SHORT).show();}}));
        d.show();
    }

    private static Button button(Activity a,String text){Button b=new Button(a);b.setText(text);b.setTextSize(17);b.setTextColor(Color.WHITE);b.setBackgroundColor(0xFF263238);return b;}
    private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
}
