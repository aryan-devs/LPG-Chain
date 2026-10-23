import React, { useState } from 'react';
import { 
  ShieldAlert, 
  AlertTriangle, 
  CheckCircle2, 
  Gauge, 
  Thermometer, 
  Radio, 
  Zap, 
  RefreshCw,
  Filter
} from 'lucide-react';
import { CapabilityPageShell } from './CapabilityPageShell';

interface AISentinelViewProps {
  onBackToHome: () => void;
  onOpenBooking: () => void;
  onNavigateToTracking: () => void;
}

export const AISentinelView: React.FC<AISentinelViewProps> = ({
  onBackToHome,
  onOpenBooking,
  onNavigateToTracking,
}) => {
  const [filterSeverity, setFilterSeverity] = useState<'all' | 'high' | 'normal'>('all');

  const RECENT_ALERTS = [
    {
      id: 'ALT-8841',
      title: 'Pressure Variance Re-stabilized',
      location: 'Hub 04 • Suburban Depot West',
      time: '4 mins ago',
      severity: 'normal',
      status: 'Resolved',
      detail: 'Pressure delta within allowable 0.02 bar baseline. Automated solenoid test passed.',
    },
    {
      id: 'ALT-8839',
      title: 'Geofence Exit Notification',
      location: 'Vehicle MH-02-CB-4912 (Transit Hub 12)',
      time: '18 mins ago',
      severity: 'high',
      status: 'Investigating',
      detail: 'Delivery vehicle deviated 340m from prescribed arterial route. Driver acknowledged road maintenance diversion.',
    },
    {
      id: 'ALT-8832',
      title: 'Tare Weight Verification Confirmed',
      location: 'Bottling Carousel Bay 3',
      time: '42 mins ago',
      severity: 'normal',
      status: 'Verified',
      detail: 'Digital weighbridge validated cylinder net weight 14.21kg (nominal ± 0.05kg tolerance).',
    },
    {
      id: 'ALT-8825',
      title: 'Tamper Seal Handshake Audit',
      location: 'Customer Doorstep • Order #LPG-9921',
      time: '1 hr ago',
      severity: 'normal',
      status: 'Passed',
      detail: 'Cryptographic QR and NFC digital seal verified by consumer smartphone app before delivery handoff.',
    },
  ];

  const filteredAlerts = RECENT_ALERTS.filter((item) => {
    if (filterSeverity === 'all') return true;
    return item.severity === filterSeverity;
  });

  return (
    <CapabilityPageShell
      title="AI Sentinel"
      subtitle="Intelligent LPG Supply Monitoring"
      description="Autonomous neural safety grid continuously monitoring container pressure, tare weight thresholds, transit geofences, and doorstep seal integrity."
      badge="SAFETY GRID"
      icon={ShieldAlert}
      accentColor="#f59e0b"
      onBackToHome={onBackToHome}
      onOpenBooking={onOpenBooking}
      onNavigateToTracking={onNavigateToTracking}
    >
      <div className="space-y-8">
        {/* ── Section 1: Risk Overview ── */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">1. Risk Overview</h2>
              <p className="text-xs text-slate-500">Live sensory metrics aggregated across all active cylinders and fleet hubs</p>
            </div>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold border border-emerald-200">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              Safety Status: Nominal
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                <span>Overall Risk Index</span>
                <ShieldAlert size={16} className="text-emerald-500" />
              </div>
              <div className="my-3">
                <div className="text-3xl font-black text-slate-900 tracking-tight">0.03%</div>
                <div className="text-[11px] text-emerald-600 font-bold mt-0.5">Ultra-Low Risk &bull; 99.97% Safe</div>
              </div>
              <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
                <div className="bg-emerald-500 h-full w-[3%]" />
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                <span>Pressure Variance</span>
                <Gauge size={16} className="text-amber-500" />
              </div>
              <div className="my-3">
                <div className="text-3xl font-black text-slate-900 tracking-tight">1.02 <span className="text-sm font-semibold text-slate-400">bar</span></div>
                <div className="text-[11px] text-slate-500 font-medium mt-0.5">Optimal Range (0.95 - 1.15 bar)</div>
              </div>
              <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
                <div className="bg-amber-500 h-full w-[65%]" />
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                <span>Ambient Temperature</span>
                <Thermometer size={16} className="text-blue-500" />
              </div>
              <div className="my-3">
                <div className="text-3xl font-black text-slate-900 tracking-tight">28.4 <span className="text-sm font-semibold text-slate-400">°C</span></div>
                <div className="text-[11px] text-slate-500 font-medium mt-0.5">Threshold Ceiling: 52.0 °C</div>
              </div>
              <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
                <div className="bg-blue-500 h-full w-[45%]" />
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                <span>Active IoT Sensory Nodes</span>
                <Radio size={16} className="text-rose-500" />
              </div>
              <div className="my-3">
                <div className="text-3xl font-black text-slate-900 tracking-tight">18,420</div>
                <div className="text-[11px] text-slate-500 font-medium mt-0.5">Micro-watt BLE & NB-IoT Grid</div>
              </div>
              <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
                <div className="bg-rose-500 h-full w-[98%]" />
              </div>
            </div>
          </div>
        </div>

        {/* ── Section 2: Suspicious Activity Monitoring ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">2. Suspicious Activity</h2>
            <p className="text-xs text-slate-500">Autonomous neural anomaly detection detecting theft attempts, black-market diversion, and tampering</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center font-bold">
                  <AlertTriangle size={18} />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Rapid Depletion Alerts</h3>
                  <span className="text-[11px] text-slate-500 font-medium">Unusual consumption spike</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                Flags consumers or commercial accounts consuming refills at over 3.5x standard historical cadence, indicating potential sub-letting or unregistered multi-burner connections.
              </p>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">24h Incidents</span>
                <span className="font-bold text-slate-900">0 Active Alerts</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
                  <Zap size={18} />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Seal Interruption Signal</h3>
                  <span className="text-[11px] text-slate-500 font-medium">Cap detachment before delivery</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                NFC cryptographic digital seals emit an immediate tamper beacon if the aluminum seal foil is cut or severed prior to the consumer app handshake.
              </p>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Seal Integrity</span>
                <span className="font-bold text-emerald-600">100% Intact</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
                  <RefreshCw size={18} />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Ghost Refill Prevention</h3>
                  <span className="text-[11px] text-slate-500 font-medium">Subsidy fraud countermeasure</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                Matches cylinder serial numbers with the consumer consumer number and physical delivery GPS coordinates to block paper-only phantom claims.
              </p>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Fraud Attempts Prevented</span>
                <span className="font-bold text-slate-900">142 this quarter</span>
              </div>
            </div>
          </div>
        </div>

        {/* ── Section 3: Delivery Anomalies ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">3. Delivery Anomalies</h2>
            <p className="text-xs text-slate-500">Automated inspection points during transit from bottling plant to neighborhood delivery truck</p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Active Transit Telemetry Checks</h3>
                <span className="text-xs text-slate-500">Live weighbridge and GPS corridor monitoring</span>
              </div>
              <span className="px-3 py-1 rounded-full bg-slate-100 text-slate-700 text-xs font-semibold">
                Updated in real time via STOMP
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-slate-100">
              <div className="p-5 space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-slate-800">Tare Weight Accuracy</span>
                  <span className="font-bold text-emerald-600">± 12g (Goal: ± 25g)</span>
                </div>
                <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                  <div className="bg-emerald-500 h-full w-[96%]" />
                </div>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Every 14.2kg domestic and 19kg commercial cylinder is weighed on certified weighbridge sensors at depot release. Any variation above 25g auto-reroutes to inspection.
                </p>
              </div>

              <div className="p-5 space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-slate-800">Geofence Route Adherence</span>
                  <span className="font-bold text-emerald-600">99.2% on Designated Track</span>
                </div>
                <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                  <div className="bg-blue-500 h-full w-[99.2%]" />
                </div>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Fleet trucks operate within strict municipal ward boundaries. Unscheduled stops longer than 12 minutes outside geofenced zones trigger automated supervisor dispatch.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* ── Section 4: Recent Alerts ── */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">4. Recent Alerts</h2>
              <p className="text-xs text-slate-500">Chronological telemetry audit log from IoT sensors and distribution gateways</p>
            </div>

            <div className="flex items-center gap-1.5 bg-white border border-slate-200 rounded-xl p-1 text-xs font-semibold">
              <Filter size={13} className="text-slate-400 ml-1.5" />
              <button
                type="button"
                onClick={() => setFilterSeverity('all')}
                className={`px-2.5 py-1 rounded-lg transition-colors ${
                  filterSeverity === 'all' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                All
              </button>
              <button
                type="button"
                onClick={() => setFilterSeverity('high')}
                className={`px-2.5 py-1 rounded-lg transition-colors ${
                  filterSeverity === 'high' ? 'bg-rose-600 text-white' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                High Priority
              </button>
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm divide-y divide-slate-100 overflow-hidden">
            {filteredAlerts.map((alert) => (
              <div key={alert.id} className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/70 transition-colors">
                <div className="flex items-start gap-3">
                  <div
                    className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5 ${
                      alert.severity === 'high'
                        ? 'bg-rose-50 text-rose-600 border border-rose-200'
                        : 'bg-emerald-50 text-emerald-600 border border-emerald-200'
                    }`}
                  >
                    {alert.severity === 'high' ? <AlertTriangle size={15} /> : <CheckCircle2 size={15} />}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-slate-900">{alert.title}</span>
                      <span className="text-[10px] font-mono font-bold text-slate-400">{alert.id}</span>
                    </div>
                    <p className="text-xs text-slate-600 mt-0.5 leading-relaxed">{alert.detail}</p>
                    <div className="text-[11px] text-slate-400 mt-1">
                      {alert.location} &bull; {alert.time}
                    </div>
                  </div>
                </div>

                <span
                  className={`text-xs font-bold px-2.5 py-1 rounded-full self-start sm:self-center flex-shrink-0 border ${
                    alert.status === 'Resolved' || alert.status === 'Verified' || alert.status === 'Passed'
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : 'bg-amber-50 text-amber-700 border-amber-200'
                  }`}
                >
                  {alert.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </CapabilityPageShell>
  );
};
