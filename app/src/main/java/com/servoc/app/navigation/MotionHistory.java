package com.servoc.app.navigation;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Deque;

/** In-memory command breadcrumb history. v0.6 records motion but does not autonomously replay it. */
public final class MotionHistory {
    public static final long WINDOW_MS=60_000L;
    private final Deque<Entry> entries=new ArrayDeque<>();
    private int lastL=Integer.MIN_VALUE,lastR=Integer.MIN_VALUE;
    private long lastAt=0;

    public synchronized void record(int left,int right,long now){
        trim(now);
        if(left==lastL&&right==lastR&&lastAt>0)return;
        entries.addLast(new Entry(now,left,right));lastL=left;lastR=right;lastAt=now;
        trim(now);
    }

    public synchronized JSONArray toJson(long now){
        trim(now);JSONArray a=new JSONArray();
        for(Entry e:entries){JSONObject x=new JSONObject();try{x.put("t_ms",e.at);x.put("l",e.left);x.put("r",e.right);}catch(Exception ignored){}a.put(x);}return a;
    }

    public synchronized int size(long now){trim(now);return entries.size();}
    public synchronized void clear(){entries.clear();lastL=lastR=Integer.MIN_VALUE;lastAt=0;}

    private void trim(long now){while(!entries.isEmpty()&&now-entries.peekFirst().at>WINDOW_MS)entries.removeFirst();}
    private static final class Entry{final long at;final int left,right;Entry(long at,int left,int right){this.at=at;this.left=left;this.right=right;}}
}
