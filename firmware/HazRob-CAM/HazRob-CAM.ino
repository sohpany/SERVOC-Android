#include <WiFi.h>
#include <WebServer.h>
#include "esp_camera.h"
#include "img_converters.h"
#include "esp32-hal-psram.h"

constexpr char SSID[]="HazRob";
constexpr uint8_t FLASH_PIN=4;
constexpr size_t JPG_CAP=160*1024;
constexpr uint8_t JPG_QUALITY=35;

// AI-Thinker ESP32-CAM pins
constexpr int PWDN=32,RESET=-1,XCLK=0,SIOD=26,SIOC=27;
constexpr int Y9=35,Y8=34,Y7=39,Y6=36,Y5=21,Y4=19,Y3=18,Y2=5;
constexpr int VSYNC=25,HREF=23,PCLK=22;

IPAddress localIP(192,168,4,2),gateway(192,168,4,1),subnet(255,255,255,0);
WebServer web(80);
WiFiServer mjpeg(81);

uint8_t *jpgBuf=nullptr;
volatile size_t jpgLen=0;
volatile uint32_t fps100=0,frameCounter=0;
uint32_t fpsAt=0,lastFrames=0,wifiAt=0;
enum FlashMode:uint8_t{F_OFF,F_AUTO,F_ON};
volatile FlashMode flashMode=F_OFF;

bool jpgOut(void*,size_t index,const void* data,size_t len){
  if(!jpgBuf||index+len>JPG_CAP)return false;
  memcpy(jpgBuf+index,data,len);
  jpgLen=index+len;
  return true;
}

bool writeAll(WiFiClient &c,const uint8_t* p,size_t n){
  uint32_t stalled=millis();
  while(n&&c.connected()){
    size_t w=c.write(p,n);
    if(w){p+=w;n-=w;stalled=millis();}
    else{
      if(millis()-stalled>2000)return false;
      delay(1);
    }
  }
  return n==0;
}

bool writeAll(WiFiClient &c,const char* s){return writeAll(c,(const uint8_t*)s,strlen(s));}

void discardRequest(WiFiClient &c){
  uint8_t state=0;
  uint32_t limit=millis()+1200;
  while(c.connected()&&(int32_t)(limit-millis())>0){
    while(c.available()){
      char ch=(char)c.read();
      state=(state==0&&ch=='\r')?1:(state==1&&ch=='\n')?2:(state==2&&ch=='\r')?3:(state==3&&ch=='\n')?4:0;
      if(state==4)return;
    }
    delay(1);
  }
}

void applyFlash(){digitalWrite(FLASH_PIN,flashMode==F_ON?HIGH:LOW);}
const char* flashName(){return flashMode==F_ON?"on":flashMode==F_AUTO?"auto":"off";}

void updateFps(){
  uint32_t now=millis();
  if(!fpsAt){fpsAt=now;lastFrames=frameCounter;return;}
  uint32_t dt=now-fpsAt;
  if(dt<1000)return;
  uint32_t df=frameCounter-lastFrames;
  fps100=(uint32_t)((uint64_t)df*100000ULL/dt);
  fpsAt=now;lastFrames=frameCounter;
}

void streamTask(void*){
  for(;;){
    WiFiClient c=mjpeg.available();
    if(!c){vTaskDelay(pdMS_TO_TICKS(2));continue;}
    c.setNoDelay(true);
    discardRequest(c);

    if(!writeAll(c,"HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nCache-Control: no-cache, no-store, must-revalidate\r\nPragma: no-cache\r\nConnection: close\r\nContent-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n")){c.stop();continue;}

    while(c.connected()&&WiFi.status()==WL_CONNECTED){
      camera_fb_t *fb=esp_camera_fb_get();
      if(!fb){vTaskDelay(pdMS_TO_TICKS(5));continue;}

      jpgLen=0;
      bool ok=frame2jpg_cb(fb,JPG_QUALITY,jpgOut,nullptr);
      esp_camera_fb_return(fb);
      if(!ok||!jpgLen){vTaskDelay(pdMS_TO_TICKS(2));continue;}

      char h[128];
      int hn=snprintf(h,sizeof(h),"--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",(unsigned)jpgLen);
      if(hn<=0||!writeAll(c,(const uint8_t*)h,(size_t)hn)||!writeAll(c,jpgBuf,jpgLen)||!writeAll(c,"\r\n"))break;

      frameCounter++;
      updateFps();
      vTaskDelay(pdMS_TO_TICKS(1));
    }
    c.stop();
  }
}

