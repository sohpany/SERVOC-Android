#include <WiFi.h>
#include <WebServer.h>
#include "esp_camera.h"
#include "img_converters.h"
#include "esp32-hal-psram.h"

// HR-0101-055 / Baseline A
// Local camera authority only: HazRob STA -> 192.168.4.2, HTTP :80, MJPEG :81.
// No Render/WSS/bootstrap/TLS runtime is started in this diagnostic baseline.
constexpr char SSID[]="HazRob",FW[]="HazRob-CAM 0.10.1 Baseline-A";
constexpr uint8_t LED=4,JQ=35;
constexpr size_t JPG_CAP=160*1024;
constexpr int PWDN=32,RST=-1,XCLK=0,SIOD=26,SIOC=27,Y9=35,Y8=34,Y7=39,Y6=36,Y5=21,Y4=19,Y3=18,Y2=5,VS=25,HR=23,PC=22;

IPAddress ip(192,168,4,2),gw(192,168,4,1),sn(255,255,255,0);
WebServer web(80);
WiFiServer mjpeg(81);
SemaphoreHandle_t camMx;
uint8_t* jpg=nullptr;
volatile size_t lastJpg=0;
volatile uint32_t fps100=0,frames=0,encUs=0,capErr=0,encErr=0;
volatile uint8_t clients=0;
uint32_t fpsAt=0,lastFrames=0,wifiAt=0;
bool camOK=false,psOK=false;
esp_err_t camErr=ESP_OK;
uint16_t pid=0;
uint8_t flashMode=0;

struct Sink{uint8_t* b;size_t cap,len;};
size_t outJpg(void* a,size_t i,const void* d,size_t n){auto*s=(Sink*)a;if(!s||i+n>s->cap)return 0;memcpy(s->b+i,d,n);return s->len=i+n,n;}
bool jpegOK(const uint8_t*p,size_t n){return p&&n>3&&p[0]==0xFF&&p[1]==0xD8&&p[n-2]==0xFF&&p[n-1]==0xD9;}
void fpsTick(){uint32_t n=millis();if(!fpsAt){fpsAt=n;lastFrames=frames;return;}uint32_t dt=n-fpsAt;if(dt<1000)return;fps100=(uint32_t)((uint64_t)(frames-lastFrames)*100000ULL/dt);fpsAt=n;lastFrames=frames;}

bool captureJpeg(uint8_t*out,size_t cap,size_t&len){
  len=0;
  if(!camOK||!out||cap<4096||xSemaphoreTake(camMx,pdMS_TO_TICKS(1200))!=pdTRUE)return false;
  camera_fb_t*f=esp_camera_fb_get();
  if(!f){capErr++;xSemaphoreGive(camMx);return false;}
  Sink s{out,cap,0};
  uint32_t t=micros();
  bool ok=frame2jpg_cb(f,JQ,outJpg,&s);
  encUs=micros()-t;
  esp_camera_fb_return(f);
  ok=ok&&jpegOK(out,s.len);
  if(ok){len=lastJpg=s.len;frames++;fpsTick();}else encErr++;
  xSemaphoreGive(camMx);
  return ok;
}

bool wr(WiFiClient&c,const uint8_t*p,size_t n){uint32_t t=millis();while(n&&c.connected()){size_t w=c.write(p,n);if(w){p+=w;n-=w;t=millis();}else if(millis()-t>2000)return false;else delay(1);}return !n;}
bool wr(WiFiClient&c,const char*s){return wr(c,(const uint8_t*)s,strlen(s));}
void drain(WiFiClient&c){uint8_t s=0;uint32_t t=millis()+1200;while(c.connected()&&(int32_t)(t-millis())>0){while(c.available()){char x=c.read();s=s==0?(x=='\r'):s==1?(x=='\n'?2:0):s==2?(x=='\r'?3:0):(x=='\n'?4:0);if(s==4)return;}delay(1);}}
const char* sensor(){return pid==0x2145?"GC2145":pid==0x26?"OV2640":pid?"OTHER":"UNKNOWN";}
void flashSet(String x){x.toLowerCase();flashMode=(x=="on"||x=="1"||x=="255")?2:x=="auto"?1:0;digitalWrite(LED,flashMode==2);}
const char* flashName(){return flashMode==2?"on":flashMode==1?"auto":"off";}

bool cameraBegin(){
  psOK=psramFound();
  if(!psOK)return camErr=ESP_ERR_NO_MEM,false;
  jpg=(uint8_t*)ps_malloc(JPG_CAP);
  if(!jpg)return camErr=ESP_ERR_NO_MEM,false;
  camera_config_t c={};
  c.ledc_channel=LEDC_CHANNEL_0;c.ledc_timer=LEDC_TIMER_0;
  c.pin_d0=Y2;c.pin_d1=Y3;c.pin_d2=Y4;c.pin_d3=Y5;c.pin_d4=Y6;c.pin_d5=Y7;c.pin_d6=Y8;c.pin_d7=Y9;
  c.pin_xclk=XCLK;c.pin_pclk=PC;c.pin_vsync=VS;c.pin_href=HR;c.pin_sccb_sda=SIOD;c.pin_sccb_scl=SIOC;c.pin_pwdn=PWDN;c.pin_reset=RST;
  c.xclk_freq_hz=20000000;
  c.pixel_format=PIXFORMAT_RGB565;
  c.frame_size=FRAMESIZE_QVGA;
  c.jpeg_quality=12;
  c.fb_count=1;
  c.grab_mode=CAMERA_GRAB_WHEN_EMPTY;
  c.fb_location=CAMERA_FB_IN_PSRAM;
  camErr=esp_camera_init(&c);
  if(camErr!=ESP_OK)return false;
  if(auto*s=esp_camera_sensor_get()){pid=s->id.PID;s->set_framesize(s,FRAMESIZE_QVGA);}
  return true;
}

