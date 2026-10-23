import React from 'react';
import {
  Truck,
  Sparkles,
  FileText,
  Package,
  CheckCircle2,
  Phone,
  ArrowRight,
  User,
  Mail,
  MapPin,
  Flame,
  ExternalLink,
} from 'lucide-react';
import { MapContainer, TileLayer, Marker, Polyline, Tooltip } from 'react-leaflet';
import L from 'leaflet';
import { CustomerOrder, CustomerProfileData, PortalTab } from '../portalTypes';

interface PortalDashboardViewProps {
  profile: CustomerProfileData;
  latestOrder: CustomerOrder | null;
  onNavigateTab: (tab: PortalTab, prompt?: string) => void;
}

// Custom vehicle marker icon
const vanIcon = L.divIcon({
  className: 'lpg-vehicle-marker-wrapper',
  html: `
    <div style="background:#e11d48; width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 10px rgba(225,29,72,0.4); border:2.5px solid white;">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"/><path d="M15 18H9"/><path d="M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"/><circle cx="17" cy="18" r="2"/><circle cx="7" cy="18" r="2"/></svg>
    </div>
  `,
  iconSize: [34, 34],
  iconAnchor: [17, 17],
});

// Custom house destination marker icon
const homeIcon = L.divIcon({
  className: 'lpg-vehicle-marker-wrapper',
  html: `
    <div style="background:#e11d48; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 10px rgba(0,0,0,0.25); border:2.5px solid white;">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
    </div>
  `,
  iconSize: [32, 32],
  iconAnchor: [16, 16],
});

