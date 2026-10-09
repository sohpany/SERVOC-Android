#include "Hardware.h"
#include "Alerts.h"
#include "SafetyOptions.h"
#include <Preferences.h>
#include <Wire.h>
#include <WiFi.h>

Settings settings;
namespace{
Preferences nv;
TwoWire EnvWire(1);
float af(float v){return v<0?-v:v;}int ri(float v){return (int)(v>=0?v+.5f:v-.5f);}bool ff(float v){return v==v&&v>-1e30f&&v<1e30f;}

/* drive: direct operator command -> PWM, no acceleration/retrace shaping */
float curL=0,curR=0;uint32_t cmdAt=0;bool staleStop=false;
float lim(float v){float m=constrain(settings.speed,20,100);return constrain(v,-m,m);}
void motorWrite(uint8_t en,uint8_t a,uint8_t b,float v,bool inv){int c=constrain(ri(v),-100,100);if(inv)c=-c;int d=map(abs(c),0,100,0,255);if(c>0){digitalWrite(a,HIGH);digitalWrite(b,LOW);}else if(c<0){digitalWrite(a,LOW);digitalWrite(b,HIGH);}else{digitalWrite(a,LOW);digitalWrite(b,LOW);d=0;}analogWrite(en,d);}
void driveWrite(){motorWrite(Cfg::DRIVE_L_EN,Cfg::DRIVE_L_IN1,Cfg::DRIVE_L_IN2,curL,Cfg::DRIVE_L_INVERT);motorWrite(Cfg::DRIVE_R_EN,Cfg::DRIVE_R_IN1,Cfg::DRIVE_R_IN2,curR,Cfg::DRIVE_R_INVERT);}
void driveUpdate(){uint32_t n=millis();if(!staleStop&&n-cmdAt>Cfg::CONTROL_COMMAND_TIMEOUT_MS&&(af(curL)>.1f||af(curR)>.1f)){curL=curR=0;staleStop=true;driveWrite();}}
void motionStopInternal(){curL=curR=0;staleStop=false;driveWrite();}

/* energy basis; Android owns SOC estimation */
uint32_t energyAt=0,driveEqMs=0,mqOnTotalMs=0;
void energyUpdate(){uint32_t n=millis();if(!energyAt){energyAt=n;return;}uint32_t dt=n-energyAt;if(dt<100)return;energyAt=n;dt=min(dt,1000UL);uint16_t load=(uint16_t)constrain((int)(af(curL)+af(curR)),0,200);driveEqMs+=(uint32_t)(((uint64_t)dt*load)/200U);if(digitalRead(Cfg::RELAY_MQ)==Cfg::RELAY_ON)mqOnTotalMs+=dt;}

/* MQ power */
bool mqPowerOn=false;uint32_t mqPowerAt=0;
void relayRaw(bool on){if(on&&!mqPowerOn)mqPowerAt=millis();else if(!on)mqPowerAt=0;digitalWrite(Cfg::RELAY_MQ,on?Cfg::RELAY_ON:Cfg::RELAY_OFF);mqPowerOn=on;if(!on)Alerts::setGas(0);}
uint32_t mqPowerMs(){return mqPowerOn&&mqPowerAt?millis()-mqPowerAt:0;}

/* analytical I2C: ADS1115 #1/#2 on Wire (GPIO21/22) */
bool probe(uint8_t a){Wire.beginTransmission(a);return Wire.endTransmission()==0;}
bool wr8(uint8_t a,uint8_t r,uint8_t v){Wire.beginTransmission(a);Wire.write(r);Wire.write(v);return Wire.endTransmission()==0;}
bool wr16(uint8_t a,uint8_t r,uint16_t v){Wire.beginTransmission(a);Wire.write(r);Wire.write(v>>8);Wire.write(v&255);return Wire.endTransmission()==0;}
bool rd(uint8_t a,uint8_t r,uint8_t*b,uint8_t n){Wire.beginTransmission(a);Wire.write(r);if(Wire.endTransmission(false))return false;if(Wire.requestFrom((int)a,(int)n)!=(int)n)return false;for(uint8_t i=0;i<n;i++)b[i]=Wire.read();return true;}
uint8_t rd8(uint8_t a,uint8_t r){uint8_t v=0;rd(a,r,&v,1);return v;}

/* environmental I2C: BME/BMP only on EnvWire (GPIO13/14) */
bool envProbe(uint8_t a){EnvWire.beginTransmission(a);return EnvWire.endTransmission()==0;}
bool envWr8(uint8_t a,uint8_t r,uint8_t v){EnvWire.beginTransmission(a);EnvWire.write(r);EnvWire.write(v);return EnvWire.endTransmission()==0;}
bool envRd(uint8_t a,uint8_t r,uint8_t*b,uint8_t n){EnvWire.beginTransmission(a);EnvWire.write(r);if(EnvWire.endTransmission(false))return false;if(EnvWire.requestFrom((int)a,(int)n)!=(int)n)return false;for(uint8_t i=0;i<n;i++)b[i]=EnvWire.read();return true;}
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

/* BME280/BMP280 chamber environment on dedicated EnvWire */
constexpr uint32_t ENV_REPROBE_MS=3000;constexpr uint8_t ENV_FAIL_LIMIT=3;
struct EnvCal{uint16_t T1,P1;int16_t T2,T3,P2,P3,P4,P5,P6,P7,P8,P9,H2,H4,H5;uint8_t H1,H3;int8_t H6;};EnvCal ec{};
uint8_t envType=0,envAddr=0,envId=0,envLastAddr=0,envLastId=0,envReadFails=0;uint32_t envReadErrorTotal=0,envBusRecoveries=0;float temp=NAN,hum=NAN,press=NAN;uint32_t envAt=0,envProbeAt=0,envLastOkAt=0;int32_t tFine=0;bool envAck76=false,envAck77=false;const char* envStatus="searching";
bool envOk(){return envType&&envReadFails==0&&ff(temp)&&ff(press)&&(envType!=1||ff(hum));}
bool envCal(){uint8_t b[24];if(!envRd(envAddr,0x88,b,24))return false;ec.T1=u16(b);ec.T2=s16(b+2);ec.T3=s16(b+4);ec.P1=u16(b+6);ec.P2=s16(b+8);ec.P3=s16(b+10);ec.P4=s16(b+12);ec.P5=s16(b+14);ec.P6=s16(b+16);ec.P7=s16(b+18);ec.P8=s16(b+20);ec.P9=s16(b+22);if(envType==1){uint8_t h1=0;if(!envRd(envAddr,0xA1,&h1,1))return false;ec.H1=h1;uint8_t h[7];if(!envRd(envAddr,0xE1,h,7))return false;ec.H2=s16(h);ec.H3=h[2];int16_t h4=(h[3]<<4)|(h[4]&15),h5=(h[5]<<4)|(h[4]>>4);if(h4&0x800)h4|=0xF000;if(h5&0x800)h5|=0xF000;ec.H4=h4;ec.H5=h5;ec.H6=(int8_t)h[6];}return true;}
bool envRead(){if(!envType)return false;uint8_t b[8];if(!envRd(envAddr,0xF7,b,envType==1?8:6)){envReadFails=min((uint8_t)255,(uint8_t)(envReadFails+1));envReadErrorTotal++;envStatus="read_failed";return false;}int32_t ap=((uint32_t)b[0]<<12)|((uint32_t)b[1]<<4)|(b[2]>>4),at=((uint32_t)b[3]<<12)|((uint32_t)b[4]<<4)|(b[5]>>4);int32_t v1=((((at>>3)-((int32_t)ec.T1<<1)))*ec.T2)>>11,v2=(((((at>>4)-(int32_t)ec.T1)*((at>>4)-(int32_t)ec.T1))>>12)*ec.T3)>>14;tFine=v1+v2;temp=((tFine*5+128)>>8)/100.0f;int64_t p1=(int64_t)tFine-128000,p2=p1*p1*ec.P6;p2+=((p1*ec.P5)<<17)+((int64_t)ec.P4<<35);p1=((p1*p1*ec.P3)>>8)+((p1*ec.P2)<<12);p1=((((int64_t)1<<47)+p1)*ec.P1)>>33;if(!p1)press=NAN;else{int64_t p=1048576-ap;p=(((p<<31)-p2)*3125)/p1;p1=(ec.P9*(p>>13)*(p>>13))>>25;p2=(ec.P8*p)>>19;p=((p+p1+p2)>>8)+((int64_t)ec.P7<<4);press=p/25600.0f;}hum=NAN;if(envType==1){int32_t ah=((uint16_t)b[6]<<8)|b[7],h=tFine-76800;h=(((((ah<<14)-((int32_t)ec.H4<<20)-((int32_t)ec.H5*h))+16384)>>15)*(((((((h*ec.H6)>>10)*(((h*ec.H3)>>11)+32768))>>10)+2097152)*ec.H2+8192)>>14));h-=(((((h>>15)*(h>>15))>>7)*ec.H1)>>4);h=constrain(h,0,419430400);hum=(h>>12)/1024.0f;}if(!ff(temp)||!ff(press)||(envType==1&&!ff(hum))){envReadFails=min((uint8_t)255,(uint8_t)(envReadFails+1));envReadErrorTotal++;envStatus="invalid_measurement";return false;}envReadFails=0;envLastOkAt=millis();envStatus="ok";return true;}
void envBegin(){envProbeAt=millis();envType=0;envAddr=0;envId=0;envAck76=envProbe(0x76);envAck77=envProbe(0x77);if(!envAck76&&!envAck77){envStatus="no_ack";return;}const uint8_t aa[]={0x76,0x77};for(uint8_t a:aa){if((a==0x76&&!envAck76)||(a==0x77&&!envAck77))continue;envLastAddr=a;uint8_t id=0;if(!envRd(a,0xD0,&id,1)){envStatus="chip_id_read_failed";continue;}envLastId=id;uint8_t type=id==0x60?1:((id==0x56||id==0x57||id==0x58)?2:0);if(!type){envStatus="unsupported_chip_id";continue;}envAddr=a;envId=id;envType=type;if(!envCal()){envStatus="calibration_read_failed";envType=0;continue;}bool configured=true;if(envType==1)configured=envWr8(a,0xF2,1);configured=configured&&envWr8(a,0xF4,0x27)&&envWr8(a,0xF5,0);if(!configured){envStatus="configuration_failed";envType=0;continue;}delay(10);envReadFails=0;if(!envRead()){envStatus="initial_read_failed";envType=0;continue;}break;}}
void envRecoverBus(){
  // Only recycle the *dedicated* BME/BMP bus after repeated actual read failures.
  // Never restart Wire(GPIO21/22), which owns the analytical ADS1115 channels.
  EnvWire.end();
  EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL);
  EnvWire.setClock(Cfg::ENV_I2C_HZ);
  EnvWire.setTimeOut(25);
  envBusRecoveries++;
}
void envUpdate(){
  uint32_t n=millis();
  if(!envType){if(n-envProbeAt>=ENV_REPROBE_MS)envBegin();return;}
  if(n-envAt<Cfg::ENV_PERIOD)return;
  envAt=n;
  if(!envRead()&&envReadFails>=ENV_FAIL_LIMIT){
    temp=hum=press=NAN;envType=0;
    envStatus="runtime_read_failed_reprobe";envProbeAt=n;
    envRecoverBus(); // Next attempt after original 3000ms backoff.
  }
}

