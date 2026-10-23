import React from 'react';
import { ArrowLeft, Sparkles, LucideIcon, Calendar, Truck } from 'lucide-react';

interface CapabilityPageShellProps {
  title: string;
  subtitle: string;
  description: string;
  badge: string;
  icon: LucideIcon;
  accentColor: string; // e.g. 'rose', 'amber', 'purple', 'cyan', 'emerald', 'blue'
  onBackToHome: () => void;
  onOpenBooking?: () => void;
  onNavigateToTracking?: () => void;
  children: React.ReactNode;
}

export const CapabilityPageShell: React.FC<CapabilityPageShellProps> = ({
  title,
  subtitle,
  description,
  badge,
  icon: Icon,
  accentColor,
  onBackToHome,
  onOpenBooking,
  onNavigateToTracking,
  children,
}) => {
  return (
    <div className="w-full bg-slate-50/60 min-h-[calc(100vh-4rem)] pb-16">
      {/* ── Top Header Banner ── */}
      <div className="border-b border-slate-200/80 bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-7 sm:py-9">
          {/* Breadcrumb & Demo Tag */}
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
              <button
                type="button"
                onClick={onBackToHome}
                className="hover:text-rose-600 transition-colors flex items-center gap-1"
              >
                <ArrowLeft size={13} />
                <span>Home</span>
              </button>
              <span className="text-slate-300">/</span>
              <span className="text-slate-500">Capabilities</span>
              <span className="text-slate-300">/</span>
              <span className="text-slate-900 font-bold">{title}</span>
            </div>

            {/* Clear DEMO / PREVIEW Label */}
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-800 text-[11px] font-black uppercase tracking-wider shadow-sm">
              <Sparkles size={12} className="text-amber-600 animate-pulse" />
              <span>DEMO / PREVIEW • {badge}</span>
            </div>
          </div>

          {/* Main Title & Action Row */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="flex items-start gap-4">
              <div
                className="w-13 h-13 sm:w-14 sm:h-14 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-sm"
                style={{
                  backgroundColor: `${accentColor}15`,
                  border: `1px solid ${accentColor}30`,
                  color: accentColor,
                }}
              >
                <Icon size={28} strokeWidth={2.2} />
              </div>
              <div className="space-y-1">
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl sm:text-3xl font-black text-slate-950 tracking-tight leading-tight">
                    {title}
                  </h1>
                </div>
                <p className="text-sm sm:text-base font-semibold text-rose-600 leading-snug">
                  {subtitle}
                </p>
                <p className="text-xs sm:text-sm text-slate-600 max-w-2xl leading-relaxed">
                  {description}
                </p>
              </div>
            </div>

            {/* Header Actions */}
            <div className="flex items-center gap-2.5 flex-shrink-0 self-start md:self-center">
              <button
                type="button"
                onClick={onBackToHome}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-300 hover:border-slate-400 bg-white text-slate-700 hover:text-slate-950 text-xs sm:text-sm font-bold shadow-sm transition-all"
              >
                <ArrowLeft size={14} />
                <span>Back to Home</span>
              </button>

              {onOpenBooking && (
                <button
                  type="button"
                  onClick={onOpenBooking}
                  className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs sm:text-sm font-bold shadow-sm shadow-rose-600/25 transition-all"
                >
                  <Calendar size={14} />
                  <span>Book Refill</span>
                </button>
              )}

              {onNavigateToTracking && (
                <button
                  type="button"
                  onClick={onNavigateToTracking}
                  className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs sm:text-sm font-bold shadow-sm transition-all"
                >
                  <Truck size={14} />
                  <span>Live Tracking</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── Content Body ── */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8">
        {children}
      </div>
    </div>
  );
};
