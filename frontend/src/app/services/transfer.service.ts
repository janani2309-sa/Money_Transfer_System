import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TransferRequestData {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
  idempotencyKey: string;
}

@Injectable({
  providedIn: 'root',
})
export class TransferService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/transfers';

  constructor(private http: HttpClient) {}

  /**
   * Executes a fund transfer.
   */
  transfer(data: TransferRequestData): Observable<any> {
    return this.http.post(this.baseUrl, data);
  }
}