/* sound / LDR / downward IR cliff */
uint16_t isqrt(uint32_t x){uint32_t op=x,res=0,one=1UL<<30;while(one>op)one>>=2;while(one){if(op>=res+one){op-=res+one;res=(res>>1)+one;}else res>>=1;one>>=2;}return (uint16_t)res;}
float soundRms=NAN,soundP2P=NAN;uint32_t soundAt=0;
void soundUpdate(){uint32_t n=millis();if(n-soundAt<Cfg::SOUND_PERIOD)return;soundAt=n;uint32_t sum=0,sq=0;uint16_t lo=4095,hi=0;for(uint16_t k=0;k<Cfg::SOUND_SAMPLES;k++){uint16_t x=analogRead(Cfg::SOUND_PIN);sum+=x;sq+=(uint32_t)x*x;lo=min(lo,x);hi=max(hi,x);}uint32_t mean=sum/Cfg::SOUND_SAMPLES,var=sq/Cfg::SOUND_SAMPLES;var=var>mean*mean?var-mean*mean:0;constexpr float V=3.3f/4095.0f;soundRms=isqrt(var)*V;soundP2P=(hi-lo)*V;}
bool irWarning=Cfg::IR_FAIL_SAFE_STARTUP,irGroundDetected=false;uint8_t irLevel=HIGH,irLostCount=0,irGroundCount=0;uint32_t irAt=0;bool ultraWarning=false,ultraStop=false;
void hazardAlert(){Alerts::setIr(irWarning||ultraWarning);}
void irUpdate(){uint32_t n=millis();if(n-irAt<Cfg::IR_PERIOD)return;irAt=n;irLevel=digitalRead(Cfg::IR_PIN);bool groundNow=irLevel==Cfg::IR_GROUND_ACTIVE;if(groundNow){irLostCount=0;if(irGroundCount<255)irGroundCount++;if(irGroundCount>=Cfg::IR_CLIFF_CLEAR_SAMPLES){irGroundDetected=true;irWarning=false;}}else{irGroundCount=0;if(irLostCount<255)irLostCount++;if(irLostCount>=Cfg::IR_CLIFF_ASSERT_SAMPLES){irGroundDetected=false;irWarning=true;}}hazardAlert();}
uint8_t ldrLevel=HIGH;uint32_t ldrAt=0;void ldrUpdate(){uint32_t n=millis();if(n-ldrAt<Cfg::LDR_PERIOD)return;ldrAt=n;ldrLevel=digitalRead(Cfg::LDR_PIN);}

