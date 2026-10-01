package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class MahalanobisMissingFeatureTest {
    @Test public void partialVectorUsesMarginalCovarianceNotPrecisionPrincipalBlock(){
        double[][] inverse={{2.777777777777778,-2.2222222222222223},{-2.2222222222222223,2.777777777777778}};
        MahalanobisModel model=MahalanobisModel.fromFrozenParameters(new double[]{0,0},inverse,0,100);
        double d=model.distance(new double[]{1.0,Double.NaN});
        // Marginal variance of feature 1 is 1.0. Coverage scaling D/d = 2 therefore gives D²=2.
        assertEquals(Math.sqrt(2.0),d,1e-9);
        // The old incorrect precision-principal-block path would have produced sqrt(5.555...),
        // so this assertion protects the marginal-covariance correction.
        assertTrue(Math.abs(d-Math.sqrt(5.555555555555556))>0.5);
        assertEquals("marginal_covariance_subspace_with_dimension_coverage_scaling",MahalanobisModel.MISSING_FEATURE_SEMANTICS);
    }

    @Test public void completeVectorRetainsFullPrecisionDistance(){
        double[][] inverse={{2.777777777777778,-2.2222222222222223},{-2.2222222222222223,2.777777777777778}};
        MahalanobisModel model=MahalanobisModel.fromFrozenParameters(new double[]{0,0},inverse,0,100);
        assertEquals(Math.sqrt(2.777777777777778),model.distance(new double[]{1.0,0.0}),1e-9);
    }
}
