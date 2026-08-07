// AccessNow WebSocket Signaling Server
// Lightweight Node.js server that exchanges WebRTC SDP offers, answers, and ICE candidates.
const http = require('http');
const WebSocket = require('ws');

const PORT = process.env.PORT || 3000;

const server = http.createServer();
const wss = new WebSocket.Server({ server });

// A simple map of client IDs to WebSocket connections
const clients = new Map();

wss.on('connection', (ws, req) => {
  const clientId = req.headers['sec-websocket-key']; // unique per connection
  console.log(`Client connected: ${clientId}`);
  clients.set(clientId, ws);

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      handleMessage(clientId, msg);
    } catch (err) {
      console.error('Invalid message', err);
    }
  });

  ws.on('close', () => {
    console.log(`Client disconnected: ${clientId}`);
    clients.delete(clientId);
  });
});

function handleMessage(senderId, msg) {
  const { type, targetId, payload } = msg;
  const targetSocket = clients.get(targetId);
  if (!targetSocket) {
    console.warn(`Target ${targetId} not connected`);
    return;
  }
  const forward = JSON.stringify({ type, from: senderId, payload });
  targetSocket.send(forward);
}

server.listen(PORT, () => {
  console.log(`Signaling server listening on port ${PORT}`);
});
