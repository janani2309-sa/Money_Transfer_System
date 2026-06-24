import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { RewardsComponent } from './rewards';
import { AuthService } from '../../services/auth.service';
import { RewardService, RewardSummary } from '../../services/reward.service';

describe('RewardsComponent', () => {
  let component: RewardsComponent;
  let fixture: ComponentFixture<RewardsComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let rewardServiceSpy: jasmine.SpyObj<RewardService>;
  let router: Router;

  const dummySummary: RewardSummary = {
    totalPoints: 600,
    totalEarned: 600,
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

  beforeEach(async () => {
    const aSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'showToast']);
    const rSpy = jasmine.createSpyObj('RewardService', ['getMyRewards', 'redeemRewards']);

    await TestBed.configureTestingModule({
      imports: [RewardsComponent],
      providers: [
        { provide: AuthService, useValue: aSpy },
        { provide: RewardService, useValue: rSpy },
        provideRouter([])
      ]
    }).compileComponents();

    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    rewardServiceSpy = TestBed.inject(RewardService) as jasmine.SpyObj<RewardService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should redirect to /login on init if unauthenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(authServiceSpy.showToast).toHaveBeenCalledWith('No active session. Please log in.', 'danger');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should load rewards summary on init if authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    rewardServiceSpy.getMyRewards.and.returnValue(of(dummySummary));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(rewardServiceSpy.getMyRewards).toHaveBeenCalled();
    expect(component.rewardSummary()).toEqual(dummySummary);
    expect(component.isLoading()).toBeFalse();
  });

  it('should show error toast if fetch rewards fails', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    rewardServiceSpy.getMyRewards.and.returnValue(throwError(() => new Error('Fetch failed')));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Failed to fetch rewards details. Please try again.');
  });

  it('should prevent opening scratch card if points are insufficient', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    const lowSummary = { ...dummySummary, totalPoints: 100 };
    rewardServiceSpy.getMyRewards.and.returnValue(of(lowSummary));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.openScratchCardModal(500);
    expect(authServiceSpy.showToast).toHaveBeenCalledWith('Requires at least 500 points to scratch!', 'danger');
    expect(component.isModalOpen()).toBeFalse();
  });

  it('should allow opening scratch card if points are sufficient', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    rewardServiceSpy.getMyRewards.and.returnValue(of(dummySummary));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.openScratchCardModal(500);
    expect(component.activeCardTier()).toBe(500);
    expect(component.isModalOpen()).toBeTrue();
  });

  it('should redeem rewards via API on triggerRedeem', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    rewardServiceSpy.getMyRewards.and.returnValue(of(dummySummary));
    rewardServiceSpy.redeemRewards.and.returnValue(of({
      success: true,
      rewardItem: 'Rs. 500 Starbucks Voucher',
      pointsRedeemed: 500,
      remainingBalance: 100
    }));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.activeCardTier.set(500);
    component.triggerRedeem();

    expect(rewardServiceSpy.redeemRewards).toHaveBeenCalledWith(500);
    expect(component.revealedPrize()).toBe('Rs. 500 Starbucks Voucher');
    expect(authServiceSpy.showToast).toHaveBeenCalledWith('Reward claimed successfully!', 'success');
  });

  it('should handle redeem rewards error gracefully', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    rewardServiceSpy.getMyRewards.and.returnValue(of(dummySummary));
    rewardServiceSpy.redeemRewards.and.returnValue(throwError(() => ({ error: { message: 'Out of stock' } })));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.activeCardTier.set(500);
    component.triggerRedeem();

    expect(authServiceSpy.showToast).toHaveBeenCalledWith('Out of stock', 'danger');
    expect(component.isModalOpen()).toBeFalse();
  });

  it('should calculate pointsLeftToNextTier correctly', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    rewardServiceSpy.getMyRewards.and.returnValue(of({ ...dummySummary, totalPoints: 300 }));

    fixture = TestBed.createComponent(RewardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.pointsLeftToNextTier).toBe(200);
  });
});
