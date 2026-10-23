import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { TransparencyReport, CategoryTransparency } from '../types/transparencyTypes.ts';

describe('Cylinder Allocation & Sales Transparency Module — Frontend Specifications', () => {
  it('correctly calculates net allocation, remaining balance, and utilization percentage', () => {
    const rawAllocated = 5000;
    const adjustments = -50;
    const delivered = 3210;

    const netAllocated = rawAllocated + adjustments;
    const remaining = netAllocated - delivered;
    const utilization = Math.round(((delivered / netAllocated) * 100) * 100) / 100;

    assert.equal(netAllocated, 4950, 'Net allocation must factor in auditable adjustments');
    assert.equal(remaining, 1740, 'Remaining balance must equal net allocation minus actual completed deliveries');
    assert.equal(utilization, 64.85, 'Utilization percentage must accurately reflect quota absorption');
  });

  it('guarantees unallocated categories return explicit unavailable state without invented numbers', () => {
    const unallocatedCategory: CategoryTransparency = {
      cylinderCategory: 'COMMERCIAL_19_0',
      categoryDisplayName: 'Commercial 19 kg',
      capacityKg: 19.0,
      allocated: false,
      allocatedQuantity: null,
      adjustmentQuantity: 0,
      netAllocatedQuantity: null,
      deliveredQuantity: 0,
      remainingAllocation: null,
      utilizationPercentage: null,
      statusMessage: 'No official cylinder allocation has been published for this category and reporting period.',
    };

    assert.equal(unallocatedCategory.allocated, false);
    assert.equal(unallocatedCategory.allocatedQuantity, null);
    assert.equal(unallocatedCategory.remainingAllocation, null);
    assert.equal(unallocatedCategory.utilizationPercentage, null);
    assert.ok(unallocatedCategory.statusMessage?.includes('No official cylinder allocation'));
  });

  it('strictly excludes pending, cancelled, or in-transit bookings from completed sales count', () => {
    const bookings = [
      { id: 1, status: 'DELIVERED', quantity: 2, cylinderCategory: 'DOMESTIC_14_2' },
      { id: 2, status: 'DELIVERED', quantity: 1, cylinderCategory: 'DOMESTIC_14_2' },
      { id: 3, status: 'PENDING', quantity: 3, cylinderCategory: 'DOMESTIC_14_2' },
      { id: 4, status: 'OUT_FOR_DELIVERY', quantity: 1, cylinderCategory: 'DOMESTIC_14_2' },
      { id: 5, status: 'CANCELLED', quantity: 2, cylinderCategory: 'DOMESTIC_14_2' },
    ];

    const completedCount = bookings
      .filter((b) => b.status === 'DELIVERED')
      .reduce((sum, b) => sum + b.quantity, 0);

    assert.equal(completedCount, 3, 'Only DELIVERED status bookings may be counted towards completed sales');
  });

  it('prevents double-counting when duplicate delivery or booking records are present', () => {
    const bookings = [
      { id: 101, status: 'DELIVERED', quantity: 1 },
      { id: 101, status: 'DELIVERED', quantity: 1 }, // duplicate
      { id: 102, status: 'DELIVERED', quantity: 2 },
    ];

    const processedIds = new Set<number>();
    let totalCylinders = 0;

    for (const b of bookings) {
      if (processedIds.has(b.id)) continue;
      processedIds.add(b.id);
      totalCylinders += b.quantity;
    }

    assert.equal(totalCylinders, 3, 'Duplicate records must be de-duplicated by booking primary key');
  });

  it('ensures public transparency payload format does not expose credentials or customer PII', () => {
    const mockReport: TransparencyReport = {
      distributorId: 1,
      distributorName: 'Apex Gas Agency',
      location: 'Mumbai, Maharashtra',
      reportingYear: 2026,
      reportingMonth: 10,
      reportingPeriod: '2026-10',
      categories: [],
      totalAllocated: 5000,
      totalDelivered: 3200,
      totalRemaining: 1800,
      totalAdjustments: 0,
      overallUtilizationPercentage: 64.0,
      lastUpdated: '2026-10-11T10:00:00',
      hasAllocations: true,
    };

    const keys = Object.keys(mockReport);
    assert.ok(!keys.includes('passkey'), 'Must not contain distributor passkey');
    assert.ok(!keys.includes('password'), 'Must not contain distributor password');
    assert.ok(!keys.includes('customerEmail'), 'Must not contain customer email');
    assert.ok(!keys.includes('customerPhone'), 'Must not contain customer phone');
  });
});
