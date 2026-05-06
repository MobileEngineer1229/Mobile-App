import Phaser from 'phaser';
import { api } from '../net/ApiClient';
import { useSession } from '../state/session';

const MAP_COLORS: Record<string, string> = {
  'volcano-isle':  '#e53935',
  'frozen-tundra': '#1e88e5',
  'desert-ruins':  '#fb8c00',
  'jungle-maze':   '#43a047',
  'city-ruins':    '#8e24aa',
};
const MAP_ICONS: Record<string, string> = {
  'volcano-isle':  '🌋',
  'frozen-tundra': '❄️',
  'desert-ruins':  '🏜️',
  'jungle-maze':   '🌿',
  'city-ruins':    '🏙️',
};
const MAPS = ['volcano-isle', 'frozen-tundra', 'desert-ruins', 'jungle-maze', 'city-ruins'];

interface RoomInfo {
  roomId: string; type: string; status: string; gameMode: string;
  playerIds: string[]; maxPlayers: number; mapId: string;
  creatorPlayerId: string | null;
}

// ── CSS injected once ─────────────────────────────────────────────────────────
const CSS = `
  .lb-overlay {
    position:fixed; inset:0; background:#0d1117; display:flex;
    flex-direction:column; font-family:'Segoe UI',system-ui,sans-serif;
    color:#eceff1; z-index:999; overflow:hidden;
  }
  /* Header */
  .lb-header {
    display:flex; align-items:center; padding:0 20px; height:54px;
    background:#161b22; border-bottom:1px solid #21262d; flex-shrink:0;
  }
  .lb-logo { font-size:18px; font-weight:800; color:#ff5722; letter-spacing:1px; }
  .lb-title { flex:1; text-align:center; font-size:13px; letter-spacing:4px; color:#546e7a; font-weight:600; }
  .lb-user  { text-align:right; }
  .lb-username { font-size:13px; color:#eceff1; }
  .lb-logout {
    display:block; margin-top:2px; font-size:11px; color:#546e7a; cursor:pointer;
    background:none; border:none; padding:0; font-family:inherit;
  }
  .lb-logout:hover { color:#ff5252; }

  /* Body scroll area */
  .lb-body {
    flex:1; overflow-y:auto; padding:16px 20px 20px;
    scrollbar-width:thin; scrollbar-color:#21262d #0d1117;
  }

  /* Section headers */
  .lb-section-header {
    display:flex; align-items:center; justify-content:space-between;
    margin-bottom:10px;
  }
  .lb-section-title { font-size:11px; letter-spacing:3px; font-weight:700; }
  .lb-section-meta  { font-size:10px; color:#37474f; }
  .lb-divider       { height:1px; background:#21262d; margin:16px 0; }

  /* Room cards */
  .lb-card {
    display:flex; align-items:center;
    background:#161b22; border-radius:8px;
    border:1px solid #21262d; margin-bottom:8px;
    padding:0 14px 0 0; overflow:hidden; min-height:54px;
  }
  .lb-card-accent { width:4px; border-radius:8px 0 0 8px; align-self:stretch; flex-shrink:0; }
  .lb-card-body   { flex:1; padding:10px 12px; min-width:0; }
  .lb-card-name   { font-size:13px; font-weight:700; color:#eceff1; margin-bottom:3px; }
  .lb-card-meta   { font-size:10px; color:#546e7a; }
  .lb-card-right  { display:flex; align-items:center; gap:12px; flex-shrink:0; }
  .lb-count {
    font-size:12px; font-weight:600; color:#78909c;
    background:#0d1117; border-radius:4px; padding:3px 8px;
    white-space:nowrap;
  }

  /* Buttons */
  .lb-btn {
    display:inline-flex; align-items:center; justify-content:center;
    border:none; border-radius:6px; padding:7px 18px;
    font-size:12px; font-weight:700; letter-spacing:1px;
    cursor:pointer; font-family:inherit; transition:filter .15s,transform .1s;
    white-space:nowrap;
  }
  .lb-btn:hover  { filter:brightness(1.2); transform:scale(1.03); }
  .lb-btn:active { transform:scale(0.97); }
  .lb-btn-join   { color:#fff; }
  .lb-btn-start  { background:#ff5722; color:#fff; }
  .lb-btn-waiting { background:#21262d; color:#546e7a; cursor:default; }
  .lb-btn-waiting:hover { filter:none; transform:none; }

  /* Create room button */
  .lb-create-btn {
    font-size:12px; color:#ff5722; background:none; border:1px solid #ff5722;
    border-radius:6px; padding:5px 14px; cursor:pointer; font-family:inherit;
    font-weight:700; letter-spacing:1px; transition:background .15s;
  }
  .lb-create-btn:hover { background:#ff572220; }

  /* Empty state */
  .lb-empty { text-align:center; color:#37474f; font-size:13px; padding:24px 0; }

  /* Modal backdrop */
  .lb-modal-backdrop {
    position:fixed; inset:0; background:#000000b0; z-index:1100;
    display:flex; align-items:center; justify-content:center;
  }
  .lb-modal {
    background:#161b22; border:1px solid #ff572250; border-radius:12px;
    padding:28px 32px; width:340px; max-width:90vw;
  }
  .lb-modal h2 {
    margin:0 0 20px; font-size:15px; color:#ff5722;
    letter-spacing:2px; text-align:center;
  }
  .lb-field { margin-bottom:16px; }
  .lb-label { font-size:10px; color:#546e7a; letter-spacing:2px; display:block; margin-bottom:6px; }
  .lb-input, .lb-select-box {
    width:100%; padding:9px 12px; background:#0d1117;
    border:1px solid #263238; border-radius:6px; color:#eceff1;
    font-size:13px; font-family:inherit; box-sizing:border-box; outline:none;
  }
  .lb-input:focus, .lb-select-box:focus { border-color:#ff5722; }
  .lb-select-box { cursor:pointer; }

  /* Mode toggle */
  .lb-toggle { display:flex; gap:8px; }
  .lb-toggle-opt {
    flex:1; padding:8px; border-radius:6px; text-align:center;
    font-size:12px; font-weight:700; cursor:pointer; border:1px solid #263238;
    background:transparent; color:#546e7a; font-family:inherit; transition:all .15s;
  }
  .lb-toggle-opt.active { border-color:#ff5722; color:#fff; background:#ff572225; }

  /* Modal actions */
  .lb-modal-actions { display:flex; gap:10px; margin-top:20px; }
  .lb-btn-full { flex:1; padding:11px; font-size:13px; }
  .lb-btn-cancel { background:#21262d; color:#eceff1; }

  /* Loading state */
  .lb-loading { text-align:center; padding:40px; color:#37474f; font-size:13px; }
`;

