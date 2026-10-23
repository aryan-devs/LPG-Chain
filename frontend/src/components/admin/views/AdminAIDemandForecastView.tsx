import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  BrainCircuit,
  Layers,
  ShieldAlert,
  Loader2,
} from 'lucide-react';
import { PlatformForecastResult } from '../../../types/adminPortalTypes';
import { AdminApiService } from '../../../services/adminApi';

const PREVIEW_FORECAST_7D: PlatformForecastResult = {
  horizonDays: 7,
  generatedAt: new Date().toISOString(),
  modelVersion: 'rf-regressor-v1.2.0-synthetic',
  algorithmName: 'Scikit-Learn Random Forest Regressor',
  dataSourceType: 'SYNTHETIC_TRAINING_DATA',
  syntheticDataDisclaimer:
    'EXPERIMENTAL PREVIEW: This machine learning model has been trained on synthetically generated historical distribution data for testing purposes. It must NOT be represented as a production-validated forecasting system or used for official commercial procurement without live calibrated field telemetry.',
  evaluationMetrics: {
    modelMae: 3.42,
    modelRmse: 4.88,
    baselineMae: 5.92,
    baselineRmse: 7.84,
    maeImprovementPct: 42.2,
    confidenceLevel: '85% (Synthetic Validation)',
  },
  totalAggregatedDemand: 2840,
  categoryForecasts: [
    {
      category: 'DOMESTIC_14_2',
      displayName: 'Domestic 14.2 kg',
      capacityKg: 14.2,
      predictedTotal: 1980,
      dailyPoints: [
        { date: '2026-10-15', dayName: 'Thu', predictedDemand: 280, confidenceLower: 260, confidenceUpper: 300 },
        { date: '2026-10-16', dayName: 'Fri', predictedDemand: 310, confidenceLower: 285, confidenceUpper: 335 },
        { date: '2026-10-17', dayName: 'Sat', predictedDemand: 340, confidenceLower: 310, confidenceUpper: 370 },
        { date: '2026-10-18', dayName: 'Sun', predictedDemand: 160, confidenceLower: 140, confidenceUpper: 180 },
        { date: '2026-10-19', dayName: 'Mon', predictedDemand: 290, confidenceLower: 265, confidenceUpper: 315 },
        { date: '2026-10-20', dayName: 'Tue', predictedDemand: 295, confidenceLower: 270, confidenceUpper: 320 },
        { date: '2026-10-21', dayName: 'Wed', predictedDemand: 305, confidenceLower: 280, confidenceUpper: 330 },
      ],
    },
    {
      category: 'COMMERCIAL_19',
      displayName: 'Commercial 19 kg',
      capacityKg: 19.0,
      predictedTotal: 640,
      dailyPoints: [
        { date: '2026-10-15', dayName: 'Thu', predictedDemand: 85, confidenceLower: 75, confidenceUpper: 95 },
        { date: '2026-10-16', dayName: 'Fri', predictedDemand: 110, confidenceLower: 95, confidenceUpper: 125 },
        { date: '2026-10-17', dayName: 'Sat', predictedDemand: 125, confidenceLower: 110, confidenceUpper: 140 },
        { date: '2026-10-18', dayName: 'Sun', predictedDemand: 95, confidenceLower: 80, confidenceUpper: 110 },
        { date: '2026-10-19', dayName: 'Mon', predictedDemand: 75, confidenceLower: 65, confidenceUpper: 85 },
        { date: '2026-10-20', dayName: 'Tue', predictedDemand: 75, confidenceLower: 65, confidenceUpper: 85 },
        { date: '2026-10-21', dayName: 'Wed', predictedDemand: 75, confidenceLower: 65, confidenceUpper: 85 },
      ],
    },
    {
      category: 'INDUSTRIAL_47_5',
      displayName: 'Industrial 47.5 kg',
      capacityKg: 47.5,
      predictedTotal: 140,
      dailyPoints: [
        { date: '2026-10-15', dayName: 'Thu', predictedDemand: 20, confidenceLower: 15, confidenceUpper: 25 },
        { date: '2026-10-16', dayName: 'Fri', predictedDemand: 25, confidenceLower: 20, confidenceUpper: 30 },
        { date: '2026-10-17', dayName: 'Sat', predictedDemand: 20, confidenceLower: 15, confidenceUpper: 25 },
        { date: '2026-10-18', dayName: 'Sun', predictedDemand: 10, confidenceLower: 5, confidenceUpper: 15 },
        { date: '2026-10-19', dayName: 'Mon', predictedDemand: 20, confidenceLower: 15, confidenceUpper: 25 },
        { date: '2026-10-20', dayName: 'Tue', predictedDemand: 22, confidenceLower: 17, confidenceUpper: 27 },
        { date: '2026-10-21', dayName: 'Wed', predictedDemand: 23, confidenceLower: 18, confidenceUpper: 28 },
      ],
    },
    {
      category: 'MINI_5',
      displayName: 'Mini 5 kg',
      capacityKg: 5.0,
      predictedTotal: 80,
      dailyPoints: [
        { date: '2026-10-15', dayName: 'Thu', predictedDemand: 10, confidenceLower: 8, confidenceUpper: 12 },
        { date: '2026-10-16', dayName: 'Fri', predictedDemand: 15, confidenceLower: 12, confidenceUpper: 18 },
        { date: '2026-10-17', dayName: 'Sat', predictedDemand: 18, confidenceLower: 14, confidenceUpper: 22 },
        { date: '2026-10-18', dayName: 'Sun', predictedDemand: 12, confidenceLower: 9, confidenceUpper: 15 },
        { date: '2026-10-19', dayName: 'Mon', predictedDemand: 8, confidenceLower: 6, confidenceUpper: 10 },
        { date: '2026-10-20', dayName: 'Tue', predictedDemand: 9, confidenceLower: 7, confidenceUpper: 11 },
        { date: '2026-10-21', dayName: 'Wed', predictedDemand: 8, confidenceLower: 6, confidenceUpper: 10 },
      ],
    },
  ],
};

