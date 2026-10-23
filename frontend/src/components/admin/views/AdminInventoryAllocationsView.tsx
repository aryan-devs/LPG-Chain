import React, { useState } from 'react';
import {
  Package,
  Layers,
  AlertTriangle,
  CheckCircle2,
  TrendingUp,
  BarChart3,
} from 'lucide-react';
import {
  DistributorInventorySummary,
  OmcAllocationItem,
} from '../../../types/adminPortalTypes';
import { AdminTable, ColumnDef } from '../components/AdminTable';
import { AdminDetailModal } from '../components/AdminDetailModal';
import { AdminConfirmModal } from '../components/AdminConfirmModal';

// Clear preview inventory summaries distinguishing physical stock from OMC allocations
const PREVIEW_INVENTORIES: DistributorInventorySummary[] = [
  {
    distributorId: 1,
    distributorName: 'Apex Gas Distribution Agency',
    distributorCode: 'DIST-001',
    city: 'Mumbai',
    totalFilled: 3200,
    totalEmpty: 1240,
    totalReserved: 110,
    totalDamaged: 18,
    totalAvailable: 3090,
    discrepancyCount: 0,
    lastAuditedAt: '2026-10-14 08:00',
    categories: [
      { category: 'DOMESTIC_14_2', displayName: 'Domestic 14.2 kg', capacityKg: 14.2, filledStock: 2400, emptyStock: 980, reservedStock: 80, damagedStock: 12, availableStock: 2320, allocatedMonthly: 3000, deliveredMonthly: 2150 },
      { category: 'COMMERCIAL_19', displayName: 'Commercial 19 kg', capacityKg: 19.0, filledStock: 680, emptyStock: 210, reservedStock: 25, damagedStock: 5, availableStock: 655, allocatedMonthly: 850, deliveredMonthly: 620 },
      { category: 'INDUSTRIAL_47_5', displayName: 'Industrial 47.5 kg', capacityKg: 47.5, filledStock: 120, emptyStock: 50, reservedStock: 5, damagedStock: 1, availableStock: 115, allocatedMonthly: 150, deliveredMonthly: 95 },
    ],
  },
  {
    distributorId: 2,
    distributorName: 'Pune Green Gas Depot',
    distributorCode: 'DIST-002',
    city: 'Pune',
    totalFilled: 2450,
    totalEmpty: 980,
    totalReserved: 95,
    totalDamaged: 22,
    totalAvailable: 2355,
    discrepancyCount: 1,
    lastAuditedAt: '2026-10-13 19:30',
    categories: [
      { category: 'DOMESTIC_14_2', displayName: 'Domestic 14.2 kg', capacityKg: 14.2, filledStock: 1850, emptyStock: 740, reservedStock: 70, damagedStock: 16, availableStock: 1780, allocatedMonthly: 2200, deliveredMonthly: 1600 },
      { category: 'COMMERCIAL_19', displayName: 'Commercial 19 kg', capacityKg: 19.0, filledStock: 600, emptyStock: 240, reservedStock: 25, damagedStock: 6, availableStock: 575, allocatedMonthly: 750, deliveredMonthly: 540 },
    ],
  },
  {
    distributorId: 3,
    distributorName: 'City Gas Services Ltd',
    distributorCode: 'DIST-003',
    city: 'Nagpur',
    totalFilled: 1980,
    totalEmpty: 650,
    totalReserved: 40,
    totalDamaged: 8,
    totalAvailable: 1940,
    discrepancyCount: 0,
    lastAuditedAt: '2026-10-14 07:30',
    categories: [
      { category: 'DOMESTIC_14_2', displayName: 'Domestic 14.2 kg', capacityKg: 14.2, filledStock: 1400, emptyStock: 480, reservedStock: 30, damagedStock: 6, availableStock: 1370, allocatedMonthly: 1800, deliveredMonthly: 1220 },
      { category: 'COMMERCIAL_19', displayName: 'Commercial 19 kg', capacityKg: 19.0, filledStock: 450, emptyStock: 130, reservedStock: 10, damagedStock: 2, availableStock: 440, allocatedMonthly: 550, deliveredMonthly: 380 },
      { category: 'INDUSTRIAL_47_5', displayName: 'Industrial 47.5 kg', capacityKg: 47.5, filledStock: 130, emptyStock: 40, reservedStock: 0, damagedStock: 0, availableStock: 130, allocatedMonthly: 160, deliveredMonthly: 110 },
    ],
  },
];

