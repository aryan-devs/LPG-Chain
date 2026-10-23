import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  AlertCircle,
  RefreshCw,
  Info,
  Flame,
  ShieldCheck,
} from 'lucide-react';
import { CustomerService } from '../../../services/customerApi';
import { TransparencyReport, CategoryTransparency } from '../../../types/transparencyTypes';

interface DistributorAllocationSalesViewProps {
  distributorId: number;
}

export const DistributorAllocationSalesView: React.FC<DistributorAllocationSalesViewProps> = ({
  distributorId,
}) => {
  const [report, setReport] = useState<TransparencyReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const now = new Date();
  const [year, setYear] = useState<number>(now.getFullYear());
  const [month, setMonth] = useState<number>(now.getMonth() + 1);

  const fetchReport = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await CustomerService.getTransparencyReport(distributorId, year, month);
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch transparency report');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [distributorId, year, month]);

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-emerald-50 text-emerald-600">
              <BarChart3 size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">
              Government Transparency Module
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Allocation Quota & Completed Sales</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Category-wise tracking of official OMC cylinder quota allocations vs verified completed customer sales.
          </p>
        </div>

        {/* Period Selector */}
        <div className="flex items-center gap-2 self-start sm:self-auto">
          <select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            className="text-xs p-2 rounded-xl bg-slate-50 border border-slate-200 font-bold text-slate-700"
          >
            {monthNames.map((name, i) => (
              <option key={name} value={i + 1}>
                {name}
              </option>
            ))}
          </select>

          <select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            className="text-xs p-2 rounded-xl bg-slate-50 border border-slate-200 font-bold text-slate-700"
          >
            <option value={2025}>2025</option>
            <option value={2026}>2026</option>
            <option value={2027}>2027</option>
          </select>

          <button
            onClick={fetchReport}
            className="p-2 rounded-xl text-slate-600 hover:bg-slate-100 border border-slate-200"
            title="Refresh Report"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Aggregate Balance Ribbon */}
      {report && (
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Total Allocated</span>
            <p className="text-2xl font-black text-slate-900 mt-1">{report.totalAllocated}</p>
            <span className="text-[11px] text-slate-500">Official OMC published quota</span>
          </div>

          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Total Delivered</span>
            <p className="text-2xl font-black text-blue-600 mt-1">{report.totalDelivered}</p>
            <span className="text-[11px] text-slate-500">OTP verified handovers</span>
          </div>

          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Remaining Balance</span>
            <p className="text-2xl font-black text-emerald-600 mt-1">{report.totalRemaining}</p>
            <span className="text-[11px] text-slate-500">Available quota balance</span>
          </div>

          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Utilization Rate</span>
            <p className="text-2xl font-black text-purple-600 mt-1">{report.overallUtilizationPercentage}%</p>
            <span className="text-[11px] text-slate-500">Sales vs net allocation</span>
          </div>
        </div>
      )}

      {/* Category Breakdown Cards */}
      {loading && !report ? (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <RefreshCw className="w-6 h-6 text-emerald-600 animate-spin mx-auto mb-2" />
          <p className="text-xs text-slate-500 font-semibold">Generating transparency figures from MySQL...</p>
        </div>
      ) : report?.categories && report.categories.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {report.categories.map((cat: CategoryTransparency) => (
            <div
              key={cat.cylinderCategory}
              className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                      <Flame size={16} />
                    </div>
                    <div>
                      <h3 className="font-bold text-sm text-slate-900">{cat.categoryDisplayName}</h3>
                      <span className="text-[10px] text-slate-400">{cat.capacityKg} kg standard</span>
                    </div>
                  </div>
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      cat.allocated
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {cat.allocated ? 'ALLOCATED' : 'UNPUBLISHED'}
                  </span>
                </div>

                <div className="space-y-2.5 my-4 text-xs">
                  <div className="flex justify-between text-slate-600">
                    <span>Allocated Quota:</span>
                    <strong className="text-slate-900">{cat.allocatedQuantity ?? '—'}</strong>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Quota Adjustments:</span>
                    <strong className={cat.adjustmentQuantity >= 0 ? 'text-emerald-600' : 'text-rose-600'}>
                      {cat.adjustmentQuantity >= 0 ? `+${cat.adjustmentQuantity}` : cat.adjustmentQuantity}
                    </strong>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Net Quota:</span>
                    <strong className="text-slate-900">{cat.netAllocatedQuantity ?? '—'}</strong>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Completed Sales:</span>
                    <strong className="text-blue-600">{cat.deliveredQuantity}</strong>
                  </div>
                  <div className="flex justify-between text-slate-600 pt-2 border-t border-slate-100">
                    <span>Remaining Balance:</span>
                    <strong className="text-emerald-700 font-bold">{cat.remainingAllocation ?? '—'}</strong>
                  </div>
                </div>

                {cat.allocated && (
                  <div>
                    <div className="flex justify-between text-[11px] font-semibold text-slate-500 mb-1">
                      <span>Quota Consumed</span>
                      <span>{cat.utilizationPercentage}%</span>
                    </div>
                    <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full bg-emerald-500 rounded-full"
                        style={{ width: `${Math.min(100, cat.utilizationPercentage || 0)}%` }}
                      />
                    </div>
                  </div>
                )}
              </div>

              <div className="pt-3 border-t border-slate-100 text-[10px] text-slate-400 mt-4">
                {cat.statusMessage}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <Info className="w-10 h-10 text-slate-300 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-800">No Allocation Data for Period</h3>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            No official OMC quota allocations have been registered for this distributor in {monthNames[month - 1]} {year}.
          </p>
        </div>
      )}

      {/* Rules Notice */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-600 space-y-1">
        <span className="font-bold text-slate-800 flex items-center gap-1.5">
          <ShieldCheck size={14} className="text-emerald-600" />
          Compliance & Transparency Assurance
        </span>
        <p className="text-[11px] text-slate-500">
          Sales figures represent only qualifying deliveries completed with verified customer handover.
          Pending, rejected, or cancelled requests are strictly excluded to ensure consumer market integrity.
        </p>
      </div>
    </div>
  );
};
