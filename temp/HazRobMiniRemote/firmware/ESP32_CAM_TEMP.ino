#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include "esp_camera.h"
#include "esp_http_server.h"

const char* WIFI_SSID = "4W-TEMP";
const char* RELAY_HOST = "https://hazrob-mini-remote.onrender.com";
const char* CHANNEL = "0Jo6HgI-45pkxeEeiA3fs1rb";

// AI Thinker ESP32-CAM pins
#define PWDN_GPIO_NUM 32
#define RESET_GPIO_NUM -1
#define XCLK_GPIO_NUM 0
#define SIOD_GPIO_NUM 26
#define SIOC_GPIO_NUM 27
#define Y9_GPIO_NUM 35
#define Y8_GPIO_NUM 34
#define Y7_GPIO_NUM 39
#define Y6_GPIO_NUM 36
#define Y5_GPIO_NUM 21
#define Y4_GPIO_NUM 19
#define Y3_GPIO_NUM 18
#define Y2_GPIO_NUM 5
#define VSYNC_GPIO_NUM 25
#define HREF_GPIO_NUM 23
#define PCLK_GPIO_NUM 22

httpd_handle_t web = NULL;
uint32_t lastUpload = 0;

static esp_err_t rootHandler(httpd_req_t* req){
  const char* text = "ESP32-CAM-TEMP /stream";
  httpd_resp_set_type(req,"text/plain");
  return httpd_resp_send(req,text,HTTPD_RESP_USE_STRLEN);
}

static esp_err_t streamHandler(httpd_req_t* req){
  static const char* TYPE = "multipart/x-mixed-replace;boundary=frame";
  static const char* BOUNDARY = "\r\n--frame\r\n";
  char header[96];
  httpd_resp_set_type(req,TYPE);
  httpd_resp_set_hdr(req,"Cache-Control","no-store");

  while(true){
    camera_fb_t* fb = esp_camera_fb_get();
    if(!fb) return ESP_FAIL;
    int n = snprintf(header,sizeof(header),
      "Content-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",
      (unsigned)fb->len);
    esp_err_t r = httpd_resp_send_chunk(req,BOUNDARY,strlen(BOUNDARY));
    if(r==ESP_OK) r = httpd_resp_send_chunk(req,header,n);
    if(r==ESP_OK) r = httpd_resp_send_chunk(req,(const char*)fb->buf,fb->len);
    esp_camera_fb_return(fb);
    if(r!=ESP_OK) break;
    delay(40);
  }
  return ESP_OK;
}

void startWeb(){
  httpd_config_t cfg = HTTPD_DEFAULT_CONFIG();
  cfg.server_port = 80;
  cfg.max_open_sockets = 4;
  if(httpd_start(&web,&cfg)==ESP_OK){
    httpd_uri_t rootUri;
    rootUri.uri = "/";
    rootUri.method = HTTP_GET;
    rootUri.handler = rootHandler;
    rootUri.user_ctx = NULL;
    httpd_register_uri_handler(web,&rootUri);

    httpd_uri_t streamUri;
    streamUri.uri = "/stream";
    streamUri.method = HTTP_GET;
    streamUri.handler = streamHandler;
    streamUri.user_ctx = NULL;
    httpd_register_uri_handler(web,&streamUri);
  }
}

bool initCamera(){
  camera_config_t c;
  c.ledc_channel = LEDC_CHANNEL_0;
  c.ledc_timer = LEDC_TIMER_0;
  c.pin_d0 = Y2_GPIO_NUM;
  c.pin_d1 = Y3_GPIO_NUM;
  c.pin_d2 = Y4_GPIO_NUM;
  c.pin_d3 = Y5_GPIO_NUM;
  c.pin_d4 = Y6_GPIO_NUM;
  c.pin_d5 = Y7_GPIO_NUM;
  c.pin_d6 = Y8_GPIO_NUM;
  c.pin_d7 = Y9_GPIO_NUM;
  c.pin_xclk = XCLK_GPIO_NUM;
  c.pin_pclk = PCLK_GPIO_NUM;
  c.pin_vsync = VSYNC_GPIO_NUM;
  c.pin_href = HREF_GPIO_NUM;
  c.pin_sccb_sda = SIOD_GPIO_NUM;
  c.pin_sccb_scl = SIOC_GPIO_NUM;
  c.pin_pwdn = PWDN_GPIO_NUM;
  c.pin_reset = RESET_GPIO_NUM;
  c.xclk_freq_hz = 20000000;
  c.pixel_format = PIXFORMAT_JPEG;
  c.grab_mode = CAMERA_GRAB_LATEST;

  if(psramFound()){
    c.frame_size = FRAMESIZE_QVGA;
    c.jpeg_quality = 12;
    c.fb_count = 2;
    c.fb_location = CAMERA_FB_IN_PSRAM;
  }else{
    c.frame_size = FRAMESIZE_QQVGA;
    c.jpeg_quality = 14;
    c.fb_count = 1;
    c.fb_location = CAMERA_FB_IN_DRAM;
  }
  return esp_camera_init(&c)==ESP_OK;
}

void uploadFrame(){
  if(WiFi.status()!=WL_CONNECTED) return;
  uint32_t now = millis();
  if(now-lastUpload<250) return;
  lastUpload = now;

  camera_fb_t* fb = esp_camera_fb_get();
  if(!fb) return;

  WiFiClientSecure tls;
  tls.setInsecure();
  HTTPClient http;
  String url = String(RELAY_HOST)+"/frame/"+CHANNEL;
  if(http.begin(tls,url)){
    http.setTimeout(1800);
    http.addHeader("Content-Type","image/jpeg");
    http.POST(fb->buf,fb->len);
    http.end();
  }
  esp_camera_fb_return(fb);
}

void setup(){
  if(!initCamera()) while(true) delay(1000);

  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
  IPAddress ip(192,168,4,2), gw(192,168,4,1), mask(255,255,255,0);
  WiFi.config(ip,gw,mask);
  WiFi.begin(WIFI_SSID);
  while(WiFi.status()!=WL_CONNECTED) delay(250);

  startWeb();
}

void loop(){
  uploadFrame();
  delay(5);
}
