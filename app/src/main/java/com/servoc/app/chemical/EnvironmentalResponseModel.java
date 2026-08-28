package com.servoc.app.chemical;

/**
 * Experimental temperature/RH response-surface models digitized/fitted by the MQDataScience project.
 * Model at a humidity anchor: correction = a + c*exp(b*T).
 * Parameters between humidity anchors are linearly interpolated.
 *
 * IMPORTANT: This class exposes the fitted factor for research and model features. It is not automatically
 * applied to safety-critical ppm values because the physical meaning/reference normalization must be
 * verified against the exact purchased sensor revision and its manufacturer chart.
 */
public final class EnvironmentalResponseModel {
    private EnvironmentalResponseModel() {}

    private static final class Anchor {
        final double rh,a,b,c;
        Anchor(double rh,double a,double b,double c){this.rh=rh;this.a=a;this.b=b;this.c=c;}
    }

    private static final class SensorEnv {
        final int sensor; final Anchor[] anchors;
        SensorEnv(int sensor,Anchor...anchors){this.sensor=sensor;this.anchors=anchors;}
    }

    private static Anchor a(double rh,double x,double y,double z){return new Anchor(rh,x,y,z);}

    private static final SensorEnv[] MODELS={
        new SensorEnv(GasEngine.MQ2,a(33,0.8579,-0.0543,0.4912),a(85,0.7818,-0.0554,0.4378)),
        new SensorEnv(GasEngine.MQ3,a(33,0.7897,-0.0423,0.5355),a(85,0.7319,-0.0446,0.4069)),
        new SensorEnv(GasEngine.MQ4,a(33,0.8597,-0.0381,0.2861),a(85,0.5838,-0.0218,0.4064)),
        new SensorEnv(GasEngine.MQ5,a(33,0.8098,-0.0413,0.3686),a(85,0.6066,-0.0283,0.3891)),
        new SensorEnv(GasEngine.MQ7,a(33,0.8315,-0.0462,0.3813),a(85,0.6708,-0.0330,0.3580)),
        new SensorEnv(GasEngine.MQ8,a(33,0.8559,-0.0611,0.1673),a(85,0.8201,-0.0606,0.1492)),
        new SensorEnv(GasEngine.MQ9,a(30,-2.5404,-0.0037,4.1019),a(60,-1.6874,-0.0043,3.0105),a(85,-1.2399,-0.0047,2.3914)),
        new SensorEnv(GasEngine.MQ135,a(33,0.8579,-0.0543,0.4912),a(85,0.7818,-0.0554,0.4378))
    };

    public static boolean hasModel(int sensor){return model(sensor)!=null;}

    public static double factor(int sensor,double tempC,double rh){
        SensorEnv m=model(sensor);
        if(m==null||!Double.isFinite(tempC)||!Double.isFinite(rh))return Double.NaN;
        Anchor[] q=m.anchors;
        if(q.length==1)return evaluate(q[0],tempC);
        if(rh<=q[0].rh)return evaluate(q[0],tempC);
        if(rh>=q[q.length-1].rh)return evaluate(q[q.length-1],tempC);
        for(int i=0;i<q.length-1;i++){
            if(rh>=q[i].rh&&rh<=q[i+1].rh){
                double t=(rh-q[i].rh)/(q[i+1].rh-q[i].rh);
                double aa=lerp(q[i].a,q[i+1].a,t);
                double bb=lerp(q[i].b,q[i+1].b,t);
                double cc=lerp(q[i].c,q[i+1].c,t);
                double v=aa+cc*Math.exp(bb*tempC);
                return Double.isFinite(v)?v:Double.NaN;
            }
        }
        return Double.NaN;
    }

    public static boolean insideResearchDomain(int sensor,double tempC,double rh){
        SensorEnv m=model(sensor); if(m==null)return false;
        return tempC>=-10&&tempC<=50&&rh>=m.anchors[0].rh&&rh<=m.anchors[m.anchors.length-1].rh;
    }

    private static double evaluate(Anchor x,double tempC){double v=x.a+x.c*Math.exp(x.b*tempC);return Double.isFinite(v)?v:Double.NaN;}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static SensorEnv model(int sensor){for(SensorEnv x:MODELS)if(x.sensor==sensor)return x;return null;}
}
