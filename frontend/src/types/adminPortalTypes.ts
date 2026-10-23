export type AdminPortalTab =
  | 'overview'
  | 'users'
  | 'distributors'
  | 'bookings-deliveries'
  | 'inventory-allocations'
  | 'drivers-vehicles'
  | 'reports-analytics'
  | 'ai-demand-forecast'
  | 'roles-security'
  | 'audit-logs'
  | 'system-settings'
  | 'kyc-documents'
  | 'complaints';

// -------------------------------------------------------------
// 1. Overview Dashboard Types
// -------------------------------------------------------------
export interface AdminKpiSummary {
  totalCustomers: number;
  totalDistributors: number;
  totalBookings: number;
  pendingBookings: number;
  activeDeliveries: number;
  completedDeliveries: number;
  totalCylinderInventory: number;
  activeAlerts: number;
  isPlaceholder?: boolean;
}

export interface BookingTrendPoint {
  date: string;
  label: string;
  bookings: number;
  deliveries: number;
}

export interface DeliveryStatusDistribution {
  status: 'PENDING' | 'ASSIGNED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';
  count: number;
  percentage: number;
  color: string;
}

export interface DistributorPerformanceSummary {
  id: number;
  name: string;
  code: string;
  city: string;
  activeBookings: number;
  completedDeliveries: number;
  onTimeDeliveryRate: number;
  allocationUtilization: number;
  status: 'ACTIVE' | 'PENDING_VERIFICATION' | 'SUSPENDED';
}

export interface SystemActivityItem {
  id: string;
  timestamp: string;
  actor: string;
  actorRole: string;
  action: string;
  entity: string;
  status: 'SUCCESS' | 'WARNING' | 'FAILED';
  details?: string;
}

export interface CategoryStockBreakdown {
  category: string;
  displayName: string;
  capacityKg: number;
  filledStock: number;
  emptyStock: number;
  reservedStock: number;
  damagedStock: number;
  availableStock: number;
  allocatedMonthly: number;
  deliveredMonthly: number;
}

// -------------------------------------------------------------
// 2. User Management Types
// -------------------------------------------------------------
export type PlatformRole = 'ADMIN' | 'DISTRIBUTOR' | 'DRIVER' | 'CONSUMER';
export type UserAccountStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface AdminUserItem {
  id: number;
  name: string;
  email: string;
  phone: string;
  address?: string;
  role: PlatformRole;
  status: UserAccountStatus;
  registrationDate: string;
  lastLoginAt?: string;
  distributorId?: number;
  distributorName?: string;
}

export interface UserFilterState {
  searchQuery: string;
  roleFilter: string;
  statusFilter: string;
}

// -------------------------------------------------------------
// 3. Distributor Management Types
// -------------------------------------------------------------
export type DistributorAccountStatus = 'ACTIVE' | 'SUSPENDED';
export type DistributorVerificationStatus = 'VERIFIED' | 'PENDING_VERIFICATION' | 'REJECTED';

export interface AdminDistributorItem {
  id: number;
  agencyName: string;
  distributorCode: string;
  licenceNumber: string;
  contactNumber: string;
  email?: string;
  address: string;
  city: string;
  state: string;
  pinCode: string;
  accountStatus: DistributorAccountStatus;
  verificationStatus: DistributorVerificationStatus;
  supportedServices: 'DOMESTIC' | 'COMMERCIAL' | 'BOTH';
  operatingHours?: string;
  activeMembersCount: number;
  vehiclesCount: number;
  driversCount: number;
  serviceRadiusKm: number;
  coveredPinCodes: string[];
  joinedDate: string;
}

// -------------------------------------------------------------
// 4. Bookings & Deliveries Types
// -------------------------------------------------------------
export type PlatformBookingStatus = 'PENDING' | 'CONFIRMED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';
export type PlatformDeliveryStatus = 'UNASSIGNED' | 'ASSIGNED' | 'PICKED_UP' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED';

