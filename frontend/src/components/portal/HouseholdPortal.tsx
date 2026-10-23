import React, { useState, useEffect } from 'react';
import { PortalSidebar } from './PortalSidebar';
import { PortalTopBar } from './PortalTopBar';
import { PortalDashboardView } from './views/PortalDashboardView';
import { PortalBookCylinderView } from './views/PortalBookCylinderView';
import { PortalTrackDeliveryView } from './views/PortalTrackDeliveryView';
import { PortalAiAssistanceView } from './views/PortalAiAssistanceView';
import { PortalKycDocumentsView } from './views/PortalKycDocumentsView';
import { PortalProfileView } from './views/PortalProfileView';
import { PortalSavedAddressesView } from './views/PortalSavedAddressesView';
import { PortalBillingPaymentsView } from './views/PortalBillingPaymentsView';
import { PortalBookingHistoryView } from './views/PortalBookingHistoryView';
import { PortalHelpSupportView } from './views/PortalHelpSupportView';
import { PortalTransparencyView } from './views/PortalTransparencyView';
import {
  PortalTab,
  CustomerOrder,
  CustomerProfileData,
  SavedAddress,
  KycDocument,
  BillingInvoice,
  NotificationItem,
  SupportTicket,
} from './portalTypes';
import { CustomerService } from '../../services/customerApi';

interface HouseholdPortalProps {
  onBackToHomepage: () => void;
  onLogout?: () => void;
  initialTab?: PortalTab;
}

const INITIAL_PROFILE: CustomerProfileData = {
  id: 'cust-1',
  name: 'Ananya Sharma',
  mobile: '+91 98765 43210',
  email: 'customer@example.com',
  consumerNumber: 'LPG-HH-98214',
  lpgId: '1729 4820 9102',
  address: 'Flat 304, Palm Grove, Pimpri, Pune, Maharashtra',
  city: 'Pune',
  state: 'Maharashtra',
  pincode: '411018',
  distributorName: 'City Gas Agency',
  distributorCode: 'DIST-PUN-042',
  distributorPhone: '+91 20 2742 8899',
  subsidizedQuotaLeft: 10,
  totalAnnualQuota: 12,
  kycStatus: 'VERIFIED',
};

