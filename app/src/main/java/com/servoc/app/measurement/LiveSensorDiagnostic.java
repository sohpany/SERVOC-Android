package com.servoc.app.measurement;

import com.servoc.app.hardware.HazRobHardwareProfile;
import org.json.JSONObject;

/**
 * Derives operator-facing health for the live MQ acquisition path.
 *
 * This diagnostic is deliberately conservative: HazRob does not currently measure the
 * 5 V sensor rail directly, so a low-voltage cause is never reported as confirmed.
 */
public final class LiveSensorDiagnostic {
    public static final long SETTLE_DIAGNOSTIC_MS=5000L;

    public static final String LIVE_OK="live_ok";
    public static final String MQ_POWER_OFF="mq_power_off";
    public static final String ADS_MISSING="ads_missing";
    public static final String ADS_PARTIAL="ads_partial";
    public static final String WAITING_SAMPLES="waiting_samples";
    public static final String SUSPECT_POWER_OR_GROUND="suspected_sensor_power_or_ground";
    public static final String PARTIAL_SIGNAL_FAULT="partial_signal_fault";
    public static final String ADS_PARTIAL_NO_SIGNAL="ads_partial_no_signal";

    private LiveSensorDiagnostic(){}

    public static final class Result {
        public final String code,severity,messageAr;
        public final int validChannels,availableChannels,expectedChannels;
        public final boolean powerCauseConfirmed;

        Result(String code,String severity,String messageAr,int valid,int available,int expected,boolean confirmed){
            this.code=code;this.severity=severity;this.messageAr=messageAr;
            this.validChannels=valid;this.availableChannels=available;this.expectedChannels=expected;
            this.powerCauseConfirmed=confirmed;
        }

        public JSONObject toJson(){
            JSONObject x=new JSONObject();
            try{
                x.put("code",code);x.put("severity",severity);x.put("message_ar",messageAr);
                x.put("valid_channels",validChannels);x.put("available_channels",availableChannels);x.put("expected_channels",expectedChannels);
                x.put("power_cause_confirmed",powerCauseConfirmed);
                x.put("diagnostic_basis","inferred");
                x.put("live_independent_of_session",true);
            }catch(Exception ignored){}
            return x;
        }
    }

    public static Result evaluate(JSONObject relay,JSONObject adc,JSONObject mq){
        final int expected=HazRobHardwareProfile.MQ.length;
        boolean power=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);
        long powerMs=relay==null?0:Math.max(0,relay.optLong("power_ms",0));
        boolean ads1=adc!=null&&adc.optBoolean("ads1",false),ads2=adc!=null&&adc.optBoolean("ads2",false);

        if(!power)return result(MQ_POWER_OFF,"info","طاقة حساسات MQ متوقفة؛ القراءة الحية تبدأ عند تشغيل MQ POWER.",0,0,expected);
        if(!ads1&&!ads2)return result(ADS_MISSING,"fault","ADS1 وADS2 غير مكتشفين؛ تحقق من VDD/GND وSDA/SCL وعناوين ADDR. لا يمكن إثبات ضعف الجهد دون قياس خط التغذية.",0,0,expected);

        int available=0,valid=0;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            boolean adsReady=hw.adsIndex==0?ads1:ads2;
            if(!adsReady)continue;
            available++;
            JSONObject q=mq==null?null:mq.optJSONObject(hw.key);
            if(q!=null&&q.optBoolean("ok",q.optBoolean("valid",false)))valid++;
        }

        if(valid==0&&powerMs>0&&powerMs<SETTLE_DIAGNOSTIC_MS){
            return result(WAITING_SAMPLES,"info","الطاقة مفعلة وADS مستجيب؛ انتظار أول دورة عينات حية قبل تشخيص الإشارة.",0,available,expected);
        }

        if(!ads1||!ads2){
            if(valid==0)return result(ADS_PARTIAL_NO_SIGNAL,"fault","أحد محولي ADS غير مكتشف، والقنوات المتاحة لا تعطي إشارة MQ صالحة. تحقق من التغذية والأرضي وAO وI²C؛ ضعف التغذية احتمال غير مؤكد.",0,available,expected);
            return result(ADS_PARTIAL,"warning","القراءة الحية تعمل جزئيًا، لكن أحد محولي ADS غير مكتشف؛ القنوات التابعة له غير متاحة.",valid,available,expected);
        }

        if(valid==0)return result(SUSPECT_POWER_OR_GROUND,"warning","ADS1 وADS2 مستجيبان لكن لا توجد أي قناة MQ صالحة. يوجد اشتباه في تغذية الحساسات أو الأرضي المشترك أو مسار AO؛ ضعف الجهد غير مؤكد لعدم وجود قياس مباشر لخط 5V.",0,available,expected);
        if(valid<available)return result(PARTIAL_SIGNAL_FAULT,"warning","القياس الحي يعمل جزئيًا؛ بعض قنوات MQ غير صالحة. افحص الحساسات وAO والتغذية المشتركة للقنوات المتأثرة.",valid,available,expected);
        return result(LIVE_OK,"ok","القراءة الحية تعمل على جميع قنوات MQ المتاحة، وهي مستقلة عن بدء أو إيقاف جلسة التسجيل.",valid,available,expected);
    }

    private static Result result(String code,String severity,String message,int valid,int available,int expected){
        return new Result(code,severity,message,valid,available,expected,false);
    }
}
