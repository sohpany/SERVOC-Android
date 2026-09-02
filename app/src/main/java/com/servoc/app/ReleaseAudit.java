package com.servoc.app;

/** Build markers for the HazRob 0.8 hardware-aligned developer release. */
public final class ReleaseAudit {
    private ReleaseAudit(){}
    public static final String VERSION="0.8.0";
    public static final int DATABASE_SCHEMA=7;
    public static final String ESP32_API="HazRob API v3 + two installed MQ groups / dual ADS1115 / manual relay power";
    public static final boolean PRECALIBRATED_MQ=false;
    public static final boolean ARABIC_RTL_UI=true;
    public static final boolean ROLE_GATE=true;
    public static final boolean UNIFIED_MEASUREMENT_UI=true;
    public static final boolean DYNAMIC_CHEMICAL_SCHEMA=true;
    public static final boolean DYNAMIC_DEVELOPER_CHEMICAL_SELECTION=true;
    public static final boolean BME_BMP_DIAGNOSTICS=true;
    public static final boolean DUAL_ADS1115=true;
    public static final boolean TWO_MQ_POWER_BANKS=true;
    public static final boolean FOUR_MQ_RELAY_ROWS=false;
    public static final boolean MANUAL_MQ_RELAY_CONTROL=true;
    public static final boolean MANUAL_MQ_WARMUP_SETTING=true;
    public static final boolean IR_WARNING_ONLY=true;
    public static final boolean DIGITAL_LDR_TELEMETRY=false;
    public static final boolean SOUND_RAW_RMS=true;
    public static final boolean NIOSH_CHEMICAL_SEED=true;
    public static final boolean NIOSH_IDLH_SEED=true;
    public static final boolean OSHA_Z1_PEL_SEED=true;
    public static final boolean VIDEO_RECORDING=true;
    public static final boolean DIRECT_ESP_CAM_MJPEG=true;
    public static final boolean CAMERA_ENDPOINT_FALLBACK=true;
    public static final boolean VIDEO_STANDALONE_FROM_REPORTS=true;
    public static final boolean FLAME_EVIDENCE_EXPERIMENTAL=true;
    public static final boolean VIDEO_LINKED_TO_MEASUREMENTS=false;
    public static final boolean SENSOR_HEALTH_DRIFT=true;
    public static final boolean GAS_DIRECTION_ASSIST=true;
    public static final boolean GAS_TRACKING_RELAY_BANK_CONTROL=false;
    public static final boolean GAS_TRACKING_RELAY_RECOMMENDATION=true;
    public static final boolean REFERENCE_BANK_EXPORT=true;
    public static final boolean DIRECT_GITHUB_PUBLISH=true;
    public static final boolean GITHUB_CREDENTIAL_PERSISTED=false;
    public static final boolean MOTION_BREADCRUMB_RECORDING=true;
    public static final boolean AUTONOMOUS_BACKTRACK=true;
}
