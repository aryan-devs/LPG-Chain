import React, { useState, useEffect, useRef } from 'react';
import {
  ArrowRight,
  ShieldCheck,
  Truck,
  Calendar,
  MapPin,
  Clock,
  ChevronLeft,
  ChevronRight,
  Shield,
  UserCheck,
  LineChart,
  Lock,
  Network,
  Zap,
  X,
  LucideIcon,
} from 'lucide-react';

interface LPGChainHeroProps {
  onNavigateToTracking: (deliveryId?: string) => void;
  onOpenBookingTab?: () => void;
  onNavigateToCapability?: (capabilityId: 'sentinel' | 'kyc' | 'analytics' | 'security' | 'distributor') => void;
}

// Scenario environments displayed in the photographic backdrop & bottom dock
const SCENARIOS = [
  {
    id: 'home-delivery',
    number: '01',
    name: 'Home Delivery',
    image: '/assets/lpg_hero_env.jpg',
    tag: 'Last-Mile',
    caption: 'Doorstep inspection & safety verification',
  },
  {
    id: 'residential-area',
    number: '02',
    name: 'Residential Fleet',
    image: '/assets/lpg_residential_truck.jpg',
    tag: 'Distribution',
    caption: 'Dynamic routing across suburban wards',
  },
  {
    id: 'distributor-yard',
    number: '03',
    name: 'Distributor Yard',
    image: '/assets/lpg_distributor_yard.jpg',
    tag: 'Hub Operations',
    caption: 'Automated depot loading & stock reconciliation',
  },
  {
    id: 'city-view',
    number: '04',
    name: 'Metropolitan Grid',
    image: '/assets/lpg_city_delivery.jpg',
    tag: 'Transit Matrix',
    caption: 'High-capacity arterial expressway logistics',
  },
];

const SCENARIO_DURATION = 8000;

// The 7 Core Systems that emerge from the LPG Cylinder Core
interface CoreSystem {
  id: string;
  name: string;
  tag: string;
  description: string;
  icon: LucideIcon;
  accentColor: string;
  badgeBg: string;
  badgeText: string;
  badgeBorder: string;
  dx: number;
  dy: number;
  actionType: 'booking' | 'tracking' | 'sentinel' | 'kyc' | 'analytics' | 'security' | 'distributor';
}

const CORE_SYSTEMS: CoreSystem[] = [
  // 1. AI Sentinel (Top Center Apex)
  {
    id: 'sentinel',
    name: 'AI Sentinel',
    tag: 'SAFETY',
    description: 'Autonomous leak & safety grid',
    icon: Shield,
    accentColor: '#f59e0b',
    badgeBg: 'bg-amber-50',
    badgeText: 'text-amber-700',
    badgeBorder: 'border-amber-200',
    dx: 0,
    dy: -258,
    actionType: 'sentinel',
  },
  // 2. AI KYC (Upper Left)
  {
    id: 'kyc',
    name: 'AI KYC',
    tag: 'VERIFIED',
    description: 'Biometric consumer verification',
    icon: UserCheck,
    accentColor: '#8b5cf6',
    badgeBg: 'bg-purple-50',
    badgeText: 'text-purple-700',
    badgeBorder: 'border-purple-200',
    dx: -205,
    dy: -155,
    actionType: 'kyc',
  },
  // 3. Analytics (Upper Right)
  {
    id: 'analytics',
    name: 'Analytics',
    tag: 'FORECAST',
    description: 'Dynamic demand & consumption metrics',
    icon: LineChart,
    accentColor: '#06b6d4',
    badgeBg: 'bg-cyan-50',
    badgeText: 'text-cyan-700',
    badgeBorder: 'border-cyan-200',
    dx: 205,
    dy: -155,
    actionType: 'analytics',
  },
  // 4. Security (Mid Left)
  {
    id: 'security',
    name: 'Security',
    tag: 'SEAL',
    description: 'Tamper-proof cryptographic seals',
    icon: Lock,
    accentColor: '#059669',
    badgeBg: 'bg-emerald-50',
    badgeText: 'text-emerald-700',
    badgeBorder: 'border-emerald-200',
    dx: -235,
    dy: -5,
    actionType: 'security',
  },
  // 5. Live Tracking (Mid Right)
  {
    id: 'tracking',
    name: 'Live Tracking',
    tag: 'IoT GPS',
    description: 'Sub-meter IoT GPS telemetry',
    icon: Truck,
    accentColor: '#10b981',
    badgeBg: 'bg-emerald-50',
    badgeText: 'text-emerald-700',
    badgeBorder: 'border-emerald-200',
    dx: 235,
    dy: -5,
    actionType: 'tracking',
  },
  // 6. Smart Booking (Lower Left)
  {
    id: 'booking',
    name: 'Smart Booking',
    tag: '1-TAP',
    description: '1-Tap instant refill dispatch',
    icon: Calendar,
    accentColor: '#e11d48',
    badgeBg: 'bg-rose-50',
    badgeText: 'text-rose-700',
    badgeBorder: 'border-rose-200',
    dx: -205,
    dy: 145,
    actionType: 'booking',
  },
  // 7. Distributor Network (Lower Right)
  {
    id: 'network',
    name: 'Distributor Network',
    tag: '1.2K HUBS',
    description: 'Certified regional depot hubs',
    icon: Network,
    accentColor: '#3b82f6',
    badgeBg: 'bg-blue-50',
    badgeText: 'text-blue-700',
    badgeBorder: 'border-blue-200',
    dx: 205,
    dy: 145,
    actionType: 'distributor',
  },
];

