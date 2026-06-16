import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { TransferService, TransferRequestData } from '../../services/transfer.service';
import { AccountData } from '../dashboard/dashboard';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './transfer.html',
  styleUrl: './transfer.css',
})
export class TransferComponent implements OnInit {
  sourceAccount = signal<AccountData | null>(null);
  toAccountId = '';
  amount: number | null = null;
  idempotencyKey = '';
  
  isLoading = signal<boolean>(false);
  isFetchingAccount = signal<boolean>(true);
  
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  
  // Modal state
  showConfirmation = signal<boolean>(false);

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private transferService: TransferService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const accountId = this.authService.currentAccountId();
    if (!accountId) {
      this.router.navigate(['/login']);
      return;
    }

    this.generateIdempotencyKey();
    this.fetchSourceAccount(accountId);
  }

  fetchSourceAccount(id: number): void {
    this.isFetchingAccount.set(true);
    this.accountService.getAccount(id).subscribe({
      next: (data: AccountData) => {
        this.sourceAccount.set(data);
        this.isFetchingAccount.set(false);
      },
      error: (err) => {
        this.isFetchingAccount.set(false);
        this.errorMessage.set('Failed to load your account balance. Please try again.');
        console.error(err);
      },
    });
  }

  generateIdempotencyKey(): void {
    this.idempotencyKey = crypto.randomUUID();
  }

  onSubmit(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const destId = parseInt(this.toAccountId, 10);
    const amt = this.amount;
    const source = this.sourceAccount();

    if (isNaN(destId)) {
      this.errorMessage.set('Destination account ID must be a valid number.');
      return;
    }

    if (source && destId === source.id) {
      this.errorMessage.set('Source and destination accounts must be different.');
      return;
    }

    if (!amt || amt <= 0) {
      this.errorMessage.set('Transfer amount must be greater than zero.');
      return;
    }

    if (source && source.balance < amt) {
      this.errorMessage.set(`Insufficient funds. Your available balance is $${source.balance.toFixed(2)}.`);
      return;
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
    this.errorMessage.set(null);

    const payload: TransferRequestData = {
      fromAccountId: this.sourceAccount()!.id,
      toAccountId: parseInt(this.toAccountId, 10),
      amount: this.amount!,
      idempotencyKey: this.idempotencyKey,
    };

    this.transferService.transfer(payload).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.successMessage.set(`Transfer of $${payload.amount.toFixed(2)} completed successfully!`);
        
        // Reset form inputs
        this.toAccountId = '';
        this.amount = null;
        
        // Refresh source account details
        this.fetchSourceAccount(payload.fromAccountId);
        // Regenerate key for next transfer
        this.generateIdempotencyKey();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.generateIdempotencyKey(); // Regenerate key on failure to allow retry

        if (err.error && err.error.message) {
          this.errorMessage.set(err.error.message);
        } else {
          this.errorMessage.set('An error occurred while processing the transfer.');
        }
      },
    });
  }
}
