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

    public static final String MQ8_H2_2026_ID="MARQUES_MQ8_H2_2026";
    public static final String MQ8_H2_2026_DOI="10.3390/app16104863";
    public static final String MQ7_CO_2022_ID="ALVEAR_MQ7_CO_2022";
    public static final String MQ7_CO_2022_DOI="10.3390/s22187015";

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
     * Highest-provenance exact-model response relations currently accepted for research identity/reference use.
     * No relation in this registry is HR069 M4 calibration; source domains are enforced and extrapolation is forbidden.
     */
    public static final Curve[] CURVES={
            c(GasEngine.MQ135,110,"Acetone",2.7295,-0.2822,10,100,0.9996,"Published Eq. 16"),
            c(GasEngine.MQ135,95,"Toluene",2.9630,-0.2838,10,100,0.9998,"Published Eq. 17"),
            c(GasEngine.MQ135,103,"Alcohol / ethanol mapping",3.8659,-0.3091,10,100,0.9996,"Published Eq. 18; source labels alcohol; HazRob legacy mapping uses ethanol for comparison"),
            c(GasEngine.MQ135,23,"Carbon dioxide",4.9999,-0.3387,10,100,0.9998,"Published Eq. 19; cross-response prior, not a selective CO2 instrument"),
            c(GasEngine.MQ135,21,"Ammonia",6.4835,-0.4060,10,100,0.9996,"Published Eq. 20"),
            c(GasEngine.MQ135,20,"Carbon monoxide",5.2629,-0.2668,10,100,0.9992,"Published Eq. 21; cross-response prior"),

            // Marques et al. (2026), Applied Sciences: C(ppm)=1000*(Rs/R0)^(-0.68).
            // Algebraically: Rs/R0 = 25808.6154 * ppm^(-1.470588...). Source table spans 200-10000 ppm H2.
            cSource(GasEngine.MQ8,75,"Hydrogen",25808.615404180728,-1.4705882352941175,
                    200,10000,Double.NaN,MQ8_H2_2026_ID,MQ8_H2_2026_DOI,
                    "Peer-reviewed MQ8 H2 Eq. 16 converted algebraically to ratio=a*ppm^b; source also reports experimental validation; approximate reference curve, non-M4"),

            // Alvear-Puertas et al. (2022), Sensors: CO(ppm)=10^((log10(17.5)-log10(Rs/R0))/0.63).
            // Therefore Rs/R0 = 17.5 * ppm^(-0.63), using the published MQ7 conversion relation.
            cSource(GasEngine.MQ7,20,"Carbon monoxide",17.5,-0.63,
                    20,2000,Double.NaN,MQ7_CO_2022_ID,MQ7_CO_2022_DOI,
                    "Peer-reviewed MQ7 CO conversion curve; exact-model reference relation inside published 20-2000 ppm domain; heater-cycle validity still required; non-M4")
    };

    private static Curve c(int sensor,int chemical,String label,double a,double b,double min,double max,double r2,String note){
        return new Curve(sensor,chemical,label,a,b,min,max,r2,UMBU_2023_ID,UMBU_2023_DOI,PEER_REVIEWED_DATASHEET_GRAPH_FIT,note);
    }

    private static Curve cSource(int sensor,int chemical,String label,double a,double b,double min,double max,double r2,
                                 String sourceId,String doi,String note){
        return new Curve(sensor,chemical,label,a,b,min,max,r2,sourceId,doi,PEER_REVIEWED_DATASHEET_GRAPH_FIT,note);
    }

    public static int count(){return CURVES.length;}
    public static boolean hasCurve(int sensor,int chemical){for(Curve c:CURVES)if(c.sensor==sensor&&c.chemical==chemical)return true;return false;}
    public static boolean hasAnyCurve(int chemical){for(Curve c:CURVES)if(c.chemical==chemical)return true;return false;}
    public static List<Curve> forChemical(int chemical){List<Curve> out=new ArrayList<>();for(Curve c:CURVES)if(c.chemical==chemical)out.add(c);return out;}
}
