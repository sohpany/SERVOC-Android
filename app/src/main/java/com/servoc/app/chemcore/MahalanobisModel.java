package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small dependency-free regularized Mahalanobis model for ChemCore anomaly/OOD gating.
 * Missing dimensions are excluded pairwise at scoring time; fitting requires finite values.
 */
public final class MahalanobisModel {
    public final double[] mean;
    public final double[][] inverseCovariance;
    public final double ridge;
    public final int trainingCount;

    private MahalanobisModel(double[] mean, double[][] inverseCovariance, double ridge, int trainingCount) {
        this.mean = mean;
        this.inverseCovariance = inverseCovariance;
        this.ridge = ridge;
        this.trainingCount = trainingCount;
    }

    public static MahalanobisModel fit(double[][] samples, double ridgeFraction) {
        List<double[]> clean = finiteRows(samples);
        if (clean.size() < 3) throw new IllegalArgumentException("at least 3 complete samples required");
        int d = clean.get(0).length;
        if (d == 0) throw new IllegalArgumentException("empty vectors");
        for (double[] x : clean) if (x.length != d) throw new IllegalArgumentException("inconsistent dimensions");

        double[] mean = new double[d];
        for (double[] x : clean) for (int j = 0; j < d; j++) mean[j] += x[j];
        for (int j = 0; j < d; j++) mean[j] /= clean.size();

        double[][] cov = new double[d][d];
        for (double[] x : clean) {
            for (int i = 0; i < d; i++) {
                double a = x[i] - mean[i];
                for (int j = 0; j < d; j++) cov[i][j] += a * (x[j] - mean[j]);
            }
        }
        double den = Math.max(1, clean.size() - 1);
        double trace = 0;
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) cov[i][j] /= den;
            trace += cov[i][i];
        }
        double avgVar = trace > 0 ? trace / d : 1.0;
        double rf = Double.isFinite(ridgeFraction) && ridgeFraction > 0 ? ridgeFraction : 1e-3;
        double ridge = Math.max(1e-12, avgVar * rf);
        for (int i = 0; i < d; i++) cov[i][i] += ridge;
        return new MahalanobisModel(mean, invert(cov), ridge, clean.size());
    }

    /** Returns squared Mahalanobis distance; finite dimensions are used as a principal subspace. */
    public double distanceSquared(double[] x) {
        if (x == null || x.length != mean.length) return Double.NaN;
        int[] idx = finiteIndices(x);
        if (idx.length == 0) return Double.NaN;
        if (idx.length == mean.length) {
            double[] d = new double[mean.length];
            for (int i = 0; i < d.length; i++) d[i] = x[i] - mean[i];
            return quadratic(d, inverseCovariance);
        }
        // For missing dimensions, use the corresponding inverse-covariance principal block.
        double[] d = new double[idx.length];
        double[][] w = new double[idx.length][idx.length];
        for (int i = 0; i < idx.length; i++) {
            d[i] = x[idx[i]] - mean[idx[i]];
            for (int j = 0; j < idx.length; j++) w[i][j] = inverseCovariance[idx[i]][idx[j]];
        }
        return quadratic(d, w) * ((double) mean.length / idx.length);
    }

    public double distance(double[] x) {
        double d2 = distanceSquared(x);
        return Double.isFinite(d2) && d2 >= 0 ? Math.sqrt(d2) : Double.NaN;
    }

    public double[] trainingDistances(double[][] samples) {
        List<double[]> rows = finiteRows(samples);
        double[] out = new double[rows.size()];
        for (int i = 0; i < out.length; i++) out[i] = distance(rows.get(i));
        return out;
    }

    public static double empiricalQuantile(double[] values, double q) {
        if (values == null || values.length == 0) return Double.NaN;
        double[] a = Arrays.stream(values).filter(Double::isFinite).toArray();
        if (a.length == 0) return Double.NaN;
        Arrays.sort(a);
        double qq = Math.max(0, Math.min(1, q));
        double p = qq * (a.length - 1);
        int lo = (int) Math.floor(p), hi = (int) Math.ceil(p);
        if (lo == hi) return a[lo];
        return a[lo] + (p - lo) * (a[hi] - a[lo]);
    }

    private static List<double[]> finiteRows(double[][] samples) {
        List<double[]> out = new ArrayList<>();
        if (samples == null) return out;
        for (double[] x : samples) {
            if (x == null || x.length == 0) continue;
            boolean ok = true;
            for (double v : x) if (!Double.isFinite(v)) { ok = false; break; }
            if (ok) out.add(Arrays.copyOf(x, x.length));
        }
        return out;
    }

    private static int[] finiteIndices(double[] x) {
        int n = 0; for (double v : x) if (Double.isFinite(v)) n++;
        int[] out = new int[n]; int k = 0;
        for (int i = 0; i < x.length; i++) if (Double.isFinite(x[i])) out[k++] = i;
        return out;
    }

    private static double quadratic(double[] d, double[][] w) {
        double sum = 0;
        for (int i = 0; i < d.length; i++) for (int j = 0; j < d.length; j++) sum += d[i] * w[i][j] * d[j];
        return sum < 0 && sum > -1e-9 ? 0 : sum;
    }

    private static double[][] invert(double[][] a) {
        int n = a.length;
        double[][] m = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            if (a[i].length != n) throw new IllegalArgumentException("matrix must be square");
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n + i] = 1.0;
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            if (Math.abs(m[pivot][col]) < 1e-15) throw new IllegalArgumentException("singular covariance");
            if (pivot != col) { double[] tmp = m[pivot]; m[pivot] = m[col]; m[col] = tmp; }
            double div = m[col][col];
            for (int j = 0; j < 2 * n; j++) m[col][j] /= div;
            for (int r = 0; r < n; r++) if (r != col) {
                double f = m[r][col];
                if (Math.abs(f) < 1e-20) continue;
                for (int j = 0; j < 2 * n; j++) m[r][j] -= f * m[col][j];
            }
        }
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(m[i], n, inv[i], 0, n);
        return inv;
    }
}
