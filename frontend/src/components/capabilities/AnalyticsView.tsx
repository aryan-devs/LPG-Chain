import React, { useState } from 'react';
import { 
  LineChart, 
  TrendingUp, 
  Calendar, 
  Truck, 
  Building2, 
  ArrowUpRight, 
  Users, 
  Clock,
  CheckCircle2
} from 'lucide-react';
import { CapabilityPageShell } from './CapabilityPageShell';

interface AnalyticsViewProps {
  onBackToHome: () => void;
  onOpenBooking: () => void;
  onNavigateToTracking: () => void;
}

export const AnalyticsView: React.FC<AnalyticsViewProps> = ({
  onBackToHome,
  onOpenBooking,
  onNavigateToTracking,
}) => {
  const [timeframe, setTimeframe] = useState<'7d' | '30d' | '90d'>('7d');

  const CHART_DATA = [
    { day: 'Mon', actual: 3420, projected: 3300, fillPercent: 78 },
    { day: 'Tue', actual: 3890, projected: 3600, fillPercent: 88 },
    { day: 'Wed', actual: 4120, projected: 3950, fillPercent: 94 },
    { day: 'Thu', actual: 4050, projected: 4100, fillPercent: 92 },
    { day: 'Fri', actual: 4480, projected: 4300, fillPercent: 100 },
    { day: 'Sat', actual: 4920, projected: 4800, fillPercent: 100 },
    { day: 'Sun', actual: 3100, projected: 3200, fillPercent: 70 },
  ];

  return (
    <CapabilityPageShell
      title="Analytics"
      subtitle="Supply Chain Performance"
      description="Holistic enterprise analytics tracking refill order cadences, doorstep delivery turnaround times, distributor bottling velocity, and predictive regional consumption demand."
      badge="ENTERPRISE TELEMETRY & BI"
      icon={LineChart}
      accentColor="#06b6d4"
      onBackToHome={onBackToHome}
      onOpenBooking={onOpenBooking}
      onNavigateToTracking={onNavigateToTracking}
    >
      <div className="space-y-8">
        {/* Timeframe Selector */}
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-1.5 bg-white border border-slate-200 p-1 rounded-xl text-xs font-bold">
            <button
              type="button"
              onClick={() => setTimeframe('7d')}
              className={`px-3 py-1.5 rounded-lg transition-colors ${
                timeframe === '7d' ? 'bg-cyan-600 text-white' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Past 7 Days
            </button>
            <button
              type="button"
              onClick={() => setTimeframe('30d')}
              className={`px-3 py-1.5 rounded-lg transition-colors ${
                timeframe === '30d' ? 'bg-cyan-600 text-white' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Past 30 Days
            </button>
            <button
              type="button"
              onClick={() => setTimeframe('90d')}
              className={`px-3 py-1.5 rounded-lg transition-colors ${
                timeframe === '90d' ? 'bg-cyan-600 text-white' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Quarterly
            </button>
          </div>

          <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
            <span className="w-2 h-2 rounded-full bg-cyan-500 animate-pulse" />
            <span>Updated: Live telemetry synchronizing</span>
          </div>
        </div>

        {/* ── Section 1: Booking Overview ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">1. Booking Overview</h2>
            <p className="text-xs text-slate-500">Volume and retention metrics across consumer households and commercial accounts</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Total Refills Dispatched</span>
                <Calendar size={16} className="text-cyan-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">27,980</div>
              <div className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 mt-1">
                <ArrowUpRight size={13} />
                <span>+12.4% vs last cycle</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Predictive Reorder Accuracy</span>
                <TrendingUp size={16} className="text-emerald-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">94.8%</div>
              <div className="text-[11px] font-medium text-slate-500 mt-1">Refill booked before depletion</div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>1-Tap App Booking Share</span>
                <Users size={16} className="text-purple-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">81.2%</div>
              <div className="text-[11px] font-medium text-slate-500 mt-1">Digital self-service channel</div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Subsidy Quota Claimed</span>
                <CheckCircle2 size={16} className="text-rose-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">100%</div>
              <div className="text-[11px] font-bold text-emerald-600 mt-1">Zero leak / Zero phantom claims</div>
            </div>
          </div>
        </div>

        {/* ── Section 2: Delivery Performance ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">2. Delivery Performance</h2>
            <p className="text-xs text-slate-500">Fleet telemetry metrics from order acceptance to customer doorstep delivery</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center gap-2 mb-2 text-slate-500 text-xs font-semibold">
                <Clock size={16} className="text-amber-500" />
                <span>Average Doorstep Fulfillment</span>
              </div>
              <div className="text-3xl font-black text-slate-900">1h 48m</div>
              <p className="text-xs text-slate-600 mt-2 leading-relaxed">
                Median elapsed duration between consumer 1-tap confirmation and vehicle driver handoff. Down from 48h industry baseline.
              </p>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center gap-2 mb-2 text-slate-500 text-xs font-semibold">
                <Truck size={16} className="text-blue-500" />
                <span>On-Time Fulfillment Rate</span>
              </div>
              <div className="text-3xl font-black text-emerald-600">98.6%</div>
              <p className="text-xs text-slate-600 mt-2 leading-relaxed">
                Deliveries completed strictly within the scheduled 2-hour customer preference window without rescheduling.
              </p>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center gap-2 mb-2 text-slate-500 text-xs font-semibold">
                <TrendingUp size={16} className="text-cyan-500" />
                <span>Route Optimization Savings</span>
              </div>
              <div className="text-3xl font-black text-slate-900">21.4%</div>
              <p className="text-xs text-slate-600 mt-2 leading-relaxed">
                Kilometers trimmed per multi-stop vehicle dispatch through AI spatial dynamic routing algorithms.
              </p>
            </div>
          </div>
        </div>

        {/* ── Section 3: Distributor Performance ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">3. Distributor Performance</h2>
            <p className="text-xs text-slate-500">Hub agency scoring, bottling plant turnaround, and weighbridge verification</p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-900">Regional Depot Scorecard Highlights</h3>
              <span className="text-xs font-semibold text-slate-500">Top Performing Hubs</span>
            </div>
            <div className="divide-y divide-slate-100">
              {[
                { name: 'Western Hub 04 (Suburban West)', orders: '4,890 / mo', turnaround: '2.8 Days', score: '99.4%', status: 'Tier 1 Prime' },
                { name: 'Metro South Central Depot 11', orders: '6,210 / mo', turnaround: '3.1 Days', score: '98.8%', status: 'Tier 1 Prime' },
                { name: 'Coastal Industrial Plant 02', orders: '3,450 / mo', turnaround: '2.6 Days', score: '99.1%', status: 'Tier 1 Prime' },
              ].map((hub) => (
                <div key={hub.name} className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-cyan-50 text-cyan-700 flex items-center justify-center font-bold">
                      <Building2 size={16} />
                    </div>
                    <div>
                      <div className="font-bold text-slate-900 text-sm">{hub.name}</div>
                      <div className="text-slate-500 mt-0.5">Throughput: {hub.orders} &bull; Cycle Turnaround: {hub.turnaround}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 self-start sm:self-center">
                    <span className="font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
                      Score: {hub.score}
                    </span>
                    <span className="font-semibold text-slate-600 bg-slate-100 px-2.5 py-1 rounded-full">
                      {hub.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── Section 4: Simple Demo Chart & Demand Statistics ── */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">4. Simple Demo Chart & Demand Statistics</h2>
              <p className="text-xs text-slate-500">7-Day projected vs actual daily cylinder consumption across municipal wards</p>
            </div>
            <div className="flex items-center gap-4 text-xs font-semibold">
              <span className="flex items-center gap-1.5 text-slate-600">
                <span className="w-3 h-3 rounded-sm bg-cyan-600" />
                Actual Refills
              </span>
              <span className="flex items-center gap-1.5 text-slate-400">
                <span className="w-3 h-3 rounded-sm bg-slate-300" />
                Projected Model
              </span>
            </div>
          </div>

          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
            <div className="grid grid-cols-7 gap-3 sm:gap-6 items-end h-56 pt-8 pb-3 border-b border-slate-100">
              {CHART_DATA.map((item) => (
                <div key={item.day} className="flex flex-col items-center h-full justify-end group cursor-pointer">
                  <div className="text-[10px] font-bold text-slate-400 mb-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    {item.actual}
                  </div>
                  <div className="w-full max-w-[36px] flex items-end justify-center gap-1 h-full">
                    {/* Projected column */}
                    <div
                      className="w-1/2 bg-slate-200 rounded-t-md transition-all group-hover:bg-slate-300"
                      style={{ height: `${Math.round((item.projected / 5000) * 100)}%` }}
                    />
                    {/* Actual column */}
                    <div
                      className="w-1/2 bg-gradient-to-t from-cyan-600 to-cyan-500 rounded-t-md transition-all group-hover:from-cyan-500 group-hover:to-cyan-400 shadow-sm"
                      style={{ height: `${Math.round((item.actual / 5000) * 100)}%` }}
                    />
                  </div>
                  <span className="text-xs font-bold text-slate-600 mt-2">{item.day}</span>
                </div>
              ))}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 pt-4 text-xs text-slate-500">
              <div>
                <strong>Peak Demand Day:</strong> Saturday (4,920 cylinders) &bull; Weekend domestic consumption spike
              </div>
              <div className="font-semibold text-cyan-700 bg-cyan-50 px-3 py-1 rounded-full border border-cyan-200">
                Forecast Accuracy: 97.4%
              </div>
            </div>
          </div>
        </div>
      </div>
    </CapabilityPageShell>
  );
};
