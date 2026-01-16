import {Routes} from '@angular/router';
import {authGuard, guestGuard} from './core/guards/auth.guard';
import {adminGuard} from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./features/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: '',
    loadComponent: () => import('./shared/layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'chat',
        canActivate: [authGuard],
        loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent)
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'legal-notices',
        loadComponent: () => import('./features/legal-notices/legal-notices.component').then(m => m.LegalNoticesComponent)
      }
    ]
  },
  {
    path: '',
    loadComponent: () => import('./shared/layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
      }
    ]
  },
  {
    path: 'activate',
    loadComponent: () => import('./shared/layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/auth/activate/activate.component').then(m => m.ActivateComponent)
      }
    ]
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./shared/layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
