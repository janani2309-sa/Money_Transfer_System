import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface UserSession {
  username: string;
  token: string;
  accountNumber: string;
}

export interface AccountDetails {
  id: number;
  accountNumber: string;
  accountType: 'SAVINGS' | 'BUSINESS' | 'SALARY';
  firstName: string;
  lastName: string;
  balance: number;
  status: string;
  openedDate: string;
  lastUpdated: string;
}

export interface UserProfile {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  accounts: AccountDetails[];
}

export interface ToastData {
  message: string;
  type: 'success' | 'danger' | 'info';
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private readonly sessionKey = 'money_transfer_session';
  private readonly sessionIdKey = 'money_transfer_session_id';
  private readonly tabIdKey = 'money_transfer_tab_id';

  // Signals
  private readonly sessionSignal = signal<UserSession | null>(this.loadSession());
  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  
  private readonly userProfileSignal = signal<UserProfile | null>(this.loadUserProfile());
  readonly userProfile = this.userProfileSignal.asReadonly();

  private readonly activeAccountSignal = signal<AccountDetails | null>(this.loadActiveAccount());
  readonly activeAccount = this.activeAccountSignal.asReadonly();

  readonly currentAccountNumber = computed(() => this.activeAccountSignal()?.accountNumber ?? null);

  // Global Toast Signal
  readonly toast = signal<ToastData | null>(null);

  // Theme Signal
  private readonly themeSignal = signal<'dark' | 'light'>(this.loadTheme());
  readonly theme = this.themeSignal.asReadonly();

  // Inactivity tracking properties
  private idleTimer: any = null;
  private activityListeners: Array<() => void> = [];

  constructor(private http: HttpClient, private router: Router) {
    this.setupStorageEventListener();
    this.applyTheme(this.themeSignal());
    
    // Resume activity tracking if already authenticated on page load
    if (this.isAuthenticated()) {
      setTimeout(() => this.startActivityTracking(), 50);
    }
  }

  toggleTheme(): void {
    const newTheme = this.themeSignal() === 'dark' ? 'light' : 'dark';
    this.themeSignal.set(newTheme);
    localStorage.setItem('money_transfer_theme', newTheme);
    this.applyTheme(newTheme);
  }

  private loadTheme(): 'dark' | 'light' {
    const saved = localStorage.getItem('money_transfer_theme');
    if (saved === 'dark' || saved === 'light') {
      return saved;
    }
    return 'dark'; // default
  }

  private applyTheme(theme: 'dark' | 'light'): void {
    const body = document.body;
    if (theme === 'light') {
      body.classList.add('light-theme');
    } else {
      body.classList.remove('light-theme');
    }
  }

  /**
   * Helper to display a toast notification.
   */
  showToast(message: string, type: 'success' | 'danger' | 'info' = 'info'): void {
    this.toast.set({ message, type });
    setTimeout(() => {
      // Auto-clear after 4 seconds
      if (this.toast()?.message === message) {
        this.toast.set(null);
      }
    }, 4000);
  }

  /**
   * Performs basic authentication by calling /users/profile.
   * If successful, saves credentials, sets unique session ID, and triggers tracking.
   */
  login(username: string, password: string): Observable<UserProfile> {
    const token = btoa(`${username}:${password}`);
    const headers = new HttpHeaders({
      Authorization: `Basic ${token}`,
    });

    return this.http.get<UserProfile>(`${this.baseUrl}/users/profile`, { headers }).pipe(
      tap((userProfile: UserProfile) => {
        // Generate a new unique session identifier
        const uniqueSessionId = crypto.randomUUID();

        // 1. Save Tab-level Session Marker
        sessionStorage.setItem(this.tabIdKey, uniqueSessionId);

        // Find default/first active account
        const activeAcc = userProfile.accounts && userProfile.accounts.length > 0 ? userProfile.accounts[0] : null;

        // 2. Save Global Session Markers
        const session: UserSession = {
          username: username,
          token: token,
          accountNumber: activeAcc ? activeAcc.accountNumber : '',
        };
        localStorage.setItem(this.sessionKey, JSON.stringify(session));
        localStorage.setItem(this.sessionIdKey, uniqueSessionId);
        localStorage.setItem('money_transfer_user_profile', JSON.stringify(userProfile));

        // 3. Update Signal State
        this.sessionSignal.set(session);
        this.userProfileSignal.set(userProfile);
        this.activeAccountSignal.set(activeAcc);

        // 4. Start Inactivity Monitor
        this.startActivityTracking();

        // Show Toast
        this.showToast('Successfully logged in', 'success');
      })
    );
  }

  /**
   * Logs out the user and clears all session items.
   */
  logout(): void {
    const wasAuthenticated = this.isAuthenticated();
    
    // Clear Storage
    localStorage.removeItem(this.sessionKey);
    localStorage.removeItem(this.sessionIdKey);
    localStorage.removeItem('money_transfer_user_profile');
    sessionStorage.removeItem(this.tabIdKey);
    
    this.sessionSignal.set(null);
    this.userProfileSignal.set(null);
    this.activeAccountSignal.set(null);
    this.stopActivityTracking();

    if (wasAuthenticated) {
      this.showToast('Successfully logged out', 'success');
    }
  }

