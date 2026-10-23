import React from 'react';
import { HERO_MODULES, HeroModuleData } from './heroModules';
import { HeroModule } from './HeroModule';

interface HeroModuleNetworkProps {
  isCylinderHovered: boolean;
  isCoreActive: boolean;
  scrollPhase: number;
  hoveredModuleId: string | null;
  onHoverModule: (id: string | null) => void;
  onClickModule: (module: HeroModuleData) => void;
  // Screen size breakpoint indicator
  isMobileOrTablet: boolean;
}

export const HeroModuleNetwork: React.FC<HeroModuleNetworkProps> = ({
  isCylinderHovered,
  isCoreActive,
  scrollPhase,
  hoveredModuleId,
  onHoverModule,
  onClickModule,
  isMobileOrTablet,
}) => {
  // Determine module visibility state
  // Hover state shows 3 initial capabilities
  // Core activation or scrollPhase >= 2 shows all 7 modules
  const showFullEcosystem = isCoreActive || scrollPhase >= 2;
  const showInitialPreview = isCylinderHovered || scrollPhase >= 1 || showFullEcosystem;

  const visibleModules = HERO_MODULES.filter((mod) => {
    if (showFullEcosystem) return true;
    if (showInitialPreview && mod.initialPreview) return true;
    return false;
  });

  // Mobile / Tablet presentation: clean accessible cards below cylinder
  if (isMobileOrTablet) {
    if (!showInitialPreview && !showFullEcosystem) {
      return null;
    }

    return (
      <div className="w-full mt-6 px-2">
        <div className="flex items-center justify-between mb-3 px-1">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
            {showFullEcosystem
              ? 'Connected Supply Ecosystem (7 Modules Active)'
              : 'Interactive Capabilities (Tap to Explore)'}
          </span>
          <span className="text-[11px] font-medium text-rose-600 bg-rose-50 px-2 py-0.5 rounded-full">
            {showFullEcosystem ? 'Core Active' : 'Touch Enabled'}
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
          {(showFullEcosystem ? HERO_MODULES : HERO_MODULES.slice(0, 3)).map((module) => (
            <HeroModule
              key={module.id}
              module={module}
              layoutMode="grid"
              isHovered={hoveredModuleId === module.id}
              onHover={onHoverModule}
              onClick={onClickModule}
            />
          ))}
        </div>
      </div>
    );
  }

  // Desktop orbital positioning around 3D cylinder canvas
  return (
    <div className="absolute inset-0 pointer-events-none z-20 overflow-visible">
      {/* SVG Connecting Telemetry Lines radiating from Cylinder Surroundings */}
      <svg
        className="absolute inset-0 w-full h-full pointer-events-none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="lineGradRed" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#f43f5e" stopOpacity="0.8" />
            <stop offset="100%" stopColor="#f43f5e" stopOpacity="0.1" />
          </linearGradient>
          <linearGradient id="lineGradOrange" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#f97316" stopOpacity="0.8" />
            <stop offset="100%" stopColor="#f97316" stopOpacity="0.1" />
          </linearGradient>
          <linearGradient id="lineGradAmber" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#d97706" stopOpacity="0.8" />
            <stop offset="100%" stopColor="#d97706" stopOpacity="0.1" />
          </linearGradient>
        </defs>

        {visibleModules.map((mod) => {
          // Precise physical anchor calculation
          const centerX = 50;
          const centerY = 50;
          const isLeft = mod.orbitalOffset.x < 0;

          // Card connection port (on the side facing the cylinder)
          const cardAnchorX = centerX + mod.orbitalOffset.x + (isLeft ? 9.5 : -9.5);
          const cardAnchorY = centerY + mod.orbitalOffset.y;

          // External connection port strictly outside the cylinder body
          const cylAnchorX = centerX + (isLeft ? -13.5 : 13.5);
          const cylAnchorY = centerY + mod.orbitalOffset.y * 0.42;

          const isCurrentHovered = hoveredModuleId === mod.id;

          const strokeColor =
            mod.accent === 'red'
              ? isCurrentHovered ? '#e11d48' : '#f87171'
              : mod.accent === 'orange'
              ? isCurrentHovered ? '#ea580c' : '#fb923c'
              : mod.accent === 'emerald'
              ? isCurrentHovered ? '#059669' : '#34d399'
              : isCurrentHovered ? '#ea580c' : '#f97316';

          // Smooth organic cubic bezier control points
          const deltaX = Math.abs(cylAnchorX - cardAnchorX);
          const c1X = isLeft ? cardAnchorX + deltaX * 0.45 : cardAnchorX - deltaX * 0.45;
          const c1Y = cardAnchorY;
          const c2X = isLeft ? cylAnchorX - deltaX * 0.35 : cylAnchorX + deltaX * 0.35;
          const c2Y = cylAnchorY;

          const pathData = `M ${cardAnchorX}% ${cardAnchorY}% C ${c1X}% ${c1Y}%, ${c2X}% ${c2Y}%, ${cylAnchorX}% ${cylAnchorY}%`;

          return (
            <g key={`line-${mod.id}`} className="transition-all duration-300">
              {/* Soft glow halo when hovered */}
              {isCurrentHovered && (
                <path
                  d={pathData}
                  fill="none"
                  stroke={strokeColor}
                  strokeWidth="4.5"
                  strokeOpacity="0.4"
                  strokeLinecap="round"
                />
              )}
              {/* Main technical connection trace */}
              <path
                d={pathData}
                fill="none"
                stroke={strokeColor}
                strokeWidth={isCurrentHovered ? '2.5' : '1.8'}
                strokeDasharray={isCurrentHovered ? 'none' : '4 3'}
                strokeOpacity={isCurrentHovered ? 0.95 : 0.65}
                strokeLinecap="round"
              />
              {/* Anchor dot on card port */}
              <circle
                cx={`${cardAnchorX}%`}
                cy={`${cardAnchorY}%`}
                r={isCurrentHovered ? '4' : '2.8'}
                fill={strokeColor}
                className="transition-all duration-200"
              />
              {/* Glowing anchor dot on cylinder surface */}
              <circle
                cx={`${cylAnchorX}%`}
                cy={`${cylAnchorY}%`}
                r={isCurrentHovered ? '4.5' : '3'}
                fill={strokeColor}
                className="transition-all duration-200"
              />
              {/* Pulse ripple on cylinder anchor */}
              <circle
                cx={`${cylAnchorX}%`}
                cy={`${cylAnchorY}%`}
                r="6"
                fill="none"
                stroke={strokeColor}
                strokeWidth="1"
                strokeOpacity={isCurrentHovered ? 0.7 : 0.3}
              />
            </g>
          );
        })}
      </svg>

      {/* Render Active Orbital Module Cards */}
      {visibleModules.map((module, index) => {
        const isHovered = hoveredModuleId === module.id;
        // Position card centered at offset
        const leftPercent = 50 + module.orbitalOffset.x;
        const topPercent = 50 + module.orbitalOffset.y;
        const staggerDelayMs = 120 + index * 90;

        return (
          <div
            key={module.id}
            className="pointer-events-auto transition-all duration-500 ease-out animate-in fade-in zoom-in-90 slide-in-from-bottom-2"
            style={{
              position: 'absolute',
              left: `${leftPercent}%`,
              top: `${topPercent}%`,
              transform: 'translate(-50%, -50%)',
              animationDuration: '500ms',
              animationDelay: `${staggerDelayMs}ms`,
              animationFillMode: 'backwards',
            }}
          >
            <HeroModule
              module={module}
              layoutMode="absolute"
              isHovered={isHovered}
              onHover={onHoverModule}
              onClick={onClickModule}
            />
          </div>
        );
      })}
    </div>
  );
};
