import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { DashboardComponent } from './dashboard';
import { AuthService, UserProfile } from '../../services/auth.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const dummyProfile: UserProfile = {
    id: 1,
    username: 'testuser',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    phoneNumber: '1234567890',
    accounts: [
      {
        id: 101,
        accountNumber: 'APXAC1',
        accountType: 'SAVINGS',
        firstName: 'John',
        lastName: 'Doe',
        balance: 100,
        status: 'ACTIVE',
        openedDate: '2026-06-23T12:00:00Z',
        lastUpdated: '2026-06-23T12:00:00Z'
      },
      {
        id: 102,
        accountNumber: 'APXAC2',
        accountType: 'BUSINESS',
        firstName: 'John',
        lastName: 'Doe',
        balance: 200,
        status: 'ACTIVE',
        openedDate: '2026-06-23T12:00:00Z',
        lastUpdated: '2026-06-23T12:00:00Z'
      }
    ]
  };


  beforeEach(async () => {
    const aSpy = jasmine.createSpyObj('AuthService', [
      'isAuthenticated',
      'refreshProfile',
      'setActiveAccount',
      'logout',
      'showToast'
    ]);

    aSpy.userProfile = signal(dummyProfile);
    aSpy.activeAccount = signal(dummyProfile.accounts[0]);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: AuthService, useValue: aSpy },
        provideRouter([])
      ]
    }).compileComponents();

    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should redirect to /login on init if unauthenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(authServiceSpy.showToast).toHaveBeenCalledWith('No active session. Please log in.', 'danger');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should refresh dashboard data on init if authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.refreshProfile.and.returnValue(of(dummyProfile));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(authServiceSpy.refreshProfile).toHaveBeenCalled();
    expect(component.isLoading()).toBeFalse();
  });

  it('should redirect to /create-account if profile has no accounts', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    const profileNoAccounts: UserProfile = { ...dummyProfile, accounts: [] };
    authServiceSpy.refreshProfile.and.returnValue(of(profileNoAccounts));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/create-account']);
  });

  it('should show toast message if refreshProfile fails', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.refreshProfile.and.returnValue(throwError(() => new Error('DB Error')));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(authServiceSpy.showToast).toHaveBeenCalledWith('Failed to load dashboard data. Check database.', 'danger');
  });

  it('should switch active account', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.refreshProfile.and.returnValue(of(dummyProfile));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.switchAccount('APXAC2');
    expect(authServiceSpy.setActiveAccount).toHaveBeenCalledWith('APXAC2');
  });

  it('should identify if fewer than three accounts', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.refreshProfile.and.returnValue(of(dummyProfile));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.hasFewerThanThreeAccounts()).toBeTrue();
  });

  it('should logout and redirect to login', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.refreshProfile.and.returnValue(of(dummyProfile));

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.logout();

    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
