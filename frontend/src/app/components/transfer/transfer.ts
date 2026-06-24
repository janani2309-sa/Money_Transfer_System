import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, AccountDetails } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { TransferService, TransferRequestData } from '../../services/transfer.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './transfer.html',
  styleUrl: './transfer.css',
})
export class TransferComponent implements OnInit {
  sourceAccount = signal<AccountDetails | null>(null);
  toAccountNumber = '';
  amount: number | null = null;
  idempotencyKey = '';
  
  isLoading = signal<boolean>(false);
  isFetchingAccount = signal<boolean>(true);
  


  // Recipient Verification Signals
  recipientName = signal<string | null>(null);
  isRecipientValid = signal<boolean>(false);
  isVerifyingRecipient = signal<boolean>(false);

  private verificationTimeout: any = null;
  
  // Modal state
  showConfirmation = signal<boolean>(false);
  userProfile: any;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private transferService: TransferService,
    private router: Router
  ) {
    this.userProfile = this.authService.userProfile;
  }

  ngOnInit(): void {
    const accountNumber = this.authService.currentAccountNumber();
    if (!accountNumber) {
      this.router.navigate(['/login']);
      return;
    }

    this.generateIdempotencyKey();
    this.fetchSourceAccount(accountNumber);
  }

  fetchSourceAccount(accountNumber: string): void {
    this.isFetchingAccount.set(true);
    this.accountService.getAccount(accountNumber).subscribe({
      next: (data: AccountDetails) => {
        this.sourceAccount.set(data);
        this.isFetchingAccount.set(false);
      },
      error: (err) => {
        this.isFetchingAccount.set(false);
        this.authService.showToast('Failed to load your account balance. Please try again.', 'danger');
        console.error(err);
      },
    });
  }

  onSourceAccountChange(accountNumber: string): void {
    const profile = this.userProfile();
    if (profile && profile.accounts) {
      const account = profile.accounts.find((a: any) => a.accountNumber === accountNumber);
      if (account) {
        this.sourceAccount.set(account);
        // Verify the recipient again in case source/destination match
        this.verifyRecipient();
      }
    }
  }

  onToAccountChange(val: string): void {
    this.toAccountNumber = val;
    this.verifyRecipient();
  }

  verifyRecipient(): void {
    if (this.verificationTimeout) {
      clearTimeout(this.verificationTimeout);
    }
    
    const acct = this.toAccountNumber.trim().toUpperCase();
    if (!acct || acct.length < 5) {
      this.recipientName.set(null);
      this.isRecipientValid.set(false);
      this.isVerifyingRecipient.set(false);
      return;
    }

    this.isVerifyingRecipient.set(true);
    this.recipientName.set(null);
    this.isRecipientValid.set(false);

    this.verificationTimeout = setTimeout(() => {
      this.accountService.verifyAccountNumber(acct).subscribe({
        next: (res: any) => {
          this.recipientName.set(res.accountHolderName);
          this.isRecipientValid.set(true);
          this.isVerifyingRecipient.set(false);
        },
        error: () => {
          this.recipientName.set(null);
          this.isRecipientValid.set(false);
          this.isVerifyingRecipient.set(false);
        }
      });
    }, 400); // 400ms debounce
  }

  generateIdempotencyKey(): void {
    this.idempotencyKey = crypto.randomUUID();
  }

  onAmountKeyPress(event: KeyboardEvent): boolean {
    const charCode = event.key;
    // Allow digits 0-9 and one decimal point
    if (charCode === '.') {
      const currentVal = String(this.amount || '');
      if (currentVal.includes('.')) {
        return false; // Only one decimal point allowed
      }
      return true;
    }
    // Allow only numbers
    if (charCode >= '0' && charCode <= '9') {
      return true;
    }
    return false;
  }

  onAmountInput(event: any): void {
    let value = event.target.value;
    
    // Remove non-numeric/non-decimal characters
    value = value.replace(/[^0-9.]/g, '');
    
    // Ensure only one decimal point
    const parts = value.split('.');
    if (parts.length > 2) {
      value = parts[0] + '.' + parts.slice(1).join('');
    }
    
    // Limit to 2 decimal places
    if (parts[1] && parts[1].length > 2) {
      value = parts[0] + '.' + parts[1].substring(0, 2);
    }
    
    // Limit total digits before decimal to 8 digits (to prevent infinite number of digits)
    if (parts[0] && parts[0].length > 8) {
      const truncatedInt = parts[0].substring(0, 8);
      value = truncatedInt + (parts[1] !== undefined ? '.' + parts[1] : '');
    }

    event.target.value = value;
    // Parse as float, if invalid (like just a dot or empty), set to null
    this.amount = (value && value !== '.') ? parseFloat(value) : null;
  }

  onSubmit(): void {
    const destAccountNumber = this.toAccountNumber.trim().toUpperCase();
    const amt = this.amount;
    const source = this.sourceAccount();

    if (!destAccountNumber) {
      this.authService.showToast('Destination account number is required.', 'danger');
      return;
    }

    if (!this.isRecipientValid()) {
      this.authService.showToast('Please specify a valid recipient account number.', 'danger');
      return;
    }

    if (source && destAccountNumber === source.accountNumber.toUpperCase()) {
      this.authService.showToast('Source and destination accounts must be different.', 'danger');
      return;
    }

    if (!amt || amt <= 0) {
      this.authService.showToast('Transfer amount must be greater than zero.', 'danger');
      return;
    }

    if (amt > 10000000) {
      this.authService.showToast('Transfer amount cannot exceed the maximum transaction limit of Rs. 10,000,000.00.', 'danger');
      return;
    }

    if (source) {
      const remaining = source.balance - amt;
      if (remaining < 0) {
        this.authService.showToast(`Insufficient funds. Your available balance is Rs. ${source.balance.toFixed(2)}.`, 'danger');
        return;
      }
      if (remaining > 0 && remaining < 1000) {
        const maxTransferable = Math.max(0, source.balance - 1000);
        this.authService.showToast(`Insufficient funds. To maintain the minimum balance of Rs. 1,000.00, the maximum you can transfer is Rs. ${maxTransferable.toFixed(2)} (or you can transfer your entire balance of Rs. ${source.balance.toFixed(2)} to prepare the account for closure).`, 'danger');
        return;
      }
    }

    // Trigger confirmation modal
    this.showConfirmation.set(true);
  }

  cancelTransfer(): void {
    this.showConfirmation.set(false);
  }

  confirmTransfer(): void {
    this.showConfirmation.set(false);
    this.isLoading.set(true);

    const payload: TransferRequestData = {
      fromAccountNumber: this.sourceAccount()!.accountNumber,
      toAccountNumber: this.toAccountNumber.trim().toUpperCase(),
      amount: this.amount!,
      idempotencyKey: this.idempotencyKey,
    };

    this.transferService.transfer(payload).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.authService.showToast(`Transfer of Rs. ${payload.amount.toFixed(2)} completed successfully!`, 'success');
        
        // Refresh profile data to sync all account balances
        this.authService.refreshProfile().subscribe();

        // Redirect to dashboard
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.generateIdempotencyKey(); // Regenerate key on failure to allow retry

        if (err.error && err.error.message) {
          this.authService.showToast(err.error.message, 'danger');
        } else {
          this.authService.showToast('An error occurred while processing the transfer.', 'danger');
        }
      },
    });
  }
}
