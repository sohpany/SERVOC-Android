package com.servoc.app.chemcore;

/**
 * Fail-closed environmental-domain policy for ChemCore response models.
 *
 * This class does not compensate MQ response. It only answers whether the measured environment is
 * demonstrably inside a model's declared T/RH domain. If a model declares a finite domain for an
 * environmental variable, the corresponding runtime measurement is required; missing data cannot
 * be treated as evidence that the session is inside domain.
 *
 * Numeric environmental compensation remains disabled until controlled HR069 experiments provide
 * sensor-specific coefficients. No generic literature coefficient is manufactured here.
 */
public final class ChemCoreEnvironmentDomainPolicy {
    public static final String VERSION="chemcore-environment-domain/1";
    public static final String SEMANTICS="declared_environment_domain_gate_no_generic_compensation";

    public static final class Result {
        public final boolean insideDomain;
        public final boolean temperatureRequired;
        public final boolean rhRequired;
        public final boolean missingRequiredMeasurement;
        public final String note;

        Result(boolean insideDomain,boolean temperatureRequired,boolean rhRequired,
               boolean missingRequiredMeasurement,String note){
            this.insideDomain=insideDomain;
            this.temperatureRequired=temperatureRequired;
            this.rhRequired=rhRequired;
            this.missingRequiredMeasurement=missingRequiredMeasurement;
            this.note=note==null?"":note;
        }
    }

    private ChemCoreEnvironmentDomainPolicy() {}

    public static Result evaluate(double minTempC,double maxTempC,double minRh,double maxRh,
                                  double temperatureC,double rh){
        boolean tRequired=declared(minTempC,maxTempC);
        boolean rhRequired=declared(minRh,maxRh);
        boolean missingT=tRequired&&!Double.isFinite(temperatureC);
        boolean missingRh=rhRequired&&!Double.isFinite(rh);
        if(missingT||missingRh){
            StringBuilder n=new StringBuilder("required environmental measurement unavailable:");
            if(missingT)n.append(" temperature");
            if(missingRh)n.append(" RH");
            return new Result(false,tRequired,rhRequired,true,n.toString());
        }
        if(tRequired&&(temperatureC<minTempC||temperatureC>maxTempC))
            return new Result(false,true,rhRequired,false,"temperature outside documented model domain");
        if(rhRequired&&(rh<minRh||rh>maxRh))
            return new Result(false,tRequired,true,false,"RH outside documented model domain");
        return new Result(true,tRequired,rhRequired,false,
                (tRequired||rhRequired)?"environment inside documented model domain":"no explicit T/RH domain declared by this response model");
    }

    private static boolean declared(double lo,double hi){
        return Double.isFinite(lo)&&Double.isFinite(hi)&&hi>=lo;
    }
}
