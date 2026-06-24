import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService, AccountDetails } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  isLoading = signal<boolean>(true);


  // Expose auth service signals to template
  userProfile: any;
  activeAccount: any;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.userProfile = this.authService.userProfile;
    this.activeAccount = this.authService.activeAccount;
  }

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.authService.showToast('No active session. Please log in.', 'danger');
      this.isLoading.set(false);
      this.router.navigate(['/login']);
      return;
    }

    this.refreshDashboard();
  }

  refreshDashboard(): void {
    this.isLoading.set(true);
    this.authService.refreshProfile().subscribe({
      next: (profile) => {
        this.isLoading.set(false);
        if (!profile || !profile.accounts || profile.accounts.length === 0) {
          this.router.navigate(['/create-account']);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.authService.showToast('Failed to load dashboard data. Check database.', 'danger');
        console.error(err);
      },
    });
  }

  switchAccount(accountNumber: string): void {
    this.authService.setActiveAccount(accountNumber);
  }

  hasFewerThanThreeAccounts(): boolean {
    const profile = this.userProfile();
    return profile ? profile.accounts.length < 3 : false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
