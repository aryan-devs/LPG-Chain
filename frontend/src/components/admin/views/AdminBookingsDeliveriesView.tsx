import React, { useState, useEffect, useMemo } from 'react';
import {
  CalendarDays,
  Eye,
  CheckCircle2,
  AlertTriangle,
  MapPin,
  Flame,
  RefreshCw,
} from 'lucide-react';
import {
  AdminBookingItem,
  DeliveryTimelineStep,
} from '../../../types/adminPortalTypes';
import { AdminTable, ColumnDef } from '../components/AdminTable';
import { AdminStatusBadge } from '../components/AdminStatusBadge';
import { AdminFilterBar, FilterDropdown } from '../components/AdminFilterBar';
import { AdminDetailModal } from '../components/AdminDetailModal';
import { AdminApiService } from '../../../services/adminApi';

const PREVIEW_BOOKINGS: AdminBookingItem[] = [
  {
    id: 10042,
    bookingNumber: 'BK-10042',
    customerId: 1,
    customerName: 'Anand Verma',
    customerPhone: '+91 98201 54321',
    customerEmail: 'anand.verma@example.com',
    deliveryAddress: 'B-402 Shanti Vihar, Andheri West, Mumbai - 400053',
    distributorId: 1,
    distributorName: 'Apex Gas Distribution Agency',
    cylinderCategory: 'DOMESTIC_14_2',
    cylinderDisplayName: 'Domestic 14.2 kg Refill',
    capacityKg: 14.2,
    quantity: 1,
    bookingDate: '2026-10-14 09:15',
    bookingStatus: 'OUT_FOR_DELIVERY',
    deliveryStatus: 'OUT_FOR_DELIVERY',
    assignedDriverId: 10,
    assignedDriverName: 'Suresh Patil',
    assignedDriverPhone: '+91 97654 32109',
    assignedVehicleId: 1,
    assignedVehicleReg: 'MH-04-AB-1234',
    estimatedDeliveryDate: '2026-10-14 (Today)',
    isOverdue: false,
  },
  {
    id: 10043,
    bookingNumber: 'BK-10043',
    customerId: 4,
    customerName: 'Pooja Kulkarni',
    customerPhone: '+91 98111 22334',
    customerEmail: 'pooja.k@gmail.com',
    deliveryAddress: 'Flat 12, Swastik Park, Hadapsar, Pune - 411028',
    distributorId: 2,
    distributorName: 'Pune Green Gas Depot',
    cylinderCategory: 'DOMESTIC_14_2',
    cylinderDisplayName: 'Domestic 14.2 kg Refill',
    capacityKg: 14.2,
    quantity: 1,
    bookingDate: '2026-10-14 11:30',
    bookingStatus: 'CONFIRMED',
    deliveryStatus: 'ASSIGNED',
    assignedDriverId: 12,
    assignedDriverName: 'Ganesh More',
    assignedDriverPhone: '+91 98220 55667',
    assignedVehicleId: 3,
    assignedVehicleReg: 'MH-12-PQ-9081',
    estimatedDeliveryDate: '2026-10-15',
    isOverdue: false,
  },
  {
    id: 10044,
    bookingNumber: 'BK-10044',
    customerId: 6,
    customerName: 'Vikram Joshi',
    customerPhone: '+91 98999 44556',
    customerEmail: 'v.joshi@outlook.com',
    deliveryAddress: 'Shaniwar Peth, Pune - 411030',
    distributorId: 2,
    distributorName: 'Pune Green Gas Depot',
    cylinderCategory: 'COMMERCIAL_19',
    cylinderDisplayName: 'Commercial 19 kg Cylinder',
    capacityKg: 19.0,
    quantity: 2,
    bookingDate: '2026-10-06 14:00',
    bookingStatus: 'PENDING',
    deliveryStatus: 'UNASSIGNED',
    estimatedDeliveryDate: '2026-10-08',
    isOverdue: true,
    exceptionNote: 'Exceeded 7-day depot SLA delivery window. Agency alert dispatched.',
  },
  {
    id: 10045,
    bookingNumber: 'BK-10045',
    customerId: 10,
    customerName: 'Grand Hotel Kitchen',
    customerPhone: '+91 98222 99887',
    customerEmail: 'kitchen@grandhotel.example',
    deliveryAddress: 'Wardha Road, Nagpur - 440015',
    distributorId: 3,
    distributorName: 'City Gas Services Ltd',
    cylinderCategory: 'INDUSTRIAL_47_5',
    cylinderDisplayName: 'Industrial 47.5 kg High Pressure',
    capacityKg: 47.5,
    quantity: 4,
    bookingDate: '2026-10-12 16:20',
    bookingStatus: 'DELIVERED',
    deliveryStatus: 'DELIVERED',
    assignedDriverId: 15,
    assignedDriverName: 'Amit Deshmukh',
    assignedVehicleReg: 'MH-31-TR-4455',
    deliveredAt: '2026-10-13 11:45',
    isOverdue: false,
  },
  {
    id: 10046,
    bookingNumber: 'BK-10046',
    customerId: 12,
    customerName: 'Meera Nair',
    customerPhone: '+91 98333 11223',
    deliveryAddress: 'Ghodbunder Road, Thane - 400607',
    distributorId: 4,
    distributorName: 'Western Bharat Gas Agency',
    cylinderCategory: 'MINI_5',
    cylinderDisplayName: 'Mini 5 kg Chhotu Cylinder',
    capacityKg: 5.0,
    quantity: 1,
    bookingDate: '2026-10-13 18:00',
    bookingStatus: 'CANCELLED',
    deliveryStatus: 'FAILED',
    exceptionNote: 'Cancelled by customer before dispatch.',
    isOverdue: false,
  },
];

