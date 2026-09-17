package com.servoc.app.communication;

/** Canonical HazRob local-network contract. Direct mode never depends on stale saved host values. */
public final class LocalNetworkContract {
    public static final String ROBOT_SSID="HazRob";
    public static final String ROBOT_HOST="192.168.4.1";
    public static final String CAMERA_HOST="192.168.4.2";
    public static final String ROBOT_SUBNET_PREFIX="192.168.4.";
    public static final String FIRMWARE_BANNER_PREFIX="HazRob ";
    public static final String MAIN_FIRMWARE_SUFFIX=" HR065";

    private LocalNetworkContract(){}

    public static boolean isRobotSubnetAddress(String address){
        if(address==null)return false;
        String a=address.trim();
        if(!a.startsWith(ROBOT_SUBNET_PREFIX))return false;
        int p=a.lastIndexOf('.');
        if(p<0||p==a.length()-1)return false;
        try{
            int last=Integer.parseInt(a.substring(p+1));
            return last>=1&&last<=254;
        }catch(NumberFormatException e){return false;}
    }

    /** Rejects unrelated 192.168.4.1 devices and requires the controlled HR065 main-firmware identity. */
    public static boolean isExpectedFirmwareBanner(String banner,String version){
        if(banner==null||version==null)return false;
        String v=version.trim();
        return !v.isEmpty()&&(FIRMWARE_BANNER_PREFIX+v+MAIN_FIRMWARE_SUFFIX).equals(banner.trim());
    }
}
