package com.servoc.app.models;

import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads controlled reference captures and evaluates the native calibration candidates for one MQ channel. */
public final class CalibrationModelLab {
    private CalibrationModelLab(){}

    public static final class Report {
        public final String sensorKey;
        public final int samples;
        public final int concentrationLevels;
        public final List<CalibrationRegressionModels.Score> scores;
        Report(String sensorKey,int samples,int concentrationLevels,List<CalibrationRegressionModels.Score> scores){this.sensorKey=sensorKey;this.samples=samples;this.concentrationLevels=concentrationLevels;this.scores=scores;}
        public CalibrationRegressionModels.Score best(){return scores.isEmpty()?null:scores.get(0);}
    }

    public static Report evaluate(ExperimentsDb db,long chemicalUid,String sensorKey){
        List<Double> x=new ArrayList<>(),y=new ArrayList<>();Set<String> levels=new HashSet<>();
        String sql="SELECT known_ppm,sensor_payload FROM captures WHERE chemical_uid=? AND known_ppm IS NOT NULL AND known_ppm>0 ORDER BY id";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(chemicalUid)})){
            while(c.moveToNext()){
                double ppm=c.getDouble(0);String payload=c.getString(1);try{JSONObject root=new JSONObject(payload);JSONObject q=root.optJSONObject(sensorKey);if(q==null)continue;double ratio=q.optDouble("ratio",Double.NaN);if(!Double.isFinite(ratio)||ratio<=0||!Double.isFinite(ppm)||ppm<=0)continue;x.add(ratio);y.add(ppm);levels.add(String.format(java.util.Locale.US,"%.9g",ppm));}catch(Exception ignored){}
            }
        }
        double[] xx=new double[x.size()],yy=new double[y.size()];for(int i=0;i<xx.length;i++){xx[i]=x.get(i);yy[i]=y.get(i);}
        return new Report(sensorKey,xx.length,levels.size(),CalibrationRegressionModels.rank(xx,yy));
    }
}
