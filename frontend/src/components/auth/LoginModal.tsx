import React, { useState } from 'react';
import {
  X,
  User,
  Lock,
  Mail,
  Phone,
  MapPin,
  ArrowRight,
  CheckCircle2,
  Flame,
  AlertCircle,
  ShieldCheck,
  UserPlus,
  LogIn,
  ChevronRight,
  Loader2,
} from 'lucide-react';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential?: string; select_by?: string }) => void;
            auto_select?: boolean;
            cancel_on_tap_outside?: boolean;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: {
              type?: 'standard' | 'icon';
              theme?: 'outline' | 'filled_blue' | 'filled_black';
              size?: 'large' | 'medium' | 'small';
              text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin';
              shape?: 'rectangular' | 'pill' | 'circle' | 'square';
              logo_alignment?: 'left' | 'center';
              width?: number | string;
              locale?: string;
            }
          ) => void;
          prompt: (notification?: (notification: any) => void) => void;
          cancel: () => void;
        };
      };
    };
  }
}

export interface UserProfile {
  id?: number;
  name: string;
  role: 'Consumer' | 'Distributor' | 'Admin' | 'Driver';
  rawRole?: string;
  email: string;
  identifier: string;
  quotaLeft?: number;
  hubName?: string;
  token?: string;
  distributorId?: number;
  roles?: string[];
}

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLoginSuccess: (user: UserProfile) => void;
  initialRole?: 'Consumer' | 'Distributor' | 'Driver' | 'Admin';
  initialMode?: 'login' | 'register';
}