export const AdminAIDemandForecastView: React.FC = () => {
  const [horizonDays, setHorizonDays] = useState<7 | 14 | 30>(7);
  const [selectedDistributorId, setSelectedDistributorId] = useState<string>('ALL');
  const [forecast, setForecast] = useState<PlatformForecastResult>(PREVIEW_FORECAST_7D);
  const [isMlOnline, setIsMlOnline] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;
    const fetchForecast = async () => {
      try {
        setIsLoading(true);
        const online = await AdminApiService.checkMlHealth();
        if (isMounted) setIsMlOnline(online);

        if (online) {
          const distId = selectedDistributorId === 'ALL' ? undefined : Number(selectedDistributorId);
          const liveData = await AdminApiService.getAiDemandForecast(horizonDays, distId);
          if (isMounted) setForecast(liveData);
        } else {
          // Calculate scale factor for 14 or 30 days preview
          const mult = horizonDays === 14 ? 2.05 : horizonDays === 30 ? 4.3 : 1.0;
          if (isMounted) {
            setForecast({
              ...PREVIEW_FORECAST_7D,
              horizonDays,
              totalAggregatedDemand: Math.round(PREVIEW_FORECAST_7D.totalAggregatedDemand * mult),
            });
          }
        }
      } catch {
        if (isMounted) {
          setIsMlOnline(false);
        }
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    fetchForecast();
    return () => {
      isMounted = false;
    };
  }, [horizonDays, selectedDistributorId]);

  const primaryDaily = forecast.categoryForecasts[0]?.dailyPoints || [];
  const maxDemandPoint = Math.max(...primaryDaily.map((p) => p.predictedDemand), 1);

  return (
    <div className="space-y-6">
      {/* View Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Sparkles size={20} className="text-indigo-600" />
            <span>AI Supply Chain Demand Forecast (Aggregated)</span>
            {isLoading && (
              <span className="flex items-center gap-1 text-[11px] font-normal text-slate-400">
                <Loader2 size={13} className="animate-spin text-indigo-500" />
                <span>Computing...</span>
              </span>
            )}
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Predictive machine learning models for national & regional LPG cylinder demand projection.
          </p>
        </div>

        {/* Horizon selector + Agency filter */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Distributor Filter */}
          <div className="relative">
            <select
              value={selectedDistributorId}
              onChange={(e) => setSelectedDistributorId(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-white border border-slate-200 text-xs font-semibold text-slate-700 cursor-pointer shadow-xs focus:outline-none focus:ring-1 focus:ring-rose-500"
            >
              <option value="ALL">Platform Aggregated (All Hubs)</option>
              <option value="1">Apex Gas Distribution Agency (DIST-001)</option>
              <option value="2">Pune Green Gas Depot (DIST-002)</option>
              <option value="3">City Gas Services Ltd (DIST-003)</option>
              <option value="4">Western Bharat Gas Agency (DIST-004)</option>
            </select>
          </div>

          {/* Horizon Pills */}
          <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl border border-slate-200">
            {([7, 14, 30] as (7 | 14 | 30)[]).map((d) => (
              <button
                key={d}
                onClick={() => setHorizonDays(d)}
                className={`px-3 py-1 text-xs font-bold rounded-lg transition-colors ${
                  horizonDays === d
                    ? 'bg-white text-indigo-700 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {d} Days
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ── MANDATORY SYNTHETIC DATA DISCLAIMER BANNER ── */}
      <div className="p-4 rounded-2xl bg-amber-50 border-2 border-amber-300 text-amber-950 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
        <div className="flex items-start gap-3">
          <div className="w-9 h-9 rounded-xl bg-amber-200 text-amber-900 flex items-center justify-center shrink-0 mt-0.5">
            <ShieldAlert size={20} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-black text-xs uppercase tracking-wider text-amber-900 bg-amber-200/80 px-2 py-0.5 rounded">
                Synthetic Training Data Model
              </span>
              <span className="text-[11px] font-semibold text-amber-800">
                Random Forest Regressor ({forecast.modelVersion})
              </span>
            </div>
            <p className="text-xs text-amber-900 mt-1 leading-relaxed">
              {forecast.syntheticDataDisclaimer}
            </p>
          </div>
        </div>

        <div className="shrink-0 flex items-center gap-2">
          <span
            className={`px-2.5 py-1 rounded-full text-[10px] font-bold border ${
              isMlOnline
                ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                : 'bg-amber-100 text-amber-800 border-amber-300'
            }`}
          >
            {isMlOnline ? 'FastAPI Service Online' : 'Static Synthetic Demo'}
          </span>
        </div>
      </div>

      {/* KPI Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">
            Projected Total Demand
          </span>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-3xl font-black text-indigo-950">
              {forecast.totalAggregatedDemand.toLocaleString()}
            </span>
            <span className="text-xs font-semibold text-indigo-600">Cylinders</span>
          </div>
          <span className="text-[11px] text-slate-400 block mt-2">
            Over the next {forecast.horizonDays} days
          </span>
        </div>

        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">
            Model Evaluation MAE
          </span>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-3xl font-black text-emerald-600">
              {forecast.evaluationMetrics.modelMae}
            </span>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded">
              +{forecast.evaluationMetrics.maeImprovementPct}% vs baseline
            </span>
          </div>
          <span className="text-[11px] text-slate-400 block mt-2">
            Baseline MAE: {forecast.evaluationMetrics.baselineMae}
          </span>
        </div>

        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">
            Model RMSE Score
          </span>
          <div className="flex items-baseline gap-2 mt-2">
            <span className="text-3xl font-black text-slate-800">
              {forecast.evaluationMetrics.modelRmse}
            </span>
            <span className="text-xs text-slate-500">Root Mean Sq Err</span>
          </div>
          <span className="text-[11px] text-slate-400 block mt-2">
            Baseline RMSE: {forecast.evaluationMetrics.baselineRmse}
          </span>
        </div>

        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">
            Algorithm Architecture
          </span>
          <div className="mt-2">
            <span className="text-sm font-black text-slate-900 block leading-tight">
              Random Forest Regressor
            </span>
            <span className="text-xs font-semibold text-indigo-600 block mt-0.5">
              Python FastAPI / Scikit-Learn
            </span>
          </div>
          <span className="text-[11px] text-slate-400 block mt-2">
            {forecast.evaluationMetrics.confidenceLevel}
          </span>
        </div>
      </div>

      {/* ── Daily Projection Curve Chart ── */}
      <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <BrainCircuit size={16} className="text-indigo-600" />
              <span>Projected Daily Cylinder Demand Curve</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Daily domestic cylinder demand projections with synthetic confidence bands
            </p>
          </div>

          <div className="flex items-center gap-3 text-xs font-semibold">
            <span className="flex items-center gap-1.5 text-slate-600">
              <span className="w-3 h-3 rounded bg-indigo-600" /> Projected Demand
            </span>
            <span className="flex items-center gap-1.5 text-slate-400">
              <span className="w-3 h-3 rounded bg-indigo-100" /> Confidence Interval
            </span>
          </div>
        </div>

        {/* Visual Forecast Bars */}
        <div className="h-52 flex items-end justify-between gap-3 pt-6 pb-2 border-b border-slate-100">
          {primaryDaily.map((point) => {
            const barHeight = Math.round((point.predictedDemand / maxDemandPoint) * 150);
            return (
              <div
                key={point.date}
                className="flex-1 flex flex-col items-center gap-2 h-full justify-end group"
              >
                <div className="relative flex flex-col items-center w-full">
                  <div
                    style={{ height: `${barHeight}px` }}
                    className="w-full max-w-[42px] bg-indigo-600 rounded-t-lg group-hover:bg-indigo-500 transition-all cursor-pointer relative shadow-xs"
                  >
                    <span className="opacity-0 group-hover:opacity-100 absolute -top-7 left-1/2 -translate-x-1/2 bg-slate-900 text-white text-[10px] font-bold px-1.5 py-0.5 rounded transition-opacity pointer-events-none whitespace-nowrap z-10">
                      {point.predictedDemand} units
                    </span>
                  </div>
                </div>
                <div className="text-center">
                  <span className="text-[11px] font-bold text-slate-700 block">
                    {point.dayName}
                  </span>
                  <span className="text-[9px] text-slate-400 font-mono">
                    {point.date.slice(5)}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex items-center justify-between text-xs text-slate-500">
          <span>Weekly Peak Projected: Saturday (Weekend Domestic Surge)</span>
          <span className="font-semibold text-indigo-600">
            Domestic Refill Share: ~69.7% of total demand
          </span>
        </div>
      </div>

      {/* ── Category Breakdown Cards ── */}
      <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
        <div>
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <Layers size={16} className="text-rose-600" />
            <span>Predicted Demand Breakdown by Cylinder Category</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Aggregated forecast volume across product variants over the {forecast.horizonDays}-day horizon
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {forecast.categoryForecasts.map((cat) => {
            const share = Math.round(
              (cat.predictedTotal / forecast.totalAggregatedDemand) * 100
            );
            return (
              <div
                key={cat.category}
                className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-slate-900">{cat.displayName}</span>
                  <span className="text-[10px] font-mono bg-white px-2 py-0.5 rounded border border-slate-200 font-semibold text-slate-600">
                    {cat.capacityKg} kg
                  </span>
                </div>

                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-black text-slate-900">
                    {cat.predictedTotal.toLocaleString()}
                  </span>
                  <span className="text-xs font-semibold text-slate-500">Units</span>
                </div>

                <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                  <div className="h-full bg-indigo-600 rounded-full" style={{ width: `${share}%` }} />
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-500">
                  <span>Share of Total: {share}%</span>
                  <span className="font-medium">
                    ~{Math.round(cat.predictedTotal / forecast.horizonDays)}/day
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
