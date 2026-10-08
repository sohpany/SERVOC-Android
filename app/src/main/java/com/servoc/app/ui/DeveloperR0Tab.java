package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** HR069-R1: developer-owned local editable clean-air R0, deliberately separate from ChemCore Stable. */
public final class DeveloperR0Tab {
    private final Context context;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final String[] keys=ManualR0Calibration.keys();
    private final EditText[] entry=new EditText[keys.length];
    private final TextView[] info=new TextView[keys.length];
    private final TextView summary;

    public DeveloperR0Tab(Context c,HazRobHost h){
        context=c;host=h;
        ManualR0Calibration.load(h.preferences());
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout root=UiKit.vbox(c);
        root.setPadding(UiKit.dp(c,10),UiKit.dp(c,8),UiKit.dp(c,10),UiKit.dp(c,20));
        root.setBackgroundColor(0xFF0E1116);scroll.addView(root);

        root.addView(UiKit.title(c,"معايرة R0 — ثمانية حساسات"),UiKit.full(c));
        TextView instructions=UiKit.small(c,
            "هذه معايرة هواء نظيف خاصة بروبوتك؛ يمكن تعديل R0 وحفظها دون إنترنت. أدخل القيمة المقاسة بالأوم لكل حساس، بعد ثبات تغذية الحساسات وإحمائها. " +
            "لا يتم اعتماد قراءة Rs على أنها R0 تلقائيًا. R0 وحدها لا تحقق معايرة PPM مرجعية. " +
            "الحفظ معطل أثناء التسجيل/التحليل النشط حتى لا تتغير المعايرة وسط الجلسة. " +
            "حفظ القيم يؤثر في التحليلات الجديدة فقط؛ جلسات القياس المحفوظة لا تُعدّل.");
        instructions.setTextColor(0xFFFFC107);root.addView(instructions,UiKit.full(c));

        for(int i=0;i<keys.length;i++){
            final int index=i;
            LinearLayout card=UiKit.card(c);
            card.addView(UiKit.title(c,keys[i].toUpperCase(Locale.US)),UiKit.full(c));
            entry[i]=UiKit.input(c,"");
            entry[i].setSingleLine(true);
            entry[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            card.addView(entry[i],UiKit.full(c));
            info[i]=UiKit.small(c,"");info[i].setTextColor(0xFFB0BEC5);
            card.addView(info[i],UiKit.full(c));
            LinearLayout actions=UiKit.hbox(c);
            Button save=UiKit.button(c,"حفظ R0");
            save.setOnClickListener(v->saveOne(index));
            actions.addView(save,UiKit.weight(1));
            Button reset=UiKit.button(c,"استعادة الأساس");
            reset.setOnClickListener(v->new AlertDialog.Builder(c)
                .setTitle("استعادة R0 السابقة")
                .setMessage("هل تريد حذف القيمة اليدوية لـ"+keys[index].toUpperCase(Locale.US)+" واستعادة خط أساس الهواء النظيف HR069؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("استعادة",(d,w)->resetOne(index)).show());
            actions.addView(reset,UiKit.weight(1));
            card.addView(actions,UiKit.full(c));root.addView(card,UiKit.full(c));
        }
        Button saveAll=UiKit.button(c,"حفظ جميع قيم R0 الثماني");
        saveAll.setOnClickListener(v->saveAll());root.addView(saveAll,UiKit.full(c));
        summary=UiKit.small(c,"");summary.setTextColor(Color.LTGRAY);
        root.addView(summary,UiKit.full(c));
        reloadFields();refresh();
    }

    public View view(){return scroll;}

    private double parse(int index){
        try{
            String raw=entry[index].getText().toString().trim().replace(',','.');
            double v=Double.parseDouble(raw);
            return Double.isFinite(v)&&v>0?v:Double.NaN;
        }catch(Exception e){return Double.NaN;}
    }
    private boolean calibrationChangeBlocked(){
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())){
            host.showToast("لا يمكن تغيير R0 أثناء القياس أو التحليل؛ أنهِ الجلسة أولًا");
            return true;
        }
        return false;
    }
    private void saveOne(int index){
        if(calibrationChangeBlocked())return;
        double value=parse(index);
        if(!Double.isFinite(value)){host.showToast("قيمة R0 غير صالحة — أدخل رقمًا موجبًا");return;}
        if(!ManualR0Calibration.save(host.preferences(),keys[index],value)){
            host.showToast("تعذّر حفظ R0 في الهاتف");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("تم تحديث R0 لـ "+keys[index].toUpperCase(Locale.US)+" — يسري على القياسات الجديدة");
    }
    private void saveAll(){
        if(calibrationChangeBlocked())return;
        Map<String,Double> values=new LinkedHashMap<>();
        for(int i=0;i<keys.length;i++){
            double v=parse(i);
            if(!Double.isFinite(v)){host.showToast("قيمة R0 غير صالحة للحساس "+keys[i]);entry[i].requestFocus();return;}
            values.put(keys[i],v);
        }
        if(!ManualR0Calibration.saveAll(host.preferences(),values)){
            host.showToast("فشل الحفظ — لم تتغير أي قيمة من R0");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("تم حفظ قيم R0 الثماني مع سجل مصدر وتوقيت معايرة واحد");
    }
    private void resetOne(int index){
        if(calibrationChangeBlocked())return;
        if(!ManualR0Calibration.reset(host.preferences(),keys[index])){
            host.showToast("تعذّر استعادة خط أساس R0");return;
        }
        reloadFields();host.refreshAllTabs();
        host.showToast("استعيد خط أساس الهواء النظيف لـ "+keys[index].toUpperCase(Locale.US));
    }
    private void reloadFields(){
        for(int i=0;i<keys.length;i++){
            double v=ManualR0Calibration.get(keys[i]);
            entry[i].setText(Double.isFinite(v)?String.format(Locale.US,"%.10f",v).replaceAll("0+$","").replaceAll("\\.$",""):"");
        }
        refresh();
    }
    public void refresh(){
        JSONObject d=host.latestDiagnostics();
        JSONObject mq=d==null?null:d.optJSONObject("mq");
        for(int i=0;i<keys.length;i++){
            String k=keys[i];JSONObject sample=mq==null?null:mq.optJSONObject(k);
            double rs=sample==null?Double.NaN:sample.optDouble("rs",Double.NaN);
            String rsLabel=Double.isFinite(rs)&&rs>0?String.format(Locale.US,"%.5f Ω",rs):"غير متاح";
            info[i].setText("Rs اللحظية: "+rsLabel+" • المصدر: "+ManualR0Calibration.getSource(k)+
                "\nآخر حفظ: "+ManualR0Calibration.getUtc(k)+" • جلسة: "+ManualR0Calibration.getSession(k));
        }
        summary.setText("R0 الحالية محفوظة على الهاتف ضمن إعدادات الجهاز ولا تُستبدل بواسطة ChemCore Stable. " +
            "الإدخال اليدوي لا يغيّر تقارير جلسات قديمة، ولا يثبت معايرة الغاز المرجعي.");
    }
}
