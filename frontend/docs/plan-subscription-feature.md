# Plan d'implémentation - Fonctionnalités Abonnement & Quotas

> **Date :** 2025-12-28  
> **Version :** 1.0.0  
> **Référence :** [API-CONTRACT.md](/backend/doc/API-CONTRACT.md)  
> **Statut :** ✅ Implémenté

---

## Table des matières

1. [Résumé](#1-résumé)
2. [Analyse de l'existant](#2-analyse-de-lexistant)
3. [Nouveaux endpoints API à intégrer](#3-nouveaux-endpoints-api-à-intégrer)
4. [Plan d'action détaillé](#4-plan-daction-détaillé)
   - [Étape 1 : Création des modèles de données](#étape-1--création-des-modèles-de-données)
   - [Étape 2 : Création du service SubscriptionService](#étape-2--création-du-service-subscriptionservice)
   - [Étape 3 : Adaptation du modèle User](#étape-3--adaptation-du-modèle-user)
   - [Étape 4 : Gestion des erreurs de quota](#étape-4--gestion-des-erreurs-de-quota)
   - [Étape 5 : Affichage des informations d'abonnement dans le profil](#étape-5--affichage-des-informations-dabonnement-dans-le-profil)
   - [Étape 6 : Création du composant de sélection de plan](#étape-6--création-du-composant-de-sélection-de-plan)
   - [Étape 7 : Affichage des quotas dans le chat](#étape-7--affichage-des-quotas-dans-le-chat)
5. [Fichiers à créer](#5-fichiers-à-créer)
6. [Fichiers à modifier](#6-fichiers-à-modifier)
7. [Ordre d'exécution recommandé](#7-ordre-dexécution-recommandé)
8. [Considérations techniques](#8-considérations-techniques)

---

## 1. Résumé

Ce plan décrit l'intégration des fonctionnalités d'abonnement et de quotas dans le frontend Angular de Serenia. L'objectif est de :

- **Adapter** les modèles existants pour correspondre à l'API Contract
- **Créer** les nouveaux DTOs pour les abonnements et quotas
- **Implémenter** un service dédié pour les endpoints d'abonnement
- **Afficher** les informations de consommation (tokens, messages)
- **Permettre** le changement de plan utilisateur

---

## 2. Analyse de l'existant

### Modèles actuels

**`user.model.ts`** - Modèles d'authentification :
```typescript
// Existants et conformes à l'API :
- User (id, lastName, firstName, email, roles)
- AuthResponse (user, token)
- RegistrationRequest, LoginRequest
- ApiMessageResponse
- ForgotPasswordRequest, ResetPasswordRequest
```

**Point d'attention :** Le champ `roles` dans `User` est un tableau, mais l'API retourne `role` (string). À adapter.

**`chat.model.ts`** - Modèles de conversation :
```typescript
// Existants et conformes à l'API :
- MessageRequest (content)
- MessageResponse (conversationId, role, content)
- ChatMessage (role, content, timestamp?)
- ConversationMessagesResponse (conversationId, messages)
```

### Services actuels

- **`auth.service.ts`** : Gère l'authentification (login, register, activate, password)
- **`auth-state.service.ts`** : Gère l'état d'authentification avec signals
- **`chat.service.ts`** : Gère les conversations et messages

### Composants concernés

- **`profile.component`** : Affiche le profil utilisateur → À enrichir avec les infos d'abonnement
- **`chat.component`** : Gère les conversations → À enrichir avec les erreurs de quota

---

## 3. Nouveaux endpoints API à intégrer

### GET `/api/subscription/status`
Récupère le statut de l'abonnement et les quotas de l'utilisateur.

**Réponse 200 OK :**
```json
{
  "planName": "FREE",
  "tokensRemainingThisMonth": 8500,
  "messagesRemainingToday": 7,
  "perMessageTokenLimit": 1000,
  "monthlyTokenLimit": 10000,
  "dailyMessageLimit": 10,
  "tokensUsedThisMonth": 1500,
  "messagesSentToday": 3,
  "monthlyResetDate": "2025-01-28T10:30:00",
  "dailyResetDate": "2025-12-29T10:30:00"
}
```

### PUT `/api/subscription/plan`
Change le plan d'abonnement de l'utilisateur.

**Requête :**
```json
{
  "planType": "PLUS"
}
```

**Réponse 200 OK :** Même structure que `GET /api/subscription/status`

### Erreur 429 (Quota dépassé)
Retournée par `POST /api/conversations/add-message` si quota dépassé.

```json
{
  "quotaType": "DAILY_MESSAGE_LIMIT",
  "limit": 10,
  "current": 10,
  "requested": 1,
  "message": "Limite quotidienne de messages atteinte"
}
```

---

## 4. Plan d'action détaillé

### Étape 1 : Création des modèles de données

**Fichier à créer :** `src/app/core/models/subscription.model.ts`

**Contenu :**
```typescript
// Enum pour les types de plan
export type PlanType = 'FREE' | 'PLUS' | 'MAX';

// Enum pour les types de quota
export type QuotaType = 'DAILY_MESSAGE_LIMIT' | 'MONTHLY_TOKEN_LIMIT' | 'MESSAGE_TOKEN_LIMIT';

// DTO du statut d'abonnement (réponse API)
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

// DTO pour l'erreur de quota (erreur 429)
export interface QuotaErrorDTO {
  quotaType: QuotaType;
  limit: number;
  current: number;
  requested: number;
  message: string;
}

// DTO pour la requête de changement de plan
export interface ChangePlanRequestDTO {
  planType: PlanType;
}

// Configuration des plans (pour l'affichage)
export interface PlanConfig {
  type: PlanType;
  name: string;
  monthlyTokenLimit: number;
  dailyMessageLimit: number;
  perMessageTokenLimit: number;
  price: string;
  features: string[];
}

// Constante avec les configurations des plans
export const PLAN_CONFIGS: PlanConfig[] = [
  {
    type: 'FREE',
    name: 'Gratuit',
    monthlyTokenLimit: 10000,
    dailyMessageLimit: 10,
    perMessageTokenLimit: 1000,
    price: '0€',
    features: ['10 messages/jour', '10 000 tokens/mois', '1 000 tokens/message']
  },
  {
    type: 'PLUS',
    name: 'Plus',
    monthlyTokenLimit: 100000,
    dailyMessageLimit: 50,
    perMessageTokenLimit: 4000,
    price: '9,99€/mois',
    features: ['50 messages/jour', '100 000 tokens/mois', '4 000 tokens/message']
  },
  {
    type: 'MAX',
    name: 'Max',
    monthlyTokenLimit: 500000,
    dailyMessageLimit: 200,
    perMessageTokenLimit: 8000,
    price: '29,99€/mois',
    features: ['200 messages/jour', '500 000 tokens/mois', '8 000 tokens/message']
  }
];
```

---

### Étape 2 : Création du service SubscriptionService

**Fichier à créer :** `src/app/core/services/subscription.service.ts`

**Responsabilités :**
- Appeler les endpoints d'abonnement
- Gérer l'état de l'abonnement avec signals (pattern similaire à `AuthStateService`)
- Exposer des computed signals pour les données dérivées

**Structure du service :**
```typescript
@Injectable({ providedIn: 'root' })
export class SubscriptionService {
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
    if (!s) return 0;
    return (s.tokensUsedThisMonth / s.monthlyTokenLimit) * 100;
  });
  readonly messagesUsagePercent = computed(() => {
    const s = this.statusSignal();
    if (!s) return 0;
    return (s.messagesSentToday / s.dailyMessageLimit) * 100;
  });

  // Méthodes
  getStatus(): Observable<SubscriptionStatusDTO>;
  changePlan(planType: PlanType): Observable<SubscriptionStatusDTO>;
  refreshStatus(): void; // Rafraîchit le statut et met à jour le signal
  clearStatus(): void;   // Réinitialise l'état (lors du logout)
}
```

**Fichier de test à créer :** `src/app/core/services/subscription.service.spec.ts`

---

### Étape 3 : Adaptation du modèle User

**Fichier à modifier :** `src/app/core/models/user.model.ts`

**Modification :**
```typescript
// AVANT
export interface User {
  id: string;
  lastName: string;
  firstName: string;
  email: string;
  roles: string[];  // Array
}

// APRÈS
export interface User {
  id: string;
  lastName: string;
  firstName: string;
  email: string;
  role: string;  // String unique selon l'API Contract
}
```

**Impact :** Vérifier et adapter tous les usages de `user.roles` dans l'application.

---

### Étape 4 : Gestion des erreurs de quota

**Fichier à modifier :** `src/app/features/chat/chat.component.ts`

**Modifications :**
1. Importer `QuotaErrorDTO` et `QuotaType`
2. Créer un signal pour stocker l'erreur de quota
3. Dans `onSendMessage()`, intercepter les erreurs HTTP 429
4. Parser le body de l'erreur en `QuotaErrorDTO`
5. Afficher un message d'erreur adapté selon le `quotaType`

**Exemple de gestion :**
```typescript
protected readonly quotaError = signal<QuotaErrorDTO | null>(null);

catchError((error: HttpErrorResponse) => {
  if (error.status === 429) {
    const quotaError = error.error as QuotaErrorDTO;
    this.quotaError.set(quotaError);
    this.errorMessage.set(this.getQuotaErrorMessage(quotaError));
  }
  // ...
})

private getQuotaErrorMessage(error: QuotaErrorDTO): string {
  switch (error.quotaType) {
    case 'DAILY_MESSAGE_LIMIT':
      return `Limite quotidienne atteinte (${error.current}/${error.limit} messages). Réessayez demain ou passez à un plan supérieur.`;
    case 'MONTHLY_TOKEN_LIMIT':
      return `Limite mensuelle de tokens atteinte. Passez à un plan supérieur pour continuer.`;
    case 'MESSAGE_TOKEN_LIMIT':
      return `Votre message est trop long. Limite : ${error.limit} tokens.`;
    default:
      return error.message;
  }
}
```

**Fichier à modifier :** `src/app/features/chat/chat.component.html`

Ajouter un bouton/lien vers la page de profil pour changer de plan si quota dépassé.

---

### Étape 5 : Affichage des informations d'abonnement dans le profil

**Fichier à modifier :** `src/app/features/profile/profile.component.ts`

**Modifications :**
1. Injecter `SubscriptionService`
2. Charger le statut d'abonnement dans `ngOnInit()`
3. Ajouter des signals pour gérer l'état du changement de plan

**Fichier à modifier :** `src/app/features/profile/profile.component.html`

**Ajouts UI :**
```html
<!-- Section Mon Abonnement -->
<div class="mt-8 bg-primary-900 border border-primary-800 rounded-xl p-6">
  <h3 class="text-lg font-semibold text-primary-300 mb-4">Mon abonnement</h3>
  
  <!-- Badge du plan actuel -->
  <div class="flex items-center gap-2 mb-6">
    <span class="px-3 py-1 bg-primary-700 text-primary-200 rounded-full text-sm font-medium">
      {{ subscriptionService.planName() }}
    </span>
  </div>

  <!-- Barre de progression Tokens mensuels -->
  <div class="mb-4">
    <div class="flex justify-between text-sm mb-1">
      <span class="text-primary-400">Tokens ce mois</span>
      <span class="text-primary-300">
        {{ status()?.tokensUsedThisMonth }} / {{ status()?.monthlyTokenLimit }}
      </span>
    </div>
    <div class="w-full bg-primary-700 rounded-full h-2">
      <div class="bg-accent-500 h-2 rounded-full" 
           [style.width.%]="subscriptionService.tokensUsagePercent()">
      </div>
    </div>
  </div>

  <!-- Barre de progression Messages quotidiens -->
  <div class="mb-4">
    <div class="flex justify-between text-sm mb-1">
      <span class="text-primary-400">Messages aujourd'hui</span>
      <span class="text-primary-300">
        {{ status()?.messagesSentToday }} / {{ status()?.dailyMessageLimit }}
      </span>
    </div>
    <div class="w-full bg-primary-700 rounded-full h-2">
      <div class="bg-accent-500 h-2 rounded-full" 
           [style.width.%]="subscriptionService.messagesUsagePercent()">
      </div>
    </div>
  </div>

  <!-- Dates de réinitialisation -->
  <div class="text-xs text-primary-500 mt-4">
    <p>Reset quotidien : {{ status()?.dailyResetDate | date:'short' }}</p>
    <p>Reset mensuel : {{ status()?.monthlyResetDate | date:'short' }}</p>
  </div>
</div>
```

---

### Étape 6 : Création du composant de sélection de plan

**Option A : Intégré dans le profil**

Ajouter directement dans `profile.component.html` une section avec les cartes de plan.

**Option B : Composant réutilisable (recommandé)**

**Fichier à créer :** `src/app/shared/ui/plan-selector/plan-selector.component.ts`

**Structure :**
```typescript
@Component({
  selector: 'app-plan-selector',
  standalone: true,
  // ...
})
export class PlanSelectorComponent {
  @Input() currentPlan: PlanType = 'FREE';
  @Output() planSelected = new EventEmitter<PlanType>();
  
  readonly plans = PLAN_CONFIGS;
  
  selectPlan(plan: PlanType): void {
    if (plan !== this.currentPlan) {
      this.planSelected.emit(plan);
    }
  }
}
```

**Fichiers à créer :**
- `src/app/shared/ui/plan-selector/plan-selector.component.ts`
- `src/app/shared/ui/plan-selector/plan-selector.component.html`
- `src/app/shared/ui/plan-selector/plan-selector.component.css`

**Affichage :** Cartes côte à côte avec :
- Nom du plan
- Prix
- Liste des features
- Bouton "Sélectionner" (désactivé si plan actuel)
- Badge "Actuel" sur le plan en cours

---

### Étape 7 : Affichage des quotas dans le chat (optionnel)

**Objectif :** Afficher un indicateur compact des messages restants dans la zone de chat.

**Fichier à modifier :** `src/app/features/chat/chat.component.ts`

**Modifications :**
1. Injecter `SubscriptionService`
2. Charger le statut au chargement du composant
3. Rafraîchir après chaque message envoyé

**Fichier à modifier :** `src/app/features/chat/chat.component.html`

**Ajout :**
```html
<!-- Indicateur compact au-dessus de l'input -->
@if (subscriptionService.status(); as status) {
  <div class="text-xs text-primary-500 mb-2 text-center">
    {{ status.messagesRemainingToday }} messages restants aujourd'hui
  </div>
}
```

---

## 5. Fichiers à créer

| Fichier | Description |
|---------|-------------|
| `src/app/core/models/subscription.model.ts` | DTOs et types pour les abonnements |
| `src/app/core/services/subscription.service.ts` | Service pour les endpoints d'abonnement |
| `src/app/core/services/subscription.service.spec.ts` | Tests unitaires du service |
| `src/app/shared/ui/plan-selector/plan-selector.component.ts` | Composant de sélection de plan |
| `src/app/shared/ui/plan-selector/plan-selector.component.html` | Template du sélecteur de plan |
| `src/app/shared/ui/plan-selector/plan-selector.component.css` | Styles du sélecteur de plan |

---

## 6. Fichiers à modifier

| Fichier | Modifications |
|---------|---------------|
| `src/app/core/models/user.model.ts` | Changer `roles: string[]` en `role: string` |
| `src/app/features/profile/profile.component.ts` | Ajouter injection SubscriptionService, charger statut |
| `src/app/features/profile/profile.component.html` | Ajouter section abonnement + sélecteur de plan |
| `src/app/features/chat/chat.component.ts` | Gérer erreurs 429, ajouter signal quotaError |
| `src/app/features/chat/chat.component.html` | Afficher erreur quota avec CTA, indicateur messages restants |
| `src/app/core/services/auth-state.service.ts` | (potentiellement) Appeler `subscriptionService.clearStatus()` au logout |

---

## 7. Ordre d'exécution recommandé

```
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 1 : FONDATIONS                         │
├─────────────────────────────────────────────────────────────────┤
│ 1.1. Créer subscription.model.ts (DTOs, types, constantes)      │
│ 1.2. Adapter user.model.ts (roles → role)                       │
│ 1.3. Vérifier/adapter les usages de User dans l'app             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 2 : SERVICE                            │
├─────────────────────────────────────────────────────────────────┤
│ 2.1. Créer subscription.service.ts                              │
│ 2.2. Implémenter getStatus()                                    │
│ 2.3. Implémenter changePlan()                                   │
│ 2.4. Ajouter les computed signals                               │
│ 2.5. Créer les tests unitaires                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 3 : COMPOSANTS UI                      │
├─────────────────────────────────────────────────────────────────┤
│ 3.1. Créer plan-selector.component                              │
│ 3.2. Enrichir profile.component avec section abonnement         │
│ 3.3. Intégrer plan-selector dans profile                        │
│ 3.4. Connecter le changement de plan au service                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 4 : GESTION DES QUOTAS                 │
├─────────────────────────────────────────────────────────────────┤
│ 4.1. Ajouter gestion erreur 429 dans chat.component.ts          │
│ 4.2. Afficher message d'erreur adapté avec CTA                  │
│ 4.3. (Optionnel) Afficher indicateur messages restants          │
│ 4.4. (Optionnel) Rafraîchir quotas après chaque message         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 5 : FINALISATION                       │
├─────────────────────────────────────────────────────────────────┤
│ 5.1. Intégrer clearStatus() au logout                           │
│ 5.2. Tests E2E (si applicables)                                 │
│ 5.3. Revue de code et nettoyage                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. Considérations techniques

### Rafraîchissement des quotas

- **Au chargement** : Le statut est chargé une fois lors de l'accès au profil ou au chat
- **Après message** : Optionnel - rafraîchir le statut après chaque message pour des quotas à jour
- **Cache** : Le signal stocke le statut, évitant des appels redondants

### Gestion d'erreurs

- **401** : Session expirée → rediriger vers login
- **404** : Subscription non trouvée → erreur système à logger
- **429** : Quota dépassé → afficher message explicite avec suggestion d'upgrade

### Performance

- Utiliser `ChangeDetectionStrategy.OnPush` sur tous les composants
- Les signals permettent une détection de changement optimisée
- Éviter les appels API inutiles grâce au cache du service

### Sécurité

- Les endpoints sont protégés par JWT (header `Authorization: Bearer <token>`)
- L'intercepteur `auth.interceptor.ts` devrait déjà ajouter le token automatiquement

### Accessibilité

- Ajouter des `aria-label` sur les barres de progression
- Utiliser des couleurs avec contraste suffisant
- S'assurer que le sélecteur de plan est navigable au clavier

---

## Notes complémentaires

### Questions à clarifier

1. **Design du sélecteur de plan** : Cartes côte à côte ou modale dédiée ?
2. **Paiement** : Y a-t-il une intégration de paiement à prévoir pour les plans payants ?
3. **Notifications** : Faut-il notifier l'utilisateur quand il approche de sa limite ?

### Dépendances

- Aucune nouvelle dépendance npm requise
- Utilisation des pipes Angular natifs (`DatePipe`)

---

*Ce plan est un document vivant. Il peut être ajusté en fonction des retours et des découvertes lors de l'implémentation.*

