package com.servoc.app.communication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.Inet4Address;
import java.net.InetAddress;

/** Keeps direct HazRob HTTP traffic on the actual 192.168.4.0/24 Wi-Fi network. */
public final class WifiRoute {
    private WifiRoute(){}

    public static boolean bindRobotWifi(Context context){
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return false;
            Network fallback=null;
            int wifiCount=0;
            for(Network n:cm.getAllNetworks()){
                NetworkCapabilities cap=cm.getNetworkCapabilities(n);
                if(cap==null||!cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))continue;
                wifiCount++;
                fallback=n;
                LinkProperties lp=cm.getLinkProperties(n);
                if(lp==null)continue;
                for(LinkAddress la:lp.getLinkAddresses()){
                    InetAddress addr=la.getAddress();
                    if(addr instanceof Inet4Address&&LocalNetworkContract.isRobotSubnetAddress(addr.getHostAddress())){
                        return cm.bindProcessToNetwork(n);
                    }
                }
            }
            // Only fall back when Android exposes exactly one Wi-Fi Network but hides its
            // link-address details. Never pick an arbitrary Wi-Fi network from several.
            return wifiCount==1&&fallback!=null&&cm.bindProcessToNetwork(fallback);
        }catch(Exception ignored){}
        return false;
    }

    /** Backward-compatible alias for existing direct-mode callers. */
    public static boolean bindConnectedWifi(Context context){return bindRobotWifi(context);}

    public static void unbind(Context context){
        try{
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm!=null)cm.bindProcessToNetwork(null);
        }catch(Exception ignored){}
    }
}
