import React, { useState } from 'react';
import { 
  UserCheck, 
  FileCheck, 
  ScanFace, 
  CheckCircle2, 
  ShieldCheck, 
  Upload, 
  Building,
  Check
} from 'lucide-react';
import { CapabilityPageShell } from './CapabilityPageShell';

interface AIKYCViewProps {
  onBackToHome: () => void;
  onOpenBooking: () => void;
  onNavigateToTracking: () => void;
}

export const AIKYCView: React.FC<AIKYCViewProps> = ({
  onBackToHome,
  onOpenBooking,
  onNavigateToTracking,
}) => {
  const [selectedTab, setSelectedTab] = useState<'distributor' | 'consumer'>('distributor');

  const COMPLIANCE_ITEMS = [
    {
      id: 'cmp-1',
      title: 'Explosives Department (PESO) Statutory License',
      category: 'Regulatory',
      status: 'Valid until 2028',
      passed: true,
      code: 'PESO-WZ-90124',
    },
    {
      id: 'cmp-2',
      title: 'Depot Bulk Storage Capacity Certificate',
      category: 'Infrastructure',
      status: 'Audited & Approved',
      passed: true,
      code: 'CAP-12000-CYL',
    },
    {
      id: 'cmp-3',
      title: 'Fire & Emergency Safety Clearance (NFPA compliant)',
      category: 'Safety',
      status: 'Annual Renewal Active',
      passed: true,
      code: 'FS-2026-8819',
    },
    {
      id: 'cmp-4',
      title: 'Biometric Delivery Personnel Background Check',
      category: 'Personnel',
      status: '100% Drivers Cleared',
      passed: true,
      code: 'BIO-DRV-44/44',
    },
  ];

  return (
    <CapabilityPageShell
      title="AI KYC"
      subtitle="Intelligent Distributor Verification"
      description="Automated biometric identity verification, OCR document validation, and regulatory compliance checks preventing black-market diversion and ensuring certified distributor operations."
      badge="COMPLIANCE & KYC"
      icon={UserCheck}
      accentColor="#8b5cf6"
      onBackToHome={onBackToHome}
      onOpenBooking={onOpenBooking}
      onNavigateToTracking={onNavigateToTracking}
    >
      <div className="space-y-8">
        {/* Profile Switcher */}
        <div className="flex items-center gap-2 bg-white border border-slate-200 p-1.5 rounded-2xl w-fit">
          <button
            type="button"
            onClick={() => setSelectedTab('distributor')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2 ${
              selectedTab === 'distributor'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-600/25'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Building size={15} />
            <span>Distributor Agency KYC</span>
          </button>
          <button
            type="button"
            onClick={() => setSelectedTab('consumer')}
            className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2 ${
              selectedTab === 'consumer'
                ? 'bg-purple-600 text-white shadow-md shadow-purple-600/25'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <UserCheck size={15} />
            <span>Consumer Subsidy KYC</span>
          </button>
        </div>

        {/* ── Section 1: Document Verification ── */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">1. Document Verification</h2>
              <p className="text-xs text-slate-500">Autonomous OCR neural parsing and statutory registry cross-check</p>
            </div>
            <span className="text-xs font-bold text-purple-700 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
              OCR Engine: 99.98% Accuracy
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Document OCR</span>
                  <FileCheck size={18} className="text-purple-600" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">Commercial Registration</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Automated boundary parsing extracting trade name, GSTIN, PESO authorization number, and jurisdictional depot bounds.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Extraction Match</span>
                <span className="font-bold text-emerald-600">100% Match</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Registry Sync</span>
                  <ShieldCheck size={18} className="text-emerald-600" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">National Database Audit</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Direct cryptographic handshake against national energy distribution registry preventing duplicate agency licenses or ghost depots.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Registry Latency</span>
                <span className="font-bold text-slate-900">&lt; 320ms</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Document Upload</span>
                  <Upload size={18} className="text-blue-600" />
                </div>
                <h3 className="text-sm font-bold text-slate-900">Supported Formats</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  Direct upload of PDF, high-resolution JPEG/PNG licenses with automatic anti-tamper watermark and EXIF tamper detection.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-500">Encryption</span>
                <span className="font-bold text-slate-900">AES-256 at Rest</span>
              </div>
            </div>
          </div>
        </div>

        {/* ── Section 2: Identity Check ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">2. Identity Check</h2>
            <p className="text-xs text-slate-500">Liveness verification and biometric identity authentication</p>
          </div>

          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
                  <ScanFace size={22} />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Biometric Liveness Verification</h3>
                  <span className="text-xs text-slate-500">Sub-second anti-spoofing facial telemetry</span>
                </div>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                Proprietary neural face matching compares live video capture with identity documents. Blocks deepfakes, 2D photograph spoofing, and synthetic masks with 99.98% verified confidence.
              </p>
              <div className="grid grid-cols-2 gap-3 pt-2">
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div className="text-xs text-slate-500">Match Confidence</div>
                  <div className="text-lg font-black text-slate-900 mt-0.5">99.85%</div>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div className="text-xs text-slate-500">Liveness Score</div>
                  <div className="text-lg font-black text-emerald-600 mt-0.5">Passed &bull; 0.99</div>
                </div>
              </div>
            </div>

            {/* Visual Simulated ID Card Preview */}
            <div className="bg-gradient-to-br from-slate-900 to-slate-950 text-white rounded-2xl p-6 border border-slate-800 shadow-xl relative overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 bg-purple-600/10 rounded-full blur-2xl" />
              <div className="flex items-center justify-between mb-4">
                <span className="text-[11px] font-bold text-purple-400 tracking-widest uppercase">
                  VERIFIED AGENCY PROFILE
                </span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-[10px] font-bold border border-emerald-500/30">
                  ACTIVE
                </span>
              </div>
              <div className="flex items-center gap-4 mb-4">
                <div className="w-14 h-14 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-400 font-black text-xl">
                  AV
                </div>
                <div>
                  <div className="text-base font-bold text-white">Adarsh Gas Agency</div>
                  <div className="text-xs text-slate-400 font-mono">Hub ID: LPG-HUB-0491</div>
                  <div className="text-xs text-slate-400">Authorized BPCL Regional Distributor</div>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2 text-xs border-t border-slate-800/80 pt-3 text-slate-400">
                <div>
                  <span className="text-[10px] uppercase block text-slate-500">Operating Ward</span>
                  <span className="text-slate-200 font-semibold">Suburban West (Zone 4)</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase block text-slate-500">Verification Hash</span>
                  <span className="text-slate-200 font-mono text-[11px]">0x9f8c...b412</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ── Section 3: Verification Status Pipeline ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">3. Verification Status</h2>
            <p className="text-xs text-slate-500">Real-time lifecycle of credentials submitted through the onboarding pipeline</p>
          </div>

          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 relative">
              {[
                { step: '01', title: 'Submitted', desc: 'Documents uploaded', done: true },
                { step: '02', title: 'OCR Extraction', desc: 'Text & license parsed', done: true },
                { step: '03', title: 'Biometric Check', desc: 'Liveness validated', done: true },
                { step: '04', title: 'Issued', desc: 'Digital Certificate ready', done: true },
              ].map((st) => (
                <div key={st.step} className="flex flex-col items-start p-4 rounded-xl bg-slate-50 border border-slate-200/80">
                  <div className="flex items-center justify-between w-full mb-2">
                    <span className="text-xs font-black text-purple-600">{st.step}</span>
                    <CheckCircle2 size={16} className="text-emerald-500" />
                  </div>
                  <div className="text-sm font-bold text-slate-900">{st.title}</div>
                  <div className="text-xs text-slate-500 mt-0.5">{st.desc}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── Section 4: Compliance Checklist ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">4. Compliance Checklist</h2>
            <p className="text-xs text-slate-500">Statutory certifications required before cylinder allocation is authorized</p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm divide-y divide-slate-100 overflow-hidden">
            {COMPLIANCE_ITEMS.map((item) => (
              <div key={item.id} className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-7 h-7 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-200 flex items-center justify-center flex-shrink-0">
                    <Check size={14} strokeWidth={3} />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-slate-900">{item.title}</div>
                    <div className="flex items-center gap-2 text-xs text-slate-500 mt-0.5">
                      <span className="font-semibold text-purple-700">{item.category}</span>
                      <span>&bull;</span>
                      <span className="font-mono">{item.code}</span>
                    </div>
                  </div>
                </div>
                <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 self-start sm:self-center">
                  {item.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </CapabilityPageShell>
  );
};
