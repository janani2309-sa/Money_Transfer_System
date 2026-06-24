import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, UserSession, UserProfile } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

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
        accountNumber: 'APXAC00001',
        accountType: 'SAVINGS',
        firstName: 'John',
        lastName: 'Doe',
        balance: 1000,
        status: 'ACTIVE',
        openedDate: '2026-06-23T12:00:00Z',
        lastUpdated: '2026-06-23T12:00:00Z'
      }
    ]
  };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();

    const rSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: rSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => {
    httpTestingController.verify();
    service.stopActivityTracking();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should load default dark theme and toggle to light', () => {
    expect(service.theme()).toBe('dark');
    service.toggleTheme();
    expect(service.theme()).toBe('light');
    expect(localStorage.getItem('money_transfer_theme')).toBe('light');
    service.toggleTheme();
    expect(service.theme()).toBe('dark');
    expect(localStorage.getItem('money_transfer_theme')).toBe('dark');
  });

  it('should show toast message and auto-clear', () => {
    jasmine.clock().install();
    service.showToast('Test Toast', 'success');
    expect(service.toast()).toEqual({ message: 'Test Toast', type: 'success' });
    jasmine.clock().tick(4000);
    expect(service.toast()).toBeNull();
    jasmine.clock().uninstall();
  });

  it('should authenticate user and store sessions', () => {
    service.login('testuser', 'password').subscribe(profile => {
      expect(profile).toEqual(dummyProfile);
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/users/profile');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Authorization')).toContain('Basic');
    req.flush(dummyProfile);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.userProfile()).toEqual(dummyProfile);
    expect(service.activeAccount()).toEqual(dummyProfile.accounts[0]);
    expect(service.currentAccountNumber()).toBe('APXAC00001');
  });

  it('should clear everything on logout', () => {
    (service as any).sessionSignal.set({ token: 'abc', username: 'testuser', accountNumber: 'APXAC00001' });
    (service as any).userProfileSignal.set(dummyProfile);
    (service as any).activeAccountSignal.set(dummyProfile.accounts[0]);
    localStorage.setItem('money_transfer_session', JSON.stringify({ token: 'abc', username: 'testuser', accountNumber: 'APXAC00001' }));
    localStorage.setItem('money_transfer_session_id', 'session123');
    localStorage.setItem('money_transfer_user_profile', JSON.stringify(dummyProfile));
    sessionStorage.setItem('money_transfer_tab_id', 'session123');

    expect(service.isAuthenticated()).toBeTrue();

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.userProfile()).toBeNull();
    expect(service.activeAccount()).toBeNull();
    expect(localStorage.getItem('money_transfer_session')).toBeNull();
    expect(localStorage.getItem('money_transfer_session_id')).toBeNull();
    expect(sessionStorage.getItem('money_transfer_tab_id')).toBeNull();
  });

  it('should switch active account', () => {
    const doubleAccountProfile = {
      ...dummyProfile,
      accounts: [
        ...dummyProfile.accounts,
        {
          id: 102,
          accountNumber: 'APXAC00002',
          accountType: 'BUSINESS' as const,
          firstName: 'John',
          lastName: 'Doe',
          balance: 5000,
          status: 'ACTIVE',
          openedDate: '2026-06-23T12:00:00Z',
          lastUpdated: '2026-06-23T12:00:00Z'
        }
      ]
    };
    
    (service as any).userProfileSignal.set(doubleAccountProfile);
    (service as any).sessionSignal.set({ token: 'abc', username: 'testuser', accountNumber: 'APXAC00001' });
    (service as any).activeAccountSignal.set(doubleAccountProfile.accounts[0]);

    expect(service.currentAccountNumber()).toBe('APXAC00001');

    service.setActiveAccount('APXAC00002');
    expect(service.currentAccountNumber()).toBe('APXAC00002');
  });

  it('should trigger logout on inactivity timeout', () => {
    jasmine.clock().install();
    spyOn(service, 'logout').and.callThrough();

    service.startActivityTracking();
    jasmine.clock().tick(45000);

    expect(service.logout).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    jasmine.clock().uninstall();
  });

  it('should validate tab session ownership correctly', () => {
    expect(service.validateTabSession()).toBeFalse();

    (service as any).sessionSignal.set({ token: 'abc', username: 'testuser', accountNumber: 'APXAC00001' });
    localStorage.setItem('money_transfer_session_id', 'id1');
    sessionStorage.setItem('money_transfer_tab_id', 'id2');
    
    spyOn(service, 'logout').and.callThrough();

    expect(service.validateTabSession()).toBeFalse();
    expect(service.logout).toHaveBeenCalled();
  });

  it('should sign up user', () => {
    const signupData = { username: 'user', password: 'password' };
    service.signup(signupData).subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/auth/signup');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(signupData);
    req.flush({ success: true });
  });

  it('should verify OTP', () => {
    service.verifyOtp('user', '123456').subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/auth/verify-otp');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'user', otp: '123456' });
    req.flush({ success: true });
  });

  it('should resend OTP', () => {
    service.resendOtp('user').subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/auth/resend-otp');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'user' });
    req.flush({ success: true });
  });

  it('should delete profile', () => {
    (service as any).sessionSignal.set({ token: 'abc', username: 'user', accountNumber: 'APXAC00001' });

    service.deleteProfile().subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/users/profile');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.headers.get('Authorization')).toBe('Basic abc');
    req.flush({ success: true });
  });

  it('should refresh profile', () => {
    (service as any).sessionSignal.set({ token: 'abc', username: 'user', accountNumber: 'APXAC00001' });
    (service as any).userProfileSignal.set(dummyProfile);

    service.refreshProfile().subscribe(profile => {
      expect(profile.username).toBe('testuser');
    });

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/users/profile');
    expect(req.request.method).toBe('GET');
    req.flush(dummyProfile);
  });
});
