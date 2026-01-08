# TICKET-005 : Adapter ChatOrchestrator

## Contexte
`ChatOrchestrator` appelle `quotaService.recordUsage()` avec l'ancienne signature. Il doit être mis à jour pour passer les 3 valeurs de tokens.

## Objectif
Mettre à jour l'appel à `recordUsage()` dans `ChatOrchestrator` pour utiliser la nouvelle signature.

## Fichier à modifier
`backend/src/main/java/com/lofo/serenia/service/chat/ChatOrchestrator.java`

## Modification

### Avant (ligne ~43)
```java
quotaService.recordUsage(userId, completionResult.totalTokensUsed());
```

### Après
```java
quotaService.recordUsage(userId, 
    completionResult.promptTokens(), 
    completionResult.cachedTokens(), 
    completionResult.completionTokens());
```

## Contexte complet de la méthode
```java
@Transactional
public ProcessedMessageResult processUserMessage(UUID userId, String content) {
    Conversation conv = conversationService.getOrCreateActiveConversation(userId);

    quotaService.checkQuotaBeforeCall(userId);

    messageService.persistUserMessage(userId, conv.getId(), content);

    List<ChatMessage> history = messageService.decryptConversationMessages(userId, conv.getId());
    ChatCompletionService.ChatCompletionResult completionResult = chatCompletionService.generateReply(
            sereniaConfig.systemPrompt(),
            history
    );

    Message assistantMsg = messageService.persistAssistantMessage(userId, conv.getId(), completionResult.content());

    // MODIFICATION ICI
    quotaService.recordUsage(userId, 
        completionResult.promptTokens(), 
        completionResult.cachedTokens(), 
        completionResult.completionTokens());

    ChatMessage chatMessage = new ChatMessage(assistantMsg.getRole(), completionResult.content());

    return new ProcessedMessageResult(conv.getId(), chatMessage);
}
```

## Critères d'acceptation
- [ ] L'appel à `recordUsage()` utilise les 3 nouveaux paramètres
- [ ] Le code compile sans erreur
- [ ] Les tests de `ChatOrchestratorTest` sont mis à jour (voir TICKET-006)

## Dépendances
- TICKET-001 (record modifié)
- TICKET-002 (parsing)
- TICKET-004 (nouvelle signature recordUsage)

## Bloque
- Aucun

