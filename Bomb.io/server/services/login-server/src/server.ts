import { AuthHandler } from './auth-handler';
import { CharacterManager } from './character-manager';
import { Logger, withLogging } from '../../../shared/logger';

const CORS = {
  'Content-Type': 'application/json',
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,PATCH,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type,Authorization',
};

const log = new Logger('Login');

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: CORS });
}
function bearer(req: Request): string {
  return req.headers.get('authorization')?.replace(/^Bearer\s+/i, '') ?? '';
}

export function createLoginApp(redis: any): void {
  const PORT  = Number(process.env.PORT ?? 8000);
  const auth  = new AuthHandler(redis);
  const chars = new CharacterManager();

  async function handler(req: Request): Promise<Response> {
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

    const { pathname } = new URL(req.url);

    try {
      // ── Auth ──────────────────────────────────────────────────────────────
      if (req.method === 'POST' && pathname === '/register') {
        const { username, password, email } = await req.json();
        if (!username || !password) return json(400, { error: 'username and password required' });
        const r = await auth.register(username, password, email);
        if ('error' in r) {
          log.auth('FAIL', username, r.error);
          return json(409, r);
        }
        log.auth('REGISTER', username, `playerId=${r.playerId}`);
        return json(201, r);
      }

      if (req.method === 'POST' && pathname === '/login') {
        const { username, password } = await req.json();
        const r = await auth.login(username, password);
        if ('error' in r) {
          log.auth('FAIL', username, r.error);
          return json(401, r);
        }
        log.auth('LOGIN', r.username, `playerId=${r.playerId}`);
        return json(200, r);
      }

      if (req.method === 'POST' && pathname === '/logout') {
        const token = bearer(req);
        await auth.logout(token);
        log.auth('LOGOUT', '(token)', 'session invalidated');
        return json(200, { ok: true });
      }

      if (req.method === 'GET' && pathname === '/validate') {
        const payload = await auth.validateToken(bearer(req));
        if (!payload) {
          log.warn('Token validation failed');
          return json(401, { error: 'Invalid or expired token' });
        }
        log.info('Token valid', { user: payload.username });
        return json(200, payload);
      }

      // ── Characters ────────────────────────────────────────────────────────
      if (req.method === 'GET' && pathname === '/characters') {
        const payload = await auth.validateToken(bearer(req));
        if (!payload) return json(401, { error: 'Unauthorized' });
        const characters = await chars.getCharacters(payload.playerId);
        log.info('Characters fetched', { user: payload.username, count: characters.length });
        return json(200, { characters });
      }

      if (req.method === 'GET' && pathname === '/characters/templates') {
        const templates = await chars.getTemplates();
        log.info('Templates fetched', { count: templates.length });
        return json(200, { templates });
      }

      if (req.method === 'POST' && pathname === '/characters/unlock') {
        const payload = await auth.validateToken(bearer(req));
        if (!payload) return json(401, { error: 'Unauthorized' });
        const { templateId } = await req.json();
        const r = await chars.unlockCharacter(payload.playerId, templateId);
        if ('error' in r) { log.warn('Unlock failed', { user: payload.username, templateId }); return json(400, r); }
        log.ok('Character unlocked', { user: payload.username, templateId });
        return json(201, r);
      }

      const xpM = pathname.match(/^\/characters\/([^/]+)\/xp$/);
      if (req.method === 'POST' && xpM) {
        const payload = await auth.validateToken(bearer(req));
        if (!payload) return json(401, { error: 'Unauthorized' });
        const { xp } = await req.json();
        const updated = await chars.addExperience(payload.playerId, xpM[1], Number(xp));
        if (!updated) return json(404, { error: 'Character not found' });
        log.ok('XP added', { user: payload.username, charId: xpM[1], xp });
        return json(200, updated);
      }

      const cosM = pathname.match(/^\/characters\/([^/]+)\/cosmetic$/);
      if (req.method === 'PATCH' && cosM) {
        const payload = await auth.validateToken(bearer(req));
        if (!payload) return json(401, { error: 'Unauthorized' });
        const { field, value } = await req.json();
        const updated = await chars.applyCosmetic(payload.playerId, cosM[1], field, value);
        if (!updated) return json(404, { error: 'Character not found' });
        log.ok('Cosmetic applied', { user: payload.username, charId: cosM[1], field });
        return json(200, updated);
      }

      return json(404, { error: 'Not found' });

    } catch (err) {
      log.error(`Handler crashed: ${pathname}`, err);
      return json(500, { error: 'Server error' });
    }
  }

  (globalThis as any).Bun.serve({
    port: PORT,
    fetch: withLogging(log, async (req) => handler(req)),
  });

  log.start(PORT, { mongo: process.env.MONGO_URI ?? 'mongodb://localhost/bombio', redis: `${process.env.REDIS_HOST ?? 'localhost'}:6379` });
}
