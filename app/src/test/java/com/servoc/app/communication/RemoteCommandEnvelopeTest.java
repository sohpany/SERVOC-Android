package com.servoc.app.communication;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public final class RemoteCommandEnvelopeTest {
    @Test public void drivePayloadIsNestedAndFlattenedForFirmwareParser()throws Exception{
        JSONObject payload=new JSONObject().put("left",37).put("right",-22);
        JSONObject c=RemoteCommandEnvelope.build("s1",7,"n1","drive",700,123456789L,payload);
        assertEquals("s1",c.getString("session"));
        assertEquals(7,c.getLong("seq"));
        assertEquals("drive",c.getString("type"));
        assertEquals(700,c.getInt("ttl_ms"));
        assertEquals(37,c.getInt("left"));
        assertEquals(-22,c.getInt("right"));
        assertEquals(37,c.getJSONObject("payload").getInt("left"));
    }

    @Test public void reservedEnvelopeFieldsCannotBeOverwrittenByPayload()throws Exception{
        JSONObject payload=new JSONObject().put("session","evil").put("seq",999).put("ttl_ms",1).put("mask",3);
        JSONObject c=RemoteCommandEnvelope.build("trusted",4,"nonce","relay",3000,5000L,payload);
        assertEquals("trusted",c.getString("session"));
        assertEquals(4,c.getLong("seq"));
        assertEquals(3000,c.getInt("ttl_ms"));
        assertEquals(3,c.getInt("mask"));
        assertEquals("evil",c.getJSONObject("payload").getString("session"));
    }

    @Test public void ttlIsBounded()throws Exception{
        assertEquals(100,RemoteCommandEnvelope.build("s",1,"n","probe",1,1,new JSONObject()).getInt("ttl_ms"));
        assertEquals(10000,RemoteCommandEnvelope.build("s",1,"n","probe",50000,1,new JSONObject()).getInt("ttl_ms"));
    }
}