/* battery ADC1 */
uint16_t batteryRaw=0;float batteryV=NAN;uint32_t batteryAt=0;
void batteryUpdate(){uint32_t n=millis();if(n-batteryAt<Cfg::BATTERY_PERIOD)return;batteryAt=n;uint32_t s=0;for(uint8_t i=0;i<8;i++)s+=analogRead(Cfg::BATTERY_PIN);batteryRaw=(uint16_t)(s/8);batteryV=batteryRaw*(3.3f/4095.0f)*Cfg::BAT_DIV;}

/* ultrasonic: interrupt edge capture prevents short HC-SR04 ECHO pulses from being missed by the main loop */
float ultraCm=NAN;uint32_t ultraAt=0,ultraPulseAt=0,ultraLastPulseUs=0,ultraLastOkAt=0,ultraTimeoutCount=0;
uint32_t ultraNoRiseCount=0,ultraNoFallCount=0,ultraStuckHighCount=0,ultraInvalidPulseCount=0;
const char* ultraStatus="awaiting_first_echo";
volatile uint32_t ultraRiseUsIsr=0,ultraFallUsIsr=0;volatile bool ultraAwaiting=false,ultraEchoReady=false;
void IRAM_ATTR ultraEchoIsr(){if(!ultraAwaiting)return;uint32_t t=micros();if(digitalRead(Cfg::ULTRA_ECHO_PIN)){ultraRiseUsIsr=t;}else if(ultraRiseUsIsr){ultraFallUsIsr=t;ultraEchoReady=true;ultraAwaiting=false;}}
void ultraUpdate(){
  const uint32_t now=millis();
  if(ultraEchoReady){
    noInterrupts();
    uint32_t rise=ultraRiseUsIsr,fall=ultraFallUsIsr;
    ultraEchoReady=false;ultraRiseUsIsr=ultraFallUsIsr=0;
    interrupts();
    uint32_t d=fall-rise;ultraLastPulseUs=d;
    if(d>0&&d<=Cfg::ULTRA_TIMEOUT_US){
      ultraCm=d*0.01715f;
      ultraLastOkAt=now;ultraStatus="ok";
      ultraWarning=ff(ultraCm)&&ultraCm>1&&ultraCm<=Cfg::ULTRA_WARN_CM;
      ultraStop=ff(ultraCm)&&ultraCm>1&&ultraCm<=Cfg::ULTRA_STOP_CM;
    }else{
      ultraCm=NAN;ultraWarning=false;ultraStop=false;ultraStatus="invalid_pulse";
      ultraInvalidPulseCount++;
    }
    hazardAlert();
  }
  if(ultraAwaiting){
    if((uint32_t)(micros()-ultraPulseAt)>Cfg::ULTRA_TIMEOUT_US){
      noInterrupts();
      bool sawRise=ultraRiseUsIsr!=0;
      ultraAwaiting=false;ultraEchoReady=false;ultraRiseUsIsr=ultraFallUsIsr=0;
      interrupts();
      ultraCm=NAN;ultraLastPulseUs=0;ultraWarning=false;ultraStop=false;
      ultraTimeoutCount++;
      if(sawRise){ultraNoFallCount++;ultraStatus="no_falling_edge";}
      else{ultraNoRiseCount++;ultraStatus="no_rising_edge";}
      hazardAlert();
    }
    return;
  }
  if(now-ultraAt<Cfg::ULTRA_PERIOD)return;
  ultraAt=now;
  // Report held-high ECHO as an electrical diagnostic, never as a valid distance.
  if(digitalRead(Cfg::ULTRA_ECHO_PIN)==HIGH){
    ultraCm=NAN;ultraLastPulseUs=0;ultraWarning=false;ultraStop=false;
    ultraStuckHighCount++;ultraTimeoutCount++;ultraStatus="echo_high_before_trigger";
    hazardAlert();return;
  }
  digitalWrite(Cfg::ULTRA_TRIG_PIN,LOW);delayMicroseconds(2);
  noInterrupts();ultraRiseUsIsr=ultraFallUsIsr=0;ultraEchoReady=false;ultraAwaiting=true;interrupts();
  ultraPulseAt=micros();
  digitalWrite(Cfg::ULTRA_TRIG_PIN,HIGH);delayMicroseconds(10);
  digitalWrite(Cfg::ULTRA_TRIG_PIN,LOW);
}

