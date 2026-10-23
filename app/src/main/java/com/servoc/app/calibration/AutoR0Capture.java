package com.servoc.app.calibration;
import java.util.*;
/** Reviewable 30-second clean-air Rs baseline; NOT gas-certified R0/ppm. */
public final class AutoR0Capture {
 public static final int TARGET=30;
 private final String[] keys=ManualR0Calibration.keys();
 private final double[] sums=new double[keys.length],sumsq=new double[keys.length],logsum=new double[keys.length];
 private final long start;private long next;private int count=0,rejected=0;
 public AutoR0Capture(long start){this.start=start;this.next=start;}
 public boolean finished(){return count==TARGET;}
 public int samples(){return count;}public int rejected(){return rejected;}
 public long elapsed(long now){return Math.max(0,now-start);}
 public boolean sample(long now,Map<String,Double> rs){
  if(finished()||now<next)return false;
  next=now+1000;
  if(rs==null||rs.size()!=keys.length){rejected++;return false;}
  for(String k:keys){Double v=rs.get(k);if(v==null||!Double.isFinite(v)||v<=0){rejected++;return false;}}
  for(int i=0;i<keys.length;i++){double v=rs.get(keys[i]);sums[i]+=v;sumsq[i]+=v*v;logsum[i]+=Math.log(v);}count++;return true;
 }
 public Map<String,Double> estimate(){
  if(!finished())return Collections.emptyMap();
  Map<String,Double> out=new LinkedHashMap<>();
  for(int i=0;i<keys.length;i++){
   double mean=sums[i]/count,var=Math.max(0,(sumsq[i]-sums[i]*sums[i]/count)/(count-1));
   if(!Double.isFinite(mean)||mean<=0||Math.sqrt(var)/mean>0.10)return Collections.emptyMap();
   out.put(keys[i],Math.exp(logsum[i]/count));
  }
  return out;
 }
}