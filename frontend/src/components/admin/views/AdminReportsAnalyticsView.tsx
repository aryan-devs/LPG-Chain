import React, { useState } from 'react';
import {
  TrendingUp,
  BarChart3,
  Download,
  FileSpreadsheet,
  CheckCircle2,
  Clock,
  Package,
  Layers,
} from 'lucide-react';
import { AnalyticsDateRange, MonthlyDeliveryTrend } from '../../../types/adminPortalTypes';
import { AdminKpiCard } from '../components/AdminKpiCard';

const PREVIEW_TREND_MONTHS: MonthlyDeliveryTrend[] = [
  { month: 'May 2026', domesticVolume: 3200, commercialVolume: 920, industrialVolume: 240, totalVolume: 4360 },
  { month: 'Jun 2026', domesticVolume: 3450, commercialVolume: 980, industrialVolume: 260, totalVolume: 4690 },
  { month: 'Jul 2026', domesticVolume: 3800, commercialVolume: 1050, industrialVolume: 290, totalVolume: 5140 },
  { month: 'Aug 2026', domesticVolume: 4100, commercialVolume: 1120, industrialVolume: 310, totalVolume: 5530 },
  { month: 'Sep 2026', domesticVolume: 4350, commercialVolume: 1240, industrialVolume: 320, totalVolume: 5910 },
  { month: 'Oct 2026', domesticVolume: 4650, commercialVolume: 1380, industrialVolume: 350, totalVolume: 6380 },
];

