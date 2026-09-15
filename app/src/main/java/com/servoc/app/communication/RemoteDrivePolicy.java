package com.servoc.app.communication;

/** Timing policy for the coalesced low-latency Remote drive stream. */
final class RemoteDrivePolicy {
    static final long PUMP_MS=20;
    static final long MIN_PUBLISH_MS=40;
    static final long HOLD_REFRESH_MS=120;
    static final int COMMAND_TTL_MS=900;

    private RemoteDrivePolicy(){}

    static boolean due(long now,long lastPublish,boolean changed,int left,int right){
        boolean moving=left!=0||right!=0;
        if(!changed&&(!moving||now-lastPublish<HOLD_REFRESH_MS))return false;
        boolean stopTransition=changed&&left==0&&right==0;
        return stopTransition||now-lastPublish>=MIN_PUBLISH_MS;
    }
}
