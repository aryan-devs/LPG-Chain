import React, { useState, useEffect, useCallback } from 'react';
import {
  Truck,
  Flame,
  Radio,
  Clock,
  ArrowRight,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  Lock,
  Activity,
} from 'lucide-react';
import { MapContainer, TileLayer } from 'react-leaflet';
import { LiveTrackingMap } from '../tracking/LiveTrackingMap';
import { UserProfile } from '../auth/LoginModal';
import { VehicleLocation } from '../../types/tracking';
import { getDeliveryDetails, DeliveryDetails } from '../../services/trackingApi';

export interface LiveTrackingPreviewProps {
  onNavigateToTracking: (deliveryId?: string) => void;
  onOpenBooking: () => void;
  currentUser?: UserProfile | null;
  onOpenLogin?: () => void;
}

export interface DeliveryStage {
  num: string;
  title: string;
  desc: string;
  status: 'completed' | 'current' | 'upcoming';
}

export const LiveTrackingPreview: React.FC<LiveTrackingPreviewProps> = ({
  onNavigateToTracking,
  onOpenBooking,
  currentUser,
  onOpenLogin,
}) => {
  // Delivery ID management
  const [activeDeliveryId, setActiveDeliveryId] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('lpg_customer_active_delivery');
      if (stored && stored.trim().length > 0) return stored.trim();
    }
    return '1';
  });

  // Active delivery boolean state
  const [hasActiveDelivery, setHasActiveDelivery] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('lpg_has_active_delivery');
      if (saved !== null) return saved === 'true';
    }
    return true;
  });

  // Live telemetry data received from LiveTrackingMap callback
  const [liveLocation, setLiveLocation] = useState<VehicleLocation | null>(null);

  // Delivery details fetched from REST endpoint
  const [deliveryDetails, setDeliveryDetails] = useState<DeliveryDetails | null>(null);

  // Load delivery metadata for the logged-in customer's delivery
  const fetchDeliveryData = useCallback(async (deliveryId: string) => {
    if (!currentUser || !hasActiveDelivery) {
      setDeliveryDetails(null);
      return;
    }
    try {
      const data = await getDeliveryDetails(deliveryId, currentUser.token);
      setDeliveryDetails(data);
    } catch {
      setDeliveryDetails(null);
    }
  }, [currentUser, hasActiveDelivery]);

  // Sync when delivery changes or storage events trigger
  useEffect(() => {
    if (currentUser && hasActiveDelivery) {
      fetchDeliveryData(activeDeliveryId);
    } else {
      setDeliveryDetails(null);
      setLiveLocation(null);
    }
  }, [currentUser, hasActiveDelivery, activeDeliveryId, fetchDeliveryData]);

  // Listen for local delivery updates from BookingModal
  useEffect(() => {
    const handleStorageUpdate = (e?: Event) => {
      const customDetail = (e as CustomEvent)?.detail;
      if (customDetail?.deliveryId) {
        setActiveDeliveryId(String(customDetail.deliveryId));
        setHasActiveDelivery(true);
      } else {
        const savedDeliveryId = localStorage.getItem('lpg_customer_active_delivery');
        const savedHasActive = localStorage.getItem('lpg_has_active_delivery');
        if (savedDeliveryId) setActiveDeliveryId(savedDeliveryId);
        if (savedHasActive !== null) setHasActiveDelivery(savedHasActive === 'true');
      }
    };

    window.addEventListener('lpg_delivery_updated', handleStorageUpdate);
    window.addEventListener('storage', handleStorageUpdate);
    return () => {
      window.removeEventListener('lpg_delivery_updated', handleStorageUpdate);
      window.removeEventListener('storage', handleStorageUpdate);
    };
  }, []);

  // Compute the 4 delivery stages
  const deliveryStages: DeliveryStage[] = [
    {
      num: '01',
      title: 'Booking Confirmed',
      desc: 'Cylinder refill order confirmed and logged in dispatch queue.',
      status: currentUser && hasActiveDelivery ? 'completed' : 'upcoming',
    },
    {
      num: '02',
      title: 'Distributor Assigned',
      desc: 'Order routed to authorized regional LPG distributor depot.',
      status:
        currentUser && hasActiveDelivery
          ? deliveryDetails?.status === 'ASSIGNED'
            ? 'current'
            : 'completed'
          : 'upcoming',
    },
    {
      num: '03',
      title: 'Out for Delivery',
      desc: 'Cylinder in transit with verified driver and real-time GPS telemetry.',
      status:
        currentUser && hasActiveDelivery
          ? deliveryDetails?.status === 'DELIVERED'
            ? 'completed'
            : deliveryDetails?.status === 'ASSIGNED'
            ? 'upcoming'
            : 'current'
          : 'upcoming',
    },
    {
      num: '04',
      title: 'Delivered',
      desc: 'Secure doorstep handover and digital verification.',
      status:
        currentUser && hasActiveDelivery && deliveryDetails?.status === 'DELIVERED'
          ? 'completed'
          : 'upcoming',
    },
  ];

  // Primary action button click handler
  const handleViewLiveDelivery = () => {
    if (!currentUser) {
      if (onOpenLogin) {
        onOpenLogin();
      } else {
        onNavigateToTracking();
      }
    } else {
      onNavigateToTracking(activeDeliveryId);
    }
  };

  return (
    <section id="live-tracking-preview" className="py-20 sm:py-28 bg-slate-50/70 border-b border-slate-200/80 scroll-mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-14 sm:mb-16">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-rose-50 border border-rose-200 text-rose-600 text-xs font-bold uppercase tracking-wider mb-3 shadow-xs">
            <Radio size={14} className="text-rose-600 animate-pulse" />
            <span>Live Delivery Tracking</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-[2.75rem] font-black tracking-tight leading-tight text-slate-950">
            Know Where Your Cylinder Is.
          </h2>
          <p className="text-base sm:text-lg text-slate-600 mt-3 leading-relaxed">
            Follow your active LPG delivery from distributor assignment to doorstep arrival.
          </p>
        </div>

        {/* Two-Column Responsive Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* ── LEFT SIDE: Delivery Status Timeline ── */}
          <div className="lg:col-span-5 bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 flex flex-col justify-between gap-6">
            
            <div>
              {/* Card Header */}
              <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-5">
                <div>
                  <h3 className="text-base font-bold text-slate-900 tracking-tight">
                    Delivery Lifecycle
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Real-time status tracking pipeline
                  </p>
                </div>
                {currentUser && hasActiveDelivery && (
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-rose-50 border border-rose-200 text-rose-600 text-xs font-bold font-mono">
                    <span className="w-2 h-2 rounded-full bg-rose-600 animate-pulse" />
                    ● LIVE
                  </span>
                )}
              </div>

              {/* Supply Chain Progression Header Banner: BOOK → ASSIGN → TRACK → DELIVER */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex items-center justify-between text-xs font-bold text-slate-700 mb-6">
                <span className="flex items-center gap-1 text-slate-900 font-black">
                  <Flame size={13} className="text-rose-600" />
                  BOOK
                </span>
                <span className="text-slate-300 font-bold">&rarr;</span>
                <span className="flex items-center gap-1 text-slate-800 font-bold">
                  ASSIGN
                </span>
                <span className="text-slate-300 font-bold">&rarr;</span>
                <span className="flex items-center gap-1 text-rose-600 font-black">
                  <Radio size={13} />
                  TRACK
                </span>
                <span className="text-slate-300 font-bold">&rarr;</span>
                <span className="flex items-center gap-1 text-emerald-700 font-bold">
                  DELIVER
                </span>
              </div>

              {/* 4 Delivery Stages Timeline */}
              <div className="space-y-1">
                {deliveryStages.map((stage, idx, arr) => {
                  const isCompleted = stage.status === 'completed';
                  const isCurrent = stage.status === 'current';
                  const isUpcoming = stage.status === 'upcoming';

                  return (
                    <div key={stage.num} className="relative flex items-start gap-4">
                      {/* Connecting Line */}
                      {idx < arr.length - 1 && (
                        <span
                          className={`absolute left-4 top-8 -bottom-3 w-0.5 transition-colors ${
                            isCompleted ? 'bg-emerald-300' : 'bg-slate-200'
                          }`}
                          aria-hidden="true"
                        />
                      )}

                      {/* Node Icon */}
                      <div
                        className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center text-xs transition-all flex-shrink-0 ${
                          isCompleted
                            ? 'bg-emerald-500 text-white shadow-sm ring-4 ring-emerald-50 font-bold'
                            : isCurrent
                            ? 'bg-rose-600 text-white shadow-md shadow-rose-600/30 ring-4 ring-rose-100 font-black'
                            : 'bg-slate-100 text-slate-400 border border-slate-200 font-semibold'
                        }`}
                      >
                        {isCompleted ? (
                          <CheckCircle2 size={16} />
                        ) : (
                          <span>{stage.num}</span>
                        )}
                      </div>

                      {/* Stage Content */}
                      <div className="flex-1 pb-5">
                        <div className="flex items-center justify-between gap-2">
                          <h4
                            className={`text-sm font-bold tracking-tight ${
                              isCurrent
                                ? 'text-rose-600'
                                : isCompleted
                                ? 'text-slate-900'
                                : 'text-slate-500'
                            }`}
                          >
                            {stage.num} &mdash; {stage.title}
                          </h4>

                          {isCompleted && (
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-[10px] font-bold uppercase tracking-wider">
                              Done
                            </span>
                          )}
                          {isCurrent && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-50 border border-rose-200 text-rose-600 text-[10px] font-black uppercase tracking-wider">
                              <span className="w-1.5 h-1.5 rounded-full bg-rose-600 animate-pulse" />
                              Current
                            </span>
                          )}
                          {isUpcoming && (
                            <span className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">
                              Upcoming
                            </span>
                          )}
                        </div>
                        <p
                          className={`text-xs mt-1 leading-relaxed ${
                            isCurrent
                              ? 'text-slate-700 font-medium'
                              : isCompleted
                              ? 'text-slate-600'
                              : 'text-slate-400'
                          }`}
                        >
                          {stage.desc}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Informative Footer Box */}
            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 text-xs text-slate-600 flex items-start gap-2.5">
              <ShieldCheck size={16} className="text-emerald-600 flex-shrink-0 mt-0.5" />
              <p className="leading-relaxed">
                {!currentUser ? (
                  <>
                    <strong className="text-slate-900 font-semibold">Real-Time Progression:</strong> Every LPG cylinder follows this verified supply chain. Sign in to view live GPS telemetry and active driver updates.
                  </>
                ) : hasActiveDelivery ? (
                  <>
                    <strong className="text-slate-900 font-semibold">Active Order:</strong> Your cylinder is currently in transit. Real-time updates stream directly from verified depot IoT sensors.
                  </>
                ) : (
                  <>
                    <strong className="text-slate-900 font-semibold">Ready to Order:</strong> Book a refill anytime to initiate automated depot routing, driver allocation, and live tracking.
                  </>
                )}
              </p>
            </div>

          </div>

          {/* ── RIGHT SIDE: Map Preview & Telemetry Console ── */}
          <div className="lg:col-span-7 bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 flex flex-col gap-6">
            
            {/* Top Control Bar */}
            <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-100">
              
              {/* Left: Mode / Status Label */}
              <div>
                {!currentUser ? (
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 border border-slate-200 text-slate-600 text-xs font-bold">
                      <Lock size={12} className="text-slate-500" />
                      PREVIEW
                    </span>
                    <span className="text-xs font-bold text-slate-700">
                      Public Map Preview
                    </span>
                  </div>
                ) : hasActiveDelivery ? (
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-rose-50 border border-rose-200 text-rose-600 text-xs font-bold font-mono">
                      <span className="w-2 h-2 rounded-full bg-rose-600 animate-pulse" />
                      ● LIVE
                    </span>
                    <span className="text-xs font-bold text-slate-900">
                      Active Delivery #{activeDeliveryId}
                    </span>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 border border-slate-200 text-slate-600 text-xs font-bold">
                      <Truck size={12} className="text-slate-500" />
                      IDLE
                    </span>
                    <span className="text-xs font-bold text-slate-800">
                      No Active Delivery
                    </span>
                  </div>
                )}
              </div>

              {/* Right: Actions & State Simulation Toggle */}
              <div className="flex items-center gap-2">
                {currentUser && (
                  <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl text-xs">
                    <button
                      type="button"
                      title="Simulate customer with active delivery"
                      onClick={() => {
                        setHasActiveDelivery(true);
                        if (typeof window !== 'undefined') localStorage.setItem('lpg_has_active_delivery', 'true');
                      }}
                      className={`px-2.5 py-1 rounded-lg font-bold transition-all ${
                        hasActiveDelivery
                          ? 'bg-rose-600 text-white shadow-xs'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      Active Order
                    </button>
                    <button
                      type="button"
                      title="Simulate customer without active delivery"
                      onClick={() => {
                        setHasActiveDelivery(false);
                        if (typeof window !== 'undefined') localStorage.setItem('lpg_has_active_delivery', 'false');
                      }}
                      className={`px-2.5 py-1 rounded-lg font-medium transition-all ${
                        !hasActiveDelivery
                          ? 'bg-slate-800 text-white shadow-xs font-bold'
                          : 'text-slate-500 hover:text-slate-800'
                      }`}
                    >
                      No Order
                    </button>
                  </div>
                )}

                {/* Primary Action Button */}
                {!currentUser ? (
                  <button
                    type="button"
                    onClick={handleViewLiveDelivery}
                    className="px-4 py-2 bg-rose-600 hover:bg-rose-500 active:scale-95 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-rose-600/25 transition-all flex items-center gap-2 cursor-pointer"
                  >
                    <span>View Live Delivery</span>
                    <ArrowRight size={14} />
                  </button>
                ) : hasActiveDelivery ? (
                  <button
                    type="button"
                    onClick={handleViewLiveDelivery}
                    className="px-4 py-2 bg-rose-600 hover:bg-rose-500 active:scale-95 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-rose-600/25 transition-all flex items-center gap-2 cursor-pointer"
                  >
                    <span>View Live Delivery</span>
                    <ExternalLink size={14} />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={onOpenBooking}
                    className="px-4 py-2 bg-rose-600 hover:bg-rose-500 active:scale-95 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-rose-600/25 transition-all flex items-center gap-2 cursor-pointer"
                  >
                    <Flame size={14} />
                    <span>Book LPG Cylinder</span>
                    <ArrowRight size={14} />
                  </button>
                )}
              </div>

            </div>

            {/* Map Preview Display Area */}
            <div className="rounded-2xl overflow-hidden border border-slate-200 shadow-inner bg-slate-50 relative min-h-[380px] flex flex-col justify-center">
              
              {/* STATE 1: PUBLIC VISITOR — Safe map preview with zero private telemetry */}
              {!currentUser && (
                <div className="relative w-full h-[380px] overflow-hidden">
                  {/* Real Leaflet Map tiles rendered safely without any vehicle coordinates or user data */}
                  <MapContainer
                    key="safe-public-map"
                    center={[18.5204, 73.8567]}
                    zoom={13}
                    zoomControl={false}
                    scrollWheelZoom={false}
                    dragging={false}
                    style={{ height: '100%', width: '100%' }}
                  >
                    <TileLayer
                      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                      url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                      maxZoom={19}
                    />
                  </MapContainer>

                  {/* Frosted Glass Overlay */}
                  <div className="absolute inset-0 bg-slate-950/45 backdrop-blur-[2px] z-10 flex flex-col items-center justify-center p-6 sm:p-8 text-center">
                    <div className="max-w-md bg-white/95 backdrop-blur-md rounded-2xl p-6 shadow-xl border border-white/60 text-slate-900 flex flex-col items-center">
                      <div className="w-12 h-12 rounded-2xl bg-rose-50 border border-rose-200 flex items-center justify-center text-rose-600 mb-3 shadow-xs">
                        <ShieldCheck size={24} />
                      </div>
                      <span className="text-[11px] font-bold uppercase tracking-wider text-rose-600 mb-1">
                        Safe Public Preview
                      </span>
                      <h4 className="text-base sm:text-lg font-black text-slate-950 tracking-tight mb-1.5">
                        Live tracking is available for active deliveries.
                      </h4>
                      <p className="text-xs text-slate-600 mb-4 max-w-sm leading-relaxed">
                        Sign in to view real-time vehicle GPS coordinates, driver allocation, and doorstep delivery tracking.
                      </p>
                      <button
                        type="button"
                        onClick={handleViewLiveDelivery}
                        className="px-5 py-2.5 bg-rose-600 hover:bg-rose-500 active:scale-95 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-rose-600/30 transition-all flex items-center gap-2 cursor-pointer"
                      >
                        <Truck size={15} />
                        <span>View Live Delivery</span>
                        <ArrowRight size={14} />
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* STATE 2: LOGGED-IN CUSTOMER WITHOUT AN ACTIVE DELIVERY */}
              {currentUser && !hasActiveDelivery && (
                <div className="w-full h-[380px] bg-slate-50/90 flex flex-col items-center justify-center p-6 sm:p-8 text-center">
                  <div className="w-14 h-14 rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-600 mb-3.5 shadow-sm">
                    <Truck size={26} />
                  </div>
                  <h3 className="text-lg sm:text-xl font-black text-slate-900 tracking-tight mb-2">
                    No active delivery
                  </h3>
                  <p className="text-xs sm:text-sm text-slate-600 max-w-sm mb-6 leading-relaxed">
                    You do not have an active LPG cylinder order in transit. Place a refill booking to follow your cylinder from depot to doorstep.
                  </p>
                  <button
                    type="button"
                    onClick={onOpenBooking}
                    className="px-6 py-3 bg-rose-600 hover:bg-rose-500 active:scale-95 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-rose-600/25 transition-all flex items-center gap-2 cursor-pointer"
                  >
                    <Flame size={16} />
                    <span>Book LPG Cylinder</span>
                    <ArrowRight size={14} />
                  </button>
                </div>
              )}

              {/* STATE 3: LOGGED-IN CUSTOMER WITH ACTIVE DELIVERY — Real tracking map */}
              {currentUser && hasActiveDelivery && (
                <LiveTrackingMap
                  key={`active-tracking-preview-${activeDeliveryId}`}
                  deliveryId={activeDeliveryId}
                  token={currentUser.token}
                  height="380px"
                  className="w-full z-10"
                  onLocationUpdate={(loc) => setLiveLocation(loc)}
                />
              )}

            </div>

            {/* ── TRACKING INFORMATION CARDS (Below Map) ── */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 pt-2 text-xs">
              
              {/* Delivery Status */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                <span className="text-[11px] font-semibold text-slate-500">Delivery Status</span>
                <div className="mt-1.5">
                  {!currentUser ? (
                    <span className="font-semibold text-slate-400">Auth Required</span>
                  ) : hasActiveDelivery ? (
                    <span className="inline-flex items-center gap-1.5 font-bold text-rose-600">
                      <span className="w-1.5 h-1.5 rounded-full bg-rose-600 animate-pulse" />
                      {deliveryDetails?.status
                        ? deliveryDetails.status.replace(/_/g, ' ')
                        : liveLocation
                        ? 'Out for Delivery'
                        : 'Tracking Active'}
                    </span>
                  ) : (
                    <span className="font-bold text-slate-600">No Active Delivery</span>
                  )}
                </div>
              </div>

              {/* Driver */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                <span className="text-[11px] font-semibold text-slate-500">Driver</span>
                <div className="mt-1.5 truncate">
                  {!currentUser ? (
                    <span className="font-semibold text-slate-400">Sign in to view</span>
                  ) : hasActiveDelivery ? (
                    <span className="font-bold text-slate-900">
                      {deliveryDetails?.driverName || (liveLocation ? 'Assigned Driver' : 'Awaiting Driver')}
                    </span>
                  ) : (
                    <span className="font-semibold text-slate-400">Unassigned</span>
                  )}
                </div>
              </div>

              {/* Vehicle */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                <span className="text-[11px] font-semibold text-slate-500">Vehicle</span>
                <div className="mt-1.5 truncate">
                  {!currentUser ? (
                    <span className="font-semibold text-slate-400">Sign in to view</span>
                  ) : hasActiveDelivery ? (
                    <span className="font-bold text-slate-900 font-mono">
                      {liveLocation?.vehicleNumber || deliveryDetails?.vehicleNumber || `Vehicle #${activeDeliveryId}`}
                    </span>
                  ) : (
                    <span className="font-semibold text-slate-400">Unassigned</span>
                  )}
                </div>
              </div>

              {/* Estimated / Live Speed */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex flex-col justify-between">
                <span className="text-[11px] font-semibold text-slate-500">Live Telemetry</span>
                <div className="mt-1.5">
                  {!currentUser ? (
                    <span className="font-semibold text-slate-400">Active only</span>
                  ) : hasActiveDelivery ? (
                    liveLocation ? (
                      <span className="font-bold text-emerald-600 flex items-center gap-1 font-mono">
                        <Activity size={12} className="text-emerald-500" />
                        {Math.round(liveLocation.speed)} km/h • En Route
                      </span>
                    ) : (
                      <span className="font-medium text-slate-600">
                        Connecting GPS...
                      </span>
                    )
                  ) : (
                    <span className="font-semibold text-slate-600">Ready to Book</span>
                  )}
                </div>
              </div>

              {/* Last Updated */}
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 col-span-2 sm:col-span-1 flex flex-col justify-between">
                <span className="text-[11px] font-semibold text-slate-500">Last Telemetry</span>
                <div className="mt-1.5">
                  {!currentUser ? (
                    <span className="font-semibold text-slate-400">&mdash;</span>
                  ) : hasActiveDelivery ? (
                    <span className="font-bold text-rose-600 flex items-center gap-1 font-mono text-[11px]">
                      <Clock size={12} />
                      {liveLocation?.recordedAt
                        ? new Date(liveLocation.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
                        : 'Live Stream'}
                    </span>
                  ) : (
                    <span className="font-semibold text-slate-600">Up to date</span>
                  )}
                </div>
              </div>

            </div>

          </div>

        </div>

      </div>
    </section>
  );
};
