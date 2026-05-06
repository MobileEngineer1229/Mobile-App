import Phaser from 'phaser';

// ── Constants ─────────────────────────────────────────────────────────────────
const TILE      = 28;    // pixels per tile
const MOVE_MS   = 150;   // ms per tile tween (smooth movement)
const PING_MS   = 2000;  // ping interval
const INPUT_MS  = 50;    // send input every N ms

// Terrain type numbers (must match server TerrainType enum)
const T_GRASS  = 0;
const T_FOREST = 1;
const T_SNOW   = 2;
const T_WATER  = 3;
const T_MTN    = 4;
const T_DEST   = 5;
const T_WALL   = 6;

const PAL: Record<number, { shadow: number; face: number; inner: number }> = {
  [T_GRASS ]: { shadow: 0x3d7a1a, face: 0x5ea32b, inner: 0x6dc235 },
  [T_FOREST]: { shadow: 0x1e5c1e, face: 0x2e7d2e, inner: 0x3d9e3d },
  [T_SNOW  ]: { shadow: 0x8ab4cc, face: 0xb8d8ea, inner: 0xd6edf7 },
  [T_WATER ]: { shadow: 0x1a4e7a, face: 0x1e7fbf, inner: 0x29a0e8 },
  [T_MTN   ]: { shadow: 0x4a4a4a, face: 0x808080, inner: 0xa0a0a0 },
  [T_DEST  ]: { shadow: 0x7a2a08, face: 0xb84118, inner: 0xd45520 },
  [T_WALL  ]: { shadow: 0x222222, face: 0x555555, inner: 0x777777 },
};

const PLAYER_COLORS = [
  0x00e676, 0xff5252, 0x448aff, 0xffea00,
  0xff6d00, 0xe040fb, 0x00bcd4, 0xf06292,
  0x76ff03, 0xff4081, 0x40c4ff, 0xffd740,
];

// Minimap dimensions
const MM_W = 120;
const MM_H = 90;
const MM_PAD = 8;

// ── Types ─────────────────────────────────────────────────────────────────────

interface ServerPlayer {
  id: string;
  username: string;
  x: number;
  y: number;
  hp: number;
  maxHp: number;
  isAlive: boolean;
  kills: number;
  bombsAvailable: number;
  bombsMax: number;
  blastRadius: number;
  teamId: number;
}

interface ServerBomb {
  id: number;
  x: number;
  y: number;
  fuse: number;
  radius: number;
}

interface ServerExplosion {
  cells: { x: number; y: number }[];
}

interface ServerTerrainUpdate {
  position: { x: number; y: number };
  newType: number;
}

interface StateMsg {
  frame: number;
  players: ServerPlayer[];
  bombs: ServerBomb[];
  explosions: ServerExplosion[];
  terrain: ServerTerrainUpdate[];
}

interface MapData {
  width: number;
  height: number;
  tiles: number[][];
}

// ── Rendered entities ─────────────────────────────────────────────────────────

interface RenderedPlayer {
  id: string;
  username: string;
  tileX: number;
  tileY: number;
  colorIdx: number;
  body: Phaser.GameObjects.Arc;
  shadow: Phaser.GameObjects.Arc;
  label: Phaser.GameObjects.Text;
  hpBar: Phaser.GameObjects.Rectangle;
  hpBg: Phaser.GameObjects.Rectangle;
  alive: boolean;
}

interface RenderedBomb {
  id: number;
  circle: Phaser.GameObjects.Arc;
  fuseText: Phaser.GameObjects.Text;
}

// ── Scene ─────────────────────────────────────────────────────────────────────

export class BattleScene extends Phaser.Scene {
  // WebSocket
  private ws: WebSocket | null = null;
  private pingTimer: ReturnType<typeof setInterval> | null = null;
  private inputTimer: ReturnType<typeof setInterval> | null = null;
  private lastPing = 0;
  private pingMs = 0;

  // Session / room data (from scene init)
  private myId = '';
  private myUsername = '';
  private token = '';
  private roomId = '';
  private battleHost = 'localhost';
  private battlePort = 5000;

  // Map
  private mapData: MapData | null = null;
  private tiles: number[][] = [];
  private mapCols = 0;
  private mapRows = 0;
  private faceTiles: Phaser.GameObjects.Rectangle[][] = [];
  private offX = 0;
  private offY = 0;

