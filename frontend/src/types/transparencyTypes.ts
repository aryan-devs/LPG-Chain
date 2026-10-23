export interface CategoryTransparency {
  cylinderCategory: string; // "DOMESTIC_14_2" | "DOMESTIC_5_0" | "COMMERCIAL_19_0"
  categoryDisplayName: string; // "Domestic 14.2 kg"
  capacityKg: number; // 14.2
  allocated: boolean; // true / false
  allocatedQuantity: number | null; // e.g. 5000
  adjustmentQuantity: number; // e.g. -50
  netAllocatedQuantity: number | null; // e.g. 4950
  deliveredQuantity: number; // e.g. 3210
  remainingAllocation: number | null; // e.g. 1740
  utilizationPercentage: number | null; // e.g. 64.85
  sourceReference?: string;
  lastUpdated?: string;
  statusMessage?: string;
}

export interface TransparencyReport {
  distributorId: number;
  distributorName: string;
  location?: string;
  reportingYear: number;
  reportingMonth: number;
  reportingPeriod: string; // "YYYY-MM"
  categories: CategoryTransparency[];
  totalAllocated: number;
  totalDelivered: number;
  totalRemaining: number;
  totalAdjustments: number;
  overallUtilizationPercentage: number;
  lastUpdated: string;
  hasAllocations: boolean;
}

export interface DistributorDirectorySummary {
  id: number;
  agencyName: string;
  licenseNumber?: string;
  contactNumber: string;
  address: string;
  pinCode: string;
  city: string;
  state: string;
  latitude?: number;
  longitude?: number;
  supportedServices?: string;
  operatingHours?: string;
  networkType?: string;
  status?: string;
}
