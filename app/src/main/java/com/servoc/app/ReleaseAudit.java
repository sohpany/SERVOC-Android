package com.servoc.app;

/** Build marker for the HazRob 0.6 architecture migration. */
public final class ReleaseAudit {
    private ReleaseAudit(){}
    public static final String VERSION="0.6.0";
    public static final String ESP32_API="HazRob API v3: compact /diag + /data adapter";
    public static final boolean PRECALIBRATED_MQ=false;
    public static final boolean ARABIC_RTL_UI=true;
    public static final boolean ROLE_GATE=true;
    public static final boolean UNIFIED_MEASUREMENT_UI=true;
    public static final boolean DYNAMIC_CHEMICAL_SCHEMA=true;
    public static final boolean BME_BMP_DIAGNOSTICS=true;
    public static final boolean REFERENCE_BANK_EXPORT=true;
    public static final boolean MOTION_BREADCRUMB_RECORDING=true;
    public static final boolean AUTONOMOUS_BACKTRACK=false;
}
