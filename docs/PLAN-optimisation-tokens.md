# Plan d'optimisation de la consommation de tokens

## Contexte
Le système de chat actuel récupère les 16 messages les plus récents et les envoie à GPT-4o-mini. La consommation de tokens augmente de manière exponentielle avec la longueur des conversations.

## Objectifs
1. **Exploiter le caching OpenAI** pour réduire les coûts (préfixe stable = 50% de réduction)
2. **Normaliser le calcul des tokens** pour refléter le coût réel

## Architecture cible

### Structure des messages envoyés à OpenAI
```
┌─────────────────────────────────────────────────┐
│ 1. System prompt                                │  ← Préfixe stable (cachable)
├─────────────────────────────────────────────────┤
│ 2. Messages 1 à 12 (historique stable)          │  ← Préfixe stable (cachable)
├─────────────────────────────────────────────────┤
│ 3. Messages 13 à 16 (4 derniers)                │  ← Variable (non caché)
└─────────────────────────────────────────────────┘
```

### Formule de normalisation
```
normalizedTokens = (promptTokens - cachedTokens) + (cachedTokens / 2) + (completionTokens * 4)
```

Basée sur la tarification GPT-4o-mini :
| Type | Prix / 1M tokens | Facteur |
|------|------------------|---------|
| Input | 0.15$ | ×1 |
| Cached | 0.075$ | ×0.5 |
| Output | 0.60$ | ×4 |

## Tickets

| # | Titre | Fichier(s) | Dépendances |
|---|-------|------------|-------------|
| [001](TICKET-001-modifier-chat-completion-result.md) | Modifier ChatCompletionResult | ChatCompletionService.java | - |
| [002](TICKET-002-adapter-parsing-completion.md) | Adapter parseCompletionAndReturnResult | ChatCompletionService.java | 001 |
| [003](TICKET-003-methode-normalisation-tokens.md) | Créer la méthode normalizeTokens | QuotaService.java | - |
| [004](TICKET-004-adapter-record-usage.md) | Adapter recordUsage() | QuotaService.java | 003 |
| [005](TICKET-005-adapter-chat-orchestrator.md) | Adapter ChatOrchestrator | ChatOrchestrator.java | 001, 002, 004 |
| [006](TICKET-006-mise-a-jour-tests.md) | Mettre à jour les tests | *Test.java | 001-005 |

## Ordre d'exécution recommandé

```
TICKET-001 ──┬──► TICKET-002 ──┐
             │                 │
TICKET-003 ──┴──► TICKET-004 ──┴──► TICKET-005 ──► TICKET-006
```

Parallélisation possible :
- TICKET-001 et TICKET-003 peuvent être faits en parallèle
- TICKET-002 et TICKET-004 peuvent être faits en parallèle (après leurs dépendances)

## Notes techniques

### Caching OpenAI
- Le cache s'active automatiquement quand le préfixe atteint ~1024 tokens
- Durée de vie du cache : 5-10 minutes d'inactivité
- En début de conversation (peu de messages), le cache peut ne pas s'activer → acceptable car la consommation est faible

### Structure CompletionUsage
```java
CompletionUsage{
    completionTokens=63, 
    promptTokens=538, 
    promptTokensDetails=PromptTokensDetails{
        cachedTokens=0,  // <-- Valeur clé
    }
}
```

## Validation finale
```bash
cd backend
mvn test
```

Tous les tests doivent passer avant de considérer l'implémentation comme terminée.

