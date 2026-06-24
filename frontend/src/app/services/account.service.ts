import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/accounts';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves account details by Account Number.
   */
  getAccount(accountNumber: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/${accountNumber}`);
  }

  /**
   * Retrieves the current balance of an account.
   */
  getBalance(accountNumber: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/${accountNumber}/balance`);
  }

  /**
   * Retrieves the transaction history of an account.
   */
  getTransactions(accountNumber: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${accountNumber}/transactions`);
  }

  /**
   * Opens a new account for the logged-in user.
   */
  openAccount(request: { accountType: string, initialDeposit: number, documentType: string, documentNumber: string }): Observable<any> {
    return this.http.post(this.baseUrl, request);
  }

  /**
   * Verifies if an account number exists and retrieves the holder's name.
   */
  verifyAccountNumber(accountNumber: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/${accountNumber}/verify`);
  }

  /**
   * Closes an active bank account with a closure reason.
   */
  closeAccount(accountNumber: string, reason: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/${accountNumber}/close`, { reason });
  }

  /**
   * Downloads the PDF statement for a given account.
   */
  downloadStatementPdf(accountNumber: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${accountNumber}/statement/pdf`, {
      responseType: 'blob',
    });
  }
}
