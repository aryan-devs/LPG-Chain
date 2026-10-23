import React, { useState } from 'react';
import { X, CheckCircle, Flame, MapPin, Phone, User, Clock, ArrowRight } from 'lucide-react';

interface BookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTrackOrder: (deliveryId: string) => void;
}

export const BookingModal: React.FC<BookingModalProps> = ({
  isOpen,
  onClose,
  onTrackOrder,
}) => {
  const [cylinderSize, setCylinderSize] = useState<'14.2' | '19.0'>('14.2');
  const [consumerName, setConsumerName] = useState('Anand Verma');
  const [address, setAddress] = useState('Flat 402, Royal Palms, Sector 14, Navi Mumbai');
  const [phone, setPhone] = useState('+91 98201 54321');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [generatedDeliveryId, setGeneratedDeliveryId] = useState('1');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    // Simulate instant dispatch handshake
    setTimeout(() => {
      setIsSubmitting(false);
      setIsSuccess(true);
      const randomId = String(Math.floor(Math.random() * 8) + 1);
      setGeneratedDeliveryId(randomId);
      if (typeof window !== 'undefined') {
        localStorage.setItem('lpg_has_active_delivery', 'true');
        localStorage.setItem('lpg_customer_active_delivery', randomId);
        window.dispatchEvent(new CustomEvent('lpg_delivery_updated', { detail: { deliveryId: randomId } }));
      }
    }, 700);
  };

  const handleTrackNow = () => {
    onClose();
    onTrackOrder(generatedDeliveryId);
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="booking-modal-title"
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-600">
              <Flame size={18} />
            </div>
            <div>
              <h2 id="booking-modal-title" className="text-base font-bold text-slate-900">
                Book LPG Cylinder Refill
              </h2>
              <p className="text-xs text-slate-500">
                Automated Allocation &bull; Sub-Second Dispatch
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close dialog"
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6">
          {!isSuccess ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Cylinder Selection */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">
                  Select Cylinder Capacity
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setCylinderSize('14.2')}
                    className={`p-3 rounded-2xl border text-left transition-all ${
                      cylinderSize === '14.2'
                        ? 'border-rose-500 bg-rose-50/60 ring-2 ring-rose-500/20'
                        : 'border-slate-200 hover:border-slate-300 bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-sm text-slate-900">14.2 kg Domestic</span>
                      <span className="text-xs font-semibold text-rose-600">&#8377;853.00</span>
                    </div>
                    <span className="text-[11px] text-slate-500">Standard residential refill</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setCylinderSize('19.0')}
                    className={`p-3 rounded-2xl border text-left transition-all ${
                      cylinderSize === '19.0'
                        ? 'border-rose-500 bg-rose-50/60 ring-2 ring-rose-500/20'
                        : 'border-slate-200 hover:border-slate-300 bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-sm text-slate-900">19.0 kg Commercial</span>
                      <span className="text-xs font-semibold text-rose-600">&#8377;1,780.00</span>
                    </div>
                    <span className="text-[11px] text-slate-500">Hotels &amp; food enterprises</span>
                  </button>
                </div>
              </div>

              {/* Delivery Address */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
                  Delivery Destination
                </label>
                <div className="relative">
                  <MapPin size={16} className="absolute left-3.5 top-3 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    placeholder="Enter street and door delivery address..."
                    className="w-full pl-10 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm text-slate-800 focus:outline-none focus:border-rose-500 focus:bg-white transition-colors"
                  />
                </div>
              </div>

              {/* Consumer Info Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1">
                    Consumer Name
                  </label>
                  <div className="relative">
                    <User size={15} className="absolute left-3 top-2.5 text-slate-400" />
                    <input
                      type="text"
                      required
                      value={consumerName}
                      onChange={(e) => setConsumerName(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm text-slate-800 focus:outline-none focus:border-rose-500 focus:bg-white transition-colors"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1">
                    Phone Number
                  </label>
                  <div className="relative">
                    <Phone size={15} className="absolute left-3 top-2.5 text-slate-400" />
                    <input
                      type="text"
                      required
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm text-slate-800 focus:outline-none focus:border-rose-500 focus:bg-white transition-colors"
                    />
                  </div>
                </div>
              </div>

              {/* IoT Sentinel Safety Guarantee Banner */}
              <div className="p-3 bg-amber-50/70 border border-amber-200/80 rounded-2xl flex items-center gap-2.5 text-xs text-amber-800">
                <Clock size={16} className="text-amber-600 flex-shrink-0" />
                <span>
                  <strong>IoT Tare Checked:</strong> Cylinder tare variance certified under 15g with tamper-evident digital seal.
                </span>
              </div>

              {/* Submit Buttons */}
              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900 rounded-xl hover:bg-slate-100 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2.5 bg-rose-600 hover:bg-rose-500 text-white text-xs sm:text-sm font-bold rounded-xl shadow-lg shadow-rose-600/30 transition-all flex items-center gap-2 disabled:opacity-50"
                >
                  {isSubmitting ? 'Allocating Nearest Depot...' : 'Confirm Instant Booking'}
                  <ArrowRight size={15} />
                </button>
              </div>
            </form>
          ) : (
            /* Success confirmation state */
            <div className="py-4 text-center space-y-4">
              <div className="w-14 h-14 mx-auto rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-600 flex items-center justify-center">
                <CheckCircle size={32} />
              </div>

              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  Refill Allocated &amp; Dispatched!
                </h3>
                <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                  Delivery Order <strong>#LPG-2024-{generatedDeliveryId}</strong> has been assigned to Delivery Partner Vehicle #{generatedDeliveryId}.
                </p>
              </div>

              <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 text-left max-w-sm mx-auto text-xs space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-500">Cylinder Type:</span>
                  <span className="font-semibold text-slate-800">{cylinderSize} kg Domestic</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Live Delivery Vehicle:</span>
                  <span className="font-mono font-bold text-blue-600">Truck #{generatedDeliveryId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Estimated Delivery:</span>
                  <span className="font-semibold text-emerald-600">22 mins (En Route)</span>
                </div>
              </div>

              <div className="flex items-center justify-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900 rounded-xl hover:bg-slate-100 transition-colors"
                >
                  Close
                </button>
                <button
                  type="button"
                  onClick={handleTrackNow}
                  className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs sm:text-sm font-bold rounded-xl shadow-lg shadow-blue-600/30 transition-all flex items-center gap-2"
                >
                  <span>Track Delivery Live</span>
                  <ArrowRight size={15} />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
