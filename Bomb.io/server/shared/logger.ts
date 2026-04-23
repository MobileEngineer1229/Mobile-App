/**
 * Shared logger — outputs to console (colored ANSI) AND to a rotating daily log file.
 *
 * Usage:
 *   import { Logger, withLogging } from '../../shared/logger';
 *   const log = new Logger('Match');
 *   log.ok('Room created', { roomId: '...' });
 *   Bun.serve({ fetch: withLogging(log, myHandler) });
 *
 * Log files are written to:  server/logs/<service>-YYYY-MM-DD.log  (plain text, no ANSI)
 *
 * Log levels (in order of severity):
 *   HTTP  — every inbound HTTP request/response
 *   DEBUG — low-level internal details
 *   INFO  — normal informational events
 *   OK    — successful key events (join, create, match found…)
 *   AUTH  — authentication events (LOGIN, REGISTER, LOGOUT, FAIL)
 *   WARN  — unexpected but non-fatal situations
 *   ERROR — failures that need attention
 */

import { appendFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';

// ── Log levels ────────────────────────────────────────────────────────────────

export type LogLevel = 'HTTP' | 'DEBUG' | 'INFO' | 'OK' | 'AUTH' | 'WARN' | 'ERROR';

const LEVEL_META: Record<LogLevel, { label: string; color: string; consoleFn: 'log' | 'warn' | 'error' }> = {
  HTTP:  { label: 'HTTP ', color: '\x1b[34m',  consoleFn: 'log'   },  // blue
  DEBUG: { label: 'DEBUG', color: '\x1b[90m',  consoleFn: 'log'   },  // gray
  INFO:  { label: 'INFO ', color: '\x1b[36m',  consoleFn: 'log'   },  // cyan
  OK:    { label: 'OK   ', color: '\x1b[92m',  consoleFn: 'log'   },  // bright green
  AUTH:  { label: 'AUTH ', color: '\x1b[94m',  consoleFn: 'log'   },  // bright blue
  WARN:  { label: 'WARN ', color: '\x1b[93m',  consoleFn: 'warn'  },  // bright yellow
  ERROR: { label: 'ERROR', color: '\x1b[91m',  consoleFn: 'error' },  // bright red
};

// ── ANSI palette ─────────────────────────────────────────────────────────────

const C = {
  reset:    '\x1b[0m',
  bold:     '\x1b[1m',
  dim:      '\x1b[2m',
  red:      '\x1b[31m',
  yellow:   '\x1b[33m',
  magenta:  '\x1b[35m',
  cyan:     '\x1b[36m',
  gray:     '\x1b[90m',
  bRed:     '\x1b[91m',
  bGreen:   '\x1b[92m',
  bYellow:  '\x1b[93m',
  bBlue:    '\x1b[94m',
  bMagenta: '\x1b[95m',
  bCyan:    '\x1b[96m',
  white:    '\x1b[97m',
};

const SERVICE_META: Record<string, { color: string; icon: string }> = {
  Login:  { color: C.bGreen,   icon: '🔐' },
  Match:  { color: C.bYellow,  icon: '🎯' },
  Battle: { color: C.bRed,     icon: '💣' },
  Rank:   { color: C.bMagenta, icon: '🏆' },
};

const HTTP_METHOD_COLORS: Record<string, string> = {
  GET:    C.bGreen,
  POST:   C.bBlue,
  DELETE: C.bRed,
  PATCH:  C.bYellow,
  PUT:    C.bCyan,
};

// ── File sink ─────────────────────────────────────────────────────────────────

const LOG_DIR = join(
  dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Z]:)/, '$1')),
  '..',
  'logs',
);

function logFilePath(service: string): string {
  const date = new Date().toISOString().slice(0, 10);
  return join(LOG_DIR, `${service.toLowerCase()}-${date}.log`);
}

let logDirReady = false;
function ensureLogDir(): void {
  if (logDirReady) return;
  try { mkdirSync(LOG_DIR, { recursive: true }); } catch {}
  logDirReady = true;
}

