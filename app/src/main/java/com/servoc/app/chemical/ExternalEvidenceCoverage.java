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
        // H2S priority evidence. The installed MQ135 is manufacturer-listed for the broader "sulfide"
        // category, but Winsen does not publish an H2S-specific MQ135 Rs/R0 curve. The specialized MQ136
        // provides a quantitative 1-200 ppm H2S reference domain and anchor specifications, but it is NOT
        // installed on HR069. These records therefore prioritize H2S transfer experiments without inventing
        // an HR069 ppm equation. High H2S exposure is also corrosive to MQ135 and must not be used casually.
        e(4,MANUFACTURER_QUALITATIVE_TARGET,"WINSEN_MQ135_H2S","manufacturer installed-sensor sulfide target-family evidence","MQ135 (installed HR069)","manufacturer target category: sulfide; no H2S-specific MQ135 ppm curve","Hydrogen sulfide priority. MQ135 is documented as sensitive to sulfide, but no H2S-specific numeric Rs/R0 vector is promoted. Winsen also warns that high-concentration H2S can corrode the sensor and attenuate sensitivity."),
        e(4,KNOWN_CONCENTRATION_RESPONSE,"WINSEN_MQ136_H2S","manufacturer quantitative H2S reference specification on specialized noninstalled sensor","MQ136 (reference sensor; NOT installed on HR069)","H2S 1-200 ppm; S=Rs(air)/Rs(50ppm H2S)>=3; alpha=R200ppm/R50ppm H2S<=0.6","Provides a quantitative H2S reference domain for experiment planning and model validation only. It cannot be transferred as an MQ135/MQ-array ppm equation; HR069 reference-gas calibration remains required."),

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

        // UCI 270 drift/concentration benchmark. No HR069 MQ channel overlaps, so these are cross-array
        // identity/concentration priors and model/drift evidence only; they still prevent re-discovery from zero.
        e(21,KNOWN_CONCENTRATION_RESPONSE,"UCI_GAS_DRIFT_270","public CC BY 4.0 pure-gas concentration-labelled drift benchmark","16-channel TGS2600/TGS2602/TGS2610/TGS2620 array","NH3 approx. 50-1000 ppm; 36-month dataset","Cross-array concentration/identity evidence; no direct HR069 sensor transfer."),
        e(54,KNOWN_CONCENTRATION_RESPONSE,"UCI_GAS_DRIFT_270","public CC BY 4.0 pure-gas concentration-labelled drift benchmark","16-channel TGS2600/TGS2602/TGS2610/TGS2620 array","Acetaldehyde 5-500 ppm; 36-month dataset","13,910-measurement benchmark contains pure acetaldehyde with concentration labels. Use for feature/model transfer and drift methods; HR069 MQ validation remains required."),
        e(89,KNOWN_CONCENTRATION_RESPONSE,"UCI_GAS_DRIFT_270","public CC BY 4.0 pure-gas concentration-labelled drift benchmark","16-channel TGS2600/TGS2602/TGS2610/TGS2620 array","Ethylene 10-300 ppm; 36-month dataset","Pure ethylene concentration-labelled cross-array evidence; no direct MQ response vector is implied."),
        e(103,KNOWN_CONCENTRATION_RESPONSE,"UCI_GAS_DRIFT_270","public CC BY 4.0 pure-gas concentration-labelled drift benchmark","16-channel TGS2600/TGS2602/TGS2610/TGS2620 array","Ethanol 10-600 ppm; 36-month dataset","Cross-array pure-ethanol concentration/drift evidence."),
        e(110,KNOWN_CONCENTRATION_RESPONSE,"UCI_GAS_DRIFT_270","public CC BY 4.0 pure-gas concentration-labelled drift benchmark","16-channel TGS2600/TGS2602/TGS2610/TGS2620 array","Acetone 12-1000 ppm; 36-month dataset","Cross-array pure-acetone concentration/drift evidence."),
        e(95,KNOWN_CONCENTRATION_RESPONSE,"UCI_GAS_DRIFT_270","public CC BY 4.0 pure-gas concentration-labelled drift benchmark","16-channel TGS2600/TGS2602/TGS2610/TGS2620 array","Toluene 10-100 ppm; 36-month dataset","Cross-array pure-toluene concentration/drift evidence."),

        // Peer-reviewed MQ3 ethylene study with controlled ppm and humidity. Numerical raw response/model
        // coefficients are requested from the authors; until received, this is known-concentration evidence,
        // not a fabricated Rs/R0 equation.
        e(89,KNOWN_CONCENTRATION_RESPONSE,"KRIVEC_ETHYLENE_MQ3_2015","peer-reviewed open-access MQ3 known-concentration experiment","MQ3 + MiCS-5914","MQ3 at 10/25/40/50 ppm; RH 0/33/64%; 500 mL/min; 600 s exposure","MQ3 response correlates with ethylene over 10-40 ppm (reported R2 > 0.97); prediction accuracy reported +/-9.2 ppm overall and +/-6.7 ppm below 50 ppm. Raw/tabulated coefficients requested; no equation invented."),

        // Peer-reviewed 2024 methanol/ethanol e-nose experiment. The array contains all eight HR069 MQ
        // models plus MQ6/MQ131/MQ137. Raw data are not public and have been requested from the author.
        e(102,SAME_ARRAY_FINGERPRINT,"TOZLU_METHANOL_MQ_2024","peer-reviewed CC BY 4.0 exact/superset MQ-array methanol fingerprint study","MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ131/MQ135/MQ137","168 odor recordings; 8 methanol/ethanol mixture classes from 0% to 100% methanol; 40 s at 10 Hz","All eight HR069 MQ models are present. Strong identity/feature-transfer evidence; classes are liquid mixture ratios rather than gas ppm. Raw 11-channel time series requested from author."),
        e(103,SAME_ARRAY_FINGERPRINT,"TOZLU_METHANOL_MQ_2024","peer-reviewed CC BY 4.0 exact/superset MQ-array methanol fingerprint study","MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ131/MQ135/MQ137","168 odor recordings; 8 methanol/ethanol mixture classes including pure ethanol","Corroborating exact/superset MQ-array ethanol/methanol-discrimination evidence; not gas-phase ppm calibration."),

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
