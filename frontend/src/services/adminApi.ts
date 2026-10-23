import {
  AdminKpiSummary,
  AdminUserItem,
  AdminDistributorItem,
  AdminBookingItem,
  OmcAllocationItem,
  AdminDriverItem,
  AdminVehicleItem,
  PlatformForecastResult,
} from '../types/adminPortalTypes';
import { getApiBaseUrl } from './trackingApi';

const ML_API_BASE_URL = 'http://localhost:8000';

function getAuthHeader(): Record<string, string> {
  const token = typeof window !== 'undefined'
    ? localStorage.getItem('token') || sessionStorage.getItem('token')
    : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * AdminApiService — Integration boundary for LPG-Chain Administrative endpoints.
 * Calls Spring Boot backend APIs when available.
 * Transparently surfaces connection status without silently masquerading mock data as live records.
 */
export const AdminApiService = {
  /**
   * Fetches real high-level platform KPI counters from backend.
   */
  async getOverviewKpi(): Promise<AdminKpiSummary> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/admin/overview-kpis`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });

    if (!res.ok) {
      throw new Error(`Overview KPIs API unavailable (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches registered platform users from /api/users.
   */
  async getAllUsers(): Promise<AdminUserItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/users`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });

    if (!res.ok) {
      throw new Error(`Users API unavailable (${res.status})`);
    }

    const data = await res.json();
    return (data || []).map((u: any) => {
      const rawRole = (u.role?.name || u.role || 'CUSTOMER').replace('ROLE_', '').toUpperCase();
      const role: 'ADMIN' | 'DISTRIBUTOR' | 'DRIVER' | 'CONSUMER' =
        rawRole === 'ADMIN' ? 'ADMIN' :
        rawRole === 'DISTRIBUTOR' ? 'DISTRIBUTOR' :
        rawRole === 'DRIVER' ? 'DRIVER' : 'CONSUMER';

      return {
        id: u.id,
        name: u.name || 'Anonymous User',
        email: u.email || 'N/A',
        phone: u.phone || 'N/A',
        address: u.address,
        role,
        status: (u.status || 'ACTIVE') as any,
        registrationDate: u.createdAt || new Date().toISOString().split('T')[0],
        distributorName: u.distributorName,
      };
    });
  },

  /**
   * Fetches registered distributors from /api/distributors.
   */
  async getAllDistributors(): Promise<AdminDistributorItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/distributors`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });

    if (!res.ok) {
      throw new Error(`Distributors API unavailable (${res.status})`);
    }

    const data = await res.json();
    return (data || []).map((d: any) => ({
      id: d.id,
      agencyName: d.agencyName || 'Distributor Agency',
      distributorCode: d.distributorCode || `DIST-${d.id?.toString().padStart(3, '0')}`,
      licenceNumber: d.licenceNumber || 'LIC-PENDING',
      contactNumber: d.contactNumber || 'N/A',
      email: d.email,
      address: d.address || 'Address Unspecified',
      city: d.city || 'City',
      state: d.state || 'State',
      pinCode: d.pinCode || '000000',
      accountStatus: (d.status === 'ACTIVE' ? 'ACTIVE' : 'SUSPENDED') as any,
      verificationStatus: (d.status === 'ACTIVE' ? 'VERIFIED' : d.status === 'PENDING_VERIFICATION' ? 'PENDING_VERIFICATION' : 'REJECTED') as any,
      supportedServices: (d.supportedServices || 'BOTH') as any,
      operatingHours: d.operatingHours || 'Mon-Sat: 09:00 - 18:00',
      activeMembersCount: d.activeMembersCount || 1,
      vehiclesCount: d.vehiclesCount || 0,
      driversCount: d.driversCount || 0,
      serviceRadiusKm: 15,
      coveredPinCodes: [d.pinCode || '400001'],
      joinedDate: d.joinedDate || new Date().toISOString().split('T')[0],
    }));
  },

  /**
   * Updates distributor agency account status (ACTIVE, SUSPENDED, PENDING_VERIFICATION).
   * Enforced on backend with ROLE_ADMIN.
   */
  async updateDistributorStatus(
    id: number,
    status: 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION'
  ): Promise<AdminDistributorItem> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/distributors/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ status }),
    });

    if (!res.ok) {
      let errorMsg = `Failed to update distributor status (${res.status})`;
      try {
        const errorData = await res.json();
        if (errorData.message) errorMsg = errorData.message;
      } catch {
        // fallback
      }
      throw new Error(errorMsg);
    }

    const d = await res.json();
    return {
      id: d.id,
      agencyName: d.agencyName || 'Distributor Agency',
      distributorCode: d.distributorCode || `DIST-${d.id?.toString().padStart(3, '0')}`,
      licenceNumber: d.licenceNumber || 'LIC-PENDING',
      contactNumber: d.contactNumber || 'N/A',
      email: d.email,
      address: d.address || 'Address Unspecified',
      city: d.city || 'City',
      state: d.state || 'State',
      pinCode: d.pinCode || '000000',
      accountStatus: (d.status === 'ACTIVE' ? 'ACTIVE' : 'SUSPENDED') as any,
      verificationStatus: (d.status === 'ACTIVE' ? 'VERIFIED' : d.status === 'PENDING_VERIFICATION' ? 'PENDING_VERIFICATION' : 'REJECTED') as any,
      supportedServices: (d.supportedServices || 'BOTH') as any,
      operatingHours: d.operatingHours || 'Mon-Sat: 09:00 - 18:00',
      activeMembersCount: d.activeMembersCount || 1,
      vehiclesCount: d.vehiclesCount || 0,
      driversCount: d.driversCount || 0,
      serviceRadiusKm: 15,
      coveredPinCodes: [d.pinCode || '400001'],
      joinedDate: d.joinedDate || new Date().toISOString().split('T')[0],
    };
  },

  /**
   * Fetches platform bookings from /api/bookings.
   */
  async getAllBookings(): Promise<AdminBookingItem[]> {
    const baseUrl = getApiBaseUrl();
    const [bookingsRes, deliveriesRes] = await Promise.allSettled([
      fetch(`${baseUrl}/api/bookings`, {
        headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      }),
      fetch(`${baseUrl}/api/deliveries`, {
        headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      }),
    ]);

    if (bookingsRes.status !== 'fulfilled' || !bookingsRes.value.ok) {
      const status = bookingsRes.status === 'fulfilled' ? bookingsRes.value.status : 'offline';
      throw new Error(`Bookings API unavailable (${status})`);
    }

    const data = await bookingsRes.value.json();

    const deliveryMap = new Map<number, any>();
    if (deliveriesRes.status === 'fulfilled' && deliveriesRes.value.ok) {
      try {
        const deliveries = await deliveriesRes.value.json();
        if (Array.isArray(deliveries)) {
          for (const d of deliveries) {
            const bId = Number(d.bookingId ?? d.booking_id);
            if (!isNaN(bId)) {
              const prev = deliveryMap.get(bId);
              if (!prev || d.id > prev.id) {
                deliveryMap.set(bId, d);
              }
            }
          }
        }
      } catch {
        // Fall back gracefully if deliveries JSON parsing fails
      }
    }

    return (data || []).map((b: any) => {
      const del = deliveryMap.get(Number(b.id));
      let delStatus = b.deliveryStatus || 'UNASSIGNED';
      if (del) {
        delStatus = del.status;
      } else if (b.status === 'CONFIRMED') {
        delStatus = 'QUEUED';
      } else if (b.status === 'DELIVERED') {
        delStatus = 'DELIVERED';
      } else if (b.status === 'OUT_FOR_DELIVERY') {
        delStatus = 'OUT_FOR_DELIVERY';
      }

      return {
        id: b.id,
        bookingNumber: `BK-${b.id?.toString().padStart(5, '0')}`,
        customerId: b.customerId,
        customerName: b.customerName || 'Customer',
        customerPhone: b.customerPhone || 'N/A',
        deliveryAddress: b.deliveryAddress || 'Address',
        distributorId: b.distributorId,
        distributorName: b.distributorName || 'Distributor',
        cylinderCategory: b.cylinderCategory || 'DOMESTIC_14_2',
        cylinderDisplayName: b.categoryDisplayName || '14.2kg Domestic Cylinder',
        capacityKg: b.capacityKg || 14.2,
        quantity: b.quantity || 1,
        bookingDate: b.bookingDate || new Date().toISOString(),
        bookingStatus: b.status || 'PENDING',
        deliveryStatus: delStatus,
        assignedDriverName: del?.driverName || b.driverName,
        assignedVehicleReg: del?.vehicleNumber || del?.vehicleRegistration || b.vehicleRegistration,
        isOverdue: false,
      };
    });
  },

  /**
   * Fetches official OMC allocations from /api/allocations.
   */
  async getAllAllocations(): Promise<OmcAllocationItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/allocations`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });

    if (!res.ok) {
      throw new Error(`Allocations API unavailable (${res.status})`);
    }

    return await res.json();
  },

  /**
   * Fetches drivers fleet from /api/drivers.
   */
  async getAllDrivers(): Promise<AdminDriverItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/drivers`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });

    if (!res.ok) {
      throw new Error(`Drivers API unavailable (${res.status})`);
    }

    const data = await res.json();
    return (data || []).map((d: any) => ({
      id: d.id,
      name: d.name,
      phone: d.phone,
      licenseNumber: d.licenseNumber || 'DL-PENDING',
      distributorId: d.distributorId,
      distributorName: d.distributorName || 'Agency Depot',
      status: (d.status || 'AVAILABLE') as any,
      assignedVehicleReg: d.assignedVehicleReg,
      completedDeliveriesTotal: d.completedDeliveriesCount || 0,
      joinedDate: d.joinedDate || new Date().toISOString().split('T')[0],
    }));
  },

  /**
   * Fetches vehicles fleet from /api/vehicles.
   */
  async getAllVehicles(): Promise<AdminVehicleItem[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/vehicles`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });

    if (!res.ok) {
      throw new Error(`Vehicles API unavailable (${res.status})`);
    }

    const data = await res.json();
    return (data || []).map((v: any) => ({
      id: v.id,
      registrationNumber: v.registrationNumber,
      vehicleType: v.vehicleType || 'MINI_TRUCK',
      capacityUnits: v.capacityUnits || 40,
      distributorId: v.distributorId,
      distributorName: v.distributorName || 'Agency Depot',
      status: (v.status || 'ACTIVE') as any,
      assignedDriverName: v.assignedDriverName,
      insuranceValidUntil: v.insuranceValidUntil || '2027-12-31',
    }));
  },

  /**
   * Calls Python ML Service for platform demand forecast.
   */
  async getAiDemandForecast(horizonDays: 7 | 14 | 30, distributorId?: number): Promise<PlatformForecastResult> {
    const url = distributorId
      ? `${ML_API_BASE_URL}/predict?days=${horizonDays}&distributor_id=${distributorId}`
      : `${ML_API_BASE_URL}/predict?days=${horizonDays}`;

    const res = await fetch(url, { headers: { 'Content-Type': 'application/json' } });
    if (!res.ok) {
      throw new Error(`AI Forecasting Service returned HTTP ${res.status}`);
    }

    return await res.json();
  },

  /**
   * Checks ML forecasting health endpoint
   */
  async checkMlHealth(): Promise<boolean> {
    try {
      const res = await fetch(`${ML_API_BASE_URL}/health`, { method: 'GET' });
      return res.ok;
    } catch {
      return false;
    }
  },

  /**
   * Fetches platform complaints from /api/complaints/admin.
   */
  async getAllComplaints(status?: string): Promise<any[]> {
    const baseUrl = getApiBaseUrl();
    const qs = status ? `?status=${encodeURIComponent(status)}` : '';
    const res = await fetch(`${baseUrl}/api/complaints/admin${qs}`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });
    if (!res.ok) throw new Error(`Complaints API returned HTTP ${res.status}`);
    return await res.json();
  },

  /**
   * Updates complaint status and resolution note.
   */
  async updateComplaintStatus(id: number | string, status: string, resolutionNote?: string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/complaints/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ status, resolutionNote }),
    });
    if (!res.ok) throw new Error(`Complaint status update failed with HTTP ${res.status}`);
    return await res.json();
  },

  /**
   * Fetches pending KYC verification documents.
   */
  async getPendingKycDocuments(): Promise<any[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/documents/admin/pending`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });
    if (!res.ok) throw new Error(`KYC review queue returned HTTP ${res.status}`);
    return await res.json();
  },

  /**
   * Approves or rejects a customer KYC document.
   */
  async reviewKycDocument(id: number | string, status: 'APPROVED' | 'REJECTED', rejectionReason?: string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/documents/admin/${id}/review`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ status, rejectionReason }),
    });
    if (!res.ok) throw new Error(`Document review failed with HTTP ${res.status}`);
    return await res.json();
  },

  /**
   * Fetches platform-wide payment records.
   */
  async getAllPayments(): Promise<any[]> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/all`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
    });
    if (!res.ok) throw new Error(`Payments API returned HTTP ${res.status}`);
    return await res.json();
  },
};