  // Entities
  private players = new Map<string, RenderedPlayer>();
  private bombs   = new Map<number, RenderedBomb>();
  private explosionGfx!: Phaser.GameObjects.Graphics;
  private colorAssign = new Map<string, number>(); // playerId -> color index
  private colorCounter = 0;

  // Local player state (for input)
  private myTileX = 1;
  private myTileY = 1;
  private myHp = 3;
  private myMaxHp = 3;
  private myBombsAvail = 1;
  private myBombsMax = 1;
  private myBlastR = 2;
  private myAlive = true;

  // Input
  private keyW!: Phaser.Input.Keyboard.Key;
  private keyA!: Phaser.Input.Keyboard.Key;
  private keyS!: Phaser.Input.Keyboard.Key;
  private keyD!: Phaser.Input.Keyboard.Key;
  private keyUp!: Phaser.Input.Keyboard.Key;
  private keyLeft!: Phaser.Input.Keyboard.Key;
  private keyDown!: Phaser.Input.Keyboard.Key;
  private keyRight!: Phaser.Input.Keyboard.Key;
  private keySpace!: Phaser.Input.Keyboard.Key;
  private inputSeq = 0;

  // Tween guard for smooth movement
  private moving = false;

  // HUD
  private hudHp!:     Phaser.GameObjects.Text;
  private hudBombs!:  Phaser.GameObjects.Text;
  private hudRadius!: Phaser.GameObjects.Text;
  private hudAlive!:  Phaser.GameObjects.Text;
  private hudPing!:   Phaser.GameObjects.Text;
  private hudStatus!: Phaser.GameObjects.Text;

  // Leaderboard
  private lbRows: Phaser.GameObjects.Text[] = [];

  // Minimap
  private mmGfx!:  Phaser.GameObjects.Graphics;
  private mmDots!: Phaser.GameObjects.Graphics;
  private mmX = 0;
  private mmY = 0;

  // State flags
  private gameStarted = false;
  private gameOver = false;

  // ── constructor ───────────────────────────────────────────────────────────

  constructor() { super('Battle'); }

  // ── init ─────────────────────────────────────────────────────────────────

  init(data: { room: any; session: any }): void {
    const { room, session } = data;
    this.myId         = session.playerId;
    this.myUsername   = session.username;
    this.token        = session.token;
    this.roomId       = room.roomId;
    this.battleHost   = room.battleServerHost ?? 'localhost';
    this.battlePort   = room.battleServerPort ?? 5000;
  }

  // ── create ────────────────────────────────────────────────────────────────

  create(): void {
    const { width, height } = this.scale;

    // Placeholder dark background while connecting
    this.add.rectangle(width / 2, height / 2, width, height, 0x111111).setScrollFactor(0).setDepth(-20);

    // Explosion graphics layer
    this.explosionGfx = this.add.graphics().setDepth(5);

    // HUD (screen-space)
    this.buildHUD(width, height);

    // Input keys
    const kb = this.input.keyboard!;
    this.keyW     = kb.addKey(Phaser.Input.Keyboard.KeyCodes.W);
    this.keyA     = kb.addKey(Phaser.Input.Keyboard.KeyCodes.A);
    this.keyS     = kb.addKey(Phaser.Input.Keyboard.KeyCodes.S);
    this.keyD     = kb.addKey(Phaser.Input.Keyboard.KeyCodes.D);
    this.keyUp    = kb.addKey(Phaser.Input.Keyboard.KeyCodes.UP);
    this.keyLeft  = kb.addKey(Phaser.Input.Keyboard.KeyCodes.LEFT);
    this.keyDown  = kb.addKey(Phaser.Input.Keyboard.KeyCodes.DOWN);
    this.keyRight = kb.addKey(Phaser.Input.Keyboard.KeyCodes.RIGHT);
    this.keySpace = kb.addKey(Phaser.Input.Keyboard.KeyCodes.SPACE);

    // Show connecting status
    this.hudStatus.setText('Connecting…');

    // Connect WebSocket
    this.connectWS();
  }

  // ── WebSocket ─────────────────────────────────────────────────────────────

