package com.servoc.app.communication;

/** Compact low-latency direct-drive packet shared by Android and HazRob firmware. */
public final class DirectDriveDatagram {
    public static final int PORT = 4210;
    public static final int VERSION = 1;
    public static final int SIZE = 8;

    private DirectDriveDatagram(){}

    public static byte[] encode(int sequence,int left,int right){
        int l=Math.max(-100,Math.min(100,left));
        int r=Math.max(-100,Math.min(100,right));
        int seq=sequence&0xFFFF;
        byte[] b=new byte[SIZE];
        b[0]='H';b[1]='R';b[2]=(byte)VERSION;
        b[3]=(byte)(seq&0xFF);b[4]=(byte)((seq>>>8)&0xFF);
        b[5]=(byte)l;b[6]=(byte)r;
        int x=0;for(int i=0;i<SIZE-1;i++)x^=b[i]&0xFF;b[7]=(byte)x;
        return b;
    }
}