export interface AdminBookingItem {
  id: number;
  bookingNumber: string;
  customerId: number;
  customerName: string;
  customerPhone: string;
  customerEmail?: string;
  deliveryAddress: string;
  distributorId: number;
  distributorName: string;
  cylinderCategory: string;
  cylinderDisplayName: string;
  capacityKg: number;
  quantity: number;
  bookingDate: string;
  bookingStatus: PlatformBookingStatus;
  deliveryStatus: PlatformDeliveryStatus;
  assignedDriverId?: number;
  assignedDriverName?: string;
  assignedDriverPhone?: string;
  assignedVehicleId?: number;
  assignedVehicleReg?: string;
  estimatedDeliveryDate?: string;
  deliveredAt?: string;
  isOverdue?: boolean;
  exceptionNote?: string;
}

export interface DeliveryTimelineStep {
  title: string;
  description: string;
  timestamp?: string;
  status: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING' | 'FAILED';
}

// -------------------------------------------------------------
// 5. Inventory & Allocations Types
// -------------------------------------------------------------
export interface DistributorInventorySummary {
  distributorId: number;
  distributorName: string;
  distributorCode: string;
  city: string;
  totalFilled: number;
  totalEmpty: number;
  totalReserved: number;
  totalDamaged: number;
  totalAvailable: number;
  discrepancyCount: number;
  lastAuditedAt?: string;
  categories: CategoryStockBreakdown[];
}

export interface OmcAllocationItem {
  id: number;
  distributorId: number;
  distributorName: string;
  distributorCode: string;
  cylinderCategory: string;
  categoryDisplayName: string;
  capacityKg: number;
  allocationMonth: number;
  allocationYear: number;
  allocatedQuantity: number;
  deliveredQuantity: number;
  adjustmentQuantity: number;
  remainingQuantity: number;
  sourceReference: string;
  status: 'ACTIVE' | 'EXHAUSTED' | 'CLOSED';
  createdAt: string;
  updatedAt?: string;
}

export interface AllocationAdjustmentItem {
  id: number;
  allocationId: number;
  adjustmentType: 'EMERGENCY_TRANSFER' | 'QUOTA_INCREASE' | 'QUOTA_REDUCTION' | 'INTER_DEPOT_RETURN';
  quantityChange: number;
  reason: string;
  referenceNumber: string;
  approvedBy: string;
  approvedAt: string;
}

// -------------------------------------------------------------
// 6. Drivers & Vehicles Types
// -------------------------------------------------------------
export type DriverStatus = 'AVAILABLE' | 'ON_DELIVERY' | 'OFF_DUTY' | 'INACTIVE';
export type VehicleStatus = 'ACTIVE' | 'IN_TRANSIT' | 'MAINTENANCE' | 'DECOMMISSIONED';

export interface AdminDriverItem {
  id: number;
  name: string;
  phone: string;
  licenseNumber: string;
  distributorId: number;
  distributorName: string;
  status: DriverStatus;
  activeDeliveryId?: number;
  assignedVehicleReg?: string;
  completedDeliveriesTotal: number;
  rating?: number;
  joinedDate: string;
}

export interface AdminVehicleItem {
  id: number;
  registrationNumber: string;
  vehicleType: 'MINI_TRUCK' | 'HEAVY_TRUCK' | 'VAN' | 'THREE_WHEELER';
  capacityUnits: number;
  distributorId: number;
  distributorName: string;
  status: VehicleStatus;
  assignedDriverName?: string;
  gpsDeviceId?: string;
  lastLocationUpdate?: string;
  insuranceValidUntil: string;
}

// -------------------------------------------------------------
// 7. Reports & Analytics Types
// -------------------------------------------------------------
export type AnalyticsDateRange = '7D' | '30D' | 'THIS_MONTH' | 'LAST_QUARTER' | 'YTD';

export interface AnalyticsOverviewMetrics {
  totalOrdersFulfilled: number;
  onTimeFulfillmentRate: number;
  averageDispatchTimeHours: number;
  totalVolumeDeliveredTons: number;
  cancellationRate: number;
  quotaCompliancePercentage: number;
}

export interface MonthlyDeliveryTrend {
  month: string;
  domesticVolume: number;
  commercialVolume: number;
  industrialVolume: number;
  totalVolume: number;
}

