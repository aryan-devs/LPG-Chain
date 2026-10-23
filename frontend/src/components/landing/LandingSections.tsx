import React from 'react';
import {
  Flame,
  ShieldCheck,
  Truck,
  CheckCircle2,
  ArrowRight,
  Navigation,
  Building2,
  Lock,
  Calendar,
  Sparkles,
  Scale,
  Activity,
  Layers,
  ChevronRight,
} from 'lucide-react';
import { LiveTrackingPreview } from './LiveTrackingPreview';

import { UserProfile } from '../auth/LoginModal';

interface LandingSectionsProps {
  onNavigateToTracking: (deliveryId?: string) => void;
  onOpenBooking: () => void;
  currentUser?: UserProfile | null;
  onOpenLogin?: () => void;
  onNavigateToCapability?: (capability: 'sentinel' | 'kyc' | 'analytics' | 'security' | 'distributor') => void;
}

export const LandingSections: React.FC<LandingSectionsProps> = ({
  onNavigateToTracking,
  onOpenBooking,
  currentUser,
  onOpenLogin,
  onNavigateToCapability,
}) => {

  // How It Works Steps
  const WORKFLOW_STEPS = [
    {
      step: '01',
      title: 'Smart Booking',
      subtitle: 'Cylinder Booking',
      description: 'Customer selects cylinder quantity and delivery address.',
      icon: Calendar,
      color: 'rose',
      accentBg: 'bg-rose-50',
      accentBorder: 'border-rose-200',
      accentText: 'text-rose-600',
    },
    {
      step: '02',
      title: 'Distributor Assignment',
      subtitle: 'Depot Allocation',
      description: 'Booking is assigned to an authorized distributor.',
      icon: Building2,
      color: 'amber',
      accentBg: 'bg-amber-50',
      accentBorder: 'border-amber-200',
      accentText: 'text-amber-600',
    },
    {
      step: '03',
      title: 'Delivery Assignment',
      subtitle: 'Fleet Dispatch',
      description: 'Driver and vehicle are assigned to the delivery.',
      icon: Truck,
      color: 'blue',
      accentBg: 'bg-blue-50',
      accentBorder: 'border-blue-200',
      accentText: 'text-blue-600',
    },
    {
      step: '04',
      title: 'Live Tracking',
      subtitle: 'Real-Time Telemetry',
      description: 'Customer can track an active delivery in real time.',
      icon: Navigation,
      color: 'purple',
      accentBg: 'bg-purple-50',
      accentBorder: 'border-purple-200',
      accentText: 'text-purple-600',
    },
    {
      step: '05',
      title: 'Delivery Completed',
      subtitle: 'Order Fulfilled',
      description: 'Delivery status is updated after successful completion.',
      icon: CheckCircle2,
      color: 'emerald',
      accentBg: 'bg-emerald-50',
      accentBorder: 'border-emerald-200',
      accentText: 'text-emerald-600',
    },
  ];

  // Core Platform Services Data (6 Practical Services - 3x2 Premium Layout)
  const CORE_SERVICES = [
    {
      step: '01',
      id: 'smart-booking',
      title: 'Smart LPG Booking',
      subtitle: 'Simple cylinder booking',
      features: [
        'Select cylinder quantity',
        'Add delivery address',
        'Track booking status',
      ],
      icon: Calendar,
      badgeBg: 'bg-rose-50',
      badgeText: 'text-rose-700',
      badgeBorder: 'border-rose-200',
      iconBg: 'bg-rose-50',
      iconText: 'text-rose-600',
      iconBorder: 'border-rose-100',
      accentGradient: 'from-rose-500 to-rose-600',
      arrowColor: 'text-rose-600 group-hover/btn:text-rose-700',
      checkColor: 'text-rose-500',
      action: onOpenBooking,
    },
    {
      step: '02',
      id: 'distributor-management',
      title: 'Distributor Management',
      subtitle: 'Connected distributor operations',
      features: [
        'Manage distributor organizations',
        'Manage memberships and roles',
        'Control organization access',
      ],
      icon: Building2,
      badgeBg: 'bg-amber-50',
      badgeText: 'text-amber-700',
      badgeBorder: 'border-amber-200',
      iconBg: 'bg-amber-50',
      iconText: 'text-amber-600',
      iconBorder: 'border-amber-100',
      accentGradient: 'from-amber-500 to-amber-600',
      arrowColor: 'text-amber-600 group-hover/btn:text-amber-700',
      checkColor: 'text-amber-500',
      action: onNavigateToCapability ? () => onNavigateToCapability('distributor') : undefined,
    },
    {
      step: '03',
      id: 'delivery-management',
      title: 'Delivery Management',
      subtitle: 'Complete delivery coordination',
      features: [
        'Assign drivers',
        'Assign vehicles',
        'Manage delivery status',
      ],
      icon: Truck,
      badgeBg: 'bg-blue-50',
      badgeText: 'text-blue-700',
      badgeBorder: 'border-blue-200',
      iconBg: 'bg-blue-50',
      iconText: 'text-blue-600',
      iconBorder: 'border-blue-100',
      accentGradient: 'from-blue-500 to-blue-600',
      arrowColor: 'text-blue-600 group-hover/btn:text-blue-700',
      checkColor: 'text-blue-500',
      action: () => onNavigateToTracking('1'),
    },
    {
      step: '04',
      id: 'real-time-tracking',
      title: 'Real-Time Tracking',
      subtitle: 'Live delivery visibility',
      features: [
        'Live vehicle location',
        'Real-time delivery updates',
        'Interactive tracking map',
      ],
      icon: Navigation,
      badgeBg: 'bg-purple-50',
      badgeText: 'text-purple-700',
      badgeBorder: 'border-purple-200',
      iconBg: 'bg-purple-50',
      iconText: 'text-purple-600',
      iconBorder: 'border-purple-100',
      accentGradient: 'from-purple-500 to-purple-600',
      arrowColor: 'text-purple-600 group-hover/btn:text-purple-700',
      checkColor: 'text-purple-500',
      action: () => onNavigateToTracking('1'),
    },
    {
      step: '05',
      id: 'secure-access',
      title: 'Secure Access',
      subtitle: 'Protected platform access',
      features: [
        'JWT authentication',
        'Role-based access',
        'Organization-level authorization',
      ],
      icon: Lock,
      badgeBg: 'bg-emerald-50',
      badgeText: 'text-emerald-700',
      badgeBorder: 'border-emerald-200',
      iconBg: 'bg-emerald-50',
      iconText: 'text-emerald-600',
      iconBorder: 'border-emerald-100',
      accentGradient: 'from-emerald-500 to-emerald-600',
      arrowColor: 'text-emerald-600 group-hover/btn:text-emerald-700',
      checkColor: 'text-emerald-500',
      action: onNavigateToCapability ? () => onNavigateToCapability('security') : undefined,
    },
    {
      step: '06',
      id: 'transparent-operations',
      title: 'Transparent Operations',
      subtitle: 'Complete delivery lifecycle',
      features: [
        'Booking status tracking',
        'Delivery status tracking',
        'Clear operational history',
      ],
      icon: Activity,
      badgeBg: 'bg-slate-100',
      badgeText: 'text-slate-800',
      badgeBorder: 'border-slate-300',
      iconBg: 'bg-slate-100',
      iconText: 'text-slate-800',
      iconBorder: 'border-slate-200',
      accentGradient: 'from-slate-700 to-slate-900',
      arrowColor: 'text-slate-800 group-hover/btn:text-slate-950',
      checkColor: 'text-slate-700',
      action: onNavigateToCapability ? () => onNavigateToCapability('analytics') : undefined,
    },
  ];

  // Why LPG-Chain Pillars
  const WHY_PILLARS = [
    {
      title: 'Convenience',
      headline: 'Refills on Autopilot',
      description: 'Say goodbye to booking phone calls, long agency queues, or surprise empty cylinders midway through dinner preparation.',
      points: [
        'AI predictive refill reminders before cylinder runs empty',
        'Guaranteed delivery time windows fitting your schedule',
        'Seamless 1-click digital booking & instant receipt generation',
      ],
      icon: Sparkles,
      color: 'rose',
    },
    {
      title: 'Transparency',
      headline: 'Certified Weight & Live GPS',
      description: 'Complete visibility into every gram of gas and every meter of your delivery truck journey.',
      points: [
        'Guaranteed 14.2kg net tare weight certified on digital scales',
        'Sub-second live map tracking with accurate minute-by-minute ETA',
        'Itemized digital receipts with verified tare weight certificates',
      ],
      icon: Scale,
      color: 'blue',
    },
    {
      title: 'Safety',
      headline: 'Zero Compromise Protection',
      description: 'Engineered with statutory PESO pressure vessel compliance, cryptographic seals, and 24/7 emergency telemetry.',
      points: [
        'Cryptographic tamper-proof seal prevents gas pilferage',
        'Mandatory 5-year hydrostatic pressure vessel re-testing',
        'Trained delivery personnel performing live doorstep safety checks',
      ],
      icon: ShieldCheck,
      color: 'emerald',
    },
    {
      title: 'Connected Distribution',
      headline: 'A Modern Unified Grid',
      description: 'Bridging central oil marketing depots, regional bottling hubs, authorized distributors, and doorstep households.',
      points: [
        'Elimination of black-market hoarding through digital custody logs',
        '1,200+ certified regional distributor hubs nationwide',
        'Optimized route dispatch reducing delivery vehicle emissions by 28%',
      ],
      icon: Layers,
      color: 'amber',
    },
  ];

  return (
    <div className="w-full bg-white text-slate-900">

      {/* ══════════════════════════════════════════════════════════════
          SECTION 2 — HOW LPG-CHAIN WORKS (5-Step Visual Flow)
          Book → Distributor → Delivery → Live Tracking → Delivered
      ══════════════════════════════════════════════════════════════ */}
      <section id="how-it-works" className="py-20 sm:py-28 bg-slate-50/70 border-b border-slate-200/80 scroll-mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          {/* Section Header */}
          <div className="text-center max-w-3xl mx-auto mb-16">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-rose-50 border border-rose-200 text-rose-700 text-xs font-bold uppercase tracking-wider mb-3">
              <Activity size={14} className="text-rose-600" />
              <span>From Booking to Delivery</span>
            </div>
            <h2 className="text-3xl sm:text-4xl lg:text-[2.75rem] font-black text-slate-950 tracking-tight leading-tight">
              How LPG-Chain Works
            </h2>
            <p className="text-base sm:text-lg text-slate-600 mt-3 leading-relaxed">
              From booking to delivery — simple, transparent, and trackable.
            </p>
          </div>

          {/* Stepper Flow Cards */}
          <div className="relative">
            {/* Desktop Connecting Line */}
            <div className="hidden lg:block absolute top-1/2 left-8 right-8 h-1 bg-gradient-to-r from-rose-200 via-blue-200 to-emerald-200 -translate-y-8 z-0" />

            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 relative z-10">
              {WORKFLOW_STEPS.map((step, idx) => {
                const IconComponent = step.icon;
                return (
                  <div
                    key={step.step}
                    className="group bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm hover:shadow-xl hover:-translate-y-1.5 transition-all duration-300 flex flex-col justify-between"
                  >
                    <div>
                      {/* Step Header */}
                      <div className="flex items-center justify-between mb-5">
                        <div className={`w-12 h-12 rounded-2xl ${step.accentBg} ${step.accentBorder} border flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform`}>
                          <IconComponent size={22} className={step.accentText} />
                        </div>
                        <span className="text-xs font-black font-mono tracking-widest text-slate-400 bg-slate-100 px-2.5 py-1 rounded-full">
                          {step.step}
                        </span>
                      </div>

                      {/* Title & Subtitle */}
                      <h3 className="text-lg font-black text-slate-900 group-hover:text-rose-600 transition-colors leading-snug">
                        {step.title}
                      </h3>
                      <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mt-0.5 mb-2.5">
                        {step.subtitle}
                      </div>

                      {/* Description */}
                      <p className="text-xs sm:text-[13px] text-slate-600 leading-relaxed">
                        {step.description}
                      </p>
                    </div>

                    {/* Step Flow Arrow Indicator (Mobile/Tablet) */}
                    {idx < WORKFLOW_STEPS.length - 1 && (
                      <div className="lg:hidden flex justify-center pt-4 text-slate-300">
                        <ChevronRight size={18} className="rotate-90 md:rotate-0" />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Bottom Quick Flow Summary Bar */}
          <div className="mt-12 p-4 sm:p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                ✓
              </div>
              <div>
                <span className="text-sm font-bold text-slate-900 block leading-tight">
                  Reliable LPG Distribution
                </span>
                <span className="text-xs text-slate-500">
                  Streamlined order processing, verified assignments, and real-time transit tracking.
                </span>
              </div>
            </div>
            <button
              type="button"
              onClick={onOpenBooking}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md transition-all ml-auto sm:ml-0"
            >
              <span>Book Cylinder Now</span>
              <ArrowRight size={14} />
            </button>
          </div>

        </div>
      </section>

      {/* ══════════════════════════════════════════════════════════════
          SECTION 3 — CORE PLATFORM SERVICES (3x2 Premium Layout)
          One Platform. Every LPG Operation.
      ══════════════════════════════════════════════════════════════ */}
      <section id="features" className="py-20 sm:py-28 bg-slate-50/50 border-b border-slate-200/80 scroll-mt-16 relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          
          {/* Section Header */}
          <div className="text-center max-w-3xl mx-auto mb-16">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-rose-50 border border-rose-200 text-rose-700 text-xs font-bold uppercase tracking-wider mb-3">
              <Layers size={14} className="text-rose-600" />
              <span>Core Platform Services</span>
            </div>
            <h2 className="text-3xl sm:text-4xl lg:text-[2.75rem] font-black text-slate-950 tracking-tight leading-tight">
              One Platform. Every LPG Operation.
            </h2>
            <p className="text-base sm:text-lg text-slate-600 mt-3 leading-relaxed">
              From booking and distributor management to delivery, tracking and secure access, LPG-Chain connects the complete LPG delivery workflow.
            </p>
          </div>

          {/* 3 x 2 Core Services Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
            {CORE_SERVICES.map((service) => {
              const IconComponent = service.icon;
              return (
                <div
                  key={service.id}
                  className="bg-white rounded-3xl p-7 sm:p-8 border border-slate-200/90 hover:border-slate-300 shadow-sm hover:shadow-xl hover:-translate-y-1.5 transition-all duration-300 flex flex-col justify-between group relative overflow-hidden"
                >
                  {/* Subtle Top Accent Line */}
                  <div className={`absolute top-0 left-8 right-8 h-1 rounded-b-full bg-gradient-to-r ${service.accentGradient} opacity-90`} />

                  <div>
                    {/* Header: Icon & Number Badge */}
                    <div className="flex items-center justify-between mb-6">
                      <div className={`w-12 h-12 rounded-2xl ${service.iconBg} ${service.iconBorder} border flex items-center justify-center ${service.iconText} shadow-inner group-hover:scale-105 transition-transform`}>
                        <IconComponent size={22} />
                      </div>
                      <span className={`text-xs font-black font-mono tracking-widest px-2.5 py-1 rounded-full border ${service.badgeBg} ${service.badgeText} ${service.badgeBorder}`}>
                        {service.step}
                      </span>
                    </div>

                    {/* Title & Subtitle */}
                    <h3 className="text-xl font-black text-slate-900 group-hover:text-slate-950 transition-colors leading-snug">
                      {service.title}
                    </h3>
                    <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mt-1 mb-4">
                      {service.subtitle}
                    </div>

                    {/* Feature Points */}
                    <div className="space-y-2.5 pt-4 border-t border-slate-100">
                      {service.features.map((feat, i) => (
                        <div key={i} className="flex items-start gap-2.5 text-xs sm:text-[13px] text-slate-600 leading-normal">
                          <CheckCircle2 size={15} className={`${service.checkColor} flex-shrink-0 mt-0.5`} />
                          <span>{feat}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Explore Action Link */}
                  {service.action ? (
                    <div className="mt-7 pt-4 border-t border-slate-100 flex items-center justify-between">
                      <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        Production Ready
                      </span>
                      <button
                        type="button"
                        onClick={service.action}
                        className={`inline-flex items-center gap-1.5 text-xs font-bold ${service.arrowColor} transition-colors group/btn cursor-pointer`}
                      >
                        <span>Explore</span>
                        <ArrowRight size={13} className="group-hover/btn:translate-x-1 transition-transform" />
                      </button>
                    </div>
                  ) : (
                    <div className="mt-7 pt-4 border-t border-slate-100 flex items-center justify-between">
                      <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        Production Ready
                      </span>
                      <span className="text-xs font-semibold text-slate-400">Integrated</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Connected Operations Bottom Strip */}
          <div className="mt-14 pt-8 border-t border-slate-200/80 flex flex-wrap items-center justify-between gap-4 text-xs">
            <div className="flex items-center gap-2.5">
              <span className="w-2 h-2 rounded-full bg-rose-600 animate-pulse" />
              <span className="font-extrabold uppercase tracking-widest text-slate-500">
                Connected Operations
              </span>
            </div>
            <div className="text-slate-600 font-semibold tracking-wide flex items-center gap-2.5 flex-wrap">
              <span>Booking</span>
              <span className="text-slate-300">&bull;</span>
              <span>Distribution</span>
              <span className="text-slate-300">&bull;</span>
              <span>Delivery</span>
              <span className="text-slate-300">&bull;</span>
              <span>Tracking</span>
              <span className="text-slate-300">&bull;</span>
              <span>Security</span>
            </div>
          </div>

        </div>
      </section>

      {/* ══════════════════════════════════════════════════════════════
          SECTION 4 — LIVE TRACKING PREVIEW
          Know Where Your Cylinder Is.
      ══════════════════════════════════════════════════════════════ */}
      <LiveTrackingPreview
        onNavigateToTracking={onNavigateToTracking}
        onOpenBooking={onOpenBooking}
        currentUser={currentUser}
        onOpenLogin={onOpenLogin}
      />

      {/* ══════════════════════════════════════════════════════════════
          SECTION 5 — WHY LPG-CHAIN
          Convenience • Transparency • Safety • Connected Distribution
      ══════════════════════════════════════════════════════════════ */}
      <section id="why-lpg-chain" className="py-20 sm:py-28 bg-white border-b border-slate-200/80 scroll-mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          {/* Section Header */}
          <div className="text-center max-w-3xl mx-auto mb-16">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-bold uppercase tracking-wider mb-3">
              <ShieldCheck size={14} className="text-emerald-600" />
              <span>The LPG-Chain Standard</span>
            </div>
            <h2 className="text-3xl sm:text-4xl lg:text-[2.75rem] font-black text-slate-950 tracking-tight leading-tight">
              Why Choose LPG-Chain?
            </h2>
            <p className="text-base sm:text-lg text-slate-600 mt-3 leading-relaxed">
              Replacing opaque distribution practices with guaranteed safety, honest weight, and digital ease.
            </p>
          </div>

          {/* 4 Strategic Pillars */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {WHY_PILLARS.map((pillar) => {
              const IconComponent = pillar.icon;
              return (
                <div
                  key={pillar.title}
                  className="bg-slate-50/80 rounded-3xl p-7 border border-slate-200 hover:border-slate-300 hover:shadow-xl hover:bg-white transition-all duration-300 flex flex-col justify-between"
                >
                  <div>
                    <div className="w-12 h-12 rounded-2xl bg-white border border-slate-200 text-slate-900 flex items-center justify-center mb-5 shadow-sm">
                      <IconComponent size={22} className="text-rose-600" />
                    </div>

                    <div className="text-xs font-bold uppercase tracking-wider text-rose-600 mb-1">
                      {pillar.title}
                    </div>
                    <h3 className="text-xl font-black text-slate-900 mb-2.5">
                      {pillar.headline}
                    </h3>
                    <p className="text-xs sm:text-[13px] text-slate-600 leading-relaxed mb-5">
                      {pillar.description}
                    </p>

                    {/* Feature points */}
                    <div className="space-y-2 pt-3 border-t border-slate-200/80">
                      {pillar.points.map((pt, i) => (
                        <div key={i} className="flex items-start gap-2 text-xs text-slate-700">
                          <span className="text-rose-500 font-bold mt-0.5">•</span>
                          <span>{pt}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Statutory Safety & Compliance Bar */}
          <div className="mt-16 rounded-3xl bg-slate-950 text-white p-8 sm:p-10 border border-slate-800 flex flex-col lg:flex-row items-center justify-between gap-8">
            <div className="max-w-2xl space-y-2 text-center lg:text-left">
              <div className="flex items-center justify-center lg:justify-start gap-2 text-xs font-bold uppercase tracking-wider text-rose-400">
                <Lock size={14} />
                <span>Statutory Compliance &amp; PESO Standards</span>
              </div>
              <h3 className="text-2xl font-black tracking-tight text-white">
                Certified Steel Pressure Vessels &amp; Dual Safety Valves
              </h3>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                All cylinders in our ecosystem strictly conform to IS 3196 hydrostatic pressure burst testing,
                20-gram automated weighbridge tolerances, and tamper-proof SHA-256 seal signatures.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={onOpenBooking}
                className="px-6 py-3.5 bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm rounded-2xl shadow-lg transition-all flex items-center gap-2"
              >
                <Flame size={18} />
                <span>Order Safe Refill</span>
              </button>
            </div>
          </div>

        </div>
      </section>

      {/* ══════════════════════════════════════════════════════════════
          SECTION 6 — FINAL CTA
          "Your LPG. Your Delivery. Connected."
      ══════════════════════════════════════════════════════════════ */}
      <section className="py-20 sm:py-28 bg-gradient-to-br from-rose-700 via-rose-600 to-red-700 text-white relative overflow-hidden">
        
        {/* Subtle Decorative Background Rings */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] rounded-full border border-white/10 pointer-events-none" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full border border-white/15 pointer-events-none" />

        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8 relative z-10">
          
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/15 backdrop-blur-md border border-white/20 text-white text-xs font-extrabold tracking-widest uppercase shadow-sm">
            <Flame size={15} className="text-amber-300" />
            <span>THE FUTURE OF SAFE ENERGY</span>
          </div>

          <h2 className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-[1.08] text-white">
            Your LPG. Your Delivery.<br />
            <span className="text-amber-300">Connected.</span>
          </h2>

          <p className="text-base sm:text-lg text-rose-100 max-w-2xl mx-auto leading-relaxed">
            Join thousands of households, distributors, and logistics fleets experiencing
            honest weight, sub-second tracking, and transparent energy supply every day.
          </p>

          {/* Action CTAs */}
          <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
            <button
              type="button"
              onClick={onOpenBooking}
              className="px-8 py-4 bg-white hover:bg-slate-50 text-rose-700 font-black text-sm sm:text-base rounded-2xl shadow-2xl hover:scale-105 active:scale-95 transition-all flex items-center gap-2.5 group"
            >
              <Flame size={20} className="text-rose-600 group-hover:rotate-12 transition-transform" />
              <span>Book LPG Cylinder</span>
              <ArrowRight size={17} className="group-hover:translate-x-1 transition-transform" />
            </button>

            <button
              type="button"
              onClick={() => onNavigateToTracking('1')}
              className="px-8 py-4 bg-rose-800/80 hover:bg-rose-900 text-white font-black text-sm sm:text-base rounded-2xl border border-rose-400/50 hover:border-white/60 shadow-xl active:scale-95 transition-all flex items-center gap-2.5"
            >
              <Navigation size={18} />
              <span>Track Active Delivery</span>
            </button>
          </div>

          {/* Assurance Trust Strip */}
          <div className="pt-8 border-t border-white/20 flex flex-wrap items-center justify-center gap-6 sm:gap-10 text-xs font-semibold text-rose-200">
            <div className="flex items-center gap-2">
              <CheckCircle2 size={16} className="text-amber-300" />
              <span>PESO Statutory Certified</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 size={16} className="text-amber-300" />
              <span>99.98% On-Time SLA</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 size={16} className="text-amber-300" />
              <span>Cryptographic QR Seals</span>
            </div>
          </div>

        </div>
      </section>

    </div>
  );
};
