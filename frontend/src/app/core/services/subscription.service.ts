import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, finalize, Observable, tap, throwError} from 'rxjs';
import {
  ChangePlanRequestDTO,
  PlanType,
  SubscriptionStatusDTO
} from '../models/subscription.model';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/subscription`;

  // Signals privés
  private readonly statusSignal = signal<SubscriptionStatusDTO | null>(null);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  // Signals publics (readonly)
  readonly status = this.statusSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  // Computed signals
  readonly planName = computed(() => this.statusSignal()?.planName ?? 'FREE');

  readonly tokensUsagePercent = computed(() => {
    const s = this.statusSignal();
    if (!s || s.monthlyTokenLimit === 0) return 0;
    return Math.min(100, (s.tokensUsedThisMonth / s.monthlyTokenLimit) * 100);
  });

  readonly messagesUsagePercent = computed(() => {
    const s = this.statusSignal();
    if (!s || s.dailyMessageLimit === 0) return 0;
    return Math.min(100, (s.messagesSentToday / s.dailyMessageLimit) * 100);
  });

  readonly tokensRemaining = computed(() => this.statusSignal()?.tokensRemainingThisMonth ?? 0);
  readonly messagesRemaining = computed(() => this.statusSignal()?.messagesRemainingToday ?? 0);

  readonly isQuotaLow = computed(() => {
    const s = this.statusSignal();
    if (!s) return false;
    return s.messagesRemainingToday <= 2 || s.tokensRemainingThisMonth < 500;
  });

  /**
   * Récupère le statut de l'abonnement de l'utilisateur
   */
  getStatus(): Observable<SubscriptionStatusDTO> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<SubscriptionStatusDTO>(`${this.apiUrl}/status`).pipe(
      tap(status => this.statusSignal.set(status)),
      catchError(error => {
        this.errorSignal.set('Impossible de charger le statut de l\'abonnement');
        return throwError(() => error);
      }),
      finalize(() => this.loadingSignal.set(false))
    );
  }

  /**
   * Change le plan d'abonnement de l'utilisateur
   */
  changePlan(planType: PlanType): Observable<SubscriptionStatusDTO> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    const request: ChangePlanRequestDTO = { planType };

    return this.http.put<SubscriptionStatusDTO>(`${this.apiUrl}/plan`, request).pipe(
      tap(status => this.statusSignal.set(status)),
      catchError(error => {
        this.errorSignal.set('Impossible de changer de plan');
        return throwError(() => error);
      }),
      finalize(() => this.loadingSignal.set(false))
    );
  }

  /**
   * Rafraîchit le statut de l'abonnement
   */
  refreshStatus(): void {
    this.getStatus().subscribe();
  }

  /**
   * Réinitialise l'état (à appeler lors du logout)
   */
  clearStatus(): void {
    this.statusSignal.set(null);
    this.errorSignal.set(null);
  }
}

