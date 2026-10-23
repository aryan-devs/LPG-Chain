import React from 'react';
import { HeroModuleData } from './heroModules';
import { HeroModuleNetwork } from './HeroModuleNetwork';

interface HeroFallbackProps {
  isCylinderHovered: boolean;
  setIsCylinderHovered: (h: boolean) => void;
  isCoreActive: boolean;
  setIsCoreActive: (a: boolean | ((p: boolean) => boolean)) => void;
  scrollPhase: number;
  hoveredModuleId: string | null;
  onHoverModule: (id: string | null) => void;
  onClickModule: (module: HeroModuleData) => void;
  isMobileOrTablet: boolean;
}

export const HeroFallback: React.FC<HeroFallbackProps> = ({
  isCylinderHovered,
  setIsCylinderHovered,
  isCoreActive,
  setIsCoreActive,
  scrollPhase,
  hoveredModuleId,
  onHoverModule,
  onClickModule,
  isMobileOrTablet,
}) => {
  return (
    <div className="relative w-full h-[540px] flex items-center justify-center select-none">
      {/* 2D High-Fidelity SVG LPG Cylinder */}
      <div
        role="button"
        tabIndex={0}
        aria-label="Interactive LPG Cylinder Fallback Representation"
        onClick={() => setIsCoreActive((p) => !p)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            setIsCoreActive((p) => !p);
          }
        }}
        onMouseEnter={() => setIsCylinderHovered(true)}
        onMouseLeave={() => setIsCylinderHovered(false)}
        className={`relative z-10 cursor-pointer transition-transform duration-500 ease-out outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded-3xl p-2 ${
          isCylinderHovered ? 'scale-105' : 'scale-100'
        } ${isCoreActive ? 'scale-105' : ''}`}
      >
        <svg
          width="240"
          height="420"
          viewBox="0 0 240 420"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="filter drop-shadow-2xl"
        >
          <defs>
            {/* Cylinder Body Red Metallic Gradient */}
            <linearGradient id="bodyRedGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#991b1b" />
              <stop offset="25%" stopColor="#dc2626" />
              <stop offset="50%" stopColor="#ef4444" />
              <stop offset="75%" stopColor="#dc2626" />
              <stop offset="100%" stopColor="#7f1d1d" />
            </linearGradient>

            {/* Shroud / Collar Gradient */}
            <linearGradient id="collarGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#7f1d1d" />
              <stop offset="30%" stopColor="#dc2626" />
              <stop offset="70%" stopColor="#ef4444" />
              <stop offset="100%" stopColor="#991b1b" />
            </linearGradient>

            {/* Brass Valve Gradient */}
            <linearGradient id="brassGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#b45309" />
              <stop offset="40%" stopColor="#f59e0b" />
              <stop offset="80%" stopColor="#fbbf24" />
              <stop offset="100%" stopColor="#92400e" />
            </linearGradient>

            {/* Foot Ring Dark Steel */}
            <linearGradient id="footRingGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#1e293b" />
              <stop offset="50%" stopColor="#475569" />
              <stop offset="100%" stopColor="#0f172a" />
            </linearGradient>
          </defs>

          {/* 1. Foot Ring Stand */}
          <rect x="36" y="370" width="168" height="34" rx="6" fill="url(#footRingGrad)" />
          {/* Foot ring vent cutouts */}
          <rect x="56" y="380" width="28" height="14" rx="4" fill="#f8fafc" />
          <rect x="106" y="380" width="28" height="14" rx="4" fill="#f8fafc" />
          <rect x="156" y="380" width="28" height="14" rx="4" fill="#f8fafc" />

          {/* 2. Lower Rounded Base */}
          <path
            d="M 24 280 C 24 350, 48 376, 120 376 C 192 376, 216 350, 216 280 Z"
            fill="url(#bodyRedGrad)"
          />

          {/* 3. Main Cylindrical Body */}
          <rect x="24" y="140" width="192" height="150" fill="url(#bodyRedGrad)" />

          {/* 4. Welded Middle Seam Bead */}
          <rect x="22" y="212" width="196" height="6" rx="3" fill="#7f1d1d" opacity="0.8" />
          <rect x="23" y="213" width="194" height="2" fill="#fca5a5" opacity="0.6" />

          {/* 5. Upper Rounded Shoulder */}
          <path
            d="M 24 150 C 24 80, 52 64, 120 64 C 188 64, 216 80, 216 150 Z"
            fill="url(#bodyRedGrad)"
          />

          {/* 6. Valve Guard Collar / Shroud with handles */}
          <path
            d="M 52 82 C 52 40, 70 30, 120 30 C 170 30, 188 40, 188 82 Z"
            fill="url(#collarGrad)"
            stroke="#991b1b"
            strokeWidth="2"
          />
          {/* Collar Handle Openings */}
          <rect x="62" y="44" width="24" height="14" rx="6" fill="#f8fafc" />
          <rect x="154" y="44" width="24" height="14" rx="6" fill="#f8fafc" />

          {/* 7. Brass Valve Body & Handwheel */}
          <rect x="110" y="36" width="20" height="34" rx="2" fill="url(#brassGrad)" />
          <rect x="102" y="26" width="36" height="10" rx="3" fill="url(#brassGrad)" />
          {/* Blue safety cap */}
          <circle cx="126" cy="50" r="7" fill="#0284c7" />

          {/* 8. LPG-Chain Brand Badge */}
          <rect x="44" y="146" width="152" height="88" rx="8" fill="rgba(0, 0, 0, 0.22)" />
          {/* Flame Icon */}
          <path
            d="M 120 150 C 128 158, 134 167, 132 173 C 130 178, 125 180, 120 180 C 115 180, 110 178, 108 173 C 106 167, 112 158, 120 150 Z"
            fill="#ffffff"
          />
          <text
            x="120"
            y="200"
            fill="#ffffff"
            fontSize="18"
            fontWeight="900"
            textAnchor="middle"
            fontFamily="sans-serif"
          >
            LPG-Chain
          </text>
          <line x1="60" y1="206" x2="180" y2="206" stroke="#ffffff" strokeWidth="1.2" strokeOpacity="0.85" />
          <text
            x="120"
            y="218"
            fill="#ffffff"
            fontSize="7.5"
            fontWeight="bold"
            letterSpacing="2"
            textAnchor="middle"
            fontFamily="sans-serif"
          >
            SAFE • SMART • CONNECTED
          </text>

          {/* Active Core Ring in Fallback (Warm Brand Rose Ring Outside) */}
          {isCoreActive && (
            <ellipse
              cx="120"
              cy="215"
              rx="106"
              ry="16"
              fill="none"
              stroke="#f43f5e"
              strokeWidth="2.5"
              strokeDasharray="6 4"
            />
          )}
        </svg>

        {/* Fallback Notice Badge */}
        <div className="text-center mt-2">
          <span className="text-[10px] font-mono uppercase tracking-wider text-slate-400 bg-slate-100 border border-slate-200 px-2 py-0.5 rounded-full">
            Standard 2D Render Mode
          </span>
        </div>
      </div>

      {/* Render Module Network Overlay */}
      <HeroModuleNetwork
        isCylinderHovered={isCylinderHovered}
        isCoreActive={isCoreActive}
        scrollPhase={scrollPhase}
        hoveredModuleId={hoveredModuleId}
        onHoverModule={onHoverModule}
        onClickModule={onClickModule}
        isMobileOrTablet={isMobileOrTablet}
      />
    </div>
  );
};
