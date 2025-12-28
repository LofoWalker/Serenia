/**
 * Types et modèles pour la gestion des abonnements et quotas
 */

// Types de plan disponibles
export type PlanType = 'FREE' | 'PLUS' | 'MAX';

// Types de quota pour les erreurs
export type QuotaType = 'DAILY_MESSAGE_LIMIT' | 'MONTHLY_TOKEN_LIMIT' | 'MESSAGE_TOKEN_LIMIT';

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
 * DTO d'un plan (réponse API GET /api/subscription/plans)
 */
export interface PlanDTO {
  type: PlanType;
  name: string;
  monthlyTokenLimit: number;
  dailyMessageLimit: number;
  perMessageTokenLimit: number;
}

/**
 * Récupère la configuration d'un plan par son type depuis une liste de plans
 */
export function getPlanByType(plans: PlanDTO[], planType: PlanType): PlanDTO | undefined {
  return plans.find(plan => plan.type === planType);
}

