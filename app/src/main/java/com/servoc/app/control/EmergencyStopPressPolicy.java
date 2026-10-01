package com.servoc.app.control;

/** Pure operator policy for the E-STOP UI. Firmware E-STOP remains the final authority. */
public final class EmergencyStopPressPolicy {
    public static final long ARM_HOLD_MS = 2000L;
    private EmergencyStopPressPolicy() {}

    /** A normal-state E-STOP may latch only after the full hold duration. */
    public static boolean shouldLatch(boolean alreadyLatched,long heldMs){
        return !alreadyLatched && heldMs>=ARM_HOLD_MS;
    }

    /** Once latched, a normal tap is intentionally the reset gesture requested by the operator. */
    public static boolean shouldResetOnTap(boolean alreadyLatched){return alreadyLatched;}
}
