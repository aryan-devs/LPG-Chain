import React from 'react';
import {
  FileText,
  Clock,
  CheckCircle2,
  TrendingUp,
  Download,
} from 'lucide-react';
import { DistributorOverviewData } from '../../../types/distributorPortalTypes';

interface DistributorReportsViewProps {
  overview: DistributorOverviewData | null;
}

export const DistributorReportsView: React.FC<DistributorReportsViewProps> = ({ overview }) => {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-rose-50 text-rose-600">
              <FileText size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600">
              Operational Reporting Desk
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Depot Performance & Audit Reports</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Turnaround time metrics, completed fulfillment compliance, and inventory movement summaries.
          </p>
        </div>

        <button
          onClick={() => window.print()}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-900 hover:bg-slate-800 text-white transition-colors self-start sm:self-auto"
        >
          <Download size={14} />
          <span>Export Summary</span>
        </button>
      </div>

      {/* 3 Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Clock size={16} className="text-amber-500" />
            <span className="text-xs font-bold text-slate-700">Average Fulfillment Queue Age</span>
          </div>
          <p className="text-2xl font-black text-slate-900">
            {overview?.averageQueueAgeHours ? `${Math.round(overview.averageQueueAgeHours / 24 * 10) / 10} days` : '0 days'}
          </p>
          <span className="text-[11px] text-slate-500 mt-1 block">
            Target benchmark: &lt; 7 calendar days
          </span>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <CheckCircle2 size={16} className="text-emerald-500" />
            <span className="text-xs font-bold text-slate-700">Verified Deliveries Handover</span>
          </div>
          <p className="text-2xl font-black text-slate-900">
            {overview?.completedDeliveriesCount ?? 0}
          </p>
          <span className="text-[11px] text-slate-500 mt-1 block">
            100% verified via doorstep customer OTP
          </span>
        </div>

        <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp size={16} className="text-purple-500" />
            <span className="text-xs font-bold text-slate-700">Monthly Quota Consumption</span>
          </div>
          <p className="text-2xl font-black text-slate-900">
            {overview?.allocatedMonthlyTotal
              ? `${Math.round(((overview?.soldMonthlyTotal || 0) / overview.allocatedMonthlyTotal) * 100)}%`
              : '0%'}
          </p>
          <span className="text-[11px] text-slate-500 mt-1 block">
            {overview?.soldMonthlyTotal ?? 0} / {overview?.allocatedMonthlyTotal ?? 0} cylinders
          </span>
        </div>
      </div>

      {/* Compliance Log Note */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
        <h2 className="text-sm font-bold text-slate-900 mb-2">Auditable Compliance Standard</h2>
        <p className="text-xs text-slate-600 leading-relaxed">
          In accordance with Petroleum and Explosives Safety Organization (PESO) and Oil Marketing Company (OMC) transparency directives, all physical cylinder deliveries, stock adjustments, and OTP handovers are stored in append-only auditable backend logs.
        </p>
      </div>
    </div>
  );
};
