import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { InputComponent } from '../../../shared/ui/input/input.component';
import { AlertComponent } from '../../../shared/ui/alert/alert.component';
@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ButtonComponent, InputComponent, AlertComponent],
  template: `
    <div>
      <h1 class="text-2xl font-bold text-primary-50 mb-2">Créer un compte</h1>
      <p class="text-primary-400 mb-6">Rejoignez Serenia et commencez votre expérience.</p>
      @if (successMessage()) {
        <app-alert type="success" [message]="successMessage()" class="mb-4 block" />
      }
      @if (errorMessage()) {
        <app-alert type="error" [message]="errorMessage()" class="mb-4 block" />
      }
      @if (!successMessage()) {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <app-input
              label="Nom"
              type="text"
              formControlName="lastName"
              placeholder="Dupont"
              autocomplete="family-name"
              [required]="true"
              [error]="getFieldError('lastName')"
            />
            <app-input
              label="Prénom"
              type="text"
              formControlName="firstName"
              placeholder="Jean"
              autocomplete="given-name"
              [required]="true"
              [error]="getFieldError('firstName')"
            />
          </div>
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
            placeholder="Minimum 6 caractères"
            autocomplete="new-password"
            [required]="true"
            [error]="getFieldError('password')"
          />
          <app-button 
            type="submit" 
            [fullWidth]="true" 
            [loading]="authState.loading()"
            [disabled]="form.invalid"
          >
            S'inscrire
          </app-button>
        </form>
      }
      <div class="mt-6 pt-6 border-t border-primary-800 text-center">
        <p class="text-primary-400 text-sm">
          Déjà un compte ?
          <a 
            routerLink="/login" 
            class="text-primary-50 hover:text-primary-200 font-medium focus:outline-none focus-visible:underline"
          >
            Se connecter
          </a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  protected readonly authState = inject(AuthStateService);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly form = this.fb.nonNullable.group({
    lastName: ['', [Validators.required]],
    firstName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });
  protected getFieldError(field: 'lastName' | 'firstName' | 'email' | 'password'): string {
    const control = this.form.get(field);
    if (!control?.touched || !control.errors) return '';
    if (control.errors['required']) return 'Ce champ est requis';
    if (control.errors['email']) return 'Email invalide';
    if (control.errors['minlength']) return 'Minimum 6 caractères';
    return '';
  }
  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set('');
    this.successMessage.set('');
    this.authService.register(this.form.getRawValue()).subscribe({
      next: (response) => {
        this.successMessage.set(response.message || 'Inscription réussie ! Vérifiez votre email pour activer votre compte.');
        this.form.reset();
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 400) {
          this.errorMessage.set(error.error?.message || 'Données invalides. Veuillez vérifier vos informations.');
        } else {
          this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
        }
      }
    });
  }
}
