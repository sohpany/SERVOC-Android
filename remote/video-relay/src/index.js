import http from "node:http";
import tls from "node:tls";
import { timingSafeEqual } from "node:crypto";
import { WebSocketServer, WebSocket } from "ws";

const PORT=Number(process.env.PORT||3000),SERVICE_VERSION="0.10.2",PROTOCOL=2;
const MAX_JPEG=256*1024,MAX_VIEWER_BUFFER=512*1024,PING_MS=30000;
const MQTT_HOST="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud",MQTT_PORT=8883,ROBOT_ID="hazrob-001";
const rooms=new Map();let mqttDiagnostic={state:"not_run"};
const clean=v=>String(v||"").replace(/\s+/g,"");
const present=v=>typeof v==="string"&&v.length>=8;
function safeEq(a,b){const aa=Buffer.from(String(a||"")),bb=Buffer.from(String(b||""));if(aa.length!==bb.length||!aa.length)return false;return timingSafeEqual(aa,bb);}
function distinct(a,b){return present(a)&&present(b)&&!safeEq(a,b);}
function bearer(req){const h=String(req.headers.authorization||"");return h.startsWith("Bearer ")?clean(h.slice(7)):"";}
function validRobotId(id){return /^[A-Za-z0-9._-]{1,64}$/.test(id);}
function reject(socket,status,text){const body=Buffer.from(text,"utf8"),reason=status===400?"Bad Request":status===401?"Unauthorized":status===404?"Not Found":"Service Unavailable";try{socket.write(`HTTP/1.1 ${status} ${reason}\r\nConnection: close\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.length}\r\n\r\n`);socket.write(body);}finally{socket.destroy();}}
function roomFor(id){let r=rooms.get(id);if(!r){r={publisher:null,publisherConnectedAt:0,viewers:new Set(),frames:0,lastFrameBytes:0,lastFrameAt:0};rooms.set(id,r);}return r;}
function cleanRoom(id,r){if(!r.publisher&&!r.viewers.size)rooms.delete(id);}
function aggregate(){let publishers=0,viewers=0;for(const r of rooms.values()){if(r.publisher?.readyState===WebSocket.OPEN)publishers++;for(const v of r.viewers)if(v.readyState===WebSocket.OPEN)viewers++;}return{rooms:rooms.size,publishers,viewers};}
function cfg(){return{
  bootstrap:clean(process.env.BOOTSTRAP_TOKEN),publish:clean(process.env.PUBLISH_TOKEN),view:clean(process.env.VIEW_TOKEN),
  appUser:String(process.env.MQTT_APP_USER||"hazrob_app").trim(),appPass:clean(process.env.MQTT_APP_PASSWORD),
  robotUser:String(process.env.MQTT_ROBOT_USER||"hazrob_robot").trim(),robotPass:clean(process.env.MQTT_ROBOT_PASSWORD||process.env.MQTT_ROBOT_PASSWORD_TEST)
};}
function roleOf(req){const x=String(req.headers["x-hazrob-client"]||"").trim().toLowerCase();return x||"legacy";}
function json(res,status,payload){res.writeHead(status,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify(payload));}

