import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Inject Basic Auth header if token exists and targeting backend APIs
  if (token && (req.url.startsWith('http://localhost:8080/api/v1') || req.url.includes('/api/v1/'))) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Basic ${token}`,
      },
    });
    return next(cloned);
  }

  return next(req);
};
