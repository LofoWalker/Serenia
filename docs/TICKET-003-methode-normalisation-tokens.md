# TICKET-003 : Créer la méthode de normalisation dans QuotaService

## Contexte
Pour uniformiser le calcul des coûts de tokens, on doit convertir les différents types de tokens en une unité commune basée sur le coût des input tokens.

## Objectif
Ajouter une méthode privée dans `QuotaService` qui normalise les tokens selon la formule de coût.

## Fichier à modifier
`backend/src/main/java/com/lofo/serenia/service/subscription/QuotaService.java`

## Règle métier - Tarification GPT-4o-mini
| Type de token | Prix / 1M tokens | Ratio vs Input |
|---------------|------------------|----------------|
| Input         | 0.15$            | 1              |
| Cached        | 0.075$           | 0.5 (÷2)       |
| Output        | 0.60$            | 4 (×4)         |

## Formule de normalisation
```
normalizedTokens = (promptTokens - cachedTokens) + (cachedTokens / 2) + (completionTokens * 4)
```

Où :
- `promptTokens - cachedTokens` = tokens d'input non cachés (coût plein)
- `cachedTokens / 2` = tokens cachés (coût divisé par 2)
- `completionTokens * 4` = tokens de sortie (coût × 4)

## Implémentation

```java
/**
 * Normalise les tokens consommés en équivalent "input tokens" pour uniformiser le coût.
 * Basé sur la tarification GPT-4o-mini :
 * - Input : 0.15$ / 1M → facteur 1
 * - Cached : 0.075$ / 1M → facteur 0.5 (2 cached = 1 input)
 * - Output : 0.60$ / 1M → facteur 4 (1 output = 4 input)
 */
private int normalizeTokens(int promptTokens, int cachedTokens, int completionTokens) {
    int nonCachedInput = promptTokens - cachedTokens;
    int cachedNormalized = cachedTokens / 2;
    int outputNormalized = completionTokens * 4;
    return nonCachedInput + cachedNormalized + outputNormalized;
}
```

## Critères d'acceptation
- [ ] La méthode est implémentée avec la formule correcte
- [ ] La Javadoc explique la logique de normalisation
- [ ] Test unitaire vérifiant la formule (voir TICKET-006)

## Exemples de calcul (pour les tests)
| promptTokens | cachedTokens | completionTokens | normalizedTokens |
|--------------|--------------|------------------|------------------|
| 1000         | 0            | 100              | 1000 + 0 + 400 = 1400 |
| 1000         | 800          | 100              | 200 + 400 + 400 = 1000 |
| 500          | 500          | 50               | 0 + 250 + 200 = 450 |
| 0            | 0            | 100              | 0 + 0 + 400 = 400 |

## Dépendances
- Aucune

## Bloque
- TICKET-004 (recordUsage)