function mqttUtf(v){const b=Buffer.from(String(v),"utf8"),h=Buffer.alloc(2);h.writeUInt16BE(b.length);return Buffer.concat([h,b]);}
function mqttRemainingLength(n){const out=[];do{let d=n%128;n=Math.floor(n/128);if(n>0)d|=0x80;out.push(d);}while(n>0);return Buffer.from(out);}
function mqttPacket(header,body){return Buffer.concat([Buffer.from([header]),mqttRemainingLength(body.length),body]);}
function mqttConnectPacket(u,p,id,willTopic="",willPayload=""){
  const hasWill=Boolean(willTopic),flags=hasWill?0xE6:0xC2;
  const variable=Buffer.concat([mqttUtf("MQTT"),Buffer.from([4,flags,0,10])]);
  const parts=[mqttUtf(id)];if(hasWill){parts.push(mqttUtf(willTopic),mqttUtf(willPayload));}parts.push(mqttUtf(u),mqttUtf(p));
  return mqttPacket(0x10,Buffer.concat([variable,...parts]));
}
function mqttConnackText(c){return({0:"accepted",1:"protocol_rejected",2:"client_id_rejected",3:"server_unavailable",4:"bad_credentials",5:"not_authorized"})[c]||"unknown";}
function mqttSubscribePacket(topic,id){const h=Buffer.alloc(2);h.writeUInt16BE(id);return mqttPacket(0x82,Buffer.concat([h,mqttUtf(topic),Buffer.from([0])]));}
function mqttPublishPacket(topic,payload,retain=false){return mqttPacket(retain?0x31:0x30,Buffer.concat([mqttUtf(topic),Buffer.from(String(payload),"utf8")]));}
function parsePublish(body){if(body.length<2)return null;const n=body.readUInt16BE(0);if(n>body.length-2)return null;return{topic:body.subarray(2,2+n).toString("utf8"),payload:body.subarray(2+n).toString("utf8")};}
class DiagMqtt{
  constructor(user,password,id,will=false){this.user=user;this.password=password;this.id=id;this.will=will;this.sock=null;this.buf=Buffer.alloc(0);this.packets=[];this.waiters=[];this.packetId=1;}
  feed(chunk){this.buf=Buffer.concat([this.buf,chunk]);for(;;){if(this.buf.length<2)return;let mul=1,len=0,i=1,d;do{if(i>=this.buf.length)return;d=this.buf[i++];len+=(d&127)*mul;mul*=128;if(i>5)throw new Error("malformed_remaining_length");}while(d&128);if(this.buf.length<i+len)return;const h=this.buf[0],body=this.buf.subarray(i,i+len);this.buf=this.buf.subarray(i+len);const p={type:h>>4,flags:h&15,body};let used=false;for(let w=0;w<this.waiters.length;w++){const x=this.waiters[w];if(x.match(p)){this.waiters.splice(w,1);clearTimeout(x.timer);x.resolve(p);used=true;break;}}if(!used)this.packets.push(p);}}
  wait(match,ms=4000){for(let i=0;i<this.packets.length;i++){if(match(this.packets[i]))return Promise.resolve(this.packets.splice(i,1)[0]);}return new Promise((resolve,reject)=>{const x={match,resolve,reject,timer:null};x.timer=setTimeout(()=>{const i=this.waiters.indexOf(x);if(i>=0)this.waiters.splice(i,1);reject(new Error("timeout"));},ms);this.waiters.push(x);});}
  async connect(){this.sock=tls.connect({host:MQTT_HOST,port:MQTT_PORT,servername:MQTT_HOST,rejectUnauthorized:true});this.sock.on("data",c=>this.feed(c));await new Promise((resolve,reject)=>{const t=setTimeout(()=>reject(new Error("tls_timeout")),6000);this.sock.once("secureConnect",()=>{clearTimeout(t);resolve();});this.sock.once("error",e=>{clearTimeout(t);reject(e);});});const root=`hazrob/${ROBOT_ID}/`;this.sock.write(mqttConnectPacket(this.user,this.password,this.id,this.will?root+"status":"",this.will?'{"mqtt":false,"tls":false,"remote_ready":false,"reason":"diag_lwt"}':""));const p=await this.wait(x=>x.type===2,5000);const code=p.body.length>=2?p.body[1]:255;if(code!==0)throw new Error(`connack_${code}_${mqttConnackText(code)}`);return true;}
  async subscribe(topic){const id=++this.packetId;this.sock.write(mqttSubscribePacket(topic,id));const p=await this.wait(x=>x.type===9&&x.body.length>=3&&x.body.readUInt16BE(0)===id,4000);const code=p.body[2];return{topic,granted:code!==0x80,code};}
  publish(topic,payload,retain=false){this.sock.write(mqttPublishPacket(topic,payload,retain));}
  async receive(topic,ms=4000){const p=await this.wait(x=>{if(x.type!==3)return false;const q=parsePublish(x.body);return q?.topic===topic;},ms);return parsePublish(p.body);}
  close(){try{this.sock?.write(Buffer.from([0xE0,0]));}catch{}try{this.sock?.end();}catch{}this.sock=null;}
}
async function testMqtt(user,password,variant){const c=new DiagMqtt(user,password,`hazrob-render-diag-${Date.now()}-${Math.random().toString(16).slice(2,8)}`);try{await c.connect();return{variant,tls:true,mqtt:true,connack:0,result:"accepted"};}catch(e){return{variant,tls:Boolean(c.sock),mqtt:false,connack:-1,result:String(e?.message||e)};}finally{c.close();}}
async function runCredentialDiagnostics(){if(process.env.HAZROB_SKIP_MQTT_DIAGNOSTICS==="1")return;const c=cfg();const base={at:new Date().toISOString(),state:"running"};mqttDiagnostic=base;
  if(present(c.appPass))console.log("HAZROB_MQTT_APP_DIAGNOSTIC "+JSON.stringify([await testMqtt(c.appUser,c.appPass,"stored")]));
  if(present(c.robotPass))console.log("HAZROB_MQTT_ROBOT_DIAGNOSTIC "+JSON.stringify([await testMqtt(c.robotUser,c.robotPass,"stored")]));
  if(!present(c.appPass)||!present(c.robotPass)){mqttDiagnostic={...base,state:"incomplete",reason:"credentials_missing"};return;}
  const root=`hazrob/${ROBOT_ID}/`,stamp=Date.now(),app=new DiagMqtt(c.appUser,c.appPass,`hazrob-app-acl-${stamp}`),robot=new DiagMqtt(c.robotUser,c.robotPass,`hazrob-robot-acl-${stamp}`,true);
  const result={...base,connect:{app:false,robot:false},subscribe:{},roundtrip:{},retained_status:{tested:false,restored:false}};
  try{
    await app.connect();result.connect.app=true;await robot.connect();result.connect.robot=true;
    for(const t of ["ack","telemetry","status"]){const s=await app.subscribe(root+t);result.subscribe[`app_${t}`]=s.granted;}
    {const s=await robot.subscribe(root+"cmd");result.subscribe.robot_cmd=s.granted;}
    let oldStatus=null;try{const q=await app.receive(root+"status",700);oldStatus=q?.payload??null;}catch{}
    const probe=JSON.stringify({session:`render-diag-${stamp}`,seq:1,nonce:`render-diag-${stamp}`,type:"probe",sent_ms:Date.now(),ttl_ms:1500});const cmdWait=robot.receive(root+"cmd",4000);app.publish(root+"cmd",probe,false);result.roundtrip.app_publish_cmd_robot_receive=Boolean(await cmdWait);
    const ack=JSON.stringify({session:`render-diag-${stamp}`,seq:1,nonce:`render-diag-${stamp}`,ok:true,payload:{render_diag:true}});const ackWait=app.receive(root+"ack",4000);robot.publish(root+"ack",ack,false);result.roundtrip.robot_publish_ack_app_receive=Boolean(await ackWait);
    const telemetry=JSON.stringify({render_diag:true,at:Date.now()});const telemetryWait=app.receive(root+"telemetry",4000);robot.publish(root+"telemetry",telemetry,false);result.roundtrip.robot_publish_telemetry_app_receive=Boolean(await telemetryWait);
    const diagStatus=JSON.stringify({render_diag:true,mqtt:true,tls:true,remote_ready:false,reason:"acl_test"});const statusWait=app.receive(root+"status",4000);robot.publish(root+"status",diagStatus,true);result.retained_status.tested=Boolean(await statusWait);
    if(oldStatus!==null){robot.publish(root+"status",oldStatus,true);result.retained_status.restored=true;}else{robot.publish(root+"status","",true);result.retained_status.restored=true;}
    result.state=Object.values(result.connect).every(Boolean)&&Object.values(result.subscribe).every(Boolean)&&Object.values(result.roundtrip).every(Boolean)&&result.retained_status.tested?"pass":"fail";
  }catch(e){result.state="fail";result.error=String(e?.message||e);}finally{app.close();robot.close();mqttDiagnostic=result;console.log("HAZROB_MQTT_ACL_DIAGNOSTIC "+JSON.stringify(result));}
}

