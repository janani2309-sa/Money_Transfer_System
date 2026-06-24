import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly authService = inject(AuthService);
  protected readonly title = signal('money-transfer-frontend');
  
  // Expose global toast state
  readonly toast = this.authService.toast;

  // Expose theme
  readonly theme = this.authService.theme;

  toggleTheme(): void {
    this.authService.toggleTheme();
  }
}
