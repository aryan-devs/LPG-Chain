import React, { useState } from 'react';
import {
  Settings,
  Server,
  Flame,
  Bell,
  Lock,
  Shield,
  CheckCircle2,
  Save,
} from 'lucide-react';
import { SupportedCylinderConfig, ServiceHealthIndicator } from '../../../types/adminPortalTypes';

const CYLINDER_CONFIGS: SupportedCylinderConfig[] = [
  { code: 'DOMESTIC_14_2', displayName: 'Domestic LPG Cylinder (14.2 kg)', capacityKg: 14.2, tareWeightKg: 15.3, serviceType: 'DOMESTIC', regulatedPriceInr: 853, status: 'ACTIVE' },
  { code: 'COMMERCIAL_19', displayName: 'Commercial LPG Cylinder (19 kg)', capacityKg: 19.0, tareWeightKg: 18.5, serviceType: 'COMMERCIAL', regulatedPriceInr: 1740, status: 'ACTIVE' },
  { code: 'INDUSTRIAL_47_5', displayName: 'Industrial High-Pressure Cylinder (47.5 kg)', capacityKg: 47.5, tareWeightKg: 42.0, serviceType: 'COMMERCIAL', regulatedPriceInr: 4320, status: 'ACTIVE' },
  { code: 'MINI_5', displayName: 'Mini Chhotu LPG Cylinder (5 kg)', capacityKg: 5.0, tareWeightKg: 4.8, serviceType: 'DOMESTIC', regulatedPriceInr: 350, status: 'ACTIVE' },
];

const SERVICE_STATUSES: ServiceHealthIndicator[] = [
  { serviceName: 'Spring Boot REST Core', portOrEndpoint: 'Port 8080 (/api)', connectionState: 'STANDBY', protocol: 'HTTP / REST JSON', notes: 'Core domain services, JPA Hibernate & RBAC Security' },
  { serviceName: 'MySQL Relational Store', portOrEndpoint: 'Port 3306 (lpgchain db)', connectionState: 'STANDBY', protocol: 'JDBC MySQL Driver', notes: 'Persistent ACID relational tables & schema migrations' },
  { serviceName: 'Python FastAPI ML Engine', portOrEndpoint: 'Port 8000 (/predict)', connectionState: 'STANDBY', protocol: 'HTTP REST / JSON', notes: 'Random Forest demand projection service' },
  { serviceName: 'Traccar GPS Telemetry', portOrEndpoint: 'Port 8082 (/api/positions)', connectionState: 'STANDBY', protocol: 'HTTP / WebSocket', notes: 'Live vehicle tracking & IoT gateway' },
  { serviceName: 'WebSocket Live Dispatch Pub/Sub', portOrEndpoint: 'STOMP (/ws-tracking)', connectionState: 'STANDBY', protocol: 'SockJS / STOMP', notes: 'Real-time vehicle movement stream to client browsers' },
];