function stripAnsi(s: string): string {
  return s.replace(/\x1b\[[0-9;]*m/g, '');
}

// ── Timestamp ─────────────────────────────────────────────────────────────────

function tsConsole(): string {
  const d  = new Date();
  const hh = d.getHours().toString().padStart(2, '0');
  const mm = d.getMinutes().toString().padStart(2, '0');
  const ss = d.getSeconds().toString().padStart(2, '0');
  const ms = d.getMilliseconds().toString().padStart(3, '0');
  return `${C.gray}${hh}:${mm}:${ss}.${ms}${C.reset}`;
}

function tsFile(): string {
  return new Date().toISOString().replace('T', ' ').slice(0, 23);
}

// ── Data formatter ────────────────────────────────────────────────────────────

function fmtData(data: Record<string, unknown>): string {
  return Object.entries(data)
    .map(([k, v]) => `${k}=${JSON.stringify(v)}`)
    .join('  ');
}

// ── Core emit ─────────────────────────────────────────────────────────────────

function statusColor(code: number): string {
  if (code < 300) return C.bGreen;
  if (code < 400) return C.bCyan;
  if (code < 500) return C.bYellow;
  return C.bRed;
}

function statusEmoji(code: number): string {
  if (code < 300) return '✅';
  if (code < 400) return '↪️ ';
  if (code < 500) return '⚠️ ';
  return '❌';
}

// ── Logger class ──────────────────────────────────────────────────────────────

export class Logger {
  private serviceTag: string;   // colored  e.g.  🔐 [Login]
  private svcLabel:   string;   // plain    e.g.  [Login]

  constructor(private service: string) {
    const meta      = SERVICE_META[service] ?? { color: C.white, icon: '⚙️ ' };
    this.serviceTag = `${meta.color}${C.bold}${meta.icon} [${service}]${C.reset}`;
    this.svcLabel   = `[${service}]`;
  }

  // ── Core emit ──────────────────────────────────────────────────────────────

  private emit(level: LogLevel, coloredMsg: string, plainMsg: string): void {
    const lm = LEVEL_META[level];

    // Console line:  HH:MM:SS.mmm  🔐 [Login]  [LEVEL]  <message>
    const levelTag = `${lm.color}${C.bold}[${lm.label}]${C.reset}`;
    console[lm.consoleFn](`${tsConsole()} ${this.serviceTag}  ${levelTag}  ${coloredMsg}`);

    // File line:     2026-04-18 14:30:22.123  [Login]  [LEVEL]  <message>  (no ANSI)
    const fileLine = `${tsFile()}  ${this.svcLabel}  [${lm.label.trim()}]  ${stripAnsi(plainMsg)}`;
    try {
      ensureLogDir();
      appendFileSync(logFilePath(this.service), fileLine + '\n', 'utf8');
    } catch { /* never crash due to log write */ }
  }

  // ── Public API ─────────────────────────────────────────────────────────────

  /** Every HTTP request/response */
  req(method: string, path: string, status: number, ms: number, note?: string): void {
    const mc    = HTTP_METHOD_COLORS[method.toUpperCase()] ?? C.white;
    const sc    = statusColor(status);
    const note_ = note ? `  ${C.gray}› ${note}${C.reset}` : '';
    this.emit(
      'HTTP',
      `${statusEmoji(status)} ${mc}${method.padEnd(6)}${C.reset} ${C.white}${path}${C.reset}  ${sc}${status}${C.reset}  ${C.dim}${ms}ms${C.reset}${note_}`,
      `${statusEmoji(status)} ${method.padEnd(6)} ${path}  ${status}  ${ms}ms${note ? '  > ' + note : ''}`,
    );
  }

  /** Service started */
  start(port: number, extra?: Record<string, unknown>): void {
    const ex  = extra ? `  ${C.gray}${fmtData(extra)}${C.reset}` : '';
    const pex = extra ? `  ${fmtData(extra)}` : '';
    this.emit('INFO',
      `🚀 ${C.bold}Server ready on :${port}${C.reset}${ex}`,
      `🚀 Server ready on :${port}${pex}`,
    );
  }

  /** Informational */
  info(msg: string, data?: Record<string, unknown>): void {
    const d  = data ? `  ${C.gray}${fmtData(data)}${C.reset}` : '';
    const pd = data ? `  ${fmtData(data)}` : '';
    this.emit('INFO', `📋 ${C.cyan}${msg}${C.reset}${d}`, `📋 ${msg}${pd}`);
  }

  /** Debug (low-level details) */
  debug(msg: string, data?: Record<string, unknown>): void {
    const d  = data ? `  ${C.gray}${fmtData(data)}${C.reset}` : '';
    const pd = data ? `  ${fmtData(data)}` : '';
    this.emit('DEBUG', `🔍 ${C.gray}${msg}${C.reset}${d}`, `🔍 ${msg}${pd}`);
  }

  /** Success / key positive event */
  ok(msg: string, data?: Record<string, unknown>): void {
    const d  = data ? `  ${C.gray}${fmtData(data)}${C.reset}` : '';
    const pd = data ? `  ${fmtData(data)}` : '';
    this.emit('OK', `🎉 ${C.bGreen}${msg}${C.reset}${d}`, `🎉 ${msg}${pd}`);
  }

  /** Auth events */
  auth(action: 'REGISTER' | 'LOGIN' | 'LOGOUT' | 'FAIL', username: string, detail?: string): void {
    const icons:  Record<string, string> = { REGISTER: '📝', LOGIN: '🔑', LOGOUT: '👋', FAIL: '🚫' };
    const colors: Record<string, string> = { REGISTER: C.bGreen, LOGIN: C.bCyan, LOGOUT: C.gray, FAIL: C.bRed };
    const d  = detail ? `  ${C.gray}› ${detail}${C.reset}` : '';
    const pd = detail ? `  > ${detail}` : '';
    this.emit('AUTH',
      `${icons[action]} ${colors[action]}${action}${C.reset}  user=${C.white}${username}${C.reset}${d}`,
      `${icons[action]} ${action}  user=${username}${pd}`,
    );
  }

  /** Player joined a room */
  join(playerId: number | string, roomId: string, extra?: Record<string, unknown>): void {
    const d  = extra ? `  ${C.gray}${fmtData(extra)}${C.reset}` : '';
    const pd = extra ? `  ${fmtData(extra)}` : '';
    this.emit('OK',
      `👤 ${C.bGreen}JOIN${C.reset}  player=${C.cyan}${playerId}${C.reset}  room=${C.bYellow}${roomId}${C.reset}${d}`,
      `👤 JOIN  player=${playerId}  room=${roomId}${pd}`,
    );
  }

  /** Player left */
  leave(playerId: number | string, roomId: string): void {
    this.emit('INFO',
      `🚪 ${C.yellow}LEAVE${C.reset}  player=${C.cyan}${playerId}${C.reset}  room=${C.gray}${roomId}${C.reset}`,
      `🚪 LEAVE  player=${playerId}  room=${roomId}`,
    );
  }

  /** Game started */
  gameStart(roomId: string, playerCount: number, mapId?: string): void {
    const map_ = mapId ? `  map=${C.magenta}${mapId}${C.reset}` : '';
    const pmap = mapId ? `  map=${mapId}` : '';
    this.emit('OK',
      `💥 ${C.bRed}GAME START${C.reset}  room=${C.bYellow}${roomId}${C.reset}  players=${C.bGreen}${playerCount}${C.reset}${map_}`,
      `💥 GAME START  room=${roomId}  players=${playerCount}${pmap}`,
    );
  }

  /** WebSocket / UDP connect */
  connect(id: string | number, detail?: string): void {
    const d  = detail ? `  ${C.gray}(${detail})${C.reset}` : '';
    const pd = detail ? `  (${detail})` : '';
    this.emit('INFO',
      `🔗 ${C.bCyan}CONNECT${C.reset}  ${C.cyan}${id}${C.reset}${d}`,
      `🔗 CONNECT  ${id}${pd}`,
    );
  }

  /** WebSocket / UDP disconnect */
  disconnect(id: string | number, detail?: string): void {
    const d  = detail ? `  ${C.gray}(${detail})${C.reset}` : '';
    const pd = detail ? `  (${detail})` : '';
    this.emit('INFO',
      `🔌 ${C.gray}DISCONNECT${C.reset}  ${C.gray}${id}${C.reset}${d}`,
      `🔌 DISCONNECT  ${id}${pd}`,
    );
  }

  /** Warning */
  warn(msg: string, data?: Record<string, unknown>): void {
    const d  = data ? `  ${C.gray}${fmtData(data)}${C.reset}` : '';
    const pd = data ? `  ${fmtData(data)}` : '';
    this.emit('WARN', `⚠️  ${C.bYellow}${msg}${C.reset}${d}`, `⚠️  ${msg}${pd}`);
  }

  /** Error */
  error(msg: string, err?: unknown): void {
    const e  = err instanceof Error ? ` → ${err.message}` : err ? ` → ${String(err)}` : '';
    this.emit('ERROR', `💀 ${C.bRed}${msg}${e}${C.reset}`, `💀 ${msg}${e}`);
  }

  /** Bomb event (battle server) */
  bomb(event: 'PLACE' | 'EXPLODE' | 'CHAIN', data: Record<string, unknown>): void {
    const icons = { PLACE: '🧨', EXPLODE: '💥', CHAIN: '⛓️ ' };
    const plain = fmtData(data);
    this.emit('INFO',
      `${icons[event]} ${C.bRed}BOMB:${event}${C.reset}  ${C.gray}${plain}${C.reset}`,
      `${icons[event]} BOMB:${event}  ${plain}`,
    );
  }

  /** Kill event */
  kill(killer: string | number, victim: string | number, roomId: string): void {
    this.emit('OK',
      `☠️  ${C.bRed}KILL${C.reset}  ${C.cyan}${killer}${C.reset} → ${C.gray}${victim}${C.reset}  room=${C.bYellow}${roomId}${C.reset}`,
      `☠️  KILL  ${killer} → ${victim}  room=${roomId}`,
    );
  }

  /** ELO / rank change */
  rank(playerId: string, elo: number, delta: number): void {
    const sign = delta >= 0 ? `+${delta}` : `${delta}`;
    const col  = delta >= 0 ? C.bGreen : C.bRed;
    this.emit('INFO',
      `📊 ${C.bMagenta}ELO${C.reset}  player=${C.cyan}${playerId}${C.reset}  elo=${C.white}${elo}${C.reset}  ${col}${sign}${C.reset}`,
      `📊 ELO  player=${playerId}  elo=${elo}  ${sign}`,
    );
  }
}

// ── Middleware ────────────────────────────────────────────────────────────────

/**
 * Wraps a Bun.serve fetch handler to auto-log every HTTP request/response.
 *
 *   Bun.serve({ fetch: withLogging(log, myHandler) })
 */
export function withLogging(
  logger: Logger,
  handler: (req: Request, server: any) => Promise<Response | undefined>,
): (req: Request, server: any) => Promise<Response | undefined> {
  return async (req, server) => {
    if (req.method === 'OPTIONS') return handler(req, server);
    const t0 = Date.now();
    const { pathname } = new URL(req.url);
    try {
      const res    = await handler(req, server);
      const status = res?.status ?? 101;
      logger.req(req.method, pathname, status, Date.now() - t0);
      return res;
    } catch (err) {
      logger.error(`Unhandled ${req.method} ${pathname}`, err);
      logger.req(req.method, pathname, 500, Date.now() - t0);
      throw err;
    }
  };
}
