// ─────────────────────────────────────────────────────────────
//  Game state manager
//  Merges TCP events (death, leaderboard) and UDP events
//  (delta updates, snapshots) into a single game state object.
// ─────────────────────────────────────────────────────────────
import { useState, useCallback, useRef } from 'react';

const INITIAL_STATE = {
  players:     [],
  foods:       [],
  viruses:     [],
  safeZone:    null,
  leaderboard: [],
  myPlayerId:  null,
  myMass:      0,
  myLevel:     1,
  myXp:        0,
  myXpToNext:  1000,
  myRank:      0,
};

export function useGame() {
  const [state, setState] = useState(INITIAL_STATE);
  const stateRef = useRef(INITIAL_STATE);

  const update = useCallback((patch) => {
    stateRef.current = { ...stateRef.current, ...patch };
    setState(s => ({ ...s, ...patch }));
  }, []);

  // ── TCP Handlers ───────────────────────────────────────────
  const onWelcome = useCallback((welcome) => {
    update({
      myPlayerId: welcome.playerId,
      myLevel:    welcome.level,
      myXp:       welcome.xp,
      myXpToNext: welcome.xpToNext,
    });
  }, [update]);

  const onDeath = useCallback((death) => {
    return death; // bubble up to App to show GameOver screen
  }, []);

  const onLeaderboard = useCallback((lb) => {
    update({
      leaderboard: lb.entries || [],
      myRank: lb.myRank || 0,
      myMass: lb.myMass || stateRef.current.myMass,
    });
  }, [update]);

  const onLevelUp = useCallback((lu) => {
    if (!lu) return;
    update({
      myLevel:    lu.newLevel,
      myXp:       lu.xp,
      myXpToNext: lu.xpToNext,
    });
  }, [update]);

  // ── UDP Handlers ───────────────────────────────────────────
  const onSnapshot = useCallback((snap) => {
    update({
      players: snap.players || [],
      foods:   snap.foods   || [],
      viruses: snap.viruses || [],
    });
  }, [update]);

  const onDelta = useCallback((delta) => {
    const current = stateRef.current;

    // Update changed players
    let players = [...current.players];
    for (const changed of (delta.changedPlayers || [])) {
      const idx = players.findIndex(p => p.playerId === changed.playerId);
      if (idx >= 0) players[idx] = changed;
      else          players.push(changed);
    }
    // Remove dead players
    for (const rid of (delta.removedPlayers || []))
      players = players.filter(p => p.playerId !== rid);

    // Update foods
    let foods = [...current.foods];
    for (const nf of (delta.newFoods || []))
      foods.push(nf);
    for (const eid of (delta.eatenFoodIds || []))
      foods = foods.filter(f => f.id !== eid);

    // Update viruses
    let viruses = [...current.viruses];
    for (const cv of (delta.changedViruses || [])) {
      const idx = viruses.findIndex(v => v.id === cv.id);
      if (idx >= 0) viruses[idx] = cv;
      else          viruses.push(cv);
    }
    for (const rid of (delta.removedVirusIds || []))
      viruses = viruses.filter(v => v.id !== rid);

    // Update own mass
    const myPlayer = players.find(p => p.playerId === current.myPlayerId);
    const myMass   = myPlayer?.totalMass || current.myMass;

    update({ players, foods, viruses, myMass });
  }, [update]);

  const onSafeZone = useCallback((sz) => {
    update({ safeZone: sz });
  }, [update]);

  const reset = useCallback(() => {
    update(INITIAL_STATE);
  }, [update]);

  return {
    state,
    onWelcome,
    onDeath,
    onLeaderboard,
    onLevelUp,
    onSnapshot,
    onDelta,
    onSafeZone,
    reset,
  };
}
