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
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import java.net.Inet4Address;
import java.net.InetAddress;

/** Direct HazRob network ownership. On Android 10+ request the known SSID as local-only and keep the
 * NetworkCallback registered for the whole Direct session; bindProcessToNetwork remains a routing
 * safeguard rather than the connection-retention mechanism. */
public final class WifiRoute {
    private static final Object LOCK=new Object();
    private static ConnectivityManager requestCm;
    private static ConnectivityManager.NetworkCallback requestCallback;
    private static volatile Network requestedNetwork;
    private static volatile boolean requestUnavailable=false;

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

    public static void requestRobotWifi(Context context){
        if(Build.VERSION.SDK_INT<29)return;
        if(!hasRequiredPermission(context))throw new SecurityException("HazRob Wi-Fi permission missing");
        synchronized(LOCK){
            if(requestCallback!=null)return;
            ConnectivityManager cm=(ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return;
            WifiNetworkSpecifier spec=new WifiNetworkSpecifier.Builder().setSsid(LocalNetworkContract.ROBOT_SSID).build();
            NetworkRequest req=new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(spec)
                    .build();
            requestCm=cm;requestUnavailable=false;
            requestCallback=new ConnectivityManager.NetworkCallback(){
                @Override public void onAvailable(Network network){requestedNetwork=network;requestUnavailable=false;try{cm.bindProcessToNetwork(network);}catch(Exception ignored){}}
                @Override public void onLost(Network network){if(network.equals(requestedNetwork))requestedNetwork=null;}
                @Override public void onUnavailable(){requestUnavailable=true;requestedNetwork=null;}
            };
            cm.requestNetwork(req,requestCallback);
        }
    }

    public static boolean waitForRobotWifi(Context context,long timeoutMs){
        try{requestRobotWifi(context);}catch(Exception ignored){}
        long until=android.os.SystemClock.elapsedRealtime()+Math.max(0,timeoutMs);
        do{
            Network n=requestedNetwork;
            if(n!=null){try{ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);if(cm!=null&&cm.bindProcessToNetwork(n))return true;}catch(Exception ignored){}}
            if(bindRobotWifi(context))return true;
            if(requestUnavailable)return false;
            if(android.os.SystemClock.elapsedRealtime()>=until)break;
            try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }while(true);
        return bindRobotWifi(context);
    }

    public static boolean bindRobotWifi(Context context){
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return false;
            Network requested=requestedNetwork;
            if(requested!=null&&cm.bindProcessToNetwork(requested))return true;
            Network fallback=null;int wifiCount=0;
            for(Network n:cm.getAllNetworks()){
                NetworkCapabilities cap=cm.getNetworkCapabilities(n);
                if(cap==null||!cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))continue;
                wifiCount++;fallback=n;
                LinkProperties lp=cm.getLinkProperties(n);if(lp==null)continue;
                for(LinkAddress la:lp.getLinkAddresses()){
                    InetAddress addr=la.getAddress();
                    if(addr instanceof Inet4Address&&LocalNetworkContract.isRobotSubnetAddress(addr.getHostAddress()))return cm.bindProcessToNetwork(n);
                }
            }
            return wifiCount==1&&fallback!=null&&cm.bindProcessToNetwork(fallback);
        }catch(Exception ignored){}
        return false;
    }

    /** Keeps an existing Direct request alive and re-pins routing after Android network changes. */
    public static boolean maintainRobotWifi(Context context){
        if(Build.VERSION.SDK_INT>=29&&requestCallback==null){try{requestRobotWifi(context);}catch(Exception ignored){}}
        return bindRobotWifi(context);
    }

    /** Backward-compatible alias for existing direct-mode callers. */
    public static boolean bindConnectedWifi(Context context){return maintainRobotWifi(context);}

    public static void releaseRobotWifi(Context context){
        synchronized(LOCK){
            if(requestCm!=null&&requestCallback!=null){try{requestCm.unregisterNetworkCallback(requestCallback);}catch(Exception ignored){}}
            requestCallback=null;requestCm=null;requestedNetwork=null;requestUnavailable=false;
        }
        unbind(context);
    }

    public static void unbind(Context context){
        try{ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);if(cm!=null)cm.bindProcessToNetwork(null);}catch(Exception ignored){}
    }
}
