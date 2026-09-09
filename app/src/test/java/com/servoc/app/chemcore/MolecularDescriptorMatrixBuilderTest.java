package com.servoc.app.chemcore;

import org.junit.Test;

import static org.junit.Assert.*;

public class MolecularDescriptorMatrixBuilderTest {
    @Test public void standardizerCentersCompleteTrainingMatrix(){
        String[] names={"MolecularWeight","TPSA"};
        double[][] rows={{10.0,1.0},{20.0,3.0},{30.0,5.0}};
        MolecularDescriptorMatrixBuilder.Standardizer s=
                MolecularDescriptorMatrixBuilder.fitStandardizer(names,rows);
        double[] a=s.transform(rows[0]),b=s.transform(rows[1]),c=s.transform(rows[2]);
        assertEquals(0.0,a[0]+b[0]+c[0],1e-12);
        assertEquals(0.0,a[1]+b[1]+c[1],1e-12);
        assertEquals(20.0,s.mean[0],1e-12);
        assertEquals(3.0,s.mean[1],1e-12);
    }

    @Test public void zeroVarianceColumnsRemainFiniteAndCentered(){
        String[] names={"Charge"};
        double[][] rows={{0.0},{0.0},{0.0}};
        MolecularDescriptorMatrixBuilder.Standardizer s=
                MolecularDescriptorMatrixBuilder.fitStandardizer(names,rows);
        assertEquals(1.0,s.standardDeviation[0],1e-12);
        assertEquals(0.0,s.transform(new double[]{0.0})[0],1e-12);
    }

    @Test(expected=IllegalArgumentException.class)
    public void nonFiniteTrainingValuesAreRejected(){
        MolecularDescriptorMatrixBuilder.fitStandardizer(
                new String[]{"TPSA"},new double[][]{{1.0},{Double.NaN}});
    }
}
