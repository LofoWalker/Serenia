# 📋 Plan de Développement — Gestion des Plans & Quotas d'Usage (MVP)

**Projet:** Serenia  
**Version:** v1.1 (MVP)  
**Date:** 2025-12

---

## 📌 Vue d'ensemble

Ce plan détaille l'implémentation d'un système de gestion des plans et quotas d'usage pour Serenia. Le système permettra de limiter :
- Les tokens par message
- Les tokens consommés par mois
- Les messages envoyés par jour

---

## 📂 Structure du plan

Le plan est divisé en 8 phases, chacune documentée dans un fichier séparé :

| Phase | Fichier | Description |
|-------|---------|-------------|
| 1 | [01-modele-donnees.md](./01-modele-donnees.md) | Modèle de données et migrations Liquibase |
| 2 | [02-repositories.md](./02-repositories.md) | Couche Repository |
| 3 | [03-services.md](./03-services.md) | Couche Service (logique métier) |
| 4 | [04-integration.md](./04-integration.md) | Intégration avec le code existant |
| 5 | [05-api-rest.md](./05-api-rest.md) | API REST et DTOs |
| 6 | [06-tests-unitaires.md](./06-tests-unitaires.md) | Tests unitaires |
| 7 | [07-tests-integration.md](./07-tests-integration.md) | Tests d'intégration |
| 8 | [08-configuration.md](./08-configuration.md) | Configuration et finalisation |

---

## 📋 Résumé des livrables

### Fichiers à créer (14 fichiers)

| Type | Fichier | Description |
|------|---------|-------------|
| Migration | `02-plans-subscriptions.yaml` | Tables plans et subscriptions |
| Entity | `Plan.java` | Définition d'un plan |
| Entity | `Subscription.java` | État de consommation utilisateur |
| Enum | `PlanType.java` | Types de plans |
| Enum | `QuotaType.java` | Types de quotas |
| Repository | `PlanRepository.java` | Accès aux plans |
| Repository | `SubscriptionRepository.java` | Accès aux subscriptions |
| Service | `TokenCountingService.java` | Comptage des tokens (strlen) |
| Service | `QuotaService.java` | Vérification et enregistrement des quotas |
| Service | `SubscriptionService.java` | Gestion des subscriptions |
| Exception | `QuotaExceededException.java` | Exception quota dépassé |
| DTO | `SubscriptionStatusDTO.java` | Statut de subscription |
| DTO | `QuotaErrorDTO.java` | Erreur de quota |
| Resource | `SubscriptionResource.java` | API REST subscription |

### Fichiers à modifier (5 fichiers)

| Fichier | Modification |
|---------|-------------|
| `changelog.xml` | Ajouter include de `02-plans-subscriptions.yaml` |
| `ChatOrchestrator.java` | Intégrer vérification et enregistrement des quotas |
| `RegistrationService.java` | Créer subscription à l'inscription |
| `ChatOrchestratorTest.java` | Ajouter tests et mocks |
| `ConversationResourceIT.java` | Adapter setup pour plans/subscriptions |

### Tests à créer (5 classes)

| Classe | Type | Description |
|--------|------|-------------|
| `QuotaServiceTest.java` | Unitaire | Tests du service de quotas |
| `TokenCountingServiceTest.java` | Unitaire | Tests de comptage de tokens |
| `SubscriptionServiceTest.java` | Unitaire | Tests du service subscription |
| `SubscriptionResourceIT.java` | Intégration | Tests API REST |
| `QuotaEnforcementIT.java` | Intégration | Tests des limites de quota |

---

## ⏱️ Estimation du temps

| Phase | Durée estimée |
|-------|---------------|
| Phase 1 : Modèle de données | 2-3h |
| Phase 2 : Repositories | 1-2h |
| Phase 3 : Services | 4-5h |
| Phase 4 : Intégration | 2-3h |
| Phase 5 : API REST | 1-2h |
| Phase 6 : Tests unitaires | 3-4h |
| Phase 7 : Tests d'intégration | 3-4h |
| Phase 8 : Configuration | 0.5h |
| **Total** | **17-24h** |

---

## 🔄 Ordre d'exécution recommandé

```
Phase 1 → Phase 2 → Phase 3 → Phase 6 (tests unitaires) → Phase 4 → Phase 5 → Phase 7 → Phase 8
```

1. **Phase 1** → Modèle de données (base pour tout le reste)
2. **Phase 2** → Repositories (nécessaire pour les services)
3. **Phase 3** → Services (logique métier)
4. **Phase 6** → Tests unitaires des services (TDD)
5. **Phase 4** → Intégration avec code existant
6. **Phase 5** → API REST
7. **Phase 7** → Tests d'intégration
8. **Phase 8** → Configuration finale

---

## ⚠️ Points d'attention

1. **Atomicité** : Utiliser `@Lock(LockModeType.PESSIMISTIC_WRITE)` ou des requêtes UPDATE atomiques
2. **Transactions** : S'assurer que `@Transactional` est bien propagé
3. **Reset des périodes** : Gérer le cas où le reset se fait pendant une requête
4. **Concurrence** : Tester avec des requêtes parallèles
5. **Migration** : Gérer les users existants sans subscription (créer avec plan FREE)

---

## 🔧 Principes de configuration

### Source unique de vérité : la base de données

- ❌ **Pas** de configuration dans `application.properties` pour les plans
- ❌ **Pas** de variables d'environnement pour les quotas
- ✅ Valeurs initialisées via migration Liquibase
- ✅ Modifications directement en base (effet immédiat)

### Calcul des tokens (MVP)

```
tokens_consommés = strlen(message_utilisateur) + strlen(réponse_assistant)
```

**Post-MVP** : Utiliser `usage.total_tokens` retourné par l'API OpenAI.

---

## 📚 Références

- [PRD Original](../PRD-plans-quotas.md)
- Documentation Quarkus Panache
- Documentation Liquibase

