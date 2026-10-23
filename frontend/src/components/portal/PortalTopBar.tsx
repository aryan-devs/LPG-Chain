import React, { useState, useRef, useEffect } from 'react';
import {
  Home,
  Bell,
  ChevronDown,
  Menu,
  Check,
  User,
  LogOut,
  ShieldCheck,
  CreditCard,
  MapPin,
} from 'lucide-react';
import { CustomerProfileData, NotificationItem, PortalTab } from './portalTypes';

interface PortalTopBarProps {
  profile: CustomerProfileData;
  notifications: NotificationItem[];
  onMarkNotificationsRead: () => void;
  onNavigateTab: (tab: PortalTab) => void;
  onOpenMobileSidebar: () => void;
  onLogout?: () => void;
}

export const PortalTopBar: React.FC<PortalTopBarProps> = ({
  profile,
  notifications,
  onMarkNotificationsRead,
  onNavigateTab,
  onOpenMobileSidebar,
  onLogout,
}) => {
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState('Household Account (Primary)');

  const accountRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);

  const unreadCount = notifications.filter((n) => !n.read).length;

  // Close popovers on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (accountRef.current && !accountRef.current.contains(e.target as Node)) {
        setIsAccountMenuOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setIsNotifOpen(false);
      }
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setIsProfileMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const accounts = [
    { id: '1', name: 'Household Account (Primary)', address: 'Pimpri, Pune • 14.2 KG Domestic' },
    { id: '2', name: 'Baner Residence (Secondary)', address: 'Baner Road, Pune • 14.2 KG Domestic' },
  ];

  const initialLetter = profile.name ? profile.name.trim().charAt(0).toUpperCase() : 'S';

  return (
    <header className="h-16 border-b border-slate-200/90 bg-white/95 backdrop-blur-md sticky top-0 z-30 px-4 sm:px-6 flex items-center justify-between">
      {/* Left: Mobile Toggle & Account Selector */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onOpenMobileSidebar}
          className="lg:hidden p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
          aria-label="Open navigation menu"
        >
          <Menu size={20} />
        </button>

        {/* Household Account Selector */}
        <div className="relative" ref={accountRef}>
          <button
            type="button"
            onClick={() => setIsAccountMenuOpen(!isAccountMenuOpen)}
            className="flex items-center gap-2 px-3 py-1.5 rounded-full hover:bg-slate-100 text-slate-800 text-sm font-semibold transition-colors group"
          >
            <div className="w-5 h-5 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center">
              <Home size={13} className="text-rose-600" />
            </div>
            <span>{selectedAccount.split(' ')[0]} Account</span>
            <ChevronDown
              size={15}
              className={`text-slate-400 group-hover:text-slate-600 transition-transform ${
                isAccountMenuOpen ? 'rotate-180' : ''
              }`}
            />
          </button>

          {isAccountMenuOpen && (
            <div className="absolute left-0 mt-2 w-72 bg-white rounded-2xl shadow-xl border border-slate-200 py-2 z-50 animate-in fade-in slide-in-from-top-2">
              <div className="px-4 py-2 border-b border-slate-100">
                <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Linked Household Accounts
                </p>
              </div>
              {accounts.map((acc) => (
                <button
                  key={acc.id}
                  type="button"
                  onClick={() => {
                    setSelectedAccount(acc.name);
                    setIsAccountMenuOpen(false);
                  }}
                  className="w-full px-4 py-2.5 flex items-start gap-3 hover:bg-slate-50 text-left transition-colors"
                >
                  <div
                    className={`w-4 h-4 rounded-full mt-0.5 border flex items-center justify-center ${
                      selectedAccount === acc.name
                        ? 'border-rose-600 bg-rose-600 text-white'
                        : 'border-slate-300'
                    }`}
                  >
                    {selectedAccount === acc.name && <Check size={10} />}
                  </div>
                  <div>
                    <p className="text-xs font-bold text-slate-800">{acc.name}</p>
                    <p className="text-[11px] text-slate-500">{acc.address}</p>
                  </div>
                </button>
              ))}
              <div className="px-3 pt-2 mt-1 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => {
                    setIsAccountMenuOpen(false);
                    onNavigateTab('addresses');
                  }}
                  className="w-full text-center py-1.5 text-xs font-bold text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                >
                  + Add New Consumer Connection
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Right: Notifications & Profile Pill */}
      <div className="flex items-center gap-3 sm:gap-4">
        {/* Notification Bell with Badge */}
        <div className="relative" ref={notifRef}>
          <button
            type="button"
            onClick={() => setIsNotifOpen(!isNotifOpen)}
            className="w-9 h-9 rounded-full flex items-center justify-center text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors relative"
            aria-label="View notifications"
          >
            <Bell size={19} />
            {unreadCount > 0 && (
              <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-rose-600 text-white text-[10px] font-black flex items-center justify-center shadow-sm">
                {unreadCount}
              </span>
            )}
          </button>

          {isNotifOpen && (
            <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2">
              <div className="px-4 py-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-900">Notifications</span>
                  {unreadCount > 0 && (
                    <span className="px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 text-[10px] font-bold">
                      {unreadCount} New
                    </span>
                  )}
                </div>
                {unreadCount > 0 && (
                  <button
                    type="button"
                    onClick={onMarkNotificationsRead}
                    className="text-[11px] font-semibold text-rose-600 hover:underline"
                  >
                    Mark all read
                  </button>
                )}
              </div>
              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
                {notifications.map((n) => (
                  <div
                    key={n.id}
                    className={`p-3.5 hover:bg-slate-50 transition-colors flex gap-3 ${
                      !n.read ? 'bg-rose-50/20' : ''
                    }`}
                  >
                    <div className="w-2 h-2 rounded-full bg-rose-600 mt-1.5 flex-shrink-0" />
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <p className="text-xs font-bold text-slate-800">{n.title}</p>
                        <span className="text-[10px] text-slate-400">{n.timestamp}</span>
                      </div>
                      <p className="text-xs text-slate-600 mt-1 leading-relaxed">{n.message}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="p-2.5 bg-slate-50 border-t border-slate-100 text-center">
                <button
                  type="button"
                  onClick={() => {
                    setIsNotifOpen(false);
                    onNavigateTab('track');
                  }}
                  className="text-xs font-bold text-slate-600 hover:text-slate-900"
                >
                  View Active Deliveries &rarr;
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Customer Profile Pill */}
        <div className="relative" ref={profileRef}>
          <button
            type="button"
            onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
            className="flex items-center gap-2.5 pl-1.5 pr-2.5 py-1 rounded-full hover:bg-slate-100 text-left transition-colors group"
          >
            <div className="w-8 h-8 rounded-full bg-slate-700 text-white font-bold text-xs flex items-center justify-center shadow-sm">
              {initialLetter}
            </div>
            <div className="hidden sm:flex flex-col">
              <span className="text-xs font-bold text-slate-800 leading-tight flex items-center gap-1">
                {profile.name}
                <ChevronDown
                  size={13}
                  className={`text-slate-400 group-hover:text-slate-600 transition-transform ${
                    isProfileMenuOpen ? 'rotate-180' : ''
                  }`}
                />
              </span>
              <span className="text-[10px] text-slate-400 leading-none mt-0.5">
                Household Customer
              </span>
            </div>
          </button>

          {isProfileMenuOpen && (
            <div className="absolute right-0 mt-2 w-56 bg-white rounded-2xl shadow-xl border border-slate-200 py-1.5 z-50 animate-in fade-in slide-in-from-top-2">
              <div className="px-4 py-2.5 border-b border-slate-100">
                <p className="text-xs font-bold text-slate-900">{profile.name}</p>
                <p className="text-[11px] text-slate-400 font-mono">{profile.consumerNumber}</p>
              </div>
              <div className="py-1">
                <button
                  type="button"
                  onClick={() => {
                    setIsProfileMenuOpen(false);
                    onNavigateTab('profile');
                  }}
                  className="w-full px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 flex items-center gap-2.5"
                >
                  <User size={14} className="text-slate-400" />
                  <span>My Profile</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsProfileMenuOpen(false);
                    onNavigateTab('addresses');
                  }}
                  className="w-full px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 flex items-center gap-2.5"
                >
                  <MapPin size={14} className="text-slate-400" />
                  <span>Saved Addresses</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsProfileMenuOpen(false);
                    onNavigateTab('billing');
                  }}
                  className="w-full px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 flex items-center gap-2.5"
                >
                  <CreditCard size={14} className="text-slate-400" />
                  <span>Billing & Receipts</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setIsProfileMenuOpen(false);
                    onNavigateTab('kyc');
                  }}
                  className="w-full px-4 py-2 text-xs text-slate-700 hover:bg-slate-50 flex items-center gap-2.5"
                >
                  <ShieldCheck size={14} className="text-slate-400" />
                  <span>KYC Status</span>
                </button>
              </div>
              <div className="pt-1 border-t border-slate-100">
                {onLogout && (
                  <button
                    type="button"
                    onClick={() => {
                      setIsProfileMenuOpen(false);
                      onLogout();
                    }}
                    className="w-full px-4 py-2 text-xs text-rose-600 hover:bg-rose-50 flex items-center gap-2.5 font-bold"
                  >
                    <LogOut size={14} />
                    <span>Sign Out</span>
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
