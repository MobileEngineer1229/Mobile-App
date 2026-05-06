import { GameMode } from '../../../shared/types';
import { Logger, withLogging } from '../../../shared/logger';
import type { RoomManager } from './room-manager';
import type { Matchmaker } from './matchmaker';

const log = new Logger('Match');

const CORS = {
  'Content-Type': 'application/json',
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type,Authorization',
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: CORS });
}

const lobbyClients = new Set<any>();

export function broadcastLobby(payload: unknown): void {
  const msg = JSON.stringify(payload);
  let count = 0;
  lobbyClients.forEach(ws => { try { ws.send(msg); count++; } catch {} });
  if (count > 0) log.info('Lobby broadcast', { type: (payload as any).type, recipients: count });
}

export function createMatchApp(rooms: RoomManager, matchmaker: Matchmaker): void {
  const PORT = Number(process.env.PORT ?? 8001);

  async function handler(req: Request, server: any): Promise<Response | undefined> {
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

    const { pathname } = new URL(req.url);

    // WebSocket upgrade
    if (pathname === '/lobby') {
      const ok = server.upgrade(req);
      return ok ? undefined : new Response('WebSocket upgrade failed', { status: 400 });
    }

    try {
      // ── LARGE rooms ────────────────────────────────────────────────────────
      if (req.method === 'GET' && pathname === '/rooms/large') {
        const roomList = await rooms.listLargeRooms();
        log.info('Large rooms listed', { count: roomList.length });
        return json(200, { rooms: roomList });
      }

      if (req.method === 'POST' && pathname === '/rooms/large/join') {
        const { playerId } = await req.json() as any;
        if (!playerId) return json(400, { error: 'playerId required' });
        const room = await matchmaker.joinLarge(playerId);
        log.join(playerId, room.roomId, { map: room.mapId, players: room.playerIds.length });
        return json(200, room);
      }

      // ── CUSTOM rooms ───────────────────────────────────────────────────────
      if (req.method === 'GET' && pathname === '/rooms') {
        const roomList = await rooms.listCustomRooms();
        log.info('Custom rooms listed', { count: roomList.length });
        return json(200, { rooms: roomList });
      }

      if (req.method === 'POST' && pathname === '/rooms') {
        const body = await req.json() as any;
        if (!body.playerId || !body.mapId) return json(400, { error: 'playerId and mapId required' });
        const gameMode = body.gameMode === 'TEAM' ? GameMode.TEAM : GameMode.INDIVIDUAL;
        const room = await rooms.createCustomRoom(body.playerId, body.mapId, Number(body.maxPlayers ?? 4), gameMode);
        log.ok('Custom room created', { roomId: room.roomId, creator: body.playerId, map: body.mapId, max: room.maxPlayers, mode: gameMode });
        broadcastLobby({ type: 'ROOM_CREATED', room });
        return json(201, room);
      }

      const joinM = pathname.match(/^\/rooms\/([^/]+)\/join$/);
      if (req.method === 'POST' && joinM) {
        const { playerId } = await req.json() as any;
        if (!playerId) return json(400, { error: 'playerId required' });
        const room = await rooms.joinRoom(playerId, joinM[1]);
        if (!room) {
          log.warn('Join failed — room full or not found', { playerId, roomId: joinM[1] });
          return json(404, { error: 'Room not found or full' });
        }
        log.join(playerId, room.roomId, { players: room.playerIds.length, max: room.maxPlayers });
        broadcastLobby({ type: 'ROOM_UPDATED', room });
        return json(200, room);
      }

      const startM = pathname.match(/^\/rooms\/([^/]+)\/start$/);
      if (req.method === 'POST' && startM) {
        const { playerId } = await req.json() as any;
        const result = await rooms.startRoom(startM[1], playerId);
        if ('error' in result) {
          log.warn('Start failed', { roomId: startM[1], reason: result.error });
          return json(400, result);
        }
        log.gameStart(result.roomId, result.playerIds.length, result.mapId);
        broadcastLobby({ type: 'MATCH_STARTING', room: result });
        return json(200, result);
      }

      const leaveM = pathname.match(/^\/rooms\/([^/]+)\/leave$/);
      if (req.method === 'DELETE' && leaveM) {
        const { playerId } = await req.json() as any;
        await rooms.leaveRoom(playerId, leaveM[1]);
        log.leave(playerId, leaveM[1]);
        return json(200, { ok: true });
      }

      // ── Matchmaking queue ──────────────────────────────────────────────────
      if (req.method === 'POST' && pathname === '/queue') {
        const body = await req.json() as any;
        const gameMode = body.gameMode === 'TEAM' ? GameMode.TEAM : GameMode.INDIVIDUAL;
        await matchmaker.enqueue(body.playerId, body.mapId, gameMode);
        const qLen = await matchmaker.getQueueLength();
        log.info('Player queued', { playerId: body.playerId, map: body.mapId, mode: gameMode, queueLen: qLen });
        return json(200, { queued: true, queueLength: qLen });
      }

      const deqM = pathname.match(/^\/queue\/([^/]+)$/);
      if (req.method === 'DELETE' && deqM) {
        await matchmaker.dequeue(deqM[1]);
        log.info('Player dequeued', { playerId: deqM[1] });
        return json(200, { ok: true });
      }

      return json(404, { error: 'Not found' });

    } catch (err) {
      log.error(`Handler crashed: ${pathname}`, err);
      return json(500, { error: (err as any)?.message ?? 'Server error' });
    }
  }

  (globalThis as any).Bun.serve({
    port: PORT,
    fetch: withLogging(log, handler),
    websocket: {
      open(ws: any) {
        lobbyClients.add(ws);
        log.connect(`ws#${lobbyClients.size}`, `total=${lobbyClients.size}`);
      },
      message(ws: any, msg: string) {
        try {
          const data = JSON.parse(msg);
          if (data.type === 'IDENTIFY') {
            ws.data = { playerId: data.playerId };
            log.info('WS client identified', { playerId: data.playerId });
          }
        } catch {}
      },
      close(ws: any) {
        lobbyClients.delete(ws);
        log.disconnect(`ws#${ws.data?.playerId ?? '?'}`, `remaining=${lobbyClients.size}`);
      },
    },
  });

  log.start(PORT, { redis: `${process.env.REDIS_HOST ?? 'localhost'}:6379` });
}
