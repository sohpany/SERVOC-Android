package com.servoc.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.data.ExperimentQueries;
import java.util.List;

public final class ExperimentsTab {
    private final Context c;private final HazRobHost host;private final ScrollView scroll;private final LinearLayout root;private final LinearLayout list;private final TextView summary;private final TextView details;

    public ExperimentsTab(Context c,HazRobHost host){
        this.c=c;this.host=host;scroll=new ScrollView(c);scroll.setFillViewport(true);root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"EXPERIMENTS / REPORTS"),UiKit.full(c));
        summary=UiKit.text(c,"Accepted experiments: 0");summary.setBackgroundColor(0xFF151A20);root.addView(summary,UiKit.full(c));

        LinearLayout export=UiKit.hbox(c);Button xlsx=UiKit.button(c,"EXPORT EXCEL (.xlsx)");xlsx.setOnClickListener(v->host.requestExport("xlsx"));export.addView(xlsx,UiKit.weight(1));Button pdf=UiKit.button(c,"EXPORT PDF");pdf.setOnClickListener(v->host.requestExport("pdf"));export.addView(pdf,UiKit.weight(1));root.addView(export,UiKit.full(c));
        details=UiKit.text(c,"Select an accepted measurement to inspect its stored telemetry and analytics.");details.setTextIsSelectable(true);details.setBackgroundColor(0xFF11171D);root.addView(details,UiKit.full(c));
        list=UiKit.vbox(c);root.addView(list,UiKit.full(c));
    }
    public View view(){return scroll;}

    public void refresh(){
        list.removeAllViews();List<ExperimentQueries.Record> rows=ExperimentQueries.recent(host.database(),100);summary.setText("Accepted experiments: "+host.database().count()+" • showing latest "+rows.size());
        if(rows.isEmpty()){list.addView(UiKit.small(c,"No accepted measurements yet. Use SENSORS → ACCEPT after a completed measurement cycle."),UiKit.full(c));return;}
        for(ExperimentQueries.Record r:rows){
            Button b=UiKit.button(c,"#"+r.id+"   "+r.ts+"   "+(r.protocol==null?"":r.protocol));
            b.setOnClickListener(v->details.setText("EXPERIMENT #"+r.id+"\nTimestamp: "+r.ts+"\nProtocol: "+n(r.protocol)+"\n\nTELEMETRY\n"+n(r.payload)+"\n\nANALYTICS\n"+n(r.analytics)));
            list.addView(b,UiKit.full(c));
        }
    }
    private static String n(String s){return s==null?"":s;}
}
