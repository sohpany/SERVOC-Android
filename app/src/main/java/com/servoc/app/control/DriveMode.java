package com.servoc.app.control;

/** Operator-selectable driving presentations. All modes share the same logical LEFT/RIGHT contract. */
public enum DriveMode {
    TANK("tank","مزدوج"),
    JOYSTICK("joystick","Joystick"),
    WHEEL("wheel","عجلة"),
    BUTTONS("buttons","أزرار");

    public final String key;
    public final String arabicLabel;
    DriveMode(String key,String label){this.key=key;this.arabicLabel=label;}

    public DriveMode next(){DriveMode[] a=values();return a[(ordinal()+1)%a.length];}
    public static DriveMode fromKey(String key){
        if(key!=null)for(DriveMode m:values())if(m.key.equalsIgnoreCase(key.trim()))return m;
        return JOYSTICK;
    }
}
