import React, { useState, useEffect, useMemo } from 'react';
import {
  Clock,
  Calendar,
  AlertTriangle,
  Truck,
  RefreshCw,
  Search,
  MapPin,
  Flame,
  CalendarDays,
  Navigation,
  Info,
} from 'lucide-react';
import {
  DistributorBookingItem,
  DistributorDeliveryItem,
  DistributorPortalTab,
  isBookingConfirmed,
} from '../../../types/distributorPortalTypes';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorOrdersQueueViewProps {
  bookings: DistributorBookingItem[];
  deliveries?: DistributorDeliveryItem[];
  distributorId: number;
  isLoading: boolean;
  onRefresh: () => void;
  onOrderScheduled: () => void;
  onNavigateTab?: (tab: DistributorPortalTab) => void;
}

type QueueSubTab = 'needs-scheduling' | 'scheduled-in-progress' | 'all';

export const DistributorOrdersQueueView: React.FC<DistributorOrdersQueueViewProps> = ({
  bookings,
  deliveries = [],
  distributorId,
  isLoading,
  onRefresh,
  onOrderScheduled,
  onNavigateTab,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [queueSubTab, setQueueSubTab] = useState<QueueSubTab>('needs-scheduling');
  const [scheduleModalBooking, setScheduleModalBooking] = useState<DistributorBookingItem | null>(null);
  const [modalActiveDelivery, setModalActiveDelivery] = useState<DistributorDeliveryItem | null>(null);
  const [drivers, setDrivers] = useState<any[]>([]);
  const [vehicles, setVehicles] = useState<any[]>([]);
  const [selectedDriverId, setSelectedDriverId] = useState<number | ''>('');
  const [selectedVehicleId, setSelectedVehicleId] = useState<number | ''>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [scheduleError, setScheduleError] = useState<string | null>(null);

  // Helper to check if a delivery is active (not in terminal state)
  const isActiveDeliveryStatus = (status?: string): boolean => {
    if (!status) return false;
    const s = status.toUpperCase().trim();
    return s === 'ASSIGNED' || s === 'PICKED_UP' || s === 'OUT_FOR_DELIVERY';
  };

  // Map active deliveries by bookingId
  const activeDeliveryByBookingId = useMemo(() => {
    const map = new Map<number, DistributorDeliveryItem>();
    if (deliveries && deliveries.length > 0) {
      for (const d of deliveries) {
        if (isActiveDeliveryStatus(d.status)) {
          const bId = Number(d.bookingId ?? (d as any).booking_id);
          if (!isNaN(bId)) {
            const prev = map.get(bId);
            if (!prev || d.id > prev.id) {
              map.set(bId, d);
            }
          }
        }
      }
    }
    return map;
  }, [deliveries]);

  // Load drivers, vehicles, and verify active delivery state when modal opens
  useEffect(() => {
    if (scheduleModalBooking) {
      const bId = Number(scheduleModalBooking.id);
      const cached = activeDeliveryByBookingId.get(bId);
      if (cached) {
        setModalActiveDelivery(cached);
      } else {
        setModalActiveDelivery(null);
        // Proactively fetch authoritative state from backend for this booking
        DistributorApiService.getDeliveriesByBooking(bId)
          .then((dels) => {
            const active = dels.find((d) => isActiveDeliveryStatus(d.status));
            if (active) {
              setModalActiveDelivery(active);
              onRefresh();
            }
          })
          .catch(() => {
            // Ignore error if offline or no deliveries
          });
      }

      DistributorApiService.getDrivers(distributorId).then((res) => {
        setDrivers(res);
        const avail = res.find((d) => (d.status || '').toUpperCase() === 'AVAILABLE');
        if (avail) setSelectedDriverId(avail.id);
        else if (res.length > 0) setSelectedDriverId(res[0].id);
      });
      DistributorApiService.getVehicles(distributorId).then((res) => {
        setVehicles(res);
        const avail = res.find((v) => (v.status || '').toUpperCase() === 'AVAILABLE');
        if (avail) setSelectedVehicleId(avail.id);
        else if (res.length > 0) setSelectedVehicleId(res[0].id);
      });
    } else {
      setModalActiveDelivery(null);
    }
  }, [scheduleModalBooking, distributorId, activeDeliveryByBookingId, onRefresh]);

  // Filter only CONFIRMED orders in queue using robust case/enum normalization
  const queuedOrders = useMemo(() => {
    return bookings.filter((b) => isBookingConfirmed(b.status));
  }, [bookings]);

  // Split into categories: Needs Scheduling vs Scheduled & In Progress
  const needsSchedulingOrders = useMemo(() => {
    return queuedOrders.filter((b) => !activeDeliveryByBookingId.has(Number(b.id)));
  }, [queuedOrders, activeDeliveryByBookingId]);

  const scheduledOrders = useMemo(() => {
    return queuedOrders.filter((b) => activeDeliveryByBookingId.has(Number(b.id)));
  }, [queuedOrders, activeDeliveryByBookingId]);

  // Select list based on active subTab
  const visibleByTab = useMemo(() => {
    if (queueSubTab === 'needs-scheduling') return needsSchedulingOrders;
    if (queueSubTab === 'scheduled-in-progress') return scheduledOrders;
    return queuedOrders;
  }, [queueSubTab, needsSchedulingOrders, scheduledOrders, queuedOrders]);

  const filtered = useMemo(() => {
    const q = searchTerm.toLowerCase().trim();
    if (!q) return visibleByTab;
    return visibleByTab.filter((b) => {
      const activeDel = activeDeliveryByBookingId.get(Number(b.id));
      const veh = activeDel ? (activeDel.vehicleNumber || activeDel.vehicleRegistration || '') : '';
      return (
        b.id.toString().includes(q) ||
        b.customerName.toLowerCase().includes(q) ||
        b.deliveryAddress.toLowerCase().includes(q) ||
        b.cylinderCategory.toLowerCase().includes(q) ||
        (activeDel && (
          activeDel.id.toString().includes(q) ||
          (activeDel.driverName && activeDel.driverName.toLowerCase().includes(q)) ||
          veh.toLowerCase().includes(q)
        ))
      );
    });
  }, [visibleByTab, searchTerm, activeDeliveryByBookingId]);

  const getQueueAge = (bookingDateStr: string) => {
    try {
      const bDate = new Date(bookingDateStr);
      const now = new Date();
      const diffMs = now.getTime() - bDate.getTime();
      const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      return { days, hours, isOverdue: days >= 7, isWarning: days >= 5 && days < 7 };
    } catch {
      return { days: 0, hours: 0, isOverdue: false, isWarning: false };
    }
  };

  const handleConfirmSchedule = async () => {
    if (!scheduleModalBooking || !selectedDriverId || !selectedVehicleId) {
      setScheduleError('Please select both an assigned driver and delivery vehicle');
      return;
    }

    // Safety check: prevent duplicate scheduling if active delivery is already known
    const existing = modalActiveDelivery || activeDeliveryByBookingId.get(Number(scheduleModalBooking.id));
    if (existing) {
      setModalActiveDelivery(existing);
      setScheduleError(
        `Booking #${scheduleModalBooking.id} already has an active delivery (Delivery #${existing.id}, status: ${existing.status}) assigned to driver ${existing.driverName || 'Depot Driver'}. Please proceed to Delivery Schedule.`
      );
      return;
    }

    try {
      setIsSubmitting(true);
      setScheduleError(null);
      await DistributorApiService.scheduleDelivery({
        bookingId: Number(scheduleModalBooking.id),
        driverId: Number(selectedDriverId),
        vehicleId: Number(selectedVehicleId),
      });
      setScheduleModalBooking(null);
      setModalActiveDelivery(null);
      onOrderScheduled();
      onRefresh();
    } catch (err: any) {
      const msg = err.message || 'Failed to schedule delivery dispatch';
      setScheduleError(msg);
      // If server reports an active delivery already in progress, immediately reconcile authoritative delivery
      if (
        msg.toLowerCase().includes('already has an active delivery') ||
        msg.toLowerCase().includes('active delivery in progress')
      ) {
        try {
          const dels = await DistributorApiService.getDeliveriesByBooking(Number(scheduleModalBooking.id));
          const active = dels.find((d) => isActiveDeliveryStatus(d.status));
          if (active) {
            setModalActiveDelivery(active);
            onRefresh();
          }
        } catch {
          // ignore
        }
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-amber-50 text-amber-600">
              <Clock size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-amber-600">
              Accepted Queue Management
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Orders & Delivery Queue</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Confirmed customer orders awaiting vehicle scheduling. Operating SLA target: 6–7 days.
          </p>
        </div>

        <button
          onClick={onRefresh}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Operational SLA Policy Card */}
      <div className="p-4 rounded-xl bg-amber-50/70 border border-amber-200/70 text-xs text-amber-900 flex items-start gap-3">
        <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold">Standard 6–7 Day LPG Fulfillment Window</span>
          <p className="text-amber-800 text-[11px] mt-0.5">
            Indian LPG distribution standards permit customer orders to remain in depot queue for up to 6–7 operational days depending on delivery route clustering and physical stock availability.
          </p>
        </div>
      </div>

      {/* Queue Filter Tabs & Search */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
        {/* Sub-tabs */}
        <div className="flex items-center p-1 bg-slate-100/80 rounded-xl border border-slate-200/80">
          <button
            type="button"
            onClick={() => setQueueSubTab('needs-scheduling')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              queueSubTab === 'needs-scheduling'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span>Needs Scheduling</span>
            <span
              className={`px-1.5 py-0.2 rounded-full text-[10px] font-black ${
                queueSubTab === 'needs-scheduling'
                  ? 'bg-amber-100 text-amber-800'
                  : 'bg-slate-200/70 text-slate-600'
              }`}
            >
              {needsSchedulingOrders.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setQueueSubTab('scheduled-in-progress')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              queueSubTab === 'scheduled-in-progress'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span>Scheduled & In Progress</span>
            <span
              className={`px-1.5 py-0.2 rounded-full text-[10px] font-black ${
                queueSubTab === 'scheduled-in-progress'
                  ? 'bg-blue-100 text-blue-800'
                  : 'bg-slate-200/70 text-slate-600'
              }`}
            >
              {scheduledOrders.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setQueueSubTab('all')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              queueSubTab === 'all'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span>All Confirmed</span>
            <span
              className={`px-1.5 py-0.2 rounded-full text-[10px] font-black ${
                queueSubTab === 'all'
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-200/70 text-slate-600'
              }`}
            >
              {queuedOrders.length}
            </span>
          </button>
        </div>

        {/* Search */}
        <div className="relative max-w-sm w-full">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search queued orders by ID, name, driver..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-white border border-slate-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500"
          />
        </div>
      </div>

      {/* Queued Orders List */}
      {isLoading && queuedOrders.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <RefreshCw className="w-6 h-6 text-amber-600 animate-spin mx-auto mb-2" />
          <p className="text-xs text-slate-500 font-semibold">Loading queued orders from backend...</p>
        </div>
      ) : filtered.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((b) => {
            const age = getQueueAge(b.bookingDate);
            const activeDelivery = activeDeliveryByBookingId.get(b.id);
            const isAssigned = activeDelivery?.status === 'ASSIGNED';
            const isOutForDelivery =
              activeDelivery?.status === 'OUT_FOR_DELIVERY' ||
              activeDelivery?.status === 'PICKED_UP';

            return (
              <div
                key={b.id}
                className={`bg-white rounded-2xl p-5 border shadow-xs transition-all flex flex-col justify-between ${
                  activeDelivery
                    ? isOutForDelivery
                      ? 'border-rose-300 ring-1 ring-rose-200/60'
                      : 'border-blue-200 hover:border-blue-300'
                    : age.isOverdue
                    ? 'border-rose-300 ring-1 ring-rose-300/50'
                    : age.isWarning
                    ? 'border-amber-300'
                    : 'border-slate-200/80 hover:border-slate-300'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-mono text-xs font-black text-slate-800 bg-slate-100 px-2 py-0.5 rounded-md">
                      #{b.id}
                    </span>

                    {activeDelivery ? (
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1 ${
                          isOutForDelivery
                            ? 'bg-rose-100 text-rose-800 border border-rose-200'
                            : 'bg-blue-100 text-blue-800 border border-blue-200'
                        }`}
                      >
                        <Truck size={11} />
                        {isOutForDelivery ? 'OUT FOR DELIVERY' : 'SCHEDULED (ASSIGNED)'}
                      </span>
                    ) : (
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          age.isOverdue
                            ? 'bg-rose-100 text-rose-800'
                            : age.isWarning
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-emerald-50 text-emerald-700'
                        }`}
                      >
                        Queue Age: {age.days}d {age.hours}h
                      </span>
                    )}
                  </div>

                  <h3 className="font-bold text-sm text-slate-900 leading-tight mb-1">{b.customerName}</h3>
                  <div className="space-y-1.5 text-xs text-slate-600 my-3">
                    <div className="flex items-center gap-1.5">
                      <Flame size={13} className="text-rose-500 shrink-0" />
                      <span>
                        <strong>{b.quantity}x</strong> {b.cylinderCategory} ({b.capacityKg} kg)
                      </span>
                    </div>
                    <div className="flex items-start gap-1.5 text-[11px] text-slate-500">
                      <MapPin size={13} className="text-slate-400 shrink-0 mt-0.5" />
                      <span className="line-clamp-2">{b.deliveryAddress}</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-[10px] text-slate-400">
                      <Calendar size={12} className="text-slate-400 shrink-0" />
                      <span>Booked on {b.bookingDate}</span>
                    </div>
                  </div>

                  {/* Active Delivery Information Panel */}
                  {activeDelivery && (
                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1.5 text-xs my-3">
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-500 font-medium">Delivery:</span>
                        <strong className="text-slate-900 font-mono">#{activeDelivery.id}</strong>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-500 font-medium">Driver:</span>
                        <strong className="text-slate-800">{activeDelivery.driverName || 'Depot Driver'}</strong>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-500 font-medium">Vehicle:</span>
                        <strong className="text-slate-800 font-mono">
                          {activeDelivery.vehicleRegistration || `Vehicle #${activeDelivery.vehicleId || '—'}`}
                        </strong>
                      </div>
                    </div>
                  )}
                </div>

                <div className="pt-3 border-t border-slate-100 mt-2">
                  {activeDelivery ? (
                    isAssigned ? (
                      <button
                        type="button"
                        onClick={() => onNavigateTab?.('delivery-schedule')}
                        className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs transition-colors cursor-pointer"
                      >
                        <CalendarDays size={14} />
                        <span>View in Delivery Schedule</span>
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => onNavigateTab?.('live-deliveries')}
                        className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-xs transition-colors cursor-pointer"
                      >
                        <Navigation size={14} />
                        <span>Track Live Dispatch</span>
                      </button>
                    )
                  ) : (
                    <button
                      type="button"
                      onClick={() => {
                        setScheduleModalBooking(b);
                        setScheduleError(null);
                      }}
                      className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold shadow-xs transition-colors cursor-pointer"
                    >
                      <Truck size={14} />
                      <span>Schedule Delivery</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <Clock className="w-10 h-10 text-slate-300 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-800">
            {queueSubTab === 'needs-scheduling'
              ? 'All Confirmed Bookings Have Been Scheduled'
              : queueSubTab === 'scheduled-in-progress'
              ? 'No Scheduled Dispatches Currently Active'
              : 'Queue is Clear'}
          </h3>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            {queueSubTab === 'needs-scheduling'
              ? 'There are no pending unassigned bookings awaiting driver scheduling. Switch to "Scheduled & In Progress" to view active assignments.'
              : 'No orders match this queue category.'}
          </p>
        </div>
      )}

      {/* Schedule Delivery Modal */}
      {scheduleModalBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center gap-2 text-amber-600">
              <Truck size={20} />
              <h2 className="text-base font-bold text-slate-900">Schedule Delivery Dispatch</h2>
            </div>

            <p className="text-xs text-slate-600">
              Assign a licensed delivery driver and registered vehicle to dispatch booking #{scheduleModalBooking.id} for{' '}
              <strong>{scheduleModalBooking.customerName}</strong>.
            </p>

            {/* Informative banner if booking already has an active delivery */}
            {modalActiveDelivery && (
              <div className="p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-xs text-blue-900 space-y-2">
                <div className="flex items-start gap-2">
                  <Info size={16} className="text-blue-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Active Delivery Already Assigned</span>
                    <p className="text-[11px] text-blue-800 mt-0.5">
                      Booking #{scheduleModalBooking.id} already has Delivery #{modalActiveDelivery.id} (Status:{' '}
                      <strong>{modalActiveDelivery.status}</strong>) assigned to driver{' '}
                      <strong>{modalActiveDelivery.driverName || 'Depot Driver'}</strong> with vehicle{' '}
                      <strong>{modalActiveDelivery.vehicleRegistration || 'Registered Vehicle'}</strong>.
                    </p>
                  </div>
                </div>

                <div className="pt-2 border-t border-blue-200/60 flex justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      const targetTab = modalActiveDelivery.status === 'ASSIGNED' ? 'delivery-schedule' : 'live-deliveries';
                      setScheduleModalBooking(null);
                      onNavigateTab?.(targetTab);
                    }}
                    className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-[11px] font-bold transition-colors cursor-pointer"
                  >
                    {modalActiveDelivery.status === 'ASSIGNED'
                      ? 'Go to Delivery Schedule'
                      : 'Go to Live Deliveries'}
                  </button>
                </div>
              </div>
            )}

            {scheduleError && (
              <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-xs border border-rose-200 space-y-2">
                <p>{scheduleError}</p>
                {scheduleError.toLowerCase().includes('already has an active delivery') && (
                  <button
                    type="button"
                    onClick={() => {
                      setScheduleModalBooking(null);
                      onNavigateTab?.('delivery-schedule');
                    }}
                    className="px-2.5 py-1 rounded-lg bg-rose-600 hover:bg-rose-700 text-white text-[11px] font-bold transition-colors cursor-pointer"
                  >
                    View in Delivery Schedule
                  </button>
                )}
              </div>
            )}

            {!modalActiveDelivery && (
              <div className="space-y-3">
                {drivers.length > 0 && !drivers.some((d) => (d.status || '').toUpperCase() === 'AVAILABLE') && (
                  <div className="p-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-[11px]">
                    <strong>Depot Driver Notice:</strong> All registered drivers are currently ASSIGNED or EN ROUTE. Once dispatches are verified via OTP handover in Live Deliveries, drivers will return to AVAILABLE.
                  </div>
                )}

                <div>
                  <label className="text-[11px] font-bold text-slate-600 block mb-1">Assigned Driver</label>
                  <select
                    value={selectedDriverId}
                    onChange={(e) => setSelectedDriverId(Number(e.target.value))}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-amber-500 font-medium"
                  >
                    {drivers.length > 0 ? (
                      drivers.map((d) => {
                        const isAvail = (d.status || '').toUpperCase() === 'AVAILABLE';
                        return (
                          <option key={d.id} value={d.id} disabled={!isAvail}>
                            {d.name || d.driverName} ({d.status || 'AVAILABLE'}) {!isAvail ? '— Active on delivery' : '— Ready'}
                          </option>
                        );
                      })
                    ) : (
                      <option value="">No authorized drivers found for depot</option>
                    )}
                  </select>
                </div>

                <div>
                  <label className="text-[11px] font-bold text-slate-600 block mb-1">Delivery Vehicle</label>
                  <select
                    value={selectedVehicleId}
                    onChange={(e) => setSelectedVehicleId(Number(e.target.value))}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-amber-500 font-medium"
                  >
                    {vehicles.length > 0 ? (
                      vehicles.map((v) => {
                        const isAvail = (v.status || '').toUpperCase() === 'AVAILABLE';
                        return (
                          <option key={v.id} value={v.id} disabled={!isAvail}>
                            {v.vehicleNumber || v.registrationNumber || `Vehicle #${v.id}`} ({v.status || 'AVAILABLE'}) {!isAvail ? '— In Transit' : '— Ready'}
                          </option>
                        );
                      })
                    ) : (
                      <option value="">No registered vehicles found for depot</option>
                    )}
                  </select>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setScheduleModalBooking(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
              >
                Close
              </button>
              {!modalActiveDelivery && (
                <button
                  type="button"
                  disabled={isSubmitting || !selectedDriverId || !selectedVehicleId}
                  onClick={handleConfirmSchedule}
                  className="px-4 py-2 rounded-xl text-xs font-bold bg-amber-600 hover:bg-amber-700 text-white shadow-xs disabled:opacity-50 cursor-pointer"
                >
                  {isSubmitting ? 'Scheduling...' : 'Confirm Schedule'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

