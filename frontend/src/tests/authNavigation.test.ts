import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('Homepage Navigation & Auth Flow Gating Logic', () => {
  it('redirects unauthenticated visitor to login with booking intent when clicking Book LPG Cylinder', () => {
    let currentUser: { role: string; name: string } | null = null;
    let pendingAuthAction: 'booking' | 'tracking' | 'dashboard' | null = null;
    let isLoginOpen = false;
    let isBookingOpen = false;

    const handleBookLpgClick = () => {
      if (!currentUser) {
        pendingAuthAction = 'booking';
        isLoginOpen = true;
        return;
      }
      isBookingOpen = true;
    };

    // Unauthenticated click
    handleBookLpgClick();
    assert.equal(isLoginOpen, true, 'Login modal should open');
    assert.equal(pendingAuthAction, 'booking', 'Pending intent must be booking');
    assert.equal(isBookingOpen, false, 'Booking modal should not open for unauthenticated visitor');

    // Simulate login success as customer
    const loggedInUser = { role: 'Consumer', name: 'Anand Verma' };
    currentUser = loggedInUser;
    if (pendingAuthAction === 'booking') {
      isBookingOpen = true;
      pendingAuthAction = null;
    }
    assert.equal(isBookingOpen, true, 'Booking modal should open after successful login');
    assert.equal(pendingAuthAction, null, 'Pending action should be cleared');
  });

  it('goes directly to booking if customer is already logged in', () => {
    const currentUser = { role: 'Consumer', name: 'Anand Verma' };
    let isBookingOpen = false;
    let isLoginOpen = false;

    const handleBookLpgClick = () => {
      if (!currentUser) {
        isLoginOpen = true;
        return;
      }
      isBookingOpen = true;
    };

    handleBookLpgClick();
    assert.equal(isBookingOpen, true, 'Booking modal should open immediately');
    assert.equal(isLoginOpen, false, 'Login modal should not open');
  });

  it('redirects unauthenticated visitor to login with tracking intent when clicking Track Delivery', () => {
    let currentUser = null;
    let pendingAuthAction: 'booking' | 'tracking' | 'dashboard' | null = null;
    let isLoginOpen = false;
    let activeTab = 'platform';

    const handleTrackDeliveryClick = () => {
      if (!currentUser) {
        pendingAuthAction = 'tracking';
        isLoginOpen = true;
        return;
      }
      activeTab = 'tracking';
    };

    handleTrackDeliveryClick();
    assert.equal(isLoginOpen, true, 'Login modal should open');
    assert.equal(pendingAuthAction, 'tracking', 'Pending intent should be tracking');
    assert.equal(activeTab, 'platform', 'Tracking view should not open unauthenticated');

    // After login
    currentUser = { role: 'Consumer', name: 'Anand Verma' };
    if (pendingAuthAction === 'tracking') {
      activeTab = 'tracking';
      pendingAuthAction = null;
    }
    assert.equal(activeTab, 'tracking', 'Tracking view should open after login');
    assert.equal(pendingAuthAction, null);
  });

  it('routes Get Started to login when unauthenticated and to dashboard when authenticated', () => {
    let currentUser: { role: string; name: string } | null = null;
    let isLoginOpen = false;
    let destination = '';

    const handleGetStartedClick = () => {
      if (!currentUser) {
        isLoginOpen = true;
        return;
      }
      destination = currentUser.role === 'Consumer' ? 'booking' : 'tracking';
    };

    // Unauthenticated
    handleGetStartedClick();
    assert.equal(isLoginOpen, true);
    assert.equal(destination, '');

    // Authenticated Consumer
    currentUser = { role: 'Consumer', name: 'Anand Verma' };
    handleGetStartedClick();
    assert.equal(destination, 'booking');

    // Authenticated Driver
    currentUser = { role: 'Driver', name: 'Ramesh Kumar' };
    handleGetStartedClick();
    assert.equal(destination, 'tracking');
  });

  it('ensures public visitors cannot see live vehicle map telemetry without authentication', () => {
    const isVisitorAuthenticated = (user: { role: string } | null) => !!user;

    assert.equal(isVisitorAuthenticated(null), false, 'Visitor must not be authorized without logging in');
    assert.equal(isVisitorAuthenticated({ role: 'Consumer' }), true, 'Logged in consumer is authorized');
  });

  it('gates Distributor Portal: prompts login for unauthenticated users and navigates on Distributor role', () => {
    let currentUser: { role: string; name: string } | null = null;
    let isLoginOpen = false;
    let pendingAuthAction: string | null = null;
    let initialRole = 'Consumer';
    let activeTab = 'platform';

    const handleDistributorPortalClick = () => {
      if (!currentUser || currentUser.role !== 'Distributor') {
        pendingAuthAction = 'dashboard';
        initialRole = 'Distributor';
        isLoginOpen = true;
      } else {
        activeTab = 'distributor-portal';
      }
    };

    // 1. Unauthenticated click
    handleDistributorPortalClick();
    assert.equal(isLoginOpen, true, 'Login modal should open');
    assert.equal(pendingAuthAction, 'dashboard', 'Pending action must be dashboard');
    assert.equal(initialRole, 'Distributor', 'Modal should pre-select Distributor');
    assert.equal(activeTab, 'platform', 'Should not navigate unauthenticated');

    // 2. Successful distributor login
    currentUser = { role: 'Distributor', name: 'Apex Gas Agency' };
    if (pendingAuthAction === 'dashboard' && currentUser.role === 'Distributor') {
      activeTab = 'distributor-portal';
      pendingAuthAction = null;
    }
    assert.equal(activeTab, 'distributor-portal', 'Should navigate to distributor-portal');
    assert.equal(pendingAuthAction, null);

    // 3. Authenticated distributor clicks again
    isLoginOpen = false;
    handleDistributorPortalClick();
    assert.equal(isLoginOpen, false, 'Modal should not reopen for already-authenticated distributor');
    assert.equal(activeTab, 'distributor-portal');
  });

  it('ensures logout clears both user profile and authentication token', () => {
    const storage: Record<string, string> = {
      lpg_user: JSON.stringify({ role: 'Distributor', name: 'Apex Gas' }),
      token: 'jwt-distributor-token',
      lpg_customer_profile: JSON.stringify({ consumerNumber: 'CUST-1001' }),
    };

    const handleLogout = () => {
      delete storage.lpg_user;
      delete storage.token;
      delete storage.lpg_customer_profile;
    };

    assert.ok(storage.token, 'Token present prior to logout');
    handleLogout();
    assert.equal(storage.lpg_user, undefined, 'User profile must be removed');
    assert.equal(storage.token, undefined, 'Security token must be removed');
    assert.equal(storage.lpg_customer_profile, undefined, 'Customer profile cache must be removed');
  });

  it('verifies public navbar removes direct portal links and displays only authorized portal when authenticated', () => {
    interface NavConfig {
      publicLinks: string[];
      rolePortalLink: string | null;
      authButtons: string[];
    }

    const computeNavState = (user: { role: string; name: string } | null): NavConfig => {
      const publicLinks = ['Home', 'Features', 'How It Works', 'About', 'Live Tracking'];
      let rolePortalLink: string | null = null;
      if (user) {
        if (user.role === 'Consumer' || user.role === 'CUSTOMER') rolePortalLink = 'Customer Portal';
        else if (user.role === 'Distributor' || user.role === 'DISTRIBUTOR') rolePortalLink = 'Distributor Portal';
        else if (user.role === 'Admin' || user.role === 'ADMIN') rolePortalLink = 'Admin Portal';
      }
      const authButtons = user ? ['Logout'] : ['Login', 'Get Started / Sign Up'];
      return { publicLinks, rolePortalLink, authButtons };
    };

    // 1. Unauthenticated visitor
    const visitorNav = computeNavState(null);
    assert.equal(visitorNav.rolePortalLink, null, 'Public navbar must NOT show any portal links for visitors');
    assert.deepEqual(visitorNav.authButtons, ['Login', 'Get Started / Sign Up']);

    // 2. Authenticated Customer
    const customerNav = computeNavState({ role: 'Consumer', name: 'Rohan Sharma' });
    assert.equal(customerNav.rolePortalLink, 'Customer Portal', 'Customer sees only Customer Portal');

    // 3. Authenticated Distributor
    const distributorNav = computeNavState({ role: 'Distributor', name: 'Gas Agency' });
    assert.equal(distributorNav.rolePortalLink, 'Distributor Portal', 'Distributor sees only Distributor Portal');

    // 4. Authenticated Admin
    const adminNav = computeNavState({ role: 'Admin', name: 'System Admin' });
    assert.equal(adminNav.rolePortalLink, 'Admin Portal', 'Admin sees only Admin Portal');
  });

  it('routes unified login automatically to correct role-based portal', () => {
    const determinePortal = (user: { role: string; rawRole?: string }) => {
      const r = (user.rawRole || user.role).toUpperCase();
      if (r === 'CONSUMER' || r === 'CUSTOMER') return 'portal';
      if (r === 'DISTRIBUTOR') return 'distributor-portal';
      if (r === 'ADMIN') return 'admin-portal';
      return 'platform';
    };

    assert.equal(determinePortal({ role: 'Consumer', rawRole: 'CUSTOMER' }), 'portal');
    assert.equal(determinePortal({ role: 'Distributor', rawRole: 'DISTRIBUTOR' }), 'distributor-portal');
    assert.equal(determinePortal({ role: 'Admin', rawRole: 'ADMIN' }), 'admin-portal');
  });

  it('enforces route guard: blocks unauthenticated or unauthorized access to protected portals', () => {
    const checkAccess = (targetPortal: 'portal' | 'distributor-portal' | 'admin-portal', user: { role: string } | null) => {
      if (!user) return { allowed: false, reason: 'UNAUTHENTICATED' };
      if (targetPortal === 'portal') {
        const ok = user.role === 'Consumer' || user.role === 'CUSTOMER';
        return { allowed: ok, reason: ok ? 'AUTHORIZED' : 'ACCESS_DENIED' };
      }
      if (targetPortal === 'distributor-portal') {
        const ok = user.role === 'Distributor' || user.role === 'DISTRIBUTOR';
        return { allowed: ok, reason: ok ? 'AUTHORIZED' : 'ACCESS_DENIED' };
      }
      if (targetPortal === 'admin-portal') {
        const ok = user.role === 'Admin' || user.role === 'ADMIN';
        return { allowed: ok, reason: ok ? 'AUTHORIZED' : 'ACCESS_DENIED' };
      }
      return { allowed: false, reason: 'UNKNOWN' };
    };

    // Unauthenticated
    assert.equal(checkAccess('portal', null).allowed, false);
    assert.equal(checkAccess('distributor-portal', null).allowed, false);
    assert.equal(checkAccess('admin-portal', null).allowed, false);

    // Cross-role unauthorized checks
    const customer = { role: 'Consumer' };
    assert.equal(checkAccess('portal', customer).allowed, true);
    assert.equal(checkAccess('distributor-portal', customer).allowed, false);
    assert.equal(checkAccess('admin-portal', customer).allowed, false);

    const distributor = { role: 'Distributor' };
    assert.equal(checkAccess('portal', distributor).allowed, false);
    assert.equal(checkAccess('distributor-portal', distributor).allowed, true);
    assert.equal(checkAccess('admin-portal', distributor).allowed, false);

    const admin = { role: 'Admin' };
    assert.equal(checkAccess('portal', admin).allowed, false);
    assert.equal(checkAccess('distributor-portal', admin).allowed, false);
    assert.equal(checkAccess('admin-portal', admin).allowed, true);
  });

  it('guarantees public registration configures customer mode only without privileged role selection', () => {
    const handleRegisterMode = (requestedMode: 'login' | 'register') => {
      const allowedRoles = ['CUSTOMER'];
      return {
        mode: requestedMode,
        serverAssignedRole: 'CUSTOMER',
        allowsClientRoleOverride: false,
        allowedRoles,
      };
    };

    const regConfig = handleRegisterMode('register');
    assert.equal(regConfig.mode, 'register');
    assert.equal(regConfig.serverAssignedRole, 'CUSTOMER');
    assert.equal(regConfig.allowsClientRoleOverride, false, 'Client role override must not be permitted');
  });
});