export const LPGChainHero: React.FC<LPGChainHeroProps> = ({
  onNavigateToTracking,
  onOpenBookingTab,
  onNavigateToCapability,
}) => {
  const sectionRef = useRef<HTMLElement>(null);
  const [scrollProgress, setScrollProgress] = useState(0);

  const [isBookingOpen, setIsBookingOpen] = useState(false);
  const [activeScenarioIndex, setActiveScenarioIndex] = useState(0);
  const [isScenarioPaused, setIsScenarioPaused] = useState(false);
  const [progressKey, setProgressKey] = useState(0);

  // Manual click activation or scroll activation
  const [isCoreActiveManual, setIsCoreActiveManual] = useState(false);
  const [shockwaveKey, setShockwaveKey] = useState(0);
  const [activeNodeToast, setActiveNodeToast] = useState<string | null>(null);

  // Subtle hover reaction on cylinder (does not permanently show cards)
  const [cylinderHovered, setCylinderHovered] = useState(false);
  const cylinderRef = useRef<HTMLDivElement>(null);

  // Lazy-load BookingModal
  const [BookingModal, setBookingModal] = useState<React.ComponentType<{
    isOpen: boolean;
    onClose: () => void;
    onTrackOrder: (id: string) => void;
  }> | null>(null);

  useEffect(() => {
    import('./BookingModal').then((m) => setBookingModal(() => m.BookingModal));
  }, []);

  // ══════════════════════════════════════════════════════════
  // SCROLL-BASED STORYTELLING LISTENER (Non-blocking, passive)
  // ══════════════════════════════════════════════════════════
  useEffect(() => {
    const handleScroll = () => {
      if (!sectionRef.current) return;
      const rect = sectionRef.current.getBoundingClientRect();
      const totalScrollable = sectionRef.current.offsetHeight - window.innerHeight;
      if (totalScrollable <= 0) return;

      const scrolled = -rect.top;
      const progress = Math.min(Math.max(scrolled / totalScrollable, 0), 1);
      setScrollProgress(progress);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Automatic scenario cycling
  useEffect(() => {
    if (isScenarioPaused) return;
    const interval = setInterval(() => {
      setActiveScenarioIndex((prev) => (prev + 1) % SCENARIOS.length);
      setProgressKey((k) => k + 1);
    }, SCENARIO_DURATION);
    return () => clearInterval(interval);
  }, [isScenarioPaused, activeScenarioIndex]);

  // Cylinder click toggle for Core activation & expansion
  const handleCylinderClick = () => {
    setIsCoreActiveManual((prev) => {
      const next = !prev;
      if (next) {
        setShockwaveKey((k) => k + 1);
        setActiveNodeToast('LPG-Chain Core Active: 7 Systems Synchronized');
        setTimeout(() => setActiveNodeToast(null), 3500);
      }
      return next;
    });
  };

  const handleSelectScenario = (index: number) => {
    setActiveScenarioIndex(index);
    setProgressKey((k) => k + 1);
  };

  const handleNodeClick = (e: React.MouseEvent, node: CoreSystem) => {
    e.stopPropagation();
    if (node.actionType === 'booking') {
      if (onOpenBookingTab) {
        onOpenBookingTab();
      } else {
        setIsBookingOpen(true);
      }
    } else if (node.actionType === 'tracking') {
      onNavigateToTracking('1');
    } else if (
      node.actionType === 'sentinel' ||
      node.actionType === 'kyc' ||
      node.actionType === 'analytics' ||
      node.actionType === 'security' ||
      node.actionType === 'distributor'
    ) {
      if (onNavigateToCapability) {
        onNavigateToCapability(node.actionType);
      } else {
        setActiveNodeToast(`${node.name}: ${node.description}`);
        setTimeout(() => setActiveNodeToast(null), 3000);
      }
    } else {
      setActiveNodeToast(`${node.name}: ${node.description}`);
      setTimeout(() => setActiveNodeToast(null), 3000);
    }
  };

  // ──────────────────────────────────────────────────────────
  // SCROLL STORYTELLING METRICS
  // Progress stages:
  // 0.00 - 0.15 : Intro / Dormant
  // 0.15 - 0.35 : Cylinder activates & Core unlocks
  // 0.35 - 0.60 : Connections unfurl & shoot outward
  // 0.60 - 0.85 : 7 Modules emerge into orbit
  // 0.85 - 1.00 : Ecosystem flourishing, seamlessly scrolls into website
  // ──────────────────────────────────────────────────────────
  const isScrollActive = scrollProgress >= 0.18;
  const isCoreActive = isCoreActiveManual || isScrollActive;

  // Active Story Stage Label
  let stageLabel = '01 • Discover LPG-Chain';
  if (scrollProgress >= 0.80) {
    stageLabel = '05 • Ecosystem Online';
  } else if (scrollProgress >= 0.55) {
    stageLabel = '04 • 7 Systems Emerged';
  } else if (scrollProgress >= 0.35) {
    stageLabel = '03 • Connections Active';
  } else if (scrollProgress >= 0.15) {
    stageLabel = '02 • Core Activated';
  }

  return (
    <section
      ref={sectionRef}
      aria-label="LPG-Chain Scroll Storytelling Hero"
      className="relative w-full select-none"
      style={{ height: '185vh', background: '#f8fafc' }}
    >
      {/* ════════════════════════════════════════════════════════
          STICKY VIEWPORT STAGE — Pinned below navbar (64px)
          Never traps the user: scrolls naturally once 185vh ends
      ════════════════════════════════════════════════════════ */}
      <div className="sticky top-16 w-full h-[calc(100vh-4rem)] overflow-hidden flex flex-col justify-between">

        {/* ── 1. PHOTOGRAPHIC BACKDROP WITH EDITORIAL DIRECTIONAL SCRIM ── */}
        <div className="absolute inset-0 pointer-events-none">
          {SCENARIOS.map((s, i) => (
            <img
              key={s.id}
              src={s.image}
              alt={s.name}
              className={`absolute inset-0 w-full h-full object-cover object-center transition-all duration-1000 ease-out ${
                i === activeScenarioIndex
                  ? 'opacity-85 scale-100'
                  : 'opacity-0 scale-105'
              }`}
            />
          ))}

          {/* Multi-Stop Directional Scrim */}
          <div
            className="absolute inset-0"
            style={{
              background:
                'linear-gradient(90deg, #ffffff 0%, rgba(255,255,255,0.98) 26%, rgba(255,255,255,0.85) 42%, rgba(255,255,255,0.35) 60%, rgba(255,255,255,0.05) 75%, transparent 88%)',
            }}
          />

          {/* Top Header Light Wash */}
          <div
            className="absolute top-0 left-0 right-0 h-24"
            style={{
              background: 'linear-gradient(180deg, rgba(255,255,255,0.8) 0%, transparent 100%)',
            }}
          />

          {/* Ambient Warm Halo behind Cylinder (expands with scroll) */}
          <div
            className="absolute hidden lg:block rounded-full pointer-events-none transition-all duration-700"
            style={{
              width: isCoreActive ? 700 : 540,
              height: isCoreActive ? 700 : 540,
              background: isCoreActive
                ? 'radial-gradient(circle at center, rgba(225,29,72,0.22) 0%, rgba(245,158,11,0.14) 40%, transparent 70%)'
                : 'radial-gradient(circle at center, rgba(244,63,94,0.14) 0%, rgba(251,146,60,0.08) 35%, transparent 70%)',
              filter: 'blur(55px)',
              top: '46%',
              right: '18%',
              transform: 'translate(50%, -50%)',
            }}
          />
        </div>

        {/* ── 2. MAIN STAGE CONTENT (Left Headline + Right Core Constellation) ── */}
        <div className="relative z-10 w-full flex-1 flex items-center px-6 sm:px-8 lg:px-12 xl:px-16 pt-4 pb-2 max-w-[1440px] mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-[1.05fr_1.2fr] gap-8 lg:gap-4 items-center w-full">

            {/* ─────────────────────────────────────────────
                LEFT COLUMN: Typography & Action Hierarchy
            ───────────────────────────────────────────── */}
            <div className="flex flex-col justify-center gap-5 sm:gap-6 py-2 lg:py-6 max-w-[580px] z-20">
              
              {/* Premium Eyebrow Pill */}
              <div className="inline-flex items-center gap-2.5 self-start px-3.5 py-1.5 rounded-full bg-rose-50/90 border border-rose-200/80 shadow-[0_2px_8px_rgba(225,29,72,0.08)] backdrop-blur-md">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-500 opacity-75" />
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-600" />
                </span>
                <span className="text-[11px] font-extrabold tracking-widest uppercase text-rose-950">
                  SMART LPG SUPPLY
                </span>
                <span className="text-rose-300 font-bold">•</span>
                <span className="text-[11px] font-semibold text-rose-700">
                  SIMPLE • SECURE
                </span>
              </div>

              {/* Main Headline */}
              <div className="space-y-1">
                <h1 className="text-[2.6rem] sm:text-5xl lg:text-[3.25rem] xl:text-[3.5rem] font-black text-slate-950 leading-[1.05] tracking-tight">
                  A Smarter Supply<br />
                  Chain for a<br />
                  <span className="bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 bg-clip-text text-transparent inline-block">
                    Safer Tomorrow
                  </span>
                </h1>
              </div>

              {/* Sub-copy */}
              <p className="text-[15px] sm:text-base lg:text-[17px] text-slate-600 leading-relaxed max-w-[500px] font-normal">
                LPG-Chain connects consumers, authorized distributors, and fleet logistics
                into one unified platform — bringing transparency, IoT verification, and
                tamper-proof safety to every doorstep.
              </p>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center gap-3.5 pt-1">
                <button
                  type="button"
                  onClick={() => {
                    if (onOpenBookingTab) {
                      onOpenBookingTab();
                    } else {
                      setIsBookingOpen(true);
                    }
                  }}
                  className="inline-flex items-center gap-2.5 px-6 sm:px-7 py-3.5 sm:py-4 bg-gradient-to-r from-rose-600 via-red-600 to-rose-700 hover:from-rose-500 hover:to-red-500 text-white font-bold text-sm rounded-full shadow-[0_12px_28px_-6px_rgba(225,29,72,0.45),inset_0_1px_1px_rgba(255,255,255,0.35)] hover:shadow-[0_16px_36px_-6px_rgba(225,29,72,0.55)] active:scale-[0.98] transition-all duration-200 group"
                >
                  <Calendar size={17} className="group-hover:rotate-6 transition-transform" />
                  <span>Book LPG Cylinder</span>
                  <ArrowRight size={15} className="group-hover:translate-x-1 transition-transform" />
                </button>

                <button
                  type="button"
                  onClick={() => onNavigateToTracking('1')}
                  className="inline-flex items-center gap-2.5 px-6 sm:px-7 py-3.5 sm:py-4 bg-white/95 hover:bg-white text-slate-800 hover:text-slate-950 font-bold text-sm rounded-full border border-slate-300/80 shadow-[0_4px_16px_rgba(15,23,42,0.06)] hover:border-slate-400 hover:shadow-[0_6px_20px_rgba(15,23,42,0.1)] active:scale-[0.98] transition-all duration-200 group backdrop-blur-md"
                >
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                  <Truck size={17} className="text-slate-600 group-hover:text-rose-600 transition-colors" />
                  <span>Track Delivery</span>
                </button>
              </div>

              {/* Enterprise Trust Grid */}
              <div className="pt-3 border-t border-slate-200/90 max-w-[510px]">
                <div className="grid grid-cols-3 gap-3">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-emerald-50 border border-emerald-200/80 flex items-center justify-center flex-shrink-0">
                      <ShieldCheck size={15} className="text-emerald-600" />
                    </div>
                    <div>
                      <div className="text-[12px] font-bold text-slate-900 leading-tight">Secure Platform</div>
                      <div className="text-[10px] text-slate-500 leading-tight">100% Certified</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-rose-50 border border-rose-200/80 flex items-center justify-center flex-shrink-0">
                      <MapPin size={15} className="text-rose-600" />
                    </div>
                    <div>
                      <div className="text-[12px] font-bold text-slate-900 leading-tight">Nationwide</div>
                      <div className="text-[10px] text-slate-500 leading-tight">28+ States</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-amber-50 border border-amber-200/80 flex items-center justify-center flex-shrink-0">
                      <Clock size={15} className="text-amber-600" />
                    </div>
                    <div>
                      <div className="text-[12px] font-bold text-slate-900 leading-tight">Fast Dispatch</div>
                      <div className="text-[10px] text-slate-500 leading-tight">&lt; 2h Delivery</div>
                    </div>
                  </div>
                </div>
              </div>

            </div>

            {/* ─────────────────────────────────────────────
                RIGHT COLUMN: The Interactive LPG Cylinder Stage
                Scroll-Driven & Click-Activated Storytelling
            ───────────────────────────────────────────── */}
            <div className="relative flex items-center justify-center py-4 lg:py-6 overflow-visible min-h-[490px] lg:min-h-[570px]">

              {/* Shockwave Impulse Ring on Click */}
              {shockwaveKey > 0 && (
                <div
                  key={shockwaveKey}
                  className="absolute rounded-full pointer-events-none border-2 border-rose-500/70 animate-ping"
                  style={{
                    width: 320,
                    height: 320,
                    animationDuration: '0.85s',
                  }}
                />
              )}

              {/* ── Active Status HUD Bar (Above Cylinder) ── */}
              <div
                className={`absolute -top-7 z-40 transition-all duration-500 flex items-center gap-2 ${
                  isCoreActive
                    ? 'translate-y-0 opacity-100 pointer-events-auto'
                    : '-translate-y-3 opacity-0 pointer-events-none'
                }`}
              >
                <div className="flex items-center gap-2 bg-slate-950/90 text-white backdrop-blur-xl px-3.5 py-1.5 rounded-full border border-white/20 shadow-xl">
                  <span className="relative flex h-2 w-2">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
                  </span>
                  <span className="text-[11px] font-black tracking-wider uppercase">
                    CORE ACTIVE • 7 SYSTEMS SYNCHRONIZED
                  </span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setIsCoreActiveManual(false);
                    }}
                    className="ml-1 p-0.5 rounded-full hover:bg-white/20 text-white/70 hover:text-white transition-colors"
                    title="Deactivate Core"
                  >
                    <X size={12} />
                  </button>
                </div>
              </div>

              {/* Dynamic Toast Feedback when clicking nodes */}
              {activeNodeToast && (
                <div className="absolute -top-12 z-50 animate-in fade-in slide-in-from-top-2 duration-300 pointer-events-none">
                  <div className="bg-rose-900/90 text-white text-xs font-bold px-4 py-1.5 rounded-full shadow-xl backdrop-blur-md border border-rose-500/40">
                    {activeNodeToast}
                  </div>
                </div>
              )}

              {/* ── Dynamic Laser Connection Lines Originating from Cylinder Core ── */}
              <svg
                className="absolute pointer-events-none z-15 overflow-visible hidden md:block"
                style={{
                  top: '50%',
                  left: '50%',
                  width: 2,
                  height: 2,
                }}
              >
                {CORE_SYSTEMS.map((system, idx) => {
                  const lineDelay = idx * 65;
                  return (
                    <g
                      key={`laser-line-${system.id}`}
                      style={{
                        transformOrigin: '0px 0px',
                        transform: isCoreActive ? 'scale(1)' : 'scale(0)',
                        opacity: isCoreActive ? 0.9 : 0,
                        transition: isCoreActive
                          ? `transform 380ms cubic-bezier(0.16, 1, 0.3, 1) ${lineDelay}ms, opacity 240ms ease-out ${lineDelay}ms`
                          : 'transform 220ms ease-in, opacity 180ms ease-in',
                      }}
                    >
                      {/* Ambient Glow Aura */}
                      <line
                        x1={0}
                        y1={0}
                        x2={system.dx}
                        y2={system.dy}
                        stroke={system.accentColor}
                        strokeWidth="3.5"
                        strokeOpacity="0.22"
                      />
                      {/* Crisp Laser Connection Line */}
                      <line
                        x1={0}
                        y1={0}
                        x2={system.dx}
                        y2={system.dy}
                        stroke="#e11d48"
                        strokeWidth="1.25"
                        strokeDasharray="4 3"
                        strokeOpacity="0.7"
                      />
                      {/* Midpoint Pulse Dot */}
                      <circle
                        cx={system.dx * 0.48}
                        cy={system.dy * 0.48}
                        r="2"
                        fill="#e11d48"
                        opacity="0.75"
                      />
                      {/* Terminal Connection Node */}
                      <circle
                        cx={system.dx}
                        cy={system.dy}
                        r="2.5"
                        fill={system.accentColor}
                        opacity="0.9"
                      />
                    </g>
                  );
                })}
              </svg>

              {/* ════════════════════════════════════════════════════
                  THE 7 CORE SATELLITE NODES
                  Smooth Emergence: Cylinder → connection grows → card travels outward → card becomes visible
              ════════════════════════════════════════════════════ */}
              {CORE_SYSTEMS.map((system, idx) => {
                const IconComponent = system.icon;

                // Timed Staggered Sequence:
                // 1. Connection line extends from cylinder (0ms + stagger)
                // 2. Card travels outward along connection vector (140ms + stagger)
                // 3. Card content smoothly fades in (200ms + stagger)
                const lineDelay = idx * 65;
                const cardTravelDelay = lineDelay + 140;
                const cardContentDelay = cardTravelDelay + 60;

                // Start position resting close to cylinder surface
                const startDx = Math.round(system.dx * 0.30);
                const startDy = Math.round(system.dy * 0.30);

                return (
                  <div
                    key={system.id}
                    className="absolute z-30 pointer-events-auto select-none hidden md:block"
                    style={{
                      transform: isCoreActive
                        ? `translate(${system.dx}px, ${system.dy}px) scale(1)`
                        : `translate(${startDx}px, ${startDy}px) scale(0.68)`,
                      opacity: isCoreActive ? 1 : 0,
                      pointerEvents: isCoreActive ? 'auto' : 'none',
                      transition: isCoreActive
                        ? `transform 480ms cubic-bezier(0.16, 1, 0.3, 1) ${cardTravelDelay}ms, opacity 340ms ease-out ${cardContentDelay}ms`
                        : 'transform 240ms ease-in, opacity 180ms ease-in',
                    }}
                  >
                    {/* Compact Glass Capsule Chip */}
                    <button
                      type="button"
                      onClick={(e) => handleNodeClick(e, system)}
                      className="group flex items-center gap-2.5 px-3 py-1.5 rounded-full bg-white/95 hover:bg-white backdrop-blur-xl border border-white/90 shadow-[0_8px_20px_rgba(15,23,42,0.10),0_1px_3px_rgba(15,23,42,0.06)] hover:shadow-[0_12px_28px_rgba(225,29,72,0.18)] hover:scale-105 active:scale-95 transition-all duration-200 cursor-pointer text-left"
                      title={system.description}
                    >
                      {/* Accent Icon Circle */}
                      <div
                        className="w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 transition-transform group-hover:scale-110"
                        style={{
                          backgroundColor: `${system.accentColor}18`,
                          border: `1px solid ${system.accentColor}35`,
                          color: system.accentColor,
                        }}
                      >
                        <IconComponent size={13} strokeWidth={2.2} />
                      </div>

                      {/* Title & Tag */}
                      <div className="flex flex-col text-left">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[11.5px] font-bold text-slate-900 group-hover:text-rose-600 transition-colors leading-tight whitespace-nowrap">
                            {system.name}
                          </span>
                          <span
                            className={`text-[8.5px] font-extrabold px-1.5 py-0.5 rounded-full border ${system.badgeBg} ${system.badgeText} ${system.badgeBorder} leading-none`}
                          >
                            {system.tag}
                          </span>
                        </div>
                      </div>
                    </button>
                  </div>
                );
              })}

              {/* ════════════ THE CENTERPIECE: REALISTIC LPG CYLINDER ════════════ */}
              <div
                ref={cylinderRef}
                onClick={handleCylinderClick}
                onMouseEnter={() => setCylinderHovered(true)}
                onMouseLeave={() => setCylinderHovered(false)}
                className="relative z-20 cursor-pointer select-none transition-transform duration-300 ease-out group"
                style={{
                  width: 320,
                  height: 490,
                  transform: 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1)',
                }}
                title={isCoreActive ? 'Click to toggle Core' : 'Click or scroll to Activate LPG-Chain Core'}
              >
                {/* 3-Tier Ground Contact Shadow */}
                <div
                  className="absolute left-1/2 -translate-x-1/2 pointer-events-none"
                  style={{
                    width: 220,
                    height: 12,
                    background: 'radial-gradient(ellipse at center, rgba(15,23,42,0.75) 0%, rgba(15,23,42,0.3) 50%, transparent 75%)',
                    filter: 'blur(3px)',
                    bottom: 2,
                  }}
                />
                <div
                  className="absolute left-1/2 -translate-x-1/2 pointer-events-none"
                  style={{
                    width: 290,
                    height: 24,
                    background: 'radial-gradient(ellipse at center, rgba(15,23,42,0.28) 0%, rgba(15,23,42,0.08) 55%, transparent 75%)',
                    filter: 'blur(10px)',
                    bottom: -2,
                  }}
                />
                <div
                  className="absolute left-1/2 -translate-x-1/2 pointer-events-none"
                  style={{
                    width: 240,
                    height: 18,
                    background: isCoreActive
                      ? 'radial-gradient(ellipse at center, rgba(225,29,72,0.35) 0%, transparent 70%)'
                      : 'radial-gradient(ellipse at center, rgba(225,29,72,0.18) 0%, transparent 70%)',
                    filter: 'blur(14px)',
                    bottom: 0,
                  }}
                />

                {/* Cylinder Image Cutout */}
                <picture className="w-full h-full flex items-center justify-center">
                  <source srcSet="/assets/lpg_cylinder_transparent.webp" type="image/webp" />
                  <img
                    src="/assets/lpg_cylinder_transparent.png"
                    alt="LPG Cylinder"
                    className="w-full h-full object-contain pointer-events-none"
                    style={{
                      filter: isCoreActive
                        ? 'drop-shadow(0 32px 50px rgba(15,23,42,0.35)) drop-shadow(0 0 25px rgba(225,29,72,0.35)) brightness(1.04)'
                        : cylinderHovered
                        ? 'drop-shadow(0 26px 42px rgba(15,23,42,0.32)) drop-shadow(0 6px 14px rgba(225,29,72,0.22)) brightness(1.02)'
                        : 'drop-shadow(0 16px 28px rgba(15,23,42,0.22)) drop-shadow(0 4px 10px rgba(0,0,0,0.10))',
                      transition: 'filter 0.3s ease',
                    }}
                    draggable={false}
                  />
                </picture>

                {/* Interactive Click/Scroll Invitation Badge (When Core is Dormant) */}
                {!isCoreActive && (
                  <div className="absolute bottom-16 left-1/2 -translate-x-1/2 pointer-events-none z-30 animate-bounce duration-1000">
                    <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-950/85 backdrop-blur-md text-white border border-white/20 shadow-xl">
                      <Zap size={12} className="text-amber-400 fill-amber-400 animate-pulse" />
                      <span className="text-[11px] font-bold tracking-wide whitespace-nowrap">
                        Scroll or Click to Activate
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* Mobile: Clean Stacked Presentation when Core is Active */}
              {isCoreActive && (
                <div className="block md:hidden w-full max-w-sm mx-auto mt-4 px-2 z-20">
                  <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-2">
                    7 Systems Synchronized
                  </div>
                  <div className="grid grid-cols-1 gap-2">
                    {CORE_SYSTEMS.map((system) => {
                      const IconComponent = system.icon;
                      return (
                        <button
                          key={system.id}
                          type="button"
                          onClick={(e) => handleNodeClick(e, system)}
                          className="flex items-center gap-3 p-2.5 rounded-xl bg-white/95 border border-slate-200/80 shadow-sm active:scale-[0.98] transition-all text-left"
                        >
                          <div
                            className="w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0"
                            style={{
                              backgroundColor: `${system.accentColor}18`,
                              color: system.accentColor,
                            }}
                          >
                            <IconComponent size={14} />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="text-[12px] font-bold text-slate-900 leading-tight">
                              {system.name}
                            </div>
                            <div className="text-[10px] text-slate-500 leading-tight truncate">
                              {system.description}
                            </div>
                          </div>
                          <span
                            className={`text-[8.5px] font-extrabold px-1.5 py-0.5 rounded-full border ${system.badgeBg} ${system.badgeText} ${system.badgeBorder}`}
                          >
                            {system.tag}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* ── Storytelling Progress HUD & Scenario Switcher ── */}
              <div className="absolute bottom-1 right-2 flex items-center gap-3 z-30">
                {/* Live Story Stage Pill */}
                <div className="hidden sm:flex items-center gap-2 select-none px-3 py-1 rounded-full bg-white/90 backdrop-blur-md border border-slate-200/80 shadow-sm">
                  <span className="w-1.5 h-1.5 rounded-full bg-rose-600 animate-pulse" />
                  <span className="text-[11px] font-bold text-slate-800">{stageLabel}</span>
                  <div className="w-12 h-1 bg-slate-200 rounded-full overflow-hidden ml-1">
                    <div
                      className="h-full bg-gradient-to-r from-rose-500 to-amber-500 transition-all duration-150"
                      style={{ width: `${Math.round(scrollProgress * 100)}%` }}
                    />
                  </div>
                </div>

                {/* Scenario Navigation Controls */}
                <div
                  className="flex items-center gap-1 bg-white/95 backdrop-blur-md px-2 py-1.5 rounded-full border border-slate-200 shadow-sm"
                  onMouseEnter={() => setIsScenarioPaused(true)}
                  onMouseLeave={() => setIsScenarioPaused(false)}
                >
                  <button
                    type="button"
                    onClick={() => {
                      const prevIdx = (activeScenarioIndex - 1 + SCENARIOS.length) % SCENARIOS.length;
                      handleSelectScenario(prevIdx);
                    }}
                    className="w-6 h-6 rounded-full flex items-center justify-center text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors"
                    title="Previous environment"
                  >
                    <ChevronLeft size={14} />
                  </button>
                  <span className="text-[10px] font-bold text-slate-600 px-1">
                    {activeScenarioIndex + 1}/{SCENARIOS.length}
                  </span>
                  <button
                    type="button"
                    onClick={() => {
                      const nextIdx = (activeScenarioIndex + 1) % SCENARIOS.length;
                      handleSelectScenario(nextIdx);
                    }}
                    className="w-6 h-6 rounded-full flex items-center justify-center text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors"
                    title="Next environment"
                  >
                    <ChevronRight size={14} />
                  </button>
                </div>
              </div>

            </div>
          </div>
        </div>

        {/* ── 3. BOTTOM SCENARIO DOCK — Full-Width Enterprise Strip ── */}
        <div className="relative w-full bg-slate-950/85 backdrop-blur-xl border-t border-white/10 mt-auto">
          {/* Progress countdown indicator */}
          <div className="absolute top-0 left-0 right-0 h-[2px] bg-white/10 overflow-hidden">
            <div
              key={progressKey}
              className={`h-full bg-gradient-to-r from-rose-500 via-amber-500 to-rose-500 transition-none ${
                isScenarioPaused ? 'opacity-50' : 'animate-[progress_8s_linear]'
              }`}
              style={{
                width: isScenarioPaused ? '100%' : undefined,
                animation: isScenarioPaused ? 'none' : undefined,
              }}
            />
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 divide-x divide-white/10">
            {SCENARIOS.map((scenario, idx) => {
              const isActive = idx === activeScenarioIndex;
              return (
                <button
                  key={scenario.id}
                  type="button"
                  onClick={() => {
                    handleSelectScenario(idx);
                    setIsScenarioPaused(true);
                    setTimeout(() => setIsScenarioPaused(false), 9000);
                  }}
                  className={`relative group overflow-hidden flex items-end text-left transition-all h-20 sm:h-24 ${
                    isActive ? 'bg-white/[0.08]' : 'hover:bg-white/[0.04]'
                  }`}
                >
                  <img
                    src={scenario.image}
                    alt={scenario.name}
                    className={`absolute inset-0 w-full h-full object-cover object-center transition-all duration-500 ${
                      isActive
                        ? 'scale-105 opacity-60'
                        : 'opacity-35 group-hover:opacity-50 group-hover:scale-105'
                    }`}
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950/95 via-slate-950/60 to-transparent" />

                  {isActive && (
                    <div className="absolute top-0 left-0 right-0 h-[3px] bg-gradient-to-r from-rose-500 via-red-500 to-amber-500 shadow-[0_2px_8px_rgba(244,63,94,0.6)]" />
                  )}

                  <div className="relative z-10 p-3 sm:p-3.5 w-full flex items-end justify-between">
                    <div>
                      <div className="flex items-center gap-1.5 mb-0.5">
                        <span className={`text-[10px] font-black tracking-wider ${isActive ? 'text-rose-400' : 'text-slate-400'}`}>
                          {scenario.number}
                        </span>
                        <span className="text-white/20 text-[10px]">•</span>
                        <span className="text-[10px] font-semibold text-white/60 tracking-wider uppercase">
                          {scenario.tag}
                        </span>
                      </div>
                      <span className={`text-[13px] font-extrabold block leading-tight ${isActive ? 'text-white' : 'text-slate-200 group-hover:text-white'}`}>
                        {scenario.name}
                      </span>
                      <span className="text-white/50 text-[11px] block leading-tight mt-0.5 hidden sm:block truncate max-w-[220px]">
                        {scenario.caption}
                      </span>
                    </div>

                    {isActive && (
                      <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse flex-shrink-0 mb-1" />
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>

      </div>

      {/* Booking Modal */}
      {BookingModal && (
        <BookingModal
          isOpen={isBookingOpen}
          onClose={() => setIsBookingOpen(false)}
          onTrackOrder={(id) => {
            setIsBookingOpen(false);
            onNavigateToTracking(id);
          }}
        />
      )}
    </section>
  );
};
