package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Runtime transcription of the Stage-7 HR069 anchor/specialist map.
 *
 * These roles are evidence metadata only. They do not create response curves, selectivity claims,
 * identity, or reportable concentration. PRIMARY_ANCHOR is deliberately narrower than a generic
 * manufacturer TARGET response; SECONDARY_TARGET retains documented target support without being
 * treated as the dominant specialist channel.
 */
public final class ChemCoreSpecialistRoleRegistry {
    public static final String VERSION="chemcore-specialist-role-map/1";

    public enum Role { NONE, SECONDARY_TARGET, PRIMARY_ANCHOR }

    private ChemCoreSpecialistRoleRegistry(){}

    public static Role role(int catalogIndex,String sensorKey){
        if(catalogIndex<0||catalogIndex>=ChemicalCatalog.COUNT||sensorKey==null)return Role.NONE;
        String s=sensorKey.trim().toLowerCase(Locale.US);
        if(isPrimary(catalogIndex,s))return Role.PRIMARY_ANCHOR;
        if(isSecondary(catalogIndex,s))return Role.SECONDARY_TARGET;
        return Role.NONE;
    }

    public static List<String> primarySensors(int catalogIndex){
        if(catalogIndex<0||catalogIndex>=ChemicalCatalog.COUNT)return Collections.emptyList();
        List<String> out=new ArrayList<>();
        for(String s:SENSORS)if(role(catalogIndex,s)==Role.PRIMARY_ANCHOR)out.add(s);
        return Collections.unmodifiableList(out);
    }

    public static List<String> secondarySensors(int catalogIndex){
        if(catalogIndex<0||catalogIndex>=ChemicalCatalog.COUNT)return Collections.emptyList();
        List<String> out=new ArrayList<>();
        for(String s:SENSORS)if(role(catalogIndex,s)==Role.SECONDARY_TARGET)out.add(s);
        return Collections.unmodifiableList(out);
    }

    /**
     * Legacy accepted-reference candidates use uid=catalogIndex+1. The label cross-check prevents
     * an arbitrary dynamic-database UID from being silently interpreted as a legacy catalogue ID.
     */
    public static int catalogIndex(long chemicalUid,String label){
        if(chemicalUid>=1&&chemicalUid<=ChemicalCatalog.COUNT){
            int candidate=(int)chemicalUid-1;
            if(matches(ChemicalCatalog.NAMES[candidate],label))return candidate;
        }
        if(label!=null&&!label.trim().isEmpty()){
            for(int i=0;i<ChemicalCatalog.COUNT;i++)if(matches(ChemicalCatalog.NAMES[i],label))return i;
        }
        return -1;
    }

    private static final String[] SENSORS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};

    private static boolean isPrimary(int c,String s){
        switch(c){
            case 20: return "mq7".equals(s);      // Carbon Monoxide
            case 21: return "mq135".equals(s);    // Ammonia
            case 75: return "mq8".equals(s);      // Hydrogen
            case 76: return "mq4".equals(s);      // Methane
            case 103:return "mq3".equals(s);      // Ethanol
            default:return false;
        }
    }

    private static boolean isSecondary(int c,String s){
        switch(c){
            case 20: return "mq9".equals(s);                                  // CO
            case 75: return "mq135".equals(s);                                // H2
            case 76: return "mq2".equals(s)||"mq5".equals(s)||"mq9".equals(s); // CH4
            case 78: return "mq2".equals(s)||"mq5".equals(s);                // Propane
            case 95: return "mq135".equals(s);                                // Toluene
            case 135:return "mq2".equals(s)||"mq5".equals(s);                // LPG
            case 139:return "mq2".equals(s)||"mq135".equals(s);              // Smoke
            default:return false;
        }
    }

    private static boolean matches(String a,String b){
        return a!=null&&b!=null&&a.trim().equalsIgnoreCase(b.trim());
    }
}
