import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, EMPTY, of, switchMap, take, tap } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ChatService } from '../../../core/services/chat.service';
import { ButtonComponent } from '../../../shared/ui/button/button.component';
import { InputComponent } from '../../../shared/ui/input/input.component';
import { AlertComponent } from '../../../shared/ui/alert/alert.component';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, ButtonComponent, InputComponent, AlertComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  protected readonly authState = inject(AuthStateService);
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
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
    this.authService
      .login(this.form.getRawValue())
      .pipe(
        switchMap(() => this.chatService.loadMyMessages().pipe(catchError(() => of(null)))),
        take(1),
        tap(() => this.router.navigate(['/chat'])),
        catchError((error: HttpErrorResponse) => {
          if (error.status === 401) {
            this.errorMessage.set('Email ou mot de passe incorrect');
          } else if (error.status === 403) {
            this.errorMessage.set("Votre compte n'est pas encore activé. Vérifiez vos emails.");
          } else {
            this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
          }
          return EMPTY;
        }),
      )
      .subscribe();
  }
}
