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
 * Configuration d'un plan (pour l'affichage)
 */
export interface PlanConfig {
  type: PlanType;
  name: string;
  monthlyTokenLimit: number;
  dailyMessageLimit: number;
  perMessageTokenLimit: number;
  price: string;
  features: string[];
  recommended?: boolean;
}

/**
 * Configurations des plans disponibles
 */
export const PLAN_CONFIGS: PlanConfig[] = [
  {
    type: 'FREE',
    name: 'Gratuit',
    monthlyTokenLimit: 10000,
    dailyMessageLimit: 10,
    perMessageTokenLimit: 1000,
    price: '0€',
    features: [
      '10 messages par jour',
      '10 000 tokens par mois',
      '1 000 tokens par message'
    ]
  },
  {
    type: 'PLUS',
    name: 'Plus',
    monthlyTokenLimit: 100000,
    dailyMessageLimit: 50,
    perMessageTokenLimit: 4000,
    price: '9,99€/mois',
    features: [
      '50 messages par jour',
      '100 000 tokens par mois',
      '4 000 tokens par message'
    ],
    recommended: true
  },
  {
    type: 'MAX',
    name: 'Max',
    monthlyTokenLimit: 500000,
    dailyMessageLimit: 200,
    perMessageTokenLimit: 8000,
    price: '29,99€/mois',
    features: [
      '200 messages par jour',
      '500 000 tokens par mois',
      '8 000 tokens par message'
    ]
  }
];

/**
 * Récupère la configuration d'un plan par son type
 */
export function getPlanConfig(planType: PlanType): PlanConfig | undefined {
  return PLAN_CONFIGS.find(plan => plan.type === planType);
}

