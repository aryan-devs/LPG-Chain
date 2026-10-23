import React from 'react';
import {
  LayoutDashboard,
  Inbox,
  Clock,
  Package,
  CalendarDays,
  Truck,
  BarChart3,
  Sparkles,
  FileText,
  Building2,
  Flame,
  ArrowLeft,
  X,
  BadgeCheck,
} from 'lucide-react';
import { DistributorPortalTab } from '../../types/distributorPortalTypes';

interface DistributorSidebarProps {
  activeTab: DistributorPortalTab;
  onTabChange: (tab: DistributorPortalTab) => void;
  onBackToHomepage?: () => void;
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
  pendingRequestsCount?: number;
  queuedOrdersCount?: number;
}

export const DistributorSidebar: React.FC<DistributorSidebarProps> = ({
  activeTab,
  onTabChange,
  onBackToHomepage,
  isMobileOpen = false,
  onCloseMobile,
  pendingRequestsCount = 0,
  queuedOrdersCount = 0,
}) => {
  const navItems: {
    id: DistributorPortalTab;
    label: string;
    icon: React.ReactNode;
    badge?: number;
    badgeColor?: string;
  }[] = [
    { id: 'overview', label: 'Overview', icon: <LayoutDashboard size={18} /> },
    {
      id: 'booking-requests',
      label: 'Booking Requests',
      icon: <Inbox size={18} />,
      badge: pendingRequestsCount,
      badgeColor: 'bg-rose-500 text-white',
    },
    {
      id: 'orders-queue',
      label: 'Orders & Queue',
      icon: <Clock size={18} />,
      badge: queuedOrdersCount,
      badgeColor: 'bg-amber-500 text-white',
    },
    { id: 'inventory', label: 'Cylinder Inventory', icon: <Package size={18} /> },
    { id: 'delivery-schedule', label: 'Delivery Schedule', icon: <CalendarDays size={18} /> },
    { id: 'live-deliveries', label: 'Live Deliveries', icon: <Truck size={18} /> },
    { id: 'transparency', label: 'Allocation & Sales', icon: <BarChart3 size={18} /> },
    {
      id: 'ai-forecast',
      label: 'AI Demand Forecast',
      icon: <Sparkles size={18} />,
      badgeColor: 'bg-indigo-600 text-white',
    },
    { id: 'reports', label: 'Reports', icon: <FileText size={18} /> },
    { id: 'agency-profile', label: 'Agency Profile', icon: <Building2 size={18} /> },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isMobileOpen && (
        <div
          role="button"
          tabIndex={0}
          aria-label="Close mobile sidebar"
          onClick={onCloseMobile}
          onKeyDown={(e) => {
            if (e.key === 'Escape') onCloseMobile?.();
          }}
          className="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm lg:hidden transition-opacity"
        />
      )}

      <aside
        className={`fixed lg:static top-0 bottom-0 left-0 z-50 w-64 bg-white border-r border-slate-200/90 flex flex-col justify-between py-5 px-4 transition-transform duration-300 ease-in-out ${
          isMobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="flex flex-col gap-6">
          {/* Brand Header */}
          <div className="flex items-center justify-between px-2">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-rose-600 to-rose-500 flex items-center justify-center text-white shadow-md shadow-rose-600/30">
                <Flame size={20} className="fill-white/20" />
              </div>
              <div className="flex flex-col">
                <span className="text-lg font-black tracking-tight text-slate-900 leading-none">
                  LPG-<span className="text-rose-600">Chain</span>
                </span>
                <span className="text-[10px] font-semibold text-rose-600 mt-1 uppercase tracking-wider flex items-center gap-1">
                  <BadgeCheck size={11} /> Distributor Portal
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

          {/* Navigation Links */}
          <nav className="flex flex-col gap-1">
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
                      ? 'bg-rose-50 text-rose-700 shadow-sm border border-rose-100/80 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={`transition-colors ${
                        isActive ? 'text-rose-600' : 'text-slate-400'
                      }`}
                    >
                      {item.icon}
                    </span>
                    <span>{item.label}</span>
                  </div>

                  {item.badge !== undefined && item.badge > 0 && (
                    <span
                      className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
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
        <div className="pt-4 border-t border-slate-100 flex flex-col gap-2">
          {onBackToHomepage && (
            <button
              onClick={onBackToHomepage}
              className="w-full flex items-center gap-2 px-3 py-2 text-xs font-medium text-slate-500 hover:text-slate-800 hover:bg-slate-50 rounded-lg transition-colors"
            >
              <ArrowLeft size={15} />
              <span>Back to Homepage</span>
            </button>
          )}

          <div className="px-3 py-2 rounded-xl bg-slate-50 border border-slate-100 flex items-center gap-2.5">
            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <div className="flex flex-col">
              <span className="text-[11px] font-semibold text-slate-700">Depot System Online</span>
              <span className="text-[9px] text-slate-400">ReBAC & OTP Secured</span>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};
