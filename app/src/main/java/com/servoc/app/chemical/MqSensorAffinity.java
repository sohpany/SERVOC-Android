package com.servoc.app.chemical;

/**
 * Training-channel planner for the 144-entry HazRob catalogue using ONLY the
 * currently installed MQ-2/3/4/5/7/8/9/135 array.
 *
 * A non-zero mask means "record these channels during controlled experiments";
 * it does NOT mean that a selective quantitative ppm equation already exists.
 */
public final class MqSensorAffinity {
    private MqSensorAffinity() {}

    public static final int B_MQ2   = 1 << GasEngine.MQ2;
    public static final int B_MQ3   = 1 << GasEngine.MQ3;
    public static final int B_MQ4   = 1 << GasEngine.MQ4;
    public static final int B_MQ5   = 1 << GasEngine.MQ5;
    public static final int B_MQ7   = 1 << GasEngine.MQ7;
    public static final int B_MQ8   = 1 << GasEngine.MQ8;
    public static final int B_MQ9   = 1 << GasEngine.MQ9;
    public static final int B_MQ135 = 1 << GasEngine.MQ135;
    public static final int ALL = B_MQ2|B_MQ3|B_MQ4|B_MQ5|B_MQ7|B_MQ8|B_MQ9|B_MQ135;

    public static final class Plan {
        public final int mask;
        public final String className;
        public final String reason;
        public final boolean quantitativeCurvePresent;

        Plan(int mask,String className,String reason,boolean quantitativeCurvePresent){
            this.mask=mask;this.className=className;this.reason=reason;
            this.quantitativeCurvePresent=quantitativeCurvePresent;
        }

        public boolean uses(int sensorId){return sensorId>=0&&sensorId<8&&(mask&(1<<sensorId))!=0;}
    }

