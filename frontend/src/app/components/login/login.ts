import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  // Login fields
  username = '';
  password = '';

  // Tab control
  isSignUp = false;
  isOtpVerification = false;

  // Sign up fields
  signUpData = {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    username: '',
    password: '',
    confirmPassword: ''
  };

  // OTP verification fields
  otpCode = '';
  debugOtp = signal<string | null>(null);

  errorMessage = signal<string | null>(null);
  isLoading = signal<boolean>(false);

  constructor(private authService: AuthService, private router: Router) {}

  toggleMode(signUp: boolean): void {
    this.isSignUp = signUp;
    this.isOtpVerification = false;
    this.errorMessage.set(null);
    this.debugOtp.set(null);
  }

  onSubmitLogin(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage.set('Please fill out all required fields.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Invalid credentials. Please verify your username and password.');
        } else if (err.status === 403) {
          this.errorMessage.set('Account is not verified. Please contact support or sign up again.');
        } else {
          this.errorMessage.set('Authentication failed. Check backend connection.');
        }
      },
    });
  }

  onSubmitSignUp(): void {
    // Basic validation
    if (this.signUpData.password !== this.signUpData.confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const payload = {
      username: this.signUpData.username,
      password: this.signUpData.password,
      firstName: this.signUpData.firstName,
      lastName: this.signUpData.lastName,
      email: this.signUpData.email,
      phoneNumber: this.signUpData.phoneNumber
    };

    this.authService.signup(payload).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        this.isOtpVerification = true;
        this.debugOtp.set(res.debugOtp || null);
        this.authService.showToast('Verification code generated!', 'info');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Registration failed. Username/Email/Phone might already be registered.');
      }
    });
  }

  onSubmitVerifyOtp(): void {
    if (!this.otpCode.trim() || this.otpCode.length !== 6) {
      this.errorMessage.set('Please enter a valid 6-digit OTP code.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.verifyOtp(this.signUpData.username, this.otpCode).subscribe({
      next: (res: any) => {
        this.authService.showToast(res.message, 'success');
        
        // Auto-login on verification success
        this.authService.login(this.signUpData.username, this.signUpData.password).subscribe({
          next: () => {
            this.isLoading.set(false);
            this.router.navigate(['/dashboard']);
          },
          error: (err) => {
            this.isLoading.set(false);
            // Fallback: send back to sign-in page
            this.toggleMode(false);
            this.username = this.signUpData.username;
          }
        });
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Invalid OTP code. Please try again.');
      }
    });
  }

  onResendOtp(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.resendOtp(this.signUpData.username).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        this.debugOtp.set(res.debugOtp || null);
        this.authService.showToast('A new verification code has been generated.', 'success');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to resend verification code.');
      }
    });
  }
}
