import React from 'react';
import { 
  Lock, 
  ShieldCheck, 
  KeyRound, 
  Users, 
  QrCode, 
  Fingerprint, 
  FileText, 
  CheckCircle2 
} from 'lucide-react';
import { CapabilityPageShell } from './CapabilityPageShell';

interface SecurityViewProps {
  onBackToHome: () => void;
  onOpenBooking: () => void;
  onNavigateToTracking: () => void;
}

export const SecurityView: React.FC<SecurityViewProps> = ({
  onBackToHome,
  onOpenBooking,
  onNavigateToTracking,
}) => {
  const RBAC_ROLES = [
    {
      role: 'Consumer',
      description: 'Domestic & commercial end-users',
      permissions: ['1-Tap Refill Booking', 'Live Telemetry Map Tracking', 'Quota Usage History', 'Digital Seal Handshake'],
      badge: 'Public Client',
    },
    {
      role: 'Distributor Agency',
      description: 'Authorized regional bottling & hub depots',
      permissions: ['Bulk Stock Reconciliation', 'Weighbridge Dispatch Sign-off', 'Driver Allocation Matrix', 'Depot Compliance Uploads'],
      badge: 'Operator Level',
    },
    {
      role: 'Logistics Driver',
      description: 'Doorstep delivery vehicle operators',
      permissions: ['GPS Telemetry Beacon Broadcast', 'Dynamic Stop Routing Navigation', 'Doorstep OTP / NFC Verification', 'Tare Weight Proof'],
      badge: 'Field Personnel',
    },
    {
      role: 'Auditor & Regulator',
      description: 'Ministry & PESO safety inspectorate',
      permissions: ['Full Cryptographic Ledger Access', 'Anti-Diversion Anomaly Logs', 'Statutory License Verification', 'System-wide Halt Control'],
      badge: 'Supervisory',
    },
  ];

  const ACCESS_LOGS = [
    {
      timestamp: 'Today, 01:28 AM',
      actor: 'Consumer: Anand Verma',
      action: 'Authenticated OTP Handshake (JWT Issued)',
      ip: '103.21.24.89 (Mumbai, IN)',
      status: 'Success',
    },
    {
      timestamp: 'Today, 01:14 AM',
      actor: 'Driver: Ramesh Kumar (Truck #02)',
      action: 'Cryptographic Custody Handshake at Doorstep',
      ip: '49.36.110.12 (GPS Cell Relay)',
      status: 'Verified',
    },
    {
      timestamp: 'Yesterday, 11:42 PM',
      actor: 'Distributor: Adarsh Gas Hub',
      action: 'Batch Dispatch Seal Manifest Sign-off (142 Cylinders)',
      ip: '115.114.78.21 (Depot Gateway)',
      status: 'Signed',
    },
    {
      timestamp: 'Yesterday, 09:15 PM',
      actor: 'Automated AI Sentinel Service',
      action: 'Hourly Tare Weight Discrepancy Integrity Sweep',
      ip: 'Internal Microservice VPC',
      status: 'Clean',
    },
  ];

  return (
    <CapabilityPageShell
      title="Security"
      subtitle="Secure LPG-Chain Platform"
      description="Zero-trust cryptographic infrastructure enforcing dual-factor authentication, granular role-based access control (RBAC), and tamper-evident digital seals across the entire physical supply chain."
      badge="CRYPTOGRAPHIC DEFENSE"
      icon={Lock}
      accentColor="#059669"
      onBackToHome={onBackToHome}
      onOpenBooking={onOpenBooking}
      onNavigateToTracking={onNavigateToTracking}
    >
      <div className="space-y-8">
        {/* ── Section 1: Authentication ── */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">1. Authentication & Identity</h2>
              <p className="text-xs text-slate-500">Multi-tier identity validation securing consumers, delivery drivers, and administrative operators</p>
            </div>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 flex items-center gap-1.5">
              <ShieldCheck size={14} className="text-emerald-600" />
              Auth Standard: NIST SP 800-63B
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Multi-Factor</span>
                  <KeyRound size={18} className="text-emerald-600" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">Dual-Factor OTP Handshake</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Cryptographically salted one-time passwords delivered via secure SMS and WhatsApp API gateways with 120-second dynamic expiration.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Session Security</span>
                <span className="font-bold text-slate-900">JWT Bearer (RSA-256)</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Device Trust</span>
                  <Fingerprint size={18} className="text-blue-600" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">Hardware Bound Keys</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Depot operators and delivery drivers bind mobile devices using Hardware Security Module (HSM) device fingerprinting to prevent session cloning.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Device Integrity</span>
                <span className="font-bold text-emerald-600">Zero Spoof Rate</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Transit Handshake</span>
                  <QrCode size={18} className="text-purple-600" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">Doorstep QR Authentication</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Every refill requires two-party confirmation: the consumer scans the driver dynamic QR code or vice versa, sealing custody cryptographically.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Dispute Rate</span>
                <span className="font-bold text-emerald-600">&lt; 0.001%</span>
              </div>
            </div>
          </div>
        </div>

        {/* ── Section 2: Role-Based Access Control (RBAC) ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">2. Role-Based Access</h2>
            <p className="text-xs text-slate-500">Granular permission boundaries restricting access across consumers, distributors, drivers, and regulators</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {RBAC_ROLES.map((item) => (
              <div key={item.role} className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider bg-slate-100 px-2 py-0.5 rounded-full">
                      {item.badge}
                    </span>
                    <Users size={15} className="text-slate-400" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">{item.role}</h3>
                  <p className="text-xs text-slate-500 mt-0.5 mb-3">{item.description}</p>

                  <div className="space-y-1.5 border-t border-slate-100 pt-3">
                    {item.permissions.map((perm) => (
                      <div key={perm} className="flex items-center gap-1.5 text-xs text-slate-700">
                        <CheckCircle2 size={13} className="text-emerald-500 flex-shrink-0" />
                        <span className="leading-tight">{perm}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ── Section 3: Security Status ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">3. Security Status</h2>
            <p className="text-xs text-slate-500">Live platform defense posture and tamper-evident seal integrity indicators</p>
          </div>

          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="space-y-2">
              <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">Platform Threat Level</div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
                <span className="text-2xl font-black text-slate-900">DEFENSE LEVEL 1</span>
              </div>
              <p className="text-xs text-slate-500">Zero active security breaches or denial-of-service alerts across API clusters.</p>
            </div>

            <div className="space-y-2">
              <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">Cryptographic Seals Active</div>
              <div className="text-2xl font-black text-emerald-600">48,910 Intact</div>
              <p className="text-xs text-slate-500">100% of cylinders currently in customer homes or fleet transit maintain verified digital seal hashes.</p>
            </div>

            <div className="space-y-2">
              <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">Certificate Revocation Status</div>
              <div className="text-2xl font-black text-slate-900">0 Revoked Today</div>
              <p className="text-xs text-slate-500">Online Certificate Status Protocol (OCSP) stapling active with sub-second token revocation.</p>
            </div>
          </div>
        </div>

        {/* ── Section 4: Access Activity ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">4. Access Activity</h2>
            <p className="text-xs text-slate-500">Immutable chronological audit ledger detailing user sessions and handshakes</p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm divide-y divide-slate-100 overflow-hidden">
            {ACCESS_LOGS.map((log) => (
              <div key={log.timestamp + log.action} className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                <div className="flex items-start gap-3">
                  <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 border border-emerald-200 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <FileText size={14} />
                  </div>
                  <div>
                    <div className="font-bold text-slate-900 text-sm">{log.action}</div>
                    <div className="text-slate-600 mt-0.5">
                      <span className="font-semibold text-slate-800">{log.actor}</span> &bull; {log.ip}
                    </div>
                    <div className="text-[11px] text-slate-400 mt-0.5">{log.timestamp}</div>
                  </div>
                </div>

                <span className="font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 self-start sm:self-center">
                  {log.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </CapabilityPageShell>
  );
};
