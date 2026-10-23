import React, { useState, useEffect } from 'react';
import {
  Building2,
  ShieldCheck,
} from 'lucide-react';
import { DistributorApiService } from '../../../services/distributorApi';

interface DistributorProfileViewProps {
  distributorId: number;
}

export const DistributorProfileView: React.FC<DistributorProfileViewProps> = ({ distributorId }) => {
  const [profile, setProfile] = useState<any>(null);

  useEffect(() => {
    let isMounted = true;
    DistributorApiService.getAgencyProfile(distributorId)
      .then((data) => {
        if (isMounted) setProfile(data);
      });

    return () => {
      isMounted = false;
    };
  }, [distributorId]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="p-1 rounded-lg bg-rose-50 text-rose-600">
              <Building2 size={18} />
            </span>
            <span className="text-xs font-bold uppercase tracking-wider text-rose-600">
              Agency Master Profile
            </span>
          </div>
          <h1 className="text-xl font-black text-slate-900">
            {profile?.agencyName || 'Authorized Distributor Depot'}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Verified agency licensing credentials, service territory, and depot operating schedule.
          </p>
        </div>

        <span className="self-start sm:self-auto inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <ShieldCheck size={14} />
          <span>Licensed OMC Agency</span>
        </span>
      </div>

      {/* Profile Details Card */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-xs space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase">Agency Name</span>
              <p className="text-base font-bold text-slate-900">
                {profile?.agencyName || 'Apex Gas Distribution Agency'}
              </p>
            </div>

            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase">OMC License Number</span>
              <p className="text-sm font-mono font-bold text-rose-700">
                {profile?.licenceNumber || 'LIC-OMC-2026-9042'}
              </p>
            </div>

            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase">Supported Services</span>
              <div className="flex items-center gap-2 mt-1">
                <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-800 border border-slate-200">
                  {profile?.supportedServices || 'DOMESTIC & COMMERCIAL'}
                </span>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase">Depot Location</span>
              <p className="text-sm text-slate-800 font-medium mt-0.5">
                {profile?.address || 'Industrial Area Depot, Pune, Maharashtra'}
              </p>
              <span className="text-xs text-slate-500 font-mono">
                PIN: {profile?.pinCode || '411018'} | City: {profile?.city || 'Pune'}, {profile?.state || 'Maharashtra'}
              </span>
            </div>

            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase">Operating Hours</span>
              <p className="text-sm text-slate-800 font-medium mt-0.5">
                {profile?.operatingHours || 'Mon-Sat: 08:30 AM - 07:00 PM'}
              </p>
            </div>

            {profile?.latitude && profile?.longitude && (
              <div>
                <span className="text-[11px] font-bold text-slate-400 uppercase">Depot Coordinates</span>
                <p className="text-xs font-mono text-slate-600 mt-0.5">
                  Lat: {profile.latitude}, Lon: {profile.longitude}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
