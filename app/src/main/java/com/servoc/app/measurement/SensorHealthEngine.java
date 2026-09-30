package com.servoc.app.measurement;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Electrical/data-quality health diagnostics for the installed HazRob MQ channels. */
public final class SensorHealthEngine {
    public static final String[] INSTALLED=installedKeys();
    private static final int WINDOW=24;

    public enum State { UNKNOWN, HEALTHY, MISSING, INVALID, SATURATED, STUCK, UNSTABLE, DRIFT_WARNING }

    public static final class Snapshot {
        public final String key;
        public final State state;
        public final int validSamples,expectedMisses;
        public final double rawMean,rawStd,noiseCv,baselineRatio,driftPercent,lastRatio;
        public final String note;
        Snapshot(String key,State state,int validSamples,int expectedMisses,double rawMean,double rawStd,double noiseCv,double baselineRatio,double driftPercent,double lastRatio,String note){
            this.key=key;this.state=state;this.validSamples=validSamples;this.expectedMisses=expectedMisses;this.rawMean=rawMean;this.rawStd=rawStd;this.noiseCv=noiseCv;this.baselineRatio=baselineRatio;this.driftPercent=driftPercent;this.lastRatio=lastRatio;this.note=note;
        }
    }

    private static final class Channel {
        final String key;final int group;final double[] raw=new double[WINDOW],ratio=new double[WINDOW];
        int count=0,next=0,expectedMisses=0,invalidExpected=0,saturationHits=0;
        double baseline=Double.NaN,lastDrift=Double.NaN,lastRatio=Double.NaN;
        boolean driftChecked=false;
        Channel(String key,int group){this.key=key;this.group=group;Arrays.fill(raw,Double.NaN);Arrays.fill(ratio,Double.NaN);}
        void add(double r,double q){raw[next]=r;ratio[next]=q;next=(next+1)%WINDOW;if(count<WINDOW)count++;lastRatio=q;expectedMisses=invalidExpected=0;if(r<=8||r>=32759)saturationHits=Math.min(8,saturationHits+1);else saturationHits=Math.max(0,saturationHits-1);}
    }

    private final Map<String,Channel> channels=new LinkedHashMap<>();

