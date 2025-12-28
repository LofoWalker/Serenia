import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {catchError, EMPTY, take, tap} from 'rxjs';
import {AuthService} from '../../../core/services/auth.service';
import {AuthStateService} from '../../../core/services/auth-state.service';
import {ButtonComponent} from '../../../shared/ui/button/button.component';
import {InputComponent} from '../../../shared/ui/input/input.component';
import {AlertComponent} from '../../../shared/ui/alert/alert.component';
import {PasswordStrengthComponent} from '../../../shared/ui/password-strength/password-strength.component';
import {passwordValidator} from '../../../core/validators/password.validator';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ButtonComponent, InputComponent, AlertComponent, PasswordStrengthComponent],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
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
    password: ['', [Validators.required, passwordValidator()]]
  });

  protected get passwordValue(): string {
    return this.form.get('password')?.value || '';
  }

  protected getFieldError(field: 'lastName' | 'firstName' | 'email' | 'password'): string {
    const control = this.form.get(field);
    if (!control?.touched || !control.errors) return '';
    if (control.errors['required']) return 'Ce champ est requis';
    if (control.errors['email']) return 'Email invalide';
    if (control.errors['passwordPolicy']) return '';  // Géré par le composant password-strength
    return '';
  }
  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set('');
    this.successMessage.set('');
    this.authService.register(this.form.getRawValue()).pipe(
      take(1),
      tap(response => {
        this.successMessage.set(response.message || 'Inscription réussie ! Vérifiez votre email pour activer votre compte.');
        this.form.reset();
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 400) {
          this.errorMessage.set(error.error?.message || 'Données invalides. Veuillez vérifier vos informations.');
        } else {
          this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
        }
        return EMPTY;
      })
    ).subscribe();
  }
}
