import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard, loginGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuards', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const aSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'validateTabSession']);
    const rSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: aSpy },
        { provide: Router, useValue: rSpy }
      ]
    });

    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  describe('authGuard', () => {
    it('should return true if user is authenticated and has valid tab session', () => {
      authServiceSpy.isAuthenticated.and.returnValue(true);
      authServiceSpy.validateTabSession.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

      expect(result).toBeTrue();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should redirect to /login and return false if not authenticated', () => {
      authServiceSpy.isAuthenticated.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

      expect(result).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should redirect to /login and return false if session validation fails', () => {
      authServiceSpy.isAuthenticated.and.returnValue(true);
      authServiceSpy.validateTabSession.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

      expect(result).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('loginGuard', () => {
    beforeEach(() => {
      sessionStorage.clear();
      localStorage.clear();
    });

    it('should return true if user is not authenticated', () => {
      authServiceSpy.isAuthenticated.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));

      expect(result).toBeTrue();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should redirect to /dashboard and return false if user is authenticated with matching session', () => {
      authServiceSpy.isAuthenticated.and.returnValue(true);
      sessionStorage.setItem('money_transfer_tab_id', 'session-123');
      localStorage.setItem('money_transfer_session_id', 'session-123');

      const result = TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));

      expect(result).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    it('should return true if authenticated but session IDs do not match', () => {
      authServiceSpy.isAuthenticated.and.returnValue(true);
      sessionStorage.setItem('money_transfer_tab_id', 'session-123');
      localStorage.setItem('money_transfer_session_id', 'session-different');

      const result = TestBed.runInInjectionContext(() => loginGuard({} as any, {} as any));

      expect(result).toBeTrue();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });
});
