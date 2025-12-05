import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { AlertComponent } from '../../shared/ui/alert/alert.component';
@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, AlertComponent],
  template: `
    <div class="max-w-2xl mx-auto px-4 py-8">
      <h1 class="text-2xl font-bold text-primary-50 mb-8">Mon profil</h1>
      @if (errorMessage()) {
        <app-alert type="error" [message]="errorMessage()" class="mb-6 block" />
      }
      @if (authState.user(); as user) {
        <div class="bg-primary-900 border border-primary-800 rounded-xl overflow-hidden">
          <div class="p-6 border-b border-primary-800">
            <div class="flex items-center gap-4">
              <div class="w-16 h-16 rounded-full bg-primary-700 flex items-center justify-center text-2xl font-bold text-primary-200">
                {{ getInitials(user.firstName, user.lastName) }}
              </div>
              <div>
                <h2 class="text-xl font-semibold text-primary-50">
                  {{ user.firstName }} {{ user.lastName }}
                </h2>
                <p class="text-primary-400">{{ user.email }}</p>
              </div>
            </div>
          </div>
          <div class="p-6 space-y-4">
            <div>
              <h3 class="text-sm font-medium text-primary-400 mb-1">Identifiant</h3>
              <p class="text-primary-200 font-mono text-sm">{{ user.id }}</p>
            </div>
            <div>
              <h3 class="text-sm font-medium text-primary-400 mb-1">Rôles</h3>
              <div class="flex flex-wrap gap-2">
                @for (role of user.roles; track role) {
                  <span class="px-2 py-1 bg-primary-800 text-primary-300 text-xs font-medium rounded">
                    {{ role }}
                  </span>
                }
              </div>
            </div>
          </div>
        </div>
        <div class="mt-8 pt-8 border-t border-primary-800">
          <h3 class="text-lg font-semibold text-red-400 mb-4">Zone de danger</h3>
          <p class="text-primary-400 mb-4">
            La suppression de votre compte est irréversible. Toutes vos données, y compris vos conversations, seront définitivement effacées.
          </p>
          @if (!showDeleteConfirm()) {
            <app-button 
              variant="danger"
              (clicked)="showDeleteConfirm.set(true)"
            >
              Supprimer mon compte
            </app-button>
          } @else {
            <div class="p-4 bg-red-900/30 border border-red-700 rounded-lg">
              <p class="text-red-300 font-medium mb-4">
                Êtes-vous sûr de vouloir supprimer votre compte ?
              </p>
              <div class="flex gap-3">
                <app-button 
                  variant="danger"
                  [loading]="deleting()"
                  (clicked)="deleteAccount()"
                >
                  Oui, supprimer
                </app-button>
                <app-button 
                  variant="secondary"
                  [disabled]="deleting()"
                  (clicked)="showDeleteConfirm.set(false)"
                >
                  Annuler
                </app-button>
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="flex items-center justify-center py-12">
          <svg class="animate-spin h-8 w-8 text-primary-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span class="sr-only">Chargement du profil...</span>
        </div>
      }
    </div>
  `
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly authState = inject(AuthStateService);
  protected readonly showDeleteConfirm = signal(false);
  protected readonly deleting = signal(false);
  protected readonly errorMessage = signal('');
  ngOnInit(): void {
    if (!this.authState.user()) {
      this.authService.getProfile().subscribe({
        error: () => {
          this.errorMessage.set('Impossible de charger le profil.');
        }
      });
    }
  }
  protected getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }
  protected deleteAccount(): void {
    this.deleting.set(true);
    this.errorMessage.set('');
    this.authService.deleteAccount().subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.deleting.set(false);
        this.errorMessage.set('Impossible de supprimer le compte. Veuillez réessayer.');
      }
    });
  }
}
