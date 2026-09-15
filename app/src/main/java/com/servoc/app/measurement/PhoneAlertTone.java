package com.servoc.app.measurement;

import android.media.AudioManager;
import android.media.ToneGenerator;

/** Phone-side operational warning tone. It does not represent calibrated ppm or IDLH. */
public final class PhoneAlertTone implements AutoCloseable {
    private static final long WARNING_REPEAT_MS=2500,DANGER_REPEAT_MS=1200;
    private final ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_ALARM,90);
    private int level=GasAlertCoordinator.NONE;
    private long lastToneAt=0;

    public synchronized void update(int requested,long nowMs){
        int next=Math.max(GasAlertCoordinator.NONE,Math.min(GasAlertCoordinator.DANGER,requested));
        if(next==GasAlertCoordinator.NONE){level=next;return;}
        long repeat=next>=GasAlertCoordinator.DANGER?DANGER_REPEAT_MS:WARNING_REPEAT_MS;
        if(next!=level||nowMs-lastToneAt>=repeat){
            int id=next>=GasAlertCoordinator.DANGER?ToneGenerator.TONE_PROP_BEEP2:ToneGenerator.TONE_PROP_BEEP;
            tone.startTone(id,next>=GasAlertCoordinator.DANGER?450:250);
            lastToneAt=nowMs;
        }
        level=next;
    }

    /** Independent UI/field verification; bypasses MQ warm-up and warning thresholds. */
    public synchronized void test(){tone.startTone(ToneGenerator.TONE_PROP_BEEP2,600);}
    public synchronized void clear(){level=GasAlertCoordinator.NONE;}
    @Override public synchronized void close(){tone.release();}
}
