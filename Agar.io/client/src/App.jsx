import React, { useState } from 'react';
import StartScreen from './components/StartScreen';
import Game        from './Game';

export default function App() {
  const [screen, setScreen] = useState('start'); // 'start' | 'game'
  const [config,  setConfig]  = useState(null);

  const handlePlay = (cfg) => {
    setConfig(cfg);
    setScreen('game');
  };

  return (
    <>
      {screen === 'start' && (
        <StartScreen onPlay={handlePlay} />
      )}
      {screen === 'game' && config && (
        <Game
          config={config}
          onBack={() => setScreen('start')}
        />
      )}
    </>
  );
}
