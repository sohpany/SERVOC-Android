package com.servoc.app.models;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CalibrationRegressionModelsTest {
    @Test public void nativeRegistryMatchesImplementedModels(){
        assertEquals(10,ModelRuntimeRegistry.nativeCount());
        for(int i:CalibrationRegressionModels.NATIVE)assertTrue(ModelRuntimeRegistry.isAndroidNative(i));
    }

    @Test public void powerRegressionRecoversSyntheticMqCurve(){
        double[] ratio={0.45,0.60,0.82,1.05,1.35,1.70};
        double[] ppm=new double[ratio.length];
        for(int i=0;i<ratio.length;i++)ppm[i]=525.0*Math.pow(ratio[i],-2.15);
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.POWER,ratio,ppm);
        assertNotNull(f);
        assertEquals(525.0,f.predict(1.0),525.0*1e-8);
        assertEquals(525.0*Math.pow(0.73,-2.15),f.predict(0.73),1e-5);
    }

    @Test public void rankingPrefersPowerForPowerLawData(){
        double[] ratio={0.40,0.52,0.67,0.83,1.02,1.25,1.52,1.80};
        double[] ppm=new double[ratio.length];
        for(int i=0;i<ratio.length;i++)ppm[i]=180.0*Math.pow(ratio[i],-2.4);
        List<CalibrationRegressionModels.Score> scores=CalibrationRegressionModels.rank(ratio,ppm);
        assertFalse(scores.isEmpty());
        assertEquals(CalibrationRegressionModels.POWER,scores.get(0).fit.catalogIndex);
        assertTrue(scores.get(0).logRmse<1e-6);
        assertTrue(scores.get(0).r2>0.999999);
    }

    @Test public void theilSenResistsOneLargeOutlier(){
        double[] x={1,2,3,4,5,6};
        double[] y={12,14,16,18,20,200};
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.THEIL_SEN,x,y);
        assertNotNull(f);
        assertEquals(22.0,f.predict(6.0),1e-9);
    }
}
