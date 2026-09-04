#include <WiFi.h>
#include <WebServer.h>
#include "esp_camera.h"
#include "img_converters.h"
#include "esp32-hal-psram.h"
#include "RemoteVideo.h"

constexpr char WIFI1[]="HazRob";
constexpr char WIFI2[]="Servoc";
constexpr char FW[]="HazRob-CAM 0.9.2-rc1";
constexpr uint8_t FLASH_PIN=4;
constexpr size_t JPG_CAP=160*1024;
constexpr uint8_t JPG_QUALITY=35;

// AI-Thinker ESP32-CAM pin map
constexpr int PWDN=32,RESET=-1,XCLK=0,SIOD=26,SIOC=27;
constexpr int Y9=35,Y8=34,Y7=39,Y6=36,Y5=21,Y4=19,Y3=18,Y2=5;
constexpr int VSYNC=25,HREF=23,PCLK=22;

IPAddress localIP(192,168,4,2),gateway(192,168,4,1),subnet(255,255,255,0),dns1(1,1,1,1),dns2(8,8,8,8);
WebServer web(80);
WiFiServer mjpeg(81);

uint8_t *jpgBuf=nullptr;
SemaphoreHandle_t cameraMutex=nullptr;
volatile size_t lastJpegBytes=0;
volatile uint32_t fps100=0,frameCounter=0,lastEncodeUs=0,captureErrors=0,encodeErrors=0;
volatile uint8_t streamClients=0;
uint32_t fpsAt=0,lastFrames=0,wifiAt=0;
uint8_t wifiChoice=0;
wl_status_t lastWifiStatus=WL_NO_SHIELD;
bool cameraOk=false,psramOk=false;
esp_err_t cameraError=ESP_OK;
uint16_t cameraPid=0;

enum FlashMode:uint8_t{F_OFF,F_AUTO,F_ON};
volatile FlashMode flashMode=F_OFF;

struct JpegSink{uint8_t* buf;size_t cap;size_t len;};
size_t jpgOut(void* arg,size_t index,const void* data,size_t len){JpegSink* s=(JpegSink*)arg;if(!s||!s->buf||index+len>s->cap)return 0;memcpy(s->buf+index,data,len);s->len=index+len;return len;}

void updateFps(){uint32_t now=millis();if(!fpsAt){fpsAt=now;lastFrames=frameCounter;return;}uint32_t dt=now-fpsAt;if(dt<1000)return;uint32_t df=frameCounter-lastFrames;fps100=(uint32_t)((uint64_t)df*100000ULL/dt);fpsAt=now;lastFrames=frameCounter;}

bool captureJpeg(uint8_t* out,size_t cap,size_t& len){
  len=0;if(!cameraOk||!out||cap<4096||!cameraMutex)return false;
  if(xSemaphoreTake(cameraMutex,pdMS_TO_TICKS(1200))!=pdTRUE)return false;
  camera_fb_t* fb=esp_camera_fb_get();if(!fb){captureErrors++;xSemaphoreGive(cameraMutex);return false;}
  JpegSink sink{out,cap,0};uint32_t t0=micros();bool ok=frame2jpg_cb(fb,JPG_QUALITY,jpgOut,&sink);lastEncodeUs=micros()-t0;esp_camera_fb_return(fb);
  if(!ok||!sink.len){encodeErrors++;xSemaphoreGive(cameraMutex);return false;}
  len=sink.len;lastJpegBytes=len;frameCounter++;updateFps();xSemaphoreGive(cameraMutex);return true;
}

bool writeAll(WiFiClient &c,const uint8_t* p,size_t n){uint32_t stalled=millis();while(n&&c.connected()){size_t w=c.write(p,n);if(w){p+=w;n-=w;stalled=millis();}else{if(millis()-stalled>2000)return false;delay(1);}}return n==0;}
bool writeAll(WiFiClient &c,const char* s){return writeAll(c,(const uint8_t*)s,strlen(s));}

void discardRequest(WiFiClient &c){uint8_t state=0;uint32_t limit=millis()+1200;while(c.connected()&&(int32_t)(limit-millis())>0){while(c.available()){char ch=(char)c.read();if(state==0)state=ch=='\r'?1:0;else if(state==1)state=ch=='\n'?2:0;else if(state==2)state=ch=='\r'?3:0;else if(state==3){if(ch=='\n')return;state=0;}}delay(1);}}

