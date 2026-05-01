import { Client, IMessage } from '@stomp/stompjs';
import { SessionEvent } from '../types/session';

function buildWsUrl(): string {
  const base = import.meta.env.VITE_SESSION_API_BASE_URL;
  if (base) {
    return `${base.replace(/^http/, 'ws')}/ws`;
  }
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws`;
}

const WS_URL = buildWsUrl();

export function subscribeToSession(
  sessionId: string,
  onEvent: (event: SessionEvent) => void,
  onError?: (error: string) => void,
): () => void {
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 0,
    onStompError: (frame) => {
      onError?.(`WebSocket error: ${frame.headers['message'] ?? 'unknown'}`);
    },
    onWebSocketError: () => {
      onError?.('WebSocket connection failed');
    },
  });

  client.onConnect = () => {
    client.subscribe(`/topic/sessions/${sessionId}`, (message: IMessage) => {
      const event: SessionEvent = JSON.parse(message.body);
      onEvent(event);
    });
  };

  client.activate();

  return () => {
    if (client.active) {
      client.deactivate();
    }
  };
}