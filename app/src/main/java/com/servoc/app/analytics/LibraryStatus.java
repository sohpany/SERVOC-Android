package com.servoc.app.analytics;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.exposure.ExposureLimits;
import com.servoc.app.measurements.PhysicalCatalog;

public final class LibraryStatus {
    private LibraryStatus() {}
    public static final String ANALYTICS_VERSION="2026.08-v1";
    public static final String CATALOG_VERSION="SERVOC-CHEM-144-v1";
    public static final String EXPOSURE_VERSION="SERVOC-OEL-source-v1";
    public static String summary(){
        return "Chemical catalog: "+ChemicalCatalog.COUNT+
                "\nPhysical measurements: "+PhysicalCatalog.COUNT+
                "\nMQ curve records: "+GasEngine.curveCount()+
                "\nCurve-supported chemicals: "+GasEngine.SUPPORTED.length+
                "\nExposure-covered chemicals: "+ExposureLimits.coveredChemicalCount()+
                "\nAnalytics library: "+ANALYTICS_VERSION;
    }
}
