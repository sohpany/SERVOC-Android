package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.analytics.AnalyticsSnapshot;
import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.exposure.ExposureLimits;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ChemicalsTab {
    private final Context c;
    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout root;
    private final LinearLayout table;
    private final TextView details;
    private final TextView[] ppmCells=new TextView[ChemicalCatalog.COUNT];
    private final TextView[] confCells=new TextView[ChemicalCatalog.COUNT];
    private final TextView[] riskCells=new TextView[ChemicalCatalog.COUNT];
    private final LinearLayout[] rows=new LinearLayout[ChemicalCatalog.COUNT];
    private Map<Integer,ValidatedGasEngine.Result> latest=new HashMap<>();

    public ChemicalsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;
        scroll=new ScrollView(c);scroll.setFillViewport(true);
        root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,6),UiKit.dp(c,5),UiKit.dp(c,6),UiKit.dp(c,10));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"CHEMICALS / 144 LIBRARY"),UiKit.full(c));

        EditText search=UiKit.input(c,"");search.setHint("Filter by name or formula");search.setHintTextColor(0xFF78909C);root.addView(search,UiKit.full(c));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int co,int af){}public void onTextChanged(CharSequence s,int st,int b,int co){filter(s.toString());}public void afterTextChanged(android.text.Editable e){}});

        details=UiKit.text(c,"Tap a chemical row for evidence, limits and model notes.");details.setBackgroundColor(0xFF151A20);details.setTextIsSelectable(true);root.addView(details,UiKit.full(c));

        table=UiKit.vbox(c);root.addView(table,UiKit.full(c));
        table.addView(header(),UiKit.full(c));
        for(int i=0;i<ChemicalCatalog.COUNT;i++){rows[i]=row(i);table.addView(rows[i],UiKit.full(c));}
        root.addView(UiKit.small(c,"Risk colors: green=very safe, light green=safe, amber=alert, orange=danger, red=critical. Gray means no validated quantitative risk interpretation."),UiKit.full(c));
    }

    public View view(){return scroll;}

    private LinearLayout header(){
        LinearLayout r=UiKit.hbox(c);r.setBackgroundColor(0xFF263238);
        addCell(r,"Chemical",2.8f,true);addCell(r,"Formula",1.1f,true);addCell(r,"PPM",1f,true);addCell(r,"Confidence",1.15f,true);addCell(r,"Risk",1.15f,true);return r;
    }

    private LinearLayout row(int chem){
        LinearLayout r=UiKit.hbox(c);r.setBackgroundColor((chem&1)==0?0xFF151B22:0xFF1B222A);
        TextView name=addCell(r,String.format(Locale.US,"%03d  %s",chem+1,ChemicalCatalog.NAMES[chem]),2.8f,false);
        addCell(r,ChemicalCatalog.FORMULAS[chem].isEmpty()?"—":ChemicalCatalog.FORMULAS[chem],1.1f,false);
        ppmCells[chem]=addCell(r,"—",1f,false);confCells[chem]=addCell(r,"—",1.15f,false);riskCells[chem]=addCell(r,"N/A",1.15f,false);UiKit.riskColor(riskCells[chem],0);
        View.OnClickListener click=v->showDetails(chem);r.setOnClickListener(click);name.setOnClickListener(click);return r;
    }

    private TextView addCell(LinearLayout row,String s,float w,boolean bold){
        TextView t=UiKit.text(c,s);t.setTextSize(11);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);t.setMaxLines(2);row.addView(t,new LinearLayout.LayoutParams(0,-2,w));return t;
    }

    public void refresh(){
        latest=AnalyticsSnapshot.analyze(host.latestDiagnostics());
        for(int chem=0;chem<ChemicalCatalog.COUNT;chem++){
            ValidatedGasEngine.Result r=latest.get(chem);
            if(r==null){ppmCells[chem].setText("—");confCells[chem].setText("—");riskCells[chem].setText("N/A");UiKit.riskColor(riskCells[chem],0);continue;}
            ppmCells[chem].setText(fmt(r.ppm));confCells[chem].setText(UiKit.stars(r.confidence));
            int risk=r.quantitative?r.risk:0;riskCells[chem].setText(risk>0?UiKit.riskName(risk):"PROVISIONAL");UiKit.riskColor(riskCells[chem],risk);
        }
    }

    private void showDetails(int chem){
        ChemicalCoverage.Status cov=ChemicalCoverage.forChemical(chem);ValidatedGasEngine.Result r=latest.get(chem);
        StringBuilder s=new StringBuilder(ChemicalCatalog.NAMES[chem]);
        if(!ChemicalCatalog.FORMULAS[chem].isEmpty())s.append(" • ").append(ChemicalCatalog.FORMULAS[chem]);
        s.append("\nCoverage: ").append(cov.method).append("\n").append(cov.note);
        if(r!=null)s.append("\n\nCurrent model estimate: ").append(fmt(r.ppm)).append(" ppm")
                .append("\nAgreement: ").append(fmt(r.agreement)).append(" %")
                .append("\nConfidence: ").append(UiKit.stars(r.confidence))
                .append("\nContributors: ").append(r.totalContributors).append(" (primary ").append(r.primaryContributors).append(")")
                .append("\nQuantitative safety use: ").append(r.quantitative?"candidate / calibration-dependent":"DISABLED")
                .append("\n").append(r.note);
        if(ExposureLimits.hasData(chem))s.append("\n\nTWA: ").append(lim(ExposureLimits.twa(chem)))
                .append("\nSTEL: ").append(lim(ExposureLimits.stel(chem)))
                .append("\nCeiling: ").append(lim(ExposureLimits.ceiling(chem)))
                .append("\nIDLH: ").append(lim(ExposureLimits.idlh(chem)))
                .append("\nLEL: ").append(ExposureLimits.lelPercent(chem)>=0?fmt(ExposureLimits.lelPercent(chem))+" %vol":"N/A");
        details.setText(s.toString());
    }

    private void filter(String q){
        String x=q==null?"":q.trim().toLowerCase(Locale.US);
        for(int i=0;i<rows.length;i++){
            String hay=(ChemicalCatalog.NAMES[i]+" "+ChemicalCatalog.FORMULAS[i]).toLowerCase(Locale.US);
            rows[i].setVisibility(x.isEmpty()||hay.contains(x)?View.VISIBLE:View.GONE);
        }
    }

    private String fmt(double x){return Double.isFinite(x)?String.format(Locale.US,x>=1000?"%.0f":x>=10?"%.1f":"%.2f",x):"—";}
    private String lim(double x){return x>=0?fmt(x)+" ppm":"N/A";}
}