void applyFlash(){digitalWrite(FLASH_PIN,flashMode==F_ON?HIGH:LOW);}
const char* flashName(){return flashMode==F_ON?"on":flashMode==F_AUTO?"auto":"off";}
void setFlashText(String m){m.toLowerCase();if(m=="on"||m=="1"||m=="255")flashMode=F_ON;else if(m=="auto")flashMode=F_AUTO;else flashMode=F_OFF;applyFlash();}

const char* sensorName(){if(cameraPid==0x2145)return "GC2145";if(cameraPid==0)return "UNKNOWN";return "OTHER";}

bool cameraBegin(){
  psramOk=psramFound();if(!psramOk){cameraError=ESP_ERR_NO_MEM;Serial.println("CAM: PSRAM missing");return false;}
  jpgBuf=(uint8_t*)ps_malloc(JPG_CAP);if(!jpgBuf){cameraError=ESP_ERR_NO_MEM;Serial.println("CAM: JPEG buffer allocation failed");return false;}
  camera_config_t c={};c.ledc_channel=LEDC_CHANNEL_0;c.ledc_timer=LEDC_TIMER_0;c.pin_d0=Y2;c.pin_d1=Y3;c.pin_d2=Y4;c.pin_d3=Y5;c.pin_d4=Y6;c.pin_d5=Y7;c.pin_d6=Y8;c.pin_d7=Y9;c.pin_xclk=XCLK;c.pin_pclk=PCLK;c.pin_vsync=VSYNC;c.pin_href=HREF;c.pin_sccb_sda=SIOD;c.pin_sccb_scl=SIOC;c.pin_pwdn=PWDN;c.pin_reset=RESET;c.xclk_freq_hz=20000000;c.pixel_format=PIXFORMAT_RGB565;c.frame_size=FRAMESIZE_QVGA;c.jpeg_quality=12;c.fb_count=1;c.grab_mode=CAMERA_GRAB_WHEN_EMPTY;c.fb_location=CAMERA_FB_IN_PSRAM;
  cameraError=esp_camera_init(&c);if(cameraError!=ESP_OK){Serial.printf("CAM: init failed 0x%X\n",(unsigned)cameraError);return false;}
  sensor_t *s=esp_camera_sensor_get();if(s){cameraPid=s->id.PID;s->set_framesize(s,FRAMESIZE_QVGA);Serial.printf("CAM: PID 0x%04X (%s)\n",cameraPid,sensorName());}
  Serial.println("CAM: initialized QVGA RGB565 -> software JPEG");return true;
}

void streamTask(void*){
  for(;;){
    WiFiClient c=mjpeg.accept();if(!c){vTaskDelay(pdMS_TO_TICKS(2));continue;}streamClients=1;c.setNoDelay(true);discardRequest(c);
    if(!cameraOk){writeAll(c,"HTTP/1.1 503 Service Unavailable\r\nConnection: close\r\nContent-Type: text/plain\r\n\r\nCAMERA INIT FAILED\n");c.stop();streamClients=0;continue;}
    if(!writeAll(c,"HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nCache-Control: no-cache, no-store, must-revalidate\r\nPragma: no-cache\r\nConnection: close\r\nContent-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n")){c.stop();streamClients=0;continue;}
    while(c.connected()&&WiFi.status()==WL_CONNECTED&&cameraOk){size_t len=0;if(!captureJpeg(jpgBuf,JPG_CAP,len)){vTaskDelay(pdMS_TO_TICKS(5));continue;}char h[128];int hn=snprintf(h,sizeof(h),"--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",(unsigned)len);if(hn<=0||!writeAll(c,(const uint8_t*)h,(size_t)hn)||!writeAll(c,jpgBuf,len)||!writeAll(c,"\r\n"))break;vTaskDelay(pdMS_TO_TICKS(1));}
    c.stop();streamClients=0;
  }
}

void connectWifi(){const char* ssid=wifiChoice?WIFI2:WIFI1;wifiChoice^=1;WiFi.disconnect(false,false);delay(20);WiFi.config(localIP,gateway,subnet,dns1,dns2);WiFi.begin(ssid);wifiAt=millis();Serial.printf("WiFi: trying %s\n",ssid);}
void wifiBegin(){WiFi.persistent(false);WiFi.mode(WIFI_STA);WiFi.setSleep(false);connectWifi();}

