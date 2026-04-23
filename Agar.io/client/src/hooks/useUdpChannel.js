// ─────────────────────────────────────────────────────────────
//  UDP Channel via WebRTC DataChannel
//
//  Browser can't use raw UDP — WebRTC DataChannel with
//  { ordered: false, maxRetransmits: 0 } gives UDP semantics:
//  unreliable, unordered, low-latency.
//
//  Signaling (SDP + ICE) happens over the TCP WebSocket.
//
//  Flow:
//    1. open()  → create RTCPeerConnection + DataChannel
//    2. createOffer() → send SDP offer over TCP
//    3. setAnswer(sdp) → called when server returns SDP answer
//    4. addIce(cand) → called when server sends ICE candidates
//    5. DataChannel opens → send MoveInput, receive DeltaUpdate
// ─────────────────────────────────────────────────────────────
import { useRef, useCallback } from 'react';
import { encodeMoveInput, decodeUdpPacket } from '../utils/proto';

const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
];

export function useUdpChannel({ onDelta, onSnapshot, onIceCandidate }) {
  const pcRef  = useRef(null);
  const dcRef  = useRef(null);
  const seqRef = useRef(0);

  // Step 1: Create peer connection and data channel, return SDP offer
  const open = useCallback(async () => {
    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
    pcRef.current = pc;

    // Unreliable, unordered = UDP semantics
    const dc = pc.createDataChannel('battle', {
      ordered:        false,
      maxRetransmits: 0,
    });
    dcRef.current = dc;
    dc.binaryType = 'arraybuffer';

    dc.onopen = () => console.log('[UDP] DataChannel open');
    dc.onclose = () => console.log('[UDP] DataChannel closed');

    dc.onmessage = async (evt) => {
      try {
        const pkt = await decodeUdpPacket(evt.data);
        if (pkt.type === 2) onSnapshot?.(pkt.fullSnapshot);  // FULL_SNAPSHOT
        if (pkt.type === 3) onDelta?.(pkt.deltaUpdate);       // DELTA_UPDATE
      } catch (e) {
        console.error('[UDP] Decode error', e);
      }
    };

    // ICE candidates → forward over TCP
    pc.onicecandidate = (evt) => {
      if (evt.candidate)
        onIceCandidate?.(
          evt.candidate.candidate,
          evt.candidate.sdpMid,
          evt.candidate.sdpMLineIndex,
        );
    };

    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    return offer.sdp; // send this over TCP
  }, [onDelta, onSnapshot, onIceCandidate]);

  // Step 2: Receive SDP answer from server (via TCP welcome msg)
  const setAnswer = useCallback(async (answerSdp) => {
    if (!pcRef.current) return;
    await pcRef.current.setRemoteDescription(
      new RTCSessionDescription({ type: 'answer', sdp: answerSdp })
    );
  }, []);

  // Step 3: Add ICE candidate from server (via TCP)
  const addIce = useCallback(async (candidate, sdpMid, sdpMLineIndex) => {
    if (!pcRef.current) return;
    try {
      await pcRef.current.addIceCandidate(
        new RTCIceCandidate({ candidate, sdpMid, sdpMLineIndex })
      );
    } catch (e) {
      console.warn('[UDP] ICE error', e);
    }
  }, []);

  // Send move input (called every animation frame ~60fps)
  const sendMove = useCallback(async (playerId, mouseX, mouseY, split, eject) => {
    const dc = dcRef.current;
    if (!dc || dc.readyState !== 'open') return;
    const buf = await encodeMoveInput(
      playerId, ++seqRef.current, mouseX, mouseY, split, eject
    );
    dc.send(buf);
  }, []);

  const close = useCallback(() => {
    dcRef.current?.close();
    pcRef.current?.close();
    pcRef.current = null;
    dcRef.current = null;
  }, []);

  return { open, setAnswer, addIce, sendMove, close };
}
