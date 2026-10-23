import React, { useState, useEffect } from 'react';
import {
  Package,
  RefreshCw,
  History,
  AlertCircle,
  Flame,
} from 'lucide-react';
import {
  DistributorInventoryItem,
  InventoryMovementItem,
} from '../../../types/distributorPortalTypes';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorInventoryViewProps {
  inventory: DistributorInventoryItem[];
  distributorId: number;
  isLoading: boolean;
  onRefresh: () => void;
  onInventoryUpdated: () => void;
}

export const DistributorInventoryView: React.FC<DistributorInventoryViewProps> = ({
  inventory,
  distributorId,
  isLoading,
  onRefresh,
  onInventoryUpdated,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('DOMESTIC_14_2');
  const [movements, setMovements] = useState<InventoryMovementItem[]>([]);
  const [loadingMovements, setLoadingMovements] = useState(false);
  const [editModalItem, setEditModalItem] = useState<DistributorInventoryItem | null>(null);

  // Edit stock form state
  const [filledStockInput, setFilledStockInput] = useState<number>(0);
  const [emptyStockInput, setEmptyStockInput] = useState<number>(0);
  const [damagedStockInput, setDamagedStockInput] = useState<number>(0);
  const [adjustReason, setAdjustReason] = useState<string>('Weekly depot physical count');
  const [refNumber, setRefNumber] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch movements when selectedCategory changes
  useEffect(() => {
    let isMounted = true;
    setLoadingMovements(true);
    DistributorApiService.getInventoryMovements(distributorId, selectedCategory)
      .then((res) => {
        if (isMounted) setMovements(res);
      })
      .catch(() => {
        if (isMounted) setMovements([]);
      })
      .finally(() => {
        if (isMounted) setLoadingMovements(false);
      });

    return () => {
      isMounted = false;
    };
  }, [distributorId, selectedCategory]);

  const handleOpenEdit = (item: DistributorInventoryItem) => {
    setEditModalItem(item);
    setFilledStockInput(item.filledStock);
    setEmptyStockInput(item.emptyStock);
    setDamagedStockInput(item.damagedStock);
    setAdjustReason('Physical stock audit reconciliation');
    setRefNumber(`INV-DEPOT-${Math.floor(1000 + Math.random() * 9000)}`);
    setFormError(null);
  };

  const handleSaveStock = async () => {
    if (!editModalItem) return;
    if (filledStockInput < 0 || emptyStockInput < 0 || damagedStockInput < 0) {
      setFormError('Stock quantities cannot be negative');
      return;
    }

    try {
      setIsSubmitting(true);
      setFormError(null);
      await DistributorApiService.updateInventoryStock(distributorId, {
        cylinderCategory: editModalItem.cylinderCategory,
        filledStock: filledStockInput,
        emptyStock: emptyStockInput,
        damagedStock: damagedStockInput,
        reason: adjustReason,
        referenceNumber: refNumber,
      });

      setEditModalItem(null);
      onInventoryUpdated();
      // Reload movements
      const updatedMovements = await DistributorApiService.getInventoryMovements(
        distributorId,
        editModalItem.cylinderCategory
      );
      setMovements(updatedMovements);
    } catch (err: any) {
      setFormError(err.message || 'Failed to update stock');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-rose-50 text-rose-600">
              <Package size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600">
              Warehouse Depot Management
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">Physical Cylinder Inventory</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Real physical cylinder counts verified at depot. Distinct from monthly OMC quota allocations.
          </p>
        </div>

        <button
          onClick={onRefresh}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors self-start sm:self-auto"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          <span>Refresh Stock</span>
        </button>
      </div>

      {/* Category Stock Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {inventory.map((item) => (
          <div
            key={item.cylinderCategory}
            className={`bg-white rounded-2xl p-5 border shadow-xs transition-all flex flex-col justify-between ${
              selectedCategory === item.cylinderCategory
                ? 'border-rose-400 ring-2 ring-rose-500/10'
                : 'border-slate-200/80 hover:border-slate-300'
            }`}
          >
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
                    <Flame size={16} />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-slate-900 leading-tight">
                      {item.categoryDisplayName}
                    </h3>
                    <span className="text-[10px] text-slate-400">{item.capacityKg} kg capacity</span>
                  </div>
                </div>

                <span className="text-[10px] font-mono font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                  {item.cylinderCategory}
                </span>
              </div>

              {/* Counts Grid */}
              <div className="grid grid-cols-2 gap-2 my-4">
                <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-[10px] font-semibold text-slate-400 uppercase">Filled Depot Stock</span>
                  <p className="text-xl font-black text-slate-900 mt-0.5">{item.filledStock}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-100">
                  <span className="text-[10px] font-semibold text-emerald-700 uppercase">Available to Dispatch</span>
                  <p className="text-xl font-black text-emerald-800 mt-0.5">{item.availableStock}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-amber-50 border border-amber-100">
                  <span className="text-[10px] font-semibold text-amber-700 uppercase">Reserved (In Queue)</span>
                  <p className="text-lg font-black text-amber-800 mt-0.5">{item.reservedStock}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-[10px] font-semibold text-slate-400 uppercase">Empty Collected</span>
                  <p className="text-lg font-black text-slate-700 mt-0.5">{item.emptyStock}</p>
                </div>
              </div>

              {item.damagedStock > 0 && (
                <div className="p-2 rounded-lg bg-rose-50 border border-rose-100 text-[11px] text-rose-700 flex items-center gap-1.5 mb-2">
                  <AlertCircle size={13} />
                  <span>{item.damagedStock} cylinder(s) quarantined / damaged</span>
                </div>
              )}
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center gap-2 mt-2">
              <button
                onClick={() => setSelectedCategory(item.cylinderCategory)}
                className={`flex-1 py-1.5 px-3 rounded-xl text-xs font-bold transition-colors ${
                  selectedCategory === item.cylinderCategory
                    ? 'bg-slate-900 text-white'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                View Movements
              </button>
              <button
                onClick={() => handleOpenEdit(item)}
                className="py-1.5 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-xs transition-colors flex items-center gap-1"
              >
                <span>Audit</span>
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Movement Audit Trail Section */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-slate-100 text-slate-700 flex items-center justify-center">
              <History size={16} />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-900">
                Auditable Stock Movements ({selectedCategory})
              </h2>
              <p className="text-[11px] text-slate-400">
                Timestamped ledger of receipts, dispatches, empty returns, and audit reconciliations
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {inventory.map((inv) => (
              <button
                key={inv.cylinderCategory}
                onClick={() => setSelectedCategory(inv.cylinderCategory)}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-colors ${
                  selectedCategory === inv.cylinderCategory
                    ? 'bg-rose-600 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {inv.categoryDisplayName}
              </button>
            ))}
          </div>
        </div>

        {loadingMovements ? (
          <div className="p-8 text-center">
            <RefreshCw className="w-5 h-5 text-slate-400 animate-spin mx-auto mb-1" />
            <span className="text-xs text-slate-400">Loading audit trail...</span>
          </div>
        ) : movements.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 font-semibold uppercase text-[10px]">
                  <th className="py-2.5 px-3">Timestamp</th>
                  <th className="py-2.5 px-3">Movement Type</th>
                  <th className="py-2.5 px-3">Quantity Delta</th>
                  <th className="py-2.5 px-3">Reason / Description</th>
                  <th className="py-2.5 px-3">Reference No</th>
                  <th className="py-2.5 px-3">Audited By</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {movements.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-2.5 px-3 font-mono text-[11px] text-slate-500">
                      {m.performedAt ? new Date(m.performedAt).toLocaleString() : 'Recent'}
                    </td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          m.quantityChange >= 0
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}
                      >
                        {m.movementType}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 font-bold font-mono">
                      <span className={m.quantityChange >= 0 ? 'text-emerald-600' : 'text-rose-600'}>
                        {m.quantityChange >= 0 ? `+${m.quantityChange}` : m.quantityChange}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-600">{m.reason}</td>
                    <td className="py-2.5 px-3 font-mono text-slate-400">{m.referenceNumber || '—'}</td>
                    <td className="py-2.5 px-3 text-slate-500 font-medium">{m.performedBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-400 py-6 text-center">
            No stock movements recorded yet for this category. Perform a physical audit above to initialize movement history.
          </p>
        )}
      </div>

      {/* Physical Stock Audit Modal */}
      {editModalItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center gap-2 text-rose-600">
              <Package size={20} />
              <h2 className="text-base font-bold text-slate-900">
                Physical Stock Count Audit ({editModalItem.categoryDisplayName})
              </h2>
            </div>

            <p className="text-xs text-slate-600">
              Record physical depot counts. All adjustments are auditable and logged in the immutable movement ledger.
            </p>

            {formError && (
              <div className="p-3 rounded-xl bg-rose-50 text-rose-700 text-xs border border-rose-200">
                {formError}
              </div>
            )}

            <div className="space-y-3">
              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="text-[11px] font-bold text-slate-600 block mb-1">Filled Cylinders</label>
                  <input
                    type="number"
                    min="0"
                    value={filledStockInput}
                    onChange={(e) => setFilledStockInput(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-full text-xs p-2 rounded-xl border border-slate-300 font-bold font-mono focus:ring-2 focus:ring-rose-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-600 block mb-1">Empty Cylinders</label>
                  <input
                    type="number"
                    min="0"
                    value={emptyStockInput}
                    onChange={(e) => setEmptyStockInput(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-full text-xs p-2 rounded-xl border border-slate-300 font-bold font-mono focus:ring-2 focus:ring-rose-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-[11px] font-bold text-slate-600 block mb-1">Damaged/Quarantine</label>
                  <input
                    type="number"
                    min="0"
                    value={damagedStockInput}
                    onChange={(e) => setDamagedStockInput(Math.max(0, parseInt(e.target.value) || 0))}
                    className="w-full text-xs p-2 rounded-xl border border-slate-300 font-bold font-mono focus:ring-2 focus:ring-rose-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-600 block mb-1">Audit Reason / Description</label>
                <input
                  type="text"
                  value={adjustReason}
                  onChange={(e) => setAdjustReason(e.target.value)}
                  placeholder="e.g. Tanker refill received / Physical count reconciliation"
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-300 focus:ring-2 focus:ring-rose-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-600 block mb-1">Reference / Invoice Number</label>
                <input
                  type="text"
                  value={refNumber}
                  onChange={(e) => setRefNumber(e.target.value)}
                  placeholder="e.g. REC-DEPOT-2026-99"
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-300 focus:ring-2 focus:ring-rose-500 focus:outline-none font-mono"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                onClick={() => setEditModalItem(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
              >
                Cancel
              </button>
              <button
                disabled={isSubmitting}
                onClick={handleSaveStock}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-xs disabled:opacity-50"
              >
                {isSubmitting ? 'Recording...' : 'Commit Stock Audit'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
