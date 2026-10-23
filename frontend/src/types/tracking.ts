/**
 * Types representing GPS tracking and telemetry payloads for LPG-Chain.
 */

export interface VehicleLocation {
  id?: number;
  vehicleId: number;
  vehicleNumber: string;
  latitude: number;
  longitude: number;
  speed: number;
  heading: number;
  recordedAt: string;
}

export interface TrackingEvent {
  deliveryId: number;
  vehicleId: number;
  vehicleNumber: string;
  latitude: number;
  longitude: number;
  speed: number;
  heading: number;
  recordedAt: string;
}

export type ConnectionStatus =
  | 'CONNECTING'
  | 'CONNECTED'
  | 'RECONNECTING'
  | 'DISCONNECTED'
  | 'ERROR';

/**
 * Validates whether the given numbers are valid geographical coordinates.
 */
export function isValidCoordinate(latitude?: unknown, longitude?: unknown): boolean {
  if (
    typeof latitude !== 'number' ||
    typeof longitude !== 'number' ||
    Number.isNaN(latitude) ||
    Number.isNaN(longitude)
  ) {
    return false;
  }
  return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
}

/**
 * Safely parses and validates a raw event object into a TrackingEvent.
 * Returns null if required fields or coordinates are invalid.
 */
export function parseTrackingEvent(raw: unknown): TrackingEvent | null {
  if (!raw || typeof raw !== 'object') {
    return null;
  }

  const obj = raw as Record<string, unknown>;

  const deliveryId = typeof obj.deliveryId === 'number' ? obj.deliveryId : Number(obj.deliveryId);
  const vehicleId = typeof obj.vehicleId === 'number' ? obj.vehicleId : Number(obj.vehicleId);
  const vehicleNumber = typeof obj.vehicleNumber === 'string' ? obj.vehicleNumber : String(obj.vehicleNumber ?? '');
  const latitude = typeof obj.latitude === 'number' ? obj.latitude : Number(obj.latitude);
  const longitude = typeof obj.longitude === 'number' ? obj.longitude : Number(obj.longitude);
  const speed = typeof obj.speed === 'number' ? obj.speed : Number(obj.speed ?? 0);
  const heading = typeof obj.heading === 'number' ? obj.heading : Number(obj.heading ?? 0);
  const recordedAt = typeof obj.recordedAt === 'string' ? obj.recordedAt : String(obj.recordedAt ?? new Date().toISOString());

  if (
    Number.isNaN(deliveryId) ||
    Number.isNaN(vehicleId) ||
    !vehicleNumber ||
    !isValidCoordinate(latitude, longitude)
  ) {
    return null;
  }

  return {
    deliveryId,
    vehicleId,
    vehicleNumber,
    latitude,
    longitude,
    speed: Number.isNaN(speed) ? 0 : speed,
    heading: Number.isNaN(heading) ? 0 : heading,
    recordedAt,
  };
}
