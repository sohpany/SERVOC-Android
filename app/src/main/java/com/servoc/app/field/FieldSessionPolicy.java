package com.servoc.app.field;

/** Pure domain guards shared by repository code and JVM contract tests. */
public final class FieldSessionPolicy {
    private FieldSessionPolicy(){}
    public static boolean mayAttachMeasurement(String kind,String operatorRole){
        String k=kind==null?"":kind.trim();
        String r=operatorRole==null?"":operatorRole.trim();
        return "USER".equals(r)&&k.startsWith("field_");
    }
    public static boolean isDeveloperOrScientific(String kind,String operatorRole){
        return !mayAttachMeasurement(kind,operatorRole);
    }
}
