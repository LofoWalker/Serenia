# Step 1.1 : Implémentation du Rate Limiting

## Contexte

L'application Serenia expose des endpoints critiques sans protection contre les abus :
- **Authentification** (`/auth/login`) : Vulnérable aux attaques brute-force
- **Chat IA** (`/conversations/add-message`) : Vulnérable aux attaques DoS et abus de quotas OpenAI

### État actuel

```java
// AuthenticationResource.java - AUCUN rate limiting
@POST
@Consumes(MediaType.APPLICATION_JSON)
public Response login(@Valid LoginRequestDTO dto) {
    UserResponseDTO userProfile = authenticationService.login(dto);
    String token = jwtService.generateToken(userProfile);
    return Response.ok(new AuthResponseDTO(userProfile, token)).build();
}
```

### Fichiers concernés

| Fichier | Modification requise |
|---------|---------------------|
| `traefik/dynamic/middlewares.yml` | Ajouter middlewares rate-limit |
| `compose.yaml` | Appliquer middlewares aux routers |
| `backend/pom.xml` | (Optionnel) Ajouter fault-tolerance |
| `AuthenticationResource.java` | (Optionnel) Annotations @RateLimit |
| `ConversationResource.java` | (Optionnel) Annotations @RateLimit |

### Risques adressés

| Attaque | Impact sans protection | Sévérité |
|---------|----------------------|----------|
| Brute-force credentials | Compromission de comptes | ÉLEVÉE |
| Credential stuffing | Compromission massive | ÉLEVÉE |
| DoS sur API chat | Indisponibilité service | MOYENNE |
| Abus quota OpenAI | Coûts financiers | MOYENNE |

---

## Objectif

1. **Protéger l'endpoint d'authentification** avec un rate limit strict (5 req/s par IP)
2. **Protéger l'API de conversation** avec un rate limit modéré (30 req/s par IP)
3. **Configurer des réponses appropriées** (HTTP 429) avec headers Retry-After
4. **Permettre une défense en profondeur** (Traefik + applicatif)

---

## Méthode

### 1.1.1 Rate Limiting via Traefik (Couche Infrastructure)

#### Avantages
- Protection avant même d'atteindre l'application
- Pas de modification du code Java
- Configuration centralisée
- Efficace contre les attaques volumétriques

#### Implémentation

**Étape 1 : Créer les middlewares**

Modifier `traefik/dynamic/middlewares.yml` :

```yaml
http:
  middlewares:
    # Middleware existant
    security-headers:
      headers:
        contentTypeNosniff: true
        frameDeny: true
        browserXssFilter: true
        stsSeconds: 31536000
        stsIncludeSubdomains: true
        stsPreload: true
        customResponseHeaders:
          server: ""

    compress:
      compress: {}

    api-strip:
      stripPrefix:
        prefixes:
          - "/api"

    # NOUVEAUX MIDDLEWARES DE RATE LIMITING
    
    # Rate limit strict pour authentification (brute-force protection)
    rate-limit-auth:
      rateLimit:
        average: 5              # 5 requêtes par seconde en moyenne
        burst: 10               # Autorise un pic de 10 requêtes
        period: 1s
        sourceCriterion:
          ipStrategy:
            depth: 1            # Utilise X-Forwarded-For (premier proxy)
            excludedIPs:
              - "127.0.0.1/32"  # Exclure localhost pour les tests

    # Rate limit modéré pour API générale
    rate-limit-api:
      rateLimit:
        average: 30             # 30 requêtes par seconde
        burst: 50               # Pic autorisé de 50
        period: 1s
        sourceCriterion:
          ipStrategy:
            depth: 1

    # Rate limit très strict pour endpoints sensibles (password reset, etc.)
    rate-limit-sensitive:
      rateLimit:
        average: 2              # 2 requêtes par seconde
        burst: 5
        period: 1s
        sourceCriterion:
          ipStrategy:
            depth: 1
```

**Étape 2 : Appliquer les middlewares aux routers**

Modifier `compose.yaml` - section backend :

