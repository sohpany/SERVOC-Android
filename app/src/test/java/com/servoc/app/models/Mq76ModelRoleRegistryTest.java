package com.servoc.app.models;

import org.junit.Test;

import static org.junit.Assert.*;

public class Mq76ModelRoleRegistryTest {
    @Test public void all76ModelsHaveScientificRolesAndPinnedLabExecution(){
        assertEquals(76,Mq76ModelRoleRegistry.count());
        assertTrue(Mq76ModelRoleRegistry.allIntegrated());
        assertEquals(76,ModelRuntimeRegistry.integratedResearchCount());
        for(int i=0;i<76;i++){
            Mq76ModelRoleRegistry.Spec s=Mq76ModelRoleRegistry.get(i);
            assertEquals(i,s.index);
            assertNotNull(s.name);
            assertFalse(s.name.isEmpty());
            assertNotNull(s.purpose);
            assertNotNull(s.deployment);
            assertTrue(Mq76ModelRoleRegistry.pinnedLabExecutable(i));
            assertTrue(ModelRuntimeRegistry.isResearchIntegrated(i));
        }
    }

    @Test public void deviceNativeSubsetRemainsExactlyTheAudited14(){
        int n=0;
        for(int i=0;i<76;i++)if(Mq76ModelRoleRegistry.isAndroidNative(i))n++;
        assertEquals(14,n);
        assertEquals(ModelRuntimeRegistry.nativeCount(),n);
    }

    @Test public void timeSeriesFamiliesAreNotSilentlyPromotedToPpmCalibration(){
        for(int i=49;i<=62;i++)assertFalse(Mq76ModelRoleRegistry.get(i).directCalibrationEligible);
    }

    @Test public void environmentalFamiliesRemainExplicitlyEnvironmental(){
        for(int i=0;i<=10;i++)
            assertEquals(Mq76ModelRoleRegistry.Purpose.ENVIRONMENTAL_RESPONSE,Mq76ModelRoleRegistry.get(i).purpose);
    }
}