export const AdminBookingsDeliveriesView: React.FC = () => {
  const [bookings, setBookings] = useState<AdminBookingItem[]>(PREVIEW_BOOKINGS);
  const [isLoading, setIsLoading] = useState(false);
  const [isUsingLiveBackend, setIsUsingLiveBackend] = useState(false);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [distributorFilter, setDistributorFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [delayFilter, setDelayFilter] = useState('ALL');

  // Detail Modal
  const [selectedBooking, setSelectedBooking] = useState<AdminBookingItem | null>(null);

  const loadBookings = async () => {
    try {
      setIsLoading(true);
      const data = await AdminApiService.getAllBookings();
      if (data && data.length > 0) {
        setBookings(data);
        setIsUsingLiveBackend(true);
      } else {
        setBookings(PREVIEW_BOOKINGS);
        setIsUsingLiveBackend(false);
      }
    } catch {
      setBookings(PREVIEW_BOOKINGS);
      setIsUsingLiveBackend(false);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const distributors = useMemo(() => {
    const list = Array.from(new Set(bookings.map((b) => b.distributorName)));
    return [{ label: 'All Distributors', value: 'ALL' }, ...list.map((d) => ({ label: d, value: d }))];
  }, [bookings]);

  const filteredBookings = useMemo(() => {
    return bookings.filter((b) => {
      const matchSearch =
        b.bookingNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        b.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        b.customerPhone.includes(searchQuery) ||
        b.deliveryAddress.toLowerCase().includes(searchQuery.toLowerCase());

      const matchDist = distributorFilter === 'ALL' || b.distributorName === distributorFilter;
      const matchCat = categoryFilter === 'ALL' || b.cylinderCategory === categoryFilter;
      const matchStatus = statusFilter === 'ALL' || b.bookingStatus === statusFilter;
      const matchDelay =
        delayFilter === 'ALL'
          ? true
          : delayFilter === 'OVERDUE'
          ? b.isOverdue
          : !b.isOverdue;

      return matchSearch && matchDist && matchCat && matchStatus && matchDelay;
    });
  }, [bookings, searchQuery, distributorFilter, categoryFilter, statusFilter, delayFilter]);

  const filterDropdowns: FilterDropdown[] = [
    {
      id: 'distributor',
      label: 'Distributor',
      value: distributorFilter,
      onChange: setDistributorFilter,
      options: distributors,
    },
    {
      id: 'status',
      label: 'Status',
      value: statusFilter,
      onChange: setStatusFilter,
      options: [
        { label: 'All Statuses', value: 'ALL' },
        { label: 'Pending', value: 'PENDING' },
        { label: 'Confirmed', value: 'CONFIRMED' },
        { label: 'Out For Delivery', value: 'OUT_FOR_DELIVERY' },
        { label: 'Delivered', value: 'DELIVERED' },
        { label: 'Cancelled', value: 'CANCELLED' },
      ],
    },
    {
      id: 'category',
      label: 'Category',
      value: categoryFilter,
      onChange: setCategoryFilter,
      options: [
        { label: 'All Categories', value: 'ALL' },
        { label: 'Domestic 14.2 kg', value: 'DOMESTIC_14_2' },
        { label: 'Commercial 19 kg', value: 'COMMERCIAL_19' },
        { label: 'Industrial 47.5 kg', value: 'INDUSTRIAL_47_5' },
        { label: 'Mini 5 kg', value: 'MINI_5' },
      ],
    },
    {
      id: 'delay',
      label: 'SLA Status',
      value: delayFilter,
      onChange: setDelayFilter,
      options: [
        { label: 'All SLAs', value: 'ALL' },
        { label: 'Overdue Window', value: 'OVERDUE' },
        { label: 'Normal Window', value: 'NORMAL' },
      ],
    },
  ];

  const columns: ColumnDef<AdminBookingItem>[] = [
    {
      key: 'bookingNumber',
      header: 'Booking Ref',
      render: (b) => (
        <div className="flex flex-col">
          <div className="flex items-center gap-1.5">
            <span className="font-bold text-slate-900 font-mono text-xs">{b.bookingNumber}</span>
            {b.isOverdue && (
              <span className="px-1.5 py-0.2 bg-rose-100 text-rose-700 text-[9px] font-bold rounded">
                OVERDUE
              </span>
            )}
          </div>
          <span className="text-[10px] text-slate-400 font-mono">{b.bookingDate}</span>
        </div>
      ),
    },
    {
      key: 'customerName',
      header: 'Customer Details',
      render: (b) => (
        <div className="flex flex-col">
          <span className="font-bold text-slate-900">{b.customerName}</span>
          <span className="text-[11px] text-slate-400">{b.customerPhone}</span>
        </div>
      ),
    },
    {
      key: 'distributorName',
      header: 'Assigned Agency',
      render: (b) => (
        <div className="flex flex-col">
          <span className="font-medium text-slate-900 text-xs">{b.distributorName}</span>
          <span className="text-[10px] text-slate-400">ID #{b.distributorId}</span>
        </div>
      ),
    },
    {
      key: 'cylinderDisplayName',
      header: 'Cylinder Spec',
      render: (b) => (
        <div className="flex items-center gap-1.5">
          <Flame size={13} className="text-rose-600 shrink-0" />
          <span className="font-semibold text-slate-800 text-xs">
            {b.quantity}x {b.cylinderDisplayName}
          </span>
        </div>
      ),
    },
    {
      key: 'bookingStatus',
      header: 'Booking Status',
      render: (b) => <AdminStatusBadge status={b.bookingStatus} size="sm" />,
    },
    {
      key: 'deliveryFleet',
      header: 'Delivery Fleet',
      render: (b) => (
        <div className="flex flex-col text-[11px]">
          {b.assignedDriverName ? (
            <>
              <span className="font-semibold text-slate-800">{b.assignedDriverName}</span>
              <span className="text-slate-400 font-mono">{b.assignedVehicleReg || 'No Vehicle'}</span>
            </>
          ) : (
            <span className="text-slate-400 italic">Unassigned</span>
          )}
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (b) => (
        <div className="flex items-center justify-end" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => setSelectedBooking(b)}
            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
            title="Inspect Booking & Timeline"
          >
            <Eye size={15} />
          </button>
        </div>
      ),
    },
  ];

  // Helper to generate visual delivery timeline steps
  const getTimelineSteps = (b: AdminBookingItem): DeliveryTimelineStep[] => {
    const isCancelled = b.bookingStatus === 'CANCELLED';
    const isDelivered = b.bookingStatus === 'DELIVERED';
    const isOut = b.bookingStatus === 'OUT_FOR_DELIVERY';
    const isConfirmed = b.bookingStatus === 'CONFIRMED' || isOut || isDelivered;

    return [
      {
        title: 'Booking Placed',
        description: `Customer submitted refill request for ${b.quantity} cylinder(s)`,
        timestamp: b.bookingDate,
        status: 'COMPLETED',
      },
      {
        title: 'Depot Confirmation',
        description: isConfirmed
          ? `Agency confirmed booking and reserved inventory`
          : isCancelled
          ? 'Booking was cancelled'
          : 'Pending depot agent confirmation',
        status: isCancelled ? 'FAILED' : isConfirmed ? 'COMPLETED' : 'PENDING',
      },
      {
        title: 'Driver & Vehicle Assignment',
        description: b.assignedDriverName
          ? `Assigned to ${b.assignedDriverName} (${b.assignedVehicleReg || 'Vehicle'})`
          : 'Awaiting available driver assignment',
        status: b.assignedDriverName ? 'COMPLETED' : 'PENDING',
      },
      {
        title: 'Out For Delivery',
        description: isOut || isDelivered
          ? 'Dispatched from depot with live GPS tracking'
          : 'Waiting for depot gate dispatch',
        status: isOut || isDelivered ? 'COMPLETED' : 'PENDING',
      },
      {
        title: 'Verified OTP Delivery',
        description: isDelivered
          ? `Completed with customer OTP verification at ${b.deliveredAt || 'Depot records'}`
          : 'Handoff pending OTP exchange at doorstep',
        status: isDelivered ? 'COMPLETED' : isCancelled ? 'FAILED' : 'PENDING',
      },
    ];
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <CalendarDays size={20} className="text-rose-600" />
            <span>Platform Bookings & Delivery Oversight</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Real-time platform-wide monitor for bookings, fulfillment dispatch timelines, and delivery exceptions.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span
            className={`px-2.5 py-1 rounded-full text-[11px] font-semibold border ${
              isUsingLiveBackend
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-slate-100 text-slate-600 border-slate-200'
            }`}
          >
            {isUsingLiveBackend ? 'Live Backend Bookings' : 'Preview Bookings'}
          </span>
          <button
            onClick={loadBookings}
            disabled={isLoading}
            className="p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-white border border-slate-200 transition-colors"
            title="Refresh Bookings"
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Admin Monitoring Principle notice */}
      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start gap-2.5">
        <CheckCircle2 size={16} className="text-rose-600 mt-0.5 shrink-0" />
        <div>
          <strong className="text-slate-900 font-bold">Operational Scope:</strong> Platform administrators monitor order volumes, service-level compliance, and exceptions. Routine order confirmation and driver assignments remain delegated to local distributor depot dispatchers.
        </div>
      </div>

      {/* Filter and Search Bar */}
      <AdminFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        searchPlaceholder="Search booking ref, customer name, phone, address..."
        dropdowns={filterDropdowns}
        onReset={() => {
          setSearchQuery('');
          setDistributorFilter('ALL');
          setCategoryFilter('ALL');
          setStatusFilter('ALL');
          setDelayFilter('ALL');
        }}
        totalResults={filteredBookings.length}
      />

      {/* Bookings Table */}
      <AdminTable
        columns={columns}
        data={filteredBookings}
        keyExtractor={(b) => b.id}
        isLoading={isLoading}
        emptyTitle="No bookings found"
        emptyDescription="No booking records match your filter criteria."
        pageSize={8}
        onRowClick={(b) => setSelectedBooking(b)}
      />

      {/* ── Booking Detail & Timeline Modal ── */}
      <AdminDetailModal
        isOpen={!!selectedBooking}
        title={`Booking #${selectedBooking?.bookingNumber}`}
        subtitle={`Placed on ${selectedBooking?.bookingDate} • Handled by ${selectedBooking?.distributorName}`}
        badge={
          selectedBooking ? (
            <AdminStatusBadge status={selectedBooking.bookingStatus} size="sm" />
          ) : undefined
        }
        icon={<CalendarDays size={18} className="text-rose-600" />}
        maxWidth="xl"
        onClose={() => setSelectedBooking(null)}
        footerActions={
          <button
            type="button"
            onClick={() => setSelectedBooking(null)}
            className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          >
            Close Overview
          </button>
        }
      >
        {selectedBooking && (
          <div className="space-y-5 text-xs text-slate-700">
            {/* Overdue Alert banner if any */}
            {selectedBooking.isOverdue && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-start gap-2.5">
                <AlertTriangle size={16} className="text-rose-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">Operational Delay Detected:</span>
                  <p className="mt-0.5 text-[11px] text-rose-700">
                    {selectedBooking.exceptionNote || 'Booking has been queued beyond the 7-day standard SLA threshold.'}
                  </p>
                </div>
              </div>
            )}

            {/* Customer & Address Card */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase">Customer</span>
                <div className="font-bold text-slate-900 mt-0.5">{selectedBooking.customerName}</div>
                <div className="text-slate-500">{selectedBooking.customerPhone}</div>
                {selectedBooking.customerEmail && (
                  <div className="text-[11px] text-slate-400">{selectedBooking.customerEmail}</div>
                )}
              </div>

              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase">Assigned Depot</span>
                <div className="font-bold text-slate-900 mt-0.5">
                  {selectedBooking.distributorName}
                </div>
                <div className="text-slate-500">ID #{selectedBooking.distributorId}</div>
              </div>

              <div className="sm:col-span-2 pt-2 border-t border-slate-200/60">
                <span className="text-[10px] text-slate-400 font-bold uppercase flex items-center gap-1">
                  <MapPin size={11} /> Delivery Address
                </span>
                <div className="font-medium text-slate-800 mt-0.5">
                  {selectedBooking.deliveryAddress}
                </div>
              </div>
            </div>

            {/* Assigned Fleet Card */}
            <div className="p-4 rounded-xl bg-white border border-slate-200 grid grid-cols-2 gap-3">
              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase block">
                  Assigned Driver
                </span>
                <span className="font-bold text-slate-900 text-sm">
                  {selectedBooking.assignedDriverName || 'Not yet dispatched'}
                </span>
                {selectedBooking.assignedDriverPhone && (
                  <span className="block text-[11px] text-slate-500">
                    {selectedBooking.assignedDriverPhone}
                  </span>
                )}
              </div>

              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase block">
                  Delivery Vehicle
                </span>
                <span className="font-bold text-slate-900 text-sm font-mono">
                  {selectedBooking.assignedVehicleReg || 'N/A'}
                </span>
                <span className="block text-[11px] text-slate-500">
                  Status: {selectedBooking.deliveryStatus}
                </span>
              </div>
            </div>

            {/* Visual Delivery Step Timeline */}
            <div className="space-y-3 pt-2">
              <span className="text-xs font-bold text-slate-900 uppercase tracking-wider block">
                Delivery Lifecycle Timeline
              </span>
              <div className="space-y-3 pl-2 border-l-2 border-slate-200 ml-2">
                {getTimelineSteps(selectedBooking).map((step, idx) => (
                  <div key={idx} className="relative pl-5">
                    <div
                      className={`absolute -left-[15px] top-0.5 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold ${
                        step.status === 'COMPLETED'
                          ? 'bg-emerald-500 text-white shadow-xs'
                          : step.status === 'FAILED'
                          ? 'bg-rose-500 text-white'
                          : 'bg-slate-200 text-slate-600'
                      }`}
                    >
                      {step.status === 'COMPLETED' ? '✓' : idx + 1}
                    </div>
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-slate-900 text-xs">{step.title}</span>
                        {step.timestamp && (
                          <span className="text-[10px] text-slate-400 font-mono">
                            {step.timestamp}
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-500 mt-0.5">{step.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </AdminDetailModal>
    </div>
  );
};
