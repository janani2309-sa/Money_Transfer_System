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
   * Retrieves account details by ID.
   */
  getAccount(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}`);
  }

  /**
   * Retrieves the current balance of an account.
   */
  getBalance(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}/balance`);
  }

  /**
   * Retrieves the transaction history of an account.
   */
  getTransactions(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${id}/transactions`);
  }
}