  private connectWS(): void {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${protocol}://${this.battleHost}:${this.battlePort}/battle`;

    try {
      this.ws = new WebSocket(url);
    } catch (e) {
      this.hudStatus.setText(`Connection failed: ${url}`);
      return;
    }

    this.ws.onopen = () => {
      this.hudStatus.setText('Authenticating…');
      this.ws!.send(JSON.stringify({
        type:     'JOIN',
        roomId:   this.roomId,
        playerId: this.myId,
        token:    this.token,
        username: this.myUsername,
      }));

      // Ping loop
      this.pingTimer = setInterval(() => {
        if (this.ws?.readyState === WebSocket.OPEN) {
          this.lastPing = Date.now();
          this.ws.send(JSON.stringify({ type: 'PING' }));
        }
      }, PING_MS);

      // Input send loop
      this.inputTimer = setInterval(() => this.sendInput(), INPUT_MS);
    };

    this.ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data as string);
        this.handleServerMsg(msg);
      } catch {}
    };

    this.ws.onerror = () => {
      this.hudStatus.setText('Connection error — server unreachable');
    };

    this.ws.onclose = (ev) => {
      this.clearTimers();
      if (!this.gameOver) {
        this.hudStatus.setText(`Disconnected (${ev.code})`).setVisible(true);
      }
    };
  }

  private handleServerMsg(msg: any): void {
    switch (msg.type) {
      case 'JOINED':    this.onJoined(msg);    break;
      case 'WAITING':   this.onWaiting(msg);   break;
      case 'GAME_START':this.onGameStart(msg); break;
      case 'STATE':     this.onState(msg);     break;
      case 'YOU_DIED':  this.onYouDied();      break;
      case 'GAME_OVER': this.onGameOver(msg);  break;
      case 'PONG':
        this.pingMs = Date.now() - this.lastPing;
        break;
      case 'ERROR':
        this.hudStatus.setText(`Server: ${msg.message}`).setVisible(true);
        break;
    }
  }

  // ── Server message handlers ───────────────────────────────────────────────

  private onJoined(msg: any): void {
    this.hudStatus.setText('Waiting for players…');

    const map: MapData = msg.mapData;
    this.mapData = map;
    this.mapCols = map.width;
    this.mapRows = map.height;
    this.tiles   = map.tiles;

    const spawnX: number = msg.spawnX;
    const spawnY: number = msg.spawnY;
    this.myTileX = spawnX;
    this.myTileY = spawnY;

    this.buildBackground();
    this.buildTiles();
    this.buildMinimap();
    this.setupCamera();
  }

  private onWaiting(msg: any): void {
    this.hudStatus.setText(`Waiting for players… ${msg.playerCount}/${msg.needed}`);
  }

  private onGameStart(msg: any): void {
    this.gameStarted = true;
    this.hudStatus.setVisible(false);
    this.hudAlive.setVisible(true);
  }

  private onState(msg: StateMsg): void {
    if (!this.gameStarted) return;

    // Update terrain
    if (msg.terrain?.length) {
      for (const u of msg.terrain) {
        const { x, y } = u.position;
        if (this.tiles[y]) this.tiles[y][x] = u.newType;
        const rect = this.faceTiles[y]?.[x];
        if (rect) {
          const pal = PAL[u.newType] ?? PAL[T_GRASS];
          rect.setFillStyle(pal.face);
        }
      }
    }

    // Track which player IDs are in this state
    const seenIds = new Set<string>();

    for (const sp of msg.players) {
      seenIds.add(sp.id);

      // Update local player stats
      if (sp.id === this.myId) {
        this.myTileX      = sp.x;
        this.myTileY      = sp.y;
        this.myHp         = sp.hp;
        this.myMaxHp      = sp.maxHp;
        this.myBombsAvail = sp.bombsAvailable;
        this.myBombsMax   = sp.bombsMax;
        this.myBlastR     = sp.blastRadius;
        this.myAlive      = sp.isAlive;
        this.updateHUD();
      }

      // Create or update rendered player
      if (!this.players.has(sp.id)) {
        this.spawnRenderedPlayer(sp);
      }
      this.updateRenderedPlayer(sp);
    }

    // Remove players that left
    for (const [id, rp] of this.players) {
      if (!seenIds.has(id)) {
        rp.body.destroy(); rp.shadow.destroy();
        rp.label.destroy(); rp.hpBar.destroy(); rp.hpBg.destroy();
        this.players.delete(id);
      }
    }

    // Sync bombs
    const seenBombs = new Set<number>();
    for (const sb of msg.bombs) {
      seenBombs.add(sb.id);
      if (!this.bombs.has(sb.id)) this.spawnRenderedBomb(sb);
      this.updateRenderedBomb(sb);
    }
    for (const [id, rb] of this.bombs) {
      if (!seenBombs.has(id)) {
        rb.circle.destroy(); rb.fuseText.destroy();
        this.bombs.delete(id);
      }
    }

    // Render explosions
    this.explosionGfx.clear();
    if (msg.explosions?.length) {
      this.explosionGfx.fillStyle(0xff6600, 0.65);
      for (const exp of msg.explosions) {
        for (const cell of exp.cells) {
          const px = this.offX + cell.x * TILE + 2;
          const py = this.offY + cell.y * TILE + 2;
          this.explosionGfx.fillRect(px, py, TILE - 4, TILE - 4);
        }
      }
    }

    // Move camera to follow local player
    const me = this.players.get(this.myId);
    if (me) {
      this.cameras.main.pan(
        me.body.x, me.body.y, 80, 'Linear', false,
      );
    }

    // Update leaderboard and minimap
    this.updateLeaderboard(msg.players);
    this.updateMinimap(msg.players);
  }

  private onYouDied(): void {
    this.myAlive = false;
    this.hudStatus.setText('💀 You were eliminated! Spectating…').setVisible(true);
  }

  private onGameOver(msg: any): void {
    this.gameOver = true;
    this.clearTimers();

    const winner = this.players.get(msg.winnerId);
    const text = msg.winnerId === this.myId
      ? '🏆 You Win!'
      : `💀 ${winner?.username ?? 'Player ' + msg.winnerId} Wins!`;

    this.hudStatus.setText(text).setVisible(true);

    // Return to lobby after 5 seconds
    this.time.delayedCall(5000, () => {
      this.ws?.close();
      this.scene.start('Lobby');
    });
  }

  // ── Input sender ──────────────────────────────────────────────────────────

  private sendInput(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.gameStarted) return;

    const up    = this.keyW.isDown    || this.keyUp.isDown;
    const down  = this.keyS.isDown    || this.keyDown.isDown;
    const left  = this.keyA.isDown    || this.keyLeft.isDown;
    const right = this.keyD.isDown    || this.keyRight.isDown;
    const bomb  = Phaser.Input.Keyboard.JustDown(this.keySpace);

    // Direction: 0=none, 1=up, 2=down, 3=left, 4=right
    let dir = 0;
    if (up)    dir = 1;
    else if (down)  dir = 2;
    else if (left)  dir = 3;
    else if (right) dir = 4;

    this.ws.send(JSON.stringify({
      type:    'INPUT',
      seq:     ++this.inputSeq,
      dir,
      bomb,
      ability: 0,
      charId:  0,
    }));
  }

  // ── Map rendering ─────────────────────────────────────────────────────────

  private buildBackground(): void {
    const mapW = this.mapCols * TILE;
    const mapH = this.mapRows * TILE;
    const bg = this.add.graphics().setDepth(-10);
    bg.fillStyle(0x87ceeb);
    bg.fillRect(0, 0, mapW, mapH * 0.35);
    bg.fillStyle(0x5b9bd5);
    bg.fillRect(0, mapH * 0.35, mapW, mapH * 0.15);
    bg.fillStyle(0x4a7fa8);
    const peaks = [
      [0, 0.38], [0.1, 0.24], [0.2, 0.35], [0.32, 0.20], [0.45, 0.38],
      [0.58, 0.22], [0.70, 0.36], [0.82, 0.21], [0.93, 0.34], [1, 0.38],
    ];
    bg.beginPath();
    bg.moveTo(0, mapH * 0.5);
    for (const [xf, yf] of peaks) bg.lineTo(mapW * xf, mapH * yf);
    bg.lineTo(mapW, mapH * 0.5);
    bg.closePath();
    bg.fillPath();
    bg.fillStyle(0x4a8a22);
    bg.fillRect(0, mapH * 0.5, mapW, mapH * 0.5);
  }

  private buildTiles(): void {
    this.offX = 0;
    this.offY = 0;
    this.faceTiles = [];

    const gfx = this.add.graphics().setDepth(0);

    for (let row = 0; row < this.mapRows; row++) {
      this.faceTiles[row] = [];
      for (let col = 0; col < this.mapCols; col++) {
        const t   = this.tiles[row]?.[col] ?? T_GRASS;
        const pal = PAL[t] ?? PAL[T_GRASS];
        const px  = this.offX + col * TILE;
        const py  = this.offY + row * TILE;

        // Shadow edge
        gfx.fillStyle(pal.shadow);
        gfx.fillRect(px, py, TILE, TILE);

        // Face
        const rect = this.add.rectangle(px + 2, py + 2, TILE - 2, TILE - 2, pal.face)
          .setOrigin(0, 0).setDepth(1);
        this.faceTiles[row][col] = rect;

        // Highlight (top-left strip)
        gfx.fillStyle(pal.inner, 0.4);
        gfx.fillRect(px + 2, py + 2, TILE - 2, 3);
        gfx.fillRect(px + 2, py + 2, 3, TILE - 2);
      }
    }
  }

  private setupCamera(): void {
    const mapW = this.mapCols * TILE;
    const mapH = this.mapRows * TILE;
    this.cameras.main.setBounds(0, 0, mapW, mapH);
    // Initial pan to spawn
    const sx = this.offX + this.myTileX * TILE + TILE / 2;
    const sy = this.offY + this.myTileY * TILE + TILE / 2;
    this.cameras.main.centerOn(sx, sy);
  }

  // ── Player rendering ──────────────────────────────────────────────────────

  private getColor(playerId: string): number {
    if (!this.colorAssign.has(playerId)) {
      this.colorAssign.set(playerId, this.colorCounter % PLAYER_COLORS.length);
      this.colorCounter++;
    }
    return PLAYER_COLORS[this.colorAssign.get(playerId)!];
  }

  private tileCenter(tx: number, ty: number): { x: number; y: number } {
    return {
      x: this.offX + tx * TILE + TILE / 2,
      y: this.offY + ty * TILE + TILE / 2,
    };
  }

  private spawnRenderedPlayer(sp: ServerPlayer): void {
    const { x: px, y: py } = this.tileCenter(sp.x, sp.y);
    const color = this.getColor(sp.id);
    const r = TILE * 0.38;

    const shadow = this.add.arc(px + 2, py + 3, r, 0, 360, false, 0x000000, 0.35).setDepth(3);
    const body   = this.add.arc(px, py, r, 0, 360, false, color).setDepth(4);
    const label  = this.add.text(px, py - r - 8, sp.username, {
      fontSize: '10px', color: '#ffffff',
      stroke: '#000000', strokeThickness: 3,
    }).setOrigin(0.5, 1).setDepth(6);

    const barW = TILE - 4;
    const hpBg  = this.add.rectangle(px, py + r + 4, barW, 4, 0x333333).setDepth(5);
    const hpBar = this.add.rectangle(px - barW / 2, py + r + 4, barW, 4, 0x00e676)
      .setOrigin(0, 0.5).setDepth(6);

    const rp: RenderedPlayer = {
      id: sp.id, username: sp.username,
      tileX: sp.x, tileY: sp.y,
      colorIdx: this.colorAssign.get(sp.id)!,
      body, shadow, label, hpBar, hpBg, alive: sp.isAlive,
    };
    this.players.set(sp.id, rp);
  }

  private updateRenderedPlayer(sp: ServerPlayer): void {
    const rp = this.players.get(sp.id);
    if (!rp) return;

    const { x: tx, y: ty } = this.tileCenter(sp.x, sp.y);

    if (rp.tileX !== sp.x || rp.tileY !== sp.y) {
      rp.tileX = sp.x;
      rp.tileY = sp.y;

      if (sp.id === this.myId && !this.moving) {
        this.moving = true;
        this.tweens.add({
          targets: [rp.body, rp.shadow, rp.label, rp.hpBar, rp.hpBg],
          x: (obj: any) => {
            if (obj === rp.shadow) return tx + 2;
            if (obj === rp.label) return tx;
            if (obj === rp.hpBg)  return tx;
            if (obj === rp.hpBar) return tx - (TILE - 4) / 2;
            return tx;
          },
          y: (obj: any) => {
            const r = TILE * 0.38;
            if (obj === rp.shadow) return ty + 3;
            if (obj === rp.label) return ty - r - 8;
            if (obj === rp.hpBg)  return ty + r + 4;
            if (obj === rp.hpBar) return ty + r + 4;
            return ty;
          },
          duration: MOVE_MS,
          ease: 'Linear',
          onComplete: () => { this.moving = false; },
        });
      } else {
        // Snap other players
        rp.body.setPosition(tx, ty);
        rp.shadow.setPosition(tx + 2, ty + 3);
        rp.label.setPosition(tx, ty - TILE * 0.38 - 8);
        rp.hpBg.setPosition(tx, ty + TILE * 0.38 + 4);
        rp.hpBar.setPosition(tx - (TILE - 4) / 2, ty + TILE * 0.38 + 4);
      }
    }

    // Alive / dead visual
    rp.alive = sp.isAlive;
    rp.body.setAlpha(sp.isAlive ? 1 : 0.25);
    rp.shadow.setAlpha(sp.isAlive ? 0.35 : 0.1);
    rp.label.setAlpha(sp.isAlive ? 1 : 0.4);

    // HP bar
    const barW = TILE - 4;
    const frac = sp.maxHp > 0 ? Math.max(0, sp.hp / sp.maxHp) : 0;
    rp.hpBar.setDisplaySize(barW * frac, 4);
    const hpColor = frac > 0.5 ? 0x00e676 : frac > 0.25 ? 0xffea00 : 0xff5252;
    rp.hpBar.setFillStyle(hpColor);
  }

  // ── Bomb rendering ────────────────────────────────────────────────────────

  private spawnRenderedBomb(sb: ServerBomb): void {
    const px = this.offX + sb.x * TILE + TILE / 2;
    const py = this.offY + sb.y * TILE + TILE / 2;
    const circle = this.add.arc(px, py, TILE * 0.32, 0, 360, false, 0x111111).setDepth(3);
    const fuseText = this.add.text(px, py, '', {
      fontSize: '9px', color: '#ff4444',
      stroke: '#000', strokeThickness: 2,
    }).setOrigin(0.5).setDepth(4);
    this.bombs.set(sb.id, { id: sb.id, circle, fuseText });
  }

  private updateRenderedBomb(sb: ServerBomb): void {
    const rb = this.bombs.get(sb.id);
    if (!rb) return;
    const frac   = sb.fuse / 60;   // server sends ticks remaining (60 ticks = 3s)
    const blink  = frac < 0.3 && Math.floor(Date.now() / 150) % 2 === 0;
    const color  = frac > 0.5 ? 0x333333 : frac > 0.3 ? 0x994400 : 0xcc2200;
    rb.circle.setFillStyle(blink ? 0xff2200 : color);
    rb.fuseText.setText(`${Math.ceil(sb.fuse / 20)}s`); // approx seconds
  }

  // ── HUD ───────────────────────────────────────────────────────────────────

  private buildHUD(width: number, height: number): void {
    const bar = this.add.rectangle(width / 2, 20, width, 40, 0x000000, 0.7)
      .setScrollFactor(0).setDepth(20);

    const ts = { fontSize: '13px', color: '#ffffff', stroke: '#000', strokeThickness: 2 };
    this.hudHp     = this.add.text(12,  12, '❤️ 3/3',      ts).setScrollFactor(0).setDepth(21);
    this.hudBombs  = this.add.text(120, 12, '💣 1/1',      ts).setScrollFactor(0).setDepth(21);
    this.hudRadius = this.add.text(230, 12, '💥 R:2',      ts).setScrollFactor(0).setDepth(21);
    this.hudAlive  = this.add.text(340, 12, '👥 —',        ts).setScrollFactor(0).setDepth(21).setVisible(false);
    this.hudPing   = this.add.text(width - 80, 12, '📡 —', ts).setScrollFactor(0).setDepth(21);

    // Status overlay (center)
    this.hudStatus = this.add.text(width / 2, height / 2, '', {
      fontSize: '22px', color: '#ffffff',
      stroke: '#000000', strokeThickness: 4,
      backgroundColor: '#00000099',
      padding: { x: 16, y: 10 },
    }).setOrigin(0.5).setScrollFactor(0).setDepth(30);

    // Leaderboard panel (top-right)
    for (let i = 0; i < 10; i++) {
      const row = this.add.text(width - 10, 44 + i * 18, '', {
        fontSize: '11px', color: '#cccccc',
        stroke: '#000000', strokeThickness: 2,
        align: 'right',
      }).setOrigin(1, 0).setScrollFactor(0).setDepth(21);
      this.lbRows.push(row);
    }
  }

  private updateHUD(): void {
    this.hudHp.setText(`❤️ ${this.myHp}/${this.myMaxHp}`);
    this.hudBombs.setText(`💣 ${this.myBombsAvail}/${this.myBombsMax}`);
    this.hudRadius.setText(`💥 R:${this.myBlastR}`);
    this.hudPing.setText(`📡 ${this.pingMs}ms`);
  }

  // ── Leaderboard ───────────────────────────────────────────────────────────

  private updateLeaderboard(players: ServerPlayer[]): void {
    const sorted = [...players]
      .filter(p => p.isAlive)
      .sort((a, b) => b.kills - a.kills || b.hp - a.hp);

    const alive = sorted.length;
    this.hudAlive.setText(`👥 ${alive}`);

    for (let i = 0; i < this.lbRows.length; i++) {
      const row = this.lbRows[i];
      if (i >= sorted.length) { row.setText(''); continue; }
      const p     = sorted[i];
      const medal = i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : `${i + 1}.`;
      const isMe  = p.id === this.myId;
      row.setText(`${medal} ${p.username}  ☠️${p.kills}  ❤️${p.hp}`);
      row.setColor(isMe ? '#ffea00' : '#cccccc');
    }
  }

  // ── Minimap ───────────────────────────────────────────────────────────────

  private buildMinimap(): void {
    const { width, height } = this.scale;
    this.mmX = MM_PAD;
    this.mmY = height - MM_H - MM_PAD - 40; // above HUD bottom

    const bg = this.add.rectangle(
      this.mmX + MM_W / 2, this.mmY + MM_H / 2,
      MM_W + 4, MM_H + 4, 0x000000, 0.7,
    ).setScrollFactor(0).setDepth(22);

    this.mmGfx  = this.add.graphics().setScrollFactor(0).setDepth(23);
    this.mmDots = this.add.graphics().setScrollFactor(0).setDepth(24);

    // Draw static tile map
    if (!this.tiles.length) return;
    const scaleX = MM_W / this.mapCols;
    const scaleY = MM_H / this.mapRows;

    for (let row = 0; row < this.mapRows; row++) {
      for (let col = 0; col < this.mapCols; col++) {
        const t = this.tiles[row]?.[col] ?? T_GRASS;
        const color = t === T_WALL  ? 0x444444
                    : t === T_DEST  ? 0x8b3300
                    : t === T_MTN   ? 0x666666
                    : t === T_WATER ? 0x1a4e7a
                    : t === T_FOREST? 0x1e5c1e
                    : t === T_SNOW  ? 0xaaccdd
                    : 0x3d7a1a;
        this.mmGfx.fillStyle(color);
        this.mmGfx.fillRect(
          this.mmX + col * scaleX, this.mmY + row * scaleY,
          Math.max(1, scaleX), Math.max(1, scaleY),
        );
      }
    }
  }

  private updateMinimap(players: ServerPlayer[]): void {
    if (!this.mmDots) return;
    this.mmDots.clear();

    const scaleX = MM_W / this.mapCols;
    const scaleY = MM_H / this.mapRows;

    // Viewport rectangle
    const cam   = this.cameras.main;
    const vx    = this.mmX + (cam.scrollX / (this.mapCols * TILE)) * MM_W;
    const vy    = this.mmY + (cam.scrollY / (this.mapRows * TILE)) * MM_H;
    const vw    = (cam.width  / (this.mapCols * TILE)) * MM_W;
    const vh    = (cam.height / (this.mapRows * TILE)) * MM_H;
    this.mmDots.lineStyle(1, 0xffffff, 0.5);
    this.mmDots.strokeRect(vx, vy, vw, vh);

    // Player dots
    for (const sp of players) {
      if (!sp.isAlive) continue;
      const color  = sp.id === this.myId ? 0xffea00 : PLAYER_COLORS[this.colorAssign.get(sp.id) ?? 0];
      const dotX   = this.mmX + sp.x * scaleX + scaleX / 2;
      const dotY   = this.mmY + sp.y * scaleY + scaleY / 2;
      this.mmDots.fillStyle(color);
      this.mmDots.fillCircle(dotX, dotY, sp.id === this.myId ? 3 : 2);
    }
  }

  // ── Cleanup ───────────────────────────────────────────────────────────────

  private clearTimers(): void {
    if (this.pingTimer)  { clearInterval(this.pingTimer);  this.pingTimer  = null; }
    if (this.inputTimer) { clearInterval(this.inputTimer); this.inputTimer = null; }
  }

  shutdown(): void {
    this.clearTimers();
    this.ws?.close();
    this.ws = null;
  }

  destroy(): void {
    this.shutdown();
  }
}
