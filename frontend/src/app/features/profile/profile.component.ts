import {ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {DatePipe, DecimalPipe, NgClass} from '@angular/common';
import {catchError, EMPTY, forkJoin, interval, Subscription, take, takeWhile, tap} from 'rxjs';
import {AuthService} from '../../core/services/auth.service';
import {AuthStateService} from '../../core/services/auth-state.service';
import {ChatService} from '../../core/services/chat.service';
import {SubscriptionService} from '../../core/services/subscription.service';
import {ButtonComponent} from '../../shared/ui/button/button.component';
import {AlertComponent} from '../../shared/ui/alert/alert.component';
import {formatPrice, getPlanByType, PlanType} from '../../core/models/subscription.model';

const PLAN_ORDER: PlanType[] = ['FREE', 'PLUS', 'MAX'];
const PAYMENT_STATUS_POLL_INTERVAL_MS = 1000;
const PAYMENT_STATUS_MAX_POLLS = 10;

@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, AlertComponent, DatePipe, DecimalPipe, NgClass],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly authState = inject(AuthStateService);
  protected readonly subscriptionService = inject(SubscriptionService);

  private statusPollingSubscription?: Subscription;

  protected readonly showDeleteConfirm = signal(false);
  protected readonly showDeleteConversationsConfirm = signal(false);
  protected readonly showPlanSelector = signal(false);
  protected readonly selectedPlan = signal<PlanType | null>(null);
  protected readonly deleting = signal(false);
  protected readonly deletingConversations = signal(false);
  protected readonly changingPlan = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly awaitingPaymentConfirmation = signal(false);

  protected readonly sortedPlans = computed(() => {
    const plans = this.subscriptionService.plans();
    return [...plans].sort((a, b) => {
      return PLAN_ORDER.indexOf(a.type) - PLAN_ORDER.indexOf(b.type);
    });
  });

  protected readonly canConfirmChange = computed(() => {
    const selected = this.selectedPlan();
    return selected !== null && selected !== this.subscriptionService.planName();
  });

  // Computed pour l'affichage du statut Stripe
  protected readonly subscriptionStatusLabel = computed(() => {
    const status = this.subscriptionService.subscriptionStatus();
    const cancelAtEnd = this.subscriptionService.cancelAtPeriodEnd();

    if (cancelAtEnd) {
      return 'Annulé (actif jusqu\'à la fin de la période)';
    }

    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'PAST_DUE': return 'Paiement en échec';
      case 'CANCELED': return 'Annulé';
      case 'INCOMPLETE': return 'Paiement incomplet';
      case 'UNPAID': return 'Impayé';
      default: return status;
    }
  });

  protected readonly subscriptionStatusClass = computed(() => {
    const status = this.subscriptionService.subscriptionStatus();
    if (this.subscriptionService.cancelAtPeriodEnd()) {
      return 'bg-yellow-600';
    }
    switch (status) {
      case 'ACTIVE': return 'bg-emerald-600';
      case 'PAST_DUE': return 'bg-red-600';
      case 'CANCELED': return 'bg-gray-600';
      default: return 'bg-yellow-600';
    }
  });

  ngOnInit(): void {
    // Gérer les callbacks de Stripe
    this.handlePaymentCallback();

    if (!this.authState.user()) {
      this.authService.getProfile().pipe(
        take(1),
        catchError(() => {
          this.errorMessage.set('Impossible de charger le profil.');
          return EMPTY;
        })
      ).subscribe();
    }

    // Charger le statut d'abonnement et les plans disponibles
    forkJoin([
      this.subscriptionService.getStatus(),
      this.subscriptionService.getPlans()
    ]).pipe(
      take(1),
      catchError(() => {
        // Erreur silencieuse - le statut sera affiché comme indisponible
        return EMPTY;
      })
    ).subscribe();
  }

  ngOnDestroy(): void {
    this.statusPollingSubscription?.unsubscribe();
  }

  /**
   * Handles Stripe payment callbacks (success/cancel).
   * On success, polls the subscription status until it changes or max attempts reached.
   */
  private handlePaymentCallback(): void {
    const paymentStatus = this.route.snapshot.queryParamMap.get('payment');

    if (paymentStatus === 'success') {
      this.successMessage.set('Paiement réussi ! Mise à jour de votre abonnement en cours...');
      this.awaitingPaymentConfirmation.set(true);
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
      this.pollSubscriptionStatus();
    } else if (paymentStatus === 'cancel') {
      this.errorMessage.set('Paiement annulé. Vous pouvez réessayer à tout moment.');
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
    }
  }

  /**
   * Polls subscription status every second until it shows a paid plan or max attempts reached.
   */
  private pollSubscriptionStatus(): void {
    const initialPlan = this.subscriptionService.planName();
    let pollCount = 0;

    this.statusPollingSubscription = interval(PAYMENT_STATUS_POLL_INTERVAL_MS).pipe(
      take(PAYMENT_STATUS_MAX_POLLS),
      takeWhile(() => {
        pollCount++;
        const currentPlan = this.subscriptionService.planName();
        const hasChanged = currentPlan !== initialPlan && currentPlan !== 'FREE';
        return !hasChanged;
      })
    ).subscribe({
      next: () => {
        this.subscriptionService.getStatus().pipe(take(1)).subscribe();
      },
      complete: () => {
        this.awaitingPaymentConfirmation.set(false);
        const finalPlan = this.subscriptionService.planName();
        if (finalPlan !== 'FREE' && finalPlan !== initialPlan) {
          this.successMessage.set('Votre abonnement est maintenant actif !');
        } else if (pollCount >= PAYMENT_STATUS_MAX_POLLS) {
          this.successMessage.set('Paiement reçu. Votre abonnement sera activé dans quelques instants.');
        }
      }
    });
  }

  protected getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  protected getPlanDisplayName(planType: PlanType): string {
    const plan = getPlanByType(this.subscriptionService.plans(), planType);
    return plan?.name ?? planType;
  }

  protected getPlanPrice(planType: PlanType): string {
    const plan = getPlanByType(this.subscriptionService.plans(), planType);
    if (!plan || plan.priceCents === 0) {
      return 'Gratuit';
    }
    return formatPrice(plan.priceCents, plan.currency) + '/mois';
  }

  protected selectPlan(plan: PlanType): void {
    // Ne pas sélectionner le plan actuel
    if (plan === this.subscriptionService.planName()) {
      return;
    }
    this.selectedPlan.set(plan);
  }

  /**
   * Lance le processus de souscription via Stripe Checkout
   */
  protected subscribeToPlan(planType: PlanType): void {
    if (planType === 'FREE') {
      return;
    }

    this.errorMessage.set('');
    this.subscriptionService.createCheckoutSession(planType).pipe(
      take(1),
      catchError(() => {
        this.errorMessage.set('Impossible de lancer le paiement. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }

  /**
   * Ouvre le portail client Stripe pour gérer l'abonnement
   */
  protected openManageSubscription(): void {
    this.errorMessage.set('');
    this.subscriptionService.openCustomerPortal().pipe(
      take(1),
      catchError(() => {
        this.errorMessage.set('Impossible d\'ouvrir le portail de gestion. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }

  protected confirmChangePlan(): void {
    const newPlan = this.selectedPlan();
    if (!newPlan || newPlan === this.subscriptionService.planName()) {
      return;
    }

    // Redirect to Stripe Checkout for paid plans
    if (newPlan !== 'FREE') {
      this.subscribeToPlan(newPlan);
      return;
    }

    // Direct plan change for downgrade to FREE
    this.changingPlan.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.subscriptionService.changePlan(newPlan).pipe(
      take(1),
      tap(() => {
        this.changingPlan.set(false);
        this.showPlanSelector.set(false);
        this.selectedPlan.set(null);
        this.successMessage.set(`Votre plan a été changé en ${this.getPlanDisplayName(newPlan)} avec succès.`);
      }),
      catchError(() => {
        this.changingPlan.set(false);
        this.errorMessage.set('Impossible de changer de plan. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }

  protected cancelPlanSelection(): void {
    this.showPlanSelector.set(false);
    this.selectedPlan.set(null);
  }

  protected deleteConversations(): void {
    this.deletingConversations.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.chatService.deleteMyConversations().pipe(
      take(1),
      tap(() => {
        this.deletingConversations.set(false);
        this.showDeleteConversationsConfirm.set(false);
        this.successMessage.set('Vos conversations ont été supprimées avec succès.');
      }),
      catchError(() => {
        this.deletingConversations.set(false);
        this.errorMessage.set('Impossible de supprimer les conversations. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }

  protected deleteAccount(): void {
    this.deleting.set(true);
    this.errorMessage.set('');
    this.authService.deleteAccount().pipe(
      take(1),
      tap(() => this.router.navigate(['/'])),
      catchError(() => {
        this.deleting.set(false);
        this.errorMessage.set('Impossible de supprimer le compte. Veuillez réessayer.');
        return EMPTY;
      })
    ).subscribe();
  }
}
