import Phaser from 'phaser';

export class BootScene extends Phaser.Scene {
  constructor() { super('Boot'); }

  preload(): void {
    // Placeholder colored rectangles used until real assets are added
    const g = this.make.graphics({ x: 0, y: 0 }, false);

    // Player texture (32x32 colored square)
    g.fillStyle(0x00ff00).fillRect(0, 0, 32, 32);
    g.generateTexture('player', 32, 32);

    // Bomb texture
    g.clear().fillStyle(0x222222).fillCircle(16, 16, 14);
    g.generateTexture('bomb', 32, 32);

    // Tile textures
    const tiles: Record<string, number> = {
      grass: 0x4caf50, forest: 0x2e7d32, snow: 0xe3f2fd,
      water: 0x1565c0, mountain: 0x795548,
      destructible: 0xa5d6a7, indestructible: 0x424242,
    };
    Object.entries(tiles).forEach(([key, color]) => {
      g.clear().fillStyle(color).fillRect(0, 0, 32, 32);
      g.generateTexture(`tile_${key}`, 32, 32);
    });

    g.destroy();
  }

  create(): void {
    this.scene.start('Login');
  }
}
