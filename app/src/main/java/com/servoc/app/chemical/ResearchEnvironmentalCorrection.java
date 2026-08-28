package com.servoc.app.chemical;

/**
 * Optional research-only temperature/RH correction model adapted from
 * MQDataScience v6.0.0 (MIT), pinned to commit
 * e88f876140587a708ee84a4d4d3acbd5720274ee.
 *
 * The source model clamps out-of-domain inputs. HazRob is intentionally stricter:
 * it returns an invalid result outside the source domain so no silent extrapolation
 * is introduced into quantitative analysis.
 */
public final class ResearchEnvironmentalCorrection {
    private ResearchEnvironmentalCorrection() {}

    public static final String SOURCE =
            "abcdaaaaaaaaa/MQDataScience v6.0.0 @ e88f876140587a708ee84a4d4d3acbd5720274ee";

    public static final class Result {
        public final boolean valid;
        public final double factor;
        public final double minTempC, maxTempC, minRh, maxRh;
        public final String note;

        Result(boolean valid,double factor,double minTempC,double maxTempC,
               double minRh,double maxRh,String note){
            this.valid=valid; this.factor=factor;
            this.minTempC=minTempC; this.maxTempC=maxTempC;
            this.minRh=minRh; this.maxRh=maxRh; this.note=note;
        }
    }

    /**
     * Returns a research correction factor for only the eight MQ models installed
     * in HazRob. The factor is NOT applied automatically by the safety engine.
     */
    public static Result factor(int sensor,double tempC,double rh){
        if(!Double.isFinite(tempC)||!Double.isFinite(rh))
            return invalid("Non-finite temperature or humidity.",33,85);

        if(tempC < -10 || tempC > 50)
            return invalid("Outside MQDataScience temperature model domain; HazRob refuses clamping/extrapolation.",
                    sensor==GasEngine.MQ9?30:33,85);

        if(sensor==GasEngine.MQ9){
            if(rh<30 || rh>85)
                return invalid("Outside MQDataScience MQ-9 RH model domain.",30,85);
            double[] p = rh<=60
                    ? lerp3(rh,30,60,-2.5404,-0.0037,4.1019,-1.6874,-0.0043,3.0105)
                    : lerp3(rh,60,85,-1.6874,-0.0043,3.0105,-1.2399,-0.0047,2.3914);
            return ok(p[0]+p[2]*Math.exp(p[1]*tempC),30,85,
                    "MQ-9 piecewise RH correction from MQDataScience; research-only until validated against HazRob reference-gas data.");
        }

        if(rh<33 || rh>85)
            return invalid("Outside MQDataScience standard RH model domain.",33,85);

        final double a33,b33,c33,a85,b85,c85;
        switch(sensor){
            case GasEngine.MQ2:
            case GasEngine.MQ135:
                a33=0.8579; b33=-0.0543; c33=0.4912;
                a85=0.7818; b85=-0.0554; c85=0.4378;
                break;
            case GasEngine.MQ3:
                a33=0.7897; b33=-0.0423; c33=0.5355;
                a85=0.7319; b85=-0.0446; c85=0.4069;
                break;
            case GasEngine.MQ4:
                a33=0.8597; b33=-0.0381; c33=0.2861;
                a85=0.5838; b85=-0.0218; c85=0.4064;
                break;
            case GasEngine.MQ5:
                a33=0.8098; b33=-0.0413; c33=0.3686;
                a85=0.6066; b85=-0.0283; c85=0.3891;
                break;
            case GasEngine.MQ7:
                a33=0.8315; b33=-0.0462; c33=0.3813;
                a85=0.6708; b85=-0.0330; c85=0.3580;
                break;
            case GasEngine.MQ8:
                a33=0.8559; b33=-0.0611; c33=0.1673;
                a85=0.8201; b85=-0.0606; c85=0.1492;
                break;
            default:
                return invalid("No MQDataScience correction model registered for this installed sensor.",33,85);
        }

        double t=(rh-33.0)/(85.0-33.0);
        double a=lerp(a33,a85,t), b=lerp(b33,b85,t), c=lerp(c33,c85,t);
        return ok(a+c*Math.exp(b*tempC),33,85,
                "MQDataScience temperature/RH correction candidate; research-only until sensor-specific validation.");
    }

    private static double[] lerp3(double x,double x0,double x1,
                                  double a0,double b0,double c0,
                                  double a1,double b1,double c1){
        double t=(x-x0)/(x1-x0);
        return new double[]{lerp(a0,a1,t),lerp(b0,b1,t),lerp(c0,c1,t)};
    }

    private static double lerp(double a,double b,double t){return a+(b-a)*t;}

    private static Result ok(double f,double minRh,double maxRh,String note){
        boolean valid=Double.isFinite(f)&&f>0;
        return new Result(valid,valid?f:Double.NaN,-10,50,minRh,maxRh,
                valid?note:"Correction calculation produced an invalid factor.");
    }

    private static Result invalid(String note,double minRh,double maxRh){
        return new Result(false,Double.NaN,-10,50,minRh,maxRh,note);
    }
}
