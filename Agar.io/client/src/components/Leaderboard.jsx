import React from 'react';

export default function Leaderboard({ entries = [], myRank, myMass }) {
  return (
    <div style={styles.panel}>
      <h3 style={styles.title}>Leaderboard</h3>
      <ol style={styles.list}>
        {entries.slice(0, 10).map((e, i) => (
          <li key={i} style={{
            ...styles.entry,
            background: e.isMe ? 'rgba(59,130,246,0.25)' : 'transparent',
          }}>
            <span style={styles.rank}>#{e.rank || i + 1}</span>
            <span style={styles.name}>{e.name}</span>
            <span style={styles.mass}>{Math.floor(e.totalMass || e.mass || 0)}</span>
          </li>
        ))}
      </ol>
      {myRank > 10 && (
        <div style={styles.myEntry}>
          <span style={styles.rank}>#{myRank}</span>
          <span style={styles.name}>You</span>
          <span style={styles.mass}>{Math.floor(myMass)}</span>
        </div>
      )}
    </div>
  );
}

const styles = {
  panel:   { position:'fixed', top:16, right:16, background:'rgba(15,23,42,0.85)',
              backdropFilter:'blur(4px)', borderRadius:10, padding:'0.75rem 1rem',
              minWidth:180, zIndex:50 },
  title:   { color:'#f1f5f9', fontSize:13, fontWeight:700, marginBottom:8,
              textTransform:'uppercase', letterSpacing:1 },
  list:    { listStyle:'none', padding:0, margin:0, display:'flex', flexDirection:'column', gap:4 },
  entry:   { display:'flex', alignItems:'center', gap:8, borderRadius:4, padding:'2px 4px' },
  rank:    { color:'#64748b', fontSize:11, minWidth:24 },
  name:    { color:'#e2e8f0', fontSize:13, flex:1, overflow:'hidden',
              whiteSpace:'nowrap', textOverflow:'ellipsis' },
  mass:    { color:'#94a3b8', fontSize:11 },
  myEntry: { display:'flex', alignItems:'center', gap:8, borderTop:'1px solid #1e293b',
              paddingTop:6, marginTop:6 },
};
