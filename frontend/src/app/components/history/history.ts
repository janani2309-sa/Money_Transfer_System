import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

export interface TransactionRecord {
  id: string;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: string;
  failureReason: string | null;
  idempotencyKey: string;
  createdOn: string;
}

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './history.html',
  styleUrl: './history.css',
})
export class HistoryComponent implements OnInit {
  transactions = signal<TransactionRecord[]>([]);
  currentAccountId = signal<number | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const accountId = this.authService.currentAccountId();
    if (!accountId) {
      this.router.navigate(['/login']);
      return;
    }

    this.currentAccountId.set(accountId);
    this.fetchTransactions(accountId);
  }

  fetchTransactions(id: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.accountService.getTransactions(id).subscribe({
      next: (data: TransactionRecord[]) => {
        this.transactions.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to retrieve transaction logs. Please try again.');
        console.error(err);
      },
    });
  }

  isDebit(tx: TransactionRecord): boolean {
    return tx.fromAccountId === this.currentAccountId();
  }
}
