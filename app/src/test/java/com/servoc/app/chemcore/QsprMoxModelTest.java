package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class QsprMoxModelTest {
    @Test public void recoversSyntheticMolecularlyConditionedResponse(){
        String[] d={"polarizability","ionization"};
        List<QsprMoxModel.Observation> rows=new ArrayList<>();
        for(int chem=0;chem<4;chem++){
            double pol=1.0+chem*.4,ion=8.0+chem*.5;
            for(int level=0;level<4;level++){
                double ppm=10*Math.pow(2,level*.5);double t=20+2*level;double rh=35+5*chem;
                double lc=Math.log10(ppm);
                double y=.8-.55*lc+.12*pol-.03*ion+.04*pol*lc-.01*ion*lc+.006*t-.0015*rh;
                rows.add(new QsprMoxModel.Observation(Map.of("polarizability",pol,"ionization",ion),ppm,t,rh,y,1));
            }
        }
        QsprMoxModel m=QsprMoxModel.fit("mq3","test",d,rows,1e-6);
        double ppm=31.6227766,t=22,rh=42,pol=1.55,ion=8.7,lc=Math.log10(ppm);
        double expected=.8-.55*lc+.12*pol-.03*ion+.04*pol*lc-.01*ion*lc+.006*t-.0015*rh;
        QsprMoxModel.Prediction p=m.predict(Map.of("polarizability",pol,"ionization",ion),ppm,t,rh);
        assertTrue(p.available);
        assertEquals(ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,p.evidenceTier);
        assertEquals(expected,p.meanLog10Ratio,0.03);
        assertTrue(p.responseRatio>0);
    }

    @Test public void missingDescriptorFailsClosed(){
        List<QsprMoxModel.Observation> rows=new ArrayList<>();
        for(int i=0;i<10;i++){double ppm=10+i;rows.add(new QsprMoxModel.Observation(Map.of("mw",40.0+i),ppm,25,50,1-.2*Math.log10(ppm),1));}
        QsprMoxModel m=QsprMoxModel.fit("mq2","test",new String[]{"mw"},rows,1e-3);
        assertFalse(m.predict(Map.of(),20,25,50).available);
    }
}
