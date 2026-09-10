import assert from "node:assert/strict";
import http from "node:http";
import { spawn } from "node:child_process";
import { WebSocket } from "ws";

const PORT=31941;
const BASE=`http://127.0.0.1:${PORT}`;
const BOOT="bootstrap-token-01",APP_PASS="app-password-01",ROBOT_PASS="robot-password-01",VIEW="view-token-01",PUBLISH="publish-token-01";
const child=spawn(process.execPath,["src/index.js"],{
  cwd:new URL("..",import.meta.url),
  env:{...process.env,PORT:String(PORT),BOOTSTRAP_TOKEN:BOOT,MQTT_APP_USER:"hazrob_app",MQTT_APP_PASSWORD:APP_PASS,MQTT_ROBOT_USER:"hazrob_robot",MQTT_ROBOT_PASSWORD_TEST:ROBOT_PASS,VIEW_TOKEN:VIEW,PUBLISH_TOKEN:PUBLISH,HAZROB_SKIP_MQTT_DIAGNOSTICS:"1"},
  stdio:["ignore","pipe","pipe"]
});
let stderr="";child.stderr.on("data",b=>stderr+=b.toString());

function request(path,headers={}){return new Promise((resolve,reject)=>{const req=http.get(BASE+path,{headers},res=>{let s="";res.setEncoding("utf8");res.on("data",x=>s+=x);res.on("end",()=>{let body=s;try{body=JSON.parse(s);}catch{}resolve({status:res.statusCode,body});});});req.on("error",reject);});}
async function waitHealth(){for(let i=0;i<50;i++){try{const r=await request("/health");if(r.status===200)return r.body;}catch{}await new Promise(r=>setTimeout(r,100));}throw new Error("relay did not start: "+stderr);}
function auth(token){return{Authorization:`Bearer ${token}`};}
function bootstrap(role){return request("/hazrob/bootstrap",{...auth(BOOT),"X-HazRob-Client":role});}
function openWs(path,token){return new Promise((resolve,reject)=>{const ws=new WebSocket(`ws://127.0.0.1:${PORT}${path}`,{headers:auth(token)});const timer=setTimeout(()=>{try{ws.terminate();}catch{}reject(new Error("WebSocket open timeout"));},3000);ws.once("open",()=>{clearTimeout(timer);resolve(ws);});ws.once("error",e=>{clearTimeout(timer);reject(e);});});}
function oneMessage(ws){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(new Error("frame timeout")),3000);ws.once("message",(data,isBinary)=>{clearTimeout(timer);resolve({data:Buffer.from(data),isBinary});});});}

try{
  const health=await waitHealth();
  assert.equal(health.version,"0.10.1");assert.equal(health.protocol,2);assert.equal(health.role_scoped_bootstrap,true);assert.equal(health.bootstrapConfigured,true);

  let r=await bootstrap("android");assert.equal(r.status,200);assert.equal(r.body.client,"android");assert.equal(r.body.mqtt_user,"hazrob_app");assert.equal(r.body.mqtt_password,APP_PASS);assert.equal(r.body.video_view_token,VIEW);assert.equal("video_publish_token" in r.body,false);
  r=await bootstrap("esp32");assert.equal(r.status,200);assert.equal(r.body.client,"esp32");assert.equal(r.body.mqtt_user,"hazrob_robot");assert.equal(r.body.mqtt_password,ROBOT_PASS);assert.equal("video_view_token" in r.body,false);assert.equal("video_publish_token" in r.body,false);
  r=await bootstrap("esp32-cam");assert.equal(r.status,200);assert.equal(r.body.client,"esp32-cam");assert.equal(r.body.video_publish_token,PUBLISH);assert.equal("mqtt_password" in r.body,false);assert.equal("video_view_token" in r.body,false);
  r=await request("/hazrob/bootstrap",auth(BOOT));assert.equal(r.status,200);assert.equal(r.body.client,"legacy");assert.equal(r.body.video_publish_token,PUBLISH);assert.equal(r.body.video_view_token,VIEW);
  r=await request("/hazrob/bootstrap",auth("wrong-token"));assert.equal(r.status,401);

  const viewer=await openWs("/hazrob/video/view/hazrob-001",VIEW);
  const publisher=await openWs("/hazrob/video/publish/hazrob-001",PUBLISH);
  const jpeg=Buffer.from([0xff,0xd8,0x01,0x02,0x03,0xff,0xd9]);
  const incoming=oneMessage(viewer);publisher.send(jpeg,{binary:true});const msg=await incoming;assert.equal(msg.isBinary,true);assert.deepEqual(msg.data,jpeg);
  r=await request("/hazrob/video/status/hazrob-001",auth(VIEW));assert.equal(r.status,200);assert.equal(r.body.publisher_online,true);assert.ok(r.body.frames>=1);assert.equal(r.body.last_frame_bytes,jpeg.length);
  viewer.close();publisher.close();
  console.log("HazRob 0.10.1 relay contract: PASS");
} finally {
  child.kill("SIGTERM");
}
