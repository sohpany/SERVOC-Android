package com.servoc.app.calibration;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/**
 * Versioned calibration container for the eight MQ channels currently used by SERVOC.
 * R0 values are intentionally not treated as validated until a real calibration is performed.
 */
public final class CalibrationProfile {
    public static final int SENSOR_COUNT = 8;
    public static final String[] SENSOR_KEYS = {"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};

    public final String id;
    public final long createdUtcMs;
    public final double[] r0;
    public final double temperatureC;
    public final double humidityRh;
    public final String note;

    public CalibrationProfile(String id, long createdUtcMs, double[] r0,
                              double temperatureC, double humidityRh, String note) {
        this.id = id == null ? "" : id;
        this.createdUtcMs = createdUtcMs;
        this.r0 = r0 == null ? emptyR0() : Arrays.copyOf(r0, SENSOR_COUNT);
        this.temperatureC = temperatureC;
        this.humidityRh = humidityRh;
        this.note = note == null ? "" : note;
    }

    public static double[] emptyR0() {
        double[] x = new double[SENSOR_COUNT];
        Arrays.fill(x, Double.NaN);
        return x;
    }

    public boolean isComplete() {
        for (double x : r0) if (!Double.isFinite(x) || x <= 0) return false;
        return true;
    }

    public int validSensorCount() {
        int n = 0;
        for (double x : r0) if (Double.isFinite(x) && x > 0) n++;
        return n;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("createdUtcMs", createdUtcMs);
            JSONArray a = new JSONArray();
            for (double x : r0) a.put(Double.isFinite(x) ? x : JSONObject.NULL);
            o.put("r0", a);
            o.put("temperatureC", Double.isFinite(temperatureC) ? temperatureC : JSONObject.NULL);
            o.put("humidityRh", Double.isFinite(humidityRh) ? humidityRh : JSONObject.NULL);
            o.put("note", note);
        } catch (Exception ignored) {}
        return o;
    }
}
