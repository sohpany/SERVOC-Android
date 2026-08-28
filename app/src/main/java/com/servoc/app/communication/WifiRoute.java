package com.servoc.app.communication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Keeps local robot/camera HTTP traffic on the Wi-Fi network even if cellular data is active. */
public final class WifiRoute {
    private WifiRoute(){}

    public static boolean bindConnectedWifi(Context context){
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return false;
            for(Network n:cm.getAllNetworks()){
                NetworkCapabilities cap=cm.getNetworkCapabilities(n);
                if(cap!=null&&cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    return cm.bindProcessToNetwork(n);
                }
            }
        }catch(Exception ignored){}
        return false;
    }

    public static void unbind(Context context){
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm!=null)cm.bindProcessToNetwork(null);
        }catch(Exception ignored){}
    }
}
