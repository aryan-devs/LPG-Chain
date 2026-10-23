import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  AlertTriangle,
  RefreshCw,
  Terminal,
} from 'lucide-react';
import {
  ForecastResponse,
  ForecastMetricsData,
} from '../../../types/distributorPortalTypes';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorDemandForecastViewProps {
  distributorId: number;
}

export const DistributorDemandForecastView: React.FC<DistributorDemandForecastViewProps> = ({
  distributorId,
}) => {
  const [horizon, setHorizon] = useState<7 | 14 | 30>(7);
  const [forecast, setForecast] = useState<ForecastResponse | null>(null);
  const [metrics, setMetrics] = useState<ForecastMetricsData | null>(null);
  const [loading, setLoading] = useState(true);
  const [serviceOnline, setServiceOnline] = useState<boolean | null>(null);

  const loadForecastData = async () => {
    try {
      setLoading(true);

      const isOnline = await DistributorApiService.checkForecastHealth();
      setServiceOnline(isOnline);

      if (!isOnline) {
        setLoading(false);
        return;
      }

      const [fcRes, metRes] = await Promise.all([
        DistributorApiService.getDemandForecast(horizon, distributorId),
        DistributorApiService.getForecastMetrics().catch(() => null),
      ]);

      setForecast(fcRes);
      if (metRes) setMetrics(metRes);
    } catch {
      // Offline fallback is handled by serviceOnline state
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadForecastData();
  }, [horizon, distributorId]);

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-indigo-50 text-indigo-600">
              <Sparkles size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
              AI / ML Forecasting Component
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Synthetic-Data Demand Forecasting</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Autoregressive machine learning demand predictions across standard 7, 14, and 30 day operational horizons.
          </p>
        </div>

        {/* Horizon Toggle */}
        <div className="flex items-center gap-2 self-start sm:self-auto">
          <div className="bg-slate-100 p-1 rounded-xl flex items-center gap-1 border border-slate-200/60">
            <button
              onClick={() => setHorizon(7)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                horizon === 7 ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              7 Days
            </button>
            <button
              onClick={() => setHorizon(14)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                horizon === 14 ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              14 Days
            </button>
            <button
              onClick={() => setHorizon(30)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                horizon === 30 ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              30 Days
            </button>
          </div>

          <button
            onClick={loadForecastData}
            className="p-2 rounded-xl text-slate-600 hover:bg-slate-100 border border-slate-200 transition-colors"
            title="Refresh Forecast"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Prominent Synthetic Data Disclaimer Banner */}
      <div className="p-4 rounded-xl bg-amber-50 border border-amber-200/80 flex items-start gap-3 shadow-xs">
        <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <div className="flex items-center gap-2">
            <span className="font-bold text-amber-900">
              COLD-START NOTICE: Trained on Synthetic Data Only
            </span>
            <span className="px-2 py-0.2 rounded-full text-[9px] font-mono font-bold bg-amber-200/60 text-amber-900 border border-amber-300">
              SYNTHETIC_TRAINING_DATA
            </span>
          </div>
          <p className="text-amber-800 text-[11px] leading-relaxed">
            The initial model is bootstrapped on a reproducible synthetic dataset (8,025 simulated transactions across 365 days) reflecting seasonal patterns and refill intervals.
            Predictions represent demonstration estimates only and must NOT automatically alter physical stock, official allocations, or delivery commitments.
          </p>
        </div>
      </div>

      {/* Offline Service Error State */}
      {serviceOnline === false && (
        <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center gap-3 text-amber-600">
            <Terminal size={22} />
            <h2 className="text-base font-bold text-slate-900">Python ML Forecast Service Offline</h2>
          </div>
          <p className="text-xs text-slate-600">
            The FastAPI forecasting microservice on port 8000 could not be reached. The rest of your Distributor Portal (Bookings, Queue, Inventory, Schedule) remains 100% operational.
          </p>

          <div className="bg-slate-900 text-slate-100 p-4 rounded-xl font-mono text-xs space-y-2">
            <p className="text-slate-400 text-[11px]">// To start the forecasting microservice in terminal:</p>
            <p className="text-emerald-400">cd ml</p>
            <p className="text-emerald-400">.\.venv\Scripts\python.exe forecast_api.py</p>
          </div>

          <div className="flex justify-start">
            <button
              onClick={loadForecastData}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs transition-colors flex items-center gap-2"
            >
              <RefreshCw size={13} />
              <span>Retry Service Connection</span>
            </button>
          </div>
        </div>
      )}

      {/* Main Forecast Overview Cards */}
      {forecast && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Total Predicted Demand</span>
              <p className="text-3xl font-black text-indigo-900 mt-1">
                {forecast.total_predicted_demand}{' '}
                <span className="text-xs font-semibold text-slate-400">units</span>
              </p>
              <span className="text-[11px] text-indigo-600 font-semibold mt-1 block">
                Next {horizon} Days Outlook
              </span>
            </div>

            {forecast.category_forecasts.map((cat) => (
              <div key={cat.cylinder_category} className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-[10px] font-bold text-slate-400 uppercase truncate">
                    {cat.category_display_name}
                  </span>
                  <span className="text-[10px] font-mono text-slate-400">{cat.capacity_kg} kg</span>
                </div>
                <p className="text-2xl font-black text-slate-900 mt-1">
                  {cat.total_predicted_quantity}{' '}
                  <span className="text-xs font-normal text-slate-400">cylinders</span>
                </p>
                <span className="text-[11px] text-slate-500 block mt-1">
                  ~{Math.round((cat.total_predicted_quantity / horizon) * 10) / 10} / day expected
                </span>
              </div>
            ))}
          </div>

          {/* Daily Demand Trend Visualization */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-sm font-bold text-slate-900">
                  Daily Demand Trajectory ({horizon}-Day Forecast)
                </h2>
                <p className="text-[11px] text-slate-400">
                  Projected aggregate daily refilling requests across categories
                </p>
              </div>
              <span className="text-xs font-mono font-semibold bg-indigo-50 text-indigo-700 px-2.5 py-1 rounded-lg">
                Model: {forecast.model_version}
              </span>
            </div>

            {/* Visual Bar Chart */}
            <div className="h-48 flex items-end gap-1.5 sm:gap-2 pt-6 pb-2 px-2 border-b border-slate-100 overflow-x-auto">
              {(() => {
                const maxVal = Math.max(...forecast.daily_summary.map((d) => d.total_predicted_quantity), 1);
                return forecast.daily_summary.map((day) => {
                  const heightPct = Math.max(12, Math.round((day.total_predicted_quantity / maxVal) * 100));
                  return (
                    <div
                      key={day.date}
                      className="flex-1 min-w-[28px] max-w-[45px] flex flex-col items-center gap-1.5 group relative"
                    >
                      {/* Tooltip */}
                      <div className="absolute -top-8 bg-slate-900 text-white text-[10px] font-mono px-1.5 py-0.5 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
                        {day.date}: {day.total_predicted_quantity} cyl
                      </div>

                      <span className="text-[10px] font-bold text-indigo-900">
                        {day.total_predicted_quantity}
                      </span>
                      <div
                        className="w-full bg-gradient-to-t from-indigo-600 to-indigo-400 rounded-t-md group-hover:from-indigo-700 group-hover:to-indigo-500 transition-all"
                        style={{ height: `${heightPct}%` }}
                      />
                      <span className="text-[9px] text-slate-400 font-mono truncate w-full text-center">
                        {day.date.slice(5)}
                      </span>
                    </div>
                  );
                });
              })()}
            </div>
          </div>

          {/* Category-Wise Breakdown Table */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
            <h2 className="text-sm font-bold text-slate-900 mb-3">Category-Wise Breakdown</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead>
                  <tr className="border-b border-slate-100 text-slate-400 uppercase text-[10px]">
                    <th className="py-2.5 px-3">Cylinder Category</th>
                    <th className="py-2.5 px-3">Capacity</th>
                    <th className="py-2.5 px-3">Service Segment</th>
                    <th className="py-2.5 px-3">Total Predicted</th>
                    <th className="py-2.5 px-3">Daily Average</th>
                    <th className="py-2.5 px-3">Peak Day</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {forecast.category_forecasts.map((cat) => {
                    const peak = cat.daily_forecast.reduce(
                      (max, d) => (d.predicted_quantity > max.predicted_quantity ? d : max),
                      cat.daily_forecast[0]
                    );
                    return (
                      <tr key={cat.cylinder_category} className="hover:bg-slate-50/80">
                        <td className="py-3 px-3 font-bold text-slate-900">{cat.category_display_name}</td>
                        <td className="py-3 px-3">{cat.capacity_kg} kg</td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              cat.service_type === 'DOMESTIC'
                                ? 'bg-rose-50 text-rose-700'
                                : 'bg-blue-50 text-blue-700'
                            }`}
                          >
                            {cat.service_type}
                          </span>
                        </td>
                        <td className="py-3 px-3 font-mono font-bold text-indigo-900">
                          {cat.total_predicted_quantity} cylinders
                        </td>
                        <td className="py-3 px-3 text-slate-600 font-mono">
                          {Math.round((cat.total_predicted_quantity / horizon) * 10) / 10} / day
                        </td>
                        <td className="py-3 px-3 text-slate-500 font-mono text-[11px]">
                          {peak?.date} ({peak?.predicted_quantity} units)
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* Model Evaluation Metrics Section */}
      {metrics && (
        <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900">Model Evaluation & Benchmark Comparison</h2>
              <p className="text-[11px] text-slate-400">
                Evaluation on chronologically held-out synthetic test records (20% held-out test split)
              </p>
            </div>
            <span className="text-[10px] font-mono text-slate-400">
              Trained: {metrics.training_timestamp ? new Date(metrics.training_timestamp).toLocaleDateString() : 'Recent'}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Algorithm</span>
              <p className="text-base font-bold text-slate-900 mt-0.5">{metrics.algorithm}</p>
              <span className="text-[11px] text-slate-500">100 trees, max depth 10</span>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Model Error (MAE / RMSE)</span>
              <p className="text-base font-bold text-indigo-900 mt-0.5">
                {metrics.overall_metrics.model_mae} / {metrics.overall_metrics.model_rmse}
              </p>
              <span className="text-[11px] text-emerald-600 font-semibold">
                {metrics.overall_metrics.mae_improvement_pct}% better than 7-day baseline
              </span>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-[10px] font-bold text-slate-400 uppercase">Baseline Benchmark (7-day MA)</span>
              <p className="text-base font-bold text-slate-700 mt-0.5">
                {metrics.overall_metrics.baseline_mae} / {metrics.overall_metrics.baseline_rmse}
              </p>
              <span className="text-[11px] text-slate-400">Simple moving average baseline</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
