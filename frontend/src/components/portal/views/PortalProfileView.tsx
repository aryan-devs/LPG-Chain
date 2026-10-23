import React, { useState } from 'react';
import {
  Phone,
  Mail,
  MapPin,
  Building,
  Edit2,
  Save,
  Check,
} from 'lucide-react';
import { CustomerProfileData } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';

interface PortalProfileViewProps {
  profile: CustomerProfileData;
  onProfileUpdated: (updated: CustomerProfileData) => void;
}

export const PortalProfileView: React.FC<PortalProfileViewProps> = ({
  profile,
  onProfileUpdated,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState(profile.name);
  const [mobile, setMobile] = useState(profile.mobile);
  const [email, setEmail] = useState(profile.email);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    const updated = await CustomerService.updateProfile({
      name,
      mobile,
      email,
    });
    onProfileUpdated(updated);
    setIsEditing(false);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2500);
  };

  return (
    <div className="max-w-4xl mx-auto flex flex-col gap-6 pb-12">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            My Customer Profile
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage your personal contact details, connection IDs, and assigned distributor agency.
          </p>
        </div>
        {!isEditing && (
          <button
            type="button"
            onClick={() => setIsEditing(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-white border border-slate-300 hover:border-slate-400 text-xs font-bold text-slate-700 shadow-sm transition-colors"
          >
            <Edit2 size={14} />
            <span>Edit Profile</span>
          </button>
        )}
      </div>

      {savedSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center gap-2">
          <Check size={16} />
          <span>Profile details updated successfully!</span>
        </div>
      )}

      {/* Main Profile Card */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 flex flex-col gap-6">
        {/* Avatar & Top Info */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full bg-slate-800 text-white font-black text-2xl flex items-center justify-center shadow-md">
              {profile.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-black text-slate-900">{profile.name}</h2>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  Verified Consumer
                </span>
              </div>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                Consumer No: {profile.consumerNumber}
              </p>
            </div>
          </div>

          <div className="text-left sm:text-right bg-slate-50 px-4 py-2.5 rounded-2xl border border-slate-200/60">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              17-Digit LPG ID
            </span>
            <span className="text-sm font-mono font-bold text-slate-900">
              {profile.lpgId}
            </span>
          </div>
        </div>

        {/* Form / Details Grid */}
        {isEditing ? (
          <form onSubmit={handleSave} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">Full Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                required
              />
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">
                Mobile Number
              </label>
              <input
                type="text"
                value={mobile}
                onChange={(e) => setMobile(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                required
              />
            </div>

            <div className="sm:col-span-2">
              <label className="text-xs font-bold text-slate-700 block mb-1">
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                required
              />
            </div>

            <div className="sm:col-span-2 flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-50 text-xs font-bold text-slate-700 transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow-md shadow-rose-600/25 transition-all flex items-center gap-1.5"
              >
                <Save size={14} />
                <span>Save Changes</span>
              </button>
            </div>
          </form>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center flex-shrink-0">
                <Phone size={16} />
              </div>
              <div>
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Registered Mobile
                </span>
                <span className="text-sm font-bold text-slate-900">{profile.mobile}</span>
                <span className="text-[10px] text-emerald-600 font-semibold block">
                  SMS / OTP Alerts Active
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center flex-shrink-0">
                <Mail size={16} />
              </div>
              <div>
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Email Address
                </span>
                <span className="text-sm font-bold text-slate-900">{profile.email}</span>
                <span className="text-[10px] text-emerald-600 font-semibold block">
                  Digital Invoices Sent Here
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3 sm:col-span-2">
              <div className="w-9 h-9 rounded-xl bg-slate-100 text-slate-600 flex items-center justify-center flex-shrink-0">
                <MapPin size={16} />
              </div>
              <div>
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Default Household Address
                </span>
                <span className="text-sm font-medium text-slate-900">{profile.address}</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Distributor Agency Card */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col gap-4">
        <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
          <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <Building size={18} />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Assigned LPG Distributor Agency
            </h3>
            <p className="text-xs text-slate-500">
              Your official cylinder allocation center and refill dispatch hub.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-1">
          <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Agency Name
            </span>
            <span className="text-xs font-bold text-slate-900 mt-1 block">
              {profile.distributorName}
            </span>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Agency Code
            </span>
            <span className="text-xs font-mono font-bold text-slate-900 mt-1 block">
              {profile.distributorCode}
            </span>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
              Distributor Desk
            </span>
            <a
              href={`tel:${profile.distributorPhone}`}
              className="text-xs font-bold text-rose-600 hover:underline mt-1 block"
            >
              {profile.distributorPhone}
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