/* measurement */
enum Stage:uint8_t{IDLE,WARM,MEASURE,READY,ERROR};enum MeasureError:uint8_t{ME_NONE,ME_POWER_OFF,ME_ADS_MISSING,ME_POWER_CHANGED,ME_NO_VALID_SAMPLES};Stage stage=IDLE;MeasureError measureError=ME_NONE;bool measurePower=false;uint32_t stageAt=0,sampleAt=0;uint16_t measureElapsedSec=0;
uint32_t rawSum[Cfg::MQ_N]={};float adcSum[Cfg::MQ_N]={},aoSum[Cfg::MQ_N]={},rsSum[Cfg::MQ_N]={};uint16_t cnt[Cfg::MQ_N]={};
void clearSums(){memset(rawSum,0,sizeof(rawSum));memset(adcSum,0,sizeof(adcSum));memset(aoSum,0,sizeof(aoSum));memset(rsSum,0,sizeof(rsSum));memset(cnt,0,sizeof(cnt));}
void resetMeasurement(){clearSums();measurePower=false;stageAt=sampleAt=0;measureElapsedSec=0;measureError=ME_NONE;stage=IDLE;}
void collect(){if(!measurePower)return;for(uint8_t i=0;i<Cfg::MQ_N;i++){if(!mq[i].ok)continue;rawSum[i]+=mq[i].raw;adcSum[i]+=mq[i].adc;aoSum[i]+=mq[i].ao;rsSum[i]+=mq[i].rs;cnt[i]++;}}
uint8_t validChannels(){uint8_t n=0;for(uint8_t i=0;i<Cfg::MQ_N;i++)if(cnt[i])n++;return n;}
const char* measureErrorName(){switch(measureError){case ME_POWER_OFF:return"mq_power_off";case ME_ADS_MISSING:return"ads_missing";case ME_POWER_CHANGED:return"power_changed";case ME_NO_VALID_SAMPLES:return"no_valid_samples";default:return"";}}
void finishMeasure(){measureElapsedSec=settings.measure;if(validChannels()){measureError=ME_NONE;stage=READY;}else{measureError=ME_NO_VALID_SAMPLES;stage=ERROR;}}
void measureUpdate(){if(stage!=MEASURE)return;uint32_t n=millis();measureElapsedSec=(uint16_t)min((uint32_t)settings.measure,(n-stageAt)/1000UL);if(mqPowerOn!=measurePower){measureError=ME_POWER_CHANGED;stage=ERROR;return;}if(n-sampleAt>=Cfg::SAMPLE_PERIOD){sampleAt=n;collect();}if(n-stageAt>=(uint32_t)settings.measure*1000UL)finishMeasure();}

