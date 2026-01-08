# TICKET-002 : Adapter parseCompletionAndReturnResult

## Contexte
La méthode `parseCompletionAndReturnResult` extrait les informations de la réponse OpenAI. Elle doit maintenant extraire les 3 valeurs de tokens distinctes depuis `CompletionUsage`.

## Objectif
Modifier le parsing pour extraire `promptTokens`, `cachedTokens` et `completionTokens` depuis la réponse OpenAI.

## Fichier à modifier
`backend/src/main/java/com/lofo/serenia/service/chat/ChatCompletionService.java`

## Structure de CompletionUsage (référence)
```
CompletionUsage{
    completionTokens=63, 
    promptTokens=538, 
    totalTokens=601, 
    promptTokensDetails=PromptTokensDetails{
        audioTokens=0, 
        cachedTokens=0,  // <-- Valeur à extraire
        additionalProperties={}
    },
    ...
}
```

## Modification

### Avant
```java
private static @NotNull ChatCompletionResult parseCompletionAndReturnResult(ChatCompletion completion) {
    String content = "";
    int totalTokensUsed = 0;

    if (!completion.choices().isEmpty()) {
        content = completion.choices().getFirst().message().content().orElse("");
    }

    if (completion.usage().isPresent()) {
        totalTokensUsed = Math.toIntExact(completion.usage().get().totalTokens());
        log.debug("{} tokens used by OpenAI API", totalTokensUsed);
    } else {
        log.warn("OpenAI API did not return usage information");
    }

    return new ChatCompletionResult(content, totalTokensUsed);
}
```

### Après
```java
private static @NotNull ChatCompletionResult parseCompletionAndReturnResult(ChatCompletion completion) {
    String content = "";
    int promptTokens = 0;
    int cachedTokens = 0;
    int completionTokens = 0;

    if (!completion.choices().isEmpty()) {
        content = completion.choices().getFirst().message().content().orElse("");
    }

    if (completion.usage().isPresent()) {
        CompletionUsage usage = completion.usage().get();
        promptTokens = Math.toIntExact(usage.promptTokens());
        completionTokens = Math.toIntExact(usage.completionTokens());
        
        if (usage.promptTokensDetails() != null) {
            cachedTokens = Math.toIntExact(usage.promptTokensDetails().cachedTokens());
        }
        
        log.debug("Tokens - Prompt: {}, Cached: {}, Completion: {}", 
                  promptTokens, cachedTokens, completionTokens);
    } else {
        log.warn("OpenAI API did not return usage information");
    }

    return new ChatCompletionResult(content, promptTokens, cachedTokens, completionTokens);
}
```

## Bug à corriger
Supprimer la ligne de code incorrecte dans `generateReply()` :
```java
completion.usage().get().   // <-- Ligne incomplète à supprimer
log.info(completion.usage().get().toString());
```

Remplacer par :
```java
log.debug("OpenAI Usage: {}", completion.usage().orElse(null));
```

## Critères d'acceptation
- [ ] Les 3 valeurs sont extraites correctement
- [ ] `cachedTokens` vaut 0 si `promptTokensDetails` est null
- [ ] Le bug de syntaxe est corrigé
- [ ] Le code compile sans erreur

## Dépendances
- TICKET-001 (record modifié)

## Bloque
- TICKET-005 (ChatOrchestrator)

