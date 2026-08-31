package com.servoc.app;

/** Build marker for the integration-audited HazRob v0.5.0 release. */
public final class ReleaseAudit {
    private ReleaseAudit(){}
    public static final String VERSION="0.5.0";
    public static final String ESP32_API="Servoc_Main3 API v2 / legacy v1 compatible";
    public static final boolean PRECALIBRATED_MQ=true;
    public static final boolean ARABIC_RTL_UI=true;
    public static final boolean BME_BMP_DIAGNOSTICS=true;
    public static final boolean REFERENCE_BANK_EXPORT=true;
}
