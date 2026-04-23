// Browser UDP simulation over WebSocket relay
// A thin relay WebSocket server on the battle server echoes UDP frames as binary WS messages.

export interface GameState {
  frame: number;
  players: PlayerStateMsg[];
  bombs: BombStateMsg[];
  explosions: ExplosionMsg[];
  terrainUpdates: TerrainUpdateMsg[];
}

export interface PlayerStateMsg {
  playerId: number; characterId: number;
  x: number; y: number; facing: number;
  hp: number; maxHp: number;
  bombsAvailable: number; bombsMax: number; blastRadius: number;
  isAlive: boolean;
}

export interface BombStateMsg {
  bombId: number; ownerId: number; x: number; y: number; fuseTicks: number; blastRadius: number;
}

export interface ExplosionMsg {
  bombId: number; cells: { x: number; y: number }[]; hitPlayers: number[];
}

export interface TerrainUpdateMsg {
  position: { x: number; y: number }; newType: number;
}

export type GameStateCallback = (state: GameState) => void;
export type GameOverCallback  = (winnerId: number | null) => void;

export class GameClient {
  private ws: WebSocket | null = null;
  private onState: GameStateCallback;
  private onGameOver: GameOverCallback;

  constructor(onState: GameStateCallback, onGameOver: GameOverCallback) {
    this.onState = onState;
    this.onGameOver = onGameOver;
  }

  connect(host: string, port: number): void {
    this.ws = new WebSocket(`ws://${host}:${port}/ws`);
    this.ws.binaryType = 'arraybuffer';

    this.ws.onmessage = (event) => {
      try {
        const text = typeof event.data === 'string'
          ? event.data
          : new TextDecoder().decode(event.data);
        const msg = JSON.parse(text);
        if (msg.type === 'GAME_OVER') {
          this.onGameOver(msg.winnerId);
        } else {
          this.onState(msg as GameState);
        }
      } catch {}
    };

    this.ws.onclose = () => console.log('[GameClient] Disconnected');
    this.ws.onerror = (e) => console.error('[GameClient] Error', e);
  }

  sendInput(sequence: number, moveDir: number, placeBomb: boolean, abilitySlot: number, characterId: number): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    const buf = new ArrayBuffer(7);
    const view = new DataView(buf);
    view.setUint16(0, sequence);
    view.setUint8(2, placeBomb ? 0x01 : 0x00);
    view.setUint8(3, moveDir);
    view.setUint8(4, abilitySlot);
    view.setUint16(5, characterId);
    this.ws.send(buf);
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
  }
}
