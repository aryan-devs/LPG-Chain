import React, { useState, useEffect, useMemo } from 'react';
import {
  ShieldCheck,
  Building2,
  Calendar,
  Layers,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Info,
  Flame,
  Scale,
  PackageCheck,
} from 'lucide-react';
import { CustomerProfileData } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';
import { TransparencyReport, DistributorDirectorySummary, CategoryTransparency } from '../../../types/transparencyTypes';

interface PortalTransparencyViewProps {
  profile: CustomerProfileData;
}

export const PortalTransparencyView: React.FC<PortalTransparencyViewProps> = ({ profile }) => {
  const [distributors, setDistributors] = useState<DistributorDirectorySummary[]>([]);
  const [selectedDistributorId, setSelectedDistributorId] = useState<number | null>(null);
  const [availablePeriods, setAvailablePeriods] = useState<string[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('');
  const [report, setReport] = useState<TransparencyReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState<boolean>(false);

  // 1. Load initial distributor list and reporting periods from backend
  useEffect(() => {
    let isMounted = true;
    async function init() {
      try {
        setLoading(true);
        setError(null);

        const [distList, periods] = await Promise.all([
          CustomerService.getDistributorDirectory(),
          CustomerService.getReportingPeriods(),
        ]);

        if (!isMounted) return;

        setDistributors(distList);
        setAvailablePeriods(periods);

        // Determine default period (current or first available)
        const now = new Date();
        const currentPeriodCode = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
        const activePeriod = periods.includes(currentPeriodCode)
          ? currentPeriodCode
          : periods.length > 0
          ? periods[0]
          : currentPeriodCode;
        setSelectedPeriod(activePeriod);

        // Determine default distributor (match profile name or first available)
        if (distList.length > 0) {
          const matched = distList.find(
            (d) =>
              profile.distributorName &&
              d.agencyName.toLowerCase().includes(profile.distributorName.toLowerCase())
          );
          setSelectedDistributorId(matched ? matched.id : distList[0].id);
        } else {
          // If public search has no records yet, fallback to distributor ID 1
          setSelectedDistributorId(1);
        }
      } catch (err: any) {
        if (!isMounted) return;
        setError(err.message || 'Unable to load transparency configuration.');
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    init();
    return () => {
      isMounted = false;
    };
  }, [profile.distributorName]);

  // 2. Load transparency report when selected distributor or period changes
  const loadReport = async (distId: number, periodCode: string, isSilent = false) => {
    if (!isSilent) setLoading(true);
    setError(null);
    try {
      const [yearStr, monthStr] = periodCode.split('-');
      const year = yearStr ? parseInt(yearStr, 10) : undefined;
      const month = monthStr ? parseInt(monthStr, 10) : undefined;

      const data = await CustomerService.getTransparencyReport(distId, year, month);
      setReport(data);
    } catch (err: any) {
      console.error('Failed to load transparency report:', err);
      setError(err.message || 'Unable to fetch verified allocation records for this distributor.');
      setReport(null);
    } finally {
      if (!isSilent) setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    if (selectedDistributorId && selectedPeriod) {
      loadReport(selectedDistributorId, selectedPeriod);
    }
  }, [selectedDistributorId, selectedPeriod]);

  const handleManualRefresh = () => {
    if (selectedDistributorId && selectedPeriod) {
      setRefreshing(true);
      loadReport(selectedDistributorId, selectedPeriod, true);
    }
  };

  // Helper formatting
  const formatPeriodDisplay = (periodCode: string) => {
    if (!periodCode) return '';
    const [year, month] = periodCode.split('-');
    if (!year || !month) return periodCode;
    const date = new Date(parseInt(year, 10), parseInt(month, 10) - 1, 1);
    return date.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  };

  const selectedDistributorName = useMemo(() => {
    const found = distributors.find((d) => d.id === selectedDistributorId);
    if (found) return found.agencyName;
    if (report) return report.distributorName;
    return profile.distributorName || 'Selected Distributor';
  }, [distributors, selectedDistributorId, report, profile.distributorName]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* ── Page Header & Compliance Badge ── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-sm">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
              <ShieldCheck size={14} className="text-rose-600" />
              Official OMC Quota Audit
            </span>
            <span className="text-xs font-medium text-slate-500">Live Database Verification</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">
            Cylinder Allocation & Sales Transparency
          </h1>
          <p className="text-sm text-slate-600 max-w-2xl">
            Real-time public record of officially allocated cylinder quotas versus actual completed deliveries.
            Calculated directly from verified delivery records stored in the LPG-Chain ledger.
          </p>
        </div>

        <button
          type="button"
          onClick={handleManualRefresh}
          disabled={loading || refreshing}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-sm font-semibold transition-all shadow-sm disabled:opacity-50"
        >
          <RefreshCw size={16} className={refreshing ? 'animate-spin text-rose-600' : 'text-slate-500'} />
          <span>{refreshing ? 'Refreshing...' : 'Refresh Records'}</span>
        </button>
      </div>

      {/* ── Information / Disclosure Callout ── */}
      <div className="p-4 rounded-xl bg-amber-50/70 border border-amber-200/80 flex items-start gap-3">
        <Info size={18} className="text-amber-600 flex-shrink-0 mt-0.5" />
        <div className="text-xs text-amber-900 space-y-1">
          <p className="font-bold">Understanding Quota Transparency Figures:</p>
          <p className="leading-relaxed">
            <strong>Allocated Quota:</strong> The official monthly cylinder volume allotted by the Oil Marketing Company (OMC).&nbsp;
            <strong>Delivered Cylinders:</strong> Counts cylinders <em>only</em> from orders with confirmed delivery handover.&nbsp;
            <strong>Remaining Allocation:</strong> Quota balance remaining for this distributor in this period.
            <span className="block mt-1 font-semibold text-amber-800">
              * Note: Remaining quota is distinct from physical depot cylinder inventory. Pending, cancelled, or in-transit orders are never counted as completed deliveries.
            </span>
          </p>
        </div>
      </div>

      {/* ── Filter Controls Bar ── */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-sm flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        {/* Distributor Selector */}
        <div className="flex-1 space-y-1.5">
          <label htmlFor="distributorSelect" className="flex items-center gap-2 text-xs font-bold text-slate-700">
            <Building2 size={14} className="text-rose-600" />
            Distributor Agency
          </label>
          <select
            id="distributorSelect"
            value={selectedDistributorId || ''}
            onChange={(e) => setSelectedDistributorId(Number(e.target.value))}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-600 text-sm font-medium text-slate-900 transition-all"
          >
            {distributors.length > 0 ? (
              distributors.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.agencyName} ({d.city}, PIN {d.pinCode})
                </option>
              ))
            ) : (
              <option value={selectedDistributorId || 1}>
                {selectedDistributorName} (Default)
              </option>
            )}
          </select>
        </div>

        {/* Reporting Period Selector */}
        <div className="w-full md:w-64 space-y-1.5">
          <label htmlFor="periodSelect" className="flex items-center gap-2 text-xs font-bold text-slate-700">
            <Calendar size={14} className="text-rose-600" />
            Reporting Period
          </label>
          <select
            id="periodSelect"
            value={selectedPeriod}
            onChange={(e) => setSelectedPeriod(e.target.value)}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-600 text-sm font-medium text-slate-900 transition-all"
          >
            {availablePeriods.length > 0 ? (
              availablePeriods.map((p) => (
                <option key={p} value={p}>
                  {formatPeriodDisplay(p)} ({p})
                </option>
              ))
            ) : (
              <option value={selectedPeriod}>
                {formatPeriodDisplay(selectedPeriod)} ({selectedPeriod})
              </option>
            )}
          </select>
        </div>
      </div>

      {/* ── Content Viewport ── */}
      {loading ? (
        // Loading State: Skeleton Cards
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map((n) => (
            <div key={n} className="bg-white rounded-2xl border border-slate-200 p-6 animate-pulse space-y-4">
              <div className="h-5 bg-slate-100 rounded w-1/2" />
              <div className="h-8 bg-slate-100 rounded w-3/4" />
              <div className="space-y-2 pt-2">
                <div className="h-4 bg-slate-100 rounded w-full" />
                <div className="h-4 bg-slate-100 rounded w-5/6" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        // Error State
        <div className="bg-white rounded-2xl border border-rose-200 p-8 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
            <AlertCircle size={24} />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-slate-900">Failed to Load Transparency Data</h3>
            <p className="text-sm text-slate-500 max-w-md mx-auto">{error}</p>
          </div>
          <button
            type="button"
            onClick={handleManualRefresh}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-sm font-bold shadow-md shadow-rose-600/25 transition-all"
          >
            Retry Verification
          </button>
        </div>
      ) : !report ? (
        // Empty State: No data available
        <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center space-y-3">
          <PackageCheck size={36} className="mx-auto text-slate-400" />
          <h3 className="text-base font-bold text-slate-800">No Verified Quota Data Available</h3>
          <p className="text-sm text-slate-500 max-w-md mx-auto">
            Official figures for this agency and reporting cycle have not yet been synchronized from the central OMC database.
          </p>
        </div>
      ) : (
        // Active Transparency Report
        <div className="space-y-6">
          {/* Summary Metric Ribbon */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Allocated</span>
              <div className="mt-2 flex items-baseline gap-1.5">
                <span className="text-3xl font-black text-slate-900">
                  {report.totalAllocated > 0 ? report.totalAllocated.toLocaleString() : '—'}
                </span>
                <span className="text-xs font-semibold text-slate-500">cylinders</span>
              </div>
              <span className="text-[11px] font-medium text-slate-400 mt-1">Official period quota</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Actual Delivered</span>
              <div className="mt-2 flex items-baseline gap-1.5">
                <span className="text-3xl font-black text-emerald-600">
                  {report.totalDelivered.toLocaleString()}
                </span>
                <span className="text-xs font-semibold text-slate-500">cylinders</span>
              </div>
              <span className="text-[11px] font-medium text-emerald-700 mt-1 flex items-center gap-1">
                <CheckCircle2 size={12} /> Verified completed sales
              </span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Remaining Balance</span>
              <div className="mt-2 flex items-baseline gap-1.5">
                <span className="text-3xl font-black text-rose-600">
                  {report.hasAllocations ? report.totalRemaining.toLocaleString() : '—'}
                </span>
                <span className="text-xs font-semibold text-slate-500">cylinders</span>
              </div>
              <span className="text-[11px] font-medium text-slate-400 mt-1">Allocation quota left</span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Quota Utilization</span>
              <div className="mt-2 flex items-baseline gap-1.5">
                <span className="text-3xl font-black text-slate-900">
                  {report.hasAllocations ? `${report.overallUtilizationPercentage}%` : '—'}
                </span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-2 mt-2 overflow-hidden">
                <div
                  className="bg-rose-600 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(report.overallUtilizationPercentage, 100)}%` }}
                />
              </div>
            </div>
          </div>

          {/* Category-by-Category Detailed Cards */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Layers size={18} className="text-rose-600" />
                Category-wise Allocation & Sales Breakdown
              </h2>
              <span className="text-xs text-slate-500">
                Period: <strong className="text-slate-800">{formatPeriodDisplay(report.reportingPeriod)}</strong>
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {report.categories.map((cat: CategoryTransparency) => {
                const isAllocated = cat.allocated && cat.allocatedQuantity !== null;

                return (
                  <div
                    key={cat.cylinderCategory}
                    className={`bg-white rounded-2xl border transition-all duration-200 overflow-hidden flex flex-col justify-between ${
                      isAllocated
                        ? 'border-slate-200/90 shadow-sm hover:shadow-md hover:border-slate-300'
                        : 'border-slate-200/70 opacity-90'
                    }`}
                  >
                    {/* Card Header */}
                    <div className="p-5 border-b border-slate-100 bg-slate-50/50">
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-2.5">
                          <div className="w-9 h-9 rounded-xl bg-rose-100/70 text-rose-600 flex items-center justify-center font-bold">
                            <Flame size={18} />
                          </div>
                          <div>
                            <h3 className="text-sm font-bold text-slate-900">{cat.categoryDisplayName}</h3>
                            <span className="text-[11px] font-medium text-slate-500 flex items-center gap-1 mt-0.5">
                              <Scale size={12} /> Capacity: {cat.capacityKg} kg
                            </span>
                          </div>
                        </div>

                        {isAllocated ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 size={10} /> Active Quota
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-600">
                            Unallocated
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Card Metrics Body */}
                    <div className="p-5 space-y-4 flex-1 flex flex-col justify-between">
                      {isAllocated ? (
                        <div className="space-y-3.5">
                          {/* Allocated Quantity */}
                          <div className="flex items-center justify-between text-sm py-1 border-b border-slate-100">
                            <span className="text-slate-600 font-medium">Officially Allocated</span>
                            <span className="font-bold text-slate-900">
                              {cat.allocatedQuantity?.toLocaleString()} units
                            </span>
                          </div>

                          {/* Net Adjustments (if any) */}
                          {cat.adjustmentQuantity !== 0 && (
                            <div className="flex items-center justify-between text-xs py-1 border-b border-slate-100 text-slate-600">
                              <span>Audited Adjustments</span>
                              <span className={cat.adjustmentQuantity > 0 ? 'text-emerald-600 font-bold' : 'text-amber-600 font-bold'}>
                                {cat.adjustmentQuantity > 0 ? `+${cat.adjustmentQuantity}` : cat.adjustmentQuantity} units
                              </span>
                            </div>
                          )}

                          {/* Verified Delivered Quantity */}
                          <div className="flex items-center justify-between text-sm py-1 border-b border-slate-100">
                            <span className="text-slate-600 font-medium">Actual Sold / Delivered</span>
                            <span className="font-bold text-emerald-600">
                              {cat.deliveredQuantity.toLocaleString()} units
                            </span>
                          </div>

                          {/* Remaining Allocation */}
                          <div className="flex items-center justify-between text-sm py-1">
                            <span className="text-slate-600 font-bold">Remaining Allocation</span>
                            <span className="font-black text-rose-600 text-base">
                              {cat.remainingAllocation !== null ? cat.remainingAllocation.toLocaleString() : '—'} units
                            </span>
                          </div>

                          {/* Utilization Bar */}
                          <div className="pt-2">
                            <div className="flex justify-between text-xs font-semibold text-slate-500 mb-1">
                              <span>Quota Utilization</span>
                              <span className="text-slate-800">{cat.utilizationPercentage ?? 0}%</span>
                            </div>
                            <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                              <div
                                className="bg-rose-600 h-2 rounded-full transition-all duration-500"
                                style={{ width: `${Math.min(cat.utilizationPercentage ?? 0, 100)}%` }}
                              />
                            </div>
                          </div>
                        </div>
                      ) : (
                        // Not Allocated Message
                        <div className="py-6 px-4 bg-slate-50/70 rounded-xl border border-dashed border-slate-200 text-center space-y-2">
                          <AlertCircle size={24} className="mx-auto text-slate-400" />
                          <p className="text-xs font-medium text-slate-600 leading-relaxed">
                            {cat.statusMessage ||
                              'No official cylinder allocation has been published for this category and reporting period.'}
                          </p>
                          {cat.deliveredQuantity > 0 && (
                            <p className="text-[11px] font-semibold text-emerald-700">
                              Verified Deliveries Recorded: {cat.deliveredQuantity}
                            </p>
                          )}
                        </div>
                      )}

                      {/* Card Footer Reference */}
                      <div className="pt-4 border-t border-slate-100 text-[11px] text-slate-500 space-y-1">
                        {cat.sourceReference && (
                          <div className="flex items-center justify-between">
                            <span>OMC Ref:</span>
                            <span className="font-mono text-slate-700 truncate max-w-[140px]">
                              {cat.sourceReference}
                            </span>
                          </div>
                        )}
                        <div className="flex items-center justify-between">
                          <span>Status:</span>
                          <span className="text-slate-700 truncate max-w-[180px]">
                            {cat.statusMessage}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Audit Verification Footer Card */}
          <div className="p-4 rounded-xl bg-slate-100/70 border border-slate-200 text-slate-600 text-xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div className="flex items-center gap-2">
              <Building2 size={16} className="text-slate-400" />
              <span>
                Agency: <strong>{selectedDistributorName}</strong> | Period: <strong>{report.reportingPeriod}</strong>
              </span>
            </div>

            <div className="flex items-center gap-2 text-slate-500">
              <span>Last Synchronized:</span>
              <strong className="text-slate-700 font-mono">
                {new Date(report.lastUpdated).toLocaleString('en-IN', {
                  day: '2-digit',
                  month: 'short',
                  year: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </strong>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
