package com.servoc.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
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
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.exposure.ExposureLimits;
import com.servoc.app.fingerprint.ChemicalFingerprint;
import com.servoc.app.fingerprint.FingerprintClassifier;
import com.servoc.app.fingerprint.FingerprintTrainer;
import com.servoc.app.simulation.AnalyticalSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hardware-independent screen used to exercise the migrated analytical libraries. */
public final class AnalysisActivity extends Activity {
    private Spinner chemicals;
    private EditText ppm;
    private TextView result;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=box(); root.setPadding(dp(10),dp(10),dp(10),dp(18)); root.setBackgroundColor(0xFF101010);
        TextView title=text("SERVOC OFFLINE ANALYSIS"); title.setTextSize(20); title.setTypeface(Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER); root.addView(title,full());

        TextView info=text(LibraryStatus.summary()+"\nExposure records: "+ExposureLimits.entryCount());
        info.setBackgroundColor(0xFF1B1B1B); root.addView(info,full());

        String[] names=new String[GasEngine.SUPPORTED.length];
        for(int i=0;i<names.length;i++){
            int c=GasEngine.SUPPORTED[i];
            names[i]=ChemicalCatalog.NAMES[c]+(ChemicalCatalog.FORMULAS[c].isEmpty()?"":" • "+ChemicalCatalog.FORMULAS[c]);
        }
        chemicals=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); chemicals.setAdapter(adapter);
        root.addView(chemicals,full());

        ppm=new EditText(this); ppm.setText("50"); ppm.setTextColor(Color.WHITE); ppm.setSingleLine(true); ppm.setBackgroundColor(0xFF222222); ppm.setPadding(dp(8),dp(8),dp(8),dp(8));
        root.addView(text("Synthetic concentration (ppm)"),full()); root.addView(ppm,full());

        Button run=button("Run synthetic curve/PPM test"); run.setOnClickListener(v->runSimulation()); root.addView(run,full());
        Button fp=button("Run fingerprint self-test"); fp.setOnClickListener(v->runFingerprintTest()); root.addView(fp,full());

        result=text("This screen uses synthetic Rs/R0 values only. It allows development without the robot, but its output is not a real environmental measurement.");
        result.setBackgroundColor(0xFF1B1B1B); root.addView(result,full());

        TextView warning=text("Validation note: the current ESP32 source still contains placeholder R0 values. OEL/LEL output is a screening reference only until calibration and independent validation of the complete measurement chain.");
        warning.setTextColor(0xFFFFCC80); root.addView(warning,full());

        ScrollView scroll=new ScrollView(this); scroll.addView(root); setContentView(scroll);
    }

    private void runSimulation(){
        int p=chemicals.getSelectedItemPosition(); if(p<0)return; int chem=GasEngine.SUPPORTED[p];
        double target; try{target=Double.parseDouble(ppm.getText().toString().trim());}catch(Exception e){Toast.makeText(this,"Invalid ppm",Toast.LENGTH_SHORT).show();return;}
        if(!Double.isFinite(target)||target<=0){Toast.makeText(this,"ppm must be > 0",Toast.LENGTH_SHORT).show();return;}
        double[] ratios=AnalyticalSimulator.chemicalAtPpm(chem,target);
        List<GasEngine.Result> all=GasEngine.analyze(ratios);
        GasEngine.Result chosen=null; for(GasEngine.Result x:all)if(x.chem==chem){chosen=x;break;}
        if(chosen==null){result.setText("No curve result for selected chemical.");return;}
        StringBuilder s=new StringBuilder();
        s.append("SIMULATED INPUT\n").append(ChemicalCatalog.NAMES[chem]).append(" @ ").append(f(target)).append(" ppm")
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
        int p=chemicals.getSelectedItemPosition(); if(p<0)return; int chem=GasEngine.SUPPORTED[p];
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
        result.setText("FINGERPRINT SELF-TEST\n"+m.fingerprint.label+"\nScore: "+f(m.score)+" %\nDistance: "+f(m.distance)+"\nContributors: "+m.contributors+"\nTraining captures: "+m.fingerprint.sampleCount+"\n\nThis only verifies the classifier code path. Real signatures must be trained from controlled exposures.");
    }

    private String f(double x){return Double.isFinite(x)?String.format(Locale.US,x>=1000?"%.0f":x>=10?"%.1f":"%.2f",x):"NA";}
    private String lim(double x){return x>=0?f(x)+" ppm":"NA";}
    private String stars(int x){return x<=0?"-":x==1?"*":x==2?"**":"***";}
    private String risk(int x){switch(x){case 1:return"VERY SAFE";case 2:return"SAFE";case 3:return"ALERT";case 4:return"DANGER";case 5:return"CRITICAL";default:return"N/A";}}
    private LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setPadding(dp(8),dp(8),dp(8),dp(8));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams full(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
