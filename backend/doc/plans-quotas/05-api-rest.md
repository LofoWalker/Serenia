# Phase 5 : API REST

**Durée estimée:** 1-2h

**Prérequis:** Phases 1, 2, 3 et 4 complétées

---

## 5.1 SubscriptionResource

### Fichier à créer

`src/main/java/com/lofo/serenia/rest/resource/SubscriptionResource.java`

### Implémentation

```java
package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import com.lofo.serenia.service.subscription.SubscriptionService;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

/**
 * Resource REST pour la gestion des subscriptions et l'observabilité des quotas.
 */
@Authenticated
@Path("/api/subscription")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Subscription", description = "Manage subscription and view quota status")
public class SubscriptionResource {

    private final SubscriptionService subscriptionService;
    private final JsonWebToken jwt;

    public SubscriptionResource(SubscriptionService subscriptionService, JsonWebToken jwt) {
        this.subscriptionService = subscriptionService;
        this.jwt = jwt;
    }

    /**
     * Récupère le statut de subscription de l'utilisateur authentifié.
     * Inclut les quotas restants et les dates de reset.
     */
    @GET
    @Path("/status")
    @Operation(
            summary = "Get subscription status",
            description = "Returns the current subscription status including plan details, " +
                    "remaining quotas, and reset dates for the authenticated user."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Subscription status returned successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionStatusDTO.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "User not authenticated"
            )
    })
    public Response getStatus() {
        UUID userId = getAuthenticatedUserId();
        SubscriptionStatusDTO status = subscriptionService.getStatus(userId);
        return Response.ok(status).build();
    }

    /**
     * Récupère l'ID de l'utilisateur authentifié depuis le JWT.
     */
    private UUID getAuthenticatedUserId() {
        String subject = jwt.getSubject();
        return UUID.fromString(subject);
    }
}
```

---

## 5.2 DTOs complets

### SubscriptionStatusDTO (rappel)

**Fichier:** `src/main/java/com/lofo/serenia/rest/dto/out/SubscriptionStatusDTO.java`

```java
package com.lofo.serenia.rest.dto.out;

import java.time.LocalDateTime;

/**
 * DTO représentant le statut de subscription d'un utilisateur.
 * Fournit toutes les informations nécessaires pour l'observabilité des quotas.
 */
public record SubscriptionStatusDTO(
        /**
         * Nom du plan actuel (FREE, PLUS, MAX).
         */
        String planName,

        /**
         * Nombre de tokens restants pour le mois en cours.
         */
        int tokensRemainingThisMonth,

        /**
         * Nombre de messages restants pour aujourd'hui.
         */
        int messagesRemainingToday,

        /**
         * Limite de tokens par message individuel.
         */
        int perMessageTokenLimit,

        /**
         * Limite mensuelle de tokens.
         */
        int monthlyTokenLimit,

        /**
         * Limite journalière de messages.
         */
        int dailyMessageLimit,

        /**
         * Tokens utilisés ce mois-ci.
         */
        int tokensUsedThisMonth,

        /**
         * Messages envoyés aujourd'hui.
         */
        int messagesSentToday,

        /**
         * Date de reset du compteur mensuel.
         */
        LocalDateTime monthlyResetDate,

        /**
         * Date de reset du compteur journalier.
         */
        LocalDateTime dailyResetDate
) {
}
```

### QuotaErrorDTO (rappel)

**Fichier:** `src/main/java/com/lofo/serenia/rest/dto/out/QuotaErrorDTO.java`

```java
package com.lofo.serenia.rest.dto.out;

/**
 * DTO pour les erreurs de quota dépassé.
 * Retourné avec un status HTTP 429.
 */
public record QuotaErrorDTO(
        /**
         * Type de quota dépassé (message_token_limit, monthly_token_limit, daily_message_limit).
         */
        String quotaType,

        /**
         * Limite du quota.
         */
        int limit,

        /**
         * Valeur actuelle du compteur.
         */
        int current,

        /**
         * Valeur demandée.
         */
        int requested,

        /**
         * Message d'erreur lisible.
         */
        String message
) {
}
```

