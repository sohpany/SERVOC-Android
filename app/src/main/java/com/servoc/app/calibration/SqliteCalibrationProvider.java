package com.servoc.app.calibration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;

import java.util.ArrayList;
import java.util.List;

/** SQLite-backed provider of the currently active device-specific MQ calibration. */
public final class SqliteCalibrationProvider implements CalibrationCurveProvider {
    private final ExperimentsDb db;

    public SqliteCalibrationProvider(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        this.db=db;
    }

    @Override public int[] chemicalIds(){
        SQLiteDatabase r=db.getReadableDatabase();List<Integer> ids=new ArrayList<>();
        try(Cursor c=r.rawQuery("SELECT DISTINCT chemical_id FROM calibration_curves WHERE active=1 ORDER BY chemical_id",null)){
            while(c.moveToNext())ids.add(c.getInt(0));
        }
        int[] out=new int[ids.size()];for(int i=0;i<out.length;i++)out[i]=ids.get(i);return out;
    }

    @Override public Estimate estimate(int sensorId,int chemicalId,double rsOverR0){
        SQLiteDatabase r=db.getReadableDatabase();
        long curveId=-1;
        String version="",referenceGas="",source="";
        try(Cursor c=r.rawQuery(
                "SELECT id,version,reference_gas,source_note FROM calibration_curves WHERE sensor_id=? AND chemical_id=? AND active=1 ORDER BY id DESC LIMIT 1",
                new String[]{Integer.toString(sensorId),Integer.toString(chemicalId)})){
            if(!c.moveToFirst())return null;
            curveId=c.getLong(0);
            version=c.isNull(1)?"":c.getString(1);
            referenceGas=c.isNull(2)?"":c.getString(2);
            source=c.isNull(3)?"":c.getString(3);
        }

        List<Double> ppms=new ArrayList<>(),ratios=new ArrayList<>();
        try(Cursor c=r.rawQuery(
                "SELECT ppm,ratio FROM calibration_points WHERE curve_id=? ORDER BY seq",
                new String[]{Long.toString(curveId)})){
            while(c.moveToNext()){
                ppms.add(c.getDouble(0));ratios.add(c.getDouble(1));
            }
        }
        if(ppms.size()<2)return new Estimate(Double.NaN,false,false,version,"Active calibration has fewer than two points.");
        double[] p=new double[ppms.size()],q=new double[ratios.size()];
        for(int i=0;i<p.length;i++){p[i]=ppms.get(i);q[i]=ratios.get(i);}

        try{
            EmpiricalMqCurve curve=new EmpiricalMqCurve(sensorId,chemicalId,p,q,System.currentTimeMillis(),referenceGas,source);
            EmpiricalMqCurve.Estimate e=curve.estimatePpm(rsOverR0);
            boolean referenceValidated=referenceGas!=null&&!referenceGas.trim().isEmpty();
            return new Estimate(e.ppm,e.valid(),referenceValidated,version,
                    e.valid()?"Active SQLite calibration; interpolation only.":"Active calibration cannot estimate this ratio without extrapolation.");
        }catch(IllegalArgumentException ex){
            return new Estimate(Double.NaN,false,false,version,"Stored calibration rejected: "+ex.getMessage());
        }
    }
}
