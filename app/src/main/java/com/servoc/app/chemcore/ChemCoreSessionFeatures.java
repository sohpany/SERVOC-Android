package com.servoc.app.chemcore;

import com.servoc.app.data.SessionSeries;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONObject;
import java.util.Arrays;

/** Bridges persisted HazRob session telemetry into the canonical ChemCore feature contract. */
public final class ChemCoreSessionFeatures {
    private ChemCoreSessionFeatures() {}

    public static final class Result {
        public final ChemCoreFeaturePipeline.Result features;
        public final double meanTemperatureC;
        public final double meanRh;
        public final double meanPressureHpa;
        public final int environmentSamples;
        public final String[] sensorKeys;

        Result(ChemCoreFeaturePipeline.Result features,double meanTemperatureC,double meanRh,double meanPressureHpa,int environmentSamples){
            this.features=features;this.meanTemperatureC=meanTemperatureC;this.meanRh=meanRh;this.meanPressureHpa=meanPressureHpa;
            this.environmentSamples=environmentSamples;this.sensorKeys=Arrays.copyOf(RobotTelemetry.MQ_KEYS,RobotTelemetry.MQ_KEYS.length);
        }
    }

    public static Result fromSeries(SessionSeries.Series series){
        if(series==null||series.samples.isEmpty())return new Result(ChemCoreFeaturePipeline.extract(null,new double[0][]),Double.NaN,Double.NaN,Double.NaN,0);
        int n=series.samples.size(),m=RobotTelemetry.MQ_KEYS.length;double[] t=new double[n];double[][] ratios=new double[n][m];
        for(double[] r:ratios)Arrays.fill(r,Double.NaN);
        double st=0,srh=0,sp=0;int nt=0,nrh=0,np=0;
        for(int i=0;i<n;i++){
            SessionSeries.Sample s=series.samples.get(i);t[i]=s.tMs/1000.0;
            for(int j=0;j<m;j++){
                JSONObject q=SessionSeries.sensor(s,RobotTelemetry.MQ_KEYS[j]);if(q==null)continue;
                boolean valid=q.optBoolean("valid",q.optBoolean("ok",true));double v=q.optDouble("ratio",Double.NaN);
                if(valid&&Double.isFinite(v)&&v>0)ratios[i][j]=v;
            }
            JSONObject e=s.env;if(e!=null){double v=e.optDouble("temperature",Double.NaN);if(Double.isFinite(v)){st+=v;nt++;}v=e.optDouble("humidity",Double.NaN);if(Double.isFinite(v)){srh+=v;nrh++;}v=e.optDouble("pressure",Double.NaN);if(Double.isFinite(v)){sp+=v;np++;}}
        }
        // Duplicate or non-monotonic timestamps can occur after transport/storage anomalies. The feature
        // pipeline will explicitly fall back to sample-index timing rather than fabricating elapsed time.
        ChemCoreFeaturePipeline.Result f=ChemCoreFeaturePipeline.extract(t,ratios);
        return new Result(f,nt>0?st/nt:Double.NaN,nrh>0?srh/nrh:Double.NaN,np>0?sp/np:Double.NaN,Math.max(nt,Math.max(nrh,np)));
    }
}
