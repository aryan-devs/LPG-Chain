export type PortalTab =
  | 'dashboard'
  | 'book'
  | 'track'
  | 'ai'
  | 'transparency'
  | 'kyc'
  | 'profile'
  | 'addresses'
  | 'billing'
  | 'history'
  | 'support';

export interface CustomerProfileData {
  id: number | string;
  name: string;
  mobile: string;
  email: string;
  consumerNumber: string;
  lpgId: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  distributorName: string;
  distributorCode: string;
  distributorPhone: string;
  subsidizedQuotaLeft: number;
  totalAnnualQuota: number;
  kycStatus: 'VERIFIED' | 'PENDING' | 'REJECTED';
}

export type OrderStatus =
  | 'CONFIRMED'
  | 'ASSIGNED'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface CustomerOrder {
  id: string; // e.g. "LPG20261010"
  bookingIdNumber: number; // numeric id
  cylinderType: '14.2 KG Domestic' | '5 KG Mini' | '19 KG Commercial';
  capacityKg: number;
  quantity: number;
  bookingDate: string; // "10 Oct 2026, 10:15 AM"
  status: OrderStatus;
  expectedDelivery: string; // "Today, 2:30 PM"
  amount: number;
  subsidyAmount: number;
  netPayable: number;
  paymentStatus: 'PAID' | 'COD' | 'PENDING';
  paymentMethod: 'UPI' | 'Card' | 'Cash on Delivery';
  deliveryAddress: string;
  deliveryId?: string;
  driverName?: string;
  driverPhone?: string;
  vehicleNumber?: string;
  driverRating?: number;
  etaMinutes?: number;
  confirmedAt?: string;
  assignedAt?: string;
  dispatchedAt?: string;
  deliveredAt?: string;
  handoverInitiated?: boolean;
  handoverInitiatedAt?: string;
  otpExpiresAt?: string;
  otpVerifiedAt?: string;
  trackingLocation?: {
    latitude: number;
    longitude: number;
  };
}

export interface SavedAddress {
  id: string;
  label: 'Home' | 'Office' | 'Parents' | 'Other';
  fullAddress: string;
  landmark?: string;
  city: string;
  state: string;
  pincode: string;
  isDefault: boolean;
}

export interface KycDocument {
  id: string;
  type: 'Aadhaar Card' | 'Ration Card' | 'Electricity Bill' | 'Bank Passbook';
  documentNumber: string;
  status: 'VERIFIED' | 'PENDING_REVIEW' | 'NOT_UPLOADED';
  uploadDate?: string;
  fileName?: string;
  remarks?: string;
}

export interface BillingInvoice {
  invoiceNumber: string;
  bookingId: string;
  date: string;
  amount: number;
  subsidyAmount: number;
  cylinderType: string;
  paymentMethod: string;
  status: 'PAID' | 'REFUNDED';
}

export interface SupportTicket {
  ticketId: string;
  subject: string;
  category: 'Delivery Issue' | 'Gas Leak / Emergency' | 'Billing & Subsidy' | 'Cylinder Quality' | 'General Query';
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';
  createdDate: string;
  description: string;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  type: 'delivery' | 'booking' | 'kyc' | 'subsidy' | 'safety';
}
