// ─────────────────────────────────────────────────────────────
//  Canvas Renderer for Agar.io
//  All drawing is on a 2D canvas.
//  Camera follows the centroid of the local player's cells.
// ─────────────────────────────────────────────────────────────
import { WORLD_W, WORLD_H, GRID_LINE_SIZE } from '../constants';

let _lastLogTick = 0;

export function renderFrame(ctx, state, myPlayerId) {
  const { players, foods, viruses, safeZone } = state;
  const canvas = ctx.canvas;
  const W = canvas.width, H = canvas.height;

  // Debug log every 2 seconds
  const now = Date.now();
  if (now - _lastLogTick > 2000) {
    _lastLogTick = now;
    const myP = players.find(p => p.playerId === myPlayerId);
    console.log('[Render] myPlayerId:', myPlayerId,
      '| players:', players.length,
      '| foods:', foods.length,
      '| myPlayer:', myP ? `cells=${myP.cells?.length} mass=${myP.totalMass}` : 'NOT FOUND');
    if (myP?.cells?.[0]) {
      const c = myP.cells[0];
      console.log('[Cell0] x:', c.x, 'y:', c.y, 'radius:', c.radius, 'mass:', c.mass);
    }
  }

  // Find own cells for camera
  const myPlayer = players.find(p => p.playerId === myPlayerId);
  let camX = WORLD_W / 2, camY = WORLD_H / 2, zoom = 1;

  if (myPlayer && myPlayer.cells.length) {
    let cx = 0, cy = 0;
    for (const c of myPlayer.cells) { cx += c.x; cy += c.y; }
    cx /= myPlayer.cells.length;
    cy /= myPlayer.cells.length;
    camX = cx; camY = cy;

    // Zoom out as player grows (Agar.io formula)
    const maxR = Math.max(...myPlayer.cells.map(c => c.radius));
    zoom = Math.min(1, Math.max(0.15, 64 / maxR));
  }

  ctx.save();
  ctx.clearRect(0, 0, W, H);

  // Background
  ctx.fillStyle = '#111827';
  ctx.fillRect(0, 0, W, H);

  // Camera transform
  ctx.translate(W / 2, H / 2);
  ctx.scale(zoom, zoom);
  ctx.translate(-camX, -camY);

  // ── Grid ─────────────────────────────────────────────────
  drawGrid(ctx, camX, camY, W, H, zoom);

  // ── World border ─────────────────────────────────────────
  ctx.strokeStyle = '#374151';
  ctx.lineWidth   = 8;
  ctx.strokeRect(0, 0, WORLD_W, WORLD_H);

  // ── Battle Royale safe zone ───────────────────────────────
  if (safeZone) {
    ctx.save();
    ctx.strokeStyle = 'rgba(239,68,68,0.6)';
    ctx.lineWidth   = 10;
    ctx.beginPath();
    ctx.arc(safeZone.centerX, safeZone.centerY, safeZone.radius, 0, Math.PI * 2);
    ctx.stroke();
    // Darken outside
    ctx.fillStyle = 'rgba(239,68,68,0.08)';
    ctx.fill();
    ctx.restore();
  }

  // ── Foods ─────────────────────────────────────────────────
  for (const f of foods) {
    ctx.beginPath();
    ctx.arc(f.x, f.y, f.radius || 6, 0, Math.PI * 2);
    ctx.fillStyle = argbToRgba(f.color) || '#00cc44';
    ctx.fill();
  }

  // ── Viruses ───────────────────────────────────────────────
  for (const v of viruses) {
    drawVirus(ctx, v);
  }

  // ── Players ───────────────────────────────────────────────
  // Sort: smaller cells drawn under larger
  const allCells = [];
  for (const p of players) {
    for (const c of p.cells)
      allCells.push({ ...c, color: argbToRgba(p.color), name: p.name,
                      isMe: p.playerId === myPlayerId, skinId: p.skinId });
  }
  allCells.sort((a, b) => a.radius - b.radius);

  for (const c of allCells) {
    drawCell(ctx, c);
  }

  ctx.restore();
}

