package com.servoc.app.chemical;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary research curve registry for the eight purchased MQ sensors.
 * Coefficients are retained separately from GasEngine so disagreement can be audited instead of hidden.
 *
 * Model stored here: ratio = a * ppm^b ; inverse: ppm = (ratio/a)^(1/b).
 * Source: abcdaaaaaaaaa/MQDataScience (MIT), whose coefficients are derived from MQ response graphs.
 * These records are RESEARCH/COMPARISON data and are not enabled for safety alarms by default.
 */
public final class ResearchCurveRegistry {
    private ResearchCurveRegistry() {}

    public static final String SOURCE = "MQDataScience MIT / datasheet-derived graph fits; audit required";
    public static final int PROVENANCE_COMMUNITY_DATASHEET_FIT = 2;

    public static final class Curve {
        public final int sensor, chemical;
        public final String gasLabel;
        public final double ratioA, ratioB, minPpm, maxPpm, outputScale;
        public final String note;

        Curve(int sensor,int chemical,String gasLabel,double ratioA,double ratioB,double minPpm,double maxPpm,double outputScale,String note){
            this.sensor=sensor; this.chemical=chemical; this.gasLabel=gasLabel;
            this.ratioA=ratioA; this.ratioB=ratioB; this.minPpm=minPpm; this.maxPpm=maxPpm;
            this.outputScale=outputScale; this.note=note;
        }

        public double ppmFromRatio(double ratio){
            if(!Double.isFinite(ratio)||ratio<=0||ratioA<=0||ratioB==0) return Double.NaN;
            double raw=Math.pow(ratio/ratioA,1.0/ratioB);
            double ppm=raw*outputScale;
            return Double.isFinite(ppm)&&ppm>=0?ppm:Double.NaN;
        }

        public double ratioFromPpm(double ppm){
            if(!Double.isFinite(ppm)||ppm<=0||ratioA<=0||ratioB==0||outputScale<=0) return Double.NaN;
            double internal=ppm/outputScale;
            double ratio=ratioA*Math.pow(internal,ratioB);
            return Double.isFinite(ratio)&&ratio>0?ratio:Double.NaN;
        }

        public boolean inDeclaredRange(double ppm){return Double.isFinite(ppm)&&ppm>=minPpm&&ppm<=maxPpm;}

        public double inverseA(){return Math.pow(1.0/ratioA,1.0/ratioB)*outputScale;}
        public double inverseB(){return 1.0/ratioB;}
    }

