import React, { useState, useEffect } from 'react';
import {
  FileCheck,
  CheckCircle2,
  XCircle,
  User,
  RefreshCw,
  AlertCircle,
  FileText,
} from 'lucide-react';
import { AdminApiService } from '../../../services/adminApi';

export const AdminKycReviewView: React.FC = () => {
  const [documents, setDocuments] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [rejectingDocId, setRejectingDocId] = useState<number | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const fetchDocuments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await AdminApiService.getPendingKycDocuments();
      setDocuments(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load pending KYC documents.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  const handleApprove = async (docId: number) => {
    try {
      await AdminApiService.reviewKycDocument(docId, 'APPROVED');
      setActionSuccess(`Document #${docId} approved successfully.`);
      setTimeout(() => setActionSuccess(null), 4000);
      fetchDocuments();
    } catch (err: any) {
      alert(`Approval failed: ${err.message}`);
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingDocId || !rejectionReason.trim()) return;
    try {
      await AdminApiService.reviewKycDocument(rejectingDocId, 'REJECTED', rejectionReason.trim());
      setActionSuccess(`Document #${rejectingDocId} rejected.`);
      setTimeout(() => setActionSuccess(null), 4000);
      setRejectingDocId(null);
      setRejectionReason('');
      fetchDocuments();
    } catch (err: any) {
      alert(`Rejection failed: ${err.message}`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
              IDENTITY & COMPLIANCE
            </span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 mt-1">KYC Document Review Queue</h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Verify citizen identity and residency records for LPG subsidy eligibility and safety compliance.
          </p>
        </div>
        <button
          onClick={fetchDocuments}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold shadow-sm transition-all self-start sm:self-auto"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          <span>Refresh Queue</span>
        </button>
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

      {/* Queue Table */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FileCheck size={18} className="text-rose-600" />
            <h2 className="text-sm font-bold text-slate-900">Pending Verification Submissions</h2>
          </div>
          <span className="text-xs font-semibold text-slate-500">
            {documents.length} Pending Review
          </span>
        </div>

        {loading ? (
          <div className="p-12 text-center text-slate-400 text-xs">
            <RefreshCw size={24} className="animate-spin mx-auto mb-2 text-rose-600" />
            Loading KYC review queue...
          </div>
        ) : documents.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs flex flex-col items-center gap-2">
            <CheckCircle2 size={32} className="text-emerald-500" />
            <p className="font-bold text-slate-700">All submissions up to date!</p>
            <p className="text-slate-400">There are currently no customer documents pending administrative review.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3 px-5">ID</th>
                  <th className="py-3 px-4">Customer</th>
                  <th className="py-3 px-4">Document Type</th>
                  <th className="py-3 px-4">Document / Ref #</th>
                  <th className="py-3 px-4">Submitted At</th>
                  <th className="py-3 px-4">File Name</th>
                  <th className="py-3 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {documents.map((doc) => (
                  <tr key={doc.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3.5 px-5 font-mono font-bold text-slate-900">#{doc.id}</td>
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-2">
                        <User size={14} className="text-slate-400" />
                        <div>
                          <span className="font-semibold text-slate-800 block">
                            {doc.userName || `User #${doc.userId}`}
                          </span>
                          <span className="text-[11px] text-slate-400 font-mono">
                            {doc.userEmail || `ID: ${doc.userId}`}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 font-bold text-slate-800">
                      {doc.documentType?.replace(/_/g, ' ')}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">
                      {doc.documentNumber || '—'}
                    </td>
                    <td className="py-3.5 px-4 text-slate-500">
                      {doc.submittedAt ? new Date(doc.submittedAt).toLocaleString('en-IN') : 'Recent'}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-1.5 font-mono text-[11px] text-slate-600">
                        <FileText size={13} className="text-slate-400" />
                        <span className="truncate max-w-[140px]">{doc.fileName || 'document.pdf'}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-5 text-right space-x-2">
                      <button
                        type="button"
                        onClick={() => handleApprove(doc.id)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-[11px] shadow-sm transition-all"
                      >
                        <CheckCircle2 size={12} />
                        <span>Approve</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setRejectingDocId(doc.id);
                          setRejectionReason('');
                        }}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-rose-300 hover:bg-rose-50 text-rose-700 font-bold text-[11px] transition-all"
                      >
                        <XCircle size={12} />
                        <span>Reject</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Reject Modal */}
      {rejectingDocId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-200 p-6 flex flex-col gap-4">
            <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
              <XCircle size={20} className="text-rose-600" />
              <h3 className="text-base font-bold text-slate-900">Reject KYC Document #{rejectingDocId}</h3>
            </div>
            <form onSubmit={handleReject} className="flex flex-col gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Reason for Rejection (Visible to Citizen)
                </label>
                <textarea
                  rows={3}
                  required
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="e.g. Scanned copy is illegible, electricity bill date is older than 3 months, or Aadhaar name does not match connection name."
                  className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setRejectingDocId(null)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-bold text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold"
                >
                  Confirm Rejection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