export const LoginModal: React.FC<LoginModalProps> = ({
  isOpen,
  onClose,
  onLoginSuccess,
  initialMode = 'login',
}) => {
  const [mode, setMode] = useState<'login' | 'register'>(initialMode);
  
  // Shared Login State
  const [emailOrId, setEmailOrId] = useState('');
  const [password, setPassword] = useState('');
  
  // Customer Registration State
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regAddress, setRegAddress] = useState('');

  // Multi-Role Selection State (returned securely by backend when user has multiple roles)
  const [multiRoles, setMultiRoles] = useState<string[]>([]);
  const [unscopedToken, setUnscopedToken] = useState<string | null>(null);
  const [pendingUserContext, setPendingUserContext] = useState<{ id?: number; name: string; email: string } | null>(null);

  // Status & Validation
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Google Identity Services State & Ref
  const googleBtnContainerRef = React.useRef<HTMLDivElement>(null);
  const [isGoogleGsiLoaded, setIsGoogleGsiLoaded] = useState(false);

  // Handle genuine Google credential response (OpenID Connect ID Token)
  const handleGoogleCredentialResponse = async (response: { credential?: string }) => {
    const idToken = response?.credential;
    if (!idToken || !idToken.trim()) {
      setErrorMessage('No Google credential returned. Please try signing in again.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const res = await fetch('/api/auth/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken: idToken.trim() }),
      });

      const data = await res.json().catch(() => null);

      if (res.ok && data?.token) {
        completeLoginSession(data, data.activeRole || 'CUSTOMER');
      } else {
        setErrorMessage(data?.message || 'Google token validation failed.');
      }
    } catch {
      setErrorMessage('Unable to connect to Google OAuth service. Please ensure the backend is running.');
    } finally {
      setIsSubmitting(false);
    }
  };

  React.useEffect(() => {
    if (isOpen) {
      setMode(initialMode);
      setErrorMessage(null);
      setSuccessMessage(null);
      setMultiRoles([]);
      setUnscopedToken(null);
      setPendingUserContext(null);
    }
  }, [isOpen, initialMode]);

  // Initialize and render Google Identity Services (GIS) Button
  React.useEffect(() => {
    if (!isOpen || mode !== 'login') return;

    const clientId = (import.meta as any).env?.VITE_GOOGLE_CLIENT_ID;
    if (!clientId) return;

    let checkInterval: any = null;

    const tryInitGsi = () => {
      if (typeof window !== 'undefined' && window.google?.accounts?.id && googleBtnContainerRef.current) {
        try {
          window.google.accounts.id.initialize({
            client_id: clientId,
            callback: handleGoogleCredentialResponse,
            auto_select: false,
            cancel_on_tap_outside: true,
          });

          googleBtnContainerRef.current.innerHTML = '';

          window.google.accounts.id.renderButton(googleBtnContainerRef.current, {
            type: 'standard',
            theme: 'outline',
            size: 'large',
            text: 'signin_with',
            shape: 'rectangular',
            logo_alignment: 'left',
            width: 320,
          });

          setIsGoogleGsiLoaded(true);
          return true;
        } catch (err) {
          console.warn('Google Identity Services initialization warning:', err);
        }
      }
      return false;
    };

    if (!tryInitGsi()) {
      let attempts = 0;
      checkInterval = setInterval(() => {
        attempts++;
        if (tryInitGsi() || attempts > 20) {
          clearInterval(checkInterval);
        }
      }, 250);
    }

    return () => {
      if (checkInterval) clearInterval(checkInterval);
    };
  }, [isOpen, mode]);

  if (!isOpen) return null;

  // ----------------------------------------------------------------
  // Helper: map backend verified role to frontend portal representation
  // ----------------------------------------------------------------
  const normalizeRole = (role: string): 'Consumer' | 'Distributor' | 'Admin' => {
    const clean = role.replace(/^ROLE_/, '').toUpperCase();
    if (clean === 'ADMIN') return 'Admin';
    if (clean === 'DISTRIBUTOR') return 'Distributor';
    return 'Consumer';
  };

  // ----------------------------------------------------------------
  // 1. Shared Unified Login Handler
  // ----------------------------------------------------------------
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const trimmedEmail = emailOrId.trim();
    if (!trimmedEmail) {
      setErrorMessage('Please enter your email or registered account identifier.');
      return;
    }
    if (!password) {
      setErrorMessage('Please enter your password.');
      return;
    }

    setIsSubmitting(true);

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: trimmedEmail, password }),
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        setErrorMessage(data?.message || 'Invalid email or password. Please verify your credentials.');
        setIsSubmitting(false);
        return;
      }

      // Check if backend mandates role selection for multi-role accounts
      if (data.roleSelectionRequired && Array.isArray(data.roles) && data.roles.length > 1) {
        setMultiRoles(data.roles);
        setUnscopedToken(data.token);
        setPendingUserContext({
          id: data.userId,
          name: data.name || trimmedEmail,
          email: data.email || trimmedEmail,
        });
        setIsSubmitting(false);
        return;
      }

      // Single active role returned by backend
      const verifiedActiveRole = data.activeRole || (data.roles && data.roles[0]) || 'CUSTOMER';
      completeLoginSession(data, verifiedActiveRole);
    } catch {
      setErrorMessage('Unable to connect to the authentication server. Please ensure the backend is running.');
      setIsSubmitting(false);
    }
  };

  // ----------------------------------------------------------------
  // 2. Multi-Role Selection Handler (via POST /api/auth/select-role)
  // ----------------------------------------------------------------
  const handleSelectRole = async (selectedRole: string) => {
    if (!unscopedToken) {
      setErrorMessage('Session expired. Please log in again.');
      setMultiRoles([]);
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const res = await fetch('/api/auth/select-role', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${unscopedToken}`,
        },
        body: JSON.stringify({ role: selectedRole }),
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        setErrorMessage(data?.message || `Failed to activate ${selectedRole} role session.`);
        setIsSubmitting(false);
        return;
      }

      completeLoginSession(data, selectedRole);
    } catch {
      setErrorMessage('Communication error while activating role session.');
      setIsSubmitting(false);
    }
  };

  // ----------------------------------------------------------------
  // 3. Complete and Store Verified Login Session
  // ----------------------------------------------------------------
  const completeLoginSession = (data: { token?: string; name?: string; email?: string; userId?: number; activeDistributorId?: number; roles?: string[] }, activeRole: string) => {
    if (!data.token) {
      setErrorMessage('Security token missing in server response.');
      setIsSubmitting(false);
      return;
    }

    const normalizedRole = normalizeRole(activeRole);
    const resolvedName = data.name || (pendingUserContext?.name) || 'LPG-Chain User';
    const resolvedEmail = data.email || (pendingUserContext?.email) || emailOrId.trim();

    const user: UserProfile = {
      id: data.userId || pendingUserContext?.id,
      name: resolvedName,
      email: resolvedEmail,
      identifier: resolvedEmail,
      role: normalizedRole,
      rawRole: activeRole,
      token: data.token,
      distributorId: data.activeDistributorId || undefined,
      roles: data.roles || [activeRole],
      quotaLeft: normalizedRole === 'Consumer' ? 11 : undefined,
      hubName: normalizedRole === 'Distributor' ? 'Authorized Distributor Agency' : undefined,
    };

    if (typeof window !== 'undefined') {
      localStorage.setItem('lpg_user', JSON.stringify(user));
      localStorage.setItem('token', data.token);
      // Trigger profile sync for customer services
      window.dispatchEvent(new CustomEvent('lpg_profile_updated', { detail: user }));
    }

    setSuccessMessage(`Welcome back, ${resolvedName}! Signing in to ${normalizedRole} Portal...`);
    setIsSubmitting(false);

    setTimeout(() => {
      onLoginSuccess(user);
      onClose();
    }, 600);
  };

  // ----------------------------------------------------------------
  // 4. Customer Registration Handler (via POST /api/auth/register)
  // ----------------------------------------------------------------
  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const trimmedName = regName.trim();
    const trimmedEmail = regEmail.trim();

    if (!trimmedName) {
      setErrorMessage('Please enter your full name.');
      return;
    }
    if (!trimmedEmail) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }
    if (regPassword.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }
    if (regPassword !== regConfirmPassword) {
      setErrorMessage('Passwords do not match. Please re-enter.');
      return;
    }

    setIsSubmitting(true);

    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: trimmedName,
          email: trimmedEmail,
          password: regPassword,
          phone: regPhone.trim() || undefined,
          address: regAddress.trim() || undefined,
          role: 'CUSTOMER', // Explicitly customer; server rejects any privileged roles
        }),
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        setErrorMessage(data?.message || 'Registration failed. Please check your information.');
        setIsSubmitting(false);
        return;
      }

      // Registration successful: auto-login or transition to login
      setSuccessMessage('Customer account created successfully! Signing you in...');

      // Immediately log in using the newly created credentials
      const loginRes = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: trimmedEmail, password: regPassword }),
      });

      if (loginRes.ok) {
        const loginData = await loginRes.json();
        completeLoginSession(loginData, 'CUSTOMER');
      } else {
        // Fallback: transition to login tab with email populated
        setIsSubmitting(false);
        setEmailOrId(trimmedEmail);
        setPassword('');
        setMode('login');
        setSuccessMessage('Registration completed! Please sign in with your password.');
      }
    } catch {
      setErrorMessage('Unable to connect to the registration service. Please verify your network.');
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="login-modal-title"
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="relative w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden">
        
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-rose-600 text-white flex items-center justify-center shadow-md shadow-rose-600/30">
              <Flame size={18} />
            </div>
            <div>
              <h2 id="login-modal-title" className="text-base font-bold text-slate-900 leading-tight">
                {multiRoles.length > 0
                  ? 'Select Authorized Role'
                  : mode === 'login'
                  ? 'LPG-Chain Unified Portal Login'
                  : 'Create Customer Account'}
              </h2>
              <p className="text-[11px] text-slate-500">
                {multiRoles.length > 0
                  ? 'Multiple verified roles available for this account'
                  : mode === 'login'
                  ? 'Access Customer, Distributor, or Admin Portal'
                  : 'Register for domestic cylinder delivery & tracking'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Multi-Role Selection Screen */}
        {multiRoles.length > 0 ? (
          <div className="p-6 space-y-4">
            <p className="text-xs text-slate-600 leading-relaxed">
              Your credentials have been verified. Select the role session you wish to activate:
            </p>
            <div className="space-y-2">
              {multiRoles.map((role) => (
                <button
                  key={role}
                  type="button"
                  disabled={isSubmitting}
                  onClick={() => handleSelectRole(role)}
                  className="w-full p-3.5 rounded-2xl border border-slate-200 hover:border-rose-400 hover:bg-rose-50/50 flex items-center justify-between transition-all group text-left"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center font-bold text-xs">
                      {role.charAt(0)}
                    </div>
                    <div>
                      <div className="text-sm font-bold text-slate-900">
                        {role === 'CUSTOMER'
                          ? 'Customer Portal'
                          : role === 'DISTRIBUTOR'
                          ? 'Distributor Agency Portal'
                          : role === 'ADMIN'
                          ? 'Administrator Portal'
                          : `${role} Portal`}
                      </div>
                      <div className="text-[11px] text-slate-500">
                        {role === 'CUSTOMER'
                          ? 'Manage household cylinder bookings & delivery tracking'
                          : role === 'DISTRIBUTOR'
                          ? 'Manage agency depot stock, vehicles, and allocations'
                          : 'Platform supervision, analytics, and governance'}
                      </div>
                    </div>
                  </div>
                  <ChevronRight size={18} className="text-slate-400 group-hover:text-rose-600 transition-colors" />
                </button>
              ))}
            </div>

            {errorMessage && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-start gap-2.5">
                <AlertCircle size={16} className="text-rose-600 shrink-0 mt-0.5" />
                <div className="leading-relaxed">{errorMessage}</div>
              </div>
            )}
          </div>
        ) : mode === 'login' ? (
          /* ══════════════════════════════════════════════════════════
             ONE SHARED LOGIN FORM (Customer, Distributor, Admin)
          ══════════════════════════════════════════════════════════ */
          <form onSubmit={handleLoginSubmit} className="p-6 space-y-4">
            {/* Account Identifier Input */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Email or Account Identifier
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 pointer-events-none">
                  <Mail size={15} />
                </span>
                <input
                  type="text"
                  required
                  value={emailOrId}
                  onChange={(e) => setEmailOrId(e.target.value)}
                  placeholder="e.g. user@example.com"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-medium"
                />
              </div>
            </div>

            {/* Password Input */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Password
                </label>
              </div>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 pointer-events-none">
                  <Lock size={15} />
                </span>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-mono"
                />
              </div>
            </div>

            {/* Error & Success Messages */}
            {errorMessage && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-start gap-2.5 animate-in fade-in duration-150">
                <AlertCircle size={16} className="text-rose-600 shrink-0 mt-0.5" />
                <div className="leading-relaxed">{errorMessage}</div>
              </div>
            )}

            {successMessage && (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium flex items-start gap-2.5 animate-in fade-in duration-150">
                <CheckCircle2 size={16} className="text-emerald-600 shrink-0 mt-0.5" />
                <div className="leading-relaxed">{successMessage}</div>
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 bg-rose-600 hover:bg-rose-500 active:scale-[0.98] text-white font-bold text-sm rounded-xl shadow-lg shadow-rose-600/30 transition-all flex items-center justify-center gap-2 disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Authenticating...</span>
                </>
              ) : (
                <>
                  <LogIn size={16} />
                  <span>Sign In</span>
                  <ArrowRight size={15} />
                </>
              )}
            </button>

            {/* Google OAuth 2.0 Sign-In */}
            <div className="relative my-3">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-200" />
              </div>
              <div className="relative flex justify-center text-[10px] uppercase font-bold text-slate-400">
                <span className="bg-white px-2 tracking-wider">Or continue with</span>
              </div>
            </div>

            {/* Google Identity Services Container & Fallback */}
            <div className="w-full flex flex-col items-center justify-center min-h-[44px]">
              <div ref={googleBtnContainerRef} className="w-full flex justify-center" />
              {!isGoogleGsiLoaded && (
                <button
                  type="button"
                  disabled={isSubmitting}
                  onClick={() => {
                    const clientId = (import.meta as any).env?.VITE_GOOGLE_CLIENT_ID;
                    if (!clientId) {
                      setErrorMessage('Google Sign-In is optional. To enable, configure VITE_GOOGLE_CLIENT_ID in your frontend .env file.');
                      return;
                    }
                    if (window.google?.accounts?.id) {
                      window.google.accounts.id.prompt();
                    } else {
                      setErrorMessage('Google Identity Services is initializing or network is unavailable. You can sign in using email and password.');
                    }
                  }}
                  className="w-full py-2.5 px-4 bg-white border border-slate-300 hover:border-slate-400 hover:bg-slate-50 text-slate-700 font-bold text-xs rounded-xl shadow-sm transition-all flex items-center justify-center gap-2.5 disabled:opacity-70"
                >
                  <svg className="w-4 h-4" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                  </svg>
                  <span>Sign in with Google</span>
                </button>
              )}
            </div>

            {/* Toggle to Customer Registration */}
            <div className="pt-2 text-center text-xs text-slate-500 border-t border-slate-100">
              Don&apos;t have an account?{' '}
              <button
                type="button"
                onClick={() => {
                  setErrorMessage(null);
                  setSuccessMessage(null);
                  setMode('register');
                }}
                className="font-bold text-rose-600 hover:text-rose-700 hover:underline inline-flex items-center gap-1"
              >
                <span>Sign up as Customer</span>
                <UserPlus size={13} />
              </button>
            </div>
          </form>
        ) : (
          /* ══════════════════════════════════════════════════════════
             CUSTOMER REGISTRATION FORM (Public Customer Access Only)
          ══════════════════════════════════════════════════════════ */
          <form onSubmit={handleRegisterSubmit} className="p-6 space-y-3.5 max-h-[75vh] overflow-y-auto">
            {/* Full Name */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Full Name *
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 pointer-events-none">
                  <User size={15} />
                </span>
                <input
                  type="text"
                  required
                  value={regName}
                  onChange={(e) => setRegName(e.target.value)}
                  placeholder="e.g. Ramesh Kumar"
                  className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-medium"
                />
              </div>
            </div>

            {/* Email Address */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Email Address *
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 pointer-events-none">
                  <Mail size={15} />
                </span>
                <input
                  type="email"
                  required
                  value={regEmail}
                  onChange={(e) => setRegEmail(e.target.value)}
                  placeholder="e.g. ramesh.kumar@gmail.com"
                  className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-medium"
                />
              </div>
            </div>

            {/* Password */}
            <div className="grid grid-cols-2 gap-2.5">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Password *
                </label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400 pointer-events-none">
                    <Lock size={14} />
                  </span>
                  <input
                    type="password"
                    required
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    placeholder="Min 6 chars"
                    className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Confirm *
                </label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400 pointer-events-none">
                    <Lock size={14} />
                  </span>
                  <input
                    type="password"
                    required
                    value={regConfirmPassword}
                    onChange={(e) => setRegConfirmPassword(e.target.value)}
                    placeholder="Confirm password"
                    className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-mono"
                  />
                </div>
              </div>
            </div>

            {/* Contact Phone (Optional) */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Contact Phone <span className="text-slate-400 font-normal lowercase">(optional)</span>
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 pointer-events-none">
                  <Phone size={15} />
                </span>
                <input
                  type="tel"
                  value={regPhone}
                  onChange={(e) => setRegPhone(e.target.value)}
                  placeholder="+91 98201 00000"
                  className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-medium"
                />
              </div>
            </div>

            {/* Delivery Address (Optional) */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Primary Delivery Address <span className="text-slate-400 font-normal lowercase">(optional)</span>
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 pointer-events-none">
                  <MapPin size={15} />
                </span>
                <input
                  type="text"
                  value={regAddress}
                  onChange={(e) => setRegAddress(e.target.value)}
                  placeholder="Flat/House No., Street, City, Pincode"
                  className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-rose-500 transition-all font-medium"
                />
              </div>
            </div>

            {/* Security Notice */}
            <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200/80 text-[11px] text-slate-500 flex items-start gap-2">
              <ShieldCheck size={14} className="text-emerald-600 shrink-0 mt-0.5" />
              <span>Public registration provisions Customer accounts only. Distributor accounts require verified agency onboarding.</span>
            </div>

            {/* Error & Success Messages */}
            {errorMessage && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-start gap-2.5">
                <AlertCircle size={16} className="text-rose-600 shrink-0 mt-0.5" />
                <div className="leading-relaxed">{errorMessage}</div>
              </div>
            )}

            {successMessage && (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium flex items-start gap-2.5">
                <CheckCircle2 size={16} className="text-emerald-600 shrink-0 mt-0.5" />
                <div className="leading-relaxed">{successMessage}</div>
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-rose-600 hover:bg-rose-500 active:scale-[0.98] text-white font-bold text-sm rounded-xl shadow-lg shadow-rose-600/30 transition-all flex items-center justify-center gap-2 disabled:opacity-70"
            >
              {isSubmitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Registering Account...</span>
                </>
              ) : (
                <>
                  <UserPlus size={16} />
                  <span>Create Account</span>
                  <ArrowRight size={15} />
                </>
              )}
            </button>

            {/* Switch to Login */}
            <div className="pt-2 text-center text-xs text-slate-500 border-t border-slate-100">
              Already have an account?{' '}
              <button
                type="button"
                onClick={() => {
                  setErrorMessage(null);
                  setSuccessMessage(null);
                  setMode('login');
                }}
                className="font-bold text-rose-600 hover:text-rose-700 hover:underline inline-flex items-center gap-1"
              >
                <span>Sign In</span>
                <LogIn size={13} />
              </button>
            </div>
          </form>
        )}

        {/* Modal Footer */}
        <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <div className="flex items-center gap-1.5">
            <ShieldCheck size={14} className="text-emerald-600" />
            <span>256-Bit Encrypted Session</span>
          </div>
          <span>LPG-Chain Core Identity</span>
        </div>

      </div>
    </div>
  );
};
