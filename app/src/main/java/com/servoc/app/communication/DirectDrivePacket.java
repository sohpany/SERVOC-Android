package com.servoc.app.communication;

import java.nio.charset.StandardCharsets;

/** HazRob 0.10.1 low-latency Direct drive datagram contract. */
final class DirectDrivePacket {
    static final String MAGIC="HZD1";
    static final int PORT=4210;

    private DirectDrivePacket(){}

    static byte[] encode(long session,long sequence,int left,int right){
        if(session<0||session>0xffffffffL)throw new IllegalArgumentException("session must be uint32");
        if(sequence<0||sequence>0xffffffffL)throw new IllegalArgumentException("sequence must be uint32");
        if(left<-100||left>100||right<-100||right>100)throw new IllegalArgumentException("drive must be -100..100");
        return (MAGIC+","+Long.toUnsignedString(session)+","+Long.toUnsignedString(sequence)+","+left+","+right).getBytes(StandardCharsets.US_ASCII);
    }
}
