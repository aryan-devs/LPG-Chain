import React from 'react';
import {
  Inbox,
  Clock,
  Truck,
  CheckCircle2,
  Package,
  TrendingUp,
  Sparkles,
  ArrowRight,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import {
  DistributorOverviewData,
  DistributorPortalTab,
  ForecastResponse,
  isBookingPending,
  isBookingConfirmed,
  isBookingOutForDelivery,
} from '../../../types/distributorPortalTypes';

interface DistributorOverviewViewProps {
  data: DistributorOverviewData | null;
  forecast: ForecastResponse | null;
  isLoading: boolean;
  onRefresh: () => void;
  onNavigateTab: (tab: DistributorPortalTab) => void;
  aiOnline: boolean;
}

export const DistributorOverviewView: React.FC<DistributorOverviewViewProps> = ({
  data,
  forecast,
  isLoading,
  onRefresh,
  onNavigateTab,
  aiOnline,
}) => {
  if (isLoading && !data) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-3">
        <RefreshCw className="w-7 h-7 text-rose-600 animate-spin" />
        <span className="text-sm font-semibold text-slate-600">Loading operational overview...</span>
      </div>
    );
  }

  if (!isLoading && !data) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[360px] p-8 bg-white rounded-2xl border border-slate-200/80 text-center gap-4">
        <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center">
          <AlertTriangle size={24} />
        </div>
        <div className="max-w-md">
          <h2 className="text-base font-bold text-slate-900">Distributor Operational Data Unavailable</h2>
          <p className="text-xs text-slate-500 mt-1">
            Unable to load depot metrics from the backend. Please verify your distributor login credentials and refresh.
          </p>
        </div>
        <button
          onClick={onRefresh}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold bg-slate-900 hover:bg-slate-800 text-white transition-colors"
        >
          <RefreshCw size={13} />
          <span>Retry Loading</span>
        </button>
      </div>
    );
  }

  const overview = data;

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome Ribbon */}
      <div className="rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 text-white p-6 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30">
              DISTRIBUTOR OPERATIONS DESK
            </span>
            <span className="text-xs text-slate-400">Live Backend Records</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-black tracking-tight text-white">
            {overview?.distributorName || 'Apex Gas Agency'}
          </h1>
          <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-xl">
            Real-time depot operations, booking queues, physical inventory audits, and demand forecasts.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start md:self-auto">
          <button
            onClick={onRefresh}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-white/10 hover:bg-white/20 text-white border border-white/10 transition-colors"
          >
            <RefreshCw size={13} className={isLoading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => onNavigateTab('booking-requests')}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-500 text-white shadow-md shadow-rose-900/30 transition-colors"
          >
            <span>Review Requests</span>
            <ArrowRight size={13} />
          </button>
        </div>
      </div>

      {/* Overdue Queue Warning Notice if any */}
      {overview && overview.overdueOrdersCount > 0 && (
        <div className="rounded-xl bg-amber-50 border border-amber-200/80 p-4 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="flex-1 text-xs">
            <span className="font-bold text-amber-900">
              {overview.overdueOrdersCount} booking(s) exceed the 7-day operation delivery window!
            </span>
            <p className="text-amber-700 mt-0.5">
              Average queue wait time is currently {overview.averageQueueAgeHours} hours. Please schedule available delivery dispatches immediately.
            </p>
          </div>
          <button
            onClick={() => onNavigateTab('orders-queue')}
            className="px-2.5 py-1 text-xs font-bold bg-amber-600 hover:bg-amber-700 text-white rounded-lg transition-colors"
          >
            Open Queue
          </button>
        </div>
      )}

      {/* 4 Primary Operational Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* New Booking Requests */}
        <div
          onClick={() => onNavigateTab('booking-requests')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-rose-300 hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">New Requests</span>
            <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center group-hover:scale-105 transition-transform">
              <Inbox size={18} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-black text-slate-900">
              {overview?.newBookingRequestsCount ?? 0}
            </span>
            <span className="text-xs font-semibold text-rose-600">Pending</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2 flex items-center justify-between">
            <span>Awaiting agency acceptance</span>
            <ArrowRight size={12} className="text-slate-300 group-hover:text-rose-600 group-hover:translate-x-0.5 transition-all" />
          </p>
        </div>

        {/* Accepted & Queued Orders */}
        <div
          onClick={() => onNavigateTab('orders-queue')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-amber-300 hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Accepted Queue</span>
            <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center group-hover:scale-105 transition-transform">
              <Clock size={18} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-black text-slate-900">
              {overview?.acceptedQueuedOrdersCount ?? 0}
            </span>
            <span className="text-xs font-semibold text-amber-600">
              ~{Math.round((overview?.averageQueueAgeHours || 0) / 24 * 10) / 10}d avg
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2 flex items-center justify-between">
            <span>Ready for delivery dispatch</span>
            <ArrowRight size={12} className="text-slate-300 group-hover:text-amber-600 group-hover:translate-x-0.5 transition-all" />
          </p>
        </div>

        {/* Scheduled / Dispatched Deliveries */}
        <div
          onClick={() => onNavigateTab('delivery-schedule')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-blue-300 hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Active Dispatches</span>
            <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:scale-105 transition-transform">
              <Truck size={18} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-black text-slate-900">
              {(overview?.scheduledDeliveriesCount ?? 0) + (overview?.dispatchedDeliveriesCount ?? 0)}
            </span>
            <span className="text-xs font-semibold text-blue-600">
              {overview?.dispatchedDeliveriesCount ?? 0} On Road
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2 flex items-center justify-between">
            <span>{overview?.scheduledDeliveriesCount ?? 0} scheduled</span>
            <ArrowRight size={12} className="text-slate-300 group-hover:text-blue-600 group-hover:translate-x-0.5 transition-all" />
          </p>
        </div>

        {/* Completed Deliveries */}
        <div
          onClick={() => onNavigateTab('transparency')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-emerald-300 hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Completed Sales</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-105 transition-transform">
              <CheckCircle2 size={18} />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-black text-slate-900">
              {overview?.completedDeliveriesCount ?? 0}
            </span>
            <span className="text-xs font-semibold text-emerald-600">Verified</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2 flex items-center justify-between">
            <span>Doorstep OTP verified</span>
            <ArrowRight size={12} className="text-slate-300 group-hover:text-emerald-600 group-hover:translate-x-0.5 transition-all" />
          </p>
        </div>
      </div>

      {/* 2-Column Section: Physical Inventory vs Allocation Transparency */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Physical Warehouse Inventory */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
                  <Package size={17} />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-slate-900">Physical Depot Inventory</h2>
                  <p className="text-[11px] text-slate-400">Auditable physical warehouse cylinders</p>
                </div>
              </div>
              <button
                onClick={() => onNavigateTab('inventory')}
                className="text-xs font-semibold text-rose-600 hover:text-rose-700 flex items-center gap-1"
              >
                <span>Manage Stock</span>
                <ArrowRight size={13} />
              </button>
            </div>

            <div className="grid grid-cols-3 gap-3 mb-4">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase">Filled</span>
                <p className="text-lg font-black text-slate-900 mt-0.5">{overview?.totalFilledStock ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase">Reserved</span>
                <p className="text-lg font-black text-amber-600 mt-0.5">{overview?.totalReservedStock ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase">Available</span>
                <p className="text-lg font-black text-emerald-600 mt-0.5">{overview?.totalAvailableStock ?? 0}</p>
              </div>
            </div>

            {/* Category breakdown table */}
            <div className="divide-y divide-slate-100 text-xs">
              {overview?.inventory && overview.inventory.length > 0 ? (
                overview.inventory.map((inv) => (
                  <div key={inv.cylinderCategory} className="py-2.5 flex items-center justify-between">
                    <div>
                      <span className="font-bold text-slate-800">{inv.categoryDisplayName}</span>
                      <span className="text-[10px] text-slate-400 ml-2">{inv.capacityKg} kg</span>
                    </div>
                    <div className="flex items-center gap-4 text-right">
                      <span className="text-slate-600">
                        Filled: <strong className="text-slate-900">{inv.filledStock}</strong>
                      </span>
                      <span className="text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded">
                        Avail: {inv.availableStock}
                      </span>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-400 py-4 text-center">No inventory records initialized yet.</p>
              )}
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
            <span>Empty cylinders collected: {overview?.totalEmptyStock ?? 0}</span>
            <span>Damaged/Quarantine: {overview?.totalDamagedStock ?? 0}</span>
          </div>
        </div>

        {/* OMC Quota Allocation & Completed Sales */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                  <TrendingUp size={17} />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-slate-900">OMC Quota Allocation & Sales</h2>
                  <p className="text-[11px] text-slate-400">Current calendar month transparency balance</p>
                </div>
              </div>
              <button
                onClick={() => onNavigateTab('transparency')}
                className="text-xs font-semibold text-emerald-600 hover:text-emerald-700 flex items-center gap-1"
              >
                <span>Full Transparency</span>
                <ArrowRight size={13} />
              </button>
            </div>

            <div className="grid grid-cols-3 gap-3 mb-4">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase">Allocated</span>
                <p className="text-lg font-black text-slate-900 mt-0.5">{overview?.allocatedMonthlyTotal ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase">Delivered</span>
                <p className="text-lg font-black text-blue-600 mt-0.5">{overview?.soldMonthlyTotal ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                <span className="text-[10px] font-semibold text-slate-400 uppercase">Remaining</span>
                <p className="text-lg font-black text-emerald-600 mt-0.5">{overview?.remainingAllocationTotal ?? 0}</p>
              </div>
            </div>

            {/* Quota Progress Bar */}
            <div className="space-y-2 mb-4">
              {(() => {
                const alloc = overview?.allocatedMonthlyTotal || 0;
                const sold = overview?.soldMonthlyTotal || 0;
                const pct = alloc > 0 ? Math.min(100, Math.round((sold / alloc) * 100)) : 0;
                return (
                  <div>
                    <div className="flex justify-between text-xs mb-1 font-semibold">
                      <span className="text-slate-600">Monthly Quota Utilization</span>
                      <span className="text-slate-900">{pct}%</span>
                    </div>
                    <div className="w-full h-2.5 rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-blue-500 to-emerald-500 rounded-full transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                );
              })()}
            </div>

            {/* Category breakdown */}
            <div className="space-y-2 text-xs">
              {overview?.allocations && overview.allocations.length > 0 ? (
                overview.allocations.map((cat) => (
                  <div key={cat.cylinderCategory} className="flex items-center justify-between py-1 border-b border-slate-50">
                    <span className="font-semibold text-slate-700">{cat.categoryDisplayName}</span>
                    <span className="text-slate-500 font-mono">
                      {cat.deliveredQuantity} / {cat.allocatedQuantity ?? 'Unpublished'} sold
                    </span>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-400 py-2 text-center">No allocation published for this month.</p>
              )}
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-[11px] text-slate-400">
            Official allocations published by Oil Marketing Company (OMC).
          </div>
        </div>
      </div>

      {/* AI Demand Forecast Quick Glance */}
      <div className="bg-gradient-to-br from-indigo-50/70 via-white to-indigo-50/30 rounded-2xl p-6 border border-indigo-100 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-sm">
              <Sparkles size={18} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-sm font-bold text-slate-900">AI Demand Forecast (Next 7 Days)</h2>
                <span className="text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200/80 px-2 py-0.5 rounded-full">
                  SYNTHETIC BASELINE
                </span>
              </div>
              <p className="text-xs text-slate-500">
                RandomForestRegressor forecasting expected refilling demand
              </p>
            </div>
          </div>

          <button
            onClick={() => onNavigateTab('ai-forecast')}
            className="self-start sm:self-auto px-3.5 py-1.5 rounded-xl text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs transition-colors flex items-center gap-1.5"
          >
            <span>Explore 7 / 14 / 30 Days</span>
            <ArrowRight size={13} />
          </button>
        </div>

        {forecast ? (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="p-4 rounded-xl bg-white border border-indigo-100/80 shadow-xs">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Total Predicted Demand</span>
              <p className="text-2xl font-black text-indigo-900 mt-1">
                {forecast.total_predicted_demand} <span className="text-xs font-semibold text-slate-500">cylinders</span>
              </p>
              <span className="text-[10px] text-indigo-600 font-semibold mt-1 inline-block">Horizon: 7 days</span>
            </div>

            {forecast.category_forecasts.map((cat) => (
              <div key={cat.cylinder_category} className="p-4 rounded-xl bg-white border border-indigo-100/80 shadow-xs">
                <span className="text-[10px] font-bold text-slate-400 uppercase truncate block">
                  {cat.category_display_name}
                </span>
                <p className="text-xl font-black text-slate-900 mt-1">
                  {cat.total_predicted_quantity} <span className="text-xs font-normal text-slate-400">units</span>
                </p>
                <span className="text-[10px] text-slate-400">
                  ~{Math.round(cat.total_predicted_quantity / 7)} / day avg
                </span>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-white/80 border border-indigo-100 text-center text-xs text-slate-500">
            {aiOnline ? (
              <span>Loading demand forecast from Python microservice...</span>
            ) : (
              <span className="text-amber-700 font-medium">
                ML forecasting microservice (port 8000) is currently offline. Start the service with{' '}
                <code className="bg-amber-100 px-1 py-0.5 rounded font-mono">python ml/forecast_api.py</code> to view forecasts.
              </span>
            )}
          </div>
        )}
      </div>

      {/* Recent Bookings Queue Snapshot */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-sm font-bold text-slate-900">Recent Customer Bookings</h2>
            <p className="text-[11px] text-slate-400">Incoming requests and accepted queue</p>
          </div>
          <button
            onClick={() => onNavigateTab('booking-requests')}
            className="text-xs font-semibold text-rose-600 hover:text-rose-700 flex items-center gap-1"
          >
            <span>View All</span>
            <ArrowRight size={13} />
          </button>
        </div>

        {overview?.recentBookings && overview.recentBookings.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 font-semibold uppercase text-[10px]">
                  <th className="py-2.5 px-3">Order ID</th>
                  <th className="py-2.5 px-3">Customer</th>
                  <th className="py-2.5 px-3">Category</th>
                  <th className="py-2.5 px-3">Address</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {overview.recentBookings.map((b) => (
                  <tr key={b.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-3 font-mono font-bold text-slate-900">#{b.id}</td>
                    <td className="py-3 px-3 font-semibold text-slate-900">{b.customerName}</td>
                    <td className="py-3 px-3">
                      <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-800 font-medium">
                        {b.cylinderCategory}
                      </span>
                    </td>
                    <td className="py-3 px-3 max-w-[200px] truncate text-slate-500">{b.deliveryAddress}</td>
                    <td className="py-3 px-3">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          isBookingPending(b.status)
                            ? 'bg-rose-50 text-rose-700 border border-rose-200'
                            : isBookingConfirmed(b.status)
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : isBookingOutForDelivery(b.status)
                            ? 'bg-blue-50 text-blue-700 border border-blue-200'
                            : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        }`}
                      >
                        {b.status}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-right">
                      {isBookingPending(b.status) && (
                        <button
                          onClick={() => onNavigateTab('booking-requests')}
                          className="px-2.5 py-1 text-[11px] font-bold bg-rose-600 hover:bg-rose-700 text-white rounded-lg transition-colors"
                        >
                          Review
                        </button>
                      )}
                      {isBookingConfirmed(b.status) && (
                        <button
                          onClick={() => onNavigateTab('orders-queue')}
                          className="px-2.5 py-1 text-[11px] font-bold bg-amber-600 hover:bg-amber-700 text-white rounded-lg transition-colors"
                        >
                          Schedule
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-400 py-6 text-center">No booking records found for this agency.</p>
        )}
      </div>
    </div>
  );
};
