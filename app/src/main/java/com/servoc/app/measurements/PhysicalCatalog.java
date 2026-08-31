package com.servoc.app.measurements;

/** Android port of MeasurementCatalog.h physical measurements. */
public final class PhysicalCatalog {
    private PhysicalCatalog() {}

    public static final String[] NAMES = {
            "Temperature", "Humidity", "Pressure", "Illuminance", "Luminous Flux",
            "Noise", "Vibration", "Ionizing Radiation", "UV Index", "PM1",
            "PM2.5", "PM10", "Wind Speed", "Wind Direction", "Altitude"
    };
    public static final String[] NAMES_AR = {
            "درجة الحرارة", "الرطوبة", "الضغط الجوي", "شدة الاستضاءة", "التدفق الضوئي",
            "الضوضاء", "الاهتزاز", "الإشعاع المؤين", "مؤشر الأشعة فوق البنفسجية", "الجسيمات PM1",
            "الجسيمات PM2.5", "الجسيمات PM10", "سرعة الرياح", "اتجاه الرياح", "الارتفاع"
    };

    public static final String[] UNITS = {
            "°C", "%RH", "hPa", "lux", "lm",
            "dB", "m/s²", "µSv/h", "UVI", "µg/m³",
            "µg/m³", "µg/m³", "m/s", "°", "m"
    };

    public static final int COUNT = NAMES.length;
}
