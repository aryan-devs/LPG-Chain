import React, { useState } from 'react';
import {
  CalendarDays,
  Truck,
  User,
  Phone,
  MapPin,
  RefreshCw,
  Send,
  AlertCircle,
  Search,
} from 'lucide-react';
import { DistributorDeliveryItem } from '../../../types/distributorPortalTypes';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorDeliveryScheduleViewProps {
  deliveries: DistributorDeliveryItem[];
  isLoading: boolean;
  onRefresh: () => void;
  onDeliveryUpdated: () => void;
  onNavigateTab: (tab: any) => void;
}

export const DistributorDeliveryScheduleView: React.FC<DistributorDeliveryScheduleViewProps> = ({
  deliveries,
  isLoading,
  onRefresh,
  onDeliveryUpdated,
  onNavigateTab,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Filter only scheduled ASSIGNED deliveries
  const scheduledDeliveries = deliveries.filter((d) => d.status === 'ASSIGNED');

  const filtered = scheduledDeliveries.filter((d) => {
    const q = searchTerm.toLowerCase();
    const veh = d.vehicleNumber || d.vehicleRegistration || '';
    return (
      d.id.toString().includes(q) ||
      (d.bookingId != null && d.bookingId.toString().includes(q)) ||
      (d.customerName && d.customerName.toLowerCase().includes(q)) ||
      (d.driverName && d.driverName.toLowerCase().includes(q)) ||
      veh.toLowerCase().includes(q) ||
      (d.deliveryAddress && d.deliveryAddress.toLowerCase().includes(q))
    );
  });

  const handleDispatch = async (delivery: DistributorDeliveryItem) => {
    try {
      setActionLoadingId(delivery.id);
      setError(null);
      await DistributorApiService.updateDeliveryStatus(delivery.id, 'OUT_FOR_DELIVERY');
      onDeliveryUpdated();
    } catch (err: any) {
      setError(err.message || 'Failed to dispatch delivery');
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-blue-50 text-blue-600">
              <CalendarDays size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-blue-600">
              Fleet Logistics Desk
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Scheduled Delivery Dispatches</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Deliveries assigned to drivers and vehicles. Click "Dispatch" when the delivery truck leaves the depot.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={onRefresh}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors"
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2">
          <AlertCircle size={16} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Search & Stats */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative max-w-sm w-full">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search scheduled dispatches..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-white border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        <div className="text-xs text-slate-500 font-medium">
          <strong>{filtered.length}</strong> scheduled dispatches ready at depot
        </div>
      </div>

      {/* Deliveries List */}
      {isLoading && scheduledDeliveries.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <RefreshCw className="w-6 h-6 text-blue-600 animate-spin mx-auto mb-2" />
          <p className="text-xs text-slate-500 font-semibold">Loading delivery schedules from backend...</p>
        </div>
      ) : filtered.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((d) => (
            <div
              key={d.id}
              className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-blue-300 transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="font-mono text-xs font-black text-blue-600 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-100">
                    Dispatch #{d.id}
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                    ASSIGNED / SCHEDULED
                  </span>
                </div>

                <div className="space-y-2 text-xs text-slate-700 mb-4">
                  <div>
                    <span className="text-[10px] text-slate-400 block">Customer & Booking</span>
                    <strong className="text-slate-900">{d.customerName || `Customer #${d.bookingId}`}</strong>
                    <span className="text-slate-500 block text-[11px]">Booking #{d.bookingId}</span>
                  </div>

                  <div className="flex items-start gap-1.5 text-[11px] text-slate-500">
                    <MapPin size={13} className="text-slate-400 shrink-0 mt-0.5" />
                    <span className="line-clamp-2">{d.deliveryAddress || 'Standard delivery address'}</span>
                  </div>

                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500 flex items-center gap-1.5 text-[11px]">
                        <User size={13} className="text-slate-400" />
                        Driver:
                      </span>
                      <strong className="text-slate-800">{d.driverName || `Driver #${d.driverId}`}</strong>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-slate-500 flex items-center gap-1.5 text-[11px]">
                        <Truck size={13} className="text-slate-400" />
                        Vehicle:
                      </span>
                      <strong className="text-slate-800 font-mono">
                        {d.vehicleNumber || d.vehicleRegistration || `Vehicle #${d.vehicleId}`}
                      </strong>
                    </div>

                    {d.driverPhone && (
                      <div className="flex items-center justify-between text-[11px] text-slate-500">
                        <span className="flex items-center gap-1.5">
                          <Phone size={12} className="text-slate-400" />
                          Phone:
                        </span>
                        <span>{d.driverPhone}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center gap-2 mt-2">
                <button
                  disabled={actionLoadingId === d.id}
                  onClick={() => handleDispatch(d)}
                  className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
                >
                  {actionLoadingId === d.id ? (
                    <RefreshCw size={12} className="animate-spin" />
                  ) : (
                    <Send size={13} />
                  )}
                  <span>Dispatch (Out for Delivery)</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200/80">
          <CalendarDays className="w-10 h-10 text-slate-300 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-800">No Scheduled Dispatches Pending</h3>
          <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
            All scheduled deliveries have departed. To assign new deliveries, schedule orders from the{' '}
            <button
              onClick={() => onNavigateTab('orders-queue')}
              className="text-blue-600 font-semibold hover:underline"
            >
              Orders & Queue
            </button>{' '}
            tab.
          </p>
        </div>
      )}
    </div>
  );
};
