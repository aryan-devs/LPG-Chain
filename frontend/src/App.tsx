import { useEffect, useState } from 'react';
import { Activity, CheckCircle, XCircle, Loader, MapPin, Key, Search, Flame, Lock, ArrowRight, Truck, Menu, X } from 'lucide-react';
import { LiveTrackingMap } from './components/tracking/LiveTrackingMap';
import { LPGChainHero } from './components/hero/LPGChainHero';
import { LandingSections } from './components/landing/LandingSections';
import { BookingModal } from './components/hero/BookingModal';
import { LoginModal, UserProfile } from './components/auth/LoginModal';
import { AISentinelView } from './components/capabilities/AISentinelView';
import { AIKYCView } from './components/capabilities/AIKYCView';
import { AnalyticsView } from './components/capabilities/AnalyticsView';
import { SecurityView } from './components/capabilities/SecurityView';
import { DistributorNetworkView } from './components/capabilities/DistributorNetworkView';
import { HouseholdPortal } from './components/portal/HouseholdPortal';
import { DistributorPortal } from './components/distributor/DistributorPortal';
import { AdminPortal } from './components/admin/AdminPortal';

// ----------------------------------------------------------------
// Types
// ----------------------------------------------------------------

type BackendStatus = 'loading' | 'up' | 'down';
type ActiveTab =
  | 'platform'
  | 'tracking'
  | 'health'
  | 'sentinel'
  | 'kyc'
  | 'analytics'
  | 'security'
  | 'distributor'
  | 'portal'
  | 'distributor-portal'
  | 'admin-portal';

interface HealthResponse {
  status: string;
  service: string;
}

// ----------------------------------------------------------------
// Access Denied Screen for Guarded Routes
// ----------------------------------------------------------------

interface AccessDeniedViewProps {
  portalName: string;
  requiredRole: string;
  currentUser: UserProfile | null;
  onBackToHomepage: () => void;
  onLoginAgain: () => void;
}

