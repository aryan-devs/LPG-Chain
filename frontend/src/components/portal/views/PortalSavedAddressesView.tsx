import React, { useState } from 'react';
import {
  Plus,
  Trash2,
  CheckCircle2,
  Home,
  Building,
  Users,
  X,
} from 'lucide-react';
import { SavedAddress } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';

interface PortalSavedAddressesViewProps {
  addresses: SavedAddress[];
  onAddressesUpdated: (updated: SavedAddress[]) => void;
}

export const PortalSavedAddressesView: React.FC<PortalSavedAddressesViewProps> = ({
  addresses,
  onAddressesUpdated,
}) => {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [label, setLabel] = useState<'Home' | 'Office' | 'Parents' | 'Other'>('Home');
  const [fullAddress, setFullAddress] = useState('');
  const [landmark, setLandmark] = useState('');
  const city = 'Pune';
  const state = 'Maharashtra';
  const [pincode, setPincode] = useState('411018');
  const [isDefault, setIsDefault] = useState(false);

  const handleAddAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullAddress.trim()) return;

    await CustomerService.addAddress({
      label,
      fullAddress: fullAddress.trim(),
      landmark: landmark.trim(),
      city,
      state,
      pincode,
      isDefault,
    });

    const refreshed = await CustomerService.getAddresses();
    onAddressesUpdated(refreshed);
    setIsAddOpen(false);
    setFullAddress('');
    setLandmark('');
  };

  const handleDelete = async (id: string) => {
    await CustomerService.deleteAddress(id);
    const refreshed = await CustomerService.getAddresses();
    onAddressesUpdated(refreshed);
  };

  const handleSetDefault = async (id: string) => {
    await CustomerService.setDefaultAddress(id);
    const refreshed = await CustomerService.getAddresses();
    onAddressesUpdated(refreshed);
  };

  return (
    <div className="max-w-4xl mx-auto flex flex-col gap-6 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Saved Delivery Addresses
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage physical addresses where your LPG cylinders are delivered.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsAddOpen(true)}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-md shadow-rose-600/25 transition-all self-start sm:self-auto"
        >
          <Plus size={16} />
          <span>Add New Address</span>
        </button>
      </div>

      {/* Address Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {addresses.map((addr) => (
          <div
            key={addr.id}
            className={`bg-white rounded-3xl border p-6 flex flex-col justify-between transition-all ${
              addr.isDefault
                ? 'border-rose-300 ring-2 ring-rose-500/10 shadow-md'
                : 'border-slate-200/90 shadow-sm hover:shadow'
            }`}
          >
            <div>
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
                    {addr.label === 'Home' ? (
                      <Home size={16} />
                    ) : addr.label === 'Office' ? (
                      <Building size={16} />
                    ) : (
                      <Users size={16} />
                    )}
                  </div>
                  <span className="text-sm font-bold text-slate-900">{addr.label}</span>
                </div>

                {addr.isDefault ? (
                  <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold flex items-center gap-1">
                    <CheckCircle2 size={12} />
                    Default
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={() => handleSetDefault(addr.id)}
                    className="text-xs text-slate-500 hover:text-rose-600 font-semibold"
                  >
                    Set as Default
                  </button>
                )}
              </div>

              <div className="pt-4 flex flex-col gap-1">
                <p className="text-xs font-semibold text-slate-800 leading-relaxed">
                  {addr.fullAddress}
                </p>
                {addr.landmark && (
                  <p className="text-xs text-slate-500">
                    <span className="font-bold">Landmark:</span> {addr.landmark}
                  </p>
                )}
                <p className="text-xs text-slate-500">
                  {addr.city}, {addr.state} - <span className="font-mono">{addr.pincode}</span>
                </p>
              </div>
            </div>

            <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between">
              <span className="text-[11px] text-slate-400 font-medium">
                Pimpri-Chinchwad Ward #14
              </span>
              {!addr.isDefault && (
                <button
                  type="button"
                  onClick={() => handleDelete(addr.id)}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                  aria-label="Delete address"
                >
                  <Trash2 size={16} />
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Add Address Modal */}
      {isAddOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in">
          <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-slate-200 p-6 sm:p-8">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <h2 className="text-base font-bold text-slate-900">Add Household Address</h2>
              <button
                type="button"
                onClick={() => setIsAddOpen(false)}
                className="p-1.5 rounded-full text-slate-400 hover:text-slate-600"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleAddAddress} className="space-y-4 pt-4">
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Address Label
                </label>
                <div className="grid grid-cols-4 gap-2">
                  {(['Home', 'Parents', 'Office', 'Other'] as const).map((l) => (
                    <button
                      key={l}
                      type="button"
                      onClick={() => setLabel(l)}
                      className={`py-2 rounded-xl border text-xs font-bold transition-colors ${
                        label === l
                          ? 'border-rose-600 bg-rose-50 text-rose-700'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      {l}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  Flat No., Building Name &amp; Street
                </label>
                <textarea
                  rows={2}
                  value={fullAddress}
                  onChange={(e) => setFullAddress(e.target.value)}
                  placeholder="e.g. Flat 402, Royal Palms, Sector 14"
                  className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">
                    Landmark (Optional)
                  </label>
                  <input
                    type="text"
                    value={landmark}
                    onChange={(e) => setLandmark(e.target.value)}
                    placeholder="Near City Mall"
                    className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">Pincode</label>
                  <input
                    type="text"
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    placeholder="411018"
                    className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                    required
                  />
                </div>
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="defaultAddress"
                  checked={isDefault}
                  onChange={(e) => setIsDefault(e.target.checked)}
                  className="w-4 h-4 text-rose-600 rounded border-slate-300 focus:ring-rose-500"
                />
                <label htmlFor="defaultAddress" className="text-xs text-slate-700 font-medium">
                  Set as default delivery address for future refills
                </label>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsAddOpen(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-bold text-slate-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-md shadow-rose-600/25 transition-all"
                >
                  Save Address
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
