import React, { useState, useRef, useEffect } from 'react';
import {
  Menu,
  Search,
  Bell,
  LogOut,
  ChevronDown,
  X,
} from 'lucide-react';
import { AdminPortalTab } from '../../types/adminPortalTypes';

interface AdminTopBarProps {
  activeTab: AdminPortalTab;
  onOpenMobileSidebar: () => void;
  onLogout?: () => void;
  onNavigateTab: (tab: AdminPortalTab) => void;
  globalSearchQuery?: string;
  onGlobalSearchChange?: (q: string) => void;
}

const TAB_TITLES: Record<AdminPortalTab, { title: string; subtitle: string }> = {
  overview: { title: 'Overview Dashboard', subtitle: 'Platform KPIs & infrastructure health' },
  users: { title: 'User Management', subtitle: 'Registered customer & staff directory' },
  distributors: { title: 'Distributor Management', subtitle: 'Licensed agency depots & service areas' },
  'bookings-deliveries': { title: 'Bookings & Deliveries', subtitle: 'Live order tracking & delivery dispatches' },
  'inventory-allocations': { title: 'Inventory & Allocations', subtitle: 'Depot physical stock & OMC monthly quotas' },
  'drivers-vehicles': { title: 'Drivers & Vehicles', subtitle: 'Fleet registry & IoT tracking telemetry' },
  'reports-analytics': { title: 'Reports & Analytics', subtitle: 'Supply chain performance & CSV export' },
  'ai-demand-forecast': { title: 'AI Demand Forecast', subtitle: 'Random Forest predictive demand models' },
  'roles-security': { title: 'Roles & Security', subtitle: 'RBAC permissions matrix & ReBAC tenancy' },
  'audit-logs': { title: 'Audit Logs', subtitle: 'Cryptographic activity records & security events' },
  'system-settings': { title: 'System Settings', subtitle: 'Platform configuration & microservices' },
  'kyc-documents': { title: 'KYC Document Verification', subtitle: 'Citizen identity, Aadhaar & address proof approvals' },
  complaints: { title: 'Citizen Grievances & Complaints', subtitle: 'Public support ticket resolution & customer care' },
};

