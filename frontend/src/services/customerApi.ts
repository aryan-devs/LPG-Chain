import { CustomerOrder, CustomerProfileData, SavedAddress, KycDocument, BillingInvoice, SupportTicket, NotificationItem, OrderStatus } from '../components/portal/portalTypes';
import { getApiBaseUrl } from './trackingApi';
import { TransparencyReport, DistributorDirectorySummary } from '../types/transparencyTypes';

function getAuthHeader(): Record<string, string> {
  const token = typeof window !== 'undefined'
    ? localStorage.getItem('token') || sessionStorage.getItem('token')
    : null;
  return token ? { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` } : {};
}

const DEFAULT_PROFILE: CustomerProfileData = {
  id: '1',
  name: 'Sneha Sharma',
  mobile: '+91 98765 43210',
  email: 'customer@example.com',
  consumerNumber: 'LPG-HH-98214',
  lpgId: '1729 4820 9102',
  address: 'Flat 304, Palm Grove, Pimpri, Pune, Maharashtra',
  city: 'Pune',
  state: 'Maharashtra',
  pincode: '411018',
  distributorName: 'City Gas Agency',
  distributorCode: 'DIST-PUN-042',
  distributorPhone: '+91 20 2742 8899',
  subsidizedQuotaLeft: 10,
  totalAnnualQuota: 12,
  kycStatus: 'VERIFIED',
};

export class CustomerService {

  // -------------------------------------------------------------
  // 1. Profile Management
  // -------------------------------------------------------------
  static async getProfile(): Promise<CustomerProfileData> {
    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/customer/profile`, {
        headers: { Accept: 'application/json', ...getAuthHeader() },
      });

      if (res.ok) {
        const data = await res.json();
        const profile: CustomerProfileData = {
          id: data.id || '1',
          name: data.name || 'Customer',
          mobile: data.phone || '+91 98765 43210',
          email: data.email || 'customer@example.com',
          consumerNumber: data.consumerNumber || `LPG-${data.id || '101'}`,
          lpgId: `1729 4820 ${data.id || '9102'}`,
          address: data.address || '',
          city: data.city || 'Pune',
          state: data.state || 'Maharashtra',
          pincode: data.pincode || '411018',
          distributorName: 'City Gas Agency',
          distributorCode: 'DIST-01',
          distributorPhone: '+91 20 2742 8899',
          subsidizedQuotaLeft: 10,
          totalAnnualQuota: 12,
          kycStatus: data.kycVerified ? 'VERIFIED' : 'PENDING',
        };
        if (typeof window !== 'undefined') {
          localStorage.setItem('lpg_customer_profile', JSON.stringify(profile));
        }
        return profile;
      }
    } catch {
      // Backend offline fallback
    }

    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('lpg_customer_profile');
      if (stored) {
        try { return JSON.parse(stored); } catch { /* ignore */ }
      }
    }
    return DEFAULT_PROFILE;
  }

  static async updateProfile(profile: Partial<CustomerProfileData>): Promise<CustomerProfileData> {
    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/customer/profile`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
        body: JSON.stringify({
          name: profile.name,
          phone: profile.mobile,
          address: profile.address,
          city: profile.city,
          state: profile.state,
          pincode: profile.pincode,
        }),
      });

      if (res.ok) {
        return await this.getProfile();
      }
    } catch {
      // Fallback
    }

    const current = await this.getProfile();
    const updated = { ...current, ...profile };
    if (typeof window !== 'undefined') {
      localStorage.setItem('lpg_customer_profile', JSON.stringify(updated));
      window.dispatchEvent(new CustomEvent('lpg_profile_updated', { detail: updated }));
    }
    return updated;
  }

  // -------------------------------------------------------------
  // 2. Saved Addresses
  // -------------------------------------------------------------
  static async getAddresses(): Promise<SavedAddress[]> {
    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/customer/addresses`, {
        headers: { Accept: 'application/json', ...getAuthHeader() },
      });

      if (res.ok) {
        const list = await res.json();
        if (Array.isArray(list) && list.length > 0) {
          const mapped: SavedAddress[] = list.map((a: any) => ({
            id: String(a.id),
            label: (a.addressType || 'Home') as any,
            fullAddress: a.addressLine1 || `${a.city}, ${a.state} - ${a.pincode}`,
            landmark: a.landmark,
            city: a.city,
            state: a.state,
            pincode: a.pincode,
            isDefault: !!a.isDefault,
          }));
          if (typeof window !== 'undefined') {
            localStorage.setItem('lpg_customer_addresses', JSON.stringify(mapped));
          }
          return mapped;
        }
      }
    } catch {
      // Fallback
    }

    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('lpg_customer_addresses');
      if (stored) {
        try { return JSON.parse(stored); } catch { /* ignore */ }
      }
    }
    return [];
  }

  static async addAddress(address: Omit<SavedAddress, 'id'>): Promise<SavedAddress> {
    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/customer/addresses`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
        body: JSON.stringify({
          addressLine1: address.fullAddress,
          landmark: address.landmark,
          city: address.city,
          state: address.state,
          pincode: address.pincode,
          addressType: address.label,
          isDefault: address.isDefault,
        }),
      });

      if (res.ok) {
        const saved = await res.json();
        return {
          id: String(saved.id),
          label: (saved.addressType || 'Home') as any,
          fullAddress: saved.addressLine1,
          landmark: saved.landmark,
          city: saved.city,
          state: saved.state,
          pincode: saved.pincode,
          isDefault: !!saved.isDefault,
        };
      }
    } catch {
      // Fallback
    }

    const current = await this.getAddresses();
    const newAddress: SavedAddress = { ...address, id: `addr-${Date.now()}` };
    if (newAddress.isDefault) {
      current.forEach((a) => (a.isDefault = false));
    }
    const updated = [...current, newAddress];
    if (typeof window !== 'undefined') {
      localStorage.setItem('lpg_customer_addresses', JSON.stringify(updated));
    }
    return newAddress;
  }

  static async deleteAddress(id: string): Promise<void> {
    try {
      const baseUrl = getApiBaseUrl();
      await fetch(`${baseUrl}/api/customer/addresses/${id}`, {
        method: 'DELETE',
        headers: getAuthHeader(),
      });
    } catch {
      // ignore
    }
    const addresses = (await this.getAddresses()).filter((a) => a.id !== id);
    if (typeof window !== 'undefined') {
      localStorage.setItem('lpg_customer_addresses', JSON.stringify(addresses));
    }
  }

  static async setDefaultAddress(id: string): Promise<void> {
    try {
      const baseUrl = getApiBaseUrl();
      await fetch(`${baseUrl}/api/customer/addresses/${id}/default`, {
        method: 'PUT',
        headers: getAuthHeader(),
      });
    } catch {
      // ignore
    }
  }

  // -------------------------------------------------------------
  // 3. Cylinder Refill Orders & Bookings
  // -------------------------------------------------------------
  static async getOrders(): Promise<CustomerOrder[]> {
    try {
      const baseUrl = getApiBaseUrl();
      const profile = await this.getProfile().catch(() => null);
      const customerId = profile?.id ? Number(profile.id) : null;

      const [bookingsRes, deliveriesRes] = await Promise.allSettled([
        fetch(`${baseUrl}/api/bookings`, {
          headers: { Accept: 'application/json', ...getAuthHeader() },
        }),
        customerId
          ? fetch(`${baseUrl}/api/deliveries/customer/${customerId}`, {
              headers: { Accept: 'application/json', ...getAuthHeader() },
            })
          : Promise.reject(new Error('No customer ID')),
      ]);

      if (bookingsRes.status === 'fulfilled' && bookingsRes.value.ok) {
        const bookings = await bookingsRes.value.json();
        if (Array.isArray(bookings)) {
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
              // ignore delivery parse errors
            }
          }

          const mapped: CustomerOrder[] = bookings.map((b: any) => {
            const del = deliveryMap.get(Number(b.id));
            let orderStatus: OrderStatus = b.status || 'PENDING';
            if (del) {
              if (del.status === 'OUT_FOR_DELIVERY') orderStatus = 'OUT_FOR_DELIVERY';
              else if (del.status === 'DELIVERED') orderStatus = 'DELIVERED';
              else if (del.status === 'ASSIGNED') orderStatus = 'ASSIGNED';
            }

            return {
              id: b.bookingNumber || `LPG${b.id}`,
              bookingIdNumber: b.id,
              cylinderType: (b.capacityKg === 5 ? '5 KG Mini' : b.capacityKg === 19 ? '19 KG Commercial' : '14.2 KG Domestic') as any,
              capacityKg: b.capacityKg || 14.2,
              quantity: b.quantity || 1,
              bookingDate: b.bookingDate ? new Date(b.bookingDate).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : 'Today',
              status: orderStatus,
              expectedDelivery: 'Today, 2:30 PM',
              amount: b.totalAmount || 853,
              subsidyAmount: b.subsidyAmount || (b.capacityKg === 14.2 ? 200 : 0),
              netPayable: b.netAmount || (b.totalAmount ? b.totalAmount - (b.subsidyAmount || 0) : 653),
              paymentStatus: (b.paymentStatus === 'PAID' ? 'PAID' : (b.paymentMethod === 'CASH_ON_DELIVERY' ? 'COD' : 'PENDING')),
              paymentMethod: (b.paymentMethod === 'CASH_ON_DELIVERY' ? 'Cash on Delivery' : 'UPI') as any,
              deliveryAddress: b.deliveryAddress || 'Saved Address',
              deliveryId: del ? String(del.id) : (b.deliveryId ? String(b.deliveryId) : String(b.id)),
              driverName: del?.driverName || b.driverName || undefined,
              driverPhone: del?.driverPhone || b.driverPhone || undefined,
              vehicleNumber: del?.vehicleNumber || del?.vehicleRegistration || b.vehicleNumber || undefined,
            };
          });
          return mapped;
        }
      }
    } catch {
      // Offline fallback
    }

    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('lpg_customer_orders');
      if (stored) {
        try { return JSON.parse(stored); } catch { /* ignore */ }
      }
    }
    return [];
  }

  static async createOrder(orderData: {
    cylinderType: '14.2 KG Domestic' | '5 KG Mini' | '19 KG Commercial';
    capacityKg: number;
    quantity: number;
    deliveryAddress: string;
    paymentMethod: 'UPI' | 'Card' | 'Cash on Delivery';
    distributorId?: number;
  }): Promise<CustomerOrder> {
    const profile = await this.getProfile().catch(() => null);
    const categoryCode = orderData.capacityKg === 5
      ? 'DOMESTIC_5_0'
      : orderData.capacityKg === 19
      ? 'COMMERCIAL_19_0'
      : 'DOMESTIC_14_2';

    const pMethod = orderData.paymentMethod === 'Cash on Delivery' ? 'CASH_ON_DELIVERY' : 'DEMO_QR';
    const baseUrl = getApiBaseUrl();

    const payload: Record<string, any> = {
      cylinderCategory: categoryCode,
      capacityKg: orderData.capacityKg,
      quantity: orderData.quantity,
      deliveryAddress: orderData.deliveryAddress,
      paymentMethod: pMethod,
    };

    if (orderData.distributorId !== undefined && orderData.distributorId !== null) {
      payload.distributorId = orderData.distributorId;
    }

    if (profile?.id && !isNaN(Number(profile.id))) {
      payload.customerId = Number(profile.id);
    }

    const res = await fetch(`${baseUrl}/api/bookings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      let errMsg = `Booking submission failed (HTTP ${res.status})`;
      try {
        const errJson = await res.json();
        errMsg = errJson.message || errJson.error || errMsg;
      } catch {
        try {
          const errText = await res.text();
          if (errText) errMsg = errText;
        } catch {}
      }
      throw new Error(errMsg);
    }

    const saved = await res.json();
    const created: CustomerOrder = {
      id: saved.bookingNumber || `LPG${saved.id}`,
      bookingIdNumber: saved.id,
      cylinderType: orderData.cylinderType,
      capacityKg: saved.capacityKg || orderData.capacityKg,
      quantity: saved.quantity || orderData.quantity,
      bookingDate: new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }),
      status: saved.status || 'PENDING',
      expectedDelivery: 'Today, 2:30 PM',
      amount: saved.totalAmount || (orderData.capacityKg === 19 ? 1785 : orderData.capacityKg === 5 ? 360 : 853),
      subsidyAmount: saved.subsidyAmount || (orderData.capacityKg === 14.2 ? 200 : 0),
      netPayable: saved.netAmount || (saved.totalAmount - (saved.subsidyAmount || 0)),
      paymentStatus: 'PENDING',
      paymentMethod: orderData.paymentMethod,
      deliveryAddress: saved.deliveryAddress || orderData.deliveryAddress,
      deliveryId: String(saved.id),
    };

    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('lpg_orders_updated', { detail: created }));
    }
    return created;
  }

  // -------------------------------------------------------------
  // 4. Payments, Demo QR & Invoices
  // -------------------------------------------------------------
  static async getDemoQr(bookingId: number | string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/qr/${bookingId}`, {
      headers: { Accept: 'application/json', ...getAuthHeader() },
    });
    if (!res.ok) throw new Error(`Demo QR failed with HTTP ${res.status}`);
    return await res.json();
  }

  static async simulatePayment(transactionReference: string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/simulate-success`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ transactionReference }),
    });
    if (!res.ok) {
      let err = `Simulate payment failed with HTTP ${res.status}`;
      try {
        const j = await res.json();
        if (j.message) err = j.message;
      } catch {}
      throw new Error(err);
    }
    return await res.json();
  }

  static async simulateFailure(transactionReference: string, reason?: string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/simulate-failure`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ transactionReference, reason: reason || 'Customer simulated payment cancellation' }),
    });
    if (!res.ok) {
      let err = `Payment failure simulation failed with HTTP ${res.status}`;
      try {
        const j = await res.json();
        if (j.message) err = j.message;
      } catch {}
      throw new Error(err);
    }
    return await res.json();
  }

  static async retryPayment(bookingId: number | string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/retry/${bookingId}`, {
      method: 'POST',
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) {
      let err = `Payment retry failed with HTTP ${res.status}`;
      try {
        const j = await res.json();
        if (j.message) err = j.message;
      } catch {}
      throw new Error(err);
    }
    return await res.json();
  }

  static async createRazorpayOrder(bookingId: number | string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/razorpay/order/${bookingId}`, {
      method: 'POST',
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) {
      let err = `Razorpay order creation failed with HTTP ${res.status}`;
      try {
        const j = await res.json();
        if (j.message) err = j.message;
      } catch {}
      throw new Error(err);
    }
    return await res.json();
  }

  static async verifyRazorpayPayment(payload: {
    bookingId: number | string;
    razorpayOrderId?: string;
    razorpayPaymentId?: string;
    razorpaySignature?: string;
    paymentReference?: string;
  }): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/razorpay/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      let err = `Payment verification failed with HTTP ${res.status}`;
      try {
        const j = await res.json();
        if (j.message) err = j.message;
      } catch {}
      throw new Error(err);
    }
    return await res.json();
  }

  static async confirmCod(bookingId: number | string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/confirm-cod`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ bookingId }),
    });
    if (!res.ok) throw new Error(`Confirm COD failed with HTTP ${res.status}`);
    return await res.json();
  }

  static async getInvoice(bookingId: number | string): Promise<BillingInvoice> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/payments/invoice/${bookingId}`, {
      headers: { Accept: 'application/json', ...getAuthHeader() },
    });
    if (!res.ok) throw new Error(`Invoice failed with HTTP ${res.status}`);
    const data = await res.json();
    return {
      invoiceNumber: data.invoiceNumber,
      bookingId: data.bookingNumber || String(data.bookingId),
      date: data.invoiceDate ? new Date(data.invoiceDate).toLocaleDateString('en-IN') : 'Today',
      amount: data.totalAmount,
      subsidyAmount: data.subsidyAmount,
      cylinderType: data.cylinderCategory || '14.2 KG Domestic',
      paymentMethod: data.paymentMethod || 'UPI',
      status: data.paymentStatus === 'PAID' ? 'PAID' : 'REFUNDED',
    };
  }

  static async getInvoices(): Promise<BillingInvoice[]> {
    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/payments/my`, {
        headers: { Accept: 'application/json', ...getAuthHeader() },
      });
      if (res.ok) {
        const payments = await res.json();
        if (Array.isArray(payments) && payments.length > 0) {
          return payments.map((p: any) => ({
            invoiceNumber: `INV-${p.bookingNumber || p.id}`,
            bookingId: p.bookingNumber || String(p.bookingId || p.id),
            date: p.paidAt ? new Date(p.paidAt).toLocaleDateString('en-IN') : 'Recent',
            amount: p.amount || 853,
            subsidyAmount: p.subsidyAmount || 200,
            cylinderType: '14.2 KG Domestic',
            paymentMethod: p.paymentMethod || 'DEMO_QR',
            status: p.paymentStatus === 'PAID' ? 'PAID' : 'REFUNDED',
          }));
        }
      }
    } catch {
      // Fallback
    }

    return [
      {
        invoiceNumber: 'INV-2026-1010',
        bookingId: 'LPG20261010',
        date: '10 Oct 2026',
        amount: 853,
        subsidyAmount: 200,
        cylinderType: '14.2 KG Domestic',
        paymentMethod: 'Demo QR (Simulated)',
        status: 'PAID',
      },
    ];
  }

  // -------------------------------------------------------------
  // 5. KYC & Connection Documents
  // -------------------------------------------------------------
  static async getKycSummary(): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/documents/summary`, {
      headers: { Accept: 'application/json', ...getAuthHeader() },
    });
    if (!res.ok) throw new Error(`KYC summary failed with HTTP ${res.status}`);
    return await res.json();
  }

  static async uploadKycDocument(file: File, documentType: string, documentNumber?: string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (documentNumber) {
      formData.append('documentNumber', documentNumber);
    }

    const res = await fetch(`${baseUrl}/api/documents/upload`, {
      method: 'POST',
      headers: { ...getAuthHeader() },
      body: formData,
    });

    if (!res.ok) {
      let errorMsg = `Upload failed with HTTP ${res.status}`;
      try {
        const errJson = await res.json();
        if (errJson.message) {
          errorMsg = errJson.message;
        } else if (errJson.error) {
          errorMsg = errJson.error;
        }
      } catch {
        // fallback to default
      }
      throw new Error(errorMsg);
    }
    return await res.json();
  }

  static getKycDocuments(): KycDocument[] {
    return [
      { id: 'kyc-1', type: 'Aadhaar Card', documentNumber: 'XXXX-XXXX-4812', status: 'VERIFIED', uploadDate: '12 Jan 2025' },
      { id: 'kyc-2', type: 'Ration Card', documentNumber: 'MH-PUN-082491', status: 'VERIFIED', uploadDate: '12 Jan 2025' },
      { id: 'kyc-3', type: 'Electricity Bill', documentNumber: 'MSEDCL-9081248', status: 'VERIFIED', uploadDate: '15 Aug 2026' },
      { id: 'kyc-4', type: 'Bank Passbook', documentNumber: 'HDFC-XXXXXX7891', status: 'VERIFIED', uploadDate: '12 Jan 2025' },
    ];
  }

  static updateKycDocument(_id: string, _fileName: string, _docNumber: string): KycDocument[] {
    return this.getKycDocuments();
  }

  // -------------------------------------------------------------
  // 6. Complaints & Grievance Tickets
  // -------------------------------------------------------------
  static async getTickets(): Promise<SupportTicket[]> {
    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/complaints/my`, {
        headers: { Accept: 'application/json', ...getAuthHeader() },
      });
      if (res.ok) {
        const list = await res.json();
        if (Array.isArray(list)) {
          return list.map((c: any) => ({
            ticketId: c.complaintNumber || `CMP-${c.id}`,
            subject: c.subject,
            category: (c.category === 'CYLINDER_LEAKAGE' ? 'Gas Leak / Emergency' :
                       c.category === 'DELIVERY_DELAY' ? 'Delivery Issue' :
                       c.category === 'PAYMENT_ISSUE' ? 'Billing & Subsidy' : 'General Query') as any,
            status: (c.status || 'OPEN') as any,
            createdDate: c.createdAt ? new Date(c.createdAt).toLocaleDateString('en-IN') : 'Recent',
            description: c.description,
          }));
        }
      }
    } catch {
      // Fallback
    }

    return [
      {
        ticketId: 'TCK-8821',
        subject: 'Query regarding DBTL subsidy amount',
        category: 'Billing & Subsidy',
        status: 'RESOLVED',
        createdDate: '15 Sep 2026',
        description: 'Wanted confirmation regarding September subsidy credit.',
      },
    ];
  }

  static async createTicket(ticket: Omit<SupportTicket, 'ticketId' | 'status' | 'createdDate'>): Promise<SupportTicket> {
    const categoryMap: Record<string, string> = {
      'Gas Leak / Emergency': 'CYLINDER_LEAKAGE',
      'Delivery Issue': 'DELIVERY_DELAY',
      'Billing & Subsidy': 'PAYMENT_ISSUE',
      'Cylinder Quality': 'REFILL_QUALITY',
      'General Query': 'OTHER',
    };

    const categoryCode = categoryMap[ticket.category] || 'OTHER';

    try {
      const baseUrl = getApiBaseUrl();
      const res = await fetch(`${baseUrl}/api/complaints`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
        body: JSON.stringify({
          category: categoryCode,
          subject: ticket.subject,
          description: ticket.description,
          priority: categoryCode === 'CYLINDER_LEAKAGE' ? 'CRITICAL' : 'MEDIUM',
        }),
      });

      if (res.ok) {
        const saved = await res.json();
        return {
          ticketId: saved.complaintNumber || `CMP-${saved.id}`,
          subject: saved.subject,
          category: ticket.category,
          status: saved.status || 'OPEN',
          createdDate: new Date().toLocaleDateString('en-IN'),
          description: saved.description,
        };
      }
    } catch {
      // Fallback
    }

    return {
      ticketId: `TCK-${Math.floor(1000 + Math.random() * 9000)}`,
      subject: ticket.subject,
      category: ticket.category,
      status: 'OPEN',
      createdDate: new Date().toLocaleDateString('en-IN'),
      description: ticket.description,
    };
  }

  // -------------------------------------------------------------
  // 7. AI Operations Assistant
  // -------------------------------------------------------------
  static async chatAiAssistant(message: string, intentId?: string): Promise<any> {
    const baseUrl = getApiBaseUrl();
    const res = await fetch(`${baseUrl}/api/ai/assistant/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
      body: JSON.stringify({ message, intentId }),
    });

    if (!res.ok) throw new Error(`AI Assistant returned HTTP ${res.status}`);
    return await res.json();
  }


  // -------------------------------------------------------------
  // 8. Notifications
  // -------------------------------------------------------------
  static getNotifications(): NotificationItem[] {
    return [
      {
        id: 'notif-1',
        title: 'Booking Confirmed',
        message: 'Your refill order has been scheduled for priority delivery.',
        timestamp: '10 mins ago',
        read: false,
        type: 'booking',
      },
      {
        id: 'notif-2',
        title: 'PAHAL Subsidy Credit',
        message: 'Direct benefit subsidy credited to your Aadhaar-linked account.',
        timestamp: '2 hours ago',
        read: true,
        type: 'subsidy',
      },
    ];
  }

  static markAllNotificationsRead(): void {
    // Local read state
  }

  // -------------------------------------------------------------
  // 9. Transparency Reports
  // -------------------------------------------------------------
  static async getTransparencyReport(distributorId: number | string, year?: number, month?: number, category?: string): Promise<TransparencyReport> {
    const params = new URLSearchParams();
    if (year) params.append('year', year.toString());
    if (month) params.append('month', month.toString());
    if (category) params.append('category', category);

    const qs = params.toString() ? `?${params.toString()}` : '';
    const res = await fetch(`${getApiBaseUrl()}/api/transparency/distributors/${distributorId}${qs}`);
    if (!res.ok) throw new Error(`Failed to load transparency report (Status: ${res.status})`);
    return res.json();
  }

  static async getReportingPeriods(): Promise<string[]> {
    try {
      const res = await fetch(`${getApiBaseUrl()}/api/transparency/periods`);
      if (res.ok) return await res.json();
    } catch {
      // ignore
    }
    return [];
  }

  static async getDistributorDirectory(): Promise<DistributorDirectorySummary[]> {
    try {
      const res = await fetch(`${getApiBaseUrl()}/api/distributors/public/search`);
      if (res.ok) return await res.json();
    } catch {
      // ignore
    }
    return [];
  }
}
