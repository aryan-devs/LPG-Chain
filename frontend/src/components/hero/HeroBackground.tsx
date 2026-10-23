import React from 'react';

export interface ScenarioData {
  id: string;
  name: string;
  image: string;
  caption: string;
}

export const SCENARIOS: ScenarioData[] = [
  {
    id: 'home-delivery',
    name: 'Home Delivery',
    image: '/assets/lpg_hero_env.jpg',
    caption: 'Doorstep cylinder delivery & safety inspection',
  },
  {
    id: 'residential-area',
    name: 'Residential Area',
    image: '/assets/lpg_residential_truck.jpg',
    caption: 'Branded delivery vehicle navigating suburban sectors',
  },
  {
    id: 'distributor-yard',
    name: 'Distributor Yard',
    image: '/assets/lpg_distributor_yard.jpg',
    caption: 'Bottling depot weighbridge staging & stock management',
  },
  {
    id: 'city-view',
    name: 'City View',
    image: '/assets/lpg_city_delivery.jpg',
    caption: 'Fleet transit across metropolitan expressways',
  },
];

interface HeroBackgroundProps {
  activeScenarioIndex?: number;
}

export const HeroBackground: React.FC<HeroBackgroundProps> = ({
  activeScenarioIndex = 0,
}) => {
  return (
    <div className="absolute inset-0 pointer-events-none overflow-hidden select-none">
      {/* ── Photographic Multi-Scenario Backdrop (100% Full-Width Hero Background) ── */}
      <div className="absolute inset-0 w-full h-full overflow-hidden">
        {SCENARIOS.map((scenario, index) => (
          <img
            key={scenario.id}
            src={scenario.image}
            alt={scenario.name}
            className={`absolute inset-0 w-full h-full object-cover object-center transform scale-105 transition-opacity duration-1000 ease-in-out ${
              index === activeScenarioIndex ? 'opacity-35 lg:opacity-45' : 'opacity-0'
            }`}
          />
        ))}

        {/* Strong white/light gradient overlay on the LEFT: ensures headline, description, and CTAs remain crisp & legible while right side retains photographic atmosphere */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-50 via-slate-50/85 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-50/90 via-transparent to-slate-50/70" />
      </div>

      {/* ── Soft warm radial ambient glow behind the cylinder stand ── */}
      <div className="absolute top-1/2 right-[12%] xl:right-[15%] -translate-y-1/2 w-[540px] h-[540px] rounded-full bg-gradient-to-tr from-rose-200/20 via-amber-100/30 to-rose-100/20 blur-3xl opacity-80" />

      {/* ── Subtle floor contact shadow vignette ── */}
      <div className="absolute bottom-6 right-[14%] xl:right-[17%] w-[440px] h-14 bg-slate-900/10 rounded-full blur-xl transform scale-y-50" />

      {/* ── Technical standard specification markers (restrained & authentic) ── */}
      <div className="absolute top-6 left-6 text-left opacity-35 hidden sm:block">
        <div className="text-[10px] font-mono uppercase tracking-widest text-slate-400">
          CYL-SPEC // 14.2 KG DOMESTIC
        </div>
        <div className="text-[9px] font-mono text-slate-400">
          EN 1442 / IS 3196 STANDARD
        </div>
      </div>
    </div>
  );
};
