import React from 'react';
import {
  Menu,
  Building2,
  ShieldCheck,
  ChevronDown,
  LogOut,
  Bell,
  Sparkles,
} from 'lucide-react';

interface DistributorTopBarProps {
  currentDistributorId: number;
  onDistributorChange: (id: number) => void;
  agencyName: string;
  agencyCode: string;
  onOpenMobileSidebar: () => void;
  onLogout?: () => void;
  onNavigateTab: (tab: any) => void;
  aiServiceOnline: boolean;
}

export const DistributorTopBar: React.FC<DistributorTopBarProps> = ({
  currentDistributorId,
  onDistributorChange,
  agencyName,
  agencyCode,
  onOpenMobileSidebar,
  onLogout,
  onNavigateTab,
  aiServiceOnline,
}) => {
  return (
    <header className="sticky top-0 z-30 h-16 bg-white/95 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 flex items-center justify-between gap-4">
      {/* Mobile Hamburger + Title */}
      <div className="flex items-center gap-3">
        <button
          onClick={onOpenMobileSidebar}
          className="p-2 -ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 lg:hidden"
          aria-label="Open sidebar"
        >
          <Menu size={20} />
        </button>

        {/* Agency Switcher / Identification */}
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-rose-100 text-rose-700 flex items-center justify-center font-bold text-xs">
            <Building2 size={16} />
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm text-slate-900 leading-tight">
                {agencyName || 'Authorized Distributor Depot'}
              </span>
              <span className="hidden sm:inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200/60 px-1.5 py-0.5 rounded-full">
                <ShieldCheck size={10} /> Active Member
              </span>
            </div>
            <div className="flex items-center gap-2 text-[11px] text-slate-400">
              <span>Code: {agencyCode || 'DIST-001'}</span>
              <span>•</span>
              {/* Distributor Selector dropdown */}
              <div className="relative inline-flex items-center">
                <select
                  value={currentDistributorId}
                  onChange={(e) => onDistributorChange(Number(e.target.value))}
                  className="bg-slate-50 border border-slate-200 text-slate-700 text-[11px] rounded px-1.5 py-0.5 font-medium appearance-none pr-5 hover:bg-slate-100 cursor-pointer focus:outline-none focus:ring-1 focus:ring-rose-500"
                >
                  <option value={1}>Agency #1 (Apex Gas)</option>
                  <option value={2}>Agency #2 (Pune Green Gas)</option>
                  <option value={3}>Agency #3 (City Gas Services)</option>
                </select>
                <ChevronDown size={10} className="absolute right-1 text-slate-400 pointer-events-none" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Actions */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* ML Status Indicator */}
        <button
          onClick={() => onNavigateTab('ai-forecast')}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold border transition-colors ${
            aiServiceOnline
              ? 'bg-indigo-50 text-indigo-700 border-indigo-200/70 hover:bg-indigo-100'
              : 'bg-amber-50 text-amber-700 border-amber-200/70 hover:bg-amber-100'
          }`}
          title={aiServiceOnline ? 'Python Forecasting API connected' : 'Forecasting service offline'}
        >
          <Sparkles size={12} className={aiServiceOnline ? 'text-indigo-600 animate-pulse' : 'text-amber-600'} />
          <span className="hidden md:inline">
            {aiServiceOnline ? 'AI Forecast Online' : 'AI Offline (Demo)'}
          </span>
        </button>

        {/* Quick notification bell */}
        <button
          onClick={() => onNavigateTab('booking-requests')}
          className="relative p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors"
          title="Booking Requests"
        >
          <Bell size={18} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500" />
        </button>

        {/* Logout button */}
        {onLogout && (
          <button
            onClick={onLogout}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:text-rose-600 hover:bg-rose-50 transition-colors border border-slate-200/80"
          >
            <LogOut size={14} />
            <span className="hidden sm:inline">Logout</span>
          </button>
        )}
      </div>
    </header>
  );
};
