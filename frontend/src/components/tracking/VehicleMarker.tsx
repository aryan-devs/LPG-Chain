import React, { useMemo } from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { VehicleLocation } from '../../types/tracking';

interface VehicleMarkerProps {
  location: VehicleLocation;
  autoOpenPopup?: boolean;
}

/**
 * Creates a Leaflet DivIcon with vehicle heading rotation and a soft radar pulse.
 * Uses a navigation arrow polygon pointing upward (0° North).
 */
function createVehicleIcon(heading: number = 0): L.DivIcon {
  const safeHeading = Number.isFinite(heading) ? heading : 0;

  return L.divIcon({
    className: 'lpg-vehicle-marker-wrapper',
    html: `
      <div style="position: relative; width: 46px; height: 46px; display: flex; align-items: center; justify-content: center;">
        <div style="position: absolute; width: 40px; height: 40px; border-radius: 50%; background: rgba(225, 29, 72, 0.25); animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;"></div>
        <div style="position: relative; z-index: 10; width: 34px; height: 34px; border-radius: 50%; background: #e11d48; border: 2.5px solid #ffffff; box-shadow: 0 4px 12px rgba(225,29,72,0.45); display: flex; align-items: center; justify-content: center; transform: rotate(${safeHeading}deg); transition: transform 0.4s ease-out;">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="#ffffff" stroke="#ffffff" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 19 21 12 17 5 21 12 2"></polygon>
          </svg>
        </div>
      </div>
    `,
    iconSize: [46, 46],
    iconAnchor: [23, 23],
    popupAnchor: [0, -23],
  });
}

export const VehicleMarker: React.FC<VehicleMarkerProps> = ({ location }) => {
  const { latitude, longitude, vehicleNumber, speed, heading, recordedAt } = location;

  const vehicleIcon = useMemo(() => {
    return createVehicleIcon(heading);
  }, [heading]);

  const formattedTime = useMemo(() => {
    if (!recordedAt) return 'N/A';
    try {
      const d = new Date(recordedAt);
      if (isNaN(d.getTime())) return recordedAt;
      return d.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return recordedAt;
    }
  }, [recordedAt]);

  return (
    <Marker position={[latitude, longitude]} icon={vehicleIcon}>
      <Popup className="lpg-tracking-popup">
        <div className="p-1 text-slate-900 font-sans min-w-[200px]">
          <div className="flex items-center justify-between border-b pb-1.5 mb-2 border-slate-200">
            <span className="text-xs uppercase tracking-wider font-semibold text-slate-500">
              Vehicle Telemetry
            </span>
            <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          </div>

          <div className="space-y-1 text-xs">
            <div className="flex justify-between">
              <span className="font-semibold text-slate-600">Vehicle:</span>
              <span className="font-bold text-slate-900">{vehicleNumber || 'N/A'}</span>
            </div>

            <div className="flex justify-between">
              <span className="font-semibold text-slate-600">Speed:</span>
              <span className="font-medium text-slate-800">
                {typeof speed === 'number' ? `${speed.toFixed(1)} km/h` : '0.0 km/h'}
              </span>
            </div>

            <div className="flex justify-between">
              <span className="font-semibold text-slate-600">Heading:</span>
              <span className="font-medium text-slate-800">
                {typeof heading === 'number' ? `${Math.round(heading)}°` : '0°'}
              </span>
            </div>

            <div className="flex justify-between">
              <span className="font-semibold text-slate-600">Last Updated:</span>
              <span className="font-mono text-slate-700">{formattedTime}</span>
            </div>

            <div className="pt-1.5 mt-1.5 border-t border-slate-100 flex justify-between text-[11px] text-slate-500">
              <span>Coordinates:</span>
              <span className="font-mono">
                {latitude.toFixed(4)}, {longitude.toFixed(4)}
              </span>
            </div>
          </div>
        </div>
      </Popup>
    </Marker>
  );
};
