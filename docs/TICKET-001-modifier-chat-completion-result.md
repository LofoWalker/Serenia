# TICKET-001 : Modifier ChatCompletionResult

## Contexte
Le record `ChatCompletionResult` retourne actuellement `totalTokensUsed`. Pour permettre la normalisation des tokens dans `QuotaService`, il doit exposer les 3 valeurs brutes retournées par OpenAI.

## Objectif
Remplacer le champ unique `totalTokensUsed` par les 3 valeurs distinctes nécessaires au calcul de normalisation.

## Fichier à modifier
`backend/src/main/java/com/lofo/serenia/service/chat/ChatCompletionService.java`

## Modification

### Avant
```java
public record ChatCompletionResult(String content, int totalTokensUsed) {}
```

### Après
```java
public record ChatCompletionResult(String content, int promptTokens, int cachedTokens, int completionTokens) {}
```

## Critères d'acceptation
- [ ] Le record contient les 4 champs : `content`, `promptTokens`, `cachedTokens`, `completionTokens`
- [ ] Le code compile sans erreur
- [ ] Les tests unitaires existants sont mis à jour (voir TICKET-006)

## Dépendances
- Aucune

## Bloque
- TICKET-002 (parsing)
- TICKET-005 (ChatOrchestrator)

