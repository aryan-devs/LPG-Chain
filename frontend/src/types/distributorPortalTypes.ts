export type DistributorPortalTab =
  | 'overview'
  | 'booking-requests'
  | 'orders-queue'
  | 'inventory'
  | 'delivery-schedule'
  | 'live-deliveries'
  | 'transparency'
  | 'ai-forecast'
  | 'reports'
  | 'agency-profile';

export interface DistributorOverviewData {
  distributorId: number;
  distributorName: string;
  distributorCode: string;
  newBookingRequestsCount: number;
  acceptedQueuedOrdersCount: number;
  scheduledDeliveriesCount: number;
  dispatchedDeliveriesCount: number;
  completedDeliveriesCount: number;
  cancelledBookingsCount: number;
  totalFilledStock: number;
  totalEmptyStock: number;
  totalReservedStock: number;
  totalDamagedStock: number;
  totalAvailableStock: number;
  allocatedMonthlyTotal: number;
  soldMonthlyTotal: number;
  remainingAllocationTotal: number;
  averageQueueAgeHours: number;
  overdueOrdersCount: number;
  inventory: DistributorInventoryItem[];
  allocations: CategoryTransparencyItem[];
  recentBookings: DistributorBookingItem[];
}

export interface DistributorInventoryItem {
  id: number;
  distributorId: number;
  distributorName: string;
  cylinderCategory: string;
  categoryDisplayName: string;
  capacityKg: number;
  filledStock: number;
  emptyStock: number;
  reservedStock: number;
  damagedStock: number;
  availableStock: number;
  lastAuditedAt?: string;
  lastAuditedBy?: string;
}

export interface InventoryMovementItem {
  id: number;
  inventoryId: number;
  cylinderCategory: string;
  movementType: string;
  quantityChange: number;
  reason: string;
  referenceNumber?: string;
  performedBy: string;
  performedAt: string;
}

export interface DistributorBookingItem {
  id: number;
  customerId: number;
  customerName: string;
  distributorId: number;
  distributorName: string;
  deliveryAddress: string;
  bookingDate: string;
  quantity: number;
  status: string; // PENDING, CONFIRMED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
  cylinderCategory: string;
  capacityKg: number;
}

export interface DistributorDeliveryItem {
  id: number;
  bookingId: number;
  customerName?: string;
  deliveryAddress?: string;
  distributorId: number;
  distributorName: string;
  driverId?: number;
  driverName?: string;
  driverPhone?: string;
  vehicleId?: number;
  vehicleRegistration?: string;
  vehicleNumber?: string;
  status: string; // ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, FAILED, CANCELLED
  assignedAt?: string;
  dispatchedAt?: string;
  deliveredAt?: string;
  latitude?: number;
  longitude?: number;
  speed?: number;
  heading?: number;
  lastLocationUpdate?: string;
}

export interface CategoryTransparencyItem {
  cylinderCategory: string;
  categoryDisplayName: string;
  capacityKg: number;
  allocated: boolean;
  allocatedQuantity?: number;
  deliveredQuantity: number;
  adjustmentQuantity: number;
  netAllocatedQuantity?: number;
  remainingAllocation?: number;
  utilizationPercentage?: number;
  sourceReference?: string;
  lastUpdated?: string;
  statusMessage?: string;
}

export interface ForecastDailyItem {
  date: string;
  day_name: string;
  predicted_quantity: number;
  confidence_lower?: number;
  confidence_upper?: number;
}

export interface ForecastCategoryItem {
  cylinder_category: string;
  category_display_name: string;
  capacity_kg: number;
  service_type: string;
  total_predicted_quantity: number;
  daily_forecast: ForecastDailyItem[];
}

export interface ForecastResponse {
  distributor_id?: number;
  horizon_days: number;
  forecast_generated_at: string;
  model_version: string;
  data_source: string; // SYNTHETIC_TRAINING_DATA
  disclaimer: string;
  evaluation_metrics: {
    model_mae?: number;
    model_rmse?: number;
    baseline_mae?: number;
    baseline_rmse?: number;
    mae_improvement_pct?: number;
  };
  total_predicted_demand: number;
  category_forecasts: ForecastCategoryItem[];
  daily_summary: { date: string; total_predicted_quantity: number }[];
}

export interface ForecastMetricsData {
  model_version: string;
  algorithm: string;
  data_source: string;
  training_timestamp: string;
  disclaimer: string;
  dataset_summary: {
    total_bookings: number;
    date_range: string;
    train_samples: number;
    test_samples: number;
  };
  overall_metrics: {
    model_mae: number;
    model_rmse: number;
    baseline_mae: number;
    baseline_rmse: number;
    mae_improvement_pct: number;
  };
  category_metrics: Record<string, {
    mae: number;
    rmse: number;
    mean_actual: number;
    mean_predicted: number;
  }>;
  feature_importance: Record<string, number>;
}

export const isBookingPending = (status?: string | null): boolean =>
  (status || '').trim().toUpperCase() === 'PENDING';

export const isBookingConfirmed = (status?: string | null): boolean =>
  (status || '').trim().toUpperCase() === 'CONFIRMED';

export const isBookingCancelled = (status?: string | null): boolean =>
  (status || '').trim().toUpperCase() === 'CANCELLED';

export const isBookingOutForDelivery = (status?: string | null): boolean =>
  (status || '').trim().toUpperCase() === 'OUT_FOR_DELIVERY';

export const isBookingDelivered = (status?: string | null): boolean =>
  (status || '').trim().toUpperCase() === 'DELIVERED';
