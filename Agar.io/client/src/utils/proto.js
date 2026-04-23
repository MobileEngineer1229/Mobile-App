// ─────────────────────────────────────────────────────────────
//  Protobuf encode / decode using protobufjs
//  Loads the .proto file at runtime from /proto/agario.proto
//  (copy proto file to public/proto/agario.proto at build time)
// ─────────────────────────────────────────────────────────────
import protobuf from 'protobufjs';

let _root = null;

async function getRoot() {
  if (_root) return _root;
  _root = await protobuf.load('/proto/agario.proto');
  return _root;
}

// ── Encode helpers ────────────────────────────────────────────

export async function encodeJoin(name, skinId = '', mode = 0, partyCode = '') {
  const root = await getRoot();
  const TcpPacket   = root.lookupType('agario.TcpPacket');
  const JoinRequest = root.lookupType('agario.JoinRequest');

  const payload = { name, skinId, mode, partyCode };
  const err = JoinRequest.verify(payload);
  if (err) throw Error(err);

  const msg = TcpPacket.create({
    type: TcpPacket.Type.JOIN_REQUEST,
    joinRequest: JoinRequest.create(payload),
  });
  return TcpPacket.encode(msg).finish(); // Uint8Array
}

export async function encodeSdpOffer(sdp) {
  const root = await getRoot();
  const TcpPacket = root.lookupType('agario.TcpPacket');
  const SdpOffer  = root.lookupType('agario.SdpOffer');
  const msg = TcpPacket.create({
    type: TcpPacket.Type.SDP_OFFER,
    sdpOffer: SdpOffer.create({ sdp }),
  });
  return TcpPacket.encode(msg).finish();
}

export async function encodeIceCandidate(candidate, sdpMid, sdpMLine) {
  const root = await getRoot();
  const TcpPacket    = root.lookupType('agario.TcpPacket');
  const IceCandidate = root.lookupType('agario.IceCandidate');
  const msg = TcpPacket.create({
    type: TcpPacket.Type.ICE_CANDIDATE,
    iceCandidate: IceCandidate.create({ candidate, sdpMid, sdpMLine }),
  });
  return TcpPacket.encode(msg).finish();
}

export async function encodeMoveInput(playerId, seq, mouseX, mouseY, split, eject) {
  const root      = await getRoot();
  const UdpPacket = root.lookupType('agario.UdpPacket');
  const MoveInput = root.lookupType('agario.MoveInput');
  const msg = UdpPacket.create({
    type: UdpPacket.Type.MOVE_INPUT,
    moveInput: MoveInput.create({
      playerId, seq, mouseX, mouseY, split, eject,
    }),
  });
  return UdpPacket.encode(msg).finish();
}

export async function encodeChatSend(content, teamOnly = false) {
  const root = await getRoot();
  const TcpPacket = root.lookupType('agario.TcpPacket');
  const ChatSend  = root.lookupType('agario.ChatSend');
  const msg = TcpPacket.create({
    type: TcpPacket.Type.CHAT_SEND,
    chatSend: ChatSend.create({ content, teamOnly }),
  });
  return TcpPacket.encode(msg).finish();
}

// ── Decode helpers ────────────────────────────────────────────

export async function decodeTcpPacket(buffer) {
  const root = await getRoot();
  const TcpPacket = root.lookupType('agario.TcpPacket');
  return TcpPacket.decode(new Uint8Array(buffer));
}

export async function decodeUdpPacket(buffer) {
  const root = await getRoot();
  const UdpPacket = root.lookupType('agario.UdpPacket');
  return UdpPacket.decode(new Uint8Array(buffer));
}
