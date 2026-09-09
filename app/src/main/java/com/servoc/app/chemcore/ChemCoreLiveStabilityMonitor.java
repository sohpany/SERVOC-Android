package com.servoc.app.chemcore;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Research-v1 stability gate for controlled ChemCore acquisition. It evaluates only signal
 * stationarity; it does not assert chemical identity or calibration validity.
 */
public final class ChemCoreLiveStabilityMonitor {
    public static final String POLICY_VERSION="chemcore-stability/1";
    public static final long WINDOW_MS=10_000L;
    public static final int MIN_SAMPLES_PER_CHANNEL=20;
    public static final int MIN_STABLE_CHANNELS=6;
    /** ~3.5% multiplicative SD in ratio space; engineering threshold pending physical validation. */
    public static final double MAX_SD_LOG10=0.015;
    /** ~0.35%/s multiplicative trend; engineering threshold pending physical validation. */
    public static final double MAX_ABS_SLOPE_LOG10_PER_S=0.0015;
    private static final String[] SENSORS={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};

    private static final class Point { final long t;final double y;Point(long t,double y){this.t=t;this.y=y;} }
    private final Map<String,Deque<Point>> channels=new LinkedHashMap<>();
    private long firstAcceptedMs=-1,lastAcceptedMs=-1;

    public ChemCoreLiveStabilityMonitor(){for(String s:SENSORS)channels.put(s,new ArrayDeque<>());}

    public synchronized void reset(){for(Deque<Point> q:channels.values())q.clear();firstAcceptedMs=lastAcceptedMs=-1;}

    public synchronized void accept(JSONObject snapshot,long nowMs){
        if(snapshot==null)return;JSONObject mq=snapshot.optJSONObject("mq");if(mq==null)return;boolean any=false;
        for(String key:SENSORS){JSONObject r=mq.optJSONObject(key);if(!valid(r))continue;double ratio=r.optDouble("ratio",Double.NaN);if(!(ratio>0)||!Double.isFinite(ratio))continue;Deque<Point> q=channels.get(key);q.addLast(new Point(nowMs,Math.log10(ratio)));trim(q,nowMs-WINDOW_MS);any=true;}
        if(any){if(firstAcceptedMs<0)firstAcceptedMs=nowMs;lastAcceptedMs=nowMs;}
    }

    public synchronized Assessment assess(){
        int valid=0,stable=0;double[] sds=new double[SENSORS.length],slopes=new double[SENSORS.length];int nsd=0,nsl=0;long minStart=Long.MAX_VALUE,maxEnd=Long.MIN_VALUE;
        for(String key:SENSORS){Deque<Point> q=channels.get(key);if(q==null||q.size()<MIN_SAMPLES_PER_CHANNEL)continue;Point first=q.peekFirst(),last=q.peekLast();if(first==null||last==null||last.t-first.t<WINDOW_MS*0.75)continue;Stats st=stats(q);if(!Double.isFinite(st.sd)||!Double.isFinite(st.slope))continue;valid++;sds[nsd++]=st.sd;slopes[nsl++]=Math.abs(st.slope);if(st.sd<=MAX_SD_LOG10&&Math.abs(st.slope)<=MAX_ABS_SLOPE_LOG10_PER_S)stable++;minStart=Math.min(minStart,first.t);maxEnd=Math.max(maxEnd,last.t);}
        double duration=valid>0&&maxEnd>=minStart?(maxEnd-minStart)/1000.0:0;
        return new Assessment(valid,stable,stable>=MIN_STABLE_CHANNELS&&valid>=MIN_STABLE_CHANNELS,duration,median(sds,nsd),median(slopes,nsl));
    }

    public static final class Assessment {
        public final int validChannels,stableChannels;public final boolean stable;public final double windowSeconds,medianSdLog10,medianAbsSlopeLog10PerS;
        Assessment(int valid,int stableChannels,boolean stable,double windowSeconds,double sd,double slope){this.validChannels=valid;this.stableChannels=stableChannels;this.stable=stable;this.windowSeconds=windowSeconds;this.medianSdLog10=sd;this.medianAbsSlopeLog10PerS=slope;}
        public String summary(){return stableChannels+"/"+validChannels+" stable • window="+String.format(java.util.Locale.US,"%.1fs",windowSeconds)+" • median SD="+fmt(medianSdLog10)+" • |slope|="+fmt(medianAbsSlopeLog10PerS);}
        private static String fmt(double x){return Double.isFinite(x)?String.format(java.util.Locale.US,"%.5f",x):"—";}
    }

    private static final class Stats{double sd=Double.NaN,slope=Double.NaN;}
    private static Stats stats(Deque<Point> q){Stats s=new Stats();int n=q.size();if(n<2)return s;double sy=0;for(Point p:q)sy+=p.y;double mean=sy/n,ss=0,sx=0,sxx=0,sxy=0;long t0=q.peekFirst().t;for(Point p:q){double x=(p.t-t0)/1000.0,d=p.y-mean;ss+=d*d;sx+=x;sxx+=x*x;sxy+=x*p.y;}s.sd=Math.sqrt(ss/(n-1));double den=n*sxx-sx*sx;s.slope=Math.abs(den)>1e-15?(n*sxy-sx*sy)/den:Double.NaN;return s;}
    private static void trim(Deque<Point> q,long minMs){while(!q.isEmpty()&&q.peekFirst().t<minMs)q.removeFirst();}
    private static boolean valid(JSONObject r){return r!=null&&r.optBoolean("valid",r.optBoolean("ok",false));}
    private static double median(double[] a,int n){if(n<=0)return Double.NaN;double[] x=Arrays.copyOf(a,n);Arrays.sort(x);return(n&1)==1?x[n/2]:0.5*(x[n/2-1]+x[n/2]);}
}
