import React, { useState } from 'react';
import {
  Shield,
  Lock,
  Key,
  Building2,
  ShieldAlert,
} from 'lucide-react';
import {
  PermissionMatrixItem,
  SecurityAuditEvent,
  DistributorMembershipItem,
} from '../../../types/adminPortalTypes';
import { AdminStatusBadge } from '../components/AdminStatusBadge';
import { AdminDetailModal } from '../components/AdminDetailModal';

const PREVIEW_PERMISSIONS: PermissionMatrixItem[] = [
  { module: 'User Accounts & Directory', description: 'Create, read, and manage platform identities', adminPerm: 'FULL_CONTROL', distributorPerm: 'READ_ONLY', consumerPerm: 'READ_ONLY', driverPerm: 'NONE' },
  { module: 'Agency Operations & Depots', description: 'Approve, suspend, and configure distributor agencies', adminPerm: 'FULL_CONTROL', distributorPerm: 'READ_WRITE', consumerPerm: 'NONE', driverPerm: 'NONE' },
  { module: 'Bookings & Order Lifecycle', description: 'Place refill requests and accept booking orders', adminPerm: 'READ_ONLY', distributorPerm: 'FULL_CONTROL', consumerPerm: 'READ_WRITE', driverPerm: 'READ_ONLY' },
  { module: 'Delivery Dispatches & GPS', description: 'Assign drivers, update vehicle dispatches, and track coordinates', adminPerm: 'READ_ONLY', distributorPerm: 'FULL_CONTROL', consumerPerm: 'READ_ONLY', driverPerm: 'READ_WRITE' },
  { module: 'OMC Allocation Quotas', description: 'Authorize monthly quotas and quota adjustments', adminPerm: 'FULL_CONTROL', distributorPerm: 'READ_ONLY', consumerPerm: 'NONE', driverPerm: 'NONE' },
  { module: 'Physical Depot Stock Audits', description: 'Perform stock counts and reconcile discrepancies', adminPerm: 'READ_ONLY', distributorPerm: 'FULL_CONTROL', consumerPerm: 'NONE', driverPerm: 'NONE' },
  { module: 'Audit Logs & Governance', description: 'Inspect cryptographic access trails and ReBAC events', adminPerm: 'FULL_CONTROL', distributorPerm: 'READ_ONLY', consumerPerm: 'NONE', driverPerm: 'NONE' },
];

const PREVIEW_MEMBERSHIPS: DistributorMembershipItem[] = [
  { id: 1, userId: 2, userName: 'Apex Gas Manager', userEmail: 'distributor@agency.example', distributorId: 1, distributorName: 'Apex Gas Distribution Agency', membershipRole: 'OWNER', status: 'ACTIVE', grantedAt: '2025-11-20' },
  { id: 2, userId: 3, userName: 'Suresh Patil', userEmail: 'driver@lpgchain.example', distributorId: 1, distributorName: 'Apex Gas Distribution Agency', membershipRole: 'DISPATCHER', status: 'ACTIVE', grantedAt: '2026-01-10' },
  { id: 3, userId: 5, userName: 'Pune Green Depot Head', userEmail: 'manager@punegreen.agency.example', distributorId: 2, distributorName: 'Pune Green Gas Depot', membershipRole: 'OWNER', status: 'ACTIVE', grantedAt: '2025-12-05' },
  { id: 4, userId: 7, userName: 'Ramesh Sawant', userEmail: 'ramesh.driver@lpgchain.example', distributorId: 1, distributorName: 'Apex Gas Distribution Agency', membershipRole: 'DISPATCHER', status: 'ACTIVE', grantedAt: '2026-03-01' },
];

