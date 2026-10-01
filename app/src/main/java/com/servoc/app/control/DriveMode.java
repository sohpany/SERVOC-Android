package com.servoc.app.control;

/** Operator-selectable driving presentations. All modes share the same logical LEFT/RIGHT contract. */
public enum DriveMode {
    TANK("tank","مزدوج"),
    JOYSTICK("joystick","Joystick"),
    BUTTONS("buttons","أزرار");

    public final String key;
    public final String arabicLabel;
    DriveMode(String key,String label){this.key=key;this.arabicLabel=label;}

    public DriveMode next(){DriveMode[] a=values();return a[(ordinal()+1)%a.length];}
    public static DriveMode fromKey(String key){
        // RC2 removed the deprecated clock-face wheel. Existing installs that had it selected
        // migrate to the joystick instead of unexpectedly falling back to another drive model.
        if(key!=null&&"wheel".equalsIgnoreCase(key.trim()))return JOYSTICK;
        if(key!=null)for(DriveMode m:values())if(m.key.equalsIgnoreCase(key.trim()))return m;
        return JOYSTICK;
    }
}
