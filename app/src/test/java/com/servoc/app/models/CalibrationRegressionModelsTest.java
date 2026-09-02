package com.servoc.app.models;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CalibrationRegressionModelsTest {
    @Test public void nativeRegistryMatchesImplementedModels(){
        assertEquals(CalibrationRegressionModels.NATIVE.length,ModelRuntimeRegistry.nativeCount());
        assertEquals(14,ModelRuntimeRegistry.nativeCount());
        for(int i:CalibrationRegressionModels.NATIVE)assertTrue(ModelRuntimeRegistry.isAndroidNative(i));
    }

    @Test public void fglsRecoversThroughOriginRelation(){
        double[] x={0.5,1,1.5,2,3,4};
        double[] y={1.5,3,4.5,6,9,12};
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.WEIGHTED_LINEAR,x,y);
        assertNotNull(f);
        assertEquals(15.0,f.predict(5.0),1e-8);
    }

    @Test public void demingRecoversExactStraightLine(){
        double[] x={1,2,3,4,5,6};
        double[] y={5,8,11,14,17,20};
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.DEMING,x,y);
        assertNotNull(f);
        assertEquals(23.0,f.predict(7.0),1e-8);
    }

    @Test public void polynomial4RecoversFourthDegreeCurve(){
        double[] x={1,2,3,4,5,6,7};
        double[] y=new double[x.length];
        for(int i=0;i<x.length;i++)y[i]=2+3*x[i]-0.5*x[i]*x[i]+0.1*Math.pow(x[i],3)+0.02*Math.pow(x[i],4);
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.POLY4,x,y);
        assertNotNull(f);
        double q=2+3*2.5-0.5*2.5*2.5+0.1*Math.pow(2.5,3)+0.02*Math.pow(2.5,4);
        assertEquals(q,f.predict(2.5),1e-6);
    }

    @Test public void ridgeLassoAndElasticNetAreExecutable(){
        double[] x={0.4,0.6,0.8,1.0,1.2,1.4,1.6,1.8};
        double[] y={900,700,560,450,370,310,265,230};
        assertNotNull(CalibrationRegressionModels.fit(CalibrationRegressionModels.RIDGE,x,y));
        assertNotNull(CalibrationRegressionModels.fit(CalibrationRegressionModels.LASSO,x,y));
        assertNotNull(CalibrationRegressionModels.fit(CalibrationRegressionModels.ELASTIC_NET,x,y));
    }

    @Test public void exponentialCurveMatchesPinnedScipyCurveFitGoldenVector(){
        double[] x={0.45,0.60,0.82,1.05,1.35,1.70,2.0,2.3};
        double[] y={12.8,13.5,14.7,16.2,18.8,22.5,27.1,33.0};
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.EXPONENTIAL,x,y);
        assertNotNull(f);
        assertEquals(3, f.coefficients.length);
        assertEquals(3.12823956,f.coefficients[0],1e-4);
        assertEquals(0.90094276,f.coefficients[1],1e-5);
        assertEquals(8.13754324,f.coefficients[2],1e-4);
    }

    @Test public void powerCurveMatchesPinnedScipyCurveFitGoldenVector(){
        double[] x={0.45,0.60,0.82,1.05,1.35,1.70,2.0,2.3};
        double[] y={830,610,455,355,270,215,185,165};
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.POWER,x,y);
        assertNotNull(f);
        assertEquals(3, f.coefficients.length);
        assertEquals(355.30081749,f.coefficients[0],1e-3);
        assertEquals(-1.03919362,f.coefficients[1],1e-5);
        assertEquals(13.07158156,f.coefficients[2],1e-3);
    }

    @Test public void powerRegressionRecoversSyntheticMqCurve(){
        double[] ratio={0.45,0.60,0.82,1.05,1.35,1.70};
        double[] ppm=new double[ratio.length];
        for(int i=0;i<ratio.length;i++)ppm[i]=525.0*Math.pow(ratio[i],-2.15);
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.POWER,ratio,ppm);
        assertNotNull(f);
        assertEquals(525.0,f.predict(1.0),1e-3);
        assertEquals(525.0*Math.pow(0.73,-2.15),f.predict(0.73),1e-3);
        assertTrue(ModelRuntimeRegistry.isUpstreamVerified(CalibrationRegressionModels.POWER));
    }

    @Test public void rankingPrefersPowerForNoOffsetPowerLawData(){
        double[] ratio={0.40,0.52,0.67,0.83,1.02,1.25,1.52,1.80};
        double[] ppm=new double[ratio.length];
        for(int i=0;i<ratio.length;i++)ppm[i]=180.0*Math.pow(ratio[i],-2.4);
        List<CalibrationRegressionModels.Score> scores=CalibrationRegressionModels.rank(ratio,ppm);
        assertFalse(scores.isEmpty());
        assertEquals(CalibrationRegressionModels.POWER,scores.get(0).fit.catalogIndex);
        assertTrue(scores.get(0).logRmse<1e-5);
        assertTrue(scores.get(0).r2>0.99999);
    }

    @Test public void theilSenResistsOneLargeOutlier(){
        double[] x={1,2,3,4,5,6};
        double[] y={12,14,16,18,20,200};
        CalibrationRegressionModels.Fit f=CalibrationRegressionModels.fit(CalibrationRegressionModels.THEIL_SEN,x,y);
        assertNotNull(f);
        assertEquals(22.0,f.predict(6.0),1e-9);
        assertTrue(ModelRuntimeRegistry.isApproximation(CalibrationRegressionModels.THEIL_SEN));
    }
}
