package com.servoc.app.chemical;

import java.util.ArrayList;
import java.util.List;

/**
 * Research-evidence coverage map for the 144-entry legacy catalogue.
 *
 * This class answers a planning question: "does credible external evidence already exist
 * that can reduce discovery work for this analyte with MQ/MOS arrays?" It deliberately
 * does NOT answer "is HR069 quantitatively calibrated?". Every record below has m4=false.
 */
public final class ExternalEvidenceCoverage {
    private ExternalEvidenceCoverage() {}

    public static final int NONE=0;
    public static final int MANUFACTURER_QUALITATIVE_TARGET=1;
    public static final int SAME_ARRAY_FINGERPRINT=2;
    public static final int KNOWN_CONCENTRATION_RESPONSE=3;
    public static final int PUBLISHED_RS_R0_CURVE=4;

    public static final class Evidence {
        public final int chemical,level;
        public final String sourceId,sourceType,sensorScope,domain,note;
        public final boolean m4;
        Evidence(int chemical,int level,String sourceId,String sourceType,String sensorScope,String domain,String note){
            this.chemical=chemical;this.level=level;this.sourceId=sourceId;this.sourceType=sourceType;
            this.sensorScope=sensorScope;this.domain=domain;this.note=note;this.m4=false;
        }
    }

    public static final Evidence[] RECORDS={
        // Peer-reviewed MQ135 Rs/R0 graph regressions (Umbu 2023, 10-100 ppm).
        e(20,PUBLISHED_RS_R0_CURVE,"UMBU_MQ135_2023","peer-reviewed datasheet graph regression","MQ135","10-100 ppm","CO; published Eq. 21; generic graph prior only"),
        e(21,PUBLISHED_RS_R0_CURVE,"UMBU_MQ135_2023","peer-reviewed datasheet graph regression","MQ135","10-100 ppm","NH3; published Eq. 20"),
        e(23,PUBLISHED_RS_R0_CURVE,"UMBU_MQ135_2023","peer-reviewed datasheet graph regression","MQ135","10-100 ppm","CO2 cross-response; not a selective CO2 meter"),
        e(95,PUBLISHED_RS_R0_CURVE,"UMBU_MQ135_2023","peer-reviewed datasheet graph regression","MQ135","10-100 ppm","Toluene; published Eq. 17"),
        e(103,PUBLISHED_RS_R0_CURVE,"UMBU_MQ135_2023","peer-reviewed datasheet graph regression","MQ135","10-100 ppm","Published alcohol curve mapped to ethanol legacy entry"),
        e(110,PUBLISHED_RS_R0_CURVE,"UMBU_MQ135_2023","peer-reviewed datasheet graph regression","MQ135","10-100 ppm","Acetone; published Eq. 16"),

        // UCI low-concentration array: the source explicitly states these levels are below nominal sensor detection limits.
        e(84,KNOWN_CONCENTRATION_RESPONSE,"UCI_GSALC_2023","public CC BY 4.0 known-concentration time series","MQ135 + 9 MOS sensors","50/100/200 ppb; 1 Hz; baseline+injection","n-Hexane. Useful for class/shape evidence, not transferable ppm calibration."),
        e(95,KNOWN_CONCENTRATION_RESPONSE,"UCI_GSALC_2023","public CC BY 4.0 known-concentration time series","MQ135 + 9 MOS sensors","50/100/200 ppb; 1 Hz; baseline+injection","Toluene response evidence below nominal detection limit."),
        e(103,KNOWN_CONCENTRATION_RESPONSE,"UCI_GSALC_2023","public CC BY 4.0 known-concentration time series","MQ135 + 9 MOS sensors","50/100/200 ppb; 1 Hz; baseline+injection","Ethanol response evidence below nominal detection limit."),
        e(105,KNOWN_CONCENTRATION_RESPONSE,"UCI_GSALC_2023","public CC BY 4.0 known-concentration time series","MQ135 + 9 MOS sensors","50/100/200 ppb; 1 Hz; baseline+injection","Isopropanol external concentration-labelled evidence."),
        e(110,KNOWN_CONCENTRATION_RESPONSE,"UCI_GSALC_2023","public CC BY 4.0 known-concentration time series","MQ135 + 9 MOS sensors","50/100/200 ppb; 1 Hz; baseline+injection","Acetone response evidence below nominal detection limit."),
        e(115,KNOWN_CONCENTRATION_RESPONSE,"UCI_GSALC_2023","public CC BY 4.0 known-concentration time series","MQ135 + 9 MOS sensors","50/100/200 ppb; 1 Hz; baseline+injection","Ethyl acetate external concentration-labelled evidence."),

        // Peer-reviewed 2026 VOC array includes both MQ135 and MQ3 among 16 channels.
        e(103,KNOWN_CONCENTRATION_RESPONSE,"FRONTIERS_VOC_2026","peer-reviewed concentration-labelled e-nose experiment","MQ135 + MQ3 + 14 other MOS channels","1,3,5,7,9,11,13,15 ppm; 25+/-3 C; 50+/-10% RH","Ethanol pure and equimolar-mixture series; source data/model evidence is external, not HR069 calibration."),
        e(105,KNOWN_CONCENTRATION_RESPONSE,"FRONTIERS_VOC_2026","peer-reviewed concentration-labelled e-nose experiment","MQ135 + MQ3 + 14 other MOS channels","1,3,5,7,9,11,13,15 ppm; 25+/-3 C; 50+/-10% RH","Isopropanol pure and mixture concentration series."),
        e(110,KNOWN_CONCENTRATION_RESPONSE,"FRONTIERS_VOC_2026","peer-reviewed concentration-labelled e-nose experiment","MQ135 + MQ3 + 14 other MOS channels","1,3,5,7,9,11,13,15 ppm; 25+/-3 C; 50+/-10% RH","Acetone pure and mixture concentration series."),

        // Exact HR069 eight channels plus MQ6; highly useful fingerprint-transfer evidence, not device calibration.
        e(103,SAME_ARRAY_FINGERPRINT,"VISUAL_SMELL_2026","peer-reviewed exact/superset MQ-array fingerprint study","MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ135","pure ethanol, repeated array measurements","All eight HR069 MQ models are present; response magnitudes/slopes/ratios are setup-dependent."),
        e(110,SAME_ARRAY_FINGERPRINT,"VISUAL_SMELL_2026","peer-reviewed exact/superset MQ-array fingerprint study","MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ135","pure acetone, repeated array measurements","All eight HR069 MQ models are present; strong source for feature design/fingerprint transfer validation."),

        // Canonical public seven-MQ fingerprint dataset. The later TakMashhido copy is not counted independently.
        e(139,SAME_ARRAY_FINGERPRINT,"MULTIMODALGASDATA_2022","peer-reviewed public MQ-array dataset","MQ2/MQ3/MQ5/MQ6/MQ7/MQ8/MQ135","6400 samples; 1600/class; Smoke/Perfume/Mixture/NoGas","Canonical CC BY 4.0 smoke fingerprint benchmark. Six of eight HR069 sensors overlap; MQ4/MQ9 are missing and MQ6 is extra. Composite class only, not pure-gas ppm."),

        // Dynamic MQ3 methanol/ethanol evidence. HR069 cannot reproduce this heater modulation today.
        e(102,SAME_ARRAY_FINGERPRINT,"MQ3_MEOH_ETOH_2026","peer-reviewed dynamic-temperature-modulation mixture study","MQ3","methanol/ethanol mixtures across humidity levels","Useful methanol identity/concentration-method prior. HR069 shared heater rail does not reproduce the published modulation protocol."),
        e(103,SAME_ARRAY_FINGERPRINT,"MQ3_MEOH_ETOH_2026","peer-reviewed dynamic-temperature-modulation mixture study","MQ3","methanol/ethanol mixtures across humidity levels","Humidity-suppressed dynamic fingerprint method; hardware/protocol transfer required."),

        // Independent environmental calibration supports robustness modelling but is voltage/setup specific.
        e(103,KNOWN_CONCENTRATION_RESPONSE,"MQ135_ENV_ETHANOL_2026","peer-reviewed environmental calibration","MQ135","0-500 ppm; T=15/25/35 C; RH=30/60/90%","Log-linear ethanol response across nine environmental states; voltage-domain model is setup-specific."),

        // Manufacturer family-level evidence without a compound-specific curve.
        e(79,MANUFACTURER_QUALITATIVE_TARGET,"WINSEN_MQ5","manufacturer qualitative sensitivity statement","MQ5","flammable-gas family","MQ5 is documented as sensitive to butane/propane/methane; no n-butane-specific Rs/R0 equation is promoted here.")
    };

