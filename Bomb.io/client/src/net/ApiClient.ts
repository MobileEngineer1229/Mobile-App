const BASE = {
  login: '/api/login',
  match: '/api/match',
  rank:  '/api/rank',
};

async function post(url: string, body: unknown, token?: string) {
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  });
  return res.json();
}

async function del(url: string, body: unknown, token?: string) {
  const res = await fetch(url, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  });
  return res.json();
}

async function get(url: string, token?: string) {
  const res = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return res.json();
}

export const api = {
  auth: {
    register: (username: string, password: string) =>
      post(`${BASE.login}/register`, { username, password }),
    login: (username: string, password: string) =>
      post(`${BASE.login}/login`, { username, password }),
    logout: (token: string) =>
      post(`${BASE.login}/logout`, {}, token),
    getCharacters: (token: string) =>
      get(`${BASE.login}/characters`, token),
  },
  match: {
    listLargeRooms:  () => get(`${BASE.match}/rooms/large`),
    listCustomRooms: () => get(`${BASE.match}/rooms`),
    joinLargeRoom:  (playerId: string, token: string) =>
      post(`${BASE.match}/rooms/large/join`, { playerId }, token),
    createCustomRoom: (playerId: string, mapId: string, maxPlayers: number, gameMode: string, token: string) =>
      post(`${BASE.match}/rooms`, { playerId, mapId, maxPlayers, gameMode }, token),
    joinRoom:  (roomId: string, playerId: string, token: string) =>
      post(`${BASE.match}/rooms/${roomId}/join`, { playerId }, token),
    startRoom: (roomId: string, playerId: string, token: string) =>
      post(`${BASE.match}/rooms/${roomId}/start`, { playerId }, token),
    leaveRoom: (roomId: string, playerId: string, token: string) =>
      del(`${BASE.match}/rooms/${roomId}/leave`, { playerId }, token),
    queueUp: (playerId: string, mapId: string, gameMode: string, token: string) =>
      post(`${BASE.match}/queue`, { playerId, mapId, gameMode }, token),
  },
  rank: {
    getLeaderboard: () => get(`${BASE.rank}/leaderboard`),
    getStats: (playerId: string) => get(`${BASE.rank}/players/${playerId}/stats`),
  },
};
