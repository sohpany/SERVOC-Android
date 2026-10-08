package com.servoc.app.calibration;

import android.content.SharedPreferences;
import org.json.JSONObject;
import com.servoc.app.chemcore.ChemCoreLabQuality;
import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** End-to-end persistence contracts for editable R0; no Android emulator required. */
public final class HR069R1EditableR0PersistenceTest {
    private static final class Prefs {
        final Map<String,Object> stored=new HashMap<>();
        boolean permitCommit=true;
        SharedPreferences preferences(){
            return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
                new Class[]{SharedPreferences.class},(proxy,method,args)->{
                    String name=method.getName();
                    if("getString".equals(name))return stored.getOrDefault(args[0],args[1]);
                    if("contains".equals(name))return stored.containsKey(args[0]);
                    if("getAll".equals(name))return new HashMap<>(stored);
                    if("edit".equals(name))return editor();
                    if("toString".equals(name))return "TestPreferences";
                    throw new UnsupportedOperationException(name);
                });
        }
        SharedPreferences.Editor editor(){
            Map<String,Object> pending=new HashMap<>();
            java.util.Set<String> removed=new java.util.HashSet<>();
            return (SharedPreferences.Editor)Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                new Class[]{SharedPreferences.Editor.class},(proxy,method,args)->{
                    String name=method.getName();
                    if("putString".equals(name)){pending.put((String)args[0],args[1]);return proxy;}
                    if("remove".equals(name)){removed.add((String)args[0]);return proxy;}
                    if("commit".equals(name)||"apply".equals(name)){
                        if(permitCommit){
                            for(String k:removed)stored.remove(k);
                            stored.putAll(pending);
                        }
                        return "commit".equals(name)?permitCommit:null;
                    }
                    throw new UnsupportedOperationException(name);
                });
        }
    }

    @Test public void updateOneThenReloadAndRenormalize()throws Exception{
        Prefs memory=new Prefs();SharedPreferences p=memory.preferences();
        ManualR0Calibration.load(p);
        double old=ManualR0Calibration.get("mq3");
        assertTrue(old>0);
        assertTrue(ManualR0Calibration.save(p,"mq3",42.5));
        ManualR0Calibration.load(p);
        assertEquals(42.5,ManualR0Calibration.get("mq3"),1e-9);
        assertEquals("manual_device_r0",ManualR0Calibration.getSource("mq3"));
        JSONObject sample=new JSONObject().put("rs",85.0).put("valid",true);
        ManualR0Calibration.apply("mq3",sample);
        assertEquals(2.0,sample.getDouble("ratio"),1e-9);
        assertTrue(sample.getBoolean("r0_provenance_complete"));
        assertFalse(sample.getBoolean("r0_reference_validated"));
        assertTrue(ManualR0Calibration.reset(p,"mq3"));
        ManualR0Calibration.load(p);
        assertEquals(old,ManualR0Calibration.get("mq3"),1e-9);
        assertEquals(FieldR0Baseline.SOURCE,ManualR0Calibration.getSource("mq3"));
    }

    @Test public void eightSensorRevisionIsAtomicAndSurvivesRestart(){
        Prefs memory=new Prefs();SharedPreferences p=memory.preferences();
        ManualR0Calibration.load(p);
        Map<String,Double> revised=new LinkedHashMap<>();
        for(String k:ManualR0Calibration.keys())revised.put(k,ManualR0Calibration.get(k)+1.0);
        assertEquals(8,revised.size());
        assertTrue(ManualR0Calibration.saveAll(p,revised));
        String sharedSession=ManualR0Calibration.getSession("mq2");
        assertFalse(sharedSession.isEmpty());
        ManualR0Calibration.load(p);
        for(String k:ManualR0Calibration.keys()){
            assertEquals(revised.get(k),ManualR0Calibration.get(k),1e-9);
            assertEquals(sharedSession,ManualR0Calibration.getSession(k));
        }
        Map<String,Double> invalid=new LinkedHashMap<>(revised);
        invalid.put("mq7",Double.NaN);
        assertFalse(ManualR0Calibration.saveAll(p,invalid));
        assertEquals(revised.get("mq2"),ManualR0Calibration.get("mq2"),1e-9);
        assertEquals(revised.get("mq7"),ManualR0Calibration.get("mq7"),1e-9);
        memory.permitCommit=false;
        assertFalse(ManualR0Calibration.save(p,"mq3",999.0));
        assertEquals(revised.get("mq3"),ManualR0Calibration.get("mq3"),1e-9);
        assertFalse(ManualR0Calibration.reset(p,"mq3"));
        assertEquals(revised.get("mq3"),ManualR0Calibration.get("mq3"),1e-9);
    }

    @Test public void unknownOrNonPhysicalInputNeverChangesR0(){
        Prefs memory=new Prefs();SharedPreferences p=memory.preferences();
        ManualR0Calibration.load(p);
        assertFalse(ManualR0Calibration.save(p,"unknown",41.0));
        assertFalse(ManualR0Calibration.save(p,"mq2",0.0));
        assertFalse(ManualR0Calibration.save(p,"mq2",Double.POSITIVE_INFINITY));
        assertFalse(ManualR0Calibration.reset(p,"unknown"));
        assertEquals(8,ManualR0Calibration.keys().length);
    }

    @Test public void changingDeviceR0NeverReinterpretsAnArchivedSession()throws Exception{
        Prefs memory=new Prefs();SharedPreferences prefs=memory.preferences();
        ManualR0Calibration.load(prefs);
        // A real archived sample stores its measured Rs and its original R0.
        JSONObject oldSample=new JSONObject().put("rs",80.0).put("r0",20.0)
                .put("ratio",4.0).put("valid",true)
                .put("r0_source","manual_device_r0")
                .put("r0_provenance_complete",true);
        double archived=AcceptedR0Normalizer.recordedRatio("mq2",oldSample);
        assertEquals(4.0,archived,1e-12);
        assertTrue(AcceptedR0Normalizer.usesRecordedDeviceR0("mq2",oldSample));

        // New device R0 changes future live calculations but not the stored evidence.
        assertTrue(ManualR0Calibration.save(prefs,"mq2",40.0));
        assertEquals(2.0,AcceptedR0Normalizer.ratio("mq2",oldSample),1e-12);
        assertEquals(archived,AcceptedR0Normalizer.recordedRatio("mq2",oldSample),1e-12);
        assertEquals(archived,AcceptedR0Normalizer.availableRecordedRatio("mq2",oldSample),1e-12);
        ManualR0Calibration.load(prefs);
        assertEquals(archived,AcceptedR0Normalizer.recordedRatio("mq2",oldSample),1e-12);

        // Legacy samples may use their recorded historical ratio, not today's R0.
        JSONObject legacy=new JSONObject().put("rs",80.0).put("ratio",1.75).put("valid",true);
        assertEquals(1.75,AcceptedR0Normalizer.recordedRatio("mq2",legacy),1e-12);
        assertFalse(AcceptedR0Normalizer.usesRecordedDeviceR0("mq2",legacy));
        JSONObject missing=new JSONObject().put("rs",80.0).put("valid",true);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.recordedRatio("mq2",missing)));
        oldSample.put("valid",false);
        assertTrue(Double.isNaN(AcceptedR0Normalizer.recordedRatio("mq2",oldSample)));
        assertEquals(4.0,AcceptedR0Normalizer.availableRecordedRatio("mq2",oldSample),1e-12);
    }

    @Test public void labAndExportProvenanceTracksActualDeviceR0Revision()throws Exception{
        Prefs p=new Prefs();SharedPreferences prefs=p.preferences();
        try{
            ManualR0Calibration.load(prefs);
            assertTrue(ManualR0Calibration.save(prefs,"mq3",47.25));
            ChemCoreLabQuality lab=ChemCoreLabQuality.evaluate(new JSONObject(),0,0);
            JSONObject provenance=lab.toJson().getJSONObject("r0_standard");
            assertEquals("mixed_device_r0",provenance.getString("source"));
            assertEquals("mixed_device_r0_sessions",provenance.getString("campaign"));
            assertEquals(7,provenance.getInt("field_baseline_channels"));
            assertEquals(8,provenance.getInt("normalized_channels"));
            assertEquals(0,provenance.getInt("samples"));
            JSONObject channels=provenance.getJSONObject("per_sensor");
            assertEquals(47.25,channels.getJSONObject("mq3").getDouble("r0"),1e-12);
            assertEquals("manual_device_r0",channels.getJSONObject("mq3").getString("source"));
            assertFalse(channels.getJSONObject("mq3").getBoolean("reference_gas_validated"));
            assertEquals(FieldR0Baseline.SOURCE,channels.getJSONObject("mq2").getString("source"));
            assertFalse(lab.toJson().getJSONObject("calibration_gate").getBoolean("field_r0_normalization_ready"));
            assertTrue(lab.toJson().getJSONObject("calibration_gate").getBoolean("r0_normalization_ready"));
        }finally{ManualR0Calibration.load(null);}
    }
}
