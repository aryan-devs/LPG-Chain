import React, { useState, useEffect, useCallback } from 'react';
import { DistributorSidebar } from './DistributorSidebar';
import { DistributorTopBar } from './DistributorTopBar';
import { DistributorOverviewView } from './views/DistributorOverviewView';
import { DistributorBookingRequestsView } from './views/DistributorBookingRequestsView';
import { DistributorOrdersQueueView } from './views/DistributorOrdersQueueView';
import { DistributorInventoryView } from './views/DistributorInventoryView';
import { DistributorDeliveryScheduleView } from './views/DistributorDeliveryScheduleView';
import { DistributorLiveDeliveriesView } from './views/DistributorLiveDeliveriesView';
import { DistributorAllocationSalesView } from './views/DistributorAllocationSalesView';
import { DistributorDemandForecastView } from './views/DistributorDemandForecastView';
import { DistributorReportsView } from './views/DistributorReportsView';
import { DistributorProfileView } from './views/DistributorProfileView';
import {
  DistributorPortalTab,
  DistributorOverviewData,
  DistributorBookingItem,
  DistributorDeliveryItem,
  DistributorInventoryItem,
  ForecastResponse,
  isBookingPending,
  isBookingConfirmed,
  isBookingCancelled,
} from '../../types/distributorPortalTypes';
import { DistributorApiService } from '../../services/distributorApi';
import { ShieldAlert } from 'lucide-react';

interface DistributorPortalProps {
  onBackToHomepage: () => void;
  onLogout?: () => void;
  onRequireLogin?: () => void;
  initialTab?: DistributorPortalTab;
}

