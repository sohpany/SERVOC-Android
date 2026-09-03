import { DurableObject } from "cloudflare:workers";

const MAX_JPEG = 1024 * 1024;
const MAX_VIEWER_BUFFER = 512 * 1024;

function bearer(request) {
  const h = request.headers.get("Authorization") || "";
  return h.startsWith("Bearer ") ? h.slice(7) : "";
}

function safeEq(a, b) {
  a = String(a || "");
  b = String(b || "");
  let diff = a.length ^ b.length;
  const n = Math.max(a.length, b.length);
  for (let i = 0; i < n; i++) diff |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return diff === 0;
}

function validRobotId(id) {
  return /^[A-Za-z0-9._-]{1,64}$/.test(id);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/health") return Response.json({ ok: true, service: "hazrob-video-relay", protocol: 1 });
    if ((request.headers.get("Upgrade") || "").toLowerCase() !== "websocket") return new Response("WebSocket required", { status: 426 });

    const m = url.pathname.match(/^\/hazrob\/video\/(publish|view)\/([^/]+)$/);
    if (!m) return new Response("Not found", { status: 404 });
    const role = m[1] === "publish" ? "publisher" : "viewer";
    const robotId = decodeURIComponent(m[2]);
    if (!validRobotId(robotId)) return new Response("Invalid robot id", { status: 400 });

    const expected = role === "publisher" ? env.PUBLISH_TOKEN : env.VIEW_TOKEN;
    if (!expected || !safeEq(bearer(request), expected)) return new Response("Unauthorized", { status: 401 });

    const id = env.VIDEO_ROOMS.idFromName(robotId);
    const room = env.VIDEO_ROOMS.get(id);
    const headers = new Headers(request.headers);
    headers.set("X-HazRob-Role", role);
    headers.set("X-HazRob-Robot", robotId);
    return room.fetch(new Request("https://room.invalid/socket", { method: "GET", headers }));
  },
};

export class VideoRoom extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.ctx = ctx;
  }

  async fetch(request) {
    if ((request.headers.get("Upgrade") || "").toLowerCase() !== "websocket") return new Response("WebSocket required", { status: 426 });
    const role = request.headers.get("X-HazRob-Role");
    if (role !== "publisher" && role !== "viewer") return new Response("Invalid role", { status: 400 });

    if (role === "publisher") {
      for (const ws of this.ctx.getWebSockets()) {
        const a = ws.deserializeAttachment() || {};
        if (a.role === "publisher") {
          try { ws.close(4001, "publisher replaced"); } catch (_) {}
        }
      }
    }

    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair);
    server.serializeAttachment({ role, connectedAt: Date.now() });
    this.ctx.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: client });
  }

  webSocketMessage(ws, message) {
    const a = ws.deserializeAttachment() || {};
    if (a.role !== "publisher") return;
    if (typeof message === "string") return;

    const bytes = message instanceof ArrayBuffer ? message.byteLength : message?.byteLength || 0;
    if (bytes < 4 || bytes > MAX_JPEG) return;
    const u8 = message instanceof ArrayBuffer ? new Uint8Array(message) : new Uint8Array(message.buffer, message.byteOffset || 0, bytes);
    if (u8[0] !== 0xff || u8[1] !== 0xd8 || u8[bytes - 2] !== 0xff || u8[bytes - 1] !== 0xd9) return;

    for (const viewer of this.ctx.getWebSockets()) {
      const va = viewer.deserializeAttachment() || {};
      if (va.role !== "viewer" || viewer.readyState !== WebSocket.OPEN) continue;
      if ((viewer.bufferedAmount || 0) > MAX_VIEWER_BUFFER) continue;
      try { viewer.send(message); } catch (_) {}
    }
  }

  webSocketClose(ws, code, reason) {
    try { ws.close(code, reason); } catch (_) {}
  }

  webSocketError(ws) {
    try { ws.close(1011, "socket error"); } catch (_) {}
  }
}
