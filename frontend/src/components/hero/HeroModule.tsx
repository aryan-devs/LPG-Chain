import React from 'react';
import { ChevronRight } from 'lucide-react';
import { HeroModuleData, ModuleAccent } from './heroModules';

interface HeroModuleProps {
  module: HeroModuleData;
  isHovered: boolean;
  onHover: (id: string | null) => void;
  onClick: (module: HeroModuleData) => void;
  layoutMode?: 'absolute' | 'grid';
  style?: React.CSSProperties;
  className?: string;
}

export const HeroModule: React.FC<HeroModuleProps> = ({
  module,
  isHovered,
  onHover,
  onClick,
  layoutMode = 'absolute',
  style,
  className = '',
}) => {
  const Icon = module.icon;

  const accentStyles: Record<ModuleAccent, {
    iconBg: string;
    iconColor: string;
    borderActive: string;
    glow: string;
  }> = {
    red: {
      iconBg: 'bg-rose-50 text-rose-600 border border-rose-100',
      iconColor: 'text-rose-600',
      borderActive: 'border-rose-400 ring-2 ring-rose-500/15',
      glow: 'shadow-rose-500/15',
    },
    orange: {
      iconBg: 'bg-amber-50 text-amber-600 border border-amber-100',
      iconColor: 'text-amber-600',
      borderActive: 'border-amber-400 ring-2 ring-amber-500/15',
      glow: 'shadow-amber-500/15',
    },
    cyan: {
      iconBg: 'bg-sky-50 text-sky-600 border border-sky-100',
      iconColor: 'text-sky-600',
      borderActive: 'border-sky-400 ring-2 ring-sky-500/15',
      glow: 'shadow-sky-500/15',
    },
    emerald: {
      iconBg: 'bg-emerald-50 text-emerald-600 border border-emerald-100',
      iconColor: 'text-emerald-600',
      borderActive: 'border-emerald-400 ring-2 ring-emerald-500/15',
      glow: 'shadow-emerald-500/15',
    },
  };

  const currentAccent = accentStyles[module.accent];

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick(module);
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      aria-label={`${module.title}: ${module.subtitle}`}
      onClick={() => onClick(module)}
      onKeyDown={handleKeyDown}
      onMouseEnter={() => onHover(module.id)}
      onMouseLeave={() => onHover(null)}
      onFocus={() => onHover(module.id)}
      onBlur={() => onHover(null)}
      style={style}
      className={`group cursor-pointer select-none text-left transition-all duration-300 ease-out outline-none focus-visible:ring-2 focus-visible:ring-rose-500 focus-visible:ring-offset-2 ${
        layoutMode === 'absolute'
          ? 'absolute w-[195px] sm:w-[212px]'
          : 'relative w-full'
      } ${
        isHovered
          ? 'scale-[1.04] -translate-y-0.5 shadow-xl z-30'
          : 'shadow-md hover:shadow-lg z-20'
      } ${className}`}
    >
      <div
        className={`bg-white/95 backdrop-blur-md rounded-xl px-2.5 py-2 border transition-all duration-300 flex items-center justify-between gap-2.5 ${
          isHovered
            ? `${currentAccent.borderActive} bg-white ${currentAccent.glow}`
            : 'border-slate-200/90 hover:border-slate-300'
        }`}
      >
        <div className="flex items-center gap-2 min-w-0">
          {/* Module Icon Container */}
          <div
            className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 transition-transform duration-300 group-hover:scale-105 ${currentAccent.iconBg}`}
          >
            <Icon size={15} strokeWidth={2.2} />
          </div>

          {/* Module Text Content */}
          <div className="min-w-0">
            <h3 className="text-xs font-bold text-slate-900 group-hover:text-rose-600 transition-colors tracking-tight truncate">
              {module.title}
            </h3>
            <p className="text-[10px] text-slate-500 truncate leading-snug">
              {module.subtitle}
            </p>
          </div>
        </div>

        {/* Action Chevron */}
        <div className="w-4 h-4 rounded-full flex items-center justify-center text-slate-400 group-hover:text-slate-800 group-hover:translate-x-0.5 transition-all flex-shrink-0">
          <ChevronRight size={13} />
        </div>
      </div>
    </div>
  );
};