function injectCSS(): void {
  if (document.getElementById('lb-style')) return;
  const style = document.createElement('style');
  style.id = 'lb-style';
  style.textContent = CSS;
  document.head.appendChild(style);
}

// ── Scene ─────────────────────────────────────────────────────────────────────
export class LobbyScene extends Phaser.Scene {
  private root?: HTMLElement;
  private refreshTimer?: Phaser.Time.TimerEvent;

  constructor() { super('Lobby'); }

  async create(): Promise<void> {
    const session = useSession.get();
    if (!session) { this.scene.start('Login'); return; }

    injectCSS();

    // Hide the Phaser canvas while lobby is shown (avoid canvas eating pointer events)
    const canvas = this.game.canvas;
    canvas.style.visibility = 'hidden';

    this.root = document.createElement('div');
    this.root.className = 'lb-overlay';
    document.body.appendChild(this.root);

    await this.render(session);

    // Auto-refresh custom rooms every 5 s
    this.refreshTimer = this.time.addEvent({
      delay: 5000, loop: true,
      callback: () => this.refreshCustomSection(session),
    });
  }

  // ── Full render ───────────────────────────────────────────────────────────

  private async render(session: any): Promise<void> {
    if (!this.root) return;
    this.root.innerHTML = `<div class="lb-loading">Loading rooms…</div>`;

    const [largeRes, customRes] = await Promise.all([
      api.match.listLargeRooms().catch(() => ({ rooms: [] })),
      api.match.listCustomRooms().catch(() => ({ rooms: [] })),
    ]);

    const largeRooms: RoomInfo[]  = largeRes.rooms ?? [];
    const customRooms: RoomInfo[] = customRes.rooms ?? [];

    this.root.innerHTML = `
      ${this.headerHTML(session)}
      <div class="lb-body" id="lb-body">
        ${this.largeRoomsHTML(largeRooms)}
        <div class="lb-divider"></div>
        ${this.customRoomsHTML(customRooms, session)}
      </div>
    `;

    this.attachHeaderEvents(session);
    this.attachLargeRoomEvents(largeRooms, session);
    this.attachCustomRoomEvents(customRooms, session);
  }

