import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { AlertComponent } from '../../../shared/ui/alert/alert.component';
@Component({
  selector: 'app-activate',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonComponent, AlertComponent],
  template: `
    <div class="text-center">
      <h1 class="text-2xl font-bold text-primary-50 mb-4">Activation du compte</h1>
      @if (authState.loading()) {
        <div class="py-8">
          <svg class="animate-spin h-12 w-12 mx-auto text-primary-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-4 text-primary-400">Activation en cours...</p>
        </div>
      } @else {
        @if (successMessage()) {
          <app-alert type="success" [message]="successMessage()" class="mb-6 block text-left" />
          <a routerLink="/login">
            <app-button [fullWidth]="true">
              Se connecter
            </app-button>
          </a>
        }
        @if (errorMessage()) {
          <app-alert type="error" [message]="errorMessage()" class="mb-6 block text-left" />
          <a routerLink="/register">
            <app-button variant="secondary" [fullWidth]="true">
              Retour à l'inscription
            </app-button>
          </a>
        }
      }
    </div>
  `
})
export class ActivateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  protected readonly authState = inject(AuthStateService);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');
  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.errorMessage.set("Token d'activation manquant ou invalide.");
      return;
    }
    this.authService.activate(token).subscribe({
      next: (response) => {
        this.successMessage.set(response.message || 'Votre compte a été activé avec succès !');
      },
      error: () => {
        this.errorMessage.set("Le lien d'activation est invalide ou a expiré. Veuillez vous réinscrire.");
      }
    });
  }
}
