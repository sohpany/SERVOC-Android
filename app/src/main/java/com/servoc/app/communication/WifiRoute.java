package com.servoc.app.communication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.SystemClock;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Direct-mode network authority.
 *
 * While Direct mode is selected this class keeps one long-lived Android network request for the
 * HazRob SSID, holds a Wi-Fi performance lock, and repeatedly re-pins process traffic to the robot
 * subnet. Cellular data may remain enabled, but robot/camera HTTP stays on 192.168.4.0/24.
 */
public final class WifiRoute {
    private static final Object LOCK=new Object();
    private static ConnectivityManager requestCm;
    private static ConnectivityManager.NetworkCallback requestCallback;
    private static volatile Network requestedNetwork;
    private static volatile boolean requestUnavailable=false;
    private static volatile long requestLostAt=0;
    private static WifiManager.WifiLock wifiLock;

    private WifiRoute(){}

    public static String requiredRuntimePermission(){
        if(Build.VERSION.SDK_INT>=33)return Manifest.permission.NEARBY_WIFI_DEVICES;
        if(Build.VERSION.SDK_INT>=29)return Manifest.permission.ACCESS_FINE_LOCATION;
        return "";
    }
    public static boolean hasRequiredPermission(Context context){
        String p=requiredRuntimePermission();
        return p.isEmpty()||context.checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;
    }

    private static void acquireWifiLockLocked(Context context){
        if(wifiLock!=null&&wifiLock.isHeld())return;
        try{
            Context app=context.getApplicationContext();
            WifiManager wm=(WifiManager)app.getSystemService(Context.WIFI_SERVICE);
            if(wm==null)return;
            int mode=Build.VERSION.SDK_INT>=29?WifiManager.WIFI_MODE_FULL_LOW_LATENCY:WifiManager.WIFI_MODE_FULL_HIGH_PERF;
            wifiLock=wm.createWifiLock(mode,"HazRob:DirectLink");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        }catch(Exception ignored){wifiLock=null;}
    }

    private static void releaseWifiLockLocked(){
        if(wifiLock!=null){try{if(wifiLock.isHeld())wifiLock.release();}catch(Exception ignored){}wifiLock=null;}
    }

    private static void clearRequestLocked(){
        if(requestCm!=null&&requestCallback!=null){try{requestCm.unregisterNetworkCallback(requestCallback);}catch(Exception ignored){}}
        requestCallback=null;requestCm=null;requestedNetwork=null;requestUnavailable=false;requestLostAt=0;
    }

    private static void pin(ConnectivityManager cm,Network network){
        if(cm==null||network==null)return;
        try{cm.bindProcessToNetwork(network);}catch(Exception ignored){}
    }

    public static void requestRobotWifi(Context context){
        if(Build.VERSION.SDK_INT<29)return;
        if(!hasRequiredPermission(context))throw new SecurityException("HazRob Wi-Fi permission missing");
        synchronized(LOCK){
            acquireWifiLockLocked(context);
            if(requestCallback!=null)return;
            ConnectivityManager cm=(ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return;
            WifiNetworkSpecifier spec=new WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID).build();
            NetworkRequest req=new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(spec)
                    .build();
            requestCm=cm;requestUnavailable=false;requestLostAt=0;
            requestCallback=new ConnectivityManager.NetworkCallback(){
                @Override public void onAvailable(Network network){requestedNetwork=network;requestUnavailable=false;requestLostAt=0;pin(cm,network);}
                @Override public void onCapabilitiesChanged(Network network,NetworkCapabilities capabilities){if(network.equals(requestedNetwork))pin(cm,network);}
                @Override public void onLinkPropertiesChanged(Network network,LinkProperties properties){if(network.equals(requestedNetwork))pin(cm,network);}
                @Override public void onLost(Network network){if(network.equals(requestedNetwork)){requestedNetwork=null;requestLostAt=SystemClock.elapsedRealtime();}}
                @Override public void onUnavailable(){requestUnavailable=true;requestedNetwork=null;requestLostAt=SystemClock.elapsedRealtime();}
            };
            cm.requestNetwork(req,requestCallback);
        }
    }

    /** Forces a fresh exact-SSID request after the ESP32 AP or Android Wi-Fi stack has restarted. */
    public static void restartRobotWifiRequest(Context context){
        if(Build.VERSION.SDK_INT<29)return;
        synchronized(LOCK){clearRequestLocked();acquireWifiLockLocked(context);}
        try{requestRobotWifi(context);}catch(Exception ignored){}
    }

    public static boolean waitForRobotWifi(Context context,long timeoutMs){
        synchronized(LOCK){acquireWifiLockLocked(context);}
        if(bindRobotWifi(context))return true;
        try{requestRobotWifi(context);}catch(Exception ignored){}
        long until=SystemClock.elapsedRealtime()+Math.max(0,timeoutMs);
        do{
            Network n=requestedNetwork;
            if(n!=null){try{ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);if(cm!=null&&cm.bindProcessToNetwork(n))return true;}catch(Exception ignored){}}
            if(bindRobotWifi(context))return true;
            if(requestUnavailable)return false;
            if(SystemClock.elapsedRealtime()>=until)break;
            try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }while(true);
        return bindRobotWifi(context);
    }

    /** Bind only to the requested HazRob network or a Wi-Fi interface that actually owns 192.168.4.x. */
    public static boolean bindRobotWifi(Context context){
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return false;
            Network requested=requestedNetwork;
            if(requested!=null&&cm.bindProcessToNetwork(requested))return true;
            for(Network n:cm.getAllNetworks()){
                NetworkCapabilities cap=cm.getNetworkCapabilities(n);
                if(cap==null||!cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))continue;
                LinkProperties lp=cm.getLinkProperties(n);if(lp==null)continue;
                for(LinkAddress la:lp.getLinkAddresses()){
                    InetAddress addr=la.getAddress();
                    if(addr instanceof Inet4Address&&LocalNetworkContract.isRobotSubnetAddress(addr.getHostAddress()))return cm.bindProcessToNetwork(n);
                }
            }
        }catch(Exception ignored){}
        return false;
    }

    /** Keeps Direct associated, requests recovery quickly after loss, and continuously re-pins routing. */
    public static boolean maintainRobotWifi(Context context){
        synchronized(LOCK){acquireWifiLockLocked(context);}
        if(bindRobotWifi(context))return true;
        if(Build.VERSION.SDK_INT>=29){
            boolean restart=false;
            synchronized(LOCK){
                long lost=requestLostAt;
                restart=requestCallback==null||requestUnavailable||(requestedNetwork==null&&lost>0&&SystemClock.elapsedRealtime()-lost>=250);
                if(restart)clearRequestLocked();
            }
            if(restart){try{requestRobotWifi(context);}catch(Exception ignored){}}
        }
        return bindRobotWifi(context);
    }

    /** Backward-compatible alias for existing direct-mode callers. */
    public static boolean bindConnectedWifi(Context context){return maintainRobotWifi(context);}

    public static void releaseRobotWifi(Context context){
        synchronized(LOCK){clearRequestLocked();releaseWifiLockLocked();}
        unbind(context);
    }

    public static void unbind(Context context){
        try{ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);if(cm!=null)cm.bindProcessToNetwork(null);}catch(Exception ignored){}
    }
}
