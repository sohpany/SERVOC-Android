package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.servoc.app.auth.RoleGate;

/** One PIN-protected developer workspace inside the unified HazRob application. */
public final class DeveloperTab {
    private final Context context;private final HazRobHost host;private final Runnable onUnlock;
    private final LinearLayout root;private final FrameLayout page;private final LinearLayout bar;
    private boolean unlocked=false;private int selected=0;
    private Button[] tabs;private ExperimentsTab experiments;private ChemCoreAnalysisTab chemCore;private SettingsTab robotSetup;private RobotConnectionSetupTab connectionSetup;private ChemCoreLibraryTab library;private View[] views;

    public DeveloperTab(Context c,HazRobHost host,Runnable onUnlock){
        context=c;this.host=host;this.onUnlock=onUnlock;
        root=UiKit.vbox(c);root.setBackgroundColor(0xFF0E1116);root.setPadding(UiKit.dp(c,6),UiKit.dp(c,6),UiKit.dp(c,6),UiKit.dp(c,10));
        bar=UiKit.hbox(c);root.addView(bar,UiKit.full(c));
        page=new FrameLayout(c);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        renderLocked();
    }

    public View view(){return root;}
    public boolean isUnlocked(){return unlocked;}
    public void requestUnlock(){if(unlocked)return;askPin();}

    private void renderLocked(){
        bar.removeAllViews();page.removeAllViews();
        LinearLayout box=UiKit.vbox(context);box.setGravity(Gravity.CENTER);box.setPadding(UiKit.dp(context,24),UiKit.dp(context,30),UiKit.dp(context,24),UiKit.dp(context,30));
        TextView title=UiKit.title(context,"المطور");title.setGravity(Gravity.CENTER);box.addView(title,UiKit.full(context));
        TextView note=UiKit.small(context,"مساحة التجارب، ChemCore، تهيئة الروبوت والاتصال وإدارة المكتبة العلمية.");note.setGravity(Gravity.CENTER);note.setTextColor(0xFFB0BEC5);box.addView(note,UiKit.full(context));
        Button open=UiKit.button(context,"دخول المطور");open.setOnClickListener(v->askPin());LinearLayout.LayoutParams p=UiKit.full(context);p.topMargin=UiKit.dp(context,18);box.addView(open,p);
        page.addView(box,new FrameLayout.LayoutParams(-1,-1));
    }

    private void askPin(){
        EditText pin=UiKit.input(context,"");pin.setHint("كلمة المرور");pin.setGravity(Gravity.CENTER);pin.setSingleLine(true);pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        LinearLayout box=UiKit.vbox(context);box.setPadding(UiKit.dp(context,20),UiKit.dp(context,8),UiKit.dp(context,20),0);box.addView(pin,UiKit.full(context));
        AlertDialog d=new AlertDialog.Builder(context).setTitle("دخول المطور").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("دخول",null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(RoleGate.DEVELOPER_PIN.equals(pin.getText().toString().trim())){d.dismiss();unlock();}else{pin.setText("");Toast.makeText(context,"كلمة المرور غير صحيحة",Toast.LENGTH_SHORT).show();}}));d.show();
    }

    private void unlock(){
        if(unlocked)return;unlocked=true;if(onUnlock!=null)onUnlock.run();
        experiments=new ExperimentsTab(context,host);chemCore=new ChemCoreAnalysisTab(context,host);robotSetup=new SettingsTab(context,host);connectionSetup=new RobotConnectionSetupTab(context,host);library=new ChemCoreLibraryTab(context,host);
        views=new View[]{experiments.view(),chemCore.view(),robotSetup.view(),connectionSetup.view(),library.view()};
        String[] names={"التجارب","ChemCore","الروبوت","الاتصال","المكتبة"};tabs=new Button[names.length];bar.removeAllViews();
        for(int i=0;i<names.length;i++){final int x=i;Button b=UiKit.button(context,names[i]);b.setTextSize(9);b.setOnClickListener(v->show(x));tabs[i]=b;bar.addView(b,UiKit.weight(1));}
        show(0);host.showToast("تم فتح تبويب المطور");
    }

    private void show(int index){if(!unlocked||views==null)return;selected=Math.max(0,Math.min(views.length-1,index));page.removeAllViews();page.addView(views[selected],new FrameLayout.LayoutParams(-1,-1));for(int i=0;i<tabs.length;i++){tabs[i].setTextColor(i==selected?Color.WHITE:Color.DKGRAY);tabs[i].setBackgroundColor(i==selected?0xFF263238:0xFFE0E0E0);}refresh();}

    public void refresh(){if(!unlocked)return;if(selected==0&&experiments!=null)experiments.refresh();else if(selected==1&&chemCore!=null)chemCore.refresh();else if(selected==2&&robotSetup!=null)robotSetup.refresh();else if(selected==3&&connectionSetup!=null)connectionSetup.refresh();else if(selected==4&&library!=null)library.refresh();}
}
