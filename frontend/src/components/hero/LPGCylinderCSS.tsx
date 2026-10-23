import React from 'react';

interface LPGCylinderCSSProps {
  isHovered?: boolean;
  className?: string;
}

export const LPGCylinderCSS: React.FC<LPGCylinderCSSProps> = ({
  isHovered = false,
  className = '',
}) => {
  return (
    <div
      className={`relative select-none transition-transform duration-500 ease-out ${isHovered ? 'scale-[1.03]' : 'scale-100'} ${className}`}
      style={{ width: 240, height: 440 }}
    >
      <svg
        viewBox="0 0 240 440"
        xmlns="http://www.w3.org/2000/svg"
        width="240"
        height="440"
        style={{ overflow: 'visible', filter: 'drop-shadow(0 28px 56px rgba(0,0,0,0.38)) drop-shadow(0 8px 20px rgba(183,28,28,0.22))' }}
      >
        <defs>
          <radialGradient id="bodyGrad" cx="32%" cy="38%" r="68%" fx="26%" fy="30%">
            <stop offset="0%" stopColor="#ef4444" />
            <stop offset="25%" stopColor="#dc2626" />
            <stop offset="52%" stopColor="#b91c1c" />
            <stop offset="75%" stopColor="#991b1b" />
            <stop offset="100%" stopColor="#7f1d1d" />
          </radialGradient>
          <radialGradient id="domeTopGrad" cx="38%" cy="42%" r="62%" fx="28%" fy="32%">
            <stop offset="0%" stopColor="#f87171" />
            <stop offset="28%" stopColor="#dc2626" />
            <stop offset="62%" stopColor="#b91c1c" />
            <stop offset="100%" stopColor="#7f1d1d" />
          </radialGradient>
          <radialGradient id="domeBotGrad" cx="38%" cy="32%" r="65%" fx="28%" fy="28%">
            <stop offset="0%" stopColor="#dc2626" />
            <stop offset="38%" stopColor="#b91c1c" />
            <stop offset="78%" stopColor="#991b1b" />
            <stop offset="100%" stopColor="#7f1d1d" />
          </radialGradient>
          <linearGradient id="footRingGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#6b1414" />
            <stop offset="18%" stopColor="#991b1b" />
            <stop offset="50%" stopColor="#7f1d1d" />
            <stop offset="82%" stopColor="#991b1b" />
            <stop offset="100%" stopColor="#6b1414" />
          </linearGradient>
          <linearGradient id="collarGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#dc2626" />
            <stop offset="50%" stopColor="#b91c1c" />
            <stop offset="100%" stopColor="#991b1b" />
          </linearGradient>
          <radialGradient id="brassGrad" cx="32%" cy="28%" r="72%" fx="26%" fy="22%">
            <stop offset="0%" stopColor="#fde68a" />
            <stop offset="28%" stopColor="#d4af37" />
            <stop offset="62%" stopColor="#b8860b" />
            <stop offset="100%" stopColor="#92650a" />
          </radialGradient>
          <linearGradient id="specularGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="rgba(0,0,0,0)" />
            <stop offset="18%" stopColor="rgba(255,255,255,0.14)" />
            <stop offset="32%" stopColor="rgba(255,255,255,0.28)" />
            <stop offset="48%" stopColor="rgba(255,255,255,0.06)" />
            <stop offset="100%" stopColor="rgba(0,0,0,0.18)" />
          </linearGradient>
          <linearGradient id="seamGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#5a0e0e" />
            <stop offset="25%" stopColor="#7f1d1d" />
            <stop offset="50%" stopColor="#5a0e0e" />
            <stop offset="75%" stopColor="#7f1d1d" />
            <stop offset="100%" stopColor="#5a0e0e" />
          </linearGradient>
          <linearGradient id="sealGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#38bdf8" />
            <stop offset="50%" stopColor="#0284c7" />
            <stop offset="100%" stopColor="#075985" />
          </linearGradient>
          <clipPath id="bodyClip">
            <rect x="42" y="90" width="156" height="300" />
          </clipPath>
        </defs>

        {/* Ground shadow */}
        <ellipse cx="120" cy="432" rx="92" ry="12" fill="rgba(0,0,0,0.22)" />

        {/* === FOOT RING / BASE STAND === */}
        <rect x="40" y="378" width="160" height="36" rx="5" fill="url(#footRingGrad)" />
        <rect x="40" y="378" width="160" height="8" rx="3" fill="rgba(255,255,255,0.06)" />
        {/* Ventilation cutout slots */}
        {[-46,-20,6,32].map((ox, i) => (
          <rect key={i} x={120 + ox} y={388} width={14} height={14} rx={2} fill="#3a0808" />
        ))}
        <ellipse cx="120" cy="414" rx="80" ry="6" fill="#6b1414" />
        <ellipse cx="120" cy="378" rx="80" ry="6" fill="#991b1b" />

        {/* === MAIN CYLINDER BODY === */}
        <rect x="42" y="90" width="156" height="288" fill="url(#bodyGrad)" />
        {/* Specular highlight */}
        <rect x="42" y="90" width="156" height="288" fill="url(#specularGrad)" />
        {/* Left & right edge shadows */}
        <rect x="42" y="90" width="16" height="288" fill="rgba(0,0,0,0.22)" />
        <rect x="182" y="90" width="16" height="288" fill="rgba(0,0,0,0.24)" />
        {/* Bottom darkening */}
        <rect x="42" y="348" width="156" height="30" fill="rgba(0,0,0,0.16)" />

        {/* === WELD SEAM === */}
        <rect x="42" y="232" width="156" height="9" fill="url(#seamGrad)" />
        <rect x="42" y="234" width="156" height="2" fill="rgba(255,255,255,0.07)" />

        {/* === LOWER DOME === */}
        <ellipse cx="120" cy="378" rx="78" ry="20" fill="url(#domeBotGrad)" />

        {/* === UPPER DOME / SHOULDER === */}
        <ellipse cx="120" cy="90" rx="78" ry="34" fill="url(#domeTopGrad)" />
        {/* Dome specular */}
        <ellipse cx="96" cy="74" rx="32" ry="16" fill="rgba(255,255,255,0.11)" />

        {/* "15.6" weight stamp */}
        <text x="120" y="86" textAnchor="middle"
          fontFamily="Arial Black, Roboto, sans-serif" fontWeight="900"
          fontSize="14" fill="rgba(255,255,255,0.60)" letterSpacing="1">
          15.6
        </text>

        {/* === BRANDING: Flame icon + LPG-Chain === */}
        <g transform="translate(120, 192)">
          <path d="M0,-40 C17,-19 32,1 30,24 C28,41 15,48 0,48 C-15,48 -28,41 -30,24 C-32,1 -17,-19 0,-40 Z" fill="white" />
          <path d="M0,-7 C9,9 14,20 13,33 C11,42 -3,44 0,35 C4,24 -4,13 0,-7 Z" fill="#b91c1c" />
        </g>

        <text x="120" y="268" textAnchor="middle"
          fontFamily="Arial Black, Impact, Roboto, sans-serif" fontWeight="900"
          fontSize="24" fill="white" letterSpacing="0.5">
          LPG-Chain
        </text>

        <text x="120" y="288" textAnchor="middle"
          fontFamily="Arial, Roboto, sans-serif" fontWeight="600"
          fontSize="8" fill="rgba(255,255,255,0.72)" letterSpacing="2.5">
          SAFE • SMART • CONNECTED
        </text>

        {/* === PROTECTIVE COLLAR / CARRY RING === */}
        <rect x="46" y="56" width="148" height="28" rx="7" fill="url(#collarGrad)" />
        <rect x="46" y="56" width="148" height="9" rx="4" fill="rgba(255,255,255,0.09)" />
        {/* Stay pillars */}
        <rect x="64" y="62" width="9" height="18" rx="3" fill="#991b1b" />
        <rect x="116" y="62" width="9" height="18" rx="3" fill="#991b1b" />
        <rect x="168" y="62" width="9" height="18" rx="3" fill="#991b1b" />
        <ellipse cx="120" cy="57" rx="74" ry="7" fill="#dc2626" />

        {/* === CARRY HANDLE / HAND RING === */}
        <path d="M 58,42 Q 58,16 120,16 Q 182,16 182,42"
          fill="none" stroke="#b91c1c" strokeWidth="11" strokeLinecap="round" />
        <path d="M 58,42 Q 58,16 120,16 Q 182,16 182,42"
          fill="none" stroke="rgba(255,255,255,0.13)" strokeWidth="4" strokeLinecap="round" />
        <rect x="52" y="38" width="12" height="24" rx="6" fill="#991b1b" />
        <rect x="176" y="38" width="12" height="24" rx="6" fill="#991b1b" />

        {/* === BRASS VALVE ASSEMBLY === */}
        {/* Hex base */}
        <rect x="96" y="14" width="48" height="24" rx="5" fill="url(#brassGrad)" />
        {/* Stem */}
        <rect x="106" y="-4" width="28" height="20" rx="5" fill="url(#brassGrad)" />
        {/* Knob cap */}
        <ellipse cx="120" cy="-5" rx="20" ry="8" fill="#d4af37" />
        <ellipse cx="115" cy="-7" rx="9" ry="3.5" fill="rgba(255,255,255,0.22)" />
        {/* Outlet spout */}
        <rect x="144" y="18" width="26" height="9" rx="4.5" fill="url(#brassGrad)" />
        {/* Blue tamper cap */}
        <rect x="166" y="14" width="16" height="16" rx="5" fill="url(#sealGrad)" />
        <rect x="168" y="15" width="7" height="5" rx="2" fill="rgba(255,255,255,0.28)" />
      </svg>
    </div>
  );
};
