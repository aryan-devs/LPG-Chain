import { VehicleLocation, isValidCoordinate } from '../types/tracking';

/**
 * Returns the configured API base URL, stripped of any trailing slash.
 * In development, defaulting to empty string routes requests through Vite's dev proxy.
 */
export function getApiBaseUrl(): string {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (typeof envUrl === 'string' && envUrl.trim().length > 0) {
    return envUrl.trim().replace(/\/+$/, '');
  }
  return '';
}

/**
 * Custom error class for Tracking API failures with HTTP status code support.
 */
export class TrackingApiError extends Error {
  public readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'TrackingApiError';
    this.status = status;
  }
}

/**
 * Fetches the latest known vehicle location for a given delivery.
 * 
 * Endpoint: GET /api/tracking/deliveries/{deliveryId}/location
 *
 * @param deliveryId - The ID of the delivery to retrieve location for.
 * @param token - Optional JWT Bearer token for authorized requests.
 */
export async function getLatestDeliveryLocation(
  deliveryId: number | string,
  token?: string | null
): Promise<VehicleLocation> {
  const numericId = Number(deliveryId);
  if (!deliveryId || Number.isNaN(numericId) || numericId <= 0) {
    throw new TrackingApiError('Invalid delivery ID provided.');
  }

  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/tracking/deliveries/${numericId}/location`;

  const headers: Record<string, string> = {
    Accept: 'application/json',
  };

  // Re-use provided token or check localStorage for stored auth token
  const authToken = token ?? (typeof window !== 'undefined' ? localStorage.getItem('token') : null);
  if (authToken) {
    headers['Authorization'] = authToken.startsWith('Bearer ') ? authToken : `Bearer ${authToken}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method: 'GET',
      headers,
    });
  } catch (networkErr) {
    const errorMsg = networkErr instanceof Error ? networkErr.message : 'Network request failed';
    throw new TrackingApiError(`Failed to connect to tracking service: ${errorMsg}`);
  }

  if (response.status === 404) {
    throw new TrackingApiError(`No location records found for delivery #${numericId}.`, 404);
  }

  if (response.status === 401) {
    throw new TrackingApiError('Authentication required to view delivery tracking.', 401);
  }

  if (response.status === 403) {
    throw new TrackingApiError('Access denied: You do not have permission to view this delivery.', 403);
  }

  if (!response.ok) {
    throw new TrackingApiError(`Tracking service error: HTTP ${response.status} ${response.statusText}`, response.status);
  }

  let data: unknown;
  try {
    data = await response.json();
  } catch {
    throw new TrackingApiError('Failed to parse response from tracking service.');
  }

  if (!data || typeof data !== 'object') {
    throw new TrackingApiError('Received invalid data format from tracking service.');
  }

  const res = data as Record<string, unknown>;
  const latitude = typeof res.latitude === 'number' ? res.latitude : Number(res.latitude);
  const longitude = typeof res.longitude === 'number' ? res.longitude : Number(res.longitude);

  if (!isValidCoordinate(latitude, longitude)) {
    throw new TrackingApiError('Tracking service returned invalid GPS coordinates.');
  }

  const vehicleId = typeof res.vehicleId === 'number' ? res.vehicleId : Number(res.vehicleId);
  const vehicleNumber = typeof res.vehicleNumber === 'string' ? res.vehicleNumber : String(res.vehicleNumber ?? 'Unknown');
  const speed = typeof res.speed === 'number' ? res.speed : Number(res.speed ?? 0);
  const heading = typeof res.heading === 'number' ? res.heading : Number(res.heading ?? 0);
  const recordedAt = typeof res.recordedAt === 'string' ? res.recordedAt : String(res.recordedAt ?? new Date().toISOString());
  const id = typeof res.id === 'number' ? res.id : undefined;

  return {
    id,
    vehicleId: Number.isNaN(vehicleId) ? 0 : vehicleId,
    vehicleNumber,
    latitude,
    longitude,
    speed: Number.isNaN(speed) ? 0 : speed,
    heading: Number.isNaN(heading) ? 0 : heading,
    recordedAt,
  };
}