export const AdminSystemSettingsView: React.FC = () => {
  const [activeSettingsTab, setActiveSettingsTab] = useState<'platform' | 'categories' | 'notifications' | 'services'>('platform');

  // Notification toggles
  const [smsDeliveryAlerts, setSmsDeliveryAlerts] = useState(true);
  const [emailDigest, setEmailDigest] = useState(true);
  const [slaDelayThresholdNotice, setSlaDelayThresholdNotice] = useState(true);
  const [quotaDepletionAlert, setQuotaDepletionAlert] = useState(true);

  // Security timeout settings
  const [sessionTimeout, setSessionTimeout] = useState('60');
  const [maxLoginFailures, setMaxLoginFailures] = useState('5');
  const [saveFeedback, setSaveFeedback] = useState<string | null>(null);

  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    setSaveFeedback('Settings preferences saved to client state. Production persist will bind in Stage 2.');
    setTimeout(() => setSaveFeedback(null), 3500);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Settings size={20} className="text-rose-600" />
            <span>Platform Configuration & System Settings</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Configure system parameters, supported cylinder variants, notifications, and telemetry services.
          </p>
        </div>

        {/* Tab switch */}
        <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-xl border border-slate-200">
          <button
            onClick={() => setActiveSettingsTab('platform')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSettingsTab === 'platform'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            General & Security
          </button>
          <button
            onClick={() => setActiveSettingsTab('categories')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSettingsTab === 'categories'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Cylinder Variants
          </button>
          <button
            onClick={() => setActiveSettingsTab('notifications')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSettingsTab === 'notifications'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Notifications
          </button>
          <button
            onClick={() => setActiveSettingsTab('services')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSettingsTab === 'services'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Service Health
          </button>
        </div>
      </div>

      {saveFeedback && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
          <CheckCircle2 size={16} className="text-emerald-600 shrink-0" />
          <span>{saveFeedback}</span>
        </div>
      )}

      {/* Tab 1: General & Security */}
      {activeSettingsTab === 'platform' && (
        <form onSubmit={handleSaveSettings} className="space-y-6">
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Server size={16} className="text-rose-600" />
              <span>Platform Environment Metadata</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80">
                <span className="text-[10px] font-bold text-slate-400 uppercase">Platform Version</span>
                <span className="text-base font-black text-slate-900 block mt-1">LPG-Chain v2.4.0</span>
                <span className="text-[11px] text-slate-500">Production Ready Architecture</span>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80">
                <span className="text-[10px] font-bold text-slate-400 uppercase">Deployment Profile</span>
                <span className="text-base font-black text-rose-600 block mt-1">Local Development (dev)</span>
                <span className="text-[11px] text-slate-500">Port 8080 • Localhost MySQL</span>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80">
                <span className="text-[10px] font-bold text-slate-400 uppercase">ReBAC & OTP Guard</span>
                <span className="text-base font-black text-emerald-600 block mt-1">Enforced (Active)</span>
                <span className="text-[11px] text-slate-500">6-Digit Cryptographic Handshake</span>
              </div>
            </div>
          </div>

          {/* Official Email Whitelist Policies (Non-sensitive) */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Shield size={16} className="text-rose-600" />
              <span>Official Organization Email Policies</span>
            </h3>
            <p className="text-xs text-slate-500">
              Approved corporate domains for administrative staff and licensed agency managers
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2">
                <span className="font-bold text-slate-900 block">Admin Domain Whitelist</span>
                <div className="flex flex-wrap gap-1.5">
                  {['lpgchain.example', 'admin.lpgchain.example', 'lpgchain.demo'].map((d) => (
                    <span
                      key={d}
                      className="px-2 py-0.5 rounded bg-white border border-slate-200 font-mono text-[11px] text-slate-700 font-semibold"
                    >
                      @{d}
                    </span>
                  ))}
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2">
                <span className="font-bold text-slate-900 block">Distributor Agency Whitelist</span>
                <div className="flex flex-wrap gap-1.5">
                  {['distributor.example', 'agency.example', 'approved-distributor.example'].map((d) => (
                    <span
                      key={d}
                      className="px-2 py-0.5 rounded bg-white border border-slate-200 font-mono text-[11px] text-slate-700 font-semibold"
                    >
                      @{d}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Session & Account Security */}
          <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Lock size={16} className="text-rose-600" />
              <span>Session & Access Control Policy</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div>
                <label className="block text-[11px] font-bold text-slate-700 mb-1">
                  JWT Session Inactivity Timeout (Minutes)
                </label>
                <input
                  type="number"
                  value={sessionTimeout}
                  onChange={(e) => setSessionTimeout(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 font-medium focus:outline-none focus:ring-1 focus:ring-rose-500"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-700 mb-1">
                  Maximum Failed Authentication Attempts (Lockout)
                </label>
                <input
                  type="number"
                  value={maxLoginFailures}
                  onChange={(e) => setMaxLoginFailures(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 font-medium focus:outline-none focus:ring-1 focus:ring-rose-500"
                />
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="submit"
                className="flex items-center gap-2 px-4 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-500 rounded-xl shadow-xs transition-colors"
              >
                <Save size={13} />
                <span>Save Security Policies</span>
              </button>
            </div>
          </div>
        </form>
      )}

      {/* Tab 2: Cylinder Categories */}
      {activeSettingsTab === 'categories' && (
        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Flame size={16} className="text-rose-600" />
              <span>Supported Cylinder Specifications & Regulated Pricing</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Standard hardware specifications, tare weights, and current OMC price tiers
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {CYLINDER_CONFIGS.map((cyl) => (
              <div
                key={cyl.code}
                className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2.5 text-xs"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-900">{cyl.displayName}</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    {cyl.status}
                  </span>
                </div>

                <div className="grid grid-cols-3 gap-2 text-slate-600 pt-1">
                  <div className="p-2 rounded-lg bg-white border border-slate-200/70">
                    <span className="text-[10px] text-slate-400 block font-bold">LPG Net Weight</span>
                    <span className="font-black text-slate-900 text-sm">{cyl.capacityKg} kg</span>
                  </div>
                  <div className="p-2 rounded-lg bg-white border border-slate-200/70">
                    <span className="text-[10px] text-slate-400 block font-bold">Tare Weight</span>
                    <span className="font-black text-slate-900 text-sm">{cyl.tareWeightKg} kg</span>
                  </div>
                  <div className="p-2 rounded-lg bg-white border border-slate-200/70">
                    <span className="text-[10px] text-slate-400 block font-bold">Tier Price</span>
                    <span className="font-black text-rose-600 text-sm">₹{cyl.regulatedPriceInr}</span>
                  </div>
                </div>

                <span className="text-[10px] text-slate-400 font-mono block">
                  Category Identifier: {cyl.code} • Segment: {cyl.serviceType}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab 3: Notifications */}
      {activeSettingsTab === 'notifications' && (
        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Bell size={16} className="text-rose-600" />
              <span>Platform Notification Dispatch Rules</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Automated notifications for customers, delivery fleets, and depot managers
            </p>
          </div>

          <div className="space-y-3 text-xs text-slate-700">
            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
              <div>
                <span className="font-bold text-slate-900 block">SMS OTP Delivery Handoff</span>
                <span className="text-[11px] text-slate-500">
                  Transmit single-use 6-digit confirmation OTP to customer upon driver arrival
                </span>
              </div>
              <button
                type="button"
                onClick={() => setSmsDeliveryAlerts(!smsDeliveryAlerts)}
                className={`w-11 h-6 rounded-full transition-colors relative ${
                  smsDeliveryAlerts ? 'bg-rose-600' : 'bg-slate-300'
                }`}
              >
                <span
                  className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                    smsDeliveryAlerts ? 'right-1' : 'left-1'
                  }`}
                />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
              <div>
                <span className="font-bold text-slate-900 block">Daily Operations Email Digest</span>
                <span className="text-[11px] text-slate-500">
                  Send morning summary of daily bookings, dispatches, and inventory anomalies to administrators
                </span>
              </div>
              <button
                type="button"
                onClick={() => setEmailDigest(!emailDigest)}
                className={`w-11 h-6 rounded-full transition-colors relative ${
                  emailDigest ? 'bg-rose-600' : 'bg-slate-300'
                }`}
              >
                <span
                  className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                    emailDigest ? 'right-1' : 'left-1'
                  }`}
                />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
              <div>
                <span className="font-bold text-slate-900 block">7-Day Delivery Window SLA Alerts</span>
                <span className="text-[11px] text-slate-500">
                  Notify administration when a booking order exceeds the statutory 7-day fulfillment window
                </span>
              </div>
              <button
                type="button"
                onClick={() => setSlaDelayThresholdNotice(!slaDelayThresholdNotice)}
                className={`w-11 h-6 rounded-full transition-colors relative ${
                  slaDelayThresholdNotice ? 'bg-rose-600' : 'bg-slate-300'
                }`}
              >
                <span
                  className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                    slaDelayThresholdNotice ? 'right-1' : 'left-1'
                  }`}
                />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
              <div>
                <span className="font-bold text-slate-900 block">Depot Quota Depletion Warning</span>
                <span className="text-[11px] text-slate-500">
                  Trigger alert when agency OMC quota reaches &gt;90% exhaustion before month end
                </span>
              </div>
              <button
                type="button"
                onClick={() => setQuotaDepletionAlert(!quotaDepletionAlert)}
                className={`w-11 h-6 rounded-full transition-colors relative ${
                  quotaDepletionAlert ? 'bg-rose-600' : 'bg-slate-300'
                }`}
              >
                <span
                  className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-transform ${
                    quotaDepletionAlert ? 'right-1' : 'left-1'
                  }`}
                />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tab 4: Service Health (Neutral State as requested) */}
      {activeSettingsTab === 'services' && (
        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Server size={16} className="text-rose-600" />
              <span>Microservice Topology & Gateway Status</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Neutral service readiness representation until live health probes bind in Stage 2
            </p>
          </div>

          <div className="space-y-3">
            {SERVICE_STATUSES.map((srv) => (
              <div
                key={srv.serviceName}
                className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-white border border-slate-200 flex items-center justify-center font-bold text-slate-500">
                    <Server size={16} />
                  </div>
                  <div>
                    <span className="font-bold text-slate-900 block">{srv.serviceName}</span>
                    <span className="text-[11px] text-slate-500">{srv.notes}</span>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <span className="font-mono text-[11px] text-slate-600 bg-white px-2 py-0.5 rounded border border-slate-200">
                    {srv.portOrEndpoint}
                  </span>
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-200/70 text-slate-700 border border-slate-300">
                    <span className="w-2 h-2 rounded-full bg-slate-400" />
                    <span>Standby / Unconnected</span>
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
