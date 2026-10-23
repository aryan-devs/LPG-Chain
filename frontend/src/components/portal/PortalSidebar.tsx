import React from 'react';
import {
  Home,
  ShoppingCart,
  Truck,
  Sparkles,
  BarChart3,
  ShieldCheck,
  User,
  MapPin,
  CreditCard,
  HelpCircle,
  Headphones,
  Flame,
  ArrowLeft,
  X,
} from 'lucide-react';
import { PortalTab } from './portalTypes';

interface PortalSidebarProps {
  activeTab: PortalTab;
  onTabChange: (tab: PortalTab) => void;
  onBackToHomepage?: () => void;
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
}

export const PortalSidebar: React.FC<PortalSidebarProps> = ({
  activeTab,
  onTabChange,
  onBackToHomepage,
  isMobileOpen = false,
  onCloseMobile,
}) => {
  const navItems: { id: PortalTab; label: string; icon: React.ReactNode }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <Home size={18} /> },
    { id: 'book', label: 'Book a Cylinder', icon: <ShoppingCart size={18} /> },
    { id: 'track', label: 'Track Delivery', icon: <Truck size={18} /> },
    { id: 'transparency', label: 'Quota Transparency', icon: <BarChart3 size={18} /> },
    { id: 'ai', label: 'AI Assistance', icon: <Sparkles size={18} /> },
    { id: 'kyc', label: 'KYC & Documents', icon: <ShieldCheck size={18} /> },
    { id: 'profile', label: 'My Profile', icon: <User size={18} /> },
    { id: 'addresses', label: 'Saved Addresses', icon: <MapPin size={18} /> },
    { id: 'billing', label: 'Billing & Payments', icon: <CreditCard size={18} /> },
    { id: 'support', label: 'Help & Support', icon: <HelpCircle size={18} /> },
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
                <span className="text-[10px] font-semibold text-slate-400 mt-1 tracking-tight">
                  Clean Energy. Connected.
                </span>
              </div>
            </div>

            {/* Close button for mobile */}
            <button
              type="button"
              onClick={onCloseMobile}
              className="lg:hidden p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              aria-label="Close menu"
            >
              <X size={20} />
            </button>
          </div>

          {/* Navigation Links */}
          <nav className="flex flex-col gap-1.5" aria-label="Portal Navigation">
            {navItems.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    onTabChange(item.id);
                    onCloseMobile?.();
                  }}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all duration-150 text-left ${
                    isActive
                      ? 'bg-rose-600 text-white shadow-md shadow-rose-600/25 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80 font-medium'
                  }`}
                >
                  <span
                    className={`transition-colors ${
                      isActive ? 'text-white' : 'text-slate-500 group-hover:text-slate-700'
                    }`}
                  >
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Bottom Support Card & Back Button */}
        <div className="flex flex-col gap-3 pt-4 border-t border-slate-100">
          <div className="p-3.5 rounded-2xl bg-rose-50/50 border border-rose-100/80 flex flex-col gap-2.5">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-full bg-rose-100 flex items-center justify-center text-rose-600 flex-shrink-0">
                <Headphones size={16} />
              </div>
              <div className="flex flex-col">
                <span className="text-xs font-bold text-slate-900">Need Help?</span>
                <span className="text-[11px] text-slate-500 leading-tight">
                  Our support team is here for you 24/7.
                </span>
              </div>
            </div>
            <button
              type="button"
              onClick={() => {
                onTabChange('support');
                onCloseMobile?.();
              }}
              className="w-full py-1.5 px-3 rounded-xl border border-rose-600 text-rose-600 hover:bg-rose-600 hover:text-white text-xs font-bold transition-all text-center"
            >
              Contact Support
            </button>
          </div>

          {onBackToHomepage && (
            <button
              type="button"
              onClick={onBackToHomepage}
              className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl text-xs font-semibold text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
            >
              <ArrowLeft size={14} />
              <span>Back to Homepage</span>
            </button>
          )}
        </div>
      </aside>
    </>
  );
};
