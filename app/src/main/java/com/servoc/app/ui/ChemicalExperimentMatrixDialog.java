package com.servoc.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.servoc.app.chemical.ChemicalExperimentStatusRepository;
import com.servoc.app.chemical.ChemicalRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Searchable/sortable material chooser for developer experiments. */
public final class ChemicalExperimentMatrixDialog {
    public interface Listener { void onSelected(ChemicalRepository.Chemical chemical, ChemicalExperimentStatusRepository.Row status); }

    private final Context context;
    private final ChemicalExperimentStatusRepository repository;
    private final Listener listener;
    private final List<ChemicalExperimentStatusRepository.Row> rows=new ArrayList<>();
    private final MatrixAdapter adapter;
    private AlertDialog dialog;
    private String sortKey="priority";
    private boolean ascending=true;

    public ChemicalExperimentMatrixDialog(Context context,ChemicalExperimentStatusRepository repository,Listener listener){
        this.context=context;this.repository=repository;this.listener=listener;this.adapter=new MatrixAdapter();
    }

    public void show(){
        LinearLayout root=UiKit.vbox(context);root.setBackgroundColor(UiKit.NON_DRIVING_BG);root.setPadding(UiKit.dp(context,10),UiKit.dp(context,8),UiKit.dp(context,10),UiKit.dp(context,8));
        TextView source=UiKit.small(context,"حالة المواد من نسخة المكتبة المفعلة • Evidence revision: "+blank(repository.libraryRevision()));
        source.setTextColor(0xFF90A4AE);root.addView(source,UiKit.full(context));
        EditText search=UiKit.input(context,"");search.setHint("بحث لحظي بالاسم العربي/الإنجليزي أو CAS");search.setSingleLine(true);root.addView(search,UiKit.full(context));
        LinearLayout header=UiKit.hbox(context);
        Button material=header(context,"المادة",3f,"name");Button fp=header(context,"بصمة",1f,"fingerprint");Button ppm=header(context,"PPM",1f,"ppm");Button m4=header(context,"M4",1f,"m4");
        header.addView(material,UiKit.weight(3));header.addView(fp,UiKit.weight(1));header.addView(ppm,UiKit.weight(1));header.addView(m4,UiKit.weight(1));root.addView(header,UiKit.full(context));
        TextView legend=UiKit.small(context,"✓ معتمد/تشغيلي   ◐ دليل جزئي أو Transfer   — غير متوفر • اضغط عنوان العمود للفرز");legend.setTextColor(0xFF90A4AE);root.addView(legend,UiKit.full(context));
        ListView list=new ListView(context);list.setDividerHeight(1);list.setAdapter(adapter);root.addView(list,new LinearLayout.LayoutParams(-1,0,1f));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){reload(s==null?"":s.toString());}public void afterTextChanged(Editable e){}});
        list.setOnItemClickListener((p,v,pos,id)->{if(pos<0||pos>=rows.size())return;ChemicalExperimentStatusRepository.Row r=rows.get(pos);if(listener!=null)listener.onSelected(r.chemical,r);if(dialog!=null)dialog.dismiss();});
        dialog=new AlertDialog.Builder(context).setTitle("اختيار مادة للتجربة").setView(root).setNegativeButton("إغلاق",null).create();
        dialog.setOnShowListener(x->reload(""));dialog.show();
    }

    private Button header(Context c,String label,float weight,String key){Button b=UiKit.button(c,label);b.setTextSize(11);b.setOnClickListener(v->{if(sortKey.equals(key))ascending=!ascending;else{sortKey=key;ascending=true;}sort();adapter.notifyDataSetChanged();});return b;}
    private void reload(String query){rows.clear();rows.addAll(repository.search(query,5000));sort();adapter.notifyDataSetChanged();}
    private void sort(){
        Comparator<ChemicalExperimentStatusRepository.Row> c;
        switch(sortKey){
            case "fingerprint":c=Comparator.comparingInt(r->r.fingerprint.ordinal());break;
            case "ppm":c=Comparator.comparingInt(r->r.ppm.ordinal());break;
            case "m4":c=Comparator.comparingInt(r->r.m4?1:0);break;
            case "name":c=(a,b)->ChemicalExperimentStatusRepository.compareText(ChemicalExperimentStatusRepository.displayName(a.chemical),ChemicalExperimentStatusRepository.displayName(b.chemical));break;
            default:c=Comparator.comparingInt(ChemicalExperimentStatusRepository.Row::priority).thenComparing(r->ChemicalExperimentStatusRepository.displayName(r.chemical),ChemicalExperimentStatusRepository::compareText);break;
        }
        if(!ascending)c=c.reversed();Collections.sort(rows,c);
    }

    private final class MatrixAdapter extends BaseAdapter {
        @Override public int getCount(){return rows.size();}
        @Override public Object getItem(int p){return rows.get(p);}
        @Override public long getItemId(int p){return rows.get(p).chemical.uid;}
        @Override public View getView(int p,View convert,ViewGroup parent){
            ChemicalExperimentStatusRepository.Row r=rows.get(p);LinearLayout line=UiKit.hbox(context);line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(UiKit.dp(context,4),UiKit.dp(context,5),UiKit.dp(context,4),UiKit.dp(context,5));
            TextView name=cell(ChemicalExperimentStatusRepository.displayName(r.chemical),Gravity.START);name.setTextSize(12);line.addView(name,UiKit.weight(3));
            line.addView(mark(r.fingerprintMark(),r.fingerprint),UiKit.weight(1));line.addView(mark(r.ppmMark(),r.ppm),UiKit.weight(1));
            TextView m4=cell(r.m4Mark(),Gravity.CENTER);m4.setTextColor(r.m4?0xFF81C784:0xFF78909C);line.addView(m4,UiKit.weight(1));return line;
        }
    }

    private TextView mark(String text,ChemicalExperimentStatusRepository.Level level){TextView v=cell(text,Gravity.CENTER);v.setTextColor(level==ChemicalExperimentStatusRepository.Level.READY?0xFF81C784:level==ChemicalExperimentStatusRepository.Level.PARTIAL?0xFFFFB74D:0xFF78909C);return v;}
    private TextView cell(String text,int gravity){TextView v=UiKit.small(context,text);v.setGravity(gravity);v.setTextColor(Color.WHITE);return v;}
    private static String blank(String s){return s==null||s.trim().isEmpty()?"embedded/unknown":s;}
}
