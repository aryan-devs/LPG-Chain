import React, { useState, useMemo } from 'react';
import { FileText, Eye } from 'lucide-react';
import { AdminAuditRecord } from '../../../types/adminPortalTypes';
import { AdminTable, ColumnDef } from '../components/AdminTable';
import { AdminStatusBadge } from '../components/AdminStatusBadge';
import { AdminFilterBar, FilterDropdown } from '../components/AdminFilterBar';
import { AdminDetailModal } from '../components/AdminDetailModal';
import { AdminEmptyState } from '../components/AdminEmptyState';

const SAMPLE_AUDIT_LOGS: AdminAuditRecord[] = [
  {
    id: 'LOG-88401',
    timestamp: '2026-10-14 19:45:12',
    actor: 'admin@lpgchain.example',
    actorRole: 'ADMIN',
    action: 'ALLOCATION_ADJUSTMENT',
    entityType: 'ALLOCATION',
    entityId: 'ALLOC-102',
    outcome: 'SUCCESS',
    sourceIp: '10.0.4.12',
    summary: 'Adjusted October 2026 commercial quota (+50 units) under emergency transfer REF-OMC-9041.',
  },
  {
    id: 'LOG-88402',
    timestamp: '2026-10-14 19:15:30',
    actor: 'distributor@agency.example',
    actorRole: 'DISTRIBUTOR',
    action: 'DRIVER_DISPATCH',
    entityType: 'DELIVERY',
    entityId: 'DEL-501',
    outcome: 'SUCCESS',
    sourceIp: '14.139.112.4',
    summary: 'Assigned Driver Suresh Patil (MH-04-AB-1234) to Booking #BK-10042.',
  },
  {
    id: 'LOG-88403',
    timestamp: '2026-10-14 18:50:22',
    actor: 'customer@example.com',
    actorRole: 'CONSUMER',
    action: 'ROLE_ELEVATION',
    entityType: 'USER',
    entityId: 'USER-1',
    outcome: 'DENIED',
    sourceIp: '152.58.18.91',
    summary: 'System blocked unauthorized attempt to self-assign privileged role DISTRIBUTOR.',
  },
  {
    id: 'LOG-88404',
    timestamp: '2026-10-14 17:30:15',
    actor: 'driver@lpgchain.example',
    actorRole: 'DRIVER',
    action: 'OTP_VERIFICATION',
    entityType: 'DELIVERY',
    entityId: 'DEL-498',
    outcome: 'SUCCESS',
    sourceIp: '106.51.24.11',
    summary: 'Completed domestic cylinder handoff with validated 6-digit customer OTP.',
  },
  {
    id: 'LOG-88405',
    timestamp: '2026-10-14 16:10:04',
    actor: 'System ReBAC Guardian',
    actorRole: 'SYSTEM',
    action: 'CROSS_ORG_ACCESS',
    entityType: 'DISTRIBUTOR',
    entityId: 'DIST-001',
    outcome: 'DENIED',
    sourceIp: '10.0.4.88',
    summary: 'Access boundary enforcement blocked user #5 from viewing cross-organization depot records.',
  },
];

