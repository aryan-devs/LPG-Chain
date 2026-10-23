import React, { useState, useEffect } from 'react';
import {
  Truck,
  Phone,
  Scale,
  Key,
  CheckCircle2,
  AlertCircle,
  Clock,
  ShieldCheck,
  RefreshCw,
  Send,
  Lock,
} from 'lucide-react';
import { LiveTrackingMap } from '../../tracking/LiveTrackingMap';
import { CustomerOrder, CustomerProfileData, PortalTab } from '../portalTypes';
import {
  getDeliveryDetails,
  initiateDeliveryHandover,
  verifyDeliveryHandoverOtp,
  DeliveryDetails,
} from '../../../services/trackingApi';

interface PortalTrackDeliveryViewProps {
  profile: CustomerProfileData;
  activeOrder: CustomerOrder | null;
  onNavigateTab: (tab: PortalTab) => void;
}

export const PortalTrackDeliveryView: React.FC<PortalTrackDeliveryViewProps> = ({
  profile,
  activeOrder,
  onNavigateTab,
}) => {
  const deliveryId = activeOrder?.deliveryId || '1';
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  const [deliveryDetails, setDeliveryDetails] = useState<DeliveryDetails | null>(null);
  const [loadingDetails, setLoadingDetails] = useState<boolean>(false);

  // Driver handover state
  const [isInitiatingHandover, setIsInitiatingHandover] = useState<boolean>(false);
  const [isVerifyingOtp, setIsVerifyingOtp] = useState<boolean>(false);
  const [otpInput, setOtpInput] = useState<string>('');
  const [handoverSuccess, setHandoverSuccess] = useState<string | null>(null);
  const [handoverError, setHandoverError] = useState<string | null>(null);
  const [devTestOtp, setDevTestOtp] = useState<string | null>(null);
  const [devTestNotice, setDevTestNotice] = useState<string | null>(null);

  const fetchDetails = async () => {
    if (!deliveryId) return;
    setLoadingDetails(true);
    try {
      const details = await getDeliveryDetails(deliveryId, token);
      if (details) {
        setDeliveryDetails(details);
      }
    } catch {
      // Fallback cleanly
    } finally {
      setLoadingDetails(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [deliveryId]);

  if (!activeOrder) {
    return (
      <div className="max-w-3xl mx-auto py-12 px-4 text-center">
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-8 sm:p-12 flex flex-col items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <Truck size={32} />
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900">
            No Active Delivery to Track
          </h2>
          <p className="text-sm text-slate-500 max-w-md">
            You do not currently have any cylinder in transit for {profile.address}. Order a refill to get live GPS driver tracking.
          </p>
          <button
            type="button"
            onClick={() => onNavigateTab('book')}
            className="mt-2 px-6 py-3 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm shadow-md shadow-rose-600/30 transition-all cursor-pointer"
          >
            Book a Cylinder Refill
          </button>
        </div>
      </div>
    );
  }

  const effectiveStatus = deliveryDetails?.status || activeOrder.status;
  const isDelivered = effectiveStatus === 'DELIVERED';
  const handoverInitiated = Boolean(deliveryDetails?.handoverInitiated || activeOrder.handoverInitiated || devTestOtp);

  const driverName = deliveryDetails?.driverName || activeOrder.driverName;
  const driverPhone = activeOrder.driverPhone;
  const vehicleNumber = deliveryDetails?.vehicleNumber || activeOrder.vehicleNumber;

  const handleInitiateHandover = async () => {
    setHandoverError(null);
    setHandoverSuccess(null);
    setIsInitiatingHandover(true);
    try {
      const res = await initiateDeliveryHandover(deliveryId, token);
      if (res.devTestOtp) {
        setDevTestOtp(res.devTestOtp);
        setDevTestNotice(res.devTestNotice || 'Explicit development test flow active');
      }
      setHandoverSuccess(res.notificationMessage || 'Handover initiated. OTP sent to customer contact.');
      await fetchDetails();
    } catch (err: any) {
      setHandoverError(err?.message || 'Failed to initiate delivery handover.');
    } finally {
      setIsInitiatingHandover(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpInput.trim()) {
      setHandoverError('Please enter the 6-digit delivery OTP.');
      return;
    }
    setHandoverError(null);
    setHandoverSuccess(null);
    setIsVerifyingOtp(true);
    try {
      const updated = await verifyDeliveryHandoverOtp(deliveryId, otpInput.trim(), token);
      setDeliveryDetails(updated);
      setHandoverSuccess('Delivery verified and marked as DELIVERED successfully!');
      setOtpInput('');
      setDevTestOtp(null);
    } catch (err: any) {
      setHandoverError(err?.message || 'Invalid OTP or handover verification failure.');
    } finally {
      setIsVerifyingOtp(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto flex flex-col gap-6 pb-12">
      {/* Top Banner / Order Info Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 sm:p-6 rounded-3xl border border-slate-200/90 shadow-sm">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-rose-600 text-white flex items-center justify-center shadow-md shadow-rose-600/25 flex-shrink-0">
            <Truck size={24} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-400 font-mono">
                #{activeOrder.id}
              </span>
              <span
                className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                  isDelivered
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-rose-100 text-rose-800'
                }`}
              >
                {isDelivered
                  ? 'Delivered & Handover Verified'
                  : driverName
                  ? 'In Transit • Driver Assigned'
                  : 'In Transit • Awaiting Driver'}
              </span>
            </div>
            <h1 className="text-lg sm:text-xl font-black text-slate-900 mt-0.5">
              Live Vehicle Delivery Telemetry
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-3 self-end sm:self-auto">
          <button
            type="button"
            onClick={fetchDetails}
            disabled={loadingDetails}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 transition-colors"
            title="Refresh delivery status"
          >
            <RefreshCw size={15} className={loadingDetails ? 'animate-spin' : ''} />
          </button>
          <div className="text-right">
            <span className="text-[10px] text-slate-400 uppercase tracking-wider block font-bold">
              {isDelivered ? 'Status' : 'Estimated Delivery'}
            </span>
            <span className="text-sm font-black text-slate-900">
              {isDelivered ? 'DELIVERED' : activeOrder.expectedDelivery}
            </span>
          </div>
        </div>
      </div>

      {/* Main Grid: Map & Order Details */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column (8 cols): Interactive Live Tracking Console */}
        <div className="lg:col-span-8 flex flex-col gap-4">
          <LiveTrackingMap
            deliveryId={deliveryId}
            token={token}
            height="460px"
            className="w-full"
            defaultCenter={[18.5204, 73.8567]}
          />
        </div>

        {/* Right Column (4 cols): Driver Info, Delivery Handover OTP, Safety Checklist */}
        <div className="lg:col-span-4 flex flex-col gap-5">
          {/* Driver Card */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 flex flex-col gap-4">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              Assigned Delivery Personnel
            </span>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-rose-50 border border-rose-100 text-rose-600 flex items-center justify-center font-bold text-sm">
                  {driverName ? driverName.charAt(0) : '—'}
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">
                    {driverName || 'Driver Not Assigned'}
                  </h3>
                  <div className="flex items-center gap-1 mt-0.5">
                    <span className="text-xs text-slate-500 font-medium">
                      {driverName ? 'Authorized Personnel' : 'Awaiting assignment'}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="pt-2 border-t border-slate-100 flex flex-col gap-2">
              <div className="flex items-center justify-between text-xs text-slate-600">
                <span>Vehicle Reg:</span>
                <span className="font-mono font-bold text-slate-900">
                  {vehicleNumber || 'Unassigned'}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs text-slate-600">
                <span>Distributor:</span>
                <span className="font-medium text-slate-900">
                  {deliveryDetails?.distributorName || profile.distributorName || 'Assigned Agency'}
                </span>
              </div>
            </div>

            {driverPhone ? (
              <a
                href={`tel:${driverPhone}`}
                className="w-full py-2.5 rounded-xl border border-rose-600 text-rose-600 hover:bg-rose-50 text-xs font-bold transition-colors flex items-center justify-center gap-2 text-center"
              >
                <Phone size={14} />
                <span>Call Delivery Driver</span>
              </a>
            ) : (
              <div className="w-full py-2 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 text-xs font-medium text-center">
                Contact number unavailable
              </div>
            )}
          </div>

          {/* Secure Delivery Handover & Customer OTP State */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold tracking-wider uppercase text-slate-700 flex items-center gap-1.5">
                <Key size={14} className="text-rose-600" /> Delivery Handover
              </span>
              <span
                className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold ${
                  isDelivered
                    ? 'bg-emerald-100 text-emerald-800'
                    : handoverInitiated
                    ? 'bg-amber-100 text-amber-800 animate-pulse'
                    : 'bg-slate-100 text-slate-600'
                }`}
              >
                {isDelivered
                  ? 'Completed'
                  : handoverInitiated
                  ? 'OTP Dispatched'
                  : 'Awaiting Doorstep'}
              </span>
            </div>

            {isDelivered ? (
              <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex flex-col gap-2">
                <div className="flex items-center gap-2 text-emerald-800 font-bold text-xs">
                  <ShieldCheck size={18} className="text-emerald-600" />
                  <span>Handover Verified &amp; Completed</span>
                </div>
                <p className="text-[11px] text-emerald-700 leading-relaxed">
                  The cylinder was verified via secure OTP and handed over safely.
                  {deliveryDetails?.deliveredAt && (
                    <span className="block mt-1 font-mono text-[10px] text-emerald-600">
                      Completed: {new Date(deliveryDetails.deliveredAt).toLocaleString()}
                    </span>
                  )}
                </p>
              </div>
            ) : handoverInitiated ? (
              <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 flex flex-col gap-2.5">
                <div className="flex items-center gap-2 text-rose-900 font-bold text-xs">
                  <Clock size={16} className="text-rose-600" />
                  <span>Doorstep Handover in Progress</span>
                </div>
                <p className="text-[11px] text-slate-600 leading-relaxed">
                  A 6-digit verification code has been dispatched to your registered contact number. Inspect tare weight and unbroken seal before providing the OTP to the driver.
                </p>
                {devTestOtp && (
                  <div className="mt-1 p-2.5 rounded-xl bg-amber-50 border border-amber-300 text-amber-900 text-xs">
                    <span className="block font-bold text-[10px] uppercase tracking-wider text-amber-800">
                      {devTestNotice || 'Development Test Mode (No SMS Provider)'}
                    </span>
                    <div className="mt-1 flex items-center justify-between">
                      <span className="text-[11px] text-amber-700">Test OTP:</span>
                      <span className="font-mono font-black text-sm tracking-widest text-amber-950 bg-white px-2 py-0.5 rounded border border-amber-200">
                        {devTestOtp}
                      </span>
                    </div>
                  </div>
                )}
                <div className="text-[10px] text-slate-400 font-medium">
                  Valid for 10 minutes. Maximum 3 verification attempts.
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col gap-2">
                <div className="flex items-center gap-2 text-slate-800 font-bold text-xs">
                  <Lock size={15} className="text-slate-500" />
                  <span>Awaiting Doorstep Handover</span>
                </div>
                <p className="text-[11px] text-slate-600 leading-relaxed">
                  Your delivery OTP is confidential and generated only when the delivery personnel arrives at your doorstep to initiate handover.
                </p>
              </div>
            )}

            {/* Authenticated Driver Action Console */}
            {!isDelivered && (
              <div className="pt-3 border-t border-slate-100 flex flex-col gap-3">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  Driver Handover Action
                </span>

                {handoverError && (
                  <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-1.5">
                    <AlertCircle size={14} className="mt-0.5 flex-shrink-0" />
                    <span>{handoverError}</span>
                  </div>
                )}

                {handoverSuccess && (
                  <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs flex items-start gap-1.5">
                    <CheckCircle2 size={14} className="mt-0.5 flex-shrink-0" />
                    <span>{handoverSuccess}</span>
                  </div>
                )}

                {!handoverInitiated ? (
                  <button
                    type="button"
                    onClick={handleInitiateHandover}
                    disabled={isInitiatingHandover}
                    className="w-full py-2.5 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all shadow flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                  >
                    {isInitiatingHandover ? (
                      <>
                        <RefreshCw size={14} className="animate-spin" />
                        <span>Initiating Doorstep Handover...</span>
                      </>
                    ) : (
                      <>
                        <Send size={14} />
                        <span>Initiate Doorstep Handover</span>
                      </>
                    )}
                  </button>
                ) : (
                  <form onSubmit={handleVerifyOtp} className="flex flex-col gap-2">
                    <div className="flex gap-2">
                      <input
                        type="text"
                        maxLength={8}
                        value={otpInput}
                        onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ''))}
                        placeholder="Enter 6-digit OTP"
                        className="flex-1 px-3 py-2 rounded-xl border border-slate-300 font-mono text-xs tracking-widest text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500"
                      />
                      <button
                        type="submit"
                        disabled={isVerifyingOtp || !otpInput.trim()}
                        className="py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition-all shadow flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
                      >
                        {isVerifyingOtp ? (
                          <RefreshCw size={14} className="animate-spin" />
                        ) : (
                          <span>Verify</span>
                        )}
                      </button>
                    </div>
                    <div className="flex justify-between items-center text-[10px] text-slate-400">
                      <span>Customer provides OTP after check</span>
                      <button
                        type="button"
                        onClick={handleInitiateHandover}
                        disabled={isInitiatingHandover}
                        className="text-rose-600 hover:underline font-bold"
                      >
                        Resend OTP
                      </button>
                    </div>
                  </form>
                )}
              </div>
            )}
          </div>

          {/* Cylinder Safety Checklist */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <Scale size={16} className="text-rose-600" />
              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                Pre-Delivery Safety Check
              </h4>
            </div>
            <ul className="text-xs text-slate-600 space-y-2.5">
              <li className="flex items-start gap-2">
                <CheckCircle2 size={15} className="text-emerald-600 flex-shrink-0 mt-0.5" />
                <span>Check that plastic safety cap &amp; white seal are unbroken.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 size={15} className="text-emerald-600 flex-shrink-0 mt-0.5" />
                <span>Demand tare weight verification on digital spring balance.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 size={15} className="text-emerald-600 flex-shrink-0 mt-0.5" />
                <span>Total gross weight must equal tare + 14.2 kg (&plusmn;150g).</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