    private static Evidence e(int chemical,int level,String sourceId,String sourceType,String sensorScope,String domain,String note){
        return new Evidence(chemical,level,sourceId,sourceType,sensorScope,domain,note);
    }

    public static List<Evidence> forChemical(int chemical){List<Evidence> out=new ArrayList<>();for(Evidence e:RECORDS)if(e.chemical==chemical)out.add(e);return out;}
    public static Evidence best(int chemical){Evidence best=null;for(Evidence e:RECORDS)if(e.chemical==chemical&&(best==null||e.level>best.level))best=e;return best;}
    public static int bestLevel(int chemical){Evidence e=best(chemical);return e==null?NONE:e.level;}
    public static boolean hasAny(int chemical){return bestLevel(chemical)>NONE;}
    public static boolean hasKnownConcentrationOrBetter(int chemical){return bestLevel(chemical)>=KNOWN_CONCENTRATION_RESPONSE;}
    public static boolean hasPublishedCurve(int chemical){return bestLevel(chemical)>=PUBLISHED_RS_R0_CURVE;}
    public static int coveredChemicalCount(){int n=0;for(int i=0;i<ChemicalCatalog.COUNT;i++)if(hasAny(i))n++;return n;}
    public static int knownConcentrationOrBetterCount(){int n=0;for(int i=0;i<ChemicalCatalog.COUNT;i++)if(hasKnownConcentrationOrBetter(i))n++;return n;}
    public static boolean anyExternalM4(){for(Evidence e:RECORDS)if(e.m4)return true;return false;}

    public static String summary(int chemical){
        Evidence best=best(chemical);if(best==null)return "No verified external MQ/MOS evidence registered.";
        int count=0;for(Evidence e:RECORDS)if(e.chemical==chemical)count++;
        return "External evidence: "+levelName(best.level)+" via "+best.sourceId+" ("+best.sensorScope+"). "+best.note+
                (count>1?" Additional independent/corroborating sources registered: "+(count-1)+".":"");
    }

    public static String levelName(int level){
        switch(level){
            case PUBLISHED_RS_R0_CURVE:return "published Rs/R0 curve prior";
            case KNOWN_CONCENTRATION_RESPONSE:return "known-concentration response";
            case SAME_ARRAY_FINGERPRINT:return "same/similar-array fingerprint";
            case MANUFACTURER_QUALITATIVE_TARGET:return "manufacturer qualitative target-family evidence";
            default:return "none";
        }
    }
}