export const AdminAuditLogsView: React.FC = () => {
  // If backend does not yet provide audit records, allow user to view empty state or preview sample logs
  const [useSampleRecords, setUseSampleRecords] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');
  const [outcomeFilter, setOutcomeFilter] = useState('ALL');
  const [selectedLog, setSelectedLog] = useState<AdminAuditRecord | null>(null);

  const logs = useSampleRecords ? SAMPLE_AUDIT_LOGS : [];

  const filteredLogs = useMemo(() => {
    return logs.filter((l) => {
      const matchSearch =
        l.actor.toLowerCase().includes(searchQuery.toLowerCase()) ||
        l.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
        l.entityId.toLowerCase().includes(searchQuery.toLowerCase()) ||
        l.summary.toLowerCase().includes(searchQuery.toLowerCase());

      const matchAction = actionFilter === 'ALL' || l.action === actionFilter;
      const matchOutcome = outcomeFilter === 'ALL' || l.outcome === outcomeFilter;

      return matchSearch && matchAction && matchOutcome;
    });
  }, [logs, searchQuery, actionFilter, outcomeFilter]);

  const filterDropdowns: FilterDropdown[] = [
    {
      id: 'action',
      label: 'Action Type',
      value: actionFilter,
      onChange: setActionFilter,
      options: [
        { label: 'All Action Types', value: 'ALL' },
        { label: 'Allocation Adjustment', value: 'ALLOCATION_ADJUSTMENT' },
        { label: 'Driver Dispatch', value: 'DRIVER_DISPATCH' },
        { label: 'Role Elevation', value: 'ROLE_ELEVATION' },
        { label: 'OTP Verification', value: 'OTP_VERIFICATION' },
        { label: 'Cross Org Access', value: 'CROSS_ORG_ACCESS' },
      ],
    },
    {
      id: 'outcome',
      label: 'Outcome',
      value: outcomeFilter,
      onChange: setOutcomeFilter,
      options: [
        { label: 'All Outcomes', value: 'ALL' },
        { label: 'Success', value: 'SUCCESS' },
        { label: 'Denied', value: 'DENIED' },
        { label: 'Failed', value: 'FAILED' },
      ],
    },
  ];

  const columns: ColumnDef<AdminAuditRecord>[] = [
    {
      key: 'timestamp',
      header: 'Timestamp',
      render: (l) => (
        <div className="flex flex-col">
          <span className="font-mono text-slate-900 font-semibold text-xs">{l.timestamp}</span>
          <span className="text-[10px] text-slate-400 font-mono">{l.id}</span>
        </div>
      ),
    },
    {
      key: 'actor',
      header: 'Actor & Role',
      render: (l) => (
        <div className="flex flex-col">
          <span className="font-bold text-slate-900 text-xs">{l.actor}</span>
          <span className="text-[10px] text-slate-500 font-semibold">{l.actorRole}</span>
        </div>
      ),
    },
    {
      key: 'action',
      header: 'Action Performed',
      render: (l) => (
        <span className="font-bold text-slate-800 text-xs font-mono">
          {l.action.replace(/_/g, ' ')}
        </span>
      ),
    },
    {
      key: 'entity',
      header: 'Entity Affected',
      render: (l) => (
        <div className="flex items-center gap-1.5">
          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-mono text-[11px] font-semibold">
            {l.entityId}
          </span>
        </div>
      ),
    },
    {
      key: 'outcome',
      header: 'Outcome',
      render: (l) => <AdminStatusBadge status={l.outcome} size="sm" />,
    },
    {
      key: 'sourceIp',
      header: 'Source IP',
      render: (l) => (
        <span className="font-mono text-slate-500 text-[11px]">{l.sourceIp}</span>
      ),
    },
    {
      key: 'actions',
      header: 'Details',
      className: 'text-right',
      render: (l) => (
        <button
          onClick={() => setSelectedLog(l)}
          className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100"
        >
          <Eye size={15} />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <FileText size={20} className="text-rose-600" />
            <span>Platform Activity Audit Trail</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Immutable system logs tracking administrative mutations, quota updates, dispatches, and authentication events.
          </p>
        </div>

        {/* Backend state / preview switcher */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500">Audit Source:</span>
          <button
            onClick={() => setUseSampleRecords(!useSampleRecords)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all border ${
              useSampleRecords
                ? 'bg-rose-50 text-rose-700 border-rose-200'
                : 'bg-slate-100 text-slate-700 border-slate-200'
            }`}
          >
            {useSampleRecords ? 'Preview Audit Trail' : 'Empty State (Default)'}
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <AdminFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        searchPlaceholder="Filter by actor, action, entity ref, or summary keywords..."
        dropdowns={filterDropdowns}
        onReset={() => {
          setSearchQuery('');
          setActionFilter('ALL');
          setOutcomeFilter('ALL');
        }}
        totalResults={filteredLogs.length}
      />

      {/* Main Table or Empty State */}
      {!useSampleRecords || filteredLogs.length === 0 ? (
        <AdminEmptyState
          icon={<FileText size={28} className="text-slate-400" />}
          title="No Audit Records Available"
          description="The backend database does not currently have persistent activity audit records stored. When audit logging is enabled in Stage 2, records will stream here in real time."
          actionLabel="Load Preview Format"
          onAction={() => setUseSampleRecords(true)}
        />
      ) : (
        <AdminTable
          columns={columns}
          data={filteredLogs}
          keyExtractor={(l) => l.id}
          pageSize={8}
          onRowClick={(l) => setSelectedLog(l)}
        />
      )}

      {/* Audit Detail Modal */}
      <AdminDetailModal
        isOpen={!!selectedLog}
        title="Audit Log Entry Inspection"
        subtitle={`Log Ref #${selectedLog?.id} • Timestamp: ${selectedLog?.timestamp}`}
        badge={selectedLog ? <AdminStatusBadge status={selectedLog.outcome} size="sm" /> : undefined}
        icon={<FileText size={18} className="text-rose-600" />}
        onClose={() => setSelectedLog(null)}
      >
        {selectedLog && (
          <div className="space-y-4 text-xs text-slate-700">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3">
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Actor Principal</span>
                <span className="font-semibold text-slate-900 text-sm">{selectedLog.actor}</span>
                <span className="text-[11px] text-slate-500">Role: {selectedLog.actorRole}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Source Network IP</span>
                <span className="font-mono text-slate-900 text-sm">{selectedLog.sourceIp}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Action Type</span>
                <span className="font-mono font-bold text-rose-600">{selectedLog.action}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Affected Target</span>
                <span className="font-mono text-slate-900">{selectedLog.entityId} ({selectedLog.entityType})</span>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-white border border-slate-200 space-y-1">
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
                Operation Audit Summary
              </span>
              <p className="text-slate-800 leading-relaxed font-medium">{selectedLog.summary}</p>
            </div>
          </div>
        )}
      </AdminDetailModal>
    </div>
  );
};
