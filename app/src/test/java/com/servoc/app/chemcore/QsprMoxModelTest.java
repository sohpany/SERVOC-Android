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
        double[] pols={1.0,1.6,2.2};
        double[] ions={7.5,8.5,9.5};
        double[] ppms={10.0,31.6227766,100.0};
        double[][] env={{20,35},{20,65},{30,35},{30,65}};
        for(double pol:pols)for(double ion:ions)for(double ppm:ppms)for(double[] e:env){
            double t=e[0],rh=e[1],lc=Math.log10(ppm);
            double y=.8-.55*lc+.12*pol-.03*ion+.04*pol*lc-.01*ion*lc+.006*t-.0015*rh;
            rows.add(new QsprMoxModel.Observation(Map.of("polarizability",pol,"ionization",ion),ppm,t,rh,y,1));
        }
        QsprMoxModel m=QsprMoxModel.fit("mq3","test",d,rows,1e-8);
        double ppm=31.6227766,t=25,rh=50,pol=1.6,ion=8.5,lc=Math.log10(ppm);
        double expected=.8-.55*lc+.12*pol-.03*ion+.04*pol*lc-.01*ion*lc+.006*t-.0015*rh;
        QsprMoxModel.Prediction p=m.predict(Map.of("polarizability",pol,"ionization",ion),ppm,t,rh);
        assertTrue(p.available);
        assertEquals(ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,p.evidenceTier);
        assertEquals(expected,p.meanLog10Ratio,1e-4);
        assertTrue(p.responseRatio>0);
        assertTrue(p.residualRmseLog10<1e-4);
        assertTrue(p.insideTrainingEnvelope);
    }

    @Test public void missingDescriptorFailsClosed(){
        List<QsprMoxModel.Observation> rows=new ArrayList<>();
        for(int i=0;i<10;i++){double ppm=10+i;rows.add(new QsprMoxModel.Observation(Map.of("mw",40.0+i),ppm,25,50,1-.2*Math.log10(ppm),1));}
        QsprMoxModel m=QsprMoxModel.fit("mq2","test",new String[]{"mw"},rows,1e-3);
        assertFalse(m.predict(Map.of(),20,25,50).available);
    }
}
