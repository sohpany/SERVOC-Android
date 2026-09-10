const http = require('http');
const { URL } = require('url');

const PORT = Number(process.env.PORT || 10000);
const CHANNEL = '0Jo6HgI-45pkxeEeiA3fs1rb';
const DRIVE_TTL_MS = 700;
const MAX_JPEG = 180 * 1024;

let drive = { l: 0, r: 0, at: 0 };
let lastFrame = null;
let cameraAt = 0;
let robotAt = 0;
const viewers = new Set();

function clamp(v) {
  v = Number(v);
  if (!Number.isFinite(v)) return 0;
  return Math.max(-100, Math.min(100, Math.round(v)));
}
function json(res, code, obj) {
  const body = Buffer.from(JSON.stringify(obj));
  res.writeHead(code, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': body.length,
    'Cache-Control': 'no-store',
    'Access-Control-Allow-Origin': '*'
  });
  res.end(body);
}
function validChannel(path) {
  return path.endsWith('/' + CHANNEL);
}
function broadcastFrame(buf) {
  const head = Buffer.from(`--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${buf.length}\r\n\r\n`);
  const tail = Buffer.from('\r\n');
  for (const res of [...viewers]) {
    try {
      res.write(head); res.write(buf); res.write(tail);
    } catch (_) {
      viewers.delete(res);
      try { res.end(); } catch (_) {}
    }
  }
}

const server = http.createServer((req, res) => {
  const u = new URL(req.url, 'http://localhost');
  const p = u.pathname;

  if (p === '/health') return json(res, 200, { ok: true, service: 'HazRobMiniRemote', version: 1 });
  if (p === '/status/' + CHANNEL) {
    const now = Date.now();
    return json(res, 200, {
      ok: true,
      robot_online: now - robotAt < 3000,
      camera_online: now - cameraAt < 3000,
      drive_age_ms: drive.at ? now - drive.at : null,
      has_frame: !!lastFrame
    });
  }

  if (p === '/drive/' + CHANNEL) {
    drive = { l: clamp(u.searchParams.get('l')), r: clamp(u.searchParams.get('r')), at: Date.now() };
    return json(res, 200, { ok: true, l: drive.l, r: drive.r });
  }

  if (p === '/cmd/' + CHANNEL) {
    robotAt = Date.now();
    const age = drive.at ? Date.now() - drive.at : 999999;
    const active = age <= DRIVE_TTL_MS;
    return json(res, 200, { l: active ? drive.l : 0, r: active ? drive.r : 0, age_ms: age });
  }

  if (p === '/frame/' + CHANNEL && req.method === 'POST') {
    const chunks = [];
    let size = 0;
    req.on('data', c => {
      size += c.length;
      if (size <= MAX_JPEG) chunks.push(c);
      else req.destroy();
    });
    req.on('end', () => {
      if (!size || size > MAX_JPEG) return json(res, 413, { ok: false });
      const buf = Buffer.concat(chunks);
      if (buf.length < 4 || buf[0] !== 0xFF || buf[1] !== 0xD8 || buf[buf.length - 2] !== 0xFF || buf[buf.length - 1] !== 0xD9) {
        return json(res, 400, { ok: false, error: 'jpeg' });
      }
      lastFrame = buf;
      cameraAt = Date.now();
      broadcastFrame(buf);
      return json(res, 200, { ok: true, bytes: buf.length });
    });
    return;
  }

  if (p === '/video/' + CHANNEL) {
    res.writeHead(200, {
      'Content-Type': 'multipart/x-mixed-replace; boundary=frame',
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*'
    });
    viewers.add(res);
    if (lastFrame) broadcastFrame(lastFrame);
    req.on('close', () => viewers.delete(res));
    return;
  }

  json(res, 404, { ok: false });
});

server.listen(PORT, '0.0.0.0', () => console.log(`HazRobMiniRemote listening on ${PORT}`));
