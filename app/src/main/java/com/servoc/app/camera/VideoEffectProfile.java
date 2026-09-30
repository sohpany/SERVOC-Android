package com.servoc.app.camera;

/** App-side only video presentation contract. No option is sent to ESP32-CAM. */
public final class VideoEffectProfile {
    private VideoEffectProfile(){}

    public enum DisplayMode {
        NORMAL("normal","عادي"),
        FISHEYE("fisheye","عين السمكة"),
        ZOOM("zoom","زوم"),
        WIDE("wide","عريض");
        public final String key,arabic;
        DisplayMode(String key,String arabic){this.key=key;this.arabic=arabic;}
        public DisplayMode next(){DisplayMode[] a=values();return a[(ordinal()+1)%a.length];}
        public static DisplayMode fromKey(String key){if(key!=null)for(DisplayMode m:values())if(m.key.equalsIgnoreCase(key.trim()))return m;return NORMAL;}
    }

    public enum Enhancement {
        NORMAL("normal","عادي",false),
        NIGHT_GREEN("night_green","رؤية ليلية خضراء",false),
        THERMAL("thermal","حراري محاكى",false),
        NIGHT("night","رؤية ليلية",false),
        BRIGHT("bright","تفتيح",false),
        DARK("dark","تغميق",false),
        AUTO("auto","تحسين تلقائي",false),
        /** GPU magnification already performs the spatial upscale; this mode adds a bounded edge-adaptive reconstruction pass. */
        DETAIL("detail","تحسين الدقة 2×",true);
        public final String key,arabic;public final boolean neighborSampling;
        Enhancement(String key,String arabic,boolean neighborSampling){this.key=key;this.arabic=arabic;this.neighborSampling=neighborSampling;}
        public Enhancement next(){Enhancement[] a=values();return a[(ordinal()+1)%a.length];}
        public static Enhancement fromKey(String key){if(key!=null)for(Enhancement e:values())if(e.key.equalsIgnoreCase(key.trim()))return e;return NORMAL;}
    }
}
