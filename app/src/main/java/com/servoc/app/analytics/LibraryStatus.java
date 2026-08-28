package com.servoc.app.analytics;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.MqSensorAffinity;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.SensorEvidenceRegistry;
import com.servoc.app.exposure.ExposureLimits;
import com.servoc.app.measurements.PhysicalCatalog;

public final class LibraryStatus {
    private LibraryStatus() {}
    public static final String ANALYTICS_VERSION="HazRob-ANALYTICS-2026.08-v5";
    public static final String CATALOG_VERSION="HAZROB-CHEM-144-v3";
    public static final String EXPOSURE_VERSION="HAZROB-OEL-source-v1";
    public static final String CALIBRATION_VERSION="HAZROB-MQ-CAL-v1";

    public static String summary(){
        return "Chemical catalog: "+ChemicalCatalog.COUNT+
                "\nPurchased MQ sensors: "+SensorEvidenceRegistry.SENSORS.length+
                "\nPhysical measurements: "+PhysicalCatalog.COUNT+
                "\nInherited MQ power-law records: "+GasEngine.curveCount()+
                "\nSecondary research curve records: "+ResearchCurveRegistry.count()+
                "\nTarget-backed curve chemicals: "+ChemicalCoverage.directCurveCount()+
                "\nManufacturer-graph curve chemicals: "+ChemicalCoverage.graphCurveChemicalCount()+
                "\nCommunity-only curve chemicals: "+ChemicalCoverage.communityOnlyCurveChemicalCount()+
                "\nFingerprint/calibration candidates: "+ChemicalCoverage.fingerprintCandidateCount()+
                "\nComposite/mixture entries: "+ChemicalCoverage.aggregateCount()+
                "\nWeak/unsupported entries: "+ChemicalCoverage.weakOrUnvalidatedCount()+
                "\nAny MQ training plan: "+MqSensorAffinity.supportedTrainingCount()+" / "+ChemicalCatalog.COUNT+
                "\nExposure-covered chemicals: "+ExposureLimits.coveredChemicalCount()+
                "\nCalibration engine: "+CALIBRATION_VERSION+
                "\nAnalytics library: "+ANALYTICS_VERSION;
    }
}