/* link health: diagnostic only; it never changes drive, measurement or MQ power */
bool linkArmed=false;uint32_t linkAt=0;

/* JSON */
String num(float v,uint8_t d=2){return ff(v)?String(v,(unsigned)d):"null";}const char* bj(bool v){return v?"true":"false";}
void loadSettings(){nv.begin(Cfg::NVS,false);settings.cam=nv.getString("camIP",Cfg::CAM);settings.speed=constrain(nv.getInt("speed",100),20,100);uint32_t w=nv.getUInt("warmup",Cfg::OPERATIONAL_WARMUP_SEC),m=nv.getUInt("measure",Cfg::ANALYSIS_WINDOW_SEC);settings.warm=(uint16_t)min(w,3600UL);settings.measure=(uint16_t)constrain(m,5UL,600UL);}
String envJson(){uint32_t age=envLastOkAt?millis()-envLastOkAt:0;String j="{\"ok\":"+String(bj(envOk()))+",\"t\":"+num(temp,2)+",\"h\":"+num(hum,2)+",\"p\":"+num(press,1);j+=",\"type\":"+String(envType)+",\"addr\":"+String(envAddr)+",\"chip_id\":"+String(envId)+",\"status\":\""+String(envStatus)+"\",\"ack_0x76\":"+String(bj(envAck76))+",\"ack_0x77\":"+String(bj(envAck77))+",\"last_addr\":"+String(envLastAddr)+",\"last_chip_id\":"+String(envLastId)+",\"read_failures\":"+String(envReadFails)+",\"last_ok_age_ms\":"+String(age)+",\"reprobe_ms\":"+String(ENV_REPROBE_MS)+",\"total_read_errors\":"+String(envReadErrorTotal)+",\"bus_recoveries\":"+String(envBusRecoveries)+",\"i2c_timeout_ms\":25"+",\"bus\":{\"sda\":"+String(Cfg::ENV_SDA)+",\"scl\":"+String(Cfg::ENV_SCL)+",\"hz\":"+String(Cfg::ENV_I2C_HZ)+"}}";return j;}
String acquisitionJson(){return "{\"model\":\"ADS1115\",\"rate_sps\":"+String(Cfg::ADS_RATE_SPS)+",\"gain\":\"GAIN_TWOTHIRDS\",\"full_scale_v\":"+num(Cfg::ADS_FULL_SCALE_V,3)+",\"supply_v\":"+num(Cfg::ADS_SUPPLY_V,3)+",\"conversions_per_channel\":"+String(Cfg::ADC_SAMPLES)+",\"scan_period_ms\":"+String(Cfg::MQ_PERIOD)+",\"channels\":"+String(Cfg::MQ_N)+",\"sequential\":true,\"bus_sda\":"+String(Cfg::SDA)+",\"bus_scl\":"+String(Cfg::SCL)+"}";}
String mqJson(bool avg){bool powered=avg?measurePower:mqPowerOn;String j="{";uint32_t nowUs=micros();for(uint8_t i=0;i<Cfg::MQ_N;i++){if(i)j+=",";const auto&c=Cfg::mq[i];j+="\""+String(c.name)+"\":{";if(avg){if(!cnt[i])j+="\"ok\":false";else{float n=cnt[i],rs=rsSum[i]/n;j+="\"ok\":true,\"raw\":"+num(rawSum[i]/n,1)+",\"adc\":"+num(adcSum[i]/n,3)+",\"ao\":"+num(aoSum[i]/n,3)+",\"rs\":"+num(rs,3)+",\"ratio\":"+num(rs/c.r0,3);}}else{j+="\"ok\":"+String(bj(mq[i].ok))+",\"raw\":"+String(mq[i].raw)+",\"adc\":"+num(mq[i].adc,3)+",\"ao\":"+num(mq[i].ao,3)+",\"rs\":"+num(mq[i].rs,3)+",\"ratio\":"+num(mq[i].ratio,3);if(mq[i].sampleUs)j+=",\"sample_us\":"+String(mq[i].sampleUs)+",\"scan_cycle\":"+String(mq[i].scanCycle)+",\"channel_offset_us\":"+String(mq[i].channelOffsetUs)+",\"age_us\":"+String(nowUs-mq[i].sampleUs);}j+=",\"powered\":"+String(bj(powered))+",\"relay\":1,\"row\":1,\"ads\":"+String(c.ads)+",\"ch\":"+String(c.ch)+",\"r0\":"+num(c.r0,3)+",\"r0_calibrated\":true,\"r0_source\":\""+String(Cfg::R0_SOURCE)+"\",\"r0_reference_validated\":"+String(bj(Cfg::R0_REFERENCE_GAS_VALIDATED))+",\"r0_session\":\""+String(Cfg::R0_SESSION)+"\",\"r0_samples\":"+String(Cfg::R0_SAMPLES)+",\"r0_environment_compensated\":"+String(bj(Cfg::R0_ENVIRONMENT_COMPENSATED))+"}";}return j+"}";}
String driveJson(){bool hazard=irWarning||ultraStop,blocked=safetyAutoStopEnabled()&&hazard;return "{\"driver\":\"L298N\",\"left\":"+String(ri(curL))+",\"right\":"+String(ri(curR))+",\"command_age_ms\":"+String(millis()-cmdAt)+",\"stale_stop\":"+String(bj(staleStop))+",\"safety_auto_stop\":"+String(bj(safetyAutoStopEnabled()))+",\"forward_hazard\":"+String(bj(hazard))+",\"forward_blocked\":"+String(bj(blocked))+",\"forward_block_reason\":\""+String(blocked?(irWarning?"cliff":ultraStop?"collision":""):"")+"\""+",\"pins\":{\"l_en\":"+String(Cfg::DRIVE_L_EN)+",\"l_in1\":"+String(Cfg::DRIVE_L_IN1)+",\"l_in2\":"+String(Cfg::DRIVE_L_IN2)+",\"r_en\":"+String(Cfg::DRIVE_R_EN)+",\"r_in1\":"+String(Cfg::DRIVE_R_IN1)+",\"r_in2\":"+String(Cfg::DRIVE_R_IN2)+"}}";}
String healthJson(){return "{\"battery\":{\"v\":"+num(batteryV,2)+",\"raw\":"+String(batteryRaw)+",\"estimated\":true},\"internal_temp\":{\"installed\":false,\"c\":null,\"gpio\":null},\"proximity\":{\"cm\":"+num(ultraCm,1)+",\"valid\":"+String(bj(ff(ultraCm)&&ultraCm>0))+",\"pulse_us\":"+String(ultraLastPulseUs)+",\"timeouts\":"+String(ultraTimeoutCount)+",\"status\":\""+String(ultraStatus)+"\""+",\"echo_level\":"+String(digitalRead(Cfg::ULTRA_ECHO_PIN))+",\"no_rise\":"+String(ultraNoRiseCount)+",\"no_fall\":"+String(ultraNoFallCount)+",\"stuck_high\":"+String(ultraStuckHighCount)+",\"invalid_pulse\":"+String(ultraInvalidPulseCount)+",\"last_ok_age_ms\":"+(ultraLastOkAt?String(millis()-ultraLastOkAt):String("null"))+",\"warning\":"+String(bj(ultraWarning))+",\"stop_zone\":"+String(bj(ultraStop))+",\"auto_stop_enabled\":"+String(bj(safetyAutoStopEnabled()))+",\"forward_blocked\":"+String(bj(safetyAutoStopEnabled()&&ultraStop))+",\"warning_cm\":"+num(Cfg::ULTRA_WARN_CM,1)+",\"stop_cm\":"+num(Cfg::ULTRA_STOP_CM,1)+",\"trig\":"+String(Cfg::ULTRA_TRIG_PIN)+",\"echo\":"+String(Cfg::ULTRA_ECHO_PIN)+"},\"energy_basis\":{\"uptime_ms\":"+String(millis())+",\"drive_eq_ms\":"+String(driveEqMs)+",\"mq_on_ms\":"+String(mqOnTotalMs)+"}}";}
String lightJson(){return "{\"installed\":true,\"gpio\":"+String(Cfg::LDR_PIN)+",\"mode\":\"lm393_threshold\",\"level\":"+String(ldrLevel)+",\"quantitative_lux\":false,\"lux\":null}";}
String irJson(){return "{\"status\":\""+String(irGroundDetected?"ground":"cliff")+"\",\"warning\":"+String(bj(irWarning))+",\"cliff_warning\":"+String(bj(irWarning))+",\"ground_detected\":"+String(bj(irGroundDetected))+",\"raw_level\":"+String(irLevel)+",\"level\":"+String(irLevel)+",\"ground_active_level\":"+String(Cfg::IR_GROUND_ACTIVE)+",\"gpio\":"+String(Cfg::IR_PIN)+",\"reference_height_cm\":"+num(Cfg::IR_REFERENCE_HEIGHT_CM,1)+",\"assert_samples\":"+String(Cfg::IR_CLIFF_ASSERT_SAMPLES)+",\"clear_samples\":"+String(Cfg::IR_CLIFF_CLEAR_SAMPLES)+"}";}
const char* measurementStateName(){switch(stage){case WARM:return"warmup";case MEASURE:return"measuring";case READY:return"ready";case ERROR:return"error";default:return"idle";}}
String measurementJson(){uint16_t remaining=stage==MEASURE&&settings.measure>measureElapsedSec?settings.measure-measureElapsedSec:0;return "{\"state\":\""+String(measurementStateName())+"\",\"mask\":"+String(measurePower?1:0)+",\"power\":"+String(bj(measurePower))+",\"manual_power\":true,\"manual_warmup\":true,\"duration_s\":"+String(settings.measure)+",\"elapsed_s\":"+String(measureElapsedSec)+",\"remaining_s\":"+String(remaining)+",\"error\":\""+String(measureErrorName())+"\",\"valid_channels\":"+String(validChannels())+",\"mq\":"+mqJson(true)+"}";}
}

