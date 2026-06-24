import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpTestingController: HttpTestingController;
  let httpClient: HttpClient;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    const aSpy = jasmine.createSpyObj('AuthService', ['getToken']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: aSpy },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should add Authorization header if token exists and targeting backend API', () => {
    authServiceSpy.getToken.and.returnValue('mock-token-123');

    httpClient.get('http://localhost:8080/api/v1/users/me').subscribe();

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/users/me');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Basic mock-token-123');
    req.flush({});
  });

  it('should not add Authorization header if no token is returned', () => {
    authServiceSpy.getToken.and.returnValue(null);

    httpClient.get('http://localhost:8080/api/v1/users/me').subscribe();

    const req = httpTestingController.expectOne('http://localhost:8080/api/v1/users/me');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should not add Authorization header if URL does not target backend API', () => {
    authServiceSpy.getToken.and.returnValue('mock-token-123');

    httpClient.get('https://api.github.com/users').subscribe();

    const req = httpTestingController.expectOne('https://api.github.com/users');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
