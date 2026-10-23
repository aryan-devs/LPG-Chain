import React, { useState } from 'react';
import {
  Users,
  Building2,
  CalendarDays,
  Clock,
  Truck,
  CheckCircle2,
  Package,
  AlertTriangle,
  ArrowRight,
  RefreshCw,
  TrendingUp,
  BarChart3,
  ShieldCheck,
  UserPlus,
  Store,
  Layers,
} from 'lucide-react';
import {
  AdminKpiSummary,
  AdminPortalTab,
  BookingTrendPoint,
  DeliveryStatusDistribution,
  DistributorPerformanceSummary,
  SystemActivityItem,
  CategoryStockBreakdown,
} from '../../../types/adminPortalTypes';
import { AdminKpiCard } from '../components/AdminKpiCard';
import { AdminStatusBadge } from '../components/AdminStatusBadge';

interface AdminOverviewViewProps {
  onNavigateTab: (tab: AdminPortalTab) => void;
  isLoading?: boolean;
  onRefresh?: () => void;
}

// Labeled preview placeholders when backend has not seeded full aggregate metrics
const PREVIEW_KPIS: AdminKpiSummary = {
  totalCustomers: 1248,
  totalDistributors: 18,
  totalBookings: 3410,
  pendingBookings: 42,
  activeDeliveries: 38,
  completedDeliveries: 3260,
  totalCylinderInventory: 9450,
  activeAlerts: 3,
  isPlaceholder: true,
};

const PREVIEW_TRENDS: BookingTrendPoint[] = [
  { date: 'Mon', label: 'Mon', bookings: 140, deliveries: 132 },
  { date: 'Tue', label: 'Tue', bookings: 165, deliveries: 150 },
  { date: 'Wed', label: 'Wed', bookings: 190, deliveries: 175 },
  { date: 'Thu', label: 'Thu', bookings: 170, deliveries: 162 },
  { date: 'Fri', label: 'Fri', bookings: 210, deliveries: 198 },
  { date: 'Sat', label: 'Sat', bookings: 240, deliveries: 220 },
  { date: 'Sun', label: 'Sun', bookings: 110, deliveries: 105 },
];

const PREVIEW_DISTRIBUTIONS: DeliveryStatusDistribution[] = [
  { status: 'DELIVERED', count: 3260, percentage: 95.6, color: 'bg-emerald-500' },
  { status: 'OUT_FOR_DELIVERY', count: 38, percentage: 1.1, color: 'bg-blue-500' },
  { status: 'ASSIGNED', count: 42, percentage: 1.2, color: 'bg-amber-500' },
  { status: 'PENDING', count: 42, percentage: 1.2, color: 'bg-indigo-500' },
  { status: 'CANCELLED', count: 28, percentage: 0.9, color: 'bg-rose-500' },
];

const PREVIEW_DISTRIBUTORS: DistributorPerformanceSummary[] = [
  { id: 1, name: 'Apex Gas Distribution Agency', code: 'DIST-001', city: 'Mumbai', activeBookings: 18, completedDeliveries: 1240, onTimeDeliveryRate: 98.2, allocationUtilization: 86.4, status: 'ACTIVE' },
  { id: 2, name: 'Pune Green Gas Depot', code: 'DIST-002', city: 'Pune', activeBookings: 14, completedDeliveries: 890, onTimeDeliveryRate: 96.5, allocationUtilization: 92.1, status: 'ACTIVE' },
  { id: 3, name: 'City Gas Services Ltd', code: 'DIST-003', city: 'Nagpur', activeBookings: 6, completedDeliveries: 620, onTimeDeliveryRate: 94.0, allocationUtilization: 78.5, status: 'ACTIVE' },
  { id: 4, name: 'Western Bharat Gas Agency', code: 'DIST-004', city: 'Thane', activeBookings: 4, completedDeliveries: 510, onTimeDeliveryRate: 99.1, allocationUtilization: 81.0, status: 'PENDING_VERIFICATION' },
];

