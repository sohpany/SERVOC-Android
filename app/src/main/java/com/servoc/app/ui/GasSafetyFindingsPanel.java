package com.servoc.app.ui;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.exposure.GasHazardPriority;

import org.json.JSONArray;

/**
 * Safety-only list of previously accepted M4 gas findings.
 * Candidate identities are never passed into this component.
 */
final class GasSafetyFindingsPanel {
    private static final String PREF="gas_safety_sort";
    private final HazRobHost host;private final LinearLayout root;private final TextView list;private final Button hazard,concentration;
    private GasHazardPriority.Mode mode;private long renderedAt=0;

    GasSafetyFindingsPanel(Context c,HazRobHost host){
        this.host=host;this.mode=GasHazardPriority.parse(host.preferences().getString(PREF,"hazard"));root=UiKit.card(c);root.addView(UiKit.title(c,"قائمة النتائج الغازية المقبولة"),UiKit.full(c));root.addView(UiKit.small(c,"هذه قائمة سلامة لنتائج M4 الكمية المقبولة فقط. فرزها لا يغير ترتيب هوية ChemCore أو نتيجة التحليل العلمي."),UiKit.full(c));
        LinearLayout controls=UiKit.hbox(c);hazard=UiKit.button(c,"فرز: الخطر");concentration=UiKit.button(c,"فرز: التركيز");hazard.setOnClickListener(v->setMode(GasHazardPriority.Mode.HAZARD));concentration.setOnClickListener(v->setMode(GasHazardPriority.Mode.CONCENTRATION));controls.addView(hazard,UiKit.weight(1));controls.addView(concentration,UiKit.weight(1));root.addView(controls,UiKit.full(c));
        list=UiKit.text(c,"لا توجد نتائج غازية كمية M4 مقبولة حتى الآن.");list.setTextIsSelectable(true);list.setBackgroundColor(0xFF11171D);root.addView(list,UiKit.full(c));refreshNow();
    }

    View view(){return root;}
    void refresh(){long now=SystemClock.elapsedRealtime();if(now-renderedAt<2500)return;refreshNow();}
    void refreshNow(){
        renderedAt=SystemClock.elapsedRealtime();try{JSONArray rows=new ChemCoreDecisionRepository(host.database()).recentAcceptedGasFindings(20,mode);list.setText(ChemCoreAnalysisPresenter.gasFindings(rows,mode,20));}catch(Exception e){list.setText("تعذر تحميل قائمة نتائج السلامة: "+e.getClass().getSimpleName());}
        boolean h=mode==GasHazardPriority.Mode.HAZARD;hazard.setEnabled(!h);concentration.setEnabled(h);
    }
    private void setMode(GasHazardPriority.Mode m){mode=m==null?GasHazardPriority.Mode.HAZARD:m;host.preferences().edit().putString(PREF,mode==GasHazardPriority.Mode.HAZARD?"hazard":"concentration").apply();refreshNow();}
}
