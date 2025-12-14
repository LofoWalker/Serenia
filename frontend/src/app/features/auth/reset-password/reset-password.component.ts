import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {catchError, EMPTY, take, tap} from 'rxjs';
import {AuthService} from '../../../core/services/auth.service';
import {AuthStateService} from '../../../core/services/auth-state.service';
import {ButtonComponent} from '../../../shared/ui/button/button.component';
import {InputComponent} from '../../../shared/ui/input/input.component';
import {AlertComponent} from '../../../shared/ui/alert/alert.component';

@Component({
  selector: 'app-reset-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ButtonComponent, InputComponent, AlertComponent],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly authState = inject(AuthStateService);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly tokenMissing = signal(false);
  private token = '';

  protected readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  }, {
    validators: [this.passwordMatchValidator]
  });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.tokenMissing.set(true);
    }
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('newPassword')?.value;
    const confirm = control.get('confirmPassword')?.value;
    return password === confirm ? null : {passwordMismatch: true};
  }

  protected getFieldError(field: 'newPassword' | 'confirmPassword'): string {
    const control = this.form.get(field);
    if (!control?.touched || !control.errors) return '';
    if (control.errors['required']) return 'Ce champ est requis';
    if (control.errors['minlength']) return 'Le mot de passe doit contenir au moins 8 caractères.';
    return '';
  }

  protected getFormError(): string {
    if (this.form.errors?.['passwordMismatch'] && this.form.get('confirmPassword')?.touched) {
      return 'Les mots de passe ne correspondent pas.';
    }
    return '';
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set('');
    this.authService.resetPassword({
      token: this.token,
      newPassword: this.form.getRawValue().newPassword
    }).pipe(
      take(1),
      tap(response => {
        this.successMessage.set(response.message);
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 400) {
          this.errorMessage.set('Le lien de réinitialisation est invalide ou a expiré. Veuillez refaire une demande.');
        } else {
          this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
        }
        return EMPTY;
      })
    ).subscribe();
  }
}

