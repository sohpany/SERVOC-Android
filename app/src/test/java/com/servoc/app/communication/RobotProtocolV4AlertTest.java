package com.servoc.app.communication;

import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.calibration.ManualR0Calibration;
import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RobotProtocolV4AlertTest {
    @Test public void normalizesAlertAndLdrFromData()throws Exception{
        JSONObject diag=new JSONObject().put("fw","HazRob 0.10.0").put("api",4).put("hardware_profile",HazRobHardwareProfile.ID);
        JSONObject data=new JSONObject()
                .put("alert",new JSONObject().put("gpio",26).put("level",5).put("name","gas_danger").put("active",true).put("gas",2).put("ir",false).put("link",false))
                .put("ldr",new JSONObject().put("installed",true).put("gpio",34).put("level",0).put("quantitative_lux",false));
        JSONObject n=RobotProtocolV4.normalize(diag,data,0,0);
        JSONObject a=n.getJSONObject("alert"),l=n.getJSONObject("ldr");
        assertTrue(a.getBoolean("active"));assertEquals(5,a.getInt("level"));assertEquals("gas_danger",a.getString("name"));assertEquals(2,a.getInt("gas"));assertEquals(HazRobHardwareProfile.ALERT_GPIO,a.getInt("gpio"));
        assertTrue(l.getBoolean("installed"));assertEquals(HazRobHardwareProfile.LDR_GPIO,l.getInt("gpio"));assertEquals(0,l.getInt("level"));assertFalse(l.getBoolean("quantitative_lux"));
    }

    @Test public void profilePinContractMatchesFirmware010(){
        assertEquals("hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1",HazRobHardwareProfile.ID);
        assertEquals("L298N",HazRobHardwareProfile.DRIVE_DRIVER);
        assertEquals(16,HazRobHardwareProfile.DRIVE_L_EN);
        assertEquals(18,HazRobHardwareProfile.DRIVE_L_IN1);
        assertEquals(27,HazRobHardwareProfile.DRIVE_L_IN2);
        assertEquals(17,HazRobHardwareProfile.DRIVE_R_EN);
        assertEquals(19,HazRobHardwareProfile.DRIVE_R_IN1);
        assertEquals(32,HazRobHardwareProfile.DRIVE_R_IN2);
        assertEquals(25,HazRobHardwareProfile.MQ_RELAY_GPIO);
        assertEquals(26,HazRobHardwareProfile.ALERT_GPIO);
        assertEquals(34,HazRobHardwareProfile.LDR_GPIO);
        assertEquals(35,HazRobHardwareProfile.IR_GPIO);
        assertEquals(36,HazRobHardwareProfile.SOUND_GPIO);
        assertEquals(33,HazRobHardwareProfile.BATTERY_ADC_GPIO);
        assertEquals(23,HazRobHardwareProfile.ULTRASONIC_TRIG_GPIO);
        assertEquals(39,HazRobHardwareProfile.ULTRASONIC_ECHO_GPIO);
        assertFalse(HazRobHardwareProfile.INTERNAL_TEMP_INSTALLED);
        assertFalse(HazRobHardwareProfile.LDR_QUANTITATIVE_LUX);
    }

    @Test public void firmwarePlaceholderR0FallsBackToAcceptedFieldBaselineWithoutReferenceValidation()throws Exception{
        ManualR0Calibration.clear(null,"mq2");
        JSONObject diag=new JSONObject().put("fw","HazRob 0.10.0").put("api",4).put("hardware_profile",HazRobHardwareProfile.ID);
        JSONObject mq2=new JSONObject().put("ok",true).put("raw",1000).put("ao",1.0).put("adc",1.0).put("rs",40.0).put("ratio",4.0).put("r0",10.0).put("r0_calibrated",false).put("r0_source","firmware_default").put("powered",true);
        JSONObject data=new JSONObject().put("mq",new JSONObject().put("mq2",mq2));
        JSONObject q=RobotProtocolV4.normalize(diag,data,0,0).getJSONObject("mq").getJSONObject("mq2");
        assertFalse(q.getBoolean("firmware_r0_calibrated"));
        assertTrue(q.getBoolean("r0_calibrated"));
        assertFalse(q.getBoolean("manual_calibration_confirmed"));
        assertEquals("CALIBRATED_FIELD_BASELINE",q.getString("calibration_status"));
        assertEquals(FieldR0Baseline.SOURCE,q.getString("r0_source"));
        assertFalse(q.getBoolean("r0_reference_validated"));
        assertEquals(FieldR0Baseline.value("mq2"),q.getDouble("r0"),1e-9);
        assertEquals(40.0/FieldR0Baseline.value("mq2"),q.getDouble("ratio"),1e-9);
    }

    @Test public void normalizesBatteryProximityAndEnergyBasis()throws Exception{
        JSONObject device=new JSONObject()
                .put("battery",new JSONObject().put("v",15.6).put("raw",2870).put("estimated",true))
                .put("internal_temp",new JSONObject().put("installed",false).put("c",JSONObject.NULL).put("gpio",14))
                .put("proximity",new JSONObject().put("cm",22.4).put("warning",true).put("trig",23).put("echo",39))
                .put("energy_basis",new JSONObject().put("uptime_ms",12345).put("drive_eq_ms",2100).put("mq_on_ms",9000));
        JSONObject n=RobotProtocolV4.normalize(new JSONObject(),new JSONObject().put("device",device),0,0).getJSONObject("device");
        assertEquals(15.6,n.getJSONObject("battery").getDouble("voltage_v"),1e-9);
        assertTrue(n.getJSONObject("proximity").getBoolean("warning"));
        assertEquals(22.4,n.getJSONObject("proximity").getDouble("distance_cm"),1e-9);
        assertFalse(n.getJSONObject("internal_temp").getBoolean("installed"));
        assertEquals(2100,n.getJSONObject("energy_basis").getLong("drive_eq_ms"));
    }
}
