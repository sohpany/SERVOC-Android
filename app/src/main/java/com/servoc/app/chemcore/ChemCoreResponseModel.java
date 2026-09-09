package com.servoc.app.chemcore;

/** Forward sensor-response model used by the ChemCore inverse solver. */
public interface ChemCoreResponseModel {
    String sensorKey();
    String version();
    ChemCoreDecision.EvidenceTier evidenceTier();
    double minPpm();
    double maxPpm();

    final class Estimate {
        public final boolean available;
        public final double meanLog10Ratio;
        public final double sigmaLog10;
        public final boolean insideDomain;
        public final String note;
        public Estimate(boolean available,double meanLog10Ratio,double sigmaLog10,boolean insideDomain,String note){
            this.available=available;this.meanLog10Ratio=meanLog10Ratio;this.sigmaLog10=sigmaLog10;
            this.insideDomain=insideDomain;this.note=note==null?"":note;
        }
    }

    Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile);
}