```yaml
backend:
    image: ${BACK_IMAGE}
    container_name: serenia-backend
    # ... configuration existante ...
    labels:
      - "traefik.enable=true"
      - "traefik.docker.network=serenia-network"
      
      # Router principal API (rate limit modéré)
      - "traefik.http.routers.backend-router.rule=Host(`api.${SERENIA_DOMAIN}`)"
      - "traefik.http.routers.backend-router.entrypoints=websecure"
      - "traefik.http.routers.backend-router.tls.certresolver=letsencrypt"
      - "traefik.http.routers.backend-router.middlewares=rate-limit-api@file,security-headers@file"
      - "traefik.http.routers.backend-router.priority=1"
      
      # Router spécifique pour /auth/* (rate limit strict) - priorité plus haute
      - "traefik.http.routers.backend-auth.rule=Host(`api.${SERENIA_DOMAIN}`) && PathPrefix(`/auth`)"
      - "traefik.http.routers.backend-auth.entrypoints=websecure"
      - "traefik.http.routers.backend-auth.tls.certresolver=letsencrypt"
      - "traefik.http.routers.backend-auth.middlewares=rate-limit-auth@file,security-headers@file"
      - "traefik.http.routers.backend-auth.priority=10"
      
      # Router pour password reset (rate limit très strict)
      - "traefik.http.routers.backend-sensitive.rule=Host(`api.${SERENIA_DOMAIN}`) && (PathPrefix(`/auth/reset`) || PathPrefix(`/auth/forgot`))"
      - "traefik.http.routers.backend-sensitive.entrypoints=websecure"
      - "traefik.http.routers.backend-sensitive.tls.certresolver=letsencrypt"
      - "traefik.http.routers.backend-sensitive.middlewares=rate-limit-sensitive@file,security-headers@file"
      - "traefik.http.routers.backend-sensitive.priority=20"
      
      - "traefik.http.services.backend-service.loadbalancer.server.port=8080"
```

**Étape 3 : Charger la configuration dynamique**

S'assurer que Traefik charge les fichiers dynamiques. Dans `compose.yaml`, section traefik :

```yaml
traefik:
    image: traefik:v3.6.1
    command:
      # ... commandes existantes ...
      - --providers.file.directory=/etc/traefik/dynamic
      - --providers.file.watch=true
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./traefik/acme.json:/etc/traefik/acme.json
      - ./traefik/dynamic:/etc/traefik/dynamic:ro   # AJOUTER ce volume
```

### 1.1.2 Rate Limiting Applicatif (Couche Application - Optionnel)

#### Avantages
- Contrôle plus fin (par utilisateur, par endpoint)
- Logique métier intégrée
- Défense en profondeur

#### Implémentation avec SmallRye Fault Tolerance

**Étape 1 : Ajouter la dépendance**

