package com.servoc.app.chemical;

/**
 * Evidence registry for the eight MQ sensors already used by HazRob.
 * These records describe manufacturer-stated targets/ranges; they do not imply selectivity.
 */
public final class SensorEvidenceRegistry {
    private SensorEvidenceRegistry() {}

    public static final class SensorSpec {
        public final int id;
        public final String model;
        public final String targets;
        public final String range;
        public final String heaterProtocol;
        public final String source;

        SensorSpec(int id, String model, String targets, String range, String heaterProtocol, String source) {
            this.id=id; this.model=model; this.targets=targets; this.range=range;
            this.heaterProtocol=heaterProtocol; this.source=source;
        }
    }

    public static final SensorSpec[] SENSORS = {
        new SensorSpec(GasEngine.MQ2, "MQ-2", "flammable gases, smoke; typical curves include propane/methane/alcohol/smoke",
                "300-10000 ppm flammable gas", "5 V heater, constant-temperature operation",
                "Winsen MQ-2 Ver1.6 manual"),
        new SensorSpec(GasEngine.MQ3, "MQ-3 / MQ-3B", "ethanol; cross-response curves include CO and H2",
                "manufacturer alcohol range; variant-dependent", "5 V heater, constant-temperature operation",
                "Winsen MQ-3B manual"),
        new SensorSpec(GasEngine.MQ4, "MQ-4", "methane/natural gas; typical curves include propane and alcohol",
                "300-10000 ppm CH4", "5 V heater, constant-temperature operation",
                "Winsen MQ-4 manual"),
        new SensorSpec(GasEngine.MQ5, "MQ-5", "LPG, methane, propane",
                "300-10000 ppm CH4/C3H8", "5 V heater, constant-temperature operation",
                "Winsen MQ-5 manual"),
        new SensorSpec(GasEngine.MQ7, "MQ-7 / MQ-7B", "carbon monoxide; cross-response to CH4 and H2",
                "10-500 ppm CO", "1.5 V detection phase; 5 V recovery/cleaning phase on MQ-7B",
                "Winsen MQ-7B manual"),
        new SensorSpec(GasEngine.MQ8, "MQ-8", "hydrogen",
                "100-1000 ppm H2", "5 V heater, constant-temperature operation",
                "Winsen MQ-8 manual"),
        new SensorSpec(GasEngine.MQ9, "MQ-9 / MQ-9B", "carbon monoxide and methane",
                "10-500 ppm CO; 300-10000 ppm CH4", "60 s at 5 V high temperature; 90 s at 1.5 V low temperature on MQ-9B",
                "Winsen MQ-9B manual"),
        new SensorSpec(GasEngine.MQ135, "MQ-135", "NH3, sulfide, benzene-series vapours, H2 and smoke/air quality",
                "10-1000 ppm for manufacturer-listed targets", "5 V heater, constant-temperature operation",
                "Winsen MQ135 Ver1.6 manual")
    };

    public static SensorSpec byId(int id) {
        for (SensorSpec s : SENSORS) if (s.id == id) return s;
        return null;
    }
}
