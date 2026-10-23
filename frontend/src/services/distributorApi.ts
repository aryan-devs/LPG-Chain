import {
  DistributorOverviewData,
  DistributorBookingItem,
  DistributorDeliveryItem,
  DistributorInventoryItem,
  InventoryMovementItem,
  ForecastResponse,
  ForecastMetricsData,
} from '../types/distributorPortalTypes';
import { getApiBaseUrl } from './trackingApi';

const ML_API_BASE_URL = 'http://localhost:8000';

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const DistributorApiService = {
  /**
   * Fetches real operational overview metrics for the distributor dashboard
   */
  async getDashboardOverview(distributorId: number): Promise<DistributorOverviewData> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/distributor/${distributorId}/overview`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) {
      throw new Error(`Failed to load distributor dashboard overview (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches real bookings assigned to this distributor agency
   */
  async getDistributorBookings(distributorId: number): Promise<DistributorBookingItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/bookings/distributor/${distributorId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) {
      throw new Error(`Failed to load distributor bookings (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Updates booking lifecycle state (Accept to CONFIRMED, Reject to CANCELLED)
   */
  async updateBookingStatus(
    bookingId: number,
    status: 'CONFIRMED' | 'CANCELLED' | 'OUT_FOR_DELIVERY' | 'DELIVERED',
    reason?: string
  ): Promise<DistributorBookingItem> {
    const baseUrl = getApiBaseUrl();
    const payload: { status: string; reason?: string } = { status };
    if (reason && reason.trim().length > 0) {
      payload.reason = reason.trim();
    }

    const res = await fetch(`${baseUrl}/api/bookings/${bookingId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      let errMsg = `Failed to update booking status (${res.status})`;
      try {
        const text = await res.text();
        try {
          const json = JSON.parse(text);
          errMsg = json.message || json.error || text || errMsg;
        } catch {
          if (text && text.trim().length > 0) {
            errMsg = text;
          }
        }
      } catch {
        // fallback to default errMsg
      }
      throw new Error(errMsg);
    }

    return await res.json();
  },

  /**
   * Fetches real delivery dispatches assigned to this distributor
   */
  async getDistributorDeliveries(distributorId: number): Promise<DistributorDeliveryItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/deliveries/distributor/${distributorId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) {
      throw new Error(`Failed to load distributor deliveries (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Schedules a delivery for a confirmed booking
   */
  async scheduleDelivery(payload: {
    bookingId: number;
    driverId: number;
    vehicleId: number;
  }): Promise<DistributorDeliveryItem> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/deliveries`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to schedule delivery (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Dispatches delivery to OUT_FOR_DELIVERY
   */
  async updateDeliveryStatus(
    deliveryId: number,
    status: 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED'
  ): Promise<DistributorDeliveryItem> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/deliveries/${deliveryId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({ status }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to update delivery status (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Initiates doorstep delivery handover OTP for an active dispatch
   */
  async initiateDeliveryHandover(deliveryId: number): Promise<{
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
  }> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/deliveries/${deliveryId}/initiate-handover`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to initiate delivery handover (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches deliveries linked to a specific booking ID
   */
  async getDeliveriesByBooking(bookingId: number): Promise<DistributorDeliveryItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/deliveries/booking/${bookingId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to fetch deliveries for booking #${bookingId} (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Verifies delivery OTP during doorstep handover
   */
  async verifyDeliveryOtp(deliveryId: number, otp: string): Promise<{ success: boolean; message: string }> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/deliveries/${deliveryId}/verify-otp`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({ otp }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Delivery OTP verification failed (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches category-wise physical depot inventory
   */
  async getInventory(distributorId: number): Promise<DistributorInventoryItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/inventory/distributor/${distributorId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) {
      throw new Error(`Failed to load inventory (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Updates physical inventory counts and logs auditable movement
   */
  async updateInventoryStock(
    distributorId: number,
    payload: {
      cylinderCategory: string;
      filledStock: number;
      emptyStock: number;
      damagedStock?: number;
      reason?: string;
      referenceNumber?: string;
    }
  ): Promise<DistributorInventoryItem> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/inventory/distributor/${distributorId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to update stock (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches auditable inventory movement history
   */
  async getInventoryMovements(
    distributorId: number,
    category: string = 'DOMESTIC_14_2'
  ): Promise<InventoryMovementItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(
      `${baseUrl}/api/inventory/distributor/${distributorId}/movements?category=${encodeURIComponent(category)}`,
      {
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader(),
        },
      }
    );

    if (!res.ok) {
      throw new Error(`Failed to load inventory movements (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches drivers for this distributor agency
   */
  async getDrivers(distributorId: number): Promise<any[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/drivers/distributor/${distributorId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) return [];
    return await res.json();
  },

  /**
   * Fetches vehicles for this distributor agency
   */
  async getVehicles(distributorId: number): Promise<any[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/vehicles/distributor/${distributorId}`, {
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!res.ok) return [];
    return await res.json();
  },

  /**
   * Fetches agency public profile details
   */
  async getAgencyProfile(distributorId: number): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/distributors/public/${distributorId}`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) return null;
    return await res.json();
  },

  // =========================================================================
  // AI Demand Forecasting Microservice APIs
  // =========================================================================

  /**
   * Checks if the Python ML microservice is available
   */
  async checkForecastHealth(): Promise<boolean> {
    try {
      const res = await fetch(`${ML_API_BASE_URL}/health`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      });
      return res.ok;
    } catch {
      return false;
    }
  },

  /**
   * Fetches category-wise and aggregate demand forecasts for 7, 14, or 30 days
   */
  async getDemandForecast(horizonDays: number = 7, distributorId?: number): Promise<ForecastResponse> {
    const url = new URL(`${ML_API_BASE_URL}/forecast`);
    url.searchParams.set('horizon_days', String(horizonDays));
    if (distributorId) {
      url.searchParams.set('distributor_id', String(distributorId));
    }

    const res = await fetch(url.toString(), {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!res.ok) {
      throw new Error(`ML forecast API error: ${res.statusText} (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches training metrics, baseline comparison, and model metadata
   */
  async getForecastMetrics(): Promise<ForecastMetricsData> {
    const res = await fetch(`${ML_API_BASE_URL}/metrics`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!res.ok) {
      throw new Error(`ML metrics API error: ${res.statusText} (${res.status})`);
    }

    return await res.json();
  },
};
