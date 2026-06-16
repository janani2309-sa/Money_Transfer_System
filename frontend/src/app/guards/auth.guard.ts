import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    // Perform tab ownership checks
    if (authService.validateTabSession()) {
      return true;
    }
  }

  // Redirect to login if not authenticated or invalid tab session
  router.navigate(['/login']);
  return false;
};

export const loginGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    const tabId = sessionStorage.getItem('money_transfer_tab_id');
    const activeSessionId = localStorage.getItem('money_transfer_session_id');
    
    if (tabId && activeSessionId && tabId === activeSessionId) {
      router.navigate(['/dashboard']);
      return false;
    }
  }

  return true;
};
