package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChemCoreFeaturePipelineTest {
    @Test public void signedResponseDirectionIsPreserved(){
        double[] t={0,1,2,3,4,5,6,7};
        double[][] r={
                {1.00,2.00},{1.00,2.00},{1.05,1.95},{1.15,1.80},
                {1.30,1.60},{1.45,1.40},{1.55,1.25},{1.60,1.20}
        };
        ChemCoreFeaturePipeline.Result x=ChemCoreFeaturePipeline.extract(t,r);
        assertEquals(2,x.sensorCount);
        assertEquals(8,x.sampleCount);
        assertTrue(x.physicalTime);
        assertEquals(2*ChemCoreFeaturePipeline.FEATURES_PER_SENSOR,x.vector.length);
        assertTrue(x.feature(0,3)>0);
        assertTrue(x.feature(1,3)<0);
        assertEquals(Math.abs(x.feature(0,3)),x.feature(0,4),1e-12);
        assertEquals(Math.abs(x.feature(1,3)),x.feature(1,4),1e-12);
    }

    @Test public void kineticFeaturesUseSuppliedSeconds(){
        double[] t={0,0.5,1,1.5,2,2.5,3,3.5,4,4.5};
        double[][] r=new double[t.length][1];
        for(int i=0;i<t.length;i++)r[i][0]=i<2?1.0:1.0+0.12*(i-1);
        ChemCoreFeaturePipeline.Result x=ChemCoreFeaturePipeline.extract(t,r);
        assertTrue(Double.isFinite(x.feature(0,8)));
        assertTrue(Double.isFinite(x.feature(0,10)));
        assertTrue(Double.isFinite(x.feature(0,11)));
        assertTrue(Double.isFinite(x.feature(0,12)));
        assertTrue(x.feature(0,12)>=x.feature(0,11));
        assertTrue(x.feature(0,11)>=x.feature(0,10));
        assertTrue(x.feature(0,14)>0);
    }

    @Test public void insufficientSensorSamplesRemainExplicitlyMissing(){
        double[][] r={{1.0},{Double.NaN},{1.1}};
        ChemCoreFeaturePipeline.Result x=ChemCoreFeaturePipeline.extract(null,r);
        assertFalse(x.physicalTime);
        assertEquals(2.0/3.0,x.feature(0,0),1e-12);
        assertTrue(Double.isNaN(x.feature(0,1)));
        assertTrue(Double.isNaN(x.feature(0,3)));
    }

    @Test public void featureNamesAreStableAndScoped(){
        assertEquals("mq3.signed_delta_log10",ChemCoreFeaturePipeline.featureName(1,3,"MQ3"));
        assertEquals("sensor0.t50_s",ChemCoreFeaturePipeline.featureName(0,11,null));
    }
}
