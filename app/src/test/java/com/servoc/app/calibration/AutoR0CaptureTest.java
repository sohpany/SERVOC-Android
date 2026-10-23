package com.servoc.app.calibration;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class AutoR0CaptureTest {
 private Map<String,Double> round(double factor){Map<String,Double> m=new LinkedHashMap<>();for(String k:ManualR0Calibration.keys())m.put(k,40*factor);return m;}
 @Test public void thirtySecondRoundGate(){AutoR0Capture c=new AutoR0Capture(1000);for(int i=0;i<30;i++){assertTrue(c.sample(1000+i*1000L,round(1)));assertFalse(c.sample(1100+i*1000L,round(1)));}assertTrue(c.finished());assertEquals(30,c.samples());assertEquals(40.0,c.estimate().get("mq2"),1e-9);}
 @Test public void missingAndUnstableRoundsFailClosed(){AutoR0Capture c=new AutoR0Capture(0);assertFalse(c.sample(0,Collections.emptyMap()));for(int i=0;i<30;i++)assertTrue(c.sample(1000+i*1000L,round(i%2==0?1.0:1.5)));assertTrue(c.finished());assertTrue(c.estimate().isEmpty());}
}