  // ── HTML builders ─────────────────────────────────────────────────────────

  private headerHTML(session: any): string {
    return `
      <header class="lb-header">
        <span class="lb-logo">💣 BOMB.IO</span>
        <span class="lb-title">LOBBY</span>
        <div class="lb-user">
          <div class="lb-username">${escHtml(session.username)}</div>
          <button class="lb-logout" id="lb-logout">Logout</button>
        </div>
      </header>
    `;
  }

  private largeRoomsHTML(rooms: RoomInfo[]): string {
    const cards = MAPS.map((mapId, i) => {
      const room    = rooms.find(r => r.mapId === mapId);
      const count   = room?.playerIds?.length ?? 0;
      const color   = MAP_COLORS[mapId] ?? '#546e7a';
      const icon    = MAP_ICONS[mapId]  ?? '🗺️';
      return `
        <div class="lb-card">
          <div class="lb-card-accent" style="background:${color}"></div>
          <div class="lb-card-body">
            <div class="lb-card-name">${icon} ${mapId}</div>
            <div class="lb-card-meta">Individual · 500 players max</div>
          </div>
          <div class="lb-card-right">
            <span class="lb-count">${count}/500</span>
            <button class="lb-btn lb-btn-join" data-large="${i}"
              style="background:${color}20; color:${color}; border:1.5px solid ${color}60;">
              JOIN
            </button>
          </div>
        </div>`;
    }).join('');

    return `
      <div class="lb-section-header">
        <span class="lb-section-title" style="color:#ff5722">SERVER ROOMS</span>
        <span class="lb-section-meta">500 players max · Individual mode</span>
      </div>
      ${cards}
    `;
  }

  private customRoomsHTML(rooms: RoomInfo[], session: any): string {
    let cardsHTML = '';
    if (!rooms.length) {
      cardsHTML = `<div class="lb-empty">No custom rooms yet — create one!</div>`;
    } else {
      cardsHTML = rooms.slice(0, 8).map((room, i) => {
        const isMine  = String(room.creatorPlayerId) === String(session.playerId);
        const canStart = isMine && room.playerIds.length >= 2;
        const modeLabel = room.gameMode === 'TEAM' ? '⚔️ Team' : '👤 Individual';
        const startBtn  = canStart
          ? `<button class="lb-btn lb-btn-start" data-start="${i}">START</button>` : '';
        const joinEl = isMine
          ? `<span class="lb-btn lb-btn-waiting">WAITING</span>`
          : `<button class="lb-btn lb-btn-join" data-join="${i}"
               style="background:#1e88e520; color:#1e88e5; border:1.5px solid #1e88e560;">JOIN</button>`;

        return `
          <div class="lb-card" ${isMine ? 'style="border-color:#ff572230;"' : ''}>
            <div class="lb-card-accent" style="background:${isMine ? '#ff5722' : '#1e88e5'}"></div>
            <div class="lb-card-body">
              <div class="lb-card-name">${MAP_ICONS[room.mapId] ?? '🗺️'} ${room.mapId}</div>
              <div class="lb-card-meta">${modeLabel} · ${room.playerIds.length}/${room.maxPlayers} players</div>
            </div>
            <div class="lb-card-right">
              ${startBtn}
              ${joinEl}
            </div>
          </div>`;
      }).join('');
    }

    return `
      <div class="lb-section-header">
        <span class="lb-section-title" style="color:#1e88e5">CUSTOM ROOMS</span>
        <button class="lb-create-btn" id="lb-create">+ Create Room</button>
      </div>
      <div id="lb-custom-rooms">${cardsHTML}</div>
    `;
  }

