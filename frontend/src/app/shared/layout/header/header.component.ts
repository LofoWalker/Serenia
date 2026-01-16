import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  protected readonly authState = inject(AuthStateService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly showLogoutConfirm = signal(false);

  protected confirmLogout(): void {
    this.authService.logout();
    this.showLogoutConfirm.set(false);
    this.router.navigate(['/']);
  }

  protected cancelLogout(): void {
    this.showLogoutConfirm.set(false);
  }
}
