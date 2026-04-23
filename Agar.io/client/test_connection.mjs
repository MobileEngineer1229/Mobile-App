// Node.js end-to-end test: connect, join, send move, verify delta updates position
import protobuf from './node_modules/protobufjs/src/index.js';
import { readFileSync } from 'fs';
import { createRequire } from 'module';

const protoText = readFileSync('./public/proto/agario.proto', 'utf8');
const require = createRequire(import.meta.url);
const WebSocket = require('./node_modules/ws/index.js');

async function test() {
  const root = protobuf.parse(protoText, { keepCase: false }).root;
  const TcpPacket = root.lookupType('agario.TcpPacket');
  const UdpPacket = root.lookupType('agario.UdpPacket');
  const JoinRequest = root.lookupType('agario.JoinRequest');

  // Encode JOIN
  const joinMsg = TcpPacket.create({
    type: TcpPacket.Type.JOIN_REQUEST,
    joinRequest: JoinRequest.create({ name: 'TestBot2', skinId: '', mode: 0, partyCode: '' }),
  });
  const joinBuf = Buffer.from(TcpPacket.encode(joinMsg).finish());

  // Encode MoveInput (0xFE prefix)
  function encodeMoveInput(playerId, seq, mouseX, mouseY) {
    const UdpPkt = root.lookupType('agario.UdpPacket');
    const MI = root.lookupType('agario.MoveInput');
    const msg = UdpPkt.create({
      type: UdpPkt.Type.MOVE_INPUT,
      moveInput: MI.create({ playerId, seq, mouseX, mouseY, split: false, eject: false }),
    });
    const udpBuf = Buffer.from(UdpPkt.encode(msg).finish());
    const wrapped = Buffer.alloc(udpBuf.length + 1);
    wrapped[0] = 0xFE;
    udpBuf.copy(wrapped, 1);
    return wrapped;
  }

  const ws = new WebSocket('ws://localhost:9001');
  ws.binaryType = 'nodebuffer';

  let myPlayerId = null;
  let startPos = null;
  let latestPos = null;
  let seq = 0;
  let gotWelcome = false;
  let gotSnapshot = false;
  let posChanged = false;
  let moveInterval = null;
  const start = Date.now();

  ws.on('open', () => {
    console.log('[Test] WebSocket CONNECTED ✓');
    ws.send(joinBuf);
  });

  ws.on('message', (data) => {
    const bytes = new Uint8Array(data);
    const elapsed = Date.now() - start;

    if (bytes[0] === 0xFD) {
      const pkt = UdpPacket.decode(bytes.slice(1));
      if (pkt.type === 2) {  // FULL_SNAPSHOT
        gotSnapshot = true;
        const snap = pkt.fullSnapshot;
        console.log(`[Test] SNAPSHOT ✓ (${elapsed}ms) players:${snap.players.length} foods:${snap.foods.length}`);
        const me = snap.players.find(p => p.playerId === myPlayerId);
        if (me?.cells?.[0]) {
          startPos = { x: me.cells[0].x, y: me.cells[0].y };
          latestPos = { ...startPos };
          console.log(`[Test]   Start pos: (${startPos.x.toFixed(1)}, ${startPos.y.toFixed(1)}) radius:${me.cells[0].radius.toFixed(1)}`);
          // Send move toward a far target
          const targetX = startPos.x + 5000;
          const targetY = startPos.y;
          console.log(`[Test]   Sending MoveInput to target: (${targetX.toFixed(1)}, ${targetY.toFixed(1)})`);
          moveInterval = setInterval(() => {
            if (myPlayerId) ws.send(encodeMoveInput(myPlayerId, ++seq, targetX, targetY));
          }, 50);
        }
      } else if (pkt.type === 3) {  // DELTA_UPDATE
        const delta = pkt.deltaUpdate;
        const me = delta.changedPlayers?.find(p => p.playerId === myPlayerId);
        if (me?.cells?.[0] && startPos) {
          const newX = me.cells[0].x;
          const newY = me.cells[0].y;
          const moved = Math.abs(newX - startPos.x) + Math.abs(newY - startPos.y);
          if (moved > 0.5 && !posChanged) {
            posChanged = true;
            console.log(`[Test] MOVEMENT CONFIRMED ✓ (${elapsed}ms)`);
            console.log(`[Test]   Old: (${startPos.x.toFixed(1)}, ${startPos.y.toFixed(1)})`);
            console.log(`[Test]   New: (${newX.toFixed(1)}, ${newY.toFixed(1)})`);
            console.log(`[Test]   Delta: ${moved.toFixed(2)} world units`);
            clearInterval(moveInterval);
            ws.close();
          }
          latestPos = { x: newX, y: newY };
        }
      }
    } else {
      const pkt = TcpPacket.decode(bytes);
      if (pkt.type === 2) {  // WELCOME
        gotWelcome = true;
        myPlayerId = pkt.welcome.playerId;
        console.log(`[Test] WELCOME ✓ (${elapsed}ms) playerId:${myPlayerId}`);
      }
    }
  });

  ws.on('error', (e) => console.error('[Test] ERROR:', e.message));
  ws.on('close', () => {
    clearInterval(moveInterval);
    console.log('\n[Test] === Results ===');
    console.log('[Test] WELCOME:', gotWelcome ? '✓' : '✗');
    console.log('[Test] SNAPSHOT:', gotSnapshot ? '✓' : '✗');
    console.log('[Test] MOVEMENT:', posChanged ? '✓' : '✗');
    process.exit(gotWelcome && gotSnapshot && posChanged ? 0 : 1);
  });

  setTimeout(() => {
    console.log('[Test] TIMEOUT');
    clearInterval(moveInterval);
    ws.close();
  }, 5000);
}

test().catch(e => { console.error('[Test] Fatal:', e); process.exit(1); });
