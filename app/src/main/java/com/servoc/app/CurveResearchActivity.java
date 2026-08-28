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

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.EnvironmentalResponseModel;
import com.servoc.app.chemical.LogPowerCurveFitter;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.SensorEvidenceRegistry;

import java.util.List;
import java.util.Locale;

/** Research-only workbench for provenance-aware MQ curves and environmental response models. */
public final class CurveResearchActivity extends Activity {
    private Spinner chemical;
    private EditText temperature, humidity;
    private TextView output;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(build());
        showCurves(0);
    }

    private View build(){
        LinearLayout root=box(); root.setPadding(dp(10),dp(8),dp(10),dp(10)); root.setBackgroundColor(0xFF0E1116);
        TextView title=text("HazRob • MQ CURVE RESEARCH"); title.setTypeface(Typeface.DEFAULT_BOLD); title.setTextSize(20); title.setGravity(Gravity.CENTER); root.addView(title,full());

        View controls=controls();
        View results=results();
        if(isWide()){
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            ScrollView a=new ScrollView(this);a.setFillViewport(true);a.addView(controls);
            ScrollView c=new ScrollView(this);c.setFillViewport(true);c.addView(results);
            LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,-1,0.85f);p1.setMargins(0,0,dp(5),0);
            LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,-1,1.35f);p2.setMargins(dp(5),0,0,0);
            row.addView(a,p1);row.addView(c,p2);root.addView(row,new LinearLayout.LayoutParams(-1,0,1f));return root;
        }
        root.addView(controls,new LinearLayout.LayoutParams(-1,-2));root.addView(results,new LinearLayout.LayoutParams(-1,-2));
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.addView(root);return s;
    }

    private View controls(){
        LinearLayout p=box();p.setPadding(dp(7),dp(7),dp(7),dp(7));p.setBackgroundColor(0xFF181D24);
        TextView info=text("Secondary research library: "+ResearchCurveRegistry.count()+" sensor/gas curve records.\nThese equations are kept separate from the primary engine so provenance and disagreement can be audited before activation.");
        info.setTextColor(0xFF90CAF9);p.addView(info,full());

        String[] names=new String[ChemicalCatalog.COUNT];
        for(int i=0;i<names.length;i++)names[i]=String.format(Locale.US,"%03d • %s",i+1,ChemicalCatalog.NAMES[i]);
        chemical=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);chemical.setAdapter(ad);
        chemical.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){showCurves(pos);}public void onNothingSelected(AdapterView<?> p){}});
        p.addView(text("Catalogue chemical"),full());p.addView(chemical,full());

        LinearLayout env=new LinearLayout(this);env.setOrientation(LinearLayout.HORIZONTAL);
        temperature=input("25");humidity=input("50");
        env.addView(temperature,new LinearLayout.LayoutParams(0,-2,1));env.addView(humidity,new LinearLayout.LayoutParams(0,-2,1));
        p.addView(text("Research environment: temperature °C / RH %"),full());p.addView(env,full());
        Button factor=button("Show environmental response factors");factor.setOnClickListener(v->showEnvironment());p.addView(factor,full());
        Button fit=button("Verify log-log curve fitter");fit.setOnClickListener(v->verifyFitter());p.addView(fit,full());

        TextView warning=text("Do not use secondary fits or environmental factors for safety alarms until they are traced to the exact sensor revision and validated with the actual HazRob sensor channel.");warning.setTextColor(0xFFFFCC80);p.addView(warning,full());
        return p;
    }

    private View results(){
        LinearLayout p=box();p.setPadding(dp(7),dp(7),dp(7),dp(7));p.setBackgroundColor(0xFF151A20);
        TextView h=text("EQUATIONS / PROVENANCE");h.setTextColor(0xFF90CAF9);h.setTypeface(Typeface.DEFAULT_BOLD);p.addView(h,full());
        output=text("");output.setTextIsSelectable(true);output.setBackgroundColor(0xFF202832);p.addView(output,full());return p;
    }

    private void showCurves(int chem){
        if(output==null)return;
        List<ResearchCurveRegistry.Curve> curves=ResearchCurveRegistry.forChemical(chem);
        StringBuilder s=new StringBuilder();
        s.append(ChemicalCatalog.NAMES[chem]);if(!ChemicalCatalog.FORMULAS[chem].isEmpty())s.append(" • ").append(ChemicalCatalog.FORMULAS[chem]);
        s.append("\n\nResearch-source curves: ").append(curves.size()).append("\nSource: ").append(ResearchCurveRegistry.SOURCE);
        if(curves.isEmpty()){
            s.append("\n\nNo secondary graph-fit equation is available for this compound using the purchased array. This does not mean zero sensor response; it means HazRob must learn the multidimensional response experimentally rather than invent a single-sensor PPM equation.");
        }else{
            for(ResearchCurveRegistry.Curve c:curves){
                SensorEvidenceRegistry.SensorSpec sp=SensorEvidenceRegistry.byId(c.sensor);
                s.append("\n\n").append(sp==null?("Sensor "+c.sensor):sp.model)
                 .append("\nRs/R0 = ").append(f(c.ratioA)).append(" × ppm^").append(f4(c.ratioB))
                 .append("\nppm = ").append(f(c.inverseA())).append(" × (Rs/R0)^").append(f4(c.inverseB()))
                 .append("\nDeclared research range: ").append(f(c.minPpm)).append("–").append(f(c.maxPpm)).append(" ppm");
                if(!c.note.isEmpty())s.append("\nNote: ").append(c.note);
            }
        }
        output.setText(s.toString());
    }

    private void showEnvironment(){
        double t,rh;try{t=Double.parseDouble(temperature.getText().toString().trim());rh=Double.parseDouble(humidity.getText().toString().trim());}catch(Exception e){output.setText("Invalid temperature or humidity.");return;}
        StringBuilder s=new StringBuilder("EXPERIMENTAL TEMPERATURE / RH RESPONSE\nT=").append(f(t)).append(" °C • RH=").append(f(rh)).append(" %\n");
        for(SensorEvidenceRegistry.SensorSpec sp:SensorEvidenceRegistry.SENSORS){
            double q=EnvironmentalResponseModel.factor(sp.id,t,rh);
            s.append("\n").append(sp.model).append(": ").append(f(q));
            if(!EnvironmentalResponseModel.insideResearchDomain(sp.id,t,rh))s.append("  [outside fitted domain]");
        }
        s.append("\n\nThese values model the environmental Rs/R0 response surface digitized from source graphs. HazRob stores them as research features; automatic PPM correction remains disabled until reference normalization is validated.");
        output.setText(s.toString());
    }

    private void verifyFitter(){
        int chem=chemical.getSelectedItemPosition();if(chem<0)return;
        List<ResearchCurveRegistry.Curve> curves=ResearchCurveRegistry.forChemical(chem);
        if(curves.isEmpty()){output.setText("Select a chemical with a secondary curve to verify the fitter.");return;}
        ResearchCurveRegistry.Curve c=curves.get(0);
        double lo=Math.max(c.minPpm,0.001), hi=Math.max(lo*1.01,c.maxPpm);
        double[] pp={lo,Math.sqrt(lo*hi),hi};double[] rr=new double[pp.length];for(int i=0;i<pp.length;i++)rr[i]=c.ratioFromPpm(pp[i]);
        // Remove source-specific output scaling before fitting ratio=a*x^b.
        double[] internal=new double[pp.length];for(int i=0;i<pp.length;i++)internal[i]=pp[i]/c.outputScale;
        LogPowerCurveFitter.Fit x=LogPowerCurveFitter.fit(internal,rr);
        output.setText("LOG-LOG FITTER SELF-CHECK\n"+(SensorEvidenceRegistry.byId(c.sensor)==null?c.gasLabel:SensorEvidenceRegistry.byId(c.sensor).model+" / "+c.gasLabel)+
                "\n\nRecovered ratio model:\nRs/R0 = "+f(x.ratioA)+" × concentration^"+f4(x.ratioB)+
                "\nInverse model:\nconcentration = "+f(x.ppmA)+" × (Rs/R0)^"+f4(x.ppmB)+
                "\nR² = "+f4(x.r2)+"\nlog10 RMSE = "+f4(x.rmseLog10)+"\nPoints = "+x.points+
                "\n\nThe same fitter can be used later on digitized manufacturer-graph points or controlled calibration points, while retaining the fit error and source metadata.");
    }

    private boolean isWide(){Configuration c=getResources().getConfiguration();return c.orientation==Configuration.ORIENTATION_LANDSCAPE||c.screenWidthDp>=700;}
    private LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setPadding(dp(8),dp(8),dp(8),dp(8));return t;}
    private EditText input(String s){EditText e=new EditText(this);e.setText(s);e.setTextColor(Color.WHITE);e.setSingleLine(true);e.setBackgroundColor(0xFF232A33);e.setPadding(dp(8),dp(8),dp(8),dp(8));return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams full(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));return p;}
    private String f(double x){return Double.isFinite(x)?String.format(Locale.US,Math.abs(x)>=1000?"%.0f":Math.abs(x)>=10?"%.2f":"%.4f",x):"NA";}
    private String f4(double x){return Double.isFinite(x)?String.format(Locale.US,"%.4f",x):"NA";}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