void cameraBegin(){
  camera_config_t c={};
  c.ledc_channel=LEDC_CHANNEL_0;c.ledc_timer=LEDC_TIMER_0;
  c.pin_d0=Y2;c.pin_d1=Y3;c.pin_d2=Y4;c.pin_d3=Y5;c.pin_d4=Y6;c.pin_d5=Y7;c.pin_d6=Y8;c.pin_d7=Y9;
  c.pin_xclk=XCLK;c.pin_pclk=PCLK;c.pin_vsync=VSYNC;c.pin_href=HREF;c.pin_sccb_sda=SIOD;c.pin_sccb_scl=SIOC;
  c.pin_pwdn=PWDN;c.pin_reset=RESET;c.xclk_freq_hz=20000000;
  c.pixel_format=PIXFORMAT_RGB565;c.frame_size=FRAMESIZE_QVGA;c.jpeg_quality=12;
  c.fb_count=1;c.grab_mode=CAMERA_GRAB_WHEN_EMPTY;c.fb_location=CAMERA_FB_IN_PSRAM;

  if(esp_camera_init(&c)!=ESP_OK)ESP.restart();
  sensor_t *s=esp_camera_sensor_get();
  if(s)s->set_framesize(s,FRAMESIZE_QVGA);
}

void wifiBegin(){
  WiFi.persistent(false);WiFi.mode(WIFI_STA);WiFi.setSleep(false);
  WiFi.config(localIP,gateway,subnet);WiFi.begin(SSID);
}

void setup(){
  pinMode(FLASH_PIN,OUTPUT);digitalWrite(FLASH_PIN,LOW);
  if(!psramFound())ESP.restart();
  jpgBuf=(uint8_t*)ps_malloc(JPG_CAP);
  if(!jpgBuf)ESP.restart();

  cameraBegin();
  wifiBegin();

  web.on("/",[]{web.send(200,"text/plain","HazRob-CAM");});
  web.on("/status",[]{
    sensor_t *s=esp_camera_sensor_get();
    uint16_t pid=s?s->id.PID:0;
    String j="{\"ok\":true,\"ip\":\""+WiFi.localIP().toString()+"\",\"pid\":"+String(pid)+",\"fps\":"+String(fps100/100.0f,2)+",\"rssi\":"+String(WiFi.RSSI())+",\"flash\":\""+flashName()+"\"}";
    web.send(200,"application/json",j);
  });
  web.on("/flash",[]{
    String m=web.arg("mode");
    if(m=="off")flashMode=F_OFF;
    else if(m=="auto")flashMode=F_AUTO;
    else if(m=="on")flashMode=F_ON;
    else return web.send(400,"text/plain","BAD");
    applyFlash();web.send(200,"text/plain","OK");
  });
  web.on("/control",[]{
    String v=web.arg("var"),x=web.arg("val");
    if(v=="led_intensity"){flashMode=x.toInt()>0?F_ON:F_OFF;applyFlash();return web.send(200,"text/plain","OK");}
    if(v=="flash"){flashMode=x.toInt()?F_ON:F_OFF;applyFlash();return web.send(200,"text/plain","OK");}
    web.send(400,"text/plain","BAD");
  });
  web.onNotFound([]{web.send(404,"text/plain","NOT FOUND");});

  web.begin();mjpeg.begin();
  xTaskCreatePinnedToCore(streamTask,"mjpeg",6144,nullptr,1,nullptr,1);
}

void loop(){
  web.handleClient();
  updateFps();
  if(WiFi.status()!=WL_CONNECTED&&millis()-wifiAt>3000){wifiAt=millis();WiFi.disconnect();WiFi.config(localIP,gateway,subnet);WiFi.begin(SSID);}
  delay(2);
}
