package com.servoc.app.communication;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/** Local handset Wi-Fi RSSI reader used by the Direct driving overlay. */
public final class WifiSignalReader {
    public static final class Snapshot{
        public final boolean available;public final int rssiDbm,qualityPercent;
        Snapshot(boolean a,int r,int q){available=a;rssiDbm=r;qualityPercent=q;}
    }
    private WifiSignalReader(){}
    @SuppressWarnings("deprecation")
    public static Snapshot read(Context c){
        try{
            WifiManager wm=(WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);if(wm==null)return new Snapshot(false,0,0);
            WifiInfo i=wm.getConnectionInfo();if(i==null)return new Snapshot(false,0,0);int r=i.getRssi();if(r<=-127||r>=0)return new Snapshot(false,r,0);
            int q=r<=-100?0:r>=-50?100:Math.round((r+100)*2f);return new Snapshot(true,r,Math.max(0,Math.min(100,q)));
        }catch(Exception e){return new Snapshot(false,0,0);}
    }
}