const PREVIEW_ACTIVITY: SystemActivityItem[] = [
  { id: 'ACT-1', timestamp: '10 mins ago', actor: 'Apex Gas Agency (DIST-001)', actorRole: 'DISTRIBUTOR', action: 'DISPATCH_DELIVERY', entity: 'Booking #BK-10042', status: 'SUCCESS', details: 'Assigned Driver Suresh Patil (MH-04-AB-1234)' },
  { id: 'ACT-2', timestamp: '24 mins ago', actor: 'System Sentinel', actorRole: 'SYSTEM', action: 'ALLOCATION_WARNING', entity: 'Distributor #2 (Pune)', status: 'WARNING', details: 'Depot monthly quota utilization exceeded 90%' },
  { id: 'ACT-3', timestamp: '45 mins ago', actor: 'Customer Anand Verma', actorRole: 'CONSUMER', action: 'CYLINDER_BOOKING', entity: 'Booking #BK-10043', status: 'SUCCESS', details: 'Domestic 14.2kg Refill Booking requested' },
  { id: 'ACT-4', timestamp: '1 hour ago', actor: 'Platform Admin', actorRole: 'ADMIN', action: 'VERIFICATION_AUDIT', entity: 'Distributor #4 (Thane)', status: 'SUCCESS', details: 'KYC Document verification initiated' },
];

const PREVIEW_CATEGORY_STOCKS: CategoryStockBreakdown[] = [
  { category: 'DOMESTIC_14_2', displayName: 'Domestic 14.2 kg', capacityKg: 14.2, filledStock: 4850, emptyStock: 1920, reservedStock: 180, damagedStock: 24, availableStock: 4670, allocatedMonthly: 6000, deliveredMonthly: 4200 },
  { category: 'COMMERCIAL_19', displayName: 'Commercial 19 kg', capacityKg: 19.0, filledStock: 1620, emptyStock: 430, reservedStock: 65, damagedStock: 12, availableStock: 1555, allocatedMonthly: 2000, deliveredMonthly: 1450 },
  { category: 'INDUSTRIAL_47_5', displayName: 'Industrial 47.5 kg', capacityKg: 47.5, filledStock: 340, emptyStock: 85, reservedStock: 15, damagedStock: 4, availableStock: 325, allocatedMonthly: 450, deliveredMonthly: 310 },
  { category: 'MINI_5', displayName: 'Mini 5 kg Chhotu', capacityKg: 5.0, filledStock: 480, emptyStock: 95, reservedStock: 20, damagedStock: 3, availableStock: 460, allocatedMonthly: 600, deliveredMonthly: 420 },
];

