import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {finalize, Observable, tap} from 'rxjs';
import {
  ActivationResponse,
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  PasswordResetResponse,
  RegistrationRequest,
  RegistrationResponse,
  ResetPasswordRequest,
  User
} from '../models/user.model';
import {AuthStateService} from './auth-state.service';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authState = inject(AuthStateService);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  register(request: RegistrationRequest): Observable<RegistrationResponse> {
    this.authState.setLoading(true);
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/register`, request).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  activate(token: string): Observable<ActivationResponse> {
    this.authState.setLoading(true);
    return this.http.get<ActivationResponse>(`${this.apiUrl}/activate`, {
      params: { token }
    }).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    this.authState.setLoading(true);
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        this.authState.setToken(response.token);
        this.authState.setUser(response.user);
      }),
      finalize(() => this.authState.setLoading(false))
    );
  }

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`).pipe(
      tap(user => this.authState.setUser(user))
    );
  }

  deleteAccount(): Observable<void> {
    this.authState.setLoading(true);
    return this.http.delete<void>(`${this.apiUrl}/me`).pipe(
      tap(() => this.logout()),
      finalize(() => this.authState.setLoading(false))
    );
  }

  logout(): void {
    this.authState.clear();
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetResponse> {
    this.authState.setLoading(true);
    return this.http.post<PasswordResetResponse>(`${this.apiUrl}/forgot-password`, request).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<PasswordResetResponse> {
    this.authState.setLoading(true);
    return this.http.post<PasswordResetResponse>(`${this.apiUrl}/reset-password`, request).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  restoreSession(): Observable<User> {
    return this.getProfile();
  }
}