  // ── Event wiring ──────────────────────────────────────────────────────────

  private attachHeaderEvents(session: any): void {
    document.getElementById('lb-logout')?.addEventListener('click', () => {
      useSession.clear();
      this.leave();
      this.scene.start('Login');
    });
    document.getElementById('lb-create')?.addEventListener('click', () => {
      this.showCreateModal(session);
    });
  }

  private attachLargeRoomEvents(rooms: RoomInfo[], session: any): void {
    this.root?.querySelectorAll('[data-large]').forEach(btn => {
      btn.addEventListener('click', async () => {
        (btn as HTMLButtonElement).disabled = true;
        (btn as HTMLButtonElement).textContent = '…';
        const result = await api.match.joinLargeRoom(session.playerId, session.token).catch(() => null);
        if (result?.roomId) {
          this.leave();
          this.scene.start('Battle', { room: result, session });
        } else {
          (btn as HTMLButtonElement).disabled = false;
          (btn as HTMLButtonElement).textContent = 'JOIN';
        }
      });
    });
  }

  private attachCustomRoomEvents(rooms: RoomInfo[], session: any): void {
    this.root?.querySelectorAll('[data-join]').forEach(btn => {
      const idx = Number((btn as HTMLElement).dataset.join);
      btn.addEventListener('click', async () => {
        const room = rooms[idx];
        if (!room) return;
        (btn as HTMLButtonElement).disabled = true;
        (btn as HTMLButtonElement).textContent = '…';
        const result = await api.match.joinRoom(room.roomId, session.playerId, session.token).catch(() => null);
        if (result?.roomId) {
          this.leave();
          this.scene.start('Battle', { room: result, session });
        } else {
          (btn as HTMLButtonElement).disabled = false;
          (btn as HTMLButtonElement).textContent = 'JOIN';
        }
      });
    });

    this.root?.querySelectorAll('[data-start]').forEach(btn => {
      const idx = Number((btn as HTMLElement).dataset.start);
      btn.addEventListener('click', async () => {
        const room = rooms[idx];
        if (!room) return;
        await api.match.startRoom(room.roomId, session.playerId, session.token);
        this.leave();
        this.scene.start('Battle', { room, session });
      });
    });
  }

  // ── Custom rooms refresh ──────────────────────────────────────────────────

  private async refreshCustomSection(session: any): Promise<void> {
    const container = document.getElementById('lb-custom-rooms');
    if (!container) return;
    const { rooms: _rooms = [] } = await api.match.listCustomRooms().catch(() => ({ rooms: [] }));
    const rooms: RoomInfo[] = _rooms;
    container.innerHTML = this.customRoomsInnerHTML(rooms, session);
    this.attachCustomRoomEvents(rooms, session);

    // Re-wire create button (header stays intact)
    document.getElementById('lb-create')?.addEventListener('click', () => {
      this.showCreateModal(session);
    });
  }

  private customRoomsInnerHTML(rooms: RoomInfo[], session: any): string {
    if (!rooms.length) return `<div class="lb-empty">No custom rooms yet — create one!</div>`;
    return rooms.slice(0, 8).map((room, i) => {
      const isMine   = String(room.creatorPlayerId) === String(session.playerId);
      const canStart = isMine && room.playerIds.length >= 2;
      const modeLabel = room.gameMode === 'TEAM' ? '⚔️ Team' : '👤 Individual';
      const startBtn  = canStart
        ? `<button class="lb-btn lb-btn-start" data-start="${i}">START</button>` : '';
      const joinEl = isMine
        ? `<span class="lb-btn lb-btn-waiting">WAITING</span>`
        : `<button class="lb-btn lb-btn-join" data-join="${i}"
             style="background:#1e88e520; color:#1e88e5; border:1.5px solid #1e88e560;">JOIN</button>`;
      return `
        <div class="lb-card" ${isMine ? 'style="border-color:#ff572230;"' : ''}>
          <div class="lb-card-accent" style="background:${isMine ? '#ff5722' : '#1e88e5'}"></div>
          <div class="lb-card-body">
            <div class="lb-card-name">${MAP_ICONS[room.mapId] ?? '🗺️'} ${room.mapId}</div>
            <div class="lb-card-meta">${modeLabel} · ${room.playerIds.length}/${room.maxPlayers} players</div>
          </div>
          <div class="lb-card-right">${startBtn}${joinEl}</div>
        </div>`;
    }).join('');
  }