function bootstrap(req,res){const c=cfg(),role=roleOf(req);if(!present(c.bootstrap)){json(res,503,{ok:false,error:"bootstrap_not_configured"});return;}if(!safeEq(bearer(req),c.bootstrap)){json(res,401,{ok:false,error:"unauthorized"});return;}
  const base={ok:true,version:SERVICE_VERSION,protocol:PROTOCOL,robot:ROBOT_ID,client:role};
  if(role==="android"){
    if(!present(c.appPass)||!present(c.view)){json(res,503,{ok:false,error:"android_bootstrap_not_configured"});return;}
    json(res,200,{...base,mqtt_user:c.appUser,mqtt_password:c.appPass,video_view_token:c.view});return;
  }
  if(role==="esp32"){
    if(!present(c.robotPass)){json(res,503,{ok:false,error:"robot_bootstrap_not_configured"});return;}
    json(res,200,{...base,mqtt_user:c.robotUser,mqtt_password:c.robotPass});return;
  }
  if(role==="esp32-cam"){
    if(!present(c.publish)){json(res,503,{ok:false,error:"camera_bootstrap_not_configured"});return;}
    json(res,200,{...base,video_publish_token:c.publish});return;
  }
  if(role==="legacy"){
    if(!present(c.appPass)||!present(c.view)||!present(c.publish)){json(res,503,{ok:false,error:"legacy_bootstrap_not_configured"});return;}
    json(res,200,{...base,mqtt_user:c.appUser,mqtt_password:c.appPass,video_view_token:c.view,video_publish_token:c.publish});return;
  }
  json(res,400,{ok:false,error:"unknown_bootstrap_client"});
}

