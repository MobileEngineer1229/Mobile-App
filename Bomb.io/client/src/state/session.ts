interface Session {
  playerId: number;
  username: string;
  token: string;
}

let _session: Session | null = null;

export const useSession = {
  get: (): Session | null => _session,
  set: (s: Session) => { _session = s; sessionStorage.setItem('session', JSON.stringify(s)); },
  clear: () => { _session = null; sessionStorage.removeItem('session'); },
  restore: (): Session | null => {
    const raw = sessionStorage.getItem('session');
    if (raw) { _session = JSON.parse(raw) as Session; }
    return _session;
  },
};
