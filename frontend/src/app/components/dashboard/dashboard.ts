import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

export interface AccountData {
  id: number;
  holderName: string;
  balance: number;
  status: string;
  lastUpdated: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  account = signal<AccountData | null>(null);
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
      this.errorMessage.set('No active session. Please log in.');
      this.isLoading.set(false);
      this.router.navigate(['/login']);
      return;
    }

    this.fetchAccountDetails(accountId);
  }

  fetchAccountDetails(id: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.accountService.getAccount(id).subscribe({
      next: (data: AccountData) => {
        this.account.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load account details. Please retry.');
        console.error(err);
      },
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
