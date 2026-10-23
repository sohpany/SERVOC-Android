package com.servoc.app.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.servoc.app.calibration.MicrophoneDbCalibration;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Responsive UI adapter for the physical-measurements table.
 *
 * The original four-column table allowed Android to shrink every column, which made Arabic labels
 * and engineering units wrap character-by-character on narrow phones. This enhancer applies fixed
 * proportional column weights, keeps the unit column on one line, emphasizes the measured value,
 * and applies the operator's persisted one-point MAX4466 dB compensation when configured.
 *
 * HR069 deliberately does not assume a fixed MAX4466 analogue gain. The raw acoustic level shown
 * before compensation is ADC-relative dBFS derived from measured RMS voltage. Once the operator
 * stores a reference-meter offset in Settings, the displayed dB value is raw dBFS + offset.
 */
public final class PhysicalMeasurementsUiEnhancer implements Runnable {
    private static final String TAG="hazrob_physical_measurements_enhancer";
    private static final long REFRESH_MS=1000L;
    private static final double NOISE_EMA_ALPHA=0.35;
    private double displayDb=Double.NaN;
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
                if(!(cell instanceof TextView))continue;
                TextView tv=(TextView)cell;
                tv.setHorizontallyScrolling(false);
                tv.setPadding(dp(6),dp(r==0?8:9),dp(6),dp(r==0?8:9));
                if(r==0){
                    tv.setSingleLine(i==2);
                    tv.setGravity(Gravity.CENTER_VERTICAL|(i==1||i==2?Gravity.CENTER_HORIZONTAL:Gravity.START));
                    continue;
                }
                if(i==0){
                    tv.setSingleLine(false);
                    tv.setTextSize(11.5f);
                    tv.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
                }else if(i==1){
                    tv.setSingleLine(true);
                    tv.setTextSize(14);
                    tv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                    tv.setTextColor(0xFF263238);
                    tv.setGravity(Gravity.CENTER);
                }else if(i==2){
                    tv.setSingleLine(true);
                    tv.setTextSize(10.5f);
                    tv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                    tv.setGravity(Gravity.CENTER);
                    tv.setMinWidth(dp(44));
                }else{
                    tv.setSingleLine(false);
                    tv.setMaxLines(3);
                    tv.setTextSize(10);
                    tv.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
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
        if(sound==null&&d!=null){JSONObject raw=d.optJSONObject("_data_api");sound=raw==null?null:raw.optJSONObject("sound");}
        double rawDb=MicrophoneDbCalibration.rawDbFs(sound);
        displayDb=Double.isFinite(rawDb)?(Double.isFinite(displayDb)?displayDb+NOISE_EMA_ALPHA*(rawDb-displayDb):rawDb):Double.NaN;
        boolean configured=MicrophoneDbCalibration.configured(host.preferences());
        double offset=MicrophoneDbCalibration.offsetDb(host.preferences());
        if(Double.isFinite(rawDb)){
            value.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            if(configured){
                double corrected=MicrophoneDbCalibration.compensate(displayDb,offset);
                value.setText(String.format(Locale.US,"%.1f",corrected));
                value.setTextColor(0xFF1B5E20);
                state.setText("MAX4466 • معوض بمرجع خارجي • raw "+String.format(Locale.US,"%.1f dBFS",rawDb)+" • offset "+String.format(Locale.US,"%+.1f dB",offset));
                state.setTextColor(0xFF1B5E20);
            }else{
                value.setText(String.format(Locale.US,"%.1f",displayDb));
                value.setTextColor(0xFF955000);
                state.setText("MAX4466 • dBFS خام • تمهيد وتحديث كل ثانية • أدخل التعويض من الإعدادات");
                state.setTextColor(0xFF955000);
            }
        }else{
            value.setText("—");
            state.setText("MAX4466 • لا توجد قراءة RMS صالحة");
            state.setTextColor(0xFF955000);
        }
    }
}