void sendStatus(){
  String err=String("0x")+String((uint32_t)cameraError,HEX);err.toUpperCase();
  String j="{\"ok\":true,\"camera_ok\":"+String(cameraOk?"true":"false")+",\"camera_error\":\""+err+"\",\"psram\":"+String(psramOk?"true":"false")+",\"fw\":\""+String(FW)+"\",\"ip\":\""+WiFi.localIP().toString()+"\",\"ssid\":\""+WiFi.SSID()+"\",\"pid\":"+String(cameraPid)+",\"sensor\":\""+sensorName()+"\",\"width\":320,\"height\":240,\"capture_format\":\"RGB565\",\"stream_format\":\"MJPEG+WSS\",\"software_jpeg\":true,\"jpeg_quality\":"+String(JPG_QUALITY)+",\"xclk_hz\":20000000,\"stream\":\"http://"+WiFi.localIP().toString()+":81/stream\",\"fps\":"+String(fps100/100.0f,2)+",\"frames\":"+String(frameCounter)+",\"encode_ms\":"+String(lastEncodeUs/1000.0f,2)+",\"jpeg_bytes\":"+String((uint32_t)lastJpegBytes)+",\"capture_errors\":"+String(captureErrors)+",\"encode_errors\":"+String(encodeErrors)+",\"stream_clients\":"+String(streamClients)+",\"rssi\":"+String(WiFi.RSSI())+",\"flash_supported\":true,\"flash_gpio\":4,\"flash\":\""+flashName()+"\",\"remote_video\":"+remoteVideoJson()+"}";
  web.sendHeader("Access-Control-Allow-Origin","*");web.send(200,"application/json",j);
}

void setup(){
  Serial.begin(115200);delay(150);Serial.println();Serial.println(FW);pinMode(FLASH_PIN,OUTPUT);digitalWrite(FLASH_PIN,LOW);cameraMutex=xSemaphoreCreateMutex();cameraOk=cameraBegin();wifiBegin();
  web.on("/",[]{web.send(200,"text/plain",FW);});web.on("/status",sendStatus);web.on("/stream",[]{String u="http://"+WiFi.localIP().toString()+":81/stream";web.sendHeader("Location",u,true);web.send(302,"text/plain","stream");});
  web.on("/flash",[]{if(web.hasArg("mode")){String m=web.arg("mode");if(m!="off"&&m!="auto"&&m!="on")return web.send(400,"text/plain","BAD");setFlashText(m);}else if(web.hasArg("on"))setFlashText(web.arg("on"));else return web.send(400,"text/plain","BAD");web.send(200,"text/plain","OK");});
  web.on("/led",[]{if(web.hasArg("on"))setFlashText(web.arg("on"));else if(web.hasArg("state"))setFlashText(web.arg("state"));else return web.send(400,"text/plain","BAD");web.send(200,"text/plain","OK");});
  web.on("/control",[]{String v=web.arg("var"),x=web.arg("val");if(v=="led_intensity"||v=="flash"||v=="led"){setFlashText(x);return web.send(200,"text/plain","OK");}web.send(400,"text/plain","BAD");});
  web.onNotFound([]{web.send(404,"text/plain","NOT FOUND");});
  web.begin();mjpeg.begin();xTaskCreatePinnedToCore(streamTask,"mjpeg",6144,nullptr,1,nullptr,1);remoteVideoBegin(captureJpeg);Serial.printf("HTTP :80, MJPEG :81, remote WSS=%s | camera=%s\n",remoteVideoJson().c_str(),cameraOk?"OK":"FAIL");
}

void loop(){
  web.handleClient();
  updateFps();
  if(WiFi.status()!=WL_CONNECTED&&millis()-wifiAt>4500)connectWifi();
  wl_status_t now=WiFi.status();
  if(now!=lastWifiStatus){
    lastWifiStatus=now;
    if(now==WL_CONNECTED)Serial.printf("WiFi: connected %s, IP %s, RSSI %d\n",WiFi.SSID().c_str(),WiFi.localIP().toString().c_str(),WiFi.RSSI());
  }
  delay(2);
}
