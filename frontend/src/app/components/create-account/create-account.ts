import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-create-account',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './create-account.html',
  styleUrl: './create-account.css',
})
export class CreateAccountComponent implements OnInit {
  accountType = 'SAVINGS';
  initialDeposit = 1000;
  documentType = 'AADHAAR_CARD';
  documentNumber = '';
  agreedToTerms = false;

  isLoading = signal<boolean>(false);


  // Available types (filtered based on existing accounts)
  availableTypes = signal<string[]>(['SAVINGS', 'BUSINESS', 'SALARY']);

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.filterAvailableAccountTypes();
  }

  filterAvailableAccountTypes(): void {
    const profile = this.authService.userProfile();
    if (profile && profile.accounts) {
      const ownedTypes = profile.accounts.map(acc => acc.accountType);
      const remainingTypes = ['SAVINGS', 'BUSINESS', 'SALARY'].filter(
        type => !ownedTypes.includes(type as any)
      );
      this.availableTypes.set(remainingTypes);
      if (remainingTypes.length > 0) {
        this.accountType = remainingTypes[0];
      } else {
        this.authService.showToast('You already own all supported account types.', 'info');
      }
    }
  }

  onSubmit(): void {
    if (!this.documentNumber.trim()) {
      this.authService.showToast('Please provide your identity document number.', 'danger');
      return;
    }
    if (!this.agreedToTerms) {
      this.authService.showToast('You must accept the terms and conditions to open an account.', 'danger');
      return;
    }
    if (this.initialDeposit < 1000) {
      this.authService.showToast('Initial deposit must be at least 1,000.00 Rs.', 'danger');
      return;
    }
    if (this.initialDeposit > 10000000) {
      this.authService.showToast('Initial deposit cannot exceed 10,000,000.00 Rs (10 Million).', 'danger');
      return;
    }

    this.isLoading.set(true);

    const request = {
      accountType: this.accountType,
      initialDeposit: this.initialDeposit,
      documentType: this.documentType,
      documentNumber: this.documentNumber,
    };

    this.accountService.openAccount(request).subscribe({
      next: (newAcc: any) => {
        // Refresh the profile to get the newly created account
        this.authService.refreshProfile().subscribe({
          next: () => {
            this.isLoading.set(false);
            this.authService.showToast(
              `Successfully opened a new ${this.accountType} account!`,
              'success'
            );
            // Switch active account to the newly created one
            this.authService.setActiveAccount(newAcc.accountNumber);
            this.router.navigate(['/dashboard']);
          },
          error: () => {
            this.isLoading.set(false);
            this.router.navigate(['/dashboard']);
          }
        });
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.authService.showToast(err.error?.message || 'Failed to open account. Please verify details.', 'danger');
      },
    });
  }
}
