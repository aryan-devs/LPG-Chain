import React from 'react';
import {
  LayoutDashboard,
  Users,
  Building2,
  CalendarDays,
  Layers,
  Truck,
  TrendingUp,
  Sparkles,
  Shield,
  FileText,
  Settings,
  Flame,
  ArrowLeft,
  X,
  LogOut,
  ShieldAlert,
  FileCheck,
  LifeBuoy,
} from 'lucide-react';
import { AdminPortalTab } from '../../types/adminPortalTypes';

interface AdminSidebarProps {
  activeTab: AdminPortalTab;
  onTabChange: (tab: AdminPortalTab) => void;
  onBackToHomepage?: () => void;
  onLogout?: () => void;
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
  pendingAlertsCount?: number;
}

export const AdminSidebar: React.FC<AdminSidebarProps> = ({
  activeTab,
  onTabChange,
  onBackToHomepage,
  onLogout,
  isMobileOpen = false,
  onCloseMobile,
  pendingAlertsCount = 3,
}) => {
  const navItems: {
    id: AdminPortalTab;
    label: string;
    icon: React.ReactNode;
    badge?: number;
    badgeColor?: string;
  }[] = [
    { id: 'overview', label: 'Overview', icon: <LayoutDashboard size={18} /> },
    { id: 'users', label: 'User Management', icon: <Users size={18} /> },
    { id: 'distributors', label: 'Distributor Management', icon: <Building2 size={18} /> },
    { id: 'bookings-deliveries', label: 'Bookings & Deliveries', icon: <CalendarDays size={18} /> },
    { id: 'inventory-allocations', label: 'Inventory & Allocations', icon: <Layers size={18} /> },
    { id: 'drivers-vehicles', label: 'Drivers & Vehicles', icon: <Truck size={18} /> },
    { id: 'reports-analytics', label: 'Reports & Analytics', icon: <TrendingUp size={18} /> },
    {
      id: 'ai-demand-forecast',
      label: 'AI Demand Forecast',
      icon: <Sparkles size={18} />,
      badgeColor: 'bg-indigo-600 text-white',
    },
    { id: 'roles-security', label: 'Roles & Security', icon: <Shield size={18} /> },
    {
      id: 'kyc-documents',
      label: 'KYC Document Review',
      icon: <FileCheck size={18} />,
      badgeColor: 'bg-emerald-600 text-white',
    },
    {
      id: 'complaints',
      label: 'Citizen Complaints',
      icon: <LifeBuoy size={18} />,
      badgeColor: 'bg-amber-600 text-white',
    },
    {
      id: 'audit-logs',
      label: 'Audit Logs',
      icon: <FileText size={18} />,
      badge: pendingAlertsCount,
      badgeColor: 'bg-rose-500 text-white',
    },
    { id: 'system-settings', label: 'System Settings', icon: <Settings size={18} /> },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isMobileOpen && (
        <div
          role="button"
          tabIndex={0}
          aria-label="Close admin mobile sidebar"
          onClick={onCloseMobile}
          onKeyDown={(e) => {
            if (e.key === 'Escape') onCloseMobile?.();
          }}
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-xs lg:hidden transition-opacity"
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={`fixed lg:static top-0 bottom-0 left-0 z-50 w-64 bg-white border-r border-slate-200/90 flex flex-col justify-between py-5 px-3.5 transition-transform duration-300 ease-in-out shrink-0 ${
          isMobileOpen ? 'translate-x-0 shadow-2xl' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="flex flex-col gap-5 overflow-y-auto pr-1">
          {/* Brand Header */}
          <div className="flex items-center justify-between px-2 pt-1">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-rose-600 to-rose-500 flex items-center justify-center text-white shadow-md shadow-rose-600/30 shrink-0">
                <Flame size={20} className="fill-white/20" />
              </div>
              <div className="flex flex-col">
                <span className="text-lg font-black tracking-tight text-slate-900 leading-none">
                  LPG-<span className="text-rose-600">Chain</span>
                </span>
                <span className="text-[10px] font-black text-rose-600 mt-1 uppercase tracking-wider flex items-center gap-1">
                  <ShieldAlert size={10} /> Admin Portal
                </span>
              </div>
            </div>

            {/* Mobile close button */}
            <button
              onClick={onCloseMobile}
              className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 lg:hidden"
            >
              <X size={18} />
            </button>
          </div>

          {/* Navigation Items (11 items) */}
          <nav className="flex flex-col gap-0.5">
            {navItems.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    onTabChange(item.id);
                    onCloseMobile?.();
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 ${
                    isActive
                      ? 'bg-rose-50 text-rose-700 shadow-xs border border-rose-100 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-3 truncate">
                    <span
                      className={`transition-colors shrink-0 ${
                        isActive ? 'text-rose-600' : 'text-slate-400'
                      }`}
                    >
                      {item.icon}
                    </span>
                    <span className="truncate">{item.label}</span>
                  </div>

                  {item.badge !== undefined && item.badge > 0 && (
                    <span
                      className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold shrink-0 ${
                        item.badgeColor || 'bg-slate-100 text-slate-700'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Footer Actions */}
        <div className="pt-4 border-t border-slate-100 flex flex-col gap-2 shrink-0">
          {onBackToHomepage && (
            <button
              onClick={onBackToHomepage}
              className="w-full flex items-center gap-2 px-3 py-2 text-xs font-medium text-slate-500 hover:text-slate-800 hover:bg-slate-50 rounded-lg transition-colors"
            >
              <ArrowLeft size={15} />
              <span>Back to Homepage</span>
            </button>
          )}

          {onLogout && (
            <button
              onClick={onLogout}
              className="w-full flex items-center gap-2 px-3 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
            >
              <LogOut size={15} />
              <span>Sign Out Admin</span>
            </button>
          )}

          <div className="px-3 py-2 rounded-xl bg-slate-50 border border-slate-100 flex items-center gap-2.5">
            <div className="w-2 h-2 rounded-full bg-slate-400" />
            <div className="flex flex-col">
              <span className="text-[11px] font-semibold text-slate-700">Governance Gateway</span>
              <span className="text-[9px] text-slate-400">ReBAC & OTP Supervised</span>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};