function AccessDeniedView({
  portalName,
  requiredRole,
  currentUser,
  onBackToHomepage,
  onLoginAgain,
}: AccessDeniedViewProps) {
  return (
    <div className="min-h-[80vh] bg-slate-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl border border-slate-200 p-8 text-center">
        <div className="w-16 h-16 rounded-2xl bg-rose-50 border border-rose-100 flex items-center justify-center mx-auto mb-5 text-rose-600 shadow-sm">
          <Lock className="w-8 h-8" />
        </div>
        <span className="inline-block px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-rose-100 text-rose-800 mb-3">
          403 Access Denied
        </span>
        <h2 className="text-2xl font-black text-slate-900 mb-2">
          {portalName} Protected
        </h2>
        <p className="text-sm text-slate-600 mb-6 leading-relaxed">
          {currentUser ? (
            <>
              You are currently authenticated as <span className="font-semibold text-slate-900">{currentUser.name}</span> with role <span className="font-mono text-rose-600 font-bold">{currentUser.role}</span>. This portal requires verified <span className="font-semibold text-slate-900">{requiredRole}</span> privileges.
            </>
          ) : (
            <>
              Authentication required. You must log in with an authorized <span className="font-semibold text-slate-900">{requiredRole}</span> account to access this portal.
            </>
          )}
        </p>

        <div className="flex flex-col gap-3">
          <button
            type="button"
            onClick={onLoginAgain}
            className="w-full py-2.5 px-4 bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm rounded-xl shadow-md shadow-rose-600/25 transition-all flex items-center justify-center gap-2"
          >
            <Lock className="w-4 h-4" />
            <span>{currentUser ? 'Switch / Re-authenticate Account' : 'Log In to Continue'}</span>
          </button>
          <button
            type="button"
            onClick={onBackToHomepage}
            className="w-full py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm rounded-xl transition-all"
          >
            Return to Homepage
          </button>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------------------
// App — LPG-Chain Shell with 3D Core Hero & Live Vehicle Tracking
// ----------------------------------------------------------------

function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('platform');
  const [isHeaderBookingOpen, setIsHeaderBookingOpen] = useState(false);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState<'login' | 'register'>('login');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [loginInitialRole, setLoginInitialRole] = useState<'Consumer' | 'Distributor' | 'Driver' | 'Admin'>('Consumer');
  type PendingAuthAction = 'booking' | 'tracking' | 'dashboard' | null;
  const [pendingAuthAction, setPendingAuthAction] = useState<PendingAuthAction>(null);
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('lpg_user');
      if (saved) {
        try {
          return JSON.parse(saved);
        } catch {
          return null;
        }
      }
    }
    return null;
  });

  // Backend Health State (preserved)
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('loading');
  const [healthData, setHealthData] = useState<HealthResponse | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);

  // Delivery Tracking Configuration State (preserved)
  const [deliveryIdInput, setDeliveryIdInput] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      const params = new URLSearchParams(window.location.search);
      const qId = params.get('deliveryId');
      if (qId && qId.trim().length > 0) return qId.trim();
    }
    return '1'; // Default development delivery ID
  });
  const [activeDeliveryId, setActiveDeliveryId] = useState<string>('1');

  // Optional JWT Token State (for authorized testing - preserved)
  const [tokenInput, setTokenInput] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('token') || '';
    }
    return '';
  });
  const [showTokenSettings, setShowTokenSettings] = useState<boolean>(false);

  // Health check polling on mount
  useEffect(() => {
    let isMounted = true;
    const checkHealth = async () => {
      try {
        setBackendStatus('loading');
        const response = await fetch('/api/health');
        if (!response.ok) {
          if (isMounted) {
            setBackendStatus('down');
            setHealthError(`Backend service returned HTTP ${response.status}. Ensure backend is running on port 8080.`);
          }
          return;
        }
        const data: HealthResponse = await response.json();
        if (isMounted) {
          setHealthData(data);
          setBackendStatus('up');
          setHealthError(null);
        }
      } catch {
        if (isMounted) {
          setBackendStatus('down');
          setHealthError('Could not reach backend service on port 8080.');
        }
      }
    };

    checkHealth();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleApplyDeliveryId = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = deliveryIdInput.trim();
    if (trimmed && Number(trimmed) > 0) {
      setActiveDeliveryId(trimmed);
      // Persist in URL query without reloading
      if (typeof window !== 'undefined') {
        const url = new URL(window.location.href);
        url.searchParams.set('deliveryId', trimmed);
        window.history.replaceState({}, '', url.toString());
      }
    }
  };

  const handleSaveToken = () => {
    if (typeof window !== 'undefined') {
      if (tokenInput.trim()) {
        localStorage.setItem('token', tokenInput.trim());
      } else {
        localStorage.removeItem('token');
      }
    }
  };

  const handleNavigateToTracking = (deliveryId?: string) => {
    if (deliveryId) {
      setDeliveryIdInput(deliveryId);
      setActiveDeliveryId(deliveryId);
    }
    setActiveTab('tracking');
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleNavigateToCapability = (
    capabilityId: 'sentinel' | 'kyc' | 'analytics' | 'security' | 'distributor'
  ) => {
    setActiveTab(capabilityId);
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleBookLpgClick = () => {
    if (!currentUser) {
      setPendingAuthAction('booking');
      setIsLoginOpen(true);
      return;
    }
    // If logged in as customer/consumer (or authorized user), go directly to booking
    setIsHeaderBookingOpen(true);
  };

  const handleTrackDeliveryClick = (deliveryId?: string) => {
    if (!currentUser) {
      setPendingAuthAction('tracking');
      setIsLoginOpen(true);
      return;
    }
    handleNavigateToTracking(deliveryId);
  };

  const handleGetStartedClick = () => {
    if (!currentUser) {
      setPendingAuthAction(null);
      setAuthModalMode('register');
      setIsLoginOpen(true);
      return;
    }
    // Directs to appropriate dashboard based on verified active role
    const r = (currentUser.rawRole || currentUser.role).toUpperCase();
    if (r === 'CONSUMER' || r === 'CUSTOMER') {
      setActiveTab('portal');
    } else if (r === 'DISTRIBUTOR') {
      setActiveTab('distributor-portal');
    } else if (r === 'ADMIN') {
      setActiveTab('admin-portal');
    } else {
      handleNavigateToTracking('1');
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('lpg_user');
      localStorage.removeItem('token');
      localStorage.removeItem('lpg_customer_profile');
    }
    setTokenInput('');
    setActiveTab('platform');
    setIsMobileMenuOpen(false);
  };

  const handleLoginSuccess = (user: UserProfile) => {
    setCurrentUser(user);
    if (typeof window !== 'undefined') {
      localStorage.setItem('lpg_user', JSON.stringify(user));
      if (user.token) {
        localStorage.setItem('token', user.token);
        setTokenInput(user.token);
      }
    }

    // Process pending destination according to requirements
    if (pendingAuthAction === 'booking') {
      // 1. BOOK LPG CYLINDER: After successful customer login -> continue to existing Booking page
      setIsHeaderBookingOpen(true);
    } else if (pendingAuthAction === 'tracking') {
      // 2 & 3. TRACK DELIVERY / LIVE TRACKING: go to existing customer delivery/tracking page
      handleNavigateToTracking('1');
    } else {
      // Automatic role-based redirection
      const r = (user.rawRole || user.role).toUpperCase();
      if (r === 'CONSUMER' || r === 'CUSTOMER') {
        setActiveTab('portal');
      } else if (r === 'DISTRIBUTOR') {
        setActiveTab('distributor-portal');
      } else if (r === 'ADMIN') {
        setActiveTab('admin-portal');
      } else {
        setActiveTab('platform');
      }
    }
    setPendingAuthAction(null);
    setIsMobileMenuOpen(false);
  };

  const scrollToSection = (sectionId: string) => {
    if (activeTab !== 'platform') {
      setActiveTab('platform');
      setTimeout(() => {
        const el = document.getElementById(sectionId);
        if (el) el.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    } else {
      const el = document.getElementById(sectionId);
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // ----------------------------------------------------------------
  // Guarded Portal Views (Role-Verified Access)
  // ----------------------------------------------------------------

  if (activeTab === 'portal') {
    const isCustomer = currentUser && (currentUser.role === 'Consumer' || (currentUser as any).rawRole === 'CUSTOMER');
    if (!isCustomer) {
      return (
        <AccessDeniedView
          portalName="Customer Portal"
          requiredRole="Customer"
          currentUser={currentUser}
          onBackToHomepage={() => {
            setActiveTab('platform');
            if (typeof window !== 'undefined') {
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }
          }}
          onLoginAgain={() => {
            setLoginInitialRole('Consumer');
            setAuthModalMode('login');
            setIsLoginOpen(true);
          }}
        />
      );
    }
    return (
      <HouseholdPortal
        onBackToHomepage={() => {
          setActiveTab('platform');
          if (typeof window !== 'undefined') {
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }
        }}
        onLogout={handleLogout}
      />
    );
  }

  if (activeTab === 'distributor-portal') {
    const isDistributor = currentUser && (currentUser.role === 'Distributor' || (currentUser as any).rawRole === 'DISTRIBUTOR');
    if (!isDistributor) {
      return (
        <AccessDeniedView
          portalName="Distributor Portal"
          requiredRole="Distributor"
          currentUser={currentUser}
          onBackToHomepage={() => {
            setActiveTab('platform');
            if (typeof window !== 'undefined') {
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }
          }}
          onLoginAgain={() => {
            setLoginInitialRole('Distributor');
            setAuthModalMode('login');
            setIsLoginOpen(true);
          }}
        />
      );
    }
    return (
      <DistributorPortal
        onBackToHomepage={() => {
          setActiveTab('platform');
          if (typeof window !== 'undefined') {
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }
        }}
        onRequireLogin={() => {
          setPendingAuthAction('dashboard');
          setLoginInitialRole('Distributor');
          setAuthModalMode('login');
          setIsLoginOpen(true);
        }}
        onLogout={handleLogout}
      />
    );
  }

  if (activeTab === 'admin-portal') {
    const isAdmin = currentUser && (currentUser.role === 'Admin' || (currentUser as any).rawRole === 'ADMIN');
    if (!isAdmin) {
      return (
        <AccessDeniedView
          portalName="Admin Portal"
          requiredRole="Admin"
          currentUser={currentUser}
          onBackToHomepage={() => {
            setActiveTab('platform');
            if (typeof window !== 'undefined') {
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }
          }}
          onLoginAgain={() => {
            setLoginInitialRole('Admin');
            setAuthModalMode('login');
            setIsLoginOpen(true);
          }}
        />
      );
    }
    return (
      <AdminPortal
        onBackToHomepage={() => {
          setActiveTab('platform');
          if (typeof window !== 'undefined') {
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }
        }}
        onRequireLogin={() => {
          setPendingAuthAction('dashboard');
          setLoginInitialRole('Admin');
          setAuthModalMode('login');
          setIsLoginOpen(true);
        }}
        onLogout={handleLogout}
      />
    );
  }

  return (
    <div className="min-h-screen bg-white text-slate-900 flex flex-col font-sans">
      {/* ── Top Navigation Bar ── */}
      <header className="border-b border-slate-200/80 bg-white/95 backdrop-blur-md sticky top-0 z-50 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Logo & Platform Name */}
          <div
            role="button"
            tabIndex={0}
            onClick={() => {
              setActiveTab('platform');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                setActiveTab('platform');
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }
            }}
            className="flex items-center gap-3 cursor-pointer outline-none select-none group"
          >
            <div className="w-9 h-9 rounded-xl bg-rose-600 flex items-center justify-center text-white shadow-md shadow-rose-600/30 font-black group-hover:scale-105 transition-transform">
              <Flame size={20} />
            </div>
            <div className="flex flex-col">
              <span className="text-lg font-black tracking-tight text-slate-900 leading-none">
                LPG-<span className="text-rose-600">Chain</span>
              </span>
              <span className="text-[10px] font-semibold text-slate-400 mt-0.5 tracking-tight">
                Safer Energy, Smarter Supply.
              </span>
            </div>
          </div>

          {/* Navigation Links / Brand Menus */}
          <nav className="hidden md:flex items-center gap-6 lg:gap-8 text-sm font-semibold text-slate-600">
            <button
              type="button"
              onClick={() => {
                setActiveTab('platform');
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
              className={`transition-colors relative py-1 ${
                activeTab === 'platform' ? 'text-rose-600 font-bold' : 'hover:text-slate-900'
              }`}
            >
              Home
              {activeTab === 'platform' && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-rose-600 rounded-full" />
              )}
            </button>

            <button
              type="button"
              onClick={() => scrollToSection('features')}
              className="hover:text-slate-900 transition-colors"
            >
              Features
            </button>

            <button
              type="button"
              onClick={() => scrollToSection('how-it-works')}
              className="hover:text-slate-900 transition-colors"
            >
              How It Works
            </button>

            <button
              type="button"
              onClick={() => scrollToSection('why-lpg-chain')}
              className="hover:text-slate-900 transition-colors"
            >
              About
            </button>

            <button
              type="button"
              onClick={() => handleTrackDeliveryClick('1')}
              className={`transition-colors relative py-1 ${
                activeTab === 'tracking' ? 'text-blue-600 font-bold' : 'hover:text-slate-900'
              }`}
            >
              Live Tracking
              {activeTab === 'tracking' && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 rounded-full" />
              )}
            </button>

            {/* Authenticated Portal Link (Only the single portal authorized for verified role) */}
            {currentUser && (currentUser.role === 'Consumer' || (currentUser as any).rawRole === 'CUSTOMER') && (
              <button
                type="button"
                onClick={() => {
                  setActiveTab('portal');
                  if (typeof window !== 'undefined') {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }
                }}
                className="transition-colors relative py-1 font-semibold text-slate-700 hover:text-rose-600"
              >
                Customer Portal
              </button>
            )}

            {currentUser && (currentUser.role === 'Distributor' || (currentUser as any).rawRole === 'DISTRIBUTOR') && (
              <button
                type="button"
                onClick={() => {
                  setActiveTab('distributor-portal');
                  if (typeof window !== 'undefined') {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }
                }}
                className="transition-colors relative py-1 font-semibold text-slate-700 hover:text-blue-600"
              >
                Distributor Portal
              </button>
            )}

            {currentUser && (currentUser.role === 'Admin' || (currentUser as any).rawRole === 'ADMIN') && (
              <button
                type="button"
                onClick={() => {
                  setActiveTab('admin-portal');
                  if (typeof window !== 'undefined') {
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }
                }}
                className="transition-colors relative py-1 font-bold text-rose-600 hover:text-rose-700"
              >
                Admin Portal
              </button>
            )}
          </nav>

          {/* Right Header Actions */}
          <div className="flex items-center gap-3">
            {/* System Health Status Indicator */}
            <button
              type="button"
              onClick={() => setActiveTab('health')}
              title={`System Status: ${backendStatus}`}
              className="hidden xl:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-100 hover:bg-slate-200 text-xs font-medium text-slate-700 transition-colors"
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  backendStatus === 'up'
                    ? 'bg-emerald-500'
                    : backendStatus === 'loading'
                    ? 'bg-amber-400 animate-pulse'
                    : 'bg-rose-500'
                }`}
              />
              <span className="text-[11px] capitalize">{backendStatus === 'up' ? 'API Online' : 'Health'}</span>
            </button>

            {/* Authenticated User Profile Badge or Login / Sign Up */}
            {currentUser ? (
              <div className="hidden sm:flex items-center gap-2">
                <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-100 rounded-full border border-slate-200 text-xs font-semibold text-slate-800">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  <span>{currentUser.name}</span>
                  <span className="text-[10px] text-slate-500 bg-white px-1.5 py-0.5 rounded-full border border-slate-200 font-mono">
                    {currentUser.role}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="text-xs text-rose-600 hover:text-rose-700 font-bold px-2.5 py-1.5 hover:bg-rose-50 rounded-lg transition-colors"
                >
                  Logout
                </button>
              </div>
            ) : (
              <div className="hidden sm:flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setPendingAuthAction(null);
                    setAuthModalMode('login');
                    setIsLoginOpen(true);
                  }}
                  className="px-4 py-1.5 rounded-full border border-slate-300 hover:border-slate-400 text-slate-700 hover:text-slate-900 text-xs sm:text-sm font-semibold transition-all bg-white shadow-sm"
                >
                  Login
                </button>
                <button
                  type="button"
                  onClick={handleGetStartedClick}
                  className="flex items-center gap-1.5 px-4 py-1.5 bg-rose-600 hover:bg-rose-500 text-white text-xs sm:text-sm font-bold rounded-full shadow-md shadow-rose-600/25 hover:shadow-rose-600/35 transition-all"
                >
                  <span>Get Started / Sign Up</span>
                  <span aria-hidden="true">&rarr;</span>
                </button>
              </div>
            )}

            {/* Mobile Hamburger Menu Toggle */}
            <button
              type="button"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors"
              aria-label="Toggle navigation menu"
            >
              {isMobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
            </button>
          </div>
        </div>

        {/* Mobile Navigation Dropdown */}
        {isMobileMenuOpen && (
          <div className="md:hidden border-t border-slate-200 bg-white/98 backdrop-blur-md px-4 py-4 space-y-3 shadow-lg">
            <button
              type="button"
              onClick={() => {
                setActiveTab('platform');
                setIsMobileMenuOpen(false);
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
              className="block w-full text-left py-2 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 rounded-lg"
            >
              Home
            </button>
            <button
              type="button"
              onClick={() => {
                setIsMobileMenuOpen(false);
                scrollToSection('features');
              }}
              className="block w-full text-left py-2 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 rounded-lg"
            >
              Features
            </button>
            <button
              type="button"
              onClick={() => {
                setIsMobileMenuOpen(false);
                scrollToSection('how-it-works');
              }}
              className="block w-full text-left py-2 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 rounded-lg"
            >
              How It Works
            </button>
            <button
              type="button"
              onClick={() => {
                setIsMobileMenuOpen(false);
                scrollToSection('why-lpg-chain');
              }}
              className="block w-full text-left py-2 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 rounded-lg"
            >
              About
            </button>
            <button
              type="button"
              onClick={() => {
                setIsMobileMenuOpen(false);
                handleTrackDeliveryClick('1');
              }}
              className="block w-full text-left py-2 px-3 text-sm font-semibold text-blue-600 hover:bg-blue-50 rounded-lg"
            >
              Live Tracking
            </button>

            {/* Authenticated user portal in mobile menu */}
            {currentUser && (currentUser.role === 'Consumer' || (currentUser as any).rawRole === 'CUSTOMER') && (
              <button
                type="button"
                onClick={() => {
                  setIsMobileMenuOpen(false);
                  setActiveTab('portal');
                }}
                className="block w-full text-left py-2 px-3 text-sm font-bold text-rose-600 bg-rose-50 rounded-lg"
              >
                Customer Portal
              </button>
            )}
            {currentUser && (currentUser.role === 'Distributor' || (currentUser as any).rawRole === 'DISTRIBUTOR') && (
              <button
                type="button"
                onClick={() => {
                  setIsMobileMenuOpen(false);
                  setActiveTab('distributor-portal');
                }}
                className="block w-full text-left py-2 px-3 text-sm font-bold text-blue-600 bg-blue-50 rounded-lg"
              >
                Distributor Portal
              </button>
            )}
            {currentUser && (currentUser.role === 'Admin' || (currentUser as any).rawRole === 'ADMIN') && (
              <button
                type="button"
                onClick={() => {
                  setIsMobileMenuOpen(false);
                  setActiveTab('admin-portal');
                }}
                className="block w-full text-left py-2 px-3 text-sm font-bold text-rose-600 bg-rose-50 rounded-lg"
              >
                Admin Portal
              </button>
            )}

            <div className="pt-2 border-t border-slate-100 flex flex-col gap-2">
              {currentUser ? (
                <>
                  <div className="flex items-center justify-between px-3 py-1.5 bg-slate-50 rounded-lg text-xs font-semibold text-slate-700">
                    <span>{currentUser.name}</span>
                    <span className="font-mono text-[10px] text-slate-500 bg-white px-2 py-0.5 rounded border border-slate-200">
                      {currentUser.role}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="w-full py-2 text-center text-xs font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 rounded-lg transition-colors"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setIsMobileMenuOpen(false);
                      setAuthModalMode('login');
                      setIsLoginOpen(true);
                    }}
                    className="py-2 text-center text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                  >
                    Login
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setIsMobileMenuOpen(false);
                      setAuthModalMode('register');
                      setIsLoginOpen(true);
                    }}
                    className="py-2 text-center text-xs font-bold text-white bg-rose-600 hover:bg-rose-500 rounded-lg shadow-sm transition-colors"
                  >
                    Get Started / Sign Up
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </header>

      {/* ── Main Content Area ── */}
      <main className="flex-1 w-full flex flex-col">
        {/* ══════════════════════════════════════════════════════════
            TAB 1: Platform & 3D Interactive Hero Landing Page
        ══════════════════════════════════════════════════════════ */}
        {activeTab === 'platform' && (
          <div className="w-full flex flex-col">
            <LPGChainHero
              onNavigateToTracking={handleTrackDeliveryClick}
              onOpenBookingTab={handleBookLpgClick}
              onNavigateToCapability={handleNavigateToCapability}
            />
            <LandingSections
              onNavigateToTracking={handleTrackDeliveryClick}
              onOpenBooking={handleBookLpgClick}
              currentUser={currentUser}
              onOpenLogin={() => {
                setPendingAuthAction('tracking');
                setIsLoginOpen(true);
              }}
              onNavigateToCapability={handleNavigateToCapability}
            />
          </div>
        )}

        {/* ══════════════════════════════════════════════════════════
            TAB 2: Live Vehicle Tracking (Preserved Phase 1 Console)
        ══════════════════════════════════════════════════════════ */}
        {activeTab === 'tracking' && (
          currentUser ? (
            <div className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col gap-6">
              <div className="flex items-center justify-between flex-wrap gap-2">
                <div>
                  <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
                    Real-Time Vehicle Tracking
                  </h1>
                  <p className="text-xs sm:text-sm text-slate-500">
                    Streaming GPS coordinates and delivery status via WebSocket STOMP
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setActiveTab('platform')}
                  className="text-xs font-semibold text-rose-600 hover:text-rose-700 bg-rose-50 px-3 py-1.5 rounded-xl border border-rose-200 transition-colors"
                >
                  &larr; Back to Platform Overview
                </button>
              </div>

              {/* Delivery Search & Token Control Panel */}
              <div className="bg-slate-900 text-white rounded-2xl p-4 sm:p-5 shadow-lg flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
                <form onSubmit={handleApplyDeliveryId} className="flex flex-wrap items-center gap-2.5">
                  <label className="text-xs uppercase font-semibold text-slate-400 tracking-wider flex items-center gap-1.5">
                    <MapPin size={14} className="text-blue-400" />
                    <span>Delivery ID:</span>
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      min="1"
                      value={deliveryIdInput}
                      onChange={(e) => setDeliveryIdInput(e.target.value)}
                      placeholder="e.g. 1"
                      className="w-28 sm:w-36 bg-slate-950 border border-slate-800 focus:border-blue-500 rounded-xl px-3 py-1.5 text-sm text-white font-mono focus:outline-none transition-colors"
                    />
                  </div>
                  <button
                    type="submit"
                    className="px-4 py-1.5 bg-blue-600 hover:bg-blue-500 text-white text-xs sm:text-sm font-semibold rounded-xl transition-colors flex items-center gap-1.5 shadow-md shadow-blue-600/30"
                  >
                    <Search size={14} />
                    <span>Track</span>
                  </button>

                  {/* Quick Presets */}
                  <div className="flex items-center gap-1.5 ml-1">
                    <span className="text-[11px] text-slate-400">Presets:</span>
                    {[1, 2, 10].map((id) => (
                      <button
                        key={id}
                        type="button"
                        onClick={() => {
                          setDeliveryIdInput(String(id));
                          setActiveDeliveryId(String(id));
                        }}
                        className={`px-2 py-1 text-xs rounded-lg font-mono border transition-colors ${
                          activeDeliveryId === String(id)
                            ? 'bg-blue-500/20 text-blue-400 border-blue-500/40'
                            : 'bg-slate-800/60 text-slate-400 hover:text-white border-slate-700/60'
                        }`}
                      >
                        #{id}
                      </button>
                    ))}
                  </div>
                </form>

                {/* Optional Auth Token Toggle */}
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowTokenSettings((prev) => !prev)}
                    className={`text-xs px-3 py-1.5 rounded-xl border flex items-center gap-1.5 transition-colors ${
                      tokenInput.trim()
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                        : 'bg-slate-800 text-slate-400 border-slate-700 hover:text-white'
                    }`}
                  >
                    <Key size={13} />
                    <span>{tokenInput.trim() ? 'JWT Token Set' : 'Add Auth Token'}</span>
                  </button>
                </div>
              </div>

              {/* Collapsible Auth Token Drawer */}
              {showTokenSettings && (
                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 text-xs space-y-3 text-white">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-slate-300">
                      Authentication Token (STOMP native header &amp; REST bearer)
                    </span>
                    <span className="text-slate-500 text-[11px]">Optional for public dev or mock setups</span>
                  </div>
                  <div className="flex gap-2">
                    <input
                      type="password"
                      value={tokenInput}
                      onChange={(e) => setTokenInput(e.target.value)}
                      placeholder="Paste JWT Bearer token here..."
                      className="flex-1 bg-slate-950 border border-slate-800 focus:border-blue-500 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={handleSaveToken}
                      className="px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-medium transition-colors"
                    >
                      Save
                    </button>
                    {tokenInput && (
                      <button
                        type="button"
                        onClick={() => {
                          setTokenInput('');
                          if (typeof window !== 'undefined') localStorage.removeItem('token');
                        }}
                        className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-400 rounded-xl transition-colors"
                      >
                        Clear
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* Complete Live Tracking Map (Leaflet) */}
              <LiveTrackingMap
                key={`tracking-${activeDeliveryId}`}
                deliveryId={activeDeliveryId}
                token={tokenInput.trim() || undefined}
                height="580px"
              />
            </div>
          ) : (
            <div className="max-w-2xl w-full mx-auto px-4 py-20 flex flex-col items-center justify-center text-center gap-6">
              <div className="w-16 h-16 rounded-2xl bg-rose-50 border border-rose-200 text-rose-600 flex items-center justify-center shadow-lg">
                <Lock size={30} />
              </div>
              <div className="space-y-2">
                <h2 className="text-2xl font-bold tracking-tight text-slate-900">
                  Authentication Required
                </h2>
                <p className="text-sm text-slate-600 max-w-md mx-auto leading-relaxed">
                  Real-time GPS vehicle tracking and active delivery routes are accessible only to authenticated consumers and fleet personnel.
                </p>
              </div>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setPendingAuthAction('tracking');
                    setIsLoginOpen(true);
                  }}
                  className="px-6 py-3 bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm rounded-xl shadow-md shadow-rose-600/30 transition-all flex items-center gap-2"
                >
                  <Truck size={16} />
                  <span>Login to View Live Tracking</span>
                  <ArrowRight size={14} />
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('platform')}
                  className="px-5 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm rounded-xl transition-colors"
                >
                  Back to Platform
                </button>
              </div>
            </div>
          )
        )}

        {/* ══════════════════════════════════════════════════════════
            TAB 3: Backend Status (Original Phase 1 Shell)
        ══════════════════════════════════════════════════════════ */}
        {activeTab === 'health' && (
          <div className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 flex flex-col items-center justify-center gap-6">
            <div className="text-center">
              <h2 className="text-2xl font-bold tracking-tight text-slate-900">
                Backend System Health
              </h2>
              <p className="mt-1 text-slate-500 text-sm">
                Validating connectivity with Spring Boot REST service
              </p>
            </div>

            <div className="bg-slate-900 text-white border border-slate-800 rounded-2xl p-6 w-full max-w-md shadow-lg">
              <div className="flex items-center gap-2 mb-4 text-slate-400 text-xs uppercase tracking-widest font-semibold">
                <Activity size={14} />
                Backend Status
              </div>

              {backendStatus === 'loading' && (
                <div className="flex items-center gap-3 text-yellow-400">
                  <Loader size={20} className="animate-spin" />
                  <span className="text-sm">Checking connection…</span>
                </div>
              )}

              {backendStatus === 'up' && healthData && (
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-3 text-emerald-400">
                    <CheckCircle size={20} />
                    <span className="text-sm font-medium">{healthData.status}</span>
                  </div>
                  <p className="text-slate-400 text-xs ml-8">{healthData.service}</p>
                </div>
              )}

              {backendStatus === 'down' && (
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-3 text-red-400">
                    <XCircle size={20} />
                    <span className="text-sm font-medium">Unreachable</span>
                  </div>
                  {healthError && (
                    <p className="text-slate-400 text-xs ml-8">{healthError}</p>
                  )}
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => setActiveTab('platform')}
              className="text-xs font-semibold text-rose-600 hover:underline"
            >
              &larr; Return to Platform
            </button>
          </div>
        )}

        {/* ══════════════════════════════════════════════════════════
            CAPABILITY DEMO PAGES (STAGE 2.5)
        ══════════════════════════════════════════════════════════ */}
        {activeTab === 'sentinel' && (
          <AISentinelView
            onBackToHome={() => {
              setActiveTab('platform');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onOpenBooking={handleBookLpgClick}
            onNavigateToTracking={() => handleTrackDeliveryClick('1')}
          />
        )}

        {activeTab === 'kyc' && (
          <AIKYCView
            onBackToHome={() => {
              setActiveTab('platform');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onOpenBooking={handleBookLpgClick}
            onNavigateToTracking={() => handleTrackDeliveryClick('1')}
          />
        )}

        {activeTab === 'analytics' && (
          <AnalyticsView
            onBackToHome={() => {
              setActiveTab('platform');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onOpenBooking={handleBookLpgClick}
            onNavigateToTracking={() => handleTrackDeliveryClick('1')}
          />
        )}

        {activeTab === 'security' && (
          <SecurityView
            onBackToHome={() => {
              setActiveTab('platform');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onOpenBooking={handleBookLpgClick}
            onNavigateToTracking={() => handleTrackDeliveryClick('1')}
          />
        )}

        {activeTab === 'distributor' && (
          <DistributorNetworkView
            onBackToHome={() => {
              setActiveTab('platform');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onOpenBooking={handleBookLpgClick}
            onNavigateToTracking={() => handleTrackDeliveryClick('1')}
          />
        )}
      </main>

      {/* ── Global Header Booking Modal ── */}
      <BookingModal
        isOpen={isHeaderBookingOpen}
        onClose={() => setIsHeaderBookingOpen(false)}
        onTrackOrder={(deliveryId) => {
          setIsHeaderBookingOpen(false);
          handleNavigateToTracking(deliveryId);
        }}
      />

      {/* ── Real Authentication Login Modal ── */}
      <LoginModal
        isOpen={isLoginOpen}
        onClose={() => {
          setIsLoginOpen(false);
          setPendingAuthAction(null);
        }}
        onLoginSuccess={handleLoginSuccess}
        initialRole={loginInitialRole}
        initialMode={authModalMode}
      />

      {/* ── Global Footer ── */}
      <footer className="border-t border-slate-200 bg-slate-50 py-6 text-center text-slate-500 text-xs">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            <span>LPG-Chain Core Intelligence Platform &bull; v1.0.0</span>
          </div>
          <div>
            &copy; {new Date().getFullYear()} LPG-Chain. Real-Time Telemetry &bull; Safety Verified.
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
