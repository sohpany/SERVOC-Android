import http from "node:http";
import tls from "node:tls";
import { timingSafeEqual } from "node:crypto";
import { WebSocketServer, WebSocket } from "ws";

const PORT=Number(process.env.PORT||3000),SERVICE_VERSION="0.10.1",PROTOCOL=2;
const MAX_JPEG=256*1024,MAX_VIEWER_BUFFER=512*1024,PING_MS=30000;
const rooms=new Map();
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
function mqttConnectPacket(u,p,id){const variable=Buffer.concat([mqttUtf("MQTT"),Buffer.from([4,0xC2,0,10])]),payload=Buffer.concat([mqttUtf(id),mqttUtf(u),mqttUtf(p)]),body=Buffer.concat([variable,payload]);return Buffer.concat([Buffer.from([0x10]),mqttRemainingLength(body.length),body]);}
function mqttConnackText(c){return({0:"accepted",1:"protocol_rejected",2:"client_id_rejected",3:"server_unavailable",4:"bad_credentials",5:"not_authorized"})[c]||"unknown";}
function testMqtt(user,password,variant){return new Promise(resolve=>{const host="e96d6c3b500149bf96ccc8b91cb2ae0c.s1.eu.hivemq.cloud",port=8883;let done=false,tlsOk=false,buf=Buffer.alloc(0);const finish=(x={})=>{if(done)return;done=true;clearTimeout(timer);try{sock.destroy();}catch{}resolve({variant,tls:tlsOk,mqtt:false,connack:-1,...x});},sock=tls.connect({host,port,servername:host,rejectUnauthorized:true},()=>{tlsOk=true;try{sock.write(mqttConnectPacket(user,password,`hazrob-render-diag-${Date.now()}`));}catch{finish({error:"write_failed"});}}),timer=setTimeout(()=>finish({error:"timeout"}),6000);sock.on("data",chunk=>{buf=Buffer.concat([buf,chunk]);if(buf.length>=4&&buf[0]===0x20&&buf[1]===0x02){const code=buf[3];if(done)return;done=true;clearTimeout(timer);try{sock.end(Buffer.from([0xE0,0x00]));}catch{}resolve({variant,tls:tlsOk,mqtt:code===0,connack:code,result:mqttConnackText(code)});}});sock.on("error",()=>finish({error:tlsOk?"socket_error":"tls_error"}));sock.on("close",()=>{if(!done)finish({error:tlsOk?"closed_before_connack":"tls_closed"});});});}
async function runCredentialDiagnostics(){if(process.env.HAZROB_SKIP_MQTT_DIAGNOSTICS==="1")return;const c=cfg();if(present(c.appPass))console.log("HAZROB_MQTT_APP_DIAGNOSTIC "+JSON.stringify([await testMqtt(c.appUser,c.appPass,"stored")]));if(present(c.robotPass))console.log("HAZROB_MQTT_ROBOT_DIAGNOSTIC "+JSON.stringify([await testMqtt(c.robotUser,c.robotPass,"stored")]));}

function bootstrap(req,res){const c=cfg(),role=roleOf(req);if(!present(c.bootstrap)){json(res,503,{ok:false,error:"bootstrap_not_configured"});return;}if(!safeEq(bearer(req),c.bootstrap)){json(res,401,{ok:false,error:"unauthorized"});return;}
  const base={ok:true,version:SERVICE_VERSION,protocol:PROTOCOL,robot:"hazrob-001",client:role};
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
    // One-release compatibility bridge for already-flashed 0.10.0 clients. 0.10.1 clients always send X-HazRob-Client.
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
    json(res,200,{ok:true,service:"hazrob-video-relay",version:SERVICE_VERSION,protocol:PROTOCOL,platform:"render",role_scoped_bootstrap:true,legacy_bridge:true,videoAuthConfigured,mqttAppConfigured,mqttRobotConfigured,bootstrapConfigured,...aggregate()});return;
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
server.listen(PORT,"0.0.0.0",()=>{console.log(`HazRob video relay ${SERVICE_VERSION} listening on ${PORT}`);runCredentialDiagnostics().catch(()=>console.log("HAZROB_MQTT_DIAGNOSTIC failed"));});
function shutdown(){clearInterval(heartbeat);for(const ws of wss.clients){try{ws.close(1001,"server restart");}catch{}}server.close(()=>process.exit(0));setTimeout(()=>process.exit(0),5000).unref();}
process.on("SIGTERM",shutdown);process.on("SIGINT",shutdown);
