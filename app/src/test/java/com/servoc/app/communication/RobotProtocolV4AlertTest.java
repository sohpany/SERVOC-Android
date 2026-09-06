package com.servoc.app.communication;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RobotProtocolV4AlertTest {
    @Test public void normalizesAlertAndLdrFromData()throws Exception{
        JSONObject diag=new JSONObject().put("fw","HazRob 0.9.3-rc1").put("api",4).put("hardware_profile",HazRobHardwareProfile.ID);
        JSONObject data=new JSONObject()
                .put("alert",new JSONObject().put("gpio",26).put("level",5).put("name","gas_danger").put("active",true).put("gas",2).put("ir",false).put("link",false))
                .put("ldr",new JSONObject().put("installed",true).put("gpio",34).put("level",0).put("quantitative_lux",false));
        JSONObject n=RobotProtocolV4.normalize(diag,data,0,0);
        JSONObject a=n.getJSONObject("alert"),l=n.getJSONObject("ldr");
        assertTrue(a.getBoolean("active"));assertEquals(5,a.getInt("level"));assertEquals("gas_danger",a.getString("name"));assertEquals(2,a.getInt("gas"));assertEquals(HazRobHardwareProfile.ALERT_GPIO,a.getInt("gpio"));
        assertTrue(l.getBoolean("installed"));assertEquals(HazRobHardwareProfile.LDR_GPIO,l.getInt("gpio"));assertEquals(0,l.getInt("level"));assertFalse(l.getBoolean("quantitative_lux"));
    }

    @Test public void profilePinContractMatchesFirmware093(){
        assertEquals("hazrob-0.9.3-1r8mq-alert-ldr-v1",HazRobHardwareProfile.ID);
        assertEquals(25,HazRobHardwareProfile.MQ_RELAY_GPIO);
        assertEquals(26,HazRobHardwareProfile.ALERT_GPIO);
        assertEquals(34,HazRobHardwareProfile.LDR_GPIO);
        assertEquals(35,HazRobHardwareProfile.IR_GPIO);
        assertEquals(36,HazRobHardwareProfile.SOUND_GPIO);
    }
}