const PREVIEW_ALLOCATIONS: OmcAllocationItem[] = [
  { id: 101, distributorId: 1, distributorName: 'Apex Gas Distribution Agency', distributorCode: 'DIST-001', cylinderCategory: 'DOMESTIC_14_2', categoryDisplayName: 'Domestic 14.2 kg', capacityKg: 14.2, allocationMonth: 10, allocationYear: 2026, allocatedQuantity: 3000, deliveredQuantity: 2150, adjustmentQuantity: 0, remainingQuantity: 850, sourceReference: 'OMC-IOCL-OCT26-MH01', status: 'ACTIVE', createdAt: '2026-10-01' },
  { id: 102, distributorId: 1, distributorName: 'Apex Gas Distribution Agency', distributorCode: 'DIST-001', cylinderCategory: 'COMMERCIAL_19', categoryDisplayName: 'Commercial 19 kg', capacityKg: 19.0, allocationMonth: 10, allocationYear: 2026, allocatedQuantity: 850, deliveredQuantity: 620, adjustmentQuantity: 50, remainingQuantity: 280, sourceReference: 'OMC-IOCL-OCT26-MH02', status: 'ACTIVE', createdAt: '2026-10-01' },
  { id: 103, distributorId: 2, distributorName: 'Pune Green Gas Depot', distributorCode: 'DIST-002', cylinderCategory: 'DOMESTIC_14_2', categoryDisplayName: 'Domestic 14.2 kg', capacityKg: 14.2, allocationMonth: 10, allocationYear: 2026, allocatedQuantity: 2200, deliveredQuantity: 1600, adjustmentQuantity: 0, remainingQuantity: 600, sourceReference: 'OMC-BPCL-OCT26-PN01', status: 'ACTIVE', createdAt: '2026-10-01' },
  { id: 104, distributorId: 2, distributorName: 'Pune Green Gas Depot', distributorCode: 'DIST-002', cylinderCategory: 'COMMERCIAL_19', categoryDisplayName: 'Commercial 19 kg', capacityKg: 19.0, allocationMonth: 10, allocationYear: 2026, allocatedQuantity: 750, deliveredQuantity: 540, adjustmentQuantity: 0, remainingQuantity: 210, sourceReference: 'OMC-BPCL-OCT26-PN02', status: 'ACTIVE', createdAt: '2026-10-01' },
  { id: 105, distributorId: 3, distributorName: 'City Gas Services Ltd', distributorCode: 'DIST-003', cylinderCategory: 'DOMESTIC_14_2', categoryDisplayName: 'Domestic 14.2 kg', capacityKg: 14.2, allocationMonth: 10, allocationYear: 2026, allocatedQuantity: 1800, deliveredQuantity: 1220, adjustmentQuantity: 0, remainingQuantity: 580, sourceReference: 'OMC-HPCL-OCT26-NG01', status: 'ACTIVE', createdAt: '2026-10-01' },
];

