import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, finalize, Observable, of, tap} from 'rxjs';
import {
  ActivationResponse,
  ApiMessageResponse,
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RegistrationRequest,
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
  private readonly authUrl = `${environment.apiUrl}/auth`;
  private readonly profileUrl = `${environment.apiUrl}/profile`;
  private readonly passwordUrl = `${environment.apiUrl}/password`;

  register(request: RegistrationRequest): Observable<ApiMessageResponse> {
    this.authState.setLoading(true);
    return this.http.post<ApiMessageResponse>(`${this.authUrl}/register`, request).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  activate(token: string): Observable<ActivationResponse> {
    this.authState.setLoading(true);
    return this.http.get<ActivationResponse>(`${this.authUrl}/activate`, {
      params: { token }
    }).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    this.authState.setLoading(true);
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, request).pipe(
      tap(response => {
        this.authState.setToken(response.token);
        this.authState.setUser(response.user);
      }),
      finalize(() => this.authState.setLoading(false))
    );
  }

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.profileUrl}`).pipe(
      tap(user => this.authState.setUser(user))
    );
  }

  deleteAccount(): Observable<void> {
    this.authState.setLoading(true);
    return this.http.delete<void>(`${this.profileUrl}`).pipe(
      tap(() => this.logout()),
      finalize(() => this.authState.setLoading(false))
    );
  }

  logout(): void {
    this.authState.clear();
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiMessageResponse> {
    this.authState.setLoading(true);
    return this.http.post<ApiMessageResponse>(`${this.passwordUrl}/forgot`, request).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiMessageResponse> {
    this.authState.setLoading(true);
    return this.http.post<ApiMessageResponse>(`${this.passwordUrl}/reset`, request).pipe(
      finalize(() => this.authState.setLoading(false))
    );
  }

  restoreSession(): Observable<User | null> {
    if (!this.authState.token()) {
      return of(null);
    }
    return this.getProfile().pipe(
      catchError(() => {
        this.authState.clear();
        return of(null);
      })
    );
  }
}

