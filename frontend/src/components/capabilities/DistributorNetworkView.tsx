import React, { useState } from 'react';
import { 
  Network, 
  Building2, 
  MapPin, 
  CheckCircle2, 
  Truck, 
  Gauge, 
  Search 
} from 'lucide-react';
import { CapabilityPageShell } from './CapabilityPageShell';

interface DistributorNetworkViewProps {
  onBackToHome: () => void;
  onOpenBooking: () => void;
  onNavigateToTracking: () => void;
}

export const DistributorNetworkView: React.FC<DistributorNetworkViewProps> = ({
  onBackToHome,
  onOpenBooking,
  onNavigateToTracking,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  const SERVICE_AREAS = [
    {
      region: 'Western Metropolitan Zone',
      hubs: 342,
      activeTrucks: 1840,
      dailyVolume: '28,400 Cylinders',
      coverage: '99.4%',
      status: 'High Demand',
    },
    {
      region: 'Northern Industrial Belt',
      hubs: 288,
      activeTrucks: 1420,
      dailyVolume: '22,100 Cylinders',
      coverage: '98.8%',
      status: 'Normal Flow',
    },
    {
      region: 'Southern Coastal Corridor',
      hubs: 310,
      activeTrucks: 1590,
      dailyVolume: '24,600 Cylinders',
      coverage: '99.1%',
      status: 'Optimal',
    },
    {
      region: 'Central Suburban Matrix',
      hubs: 308,
      activeTrucks: 1610,
      dailyVolume: '21,900 Cylinders',
      coverage: '98.5%',
      status: 'Optimal',
    },
  ];

  const DEPOT_HUBS = [
    {
      id: 'HUB-0491',
      name: 'Adarsh Regional Agency',
      location: 'Suburban West Ward (Zone 4)',
      inventory: '3,840 Domestic / 480 Commercial',
      truckFleet: '28 Active Trucks',
      status: 'Active • Dispatching',
    },
    {
      id: 'HUB-0312',
      name: 'Pragati Energy Gas Distribution',
      location: 'Metro South Central Depot',
      inventory: '5,120 Domestic / 720 Commercial',
      truckFleet: '36 Active Trucks',
      status: 'Active • Loading Carousel',
    },
    {
      id: 'HUB-0884',
      name: 'Sagar Fuel & Logistics Hub',
      location: 'Coastal Industrial Gateway',
      inventory: '4,450 Domestic / 610 Commercial',
      truckFleet: '32 Active Trucks',
      status: 'Active • Dispatching',
    },
  ];

  const filteredHubs = DEPOT_HUBS.filter(
    (hub) =>
      hub.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      hub.location.toLowerCase().includes(searchQuery.toLowerCase()) ||
      hub.id.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <CapabilityPageShell
      title="Distributor Network"
      subtitle="Connected Distributor Network"
      description="Unified enterprise network connecting over 1,200 regional bottling depots, authorized private agency hubs, and neighborhood delivery vehicle fleets under a single real-time operations console."
      badge="1,200+ NATIONWIDE HUBS"
      icon={Network}
      accentColor="#3b82f6"
      onBackToHome={onBackToHome}
      onOpenBooking={onOpenBooking}
      onNavigateToTracking={onNavigateToTracking}
    >
      <div className="space-y-8">
        {/* ── Section 1: Distributor Overview ── */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">1. Distributor Overview</h2>
              <p className="text-xs text-slate-500">Aggregate capacity and throughput across the nationwide hub ecosystem</p>
            </div>
            <span className="text-xs font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-full border border-blue-200 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
              1,248 Certified Hubs Online
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Active Regional Hubs</span>
                <Building2 size={16} className="text-blue-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">1,248</div>
              <div className="text-[11px] font-bold text-emerald-600 mt-1">Across 28 States & UTs</div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Active Delivery Fleet</span>
                <Truck size={16} className="text-emerald-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">6,460</div>
              <div className="text-[11px] font-medium text-slate-500 mt-1">Live GPS Beacon Transmitting</div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Daily Bottling Throughput</span>
                <Gauge size={16} className="text-purple-600" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">97,000</div>
              <div className="text-[11px] font-medium text-slate-500 mt-1">Cylinders verified / 24h</div>
            </div>

            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Consumer Rating</span>
                <CheckCircle2 size={16} className="text-amber-500" />
              </div>
              <div className="text-2xl sm:text-3xl font-black text-slate-900">4.92 / 5</div>
              <div className="text-[11px] font-bold text-amber-600 mt-1">140k+ verified reviews</div>
            </div>
          </div>
        </div>

        {/* ── Section 2: Service Areas ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">2. Service Areas</h2>
            <p className="text-xs text-slate-500">Regional hub operational clusters and dynamic geofenced distribution corridors</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {SERVICE_AREAS.map((area) => (
              <div key={area.region} className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
                      <MapPin size={16} />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-slate-900">{area.region}</h3>
                      <span className="text-xs text-slate-500">{area.hubs} Hubs &bull; {area.activeTrucks} Active Fleet</span>
                    </div>
                  </div>
                  <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700">
                    {area.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 pt-1 text-xs">
                  <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-400 block text-[10px] uppercase">Daily Flow</span>
                    <span className="font-bold text-slate-800 text-sm">{area.dailyVolume}</span>
                  </div>
                  <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                    <span className="text-slate-400 block text-[10px] uppercase">Ward Coverage</span>
                    <span className="font-bold text-emerald-600 text-sm">{area.coverage} Coverage</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ── Section 3: Distributor Status ── */}
        <div>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
            <div>
              <h2 className="text-lg font-black text-slate-900 tracking-tight">3. Distributor Status</h2>
              <p className="text-xs text-slate-500">Real-time inventory levels and active dispatch capacity per regional agency</p>
            </div>

            {/* Quick search input */}
            <div className="relative w-full sm:w-64">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search hub or zone..."
                className="w-full bg-white border border-slate-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-slate-900 focus:outline-none focus:border-blue-500 transition-colors shadow-sm"
              />
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm divide-y divide-slate-100 overflow-hidden">
            {filteredHubs.map((hub) => (
              <div key={hub.id} className="p-4 sm:p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 text-xs hover:bg-slate-50/60 transition-colors">
                <div className="flex items-start gap-3">
                  <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 border border-blue-100 flex items-center justify-center flex-shrink-0 font-bold">
                    <Building2 size={18} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-slate-900">{hub.name}</span>
                      <span className="font-mono text-[10px] font-bold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded">
                        {hub.id}
                      </span>
                    </div>
                    <div className="text-slate-500 mt-0.5">{hub.location}</div>
                    <div className="text-slate-600 font-medium mt-1">
                      <strong>Inventory:</strong> {hub.inventory} &bull; <strong>Fleet:</strong> {hub.truckFleet}
                    </div>
                  </div>
                </div>

                <span className="font-bold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200 self-start md:self-center flex-shrink-0">
                  {hub.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* ── Section 4: Network Activity ── */}
        <div>
          <div className="mb-4">
            <h2 className="text-lg font-black text-slate-900 tracking-tight">4. Network Activity Feed</h2>
            <p className="text-xs text-slate-500">Live operational events logged across depot weighbridges and fleet gateways</p>
          </div>

          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
            {[
              { time: '01:25 AM', hub: 'Depot Hub 04', text: 'Batch replenishment of 240 domestic 14.2kg cylinders received from Bottling Plant 02.' },
              { time: '01:12 AM', hub: 'South Metro Hub 11', text: 'Morning delivery truck dispatch (44 vehicles) cleared with zero tare variance flags.' },
              { time: '12:54 AM', hub: 'Central Depot 08', text: 'Automated weighbridge recalibration routine completed successfully. Certificate hash generated.' },
            ].map((ev) => (
              <div key={ev.time + ev.hub} className="flex items-start gap-3 text-xs pb-3 border-b border-slate-100 last:border-0 last:pb-0">
                <span className="w-2 h-2 rounded-full bg-blue-500 mt-1.5 flex-shrink-0" />
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900">{ev.hub}</span>
                    <span className="text-[10px] text-slate-400 font-mono">{ev.time}</span>
                  </div>
                  <p className="text-slate-600 mt-0.5">{ev.text}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </CapabilityPageShell>
  );
};
