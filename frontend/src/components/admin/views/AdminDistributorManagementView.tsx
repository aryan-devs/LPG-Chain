import React, { useState, useEffect, useMemo } from 'react';
import {
  Building2,
  CheckCircle2,
  XCircle,
  Eye,
  ShieldCheck,
  MapPin,
  Phone,
  Clock,
  Layers,
  Truck,
  Users,
  FileCheck,
  RefreshCw,
  AlertTriangle,
} from 'lucide-react';
import {
  AdminDistributorItem,
} from '../../../types/adminPortalTypes';
import { AdminTable, ColumnDef } from '../components/AdminTable';
import { AdminStatusBadge } from '../components/AdminStatusBadge';
import { AdminFilterBar, FilterDropdown } from '../components/AdminFilterBar';
import { AdminDetailModal } from '../components/AdminDetailModal';
import { AdminConfirmModal } from '../components/AdminConfirmModal';
import { AdminApiService } from '../../../services/adminApi';

export const AdminDistributorManagementView: React.FC = () => {
  const [distributors, setDistributors] = useState<AdminDistributorItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUsingLiveBackend, setIsUsingLiveBackend] = useState(false);

  // Status Action Feedback state
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [isActionLoading, setIsActionLoading] = useState(false);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [cityFilter, setCityFilter] = useState('ALL');
  const [serviceFilter, setServiceFilter] = useState('ALL');
  const [verificationFilter, setVerificationFilter] = useState('ALL');
  const [accountFilter, setAccountFilter] = useState('ALL');

  // Detail Modal
  const [selectedDistributor, setSelectedDistributor] = useState<AdminDistributorItem | null>(null);

  // Confirmation Modal
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    distributor: AdminDistributorItem | null;
    actionType: 'ACCOUNT_SUSPEND' | 'ACCOUNT_ACTIVATE' | 'VERIFICATION_APPROVE' | 'VERIFICATION_REJECT';
  }>({
    isOpen: false,
    distributor: null,
    actionType: 'ACCOUNT_SUSPEND',
  });

  const loadDistributors = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await AdminApiService.getAllDistributors();
      setDistributors(data || []);
      setIsUsingLiveBackend(true);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch distributors from backend API');
      setDistributors([]);
      setIsUsingLiveBackend(false);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDistributors();
  }, []);

  const cities = useMemo(() => {
    const list = Array.from(new Set(distributors.map((d) => d.city).filter(Boolean)));
    return [{ label: 'All Cities', value: 'ALL' }, ...list.map((c) => ({ label: c, value: c }))];
  }, [distributors]);

  const filteredDistributors = useMemo(() => {
    return distributors.filter((d) => {
      const matchSearch =
        d.agencyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        d.distributorCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
        d.licenceNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        d.contactNumber.includes(searchQuery);

      const matchCity = cityFilter === 'ALL' || d.city === cityFilter;
      const matchService = serviceFilter === 'ALL' || d.supportedServices === serviceFilter;
      const matchVerification =
        verificationFilter === 'ALL' || d.verificationStatus === verificationFilter;
      const matchAccount = accountFilter === 'ALL' || d.accountStatus === accountFilter;

      return matchSearch && matchCity && matchService && matchVerification && matchAccount;
    });
  }, [distributors, searchQuery, cityFilter, serviceFilter, verificationFilter, accountFilter]);

  const handleExecuteAction = async () => {
    if (!confirmModal.distributor) return;
    const targetId = confirmModal.distributor.id;
    const act = confirmModal.actionType;

    let nextStatus: 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' = 'ACTIVE';
    if (act === 'ACCOUNT_SUSPEND' || act === 'VERIFICATION_REJECT') {
      nextStatus = 'SUSPENDED';
    } else if (act === 'ACCOUNT_ACTIVATE' || act === 'VERIFICATION_APPROVE') {
      nextStatus = 'ACTIVE';
    }

    try {
      setIsActionLoading(true);
      setActionError(null);
      const updated = await AdminApiService.updateDistributorStatus(targetId, nextStatus);

      setDistributors((prev) =>
        prev.map((d) => (d.id === targetId ? updated : d))
      );

      if (selectedDistributor && selectedDistributor.id === targetId) {
        setSelectedDistributor(updated);
      }

      setActionSuccess(`Agency "${updated.agencyName}" status successfully updated to ${nextStatus}.`);
      setConfirmModal({
        isOpen: false,
        distributor: null,
        actionType: 'ACCOUNT_SUSPEND',
      });
      setTimeout(() => setActionSuccess(null), 5000);
    } catch (err: any) {
      setActionError(err.message || 'Failed to update agency status on backend.');
    } finally {
      setIsActionLoading(false);
    }
  };

  const filterDropdowns: FilterDropdown[] = [
    {
      id: 'city',
      label: 'City',
      value: cityFilter,
      onChange: setCityFilter,
      options: cities,
    },
    {
      id: 'verification',
      label: 'Verification',
      value: verificationFilter,
      onChange: setVerificationFilter,
      options: [
        { label: 'All Verification', value: 'ALL' },
        { label: 'Verified', value: 'VERIFIED' },
        { label: 'Pending Verification', value: 'PENDING_VERIFICATION' },
        { label: 'Rejected', value: 'REJECTED' },
      ],
    },
    {
      id: 'account',
      label: 'Account Status',
      value: accountFilter,
      onChange: setAccountFilter,
      options: [
        { label: 'All Accounts', value: 'ALL' },
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Suspended', value: 'SUSPENDED' },
      ],
    },
    {
      id: 'service',
      label: 'Services',
      value: serviceFilter,
      onChange: setServiceFilter,
      options: [
        { label: 'All Services', value: 'ALL' },
        { label: 'Domestic Only', value: 'DOMESTIC' },
        { label: 'Commercial Only', value: 'COMMERCIAL' },
        { label: 'Both Domestic & Comm', value: 'BOTH' },
      ],
    },
  ];

  const columns: ColumnDef<AdminDistributorItem>[] = [
    {
      key: 'agencyName',
      header: 'Agency Information',
      render: (d) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-700 flex items-center justify-center font-bold text-xs border border-rose-100">
            <Building2 size={16} />
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-slate-900">{d.agencyName}</span>
            <div className="flex items-center gap-2 text-[11px] text-slate-400">
              <span className="font-mono text-rose-600 font-semibold">{d.distributorCode}</span>
              <span>•</span>
              <span>Lic: {d.licenceNumber}</span>
            </div>
          </div>
        </div>
      ),
    },
    {
      key: 'location',
      header: 'Depot Location',
      render: (d) => (
        <div className="flex flex-col text-slate-600">
          <span className="font-medium text-slate-900">{d.city}, {d.state}</span>
          <span className="text-[11px] text-slate-400">PIN: {d.pinCode}</span>
        </div>
      ),
    },
    {
      key: 'verificationStatus',
      header: 'Verification Status',
      render: (d) => <AdminStatusBadge status={d.verificationStatus} size="sm" />,
    },
    {
      key: 'accountStatus',
      header: 'Account Status',
      render: (d) => <AdminStatusBadge status={d.accountStatus} size="sm" />,
    },
    {
      key: 'fleet',
      header: 'Depot Fleet',
      render: (d) => (
        <div className="flex items-center gap-3 text-slate-600 text-xs font-semibold">
          <span className="flex items-center gap-1" title="Vehicles">
            <Truck size={13} className="text-slate-400" /> {d.vehiclesCount}
          </span>
          <span className="flex items-center gap-1" title="Drivers">
            <Users size={13} className="text-slate-400" /> {d.driversCount}
          </span>
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (d) => (
        <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => setSelectedDistributor(d)}
            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer"
            title="Inspect Agency Details"
          >
            <Eye size={15} />
          </button>
          {d.verificationStatus === 'PENDING_VERIFICATION' && (
            <button
              onClick={() =>
                setConfirmModal({
                  isOpen: true,
                  distributor: d,
                  actionType: 'VERIFICATION_APPROVE',
                })
              }
              className="p-1.5 rounded-lg text-emerald-600 hover:bg-emerald-50 transition-colors cursor-pointer"
              title="Verify & Approve Agency"
            >
              <FileCheck size={15} />
            </button>
          )}
          <button
            onClick={() =>
              setConfirmModal({
                isOpen: true,
                distributor: d,
                actionType:
                  d.accountStatus === 'ACTIVE' ? 'ACCOUNT_SUSPEND' : 'ACCOUNT_ACTIVATE',
              })
            }
            className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
              d.accountStatus === 'ACTIVE'
                ? 'text-rose-600 hover:bg-rose-50'
                : 'text-emerald-600 hover:bg-emerald-50'
            }`}
            title={d.accountStatus === 'ACTIVE' ? 'Suspend Agency Account' : 'Reactivate Agency Account'}
          >
            {d.accountStatus === 'ACTIVE' ? <XCircle size={15} /> : <CheckCircle2 size={15} />}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Building2 size={20} className="text-rose-600" />
            <span>Distributor Agency Management</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Oversee licensed agency depots, territorial coverage zones, verification credentials, and live account statuses.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span
            className={`px-2.5 py-1 rounded-full text-[11px] font-semibold border ${
              isUsingLiveBackend
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-slate-100 text-slate-600 border-slate-200'
            }`}
          >
            {isUsingLiveBackend ? 'Live Backend Agencies' : 'Disconnected'}
          </span>
          <button
            onClick={loadDistributors}
            disabled={isLoading}
            className="p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-white border border-slate-200 transition-colors cursor-pointer"
            title="Refresh Distributors"
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Success Banner */}
      {actionSuccess && (
        <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2.5">
          <CheckCircle2 size={16} className="text-emerald-600 shrink-0" />
          <span className="font-medium">{actionSuccess}</span>
        </div>
      )}

      {/* Error Banner for Action */}
      {actionError && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} className="text-rose-600 shrink-0" />
            <span>{actionError}</span>
          </div>
          <button
            onClick={() => setActionError(null)}
            className="text-xs font-semibold text-rose-600 hover:underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Error Banner for Initial Load */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} className="text-rose-600 shrink-0" />
            <div>
              <span className="font-semibold block">Failed to load distributors from backend</span>
              <span>{error}</span>
            </div>
          </div>
          <button
            onClick={loadDistributors}
            className="px-3 py-1.5 bg-rose-600 text-white rounded-lg font-semibold hover:bg-rose-700 transition cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* Distinction Note between Verification & Activation */}
      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start gap-2.5">
        <ShieldCheck size={16} className="text-rose-600 mt-0.5 shrink-0" />
        <div>
          <strong className="text-slate-900 font-bold">Administrative Governance:</strong>{' '}
          Status changes are persisted securely to MySQL database via Spring Boot REST API. Suspending an agency prevents dispatch allocations and portal access.
        </div>
      </div>

      {/* Filter and Search Bar */}
      <AdminFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        searchPlaceholder="Search agency name, license number, or code..."
        dropdowns={filterDropdowns}
        onReset={() => {
          setSearchQuery('');
          setCityFilter('ALL');
          setServiceFilter('ALL');
          setVerificationFilter('ALL');
          setAccountFilter('ALL');
        }}
        totalResults={filteredDistributors.length}
      />

      {/* Distributor Table */}
      <AdminTable
        columns={columns}
        data={filteredDistributors}
        keyExtractor={(d) => d.id}
        isLoading={isLoading}
        emptyTitle="No distributor agencies found"
        emptyDescription={
          error
            ? "Could not retrieve distributor records due to a connection or authorization issue."
            : "No registered distributor agencies match your current search and filter selections."
        }
        pageSize={8}
        onRowClick={(d) => setSelectedDistributor(d)}
      />

      {/* ── Detail Modal ── */}
      <AdminDetailModal
        isOpen={!!selectedDistributor}
        title={selectedDistributor?.agencyName || 'Agency Details'}
        subtitle={`Agency Code: ${selectedDistributor?.distributorCode} • License: ${selectedDistributor?.licenceNumber}`}
        badge={
          selectedDistributor ? (
            <AdminStatusBadge status={selectedDistributor.accountStatus} size="sm" />
          ) : undefined
        }
        icon={<Building2 size={18} className="text-rose-600" />}
        onClose={() => setSelectedDistributor(null)}
        footerActions={
          selectedDistributor && (
            <>
              <button
                type="button"
                onClick={() => setSelectedDistributor(null)}
                className="px-3.5 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-200/60 rounded-xl cursor-pointer"
              >
                Close
              </button>
              <button
                type="button"
                onClick={() => {
                  setConfirmModal({
                    isOpen: true,
                    distributor: selectedDistributor,
                    actionType:
                      selectedDistributor.accountStatus === 'ACTIVE'
                        ? 'ACCOUNT_SUSPEND'
                        : 'ACCOUNT_ACTIVATE',
                  });
                }}
                className={`px-4 py-2 text-xs font-bold rounded-xl text-white shadow-xs cursor-pointer ${
                  selectedDistributor.accountStatus === 'ACTIVE'
                    ? 'bg-rose-600 hover:bg-rose-500'
                    : 'bg-emerald-600 hover:bg-emerald-500'
                }`}
              >
                {selectedDistributor.accountStatus === 'ACTIVE'
                  ? 'Suspend Agency'
                  : 'Reactivate Agency'}
              </button>
            </>
          )
        }
      >
        {selectedDistributor && (
          <div className="space-y-4 text-xs text-slate-700">
            {/* Primary Info Card */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="flex items-center gap-2.5">
                <Phone size={15} className="text-slate-400 shrink-0" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Contact Phone</span>
                  <span className="font-semibold text-slate-900">{selectedDistributor.contactNumber}</span>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <Clock size={15} className="text-slate-400 shrink-0" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Operating Hours</span>
                  <span className="font-semibold text-slate-900">{selectedDistributor.operatingHours}</span>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <Layers size={15} className="text-slate-400 shrink-0" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Supported Services</span>
                  <span className="font-bold text-rose-700">{selectedDistributor.supportedServices}</span>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <MapPin size={15} className="text-slate-400 shrink-0" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Territory PIN Code</span>
                  <span className="font-semibold text-slate-900">{selectedDistributor.pinCode}</span>
                </div>
              </div>
            </div>

            {/* Address */}
            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-start gap-2.5">
              <MapPin size={15} className="text-slate-400 shrink-0 mt-0.5" />
              <div>
                <span className="text-[10px] text-slate-400 block">Physical Depot Address</span>
                <span className="font-medium text-slate-800 leading-relaxed">
                  {selectedDistributor.address}, {selectedDistributor.city}, {selectedDistributor.state} - {selectedDistributor.pinCode}
                </span>
              </div>
            </div>

            {/* Fleet and Drivers Summary */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3">
              <div className="p-3 bg-white rounded-lg border border-slate-200/80">
                <span className="text-[10px] font-semibold text-slate-500 block">Registered Fleet</span>
                <span className="text-lg font-black text-slate-900">
                  {selectedDistributor.vehiclesCount} Delivery Vehicles
                </span>
              </div>
              <div className="p-3 bg-white rounded-lg border border-slate-200/80">
                <span className="text-[10px] font-semibold text-slate-500 block">Approved Drivers</span>
                <span className="text-lg font-black text-slate-900">
                  {selectedDistributor.driversCount} Drivers
                </span>
              </div>
            </div>
          </div>
        )}
      </AdminDetailModal>

      {/* ── Confirmation Modal ── */}
      <AdminConfirmModal
        isOpen={confirmModal.isOpen}
        title={
          confirmModal.actionType === 'ACCOUNT_SUSPEND'
            ? 'Suspend Distributor Agency'
            : confirmModal.actionType === 'ACCOUNT_ACTIVATE'
            ? 'Reactivate Distributor Agency'
            : confirmModal.actionType === 'VERIFICATION_APPROVE'
            ? 'Verify & Approve Agency'
            : 'Reject Agency Verification'
        }
        description={`Are you sure you want to ${
          confirmModal.actionType === 'ACCOUNT_SUSPEND' ? 'suspend' : 'activate'
        } ${confirmModal.distributor?.agencyName} (${confirmModal.distributor?.distributorCode})? This status change will be persisted to MySQL and reflected across the platform.`}
        confirmLabel={
          confirmModal.actionType === 'ACCOUNT_SUSPEND' ? 'Suspend Agency' : 'Activate Agency'
        }
        isDestructive={
          confirmModal.actionType === 'ACCOUNT_SUSPEND' ||
          confirmModal.actionType === 'VERIFICATION_REJECT'
        }
        isLoading={isActionLoading}
        onConfirm={handleExecuteAction}
        onClose={() => {
          if (!isActionLoading) {
            setConfirmModal({
              isOpen: false,
              distributor: null,
              actionType: 'ACCOUNT_SUSPEND',
            });
          }
        }}
      />
    </div>
  );
};