export const AdminOverviewView: React.FC<AdminOverviewViewProps> = ({
  onNavigateTab,
  isLoading = false,
  onRefresh,
}) => {
  const [showPreviewData, setShowPreviewData] = useState(true);

  const kpis = PREVIEW_KPIS;
  const maxTrend = Math.max(...PREVIEW_TRENDS.map((t) => Math.max(t.bookings, t.deliveries)), 1);

  return (
    <div className="space-y-6">
      {/* ── Top Hero Banner ── */}
      <div className="rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 text-white p-6 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30">
              PLATFORM OVERSIGHT & GOVERNANCE
            </span>
            <span className="text-xs text-slate-400">National LPG Infrastructure</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-black tracking-tight text-white">
            Administrative Control Center
          </h1>
          <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-xl">
            Real-time platform monitoring, multi-depot inventory visibility, user accounts, and supply chain governance.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start md:self-auto">
          {onRefresh && (
            <button
              onClick={onRefresh}
              disabled={isLoading}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-white/10 hover:bg-white/20 text-white border border-white/10 transition-colors"
            >
              <RefreshCw size={13} className={isLoading ? 'animate-spin' : ''} />
              <span>Refresh</span>
            </button>
          )}
          <button
            onClick={() => onNavigateTab('distributors')}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-500 text-white shadow-md shadow-rose-900/30 transition-colors"
          >
            <span>Manage Agencies</span>
            <ArrowRight size={13} />
          </button>
        </div>
      </div>

      {/* Backend Integration Placeholder Notice */}
      <div className="rounded-xl bg-amber-50/80 border border-amber-200/80 p-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-amber-900">
        <div className="flex items-center gap-2.5">
          <AlertTriangle size={16} className="text-amber-600 shrink-0" />
          <span>
            <strong className="font-semibold">Backend Integration Boundary:</strong> Aggregated platform analytics are running in UI preview mode. Live endpoints will bind in Stage 2.
          </span>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-[11px] text-amber-700">Display Preview Metrics:</span>
          <button
            type="button"
            onClick={() => setShowPreviewData(!showPreviewData)}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-colors ${
              showPreviewData
                ? 'bg-amber-200 text-amber-900'
                : 'bg-white border border-amber-300 text-amber-800'
            }`}
          >
            {showPreviewData ? 'Enabled' : 'Empty State'}
          </button>
        </div>
      </div>

      {/* ── 8 KPI Cards ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <AdminKpiCard
          title="Total Customers"
          value={showPreviewData ? kpis.totalCustomers.toLocaleString() : '0'}
          subtitle="Registered consumer accounts"
          icon={<Users size={20} />}
          iconBgColor="bg-blue-50"
          iconColor="text-blue-600"
          trend={showPreviewData ? { value: '+12% this month', isPositive: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('users')}
        />

        <AdminKpiCard
          title="Total Distributors"
          value={showPreviewData ? kpis.totalDistributors : '0'}
          subtitle="Licensed partner agencies"
          icon={<Building2 size={20} />}
          iconBgColor="bg-purple-50"
          iconColor="text-purple-600"
          trend={showPreviewData ? { value: '4 pending approval', isNeutral: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('distributors')}
        />

        <AdminKpiCard
          title="Total Bookings"
          value={showPreviewData ? kpis.totalBookings.toLocaleString() : '0'}
          subtitle="Platform booking orders"
          icon={<CalendarDays size={20} />}
          iconBgColor="bg-rose-50"
          iconColor="text-rose-600"
          trend={showPreviewData ? { value: '+8.4% volume', isPositive: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('bookings-deliveries')}
        />

        <AdminKpiCard
          title="Pending Bookings"
          value={showPreviewData ? kpis.pendingBookings : '0'}
          subtitle="Awaiting depot confirmation"
          icon={<Clock size={20} />}
          iconBgColor="bg-amber-50"
          iconColor="text-amber-600"
          trend={showPreviewData ? { value: 'Avg age 3.2 hrs', isNeutral: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('bookings-deliveries')}
        />

        <AdminKpiCard
          title="Active Deliveries"
          value={showPreviewData ? kpis.activeDeliveries : '0'}
          subtitle="Cylinders in transit"
          icon={<Truck size={20} />}
          iconBgColor="bg-blue-50"
          iconColor="text-blue-600"
          trend={showPreviewData ? { value: 'GPS fleet online', isPositive: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('bookings-deliveries')}
        />

        <AdminKpiCard
          title="Completed Deliveries"
          value={showPreviewData ? kpis.completedDeliveries.toLocaleString() : '0'}
          subtitle="OTP verified handoffs"
          icon={<CheckCircle2 size={20} />}
          iconBgColor="bg-emerald-50"
          iconColor="text-emerald-600"
          trend={showPreviewData ? { value: '98.5% success', isPositive: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('bookings-deliveries')}
        />

        <AdminKpiCard
          title="Total Cylinder Inventory"
          value={showPreviewData ? kpis.totalCylinderInventory.toLocaleString() : '0'}
          subtitle="Aggregated depot stock"
          icon={<Package size={20} />}
          iconBgColor="bg-indigo-50"
          iconColor="text-indigo-600"
          trend={showPreviewData ? { value: '4 categories audited', isPositive: true } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('inventory-allocations')}
        />

        <AdminKpiCard
          title="Active Alerts"
          value={showPreviewData ? kpis.activeAlerts : '0'}
          subtitle="Depot & quota anomalies"
          icon={<AlertTriangle size={20} />}
          iconBgColor="bg-rose-50"
          iconColor="text-rose-600"
          trend={showPreviewData ? { value: 'Requires review', isPositive: false } : undefined}
          isPlaceholder={showPreviewData}
          onClick={() => onNavigateTab('audit-logs')}
        />
      </div>

      {/* ── Main Dashboard Content Split ── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 spans): Booking Trends + Distributor Performance */}
        <div className="lg:col-span-2 space-y-6">
          {/* Booking & Delivery Trends Chart */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <BarChart3 size={16} className="text-rose-600" />
                  <span>Platform Booking & Dispatch Trends</span>
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  7-day rolling order volume vs successful deliveries
                </p>
              </div>
              <div className="flex items-center gap-4 text-xs font-semibold">
                <span className="flex items-center gap-1.5 text-slate-600">
                  <span className="w-3 h-3 rounded bg-rose-600" /> Bookings
                </span>
                <span className="flex items-center gap-1.5 text-slate-600">
                  <span className="w-3 h-3 rounded bg-emerald-500" /> Deliveries
                </span>
              </div>
            </div>

            {/* Visual Bar Chart */}
            <div className="h-48 flex items-end justify-between gap-3 pt-6 pb-2 border-b border-slate-100">
              {PREVIEW_TRENDS.map((point) => {
                const bHeight = Math.round((point.bookings / maxTrend) * 140);
                const dHeight = Math.round((point.deliveries / maxTrend) * 140);
                return (
                  <div key={point.date} className="flex-1 flex flex-col items-center gap-2 h-full justify-end">
                    <div className="flex items-end gap-1.5 w-full justify-center">
                      <div
                        style={{ height: `${bHeight}px` }}
                        title={`${point.bookings} Bookings`}
                        className="w-4 sm:w-6 bg-rose-600 rounded-t-md hover:bg-rose-500 transition-all cursor-pointer"
                      />
                      <div
                        style={{ height: `${dHeight}px` }}
                        title={`${point.deliveries} Deliveries`}
                        className="w-4 sm:w-6 bg-emerald-500 rounded-t-md hover:bg-emerald-400 transition-all cursor-pointer"
                      />
                    </div>
                    <span className="text-[11px] font-semibold text-slate-500">
                      {point.label}
                    </span>
                  </div>
                );
              })}
            </div>
            <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
              <span>Weekly Average: 182 orders/day</span>
              <span className="text-emerald-600 font-semibold">Fulfillment Rate: 96.2%</span>
            </div>
          </div>

          {/* Distributor Performance Overview */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <Building2 size={16} className="text-rose-600" />
                  <span>Distributor Agency Performance</span>
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  High-volume agency delivery efficiency and quota utilization
                </p>
              </div>
              <button
                onClick={() => onNavigateTab('distributors')}
                className="text-xs font-bold text-rose-600 hover:text-rose-700 flex items-center gap-1"
              >
                <span>View All ({PREVIEW_DISTRIBUTORS.length})</span>
                <ArrowRight size={13} />
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50/60 text-slate-600 font-bold uppercase tracking-wider text-[11px]">
                    <th className="py-2.5 px-3">Agency Name</th>
                    <th className="py-2.5 px-3">City</th>
                    <th className="py-2.5 px-3">Active Queue</th>
                    <th className="py-2.5 px-3">On-Time %</th>
                    <th className="py-2.5 px-3">Allocation Used</th>
                    <th className="py-2.5 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {PREVIEW_DISTRIBUTORS.map((dist) => (
                    <tr key={dist.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-3">
                        <div className="font-bold text-slate-900">{dist.name}</div>
                        <span className="text-[10px] text-slate-400 font-mono">{dist.code}</span>
                      </td>
                      <td className="py-3 px-3 font-medium text-slate-600">{dist.city}</td>
                      <td className="py-3 px-3 font-semibold text-slate-800">
                        {dist.activeBookings} bookings
                      </td>
                      <td className="py-3 px-3 font-bold text-emerald-600">
                        {dist.onTimeDeliveryRate}%
                      </td>
                      <td className="py-3 px-3">
                        <div className="w-24 bg-slate-100 rounded-full h-2 overflow-hidden mb-1">
                          <div
                            className={`h-full rounded-full ${
                              dist.allocationUtilization > 90
                                ? 'bg-amber-500'
                                : 'bg-rose-600'
                            }`}
                            style={{ width: `${dist.allocationUtilization}%` }}
                          />
                        </div>
                        <span className="text-[10px] text-slate-500 font-medium">
                          {dist.allocationUtilization}%
                        </span>
                      </td>
                      <td className="py-3 px-3">
                        <AdminStatusBadge status={dist.status} size="sm" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Right Column (1 span): Delivery Distribution + Quick Actions + Recent Activity */}
        <div className="space-y-6">
          {/* Quick Actions Panel */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
            <h3 className="text-sm font-bold text-slate-900 mb-3 flex items-center gap-2">
              <ShieldCheck size={16} className="text-rose-600" />
              <span>Platform Quick Actions</span>
            </h3>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                onClick={() => onNavigateTab('users')}
                className="p-3 rounded-xl bg-slate-50 hover:bg-rose-50 hover:border-rose-200 border border-slate-200/80 text-left transition-all group"
              >
                <div className="w-8 h-8 rounded-lg bg-white text-rose-600 shadow-xs flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
                  <UserPlus size={16} />
                </div>
                <div className="text-xs font-bold text-slate-800 group-hover:text-rose-700">
                  Manage Users
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">Filter & manage roles</div>
              </button>

              <button
                onClick={() => onNavigateTab('distributors')}
                className="p-3 rounded-xl bg-slate-50 hover:bg-rose-50 hover:border-rose-200 border border-slate-200/80 text-left transition-all group"
              >
                <div className="w-8 h-8 rounded-lg bg-white text-rose-600 shadow-xs flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
                  <Store size={16} />
                </div>
                <div className="text-xs font-bold text-slate-800 group-hover:text-rose-700">
                  Agencies
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">Verify & review depots</div>
              </button>

              <button
                onClick={() => onNavigateTab('inventory-allocations')}
                className="p-3 rounded-xl bg-slate-50 hover:bg-rose-50 hover:border-rose-200 border border-slate-200/80 text-left transition-all group"
              >
                <div className="w-8 h-8 rounded-lg bg-white text-rose-600 shadow-xs flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
                  <Layers size={16} />
                </div>
                <div className="text-xs font-bold text-slate-800 group-hover:text-rose-700">
                  Allocations
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">OMC quotas & physical stock</div>
              </button>

              <button
                onClick={() => onNavigateTab('reports-analytics')}
                className="p-3 rounded-xl bg-slate-50 hover:bg-rose-50 hover:border-rose-200 border border-slate-200/80 text-left transition-all group"
              >
                <div className="w-8 h-8 rounded-lg bg-white text-rose-600 shadow-xs flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
                  <TrendingUp size={16} />
                </div>
                <div className="text-xs font-bold text-slate-800 group-hover:text-rose-700">
                  Analytics
                </div>
                <div className="text-[10px] text-slate-500 mt-0.5">Reports & CSV export</div>
              </button>
            </div>
          </div>

          {/* Delivery Status Distribution */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
            <h3 className="text-sm font-bold text-slate-900 mb-3 flex items-center justify-between">
              <span>Delivery Status Distribution</span>
              <span className="text-[11px] font-semibold text-slate-500">Live Breakdown</span>
            </h3>

            <div className="space-y-3">
              {PREVIEW_DISTRIBUTIONS.map((item) => (
                <div key={item.status} className="space-y-1">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-slate-700">
                      {item.status.replace(/_/g, ' ')}
                    </span>
                    <span className="text-slate-500 font-mono text-[11px]">
                      {item.count.toLocaleString()} ({item.percentage}%)
                    </span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-full rounded-full ${item.color}`}
                      style={{ width: `${Math.max(item.percentage, 2)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Recent System Activity */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-slate-900">Recent System Activity</h3>
              <button
                onClick={() => onNavigateTab('audit-logs')}
                className="text-[11px] font-bold text-rose-600 hover:text-rose-700"
              >
                View Audit
              </button>
            </div>

            <div className="space-y-3">
              {PREVIEW_ACTIVITY.map((act) => (
                <div
                  key={act.id}
                  className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex items-start gap-2.5"
                >
                  <div
                    className={`w-2 h-2 rounded-full mt-1.5 shrink-0 ${
                      act.status === 'SUCCESS'
                        ? 'bg-emerald-500'
                        : act.status === 'WARNING'
                        ? 'bg-amber-500'
                        : 'bg-rose-500'
                    }`}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1">
                      <span className="text-xs font-bold text-slate-900 truncate">
                        {act.action.replace(/_/g, ' ')}
                      </span>
                      <span className="text-[10px] text-slate-400 shrink-0">{act.timestamp}</span>
                    </div>
                    <p className="text-[11px] text-slate-600 mt-0.5 truncate">{act.actor}</p>
                    {act.details && (
                      <p className="text-[10px] text-slate-400 mt-0.5 leading-snug">{act.details}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ── Inventory Overview by Cylinder Category ── */}
      <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Package size={16} className="text-rose-600" />
              <span>Depot Inventory Overview by Cylinder Category</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Physical depot cylinders: filled, empty, reserved, and damaged inventory across all partner hubs
            </p>
          </div>
          <button
            onClick={() => onNavigateTab('inventory-allocations')}
            className="text-xs font-bold text-rose-600 hover:text-rose-700 flex items-center gap-1"
          >
            <span>Detailed Allocations</span>
            <ArrowRight size={13} />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {PREVIEW_CATEGORY_STOCKS.map((cat) => (
            <div
              key={cat.category}
              className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-3"
            >
              <div className="flex items-center justify-between">
                <span className="font-bold text-xs text-slate-900">{cat.displayName}</span>
                <span className="text-[10px] font-mono bg-white px-2 py-0.5 rounded border border-slate-200 text-slate-600 font-semibold">
                  {cat.capacityKg} kg
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2 rounded-lg bg-emerald-50/70 border border-emerald-100">
                  <span className="text-[10px] font-semibold text-emerald-800">Filled Available</span>
                  <div className="text-base font-black text-emerald-700 mt-0.5">
                    {cat.availableStock.toLocaleString()}
                  </div>
                </div>

                <div className="p-2 rounded-lg bg-slate-100/80 border border-slate-200">
                  <span className="text-[10px] font-semibold text-slate-600">Empty Return</span>
                  <div className="text-base font-black text-slate-700 mt-0.5">
                    {cat.emptyStock.toLocaleString()}
                  </div>
                </div>

                <div className="p-2 rounded-lg bg-amber-50/70 border border-amber-100">
                  <span className="text-[10px] font-semibold text-amber-800">Reserved</span>
                  <div className="text-sm font-bold text-amber-700 mt-0.5">
                    {cat.reservedStock.toLocaleString()}
                  </div>
                </div>

                <div className="p-2 rounded-lg bg-rose-50/70 border border-rose-100">
                  <span className="text-[10px] font-semibold text-rose-800">Damaged / OOS</span>
                  <div className="text-sm font-bold text-rose-700 mt-0.5">
                    {cat.damagedStock.toLocaleString()}
                  </div>
                </div>
              </div>

              <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between text-[11px] text-slate-500">
                <span>Monthly Quota: {cat.allocatedMonthly}</span>
                <span className="font-semibold text-slate-700">
                  Delivered: {cat.deliveredMonthly}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
