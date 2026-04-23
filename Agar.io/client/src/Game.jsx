import React, { useEffect, useRef, useCallback } from 'react';
import { renderFrame, canvasToWorld }   from './utils/renderer';
import { useTcpSocket }                  from './hooks/useTcpSocket';
import { useGame }                       from './hooks/useGame';
import Leaderboard                       from './components/Leaderboard';
import HUD                               from './components/HUD';
import GameOver                          from './components/GameOver';

export default function Game({ config, onBack }) {
  const canvasRef = useRef(null);
  const mouseRef  = useRef({ x: 0, y: 0 });
  const keysRef   = useRef({ split: false, eject: false });
  const seqRef    = useRef(0);
  const animRef   = useRef(null);
  const [death,   setDeath]   = React.useState(null);
  const [connMsg, setConnMsg] = React.useState('Connecting...');

  const {
    state, onWelcome, onDeath, onLeaderboard,
    onSnapshot, onDelta, onSafeZone, reset,
  } = useGame();

  // TCP-only mode: game data (snapshots/deltas) goes over WebSocket.
  const tcpRef = useRef(null);

  // Stable death handler
  const onDeathTcp = useCallback((d) => setDeath(d), []);

  // Clear overlay on first snapshot (player is visible at that point)
  const onSnapshotWrapped = useCallback((snap) => {
    setConnMsg(null);
    onSnapshot(snap);
  }, [onSnapshot]);

  const tcp = useTcpSocket({
    onWelcome,
    onDeath:    onDeathTcp,
    onLeaderboard,
    onSnapshot: onSnapshotWrapped,
    onDelta,
  });

  // Keep a ref so the stable game-loop callback can always reach tcp
  useEffect(() => { tcpRef.current = tcp; }, [tcp]);

  // ── Connect on mount ─────────────────────────────────────
  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setConnMsg('Connecting to server...');
        console.log('[Game] Connecting to server...');
        await tcp.connect();
        if (!active) return;
        setConnMsg('Joining game...');
        console.log('[Game] Connected. Sending JOIN:', config.name);
        await tcp.sendJoin(config.name, config.skinId, config.mode);
        if (!active) return;
        setConnMsg('Loading world...');
      } catch (e) {
        if (active) {
          const msg = e?.message || String(e);
          setConnMsg('Connection failed: ' + msg);
          console.error('[Game] Connect failed:', e);
        }
      }
    })();
    return () => {
      active = false;
      tcpRef.current?.disconnect();
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Input capture ─────────────────────────────────────────
  useEffect(() => {
    const onMouseMove = (e) => {
      mouseRef.current = { x: e.clientX, y: e.clientY };
    };
    const onKeyDown = (e) => {
      if (e.code === 'Space') { e.preventDefault(); keysRef.current.split = true; }
      if (e.code === 'KeyW')  keysRef.current.eject = true;
    };
    const onKeyUp = (e) => {
      if (e.code === 'Space') keysRef.current.split = false;
      if (e.code === 'KeyW')  keysRef.current.eject = false;
    };

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('keydown',   onKeyDown);
    window.addEventListener('keyup',     onKeyUp);
    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('keydown',   onKeyDown);
      window.removeEventListener('keyup',     onKeyUp);
    };
  }, []);

  // ── Game loop: send input + render ────────────────────────
  const stateRef = React.useRef(state);
  stateRef.current = state;

  const loop = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const s   = stateRef.current;

    const myPlayer = s.players.find(p => p.playerId === s.myPlayerId);
    let camX = 7071, camY = 7071, zoom = 1;
    if (myPlayer?.cells?.length) {
      let cx = 0, cy = 0;
      for (const c of myPlayer.cells) { cx += c.x; cy += c.y; }
      cx /= myPlayer.cells.length;
      cy /= myPlayer.cells.length;
      camX = cx; camY = cy;
      const maxR = Math.max(...myPlayer.cells.map(c => c.radius || 1));
      zoom = Math.min(1, Math.max(0.15, 64 / maxR));
    }

    const world = canvasToWorld(
      mouseRef.current.x, mouseRef.current.y,
      canvas, camX, camY, zoom
    );

    if (s.myPlayerId) {
      tcpRef.current?.sendMoveInput(
        s.myPlayerId, world.x, world.y,
        keysRef.current.split,
        keysRef.current.eject,
        ++seqRef.current,
      );
      keysRef.current.split = false;
    }

    renderFrame(ctx, s, s.myPlayerId);
    animRef.current = requestAnimationFrame(loop);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const resize = () => {
      canvas.width  = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener('resize', resize);

    animRef.current = requestAnimationFrame(loop);
    return () => {
      cancelAnimationFrame(animRef.current);
      window.removeEventListener('resize', resize);
    };
  }, [loop]);

  // ── Render ────────────────────────────────────────────────
  return (
    <div style={{ width:'100vw', height:'100vh', position:'relative' }}>
      <canvas ref={canvasRef} style={{ display:'block' }} />

      {/* Connection status overlay — hidden once welcome received */}
      {connMsg && (
        <div style={{
          position:'absolute', top:'50%', left:'50%',
          transform:'translate(-50%,-50%)',
          background:'rgba(0,0,0,0.75)', color:'#fff',
          padding:'1rem 2rem', borderRadius:8,
          fontSize:16, textAlign:'center', zIndex:50,
          border: connMsg.startsWith('Connection failed') ? '1px solid #f87171' : 'none',
        }}>
          {connMsg}
          {connMsg.startsWith('Connection failed') && (
            <div style={{ marginTop:8 }}>
              <button
                style={{ background:'#3b82f6', border:'none', borderRadius:6,
                          color:'#fff', padding:'0.5rem 1rem', cursor:'pointer' }}
                onClick={onBack}
              >
                Back to Menu
              </button>
            </div>
          )}
        </div>
      )}

      <Leaderboard
        entries={state.leaderboard}
        myRank={state.leaderboard.find(e => e.playerId === state.myPlayerId)?.rank}
        myMass={state.myMass}
      />

      <HUD
        mass={state.myMass}
        level={state.myLevel}
        xp={state.myXp}
        xpToNext={state.myXpToNext}
      />

      {death && (
        <GameOver
          death={death}
          onReplay={() => { setDeath(null); reset(); }}
          onMenu={onBack}
        />
      )}
    </div>
  );
}
