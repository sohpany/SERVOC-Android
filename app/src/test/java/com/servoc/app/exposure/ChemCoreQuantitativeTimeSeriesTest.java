package com.servoc.app.exposure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public final class ChemCoreQuantitativeTimeSeriesTest {
    private static ExposureRepository.Limit limit(String type,double value,double durationMin){return new ExposureRepository.Limit(1,77,"NIOSH","US",type,value,"ppm",durationMin,"","SRC","R1");}

    @Test public void completeFifteenMinuteSeriesProducesValidStelAverageAndPreservesEveryPoint()throws Exception{
        List<ChemCoreQuantitativeTimeSeries.Point> points=new ArrayList<>();for(int s=0;s<=900;s++)points.add(new ChemCoreQuantitativeTimeSeries.Point(s*1000L,12.0));
        JSONObject q=ChemCoreQuantitativeTimeSeries.summarize("S",77,points,points.size(),points.size(),Collections.singletonList(limit("STEL",10,15)),25,50,1013.25,null);
        assertTrue(q.getBoolean("valid"));assertEquals(15.0,q.getDouble("covered_minutes"),1e-9);assertEquals(12.0,q.getDouble("time_weighted_mean_ppm"),1e-9);
        assertTrue(q.getBoolean("all_valid_quantitative_points_preserved"));assertTrue(q.getBoolean("raw_telemetry_preserved_elsewhere"));assertEquals("telemetry_samples",q.getString("raw_telemetry_source"));
        JSONArray rows=q.getJSONArray("points");assertEquals(points.size(),rows.length());assertEquals(0,rows.getJSONObject(0).getLong("t_ms"));assertEquals(12.0,rows.getJSONObject(0).getDouble("ppm"),1e-9);assertEquals(900_000,rows.getJSONObject(rows.length()-1).getLong("t_ms"));
        JSONArray windows=q.getJSONArray("windows");assertEquals(1,windows.length());JSONObject w=windows.getJSONObject(0);assertEquals("STEL",w.getString("role"));assertTrue(w.getBoolean("valid"));assertEquals(15.0,w.getDouble("covered_minutes"),1e-9);assertEquals(12.0,w.getDouble("average_ppm"),1e-9);assertEquals(1.0,w.getDouble("coverage_fraction"),1e-9);
    }

    @Test public void oneMinuteSeriesCannotUnlockFifteenMinuteStel()throws Exception{
        List<ChemCoreQuantitativeTimeSeries.Point> points=new ArrayList<>();for(int s=0;s<=60;s++)points.add(new ChemCoreQuantitativeTimeSeries.Point(s*1000L,20.0));
        JSONObject q=ChemCoreQuantitativeTimeSeries.summarize("S",77,points,points.size(),points.size(),Collections.singletonList(limit("STEL",10,15)),25,50,1013.25,null);
        JSONObject w=q.getJSONArray("windows").getJSONObject(0);assertFalse(w.getBoolean("valid"));assertEquals(1.0,w.getDouble("covered_minutes"),1e-9);assertEquals(1.0/15.0,w.getDouble("coverage_fraction"),1e-9);assertEquals(points.size(),q.getJSONArray("points").length());
    }

    @Test public void longTelemetryGapIsNotIntegratedAcrossButPointsRemainAvailable()throws Exception{
        List<ChemCoreQuantitativeTimeSeries.Point> points=new ArrayList<>();points.add(new ChemCoreQuantitativeTimeSeries.Point(0,10));points.add(new ChemCoreQuantitativeTimeSeries.Point(1000,10));points.add(new ChemCoreQuantitativeTimeSeries.Point(10_000,100));points.add(new ChemCoreQuantitativeTimeSeries.Point(11_000,100));
        JSONObject q=ChemCoreQuantitativeTimeSeries.summarize("S",77,points,4,4,Collections.emptyList(),25,50,1013.25,null);
        assertEquals(2.0/60.0,q.getDouble("covered_minutes"),1e-9);assertEquals(55.0,q.getDouble("time_weighted_mean_ppm"),1e-9);assertEquals(4,q.getJSONArray("points").length());
    }

    @Test public void environmentBasisIsRetainedForLaterUnitConversion()throws Exception{
        List<ChemCoreQuantitativeTimeSeries.Point> points=List.of(new ChemCoreQuantitativeTimeSeries.Point(0,1),new ChemCoreQuantitativeTimeSeries.Point(1000,1));
        JSONObject q=ChemCoreQuantitativeTimeSeries.summarize("S",77,points,2,2,Collections.emptyList(),26.5,42,1002.3,null);JSONObject e=q.getJSONObject("environment_basis");assertEquals(26.5,e.getDouble("mean_temperature_c"),1e-9);assertEquals(1002.3,e.getDouble("mean_pressure_hpa"),1e-9);assertTrue(e.getBoolean("temperature_available"));assertTrue(e.getBoolean("pressure_available"));
    }
}
