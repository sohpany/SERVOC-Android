package com.servoc.app.chemcore;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChemCoreAnalyticalObjectSemanticsTest {
    @Test public void fixedPanelObjectTypesMatchStage4Taxonomy(){
        for(int i=0;i<135;i++)assertEquals(ChemCoreAnalyticalObjectSemantics.Type.PURE_ANALYTE,ChemCoreAnalyticalObjectSemantics.typeForCatalogIndex(i));
        for(int i=135;i<=139;i++)assertEquals(ChemCoreAnalyticalObjectSemantics.Type.MIXTURE,ChemCoreAnalyticalObjectSemantics.typeForCatalogIndex(i));
        for(int i=140;i<=142;i++)assertEquals(ChemCoreAnalyticalObjectSemantics.Type.AGGREGATE_METRIC,ChemCoreAnalyticalObjectSemantics.typeForCatalogIndex(i));
        assertEquals(ChemCoreAnalyticalObjectSemantics.Type.MIXTURE,ChemCoreAnalyticalObjectSemantics.typeForCatalogIndex(143));
    }

    @Test public void mixturesAndAggregateMetricsDoNotInheritPureAnalytePpm(){
        assertTrue(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(103)); // Ethanol
        assertFalse(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(135)); // LPG
        assertFalse(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(139)); // Smoke
        assertFalse(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(140)); // TVOC
        assertFalse(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(141)); // NOx
        assertFalse(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(142)); // LEL
        assertFalse(ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(143)); // R410A
    }
}