    // Catalogue mapping: H2=75, LPG=135, propane=78, ethanol/alcohol=103, CH4=76,
    // smoke=139, CO=20, benzene=34, n-hexane=84, acetone=110, toluene=95,
    // CO2=23, ammonia=21.
    public static final Curve[] CURVES={
        c(GasEngine.MQ2,135,"LPG",17.6135,-0.4539,200,10000,1,"LPG aggregate"),
        c(GasEngine.MQ2,78,"propane",19.5575,-0.4610,200,10000,1,""),
        c(GasEngine.MQ2,75,"H2",25.7473,-0.4731,200,10000,1,""),
        c(GasEngine.MQ2,103,"alcohol",19.2641,-0.3604,200,10000,1,"Generic alcohol curve mapped to ethanol for comparison"),
        c(GasEngine.MQ2,76,"CH4",20.7074,-0.3600,200,10000,1,""),
        c(GasEngine.MQ2,139,"smoke",26.4698,-0.3876,200,10000,1,"Composite smoke response"),
        c(GasEngine.MQ2,20,"CO",28.0240,-0.3182,200,10000,1,""),

        c(GasEngine.MQ3,103,"Alcohol",0.5565,-0.6333,5.15,500,50,"Source model uses 50x conversion from its internal alcohol concentration axis"),
        c(GasEngine.MQ3,34,"Benzene",1.7957,-0.3690,5.15,500,50,"MQ-3 cross-response; research comparison only"),
        c(GasEngine.MQ3,84,"Hexane",22.9337,-0.3580,5.15,500,50,"MQ-3 cross-response; research comparison only"),
        c(GasEngine.MQ3,135,"LPG",27.8286,-0.2847,5.15,500,50,"Research comparison only"),
        c(GasEngine.MQ3,20,"CO",31.9559,-0.2325,5.15,500,50,"Research comparison only"),
        c(GasEngine.MQ3,76,"CH4",44.8389,-0.0476,5.15,500,50,"Very shallow response; weak quantitative discrimination"),

        c(GasEngine.MQ4,76,"CH4",11.5517,-0.3541,200,10000,1,""),
        c(GasEngine.MQ4,135,"LPG",14.1866,-0.3227,200,10000,1,""),
        c(GasEngine.MQ4,75,"H2",9.6432,-0.1765,200,10000,1,"Cross-response"),
        c(GasEngine.MQ4,139,"smoke",7.0500,-0.1065,200,10000,1,"Composite smoke response"),
        c(GasEngine.MQ4,103,"alcohol",5.7994,-0.0696,200,10000,1,"Shallow cross-response"),
        c(GasEngine.MQ4,20,"CO",5.4900,-0.0495,200,10000,1,"Extremely shallow cross-response"),

        c(GasEngine.MQ5,135,"LPG",6.1067,-0.4102,200,10000,1,""),
        c(GasEngine.MQ5,76,"CH4",6.9870,-0.3777,200,10000,1,""),
        c(GasEngine.MQ5,75,"H2",7.4265,-0.2847,200,10000,1,""),
        c(GasEngine.MQ5,103,"alcohol",10.8274,-0.2104,200,10000,1,"Cross-response"),
        c(GasEngine.MQ5,20,"CO",8.2236,-0.1492,200,10000,1,"Cross-response"),

        c(GasEngine.MQ7,75,"H2",21.1794,-0.7174,50,4000,1,"Research source range; verify against exact purchased revision"),
        c(GasEngine.MQ7,20,"CO",20.8900,-0.6568,50,4000,1,"Heater protocol is revision-dependent; verify MQ-7/MQ-7B module"),
        c(GasEngine.MQ7,135,"LPG",14.9745,-0.1329,50,4000,1,"Cross-response"),
        c(GasEngine.MQ7,76,"CH4",19.8094,-0.0900,50,4000,1,"Shallow cross-response"),
        c(GasEngine.MQ7,103,"Alcohol",20.2524,-0.0655,50,4000,1,"Shallow cross-response"),

        c(GasEngine.MQ8,75,"H2",18391.5667,-1.4494,200,10000,1,"Primary target; source range broader than some manufacturer specifications"),
        c(GasEngine.MQ8,103,"alcohol",468.8045,-0.5574,200,10000,1,"Cross-response"),
        c(GasEngine.MQ8,135,"LPG",152.1255,-0.2922,200,10000,1,"Cross-response"),
        c(GasEngine.MQ8,76,"CH4",117.9639,-0.1448,200,10000,1,"Cross-response"),
        c(GasEngine.MQ8,20,"CO",126.6050,-0.1173,200,10000,1,"Cross-response"),

        c(GasEngine.MQ9,20,"CO",16.8797,-0.4445,200,1000,1,"CO curve; verify heater phase and exact sensor revision"),
        c(GasEngine.MQ9,135,"LPG",24.9180,-0.4695,200,10000,1,""),
        c(GasEngine.MQ9,76,"CH4",21.8247,-0.3663,200,10000,1,""),

        c(GasEngine.MQ135,110,"Acetone",2.7988,-0.2913,10,200,1,"Research graph fit"),
        c(GasEngine.MQ135,95,"Toluene",2.9788,-0.2892,10,200,1,"Research graph fit"),
        c(GasEngine.MQ135,103,"Alcohol",3.8623,-0.3126,10,200,1,"Generic alcohol mapped to ethanol for comparison"),
        c(GasEngine.MQ135,23,"CO2",5.2278,-0.3585,10,200,1,"Cross-response model; MQ-135 is not a selective CO2 instrument"),
        c(GasEngine.MQ135,21,"NH4/NH3",6.2968,-0.3892,10,200,1,"Source labels NH4; mapped to ammonia catalogue entry for comparison only"),
        c(GasEngine.MQ135,20,"CO",5.0533,-0.2541,10,200,1,"Cross-response")
    };

    private static Curve c(int sensor,int chemical,String label,double a,double b,double min,double max,double scale,String note){
        return new Curve(sensor,chemical,label,a,b,min,max,scale,note);
    }

    public static int count(){return CURVES.length;}

    public static List<Curve> forChemical(int chemical){
        List<Curve> out=new ArrayList<>();
        for(Curve c:CURVES) if(c.chemical==chemical) out.add(c);
        return out;
    }

    public static List<Curve> forSensor(int sensor){
        List<Curve> out=new ArrayList<>();
        for(Curve c:CURVES) if(c.sensor==sensor) out.add(c);
        return out;
    }
}
