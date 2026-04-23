export const SERVER_TCP_URL  = 'ws://localhost:9001';  // WebSocket (TCP)
export const WORLD_W         = 14142;
export const WORLD_H         = 14142;
export const VIEWPORT_W      = () => window.innerWidth;
export const VIEWPORT_H      = () => window.innerHeight;
export const GRID_LINE_SIZE  = 50;
export const FOOD_RADIUS     = 6;

export const COLORS = [
  '#e74c3c','#3498db','#2ecc71','#f39c12',
  '#9b59b6','#1abc9c','#e91e63','#ff5722',
];

export const GameMode = {
  FFA:           0,
  BATTLE_ROYALE: 1,
  TEAMS:         2,
  EXPERIMENTAL:  3,
  PARTY:         4,
  RUSH:          5,
};

export const GAME_MODE_LABELS = {
  [GameMode.FFA]:           'Free For All',
  [GameMode.BATTLE_ROYALE]: 'Battle Royale',
  [GameMode.TEAMS]:         'Teams',
  [GameMode.EXPERIMENTAL]:  'Experimental',
  [GameMode.PARTY]:         'Party',
  [GameMode.RUSH]:          'Rush',
};
