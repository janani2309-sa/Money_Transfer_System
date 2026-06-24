import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class ProfileComponent implements OnInit {
  // Expose user profile from AuthService
  userProfile: any;
  isLoading = signal<boolean>(false);

  // Modals state
  showCloseModal = signal<boolean>(false);
  showDeleteModal = signal<boolean>(false);
  
  // Close account details
  accountToClose = '';
  closureReasonType = 'NO_LONGER_NEEDED';
  customClosureReason = '';

  // Delete profile details
  deleteConfirmInput = '';

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {
    this.userProfile = this.authService.userProfile;
  }

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }
    this.refreshProfileData();
  }

  refreshProfileData(): void {
    this.isLoading.set(true);
    this.authService.refreshProfile().subscribe({
      next: () => this.isLoading.set(false),
      error: () => {
        this.isLoading.set(false);
        this.authService.showToast('Failed to load profile data.', 'danger');
      }
    });
  }

  // Close Account Modal Handlers
  openCloseAccountModal(accountNumber: string, balance: number): void {
    if (balance > 0) {
      this.authService.showToast(
        `Cannot close account. Balance must be Rs. 0.00. Please transfer remaining Rs. ${balance.toFixed(2)} first.`,
        'danger'
      );
      return;
    }
    this.accountToClose = accountNumber;
    this.closureReasonType = 'NO_LONGER_NEEDED';
    this.customClosureReason = '';
    this.showCloseModal.set(true);
  }

  closeCloseModal(): void {
    this.showCloseModal.set(false);
  }

  submitAccountClosure(): void {
    let finalReason = '';
    switch (this.closureReasonType) {
      case 'NO_LONGER_NEEDED':
        finalReason = 'No longer needed';
        break;
      case 'HIGH_FEES':
        finalReason = 'Fees are too high';
        break;
      case 'POOR_SUPPORT':
        finalReason = 'Poor customer service';
        break;
      case 'OTHER':
        finalReason = this.customClosureReason.trim() || 'Other reason';
        break;
    }

    this.isLoading.set(true);
    this.showCloseModal.set(false);

    this.accountService.closeAccount(this.accountToClose, finalReason).subscribe({
      next: () => {
        this.authService.showToast(
          `Account ${this.accountToClose} successfully closed.`,
          'success'
        );
        this.refreshProfileData();
      },
      error: (err) => {
        this.isLoading.set(false);
        const errMsg = err.error?.message || 'Failed to close account. Please try again.';
        this.authService.showToast(errMsg, 'danger');
      }
    });
  }

  // Delete Profile Modal Handlers
  openDeleteProfileModal(): void {
    this.deleteConfirmInput = '';
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
  }

  submitProfileDeletion(): void {
    const expectedUsername = this.userProfile()?.username;
    if (this.deleteConfirmInput !== expectedUsername) {
      this.authService.showToast(`Verification text mismatch. Please type "${expectedUsername}".`, 'danger');
      return;
    }

    this.isLoading.set(true);
    this.showDeleteModal.set(false);

    this.authService.deleteProfile().subscribe({
      next: () => {
        this.isLoading.set(false);
        this.authService.logout();
        this.authService.showToast('Your profile has been successfully anonymized and deleted.', 'success');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading.set(false);
        const errMsg = err.error?.message || 'Failed to delete profile. Please try again.';
        this.authService.showToast(errMsg, 'danger');
      }
    });
  }
}
