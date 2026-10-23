import { useState, useEffect, useCallback, useRef } from 'react';
import { HeroModuleData } from './heroModules';

export function isWebGLAvailable(): boolean {
  if (typeof window === 'undefined') return false;
  try {
    const canvas = document.createElement('canvas');
    return !!(
      window.WebGLRenderingContext &&
      (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
    );
  } catch {
    return false;
  }
}

export function useReducedMotion(): boolean {
  const [reducedMotion, setReducedMotion] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    setReducedMotion(mediaQuery.matches);

    const listener = (event: MediaQueryListEvent) => {
      setReducedMotion(event.matches);
    };

    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', listener);
      return () => mediaQuery.removeEventListener('change', listener);
    } else {
      mediaQuery.addListener(listener);
      return () => mediaQuery.removeListener(listener);
    }
  }, []);

  return reducedMotion;
}

export interface HeroInteractionState {
  isCylinderHovered: boolean;
  setIsCylinderHovered: (hovered: boolean) => void;
  isCoreActive: boolean;
  setIsCoreActive: (active: boolean | ((prev: boolean) => boolean)) => void;
  hoveredModuleId: string | null;
  setHoveredModuleId: (id: string | null) => void;
  scrollPhase: number; // 0 (initial), 1 (approaching), 2 (core activating), 3 (full ecosystem)
  scrollProgress: number; // 0.0 to 1.0
  normalizedMouse: { x: number; y: number };
  setNormalizedMouse: (pos: { x: number; y: number }) => void;
  reducedMotion: boolean;
  webGLSupported: boolean;
  activeInspectModule: HeroModuleData | null;
  setActiveInspectModule: (module: HeroModuleData | null) => void;
  isBookingOpen: boolean;
  setIsBookingOpen: (open: boolean) => void;
  handleCylinderClick: () => void;
}

export function useHeroInteraction(containerRef: React.RefObject<HTMLElement>): HeroInteractionState {
  const [isCylinderHovered, setIsCylinderHovered] = useState(false);
  const [isCoreActive, setIsCoreActive] = useState(false);
  const [hoveredModuleId, setHoveredModuleId] = useState<string | null>(null);
  const [scrollProgress, setScrollProgress] = useState(0);
  const [scrollPhase, setScrollPhase] = useState(0);
  const [normalizedMouse, setNormalizedMouse] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [webGLSupported, setWebGLSupported] = useState(true);
  const [activeInspectModule, setActiveInspectModule] = useState<HeroModuleData | null>(null);
  const [isBookingOpen, setIsBookingOpen] = useState(false);

  const reducedMotion = useReducedMotion();
  const lastScrollProgressRef = useRef(0);

  // Check WebGL availability on mount
  useEffect(() => {
    setWebGLSupported(isWebGLAvailable());
  }, []);

  // Smooth scroll discovery handling
  useEffect(() => {
    const handleScroll = () => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const totalHeroHeight = rect.height;
      const scrolledPastTop = Math.max(0, -rect.top);
      const progress = Math.min(1, Math.max(0, scrolledPastTop / (totalHeroHeight * 0.65)));

      lastScrollProgressRef.current = progress;
      setScrollProgress(progress);

      // Phase breakdown:
      // Phase 0: 0.00 - 0.15 (Initial cylinder state)
      // Phase 1: 0.15 - 0.35 (Cylinder tilts, core begins activating)
      // Phase 2: 0.35 - 0.65 (Internal tech layer visible, data lines emerge)
      // Phase 3: 0.65 - 1.00 (Full 7-module ecosystem revealed)
      let phase = 0;
      if (progress >= 0.65) phase = 3;
      else if (progress >= 0.35) phase = 2;
      else if (progress >= 0.15) phase = 1;
      setScrollPhase(phase);

      // Auto-activate core when scrolling into phase 2+
      if (phase >= 2) {
        setIsCoreActive(true);
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, [containerRef]);

  // Cylinder click handler (activates Core + spring reaction)
  const handleCylinderClick = useCallback(() => {
    setIsCoreActive((prev) => !prev);
  }, []);

  return {
    isCylinderHovered,
    setIsCylinderHovered,
    isCoreActive,
    setIsCoreActive,
    hoveredModuleId,
    setHoveredModuleId,
    scrollPhase,
    scrollProgress,
    normalizedMouse,
    setNormalizedMouse,
    reducedMotion,
    webGLSupported,
    activeInspectModule,
    setActiveInspectModule,
    isBookingOpen,
    setIsBookingOpen,
    handleCylinderClick,
  };
}
