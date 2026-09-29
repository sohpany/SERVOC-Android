package com.servoc.app.chemical;

import java.util.ArrayList;
import java.util.List;

/**
 * Peer-reviewed exact-model response relations that are not power-law curves.
 * These are provisional literature/reference models only; they never constitute HR069 M4 calibration.
 */
public final class LinearLiteratureResponseRegistry {
    private LinearLiteratureResponseRegistry() {}

    public static final String ALVEAR_NOX_ID="ALVEAR_MQ135_NOX_2022";
    public static final String ALVEAR_NOX_DOI="10.3390/s22187015";

    public static final class Curve {
        public final int sensor,chemical;
        public final String gasLabel,sourceId,doi,note;
        /** ppm = ppmIntercept + ppmSlope * (Rs/R0). */
        public final double ppmIntercept,ppmSlope,minPpm,maxPpm;
        public final double refTempC,refRhPercent,refO2Percent,loadResistanceKohm;

        Curve(int sensor,int chemical,String gasLabel,double intercept,double slope,
              double minPpm,double maxPpm,double refTempC,double refRhPercent,double refO2Percent,
              double loadResistanceKohm,String sourceId,String doi,String note){
            this.sensor=sensor;this.chemical=chemical;this.gasLabel=gasLabel;
            this.ppmIntercept=intercept;this.ppmSlope=slope;this.minPpm=minPpm;this.maxPpm=maxPpm;
            this.refTempC=refTempC;this.refRhPercent=refRhPercent;this.refO2Percent=refO2Percent;
            this.loadResistanceKohm=loadResistanceKohm;this.sourceId=sourceId;this.doi=doi;this.note=note;
        }

        public double ratioFromPpm(double ppm){
            if(!Double.isFinite(ppm)||ppmSlope==0)return Double.NaN;
            double ratio=(ppm-ppmIntercept)/ppmSlope;
            return Double.isFinite(ratio)&&ratio>0?ratio:Double.NaN;
        }

        public double ppmFromRatio(double ratio){
            if(!Double.isFinite(ratio)||ratio<=0)return Double.NaN;
            double ppm=ppmIntercept+ppmSlope*ratio;
            return Double.isFinite(ppm)&&ppm>0?ppm:Double.NaN;
        }

        public boolean inDeclaredRange(double ppm){return Double.isFinite(ppm)&&ppm>=minPpm&&ppm<=maxPpm;}
    }

    public static final Curve[] CURVES={
            // Alvear-Puertas et al. (Sensors 2022), Eq. 3:
            // NOx(ppm)=156-133*(Rs/R0), reference conditions 20 C, 65% RH, 21% O2, RL=20 kOhm.
            new Curve(GasEngine.MQ135,141,"NOx",156.0,-133.0,
                    1.0,150.0,20.0,65.0,21.0,20.0,
                    ALVEAR_NOX_ID,ALVEAR_NOX_DOI,
                    "Peer-reviewed MQ135 NOx conversion relation; exact installed model. Reference environmental conditions are preserved; BME280 T/RH/P should be logged for transfer/uncertainty analysis. Non-M4.")
    };

    public static int count(){return CURVES.length;}
    public static boolean hasCurve(int sensor,int chemical){for(Curve c:CURVES)if(c.sensor==sensor&&c.chemical==chemical)return true;return false;}
    public static List<Curve> forChemical(int chemical){List<Curve> out=new ArrayList<>();for(Curve c:CURVES)if(c.chemical==chemical)out.add(c);return out;}
}
