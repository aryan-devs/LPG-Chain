import React, { useEffect, useState, useRef, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { VehicleLocation, ConnectionStatus, TrackingEvent } from '../../types/tracking';
import { getLatestDeliveryLocation } from '../../services/trackingApi';
import { createTrackingSubscription, TrackingSubscriptionHandle } from '../../services/trackingWebSocket';
import { VehicleMarker } from './VehicleMarker';
import { TrackingStatus } from './TrackingStatus';
import {
  Navigation,
  Gauge,
  Compass,
  Clock,
  Truck,
  LocateFixed,
  Loader2,
  RefreshCw,
  ShieldCheck,
  ShieldAlert,
  Radio,
  MapPin,
} from 'lucide-react';

// Leaflet default icon path fix for bundlers
delete (L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown })._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Custom destination marker for delivery address (LPG red with house icon)
const deliveryHomeIcon = L.divIcon({
  className: 'lpg-destination-marker-wrapper',
  html: `
    <div style="background:#e11d48; width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 12px rgba(225,29,72,0.4); border:2.5px solid white;">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
        <polyline points="9 22 9 12 15 12 15 22"/>
      </svg>
    </div>
  `,
  iconSize: [34, 34],
  iconAnchor: [17, 17],
  popupAnchor: [0, -17],
});

// Inner controller to pan or fly to updated vehicle coordinates without remounting the map
interface MapViewControllerProps {
  location: VehicleLocation | null;
  autoCenter: boolean;
}

const MapViewController: React.FC<MapViewControllerProps> = ({ location, autoCenter }) => {
  const map = useMap();
  const hasCenteredInitially = useRef(false);

  useEffect(() => {
    if (!location) return;

    const targetCoords: [number, number] = [location.latitude, location.longitude];

    if (!hasCenteredInitially.current) {
      map.setView(targetCoords, map.getZoom(), { animate: true });
      hasCenteredInitially.current = true;
    } else if (autoCenter) {
      map.panTo(targetCoords, { animate: true, duration: 0.8 });
    }
  }, [location, autoCenter, map]);

  return null;
};

// Map size invalidator to ensure Leaflet renders all tiles properly when container resizes
const MapResizer: React.FC = () => {
  const map = useMap();
  useEffect(() => {
    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 200);
    return () => clearTimeout(timer);
  }, [map]);
  return null;
};

export interface LiveTrackingMapProps {
  deliveryId: number | string;
  token?: string | null;
  height?: string;
  className?: string;
  defaultCenter?: [number, number];
  defaultZoom?: number;
  onLocationUpdate?: (location: VehicleLocation) => void;
  onStatusChange?: (status: ConnectionStatus) => void;
}