---

## 5.3 Exemples de réponses API

### GET /api/subscription/status

**Réponse 200 OK:**

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
    "dailyResetDate": "2024-12-29T10:30:00"
}
```

### POST /api/conversations/add-message (quota dépassé)

**Réponse 429 Too Many Requests:**

```json
{
    "quotaType": "daily_message_limit",
    "limit": 10,
    "current": 10,
    "requested": 1,
    "message": "Daily message limit reached: 10/10 messages sent today"
}
```

---

## 5.4 Documentation OpenAPI

### Configuration Quarkus

Vérifier que la configuration OpenAPI est présente dans `application.properties` :

```properties
# OpenAPI / Swagger UI
quarkus.swagger-ui.always-include=true
quarkus.smallrye-openapi.info-title=Serenia API
quarkus.smallrye-openapi.info-version=1.1.0
quarkus.smallrye-openapi.info-description=Serenia Chat API with Plans and Quotas
```

### Accès à la documentation

- **Swagger UI:** http://localhost:8080/q/swagger-ui
- **OpenAPI JSON:** http://localhost:8080/q/openapi
- **OpenAPI YAML:** http://localhost:8080/q/openapi?format=yaml

---

## 5.5 Mapper (optionnel)

Si la logique de mapping devient complexe, créer un mapper dédié.

**Fichier:** `src/main/java/com/lofo/serenia/mapper/SubscriptionMapper.java`

```java
package com.lofo.serenia.mapper;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

/**
 * Mapper pour les conversions Subscription <-> DTO.
 */
@ApplicationScoped
public class SubscriptionMapper {

    /**
     * Convertit une Subscription en SubscriptionStatusDTO.
     */
    public SubscriptionStatusDTO toStatusDTO(Subscription subscription) {
        Plan plan = subscription.getPlan();

        int tokensRemaining = Math.max(0,
                plan.getMonthlyTokenLimit() - subscription.getTokensUsedThisMonth());
        int messagesRemaining = Math.max(0,
                plan.getDailyMessageLimit() - subscription.getMessagesSentToday());

        LocalDateTime monthlyResetDate = subscription.getMonthlyPeriodStart().plusMonths(1);
        LocalDateTime dailyResetDate = subscription.getDailyPeriodStart().plusDays(1);

        return new SubscriptionStatusDTO(
                plan.getName().name(),
                tokensRemaining,
                messagesRemaining,
                plan.getPerMessageTokenLimit(),
                plan.getMonthlyTokenLimit(),
                plan.getDailyMessageLimit(),
                subscription.getTokensUsedThisMonth(),
                subscription.getMessagesSentToday(),
                monthlyResetDate,
                dailyResetDate
        );
    }
}
```

---

## 5.6 Tâches

- [ ] Créer `SubscriptionResource.java`
- [ ] Créer/vérifier `SubscriptionStatusDTO.java`
- [ ] Créer/vérifier `QuotaErrorDTO.java`
- [ ] Créer `SubscriptionMapper.java` (optionnel)
- [ ] Vérifier la documentation OpenAPI générée
- [ ] Tester l'endpoint manuellement

---

## 5.7 Tests manuels

```bash
# Obtenir un token JWT
TOKEN="votre_token_jwt"

# Tester l'endpoint status
curl -X GET http://localhost:8080/api/subscription/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" | jq .

# Tester sans authentification (doit retourner 401)
curl -X GET http://localhost:8080/api/subscription/status \
  -H "Accept: application/json" -v
```

---

[← Phase précédente : Intégration](./04-integration.md) | [Retour au README](./README.md) | [Phase suivante : Tests unitaires →](./06-tests-unitaires.md)

