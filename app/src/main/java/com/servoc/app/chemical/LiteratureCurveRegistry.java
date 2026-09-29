package com.servoc.app.chemical;

import java.util.ArrayList;
import java.util.List;

/**
 * Peer-reviewed external response-curve priors for sensors physically installed in HazRob.
 *
 * IMPORTANT: these are literature/data-sheet graph fits, not HR069 device-specific M4 calibration.
 * They can replace a lower-trust duplicate generic/community fit for the SAME sensor/analyte
 * inside the research inference path, but they can never set Result.quantitative=true by source alone.
 */
public final class LiteratureCurveRegistry {
    private LiteratureCurveRegistry() {}

    public static final String UMBU_2023_ID="UMBU_MQ135_2023";
    public static final String UMBU_2023_DOI="10.23960/jtaf.v11i2.314";
    public static final String UMBU_2023_TITLE="Analisis Grafik Karakteristik Sensitivitas Sensor MQ-135 untuk Menentukan Persamaan Hubungan antara ppm dan Rs/Ro";

    public static final int PEER_REVIEWED_DATASHEET_GRAPH_FIT=3;

    public static final class Curve {
        public final int sensor,chemical;
        public final String gasLabel,sourceId,doi;
        /** ratio = ratioA * ppm^ratioB */
        public final double ratioA,ratioB,minPpm,maxPpm,r2;
        public final int evidenceLevel;
        public final String note;

        Curve(int sensor,int chemical,String gasLabel,double ratioA,double ratioB,
              double minPpm,double maxPpm,double r2,String sourceId,String doi,
              int evidenceLevel,String note){
            this.sensor=sensor;this.chemical=chemical;this.gasLabel=gasLabel;
            this.ratioA=ratioA;this.ratioB=ratioB;this.minPpm=minPpm;this.maxPpm=maxPpm;this.r2=r2;
            this.sourceId=sourceId;this.doi=doi;this.evidenceLevel=evidenceLevel;this.note=note;
        }

        public double ppmFromRatio(double ratio){
            if(!Double.isFinite(ratio)||ratio<=0||ratioA<=0||ratioB==0)return Double.NaN;
            double ppm=Math.pow(ratio/ratioA,1.0/ratioB);
            return Double.isFinite(ppm)&&ppm>0?ppm:Double.NaN;
        }

        public double ratioFromPpm(double ppm){
            if(!Double.isFinite(ppm)||ppm<=0||ratioA<=0||ratioB==0)return Double.NaN;
            double ratio=ratioA*Math.pow(ppm,ratioB);
            return Double.isFinite(ratio)&&ratio>0?ratio:Double.NaN;
        }

        public boolean inDeclaredRange(double ppm){return Double.isFinite(ppm)&&ppm>=minPpm&&ppm<=maxPpm;}
    }

    /**
     * Umbu (2023) digitized ten points per gas from the MQ-135 typical sensitivity graph
     * and fitted power regressions in log-log space. The published analyzed interval is
     * 10-100 ppm. R2 values are retained to make the provenance/audit trail explicit.
     *
     * Where GasEngine/MQDataScience contain a duplicate MQ135/analyte equation, this
     * peer-reviewed fit is preferred INSIDE ITS DECLARED 10-100 ppm domain. Outside that
     * domain HazRob may fall back to lower-tier research evidence; it must not extrapolate
     * this peer-reviewed equation beyond the source data.
     */
    public static final Curve[] CURVES={
            c(GasEngine.MQ135,110,"Acetone",2.7295,-0.2822,10,100,0.9996,"Published Eq. 16"),
            c(GasEngine.MQ135,95,"Toluene",2.9630,-0.2838,10,100,0.9998,"Published Eq. 17"),
            c(GasEngine.MQ135,103,"Alcohol / ethanol mapping",3.8659,-0.3091,10,100,0.9996,"Published Eq. 18; source labels alcohol; HazRob legacy mapping uses ethanol for comparison"),
            c(GasEngine.MQ135,23,"Carbon dioxide",4.9999,-0.3387,10,100,0.9998,"Published Eq. 19; cross-response prior, not a selective CO2 instrument"),
            c(GasEngine.MQ135,21,"Ammonia",6.4835,-0.4060,10,100,0.9996,"Published Eq. 20"),
            c(GasEngine.MQ135,20,"Carbon monoxide",5.2629,-0.2668,10,100,0.9992,"Published Eq. 21; cross-response prior")
    };

    private static Curve c(int sensor,int chemical,String label,double a,double b,double min,double max,double r2,String note){
        return new Curve(sensor,chemical,label,a,b,min,max,r2,UMBU_2023_ID,UMBU_2023_DOI,PEER_REVIEWED_DATASHEET_GRAPH_FIT,note);
    }

    public static int count(){return CURVES.length;}
    public static boolean hasCurve(int sensor,int chemical){for(Curve c:CURVES)if(c.sensor==sensor&&c.chemical==chemical)return true;return false;}
    public static boolean hasAnyCurve(int chemical){for(Curve c:CURVES)if(c.chemical==chemical)return true;return false;}
    public static List<Curve> forChemical(int chemical){List<Curve> out=new ArrayList<>();for(Curve c:CURVES)if(c.chemical==chemical)out.add(c);return out;}
}