  /**
   * Switches the active selected account.
   */
  setActiveAccount(accountNumber: string): void {
    const profile = this.userProfileSignal();
    if (!profile) return;
    const account = profile.accounts.find(a => a.accountNumber === accountNumber);
    if (account) {
      this.activeAccountSignal.set(account);
      const session = this.sessionSignal();
      if (session) {
        session.accountNumber = accountNumber;
        localStorage.setItem(this.sessionKey, JSON.stringify(session));
      }
    }
  }

  /**
   * Refreshes the user profile and accounts list in memory.
   */
  refreshProfile(): Observable<UserProfile> {
    const headers = new HttpHeaders({
      Authorization: `Basic ${this.getToken()}`,
    });
    return this.http.get<UserProfile>(`${this.baseUrl}/users/profile`, { headers }).pipe(
      tap((userProfile: UserProfile) => {
        localStorage.setItem('money_transfer_user_profile', JSON.stringify(userProfile));
        this.userProfileSignal.set(userProfile);
        
        // Sync active account
        const active = this.activeAccountSignal();
        const found = userProfile.accounts.find(a => a.accountNumber === active?.accountNumber);
        if (found) {
          this.activeAccountSignal.set(found);
        } else if (userProfile.accounts.length > 0) {
          this.setActiveAccount(userProfile.accounts[0].accountNumber);
        }
      })
    );
  }

  /**
   * Deletes and anonymizes the logged-in user profile, then logs out.
   */
  deleteProfile(): Observable<any> {
    const headers = new HttpHeaders({
      Authorization: `Basic ${this.getToken()}`,
    });
    return this.http.delete(`${this.baseUrl}/users/profile`, { headers });
  }

  /**
   * Signup endpoint wrapper.
   */
  signup(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/signup`, data);
  }

  /**
   * OTP verification endpoint wrapper.
   */
  verifyOtp(username: string, otp: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/verify-otp`, { username, otp });
  }

  /**
   * Resend OTP endpoint wrapper.
   */
  resendOtp(username: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/resend-otp`, { username });
  }

  /**
   * Helper to retrieve token for HTTP interceptor.
   */
  getToken(): string | null {
    return this.sessionSignal()?.token ?? null;
  }

  /**
   * Validates if the current tab owns the active session.
   * If not, terminates the session globally and triggers redirect.
   */
  validateTabSession(): boolean {
    if (!this.isAuthenticated()) {
      return false;
    }

    const tabId = sessionStorage.getItem(this.tabIdKey);
    const activeSessionId = localStorage.getItem(this.sessionIdKey);

    if (!tabId || !activeSessionId || tabId !== activeSessionId) {
      // Current tab does not match the active session. Invalidate globally!
      this.logout();
      this.showToast('Session invalidated. Only one tab/session is allowed.', 'danger');
      return false;
    }

    return true;
  }

  /**
   * Activity tracking setup (45 seconds).
   */
  startActivityTracking(): void {
    this.stopActivityTracking();
    this.resetIdleTimer();

    const events = ['mousemove', 'keypress', 'click', 'scroll', 'touchstart'];
    const handleActivity = () => this.resetIdleTimer();

    events.forEach((event) => {
      window.addEventListener(event, handleActivity, { passive: true });
    });

    this.activityListeners = events.map((event) => {
      return () => window.removeEventListener(event, handleActivity);
    });
  }

  stopActivityTracking(): void {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
      this.idleTimer = null;
    }
    this.activityListeners.forEach((removeListener) => removeListener());
    this.activityListeners = [];
  }

  private resetIdleTimer(): void {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
    }
    this.idleTimer = setTimeout(() => {
      this.handleInactivityTimeout();
    }, 45000); // 45 seconds timeout
  }

  private handleInactivityTimeout(): void {
    this.logout();
    this.showToast('Session timed out due to inactivity', 'danger');
    this.router.navigate(['/login']);
  }

  /**
   * Listen to storage events to force-logout this tab if another tab logs in/out.
   */
  private setupStorageEventListener(): void {
    window.addEventListener('storage', (event) => {
      if (event.key === this.sessionIdKey) {
        const activeSessionId = event.newValue;
        const tabId = sessionStorage.getItem(this.tabIdKey);

        if (!activeSessionId || activeSessionId !== tabId) {
          // The active session ID was updated or cleared by another tab. Force logout this tab.
          this.sessionSignal.set(null);
          this.userProfileSignal.set(null);
          this.activeAccountSignal.set(null);
          this.stopActivityTracking();
          sessionStorage.removeItem(this.tabIdKey);
          
          this.showToast('Session terminated. Logged in from another tab or signed out.', 'danger');
          this.router.navigate(['/login']);
        }
      }
    });
  }

  private loadSession(): UserSession | null {
    const data = localStorage.getItem(this.sessionKey);
    if (!data) return null;
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }

  private loadUserProfile(): UserProfile | null {
    const data = localStorage.getItem('money_transfer_user_profile');
    if (!data) return null;
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }

  private loadActiveAccount(): AccountDetails | null {
    const profile = this.loadUserProfile();
    const session = this.loadSession();
    if (!profile || !session) return null;
    return profile.accounts.find(a => a.accountNumber === session.accountNumber) || profile.accounts[0] || null;
  }
}
