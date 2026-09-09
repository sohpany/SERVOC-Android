#include "Hardware.h"
#include "Alerts.h"
#include <Preferences.h>
#include <Wire.h>
#include <WiFi.h>

Settings settings;
namespace{
Preferences nv;
float af(float v){return v<0?-v:v;}int ri(float v){return (int)(v>=0?v+.5f:v-.5f);}bool ff(float v){return v==v&&v>-1e30f&&v<1e30f;}

/* drive */
float tarL=0,tarR=0,curL=0,curR=0;uint32_t cmdAt=0,driveAt=0;bool fs=false,retracing=false;
float lim(float v){float m=constrain(settings.speed,20,100);return constrain(v,-m,m);}
void motorWrite(uint8_t en,uint8_t a,uint8_t b,float v,bool inv){int c=constrain(ri(v),-100,100);if(inv)c=-c;int d=map(abs(c),0,100,0,255);if(c>0){digitalWrite(a,HIGH);digitalWrite(b,LOW);}else if(c<0){digitalWrite(a,LOW);digitalWrite(b,HIGH);}else{digitalWrite(a,LOW);digitalWrite(b,LOW);d=0;}analogWrite(en,d);}
void driveWrite(){motorWrite(Cfg::DRIVE_L_EN,Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT);motorWrite(Cfg::DRIVE_R_EN,Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT);}
float ramp(float c,float t,float dt){if(af(t-c)<.1f)return t;bool rev=(c>0&&t<0)||(c<0&&t>0);float rate=rev?Cfg::REVERSE:(af(t)<af(c)?Cfg::DECEL:Cfg::ACCEL),d=rate*dt;if(rev)return c>0?max(0.0f,c-d):min(0.0f,c+d);return c<t?min(t,c+d):max(t,c-d);}
constexpr uint16_t HIST_STEP=250,HIST_N=Cfg::RETRACE_MS/HIST_STEP;struct Hist{int8_t l,r;};Hist hist[HIST_N];
uint16_t histHead=0,histCount=0,retrIdx=0,retrRemain=0;uint32_t histAt=0,retrAt=0;
void clearHistory(){histHead=histCount=retrIdx=retrRemain=0;}
void historyUpdate(uint32_t n){if(retracing||fs||n-histAt<HIST_STEP)return;histAt=n;hist[histHead]={(int8_t)constrain(ri(curL),-100,100),(int8_t)constrain(ri(curR),-100,100)};histHead=(histHead+1)%HIST_N;if(histCount<HIST_N)histCount++;}
void zeroMotionPreserveHistory(){tarL=tarR=curL=curR=0;driveWrite();}
void beginRetrace(uint32_t n){zeroMotionPreserveHistory();fs=true;retracing=false;if(!histCount)return;uint16_t i=(histHead+HIST_N-1)%HIST_N,k=histCount;while(k&&!hist[i].l&&!hist[i].r){i=(i+HIST_N-1)%HIST_N;k--;}if(!k)return;retrIdx=i;retrRemain=k;retrAt=n;retracing=true;}
void retraceUpdate(uint32_t n){if(!retracing||n-retrAt<HIST_STEP)return;retrAt=n;if(!retrRemain){tarL=tarR=curL=curR=0;driveWrite();retracing=false;return;}Hist h=hist[retrIdx];tarL=constrain(-(int)h.l,-Cfg::RETRACE_MAX_SPEED,Cfg::RETRACE_MAX_SPEED);tarR=constrain(-(int)h.r,-Cfg::RETRACE_MAX_SPEED,Cfg::RETRACE_MAX_SPEED);retrIdx=(retrIdx+HIST_N-1)%HIST_N;retrRemain--;}
void driveUpdate(){uint32_t n=millis();if(!retracing&&!fs&&n-cmdAt>Cfg::DRIVE_LEASE_MS&&(af(tarL)>.1f||af(tarR)>.1f))tarL=tarR=0;retraceUpdate(n);historyUpdate(n);if(n-driveAt<10)return;float dt=min(n-driveAt,50UL)/1000.0f;driveAt=n;curL=ramp(curL,tarL,dt);curR=ramp(curR,tarR,dt);driveWrite();}
void motionStopInternal(){tarL=tarR=curL=curR=0;retracing=fs=false;clearHistory();driveWrite();}

/* energy basis; Android owns SOC estimation */
uint32_t energyAt=0,driveEqMs=0,mqOnTotalMs=0;
void energyUpdate(){uint32_t n=millis();if(!energyAt){energyAt=n;return;}uint32_t dt=n-energyAt;if(dt<100)return;energyAt=n;dt=min(dt,1000UL);uint16_t load=(uint16_t)constrain((int)(af(curL)+af(curR)),0,200);driveEqMs+=(uint32_t)(((uint64_t)dt*load)/200U);if(digitalRead(Cfg::RELAY_MQ)==Cfg::RELAY_ON)mqOnTotalMs+=dt;}

/* MQ power */
bool mqPowerOn=false;uint32_t mqPowerAt=0;
void relayRaw(bool on){if(on&&!mqPowerOn)mqPowerAt=millis();else if(!on)mqPowerAt=0;digitalWrite(Cfg::RELAY_MQ,on?Cfg::RELAY_ON:Cfg::RELAY_OFF);mqPowerOn=on;if(!on)Alerts::setGas(0);}
uint32_t mqPowerMs(){return mqPowerOn&&mqPowerAt?millis()-mqPowerAt:0;}

/* compact I2C */
bool probe(uint8_t a){Wire.beginTransmission(a);return Wire.endTransmission()==0;}
bool wr8(uint8_t a,uint8_t r,uint8_t v){Wire.beginTransmission(a);Wire.write(r);Wire.write(v);return Wire.endTransmission()==0;}
bool wr16(uint8_t a,uint8_t r,uint16_t v){Wire.beginTransmission(a);Wire.write(r);Wire.write(v>>8);Wire.write(v&255);return Wire.endTransmission()==0;}
bool rd(uint8_t a,uint8_t r,uint8_t*b,uint8_t n){Wire.beginTransmission(a);Wire.write(r);if(Wire.endTransmission(false))return false;if(Wire.requestFrom((int)a,(int)n)!=(int)n)return false;for(uint8_t i=0;i<n;i++)b[i]=Wire.read();return true;}
uint8_t rd8(uint8_t a,uint8_t r){uint8_t v=0;rd(a,r,&v,1);return v;}
uint16_t u16(const uint8_t*p){return (uint16_t)p[0]|((uint16_t)p[1]<<8);}int16_t s16(const uint8_t*p){return (int16_t)u16(p);}

/* ADS1115 */
bool ads1ok=false,ads2ok=false;uint32_t adsHealthAt=0;
struct Reading{uint16_t raw=0;float adc=NAN,ao=NAN,rs=NAN,ratio=NAN;bool ok=false;uint32_t sampleUs=0,scanCycle=0,channelOffsetUs=0;};
Reading mq[Cfg::MQ_N];uint32_t mqAt=0,mqScanCycle=0,mqCycleStartUs=0;uint8_t mqIndex=0;
bool adsOK(uint8_t n){return n?ads2ok:ads1ok;}
bool adsBegin(uint8_t n){bool ok=probe(n?Cfg::ADS2_ADDR:Cfg::ADS1_ADDR);if(n)ads2ok=ok;else ads1ok=ok;return ok;}
int16_t adsRead(uint8_t a,uint8_t ch){uint16_t cfg=0x8000|((uint16_t)(4+ch)<<12)|0x0100|0x00E0|0x0003;if(!wr16(a,1,cfg))return INT16_MIN;delayMicroseconds(1300);uint8_t b[2];if(!rd(a,0,b,2))return INT16_MIN;return (int16_t)(((uint16_t)b[0]<<8)|b[1]);}
void adsHealthUpdate(){uint32_t n=millis();if(n-adsHealthAt<Cfg::ADS_REPROBE_MS)return;adsHealthAt=n;bool p1=probe(Cfg::ADS1_ADDR),p2=probe(Cfg::ADS2_ADDR);if(!p1)ads1ok=false;else if(!ads1ok)adsBegin(0);if(!p2)ads2ok=false;else if(!ads2ok)adsBegin(1);}
void readMQ(uint8_t i){const auto&c=Cfg::mq[i];Reading&r=mq[i];if(!mqPowerOn||!adsOK(c.ads)){r=Reading();return;}int32_t sum=0;uint8_t got=0,a=c.ads?Cfg::ADS2_ADDR:Cfg::ADS1_ADDR;for(uint8_t n=0;n<Cfg::ADC_SAMPLES;n++){int16_t x=adsRead(a,c.ch);if(x==INT16_MIN)continue;sum+=x;got++;}if(!got){r=Reading();if(c.ads)ads2ok=false;else ads1ok=false;return;}uint32_t sampled=micros();int32_t av=sum/got;if(av<0)av=0;r.raw=(uint16_t)av;r.adc=r.ao=av*(Cfg::ADS_FULL_SCALE_V/32768.0f);r.sampleUs=sampled;r.scanCycle=mqScanCycle;r.channelOffsetUs=sampled-mqCycleStartUs;if(r.ao<=.01f||r.ao>=Cfg::MQ_VCC){r.ok=false;return;}r.rs=Cfg::RL*(Cfg::MQ_VCC-r.ao)/r.ao;r.ratio=r.rs/c.r0;r.ok=ff(r.rs)&&r.rs>0;}
void mqUpdate(){uint32_t n=millis();uint16_t slot=Cfg::MQ_PERIOD/Cfg::MQ_N;if(slot<2)slot=2;if(n-mqAt<slot)return;mqAt=n;if(!mqIndex){if(++mqScanCycle==0)mqScanCycle=1;mqCycleStartUs=micros();}readMQ(mqIndex);if(++mqIndex>=Cfg::MQ_N)mqIndex=0;}

/* BME280/BMP280 chamber environment */
struct EnvCal{uint16_t T1,P1;int16_t T2,T3,P2,P3,P4,P5,P6,P7,P8,P9,H2,H4,H5;uint8_t H1,H3;int8_t H6;};EnvCal ec{};
uint8_t envType=0,envAddr=0,envId=0;float temp=NAN,hum=NAN,press=NAN;uint32_t envAt=0;int32_t tFine=0;
bool envCal(){uint8_t b[24];if(!rd(envAddr,0x88,b,24))return false;ec.T1=u16(b);ec.T2=s16(b+2);ec.T3=s16(b+4);ec.P1=u16(b+6);ec.P2=s16(b+8);ec.P3=s16(b+10);ec.P4=s16(b+12);ec.P5=s16(b+14);ec.P6=s16(b+16);ec.P7=s16(b+18);ec.P8=s16(b+20);ec.P9=s16(b+22);if(envType==1){ec.H1=rd8(envAddr,0xA1);uint8_t h[7];if(!rd(envAddr,0xE1,h,7))return false;ec.H2=s16(h);ec.H3=h[2];int16_t h4=(h[3]<<4)|(h[4]&15),h5=(h[5]<<4)|(h[4]>>4);if(h4&0x800)h4|=0xF000;if(h5&0x800)h5|=0xF000;ec.H4=h4;ec.H5=h5;ec.H6=(int8_t)h[6];}return true;}
void envRead(){if(!envType)return;uint8_t b[8];if(!rd(envAddr,0xF7,b,envType==1?8:6))return;int32_t ap=((uint32_t)b[0]<<12)|((uint32_t)b[1]<<4)|(b[2]>>4),at=((uint32_t)b[3]<<12)|((uint32_t)b[4]<<4)|(b[5]>>4);int32_t v1=((((at>>3)-((int32_t)ec.T1<<1)))*ec.T2)>>11,v2=(((((at>>4)-(int32_t)ec.T1)*((at>>4)-(int32_t)ec.T1))>>12)*ec.T3)>>14;tFine=v1+v2;temp=((tFine*5+128)>>8)/100.0f;int64_t p1=(int64_t)tFine-128000,p2=p1*p1*ec.P6;p2+=((p1*ec.P5)<<17)+((int64_t)ec.P4<<35);p1=((p1*p1*ec.P3)>>8)+((p1*ec.P2)<<12);p1=((((int64_t)1<<47)+p1)*ec.P1)>>33;if(!p1)press=NAN;else{int64_t p=1048576-ap;p=(((p<<31)-p2)*3125)/p1;p1=(ec.P9*(p>>13)*(p>>13))>>25;p2=(ec.P8*p)>>19;p=((p+p1+p2)>>8)+((int64_t)ec.P7<<4);press=p/25600.0f;}hum=NAN;if(envType==1){int32_t ah=((uint16_t)b[6]<<8)|b[7],h=tFine-76800;h=(((((ah<<14)-((int32_t)ec.H4<<20)-((int32_t)ec.H5*h))+16384)>>15)*(((((((h*ec.H6)>>10)*(((h*ec.H3)>>11)+32768))>>10)+2097152)*ec.H2+8192)>>14));h-=(((((h>>15)*(h>>15))>>7)*ec.H1)>>4);h=constrain(h,0,419430400);hum=(h>>12)/1024.0f;}}
void envBegin(){const uint8_t aa[]={0x76,0x77};for(uint8_t a:aa){if(!probe(a))continue;uint8_t id=rd8(a,0xD0),type=id==0x60?1:((id==0x56||id==0x57||id==0x58)?2:0);if(!type)continue;envAddr=a;envId=id;envType=type;if(!envCal()){envType=0;continue;}if(envType==1)wr8(a,0xF2,1);wr8(a,0xF4,0x27);wr8(a,0xF5,0);delay(10);envRead();break;}}
void envUpdate(){if(!envType||millis()-envAt<Cfg::ENV_PERIOD)return;envAt=millis();envRead();}

/* sound / LDR / IR */
uint16_t isqrt(uint32_t x){uint32_t op=x,res=0,one=1UL<<30;while(one>op)one>>=2;while(one){if(op>=res+one){op-=res+one;res=(res>>1)+one;}else res>>=1;one>>=2;}return (uint16_t)res;}
float soundRms=NAN,soundP2P=NAN;uint32_t soundAt=0;
void soundUpdate(){uint32_t n=millis();if(n-soundAt<Cfg::SOUND_PERIOD)return;soundAt=n;uint32_t sum=0,sq=0;uint16_t lo=4095,hi=0;for(uint16_t k=0;k<Cfg::SOUND_SAMPLES;k++){uint16_t x=analogRead(Cfg::SOUND_PIN);sum+=x;sq+=(uint32_t)x*x;lo=min(lo,x);hi=max(hi,x);}uint32_t mean=sum/Cfg::SOUND_SAMPLES,var=sq/Cfg::SOUND_SAMPLES;var=var>mean*mean?var-mean*mean:0;constexpr float V=3.3f/4095.0f;soundRms=isqrt(var)*V;soundP2P=(hi-lo)*V;}
bool irWarning=false;uint8_t irLevel=HIGH;uint32_t irAt=0;bool ultraWarning=false;
void hazardAlert(){Alerts::setIr(irWarning||ultraWarning);}
void irUpdate(){uint32_t n=millis();if(n-irAt<Cfg::IR_PERIOD)return;irAt=n;irLevel=digitalRead(Cfg::IR_PIN);irWarning=irLevel==Cfg::IR_ACTIVE;hazardAlert();}
uint8_t ldrLevel=HIGH;uint32_t ldrAt=0;void ldrUpdate(){uint32_t n=millis();if(n-ldrAt<Cfg::LDR_PERIOD)return;ldrAt=n;ldrLevel=digitalRead(Cfg::LDR_PIN);}

/* battery ADC1 */
uint16_t batteryRaw=0;float batteryV=NAN;uint32_t batteryAt=0;
void batteryUpdate(){uint32_t n=millis();if(n-batteryAt<Cfg::BATTERY_PERIOD)return;batteryAt=n;uint32_t s=0;for(uint8_t i=0;i<8;i++)s+=analogRead(Cfg::BATTERY_PIN);batteryRaw=(uint16_t)(s/8);batteryV=batteryRaw*(3.3f/4095.0f)*Cfg::BAT_DIV;}

/* ultrasonic */
float ultraCm=NAN;uint8_t ultraState=0;uint32_t ultraAt=0,ultraPulseAt=0,ultraRiseUs=0;
void ultraUpdate(){uint32_t now=millis(),us=micros();if(!ultraState){if(now-ultraAt<Cfg::ULTRA_PERIOD)return;ultraAt=now;digitalWrite(Cfg::ULTRA_TRIG_PIN,LOW);delayMicroseconds(2);digitalWrite(Cfg::ULTRA_TRIG_PIN,HIGH);delayMicroseconds(10);digitalWrite(Cfg::ULTRA_TRIG_PIN,LOW);ultraPulseAt=micros();ultraState=1;return;}if(ultraState==1){if(digitalRead(Cfg::ULTRA_ECHO_PIN)){ultraRiseUs=us;ultraState=2;return;}if(us-ultraPulseAt>Cfg::ULTRA_TIMEOUT_US){ultraCm=NAN;ultraWarning=false;ultraState=0;hazardAlert();}}else{if(!digitalRead(Cfg::ULTRA_ECHO_PIN)){uint32_t d=us-ultraRiseUs;ultraCm=d*0.01715f;ultraWarning=ff(ultraCm)&&ultraCm>1&&ultraCm<Cfg::ULTRA_WARN_CM;ultraState=0;hazardAlert();return;}if(us-ultraRiseUs>Cfg::ULTRA_TIMEOUT_US){ultraCm=NAN;ultraWarning=false;ultraState=0;hazardAlert();}}}

/* measurement */
enum Stage:uint8_t{IDLE,WARM,MEASURE,READY,ERROR};Stage stage=IDLE;bool measurePower=false;uint32_t stageAt=0,sampleAt=0;
uint32_t rawSum[Cfg::MQ_N]={};float adcSum[Cfg::MQ_N]={},aoSum[Cfg::MQ_N]={},rsSum[Cfg::MQ_N]={};uint16_t cnt[Cfg::MQ_N]={};
void clearSums(){memset(rawSum,0,sizeof(rawSum));memset(adcSum,0,sizeof(adcSum));memset(aoSum,0,sizeof(aoSum));memset(rsSum,0,sizeof(rsSum));memset(cnt,0,sizeof(cnt));}
void resetMeasurement(){clearSums();measurePower=false;stageAt=sampleAt=0;stage=IDLE;}
void collect(){if(!measurePower)return;for(uint8_t i=0;i<Cfg::MQ_N;i++){if(!mq[i].ok)continue;rawSum[i]+=mq[i].raw;adcSum[i]+=mq[i].adc;aoSum[i]+=mq[i].ao;rsSum[i]+=mq[i].rs;cnt[i]++;}}
void finishMeasure(){bool ok=false;for(uint8_t i=0;i<Cfg::MQ_N;i++)if(cnt[i]){ok=true;break;}stage=ok?READY:ERROR;}
void measureUpdate(){if(stage!=MEASURE)return;if(mqPowerOn!=measurePower){resetMeasurement();return;}uint32_t n=millis();if(n-sampleAt>=Cfg::SAMPLE_PERIOD){sampleAt=n;collect();}if(n-stageAt>=(uint32_t)settings.measure*1000UL)finishMeasure();}

/* link loss */
bool linkArmed=false,linkLost=false,linkHold=false;uint32_t linkAt=0,holdAt=0;
void recoverLink(uint32_t n){if(linkHold||linkLost||retracing){zeroMotionPreserveHistory();retracing=false;fs=false;linkHold=false;linkLost=false;clearHistory();Alerts::setLink(false);}linkAt=n;}
void beginLinkHold(uint32_t n){if(linkHold||retracing)return;zeroMotionPreserveHistory();fs=true;linkHold=linkLost=true;holdAt=n;Alerts::setLink(true);}
void linkWatchUpdate(){if(!linkArmed)return;uint32_t n=millis();if(retracing)return;if(!linkHold){if(n-linkAt>Cfg::LINK_DETECT_MS)beginLinkHold(n);return;}if(n-linkAt<=Cfg::LINK_DETECT_MS){recoverLink(n);return;}if(n-holdAt<Cfg::LINK_HOLD_MS)return;linkHold=false;resetMeasurement();relayRaw(false);beginRetrace(n);}

/* JSON */
String num(float v,uint8_t d=2){return ff(v)?String(v,(unsigned)d):"null";}const char* bj(bool v){return v?"true":"false";}
void loadSettings(){nv.begin(Cfg::NVS,false);settings.cam=nv.getString("camIP",Cfg::CAM);settings.speed=constrain(nv.getInt("speed",100),20,100);uint32_t w=nv.getUInt("warmup",Cfg::OPERATIONAL_WARMUP_SEC),m=nv.getUInt("measure",Cfg::ANALYSIS_WINDOW_SEC);settings.warm=(uint16_t)min(w,3600UL);settings.measure=(uint16_t)constrain(m,5UL,600UL);}
String envJson(){return "{\"ok\":"+String(bj(envType))+",\"t\":"+num(temp,2)+",\"h\":"+num(hum,2)+",\"p\":"+num(press,1)+"}";}
String acquisitionJson(){return "{\"model\":\"ADS1115\",\"rate_sps\":"+String(Cfg::ADS_RATE_SPS)+",\"gain\":\"GAIN_TWOTHIRDS\",\"full_scale_v\":"+num(Cfg::ADS_FULL_SCALE_V,3)+",\"supply_v\":"+num(Cfg::ADS_SUPPLY_V,3)+",\"conversions_per_channel\":"+String(Cfg::ADC_SAMPLES)+",\"scan_period_ms\":"+String(Cfg::MQ_PERIOD)+",\"channels\":"+String(Cfg::MQ_N)+",\"sequential\":true}";}
String mqJson(bool avg){bool powered=avg?measurePower:mqPowerOn;String j="{";uint32_t nowUs=micros();for(uint8_t i=0;i<Cfg::MQ_N;i++){if(i)j+=",";const auto&c=Cfg::mq[i];j+="\""+String(c.name)+"\":{";if(avg){if(!cnt[i])j+="\"ok\":false";else{float n=cnt[i],rs=rsSum[i]/n;j+="\"ok\":true,\"raw\":"+num(rawSum[i]/n,1)+",\"adc\":"+num(adcSum[i]/n,3)+",\"ao\":"+num(aoSum[i]/n,3)+",\"rs\":"+num(rs,3)+",\"ratio\":"+num(rs/c.r0,3);}}else{j+="\"ok\":"+String(bj(mq[i].ok))+",\"raw\":"+String(mq[i].raw)+",\"adc\":"+num(mq[i].adc,3)+",\"ao\":"+num(mq[i].ao,3)+",\"rs\":"+num(mq[i].rs,3)+",\"ratio\":"+num(mq[i].ratio,3);if(mq[i].sampleUs)j+=",\"sample_us\":"+String(mq[i].sampleUs)+",\"scan_cycle\":"+String(mq[i].scanCycle)+",\"channel_offset_us\":"+String(mq[i].channelOffsetUs)+",\"age_us\":"+String(nowUs-mq[i].sampleUs);}j+=",\"powered\":"+String(bj(powered))+",\"relay\":1,\"row\":1,\"ads\":"+String(c.ads)+",\"ch\":"+String(c.ch)+",\"r0\":"+num(c.r0,3)+",\"r0_calibrated\":false,\"r0_source\":\"firmware_default\"}";}return j+"}";}
String driveJson(){return "{\"driver\":\"L298N\",\"left\":"+String(ri(curL))+",\"right\":"+String(ri(curR))+",\"failsafe\":"+String(bj(fs))+",\"retrace\":"+String(bj(retracing))+",\"hold\":"+String(bj(linkHold))+",\"history\":"+String(histCount)+",\"pins\":{\"l_en\":"+String(Cfg::DRIVE_L_EN)+",\"l_in1\":"+String(Cfg::DRIVE_L_IN1)+",\"l_in2\":"+String(Cfg::DRIVE_L_IN2)+",\"r_en\":"+String(Cfg::DRIVE_R_EN)+",\"r_in1\":"+String(Cfg::DRIVE_R_IN1)+",\"r_in2\":"+String(Cfg::DRIVE_R_IN2)+"}}";}
String healthJson(){return "{\"battery\":{\"v\":"+num(batteryV,2)+",\"raw\":"+String(batteryRaw)+",\"estimated\":true},\"internal_temp\":{\"installed\":false,\"c\":null,\"gpio\":"+String(Cfg::INTERNAL_TEMP_RESERVED_PIN)+"},\"proximity\":{\"cm\":"+num(ultraCm,1)+",\"warning\":"+String(bj(ultraWarning))+",\"trig\":"+String(Cfg::ULTRA_TRIG_PIN)+",\"echo\":"+String(Cfg::ULTRA_ECHO_PIN)+"},\"energy_basis\":{\"uptime_ms\":"+String(millis())+",\"drive_eq_ms\":"+String(driveEqMs)+",\"mq_on_ms\":"+String(mqOnTotalMs)+"}}";}
String lightJson(){return "{\"installed\":true,\"gpio\":"+String(Cfg::LDR_PIN)+",\"mode\":\"lm393_threshold\",\"level\":"+String(ldrLevel)+",\"quantitative_lux\":false,\"lux\":null}";}
const char* linkState(){return retracing?"retrace":linkHold?"hold":linkLost?"lost":"ok";}uint32_t holdRemaining(){if(!linkHold)return 0;uint32_t e=millis()-holdAt;return e>=Cfg::LINK_HOLD_MS?0:Cfg::LINK_HOLD_MS-e;}
}

