package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class QsprValidationProtocolTest {
    @Test public void leaveCompoundOutNeverRequiresRowRandomization(){
        List<QsprValidationProtocol.LabeledObservation> rows=new ArrayList<>();
        for(long chemical=1;chemical<=4;chemical++){
            for(int i=0;i<8;i++){
                Map<String,Double> d=new LinkedHashMap<>();d.put("MolecularWeight",20.0*chemical);
                double ppm=10.0+i*5.0,temp=20.0+i*0.2,rh=40.0+i*0.3;
                double y=0.2-0.25*Math.log10(ppm)+0.001*d.get("MolecularWeight")+0.001*(temp-20)-0.0005*(rh-40);
                QsprMoxModel.Observation o=new QsprMoxModel.Observation(d,ppm,temp,rh,y,1.0);
                rows.add(new QsprValidationProtocol.LabeledObservation(chemical,"S"+chemical+"-"+i,o));
            }
        }
        QsprValidationProtocol.Result r=QsprValidationProtocol.leaveCompoundOut("mq2","test",
                new String[]{"MolecularWeight"},rows,1e-3);
        assertEquals(QsprValidationProtocol.LEAVE_COMPOUND_OUT,r.protocol);
        assertEquals(4,r.compoundsAttempted);
        assertTrue(r.compoundsEvaluated>=3);
        assertTrue(r.predictions>0);
        assertTrue(Double.isFinite(r.rmseLog10));
    }
}
