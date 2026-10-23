import React, { useState } from 'react';
import {
  Truck,
  RefreshCw,
  Key,
  CheckCircle2,
  AlertCircle,
  Navigation,
  Radio,
} from 'lucide-react';
import { DistributorDeliveryItem } from '../../../types/distributorPortalTypes';
import { LiveTrackingMap } from '../../tracking/LiveTrackingMap';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorLiveDeliveriesViewProps {
  deliveries: DistributorDeliveryItem[];
  isLoading: boolean;
  onRefresh: () => void;
  onDeliveryUpdated: () => void;
  onNavigateTab: (tab: any) => void;
}

export const DistributorLiveDeliveriesView: React.FC<DistributorLiveDeliveriesViewProps> = ({
  deliveries,
  isLoading,
  onRefresh,
  onDeliveryUpdated,
  onNavigateTab,
}) => {
  // Only active dispatches currently on the road
  const activeDeliveries = deliveries.filter((d) => d.status === 'OUT_FOR_DELIVERY');
  const [selectedDeliveryId, setSelectedDeliveryId] = useState<number | null>(
    activeDeliveries.length > 0 ? activeDeliveries[0].id : null
  );

  // OTP Verification Modal state
  const [otpModalDelivery, setOtpModalDelivery] = useState<DistributorDeliveryItem | null>(null);
  const [otpInput, setOtpInput] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [isInitiating, setIsInitiating] = useState(false);
  const [otpError, setOtpError] = useState<string | null>(null);
  const [otpSuccess, setOtpSuccess] = useState<string | null>(null);
  const [devTestOtp, setDevTestOtp] = useState<string | null>(null);

  const selectedDelivery =
    activeDeliveries.find((d) => d.id === selectedDeliveryId) ||
    (activeDeliveries.length > 0 ? activeDeliveries[0] : null);

  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  const handleInitiateHandover = async () => {
    if (!otpModalDelivery) return;
    try {
      setIsInitiating(true);
      setOtpError(null);
      const res = await DistributorApiService.initiateDeliveryHandover(otpModalDelivery.id);
      if (res.devTestOtp) {
        setDevTestOtp(res.devTestOtp);
        setOtpInput(res.devTestOtp);
      }
    } catch (err: any) {
      setOtpError(err.message || 'Failed to initiate delivery handover');
    } finally {
      setIsInitiating(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (!otpModalDelivery || !otpInput.trim()) {
      setOtpError('Please enter the 6-digit delivery OTP');
      return;
    }

    try {
      setIsVerifying(true);
      setOtpError(null);
      await DistributorApiService.verifyDeliveryOtp(otpModalDelivery.id, otpInput.trim());
      setOtpSuccess(`Delivery #${otpModalDelivery.id} verified and marked DELIVERED!`);
      setTimeout(() => {
        setOtpModalDelivery(null);
        setOtpInput('');
        setDevTestOtp(null);
        setOtpSuccess(null);
        onDeliveryUpdated();
      }, 1500);
    } catch (err: any) {
      setOtpError(err.message || 'Invalid delivery OTP');
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-rose-50 text-rose-600">
              <Radio size={18} className="animate-pulse" />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600">
              GPS Telemetry & Live Fleet Tracking
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Live Dispatches On The Road</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Real-time Traccar & Leaflet GPS tracking for dispatches currently out for delivery.
          </p>
        </div>

        <button
          onClick={onRefresh}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors self-start sm:self-auto"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          <span>Refresh Tracking</span>
        </button>
      </div>

      {activeDeliveries.length > 0 ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Active Deliveries List Sidebar */}
          <div className="space-y-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500 px-1">
              Active En-Route Dispatches ({activeDeliveries.length})
            </h2>

            <div className="space-y-2.5">
              {activeDeliveries.map((d) => {
                const isSelected = selectedDelivery?.id === d.id;
                return (
                  <div
                    key={d.id}
                    onClick={() => setSelectedDeliveryId(d.id)}
                    className={`p-4 rounded-2xl border transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-rose-50/50 border-rose-400 ring-2 ring-rose-500/10 shadow-sm'
                        : 'bg-white border-slate-200/80 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-mono text-xs font-bold text-slate-900">
                        Dispatch #{d.id}
                      </span>
                      <span className="flex items-center gap-1 text-[10px] font-bold text-rose-600 bg-rose-100/60 px-2 py-0.5 rounded-full">
                        <Radio size={10} className="animate-pulse" /> Live En Route
                      </span>
                    </div>

                    <p className="text-xs font-bold text-slate-800">{d.customerName || `Booking #${d.bookingId}`}</p>
                    <p className="text-[11px] text-slate-500 line-clamp-1 mb-2">{d.deliveryAddress}</p>

                    <div className="text-[11px] text-slate-600 pt-2 border-t border-slate-100/80 flex items-center justify-between">
                      <span>Driver: <strong>{d.driverName || 'Depot Driver'}</strong></span>
                      <span className="font-mono text-slate-500">{d.vehicleNumber || d.vehicleRegistration || 'Vehicle'}</span>
                    </div>

                    <div className="mt-3 pt-2 border-t border-slate-100/80 flex justify-end">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setOtpModalDelivery(d);
                          setOtpInput('');
                          setOtpError(null);
                          setOtpSuccess(null);
                        }}
                        className="px-2.5 py-1 text-[11px] font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg transition-colors flex items-center gap-1 shadow-xs"
                      >
                        <Key size={12} />
                        <span>Doorstep OTP Handover</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Map View Port */}
          <div className="lg:col-span-2 space-y-4">
            {selectedDelivery ? (
              <div className="bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-xs">
                <div className="p-4 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <Navigation size={16} className="text-rose-600" />
                    <span className="text-xs font-bold text-slate-900">
                      Tracking Dispatch #{selectedDelivery.id} (Vehicle: {selectedDelivery.vehicleNumber || selectedDelivery.vehicleRegistration || 'Active'})
                    </span>
                  </div>

                  <button
                    onClick={() => {
                      setOtpModalDelivery(selectedDelivery);
                      setOtpInput('');
                      setOtpError(null);
                      setOtpSuccess(null);
                    }}
                    className="px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs flex items-center gap-1.5 transition-colors"
                  >
                    <Key size={13} />
                    <span>Complete Handover (OTP)</span>
                  </button>
                </div>

                {/* Leaflet Live Map */}
                <div className="h-[460px] w-full relative">
                  <LiveTrackingMap
                    deliveryId={selectedDelivery.id}
                    token={token}
                    height="100%"
                    className="w-full h-full"
                  />
                </div>
              </div>
            ) : null}
          </div>
        </div>
      ) : (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <Truck className="w-10 h-10 text-slate-300 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-800">No Dispatches Currently En Route</h3>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            Vehicle live tracking activates only after a scheduled delivery is officially dispatched from the depot.
          </p>
          <button
            onClick={() => onNavigateTab('delivery-schedule')}
            className="mt-4 px-4 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white transition-colors"
          >
            Go to Scheduled Deliveries
          </button>
        </div>
      )}

      {/* Handover OTP Modal */}
      {otpModalDelivery && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center gap-2 text-emerald-600">
              <Key size={20} />
              <h2 className="text-base font-bold text-slate-900">Doorstep Delivery Handover OTP</h2>
            </div>

            <p className="text-xs text-slate-600">
              Enter the 6-digit verification OTP provided by customer{' '}
              <strong>{otpModalDelivery.customerName || `Customer #${otpModalDelivery.bookingId}`}</strong> to verify physical cylinder handover.
            </p>

            {otpError && (
              <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-xs border border-rose-200 flex items-center gap-2">
                <AlertCircle size={15} className="shrink-0" />
                <span>{otpError}</span>
              </div>
            )}

            {otpSuccess && (
              <div className="p-3 rounded-xl bg-emerald-50 text-emerald-700 text-xs border border-emerald-200 flex items-center gap-2">
                <CheckCircle2 size={15} className="shrink-0" />
                <span>{otpSuccess}</span>
              </div>
            )}

            {devTestOtp && (
              <div className="p-3 rounded-xl bg-amber-50 text-amber-900 text-xs border border-amber-200 flex items-center justify-between">
                <span>Handover OTP Generated: <strong className="font-mono text-sm">{devTestOtp}</strong></span>
                <button
                  type="button"
                  onClick={() => setOtpInput(devTestOtp)}
                  className="px-2 py-0.5 rounded-md bg-amber-200 hover:bg-amber-300 font-bold text-[10px] cursor-pointer"
                >
                  Use Code
                </button>
              </div>
            )}

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="text-[11px] font-bold text-slate-600 block">6-Digit Verification OTP</label>
                <button
                  type="button"
                  disabled={isInitiating}
                  onClick={handleInitiateHandover}
                  className="text-[10px] font-bold text-emerald-700 hover:text-emerald-800 underline disabled:opacity-50 cursor-pointer"
                >
                  {isInitiating ? 'Generating OTP...' : 'Generate / Request OTP'}
                </button>
              </div>
              <input
                type="text"
                maxLength={6}
                value={otpInput}
                onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ''))}
                placeholder="Enter 6-digit OTP"
                className="w-full text-center tracking-widest text-lg font-black font-mono p-3 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500"
              />
              <span className="text-[10px] text-slate-400 mt-1 block">
                Generated securely when driver initiates doorstep handover at customer location.
              </span>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                disabled={isVerifying}
                onClick={() => setOtpModalDelivery(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
              >
                Cancel
              </button>
              <button
                disabled={isVerifying || otpInput.length < 4}
                onClick={handleVerifyOtp}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs disabled:opacity-50"
              >
                {isVerifying ? 'Verifying...' : 'Verify & Handover'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
