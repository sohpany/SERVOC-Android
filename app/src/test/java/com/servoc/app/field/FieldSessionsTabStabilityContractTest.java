package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Regression gates for RC2 user field-sessions tab; physical-device confirmation is separate. */
public class FieldSessionsTabStabilityContractTest {
    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
    @Test public void spinnerCallbacksUseStableSnapshotsAndCannotReenterRefresh() throws Exception {
        String ui=read("src/main/java/com/servoc/app/ui/FieldSessionsTab.java");
        assertTrue(ui.contains("if(selectingScope||refreshing||pos!=p.getSelectedItemPosition())return;"));
        assertTrue(ui.contains("List<FieldSessionRepository.Site> all=displayedSites;"));
        assertTrue(ui.contains("List<FieldSessionRepository.FieldSession> all=displayedSessions;"));
        assertFalse(ui.contains("List<FieldSessionRepository.Site> all=repo.sites()"));
        assertFalse(ui.contains("List<FieldSessionRepository.FieldSession> all=repo.sessionsForSite"));
        assertTrue(ui.contains("if(refreshing)return;"));
        assertTrue(ui.contains("finally{selectingScope=false;refreshing=false;}"));
    }
    @Test public void renderingHandlesDatabaseFailureWithoutKillingActivity() throws Exception {
        String ui=read("src/main/java/com/servoc/app/ui/FieldSessionsTab.java");
        assertTrue(ui.contains("catch(RuntimeException e)"));
        assertTrue(ui.contains("Log.e(TAG,"));
        assertTrue(ui.contains("private void render()"));
    }
    @Test public void countsAreGroupedAndLegacyFieldSchemaIsRecreatedWithoutDroppingRecords() throws Exception {
        String ui=read("src/main/java/com/servoc/app/ui/FieldSessionsTab.java");
        String repository=read("src/main/java/com/servoc/app/field/FieldSessionRepository.java");
        String database=read("src/main/java/com/servoc/app/data/ExperimentsDb.java");
        assertTrue(ui.contains("repo.measurementCountsForSite(selectedSiteId)"));
        assertFalse(ui.contains("repo.measurementSessionIds("));
        assertTrue(repository.contains("WHERE fs.site_id=? GROUP BY fsm.field_session_id"));
        assertTrue(database.contains("if(!database.isReadOnly())createV9UserFieldTables(database);"));
        assertTrue(database.contains("CREATE TABLE IF NOT EXISTS sites"));
        assertTrue(database.contains("CREATE TABLE IF NOT EXISTS field_sessions"));
    }
}
