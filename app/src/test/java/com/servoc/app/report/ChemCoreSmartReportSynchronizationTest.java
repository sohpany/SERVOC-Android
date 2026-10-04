package com.servoc.app.report;

import com.servoc.app.chemcore.ChemCoreReportContract;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration lock between the analytical engine and the single SmartReport engine.
 * Any ChemCore report-contract change must be acknowledged here and in SmartReportAnalyticalContract.
 */
public final class ChemCoreSmartReportSynchronizationTest {
    @Test public void smartReportMustExplicitlyMatchCurrentChemCoreContract(){
        assertEquals(ChemCoreReportContract.VERSION,SmartReportAnalyticalContract.EXPECTED_CHEMCORE_VERSION);
        assertTrue(ChemCoreReportContract.SCHEMA_REVISION>0);
        assertTrue(SmartReportAnalyticalContract.SCHEMA_REVISION>0);
    }

    @Test public void smartReportV2CarriesAdvancedAnalyticalCapabilities(){
        assertEquals("HR069-SMARTREPORT-2",SmartReportAnalyticalContract.VERSION);
        assertEquals(2,SmartReportAnalyticalContract.SCHEMA_REVISION);
        assertNotNull(SmartReportAnalyticalContract.Capability.valueOf("CROSS_CHANNEL_CORRELATION"));
        assertNotNull(SmartReportAnalyticalContract.Capability.valueOf("COMMON_C_CURVE_CONSISTENCY"));
        assertNotNull(SmartReportAnalyticalContract.Capability.valueOf("MOLECULAR_QSPR"));
        assertNotNull(SmartReportAnalyticalContract.Capability.valueOf("QUANTITATIVE_PPM"));
    }
}