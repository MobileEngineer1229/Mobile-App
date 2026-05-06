const TICK_RATE = 20; // Hz
const TICK_MS   = 1000 / TICK_RATE;

export class GameLoop {
  currentFrame = 0;
  private timer: ReturnType<typeof setInterval> | null = null;
  private lastTime = 0;

  constructor(private onTick: (frame: number) => void) {}

  start(): void {
    this.lastTime = Date.now();
    this.timer = setInterval(() => {
      this.currentFrame++;
      this.onTick(this.currentFrame);
    }, TICK_MS);
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  get tickRate(): number {
    return TICK_RATE;
  }
}
