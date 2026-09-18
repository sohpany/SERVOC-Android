package com.servoc.app.analytics;

import com.servoc.app.chemcore.ChemCoreFeaturePipeline;
import org.junit.Test;
import static org.junit.Assert.*;

public class TemporalFeatureExtractorCanonicalAdapterTest {
    @Test public void legacyViewIsMappedFromCanonicalContract(){
        double[][] r={{1.0},{1.0},{1.1},{1.2},{1.3},{1.4},{1.5},{1.6}};
        ChemCoreFeaturePipeline.Result c=ChemCoreFeaturePipeline.extract(null,r);
        double[] legacy=TemporalFeatureExtractor.extract(r);
        assertEquals(TemporalFeatureExtractor.FEATURES_PER_SENSOR,legacy.length);
        assertEquals(c.feature(0,0),legacy[0],1e-12);
        assertEquals(c.feature(0,1),legacy[1],1e-12);
        assertEquals(c.feature(0,2),legacy[2],1e-12);
        assertEquals(c.feature(0,3),legacy[3],1e-12);
        assertEquals(c.feature(0,5),legacy[4],1e-12);
        assertEquals(c.feature(0,6),legacy[5],1e-12);
        assertEquals(c.feature(0,8),legacy[6],1e-12);
        assertEquals(c.feature(0,14),legacy[7],1e-12);
    }
}