export const AdminReportsAnalyticsView: React.FC = () => {
  const [dateRange, setDateRange] = useState<AnalyticsDateRange>('30D');
  const [exportNotice, setExportNotice] = useState<string | null>(null);

  const maxVolume = Math.max(...PREVIEW_TREND_MONTHS.map((m) => m.totalVolume));

  const handleTriggerExport = (reportType: string) => {
    setExportNotice(
      `CSV Export queued for "${reportType}". Backend export endpoints will stream generated CSVs in Stage 2.`
    );
    setTimeout(() => {
      setExportNotice(null);
    }, 4000);
  };

  return (
    <div className="space-y-6">
      {/* View Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <TrendingUp size={20} className="text-rose-600" />
            <span>Platform Reports & Operations Analytics</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Macro-level performance telemetry, delivery completion rates, inventory velocity, and allocation compliance.
          </p>
        </div>

        {/* Date-Range Selector & Export */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl border border-slate-200">
            {(['7D', '30D', 'THIS_MONTH', 'LAST_QUARTER', 'YTD'] as AnalyticsDateRange[]).map(
              (r) => (
                <button
                  key={r}
                  onClick={() => setDateRange(r)}
                  className={`px-2.5 py-1 text-xs font-bold rounded-lg transition-colors ${
                    dateRange === r
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {r.replace(/_/g, ' ')}
                </button>
              )
            )}
          </div>

          <button
            onClick={() => handleTriggerExport('National Operations Summary')}
            className="flex items-center gap-1.5 px-3 py-2 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-xl transition-colors shadow-xs"
          >
            <Download size={13} />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Export notification */}
      {exportNotice && (
        <div className="p-3.5 rounded-xl bg-slate-900 text-white text-xs flex items-center justify-between gap-3 animate-in fade-in duration-150">
          <div className="flex items-center gap-2">
            <FileSpreadsheet size={16} className="text-rose-400" />
            <span>{exportNotice}</span>
          </div>
          <button
            onClick={() => setExportNotice(null)}
            className="text-[11px] text-slate-400 hover:text-white"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* KPI Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <AdminKpiCard
          title="Fulfillment Rate"
          value="97.8%"
          subtitle="On-time delivery SLA target >95%"
          icon={<CheckCircle2 size={20} />}
          iconBgColor="bg-emerald-50"
          iconColor="text-emerald-600"
          trend={{ value: '+1.4% vs last month', isPositive: true }}
          isPlaceholder
        />

        <AdminKpiCard
          title="Avg Dispatch Latency"
          value="4.8 hrs"
          subtitle="Booking to driver gate departure"
          icon={<Clock size={20} />}
          iconBgColor="bg-blue-50"
          iconColor="text-blue-600"
          trend={{ value: '-35 mins improvement', isPositive: true }}
          isPlaceholder
        />

        <AdminKpiCard
          title="Inventory Turnover"
          value="14.2 Days"
          subtitle="Depot cylinder cycle time"
          icon={<Package size={20} />}
          iconBgColor="bg-purple-50"
          iconColor="text-purple-600"
          trend={{ value: 'Optimal velocity', isPositive: true }}
          isPlaceholder
        />

        <AdminKpiCard
          title="Allocation Compliance"
          value="92.4%"
          subtitle="Depot sales vs authorized quota"
          icon={<Layers size={20} />}
          iconBgColor="bg-rose-50"
          iconColor="text-rose-600"
          trend={{ value: 'No critical excess', isNeutral: true }}
          isPlaceholder
        />
      </div>

      {/* 6-Month Volume Trend Breakdown */}
      <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <BarChart3 size={16} className="text-rose-600" />
              <span>Multi-Category Monthly Volume Trends</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Breakdown of Domestic (14.2kg), Commercial (19kg), and Industrial (47.5kg) cylinders delivered
            </p>
          </div>

          <div className="flex items-center gap-4 text-xs font-semibold">
            <span className="flex items-center gap-1.5 text-slate-600">
              <span className="w-3 h-3 rounded bg-rose-600" /> Domestic
            </span>
            <span className="flex items-center gap-1.5 text-slate-600">
              <span className="w-3 h-3 rounded bg-indigo-600" /> Commercial
            </span>
            <span className="flex items-center gap-1.5 text-slate-600">
              <span className="w-3 h-3 rounded bg-amber-500" /> Industrial
            </span>
          </div>
        </div>

        {/* Visual Stacked Bar Chart */}
        <div className="h-56 flex items-end justify-between gap-4 pt-6 pb-2 border-b border-slate-100">
          {PREVIEW_TREND_MONTHS.map((m) => {
            const dH = Math.round((m.domesticVolume / maxVolume) * 160);
            const cH = Math.round((m.commercialVolume / maxVolume) * 160);
            const iH = Math.round((m.industrialVolume / maxVolume) * 160);

            return (
              <div key={m.month} className="flex-1 flex flex-col items-center gap-2 h-full justify-end">
                <div className="flex items-end gap-1 w-full justify-center">
                  <div
                    style={{ height: `${dH}px` }}
                    title={`${m.domesticVolume} Domestic`}
                    className="w-3 sm:w-5 bg-rose-600 rounded-t-sm hover:opacity-85 transition-all cursor-pointer"
                  />
                  <div
                    style={{ height: `${cH}px` }}
                    title={`${m.commercialVolume} Commercial`}
                    className="w-3 sm:w-5 bg-indigo-600 rounded-t-sm hover:opacity-85 transition-all cursor-pointer"
                  />
                  <div
                    style={{ height: `${iH}px` }}
                    title={`${m.industrialVolume} Industrial`}
                    className="w-3 sm:w-5 bg-amber-500 rounded-t-sm hover:opacity-85 transition-all cursor-pointer"
                  />
                </div>
                <span className="text-[11px] font-semibold text-slate-500 text-center">
                  {m.month.split(' ')[0]}
                </span>
              </div>
            );
          })}
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
          <span>Total Oct Volume: <strong>6,380 Units</strong> (~118 Metric Tons)</span>
          <button
            onClick={() => handleTriggerExport('Monthly Volume Breakdown')}
            className="text-rose-600 hover:text-rose-700 font-bold flex items-center gap-1"
          >
            <Download size={12} />
            <span>Export Volume Report</span>
          </button>
        </div>
      </div>

      {/* Additional Report Generation Panels */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl bg-white border border-slate-200/80 shadow-xs flex flex-col justify-between gap-3">
          <div>
            <h4 className="text-xs font-bold text-slate-900">Distributor Performance Comparison</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Fulfillment rate, dispatch latency, and customer rating scores across all depots.
            </p>
          </div>
          <button
            onClick={() => handleTriggerExport('Agency Performance Comparison')}
            className="w-full py-2 rounded-lg text-xs font-semibold text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center justify-center gap-1.5 transition-colors"
          >
            <Download size={13} />
            <span>Export Agency CSV</span>
          </button>
        </div>

        <div className="p-4 rounded-xl bg-white border border-slate-200/80 shadow-xs flex flex-col justify-between gap-3">
          <div>
            <h4 className="text-xs font-bold text-slate-900">Allocation vs Sales Audit</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Regulatory compliance verification comparing OMC quotas against delivered cylinders.
            </p>
          </div>
          <button
            onClick={() => handleTriggerExport('OMC Allocation Reconciliation')}
            className="w-full py-2 rounded-lg text-xs font-semibold text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center justify-center gap-1.5 transition-colors"
          >
            <Download size={13} />
            <span>Export Reconciliation</span>
          </button>
        </div>

        <div className="p-4 rounded-xl bg-white border border-slate-200/80 shadow-xs flex flex-col justify-between gap-3">
          <div>
            <h4 className="text-xs font-bold text-slate-900">Cylinder Defect & Damage Log</h4>
            <p className="text-[11px] text-slate-500 mt-1">
              Damaged cylinders flagged for statutory hydraulic testing and valve servicing.
            </p>
          </div>
          <button
            onClick={() => handleTriggerExport('Cylinder Maintenance and Defect Log')}
            className="w-full py-2 rounded-lg text-xs font-semibold text-slate-700 bg-slate-50 hover:bg-slate-100 border border-slate-200 flex items-center justify-center gap-1.5 transition-colors"
          >
            <Download size={13} />
            <span>Export Defect Log</span>
          </button>
        </div>
      </div>
    </div>
  );
};
