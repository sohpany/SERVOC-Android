package com.servoc.app.simulation;

import com.servoc.app.chemical.GasEngine;

/** Offline-only analytical simulator. Never represents a real environmental measurement. */
public final class AnalyticalSimulator {
    private AnalyticalSimulator() {}
    public static double[] chemicalAtPpm(int chemicalId,double ppm){
        double[] r=GasEngine.simulateRatios(chemicalId,ppm);
        for(int i=0;i<r.length;i++) if(Double.isFinite(r[i])) r[i]*=(1.0+((i%3)-1)*0.018);
        return r;
    }
}
