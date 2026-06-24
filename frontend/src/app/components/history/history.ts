import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

export interface TransactionRecord {
  id: string;
  fromAccountNumber: string;
  toAccountNumber: string;
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
  currentAccountNumber = signal<string | null>(null);
  isLoading = signal<boolean>(true);
  isDownloading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {}

  downloadPdf(): void {
    const accountNumber = this.currentAccountNumber();
    if (!accountNumber) {
      return;
    }

    this.isDownloading.set(true);
    this.errorMessage.set(null);

    this.accountService.downloadStatementPdf(accountNumber).subscribe({
      next: (blob: Blob) => {
        this.isDownloading.set(false);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `statement-${accountNumber}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.isDownloading.set(false);
        this.errorMessage.set('Failed to download statement. Please try again.');
        console.error(err);
      },
    });
  }

  ngOnInit(): void {
    const accountNumber = this.authService.currentAccountNumber();
    if (!accountNumber) {
      this.router.navigate(['/login']);
      return;
    }

    this.currentAccountNumber.set(accountNumber);
    this.fetchTransactions(accountNumber);
  }

  fetchTransactions(accountNumber: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.accountService.getTransactions(accountNumber).subscribe({
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
    return tx.fromAccountNumber === this.currentAccountNumber();
  }
}
