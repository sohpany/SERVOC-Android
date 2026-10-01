package com.servoc.app.control;

/**
 * Per-presentation horizontal steering reversal for operator controls.
 *
 * Reversal swaps logical LEFT/RIGHT commands. This flips steering/pivot direction while
 * preserving forward/reverse travel signs. Tank sliders are deliberately excluded so each
 * slider always remains a direct command for its own physical side.
 */
public final class DriveDirectionOption {
    public static final String PREF_FILE = "servoc";
    public static final String PREF_REVERSE_JOYSTICK = "drive_reverse_joystick";
    public static final String PREF_REVERSE_WHEEL = "drive_reverse_wheel";
    public static final String PREF_REVERSE_BUTTONS = "drive_reverse_buttons";

    private DriveDirectionOption() {}

    public static String preferenceKey(DriveMode mode) {
        if (mode == DriveMode.JOYSTICK) return PREF_REVERSE_JOYSTICK;
        if (mode == DriveMode.WHEEL) return PREF_REVERSE_WHEEL;
        if (mode == DriveMode.BUTTONS) return PREF_REVERSE_BUTTONS;
        return "";
    }

    public static boolean supportsReverse(DriveMode mode) {
        return mode == DriveMode.JOYSTICK || mode == DriveMode.WHEEL || mode == DriveMode.BUTTONS;
    }

    public static DriveMixer.Command apply(DriveMode mode, boolean reverse, int left, int right) {
        if (reverse && supportsReverse(mode)) return new DriveMixer.Command(right, left);
        return new DriveMixer.Command(left, right);
    }
}
