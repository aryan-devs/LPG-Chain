import React, { useState, useEffect } from 'react';
import {
  LifeBuoy,
  CheckCircle2,
  User,
  RefreshCw,
  AlertCircle,
  MessageSquare,
  Calendar,
} from 'lucide-react';
import { AdminApiService } from '../../../services/adminApi';

export const AdminComplaintsView: React.FC = () => {
  const [complaints, setComplaints] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [newStatus, setNewStatus] = useState<string>('RESOLVED');
  const [resolutionNote, setResolutionNote] = useState<string>('');
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const fetchComplaints = async () => {
    setLoading(true);
    setError(null);
    try {
      const statusParam = filterStatus === 'ALL' ? undefined : filterStatus;
      const data = await AdminApiService.getAllComplaints(statusParam);
      setComplaints(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load complaints.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, [filterStatus]);

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolvingId) return;

    try {
      await AdminApiService.updateComplaintStatus(resolvingId, newStatus, resolutionNote.trim());
      setActionSuccess(`Complaint #${resolvingId} updated to ${newStatus}.`);
      setTimeout(() => setActionSuccess(null), 4000);
      setResolvingId(null);
      setResolutionNote('');
      fetchComplaints();
    } catch (err: any) {
      alert(`Status update failed: ${err.message}`);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'RESOLVED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'IN_PROGRESS':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'OPEN':
      default:
        return 'bg-amber-50 text-amber-700 border-amber-200';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
              CITIZEN GRIEVANCES
            </span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 mt-1">Platform Complaints &amp; Resolution</h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Track customer service issues, delivery delays, gas leak emergency tickets, and billing disputes.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          {/* Status Filter */}
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="p-2 rounded-xl border border-slate-300 bg-white text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-rose-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="OPEN">Open Only</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="REJECTED">Rejected</option>
          </select>

          <button
            onClick={fetchComplaints}
            disabled={loading}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold shadow-sm transition-all"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {actionSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 size={16} />
          <span>{actionSuccess}</span>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold flex items-center gap-2">
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {/* Complaints List */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <LifeBuoy size={18} className="text-rose-600" />
            <h2 className="text-sm font-bold text-slate-900">Complaints Queue</h2>
          </div>
          <span className="text-xs font-semibold text-slate-500">
            {complaints.length} Records Found
          </span>
        </div>

        {loading ? (
          <div className="p-12 text-center text-slate-400 text-xs">
            <RefreshCw size={24} className="animate-spin mx-auto mb-2 text-rose-600" />
            Loading complaints...
          </div>
        ) : complaints.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs flex flex-col items-center gap-2">
            <CheckCircle2 size={32} className="text-emerald-500" />
            <p className="font-bold text-slate-700">No grievances matching current filter.</p>
            <p className="text-slate-400">All customer issues in this category have been addressed.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {complaints.map((c) => (
              <div key={c.id} className="p-5 hover:bg-slate-50/60 transition-colors flex flex-col gap-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-slate-900 text-xs">
                      {c.complaintNumber || `CMP-${c.id}`}
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${getStatusBadge(
                        c.status
                      )}`}
                    >
                      {c.status}
                    </span>
                    <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[10px] font-semibold">
                      {c.category?.replace(/_/g, ' ')}
                    </span>
                  </div>

                  <div className="flex items-center gap-3 text-xs text-slate-400">
                    <span className="flex items-center gap-1">
                      <Calendar size={13} />
                      {c.createdAt ? new Date(c.createdAt).toLocaleDateString('en-IN') : 'Recent'}
                    </span>
                    {c.bookingNumber && (
                      <span className="font-mono text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md text-[11px]">
                        Ref: {c.bookingNumber}
                      </span>
                    )}
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-bold text-slate-900">{c.subject}</h3>
                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">{c.description}</p>
                </div>

                {/* Resolution note if already resolved */}
                {c.resolutionNote && (
                  <div className="p-3 rounded-2xl bg-emerald-50/80 border border-emerald-200/80 text-xs text-emerald-900 flex items-start gap-2">
                    <CheckCircle2 size={15} className="text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold">Resolution Note: </span>
                      <span>{c.resolutionNote}</span>
                    </div>
                  </div>
                )}

                {/* Footer with customer info & action */}
                <div className="pt-2 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                  <div className="flex items-center gap-2 text-slate-500">
                    <User size={13} />
                    <span>Customer: {c.customerName || `ID: ${c.customerId}`}</span>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-auto">
                    <button
                      type="button"
                      onClick={() => {
                        setResolvingId(c.id);
                        setNewStatus(c.status === 'OPEN' ? 'IN_PROGRESS' : 'RESOLVED');
                        setResolutionNote(c.resolutionNote || '');
                      }}
                      className="px-3 py-1.5 rounded-xl border border-slate-200 hover:border-rose-400 hover:bg-rose-50 text-slate-700 hover:text-rose-700 font-bold text-xs transition-colors flex items-center gap-1.5"
                    >
                      <MessageSquare size={13} />
                      <span>Update Status &amp; Respond</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Resolution Modal */}
      {resolvingId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-200 p-6 flex flex-col gap-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-900">
                Update Complaint #{resolvingId}
              </h3>
              <button
                type="button"
                onClick={() => setResolvingId(null)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleUpdateStatus} className="flex flex-col gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Change Status To
                </label>
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 font-semibold focus:outline-none focus:ring-2 focus:ring-rose-500"
                >
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="REJECTED">REJECTED</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Resolution Note / Action Taken
                </label>
                <textarea
                  rows={3}
                  required
                  value={resolutionNote}
                  onChange={(e) => setResolutionNote(e.target.value)}
                  placeholder="e.g. Delivery rescheduled with priority slot. Delivery agent counselled and verified on tare weight adherence."
                  className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setResolvingId(null)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-bold text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold"
                >
                  Save Resolution
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
