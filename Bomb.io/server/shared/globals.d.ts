// ── Bun / Node runtime globals ────────────────────────────────────────────────
declare const process: {
  env: Record<string, string | undefined>;
  exit(code?: number): never;
  on(event: string, listener: (...args: any[]) => void): void;
};
declare const __dirname:  string;
declare const __filename: string;
declare const crypto: Crypto;

declare class Buffer extends Uint8Array {
  static from(value: string | ArrayBuffer | ArrayBufferView | number[], encoding?: string): Buffer;
  static alloc(size: number, fill?: number | string): Buffer;
  static concat(buffers: Buffer[], totalLength?: number): Buffer;
  toString(encoding?: string): string;
}

// ── ioredis ───────────────────────────────────────────────────────────────────
declare module 'ioredis' {
  interface RedisOptions {
    host?: string;
    port?: number;
    password?: string;
    db?: number;
    lazyConnect?: boolean;
  }
  class Redis {
    constructor(options?: RedisOptions);
    connect(): Promise<void>;
    get(key: string): Promise<string | null>;
    set(key: string, value: string): Promise<'OK'>;
    setex(key: string, seconds: number, value: string): Promise<'OK'>;
    del(...keys: string[]): Promise<number>;
    exists(...keys: string[]): Promise<number>;
    hset(key: string, field: string | Record<string, any>, value?: string): Promise<number>;
    hget(key: string, field: string): Promise<string | null>;
    hgetall(key: string): Promise<Record<string, string>>;
    rpush(key: string, ...values: string[]): Promise<number>;
    lpush(key: string, ...values: string[]): Promise<number>;
    lrange(key: string, start: number, stop: number): Promise<string[]>;
    llen(key: string): Promise<number>;
    lrem(key: string, count: number, element: string): Promise<number>;
    zadd(key: string, score: number, member: string): Promise<number>;
    zrevrange(key: string, start: number, stop: number, withScores?: 'WITHSCORES'): Promise<string[]>;
    zrevrank(key: string, member: string): Promise<number | null>;
    publish(channel: string, message: string): Promise<number>;
    subscribe(channel: string, cb?: (err: Error | null, count: number) => void): Promise<void>;
    on(event: string, listener: (...args: any[]) => void): this;
    duplicate(): Redis;
    disconnect(): void;
  }
  export = Redis;
}

// ── bcryptjs ──────────────────────────────────────────────────────────────────
declare module 'bcryptjs' {
  function hash(data: string, saltOrRounds: string | number): Promise<string>;
  function compare(data: string, encrypted: string): Promise<boolean>;
  function genSalt(rounds?: number): Promise<string>;
  export { hash, compare, genSalt };
}

// ── jsonwebtoken ──────────────────────────────────────────────────────────────
declare module 'jsonwebtoken' {
  interface SignOptions { expiresIn?: string | number; algorithm?: string; }
  interface VerifyOptions { algorithms?: string[]; }
  function sign(payload: object | string, secret: string, options?: SignOptions): string;
  function verify(token: string, secret: string, options?: VerifyOptions): object | string;
  function decode(token: string): object | string | null;
  export { sign, verify, decode };
}
