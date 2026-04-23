Optimized Node.js Setup for Your Game
Since you're building this now, here's the modern, high-performance Node.js stack I'd recommend:

1. Use Bun Instead of Node.js (3-4x faster for UDP)
typescript
// battle-service/src/udp-server.ts using Bun
import { createSocket } from 'bun';

export class UDPServer {
  private socket: ReturnType<typeof createSocket>;
  
  constructor(port: number) {
    // Bun's UDP socket is significantly faster than Node.js dgram
    this.socket = createSocket({
      type: 'udp4',
      port: port,
      hostname: '0.0.0.0'
    });
    
    this.socket.on('data', (data, addr) => {
      this.handleMessage(data, addr);
    });
  }
  
  // Bun handles binary data more efficiently
  private handleMessage(data: Buffer, addr: any): void {
    // Process message
  }
}
2. Use uWebSockets.js for TCP (Not Socket.io)
typescript
// map-service/src/server.ts
import uWS from 'uWebSockets.js';

const app = uWS.App({})
  .ws('/*', {
    // 8x faster than Socket.io
    compression: uWS.SHARED_COMPRESSOR,
    maxPayloadLength: 16 * 1024,
    idleTimeout: 32,
    
    open: (ws) => {
      console.log('Player connected');
    },
    
    message: (ws, message, isBinary) => {
      // Handle binary messages for efficiency
      const view = new DataView(message);
      const opCode = view.getUint8(0);
      // Process...
    }
  })
  .listen(8080, (token) => {
    if (token) {
      console.log('Map server listening on port 8080');
    }
  });
3. Collision Detection Optimization (Critical!)
This is where Node.js can struggle if done wrong. Here's the correct approach:

typescript
// shared/utils/SpatialGrid.ts
export class SpatialGrid {
  private grid: Map<string, Set<string>> = new Map();
  private cellSize: number;
  
  constructor(cellSize: number = 500) {
    this.cellSize = cellSize;
  }
  
  // O(1) insertion
  insert(entity: Entity): void {
    const cells = this.getCellsForEntity(entity);
    cells.forEach(cellKey => {
      if (!this.grid.has(cellKey)) {
        this.grid.set(cellKey, new Set());
      }
      this.grid.get(cellKey)!.add(entity.id);
    });
  }
  
  // O(1) lookup - only checks nearby cells
  getNearbyEntities(x: number, y: number, radius: number): Set<string> {
    const nearby = new Set<string>();
    const startCellX = Math.floor((x - radius) / this.cellSize);
    const endCellX = Math.floor((x + radius) / this.cellSize);
    const startCellY = Math.floor((y - radius) / this.cellSize);
    const endCellY = Math.floor((y + radius) / this.cellSize);
    
    for (let cx = startCellX; cx <= endCellX; cx++) {
      for (let cy = startCellY; cy <= endCellY; cy++) {
        const cellKey = `${cx}:${cy}`;
        const cellEntities = this.grid.get(cellKey);
        if (cellEntities) {
          cellEntities.forEach(id => nearby.add(id));
        }
      }
    }
    
    return nearby;
  }
}
4. Worker Threads for Heavy Computation
typescript
// battle-service/src/worker-pool.ts
import { Worker } from 'worker_threads';

export class CollisionWorkerPool {
  private workers: Worker[] = [];
  private taskQueue: any[] = [];
  private activeWorkers = 0;
  
  constructor(workerCount: number = 4) {
    for (let i = 0; i < workerCount; i++) {
      const worker = new Worker('./collision-worker.js');
      worker.on('message', (result) => {
        this.activeWorkers--;
        this.processQueue();
        // Handle collision results
      });
      this.workers.push(worker);
    }
  }
  
  // Offload collision detection to separate threads
  checkCollisions(entities: Entity[]): Promise<CollisionResult[]> {
    return new Promise((resolve) => {
      this.taskQueue.push({ entities, resolve });
      this.processQueue();
    });
  }
}


I am going to make 2 types of rooms
1. large maps(no limit player, always created by server: game mode individual => but same team not kill)
2. user can create rooms with map(set the player_count, mode: individual or team work (divide into 2 teams randomly))

implement above room logic