export interface DeliveryDetails {
  id: number;
  bookingId?: number;
  customerId?: number;
  customerName?: string;
  driverId?: number;
  driverName?: string;
  vehicleId?: number;
  vehicleNumber?: string;
  distributorId?: number;
  distributorName?: string;
  status: string;
  assignedAt?: string;
  pickedUpAt?: string;
  outForDeliveryAt?: string;
  deliveredAt?: string;
  handoverInitiated?: boolean;
  handoverInitiatedAt?: string;
  otpExpiresAt?: string;
  otpVerifiedAt?: string;
}

export interface DeliveryHandoverResponse {
  deliveryId: number;
  status: string;
  handoverStage: string;
  recipientPhoneMasked?: string;
  otpExpiresAt?: string;
  remainingAttempts?: number;
  notificationStatus?: string;
  notificationMessage?: string;
  devTestOtp?: string;
  devTestNotice?: string;
}

/**
 * Initiates doorstep delivery handover and requests OTP dispatch to customer's registered phone.
 * Restricted to assigned driver, distributor member, or administrator.
 *
 * Endpoint: POST /api/deliveries/{deliveryId}/initiate-handover
 */
export async function initiateDeliveryHandover(
  deliveryId: number | string,
  token?: string | null
): Promise<DeliveryHandoverResponse> {
  const numericId = Number(deliveryId);
  if (!deliveryId || Number.isNaN(numericId) || numericId <= 0) {
    throw new TrackingApiError('Invalid delivery ID provided.');
  }

  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/deliveries/${numericId}/initiate-handover`;

  const headers: Record<string, string> = {
    Accept: 'application/json',
  };

  const authToken = token ?? (typeof window !== 'undefined' ? localStorage.getItem('token') : null);
  if (authToken) {
    headers['Authorization'] = authToken.startsWith('Bearer ') ? authToken : `Bearer ${authToken}`;
  }

  const res = await fetch(url, { method: 'POST', headers });
  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new TrackingApiError(errData.message || `Failed to initiate handover (HTTP ${res.status})`, res.status);
  }
  return (await res.json()) as DeliveryHandoverResponse;
}

/**
 * Submits the customer OTP to verify delivery handover and complete delivery.
 * Restricted to assigned driver, distributor member, or administrator.
 *
 * Endpoint: POST /api/deliveries/{deliveryId}/verify-otp
 */
export async function verifyDeliveryHandoverOtp(
  deliveryId: number | string,
  otp: string,
  token?: string | null
): Promise<DeliveryDetails> {
  const numericId = Number(deliveryId);
  if (!deliveryId || Number.isNaN(numericId) || numericId <= 0) {
    throw new TrackingApiError('Invalid delivery ID provided.');
  }

  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/deliveries/${numericId}/verify-otp`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };

  const authToken = token ?? (typeof window !== 'undefined' ? localStorage.getItem('token') : null);
  if (authToken) {
    headers['Authorization'] = authToken.startsWith('Bearer ') ? authToken : `Bearer ${authToken}`;
  }

  const res = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify({ otp: otp.trim() }),
  });

  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new TrackingApiError(errData.message || `Invalid OTP or verification failure (HTTP ${res.status})`, res.status);
  }
  return (await res.json()) as DeliveryDetails;
}

/**
 * Fetches delivery details including driver, vehicle and status for an authorized user.
 * 
 * Endpoint: GET /api/deliveries/{deliveryId}
 */
export async function getDeliveryDetails(
  deliveryId: number | string,
  token?: string | null
): Promise<DeliveryDetails | null> {
  const numericId = Number(deliveryId);
  if (!deliveryId || Number.isNaN(numericId) || numericId <= 0) {
    return null;
  }

  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/deliveries/${numericId}`;

  const headers: Record<string, string> = {
    Accept: 'application/json',
  };

  const authToken = token ?? (typeof window !== 'undefined' ? localStorage.getItem('token') : null);
  if (authToken) {
    headers['Authorization'] = authToken.startsWith('Bearer ') ? authToken : `Bearer ${authToken}`;
  }

  try {
    const res = await fetch(url, { method: 'GET', headers });
    if (res.ok) {
      return (await res.json()) as DeliveryDetails;
    }
  } catch {
    // Network / backend error -> fallback cleanly
  }
  return null;
}