const server=http.createServer((req,res)=>{
  let url;try{url=new URL(req.url||"/","http://localhost");}catch{json(res,400,{ok:false,error:"invalid_url"});return;}res.setHeader("Cache-Control","no-store");const c=cfg();
  if(url.pathname==="/health"){
    const videoAuthConfigured=present(c.publish)&&present(c.view)&&distinct(c.publish,c.view);
    const mqttAppConfigured=present(c.appPass),mqttRobotConfigured=present(c.robotPass);
    const bootstrapConfigured=present(c.bootstrap)&&videoAuthConfigured&&mqttAppConfigured&&mqttRobotConfigured;
    json(res,200,{ok:true,service:"hazrob-video-relay",version:SERVICE_VERSION,protocol:PROTOCOL,platform:"render",role_scoped_bootstrap:true,legacy_bridge:true,videoAuthConfigured,mqttAppConfigured,mqttRobotConfigured,bootstrapConfigured,mqttDiagnostic,...aggregate()});return;
  }
  if(url.pathname==="/hazrob/bootstrap"){bootstrap(req,res);return;}
  const sm=url.pathname.match(/^\/hazrob\/video\/status\/([^/]+)$/);
  if(sm){let id;try{id=decodeURIComponent(sm[1]);}catch{json(res,400,{ok:false,error:"invalid_robot_id"});return;}if(!validRobotId(id)){json(res,400,{ok:false,error:"invalid_robot_id"});return;}if(!present(c.view)||!safeEq(bearer(req),c.view)){json(res,401,{ok:false,error:"unauthorized"});return;}const r=rooms.get(id),publisherOnline=Boolean(r&&r.publisher&&r.publisher.readyState===WebSocket.OPEN),viewers=r?[...r.viewers].filter(v=>v.readyState===WebSocket.OPEN).length:0,lastFrameAt=r?.lastFrameAt||0;json(res,200,{ok:true,service:"hazrob-video-relay",version:SERVICE_VERSION,protocol:PROTOCOL,robot_id:id,publisher_online:publisherOnline,publisher_connected_at:r?.publisherConnectedAt||0,viewers,frames:r?.frames||0,last_frame_bytes:r?.lastFrameBytes||0,last_frame_at:lastFrameAt,last_frame_age_ms:lastFrameAt?Math.max(0,Date.now()-lastFrameAt):null});return;}
  if(url.pathname==="/"){json(res,200,{service:"hazrob-video-relay",version:SERVICE_VERSION,protocol:PROTOCOL,health:"/health"});return;}
  res.writeHead(404,{"Content-Type":"text/plain; charset=utf-8"});res.end("Not found");
});

