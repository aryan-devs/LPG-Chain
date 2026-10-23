import React, { useState, useEffect } from 'react';
import {
  Flame,
  CheckCircle,
  Truck,
  MapPin,
  Clock,
  ShieldCheck,
  ArrowRight,
  Check,
  QrCode,
  CreditCard,
  AlertTriangle,
  RefreshCw,
  Copy,
  Printer,
  XCircle,
  Lock,
  Info,
} from 'lucide-react';
import QRCode from 'qrcode';
import { CustomerOrder, CustomerProfileData, SavedAddress, PortalTab } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';

interface PortalBookCylinderViewProps {
  profile: CustomerProfileData;
  addresses: SavedAddress[];
  onOrderCreated: (order: CustomerOrder) => void;
  onNavigateTab: (tab: PortalTab) => void;
}

export const PortalBookCylinderView: React.FC<PortalBookCylinderViewProps> = ({
  profile,
  addresses,
  onOrderCreated,
  onNavigateTab,
}) => {
  const [selectedCapacity, setSelectedCapacity] = useState<14.2 | 5 | 19>(14.2);
  const [quantity, setQuantity] = useState<number>(1);
  const [selectedAddressId, setSelectedAddressId] = useState<string>(
    addresses.find((a) => a.isDefault)?.id || addresses[0]?.id || 'addr-custom'
  );
  const [customAddress, setCustomAddress] = useState<string>('');
  const [deliverySlot, setDeliverySlot] = useState<string>('afternoon');
  const [paymentMethod, setPaymentMethod] = useState<'UPI' | 'Card' | 'Cash on Delivery'>('UPI');
  const [optInSubsidy, setOptInSubsidy] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [submissionError, setSubmissionError] = useState<string | null>(null);

  // Checkout flow state
  const [createdOrder, setCreatedOrder] = useState<CustomerOrder | null>(null);
  const [checkoutStep, setCheckoutStep] = useState<'FORM' | 'ONLINE_CHECKOUT' | 'SUCCESS'>('FORM');
  const [orderPaymentStatus, setOrderPaymentStatus] = useState<'PENDING' | 'PAID' | 'FAILED'>('PENDING');
  const [activePaymentTab, setActivePaymentTab] = useState<'UPI_QR' | 'CARD_DEMO'>('UPI_QR');

  // Online / Demo Payment state
  const [razorpayOrderMeta, setRazorpayOrderMeta] = useState<any>(null);
  const [dynamicQrUrl, setDynamicQrUrl] = useState<string>('');
  const [upiString, setUpiString] = useState<string>('');
  const [isSimulatingPayment, setIsSimulatingPayment] = useState<boolean>(false);
  const [isRetryingPayment, setIsRetryingPayment] = useState<boolean>(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [copiedUpi, setCopiedUpi] = useState<boolean>(false);
  const [txnReference, setTxnReference] = useState<string>('');

  const cylinderOptions = [
    {
      capacity: 14.2 as const,
      title: '14.2 KG Domestic Cylinder',
      tag: 'Most Popular • Subsidized',
      price: 853,
      subsidy: 200,
      description: 'Standard household cylinder suitable for families of 3-5 members.',
    },
    {
      capacity: 5 as const,
      title: '5 KG Mini Chhotu Cylinder',
      tag: 'Compact • Quick Refill',
      price: 340,
      subsidy: 0,
      description: 'Convenient portable cylinder ideal for students and singles.',
    },
    {
      capacity: 19 as const,
      title: '19 KG Jumbo Connection',
      tag: 'Heavy Household Use',
      price: 1850,
      subsidy: 0,
      description: 'High capacity cylinder for large families or catering functions.',
    },
  ];

  const selectedCylinder = cylinderOptions.find((c) => c.capacity === selectedCapacity)!;
  const baseTotal = selectedCylinder.price * quantity;
  const subsidyTotal = optInSubsidy && selectedCapacity === 14.2 ? selectedCylinder.subsidy * quantity : 0;
  const netPayable = baseTotal;

  // Breakdown for GST memo (5% GST: 2.5% CGST + 2.5% SGST)
  const taxableBase = Math.round((netPayable / 1.05) * 100) / 100;
  const totalTax = Math.round((netPayable - taxableBase) * 100) / 100;
  const cgstAmount = Math.round((totalTax / 2) * 100) / 100;
  const sgstAmount = Math.round((totalTax - cgstAmount) * 100) / 100;

  const currentAddressText =
    selectedAddressId === 'addr-custom'
      ? customAddress
      : addresses.find((a) => a.id === selectedAddressId)?.fullAddress || profile.address;

  // Generate dynamic QR code whenever UPI string or checkout step changes
  useEffect(() => {
    if (upiString) {
      QRCode.toDataURL(upiString, {
        width: 240,
        margin: 1,
        color: {
          dark: '#0f172a',
          light: '#ffffff',
        },
      })
        .then((url) => setDynamicQrUrl(url))
        .catch((err) => console.error('Failed to generate dynamic QR code:', err));
    }
  }, [upiString]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setSubmissionError(null);

    try {
      const order = await CustomerService.createOrder({
        cylinderType:
          selectedCapacity === 5
            ? '5 KG Mini'
            : selectedCapacity === 19
            ? '19 KG Commercial'
            : '14.2 KG Domestic',
        capacityKg: selectedCapacity,
        quantity,
        deliveryAddress: currentAddressText,
        paymentMethod,
      });

      setCreatedOrder(order);
      onOrderCreated(order);

      if (paymentMethod === 'Cash on Delivery') {
        // Cash on delivery: persists immediately, stays PENDING until authorized handover
        setOrderPaymentStatus('PENDING');
        setCheckoutStep('SUCCESS');
      } else {
        // Online payment (UPI or Card): Initialize Razorpay / Demo checkout
        const bookingRef = order.bookingIdNumber || order.id;
        try {
          const rzpOrder = await CustomerService.createRazorpayOrder(bookingRef);
          setRazorpayOrderMeta(rzpOrder);
          const ref = rzpOrder.paymentReference || `TXN-DEMO-${bookingRef}-${Date.now()}`;
          setTxnReference(ref);

          const generatedUpi = `upi://pay?pa=lpgchain@demo&pn=LPG-Chain%20Refill&am=${order.amount}&tr=${ref}&cu=INR&tn=Refill%20Order%20${order.id}`;
          setUpiString(generatedUpi);
        } catch {
          // Fallback demo reference
          const fallbackRef = `TXN-DEMO-${bookingRef}-${Date.now()}`;
          setTxnReference(fallbackRef);
          setUpiString(`upi://pay?pa=lpgchain@demo&pn=LPG-Chain%20Refill&am=${order.amount}&tr=${fallbackRef}&cu=INR`);
        }

        setOrderPaymentStatus('PENDING');
        setCheckoutStep('ONLINE_CHECKOUT');
      }
    } catch (err: any) {
      setSubmissionError(err.message || 'Failed to place booking. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSimulatePaymentSuccess = async () => {
    if (!createdOrder) return;
    setIsSimulatingPayment(true);
    setPaymentError(null);

    try {
      const refToSettle = txnReference || createdOrder.bookingIdNumber || createdOrder.id;
      await CustomerService.simulatePayment(String(refToSettle));
      setOrderPaymentStatus('PAID');
      setCheckoutStep('SUCCESS');
    } catch (err: any) {
      setPaymentError(err.message || 'Payment simulation failed. Please retry.');
      setOrderPaymentStatus('FAILED');
    } finally {
      setIsSimulatingPayment(false);
    }
  };

  const handleSimulatePaymentFailure = async () => {
    if (!createdOrder) return;
    setIsSimulatingPayment(true);
    try {
      const refToSettle = txnReference || createdOrder.bookingIdNumber || createdOrder.id;
      await CustomerService.simulateFailure(String(refToSettle), 'Customer simulated cancellation in demo checkout');
      setPaymentError('Simulated payment was cancelled or declined by user.');
      setOrderPaymentStatus('FAILED');
    } catch (err: any) {
      setPaymentError(err.message || 'Payment failure simulation failed');
      setOrderPaymentStatus('FAILED');
    } finally {
      setIsSimulatingPayment(false);
    }
  };

  const handleRetryPayment = async () => {
    if (!createdOrder) return;
    setIsRetryingPayment(true);
    setPaymentError(null);

    try {
      const bookingRef = createdOrder.bookingIdNumber || createdOrder.id;
      const retryRes = await CustomerService.retryPayment(bookingRef);
      const newRef = retryRes.transactionReference || `TXN-RETRY-${bookingRef}-${Date.now()}`;
      setTxnReference(newRef);
      setUpiString(`upi://pay?pa=lpgchain@demo&pn=LPG-Chain%20Refill&am=${createdOrder.amount}&tr=${newRef}&cu=INR`);
      setOrderPaymentStatus('PENDING');
    } catch (err: any) {
      setPaymentError(err.message || 'Failed to initialize payment retry');
    } finally {
      setIsRetryingPayment(false);
    }
  };

  const handleCopyUpi = () => {
    if (!upiString) return;
    navigator.clipboard.writeText(upiString);
    setCopiedUpi(true);
    setTimeout(() => setCopiedUpi(false), 2000);
  };

  // -------------------------------------------------------------
  // VIEW: Razorpay-Inspired Demo / Online Checkout Modal
  // -------------------------------------------------------------
  if (checkoutStep === 'ONLINE_CHECKOUT' && createdOrder) {
    return (
      <div className="max-w-2xl mx-auto py-8 px-4 animate-in fade-in duration-200">
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl overflow-hidden flex flex-col">
          {/* Razorpay-style Trusted Header */}
          <div className="bg-slate-900 text-white p-6 sm:p-7 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-rose-600 flex items-center justify-center text-white font-black text-lg shadow-md shadow-rose-600/40">
                <Flame size={24} />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-black tracking-tight text-white">LPG-Chain Checkout</h2>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold border border-emerald-500/30 flex items-center gap-1">
                    <Lock size={10} /> 256-Bit SSL Secured
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-0.5 font-mono">
                  Booking #{createdOrder.id} &bull; {createdOrder.cylinderType}
                </p>
              </div>
            </div>

            <div className="bg-slate-800/90 border border-slate-700 rounded-2xl px-4 py-2.5 text-right self-start sm:self-auto">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                Total Payable
              </span>
              <span className="text-xl font-black text-rose-400">₹{createdOrder.amount}.00</span>
            </div>
          </div>

          {/* Sandbox Test Mode Disclaimer Notice */}
          <div className="bg-amber-50 border-b border-amber-200/80 px-6 py-3 flex items-start gap-3 text-amber-900 text-xs">
            <Info size={16} className="text-amber-600 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold">
                {razorpayOrderMeta?.isConfigured
                  ? 'Razorpay Test Gateway Mode'
                  : 'Sandbox Demo Payment Mode — No Real Money Deducted'}
              </span>
              <p className="text-[11px] text-amber-800 mt-0.5">
                {razorpayOrderMeta?.isConfigured
                  ? `Connected to test gateway order ID: ${razorpayOrderMeta.razorpayOrderId}. Test payments supported.`
                  : 'This is a fully functional zero-cost prototype checkout. Test either the simulated success settlement or simulated failure/cancellation flow below.'}
              </p>
            </div>
          </div>

          {/* Payment Instrument Tabs */}
          <div className="flex border-b border-slate-100 bg-slate-50/50">
            <button
              type="button"
              onClick={() => setActivePaymentTab('UPI_QR')}
              className={`flex-1 py-3.5 px-4 text-xs font-bold flex items-center justify-center gap-2 border-b-2 transition-all ${
                activePaymentTab === 'UPI_QR'
                  ? 'border-rose-600 text-rose-600 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <QrCode size={16} />
              <span>UPI / Dynamic QR Code</span>
            </button>
            <button
              type="button"
              onClick={() => setActivePaymentTab('CARD_DEMO')}
              className={`flex-1 py-3.5 px-4 text-xs font-bold flex items-center justify-center gap-2 border-b-2 transition-all ${
                activePaymentTab === 'CARD_DEMO'
                  ? 'border-rose-600 text-rose-600 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <CreditCard size={16} />
              <span>Cards / NetBanking (Test)</span>
            </button>
          </div>

          <div className="p-6 sm:p-8 flex flex-col gap-6">
            {/* Payment Failed / Error Banner */}
            {orderPaymentStatus === 'FAILED' && (
              <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 flex flex-col gap-3 text-rose-900 animate-in fade-in">
                <div className="flex items-center gap-2">
                  <XCircle size={18} className="text-rose-600 shrink-0" />
                  <span className="text-xs font-bold">Payment Attempt Failed or Cancelled</span>
                </div>
                <p className="text-xs text-rose-700">
                  {paymentError || 'The simulated transaction was cancelled. No charges were made.'}
                </p>
                <div className="flex items-center gap-2 pt-1">
                  <button
                    type="button"
                    onClick={handleRetryPayment}
                    disabled={isRetryingPayment}
                    className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:bg-rose-300 text-white font-bold text-xs flex items-center gap-1.5 shadow-sm transition-all"
                  >
                    <RefreshCw size={14} className={isRetryingPayment ? 'animate-spin' : ''} />
                    <span>Retry Payment Now</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setOrderPaymentStatus('PENDING');
                      setCreatedOrder({ ...createdOrder, paymentMethod: 'Cash on Delivery' });
                      setCheckoutStep('SUCCESS');
                    }}
                    className="px-3.5 py-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-xs transition-colors"
                  >
                    Switch to Cash on Delivery
                  </button>
                </div>
              </div>
            )}

            {/* TAB 1: UPI & Dynamic QR Code */}
            {activePaymentTab === 'UPI_QR' && orderPaymentStatus !== 'FAILED' && (
              <div className="flex flex-col items-center gap-5 text-center">
                <div className="flex flex-col items-center gap-1">
                  <span className="text-xs font-bold text-slate-800">
                    Scan with any UPI App (GPay, PhonePe, Paytm)
                  </span>
                  <span className="text-[11px] text-slate-400">
                    Dynamic QR linked to Transaction Ref: <code className="text-slate-600 font-mono">{txnReference || createdOrder.id}</code>
                  </span>
                </div>

                {/* Dynamically Generated QR Code */}
                <div className="p-4 bg-white rounded-3xl border-2 border-slate-200 shadow-md flex items-center justify-center">
                  {dynamicQrUrl ? (
                    <img
                      src={dynamicQrUrl}
                      alt={`Dynamic UPI QR for LPG Refill ₹${createdOrder.amount}`}
                      className="w-48 h-48 rounded-xl object-contain"
                    />
                  ) : (
                    <div className="w-48 h-48 flex items-center justify-center bg-slate-50 text-slate-400">
                      <RefreshCw size={24} className="animate-spin text-rose-500" />
                    </div>
                  )}
                </div>

                {/* UPI Deep Link / Copy String */}
                <div className="w-full max-w-md bg-slate-50 border border-slate-200/90 rounded-2xl p-3 flex items-center justify-between gap-3 text-left">
                  <div className="min-w-0">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                      VPA / Merchant String
                    </span>
                    <p className="text-xs font-mono text-slate-700 truncate">
                      {upiString || `upi://pay?pa=lpgchain@demo&am=${createdOrder.amount}`}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={handleCopyUpi}
                    className="px-2.5 py-1.5 rounded-xl border border-slate-200 hover:bg-white text-slate-700 hover:text-rose-600 font-bold text-xs flex items-center gap-1 shrink-0 transition-colors"
                  >
                    {copiedUpi ? <Check size={14} className="text-emerald-600" /> : <Copy size={14} />}
                    <span>{copiedUpi ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>

                {/* Simulation Action Buttons */}
                <div className="w-full flex flex-col sm:flex-row items-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={handleSimulatePaymentSuccess}
                    disabled={isSimulatingPayment}
                    className="w-full py-3.5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-300 text-white font-bold text-xs shadow-md shadow-emerald-600/25 transition-all flex items-center justify-center gap-2"
                  >
                    {isSimulatingPayment ? (
                      <RefreshCw size={16} className="animate-spin" />
                    ) : (
                      <CheckCircle size={16} />
                    )}
                    <span>Simulate Payment Success (₹{createdOrder.amount})</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleSimulatePaymentFailure}
                    disabled={isSimulatingPayment}
                    className="w-full sm:w-auto px-5 py-3.5 rounded-2xl border border-slate-200 hover:bg-rose-50 hover:border-rose-300 text-slate-700 hover:text-rose-700 font-bold text-xs transition-colors whitespace-nowrap"
                  >
                    Simulate Cancel / Failure
                  </button>
                </div>
              </div>
            )}

            {/* TAB 2: Cards / NetBanking Demo */}
            {activePaymentTab === 'CARD_DEMO' && orderPaymentStatus !== 'FAILED' && (
              <div className="flex flex-col gap-4 text-left">
                <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between text-xs font-bold text-slate-700">
                    <span>Test Card Details</span>
                    <span className="text-[10px] bg-slate-200 text-slate-700 px-2 py-0.5 rounded-md font-mono">
                      Sandbox Safe
                    </span>
                  </div>
                  <div>
                    <label className="text-[11px] text-slate-500 block mb-1">Card Number</label>
                    <input
                      type="text"
                      readOnly
                      value="4111 2222 3333 4444"
                      className="w-full px-3 py-2 rounded-xl border border-slate-300 bg-white font-mono text-xs text-slate-800"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-[11px] text-slate-500 block mb-1">Expiry</label>
                      <input
                        type="text"
                        readOnly
                        value="12 / 28"
                        className="w-full px-3 py-2 rounded-xl border border-slate-300 bg-white font-mono text-xs text-slate-800"
                      />
                    </div>
                    <div>
                      <label className="text-[11px] text-slate-500 block mb-1">CVV</label>
                      <input
                        type="password"
                        readOnly
                        value="123"
                        className="w-full px-3 py-2 rounded-xl border border-slate-300 bg-white font-mono text-xs text-slate-800"
                      />
                    </div>
                  </div>
                </div>

                <div className="w-full flex flex-col sm:flex-row items-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={handleSimulatePaymentSuccess}
                    disabled={isSimulatingPayment}
                    className="w-full py-3.5 rounded-2xl bg-slate-900 hover:bg-slate-800 disabled:bg-slate-300 text-white font-bold text-xs shadow-md transition-all flex items-center justify-center gap-2"
                  >
                    {isSimulatingPayment ? (
                      <RefreshCw size={16} className="animate-spin" />
                    ) : (
                      <CheckCircle size={16} />
                    )}
                    <span>Authorize Test Card Payment (₹{createdOrder.amount})</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleSimulatePaymentFailure}
                    disabled={isSimulatingPayment}
                    className="w-full sm:w-auto px-5 py-3.5 rounded-2xl border border-slate-200 hover:bg-rose-50 text-slate-700 font-bold text-xs transition-colors whitespace-nowrap"
                  >
                    Simulate Decline
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="p-4 bg-slate-50/80 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
            <span>Server-side verification enabled</span>
            <button
              type="button"
              onClick={() => setCheckoutStep('FORM')}
              className="text-slate-600 hover:text-slate-900 font-bold underline"
            >
              Cancel &amp; Modify Order
            </button>
          </div>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // VIEW: Booking Confirmation & Digital Tax Invoice
  // -------------------------------------------------------------
  if (checkoutStep === 'SUCCESS' && createdOrder) {
    const isCod = createdOrder.paymentMethod === 'Cash on Delivery';
    const isPaid = orderPaymentStatus === 'PAID';

    return (
      <div className="max-w-3xl mx-auto py-8 px-4 animate-in zoom-in-95 duration-200">
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl p-6 sm:p-10 flex flex-col gap-6">
          {/* Status Header */}
          <div className="text-center flex flex-col items-center gap-3">
            <div
              className={`w-16 h-16 rounded-full flex items-center justify-center ${
                isPaid
                  ? 'bg-emerald-100 text-emerald-600'
                  : 'bg-amber-100 text-amber-600'
              }`}
            >
              {isPaid ? <CheckCircle size={38} /> : <Clock size={38} />}
            </div>

            <div>
              <span
                className={`px-3 py-1 rounded-full text-xs font-bold border ${
                  isPaid
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-amber-50 text-amber-800 border-amber-200'
                }`}
              >
                {isPaid ? 'Refill Order Confirmed &amp; Paid' : 'Refill Order Placed &bull; Pay on Delivery'}
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                Booking Reference: #{createdOrder.id}
              </h2>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                Transaction ID: {txnReference || (isCod ? 'COD-HANDOVER-PENDING' : `TXN-${createdOrder.id}`)}
              </p>
            </div>
          </div>

          {/* Notice for COD vs Online Paid */}
          {isCod ? (
            <div className="rounded-2xl bg-amber-50 border border-amber-200/80 p-4 flex items-start gap-3 text-amber-900 text-xs">
              <Clock size={18} className="text-amber-700 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Cash on Delivery Handover Protocol</span>
                <p className="text-[11px] text-amber-800 mt-0.5 leading-relaxed">
                  Please keep exact cash of <strong>₹{createdOrder.amount}.00</strong> ready. Hand over the cash to the authorized delivery personnel upon physical cylinder delivery. The delivery executive will verify your 6-digit delivery OTP and record cash collection in real-time.
                </p>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl bg-emerald-50 border border-emerald-200/80 p-4 flex items-start gap-3 text-emerald-900 text-xs">
              <ShieldCheck size={18} className="text-emerald-700 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Demo Payment Verified &amp; Settled</span>
                <p className="text-[11px] text-emerald-800 mt-0.5">
                  Payment for booking #{createdOrder.id} has been recorded successfully in the database. Your refill has been placed into the distributor's priority dispatch queue.
                </p>
              </div>
            </div>
          )}

          {/* Digital Tax Invoice / Cash Memo Breakdown */}
          <div className="bg-slate-50/70 rounded-2xl border border-slate-200 p-5 sm:p-6 flex flex-col gap-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                  LPG Refill Digital Tax Memo
                </h3>
                <span className="text-[11px] text-slate-400">
                  Consumer ID: {profile.consumerNumber} &bull; Distributor: {profile.distributorName}
                </span>
              </div>
              <span className="px-2.5 py-1 rounded-lg bg-white border border-slate-200 text-slate-800 text-[11px] font-bold">
                GST Compliant
              </span>
            </div>

            <div className="flex flex-col gap-2.5 text-xs text-slate-600">
              <div className="flex justify-between">
                <span>Cylinder Refill ({createdOrder.quantity}x {createdOrder.cylinderType}):</span>
                <span className="font-bold text-slate-800">₹{taxableBase}.00</span>
              </div>
              <div className="flex justify-between">
                <span>Central GST (CGST 2.5%):</span>
                <span className="font-semibold text-slate-800">₹{cgstAmount}.00</span>
              </div>
              <div className="flex justify-between">
                <span>State GST (SGST 2.5%):</span>
                <span className="font-semibold text-slate-800">₹{sgstAmount}.00</span>
              </div>
              <div className="flex justify-between text-emerald-700 bg-emerald-50/60 p-2 rounded-xl">
                <span>PAHAL (DBTL) Subsidy Benefit (to be credited to bank):</span>
                <span className="font-bold">-₹{createdOrder.subsidyAmount}.00</span>
              </div>
              <div className="flex justify-between">
                <span>Delivery Address:</span>
                <span className="font-medium text-slate-800 text-right max-w-xs truncate">
                  {createdOrder.deliveryAddress}
                </span>
              </div>
              <div className="flex justify-between">
                <span>Payment Mode &amp; Status:</span>
                <span className="font-bold text-slate-900">
                  {createdOrder.paymentMethod} &bull;{' '}
                  <span className={isPaid ? 'text-emerald-600' : 'text-amber-600'}>
                    {isPaid ? 'PAID' : 'PENDING'}
                  </span>
                </span>
              </div>
              <div className="pt-3 border-t border-slate-200 flex justify-between items-baseline text-sm font-black text-slate-900">
                <span>Total Amount:</span>
                <span className="text-lg text-rose-600">₹{createdOrder.amount}.00</span>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row items-center gap-3 w-full pt-2">
            <button
              type="button"
              onClick={() => onNavigateTab('track')}
              className="w-full py-3.5 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm shadow-lg shadow-rose-600/30 transition-all flex items-center justify-center gap-2"
            >
              <Truck size={18} />
              <span>Track Live Delivery Now</span>
            </button>
            <button
              type="button"
              onClick={() => window.print()}
              className="w-full sm:w-auto px-5 py-3.5 rounded-2xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-sm flex items-center justify-center gap-2 transition-colors whitespace-nowrap"
            >
              <Printer size={16} />
              <span>Print Tax Memo</span>
            </button>
            <button
              type="button"
              onClick={() => onNavigateTab('dashboard')}
              className="w-full sm:w-auto px-5 py-3.5 rounded-2xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-sm transition-colors whitespace-nowrap"
            >
              Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // VIEW: Main Booking Form
  // -------------------------------------------------------------
  return (
    <div className="max-w-5xl mx-auto flex flex-col gap-8 pb-12">
      {/* Header */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
          Book LPG Cylinder
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Instant booking, automatic distributor allocation, and direct DBTL subsidy crediting.
        </p>
      </div>

      {submissionError && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 flex items-center gap-3 text-rose-800 text-xs font-medium">
          <AlertTriangle size={18} className="text-rose-600 shrink-0" />
          <span>{submissionError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Cylinder Selection & Address (8 cols) */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          {/* 1. Cylinder Capacity */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <label className="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
                <Flame size={16} className="text-rose-600" />
                Select Cylinder Capacity
              </label>
              <span className="text-xs text-slate-500 font-medium">
                Quota Remaining: {profile.subsidizedQuotaLeft} cylinders
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {cylinderOptions.map((opt) => {
                const isSelected = selectedCapacity === opt.capacity;
                return (
                  <button
                    key={opt.capacity}
                    type="button"
                    onClick={() => setSelectedCapacity(opt.capacity)}
                    className={`p-4 rounded-2xl border text-left transition-all flex flex-col justify-between ${
                      isSelected
                        ? 'border-rose-600 bg-rose-50/40 ring-2 ring-rose-600/20'
                        : 'border-slate-200 hover:border-slate-300 bg-white'
                    }`}
                  >
                    <div>
                      <span className="inline-block px-2 py-0.5 rounded-md bg-slate-100 text-[10px] font-bold text-slate-600 mb-2">
                        {opt.tag}
                      </span>
                      <h3 className="text-sm font-bold text-slate-900">{opt.title}</h3>
                      <p className="text-[11px] text-slate-500 mt-1 leading-tight">
                        {opt.description}
                      </p>
                    </div>
                    <div className="mt-4 pt-2 border-t border-slate-100 flex items-baseline justify-between">
                      <span className="text-base font-black text-slate-900">₹{opt.price}</span>
                      {opt.subsidy > 0 && (
                        <span className="text-[10px] font-bold text-emerald-600">
                          -₹{opt.subsidy} Subsidy
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Quantity Selector */}
            <div className="pt-2 flex items-center justify-between border-t border-slate-100">
              <span className="text-xs font-bold text-slate-700">Refill Quantity:</span>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="w-8 h-8 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 font-bold flex items-center justify-center"
                >
                  -
                </button>
                <span className="text-sm font-bold text-slate-900">{quantity}</span>
                <button
                  type="button"
                  onClick={() => setQuantity(Math.min(3, quantity + 1))}
                  className="w-8 h-8 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 font-bold flex items-center justify-center"
                >
                  +
                </button>
              </div>
            </div>
          </div>

          {/* 2. Delivery Address */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 flex flex-col gap-4">
            <label className="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <MapPin size={16} className="text-rose-600" />
              Delivery Address
            </label>

            <div className="flex flex-col gap-2.5">
              {addresses.map((addr) => (
                <label
                  key={addr.id}
                  className={`flex items-start gap-3 p-3.5 rounded-2xl border cursor-pointer transition-all ${
                    selectedAddressId === addr.id
                      ? 'border-rose-600 bg-rose-50/30'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="address"
                    value={addr.id}
                    checked={selectedAddressId === addr.id}
                    onChange={() => setSelectedAddressId(addr.id)}
                    className="mt-1 text-rose-600 focus:ring-rose-500"
                  />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-900">{addr.label}</span>
                      {addr.isDefault && (
                        <span className="px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-700 text-[10px] font-bold">
                          Default
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-600 mt-0.5 leading-relaxed">
                      {addr.fullAddress}, {addr.city} - {addr.pincode}
                    </p>
                  </div>
                </label>
              ))}

              <label
                className={`flex items-start gap-3 p-3.5 rounded-2xl border cursor-pointer transition-all ${
                  selectedAddressId === 'addr-custom'
                    ? 'border-rose-600 bg-rose-50/30'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <input
                  type="radio"
                  name="address"
                  value="addr-custom"
                  checked={selectedAddressId === 'addr-custom'}
                  onChange={() => setSelectedAddressId('addr-custom')}
                  className="mt-1 text-rose-600 focus:ring-rose-500"
                />
                <div className="flex-1">
                  <span className="text-xs font-bold text-slate-900">Custom Address for this refill</span>
                  {selectedAddressId === 'addr-custom' && (
                    <textarea
                      rows={2}
                      value={customAddress}
                      onChange={(e) => setCustomAddress(e.target.value)}
                      placeholder="Enter full flat number, building name, street and landmark..."
                      className="w-full mt-2 p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:ring-2 focus:ring-rose-500 focus:outline-none"
                      required={selectedAddressId === 'addr-custom'}
                    />
                  )}
                </div>
              </label>
            </div>
          </div>

          {/* 3. Delivery Slot & DBTL Subsidy */}
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 flex flex-col gap-4">
            <label className="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <Clock size={16} className="text-rose-600" />
              Preferred Delivery Slot
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {[
                { id: 'morning', label: 'Morning Slot', time: '09:00 AM - 01:00 PM' },
                { id: 'afternoon', label: 'Afternoon Slot', time: '01:00 PM - 05:00 PM' },
                { id: 'evening', label: 'Evening Slot', time: '05:00 PM - 08:30 PM' },
              ].map((slot) => (
                <button
                  key={slot.id}
                  type="button"
                  onClick={() => setDeliverySlot(slot.id)}
                  className={`p-3 rounded-2xl border text-left transition-all ${
                    deliverySlot === slot.id
                      ? 'border-rose-600 bg-rose-50/40 font-bold'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <p className="text-xs font-bold text-slate-800">{slot.label}</p>
                  <p className="text-[11px] text-slate-500 mt-0.5">{slot.time}</p>
                </button>
              ))}
            </div>

            {/* Subsidy Opt-In Toggle */}
            {selectedCapacity === 14.2 && (
              <div className="mt-2 p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200/80 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center">
                    <ShieldCheck size={18} />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-emerald-900">
                      PAHAL (DBTL) Subsidy Benefit
                    </p>
                    <p className="text-[11px] text-emerald-700">
                      Direct cash transfer of ₹200 will be credited to linked bank A/C ending in 7891.
                    </p>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={optInSubsidy}
                  onChange={(e) => setOptInSubsidy(e.target.checked)}
                  className="w-5 h-5 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500"
                />
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Order Summary & Checkout (4 cols) */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sticky top-24 flex flex-col gap-6">
            <h2 className="text-base font-bold text-slate-900 pb-3 border-b border-slate-100">
              Payment Summary
            </h2>

            {/* Price Breakdown */}
            <div className="flex flex-col gap-3 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Refill Base Price ({quantity}x):</span>
                <span className="font-semibold text-slate-800">₹{baseTotal}.00</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Distributor Delivery Fee:</span>
                <span className="font-semibold text-emerald-600">FREE</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Taxes &amp; GST (5%):</span>
                <span className="font-semibold text-slate-800">Included</span>
              </div>
              {subsidyTotal > 0 && (
                <div className="flex justify-between text-emerald-700 font-semibold bg-emerald-50 p-2 rounded-xl">
                  <span>DBTL Subsidy Cashback:</span>
                  <span>-₹{subsidyTotal}.00</span>
                </div>
              )}
              <div className="pt-3 border-t border-slate-200 flex justify-between items-baseline">
                <span className="text-sm font-bold text-slate-900">Amount Payable:</span>
                <span className="text-xl font-black text-rose-600">₹{netPayable}.00</span>
              </div>
            </div>

            {/* Payment Mode Selector */}
            <div className="flex flex-col gap-2 pt-2 border-t border-slate-100">
              <label className="text-xs font-bold text-slate-700">Payment Option:</label>
              {(['UPI', 'Card', 'Cash on Delivery'] as const).map((method) => (
                <button
                  key={method}
                  type="button"
                  onClick={() => setPaymentMethod(method)}
                  className={`p-2.5 rounded-xl border text-left text-xs font-semibold flex items-center justify-between transition-all ${
                    paymentMethod === method
                      ? 'border-rose-600 bg-rose-50/40 text-rose-700'
                      : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    {method === 'UPI' && <QrCode size={14} className="text-rose-600" />}
                    {method === 'Card' && <CreditCard size={14} className="text-rose-600" />}
                    {method === 'Cash on Delivery' && <Clock size={14} className="text-amber-600" />}
                    <span>{method}</span>
                  </div>
                  {paymentMethod === method && <Check size={14} className="text-rose-600" />}
                </button>
              ))}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 rounded-2xl bg-rose-600 hover:bg-rose-500 disabled:bg-slate-300 text-white font-bold text-sm shadow-md shadow-rose-600/30 transition-all flex items-center justify-center gap-2"
            >
              {isSubmitting ? (
                <span>Dispatching Refill...</span>
              ) : (
                <>
                  <span>
                    {paymentMethod === 'Cash on Delivery'
                      ? `Confirm Cash on Delivery (₹${netPayable})`
                      : `Proceed to Payment (₹${netPayable})`}
                  </span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>

            <p className="text-[10px] text-slate-400 text-center leading-relaxed">
              Safe OTP delivery verification will be required upon physical cylinder handover.
            </p>
          </div>
        </div>
      </form>
    </div>
  );
};
