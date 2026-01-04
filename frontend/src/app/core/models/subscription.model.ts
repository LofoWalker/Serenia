/**
 * Types et modèles pour la gestion des abonnements et quotas
 */

// Types de plan disponibles
export type PlanType = 'FREE' | 'PLUS' | 'MAX';

// Types de quota pour les erreurs
export type QuotaType = 'DAILY_MESSAGE_LIMIT' | 'MONTHLY_TOKEN_LIMIT' | 'MESSAGE_TOKEN_LIMIT';

// Statuts d'abonnement Stripe
export type SubscriptionStatusType = 'ACTIVE' | 'CANCELED' | 'PAST_DUE' | 'INCOMPLETE' | 'UNPAID';

/**
 * DTO du statut d'abonnement (réponse API)
 */
export interface SubscriptionStatusDTO {
  planName: PlanType;
  tokensRemainingThisMonth: number;
  messagesRemainingToday: number;
  perMessageTokenLimit: number;
  monthlyTokenLimit: number;
  dailyMessageLimit: number;
  tokensUsedThisMonth: number;
  messagesSentToday: number;
  monthlyResetDate: string;  // ISO 8601
  dailyResetDate: string;    // ISO 8601
  // Champs Stripe
  status: SubscriptionStatusType;
  currentPeriodEnd: string | null;  // ISO 8601
  cancelAtPeriodEnd: boolean;
  priceCents: number;
  currency: string;
  hasStripeSubscription: boolean;
  // Discount information from promotion codes
  hasActiveDiscount: boolean;
  discountDescription: string;  // Human-readable: "10% off", "€5 off for 3 months"
  discountEndDate: string | null;  // ISO 8601, null if permanent
}

/**
 * DTO pour l'erreur de quota (erreur 429)
 */
export interface QuotaErrorDTO {
  quotaType: QuotaType;
  limit: number;
  current: number;
  requested: number;
  message: string;
}

/**
 * DTO pour la requête de changement de plan
 */
export interface ChangePlanRequestDTO {
  planType: PlanType;
}

/**
 * DTO pour la requête de checkout Stripe
 */
export interface CheckoutRequestDTO {
  planType: PlanType;
}

/**
 * DTO pour la réponse de session Checkout Stripe
 */
export interface CheckoutSessionDTO {
  sessionId: string;
  url: string;
}

/**
 * DTO pour la réponse de session Portail Stripe
 */
export interface PortalSessionDTO {
  url: string;
}

/**
 * DTO d'un plan (réponse API GET /api/subscription/plans)
 */
export interface PlanDTO {
  type: PlanType;
  name: string;
  monthlyTokenLimit: number;
  dailyMessageLimit: number;
  perMessageTokenLimit: number;
  priceCents: number;
  currency: string;
}

/**
 * Récupère la configuration d'un plan par son type depuis une liste de plans
 */
export function getPlanByType(plans: PlanDTO[], planType: PlanType): PlanDTO | undefined {
  return plans.find(plan => plan.type === planType);
}

/**
 * Formate un prix en centimes vers une chaîne de caractères
 */
export function formatPrice(priceCents: number, currency: string = 'EUR'): string {
  const price = priceCents / 100;
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: currency.toUpperCase()
  }).format(price);
}