const PREVIEW_SECURITY_EVENTS: SecurityAuditEvent[] = [
  { id: 'SEC-101', timestamp: '12 mins ago', eventType: 'LOGIN_SUCCESS', actor: 'admin@lpgchain.example', ipAddress: '10.0.4.12', resourceTarget: '/api/admin/overview-kpis', severity: 'INFO', details: 'Authenticated with administrative JWT' },
  { id: 'SEC-102', timestamp: '34 mins ago', eventType: 'ROLE_ELEVATION_ATTEMPT', actor: 'customer@example.com', ipAddress: '152.58.18.91', resourceTarget: '/api/users/roles', severity: 'WARNING', details: 'Blocked attempt by standard customer to self-assign privileged role' },
  { id: 'SEC-103', timestamp: '1 hour ago', eventType: 'CROSS_ORG_DENIAL', actor: 'manager@punegreen.agency.example', ipAddress: '14.139.112.4', resourceTarget: '/api/distributor/1/inventory', severity: 'WARNING', details: 'ReBAC boundary violation: Distributor #2 attempted cross-agency access to Distributor #1 inventory' },
  { id: 'SEC-104', timestamp: '2 hours ago', eventType: 'TOKEN_EXPIRED', actor: 'driver@lpgchain.example', ipAddress: '106.51.24.11', resourceTarget: '/api/deliveries/501', severity: 'INFO', details: 'Session expired naturally after 24 hours, redirected to login' },
];

