import React, { useState } from 'react';
import { AdminPortalTab } from '../../types/adminPortalTypes';
import { AdminSidebar } from './AdminSidebar';
import { AdminTopBar } from './AdminTopBar';

// 11 Admin Views
import { AdminOverviewView } from './views/AdminOverviewView';
import { AdminUserManagementView } from './views/AdminUserManagementView';
import { AdminDistributorManagementView } from './views/AdminDistributorManagementView';
import { AdminBookingsDeliveriesView } from './views/AdminBookingsDeliveriesView';
import { AdminInventoryAllocationsView } from './views/AdminInventoryAllocationsView';
import { AdminDriversVehiclesView } from './views/AdminDriversVehiclesView';
import { AdminReportsAnalyticsView } from './views/AdminReportsAnalyticsView';
import { AdminAIDemandForecastView } from './views/AdminAIDemandForecastView';
import { AdminRolesSecurityView } from './views/AdminRolesSecurityView';
import { AdminAuditLogsView } from './views/AdminAuditLogsView';
import { AdminSystemSettingsView } from './views/AdminSystemSettingsView';
import { AdminKycReviewView } from './views/AdminKycReviewView';
import { AdminComplaintsView } from './views/AdminComplaintsView';

interface AdminPortalProps {
  onBackToHomepage: () => void;
  onLogout?: () => void;
  onRequireLogin?: () => void;
  initialTab?: AdminPortalTab;
}

export const AdminPortal: React.FC<AdminPortalProps> = ({
  onBackToHomepage,
  onLogout,
  onRequireLogin,
  initialTab = 'overview',
}) => {
  const [activeTab, setActiveTab] = useState<AdminPortalTab>(initialTab);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);
  const [globalSearch, setGlobalSearch] = useState('');

  // Guard: if user is not authenticated or not admin, invoke login modal
  React.useEffect(() => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const userStr = typeof window !== 'undefined' ? localStorage.getItem('lpg_user') : null;
    if ((!token || !userStr) && onRequireLogin) {
      onRequireLogin();
    }
  }, [onRequireLogin]);

  const handleTabChange = (tab: AdminPortalTab) => {
    setActiveTab(tab);
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const renderActiveView = () => {
    switch (activeTab) {
      case 'overview':
        return <AdminOverviewView onNavigateTab={handleTabChange} />;
      case 'users':
        return <AdminUserManagementView />;
      case 'distributors':
        return <AdminDistributorManagementView />;
      case 'bookings-deliveries':
        return <AdminBookingsDeliveriesView />;
      case 'inventory-allocations':
        return <AdminInventoryAllocationsView />;
      case 'drivers-vehicles':
        return <AdminDriversVehiclesView />;
      case 'reports-analytics':
        return <AdminReportsAnalyticsView />;
      case 'ai-demand-forecast':
        return <AdminAIDemandForecastView />;
      case 'roles-security':
        return <AdminRolesSecurityView />;
      case 'audit-logs':
        return <AdminAuditLogsView />;
      case 'system-settings':
        return <AdminSystemSettingsView />;
      case 'kyc-documents':
        return <AdminKycReviewView />;
      case 'complaints':
        return <AdminComplaintsView />;
      default:
        return <AdminOverviewView onNavigateTab={handleTabChange} />;
    }
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-900 flex flex-row font-sans selection:bg-rose-100 selection:text-rose-900">
      {/* ── Left Admin Sidebar ── */}
      <AdminSidebar
        activeTab={activeTab}
        onTabChange={handleTabChange}
        onBackToHomepage={onBackToHomepage}
        onLogout={onLogout}
        isMobileOpen={isMobileSidebarOpen}
        onCloseMobile={() => setIsMobileSidebarOpen(false)}
      />

      {/* ── Main Viewport Container ── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* ── Admin Top Bar ── */}
        <AdminTopBar
          activeTab={activeTab}
          onOpenMobileSidebar={() => setIsMobileSidebarOpen(true)}
          onLogout={onLogout}
          onNavigateTab={handleTabChange}
          globalSearchQuery={globalSearch}
          onGlobalSearchChange={setGlobalSearch}
        />

        {/* ── View Body ── */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto">
          {renderActiveView()}
        </main>
      </div>
    </div>
  );
};