export const LiveTrackingMap: React.FC<LiveTrackingMapProps> = ({
  deliveryId,
  token,
  height = '520px',
  className = '',
  defaultCenter = [18.5204, 73.8567], // Pune coordinates default
  defaultZoom = 14,
  onLocationUpdate,
  onStatusChange,
}) => {
  const [location, setLocation] = useState<VehicleLocation | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('CONNECTING');
  const [isLoadingInitial, setIsLoadingInitial] = useState<boolean>(true);
  const [apiError, setApiError] = useState<string | null>(null);
  const [wsError, setWsError] = useState<string | null>(null);
  const [autoCenter, setAutoCenter] = useState<boolean>(true);

  const subscriptionRef = useRef<TrackingSubscriptionHandle | null>(null);
  const isMountedRef = useRef<boolean>(true);
  const mapInstanceRef = useRef<L.Map | null>(null);

  // Updates status both locally and via callback
  const handleStatusChange = useCallback(
    (status: ConnectionStatus) => {
      if (!isMountedRef.current) return;
      setConnectionStatus(status);
      onStatusChange?.(status);
    },
    [onStatusChange]
  );

  // Updates location both locally and via callback
  const handleNewLocation = useCallback(
    (newLocation: VehicleLocation) => {
      if (!isMountedRef.current) return;
      setLocation(newLocation);
      setApiError(null);
      setWsError(null);
      onLocationUpdate?.(newLocation);
    },
    [onLocationUpdate]
  );

  // Initial REST fetch to populate initial vehicle position
  const fetchInitialLocation = useCallback(async () => {
    setIsLoadingInitial(true);
    setApiError(null);
    try {
      const initialLoc = await getLatestDeliveryLocation(deliveryId, token);
      if (isMountedRef.current) {
        handleNewLocation(initialLoc);
      }
    } catch (err) {
      if (isMountedRef.current) {
        const errorMsg =
          err instanceof Error
            ? err.message
            : 'Could not fetch initial delivery location.';
        setApiError(errorMsg);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoadingInitial(false);
      }
    }
  }, [deliveryId, token, handleNewLocation]);

  // Establish and manage STOMP WebSocket connection
  const connectWebSocket = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    const sub = createTrackingSubscription({
      deliveryId,
      token,
      onLocationUpdate: (event: TrackingEvent) => {
        handleNewLocation({
          vehicleId: event.vehicleId,
          vehicleNumber: event.vehicleNumber,
          latitude: event.latitude,
          longitude: event.longitude,
          speed: event.speed,
          heading: event.heading,
          recordedAt: event.recordedAt,
        });
      },
      onStatusChange: (status) => {
        handleStatusChange(status);
      },
      onError: (errMsg) => {
        if (isMountedRef.current) {
          setWsError(errMsg);
        }
      },
    });

    subscriptionRef.current = sub;
  }, [deliveryId, token, handleNewLocation, handleStatusChange]);

  // Initial mount: fetch REST location and connect WebSocket
  useEffect(() => {
    isMountedRef.current = true;

    fetchInitialLocation();
    connectWebSocket();

    return () => {
      isMountedRef.current = false;
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [fetchInitialLocation, connectWebSocket]);

  // Center map manually on current vehicle position
  const handleRecenter = () => {
    if (mapInstanceRef.current && location) {
      mapInstanceRef.current.flyTo([location.latitude, location.longitude], defaultZoom, {
        animate: true,
        duration: 1.0,
      });
    } else if (mapInstanceRef.current) {
      mapInstanceRef.current.flyTo(defaultCenter, defaultZoom, {
        animate: true,
        duration: 1.0,
      });
    }
  };

  const handleManualReconnect = () => {
    fetchInitialLocation();
    connectWebSocket();
  };

  // Formats recordedAt nicely
  const formatTime = (ts?: string) => {
    if (!ts) return 'Awaiting ping';
    try {
      const d = new Date(ts);
      if (isNaN(d.getTime())) return ts;
      return d.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return ts;
    }
  };

  const mapCenter: [number, number] = location
    ? [location.latitude, location.longitude]
    : defaultCenter;

  const isAuthError =
    (apiError && (apiError.toLowerCase().includes('authentication') || apiError.toLowerCase().includes('log in') || apiError.toLowerCase().includes('unauthorized'))) ||
    (wsError && (wsError.toLowerCase().includes('authentication') || wsError.toLowerCase().includes('unauthorized') || wsError.toLowerCase().includes('jwt')));

  const isNoDataRecordedYet =
    apiError && (apiError.toLowerCase().includes('no location') || apiError.toLowerCase().includes('404'));

  return (
    <div className={`bg-white border border-slate-200/90 rounded-3xl shadow-sm overflow-hidden flex flex-col ${className}`}>
      {/* ── 1. Tracking Console Header Bar (Light Theme) ── */}
      <div className="p-4 sm:p-5 border-b border-slate-100 bg-white flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-rose-50 border border-rose-100 text-rose-600 flex items-center justify-center flex-shrink-0 shadow-xs">
              <Truck size={20} />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 tracking-tight">
                Live Vehicle Tracking
              </h2>
              <p className="text-xs text-slate-500">
                Tracking Delivery <span className="font-mono text-rose-600 font-bold">#{deliveryId}</span>
              </p>
            </div>
          </div>

          <TrackingStatus
            status={connectionStatus}
            lastUpdated={location?.recordedAt}
            onReconnect={handleManualReconnect}
          />
        </div>

        {/* ── 2. Real-Time Telemetry Metrics Grid (4 Light Cards) ── */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3 text-left">
          {/* Vehicle Number */}
          <div className="bg-slate-50/80 border border-slate-200/80 rounded-2xl p-3 flex flex-col justify-between">
            <div className="flex items-center gap-1.5 text-slate-500 text-[11px] uppercase tracking-wider font-bold">
              <Truck size={13} className="text-rose-600" />
              <span>Vehicle</span>
            </div>
            <div className="mt-1 font-mono font-bold text-sm sm:text-base text-slate-900 truncate">
              {location?.vehicleNumber || (isLoadingInitial ? 'Locating...' : 'MH 12 AB 4587')}
            </div>
          </div>

          {/* Current Speed */}
          <div className="bg-slate-50/80 border border-slate-200/80 rounded-2xl p-3 flex flex-col justify-between">
            <div className="flex items-center gap-1.5 text-slate-500 text-[11px] uppercase tracking-wider font-bold">
              <Gauge size={13} className="text-emerald-600" />
              <span>Speed</span>
            </div>
            <div className="mt-1 font-mono font-bold text-sm sm:text-base text-emerald-700">
              {location && typeof location.speed === 'number'
                ? `${location.speed.toFixed(1)} km/h`
                : '0.0 km/h'}
            </div>
          </div>

          {/* Heading / Direction */}
          <div className="bg-slate-50/80 border border-slate-200/80 rounded-2xl p-3 flex flex-col justify-between">
            <div className="flex items-center gap-1.5 text-slate-500 text-[11px] uppercase tracking-wider font-bold">
              <Compass size={13} className="text-amber-600" />
              <span>Heading</span>
            </div>
            <div className="mt-1 flex items-center gap-1.5 font-mono font-bold text-sm sm:text-base text-amber-700">
              <Navigation
                size={14}
                style={{
                  transform: `rotate(${location?.heading ?? 0}deg)`,
                  transition: 'transform 0.4s ease-out',
                }}
              />
              <span>{location ? `${Math.round(location.heading)}°` : '0°'}</span>
            </div>
          </div>

          {/* Last Ping Timestamp */}
          <div className="bg-slate-50/80 border border-slate-200/80 rounded-2xl p-3 flex flex-col justify-between">
            <div className="flex items-center gap-1.5 text-slate-500 text-[11px] uppercase tracking-wider font-bold">
              <Clock size={13} className="text-indigo-600" />
              <span>Last Ping</span>
            </div>
            <div className="mt-1 font-mono font-bold text-sm sm:text-base text-slate-800 truncate">
              {formatTime(location?.recordedAt)}
            </div>
          </div>
        </div>

        {/* ── 3. Friendly Informational States / Error Handling ── */}
        {isAuthError && !location && (
          <div className="flex items-start gap-3 p-3.5 rounded-2xl bg-amber-50/90 border border-amber-200/90 text-amber-900 text-xs">
            <ShieldAlert size={16} className="text-amber-600 shrink-0 mt-0.5" />
            <div className="flex-1 leading-relaxed">
              <span className="font-bold">Authentication Notice:</span> Live driver GPS telemetry streams require a verified customer session. Sign in with your registered account to track this delivery in real time.
            </div>
            <button
              type="button"
              onClick={handleManualReconnect}
              className="px-2.5 py-1 text-xs font-semibold bg-white hover:bg-amber-100 rounded-lg border border-amber-300 text-amber-900 flex items-center gap-1 shadow-xs transition-colors shrink-0"
            >
              <RefreshCw size={11} /> Retry
            </button>
          </div>
        )}

        {isNoDataRecordedYet && !location && (
          <div className="flex items-start gap-3 p-3.5 rounded-2xl bg-blue-50/90 border border-blue-200/90 text-blue-900 text-xs">
            <Radio size={16} className="text-blue-600 shrink-0 mt-0.5" />
            <div className="flex-1 leading-relaxed">
              <span className="font-bold">Awaiting Vehicle Dispatch:</span> Delivery order is confirmed and scheduled. Real-time GPS stream activates once the delivery vehicle departs the agency hub.
            </div>
            <button
              type="button"
              onClick={handleManualReconnect}
              className="px-2.5 py-1 text-xs font-semibold bg-white hover:bg-blue-100 rounded-lg border border-blue-200 text-blue-800 flex items-center gap-1 shadow-xs transition-colors shrink-0"
            >
              <RefreshCw size={11} /> Refresh
            </button>
          </div>
        )}
      </div>

      {/* ── 4. Responsive Leaflet Map Section (Light OSM Tiles) ── */}
      <div className="relative w-full overflow-hidden bg-slate-100" style={{ height }}>
        {/* Loading overlay for initial fetch */}
        {isLoadingInitial && !location && (
          <div className="absolute inset-0 z-[1000] bg-white/70 backdrop-blur-xs flex flex-col items-center justify-center gap-3 text-slate-700">
            <Loader2 size={32} className="animate-spin text-rose-600" />
            <p className="text-xs font-semibold text-slate-600">Connecting to live vehicle telemetry...</p>
          </div>
        )}

        {/* Map Floating Actions (Clean Light Theme) */}
        <div className="absolute top-4 right-4 z-[999] flex flex-col gap-2">
          <button
            type="button"
            onClick={handleRecenter}
            title={location ? 'Center on Vehicle' : 'Center on Delivery Address'}
            className="p-2.5 rounded-xl bg-white/95 hover:bg-slate-50 text-rose-600 hover:text-rose-700 border border-slate-200/90 shadow-md backdrop-blur-sm transition-all hover:scale-105 active:scale-95"
          >
            <LocateFixed size={18} />
          </button>

          <button
            type="button"
            onClick={() => setAutoCenter((prev) => !prev)}
            title={autoCenter ? 'Auto-pan active (click to free map)' : 'Auto-pan paused (click to follow vehicle)'}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold border shadow-md backdrop-blur-sm transition-all flex items-center gap-1.5 ${
              autoCenter
                ? 'bg-rose-600 text-white border-rose-500 hover:bg-rose-700'
                : 'bg-white/95 text-slate-700 border-slate-200 hover:bg-slate-50'
            }`}
          >
            <span className={`w-1.5 h-1.5 rounded-full ${autoCenter ? 'bg-white animate-pulse' : 'bg-slate-400'}`} />
            <span>{autoCenter ? 'Following' : 'Free Map'}</span>
          </button>
        </div>

        {/* Leaflet Map with Light OpenStreetMap Tiles */}
        <MapContainer
          center={mapCenter}
          zoom={defaultZoom}
          scrollWheelZoom={true}
          style={{ height: '100%', width: '100%' }}
          ref={(ref) => {
            mapInstanceRef.current = ref;
          }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            maxZoom={19}
          />

          <MapResizer />
          <MapViewController location={location} autoCenter={autoCenter} />

          {/* Delivery Home Marker */}
          <Marker position={defaultCenter} icon={deliveryHomeIcon}>
            <Popup className="lpg-tracking-popup">
              <div className="p-1 text-slate-900 font-sans min-w-[180px]">
                <div className="flex items-center gap-1.5 border-b pb-1.5 mb-1.5 border-slate-100">
                  <MapPin size={13} className="text-rose-600" />
                  <span className="text-xs uppercase tracking-wider font-bold text-rose-600">
                    Delivery Destination
                  </span>
                </div>
                <p className="text-xs text-slate-600">
                  Customer household delivery address
                </p>
                <div className="mt-1 text-[11px] font-mono text-slate-400">
                  Pune Hub &bull; Zone 4
                </div>
              </div>
            </Popup>
          </Marker>

          {/* Live Vehicle Marker with LPG-red accents */}
          {location && <VehicleMarker location={location} />}
        </MapContainer>
      </div>

      {/* ── 5. Clean Light Footer Telemetry Info ── */}
      <div className="px-5 py-2.5 bg-slate-50/90 border-t border-slate-100 flex flex-wrap items-center justify-between text-[11px] text-slate-500 gap-2">
        <div className="flex items-center gap-1.5">
          <ShieldCheck size={13} className="text-emerald-600" />
          <span>Secure telemetry topic: <span className="font-mono text-slate-600 font-semibold">/topic/deliveries/{deliveryId}/tracking</span></span>
        </div>
        {location ? (
          <div className="font-mono text-slate-600 font-semibold">
            GPS: {location.latitude.toFixed(5)}, {location.longitude.toFixed(5)}
          </div>
        ) : (
          <div className="font-mono text-slate-400">
            Destination: {defaultCenter[0]}, {defaultCenter[1]}
          </div>
        )}
      </div>
    </div>
  );
};
