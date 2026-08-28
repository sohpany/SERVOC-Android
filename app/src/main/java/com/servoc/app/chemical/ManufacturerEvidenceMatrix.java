package com.servoc.app.chemical;

/**
 * Manufacturer-evidence matrix for ONLY the eight MQ devices installed in HazRob.
 *
 * TARGET means the analyte/category is explicitly a target/detection-range species in
 * the current Winsen documentation. GRAPH_RESPONSE means it appears on a typical
 * sensitivity graph but is not necessarily a selective target. This distinction is
 * critical because a plotted cross-response does not turn an MQ sensor into a selective meter.
 */
public final class ManufacturerEvidenceMatrix {
    private ManufacturerEvidenceMatrix() {}

    public static final int NONE=0;
    public static final int GRAPH_RESPONSE=1;
    public static final int TARGET=2;

    public static final class Evidence {
        public final int level;
        public final double minPpm;
        public final double maxPpm;
        public final String source;
        public final String note;
        Evidence(int level,double minPpm,double maxPpm,String source,String note){
            this.level=level;this.minPpm=minPpm;this.maxPpm=maxPpm;this.source=source;this.note=note;
        }
    }

    public static Evidence get(int sensor,int chemical){
        switch(sensor){
            case GasEngine.MQ2:
                // Current Winsen MQ-2 typical graph: propane, methane, alcohol, smoke.
                if(chemical==78) return target(300,10000,"Winsen MQ-2 manual","Propane is a documented flammable-gas response/target species.");
                if(chemical==76) return target(300,10000,"Winsen MQ-2 manual","Methane is a documented flammable-gas response/target species.");
                if(chemical==139) return target(300,10000,"Winsen MQ-2 manual","Smoke is explicitly within the MQ-2 target category; ppm is operational rather than a pure-analyte concentration.");
                if(chemical==135) return target(300,10000,"Winsen MQ-2 product target: flammable gas/smoke","LPG is treated as a flammable-gas target category, but exact mixture composition affects response.");
                if(chemical==103) return graph(300,10000,"Winsen MQ-2 typical sensitivity graph","Alcohol appears on the graph as cross-response, not as a selective MQ-2 target.");
                break;

            case GasEngine.MQ3:
                // MQ-3B target ethanol; graph also shows CO and H2.
                if(chemical==103) return target(25,500,"Winsen MQ-3B manual","Ethanol/alcohol is the primary target.");
                if(chemical==20) return graph(50,500,"Winsen MQ-3B typical sensitivity graph","CO is plotted cross-response.");
                if(chemical==75) return graph(50,500,"Winsen MQ-3B typical sensitivity graph","H2 is plotted cross-response.");
                break;

            case GasEngine.MQ4:
                // MQ-4 target CH4/flammable gas; graph: CH4, alcohol, propane.
                if(chemical==76) return target(300,10000,"Winsen MQ-4 manual","Methane is the primary target.");
                if(chemical==78) return graph(300,10000,"Winsen MQ-4 typical sensitivity graph","Propane appears on the manufacturer graph as flammable-gas response.");
                if(chemical==103) return graph(300,10000,"Winsen MQ-4 typical sensitivity graph","Alcohol is plotted cross-response.");
                break;

            case GasEngine.MQ5:
                // MQ-5 product target: propane/LPG/methane; graph also shows ethanol.
                if(chemical==78||chemical==135||chemical==76)
                    return target(300,10000,"Winsen MQ-5 manual/product specification","Propane/LPG/methane are documented targets.");
                if(chemical==103) return graph(300,10000,"Winsen MQ-5 typical sensitivity graph","Ethanol is plotted cross-response.");
                break;

            case GasEngine.MQ7:
                // MQ-7B target CO; graph also shows CH4 and H2.
                if(chemical==20) return target(10,500,"Winsen MQ-7B manual","CO is the target; 60 s high / 90 s low heater cycle is required.");
                if(chemical==76) return graph(10,500,"Winsen MQ-7B typical sensitivity graph","Methane is plotted cross-response, not the target.");
                if(chemical==75) return graph(10,500,"Winsen MQ-7B typical sensitivity graph","Hydrogen is plotted cross-response, not the target.");
                break;

            case GasEngine.MQ8:
                // MQ-8 target H2; graph also shows alcohol, CO and CH4.
                if(chemical==75) return target(100,1000,"Winsen MQ-8 manual","Hydrogen is the primary target.");
                if(chemical==103) return graph(100,1000,"Winsen MQ-8 typical sensitivity graph","Alcohol is plotted cross-response.");
                if(chemical==20) return graph(100,1000,"Winsen MQ-8 typical sensitivity graph","CO is plotted cross-response.");
                if(chemical==76) return graph(100,1000,"Winsen MQ-8 typical sensitivity graph","Methane is plotted cross-response.");
                break;

            case GasEngine.MQ9:
                // MQ-9B target CO and CH4; graph also shows H2.
                if(chemical==20) return target(10,500,"Winsen MQ-9B manual","CO target; low-temperature detection phase is required.");
                if(chemical==76) return target(300,10000,"Winsen MQ-9B manual","Methane target; high-temperature phase is used for methane detection.");
                if(chemical==75) return graph(10,10000,"Winsen MQ-9B typical sensitivity graph","Hydrogen is plotted as cross-response.");
                break;

            case GasEngine.MQ135:
                // Product/manual: NH3, sulfide and benzene-series vapours; detection range
                // is explicitly specified for NH3, toluene, H2 and smoke.
                if(chemical==21) return target(10,1000,"Winsen MQ135 manual","Ammonia is an explicit target/range species.");
                if(chemical==95) return target(10,1000,"Winsen MQ135 manual","Toluene is an explicit detection-range species and representative benzene-series vapour.");
                if(chemical==75) return target(10,1000,"Winsen MQ135 manual","Hydrogen is explicitly listed in the detection range.");
                if(chemical==139) return target(10,1000,"Winsen MQ135 manual","Smoke is explicitly listed; it remains a composite operational metric.");
                if(chemical==34) return graph(10,1000,"Winsen MQ135 target category: benzene-series vapours","Benzene-series sensitivity is documented, but a compound-specific quantitative range must be calibrated.");
                if(chemical==4) return graph(10,1000,"Winsen MQ135 target category: sulfide","Sulfide sensitivity is documented; H2S mapping is only a research proxy until exact-gas calibration and compatibility validation.");
                break;
            default:
                break;
        }
        return new Evidence(NONE,Double.NaN,Double.NaN,"","No current manufacturer target/graph evidence registered for this sensor/analyte pair.");
    }

    public static int level(int sensor,int chemical){return get(sensor,chemical).level;}
    public static boolean isTarget(int sensor,int chemical){return level(sensor,chemical)==TARGET;}
    public static boolean isGraphResponse(int sensor,int chemical){return level(sensor,chemical)==GRAPH_RESPONSE;}

    private static Evidence target(double min,double max,String source,String note){return new Evidence(TARGET,min,max,source,note);}
    private static Evidence graph(double min,double max,String source,String note){return new Evidence(GRAPH_RESPONSE,min,max,source,note);}
}
