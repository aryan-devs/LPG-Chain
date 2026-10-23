import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { ConnectionStatus, TrackingEvent, parseTrackingEvent } from '../types/tracking';

/**
 * Resolves the WebSocket broker URL.
 * Checks environment variable VITE_WS_URL first.
 * Falls back to the current window origin with ws:// or wss:// on /ws.
 */
export function getWebSocketBrokerUrl(): string {
  const envWs = import.meta.env.VITE_WS_URL;
  if (typeof envWs === 'string' && envWs.trim().length > 0) {
    return envWs.trim();
  }
  if (typeof window !== 'undefined') {
    const isHttps = window.location.protocol === 'https:';
    const protocol = isHttps ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  }
  return 'ws://localhost:8080/ws';
}

export interface TrackingSubscriptionOptions {
  deliveryId: number | string;
  token?: string | null;
  brokerUrl?: string;
  reconnectDelay?: number;
  onLocationUpdate: (event: TrackingEvent) => void;
  onStatusChange: (status: ConnectionStatus) => void;
  onError?: (errorMessage: string) => void;
}

export interface TrackingSubscriptionHandle {
  unsubscribe: () => void;
  reconnect: () => void;
}

/**
 * Creates and manages a resilient STOMP over WebSocket connection for vehicle tracking.
 * Strictly subscribes to the delivery-specific topic: /topic/deliveries/{deliveryId}/tracking.
 */
export function createTrackingSubscription(
  options: TrackingSubscriptionOptions
): TrackingSubscriptionHandle {
  const {
    deliveryId,
    token,
    brokerUrl = getWebSocketBrokerUrl(),
    reconnectDelay = 5000,
    onLocationUpdate,
    onStatusChange,
    onError,
  } = options;

  let activeSubscription: StompSubscription | null = null;
  let isIntentionallyClosed = false;

  // Retrieve auth token from options or localStorage
  const authToken = token ?? (typeof window !== 'undefined' ? localStorage.getItem('token') : null);

  // Require plausible JWT (header.payload.signature) to avoid rejected connections and reconnect thrashing
  const isPlausibleJwt =
    typeof authToken === 'string' &&
    !authToken.startsWith('demo-jwt-') &&
    authToken.split('.').length === 3;

  if (!authToken || !isPlausibleJwt) {
    onStatusChange('DISCONNECTED');
    onError?.('Authentication required: verified customer account needed to stream live GPS telemetry.');
    return {
      unsubscribe: () => {},
      reconnect: () => {},
    };
  }

  const connectHeaders: Record<string, string> = {
    Authorization: authToken.startsWith('Bearer ') ? authToken : `Bearer ${authToken}`,
  };

  const topicDestination = `/topic/deliveries/${Number(deliveryId)}/tracking`;

  onStatusChange('CONNECTING');

  const client = new Client({
    brokerURL: brokerUrl,
    connectHeaders,
    reconnectDelay,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,

    beforeConnect: () => {
      if (!isIntentionallyClosed) {
        onStatusChange('CONNECTING');
      }
    },

    onConnect: () => {
      if (isIntentionallyClosed) {
        return;
      }

      onStatusChange('CONNECTED');

      // Unsubscribe any stale subscription handle before creating a new one
      if (activeSubscription) {
        try {
          activeSubscription.unsubscribe();
        } catch {
          // ignore
        }
        activeSubscription = null;
      }

      try {
        activeSubscription = client.subscribe(topicDestination, (message: IMessage) => {
          try {
            if (!message.body) {
              return;
            }
            const raw = JSON.parse(message.body);
            const parsedEvent = parseTrackingEvent(raw);
            if (parsedEvent) {
              onLocationUpdate(parsedEvent);
            } else {
              onError?.('Received unparsable tracking event payload');
            }
          } catch (err) {
            const msg = err instanceof Error ? err.message : 'JSON parse failure';
            onError?.(`Failed to process incoming tracking message: ${msg}`);
          }
        });
      } catch (subErr) {
        const msg = subErr instanceof Error ? subErr.message : 'Subscription error';
        onError?.(`Failed to subscribe to tracking topic: ${msg}`);
      }
    },

    onStompError: (frame) => {
      const errorMsg = frame.headers['message'] || frame.body || 'STOMP broker reported an error';
      onError?.(errorMsg);
      onStatusChange('ERROR');

      // Deactivate client on auth rejection to stop infinite reconnect loops
      const lower = errorMsg.toLowerCase();
      if (lower.includes('unauthorized') || lower.includes('invalid jwt') || lower.includes('access denied')) {
        isIntentionallyClosed = true;
        try {
          client.deactivate();
        } catch {
          // ignore
        }
      }
    },

    onWebSocketClose: () => {
      if (!isIntentionallyClosed) {
        onStatusChange('RECONNECTING');
      } else {
        onStatusChange('DISCONNECTED');
      }
    },

    onWebSocketError: () => {
      if (!isIntentionallyClosed) {
        onStatusChange('ERROR');
      }
    },

    onDisconnect: () => {
      onStatusChange('DISCONNECTED');
    },
  });

  try {
    client.activate();
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Activation error';
    onError?.(`Failed to activate STOMP client: ${msg}`);
    onStatusChange('ERROR');
  }

  return {
    unsubscribe: () => {
      isIntentionallyClosed = true;
      if (activeSubscription) {
        try {
          activeSubscription.unsubscribe();
        } catch {
          // ignore
        }
        activeSubscription = null;
      }
      try {
        client.deactivate();
      } catch {
        // ignore
      }
      onStatusChange('DISCONNECTED');
    },
    reconnect: () => {
      isIntentionallyClosed = false;
      onStatusChange('RECONNECTING');
      try {
        client.activate();
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Reconnection error';
        onError?.(`Failed to reconnect: ${msg}`);
      }
    },
  };
}
