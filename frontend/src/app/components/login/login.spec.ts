import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    const aSpy = jasmine.createSpyObj('AuthService', [
      'login',
      'signup',
      'verifyOtp',
      'resendOtp',
      'showToast'
    ]);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, FormsModule],
      providers: [
        { provide: AuthService, useValue: aSpy },
        provideRouter([])
      ]
    }).compileComponents();

    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should toggle mode correctly', () => {
    component.toggleMode(true);
    expect(component.isSignUp).toBeTrue();
    expect(component.isOtpVerification).toBeFalse();
    expect(component.errorMessage()).toBeNull();
    expect(component.debugOtp()).toBeNull();

    component.toggleMode(false);
    expect(component.isSignUp).toBeFalse();
  });

  it('should show error if submitting empty login credentials', () => {
    component.username = '';
    component.password = '';
    component.onSubmitLogin();
    expect(component.errorMessage()).toBe('Please fill out all required fields.');
  });

  it('should redirect to dashboard on successful login', () => {
    component.username = 'user';
    component.password = 'pass';
    authServiceSpy.login.and.returnValue(of({} as any));

    component.onSubmitLogin();

    expect(authServiceSpy.login).toHaveBeenCalledWith('user', 'pass');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.isLoading()).toBeFalse();
  });

  it('should set invalid credentials error on 401 response', () => {
    component.username = 'user';
    component.password = 'pass';
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 401 })));

    component.onSubmitLogin();

    expect(component.errorMessage()).toBe('Invalid credentials. Please verify your username and password.');
    expect(component.isLoading()).toBeFalse();
  });

  it('should set unverified error on 403 response', () => {
    component.username = 'user';
    component.password = 'pass';
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 403 })));

    component.onSubmitLogin();

    expect(component.errorMessage()).toBe('Account is not verified. Please contact support or sign up again.');
  });

  it('should set fallback error on other login errors', () => {
    component.username = 'user';
    component.password = 'pass';
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 500 })));

    component.onSubmitLogin();

    expect(component.errorMessage()).toBe('Authentication failed. Check backend connection.');
  });

  it('should show error on signup if passwords do not match', () => {
    component.signUpData.password = '123';
    component.signUpData.confirmPassword = '456';
    component.onSubmitSignUp();
    expect(component.errorMessage()).toBe('Passwords do not match.');
  });

  it('should trigger signup and enter OTP mode on success', () => {
    component.signUpData.username = 'newuser';
    component.signUpData.password = '123';
    component.signUpData.confirmPassword = '123';
    authServiceSpy.signup.and.returnValue(of({ debugOtp: '999999' }));

    component.onSubmitSignUp();

    expect(authServiceSpy.signup).toHaveBeenCalled();
    expect(component.isOtpVerification).toBeTrue();
    expect(component.debugOtp()).toBe('999999');
    expect(authServiceSpy.showToast).toHaveBeenCalledWith('Verification code generated!', 'info');
  });

  it('should handle signup error gracefully', () => {
    component.signUpData.password = '123';
    component.signUpData.confirmPassword = '123';
    authServiceSpy.signup.and.returnValue(throwError(() => ({ error: { message: 'Username exists' } })));

    component.onSubmitSignUp();

    expect(component.errorMessage()).toBe('Username exists');
  });

  it('should show error if entering invalid OTP format', () => {
    component.otpCode = '12';
    component.onSubmitVerifyOtp();
    expect(component.errorMessage()).toBe('Please enter a valid 6-digit OTP code.');
  });

  it('should verify OTP and trigger auto-login on success', () => {
    component.signUpData.username = 'newuser';
    component.signUpData.password = '123';
    component.otpCode = '123456';
    authServiceSpy.verifyOtp.and.returnValue(of({ message: 'Verified' }));
    authServiceSpy.login.and.returnValue(of({} as any));

    component.onSubmitVerifyOtp();

    expect(authServiceSpy.verifyOtp).toHaveBeenCalledWith('newuser', '123456');
    expect(authServiceSpy.login).toHaveBeenCalledWith('newuser', '123');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should handle verify OTP failure', () => {
    component.otpCode = '123456';
    authServiceSpy.verifyOtp.and.returnValue(throwError(() => ({ error: { message: 'Wrong OTP' } })));

    component.onSubmitVerifyOtp();

    expect(component.errorMessage()).toBe('Wrong OTP');
  });

  it('should trigger OTP resend and show toast', () => {
    component.signUpData.username = 'newuser';
    authServiceSpy.resendOtp.and.returnValue(of({ debugOtp: '888888' }));

    component.onResendOtp();

    expect(authServiceSpy.resendOtp).toHaveBeenCalledWith('newuser');
    expect(component.debugOtp()).toBe('888888');
    expect(authServiceSpy.showToast).toHaveBeenCalledWith('A new verification code has been generated.', 'success');
  });
});
