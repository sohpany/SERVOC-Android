import http from "node:http";
import { timingSafeEqual } from "node:crypto";
import { WebSocketServer, WebSocket } from "ws";

const PORT = Number(process.env.PORT || 3000);
const MAX_JPEG = 1024 * 1024;
const MAX_VIEWER_BUFFER = 512 * 1024;
const PING_MS = 30000;
const rooms = new Map();

function validRobotId(id) { return /^[A-Za-z0-9._-]{1,64}$/.test(id); }
function bearer(req) { const h=String(req.headers.authorization||""); return h.startsWith("Bearer ")?h.slice(7):""; }
function safeEq(a,b){const aa=Buffer.from(String(a||"")),bb=Buffer.from(String(b||""));if(aa.length!==bb.length||aa.length===0)return false;return timingSafeEqual(aa,bb);}
function reject(socket,status,text){const body=Buffer.from(text,"utf8");const reason=status===400?"Bad Request":status===401?"Unauthorized":status===404?"Not Found":"Service Unavailable";try{socket.write(`HTTP/1.1 ${status} ${reason}\r\nConnection: close\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.length}\r\n\r\n`);socket.write(body);}finally{socket.destroy();}}
function roomFor(robotId){let room=rooms.get(robotId);if(!room){room={publisher:null,viewers:new Set()};rooms.set(robotId,room);}return room;}
function cleanRoom(robotId,room){if(!room.publisher&&room.viewers.size===0)rooms.delete(robotId);}

const server=http.createServer((req,res)=>{
  const url=new URL(req.url||"/","http://localhost");
  res.setHeader("Cache-Control","no-store");

  if(url.pathname==="/health"){
    const configured=Boolean(process.env.PUBLISH_TOKEN&&process.env.VIEW_TOKEN);
    res.writeHead(200,{"Content-Type":"application/json; charset=utf-8"});
    res.end(JSON.stringify({ok:true,service:"hazrob-video-relay",protocol:1,platform:"render",authConfigured:configured}));return;
  }

  if(url.pathname==="/hazrob/bootstrap"){
    const key=process.env.BOOTSTRAP_TOKEN||"";
    const appSecret=process.env.VIEW_TOKEN||"";
    if(!key||!appSecret){res.writeHead(503,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({ok:false,error:"bootstrap_not_configured"}));return;}
    if(!safeEq(bearer(req),key)){res.writeHead(401,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({ok:false,error:"unauthorized"}));return;}
    res.writeHead(200,{"Content-Type":"application/json; charset=utf-8"});
    res.end(JSON.stringify({ok:true,robot:"hazrob-001",mqtt_user:"hazrob_app",mqtt_password:appSecret,video_view_token:appSecret}));return;
  }

  const statusMatch=url.pathname.match(/^\/hazrob\/video\/status\/([^/]+)$/);
  if(statusMatch){
    let robotId;try{robotId=decodeURIComponent(statusMatch[1]);}catch{res.writeHead(400,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({ok:false,error:"invalid_robot_id"}));return;}
    if(!validRobotId(robotId)){res.writeHead(400,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({ok:false,error:"invalid_robot_id"}));return;}
    if(!process.env.VIEW_TOKEN||!safeEq(bearer(req),process.env.VIEW_TOKEN)){res.writeHead(401,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({ok:false,error:"unauthorized"}));return;}
    const room=rooms.get(robotId),publisherOnline=Boolean(room&&room.publisher&&room.publisher.readyState===WebSocket.OPEN),viewers=room?[...room.viewers].filter(v=>v.readyState===WebSocket.OPEN).length:0;
    res.writeHead(200,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({ok:true,service:"hazrob-video-relay",protocol:1,robot_id:robotId,publisher_online:publisherOnline,viewers}));return;
  }

  if(url.pathname==="/"){res.writeHead(200,{"Content-Type":"application/json; charset=utf-8"});res.end(JSON.stringify({service:"hazrob-video-relay",version:"0.9.1",health:"/health"}));return;}
  res.writeHead(404,{"Content-Type":"text/plain; charset=utf-8"});res.end("Not found");
});

const wss=new WebSocketServer({noServer:true,perMessageDeflate:false,maxPayload:MAX_JPEG});
server.on("upgrade",(req,socket,head)=>{
  let url;try{url=new URL(req.url||"/","http://localhost");}catch{reject(socket,400,"Invalid URL");return;}
  const m=url.pathname.match(/^\/hazrob\/video\/(publish|view)\/([^/]+)$/);if(!m){reject(socket,404,"Not found");return;}
  let robotId;try{robotId=decodeURIComponent(m[2]);}catch{reject(socket,400,"Invalid robot id");return;}if(!validRobotId(robotId)){reject(socket,400,"Invalid robot id");return;}
  const role=m[1]==="publish"?"publisher":"viewer",expected=role==="publisher"?process.env.PUBLISH_TOKEN:process.env.VIEW_TOKEN;
  if(!expected){reject(socket,503,"Relay credentials are not configured");return;}if(!safeEq(bearer(req),expected)){reject(socket,401,"Unauthorized");return;}
  wss.handleUpgrade(req,socket,head,ws=>{ws.hazRole=role;ws.hazRobotId=robotId;ws.isAlive=true;wss.emit("connection",ws,req);});
});

wss.on("connection",ws=>{
  const robotId=ws.hazRobotId,role=ws.hazRole,room=roomFor(robotId);
  ws.on("pong",()=>{ws.isAlive=true;});ws.on("error",()=>{});
  if(role==="publisher"){const old=room.publisher;if(old&&old!==ws){try{old.close(4001,"publisher replaced");}catch{}}room.publisher=ws;}else room.viewers.add(ws);
  ws.on("message",(data,isBinary)=>{
    if(role!=="publisher"||!isBinary)return;const frame=Buffer.isBuffer(data)?data:Buffer.from(data),n=frame.length;
    if(n<4||n>MAX_JPEG||frame[0]!==0xff||frame[1]!==0xd8||frame[n-2]!==0xff||frame[n-1]!==0xd9)return;
    for(const viewer of room.viewers){if(viewer.readyState!==WebSocket.OPEN||viewer.bufferedAmount>MAX_VIEWER_BUFFER)continue;try{viewer.send(frame,{binary:true,compress:false});}catch{}}
  });
  ws.on("close",()=>{if(role==="publisher"){if(room.publisher===ws)room.publisher=null;}else room.viewers.delete(ws);cleanRoom(robotId,room);});
});

const heartbeat=setInterval(()=>{for(const ws of wss.clients){if(ws.isAlive===false){try{ws.terminate();}catch{}continue;}ws.isAlive=false;try{ws.ping();}catch{}}},PING_MS);heartbeat.unref();
server.listen(PORT,"0.0.0.0",()=>console.log(`HazRob video relay listening on ${PORT}`));
function shutdown(){clearInterval(heartbeat);for(const ws of wss.clients){try{ws.close(1001,"server restart");}catch{}}server.close(()=>process.exit(0));setTimeout(()=>process.exit(0),5000).unref();}
process.on("SIGTERM",shutdown);process.on("SIGINT",shutdown);
