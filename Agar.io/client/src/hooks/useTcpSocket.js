// ─────────────────────────────────────────────────────────────
//  TCP WebSocket hook  (reliable channel)
//  Handles: join, welcome, SDP signaling, death, leaderboard, chat
// ─────────────────────────────────────────────────────────────
import { useRef, useCallback, useEffect } from 'react';
import { SERVER_TCP_URL } from '../constants';
import {
  encodeJoin, encodeSdpOffer, encodeIceCandidate,
  encodeMoveInput, decodeTcpPacket, decodeUdpPacket,
} from '../utils/proto';

// 0xFD = server → client game state (UdpPacket blob over TCP)
// 0xFE = client → server MoveInput  (UdpPacket blob over TCP)
const UDP_OVER_TCP_SERVER = 0xFD;
const UDP_OVER_TCP_CLIENT = 0xFE;

export function useTcpSocket({ onWelcome, onDeath, onLeaderboard,
                                onChat,   onSnapshot, onDelta, onLevelUp }) {
  const wsRef = useRef(null);

  const connect = useCallback(() => {
    return new Promise((resolve, reject) => {
      // Close any stale socket first (handles React StrictMode double-invoke)
      if (wsRef.current && wsRef.current.readyState <= WebSocket.OPEN) {
        wsRef.current.close();
      }
      const ws = new WebSocket(SERVER_TCP_URL);
      ws.binaryType = 'arraybuffer';
      wsRef.current = ws;

      ws.onopen = () => { console.log('[TCP] Connected to', SERVER_TCP_URL); resolve(ws); };
      ws.onerror = (e) => { console.error('[TCP] Connection error', e); reject(e); };
      ws.onclose = () => console.log('[TCP] Disconnected');

      ws.onmessage = async (evt) => {
        try {
          const bytes = new Uint8Array(evt.data);
          if (bytes.length === 0) return;

          if (bytes[0] === UDP_OVER_TCP_SERVER) {
            // Game state packet (UdpPacket) sent over TCP by server
            const pkt = await decodeUdpPacket(bytes.slice(1));
            console.log('[TCP] UDP-over-TCP pkt type:', pkt.type,
              pkt.type === 2 ? `snap players=${pkt.fullSnapshot?.players?.length}` :
              pkt.type === 3 ? `delta changed=${pkt.deltaUpdate?.changedPlayers?.length}` : '');
            if (pkt.type === 2) onSnapshot?.(pkt.fullSnapshot);  // FULL_SNAPSHOT
            if (pkt.type === 3) onDelta?.(pkt.deltaUpdate);       // DELTA_UPDATE
            return;
          }

          // Standard TcpPacket
          const pkt = await decodeTcpPacket(evt.data);
          console.log('[TCP] TcpPacket type:', pkt.type,
            pkt.type === 2 ? `welcome pid=${pkt.welcome?.playerId}` : '');
          switch (pkt.type) {
            case 2:  onWelcome?.(pkt.welcome);        break; // WELCOME
            case 5:  onDeath?.(pkt.death);             break; // DEATH
            case 6:  onLeaderboard?.(pkt.leaderboard); break; // LEADERBOARD
            case 8:  onChat?.(pkt.chatMsg);            break; // CHAT_MSG
            case 11: onLevelUp?.(pkt.levelUp);         break; // LEVEL_UP
            default: break;
          }
        } catch (e) {
          console.error('[TCP] Decode error', e);
        }
      };
    });
  }, [onWelcome, onDeath, onLeaderboard, onChat, onSnapshot, onDelta]);

  const sendJoin = useCallback(async (name, skinId, mode) => {
    console.log('[TCP] Sending JOIN name:', name, 'mode:', mode);
    const buf = await encodeJoin(name, skinId, mode);
    wsRef.current?.send(buf);
  }, []);

  const sendSdpOffer = useCallback(async (sdp) => {
    const buf = await encodeSdpOffer(sdp);
    wsRef.current?.send(buf);
  }, []);

  const sendIceCandidate = useCallback(async (candidate, mid, mLine) => {
    const buf = await encodeIceCandidate(candidate, mid, mLine);
    wsRef.current?.send(buf);
  }, []);

  // Send MoveInput over TCP (prefixed with 0xFE so server distinguishes it
  // from TcpPacket blobs — mirrors the DataChannel path for local dev).
  const sendMoveInput = useCallback(async (playerId, mouseX, mouseY, split, eject, seq) => {
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    const udpBuf = await encodeMoveInput(playerId, seq, mouseX, mouseY, split, eject);
    const wrapped = new Uint8Array(udpBuf.length + 1);
    wrapped[0] = UDP_OVER_TCP_CLIENT;
    wrapped.set(udpBuf, 1);
    ws.send(wrapped);
  }, []);

  const disconnect = useCallback(() => {
    wsRef.current?.close();
  }, []);

  useEffect(() => () => disconnect(), [disconnect]);

  return { connect, sendJoin, sendSdpOffer, sendIceCandidate, sendMoveInput, disconnect };
}

