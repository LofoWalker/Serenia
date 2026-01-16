import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize, Observable, tap, throwError } from 'rxjs';
import {
  ChangePlanRequestDTO,
  CheckoutRequestDTO,
  CheckoutSessionDTO,
  PlanDTO,
  PlanType,
  PortalSessionDTO,
  SubscriptionStatusDTO,
} from '../models/subscription.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/subscription`;

  // Private signals
  private readonly statusSignal = signal<SubscriptionStatusDTO | null>(null);
  private readonly plansSignal = signal<PlanDTO[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly loadingPlansSignal = signal<boolean>(false);
  private readonly loadingCheckoutSignal = signal<boolean>(false);
  private readonly loadingPortalSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  // Public signals (readonly)
  readonly status = this.statusSignal.asReadonly();
  readonly plans = this.plansSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly loadingPlans = this.loadingPlansSignal.asReadonly();
  readonly loadingCheckout = this.loadingCheckoutSignal.asReadonly();
  readonly loadingPortal = this.loadingPortalSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  // Computed signals
  readonly planName = computed(() => this.statusSignal()?.planName ?? 'FREE');

  readonly currentPlanConfig = computed(() => {
    const planName = this.planName();
    return this.plansSignal().find((p) => p.type === planName);
  });

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
  readonly tokensUsed = computed(() => this.statusSignal()?.tokensUsedThisMonth ?? 0);
  readonly monthlyTokenLimit = computed(() => this.statusSignal()?.monthlyTokenLimit ?? 0);
  readonly messagesSent = computed(() => this.statusSignal()?.messagesSentToday ?? 0);
  readonly dailyMessageLimit = computed(() => this.statusSignal()?.dailyMessageLimit ?? 0);
  readonly dailyResetDate = computed(() => this.statusSignal()?.dailyResetDate ?? '');
  readonly monthlyResetDate = computed(() => this.statusSignal()?.monthlyResetDate ?? '');

  // Stripe computed signals
  readonly subscriptionStatus = computed(() => this.statusSignal()?.status ?? 'ACTIVE');
  readonly hasStripeSubscription = computed(
    () => this.statusSignal()?.hasStripeSubscription ?? false,
  );
  readonly cancelAtPeriodEnd = computed(() => this.statusSignal()?.cancelAtPeriodEnd ?? false);
  readonly currentPeriodEnd = computed(() => this.statusSignal()?.currentPeriodEnd ?? null);
  readonly priceCents = computed(() => this.statusSignal()?.priceCents ?? 0);
  readonly currency = computed(() => this.statusSignal()?.currency ?? 'EUR');

  readonly isFreePlan = computed(() => this.planName() === 'FREE');
  readonly isPaidPlan = computed(() => this.planName() !== 'FREE');

  readonly isSubscriptionActive = computed(() => {
    const status = this.subscriptionStatus();
    return status === 'ACTIVE' || (status === 'CANCELED' && this.cancelAtPeriodEnd());
  });

  readonly isPaymentFailed = computed(() => this.subscriptionStatus() === 'PAST_DUE');

  readonly isQuotaLow = computed(() => {
    const s = this.statusSignal();
    if (!s) return false;
    return s.messagesRemainingToday <= 2 || s.tokensRemainingThisMonth < 500;
  });

  // Discount computed signals
  readonly hasActiveDiscount = computed(() => this.statusSignal()?.hasActiveDiscount ?? false);
  readonly discountDescription = computed(() => this.statusSignal()?.discountDescription ?? '');
  readonly discountEndDate = computed(() => this.statusSignal()?.discountEndDate ?? null);

  /**
   * Retrieves the list of available plans.
   */
  getPlans(): Observable<PlanDTO[]> {
    this.loadingPlansSignal.set(true);

    return this.http.get<PlanDTO[]>(`${this.apiUrl}/plans`).pipe(
      tap((plans) => this.plansSignal.set(plans)),
      catchError((error) => {
        this.errorSignal.set('Impossible de charger les plans');
        return throwError(() => error);
      }),
      finalize(() => this.loadingPlansSignal.set(false)),
    );
  }

  /**
   * Retrieves the user's subscription status.
   */
  getStatus(): Observable<SubscriptionStatusDTO> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    return this.http.get<SubscriptionStatusDTO>(`${this.apiUrl}/status`).pipe(
      tap((status) => this.statusSignal.set(status)),
      catchError((error) => {
        this.errorSignal.set("Impossible de charger le statut de l'abonnement");
        return throwError(() => error);
      }),
      finalize(() => this.loadingSignal.set(false)),
    );
  }

  /**
   * Changes the user's subscription plan (for FREE plan downgrades only).
   */
  changePlan(planType: PlanType): Observable<SubscriptionStatusDTO> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    const request: ChangePlanRequestDTO = { planType };

    return this.http.put<SubscriptionStatusDTO>(`${this.apiUrl}/plan`, request).pipe(
      tap((status) => this.statusSignal.set(status)),
      catchError((error) => {
        this.errorSignal.set('Impossible de changer de plan');
        return throwError(() => error);
      }),
      finalize(() => this.loadingSignal.set(false)),
    );
  }

  /**
   * Creates a Stripe Checkout session for subscribing to a paid plan.
   * Redirects the user to the Stripe payment page after a brief delay.
   */
  createCheckoutSession(planType: PlanType): Observable<CheckoutSessionDTO> {
    this.loadingCheckoutSignal.set(true);
    this.errorSignal.set(null);

    const request: CheckoutRequestDTO = { planType };

    return this.http.post<CheckoutSessionDTO>(`${this.apiUrl}/checkout`, request).pipe(
      tap((session) => {
        setTimeout(() => {
          window.location.href = session.url;
        }, 100);
      }),
      catchError((error) => {
        this.errorSignal.set('Impossible de créer la session de paiement');
        this.loadingCheckoutSignal.set(false);
        return throwError(() => error);
      }),
      finalize(() => {
        setTimeout(() => this.loadingCheckoutSignal.set(false), 3000);
      }),
    );
  }

  /**
   * Opens the Stripe Customer Portal to manage the subscription.
   * Redirects the user to the portal after a brief delay.
   */
  openCustomerPortal(): Observable<PortalSessionDTO> {
    this.loadingPortalSignal.set(true);
    this.errorSignal.set(null);

    return this.http.post<PortalSessionDTO>(`${this.apiUrl}/portal`, {}).pipe(
      tap((session) => {
        setTimeout(() => {
          window.location.href = session.url;
        }, 100);
      }),
      catchError((error) => {
        this.errorSignal.set("Impossible d'ouvrir le portail de gestion");
        this.loadingPortalSignal.set(false);
        return throwError(() => error);
      }),
      finalize(() => {
        setTimeout(() => this.loadingPortalSignal.set(false), 3000);
      }),
    );
  }

  /**
   * Refreshes the subscription status.
   */
  refreshStatus(): void {
    this.getStatus().subscribe();
  }

  /**
   * Clears the subscription state (call on logout).
   */
  clearStatus(): void {
    this.statusSignal.set(null);
    this.errorSignal.set(null);
  }
}
