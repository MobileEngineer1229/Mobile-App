// Browser battle transport over WebSocket.
// The battle server accepts JSON messages at /battle; browsers cannot use raw UDP.

export interface GameState {
  frame: number;
  players: PlayerStateMsg[];
  bombs: BombStateMsg[];
  explosions: ExplosionMsg[];
  terrainUpdates: TerrainUpdateMsg[];
}

export interface PlayerStateMsg {
  playerId: string; characterId: number;
  x: number; y: number; facing: number;
  hp: number; maxHp: number;
  bombsAvailable: number; bombsMax: number; blastRadius: number;
  isAlive: boolean;
}

export interface BombStateMsg {
  bombId: number; ownerId: number; x: number; y: number; fuseTicks: number; blastRadius: number;
}

export interface ExplosionMsg {
  bombId: number; cells: { x: number; y: number }[]; hitPlayers: string[];
}

export interface TerrainUpdateMsg {
  position: { x: number; y: number }; newType: number;
}

export type GameStateCallback = (state: GameState) => void;
export type GameOverCallback  = (winnerId: string | null) => void;

export class GameClient {
  private ws: WebSocket | null = null;
  private onState: GameStateCallback;
  private onGameOver: GameOverCallback;

  constructor(onState: GameStateCallback, onGameOver: GameOverCallback) {
    this.onState = onState;
    this.onGameOver = onGameOver;
  }

  connect(host: string, port: number, join?: {
    roomId: string;
    playerId: string;
    token: string;
    username: string;
  }): void {
    this.ws = new WebSocket(`ws://${host}:${port}/battle`);

    this.ws.onopen = () => {
      if (join) this.ws?.send(JSON.stringify({ type: 'JOIN', ...join }));
    };
    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data as string);
        if (msg.type === 'GAME_OVER') {
          this.onGameOver(msg.winnerId);
        } else if (msg.type === 'STATE') {
          this.onState(msg as GameState);
        } else {
          console.log('[GameClient] Server message', msg);
        }
      } catch {}
    };

    this.ws.onclose = () => console.log('[GameClient] Disconnected');
    this.ws.onerror = (e) => console.error('[GameClient] Error', e);
  }

  sendInput(sequence: number, moveDir: number, placeBomb: boolean, abilitySlot: number, characterId: number): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify({
      type: 'INPUT',
      seq: sequence,
      dir: moveDir,
      bomb: placeBomb,
      ability: abilitySlot,
      charId: characterId,
    }));
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
  }
}