export const HouseholdPortal: React.FC<HouseholdPortalProps> = ({
  onBackToHomepage,
  onLogout,
  initialTab = 'dashboard',
}) => {
  const [activeTab, setActiveTab] = useState<PortalTab>(initialTab);
  const [aiPrompt, setAiPrompt] = useState<string | undefined>(undefined);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  // Customer Data State
  const [profile, setProfile] = useState<CustomerProfileData>(() => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('lpg_customer_profile');
      if (stored) {
        try { return JSON.parse(stored); } catch { /* ignore */ }
      }
    }
    return INITIAL_PROFILE;
  });
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [addresses, setAddresses] = useState<SavedAddress[]>([]);
  const [documents, setDocuments] = useState<KycDocument[]>(() => CustomerService.getKycDocuments());
  const [invoices, setInvoices] = useState<BillingInvoice[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>(() =>
    CustomerService.getNotifications()
  );
  const [tickets, setTickets] = useState<SupportTicket[]>([]);

  // Load customer data asynchronously from backend APIs
  useEffect(() => {
    let isMounted = true;
    CustomerService.getProfile().then((p) => {
      if (isMounted) setProfile(p);
    });
    CustomerService.getAddresses().then((a) => {
      if (isMounted) setAddresses(a);
    });
    CustomerService.getOrders().then((ord) => {
      if (isMounted) setOrders(ord);
    });
    CustomerService.getInvoices().then((inv) => {
      if (isMounted) setInvoices(inv);
    });
    CustomerService.getTickets().then((t) => {
      if (isMounted) setTickets(t);
    });
    return () => {
      isMounted = false;
    };
  }, []);

  // Listen for real-time order or profile updates
  useEffect(() => {
    const handleOrderUpdate = () => {
      CustomerService.getOrders().then((ord) => setOrders(ord));
    };
    const handleProfileUpdate = () => {
      CustomerService.getProfile().then((p) => setProfile(p));
    };

    window.addEventListener('lpg_orders_updated', handleOrderUpdate);
    window.addEventListener('lpg_profile_updated', handleProfileUpdate);
    return () => {
      window.removeEventListener('lpg_orders_updated', handleOrderUpdate);
      window.removeEventListener('lpg_profile_updated', handleProfileUpdate);
    };
  }, []);

  // Latest active order for dashboard / tracking
  const latestOrder = orders.length > 0 ? orders[0] : null;

  const handleNavigateTab = (tab: PortalTab, prompt?: string) => {
    setActiveTab(tab);
    if (prompt) {
      setAiPrompt(prompt);
    } else {
      setAiPrompt(undefined);
    }
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleMarkNotificationsRead = () => {
    CustomerService.markAllNotificationsRead();
    setNotifications(CustomerService.getNotifications());
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-900 flex flex-row font-sans selection:bg-rose-100 selection:text-rose-900">
      {/* ── Left Sidebar ── */}
      <PortalSidebar
        activeTab={activeTab}
        onTabChange={(tab) => handleNavigateTab(tab)}
        onBackToHomepage={onBackToHomepage}
        isMobileOpen={isMobileSidebarOpen}
        onCloseMobile={() => setIsMobileSidebarOpen(false)}
      />

      {/* ── Main Content Container ── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* ── Top Bar ── */}
        <PortalTopBar
          profile={profile}
          notifications={notifications}
          onMarkNotificationsRead={handleMarkNotificationsRead}
          onNavigateTab={handleNavigateTab}
          onOpenMobileSidebar={() => setIsMobileSidebarOpen(true)}
          onLogout={onLogout}
        />

        {/* ── Viewport Body ── */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto">
          {activeTab === 'dashboard' && (
            <PortalDashboardView
              profile={profile}
              latestOrder={latestOrder}
              onNavigateTab={handleNavigateTab}
            />
          )}

          {activeTab === 'book' && (
            <PortalBookCylinderView
              profile={profile}
              addresses={addresses}
              onOrderCreated={(newOrder) => {
                setOrders((prev) => [newOrder, ...prev]);
              }}
              onNavigateTab={handleNavigateTab}
            />
          )}

          {activeTab === 'track' && (
            <PortalTrackDeliveryView
              profile={profile}
              activeOrder={latestOrder}
              onNavigateTab={handleNavigateTab}
            />
          )}

          {activeTab === 'ai' && (
            <PortalAiAssistanceView
              profile={profile}
              initialPrompt={aiPrompt}
              onNavigateTab={handleNavigateTab}
            />
          )}

          {activeTab === 'transparency' && (
            <PortalTransparencyView profile={profile} />
          )}

          {activeTab === 'kyc' && (
            <PortalKycDocumentsView
              profile={profile}
              documents={documents}
              onDocumentsUpdated={(docs) => setDocuments(docs)}
            />
          )}

          {activeTab === 'profile' && (
            <PortalProfileView
              profile={profile}
              onProfileUpdated={(p) => setProfile(p)}
            />
          )}

          {activeTab === 'addresses' && (
            <PortalSavedAddressesView
              addresses={addresses}
              onAddressesUpdated={(addr) => setAddresses(addr)}
            />
          )}

          {activeTab === 'billing' && (
            <PortalBillingPaymentsView
              profile={profile}
              invoices={invoices}
            />
          )}

          {activeTab === 'history' && (
            <PortalBookingHistoryView
              orders={orders}
              onNavigateTab={handleNavigateTab}
              onReorder={() => handleNavigateTab('book')}
            />
          )}

          {activeTab === 'support' && (
            <PortalHelpSupportView
              profile={profile}
              tickets={tickets}
              onTicketCreated={(t) => setTickets((prev) => [t, ...prev])}
            />
          )}
        </main>
      </div>
    </div>
  );
};
