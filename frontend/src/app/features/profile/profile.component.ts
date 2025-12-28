import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {DatePipe, DecimalPipe, NgClass} from '@angular/common';
import {catchError, EMPTY, forkJoin, take, tap} from 'rxjs';
import {AuthService} from '../../core/services/auth.service';
import {AuthStateService} from '../../core/services/auth-state.service';
import {ChatService} from '../../core/services/chat.service';
import {SubscriptionService} from '../../core/services/subscription.service';
import {ButtonComponent} from '../../shared/ui/button/button.component';
import {AlertComponent} from '../../shared/ui/alert/alert.component';
import {getPlanByType, PlanType} from '../../core/models/subscription.model';

// Ordre d'affichage des plans
const PLAN_ORDER: PlanType[] = ['FREE', 'PLUS', 'MAX'];

@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, AlertComponent, DatePipe, DecimalPipe, NgClass],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  protected readonly authState = inject(AuthStateService);
  protected readonly subscriptionService = inject(SubscriptionService);

  protected readonly showDeleteConfirm = signal(false);
  protected readonly showDeleteConversationsConfirm = signal(false);
  protected readonly showPlanSelector = signal(false);
  protected readonly selectedPlan = signal<PlanType | null>(null);
  protected readonly deleting = signal(false);
  protected readonly deletingConversations = signal(false);
  protected readonly changingPlan = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

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

  ngOnInit(): void {
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

  protected getInitials(firstName: string, lastName: string): string {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  protected getPlanDisplayName(planType: PlanType): string {
    const plan = getPlanByType(this.subscriptionService.plans(), planType);
    return plan?.name ?? planType;
  }

  protected selectPlan(plan: PlanType): void {
    // Ne pas sélectionner le plan actuel
    if (plan === this.subscriptionService.planName()) {
      return;
    }
    this.selectedPlan.set(plan);
  }

  protected confirmChangePlan(): void {
    const newPlan = this.selectedPlan();
    if (!newPlan || newPlan === this.subscriptionService.planName()) {
      return;
    }

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
