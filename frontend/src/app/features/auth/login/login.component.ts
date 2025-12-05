import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { InputComponent } from '../../../shared/ui/input/input.component';
import { AlertComponent } from '../../../shared/ui/alert/alert.component';
@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ButtonComponent, InputComponent, AlertComponent],
  template: `
    <div>
      <h1 class="text-2xl font-bold text-primary-50 mb-2">Connexion</h1>
      <p class="text-primary-400 mb-6">Bienvenue ! Connectez-vous pour continuer.</p>
      @if (errorMessage()) {
        <app-alert type="error" [message]="errorMessage()" class="mb-4 block" />
      }
      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
        <app-input
          label="Email"
          type="email"
          formControlName="email"
          placeholder="votre@email.com"
          autocomplete="email"
          [required]="true"
          [error]="getFieldError('email')"
        />
        <app-input
          label="Mot de passe"
          type="password"
          formControlName="password"
          placeholder="••••••••"
          autocomplete="current-password"
          [required]="true"
          [error]="getFieldError('password')"
        />
        <app-button 
          type="submit" 
          [fullWidth]="true" 
          [loading]="authState.loading()"
          [disabled]="form.invalid"
        >
          Se connecter
        </app-button>
      </form>
      <div class="mt-6 pt-6 border-t border-primary-800 text-center">
        <p class="text-primary-400 text-sm">
          Pas encore de compte ?
          <a 
            routerLink="/register" 
            class="text-primary-50 hover:text-primary-200 font-medium focus:outline-none focus-visible:underline"
          >
            S'inscrire
          </a>
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly authState = inject(AuthStateService);
  protected readonly errorMessage = signal('');
  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });
  protected getFieldError(field: 'email' | 'password'): string {
    const control = this.form.get(field);
    if (!control?.touched || !control.errors) return '';
    if (control.errors['required']) return 'Ce champ est requis';
    if (control.errors['email']) return 'Email invalide';
    return '';
  }
  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set('');
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.router.navigate(['/chat']);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.errorMessage.set('Email ou mot de passe incorrect');
        } else if (error.status === 403) {
          this.errorMessage.set("Votre compte n'est pas encore activé. Vérifiez vos emails.");
        } else {
          this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
        }
      }
    });
  }
}