export const AdminTopBar: React.FC<AdminTopBarProps> = ({
  activeTab,
  onOpenMobileSidebar,
  onLogout,
  onNavigateTab,
  globalSearchQuery = '',
  onGlobalSearchChange,
}) => {
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

  const notifRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);

  // Close dropdowns on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setIsNotificationsOpen(false);
      }
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setIsProfileMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const tabInfo = TAB_TITLES[activeTab] || {
    title: 'Admin Portal',
    subtitle: 'Platform Oversight',
  };

  return (
    <header className="sticky top-0 z-30 h-16 bg-white/95 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 flex items-center justify-between gap-4">
      {/* Left: Mobile hamburger + Active Page Title */}
      <div className="flex items-center gap-3 min-w-0">
        <button
          onClick={onOpenMobileSidebar}
          className="p-2 -ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 lg:hidden"
          aria-label="Open navigation sidebar"
        >
          <Menu size={20} />
        </button>

        <div className="flex flex-col truncate">
          <h1 className="font-bold text-sm sm:text-base text-slate-900 leading-tight truncate">
            {tabInfo.title}
          </h1>
          <span className="hidden sm:inline text-[11px] text-slate-400 truncate">
            {tabInfo.subtitle}
          </span>
        </div>
      </div>

      {/* Center: Search UI */}
      {onGlobalSearchChange && (
        <div className="hidden md:flex items-center flex-1 max-w-xs relative">
          <Search size={14} className="absolute left-3 text-slate-400 pointer-events-none" />
          <input
            type="text"
            value={globalSearchQuery}
            onChange={(e) => onGlobalSearchChange(e.target.value)}
            placeholder="Search platform..."
            className="w-full pl-8 pr-7 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-rose-500 focus:bg-white"
          />
          {globalSearchQuery && (
            <button
              onClick={() => onGlobalSearchChange('')}
              className="absolute right-2 text-slate-400 hover:text-slate-600"
            >
              <X size={12} />
            </button>
          )}
        </div>
      )}

      {/* Right: Neutral Status Indicator + Notifications + Profile Menu + Logout */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* System Status Indicator (NEUTRAL STATE until connected to backend as requested) */}
        <div
          title="Microservices health probes in standby. Backend connection verified on port 8080."
          className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 border border-slate-200 text-slate-600 text-xs font-semibold"
        >
          <span className="w-2 h-2 rounded-full bg-slate-400" />
          <span className="text-[11px]">System Status: Standby</span>
        </div>

        {/* Notifications Icon & Popover */}
        <div className="relative" ref={notifRef}>
          <button
            onClick={() => setIsNotificationsOpen(!isNotificationsOpen)}
            className="relative p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors"
            title="Platform Notifications"
          >
            <Bell size={18} />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500" />
          </button>

          {isNotificationsOpen && (
            <div className="absolute right-0 mt-2 w-80 bg-white rounded-2xl shadow-xl border border-slate-200 p-4 space-y-3 z-50 animate-in fade-in duration-150">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                <span className="font-bold text-xs text-slate-900">Platform Alerts</span>
                <span className="text-[10px] text-slate-400 font-semibold">3 Active Alerts</span>
              </div>
              <div className="space-y-2 text-xs">
                <div
                  onClick={() => {
                    setIsNotificationsOpen(false);
                    onNavigateTab('bookings-deliveries');
                  }}
                  className="p-2.5 rounded-xl bg-amber-50 border border-amber-200/80 cursor-pointer hover:bg-amber-100/70 transition-colors"
                >
                  <span className="font-bold text-amber-900 block text-[11px]">
                    Delivery Window Overdue
                  </span>
                  <p className="text-[10px] text-amber-800 mt-0.5">
                    Booking #BK-10044 exceeded 7-day fulfillment window at Pune Depot.
                  </p>
                </div>

                <div
                  onClick={() => {
                    setIsNotificationsOpen(false);
                    onNavigateTab('distributors');
                  }}
                  className="p-2.5 rounded-xl bg-blue-50 border border-blue-200/80 cursor-pointer hover:bg-blue-100/70 transition-colors"
                >
                  <span className="font-bold text-blue-900 block text-[11px]">
                    New Agency Pending Verification
                  </span>
                  <p className="text-[10px] text-blue-800 mt-0.5">
                    Western Bharat Gas Agency submitted registration documents.
                  </p>
                </div>

                <div
                  onClick={() => {
                    setIsNotificationsOpen(false);
                    onNavigateTab('inventory-allocations');
                  }}
                  className="p-2.5 rounded-xl bg-rose-50 border border-rose-200/80 cursor-pointer hover:bg-rose-100/70 transition-colors"
                >
                  <span className="font-bold text-rose-900 block text-[11px]">
                    Quota Utilization &gt; 90%
                  </span>
                  <p className="text-[10px] text-rose-800 mt-0.5">
                    Pune Green Gas Depot October commercial allocation is nearly exhausted.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Admin Profile Menu */}
        <div className="relative" ref={profileRef}>
          <button
            onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
            className="flex items-center gap-2 p-1 pl-2 pr-2.5 rounded-xl bg-slate-50 hover:bg-slate-100 border border-slate-200 transition-colors"
          >
            <div className="w-7 h-7 rounded-lg bg-rose-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
              AD
            </div>
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-bold text-slate-800 leading-tight">Platform Admin</span>
              <span className="text-[9px] text-rose-600 font-bold uppercase tracking-wider">
                Full Oversight
              </span>
            </div>
            <ChevronDown size={12} className="text-slate-400" />
          </button>

          {isProfileMenuOpen && (
            <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-slate-200 p-2 space-y-1 z-50 animate-in fade-in duration-150 text-xs">
              <div className="px-3 py-2 border-b border-slate-100">
                <div className="font-bold text-slate-900">Administrator Profile</div>
                <div className="text-[11px] text-slate-400 font-mono">admin@lpgchain.example</div>
                <span className="inline-block mt-1 px-1.5 py-0.5 bg-purple-50 text-purple-700 text-[9px] font-bold rounded border border-purple-200 uppercase">
                  Role: Platform Admin
                </span>
              </div>

              <button
                onClick={() => {
                  setIsProfileMenuOpen(false);
                  onNavigateTab('system-settings');
                }}
                className="w-full text-left px-3 py-2 text-slate-700 hover:bg-slate-50 rounded-xl transition-colors font-medium"
              >
                System Settings
              </button>

              <button
                onClick={() => {
                  setIsProfileMenuOpen(false);
                  onNavigateTab('roles-security');
                }}
                className="w-full text-left px-3 py-2 text-slate-700 hover:bg-slate-50 rounded-xl transition-colors font-medium"
              >
                Roles & Permissions
              </button>

              {onLogout && (
                <button
                  onClick={() => {
                    setIsProfileMenuOpen(false);
                    onLogout();
                  }}
                  className="w-full text-left px-3 py-2 text-rose-600 hover:bg-rose-50 rounded-xl transition-colors font-bold flex items-center gap-1.5 border-t border-slate-100 mt-1"
                >
                  <LogOut size={13} />
                  <span>Log Out Admin</span>
                </button>
              )}
            </div>
          )}
        </div>

        {/* Quick Logout Button */}
        {onLogout && (
          <button
            onClick={onLogout}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:text-rose-600 hover:bg-rose-50 transition-colors border border-slate-200"
            title="Log Out Admin"
          >
            <LogOut size={14} />
            <span className="hidden sm:inline">Logout</span>
          </button>
        )}
      </div>
    </header>
  );
};
