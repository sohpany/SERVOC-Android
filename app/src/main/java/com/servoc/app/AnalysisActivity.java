package com.servoc.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.servoc.app.analytics.LibraryStatus;
import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.SensorEvidenceRegistry;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.exposure.ExposureLimits;
import com.servoc.app.fingerprint.ChemicalFingerprint;
import com.servoc.app.fingerprint.FingerprintClassifier;
import com.servoc.app.fingerprint.FingerprintTrainer;
import com.servoc.app.sensors.RobotTelemetry;
import com.servoc.app.simulation.AnalyticalSimulator;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Analytical workbench: offline models plus optional live ESP32 Rs/R0 input. */
public final class AnalysisActivity extends Activity {
    private Spinner chemicals;
    private EditText ppm;
    private TextView result;
    private TextView coverage;
    private double[] liveRatios;
    private int liveSensorCount=0;
    private boolean liveSessionReady=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        readLiveInput();
        setContentView(buildResponsiveUi());
        showCoverage(0);
    }

    private void readLiveInput(){
        String raw=getIntent()==null?null:getIntent().getStringExtra("live_diag");
        if(raw==null||raw.isEmpty())return;
        try{
            JSONObject d=new JSONObject(raw);
            liveRatios= RobotTelemetry.extractRatios(d);
            liveSensorCount=RobotTelemetry.availableMqCount(d);
            liveSessionReady=RobotTelemetry.measurementReady(d);
        }catch(Exception ignored){}
    }

    private View buildResponsiveUi(){
        LinearLayout root=box();
        root.setPadding(dp(10),dp(8),dp(10),dp(12));
        root.setBackgroundColor(0xFF0E1116);

        TextView title=text("HazRob • ANALYSIS LAB");
        title.setTextSize(20); title.setTypeface(Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        root.addView(title,full());

        if(isWide()){
            LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.HORIZONTAL);
            ScrollView leftScroll=new ScrollView(this); leftScroll.setFillViewport(true); leftScroll.addView(buildControls());
            ScrollView rightScroll=new ScrollView(this); rightScroll.setFillViewport(true); rightScroll.addView(buildResultPane());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f); lp.setMargins(0,0,dp(5),0);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1.15f); rp.setMargins(dp(5),0,0,0);
            body.addView(leftScroll,lp); body.addView(rightScroll,rp);
            root.addView(body,new LinearLayout.LayoutParams(-1,0,1f));
            return root;
        }

        root.addView(buildControls(),new LinearLayout.LayoutParams(-1,-2));
        root.addView(buildResultPane(),new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root);
        return scroll;
    }

    private View buildControls(){
        LinearLayout panel=box(); panel.setPadding(dp(7),dp(7),dp(7),dp(7)); panel.setBackgroundColor(0xFF181D24);

        TextView info=text(LibraryStatus.summary()+"\nExposure records: "+ExposureLimits.entryCount()+
                (liveSensorCount>0?"\nLive ESP32 channels reported: "+liveSensorCount+" • "+(liveSessionReady?"completed-session averages":"current live ratios"):"\nLive ESP32 input: none"));
        info.setBackgroundColor(0xFF202832); panel.addView(info,full());

        String[] names=new String[ChemicalCatalog.COUNT];
        for(int i=0;i<names.length;i++){
            names[i]=String.format(Locale.US,"%03d • %s%s",i+1,ChemicalCatalog.NAMES[i],
                    ChemicalCatalog.FORMULAS[i].isEmpty()?"":" • "+ChemicalCatalog.FORMULAS[i]);
        }
        chemicals=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); chemicals.setAdapter(adapter);
        chemicals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id){showCoverage(position);}
            @Override public void onNothingSelected(AdapterView<?> p){}
        });
        panel.addView(text("Chemical / compound"),full()); panel.addView(chemicals,full());

        coverage=text(""); coverage.setBackgroundColor(0xFF111821); panel.addView(coverage,full());

        ppm=new EditText(this); ppm.setText("50"); ppm.setTextColor(Color.WHITE); ppm.setSingleLine(true);
        ppm.setBackgroundColor(0xFF232A33); ppm.setPadding(dp(8),dp(8),dp(8),dp(8));
        panel.addView(text("Synthetic / known calibration concentration (ppm)"),full()); panel.addView(ppm,full());

        if(liveSensorCount>0){
            Button live=button("Analyze latest hardware Rs/R0 (provisional)"); live.setOnClickListener(v->runLiveAnalysis()); panel.addView(live,full());
        }
        Button run=button("Run direct curve / PPM self-test"); run.setOnClickListener(v->runSimulation()); panel.addView(run,full());
        Button fp=button("Run fingerprint engine self-test"); fp.setOnClickListener(v->runFingerprintTest()); panel.addView(fp,full());
        Button sensors=button("MQ array & protocols"); sensors.setOnClickListener(v->showSensorMatrix()); panel.addView(sensors,full());

        TextView warning=text("Live hardware values are accepted as acquisition data, but safety/occupational interpretation remains disabled until each sensor has a validated R0/reference-gas calibration and MQ-7/MQ-9 heater protocol is verified.");
        warning.setTextColor(0xFFFFCC80); panel.addView(warning,full());
        return panel;
    }

    private View buildResultPane(){
        LinearLayout pane=box(); pane.setPadding(dp(7),dp(7),dp(7),dp(7)); pane.setBackgroundColor(0xFF151A20);
        TextView h=text("RESULT / MODEL NOTES"); h.setTextColor(0xFF90CAF9); h.setTypeface(Typeface.DEFAULT_BOLD); pane.addView(h,full());
        result=text(liveSensorCount>0?"Live ESP32 acquisition is available. Select a chemical and use the provisional hardware analysis, or use synthetic tests to exercise the library.":
                "Select any of the 144 catalogue entries. HazRob will show whether a direct curve exists or whether the analyte belongs to the empirical electronic-nose training path.");
        result.setBackgroundColor(0xFF202832); result.setTextIsSelectable(true); pane.addView(result,full());
        TextView note=text("Real PPM is not considered validated in this prototype until sensor-specific calibration is completed. Raw/ADC/AO/Rs/Rs-R0 can still be tested end-to-end now.");
        note.setTextColor(0xFFB8C1CC); pane.addView(note,full());
        return pane;
    }

    private void showCoverage(int chem){
        if(coverage==null || chem<0 || chem>=ChemicalCatalog.COUNT) return;
        ChemicalCoverage.Status s=ChemicalCoverage.forChemical(chem);
        String label=s.level==ChemicalCoverage.DIRECT_CURVE?"DIRECT CURVE":
                s.level==ChemicalCoverage.FINGERPRINT_CANDIDATE?"FINGERPRINT / TRAINING CANDIDATE":
                s.level==ChemicalCoverage.AGGREGATE?"COMPOSITE METRIC":"UNVALIDATED / WEAK MQ EVIDENCE";
        coverage.setText(label+"\n"+s.method+"\n"+s.note);
        coverage.setTextColor(s.level==ChemicalCoverage.DIRECT_CURVE?0xFF81C784:
                s.level==ChemicalCoverage.FINGERPRINT_CANDIDATE?0xFF90CAF9:0xFFFFCC80);
    }

    private void runLiveAnalysis(){
        if(liveRatios==null){result.setText("No live sensor ratios were passed from the control screen.");return;}
        int chem=chemicals.getSelectedItemPosition(); if(chem<0)return;
        // Current ESP32 firmware does not prove the required heater-cycle protocol for MQ-7/MQ-9.
        boolean[] heaterValid={true,true,true,true,false,true,false,true};
        ValidatedGasEngine.Result r=ValidatedGasEngine.analyzeOne(liveRatios,chem,heaterValid);
        StringBuilder s=new StringBuilder("LIVE HARDWARE Rs/R0 REVIEW — PROVISIONAL\n")
                .append(ChemicalCatalog.NAMES[chem]).append("\n\nRatios received:\n");
        for(int i=0;i<liveRatios.length;i++)if(Double.isFinite(liveRatios[i]))s.append(RobotTelemetry.MQ_KEYS[i].toUpperCase(Locale.US)).append(" = ").append(f(liveRatios[i])).append('\n');
        if(r==null){
            s.append("\nNo eligible evidence-backed curve for this chemical using the currently valid channels.");
        }else{
            s.append("\nModel estimate: ").append(f(r.ppm)).append(" ppm")
             .append("\nAgreement: ").append(f(r.agreement)).append(" %")
             .append("\nContributors: ").append(r.totalContributors)
             .append("\nPrimary: ").append(r.primaryContributors)
             .append("  manufacturer-cross: ").append(r.manufacturerCrossContributors)
             .append("  community-cross: ").append(r.communityCrossContributors)
             .append("\nModel note: ").append(r.note);
        }
        s.append("\n\nSAFETY INTERPRETATION: DISABLED\nReason: the loaded ESP32 firmware currently uses default R0 values and does not expose a validated device-calibration flag. This result is for acquisition/model-path testing only.");
        result.setText(s.toString());
    }

    private void runSimulation(){
        int chem=chemicals.getSelectedItemPosition(); if(chem<0)return;
        ChemicalCoverage.Status cov=ChemicalCoverage.forChemical(chem);
        if(cov.level!=ChemicalCoverage.DIRECT_CURVE){
            result.setText(ChemicalCatalog.NAMES[chem]+"\n\nNo direct quantitative MQ curve is enabled.\n\nCurrent path: "+cov.method+"\n"+cov.note+
                    "\n\nFor this entry HazRob requires controlled training captures before it will estimate identity or concentration.");
            return;
        }
        double target; try{target=Double.parseDouble(ppm.getText().toString().trim());}catch(Exception e){Toast.makeText(this,"Invalid ppm",Toast.LENGTH_SHORT).show();return;}
        if(!Double.isFinite(target)||target<=0){Toast.makeText(this,"ppm must be > 0",Toast.LENGTH_SHORT).show();return;}
        double[] ratios=AnalyticalSimulator.chemicalAtPpm(chem,target);
        GasEngine.Result chosen=GasEngine.analyzeOne(ratios,chem);
        if(chosen==null){result.setText("No curve result for selected chemical.");return;}
        StringBuilder s=new StringBuilder();
        s.append("SYNTHETIC DIRECT-CURVE TEST\n").append(ChemicalCatalog.NAMES[chem]).append(" @ ").append(f(target)).append(" ppm")
         .append("\n\nRecovered PPM: ").append(f(chosen.ppm))
         .append("\nAgreement: ").append(f(chosen.agreement)).append(" %")
         .append("\nContributors: ").append(chosen.contributors)
         .append("\nConfidence: ").append(stars(chosen.confidence))
         .append("\nScreening risk: ").append(risk(chosen.risk));
        if(ExposureLimits.hasData(chem)){
            s.append("\n\nTWA: ").append(lim(ExposureLimits.twa(chem)))
             .append("\nSTEL: ").append(lim(ExposureLimits.stel(chem)))
             .append("\nCeiling: ").append(lim(ExposureLimits.ceiling(chem)))
             .append("\nIDLH: ").append(lim(ExposureLimits.idlh(chem)))
             .append("\nLEL: ").append(ExposureLimits.lelPercent(chem)>=0?f(ExposureLimits.lelPercent(chem))+" %vol":"NA");
        } else s.append("\n\nNo source-backed exposure entry for this chemical yet.");
        result.setText(s.toString());
    }

    private void runFingerprintTest(){
        int chem=chemicals.getSelectedItemPosition(); if(chem<0)return;
        if(ChemicalCoverage.forChemical(chem).level!=ChemicalCoverage.DIRECT_CURVE){
            result.setText("Fingerprint engine is ready for this catalogue entry, but synthetic generation is intentionally unavailable because there is no evidence-backed direct curve. Real fingerprint training requires measured captures from the MQ array.");
            return;
        }
        double target=50; try{target=Double.parseDouble(ppm.getText().toString().trim());}catch(Exception ignored){}
        List<double[]> samples=new ArrayList<>();
        for(int k=0;k<7;k++){
            double[] x=AnalyticalSimulator.chemicalAtPpm(chem,target*(0.97+k*0.01));
            for(int i=0;i<x.length;i++)if(Double.isFinite(x[i]))x[i]*=1.0+(k-3)*0.006*((i&1)==0?1:-1);
            samples.add(x);
        }
        ChemicalFingerprint model=FingerprintTrainer.train(chem,ChemicalCatalog.NAMES[chem],samples);
        List<ChemicalFingerprint> lib=new ArrayList<>(); lib.add(model);
        List<FingerprintClassifier.Match> matches=FingerprintClassifier.classify(AnalyticalSimulator.chemicalAtPpm(chem,target*1.02),lib);
        if(matches.isEmpty()){result.setText("Fingerprint self-test: no comparable channels.");return;}
        FingerprintClassifier.Match m=matches.get(0);
        result.setText("FINGERPRINT ENGINE SELF-TEST\n"+m.fingerprint.label+"\nScore: "+f(m.score)+" %\nDistance: "+f(m.distance)+"\nContributors: "+m.contributors+"\nTraining captures: "+m.fingerprint.sampleCount+"\n\nThis verifies the classifier code path only. Real signatures must come from controlled measured exposures.");
    }

    private void showSensorMatrix(){
        StringBuilder s=new StringBuilder("HAZROB MQ LIBRARY\n");
        for(SensorEvidenceRegistry.SensorSpec x:SensorEvidenceRegistry.SENSORS){
            s.append("\n").append(x.model).append("\nTarget/cross-response: ").append(x.targets)
             .append("\nRange: ").append(x.range)
             .append("\nHeater: ").append(x.heaterProtocol)
             .append("\nEvidence: ").append(x.source).append("\n");
        }
        s.append("\nThe application supports eight MQ model slots dynamically. The ESP32 firmware currently loaded/configured for direct ADC acquisition reports six physical channels; MQ-5 and MQ-9 remain library/future-MUX channels until added to firmware/hardware.");
        result.setText(s.toString());
    }

    private boolean isWide(){Configuration c=getResources().getConfiguration();return c.orientation==Configuration.ORIENTATION_LANDSCAPE||c.screenWidthDp>=700;}
    private String f(double x){return Double.isFinite(x)?String.format(Locale.US,x>=1000?"%.0f":x>=10?"%.1f":"%.3f",x):"NA";}
    private String lim(double x){return x>=0?f(x)+" ppm":"NA";}
    private String stars(int x){return x<=0?"-":x==1?"*":x==2?"**":"***";}
    private String risk(int x){switch(x){case 1:return"VERY SAFE";case 2:return"SAFE";case 3:return"ALERT";case 4:return"DANGER";case 5:return"CRITICAL";default:return"N/A";}}
    private LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setPadding(dp(8),dp(8),dp(8),dp(8));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams full(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
