import React, { useState, useEffect, useMemo } from 'react';
import {
  Users,
  Eye,
  Shield,
  Phone,
  Mail,
  MapPin,
  Calendar,
  Building2,
  RefreshCw,
  AlertTriangle,
  Info,
} from 'lucide-react';
import { AdminUserItem } from '../../../types/adminPortalTypes';
import { AdminTable, ColumnDef } from '../components/AdminTable';
import { AdminStatusBadge } from '../components/AdminStatusBadge';
import { AdminFilterBar, FilterDropdown } from '../components/AdminFilterBar';
import { AdminDetailModal } from '../components/AdminDetailModal';
import { AdminConfirmModal } from '../components/AdminConfirmModal';
import { AdminApiService } from '../../../services/adminApi';

export const AdminUserManagementView: React.FC = () => {
  const [users, setUsers] = useState<AdminUserItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUsingLiveBackend, setIsUsingLiveBackend] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Selected User Modal state
  const [selectedUser, setSelectedUser] = useState<AdminUserItem | null>(null);

  // Status Info Modal (informing admin that backend user schema does not support user status updates)
  const [statusInfoModal, setStatusInfoModal] = useState<{
    isOpen: boolean;
    user: AdminUserItem | null;
  }>({
    isOpen: false,
    user: null,
  });

  const loadUsersFromBackend = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await AdminApiService.getAllUsers();
      setUsers(data || []);
      setIsUsingLiveBackend(true);
    } catch (err: any) {
      setError(err.message || 'Failed to load user directory from backend');
      setUsers([]);
      setIsUsingLiveBackend(false);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUsersFromBackend();
  }, []);

  // Filtered dataset
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchSearch =
        u.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.phone.includes(searchQuery);

      const matchRole = roleFilter === 'ALL' || u.role === roleFilter;
      const matchStatus = statusFilter === 'ALL' || u.status === statusFilter;

      return matchSearch && matchRole && matchStatus;
    });
  }, [users, searchQuery, roleFilter, statusFilter]);

  const handleOpenStatusInfo = (user: AdminUserItem) => {
    setStatusInfoModal({
      isOpen: true,
      user,
    });
  };

  const filterDropdowns: FilterDropdown[] = [
    {
      id: 'role',
      label: 'Role',
      value: roleFilter,
      onChange: setRoleFilter,
      options: [
        { label: 'All Roles', value: 'ALL' },
        { label: 'Consumers', value: 'CONSUMER' },
        { label: 'Distributors', value: 'DISTRIBUTOR' },
        { label: 'Drivers', value: 'DRIVER' },
        { label: 'Admins', value: 'ADMIN' },
      ],
    },
    {
      id: 'status',
      label: 'Status',
      value: statusFilter,
      onChange: setStatusFilter,
      options: [
        { label: 'All Statuses', value: 'ALL' },
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Suspended', value: 'SUSPENDED' },
        { label: 'Inactive', value: 'INACTIVE' },
      ],
    },
  ];

  const columns: ColumnDef<AdminUserItem>[] = [
    {
      key: 'name',
      header: 'User / Identity',
      render: (u) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center font-bold text-xs text-slate-700 shrink-0">
            {u.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="font-semibold text-slate-900 flex items-center gap-1.5">
              <span>{u.name}</span>
              {u.role === 'ADMIN' && (
                <span className="px-1.5 py-0.2 rounded-sm text-[9px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
                  ROOT
                </span>
              )}
            </div>
            <div className="text-[11px] text-slate-400 font-mono">{u.email}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'phone',
      header: 'Contact',
      className: 'hidden md:table-cell',
      render: (u) => (
        <div className="text-xs text-slate-600 font-mono">
          {u.phone}
        </div>
      ),
    },
    {
      key: 'role',
      header: 'Assigned Role',
      render: (u) => (
        <span
          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold border ${
            u.role === 'ADMIN'
              ? 'bg-rose-50 text-rose-700 border-rose-200'
              : u.role === 'DISTRIBUTOR'
              ? 'bg-blue-50 text-blue-700 border-blue-200'
              : u.role === 'DRIVER'
              ? 'bg-amber-50 text-amber-700 border-amber-200'
              : 'bg-emerald-50 text-emerald-700 border-emerald-200'
          }`}
        >
          <Shield size={11} />
          <span>{u.role}</span>
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (u) => <AdminStatusBadge status={u.status} size="sm" />,
    },
    {
      key: 'registrationDate',
      header: 'Registered',
      className: 'hidden lg:table-cell',
      render: (u) => (
        <div className="text-xs text-slate-500 flex items-center gap-1">
          <Calendar size={12} className="text-slate-400" />
          <span>{u.registrationDate}</span>
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (u) => (
        <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
          <button
            onClick={() => setSelectedUser(u)}
            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
            title="View User Details"
          >
            <Eye size={15} />
          </button>
          <button
            onClick={() => handleOpenStatusInfo(u)}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
            title="Account Status Info"
          >
            <Info size={15} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      {/* View Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Users size={20} className="text-rose-600" />
            <span>User Management & Identity Directory</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Oversee registered consumers, agency staff, drivers, and administrators across the platform.
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
            {isUsingLiveBackend ? 'Live Backend Users' : 'Disconnected'}
          </span>
          <button
            onClick={loadUsersFromBackend}
            disabled={isLoading}
            className="p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-white border border-slate-200 transition-colors cursor-pointer"
            title="Refresh Users"
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Error Alert if API fails */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} className="text-rose-600 shrink-0" />
            <div>
              <span className="font-semibold block">Failed to load live users from backend</span>
              <span>{error}</span>
            </div>
          </div>
          <button
            onClick={loadUsersFromBackend}
            className="px-3 py-1.5 bg-rose-600 text-white rounded-lg font-semibold hover:bg-rose-700 transition cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <AdminFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        searchPlaceholder="Search by name, email, or phone number..."
        dropdowns={filterDropdowns}
        onReset={() => {
          setSearchQuery('');
          setRoleFilter('ALL');
          setStatusFilter('ALL');
        }}
        totalResults={filteredUsers.length}
      />

      {/* User Table */}
      <AdminTable
        columns={columns}
        data={filteredUsers}
        keyExtractor={(u) => u.id}
        isLoading={isLoading}
        emptyTitle="No users found"
        emptyDescription={
          error
            ? "Could not retrieve user accounts due to a connection or authorization issue."
            : "No registered users matched your search filters."
        }
        pageSize={8}
        onRowClick={(u) => setSelectedUser(u)}
      />

      {/* ── User Details Modal / Panel ── */}
      <AdminDetailModal
        isOpen={!!selectedUser}
        title={selectedUser?.name || 'User Profile Details'}
        subtitle={`Account ID #${selectedUser?.id} • Registered ${selectedUser?.registrationDate}`}
        badge={selectedUser ? <AdminStatusBadge status={selectedUser.status} size="sm" /> : undefined}
        icon={<Users size={18} className="text-rose-600" />}
        onClose={() => setSelectedUser(null)}
        footerActions={
          selectedUser && (
            <button
              type="button"
              onClick={() => setSelectedUser(null)}
              className="px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-200/60 rounded-xl cursor-pointer"
            >
              Close
            </button>
          )
        }
      >
        {selectedUser && (
          <div className="space-y-4 text-xs text-slate-700">
            {/* Primary Details Card */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="flex items-center gap-2.5">
                <Mail size={15} className="text-slate-400" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Email Address</span>
                  <span className="font-semibold text-slate-900">{selectedUser.email}</span>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <Phone size={15} className="text-slate-400" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Phone Number</span>
                  <span className="font-semibold text-slate-900">{selectedUser.phone}</span>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <Shield size={15} className="text-slate-400" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Resolved Role</span>
                  <span className="font-bold text-rose-700">{selectedUser.role}</span>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <Calendar size={15} className="text-slate-400" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Registration Date</span>
                  <span className="font-semibold text-slate-900">{selectedUser.registrationDate}</span>
                </div>
              </div>
            </div>

            {/* Address Details */}
            {selectedUser.address && (
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-start gap-2.5">
                <MapPin size={15} className="text-slate-400 shrink-0 mt-0.5" />
                <div>
                  <span className="text-[10px] text-slate-400 block">Primary Delivery Address</span>
                  <span className="font-medium text-slate-800 leading-relaxed">
                    {selectedUser.address}
                  </span>
                </div>
              </div>
            )}

            {/* Distributor Association if applicable */}
            {selectedUser.distributorName && (
              <div className="p-3.5 rounded-xl bg-blue-50/60 border border-blue-200/70 flex items-start gap-2.5">
                <Building2 size={15} className="text-blue-600 shrink-0 mt-0.5" />
                <div>
                  <span className="text-[10px] text-blue-500 font-semibold uppercase tracking-wider block">
                    Associated Distributor
                  </span>
                  <span className="font-semibold text-blue-950">
                    {selectedUser.distributorName}
                  </span>
                </div>
              </div>
            )}

            {/* Read-Only Status Notice */}
            <div className="p-3 rounded-xl bg-slate-100 border border-slate-200 text-slate-600 flex items-center gap-2">
              <Info size={14} className="text-slate-500 shrink-0" />
              <span className="text-[11px]">
                User identity and role assignment are verified from Spring Boot. Account status is currently read-only.
              </span>
            </div>
          </div>
        )}
      </AdminDetailModal>

      {/* Informational Modal for Status Management */}
      <AdminConfirmModal
        isOpen={statusInfoModal.isOpen}
        title="User Account Status (Read-Only)"
        description={`Account for ${statusInfoModal.user?.name || 'this user'} is managed by the backend identity service. In the current MySQL JPA schema, user status changes are not yet exposed via REST API. Operations cannot be simulated locally.`}
        confirmLabel="Understood"
        onConfirm={() => setStatusInfoModal({ isOpen: false, user: null })}
        onClose={() => setStatusInfoModal({ isOpen: false, user: null })}
      />
    </div>
  );
};