export const PortalDashboardView: React.FC<PortalDashboardViewProps> = ({
  profile,
  latestOrder,
  onNavigateTab,
}) => {
  const firstName = profile.name.split(' ')[0] || 'Sneha';

  const quickActionPrompts = [
    'How to book a cylinder?',
    'Safety guidelines',
    'Check availability',
    'Change delivery address',
    'Billing and payment help',
    'Cylinder usage tips',
  ];

  // Coordinates for the Pune route preview
  const driverPos: [number, number] = [18.5304, 73.8467];
  const homePos: [number, number] = [18.5204, 73.8567];
  const routePolyline: [number, number][] = [
    [18.5304, 73.8467],
    [18.5274, 73.8497],
    [18.5244, 73.8527],
    [18.5204, 73.8567],
  ];

  return (
    <div className="flex flex-col gap-6 max-w-7xl mx-auto w-full pb-10">
      {/* ── 1. Personalized Welcome Banner ── */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-amber-50 via-orange-50/70 to-rose-50/80 border border-slate-200/80 shadow-sm p-6 sm:p-8 lg:p-10 flex flex-col md:flex-row items-center justify-between gap-6 min-h-[200px]">
        {/* Real Household Kitchen Background Asset */}
        <img
          src="/assets/household_kitchen_banner.jpg"
          alt="Modern Household Kitchen"
          className="absolute inset-0 w-full h-full object-cover object-[center_35%] pointer-events-none transition-opacity duration-500"
          onError={(e) => {
            // Graceful fallback to warm background gradient if image fails to load
            (e.target as HTMLElement).style.display = 'none';
          }}
        />

        {/* Subtle Light Gradient Overlay ensuring high contrast & readability */}
        <div className="absolute inset-0 bg-gradient-to-r from-white/95 via-white/85 to-white/40 md:to-white/20 pointer-events-none" />
        <div className="absolute inset-0 bg-gradient-to-t from-white/70 via-transparent to-transparent pointer-events-none md:hidden" />

        {/* Soft decorative glow circles */}
        <div className="absolute top-0 right-1/4 w-80 h-80 bg-rose-200/15 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-10 w-60 h-60 bg-amber-200/15 rounded-full blur-2xl pointer-events-none" />

        {/* Left greeting text */}
        <div className="relative z-10 flex flex-col gap-3 max-w-xl text-center md:text-left">
          <div className="flex flex-col">
            <span className="text-xs sm:text-sm uppercase tracking-wider font-bold text-rose-600">
              Welcome back,
            </span>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-900 tracking-tight mt-0.5">
              {firstName}!
            </h1>
            <p className="text-sm sm:text-base text-slate-700 mt-1 font-medium">
              Your LPG needs, made simple.
            </p>
          </div>

          {/* Badges: Safe, Reliable, Hassle-free */}
          <div className="flex flex-wrap items-center justify-center md:justify-start gap-2.5 mt-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/95 border border-slate-200/80 text-xs font-bold text-slate-700 shadow-xs backdrop-blur-xs">
              <span className="text-rose-500">🛡</span> Safe
            </span>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/95 border border-slate-200/80 text-xs font-bold text-slate-700 shadow-xs backdrop-blur-xs">
              <span className="text-rose-500">🚚</span> Reliable
            </span>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/95 border border-slate-200/80 text-xs font-bold text-slate-700 shadow-xs backdrop-blur-xs">
              <span className="text-emerald-500">🌱</span> Hassle-free
            </span>
          </div>
        </div>

        {/* Right side Cylinder visual and tagline */}
        <div className="relative z-10 flex flex-col items-center md:items-end gap-2 flex-shrink-0">
          <div className="flex items-center gap-4 bg-white/80 md:bg-transparent backdrop-blur-xs md:backdrop-blur-none p-3 md:p-0 rounded-2xl border border-white/60 md:border-transparent shadow-xs md:shadow-none">
            <div className="relative w-28 sm:w-36 h-28 sm:h-36 flex items-center justify-center">
              <img
                src="/assets/lpg_cylinder_transparent.webp"
                alt="Domestic LPG Cylinder"
                className="w-full h-full object-contain filter drop-shadow-xl hover:scale-105 transition-transform"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = '/assets/lpg_cylinder_transparent.png';
                }}
              />
            </div>
            <div className="hidden sm:flex flex-col text-right">
              <span className="font-serif italic text-lg lg:text-xl font-bold text-slate-900 leading-tight">
                Clean Cooking
              </span>
              <span className="font-serif italic text-lg lg:text-xl font-bold text-rose-600 leading-tight flex items-center justify-end gap-1">
                Happier Homes <span className="text-rose-600 not-italic">♥</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ── 2. Four Quick Action Cards ── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Quick Action 1: Book a Cylinder */}
        <div
          role="button"
          tabIndex={0}
          onClick={() => onNavigateTab('book')}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onNavigateTab('book');
          }}
          className="group p-5 rounded-2xl bg-white border border-slate-200/90 shadow-sm hover:shadow-md hover:border-rose-200 transition-all cursor-pointer flex flex-col justify-between"
        >
          <div className="flex items-start justify-between">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center shadow-inner group-hover:scale-105 transition-transform">
              <Flame size={22} className="fill-rose-600/20" />
            </div>
            <div className="w-8 h-8 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center group-hover:bg-rose-600 group-hover:text-white transition-colors">
              <ArrowRight size={15} />
            </div>
          </div>
          <div className="mt-4">
            <h2 className="text-base font-bold text-slate-900 group-hover:text-rose-600 transition-colors">
              Book a Cylinder
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Order a new cylinder in a few clicks.
            </p>
          </div>
        </div>

        {/* Quick Action 2: Track Delivery */}
        <div
          role="button"
          tabIndex={0}
          onClick={() => onNavigateTab('track')}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onNavigateTab('track');
          }}
          className="group p-5 rounded-2xl bg-white border border-slate-200/90 shadow-sm hover:shadow-md hover:border-rose-200 transition-all cursor-pointer flex flex-col justify-between"
        >
          <div className="flex items-start justify-between">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center shadow-inner group-hover:scale-105 transition-transform">
              <MapPin size={22} />
            </div>
            <div className="w-8 h-8 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center group-hover:bg-rose-600 group-hover:text-white transition-colors">
              <ArrowRight size={15} />
            </div>
          </div>
          <div className="mt-4">
            <h2 className="text-base font-bold text-slate-900 group-hover:text-rose-600 transition-colors">
              Track Delivery
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Track your active delivery in real time.
            </p>
          </div>
        </div>

        {/* Quick Action 3: AI Assistance */}
        <div
          role="button"
          tabIndex={0}
          onClick={() => onNavigateTab('ai')}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onNavigateTab('ai');
          }}
          className="group p-5 rounded-2xl bg-white border border-slate-200/90 shadow-sm hover:shadow-md hover:border-rose-200 transition-all cursor-pointer flex flex-col justify-between"
        >
          <div className="flex items-start justify-between">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center shadow-inner group-hover:scale-105 transition-transform">
              <Sparkles size={22} />
            </div>
            <div className="w-8 h-8 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center group-hover:bg-rose-600 group-hover:text-white transition-colors">
              <ArrowRight size={15} />
            </div>
          </div>
          <div className="mt-4">
            <h2 className="text-base font-bold text-slate-900 group-hover:text-rose-600 transition-colors">
              AI Assistance
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Get instant help for bookings, safety, usage and more.
            </p>
          </div>
        </div>

        {/* Quick Action 4: KYC & Documents */}
        <div
          role="button"
          tabIndex={0}
          onClick={() => onNavigateTab('kyc')}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onNavigateTab('kyc');
          }}
          className="group p-5 rounded-2xl bg-white border border-slate-200/90 shadow-sm hover:shadow-md hover:border-emerald-200 transition-all cursor-pointer flex flex-col justify-between"
        >
          <div className="flex items-start justify-between">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center shadow-inner group-hover:scale-105 transition-transform">
              <FileText size={22} />
            </div>
            <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition-colors">
              <ArrowRight size={15} />
            </div>
          </div>
          <div className="mt-4">
            <h2 className="text-base font-bold text-slate-900 group-hover:text-emerald-600 transition-colors">
              KYC & Documents
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Complete or update your KYC verification.
            </p>
          </div>
        </div>
      </div>

      {/* ── 3. Middle Row: Your Latest Order & Delivery in Progress ── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (7 cols): Your Latest Order */}
        <div className="lg:col-span-7 bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col justify-between">
          <div>
            {/* Card Header */}
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
                  <Package size={18} />
                </div>
                <h2 className="text-base sm:text-lg font-bold text-slate-900">
                  Your Latest Order
                </h2>
              </div>
              <button
                type="button"
                onClick={() => onNavigateTab('history')}
                className="text-xs font-bold text-rose-600 hover:text-rose-700 hover:bg-rose-50 px-3 py-1.5 rounded-full border border-rose-200 transition-colors flex items-center gap-1"
              >
                <span>View All Orders</span>
                <span aria-hidden="true">&rarr;</span>
              </button>
            </div>

            {/* Order Content */}
            {latestOrder ? (
              <div className="pt-6">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  {/* Cylinder Image & Details */}
                  <div className="flex items-center gap-4">
                    <div className="w-16 h-20 flex-shrink-0 flex items-center justify-center">
                      <img
                        src="/assets/lpg_cylinder_transparent.webp"
                        alt="LPG Cylinder"
                        className="max-h-full object-contain filter drop-shadow"
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = '/assets/lpg_cylinder_transparent.png';
                        }}
                      />
                    </div>
                    <div>
                      <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        {latestOrder.capacityKg} KG Domestic
                      </span>
                      <h3 className="text-base sm:text-lg font-black text-slate-900">
                        Domestic LPG Cylinder
                      </h3>
                      <p className="text-xs font-semibold text-slate-500 font-mono mt-0.5">
                        Booking ID: #{latestOrder.id}
                      </p>
                    </div>
                  </div>

                  {/* Status Badge & Expected Time */}
                  <div className="flex flex-col sm:items-end">
                    <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                      {latestOrder.status === 'ASSIGNED'
                        ? 'Assigned to Distributor'
                        : latestOrder.status === 'OUT_FOR_DELIVERY'
                        ? 'Out for Delivery'
                        : latestOrder.status === 'DELIVERED'
                        ? 'Delivered'
                        : 'Confirmed'}
                    </span>
                    <span className="text-[11px] text-slate-400 mt-2">Expected by</span>
                    <span className="text-sm font-black text-slate-900">
                      {latestOrder.expectedDelivery}
                    </span>
                  </div>
                </div>

                {/* 4-Step Progress Line */}
                <div className="mt-8 pt-6 border-t border-slate-100">
                  <div className="relative flex items-center justify-between">
                    {/* Connecting background line */}
                    <div className="absolute top-3.5 left-4 right-4 h-0.5 bg-slate-200 -z-0" />
                    {/* Active green connecting line */}
                    <div className="absolute top-3.5 left-4 w-1/3 h-0.5 bg-emerald-500 -z-0" />

                    {/* Step 1: Confirmed */}
                    <div className="flex flex-col items-center text-center relative z-10">
                      <div className="w-7 h-7 rounded-full bg-emerald-500 text-white flex items-center justify-center shadow-sm">
                        <CheckCircle2 size={16} />
                      </div>
                      <span className="text-xs font-bold text-slate-800 mt-2">Confirmed</span>
                      <span className="text-[10px] text-slate-400">10 Oct, 10:15 AM</span>
                    </div>

                    {/* Step 2: Assigned */}
                    <div className="flex flex-col items-center text-center relative z-10">
                      <div className="w-7 h-7 rounded-full bg-emerald-500 text-white flex items-center justify-center shadow-sm">
                        <CheckCircle2 size={16} />
                      </div>
                      <span className="text-xs font-bold text-slate-800 mt-2">Assigned</span>
                      <span className="text-[10px] text-slate-400">10 Oct, 11:20 AM</span>
                    </div>

                    {/* Step 3: Out for Delivery */}
                    <div className="flex flex-col items-center text-center relative z-10">
                      <div className="w-7 h-7 rounded-full bg-white border-2 border-slate-400 text-slate-400 flex items-center justify-center shadow-sm">
                        <span className="w-2 h-2 rounded-full bg-slate-400" />
                      </div>
                      <span className="text-xs font-bold text-slate-700 mt-2">
                        Out for Delivery
                      </span>
                      <span className="text-[10px] text-slate-400">Expected 2:30 PM</span>
                    </div>

                    {/* Step 4: Delivered */}
                    <div className="flex flex-col items-center text-center relative z-10">
                      <div className="w-7 h-7 rounded-full bg-slate-100 border border-slate-200 text-slate-300 flex items-center justify-center">
                        <span className="w-1.5 h-1.5 rounded-full bg-slate-300" />
                      </div>
                      <span className="text-xs font-medium text-slate-400 mt-2">Delivered</span>
                      <span className="text-[10px] text-slate-400">Pending</span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              /* Empty state if no active booking */
              <div className="py-10 flex flex-col items-center text-center gap-3">
                <div className="w-14 h-14 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400">
                  <Package size={26} />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-800">No Active Bookings</h3>
                  <p className="text-xs text-slate-500 max-w-xs mt-1">
                    You do not have any pending cylinder delivery. Order a refill whenever needed.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => onNavigateTab('book')}
                  className="mt-2 px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition-all shadow-md shadow-rose-600/25"
                >
                  Book a Cylinder Refill
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Right Column (5 cols): Delivery in Progress Live Card */}
        <div className="lg:col-span-5 bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col">
          {/* Header */}
          <div className="p-4 sm:px-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-600 animate-ping" />
              <h2 className="text-sm font-bold text-slate-900">Delivery in Progress</h2>
            </div>
            <span className="px-2.5 py-0.5 rounded-full bg-rose-600 text-white text-[10px] font-black tracking-wide flex items-center gap-1 shadow-sm">
              <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
              LIVE
            </span>
          </div>

          {/* Map Preview Area */}
          <div className="relative h-48 sm:h-52 w-full bg-slate-100">
            <MapContainer
              center={[18.5254, 73.8517]}
              zoom={13}
              zoomControl={false}
              attributionControl={false}
              className="h-full w-full z-0"
              scrollWheelZoom={false}
            >
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              <Polyline
                positions={routePolyline}
                color="#e11d48"
                weight={4}
                opacity={0.8}
                dashArray="6, 8"
              />
              <Marker position={driverPos} icon={vanIcon}>
                <Tooltip permanent direction="top" offset={[0, -18]} className="font-sans">
                  <span className="text-xs font-bold text-slate-900">
                    Driver is on the way &bull; 18 mins away
                  </span>
                </Tooltip>
              </Marker>
              <Marker position={homePos} icon={homeIcon} />
            </MapContainer>

            {/* City Label Badge */}
            <div className="absolute bottom-3 left-4 z-10 pointer-events-none">
              <span className="text-lg font-black text-slate-800 drop-shadow-md">
                Pune
              </span>
            </div>

            {/* Tap to open full track screen */}
            <button
              type="button"
              onClick={() => onNavigateTab('track')}
              className="absolute top-3 right-3 z-10 px-2.5 py-1 rounded-lg bg-white/95 shadow-md border border-slate-200 text-[11px] font-bold text-slate-700 hover:text-rose-600 transition-colors flex items-center gap-1"
            >
              <ExternalLink size={12} />
              <span>Full Screen</span>
            </button>
          </div>

          {/* Driver & Vehicle Details Footer */}
          <div className="p-4 sm:p-5 flex items-center justify-between gap-3 border-t border-slate-100 bg-white">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-rose-50 border border-rose-100 overflow-hidden flex-shrink-0 flex items-center justify-center font-bold text-rose-700 text-xs">
                {latestOrder?.driverName ? latestOrder.driverName.charAt(0) : '—'}
              </div>
              <div>
                <p className="text-xs font-bold text-slate-900 leading-tight">
                  {latestOrder?.driverName || 'Driver Unassigned'}
                </p>
                <div className="flex items-center gap-1 mt-0.5">
                  <span className="text-[11px] text-slate-500 font-medium">
                    {latestOrder?.driverName ? 'Assigned Delivery Executive' : 'Awaiting Assignment'}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-col items-end gap-1">
              {latestOrder?.driverPhone ? (
                <a
                  href={`tel:${latestOrder.driverPhone}`}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-50 border border-slate-200 hover:border-slate-300 text-slate-700 text-xs font-semibold transition-colors"
                >
                  <Phone size={12} className="text-rose-600" />
                  <span>{latestOrder.driverPhone}</span>
                </a>
              ) : (
                <span className="text-[11px] text-slate-400 font-medium">Phone unavailable</span>
              )}
              <span className="inline-flex items-center gap-1 text-[11px] font-mono text-slate-500 font-bold">
                <Truck size={12} className="text-slate-400" />
                {latestOrder?.vehicleNumber || 'Vehicle Unassigned'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ── 4. Bottom Row: AI Assistance Card & Your Account Card ── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (7 cols): How can we help you today? */}
        <div className="lg:col-span-7 bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col justify-between">
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center flex-shrink-0">
                  <Sparkles size={20} />
                </div>
                <div>
                  <h2 className="text-base font-bold text-slate-900">
                    How can we help you today?
                  </h2>
                  <p className="text-xs text-slate-500">
                    Get instant answers with our AI assistant
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => onNavigateTab('ai')}
                className="px-4 py-2 rounded-full bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition-all shadow-md shadow-rose-600/25 flex items-center justify-center gap-1.5 self-start sm:self-auto"
              >
                <span>Chat with AI Assistant</span>
                <span aria-hidden="true">&rarr;</span>
              </button>
            </div>

            {/* Prompt Chips */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-5">
              {quickActionPrompts.map((prompt) => (
                <button
                  key={prompt}
                  type="button"
                  onClick={() => onNavigateTab('ai', prompt)}
                  className="px-3.5 py-2.5 rounded-xl border border-slate-200 hover:border-rose-300 hover:bg-rose-50/50 text-slate-700 hover:text-rose-600 text-xs font-semibold text-center transition-all"
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column (5 cols): Your Account Card */}
        <div className="lg:col-span-5 bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <h2 className="text-base font-bold text-slate-900">Your Account</h2>
              <button
                type="button"
                onClick={() => onNavigateTab('profile')}
                className="text-xs font-bold text-rose-600 hover:text-rose-700 flex items-center gap-1"
              >
                <span>View Profile</span>
                <span aria-hidden="true">&rarr;</span>
              </button>
            </div>

            <div className="pt-4 flex items-center justify-between gap-4">
              <div className="flex flex-col gap-3">
                <div className="flex items-center gap-2.5">
                  <User size={15} className="text-slate-400 flex-shrink-0" />
                  <div>
                    <span className="text-[10px] text-slate-400 block leading-tight">Name</span>
                    <span className="text-xs font-bold text-slate-900">{profile.name}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2.5">
                  <Phone size={15} className="text-slate-400 flex-shrink-0" />
                  <div>
                    <span className="text-[10px] text-slate-400 block leading-tight">Mobile Number</span>
                    <span className="text-xs font-bold text-slate-900">{profile.mobile}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2.5">
                  <Mail size={15} className="text-slate-400 flex-shrink-0" />
                  <div>
                    <span className="text-[10px] text-slate-400 block leading-tight">Email</span>
                    <span className="text-xs font-bold text-slate-900">{profile.email}</span>
                  </div>
                </div>

                <div className="flex items-center gap-2.5">
                  <MapPin size={15} className="text-slate-400 flex-shrink-0" />
                  <div>
                    <span className="text-[10px] text-slate-400 block leading-tight">Address</span>
                    <span className="text-xs font-bold text-slate-900 truncate max-w-[200px] block">
                      Pimpri, Pune, Maharashtra
                    </span>
                  </div>
                </div>
              </div>

              {/* Cozy House Vector Graphic */}
              <div className="hidden sm:flex flex-col items-center justify-center p-3 rounded-2xl bg-gradient-to-b from-amber-50 to-orange-50 border border-orange-100 flex-shrink-0">
                <svg width="70" height="60" viewBox="0 0 100 90" fill="none" xmlns="http://www.w3.org/2000/svg">
                  {/* Roof */}
                  <path d="M50 15L15 45H85L50 15Z" fill="#DC2626" />
                  {/* Chimney */}
                  <rect x="68" y="22" width="10" height="18" fill="#991B1B" />
                  {/* Smoke puff */}
                  <circle cx="73" cy="14" r="3" fill="#E2E8F0" opacity="0.8" />
                  <circle cx="76" cy="9" r="4" fill="#E2E8F0" opacity="0.6" />
                  {/* House Body */}
                  <rect x="22" y="45" width="56" height="38" rx="2" fill="#FEF3C7" stroke="#F59E0B" strokeWidth="2" />
                  {/* Door */}
                  <rect x="42" y="55" width="16" height="28" rx="2" fill="#B45309" />
                  <circle cx="46" cy="69" r="1.5" fill="#FEF3C7" />
                  {/* Window Left */}
                  <rect x="27" y="52" width="10" height="10" rx="1" fill="#93C5FD" stroke="#3B82F6" strokeWidth="1" />
                  {/* Window Right */}
                  <rect x="63" y="52" width="10" height="10" rx="1" fill="#93C5FD" stroke="#3B82F6" strokeWidth="1" />
                  {/* Grass ground */}
                  <path d="M5 83C25 80 75 80 95 83L92 88H8L5 83Z" fill="#10B981" />
                  {/* Small tree */}
                  <circle cx="12" cy="65" r="9" fill="#059669" />
                  <rect x="10.5" y="74" width="3" height="10" fill="#78350F" />
                </svg>
                <span className="text-[10px] font-bold text-amber-900 mt-1">Verified Home</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
