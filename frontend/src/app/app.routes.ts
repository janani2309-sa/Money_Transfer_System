import { Routes } from '@angular/router';
import { authGuard, loginGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login').then((m) => m.LoginComponent),
    canActivate: [loginGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./components/dashboard/dashboard').then((m) => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: 'transfer',
    loadComponent: () => import('./components/transfer/transfer').then((m) => m.TransferComponent),
    canActivate: [authGuard],
  },
  {
    path: 'history',
    loadComponent: () => import('./components/history/history').then((m) => m.HistoryComponent),
    canActivate: [authGuard],
  },
  {
    path: 'create-account',
    loadComponent: () => import('./components/create-account/create-account').then((m) => m.CreateAccountComponent),
    canActivate: [authGuard],
  },
  {
    path: 'profile',
    loadComponent: () => import('./components/profile/profile').then((m) => m.ProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: 'rewards',
    loadComponent: () => import('./components/rewards/rewards').then((m) => m.RewardsComponent),
    canActivate: [authGuard],
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