export const DistributorPortal: React.FC<DistributorPortalProps> = ({
  onBackToHomepage,
  onLogout,
  onRequireLogin,
  initialTab = 'overview',
}) => {
  const [activeTab, setActiveTab] = useState<DistributorPortalTab>(initialTab);
  const [distributorId, setDistributorId] = useState<number>(1);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  // Operational State
  const [overviewData, setOverviewData] = useState<DistributorOverviewData | null>(null);
  const [bookings, setBookings] = useState<DistributorBookingItem[]>([]);
  const [deliveries, setDeliveries] = useState<DistributorDeliveryItem[]>([]);
  const [inventory, setInventory] = useState<DistributorInventoryItem[]>([]);
  const [forecast, setForecast] = useState<ForecastResponse | null>(null);
  const [aiOnline, setAiOnline] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);

  const loadAllData = useCallback(async (showSpinner = true) => {
    try {
      if (showSpinner) {
        setIsLoading(true);
      }
      setAuthError(null);

      const token = typeof window !== 'undefined' ? (localStorage.getItem('token') || sessionStorage.getItem('token')) : null;
      if (!token) {
        setAuthError('Authentication required: Sign in with your verified distributor account to view live agency records.');
        setIsLoading(false);
        return;
      }

      // Check ML health asynchronously without blocking operational database queries
      DistributorApiService.checkForecastHealth()
        .then((mlHealthy) => {
          setAiOnline(mlHealthy);
          if (mlHealthy) {
            DistributorApiService.getDemandForecast(7, distributorId)
              .then((fc) => setForecast(fc))
              .catch(() => setForecast(null));
          }
        })
        .catch(() => setAiOnline(false));

      // Load backend records with Promise.allSettled so any partial failure does not discard bookings
      try {
        const [ovRes, bkRes, delRes, invRes] = await Promise.allSettled([
          DistributorApiService.getDashboardOverview(distributorId),
          DistributorApiService.getDistributorBookings(distributorId),
          DistributorApiService.getDistributorDeliveries(distributorId),
          DistributorApiService.getInventory(distributorId),
        ]);

        if (bkRes.status === 'fulfilled') {
          setBookings(bkRes.value);
        }
        if (ovRes.status === 'fulfilled') {
          setOverviewData(ovRes.value);
        }
        if (delRes.status === 'fulfilled') {
          setDeliveries(delRes.value);
        }
        if (invRes.status === 'fulfilled') {
          setInventory(invRes.value);
        }

        const errors = [ovRes, bkRes, delRes, invRes]
          .filter((r): r is PromiseRejectedResult => r.status === 'rejected')
          .map((r) => r.reason?.message || String(r.reason));

        const authErr = errors.find((msg) => msg.includes('401') || msg.includes('403'));
        if (authErr) {
          setAuthError('Session expired or access denied: Please sign in with a verified distributor account (distributor@agency.example).');
        } else if (bkRes.status === 'rejected') {
          setAuthError(`Unable to load operational bookings: ${bkRes.reason?.message || 'API error'}`);
        }
      } catch (apiErr: any) {
        const msg = apiErr?.message || '';
        if (msg.includes('401') || msg.includes('403')) {
          setAuthError('Session expired or access denied: Please sign in with a verified distributor account (distributor@agency.example).');
        } else {
          setAuthError(`Unable to load operational records: ${msg}`);
        }
      }
    } catch (err) {
      console.error('Error loading distributor operational data:', err);
    } finally {
      setIsLoading(false);
    }
  }, [distributorId]);

  // Synchronously update authoritative booking state with server response and sync database in background
  const handleBookingUpdated = useCallback(
    async (updatedBooking?: DistributorBookingItem) => {
      if (updatedBooking && updatedBooking.id) {
        // 1. Immediate state update with confirmed server response
        setBookings((prevBookings) =>
          prevBookings.map((b) =>
            b.id === updatedBooking.id
              ? { ...b, ...updatedBooking, status: updatedBooking.status }
              : b
          )
        );

        // 2. Immediate overview metrics & recent bookings synchronization
        setOverviewData((prev) => {
          if (!prev) return prev;
          const wasPending = prev.recentBookings?.some(
            (b) => b.id === updatedBooking.id && isBookingPending(b.status)
          );
          const updatedRecent = prev.recentBookings
            ? prev.recentBookings.map((b) =>
                b.id === updatedBooking.id
                  ? { ...b, ...updatedBooking, status: updatedBooking.status }
                  : b
              )
            : [];
          return {
            ...prev,
            newBookingRequestsCount: wasPending
              ? Math.max(0, prev.newBookingRequestsCount - 1)
              : prev.newBookingRequestsCount,
            acceptedQueuedOrdersCount: isBookingConfirmed(updatedBooking.status)
              ? prev.acceptedQueuedOrdersCount + (wasPending ? 1 : 0)
              : prev.acceptedQueuedOrdersCount,
            cancelledBookingsCount: isBookingCancelled(updatedBooking.status)
              ? prev.cancelledBookingsCount + (wasPending ? 1 : 0)
              : prev.cancelledBookingsCount,
            recentBookings: updatedRecent,
          };
        });
      }

      // 3. Synchronize all records from database in background
      await loadAllData(false);
    },
    [loadAllData]
  );

  useEffect(() => {
    loadAllData(true);
  }, [loadAllData]);

  const handleTabChange = (tab: DistributorPortalTab) => {
    setActiveTab(tab);
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const pendingCount = bookings.filter((b) => isBookingPending(b.status)).length;
  const queuedCount = bookings.filter((b) => isBookingConfirmed(b.status)).length;

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-900 flex flex-row font-sans selection:bg-rose-100 selection:text-rose-900">
      {/* ── Left Sidebar ── */}
      <DistributorSidebar
        activeTab={activeTab}
        onTabChange={handleTabChange}
        onBackToHomepage={onBackToHomepage}
        isMobileOpen={isMobileSidebarOpen}
        onCloseMobile={() => setIsMobileSidebarOpen(false)}
        pendingRequestsCount={pendingCount}
        queuedOrdersCount={queuedCount}
      />

      {/* ── Main View Container ── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* ── Top Bar ── */}
        <DistributorTopBar
          currentDistributorId={distributorId}
          onDistributorChange={(id) => setDistributorId(id)}
          agencyName={overviewData?.distributorName || 'Apex Gas Distribution Agency'}
          agencyCode={overviewData?.distributorCode || 'DIST-001'}
          onOpenMobileSidebar={() => setIsMobileSidebarOpen(true)}
          onLogout={onLogout}
          onNavigateTab={handleTabChange}
          aiServiceOnline={aiOnline}
        />

        {/* ── Viewport Body ── */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto space-y-6">
          {authError && (
            <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
                  <ShieldAlert size={18} />
                </div>
                <div className="text-xs">
                  <strong className="block font-bold text-amber-950">Distributor Authentication Required</strong>
                  <span className="text-amber-800">{authError}</span>
                </div>
              </div>
              {onRequireLogin && (
                <button
                  type="button"
                  onClick={onRequireLogin}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold transition-all shadow-sm shrink-0"
                >
                  Sign In as Distributor
                </button>
              )}
            </div>
          )}
          {activeTab === 'overview' && (
            <DistributorOverviewView
              data={overviewData}
              forecast={forecast}
              isLoading={isLoading}
              onRefresh={() => loadAllData(true)}
              onNavigateTab={handleTabChange}
              aiOnline={aiOnline}
            />
          )}

          {activeTab === 'booking-requests' && (
            <DistributorBookingRequestsView
              bookings={bookings}
              isLoading={isLoading}
              onRefresh={() => loadAllData(true)}
              onBookingUpdated={handleBookingUpdated}
            />
          )}

          {activeTab === 'orders-queue' && (
            <DistributorOrdersQueueView
              bookings={bookings}
              deliveries={deliveries}
              distributorId={distributorId}
              isLoading={isLoading}
              onRefresh={() => loadAllData(true)}
              onOrderScheduled={() => loadAllData(false)}
              onNavigateTab={handleTabChange}
            />
          )}

          {activeTab === 'inventory' && (
            <DistributorInventoryView
              inventory={inventory}
              distributorId={distributorId}
              isLoading={isLoading}
              onRefresh={loadAllData}
              onInventoryUpdated={loadAllData}
            />
          )}

          {activeTab === 'delivery-schedule' && (
            <DistributorDeliveryScheduleView
              deliveries={deliveries}
              isLoading={isLoading}
              onRefresh={loadAllData}
              onDeliveryUpdated={loadAllData}
              onNavigateTab={handleTabChange}
            />
          )}

          {activeTab === 'live-deliveries' && (
            <DistributorLiveDeliveriesView
              deliveries={deliveries}
              isLoading={isLoading}
              onRefresh={loadAllData}
              onDeliveryUpdated={loadAllData}
              onNavigateTab={handleTabChange}
            />
          )}

          {activeTab === 'transparency' && (
            <DistributorAllocationSalesView distributorId={distributorId} />
          )}

          {activeTab === 'ai-forecast' && (
            <DistributorDemandForecastView distributorId={distributorId} />
          )}

          {activeTab === 'reports' && (
            <DistributorReportsView overview={overviewData} />
          )}

          {activeTab === 'agency-profile' && (
            <DistributorProfileView distributorId={distributorId} />
          )}
        </main>
      </div>
    </div>
  );
};
