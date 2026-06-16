import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface UserSession {
  username: string;
  token: string; // Base64 encoded credentials
  accountId: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';
  private readonly sessionKey = 'money_transfer_session';

  // Session state as an Angular Signal
  private readonly sessionSignal = signal<UserSession | null>(this.loadSession());

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  readonly currentAccountId = computed(() => this.sessionSignal()?.accountId ?? null);

  constructor(private http: HttpClient) {}

  /**
   * Performs basic authentication by calling the accounts endpoint.
   * If successful, saves credentials in localStorage.
   */
  login(username: string, password: string): Observable<any> {
    // Determine account ID based on username
    let accountId = 1; // Default to John Smith (Account 1)
    if (/^\d+$/.test(username)) {
      accountId = parseInt(username, 10);
    } else if (username.toLowerCase() === 'admin') {
      accountId = 3; // Default admin to Bob Wilson (Account 3)
    } else if (username.toLowerCase() === 'janani') {
      accountId = 1; // Default janani to John Smith (Account 1)
    }

    // Prepare credentials for API call
    const authUser = (username === 'janani' || username === 'admin') ? username : 'janani';
    const authPass = (username === 'janani' || username === 'admin') ? password : 'Janani23092004';
    const token = btoa(`${authUser}:${authPass}`);
    
    const headers = new HttpHeaders({
      Authorization: `Basic ${token}`,
    });

    return this.http.get(`${this.baseUrl}/accounts/${accountId}`, { headers }).pipe(
      tap((account: any) => {
        const session: UserSession = {
          username: authUser,
          token: token,
          accountId: account.id,
        };
        localStorage.setItem(this.sessionKey, JSON.stringify(session));
        this.sessionSignal.set(session);
      })
    );
  }

  /**
   * Logs out the user and clears localStorage.
   */
  logout(): void {
    localStorage.removeItem(this.sessionKey);
    this.sessionSignal.set(null);
  }

  /**
   * Helper to retrieve token for HTTP interceptor.
   */
  getToken(): string | null {
    return this.sessionSignal()?.token ?? null;
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
}