export const AdminRolesSecurityView: React.FC = () => {
  const [selectedEvent, setSelectedEvent] = useState<SecurityAuditEvent | null>(null);

  const renderPermBadge = (perm: string) => {
    switch (perm) {
      case 'FULL_CONTROL':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
            Full Control
          </span>
        );
      case 'READ_WRITE':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            Read / Write
          </span>
        );
      case 'READ_ONLY':
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
            Read Only
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-400 border border-slate-200">
            No Access
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* View Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Shield size={20} className="text-rose-600" />
            <span>Role-Based Access & Security Governance</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Audit platform RBAC authorization matrices, distributor ReBAC memberships, and access boundary violations.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            ReBAC & JWT Verified
          </span>
        </div>
      </div>

      {/* Safety Notice regarding Unrestricted Role Editing */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start gap-2.5">
        <Lock size={16} className="text-rose-600 mt-0.5 shrink-0" />
        <div>
          <strong className="text-slate-900 font-bold block">Security Governance Policy (Stage 1):</strong>
          In accordance with cybersecurity protocols, unrestricted ad-hoc permission mutation and security role assignments are strictly locked to programmatic database migrations. Stage 2 will introduce dual-admin signed approval workflows for privilege changes.
        </div>
      </div>

      {/* ── Platform RBAC Permission Matrix ── */}
      <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
        <div>
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <Key size={16} className="text-rose-600" />
            <span>Role-Based Access Control (RBAC) Entitlements</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Standard platform permissions enforced at Spring Security filter and controller levels
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/70 text-slate-600 font-bold uppercase text-[10px]">
                <th className="py-3 px-3">System Module</th>
                <th className="py-3 px-3">Scope Description</th>
                <th className="py-3 px-3 text-center">ADMIN</th>
                <th className="py-3 px-3 text-center">DISTRIBUTOR</th>
                <th className="py-3 px-3 text-center">CONSUMER</th>
                <th className="py-3 px-3 text-center">DRIVER</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {PREVIEW_PERMISSIONS.map((perm) => (
                <tr key={perm.module} className="hover:bg-slate-50/70 transition-colors">
                  <td className="py-3 px-3 font-bold text-slate-900">{perm.module}</td>
                  <td className="py-3 px-3 text-slate-500 text-[11px]">{perm.description}</td>
                  <td className="py-3 px-3 text-center">{renderPermBadge(perm.adminPerm)}</td>
                  <td className="py-3 px-3 text-center">{renderPermBadge(perm.distributorPerm)}</td>
                  <td className="py-3 px-3 text-center">{renderPermBadge(perm.consumerPerm)}</td>
                  <td className="py-3 px-3 text-center">{renderPermBadge(perm.driverPerm)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Two Columns: ReBAC Memberships + Security Audit Events ── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left: Distributor Memberships (ReBAC) */}
        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Building2 size={16} className="text-rose-600" />
                <span>Distributor ReBAC Memberships</span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Organizational tenancy bindings isolating agency data
              </p>
            </div>
            <span className="text-[11px] font-semibold text-slate-400">
              {PREVIEW_MEMBERSHIPS.length} Bound Users
            </span>
          </div>

          <div className="space-y-2.5">
            {PREVIEW_MEMBERSHIPS.map((mem) => (
              <div
                key={mem.id}
                className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between gap-3"
              >
                <div>
                  <div className="font-bold text-slate-900 text-xs">{mem.userName}</div>
                  <span className="text-[11px] text-slate-500">{mem.userEmail}</span>
                  <div className="text-[10px] text-rose-600 font-semibold mt-0.5">
                    {mem.distributorName} (Depot #{mem.distributorId})
                  </div>
                </div>

                <div className="flex flex-col items-end gap-1">
                  <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-900 text-white font-mono">
                    {mem.membershipRole}
                  </span>
                  <AdminStatusBadge status={mem.status} size="sm" />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right: Security Events */}
        <div className="p-5 bg-white rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <ShieldAlert size={16} className="text-rose-600" />
                <span>Security Event Audit</span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Authentication attempts and ReBAC boundary enforcement
              </p>
            </div>
            <span className="text-[11px] font-semibold text-slate-400">Real-time Stream</span>
          </div>

          <div className="space-y-2.5">
            {PREVIEW_SECURITY_EVENTS.map((event) => (
              <div
                key={event.id}
                onClick={() => setSelectedEvent(event)}
                className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 flex items-start justify-between gap-3 cursor-pointer hover:bg-slate-100/70 transition-colors"
              >
                <div className="flex items-start gap-2.5">
                  <div
                    className={`w-2 h-2 rounded-full mt-1.5 shrink-0 ${
                      event.severity === 'CRITICAL'
                        ? 'bg-rose-600'
                        : event.severity === 'WARNING'
                        ? 'bg-amber-500'
                        : 'bg-emerald-500'
                    }`}
                  />
                  <div>
                    <div className="font-bold text-slate-900 text-xs">
                      {event.eventType.replace(/_/g, ' ')}
                    </div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      Actor: <span className="font-mono text-slate-700">{event.actor}</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-0.5">{event.details}</div>
                  </div>
                </div>

                <div className="flex flex-col items-end gap-1 shrink-0 text-right">
                  <span className="text-[10px] text-slate-400 font-mono">{event.timestamp}</span>
                  <span className="text-[10px] font-mono text-slate-500 bg-white px-1.5 py-0.5 rounded border border-slate-200">
                    {event.ipAddress}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Security Event Detail Modal */}
      <AdminDetailModal
        isOpen={!!selectedEvent}
        title="Security Telemetry Inspection"
        subtitle={`Audit ID: ${selectedEvent?.id} • Logged ${selectedEvent?.timestamp}`}
        badge={
          selectedEvent ? (
            <AdminStatusBadge
              status={
                selectedEvent.severity === 'CRITICAL'
                  ? 'CRITICAL'
                  : selectedEvent.severity === 'WARNING'
                  ? 'WARNING'
                  : 'SUCCESS'
              }
              size="sm"
            />
          ) : undefined
        }
        icon={<Shield size={18} className="text-rose-600" />}
        onClose={() => setSelectedEvent(null)}
      >
        {selectedEvent && (
          <div className="space-y-4 text-xs text-slate-700">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3">
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Event Type</span>
                <span className="font-bold text-slate-900 text-sm">{selectedEvent.eventType}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Source IP Address</span>
                <span className="font-mono font-semibold text-slate-900">{selectedEvent.ipAddress}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Actor Principal</span>
                <span className="font-mono text-slate-900">{selectedEvent.actor}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Target Resource</span>
                <span className="font-mono text-slate-700">{selectedEvent.resourceTarget}</span>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-white border border-slate-200">
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">
                Event Forensic Summary
              </span>
              <p className="text-slate-800 leading-relaxed">{selectedEvent.details}</p>
            </div>
          </div>
        )}
      </AdminDetailModal>
    </div>
  );
};