void hwBegin(){loadSettings();Alerts::begin();pinMode(Cfg::RELAY_MQ,OUTPUT);relayRaw(false);pinMode(Cfg::IR_PIN,INPUT);pinMode(Cfg::LDR_PIN,INPUT);pinMode(Cfg::SOUND_PIN,INPUT);pinMode(Cfg::BATTERY_PIN,INPUT);pinMode(Cfg::ULTRA_TRIG_PIN,OUTPUT);digitalWrite(Cfg::ULTRA_TRIG_PIN,LOW);pinMode(Cfg::ULTRA_ECHO_PIN,INPUT);attachInterrupt(digitalPinToInterrupt(Cfg::ULTRA_ECHO_PIN),ultraEchoIsr,CHANGE);analogReadResolution(12);analogSetPinAttenuation(Cfg::SOUND_PIN,ADC_11db);analogSetPinAttenuation(Cfg::BATTERY_PIN,ADC_11db);pinMode(Cfg::DRIVE_L_EN,OUTPUT);pinMode(Cfg::DRIVE_L_IN1,OUTPUT);pinMode(Cfg::DRIVE_L_IN2,OUTPUT);pinMode(Cfg::DRIVE_R_EN,OUTPUT);pinMode(Cfg::DRIVE_R_IN1,OUTPUT);pinMode(Cfg::DRIVE_R_IN2,OUTPUT);digitalWrite(Cfg::DRIVE_L_IN1,LOW);digitalWrite(Cfg::DRIVE_L_IN2,LOW);digitalWrite(Cfg::DRIVE_R_IN1,LOW);digitalWrite(Cfg::DRIVE_R_IN2,LOW);analogWrite(Cfg::DRIVE_L_EN,0);analogWrite(Cfg::DRIVE_R_EN,0);cmdAt=energyAt=millis();driveWrite();Wire.begin(Cfg::SDA,Cfg::SCL);Wire.setClock(Cfg::ADS_I2C_HZ);EnvWire.begin(Cfg::ENV_SDA,Cfg::ENV_SCL);EnvWire.setClock(Cfg::ENV_I2C_HZ);EnvWire.setTimeOut(25);adsBegin(0);adsBegin(1);adsHealthAt=millis();envBegin();}
void hwUpdate(){driveUpdate();energyUpdate();adsHealthUpdate();mqUpdate();envUpdate();soundUpdate();irUpdate();ldrUpdate();batteryUpdate();ultraUpdate();measureUpdate();Alerts::update();}
void driveSet(int l,int r){
    float requestedL=lim(l),requestedR=lim(r);
    // IR/ultrasonic warnings remain active, but automatic motion blocking is disabled in RC2.
    // E-STOP, explicit stop and command-loss watchdog remain independent and active.
    bool forwardRequested=(requestedL+requestedR)>0.1f;
    bool forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop);
    if(forwardBlocked&&forwardRequested){curL=curR=0;}else{curL=requestedL;curR=requestedR;}
    cmdAt=millis();staleStop=false;driveWrite();
}
void motionStop(){motionStopInternal();}void emergencyStop(){motionStopInternal();resetMeasurement();relayRaw(false);Alerts::emergencyStop();}
bool relaySetMask(uint8_t on){if(on>1)return false;if(stage==MEASURE||stage==WARM)resetMeasurement();relayRaw(on);return true;}void sensorPowerOff(){resetMeasurement();relayRaw(false);}
bool measureStart(){clearSums();measureError=ME_NONE;measureElapsedSec=0;measurePower=mqPowerOn;stageAt=sampleAt=millis();if(!measurePower){measureError=ME_POWER_OFF;stage=ERROR;return false;}if(!ads1ok&&!ads2ok){measureError=ME_ADS_MISSING;stage=ERROR;return false;}stage=MEASURE;return true;}void measureCancel(){resetMeasurement();Alerts::setGas(0);}
bool alertSet(uint8_t s,uint32_t ttl){if(s>2)return false;Alerts::setGas(s,ttl);return true;}void alertTest(){Alerts::test();}
void linkHeartbeat(){linkArmed=true;linkAt=millis();Alerts::setLink(false);}void linkSetArmed(bool a){linkArmed=a;linkAt=a?millis():0;Alerts::setLink(false);}
void saveSettings(){nv.putString("camIP",settings.cam);nv.putInt("speed",settings.speed);nv.putUInt("warmup",settings.warm);nv.putUInt("measure",settings.measure);}
String settingsJson(){return "{\"cam\":\""+settings.cam+"\",\"speed\":"+String(settings.speed)+",\"warm\":"+String(settings.warm)+",\"measure\":"+String(settings.measure)+",\"drive_driver\":\"L298N\",\"warm_manual\":true,\"mq_power_manual\":true}";}
String dataJson(){String j="{\"env\":"+envJson()+",\"device\":"+healthJson()+",\"sound\":{\"rms\":"+num(soundRms,3)+",\"rms_v\":"+num(soundRms,3)+",\"p2p\":"+num(soundP2P,3)+",\"p2p_v\":"+num(soundP2P,3)+",\"db_spl\":null,\"db_estimated\":false,\"calibrated_db_spl\":false,\"gain_reference\":null,\"gain_state\":\"trimmer_endpoint_fixed_unquantified\"},\"ir\":"+irJson()+",\"ldr\":"+lightJson()+",\"alert\":"+Alerts::json()+",\"relay\":{\"mask\":"+String(mqPowerOn?1:0)+",\"on\":"+String(bj(mqPowerOn))+",\"gpio\":"+String(Cfg::RELAY_MQ)+",\"power_ms\":"+String(mqPowerMs())+",\"channels\":1},\"adc\":{\"ads1\":"+String(bj(ads1ok))+",\"ads2\":"+String(bj(ads2ok))+",\"acquisition\":"+acquisitionJson()+"},\"drive\":"+driveJson()+",\"mq\":"+mqJson(false)+",\"measurement\":"+measurementJson()+"}";return j;}
String diagJson(){String j="{\"fw\":\""+String(Cfg::FW)+"\",\"build_revision\":\""+String(Cfg::ENGINEERING_REVISION)+"\",\"api\":"+String(Cfg::API)+",\"hardware_profile\":\""+String(Cfg::HW_PROFILE)+"\",\"heap\":"+String(ESP.getFreeHeap())+",\"clients\":"+String(WiFi.softAPgetStationNum())+",\"device\":"+healthJson()+",\"drive\":"+driveJson()+",\"relay\":{\"mask\":"+String(mqPowerOn?1:0)+",\"on\":"+String(bj(mqPowerOn))+",\"gpio\":"+String(Cfg::RELAY_MQ)+",\"power_ms\":"+String(mqPowerMs())+",\"channels\":1},\"adc\":{\"ads1\":"+String(bj(ads1ok))+",\"ads2\":"+String(bj(ads2ok))+",\"acquisition\":"+acquisitionJson()+"},\"link\":{\"armed\":"+String(bj(linkArmed))+",\"heartbeat_age_ms\":"+String(linkArmed?millis()-linkAt:0)+"},\"env\":"+envJson()+",\"envType\":"+String(envType)+",\"envAddr\":"+String(envAddr)+",\"envId\":"+String(envId)+",\"envStatus\":\""+String(envStatus)+"\",\"envAck76\":"+String(bj(envAck76))+",\"envAck77\":"+String(bj(envAck77))+",\"envReadFailures\":"+String(envReadFails)+",\"ldr\":"+lightJson()+",\"alert\":"+Alerts::json()+",\"ir\":"+irJson()+",\"mqPowerManual\":true,\"warmupManual\":true}";return j;}
