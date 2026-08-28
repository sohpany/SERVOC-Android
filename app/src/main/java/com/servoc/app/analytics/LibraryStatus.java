package com.servoc.app.analytics;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.SensorEvidenceRegistry;
import com.servoc.app.exposure.ExposureLimits;
import com.servoc.app.measurements.PhysicalCatalog;

public final class LibraryStatus {
    private LibraryStatus() {}
    public static final String ANALYTICS_VERSION="HazRob-ANALYTICS-2026.08-v3";
    public static final String CATALOG_VERSION="HAZROB-CHEM-144-v2";
    public static final String EXPOSURE_VERSION="HAZROB-OEL-source-v1";

    public static String summary(){
        return "Chemical catalog: "+ChemicalCatalog.COUNT+
                "\nPurchased MQ sensors: "+SensorEvidenceRegistry.SENSORS.length+
                "\nPhysical measurements: "+PhysicalCatalog.COUNT+
                "\nPrimary MQ log-power records: "+GasEngine.curveCount()+
                "\nSecondary research curve records: "+ResearchCurveRegistry.count()+
                "\nDirect curve chemicals: "+ChemicalCoverage.directCurveCount()+
                "\nFingerprint-training candidates: "+ChemicalCoverage.fingerprintCandidateCount()+
                "\nWeak/unvalidated MQ entries: "+ChemicalCoverage.weakOrUnvalidatedCount()+
                "\nExposure-covered chemicals: "+ExposureLimits.coveredChemicalCount()+
                "\nAnalytics library: "+ANALYTICS_VERSION;
    }
}
