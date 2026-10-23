import React, { useState } from 'react';
import {
  Inbox,
  CheckCircle,
  XCircle,
  AlertCircle,
  RefreshCw,
  Search,
  Calendar,
  MapPin,
  Flame,
  User,
} from 'lucide-react';
import { DistributorBookingItem, isBookingPending } from '../../../types/distributorPortalTypes';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorBookingRequestsViewProps {
  bookings: DistributorBookingItem[];
  isLoading: boolean;
  onRefresh: () => void;
  onBookingUpdated: (updatedBooking?: DistributorBookingItem) => Promise<void> | void;
}

export const DistributorBookingRequestsView: React.FC<DistributorBookingRequestsViewProps> = ({
  bookings,
  isLoading,
  onRefresh,
  onBookingUpdated,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [rejectModalBooking, setRejectModalBooking] = useState<DistributorBookingItem | null>(null);
  const [rejectReason, setRejectReason] = useState('Out of physical stock at depot');
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Filter only PENDING booking requests using robust case/enum normalization
  const pendingBookings = bookings.filter((b) => isBookingPending(b.status));

  const filtered = pendingBookings.filter((b) => {
    const q = searchTerm.toLowerCase();
    return (
      b.id.toString().includes(q) ||
      b.customerName.toLowerCase().includes(q) ||
      b.deliveryAddress.toLowerCase().includes(q) ||
      b.cylinderCategory.toLowerCase().includes(q)
    );
  });

  const handleAccept = async (booking: DistributorBookingItem) => {
    if (actionLoadingId !== null) return; // prevent conflicting simultaneous requests
    try {
      setActionLoadingId(booking.id);
      setActionError(null);
      setActionSuccess(null);
      const updated = await DistributorApiService.updateBookingStatus(booking.id, 'CONFIRMED');
      setActionSuccess(`Booking #${booking.id} for ${booking.customerName} accepted and queued for dispatch.`);
      await onBookingUpdated(updated);
    } catch (err: any) {
      setActionError(err.message || 'Failed to accept booking request');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleConfirmReject = async () => {
    if (!rejectModalBooking || actionLoadingId !== null) return;
    try {
      setActionLoadingId(rejectModalBooking.id);
      setActionError(null);
      setActionSuccess(null);
      const updated = await DistributorApiService.updateBookingStatus(
        rejectModalBooking.id,
        'CANCELLED',
        rejectReason
      );
      setActionSuccess(`Booking #${rejectModalBooking.id} rejected (${rejectReason}).`);
      setRejectModalBooking(null);
      await onBookingUpdated(updated);
    } catch (err: any) {
      setActionError(err.message || 'Failed to reject booking request');
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-rose-50 text-rose-600">
              <Inbox size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600">
              Agency Inbound Queue
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">New Booking Requests</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Incoming customer bookings assigned to your authorized agency depot for acceptance.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              setActionError(null);
              setActionSuccess(null);
              onRefresh();
            }}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors"
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {actionSuccess && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between gap-2 shadow-xs animate-in fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle size={16} className="shrink-0 text-emerald-600" />
            <span className="font-medium">{actionSuccess}</span>
          </div>
          <button
            onClick={() => setActionSuccess(null)}
            className="p-1 rounded-md text-emerald-600 hover:bg-emerald-100 transition-colors font-bold text-xs"
            aria-label="Dismiss message"
          >
            ✕
          </button>
        </div>
      )}

      {actionError && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center justify-between gap-2 shadow-xs animate-in fade-in">
          <div className="flex items-center gap-2">
            <AlertCircle size={16} className="shrink-0" />
            <span className="font-medium">{actionError}</span>
          </div>
          <button
            onClick={() => setActionError(null)}
            className="p-1 rounded-md text-rose-600 hover:bg-rose-100 transition-colors font-bold text-xs"
            aria-label="Dismiss error"
          >
            ✕
          </button>
        </div>
      )}

      {/* Search & Counter Ribbon */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative max-w-sm w-full">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search by ID, customer name, address..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-white border border-slate-200 focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500"
          />
        </div>

        <div className="text-xs text-slate-500 font-medium">
          Showing <strong>{filtered.length}</strong> of <strong>{pendingBookings.length}</strong> pending requests
        </div>
      </div>

      {/* Booking Requests Cards */}
      {isLoading && pendingBookings.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <RefreshCw className="w-6 h-6 text-rose-600 animate-spin mx-auto mb-2" />
          <p className="text-xs text-slate-500 font-semibold">Loading booking requests from backend...</p>
        </div>
      ) : filtered.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((b) => (
            <div
              key={b.id}
              className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-slate-300 transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="font-mono text-xs font-black text-rose-600 bg-rose-50 px-2 py-0.5 rounded-md border border-rose-100">
                    #{b.id}
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
                    PENDING REVIEW
                  </span>
                </div>

                <div className="flex items-center gap-2 mb-2">
                  <div className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 text-xs font-bold">
                    <User size={14} />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-900 leading-tight">{b.customerName}</h3>
                    <span className="text-[10px] text-slate-400">Customer ID #{b.customerId}</span>
                  </div>
                </div>

                <div className="space-y-1.5 text-xs text-slate-600 my-3">
                  <div className="flex items-center gap-1.5">
                    <Flame size={13} className="text-rose-500 shrink-0" />
                    <span>
                      <strong>{b.quantity}x</strong> {b.cylinderCategory} ({b.capacityKg} kg)
                    </span>
                  </div>
                  <div className="flex items-start gap-1.5 text-[11px] text-slate-500">
                    <MapPin size={13} className="text-slate-400 shrink-0 mt-0.5" />
                    <span className="line-clamp-2">{b.deliveryAddress}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-[10px] text-slate-400">
                    <Calendar size={12} className="text-slate-400 shrink-0" />
                    <span>Booked on {b.bookingDate}</span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-3 border-t border-slate-100 flex items-center gap-2 mt-2">
                <button
                  disabled={actionLoadingId !== null}
                  onClick={() => handleAccept(b)}
                  className="flex-1 flex items-center justify-center gap-1 py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {actionLoadingId === b.id ? (
                    <RefreshCw size={12} className="animate-spin" />
                  ) : (
                    <CheckCircle size={14} />
                  )}
                  <span>Accept & Queue</span>
                </button>

                <button
                  disabled={actionLoadingId !== null}
                  onClick={() => setRejectModalBooking(b)}
                  className="py-2 px-3 rounded-xl border border-rose-200 hover:bg-rose-50 text-rose-600 text-xs font-bold transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                  title="Reject booking with reason"
                >
                  <XCircle size={15} />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <Inbox className="w-10 h-10 text-slate-300 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-800">No Pending Booking Requests</h3>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            All customer requests assigned to your depot have been reviewed. Accepted orders are in the delivery queue.
          </p>
        </div>
      )}

      {/* Rejection Modal */}
      {rejectModalBooking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center gap-2 text-rose-600">
              <XCircle size={20} />
              <h2 className="text-base font-bold text-slate-900">Reject Booking Request</h2>
            </div>
            <p className="text-xs text-slate-600">
              You are rejecting booking #{rejectModalBooking.id} for customer{' '}
              <strong>{rejectModalBooking.customerName}</strong>. Please select a valid operational reason:
            </p>

            <select
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full text-xs p-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-rose-500 font-medium"
            >
              <option value="Out of physical stock at depot">Out of physical stock at depot</option>
              <option value="Customer delivery address outside service PIN code">
                Customer delivery address outside service PIN code
              </option>
              <option value="Vehicle capacity temporarily full">Vehicle capacity temporarily full</option>
              <option value="Duplicate or incomplete consumer address details">
                Duplicate or incomplete consumer address details
              </option>
            </select>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setRejectModalBooking(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
              >
                Cancel
              </button>
              <button
                disabled={actionLoadingId === rejectModalBooking.id}
                onClick={handleConfirmReject}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-xs"
              >
                {actionLoadingId === rejectModalBooking.id ? 'Processing...' : 'Confirm Rejection'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