  // ── Create Room Modal ─────────────────────────────────────────────────────

  private showCreateModal(session: any): void {
    const mapOptions = MAPS.map(m =>
      `<option value="${m}">${MAP_ICONS[m] ?? ''} ${m}</option>`
    ).join('');

    const backdrop = document.createElement('div');
    backdrop.className = 'lb-modal-backdrop';
    backdrop.innerHTML = `
      <div class="lb-modal" id="lb-modal-inner">
        <h2>CREATE ROOM</h2>
        <div class="lb-field">
          <label class="lb-label">MAP</label>
          <select class="lb-input lb-select-box" id="modal-map">${mapOptions}</select>
        </div>
        <div class="lb-field">
          <label class="lb-label">MAX PLAYERS (2–20)</label>
          <input class="lb-input" id="modal-max" type="number" min="2" max="20" value="8" placeholder="Max players"/>
        </div>
        <div class="lb-field">
          <label class="lb-label">MODE</label>
          <div class="lb-toggle">
            <button class="lb-toggle-opt active" data-mode="INDIVIDUAL">👤 Individual</button>
            <button class="lb-toggle-opt"        data-mode="TEAM">⚔️ Team</button>
          </div>
        </div>
        <div id="modal-status" style="color:#ff1744; font-size:11px; text-align:center; min-height:16px;"></div>
        <div class="lb-modal-actions">
          <button class="lb-btn lb-btn-cancel lb-btn-full" id="modal-cancel">Cancel</button>
          <button class="lb-btn lb-btn-start  lb-btn-full" id="modal-create">CREATE</button>
        </div>
      </div>
    `;

    document.body.appendChild(backdrop);

    // Mode toggle
    let gameMode = 'INDIVIDUAL';
    backdrop.querySelectorAll('.lb-toggle-opt').forEach(btn => {
      btn.addEventListener('click', () => {
        backdrop.querySelectorAll('.lb-toggle-opt').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        gameMode = (btn as HTMLElement).dataset.mode ?? 'INDIVIDUAL';
      });
    });

    const close = () => backdrop.remove();

    document.getElementById('modal-cancel')?.addEventListener('click', close);
    backdrop.addEventListener('click', e => { if (e.target === backdrop) close(); });

    document.getElementById('modal-create')?.addEventListener('click', async () => {
      const mapId  = (document.getElementById('modal-map') as HTMLSelectElement).value;
      const maxStr = (document.getElementById('modal-max') as HTMLInputElement).value;
      const maxP   = parseInt(maxStr) || 8;
      const status = document.getElementById('modal-status')!;

      if (maxP < 2 || maxP > 20) { status.textContent = 'Max players must be 2–20'; return; }

      const createBtn = document.getElementById('modal-create') as HTMLButtonElement;
      createBtn.disabled = true; createBtn.textContent = '…';

      const result = await api.match.createCustomRoom(
        session.playerId, mapId, maxP, gameMode, session.token,
      ).catch(() => null);

      if (result?.error) {
        status.textContent = result.error;
        createBtn.disabled = false; createBtn.textContent = 'CREATE';
        return;
      }

      close();
      await this.refreshCustomSection(session);
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  private leave(): void {
    this.root?.remove();
    this.root = undefined;
    this.refreshTimer?.destroy();
    // Restore canvas visibility
    this.game.canvas.style.visibility = '';
  }

  shutdown(): void { this.leave(); }
  destroy():  void { this.leave(); }
}

function escHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
