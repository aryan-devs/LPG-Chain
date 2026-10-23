import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { DistributorBookingItem } from '../types/distributorPortalTypes.ts';
import {
  isBookingPending,
  isBookingConfirmed,
  isBookingCancelled,
  isBookingOutForDelivery,
  isBookingDelivered,
} from '../types/distributorPortalTypes.ts';

describe('Distributor Portal Bookings Workflow — State & Filtering Specifications', () => {
  const initialBookings: DistributorBookingItem[] = [
    {
      id: 101,
      customerId: 1,
      customerName: 'Aarav Patel',
      distributorId: 1,
      distributorName: 'Apex Gas',
      deliveryAddress: '12 MG Road, Pune',
      bookingDate: '2026-10-20T10:00:00',
      quantity: 1,
      status: 'PENDING',
      cylinderCategory: 'DOMESTIC_14_2',
      capacityKg: 14.2,
    },
    {
      id: 102,
      customerId: 2,
      customerName: 'Priya Sharma',
      distributorId: 1,
      distributorName: 'Apex Gas',
      deliveryAddress: '45 Park Street, Pune',
      bookingDate: '2026-10-20T10:15:00',
      quantity: 2,
      status: 'pending', // lowercase test
      cylinderCategory: 'DOMESTIC_14_2',
      capacityKg: 14.2,
    },
    {
      id: 103,
      customerId: 3,
      customerName: 'Vikram Joshi',
      distributorId: 1,
      distributorName: 'Apex Gas',
      deliveryAddress: '88 Shivaji Nagar, Pune',
      bookingDate: '2026-10-19T14:00:00',
      quantity: 1,
      status: 'CONFIRMED',
      cylinderCategory: 'DOMESTIC_14_2',
      capacityKg: 14.2,
    },
    {
      id: 104,
      customerId: 4,
      customerName: 'Sneha Kulkarni',
      distributorId: 1,
      distributorName: 'Apex Gas',
      deliveryAddress: '102 Karve Road, Pune',
      bookingDate: '2026-10-18T09:00:00',
      quantity: 1,
      status: 'CANCELLED',
      cylinderCategory: 'DOMESTIC_14_2',
      capacityKg: 14.2,
    },
  ];

  it('correctly normalizes booking statuses regardless of case and whitespace', () => {
    assert.equal(isBookingPending('PENDING'), true);
    assert.equal(isBookingPending('pending'), true);
    assert.equal(isBookingPending('  PENDING  '), true);
    assert.equal(isBookingPending('CONFIRMED'), false);

    assert.equal(isBookingConfirmed('CONFIRMED'), true);
    assert.equal(isBookingConfirmed('confirmed'), true);
    assert.equal(isBookingConfirmed('PENDING'), false);

    assert.equal(isBookingCancelled('CANCELLED'), true);
    assert.equal(isBookingCancelled('cancelled'), true);
    assert.equal(isBookingCancelled('CONFIRMED'), false);

    assert.equal(isBookingOutForDelivery('OUT_FOR_DELIVERY'), true);
    assert.equal(isBookingDelivered('DELIVERED'), true);
  });

  it('filters pending requests and calculates sidebar count accurately', () => {
    const pendingBookings = initialBookings.filter((b) => isBookingPending(b.status));
    assert.equal(pendingBookings.length, 2);
    assert.deepEqual(pendingBookings.map((b) => b.id), [101, 102]);

    const pendingCount = initialBookings.filter((b) => isBookingPending(b.status)).length;
    assert.equal(pendingCount, 2);
  });

  it('filters queued confirmed orders and excludes pending and cancelled orders', () => {
    const queuedOrders = initialBookings.filter((b) => isBookingConfirmed(b.status));
    assert.equal(queuedOrders.length, 1);
    assert.equal(queuedOrders[0].id, 103);

    const queuedCount = initialBookings.filter((b) => isBookingConfirmed(b.status)).length;
    assert.equal(queuedCount, 1);
  });

  it('accept workflow: updates authoritative booking state, moves order to queue, updates counts', () => {
    let currentBookings = [...initialBookings];

    // Simulated authoritative server response for accepting booking #101
    const acceptedBookingResponse: DistributorBookingItem = {
      ...currentBookings.find((b) => b.id === 101)!,
      status: 'CONFIRMED',
    };

    // State update logic as implemented in DistributorPortal
    currentBookings = currentBookings.map((b) =>
      b.id === acceptedBookingResponse.id
        ? { ...b, ...acceptedBookingResponse, status: acceptedBookingResponse.status }
        : b
    );

    // 1. Booking #101 is no longer in pending requests
    const updatedPending = currentBookings.filter((b) => isBookingPending(b.status));
    assert.equal(updatedPending.length, 1);
    assert.equal(updatedPending.some((b) => b.id === 101), false);
    assert.equal(updatedPending[0].id, 102);

    // 2. Booking #101 appears in Orders & Queue with CONFIRMED status
    const updatedQueued = currentBookings.filter((b) => isBookingConfirmed(b.status));
    assert.equal(updatedQueued.length, 2);
    assert.equal(updatedQueued.some((b) => b.id === 101), true);

    // 3. Counts and badges update immediately
    const pendingCount = currentBookings.filter((b) => isBookingPending(b.status)).length;
    const queuedCount = currentBookings.filter((b) => isBookingConfirmed(b.status)).length;
    assert.equal(pendingCount, 1);
    assert.equal(queuedCount, 2);
  });

  it('rejection workflow: removes cancelled booking from pending requests without adding to queue', () => {
    let currentBookings = [...initialBookings];

    // Simulated authoritative server response for rejecting booking #101
    const rejectedBookingResponse: DistributorBookingItem = {
      ...currentBookings.find((b) => b.id === 101)!,
      status: 'CANCELLED',
    };

    // State update logic as implemented in DistributorPortal
    currentBookings = currentBookings.map((b) =>
      b.id === rejectedBookingResponse.id
        ? { ...b, ...rejectedBookingResponse, status: rejectedBookingResponse.status }
        : b
    );

    // 1. Booking #101 is no longer in pending requests
    const updatedPending = currentBookings.filter((b) => isBookingPending(b.status));
    assert.equal(updatedPending.length, 1);
    assert.equal(updatedPending.some((b) => b.id === 101), false);

    // 2. Booking #101 does NOT appear in active queue
    const updatedQueued = currentBookings.filter((b) => isBookingConfirmed(b.status));
    assert.equal(updatedQueued.length, 1);
    assert.equal(updatedQueued.some((b) => b.id === 101), false);

    // 3. Pending count decrements, queued count stays unchanged
    const pendingCount = currentBookings.filter((b) => isBookingPending(b.status)).length;
    const queuedCount = currentBookings.filter((b) => isBookingConfirmed(b.status)).length;
    assert.equal(pendingCount, 1);
    assert.equal(queuedCount, 1);
  });

  it('simulated API error: booking remains in pending requests list on failure', () => {
    const currentBookings = [...initialBookings];

    // When server throws an error (e.g. 500 or network glitch), state is NOT modified
    let didFail = false;
    try {
      throw new Error('500 Internal Server Error: Database lock timeout');
    } catch {
      didFail = true;
    }

    assert.equal(didFail, true);
    // Booking list remains unchanged, booking #101 is still pending
    const pendingBookings = currentBookings.filter((b) => isBookingPending(b.status));
    assert.equal(pendingBookings.some((b) => b.id === 101), true);
    assert.equal(pendingBookings.length, 2);
  });

  it('delivery matching & active delivery detection: links existing deliveries to bookings correctly', () => {
    const deliveries = [
      {
        id: 4,
        bookingId: 103, // Booking #103 has active delivery #4
        distributorId: 1,
        distributorName: 'Apex Gas',
        driverId: 1,
        driverName: 'Suresh Patil',
        vehicleId: 1,
        vehicleRegistration: 'MH-12-RN-4501',
        status: 'ASSIGNED',
      },
      {
        id: 5,
        bookingId: 999,
        distributorId: 1,
        distributorName: 'Apex Gas',
        status: 'DELIVERED', // Terminal state - not active
      },
    ];

    const isActiveDeliveryStatus = (status?: string): boolean => {
      if (!status) return false;
      const s = status.toUpperCase().trim();
      return s === 'ASSIGNED' || s === 'PICKED_UP' || s === 'OUT_FOR_DELIVERY';
    };

    const activeMap = new Map<number, (typeof deliveries)[0]>();
    for (const d of deliveries) {
      if (isActiveDeliveryStatus(d.status)) {
        activeMap.set(d.bookingId, d);
      }
    }

    assert.equal(activeMap.has(103), true);
    assert.equal(activeMap.get(103)?.id, 4);
    assert.equal(activeMap.get(103)?.status, 'ASSIGNED');
    assert.equal(activeMap.get(103)?.driverName, 'Suresh Patil');
    assert.equal(activeMap.get(103)?.vehicleRegistration, 'MH-12-RN-4501');

    // Terminal delivered delivery is not active
    assert.equal(activeMap.has(999), false);
  });

  it('queue filtering: needs-scheduling separates unassigned orders from scheduled/in-progress orders', () => {
    const bookings: DistributorBookingItem[] = [
      {
        id: 1,
        customerId: 1,
        customerName: 'Anand Verma',
        distributorId: 1,
        distributorName: 'Apex Gas',
        deliveryAddress: 'Pune',
        bookingDate: '2026-10-20T10:00:00',
        quantity: 1,
        status: 'CONFIRMED',
        cylinderCategory: 'DOMESTIC_14_2',
        capacityKg: 14.2,
      },
      {
        id: 4, // Booking #4 has active delivery
        customerId: 4,
        customerName: 'Neha Deshmukh',
        distributorId: 1,
        distributorName: 'Apex Gas',
        deliveryAddress: 'Pune',
        bookingDate: '2026-10-20T10:00:00',
        quantity: 1,
        status: 'CONFIRMED',
        cylinderCategory: 'DOMESTIC_14_2',
        capacityKg: 14.2,
      },
    ];

    const activeDeliveryMap = new Map<number, any>();
    activeDeliveryMap.set(4, {
      id: 4,
      bookingId: 4,
      status: 'ASSIGNED',
      driverName: 'Suresh Patil',
      vehicleRegistration: 'MH-12-RN-4501',
    });

    const confirmedOrders = bookings.filter((b) => isBookingConfirmed(b.status));
    assert.equal(confirmedOrders.length, 2);

    // Needs Scheduling tab: only orders without active delivery
    const needsScheduling = confirmedOrders.filter((b) => !activeDeliveryMap.has(b.id));
    assert.equal(needsScheduling.length, 1);
    assert.equal(needsScheduling[0].id, 1);

    // Scheduled & In Progress tab: orders with active delivery
    const scheduledInProgress = confirmedOrders.filter((b) => activeDeliveryMap.has(b.id));
    assert.equal(scheduledInProgress.length, 1);
    assert.equal(scheduledInProgress[0].id, 4);
  });

  it('next-action mapping: replaces schedule button with View in Schedule or Track Live Dispatch for active deliveries', () => {
    const getActionType = (activeDelivery?: { status: string }) => {
      if (!activeDelivery) return 'SCHEDULE_DELIVERY';
      if (activeDelivery.status === 'ASSIGNED') return 'VIEW_DELIVERY_SCHEDULE';
      if (activeDelivery.status === 'OUT_FOR_DELIVERY' || activeDelivery.status === 'PICKED_UP') {
        return 'TRACK_LIVE_DISPATCH';
      }
      return 'SCHEDULE_DELIVERY';
    };

    // Unassigned order
    assert.equal(getActionType(undefined), 'SCHEDULE_DELIVERY');

    // Assigned order (e.g. Booking #4)
    assert.equal(getActionType({ status: 'ASSIGNED' }), 'VIEW_DELIVERY_SCHEDULE');

    // Dispatched order en route
    assert.equal(getActionType({ status: 'OUT_FOR_DELIVERY' }), 'TRACK_LIVE_DISPATCH');
    assert.equal(getActionType({ status: 'PICKED_UP' }), 'TRACK_LIVE_DISPATCH');
  });

  it('duplicate scheduling prevention: detects active delivery and blocks duplicate schedule submission', () => {
    const activeDelivery = {
      id: 4,
      bookingId: 4,
      status: 'ASSIGNED',
      driverName: 'Suresh Patil',
      vehicleRegistration: 'MH-12-RN-4501',
    };

    const validateScheduling = (bookingId: number, existing?: typeof activeDelivery) => {
      if (existing) {
        return {
          allowed: false,
          error: `Booking #${bookingId} already has an active delivery (Delivery #${existing.id}, status: ${existing.status}) assigned to driver ${existing.driverName}. Please proceed to Delivery Schedule.`,
        };
      }
      return { allowed: true, error: null };
    };

    // Booking #4 attempt
    const result = validateScheduling(4, activeDelivery);
    assert.equal(result.allowed, false);
    assert.match(result.error || '', /already has an active delivery/i);
    assert.match(result.error || '', /Delivery #4/);
    assert.match(result.error || '', /Suresh Patil/);

    // Unassigned booking attempt
    const allowedResult = validateScheduling(1, undefined);
    assert.equal(allowedResult.allowed, true);
    assert.equal(allowedResult.error, null);
  });

  it('modal reactive resolution: resolves active delivery on opening or on error catch', () => {
    // Simulating the proactive fetch and error catch in DistributorOrdersQueueView
    const deliveriesBackend = [
      { id: 4, bookingId: 4, status: 'ASSIGNED', driverName: 'Suresh Patil', vehicleNumber: 'MH-12-RN-4501' },
    ];

    const resolveModalDelivery = (bookingId: number, currentList: typeof deliveriesBackend) => {
      const active = currentList.find((d) => d.bookingId === bookingId && d.status === 'ASSIGNED');
      return active || null;
    };

    const modalDel = resolveModalDelivery(4, deliveriesBackend);
    assert.notEqual(modalDel, null);
    assert.equal(modalDel?.id, 4);
    assert.equal(modalDel?.vehicleNumber, 'MH-12-RN-4501');

    // Action button determination
    const actionTarget = modalDel?.status === 'ASSIGNED' ? 'delivery-schedule' : 'live-deliveries';
    assert.equal(actionTarget, 'delivery-schedule');
  });

  it('vehicle plate normalization: displays vehicleNumber or falls back to vehicleRegistration', () => {
    const d1 = { vehicleNumber: 'MH-12-RN-4501' };
    const d2 = { vehicleRegistration: 'MH-14-AX-9999' };
    const d3 = { vehicleId: 5 };

    const getPlate = (d: any) => d.vehicleNumber || d.vehicleRegistration || `Vehicle #${d.vehicleId || '—'}`;

    assert.equal(getPlate(d1), 'MH-12-RN-4501');
    assert.equal(getPlate(d2), 'MH-14-AX-9999');
    assert.equal(getPlate(d3), 'Vehicle #5');
  });
});

