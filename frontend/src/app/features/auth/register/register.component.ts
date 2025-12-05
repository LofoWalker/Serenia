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
