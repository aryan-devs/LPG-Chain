import React, { useState } from 'react';
import {
  Package,
  Search,
  CheckCircle2,
  Clock,
  Truck,
  RotateCcw,
  ArrowRight,
} from 'lucide-react';
import { CustomerOrder, PortalTab } from '../portalTypes';

interface PortalBookingHistoryViewProps {
  orders: CustomerOrder[];
  onNavigateTab: (tab: PortalTab) => void;
  onReorder: (capacityKg: number) => void;
}

export const PortalBookingHistoryView: React.FC<PortalBookingHistoryViewProps> = ({
  orders,
  onNavigateTab,
  onReorder,
}) => {
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const filteredOrders = orders.filter((order) => {
    const matchesFilter =
      filterStatus === 'ALL' ||
      (filterStatus === 'ACTIVE' &&
        (order.status === 'ASSIGNED' ||
          order.status === 'CONFIRMED' ||
          order.status === 'OUT_FOR_DELIVERY')) ||
      (filterStatus === 'DELIVERED' && order.status === 'DELIVERED') ||
      (filterStatus === 'CANCELLED' && order.status === 'CANCELLED');

    const matchesSearch =
      order.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      order.cylinderType.toLowerCase().includes(searchQuery.toLowerCase()) ||
      order.bookingDate.toLowerCase().includes(searchQuery.toLowerCase());

    return matchesFilter && matchesSearch;
  });

  return (
    <div className="max-w-5xl mx-auto flex flex-col gap-6 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Booking &amp; Refill History
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Complete record of your household LPG cylinder refill orders and deliveries.
          </p>
        </div>
        <button
          type="button"
          onClick={() => onNavigateTab('book')}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-md shadow-rose-600/25 transition-all self-start sm:self-auto"
        >
          <span>Order Refill Now</span>
          <ArrowRight size={14} />
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200/90 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Filter Pills */}
        <div className="flex items-center gap-2 overflow-x-auto w-full sm:w-auto">
          {[
            { id: 'ALL', label: 'All Orders' },
            { id: 'ACTIVE', label: 'In Progress' },
            { id: 'DELIVERED', label: 'Delivered' },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setFilterStatus(tab.id)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
                filterStatus === tab.id
                  ? 'bg-rose-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <Search size={15} className="absolute left-3 top-2.5 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by ID or date..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
          />
        </div>
      </div>

      {/* Orders List */}
      <div className="flex flex-col gap-4">
        {filteredOrders.length > 0 ? (
          filteredOrders.map((order) => {
            const isActive =
              order.status === 'ASSIGNED' ||
              order.status === 'CONFIRMED' ||
              order.status === 'OUT_FOR_DELIVERY';

            return (
              <div
                key={order.id}
                className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 sm:p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 hover:shadow-md transition-shadow"
              >
                <div className="flex items-start sm:items-center gap-4">
                  <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center flex-shrink-0">
                    <Package size={22} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-slate-900 text-sm">
                        #{order.id}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1 ${
                          order.status === 'DELIVERED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {order.status === 'DELIVERED' ? (
                          <CheckCircle2 size={11} />
                        ) : (
                          <Clock size={11} />
                        )}
                        {order.status}
                      </span>
                    </div>
                    <p className="text-xs font-bold text-slate-800 mt-1">
                      {order.cylinderType} &bull; {order.quantity} Unit
                    </p>
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      Booked on: {order.bookingDate} &bull; {order.deliveryAddress}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-4 self-end md:self-auto">
                  <div className="text-right">
                    <span className="text-sm font-black text-slate-900 block">
                      ₹{order.amount}.00
                    </span>
                    <span className="text-[10px] text-slate-400 font-medium">
                      {order.paymentMethod} &bull; {order.paymentStatus}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {isActive ? (
                      <button
                        type="button"
                        onClick={() => onNavigateTab('track')}
                        className="px-3.5 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs shadow-sm flex items-center gap-1.5 transition-colors"
                      >
                        <Truck size={13} />
                        <span>Track Live</span>
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => onReorder(order.capacityKg)}
                        className="px-3.5 py-1.5 rounded-xl border border-slate-200 hover:border-rose-400 hover:bg-rose-50 text-slate-700 hover:text-rose-600 font-bold text-xs flex items-center gap-1.5 transition-colors"
                      >
                        <RotateCcw size={13} />
                        <span>Re-order</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center flex flex-col items-center gap-3">
            <Package size={32} className="text-slate-300" />
            <h3 className="text-base font-bold text-slate-800">No Orders Found</h3>
            <p className="text-xs text-slate-500">
              No bookings matched your filter criteria.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
