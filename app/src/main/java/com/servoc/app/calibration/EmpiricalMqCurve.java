package com.servoc.app.calibration;

import java.util.Arrays;

/**
 * Sensor-specific, analyte-specific calibration curve built from controlled exposures.
 * Interpolation is performed in log10(ppm)-log10(Rs/R0) space and extrapolation is
 * deliberately disabled. This permits HazRob to learn a usable quantitative model
 * for catalogue compounds when the installed MQ array produces a repeatable response,
 * without inventing a universal datasheet equation.
 */
public final class EmpiricalMqCurve {
    public static final int OK = 0;
    public static final int OUTSIDE_CALIBRATION_RANGE = 1;
    public static final int INVALID_INPUT = 2;
    public static final int NON_MONOTONIC_CALIBRATION = 3;

    public static final class Estimate {
        public final double ppm;
        public final int status;
        public final int lowerPoint;
        public final int upperPoint;

        Estimate(double ppm, int status, int lowerPoint, int upperPoint) {
            this.ppm = ppm;
            this.status = status;
            this.lowerPoint = lowerPoint;
            this.upperPoint = upperPoint;
        }

        public boolean valid() { return status == OK && Double.isFinite(ppm) && ppm > 0; }
    }

    public final int sensorId;
    public final int chemicalId;
    public final long createdAt;
    public final String referenceGas;
    public final String sourceNote;
    private final double[] ppm;
    private final double[] ratio;
    private final boolean ratioIncreasing;

    public EmpiricalMqCurve(int sensorId, int chemicalId, double[] ppm, double[] ratio,
                            long createdAt, String referenceGas, String sourceNote) {
        if (ppm == null || ratio == null || ppm.length != ratio.length || ppm.length < 2)
            throw new IllegalArgumentException("At least two paired calibration points are required");
        this.sensorId = sensorId;
        this.chemicalId = chemicalId;
        this.createdAt = createdAt;
        this.referenceGas = referenceGas == null ? "" : referenceGas;
        this.sourceNote = sourceNote == null ? "" : sourceNote;

        Integer[] order = new Integer[ppm.length];
        for (int i=0;i<order.length;i++) order[i]=i;
        Arrays.sort(order, (a,b) -> Double.compare(ppm[a], ppm[b]));

        this.ppm = new double[ppm.length];
        this.ratio = new double[ratio.length];
        for (int i=0;i<order.length;i++) {
            int j=order[i];
            if (!Double.isFinite(ppm[j]) || ppm[j] <= 0 ||
                    !Double.isFinite(ratio[j]) || ratio[j] <= 0)
                throw new IllegalArgumentException("Calibration points must be finite and positive");
            this.ppm[i]=ppm[j];
            this.ratio[i]=ratio[j];
            if (i>0 && this.ppm[i] <= this.ppm[i-1])
                throw new IllegalArgumentException("Calibration concentrations must be unique");
        }
        ratioIncreasing = this.ratio[this.ratio.length-1] > this.ratio[0];
        if (!isMonotonic())
            throw new IllegalArgumentException("Rs/R0 calibration response must be monotonic");
    }

    public int pointCount() { return ppm.length; }
    public double minPpm() { return ppm[0]; }
    public double maxPpm() { return ppm[ppm.length-1]; }
    public double[] ppmPoints() { return ppm.clone(); }
    public double[] ratioPoints() { return ratio.clone(); }

    public Estimate estimatePpm(double measuredRatio) {
        if (!Double.isFinite(measuredRatio) || measuredRatio <= 0)
            return new Estimate(Double.NaN, INVALID_INPUT, -1, -1);

        double rMin = Math.min(ratio[0], ratio[ratio.length-1]);
        double rMax = Math.max(ratio[0], ratio[ratio.length-1]);
        if (measuredRatio < rMin || measuredRatio > rMax)
            return new Estimate(Double.NaN, OUTSIDE_CALIBRATION_RANGE, -1, -1);

        for (int i=0;i<ratio.length-1;i++) {
            double r1=ratio[i], r2=ratio[i+1];
            boolean bracket = ratioIncreasing
                    ? measuredRatio >= r1 && measuredRatio <= r2
                    : measuredRatio <= r1 && measuredRatio >= r2;
            if (!bracket) continue;

            double x1=Math.log10(r1), x2=Math.log10(r2), x=Math.log10(measuredRatio);
            double y1=Math.log10(ppm[i]), y2=Math.log10(ppm[i+1]);
            if (Math.abs(x2-x1) < 1e-12) return new Estimate(Double.NaN, INVALID_INPUT, i, i+1);
            double t=(x-x1)/(x2-x1);
            double value=Math.pow(10.0, y1+t*(y2-y1));
            return new Estimate(value, OK, i, i+1);
        }
        return new Estimate(Double.NaN, OUTSIDE_CALIBRATION_RANGE, -1, -1);
    }

    /** R-squared of one global log-power approximation; diagnostic only. */
    public double logPowerR2() {
        int n=ppm.length;
        double sx=0, sy=0;
        for(int i=0;i<n;i++){ sx+=Math.log10(ratio[i]); sy+=Math.log10(ppm[i]); }
        double mx=sx/n, my=sy/n, sxx=0, sxy=0;
        for(int i=0;i<n;i++){
            double dx=Math.log10(ratio[i])-mx;
            double dy=Math.log10(ppm[i])-my;
            sxx+=dx*dx; sxy+=dx*dy;
        }
        if(sxx<=0) return Double.NaN;
        double b=sxy/sxx, a=my-b*mx;
        double ssTot=0, ssRes=0;
        for(int i=0;i<n;i++){
            double y=Math.log10(ppm[i]);
            double pred=a+b*Math.log10(ratio[i]);
            ssTot+=(y-my)*(y-my); ssRes+=(y-pred)*(y-pred);
        }
        return ssTot<=0 ? 1.0 : 1.0-ssRes/ssTot;
    }

    private boolean isMonotonic() {
        for(int i=1;i<ratio.length;i++) {
            if (ratioIncreasing) {
                if (ratio[i] <= ratio[i-1]) return false;
            } else {
                if (ratio[i] >= ratio[i-1]) return false;
            }
        }
        return true;
    }
}