export const AdminInventoryAllocationsView: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'physical-inventory' | 'omc-allocations'>('physical-inventory');
  const [selectedInventory, setSelectedInventory] = useState<DistributorInventorySummary | null>(null);
  const [selectedAllocation, setSelectedAllocation] = useState<OmcAllocationItem | null>(null);

  // New adjustment modal state
  const [isAdjustmentModalOpen, setIsAdjustmentModalOpen] = useState(false);
  const [adjustmentForm, setAdjustmentForm] = useState({
    type: 'EMERGENCY_TRANSFER',
    quantityChange: 50,
    reason: '',
    referenceNumber: '',
  });

  const [confirmModalOpen, setConfirmModalOpen] = useState(false);

  const inventoryColumns: ColumnDef<DistributorInventorySummary>[] = [
    {
      key: 'agency',
      header: 'Agency Depot',
      render: (inv) => (
        <div className="flex flex-col">
          <span className="font-bold text-slate-900">{inv.distributorName}</span>
          <span className="text-[11px] text-slate-400 font-mono">
            {inv.distributorCode} • {inv.city}
          </span>
        </div>
      ),
    },
    {
      key: 'totalFilled',
      header: 'Filled Cylinders',
      render: (inv) => (
        <span className="font-bold text-emerald-700 text-xs">
          {inv.totalFilled.toLocaleString()}
        </span>
      ),
    },
    {
      key: 'totalEmpty',
      header: 'Empty Cylinders',
      render: (inv) => (
        <span className="font-semibold text-slate-700 text-xs">
          {inv.totalEmpty.toLocaleString()}
        </span>
      ),
    },
    {
      key: 'totalReserved',
      header: 'Reserved',
      render: (inv) => (
        <span className="font-semibold text-amber-700 text-xs">
          {inv.totalReserved.toLocaleString()}
        </span>
      ),
    },
    {
      key: 'totalDamaged',
      header: 'Damaged / OOS',
      render: (inv) => (
        <span
          className={`font-semibold text-xs ${
            inv.totalDamaged > 15 ? 'text-rose-600 font-bold' : 'text-slate-600'
          }`}
        >
          {inv.totalDamaged.toLocaleString()}
        </span>
      ),
    },
    {
      key: 'discrepancyCount',
      header: 'Discrepancy Audit',
      render: (inv) =>
        inv.discrepancyCount > 0 ? (
          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-50 border border-rose-200 px-2 py-0.5 rounded-full">
            <AlertTriangle size={12} /> {inv.discrepancyCount} Anomaly
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
            <CheckCircle2 size={12} /> Reconciled
          </span>
        ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (inv) => (
        <button
          onClick={() => setSelectedInventory(inv)}
          className="px-2.5 py-1 text-xs font-semibold text-rose-600 hover:bg-rose-50 border border-rose-200 rounded-lg transition-colors"
        >
          Inspect Stock
        </button>
      ),
    },
  ];

  const allocationColumns: ColumnDef<OmcAllocationItem>[] = [
    {
      key: 'agency',
      header: 'Agency & Category',
      render: (a) => (
        <div className="flex flex-col">
          <span className="font-bold text-slate-900">{a.distributorName}</span>
          <span className="text-[11px] text-rose-600 font-semibold">{a.categoryDisplayName}</span>
        </div>
      ),
    },
    {
      key: 'period',
      header: 'Allocation Period',
      render: (a) => (
        <span className="font-mono text-slate-700 text-xs font-medium">
          {a.allocationMonth}/{a.allocationYear}
        </span>
      ),
    },
    {
      key: 'allocatedQuantity',
      header: 'Allocated Quota',
      render: (a) => (
        <span className="font-bold text-slate-900 text-xs">
          {a.allocatedQuantity.toLocaleString()}
        </span>
      ),
    },
    {
      key: 'deliveredQuantity',
      header: 'Completed Sales',
      render: (a) => (
        <span className="font-semibold text-emerald-700 text-xs">
          {a.deliveredQuantity.toLocaleString()}
        </span>
      ),
    },
    {
      key: 'remainingQuantity',
      header: 'Remaining Balance',
      render: (a) => {
        const util = Math.round((a.deliveredQuantity / a.allocatedQuantity) * 100);
        return (
          <div className="flex flex-col text-xs">
            <span className="font-bold text-slate-800">{a.remainingQuantity} units</span>
            <span className="text-[10px] text-slate-500 font-mono">{util}% quota utilized</span>
          </div>
        );
      },
    },
    {
      key: 'sourceReference',
      header: 'OMC Source Ref',
      render: (a) => (
        <span className="font-mono text-[11px] text-slate-500 bg-slate-50 px-2 py-0.5 rounded border border-slate-200">
          {a.sourceReference}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (a) => (
        <button
          onClick={() => {
            setSelectedAllocation(a);
            setIsAdjustmentModalOpen(true);
          }}
          className="px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-100 border border-slate-200 rounded-lg transition-colors"
        >
          Adjust Quota
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      {/* View Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Layers size={20} className="text-rose-600" />
            <span>Inventory Audits & OMC Quota Allocations</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Maintain strict distinction between physical depot cylinder counts and regulatory monthly OMC quotas.
          </p>
        </div>

        {/* Sub-tab Navigation */}
        <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-xl border border-slate-200/80">
          <button
            onClick={() => setActiveSubTab('physical-inventory')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'physical-inventory'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Physical Stock Audit
          </button>
          <button
            onClick={() => setActiveSubTab('omc-allocations')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'omc-allocations'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            OMC Quotas & Adjustments
          </button>
        </div>
      </div>

      {/* Distinction Explanatory Banner */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-700 grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="flex items-start gap-2.5">
          <Package size={16} className="text-rose-600 mt-0.5 shrink-0" />
          <div>
            <strong className="text-slate-900 font-bold block">Physical Cylinder Inventory:</strong>
            Actual hardware counts on depot premises (filled refills, empty returns, reserved customer cylinders, and damaged cylinders undergoing hydraulic testing).
          </div>
        </div>

        <div className="flex items-start gap-2.5">
          <BarChart3 size={16} className="text-rose-600 mt-0.5 shrink-0" />
          <div>
            <strong className="text-slate-900 font-bold block">OMC Allocation Quota:</strong>
            Regulatory monthly quota authorized by Oil Marketing Companies (IOCL, BPCL, HPCL) determining maximum subsidized and commercial volume an agency may distribute.
          </div>
        </div>
      </div>

      {/* Tab 1: Physical Inventory View */}
      {activeSubTab === 'physical-inventory' && (
        <div className="space-y-4">
          <AdminTable
            columns={inventoryColumns}
            data={PREVIEW_INVENTORIES}
            keyExtractor={(inv) => inv.distributorId}
            emptyTitle="No inventory data available"
            pageSize={5}
            onRowClick={(inv) => setSelectedInventory(inv)}
          />
        </div>
      )}

      {/* Tab 2: OMC Allocations View */}
      {activeSubTab === 'omc-allocations' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">
              Active Monthly Quotas (October 2026)
            </span>
          </div>

          <AdminTable
            columns={allocationColumns}
            data={PREVIEW_ALLOCATIONS}
            keyExtractor={(a) => a.id}
            emptyTitle="No OMC allocations recorded"
            pageSize={5}
          />
        </div>
      )}

      {/* ── Category Breakdown Detail Modal ── */}
      <AdminDetailModal
        isOpen={!!selectedInventory}
        title={selectedInventory?.distributorName || 'Physical Inventory Audit'}
        subtitle={`Depot Code: ${selectedInventory?.distributorCode} • Last Reconciled: ${selectedInventory?.lastAuditedAt}`}
        icon={<Package size={18} className="text-rose-600" />}
        maxWidth="xl"
        onClose={() => setSelectedInventory(null)}
        footerActions={
          <button
            onClick={() => setSelectedInventory(null)}
            className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl"
          >
            Close Audit
          </button>
        }
      >
        {selectedInventory && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-100">
                <span className="text-[10px] text-emerald-800 font-semibold block">Total Available</span>
                <span className="text-lg font-black text-emerald-700">
                  {selectedInventory.totalAvailable.toLocaleString()}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-slate-100 border border-slate-200">
                <span className="text-[10px] text-slate-600 font-semibold block">Empty Returns</span>
                <span className="text-lg font-black text-slate-700">
                  {selectedInventory.totalEmpty.toLocaleString()}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-amber-50 border border-amber-100">
                <span className="text-[10px] text-amber-800 font-semibold block">Reserved</span>
                <span className="text-lg font-black text-amber-700">
                  {selectedInventory.totalReserved.toLocaleString()}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-100">
                <span className="text-[10px] text-rose-800 font-semibold block">Damaged / OOS</span>
                <span className="text-lg font-black text-rose-700">
                  {selectedInventory.totalDamaged.toLocaleString()}
                </span>
              </div>
            </div>

            {/* Category breakdown table */}
            <div className="border border-slate-200 rounded-xl overflow-hidden">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold uppercase text-[10px]">
                  <tr>
                    <th className="p-2.5">Category</th>
                    <th className="p-2.5">Filled</th>
                    <th className="p-2.5">Empty</th>
                    <th className="p-2.5">Reserved</th>
                    <th className="p-2.5">Damaged</th>
                    <th className="p-2.5">Net Available</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {selectedInventory.categories.map((c) => (
                    <tr key={c.category}>
                      <td className="p-2.5 font-bold text-slate-900">{c.displayName}</td>
                      <td className="p-2.5 text-emerald-600 font-semibold">{c.filledStock}</td>
                      <td className="p-2.5 text-slate-600">{c.emptyStock}</td>
                      <td className="p-2.5 text-amber-600">{c.reservedStock}</td>
                      <td className="p-2.5 text-rose-600">{c.damagedStock}</td>
                      <td className="p-2.5 font-bold text-slate-900">{c.availableStock}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </AdminDetailModal>

      {/* ── Allocation Adjustment Modal ── */}
      <AdminDetailModal
        isOpen={isAdjustmentModalOpen && !!selectedAllocation}
        title="Record Auditable Quota Adjustment"
        subtitle={`Allocation #${selectedAllocation?.id} • ${selectedAllocation?.distributorName}`}
        icon={<TrendingUp size={18} className="text-rose-600" />}
        maxWidth="md"
        onClose={() => setIsAdjustmentModalOpen(false)}
        footerActions={
          <>
            <button
              type="button"
              onClick={() => setIsAdjustmentModalOpen(false)}
              className="px-3.5 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-200/60 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => setConfirmModalOpen(true)}
              className="px-4 py-2 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-xl shadow-xs"
            >
              Review & Submit Adjustment
            </button>
          </>
        }
      >
        <div className="space-y-4 text-xs text-slate-700">
          <div>
            <label className="block text-[11px] font-bold text-slate-700 mb-1">
              Adjustment Type
            </label>
            <select
              value={adjustmentForm.type}
              onChange={(e) => setAdjustmentForm({ ...adjustmentForm, type: e.target.value })}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500"
            >
              <option value="EMERGENCY_TRANSFER">Emergency Depot Transfer</option>
              <option value="QUOTA_INCREASE">Official Quota Increase</option>
              <option value="QUOTA_REDUCTION">Regulatory Quota Reduction</option>
              <option value="INTER_DEPOT_RETURN">Inter-Depot Stock Return</option>
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-bold text-slate-700 mb-1">
              Quantity Change (Cylinders)
            </label>
            <input
              type="number"
              value={adjustmentForm.quantityChange}
              onChange={(e) =>
                setAdjustmentForm({ ...adjustmentForm, quantityChange: Number(e.target.value) })
              }
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold text-slate-700 mb-1">
              Reason / Justification
            </label>
            <textarea
              rows={2}
              value={adjustmentForm.reason}
              onChange={(e) => setAdjustmentForm({ ...adjustmentForm, reason: e.target.value })}
              placeholder="e.g. Festival demand surge authorized by state OMC director..."
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500"
            />
          </div>

          <div>
            <label className="block text-[11px] font-bold text-slate-700 mb-1">
              OMC Approval Reference Number
            </label>
            <input
              type="text"
              value={adjustmentForm.referenceNumber}
              onChange={(e) =>
                setAdjustmentForm({ ...adjustmentForm, referenceNumber: e.target.value })
              }
              placeholder="e.g. REF-OMC-2026-9041"
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500 font-mono"
            />
          </div>
        </div>
      </AdminDetailModal>

      {/* Confirm Adjustment Modal */}
      <AdminConfirmModal
        isOpen={confirmModalOpen}
        title="Confirm Quota Adjustment"
        description={`Record adjustment of ${adjustmentForm.quantityChange} cylinders under type ${adjustmentForm.type} for ${selectedAllocation?.distributorName}.`}
        confirmLabel="Confirm Record"
        stageNote="Non-destructive UI preview: Quota adjustments dispatch to /api/allocations/{id}/adjustments in Stage 2."
        onConfirm={() => {
          setConfirmModalOpen(false);
          setIsAdjustmentModalOpen(false);
        }}
        onClose={() => setConfirmModalOpen(false)}
      />
    </div>
  );
};
