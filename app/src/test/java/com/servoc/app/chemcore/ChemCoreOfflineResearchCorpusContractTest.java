package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import static org.junit.Assert.*;

/** Locks the current HR069 scientific source manifest into the APK without upgrading evidence. */
public final class ChemCoreOfflineResearchCorpusContractTest {
    private static String sha(byte[] raw)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw);
        StringBuilder s=new StringBuilder(64);
        for(byte b:digest)s.append(String.format(Locale.US,"%02x",b&255));
        return s.toString();
    }
    @Test public void allFourteenCanonicalSourcesAreBundledWithExactHashesAndNonM4Policy()throws Exception{
        Path folder=Paths.get("build/generated/hazrobAssets/generated/chemcore/hr069_offline");
        JSONObject manifest=new JSONObject(Files.readString(folder.resolve("manifest.json"),StandardCharsets.UTF_8));
        assertEquals(ChemCoreOfflineResearchCorpus.SCHEMA,manifest.getString("schema"));
        assertEquals("0.10.2",manifest.getString("release"));
        assertEquals("HR069",manifest.getString("hardware_revision"));
        assertEquals("research_data/hr069_analytical_library",manifest.getString("authoritative_root"));
        assertFalse(manifest.getBoolean("m4_promotion"));
        assertFalse(manifest.getBoolean("runtime_calibration_override"));
        assertEquals(14,manifest.getInt("source_count"));
        JSONArray sources=manifest.getJSONArray("sources");
        assertEquals(14,sources.length());
        for(int i=0;i<sources.length();i++){
            JSONObject src=sources.getJSONObject(i);
            byte[] bundled=Files.readAllBytes(folder.resolve(src.getString("name")));
            byte[] repo=Files.readAllBytes(Paths.get("../research_data/hr069_analytical_library",src.getString("name")));
            assertArrayEquals("Bundled source must be identical to current canonical repository source",repo,bundled);
            assertEquals(src.getString("name"),sha(bundled),src.getString("sha256"));
            assertEquals(bundled.length,src.getInt("bytes"));
            assertEquals(src.getString("source_schema"),new JSONObject(new String(bundled,StandardCharsets.UTF_8)).getString("schema"));
        }
    }

    @Test public void scientificCorpusNeverReplacesUserSessionsOrCalibrations()throws Exception{
        String j=Files.readString(Paths.get("src/main/java/com/servoc/app/chemcore/ChemCoreOfflineResearchCorpus.java"),StandardCharsets.UTF_8);
        assertTrue(j.contains("chemcore_offline_research_sources"));
        assertFalse(j.contains("DELETE FROM field_sessions"));
        assertFalse(j.contains("DELETE FROM calibration_curves"));
        assertFalse(j.contains("M4_HAZROB_VALIDATED"));
        assertTrue(j.contains("EXTERNAL_RESEARCH_NON_M4"));
        String bootstrap=Files.readString(Paths.get("src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java"),StandardCharsets.UTF_8);
        assertTrue(bootstrap.contains("ChemCoreOfflineResearchCorpus.importIfPresent(context,db)"));
    }
}