    public static Plan forChemical(int chem){
        if(chem<0||chem>=ChemicalCatalog.COUNT)
            return new Plan(0,"invalid","Invalid catalogue index.",false);

        if(hasDirectCurve(chem)){
            return new Plan(maskForDirectChemical(chem),"direct-library",
                    "At least one Rs/R0 power-law equation already exists. Record all relevant installed MQ channels for consensus and future device-specific calibration.",true);
        }

        // Highly oxidizing/corrosive/reactive species: do not assume safe exposure of the MQ array.
        if(contains(new int[]{0,10,11,12,13,14,15,16,17,18,19,42,43,44,45,46,47,60,61},chem))
            return new Plan(0,"compatibility-review",
                    "No enabled MQ training plan. Material may be strongly oxidizing/corrosive/reactive or unsupported; sensor/chamber compatibility must be reviewed before exposure.",false);

        // Gases with little/no useful response basis in this installed SnO2 array.
        if(contains(new int[]{22,24,49,123,124,125,126,127,128,129,130,131,132,133,134,143},chem))
            return new Plan(0,"unsupported-current-array",
                    "No evidence-backed training channel in the current MQ array. Retain in catalogue but do not fabricate a ppm model.",false);

        // CO family.
        if(chem==20)
            return new Plan(B_MQ7|B_MQ9|B_MQ2|B_MQ5|B_MQ135,"CO",
                    "MQ-7/MQ-9 are primary CO channels; other channels are cross-response/fingerprint support.",true);

        // Hydrogen.
        if(chem==75)
            return new Plan(B_MQ8|B_MQ2|B_MQ5|B_MQ7|B_MQ135,"hydrogen",
                    "MQ-8 is the primary H2 channel; MQ-2/MQ-5/MQ-7/MQ-135 add fingerprint evidence.",true);

        // Methane, light hydrocarbons, combustible gas and operational mixtures.
        if((chem>=76&&chem<=94) || chem==135 || chem==136 || chem==137 || chem==138 || chem==142)
            return new Plan(B_MQ2|B_MQ4|B_MQ5|B_MQ9|B_MQ8,"combustible-hydrocarbon",
                    "MQ-4/MQ-5 and MQ-9 are strong methane/flammable channels; MQ-2 and MQ-8 contribute array-pattern information. Mixture entries require fingerprint/mixture training.",false);

        // Alcohols: MQ-3 is primary, with broad SnO2 cross-response retained as a signature.
        if(chem>=102&&chem<=109)
            return new Plan(B_MQ3|B_MQ2|B_MQ5|B_MQ8|B_MQ135,"alcohol/VOC",
                    "MQ-3 is the principal alcohol channel; MQ-2/MQ-5/MQ-8/MQ-135 can improve discrimination after controlled training.",false);

        // Aromatic hydrocarbons.
        if((chem>=95&&chem<=101) || chem==34 || chem==50 || chem==51)
            return new Plan(B_MQ135|B_MQ3|B_MQ2,"aromatic-VOC",
                    "MQ-135 has manufacturer-stated sensitivity to benzene-series vapours; MQ-3/MQ-2 are fingerprint support, not selective analyte meters.",false);

        // Ketones, aldehydes, esters, ethers and other oxygenated VOCs.
        if((chem>=25&&chem<=33) || (chem>=54&&chem<=59) || (chem>=71&&chem<=74) || (chem>=110&&chem<=122))
            return new Plan(B_MQ135|B_MQ3|B_MQ2|B_MQ5,"oxygenated-VOC",
                    "Broad MOS cross-sensitivity can provide a multi-channel signature. Concentration requires controlled analyte-specific calibration; no universal equation is assumed.",false);

        // Sulfur-containing reducing gases/vapours. MQ-135 lists sulfide sensitivity but selectivity is weak.
        if(chem==4 || (chem>=36&&chem<=41))
            return new Plan(B_MQ135|B_MQ2|B_MQ5,"sulfur/reducing",
                    "MQ-135 has sulfide sensitivity and combustible MQ channels may respond. Use controlled low-risk compatibility testing and fingerprinting; do not infer selective ppm from cross-response.",false);

        // Ammonia and nitrogen-containing VOCs.
        if(chem==21 || chem==26 || chem==29 || chem==32 || chem==52 || chem==53)
            return new Plan(B_MQ135|B_MQ3,"nitrogen-containing",
                    "MQ-135 is the installed channel with manufacturer target evidence for ammonia; other nitrogen compounds remain empirical fingerprint candidates.",false);

        // Halogenated organics not already excluded: possible MOS response is compound-dependent and often weak.
        if(chem==33 || chem==48 || (chem>=62&&chem<=70))
            return new Plan(B_MQ135|B_MQ2,"halogenated-organic",
                    "Potential broad MOS response only. Treat as exploratory fingerprint data unless controlled calibration demonstrates repeatable monotonic response.",false);

        // Aggregate air-quality outputs.
        if(chem==139 || chem==140)
            return new Plan(B_MQ2|B_MQ135|B_MQ3|B_MQ5,"aggregate-air-quality",
                    "Smoke/TVOC are composite metrics. Use array response or calibrated operational index rather than a single pure-gas equation.",false);
        if(chem==141)
            return new Plan(0,"aggregate-NOx",
                    "The current installed MQ array has no dedicated oxidizing-NOx channel; keep unsupported rather than infer NOx from unrelated sensors.",false);

        return new Plan(B_MQ135|B_MQ2,"exploratory-MOS",
                "No direct library curve. Record only as exploratory multi-sensor response and promote to a model after controlled repeatability and calibration tests.",false);
    }

    public static int supportedTrainingCount(){
        int n=0;for(int i=0;i<ChemicalCatalog.COUNT;i++)if(forChemical(i).mask!=0)n++;return n;
    }
    public static int unsupportedCount(){return ChemicalCatalog.COUNT-supportedTrainingCount();}

    private static boolean hasDirectCurve(int chem){for(int c:GasEngine.SUPPORTED)if(c==chem)return true;return false;}

    private static int maskForDirectChemical(int chem){
        if(chem==20) return B_MQ7|B_MQ9|B_MQ2|B_MQ5|B_MQ135;
        if(chem==21) return B_MQ135;
        if(chem==23) return B_MQ135;
        if(chem==34) return B_MQ3|B_MQ135;
        if(chem==75) return B_MQ8|B_MQ2|B_MQ5|B_MQ7;
        if(chem==76) return B_MQ4|B_MQ5|B_MQ9|B_MQ2;
        if(chem==78) return B_MQ2|B_MQ5;
        if(chem==84) return B_MQ3|B_MQ2|B_MQ5;
        if(chem==95) return B_MQ135|B_MQ3;
        if(chem==103) return B_MQ3|B_MQ2|B_MQ5|B_MQ135;
        if(chem==110) return B_MQ135|B_MQ3;
        if(chem==135) return B_MQ2|B_MQ4|B_MQ5|B_MQ9;
        if(chem==139) return B_MQ2|B_MQ135;
        return ALL;
    }

    private static boolean contains(int[] a,int v){for(int x:a)if(x==v)return true;return false;}
}
