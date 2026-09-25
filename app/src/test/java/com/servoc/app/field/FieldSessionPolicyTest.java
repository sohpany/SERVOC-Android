package com.servoc.app.field;

import org.junit.Test;
import static org.junit.Assert.*;

public class FieldSessionPolicyTest {
    @Test public void ordinaryUserFieldCycleCanAttach(){
        assertTrue(FieldSessionPolicy.mayAttachMeasurement("field_mq_cycle","USER"));
    }
    @Test public void developerMqCycleCannotAttach(){
        assertFalse(FieldSessionPolicy.mayAttachMeasurement("developer_mq_cycle","DEVELOPER"));
    }
    @Test public void controlledChemCoreCaptureCannotAttach(){
        assertFalse(FieldSessionPolicy.mayAttachMeasurement("developer_chemcore_controlled_capture","DEVELOPER"));
    }
    @Test public void roleMustAlsoBeUser(){
        assertFalse(FieldSessionPolicy.mayAttachMeasurement("field_mq_cycle","DEVELOPER"));
    }
}
