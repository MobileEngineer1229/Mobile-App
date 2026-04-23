import React, { useState } from 'react';
import { GAME_MODE_LABELS, GameMode, COLORS } from '../constants';

const SKINS = [
  { id: '',        label: 'Default' },
  { id: 'earth',   label: 'Earth'   },
  { id: 'mars',    label: 'Mars'    },
  { id: 'doge',    label: 'Doge'    },
  { id: 'reddit',  label: 'Reddit'  },
];

export default function StartScreen({ onPlay }) {
  const [name,       setName]       = useState('');
  const [mode,       setMode]       = useState(GameMode.FFA);
  const [skinId,     setSkinId]     = useState('');
  const [color,      setColor]      = useState(COLORS[0]);
  const [partyCode,  setPartyCode]  = useState('');

  const handlePlay = () => {
    if (!name.trim()) return;
    onPlay({ name: name.trim(), mode, skinId, color, partyCode });
  };

  return (
    <div style={styles.overlay}>
      <div style={styles.card}>
        <h1 style={styles.title}>agar<span style={{ color: '#3b82f6' }}>.io</span></h1>
        <p style={styles.sub}>Eat or be eaten</p>

        <input
          style={styles.input}
          type="text"
          placeholder="Enter your name..."
          maxLength={16}
          value={name}
          onChange={e => setName(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handlePlay()}
        />

        {/* Game Mode */}
        <div style={styles.section}>
          <label style={styles.label}>Mode</label>
          <div style={styles.grid}>
            {Object.entries(GAME_MODE_LABELS).map(([k, v]) => (
              <button
                key={k}
                style={{ ...styles.modeBtn,
                  background: parseInt(k) === mode ? '#3b82f6' : '#1f2937' }}
                onClick={() => setMode(parseInt(k))}
              >
                {v}
              </button>
            ))}
          </div>
        </div>

        {mode === GameMode.PARTY && (
          <input
            style={styles.input}
            type="text"
            placeholder="Party code..."
            value={partyCode}
            onChange={e => setPartyCode(e.target.value)}
          />
        )}

        {/* Skin */}
        <div style={styles.section}>
          <label style={styles.label}>Skin</label>
          <div style={styles.row}>
            {SKINS.map(s => (
              <button
                key={s.id}
                style={{ ...styles.skinBtn,
                  borderColor: s.id === skinId ? '#3b82f6' : 'transparent' }}
                onClick={() => setSkinId(s.id)}
              >
                {s.label}
              </button>
            ))}
          </div>
        </div>

        {/* Color */}
        <div style={styles.section}>
          <label style={styles.label}>Color</label>
          <div style={styles.row}>
            {COLORS.map(c => (
              <button
                key={c}
                style={{ ...styles.colorBtn, background: c,
                  outline: c === color ? '3px solid #fff' : 'none' }}
                onClick={() => setColor(c)}
              />
            ))}
          </div>
        </div>

        <button
          style={{ ...styles.playBtn, opacity: name.trim() ? 1 : 0.5 }}
          onClick={handlePlay}
          disabled={!name.trim()}
        >
          PLAY
        </button>

        {/* Controls reminder */}
        <p style={styles.controls}>
          Move: Mouse &nbsp;|&nbsp; Split: Space &nbsp;|&nbsp; Eject: W
        </p>
      </div>
    </div>
  );
}

const styles = {
  overlay:  { position:'fixed', inset:0, background:'#0f172a',
               display:'flex', alignItems:'center', justifyContent:'center', zIndex:100 },
  card:     { background:'#1e293b', borderRadius:16, padding:'2rem 2.5rem',
               width:420, display:'flex', flexDirection:'column', gap:16 },
  title:    { fontSize:48, fontWeight:900, color:'#f1f5f9', textAlign:'center' },
  sub:      { color:'#64748b', textAlign:'center', marginTop:-12 },
  input:    { background:'#0f172a', border:'1px solid #334155', borderRadius:8,
               padding:'0.65rem 1rem', color:'#f1f5f9', fontSize:16, outline:'none' },
  section:  { display:'flex', flexDirection:'column', gap:8 },
  label:    { color:'#94a3b8', fontSize:13, fontWeight:600 },
  grid:     { display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:6 },
  modeBtn:  { border:'none', borderRadius:6, padding:'0.4rem', color:'#f1f5f9',
               cursor:'pointer', fontSize:12 },
  row:      { display:'flex', gap:8, flexWrap:'wrap' },
  skinBtn:  { background:'#0f172a', border:'2px solid transparent', borderRadius:6,
               padding:'0.3rem 0.7rem', color:'#f1f5f9', cursor:'pointer', fontSize:13 },
  colorBtn: { width:28, height:28, borderRadius:'50%', border:'none', cursor:'pointer' },
  playBtn:  { background:'#3b82f6', border:'none', borderRadius:8, padding:'0.75rem',
               color:'#fff', fontSize:18, fontWeight:700, cursor:'pointer', marginTop:8 },
  controls: { color:'#475569', fontSize:12, textAlign:'center' },
};
