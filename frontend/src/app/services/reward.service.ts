import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RewardDetail {
  id: number;
  transactionId: string;
  fromAccountNumber: string;
  toAccountNumber: string;
  transactionAmount: number;
  pointsEarned: number;
  createdAt: string;
}

export interface RewardRedemption {
  id: number;
  pointsRedeemed: number;
  rewardItem: string;
  createdAt: string;
}

export interface RewardSummary {
  totalPoints: number;
  totalEarned: number;
  totalRedeemed: number;
  history: RewardDetail[];
  redemptions: RewardRedemption[];
}

export interface RedeemResponse {
  success: boolean;
  rewardItem: string;
  pointsRedeemed: number;
  remainingBalance: number;
}

@Injectable({
  providedIn: 'root',
})
export class RewardService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/rewards';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves the authenticated user's reward summary and history.
   */
  getMyRewards(): Observable<RewardSummary> {
    return this.http.get<RewardSummary>(`${this.baseUrl}/me`);
  }

  /**
   * Redeems user reward points for a random scratch card reward.
   */
  redeemRewards(points: number): Observable<RedeemResponse> {
    return this.http.post<RedeemResponse>(`${this.baseUrl}/redeem`, {
      pointsToRedeem: points,
    });
  }
}