// ── Draw grid ─────────────────────────────────────────────────
function drawGrid(ctx, camX, camY, W, H, zoom) {
  const step = GRID_LINE_SIZE;
  const vw   = W / zoom, vh = H / zoom;
  const startX = Math.floor((camX - vw / 2) / step) * step;
  const startY = Math.floor((camY - vh / 2) / step) * step;

  ctx.save();
  ctx.strokeStyle = 'rgba(255,255,255,0.04)';
  ctx.lineWidth   = 1 / zoom;

  for (let x = startX; x < camX + vw / 2 + step; x += step) {
    ctx.beginPath();
    ctx.moveTo(x, camY - vh / 2 - step);
    ctx.lineTo(x, camY + vh / 2 + step);
    ctx.stroke();
  }
  for (let y = startY; y < camY + vh / 2 + step; y += step) {
    ctx.beginPath();
    ctx.moveTo(camX - vw / 2 - step, y);
    ctx.lineTo(camX + vw / 2 + step, y);
    ctx.stroke();
  }
  ctx.restore();
}

// ── Draw a player cell ────────────────────────────────────────
function drawCell(ctx, c) {
  const { x, y, radius, color, name, isMe } = c;

  // Shadow glow for own cell
  if (isMe) {
    ctx.save();
    ctx.shadowColor  = color;
    ctx.shadowBlur   = 20;
  }

  ctx.beginPath();
  ctx.arc(x, y, radius, 0, Math.PI * 2);
  ctx.fillStyle = color;
  ctx.fill();

  // Border
  ctx.strokeStyle = isMe ? '#fff' : 'rgba(0,0,0,0.3)';
  ctx.lineWidth   = Math.max(2, radius * 0.03);
  ctx.stroke();

  if (isMe) ctx.restore();

  // Name label (only if radius is big enough)
  if (radius > 18 && name) {
    ctx.save();
    const fontSize = Math.max(12, Math.min(radius * 0.35, 48));
    ctx.font        = `bold ${fontSize}px 'Segoe UI', sans-serif`;
    ctx.textAlign   = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle   = 'rgba(255,255,255,0.9)';
    ctx.strokeStyle = 'rgba(0,0,0,0.7)';
    ctx.lineWidth   = fontSize * 0.15;
    ctx.strokeText(name, x, y);
    ctx.fillText(name, x, y);
    ctx.restore();
  }
}

// ── Draw a virus ──────────────────────────────────────────────
function drawVirus(ctx, v) {
  const { x, y, radius, isRed } = v;
  const color  = isRed ? '#ff4444' : '#33ff33';
  const spikes = 24;

  ctx.save();
  ctx.beginPath();
  for (let i = 0; i < spikes * 2; i++) {
    const angle = (i / (spikes * 2)) * Math.PI * 2 - Math.PI / 2;
    const r     = i % 2 === 0 ? radius * 1.15 : radius * 0.85;
    const px    = x + Math.cos(angle) * r;
    const py    = y + Math.sin(angle) * r;
    i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py);
  }
  ctx.closePath();
  ctx.fillStyle   = color + '33';  // semi-transparent fill
  ctx.strokeStyle = color;
  ctx.lineWidth   = 3;
  ctx.fill();
  ctx.stroke();
  ctx.restore();
}

// ── Utilities ─────────────────────────────────────────────────
function argbToRgba(argb) {
  if (!argb) return '#3498db';
  const a = ((argb >>> 24) & 0xff) / 255;
  const r = (argb >>> 16) & 0xff;
  const g = (argb >>> 8)  & 0xff;
  const b =  argb         & 0xff;
  return `rgba(${r},${g},${b},${a})`;
}

// Convert canvas pixel coords to world coords
export function canvasToWorld(canvasX, canvasY, canvas, camX, camY, zoom) {
  const rect = canvas.getBoundingClientRect();
  const cx   = canvasX - rect.left  - canvas.width  / 2;
  const cy   = canvasY - rect.top   - canvas.height / 2;
  return {
    x: camX + cx / zoom,
    y: camY + cy / zoom,
  };
}