// -------------------------------------------------------------
// 8. AI Demand Forecast Types
// -------------------------------------------------------------
export interface PlatformForecastDaily {
  date: string;
  dayName: string;
  predictedDemand: number;
  confidenceLower: number;
  confidenceUpper: number;
  historicalComparison?: number;
}

export interface PlatformCategoryForecast {
  category: string;
  displayName: string;
  capacityKg: number;
  predictedTotal: number;
  dailyPoints: PlatformForecastDaily[];
}

export interface PlatformForecastResult {
  horizonDays: 7 | 14 | 30;
  generatedAt: string;
  modelVersion: string;
  algorithmName: string;
  dataSourceType: 'SYNTHETIC_TRAINING_DATA' | 'LIVE_DEPOT_HISTORY';
  syntheticDataDisclaimer: string;
  evaluationMetrics: {
    modelMae: number;
    modelRmse: number;
    baselineMae: number;
    baselineRmse: number;
    maeImprovementPct: number;
    confidenceLevel: string;
  };
  totalAggregatedDemand: number;
  categoryForecasts: PlatformCategoryForecast[];
}

// -------------------------------------------------------------
// 9. Roles & Security Types
// -------------------------------------------------------------
export interface PermissionMatrixItem {
  module: string;
  description: string;
  adminPerm: 'FULL_CONTROL' | 'READ_WRITE' | 'READ_ONLY' | 'NONE';
  distributorPerm: 'FULL_CONTROL' | 'READ_WRITE' | 'READ_ONLY' | 'NONE';
  consumerPerm: 'FULL_CONTROL' | 'READ_WRITE' | 'READ_ONLY' | 'NONE';
  driverPerm: 'FULL_CONTROL' | 'READ_WRITE' | 'READ_ONLY' | 'NONE';
}

export interface SecurityAuditEvent {
  id: string;
  timestamp: string;
  eventType: 'LOGIN_SUCCESS' | 'LOGIN_FAILED' | 'ROLE_ELEVATION_ATTEMPT' | 'CROSS_ORG_DENIAL' | 'TOKEN_EXPIRED';
  actor: string;
  ipAddress: string;
  resourceTarget: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  details: string;
}

export interface DistributorMembershipItem {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  distributorId: number;
  distributorName: string;
  membershipRole: 'OWNER' | 'MANAGER' | 'DISPATCHER';
  status: 'ACTIVE' | 'REVOKED';
  grantedAt: string;
}

// -------------------------------------------------------------
// 10. Audit Logs Types
// -------------------------------------------------------------
export interface AdminAuditRecord {
  id: string;
  timestamp: string;
  actor: string;
  actorRole: PlatformRole | 'SYSTEM';
  action: string;
  entityType: 'BOOKING' | 'DELIVERY' | 'ALLOCATION' | 'USER' | 'DISTRIBUTOR' | 'SECURITY';
  entityId: string;
  outcome: 'SUCCESS' | 'DENIED' | 'FAILED';
  sourceIp: string;
  summary: string;
}

export interface AuditLogFilterState {
  actionType: string;
  actorQuery: string;
  dateRange: string;
}

// -------------------------------------------------------------
// 11. System Settings Types
// -------------------------------------------------------------
export interface SupportedCylinderConfig {
  code: string;
  displayName: string;
  capacityKg: number;
  tareWeightKg: number;
  serviceType: 'DOMESTIC' | 'COMMERCIAL';
  regulatedPriceInr: number;
  status: 'ACTIVE' | 'RESTRICTED';
}

export interface ServiceHealthIndicator {
  serviceName: string;
  portOrEndpoint: string;
  connectionState: 'STANDBY' | 'CONNECTED' | 'DISCONNECTED';
  protocol: string;
  notes: string;
}

export interface PlatformSystemSettings {
  platformVersion: string;
  environmentName: string;
  officialEmailDomains: {
    adminDomains: string[];
    distributorDomains: string[];
  };
  supportedCategories: SupportedCylinderConfig[];
  services: ServiceHealthIndicator[];
  sessionTimeoutMinutes: number;
  maxFailedLoginAttempts: number;
}
