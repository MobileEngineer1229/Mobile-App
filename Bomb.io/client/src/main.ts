import Phaser from 'phaser';
import { BootScene }   from './scenes/BootScene';
import { LoginScene }  from './scenes/LoginScene';
import { LobbyScene }  from './scenes/LobbyScene';
import { BattleScene } from './scenes/BattleScene';
import { useSession }  from './state/session';

useSession.restore();

new Phaser.Game({
  type: Phaser.AUTO,
  width: 800,
  height: 600,
  backgroundColor: '#1a1a2e',
  parent: 'game-container',
  scene: [BootScene, LoginScene, LobbyScene, BattleScene],
});
