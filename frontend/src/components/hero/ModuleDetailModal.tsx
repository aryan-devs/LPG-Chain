import React from 'react';
import { X, CheckCircle2, ArrowRight } from 'lucide-react';
import { HeroModuleData } from './heroModules';

interface ModuleDetailModalProps {
  module: HeroModuleData | null;
  onClose: () => void;
  onNavigateToTracking?: () => void;
  onOpenBooking?: () => void;
}

export const ModuleDetailModal: React.FC<ModuleDetailModalProps> = ({
  module,
  onClose,
  onNavigateToTracking,
  onOpenBooking,
}) => {
  if (!module) return null;

  const Icon = module.icon;

  const handleAction = () => {
    onClose();
    if (module.route === 'tracking' && onNavigateToTracking) {
      onNavigateToTracking();
    } else if (module.route === 'booking' && onOpenBooking) {
      onOpenBooking();
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="module-detail-title"
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="p-6 pb-4 border-b border-slate-100 bg-slate-50/60 flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 border border-blue-200 text-blue-600 flex items-center justify-center">
              <Icon size={24} />
            </div>
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-blue-600 bg-blue-100/60 px-2 py-0.5 rounded-full">
                {module.category}
              </span>
              <h2 id="module-detail-title" className="text-lg font-bold text-slate-900 mt-1">
                {module.title}
              </h2>
              <p className="text-xs font-medium text-slate-500">{module.subtitle}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close details"
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4">
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1">
              Operational Scope
            </h4>
            <p className="text-xs sm:text-sm text-slate-700 leading-relaxed">
              {module.description}
            </p>
          </div>

          {/* Technical Specifications Table */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
              Architecture &amp; Performance Metrics
            </h4>
            <div className="bg-slate-50 rounded-2xl border border-slate-200/80 divide-y divide-slate-100 text-xs">
              {module.technicalSpecs.map((spec, idx) => (
                <div key={idx} className="flex justify-between items-center px-4 py-2.5">
                  <span className="text-slate-500 font-medium">{spec.label}</span>
                  <span className="font-mono font-bold text-slate-800">{spec.value}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Status Indicator */}
          <div className="p-3 bg-emerald-50/80 border border-emerald-200/80 rounded-2xl flex items-center gap-2.5 text-xs text-emerald-800">
            <CheckCircle2 size={16} className="text-emerald-600 flex-shrink-0" />
            <span>
              <strong>LPG-Chain Core Integration:</strong> Active enterprise node connected to unified data plane.
            </span>
          </div>

          {/* Action Button */}
          <div className="pt-2 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900 rounded-xl hover:bg-slate-100 transition-colors"
            >
              Dismiss
            </button>

            {module.route === 'tracking' ? (
              <button
                type="button"
                onClick={handleAction}
                className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs sm:text-sm font-bold rounded-xl shadow-lg shadow-blue-600/30 transition-all flex items-center gap-2"
              >
                <span>Launch Live Tracking Console</span>
                <ArrowRight size={14} />
              </button>
            ) : module.route === 'booking' ? (
              <button
                type="button"
                onClick={handleAction}
                className="px-5 py-2.5 bg-rose-600 hover:bg-rose-500 text-white text-xs sm:text-sm font-bold rounded-xl shadow-lg shadow-rose-600/30 transition-all flex items-center gap-2"
              >
                <span>Initiate Quick Refill</span>
                <ArrowRight size={14} />
              </button>
            ) : (
              <button
                type="button"
                onClick={onClose}
                className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white text-xs sm:text-sm font-bold rounded-xl transition-all"
              >
                Acknowledge Specs
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
