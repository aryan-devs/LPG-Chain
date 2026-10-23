import React, { useState, useEffect } from 'react';
import {
  ShieldCheck,
  FileText,
  Upload,
  CheckCircle2,
  Clock,
  AlertCircle,
  XCircle,
  Check,
  RefreshCw,
} from 'lucide-react';
import { CustomerProfileData, KycDocument } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';

interface PortalKycDocumentsViewProps {
  profile: CustomerProfileData;
  documents: KycDocument[];
  onDocumentsUpdated: (docs: KycDocument[]) => void;
}

export const PortalKycDocumentsView: React.FC<PortalKycDocumentsViewProps> = ({
  profile,
}) => {
  const [kycSummary, setKycSummary] = useState<any | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [uploadSuccess, setUploadSuccess] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const [docType, setDocType] = useState<string>('ELECTRICITY_BILL');
  const [docNumber, setDocNumber] = useState<string>('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const loadSummary = async () => {
    try {
      setLoading(true);
      const data = await CustomerService.getKycSummary();
      setKycSummary(data);
    } catch {
      // Graceful fallback
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSummary();
  }, []);

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setUploadError('Please select a file to upload.');
      return;
    }

    if (selectedFile.size > 10 * 1024 * 1024) {
      setUploadError(`File is too large (${(selectedFile.size / (1024 * 1024)).toFixed(1)}MB). Maximum allowed size is 10MB.`);
      return;
    }

    const validExtensions = ['.pdf', '.jpg', '.jpeg', '.png', '.webp'];
    const lowerName = selectedFile.name.toLowerCase();
    const hasValidExt = validExtensions.some(ext => lowerName.endsWith(ext));
    if (!hasValidExt) {
      setUploadError('Invalid file format. Please upload a PDF or image file (JPG, PNG, WEBP).');
      return;
    }

    setIsUploading(true);
    setUploadError(null);
    setUploadSuccess(null);

    try {
      await CustomerService.uploadKycDocument(selectedFile, docType, docNumber.trim());
      setUploadSuccess('Document uploaded successfully. It is now registered and pending administrative review.');
      setSelectedFile(null);
      setDocNumber('');
      await loadSummary();
      setTimeout(() => setUploadSuccess(null), 6000);
    } catch (err: any) {
      setUploadError(err.message || 'Failed to upload document.');
    } finally {
      setIsUploading(false);
    }
  };

  const allApproved = kycSummary?.allApproved ?? false;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return {
          bg: 'bg-emerald-50 text-emerald-700 border-emerald-200',
          label: 'Approved',
          icon: <CheckCircle2 size={13} className="text-emerald-600" />,
        };
      case 'SUBMITTED':
      case 'PENDING_REVIEW':
        return {
          bg: 'bg-amber-50 text-amber-700 border-amber-200',
          label: 'Pending Review',
          icon: <Clock size={13} className="text-amber-600" />,
        };
      case 'REJECTED':
        return {
          bg: 'bg-rose-50 text-rose-700 border-rose-200',
          label: 'Rejected',
          icon: <XCircle size={13} className="text-rose-600" />,
        };
      case 'REQUIRED':
      case 'NOT_SUBMITTED':
      default:
        return {
          bg: 'bg-slate-100 text-slate-600 border-slate-200',
          label: 'Required / Not Submitted',
          icon: <AlertCircle size={13} className="text-slate-400" />,
        };
    }
  };

  return (
    <div className="max-w-5xl mx-auto flex flex-col gap-6 pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            KYC &amp; Connection Documents
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Government mandated verification for DBTL subsidy and safety compliance.
          </p>
        </div>
        <button
          onClick={loadSummary}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 text-xs font-bold transition-all self-start sm:self-auto"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          <span>Refresh Status</span>
        </button>
      </div>

      {/* KYC Status Overview Banner */}
      {allApproved ? (
        <div className="bg-gradient-to-r from-emerald-600 to-teal-600 rounded-3xl text-white shadow-md p-6 sm:p-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center shrink-0 text-white">
              <ShieldCheck size={32} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded-full bg-white text-emerald-800 text-[10px] font-black uppercase tracking-wider">
                  Verified &bull; Compliance Complete
                </span>
              </div>
              <h2 className="text-xl sm:text-2xl font-black mt-1">
                Your documents are up to date.
              </h2>
              <p className="text-xs text-emerald-100 mt-0.5 max-w-md">
                Aadhaar seeding &amp; DBTL subsidy mandate verified for consumer #{profile.consumerNumber}.
              </p>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-md px-5 py-3 rounded-2xl border border-white/20 text-right self-stretch md:self-auto">
            <span className="text-[10px] text-emerald-100 uppercase tracking-wider block font-bold">
              Subsidized Cylinders
            </span>
            <span className="text-xl font-black text-white">
              {profile.subsidizedQuotaLeft} / {profile.totalAnnualQuota} Available
            </span>
          </div>
        </div>
      ) : (
        <div className="bg-gradient-to-r from-amber-600 via-orange-600 to-amber-700 rounded-3xl text-white shadow-md p-6 sm:p-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center shrink-0 text-white">
              <Clock size={32} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded-full bg-white text-amber-900 text-[10px] font-black uppercase tracking-wider">
                  Action Required &bull; Verification Incomplete
                </span>
              </div>
              <h2 className="text-xl sm:text-2xl font-black mt-1">
                {kycSummary?.remainingRequiredCount ?? 1} Document(s) Still Required
              </h2>
              <p className="text-xs text-amber-100 mt-0.5 max-w-md">
                Upload your mandatory proof of residence and identity to activate uninterrupted DBTL subsidies.
              </p>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-md px-5 py-3 rounded-2xl border border-white/20 text-right self-stretch md:self-auto">
            <span className="text-[10px] text-amber-100 uppercase tracking-wider block font-bold">
              Pending Review
            </span>
            <span className="text-xl font-black text-white">
              {kycSummary?.pendingReviewCount ?? 0} In Review
            </span>
          </div>
        </div>
      )}

      {/* 6 Key Requirements Summary Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm text-center">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Required
          </span>
          <span className="text-2xl font-black text-slate-900">
            {kycSummary?.requiredCount ?? 4}
          </span>
        </div>
        <div className="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm text-center">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Submitted
          </span>
          <span className="text-2xl font-black text-blue-600">
            {kycSummary?.submittedCount ?? 0}
          </span>
        </div>
        <div className="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm text-center">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Pending Review
          </span>
          <span className="text-2xl font-black text-amber-600">
            {kycSummary?.pendingReviewCount ?? 0}
          </span>
        </div>
        <div className="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm text-center">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Approved
          </span>
          <span className="text-2xl font-black text-emerald-600">
            {kycSummary?.approvedCount ?? 0}
          </span>
        </div>
        <div className="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm text-center">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Rejected
          </span>
          <span className="text-2xl font-black text-rose-600">
            {kycSummary?.rejectedCount ?? 0}
          </span>
        </div>
        <div className="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm text-center">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Remaining
          </span>
          <span className="text-2xl font-black text-indigo-600">
            {kycSummary?.remainingRequiredCount ?? 4}
          </span>
        </div>
      </div>

      {/* Documents List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {(kycSummary?.documents || []).map((doc: any) => {
          const badge = getStatusBadge(doc.status);
          return (
            <div
              key={doc.id || doc.documentType}
              className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 sm:p-6 flex flex-col justify-between hover:shadow-md transition-shadow"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center shrink-0">
                      <FileText size={20} />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-slate-900">
                        {doc.documentType?.replace(/_/g, ' ')}
                      </h3>
                      <p className="text-xs font-mono text-slate-500">
                        {doc.documentNumber || 'Pending Upload'}
                      </p>
                    </div>
                  </div>

                  <span
                    className={`px-2.5 py-1 rounded-full text-[11px] font-bold flex items-center gap-1 border shrink-0 ${badge.bg}`}
                  >
                    {badge.icon}
                    <span>{badge.label}</span>
                  </span>
                </div>

                {/* Rejection reason alert */}
                {doc.status === 'REJECTED' && doc.rejectionReason && (
                  <div className="mt-4 p-3 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-start gap-2">
                    <XCircle size={15} className="text-rose-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold">Rejection Reason: </span>
                      <span>{doc.rejectionReason}</span>
                    </div>
                  </div>
                )}
              </div>

              <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
                <span>
                  Submitted:{' '}
                  {doc.submissionTimestamp
                    ? new Date(doc.submissionTimestamp).toLocaleDateString('en-IN')
                    : 'Not submitted'}
                </span>
                <span className="font-mono text-[11px] text-slate-500 truncate max-w-[150px]">
                  {doc.fileName || '—'}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Upload Document Form */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col gap-5">
        <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
          <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
            <Upload size={18} />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">
              Submit Required Identity or Address Proof
            </h2>
            <p className="text-xs text-slate-500">
              Upload PDF or image files (max 10MB) for administrative verification.
            </p>
          </div>
        </div>

        {uploadSuccess && (
          <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center gap-2 animate-in fade-in">
            <Check size={16} />
            <span>{uploadSuccess}</span>
          </div>
        )}

        {uploadError && (
          <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{uploadError}</span>
          </div>
        )}

        <form onSubmit={handleUpload} className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              Document Category
            </label>
            <select
              value={docType}
              onChange={(e) => setDocType(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 font-semibold focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              <option value="AADHAAR_CARD">Aadhaar Card (UIDAI)</option>
              <option value="ELECTRICITY_BILL">Electricity Bill (Address Proof)</option>
              <option value="RATION_CARD">Ration Card (Household)</option>
              <option value="BANK_PASSBOOK">Bank Passbook (DBTL Mandate)</option>
            </select>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              Document / Identifier Number
            </label>
            <input
              type="text"
              value={docNumber}
              onChange={(e) => setDocNumber(e.target.value)}
              placeholder="e.g. 5432-1098-7654"
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              Select Document File <span className="text-slate-400 font-normal">(PDF, JPG, PNG, max 10MB)</span>
            </label>
            <input
              type="file"
              required
              accept=".pdf,.jpg,.jpeg,.png,.webp"
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  setSelectedFile(e.target.files[0]);
                }
              }}
              className="w-full text-xs text-slate-500 file:mr-2 file:py-2 file:px-3 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-rose-50 file:text-rose-700 hover:file:bg-rose-100 cursor-pointer"
            />
            {selectedFile && (
              <div className="mt-1.5 flex items-center gap-2 text-[11px] text-slate-600 bg-slate-50 px-2.5 py-1 rounded-lg border border-slate-200">
                <span className="font-semibold truncate max-w-[200px]">{selectedFile.name}</span>
                <span className="text-slate-400">({(selectedFile.size / 1024).toFixed(0)} KB)</span>
              </div>
            )}
          </div>

          <div className="sm:col-span-3 flex justify-end">
            <button
              type="submit"
              disabled={isUploading || !selectedFile}
              className="px-6 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:bg-slate-300 text-white font-bold text-xs shadow-md shadow-rose-600/25 transition-all flex items-center gap-2 cursor-pointer disabled:cursor-not-allowed"
            >
              {isUploading ? (
                <>
                  <RefreshCw size={14} className="animate-spin" />
                  <span>Uploading &amp; Verifying...</span>
                </>
              ) : (
                <>
                  <Upload size={14} />
                  <span>Submit for Verification</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
