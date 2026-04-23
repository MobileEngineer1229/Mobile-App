import Phaser from 'phaser';
import { api } from '../net/ApiClient';
import { useSession } from '../state/session';

export class LoginScene extends Phaser.Scene {
  private inputs: HTMLInputElement[] = [];

  constructor() { super('Login'); }

  create(): void {
    const { width, height } = this.scale;
    const cx = width / 2;
    const cy = height / 2;

    // Dark gradient background panel
    const panel = this.add.graphics();
    panel.fillStyle(0x0d1117, 0.95);
    panel.fillRoundedRect(cx - 160, cy - 200, 320, 400, 16);
    panel.lineStyle(1, 0xff5722, 0.4);
    panel.strokeRoundedRect(cx - 160, cy - 200, 320, 400, 16);

    // Bomb emoji + title
    this.add.text(cx, cy - 155, '💣', { fontSize: '36px' }).setOrigin(0.5);
    this.add.text(cx, cy - 105, 'BOMB.IO', {
      fontSize: '36px', color: '#ff5722', fontStyle: 'bold',
      shadow: { offsetX: 0, offsetY: 2, color: '#ff1744', blur: 8, fill: true },
    }).setOrigin(0.5);
    this.add.text(cx, cy - 72, 'Explode your enemies', {
      fontSize: '12px', color: '#546e7a',
    }).setOrigin(0.5);

    // Divider
    const div = this.add.graphics();
    div.lineStyle(1, 0xff5722, 0.2);
    div.lineBetween(cx - 120, cy - 54, cx + 120, cy - 54);

    // Tab state
    let isRegister = false;
    const tabLogin    = this.makeTab(cx - 60, cy - 34, 'Login',    true);
    const tabRegister = this.makeTab(cx + 60, cy - 34, 'Register', false);
    const tabLine = this.add.graphics();

    const updateTabs = () => {
      tabLogin.setColor(isRegister ? '#546e7a' : '#ff5722');
      tabRegister.setColor(isRegister ? '#ff5722' : '#546e7a');
      tabLine.clear();
      tabLine.lineStyle(2, 0xff5722, 1);
      const lx = isRegister ? cx + 20 : cx - 100;
      tabLine.lineBetween(lx, cy - 20, lx + 80, cy - 20);
      btnText.setText(isRegister ? 'CREATE ACCOUNT' : 'LOGIN');
      subtitleText.setText(isRegister ? 'Already have an account?' : "Don't have an account?");
      switchText.setText(isRegister ? 'Login' : 'Register');
      statusText.setText('');
    };

    tabLogin.setInteractive({ useHandCursor: true }).on('pointerdown', () => {
      isRegister = false; updateTabs();
    });
    tabRegister.setInteractive({ useHandCursor: true }).on('pointerdown', () => {
      isRegister = true; updateTabs();
    });

    // Input fields
    const usernameEl = this.makeInput('Username', cx, cy + 10);
    const passwordEl = this.makeInput('Password', cx, cy + 60, true);

    // Status text
    const statusText = this.add.text(cx, cy + 104, '', {
      fontSize: '12px', color: '#ff1744', wordWrap: { width: 260 },
    }).setOrigin(0.5);

    // Action button
    const btnGfx = this.add.graphics();
    btnGfx.fillStyle(0xff5722, 1);
    btnGfx.fillRoundedRect(cx - 120, cy + 120, 240, 40, 8);

    const btnText = this.add.text(cx, cy + 140, 'LOGIN', {
      fontSize: '15px', color: '#fff', fontStyle: 'bold', letterSpacing: 2,
    }).setOrigin(0.5).setInteractive({ useHandCursor: true });

    btnText.on('pointerover',  () => { btnGfx.clear(); btnGfx.fillStyle(0xff7043, 1); btnGfx.fillRoundedRect(cx - 120, cy + 120, 240, 40, 8); });
    btnText.on('pointerout',   () => { btnGfx.clear(); btnGfx.fillStyle(0xff5722, 1); btnGfx.fillRoundedRect(cx - 120, cy + 120, 240, 40, 8); });
    btnText.on('pointerdown', async () => {
      const u = usernameEl.value.trim();
      const p = passwordEl.value;
      if (!u || !p) { statusText.setText('Please fill in all fields'); return; }
      btnText.setText('...');

      const result = isRegister
        ? await api.auth.register(u, p)
        : await api.auth.login(u, p);

      if ('error' in result) {
        statusText.setText(result.error);
        btnText.setText(isRegister ? 'CREATE ACCOUNT' : 'LOGIN');
        return;
      }
      useSession.set(result as any);
      this.cleanup();
      this.scene.start('Lobby');
    });

    // Switch mode link
    const subtitleText = this.add.text(cx - 2, cy + 175, "Don't have an account?", {
      fontSize: '11px', color: '#546e7a',
    }).setOrigin(1, 0.5);

    const switchText = this.add.text(cx + 2, cy + 175, 'Register', {
      fontSize: '11px', color: '#ff5722',
    }).setOrigin(0, 0.5).setInteractive({ useHandCursor: true });

    switchText.on('pointerdown', () => {
      isRegister = !isRegister; updateTabs();
    });

    updateTabs();

    this.events.on('shutdown', () => this.cleanup());
  }

  private makeTab(x: number, y: number, label: string, active: boolean): Phaser.GameObjects.Text {
    return this.add.text(x, y, label, {
      fontSize: '14px',
      color: active ? '#ff5722' : '#546e7a',
      fontStyle: 'bold',
    }).setOrigin(0.5);
  }

  private makeInput(placeholder: string, _cx: number, cy: number, password = false): HTMLInputElement {
    const el = document.createElement('input');
    el.type        = password ? 'password' : 'text';
    el.placeholder = placeholder;
    el.style.cssText = [
      'position:fixed',
      `left:50%`,
      `top:calc(50% + ${cy - this.scale.height / 2}px)`,
      'transform:translateX(-50%)',
      'width:240px',
      'padding:10px 14px',
      'font-size:14px',
      'border-radius:8px',
      'border:1px solid #263238',
      'background:#161b22',
      'color:#eceff1',
      'outline:none',
      'box-sizing:border-box',
    ].join(';');
    el.addEventListener('focus', () => el.style.borderColor = '#ff5722');
    el.addEventListener('blur',  () => el.style.borderColor = '#263238');
    document.body.appendChild(el);
    this.inputs.push(el);
    return el;
  }

  private cleanup(): void {
    this.inputs.forEach(el => el.remove());
    this.inputs = [];
  }
}
