package com.servoc.app.analytics;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/** Ensures the analytical MQ order consumes heater qualification from the hardware profile. */
public final class AnalyticsSnapshotHeaterValidityTest {
    @Test public void currentHr069ProfileWithholdsMq7AndMq9(){
        assertArrayEquals(
                new boolean[]{true,true,true,true,false,true,false,true},
                AnalyticsSnapshot.manufacturerHeaterValidity());
    }
}