    public SensorHealthEngine(){for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ)channels.put(hw.key,new Channel(hw.key,hw.relay));}

    /** Accept one normalized API snapshot. Live health follows actual relay power, not measurement-screen lifecycle. */
    public synchronized void accept(JSONObject snapshot,long elapsedMs){
        if(snapshot==null)return;
        JSONObject relay=snapshot.optJSONObject("relay");int mask=relay==null?0:relay.optInt("mask",0)&HazRobHardwareProfile.INSTALLED_RELAY_MASK;
        JSONObject live=snapshot.optJSONObject("mq");
        for(Channel c:channels.values())if((mask&(1<<(c.group-1)))!=0)observeExpected(c,live==null?null:live.optJSONObject(c.key));

        JSONObject m=snapshot.optJSONObject("measurement");String state=m==null?"idle":m.optString("state","idle");
        if("ready".equals(state)&&m!=null){JSONObject avg=m.optJSONObject("mq");if(avg!=null)for(Channel c:channels.values()){JSONObject r=avg.optJSONObject(c.key);if(valid(r))c.lastRatio=finite(r,"ratio");}}
        annotate(snapshot);
    }

    /** Adds the temporal health/noise result to the same canonical channel quality object stored and consumed downstream. */
    private void annotate(JSONObject snapshot){
        JSONObject mq=snapshot.optJSONObject("mq");if(mq==null)return;
        for(Channel c:channels.values()){
            JSONObject r=mq.optJSONObject(c.key);if(r==null)continue;Snapshot s=assess(c);
            try{
                JSONObject q=r.optJSONObject("quality");if(q==null){q=new JSONObject();r.put("quality",q);}
                q.put("temporal_state",s.state.name());q.put("temporal_valid_samples",s.validSamples);q.put("temporal_expected_misses",s.expectedMisses);
                q.put("raw_mean",Double.isFinite(s.rawMean)?s.rawMean:JSONObject.NULL);q.put("raw_std",Double.isFinite(s.rawStd)?s.rawStd:JSONObject.NULL);
                boolean noiseReady=s.validSamples>=10&&Double.isFinite(s.noiseCv);q.put("noise_evaluable",noiseReady);q.put("noise_cv",noiseReady?s.noiseCv:JSONObject.NULL);
                q.put("noise_flag",noiseReady?s.state==State.UNSTABLE:JSONObject.NULL);q.put("noise_status",noiseReady?(s.state==State.UNSTABLE?"UNSTABLE":"WITHIN_TEMPORAL_THRESHOLD"):"INSUFFICIENT_TEMPORAL_SAMPLES");
                q.put("temporal_saturation_flag",s.state==State.SATURATED);q.put("stuck_flag",s.state==State.STUCK);
                q.put("drift_warning",s.state==State.DRIFT_WARNING);q.put("drift_percent",Double.isFinite(s.driftPercent)?s.driftPercent:JSONObject.NULL);
            }catch(Exception ignored){}
        }
    }

    private static void observeExpected(Channel c,JSONObject r){
        if(r==null){c.expectedMisses=Math.min(99,c.expectedMisses+1);return;}
        if(!valid(r)){c.invalidExpected=Math.min(99,c.invalidExpected+1);return;}
        double raw=finite(r,"raw"),ratio=finite(r,"ratio");
        if(!Double.isFinite(raw)||!Double.isFinite(ratio)||ratio<=0){c.invalidExpected=Math.min(99,c.invalidExpected+1);return;}
        c.add(raw,ratio);
    }

    public synchronized void setBaseline(String key,double ratio){Channel c=channels.get(key);if(c!=null)c.baseline=Double.isFinite(ratio)&&ratio>0?ratio:Double.NaN;}
    public synchronized double baseline(String key){Channel c=channels.get(key);return c==null?Double.NaN:c.baseline;}

    public synchronized int captureCleanAirBaseline(JSONObject snapshot){
        JSONObject m=snapshot==null?null:snapshot.optJSONObject("measurement");if(m==null||!"ready".equalsIgnoreCase(m.optString("state")))return 0;
        JSONObject avg=m.optJSONObject("mq");if(avg==null)return 0;int n=0;
        for(Channel c:channels.values()){JSONObject r=avg.optJSONObject(c.key);double q=finite(r,"ratio");if(valid(r)&&Double.isFinite(q)&&q>0){c.baseline=q;c.lastDrift=0;c.driftChecked=false;n++;}}
        return n;
    }

    public synchronized int evaluateCleanAirDrift(JSONObject snapshot){
        JSONObject m=snapshot==null?null:snapshot.optJSONObject("measurement");if(m==null||!"ready".equalsIgnoreCase(m.optString("state")))return 0;
        JSONObject avg=m.optJSONObject("mq");if(avg==null)return 0;int n=0;
        for(Channel c:channels.values()){
            if(!Double.isFinite(c.baseline)||c.baseline<=0)continue;JSONObject r=avg.optJSONObject(c.key);double q=finite(r,"ratio");if(!valid(r)||!Double.isFinite(q)||q<=0)continue;
            c.lastRatio=q;c.lastDrift=100.0*(q-c.baseline)/c.baseline;c.driftChecked=true;n++;
        }
        return n;
    }

    public synchronized Snapshot snapshot(String key){Channel c=channels.get(key);return c==null?unknown(key):assess(c);}
    public synchronized Map<String,Snapshot> snapshots(){Map<String,Snapshot> out=new LinkedHashMap<>();for(Channel c:channels.values())out.put(c.key,assess(c));return out;}

    private static Snapshot assess(Channel c){
        Stats s=stats(c.raw,c.count);State state;String note;
        if(c.expectedMisses>=3){state=State.MISSING;note="لا تصل بيانات أثناء تشغيل مجموعة الحساس";}
        else if(c.invalidExpected>=3){state=State.INVALID;note="بيانات ADC/Rs غير صالحة بصورة متكررة";}
        else if(c.saturationHits>=2){state=State.SATURATED;note="ADC قريب من حد القياس";}
        else if(c.count>=16&&s.range<=1.0){state=State.STUCK;note="الإشارة شبه ثابتة بصورة غير طبيعية";}
        else if(c.count>=10&&Double.isFinite(s.cv)&&s.cv>0.12){state=State.UNSTABLE;note="تذبذب مرتفع في الإشارة";}
        else if(c.driftChecked&&Double.isFinite(c.lastDrift)&&Math.abs(c.lastDrift)>=30.0){state=State.DRIFT_WARNING;note="انحراف عن baseline الهواء النظيف";}
        else if(c.count>=4){state=State.HEALTHY;note="قناة الاستحواذ سليمة";}
        else{state=State.UNKNOWN;note="عينات غير كافية";}
        return new Snapshot(c.key,state,c.count,c.expectedMisses,s.mean,s.std,s.cv,c.baseline,c.driftChecked?c.lastDrift:Double.NaN,c.lastRatio,note);
    }

    private static final class Stats{double mean=Double.NaN,std=Double.NaN,cv=Double.NaN,range=Double.NaN;}
    private static Stats stats(double[] x,int count){Stats s=new Stats();if(count<=0)return s;int n=Math.min(count,x.length),valid=0;double sum=0,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(int i=0;i<n;i++){double v=x[i];if(!Double.isFinite(v))continue;valid++;sum+=v;min=Math.min(min,v);max=Math.max(max,v);}if(valid==0)return s;s.mean=sum/valid;double ss=0;for(int i=0;i<n;i++){double v=x[i];if(Double.isFinite(v)){double d=v-s.mean;ss+=d*d;}}s.std=Math.sqrt(ss/valid);s.cv=Math.abs(s.mean)>1e-9?s.std/Math.abs(s.mean):Double.NaN;s.range=max-min;return s;}
    private static boolean valid(JSONObject r){return r!=null&&r.optBoolean("valid",r.optBoolean("ok",false));}
    private static double finite(JSONObject r,String key){if(r==null||r.isNull(key))return Double.NaN;double v=r.optDouble(key,Double.NaN);return Double.isFinite(v)?v:Double.NaN;}
    private static Snapshot unknown(String key){return new Snapshot(key,State.UNKNOWN,0,0,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,"حساس غير معروف");}
    private static String[] installedKeys(){String[] out=new String[HazRobHardwareProfile.MQ.length];for(int i=0;i<out.length;i++)out[i]=HazRobHardwareProfile.MQ[i].key;return out;}
    public static String stateAr(State s){switch(s){case HEALTHY:return"سليم";case MISSING:return"مفقود";case INVALID:return"غير صالح";case SATURATED:return"تشبع ADC";case STUCK:return"إشارة ثابتة";case UNSTABLE:return"غير مستقر";case DRIFT_WARNING:return"انحراف baseline";default:return"غير مقيم";}}
    public static String compact(Snapshot s){if(s==null)return"غير مقيم";String x=stateAr(s.state);if(Double.isFinite(s.driftPercent))x+=String.format(Locale.US," • drift %+.1f%%",s.driftPercent);return x;}
}
