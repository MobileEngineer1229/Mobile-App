import React from 'react';

export default function HUD({ mass, level, xp, xpToNext, mode }) {
  const pct = xpToNext > 0 ? Math.min(100, (xp / xpToNext) * 100) : 100;

  return (
    <div style={styles.hud}>
      {/* Mass */}
      <div style={styles.massBox}>
        <span style={styles.massVal}>{Math.floor(mass)}</span>
        <span style={styles.massLbl}>mass</span>
      </div>

      {/* Level + XP bar */}
      <div style={styles.xpBox}>
        <div style={styles.xpRow}>
          <span style={styles.lvl}>Lv {level}</span>
          <span style={styles.xpTxt}>{xp} / {xpToNext} XP</span>
        </div>
        <div style={styles.xpTrack}>
          <div style={{ ...styles.xpFill, width: `${pct}%` }} />
        </div>
      </div>

      {/* Mode badge */}
      <div style={styles.modeBadge}>{mode || 'FFA'}</div>
    </div>
  );
}

const styles = {
  hud:      { position:'fixed', bottom:16, left:'50%', transform:'translateX(-50%)',
               display:'flex', alignItems:'center', gap:16, zIndex:50,
               background:'rgba(15,23,42,0.8)', backdropFilter:'blur(4px)',
               borderRadius:12, padding:'0.5rem 1.25rem' },
  massBox:  { display:'flex', flexDirection:'column', alignItems:'center' },
  massVal:  { color:'#f1f5f9', fontSize:22, fontWeight:700 },
  massLbl:  { color:'#64748b', fontSize:11 },
  xpBox:    { display:'flex', flexDirection:'column', gap:4, minWidth:160 },
  xpRow:    { display:'flex', justifyContent:'space-between' },
  lvl:      { color:'#3b82f6', fontWeight:700, fontSize:13 },
  xpTxt:    { color:'#64748b', fontSize:11 },
  xpTrack:  { height:6, background:'#1e293b', borderRadius:3, overflow:'hidden' },
  xpFill:   { height:'100%', background:'linear-gradient(90deg,#3b82f6,#8b5cf6)',
               borderRadius:3, transition:'width 0.4s' },
  modeBadge:{ background:'#1e293b', borderRadius:6, padding:'0.2rem 0.6rem',
               color:'#94a3b8', fontSize:11, fontWeight:600,
               textTransform:'uppercase', letterSpacing:0.5 },
};