void streamTask(void*){
  for(;;){
    WiFiClient c=mjpeg.accept();
    if(!c){vTaskDelay(pdMS_TO_TICKS(2));continue;}
    clients=1;
    c.setNoDelay(true);
    drain(c);
    if(!camOK){wr(c,"HTTP/1.1 503 Service Unavailable\r\nConnection: close\r\n\r\nCAMERA FAIL\n");c.stop();clients=0;continue;}
    if(!wr(c,"HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nCache-Control: no-cache, no-store, must-revalidate\r\nPragma: no-cache\r\nConnection: close\r\nContent-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n")){c.stop();clients=0;continue;}
    while(c.connected()&&WiFi.status()==WL_CONNECTED&&camOK){
      size_t n=0;
      if(!captureJpeg(jpg,JPG_CAP,n)){vTaskDelay(pdMS_TO_TICKS(5));continue;}
      char h[96];
      int hn=snprintf(h,sizeof(h),"--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",(unsigned)n);
      if(hn<1||!wr(c,(uint8_t*)h,(size_t)hn)||!wr(c,jpg,n)||!wr(c,"\r\n"))break;
      vTaskDelay(pdMS_TO_TICKS(1));
    }
    c.stop();clients=0;
  }
}

void wifiConnect(){
  WiFi.disconnect(false,false);
  delay(20);
  WiFi.config(ip,gw,sn);
  WiFi.begin(SSID);
  wifiAt=millis();
}

void status(){
  String e=String("0x")+String((uint32_t)camErr,HEX);e.toUpperCase();
  String j="{\"ok\":true,\"baseline\":\"A\",\"camera_ok\":"+String(camOK?"true":"false")+",\"camera_error\":\""+e+"\",\"psram\":"+(psOK?"true":"false")+",\"fw\":\""+FW+"\",\"ip\":\""+WiFi.localIP().toString()+"\",\"ssid\":\""+WiFi.SSID()+"\",\"pid\":"+pid+",\"sensor\":\""+sensor()+"\",\"width\":320,\"height\":240,\"capture_format\":\"RGB565\",\"jpeg_encoder\":\"software_frame2jpg\",\"stream_format\":\"MJPEG\",\"remote_video\":false,\"jpeg_quality\":"+JQ+",\"fps\":"+String(fps100/100.0f,2)+",\"frames\":"+frames+",\"encode_ms\":"+String(encUs/1000.0f,2)+",\"jpeg_bytes\":"+(uint32_t)lastJpg+",\"capture_errors\":"+capErr+",\"encode_errors\":"+encErr+",\"stream_clients\":"+clients+",\"rssi\":"+WiFi.RSSI()+",\"flash_gpio\":4,\"flash\":\""+flashName()+"\"}";
  web.sendHeader("Access-Control-Allow-Origin","*");
  web.send(200,"application/json",j);
}

void setup(){
  Serial.begin(115200);delay(150);
  pinMode(LED,OUTPUT);digitalWrite(LED,LOW);
  camMx=xSemaphoreCreateMutex();
  camOK=cameraBegin();

  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
  wifiConnect();

  web.on("/",[]{web.send(200,"text/plain",FW);});
  web.on("/status",status);
  web.on("/stream",[]{web.sendHeader("Location","http://"+WiFi.localIP().toString()+":81/stream",true);web.send(302,"text/plain","stream");});
  web.on("/flash",[]{if(web.hasArg("mode"))flashSet(web.arg("mode"));else if(web.hasArg("on"))flashSet(web.arg("on"));else return web.send(400,"text/plain","BAD");web.send(200,"text/plain","OK");});
  web.on("/led",[]{if(web.hasArg("on"))flashSet(web.arg("on"));else if(web.hasArg("state"))flashSet(web.arg("state"));else return web.send(400,"text/plain","BAD");web.send(200,"text/plain","OK");});
  web.on("/control",[]{String v=web.arg("var");if(v!="led_intensity"&&v!="flash"&&v!="led")return web.send(400,"text/plain","BAD");flashSet(web.arg("val"));web.send(200,"text/plain","OK");});
  web.onNotFound([]{web.send(404,"text/plain","NOT FOUND");});
  web.begin();
  mjpeg.begin();
  xTaskCreatePinnedToCore(streamTask,"mjpeg",6144,nullptr,1,nullptr,1);
}

void loop(){
  web.handleClient();
  fpsTick();
  if(WiFi.status()!=WL_CONNECTED&&millis()-wifiAt>4500)wifiConnect();
  delay(2);
}
