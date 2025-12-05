import { Injectable, signal, computed } from '@angular/core';
import { User } from '../models/user.model';

const TOKEN_KEY = 'serenia_token';

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  private readonly userSignal = signal<User | null>(null);
  private readonly tokenSignal = signal<string | null>(this.getStoredToken());
  private readonly loadingSignal = signal(false);

  readonly user = this.userSignal.asReadonly();
  readonly token = this.tokenSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal() && !!this.userSignal());
  readonly userFullName = computed(() => {
    const user = this.userSignal();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  setUser(user: User | null): void {
    this.userSignal.set(user);
  }

  setToken(token: string | null): void {
    this.tokenSignal.set(token);
    if (token) {
      sessionStorage.setItem(TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
    }
  }

  setLoading(loading: boolean): void {
    this.loadingSignal.set(loading);
  }

  clear(): void {
    this.userSignal.set(null);
    this.setToken(null);
  }

  private getStoredToken(): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    return sessionStorage.getItem(TOKEN_KEY);
  }
}