void hwBegin(){loadSettings();Alerts::begin();pinMode(Cfg::RELAY_MQ,OUTPUT);relayRaw(false);pinMode(Cfg::IR_PIN,INPUT);pinMode(Cfg::LDR_PIN,INPUT);pinMode(Cfg::SOUND_PIN,INPUT);pinMode(Cfg::BATTERY_PIN,INPUT);pinMode(Cfg::ULTRA_TRIG_PIN,OUTPUT);digitalWrite(Cfg::ULTRA_TRIG_PIN,LOW);pinMode(Cfg::ULTRA_ECHO_PIN,INPUT);analogReadResolution(12);analogSetPinAttenuation(Cfg::SOUND_PIN,ADC_11db);analogSetPinAttenuation(Cfg::BATTERY_PIN,ADC_11db);pinMode(Cfg::DRIVE_L_EN,OUTPUT);pinMode(Cfg::DRIVE_L_IN1,OUTPUT);pinMode(Cfg::DRIVE_L_IN2,OUTPUT);pinMode(Cfg::DRIVE_R_EN,OUTPUT);pinMode(Cfg::DRIVE_R_IN1,OUTPUT);pinMode(Cfg::DRIVE_R_IN2,OUTPUT);digitalWrite(Cfg::DRIVE_L_IN1,LOW);digitalWrite(Cfg::DRIVE_L_IN2,LOW);digitalWrite(Cfg::DRIVE_R_IN1,LOW);digitalWrite(Cfg::DRIVE_R_IN2,LOW);analogWrite(Cfg::DRIVE_L_EN,0);analogWrite(Cfg::DRIVE_R_EN,0);cmdAt=driveAt=histAt=energyAt=millis();driveWrite();Wire.begin(Cfg::SDA,Cfg::SCL);Wire.setClock(400000);adsBegin(0);adsBegin(1);adsHealthAt=millis();envBegin();}
void hwUpdate(){driveUpdate();energyUpdate();adsHealthUpdate();mqUpdate();envUpdate();soundUpdate();irUpdate();ldrUpdate();batteryUpdate();ultraUpdate();measureUpdate();linkWatchUpdate();Alerts::update();}
void driveSet(int l,int r){if(retracing||linkHold||linkLost)recoverLink(millis());fs=false;tarL=lim(l);tarR=lim(r);cmdAt=millis();}
void motionStop(){motionStopInternal();}void emergencyStop(){motionStopInternal();resetMeasurement();relayRaw(false);Alerts::emergencyStop();}
bool relaySetMask(uint8_t on){if(on>1)return false;if(stage==MEASURE||stage==WARM)resetMeasurement();relayRaw(on);return true;}void sensorPowerOff(){resetMeasurement();relayRaw(false);}
void measureStart(){clearSums();measurePower=mqPowerOn;stageAt=sampleAt=millis();stage=measurePower?MEASURE:ERROR;}void measureCancel(){resetMeasurement();Alerts::setGas(0);}
bool alertSet(uint8_t s,uint32_t ttl){if(s>2)return false;Alerts::setGas(s,ttl);return true;}void alertTest(){Alerts::test();}
void linkHeartbeat(){uint32_t n=millis();linkArmed=true;if(linkHold||linkLost||retracing)recoverLink(n);else linkAt=n;}void linkSetArmed(bool a){linkArmed=a;linkHold=linkLost=false;Alerts::setLink(false);if(!a){fs=false;linkAt=holdAt=0;}else linkAt=millis();}
void saveSettings(){nv.putString("camIP",settings.cam);nv.putInt("speed",settings.speed);nv.putUInt("warmup",settings.warm);nv.putUInt("measure",settings.measure);}
String settingsJson(){return "{\"cam\":\""+settings.cam+"\",\"speed\":"+String(settings.speed)+",\"warm\":"+String(settings.warm)+",\"measure\":"+String(settings.measure)+",\"drive_driver\":\"L298N\",\"warm_manual\":true,\"mq_power_manual\":true}";}
String dataJson(){String j="{\"env\":"+envJson()+",\"device\":"+healthJson()+",\"sound\":{\"rms\":"+num(soundRms,3)+",\"p2p\":"+num(soundP2P,3)+"},\"ir\":{\"warning\":"+String(bj(irWarning))+",\"level\":"+String(irLevel)+"},\"ldr\":"+lightJson()+",\"alert\":"+Alerts::json()+",\"relay\":{\"mask\":"+String(mqPowerOn?1:0)+",\"on\":"+String(bj(mqPowerOn))+",\"gpio\":"+String(Cfg::RELAY_MQ)+",\"power_ms\":"+String(mqPowerMs())+",\"channels\":1},\"adc\":{\"ads1\":"+String(bj(ads1ok))+",\"ads2\":"+String(bj(ads2ok))+",\"acquisition\":"+acquisitionJson()+"},\"drive\":"+driveJson()+",\"mq\":"+mqJson(false)+",\"measurement\":{\"state\":"+String((uint8_t)stage)+",\"mask\":"+String(measurePower?1:0)+",\"power\":"+String(bj(measurePower))+",\"manual_power\":true,\"manual_warmup\":true,\"duration_s\":"+String(settings.measure)+",\"mq\":"+mqJson(true)+"}}";return j;}
String diagJson(){String j="{\"fw\":\""+String(Cfg::FW)+"\",\"api\":"+String(Cfg::API)+",\"hardware_profile\":\""+String(Cfg::HW_PROFILE)+"\",\"heap\":"+String(ESP.getFreeHeap())+",\"clients\":"+String(WiFi.softAPgetStationNum())+",\"device\":"+healthJson()+",\"failsafe\":"+String(bj(fs))+",\"retrace\":"+String(bj(retracing))+",\"drive\":"+driveJson()+",\"relay\":{\"mask\":"+String(mqPowerOn?1:0)+",\"on\":"+String(bj(mqPowerOn))+",\"gpio\":"+String(Cfg::RELAY_MQ)+",\"power_ms\":"+String(mqPowerMs())+",\"channels\":1},\"adc\":{\"ads1\":"+String(bj(ads1ok))+",\"ads2\":"+String(bj(ads2ok))+",\"acquisition\":"+acquisitionJson()+"},\"link\":{\"armed\":"+String(bj(linkArmed))+",\"lost\":"+String(bj(linkLost))+",\"hold\":"+String(bj(linkHold))+",\"state\":\""+String(linkState())+"\",\"hold_remaining_ms\":"+String(holdRemaining())+",\"heartbeat_age_ms\":"+String(linkArmed?millis()-linkAt:0)+"},\"envType\":"+String(envType)+",\"envAddr\":"+String(envAddr)+",\"envId\":"+String(envId)+",\"ldr\":"+lightJson()+",\"alert\":"+Alerts::json()+",\"ir\":{\"warning\":"+String(bj(irWarning))+",\"level\":"+String(irLevel)+"},\"mqPowerManual\":true,\"warmupManual\":true}";return j;}
