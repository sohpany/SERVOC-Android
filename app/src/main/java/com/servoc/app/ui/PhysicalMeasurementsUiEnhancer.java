package com.servoc.app.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Responsive UI adapter for the physical-measurements table.
 *
 * The original four-column table allowed Android to shrink every column, which made Arabic labels
 * and engineering units wrap character-by-character on narrow phones. This enhancer applies fixed
 * proportional column weights and restores the MAX4466 dB SPL value from the raw API4 payload when
 * an older normalization layer has omitted db_spl.
 */
public final class PhysicalMeasurementsUiEnhancer implements Runnable {
    private static final String TAG="hazrob_physical_measurements_enhancer";
    private static final long REFRESH_MS=250L;
    private static final float[] WEIGHTS={0.30f,0.18f,0.16f,0.36f};
    private static final int NOISE_ROW=6; // header row + PhysicalCatalog index 5 (Noise)

    private final Activity activity;
    private final HazRobHost host;
    private final View root;
    private boolean running=true;

    private PhysicalMeasurementsUiEnhancer(Activity activity,HazRobHost host,View root){
        this.activity=activity;
        this.host=host;
        this.root=root;
    }

    public static void install(Activity activity){
        if(!(activity instanceof HazRobHost))return;
        View root=activity.findViewById(android.R.id.content);
        if(root==null||root.getTag()==TAG)return;
        root.setTag(TAG);
        PhysicalMeasurementsUiEnhancer e=new PhysicalMeasurementsUiEnhancer(activity,(HazRobHost)activity,root);
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(View v){e.running=true;e.scheduleNow();}
            @Override public void onViewDetachedFromWindow(View v){e.running=false;v.removeCallbacks(e);}
        });
        e.scheduleNow();
    }

    private void scheduleNow(){root.removeCallbacks(this);root.post(this);}

    @Override public void run(){
        if(!running||!root.isAttachedToWindow())return;
        TableLayout table=findPhysicalTable(root);
        if(table!=null){
            applyResponsiveColumns(table);
            updateNoise(table);
        }
        root.postDelayed(this,REFRESH_MS);
    }

    private TableLayout findPhysicalTable(View v){
        if(v instanceof TableLayout){
            TableLayout t=(TableLayout)v;
            if(isPhysicalTable(t))return t;
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                TableLayout found=findPhysicalTable(g.getChildAt(i));
                if(found!=null)return found;
            }
        }
        return null;
    }

    private boolean isPhysicalTable(TableLayout table){
        if(table.getChildCount()<2||!(table.getChildAt(0) instanceof TableRow))return false;
        TableRow h=(TableRow)table.getChildAt(0);
        if(h.getChildCount()<4)return false;
        CharSequence a=text(h.getChildAt(0)),b=text(h.getChildAt(1)),c=text(h.getChildAt(2));
        return "القياس".contentEquals(a)&&"القيمة".contentEquals(b)&&"الوحدة".contentEquals(c);
    }

    private static CharSequence text(View v){return v instanceof TextView?((TextView)v).getText():"";}

    private void applyResponsiveColumns(TableLayout table){
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(false);
        for(int r=0;r<table.getChildCount();r++){
            View rv=table.getChildAt(r);
            if(!(rv instanceof TableRow))continue;
            TableRow row=(TableRow)rv;
            row.setWeightSum(1f);
            for(int i=0;i<Math.min(4,row.getChildCount());i++){
                View cell=row.getChildAt(i);
                cell.setLayoutParams(new TableRow.LayoutParams(0,TableRow.LayoutParams.WRAP_CONTENT,WEIGHTS[i]));
                if(cell instanceof TextView){
                    TextView tv=(TextView)cell;
                    tv.setSingleLine(false);
                    tv.setHorizontallyScrolling(false);
                    tv.setPadding(dp(6),dp(r==0?8:9),dp(6),dp(r==0?8:9));
                    if(r>0&&i==1){tv.setTextSize(14);tv.setTextColor(0xFFECEFF1);}
                    else if(r>0&&i==3)tv.setTextSize(10);
                }
            }
        }
    }

    private int dp(int v){return Math.round(v*activity.getResources().getDisplayMetrics().density);}

    private void updateNoise(TableLayout table){
        if(table.getChildCount()<=NOISE_ROW||!(table.getChildAt(NOISE_ROW) instanceof TableRow))return;
        TableRow row=(TableRow)table.getChildAt(NOISE_ROW);
        if(row.getChildCount()<4||!(row.getChildAt(1) instanceof TextView)||!(row.getChildAt(3) instanceof TextView))return;
        TextView value=(TextView)row.getChildAt(1),state=(TextView)row.getChildAt(3);
        JSONObject d=host.latestDiagnostics();
        JSONObject sound=d==null?null:d.optJSONObject("sound");
        double db=sound==null?Double.NaN:sound.optDouble("db_spl",Double.NaN);
        double gain=sound==null?Double.NaN:sound.optDouble("gain_reference",Double.NaN);
        if(!Double.isFinite(db)&&d!=null){
            JSONObject raw=d.optJSONObject("_data_api");
            JSONObject rs=raw==null?null:raw.optJSONObject("sound");
            if(rs!=null){
                db=rs.optDouble("db_spl",Double.NaN);
                gain=rs.optDouble("gain_reference",Double.NaN);
            }
        }
        if(Double.isFinite(db)){
            value.setText(String.format(Locale.US,"%.1f",db));
            value.setTextColor(Color.WHITE);
            String g=Double.isFinite(gain)?String.format(Locale.US,"%.0f×",gain):"25×";
            state.setText("MAX4466 • dB SPL تقديري • Gain "+g);
            state.setTextColor(0xFF81C784);
        }
    }
}
