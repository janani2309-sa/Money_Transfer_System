import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RewardService, RewardSummary } from './reward.service';

describe('RewardService', () => {
  let service: RewardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RewardService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(RewardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch reward summary', () => {
    const dummySummary: RewardSummary = {
      totalPoints: 10,
      totalEarned: 10,
      totalRedeemed: 0,
      history: [
        {
          id: 1,
          transactionId: 'TX1',
          fromAccountNumber: 'APXAC1',
          toAccountNumber: 'APXAC2',
          transactionAmount: 1000,
          pointsEarned: 10,
          createdAt: '2026-06-23T12:00:00Z'
        }
      ],
      redemptions: []
    };

    service.getMyRewards().subscribe((res) => {
      expect(res).toEqual(dummySummary);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/rewards/me');
    expect(req.request.method).toBe('GET');
    req.flush(dummySummary);
  });

  it('should redeem rewards', () => {
    const mockRedeemResponse = {
      success: true,
      rewardItem: 'Rs. 500 Amazon Gift Card',
      pointsRedeemed: 500,
      remainingBalance: 100
    };

    service.redeemRewards(500).subscribe((res) => {
      expect(res).toEqual(mockRedeemResponse);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/rewards/redeem');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ pointsToRedeem: 500 });
    req.flush(mockRedeemResponse);
  });
});