const wss=new WebSocketServer({noServer:true,perMessageDeflate:false,maxPayload:MAX_JPEG});
server.on("upgrade",(req,socket,head)=>{let url;try{url=new URL(req.url||"/","http://localhost");}catch{reject(socket,400,"Invalid URL");return;}const m=url.pathname.match(/^\/hazrob\/video\/(publish|view)\/([^/]+)$/);if(!m){reject(socket,404,"Not found");return;}let id;try{id=decodeURIComponent(m[2]);}catch{reject(socket,400,"Invalid robot id");return;}if(!validRobotId(id)){reject(socket,400,"Invalid robot id");return;}const c=cfg(),role=m[1]==="publish"?"publisher":"viewer",expected=role==="publisher"?c.publish:c.view;if(!present(expected)){reject(socket,503,"Relay credentials are not configured");return;}if(!safeEq(bearer(req),expected)){reject(socket,401,"Unauthorized");return;}wss.handleUpgrade(req,socket,head,ws=>{ws.hazRole=role;ws.hazRobotId=id;ws.isAlive=true;wss.emit("connection",ws,req);});});

wss.on("connection",ws=>{const id=ws.hazRobotId,role=ws.hazRole,r=roomFor(id);ws.on("pong",()=>{ws.isAlive=true;});ws.on("error",()=>{});if(role==="publisher"){const old=r.publisher;if(old&&old!==ws){try{old.close(4001,"publisher replaced");}catch{}}r.publisher=ws;r.publisherConnectedAt=Date.now();}else r.viewers.add(ws);ws.on("message",(data,isBinary)=>{if(role!=="publisher"||!isBinary)return;const frame=Buffer.isBuffer(data)?data:Buffer.from(data),n=frame.length;if(n<4||n>MAX_JPEG||frame[0]!==0xff||frame[1]!==0xd8||frame[n-2]!==0xff||frame[n-1]!==0xd9)return;r.frames++;r.lastFrameBytes=n;r.lastFrameAt=Date.now();for(const v of r.viewers){if(v.readyState!==WebSocket.OPEN||v.bufferedAmount>MAX_VIEWER_BUFFER)continue;try{v.send(frame,{binary:true,compress:false});}catch{}}});ws.on("close",()=>{if(role==="publisher"){if(r.publisher===ws){r.publisher=null;r.publisherConnectedAt=0;}}else r.viewers.delete(ws);cleanRoom(id,r);});});

const heartbeat=setInterval(()=>{for(const ws of wss.clients){if(ws.isAlive===false){try{ws.terminate();}catch{}continue;}ws.isAlive=false;try{ws.ping();}catch{}}},PING_MS);heartbeat.unref();
server.listen(PORT,"0.0.0.0",()=>{console.log(`HazRob video relay ${SERVICE_VERSION} listening on ${PORT}`);runCredentialDiagnostics().catch(e=>{mqttDiagnostic={state:"fail",error:String(e?.message||e)};console.log("HAZROB_MQTT_DIAGNOSTIC failed "+mqttDiagnostic.error);});});
function shutdown(){clearInterval(heartbeat);for(const ws of wss.clients){try{ws.close(1001,"server restart");}catch{}}server.close(()=>process.exit(0));setTimeout(()=>process.exit(0),5000).unref();}
process.on("SIGTERM",shutdown);process.on("SIGINT",shutdown);
