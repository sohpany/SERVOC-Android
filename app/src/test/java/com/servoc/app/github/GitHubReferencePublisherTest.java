package com.servoc.app.github;

import com.servoc.app.data.ExperimentQueries;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.Assert.*;

public class GitHubReferencePublisherTest {
    @Test public void canonicalNameIgnoresUnsafeStoredReferenceName(){
        ExperimentQueries.Record r=new ExperimentQueries.Record(42,"2026-08-31T14:00:00Z","{}","p","{}",true,"2026-08-31T14:01:00Z","../../escape.json");
        String name=GitHubReferencePublisher.canonicalFileName(r);
        assertEquals("HazRob_APPROVED_000042_20260831_140000.json",name);
        assertFalse(name.contains("/"));assertFalse(name.contains("\\"));assertFalse(name.contains(".."));
    }

    @Test public void requestIsDeterministicCredentialFreeCreateOnlyPayload()throws Exception{
        byte[] bytes="{\"status\":\"APPROVED\"}".getBytes(StandardCharsets.UTF_8);
        JSONObject j=GitHubReferencePublisher.buildRequest("HazRob_APPROVED_000001_x.json",bytes);
        assertEquals(GitHubReferencePublisher.BRANCH,j.getString("branch"));
        assertArrayEquals(bytes,Base64.getDecoder().decode(j.getString("content")));
        assertFalse(j.has("sha"));assertFalse(j.toString().toLowerCase().contains("token"));
    }

    @Test(expected=IllegalArgumentException.class) public void requestRejectsPathTraversal()throws Exception{
        GitHubReferencePublisher.buildRequest("../escape.json","x".getBytes(StandardCharsets.UTF_8));
    }
}