Dans `backend/pom.xml` :

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>
```

**Étape 2 : Créer un service de rate limiting personnalisé**

```java
package com.lofo.serenia.service.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RateLimitService {

    // Rate limits par IP pour l'authentification
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    
    // Rate limits par userId pour l'API
    private final Map<UUID, Bucket> userApiBuckets = new ConcurrentHashMap<>();

    public boolean tryConsumeAuthAttempt(String ipAddress) {
        Bucket bucket = authBuckets.computeIfAbsent(ipAddress, this::createAuthBucket);
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeApiCall(UUID userId) {
        Bucket bucket = userApiBuckets.computeIfAbsent(userId, this::createUserApiBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createAuthBucket(String ip) {
        // 5 tentatives par minute pour l'auth
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createUserApiBucket(UUID userId) {
        // 60 requêtes par minute pour l'API
        Bandwidth limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

**Étape 3 : Créer un filtre JAX-RS**

```java
package com.lofo.serenia.rest.filter;

import com.lofo.serenia.service.security.RateLimitService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
@RateLimited  // Annotation personnalisée
public class RateLimitFilter implements ContainerRequestFilter {

    @Inject
    RateLimitService rateLimitService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String clientIp = getClientIp(requestContext);

        if (path.startsWith("/auth")) {
            if (!rateLimitService.tryConsumeAuthAttempt(clientIp)) {
                log.warn("Rate limit exceeded for auth from IP: {}", maskIp(clientIp));
                abortWithTooManyRequests(requestContext);
            }
        }
    }

    private void abortWithTooManyRequests(ContainerRequestContext context) {
        context.abortWith(
            Response.status(429)
                .header("Retry-After", "60")
                .entity("{\"error\": \"Too many requests. Please try again later.\"}")
                .build()
        );
    }

    private String getClientIp(ContainerRequestContext context) {
        String xff = context.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        // Fallback - ne devrait pas arriver derrière Traefik
        return "unknown";
    }

    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".xxx.xxx";
        }
        return "masked";
    }
}
```

**Étape 4 : Annotation personnalisée**

```java
package com.lofo.serenia.rest.filter;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RateLimited {
}
```

---

## Architecture

### Défense en profondeur

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              INTERNET                                    │
│                           (Attaquant)                                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 100 req/s (attaque brute-force)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         COUCHE 1 : TRAEFIK                               │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  Rate Limit Middleware (par IP)                                    │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │  │
│  │  │ /auth/*         │  │ /auth/reset/*   │  │ /*              │   │  │
│  │  │ 5 req/s         │  │ 2 req/s         │  │ 30 req/s        │   │  │
│  │  │ burst: 10       │  │ burst: 5        │  │ burst: 50       │   │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  Résultat: 95 req/s → 429 Too Many Requests                             │
│            5 req/s passent                                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 5 req/s (passent le premier filtre)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       COUCHE 2 : APPLICATION                             │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  RateLimitFilter (par IP + par User si authentifié)               │  │
│  │                                                                    │  │
│  │  - Bucket4j in-memory                                             │  │
│  │  - 5 auth attempts / minute / IP                                  │  │
│  │  - 60 API calls / minute / User                                   │  │
│  │                                                                    │  │
│  │  Avantage: Protection même si Traefik est bypassé                 │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Requêtes légitimes uniquement
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         COUCHE 3 : LOGIQUE MÉTIER                        │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  - Account lockout après 5 échecs (30 min)                        │  │
│  │  - Alerting sur patterns suspects                                 │  │
│  │  - Audit logging                                                  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Flux de requête avec rate limiting

```
┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Client  │     │ Traefik  │     │   Backend    │     │   Service    │
│   (IP)   │     │          │     │   Filter     │     │    Auth      │
└────┬─────┘     └────┬─────┘     └──────┬───────┘     └──────┬───────┘
     │                │                  │                    │
     │  POST /auth/login                 │                    │
     │  (1ère requête)                   │                    │
     │───────────────►│                  │                    │
     │                │  Bucket IP: 9/10 │                    │
     │                │  (OK, -1 token)  │                    │
     │                │─────────────────►│                    │
     │                │                  │ Bucket IP: 4/5     │
     │                │                  │ (OK, -1 token)     │
     │                │                  │───────────────────►│
     │                │                  │                    │ Validate
     │                │                  │◄───────────────────│
     │                │◄─────────────────│                    │
     │◄───────────────│  200 OK + JWT    │                    │
     │                │                  │                    │
     │  ... 10 requêtes rapides ...      │                    │
     │                │                  │                    │
     │  POST /auth/login                 │                    │
     │  (11ème requête)                  │                    │
     │───────────────►│                  │                    │
     │                │  Bucket IP: 0/10 │                    │
     │                │  (REFUSÉ)        │                    │
     │◄───────────────│                  │                    │
     │  429 Too Many  │                  │                    │
     │  Retry-After:60│                  │                    │
```

---

## Tests d'Acceptance

### TA-1.1.1 : Rate Limiting Auth via Traefik

**Prérequis** : Environnement de test déployé avec nouvelle configuration Traefik

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Requête normale | 1 POST `/auth/login` | 200 OK ou 401 (selon credentials) |
| 2 | Burst autorisé | 10 POST `/auth/login` en 1s | Toutes passent (burst=10) |
| 3 | Rate limit atteint | 15 POST `/auth/login` en 2s | 429 après la 10-11ème |
| 4 | Recovery | Attendre 2s, refaire 1 requête | 200 OK (bucket rechargé) |
| 5 | IP différente | Même test depuis autre IP | Quota indépendant |

**Script de test automatisé :**

```bash
#!/bin/bash
# test_rate_limit_auth.sh

API_URL="https://api.serenia.studio/auth/login"
PAYLOAD='{"email":"test@test.com","password":"wrong"}'

echo "=== Test Rate Limiting /auth/login ==="
echo "Envoi de 20 requêtes rapides..."

declare -a results

for i in {1..20}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" \
        --max-time 5)
    results+=("$STATUS")
    echo "Requête $i: HTTP $STATUS"
done

# Analyse des résultats
count_429=$(printf '%s\n' "${results[@]}" | grep -c "429")
count_success=$(printf '%s\n' "${results[@]}" | grep -cE "200|401")

echo ""
echo "=== Résultats ==="
echo "Requêtes passées (200/401): $count_success"
echo "Requêtes bloquées (429): $count_429"

if [ "$count_429" -ge 5 ]; then
    echo "✅ TEST PASSÉ: Rate limiting actif"
    exit 0
else
    echo "❌ TEST ÉCHOUÉ: Rate limiting non fonctionnel"
    exit 1
fi
```

### TA-1.1.2 : Rate Limiting API via Traefik

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Usage normal | 10 POST `/conversations/add-message` en 10s | Toutes passent |
| 2 | Burst autorisé | 50 requêtes en 1s | Toutes passent |
| 3 | Rate limit | 100 requêtes en 2s | 429 après ~50-60 |
| 4 | User authentifié | Même test avec JWT valide | Rate limit identique |

**Script de test :**

```bash
#!/bin/bash
# test_rate_limit_api.sh

API_URL="https://api.serenia.studio/conversations/add-message"
JWT="votre_token_ici"
PAYLOAD='{"content":"Test message"}'

echo "=== Test Rate Limiting /conversations/add-message ==="

for i in {1..100}; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT" \
        -d "$PAYLOAD" \
        --max-time 5)
    
    if [ "$STATUS" = "429" ]; then
        echo "Rate limit atteint à la requête $i"
        exit 0
    fi
    
    # Petite pause pour ne pas surcharger le test
    [ $((i % 10)) -eq 0 ] && echo "Requêtes envoyées: $i"
done

echo "⚠️ 100 requêtes passées sans 429 - vérifier la configuration"
```

### TA-1.1.3 : Headers de Réponse 429

| # | Vérification | Commande | Résultat Attendu |
|---|--------------|----------|------------------|
| 1 | Status code | Déclencher rate limit | HTTP 429 |
| 2 | Retry-After | Inspecter headers | Header `Retry-After` présent |
| 3 | Body informatif | Lire body | Message explicatif JSON |

```bash
# Déclencher le rate limit et inspecter la réponse
for i in {1..15}; do
    curl -s -w "\nHTTP: %{http_code}\n" \
        -X POST "https://api.serenia.studio/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"test@test.com","password":"wrong"}' \
        -D - | head -20
done
```

### TA-1.1.4 : Isolation par IP (X-Forwarded-For)

| # | Scénario | Configuration | Résultat |
|---|----------|---------------|----------|
| 1 | IPs différentes | 2 machines/VPNs | Quotas indépendants |
| 2 | Spoofing X-FF | Header X-Forwarded-For modifié | Ignoré (Traefik gère) |
| 3 | Même IP, users différents | 2 users, 1 IP | Même quota IP |

---

## Vulnérabilités Résiduelles

### Après implémentation

| Risque | Mitigation | Risque Résiduel |
|--------|------------|-----------------|
| Brute-force distribué (botnet) | Rate limit par IP | MOYEN - Considérer CAPTCHA |
| Account enumeration | Réponses identiques success/fail | FAIBLE - Timing attacks possibles |
| DoS applicatif | Rate limit Traefik | FAIBLE - L'app reste protégée |

### Recommandations complémentaires (hors scope)

1. **CAPTCHA** après 3 échecs de login
2. **Account lockout** temporaire (30 min) après 5 échecs
3. **Alerting** sur patterns de brute-force détectés
4. **Geo-blocking** si applicable

---

## Critères de Complétion

- [ ] Fichier `traefik/dynamic/middlewares.yml` mis à jour avec rate-limit-auth et rate-limit-api
- [ ] `compose.yaml` mis à jour avec labels de routers séparés par priorité
- [ ] Volume dynamique Traefik configuré
- [ ] Redémarrage Traefik effectué et configuration chargée
- [ ] Test TA-1.1.1 passe (auth rate limited à 5 req/s)
- [ ] Test TA-1.1.2 passe (API rate limited à 30 req/s)
- [ ] Test TA-1.1.3 passe (429 avec Retry-After)
- [ ] Test TA-1.1.4 passe (isolation par IP)
- [ ] (Optionnel) Rate limiting applicatif implémenté comme défense en profondeur
- [ ] Documentation mise à jour
