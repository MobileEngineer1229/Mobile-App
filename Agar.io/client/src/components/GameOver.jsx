import React from 'react';

export default function GameOver({ death, onReplay, onMenu }) {
  const { killerName, finalScore, rank, xpGained, levelUp, newLevel } = death || {};

  return (
    <div style={styles.overlay}>
      <div style={styles.card}>
        <h2 style={styles.title}>You were eaten!</h2>

        {killerName && (
          <p style={styles.killer}>
            by <span style={{ color:'#ef4444' }}>{killerName}</span>
          </p>
        )}

        <div style={styles.stats}>
          <div style={styles.stat}>
            <span style={styles.statVal}>{finalScore || 0}</span>
            <span style={styles.statLbl}>Score</span>
          </div>
          {rank > 0 && (
            <div style={styles.stat}>
              <span style={styles.statVal}>#{rank}</span>
              <span style={styles.statLbl}>Rank</span>
            </div>
          )}
          {xpGained > 0 && (
            <div style={styles.stat}>
              <span style={{ ...styles.statVal, color:'#8b5cf6' }}>+{xpGained}</span>
              <span style={styles.statLbl}>XP</span>
            </div>
          )}
        </div>

        {levelUp && (
          <div style={styles.levelUp}>
            Level Up! → <strong>Lv {newLevel}</strong>
          </div>
        )}

        <div style={styles.buttons}>
          <button style={styles.replayBtn} onClick={onReplay}>Play Again</button>
          <button style={styles.menuBtn}   onClick={onMenu}>Menu</button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  overlay:  { position:'fixed', inset:0, background:'rgba(0,0,0,0.7)',
               display:'flex', alignItems:'center', justifyContent:'center', zIndex:200 },
  card:     { background:'#1e293b', borderRadius:16, padding:'2rem 2.5rem',
               textAlign:'center', display:'flex', flexDirection:'column',
               alignItems:'center', gap:16, minWidth:320 },
  title:    { color:'#ef4444', fontSize:32, fontWeight:800 },
  killer:   { color:'#94a3b8', fontSize:18 },
  stats:    { display:'flex', gap:24 },
  stat:     { display:'flex', flexDirection:'column', alignItems:'center', gap:4 },
  statVal:  { color:'#f1f5f9', fontSize:28, fontWeight:700 },
  statLbl:  { color:'#64748b', fontSize:12 },
  levelUp:  { background:'rgba(139,92,246,0.2)', color:'#a78bfa',
               borderRadius:8, padding:'0.4rem 1rem', fontSize:14 },
  buttons:  { display:'flex', gap:12 },
  replayBtn:{ background:'#3b82f6', border:'none', borderRadius:8, padding:'0.6rem 1.5rem',
               color:'#fff', fontSize:16, fontWeight:700, cursor:'pointer' },
  menuBtn:  { background:'#334155', border:'none', borderRadius:8, padding:'0.6rem 1.5rem',
               color:'#f1f5f9', fontSize:16, cursor:'pointer' },
};
