# Step 1.4 : Headers de Sécurité HTTP (Backend)

## Contexte

Le backend Quarkus expose une API REST qui doit inclure des headers de sécurité HTTP pour protéger contre diverses attaques. Actuellement, ces headers sont partiellement configurés côté frontend (Nginx) mais absents côté backend (API).

### État actuel

**Frontend (nginx.conf) - Headers présents :**
```nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

**Backend (API) - Headers manquants :**
```bash
curl -I https://api.serenia.studio/health
# Résultat: Pas de X-Frame-Options, pas de HSTS, etc.
```

### Fichiers concernés

| Fichier | Modification |
|---------|--------------|
| `traefik/dynamic/middlewares.yml` | Ajouter middleware security-headers |
| `compose.yaml` | Appliquer middleware au backend |
| (Alternatif) `application.properties` | Configuration Quarkus native |

### Headers de sécurité recommandés (OWASP)

| Header | Objectif | Priorité |
|--------|----------|----------|
| `Strict-Transport-Security` (HSTS) | Forcer HTTPS | CRITIQUE |
| `X-Content-Type-Options` | Prévenir MIME sniffing | ÉLEVÉE |
| `X-Frame-Options` | Prévenir clickjacking | ÉLEVÉE |
| `Content-Security-Policy` | Prévenir XSS | ÉLEVÉE |
| `Referrer-Policy` | Contrôler les referrers | MOYENNE |
| `Permissions-Policy` | Contrôler les features | MOYENNE |
| `X-Permitted-Cross-Domain-Policies` | Contrôler Adobe policies | FAIBLE |
| `Server` | Masquer le serveur | FAIBLE |

---

## Objectif

1. **Appliquer les headers de sécurité** à toutes les réponses de l'API backend
2. **Configurer HSTS** avec preload pour une protection maximale
3. **Masquer les informations serveur** (versions, technologies)
4. **Valider avec Mozilla Observatory** pour un score A+

---

## Méthode

### 1.4.1 Configuration via Traefik (Recommandé)

#### Avantages
- Configuration centralisée
- Pas de modification du code Java
- Applicable à tous les services derrière Traefik

#### Implémentation

**Étape 1 : Créer le middleware de sécurité**

Modifier `traefik/dynamic/middlewares.yml` :

```yaml
http:
  middlewares:
    # Headers de sécurité pour le frontend
    security-headers:
      headers:
        # Protection contre le clickjacking
        frameDeny: true
        
        # Prévenir MIME type sniffing
        contentTypeNosniff: true
        
        # Activer filtre XSS des navigateurs (legacy)
        browserXssFilter: true
        
        # HSTS - Force HTTPS pendant 1 an
        stsSeconds: 31536000
        stsIncludeSubdomains: true
        stsPreload: true
        
        # Masquer le serveur
        customResponseHeaders:
          server: ""
          X-Powered-By: ""

    # Headers de sécurité pour le backend API
    security-headers-api:
      headers:
        # Protection contre le clickjacking (API ne doit jamais être framée)
        customFrameOptionsValue: "DENY"
        
        # Prévenir MIME type sniffing
        contentTypeNosniff: true
        
        # HSTS - Force HTTPS
        stsSeconds: 31536000
        stsIncludeSubdomains: true
        stsPreload: true
        
        # CSP stricte pour API (pas de contenu HTML attendu)
        contentSecurityPolicy: "default-src 'none'; frame-ancestors 'none'"
        
        # Referrer-Policy
        referrerPolicy: "strict-origin-when-cross-origin"
        
        # Permissions-Policy (désactiver toutes les features)
        permissionsPolicy: "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()"
        
        # Masquer les informations serveur
        customResponseHeaders:
          server: ""
          X-Powered-By: ""
          X-AspNet-Version: ""
          X-Permitted-Cross-Domain-Policies: "none"
        
        # Headers additionnels de sécurité
        customRequestHeaders:
          X-Request-Id: ""  # Sera rempli par Traefik si configuré

    # Rate limiting (de step 1.1)
    rate-limit-auth:
      rateLimit:
        average: 5
        burst: 10
        period: 1s
        sourceCriterion:
          ipStrategy:
            depth: 1

    rate-limit-api:
      rateLimit:
        average: 30
        burst: 50
        period: 1s
        sourceCriterion:
          ipStrategy:
            depth: 1

    compress:
      compress: {}

    api-strip:
      stripPrefix:
        prefixes:
          - "/api"
```

**Étape 2 : Appliquer les middlewares au backend**

Modifier `compose.yaml` :

```yaml
backend:
    image: ${BACK_IMAGE}
    container_name: serenia-backend
    restart: unless-stopped
    # ... autres configurations ...
    labels:
      - "traefik.enable=true"
      - "traefik.docker.network=serenia-network"
      
      # Router principal avec sécurité + rate limit
      - "traefik.http.routers.backend-router.rule=Host(`api.${SERENIA_DOMAIN}`)"
      - "traefik.http.routers.backend-router.entrypoints=websecure"
      - "traefik.http.routers.backend-router.tls.certresolver=letsencrypt"
      - "traefik.http.routers.backend-router.middlewares=security-headers-api@file,rate-limit-api@file"
      - "traefik.http.routers.backend-router.priority=1"
      
      # Router pour auth avec rate limit strict
      - "traefik.http.routers.backend-auth.rule=Host(`api.${SERENIA_DOMAIN}`) && PathPrefix(`/auth`)"
      - "traefik.http.routers.backend-auth.entrypoints=websecure"
      - "traefik.http.routers.backend-auth.tls.certresolver=letsencrypt"
      - "traefik.http.routers.backend-auth.middlewares=security-headers-api@file,rate-limit-auth@file"
      - "traefik.http.routers.backend-auth.priority=10"
      
      - "traefik.http.services.backend-service.loadbalancer.server.port=8080"
```

**Étape 3 : Appliquer les middlewares au frontend**

```yaml
frontend:
    image: ${FRONT_IMAGE}
    container_name: serenia-frontend
    restart: unless-stopped
    labels:
      - "traefik.enable=true"
      - "traefik.docker.network=serenia-network"
      - "traefik.http.routers.frontend-router.rule=Host(`${SERENIA_DOMAIN}`)"
      - "traefik.http.routers.frontend-router.entrypoints=websecure"
      - "traefik.http.routers.frontend-router.tls.certresolver=letsencrypt"
      - "traefik.http.routers.frontend-router.middlewares=security-headers@file,compress@file"
      - "traefik.http.services.frontend-service.loadbalancer.server.port=80"
```

### 1.4.2 Configuration Alternative via Quarkus

Si vous préférez configurer les headers directement dans Quarkus :

**Option A : Via application.properties**

```properties
# Security Headers
quarkus.http.header.X-Frame-Options.value=DENY
quarkus.http.header.X-Content-Type-Options.value=nosniff
quarkus.http.header.X-XSS-Protection.value=1; mode=block
quarkus.http.header.Strict-Transport-Security.value=max-age=31536000; includeSubDomains; preload
quarkus.http.header.Referrer-Policy.value=strict-origin-when-cross-origin
quarkus.http.header.Permissions-Policy.value=accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()
quarkus.http.header.Content-Security-Policy.value=default-src 'none'; frame-ancestors 'none'

# Masquer le serveur
quarkus.http.header.Server.value=
```

**Option B : Via un filtre JAX-RS**

```java
package com.lofo.serenia.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, 
                       ContainerResponseContext responseContext) {
        
        var headers = responseContext.getHeaders();
        
        // Protection contre clickjacking
        headers.putSingle("X-Frame-Options", "DENY");
        
        // Prévenir MIME sniffing
        headers.putSingle("X-Content-Type-Options", "nosniff");
        
        // HSTS (si pas déjà géré par Traefik)
        headers.putSingle("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains; preload");
        
        // CSP pour API
        headers.putSingle("Content-Security-Policy", 
            "default-src 'none'; frame-ancestors 'none'");
        
        // Referrer Policy
        headers.putSingle("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy
        headers.putSingle("Permissions-Policy", 
            "accelerometer=(), camera=(), geolocation=(), gyroscope=(), " +
            "magnetometer=(), microphone=(), payment=(), usb=()");
        
        // Masquer le serveur
        headers.remove("Server");
        headers.remove("X-Powered-By");
    }
}
```

### 1.4.3 Explication Détaillée des Headers

#### Strict-Transport-Security (HSTS)

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

| Paramètre | Valeur | Signification |
|-----------|--------|---------------|
| `max-age` | 31536000 | Cache HTTPS pendant 1 an |
| `includeSubDomains` | - | Applique à tous les sous-domaines |
| `preload` | - | Éligible pour la preload list Chrome |

**Pour activer le preload :**
1. Configurer HSTS avec les paramètres ci-dessus
2. Soumettre le domaine sur https://hstspreload.org/

#### X-Frame-Options

```
X-Frame-Options: DENY
```

| Valeur | Comportement |
|--------|--------------|
| `DENY` | Jamais dans une iframe |
| `SAMEORIGIN` | Seulement depuis le même domaine |
| `ALLOW-FROM uri` | Depuis une URI spécifique (obsolète) |

**Pour une API** : Toujours `DENY` car l'API ne devrait jamais être affichée dans une iframe.

#### Content-Security-Policy (Backend)

```
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
```

Pour une API qui retourne uniquement du JSON :
- `default-src 'none'` : Aucune ressource autorisée
- `frame-ancestors 'none'` : Équivalent moderne de X-Frame-Options: DENY

#### Permissions-Policy

```
Permissions-Policy: accelerometer=(), camera=(), geolocation=(), ...
```

Désactive l'accès aux features du navigateur. Pour une API, tout devrait être désactivé.

---

## Architecture

### Flux des Headers de Sécurité

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SECURITY HEADERS FLOW                                 │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌─────────────────────────────────────────────────────┐
│   Client     │     │                      TRAEFIK                        │
│  (Browser)   │     │                                                     │
└──────┬───────┘     │  ┌─────────────────────────────────────────────────┐│
       │             │  │           MIDDLEWARES CHAIN                     ││
       │             │  │                                                 ││
       │  Request    │  │  1. Rate Limiting                              ││
       │────────────►│  │     └─► Check token bucket                     ││
       │             │  │                                                 ││
       │             │  │  2. Forward to Backend                         ││
       │             │  │     └─► Request unchanged                      ││
       │             │  │                                                 ││
       │             │  │  3. Backend Response                           ││
       │             │  │     └─► Body: {"data": ...}                    ││
       │             │  │                                                 ││
       │             │  │  4. Security Headers Middleware                ││
       │             │  │     └─► ADD: X-Frame-Options: DENY             ││
       │             │  │     └─► ADD: X-Content-Type-Options: nosniff   ││
       │             │  │     └─► ADD: Strict-Transport-Security: ...    ││
       │             │  │     └─► ADD: Content-Security-Policy: ...      ││
       │             │  │     └─► ADD: Referrer-Policy: ...              ││
       │             │  │     └─► REMOVE: Server                         ││
       │             │  │                                                 ││
       │  Response   │  └─────────────────────────────────────────────────┘│
       │◄────────────│                                                     │
       │             │  Response Headers:                                  │
       │             │  ├─ X-Frame-Options: DENY                          │
       │             │  ├─ X-Content-Type-Options: nosniff                │
       │             │  ├─ Strict-Transport-Security: max-age=...        │
       │             │  ├─ Content-Security-Policy: default-src 'none'   │
       │             │  └─ ... (autres headers)                           │
       │             └─────────────────────────────────────────────────────┘
```

### Matrice de Protection par Header

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PROTECTION MATRIX                                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┬───────────┬───────────┬───────────┬───────────┐
│        ATTAQUE          │   HSTS    │ X-Frame   │   CSP     │ X-CT-Opt │
├─────────────────────────┼───────────┼───────────┼───────────┼───────────┤
│ Man-in-the-Middle       │    ✅     │           │           │           │
│ SSL Stripping           │    ✅     │           │           │           │
│ Clickjacking            │           │    ✅     │    ✅     │           │
│ MIME Confusion          │           │           │           │    ✅     │
│ XSS (reflected)         │           │           │    ✅     │           │
│ Frame injection         │           │    ✅     │    ✅     │           │
│ Content injection       │           │           │    ✅     │    ✅     │
│ Information disclosure  │ (Server)  │           │           │           │
└─────────────────────────┴───────────┴───────────┴───────────┴───────────┘

Légende:
✅ = Protection directe
(Server) = Masquage du header Server
```

---

## Tests d'Acceptance

### TA-1.4.1 : Headers de Sécurité Présents

| # | Header | Valeur Attendue | Test |
|---|--------|-----------------|------|
| 1 | `X-Frame-Options` | `DENY` | ✅ / ❌ |
| 2 | `X-Content-Type-Options` | `nosniff` | ✅ / ❌ |
| 3 | `Strict-Transport-Security` | `max-age=31536000...` | ✅ / ❌ |
| 4 | `Content-Security-Policy` | `default-src 'none'...` | ✅ / ❌ |
| 5 | `Referrer-Policy` | `strict-origin-when-cross-origin` | ✅ / ❌ |
| 6 | `Server` | Absent ou vide | ✅ / ❌ |

**Script de test :**

```bash
#!/bin/bash
# test_security_headers.sh

API_URL="https://api.serenia.studio/health"

echo "=== Test Security Headers ==="
echo "URL: $API_URL"
echo ""

HEADERS=$(curl -s -I "$API_URL")

echo "Response Headers:"
echo "$HEADERS" | grep -E "^[A-Za-z-]+:" | head -20
echo ""

# Vérifications
check_header() {
    local name="$1"
    local expected="$2"
    
    if echo "$HEADERS" | grep -qi "^$name"; then
        VALUE=$(echo "$HEADERS" | grep -i "^$name" | cut -d: -f2- | tr -d '\r')
        if [ -n "$expected" ]; then
            if echo "$VALUE" | grep -qi "$expected"; then
                echo "✅ $name: $VALUE"
            else
                echo "⚠️ $name: $VALUE (attendu: $expected)"
            fi
        else
            echo "✅ $name présent"
        fi
    else
        echo "❌ $name MANQUANT"
    fi
}

check_header "X-Frame-Options" "DENY"
check_header "X-Content-Type-Options" "nosniff"
check_header "Strict-Transport-Security" "max-age"
check_header "Content-Security-Policy" ""
check_header "Referrer-Policy" ""

# Vérifier que Server est masqué
if echo "$HEADERS" | grep -qi "^Server:"; then
    SERVER=$(echo "$HEADERS" | grep -i "^Server:" | cut -d: -f2-)
    if [ -z "$(echo $SERVER | tr -d '[:space:]')" ]; then
        echo "✅ Server header vide (masqué)"
    else
        echo "⚠️ Server header exposé: $SERVER"
    fi
else
    echo "✅ Server header absent"
fi
```

### TA-1.4.2 : Validation Mozilla Observatory

| # | Critère | Score Minimum |
|---|---------|---------------|
| 1 | Score global | A (minimum) |
| 2 | Score global | A+ (objectif) |

**Test avec Mozilla Observatory :**

```bash
# Installation de l'outil CLI
npm install -g observatory-cli

# Scan
observatory api.serenia.studio

# Résultat attendu:
# Score: A+ (ou minimum A)
# ✓ Content Security Policy
# ✓ Strict Transport Security
# ✓ X-Content-Type-Options
# ✓ X-Frame-Options
# ✓ Referrer-Policy
```

**Ou via l'interface web :**
1. Aller sur https://observatory.mozilla.org/
2. Entrer `api.serenia.studio`
3. Lancer le scan
4. Vérifier le score

### TA-1.4.3 : HSTS Preload Éligible

| # | Critère | Vérification |
|---|---------|--------------|
| 1 | HTTPS partout | Pas de HTTP | ✅ / ❌ |
| 2 | Redirect HTTP → HTTPS | `curl -I http://...` → 301/302 | ✅ / ❌ |
| 3 | HSTS header | `max-age >= 31536000` | ✅ / ❌ |
| 4 | includeSubDomains | Présent | ✅ / ❌ |
| 5 | preload | Présent | ✅ / ❌ |

**Test preload eligibility :**

```bash
# Vérifier le header HSTS
curl -s -I "https://api.serenia.studio" | grep -i "strict-transport"

# Vérifier la redirection HTTP
curl -s -I "http://api.serenia.studio" | head -5

# Soumettre pour preload (une fois les critères remplis)
# Aller sur https://hstspreload.org/ et soumettre le domaine
```

### TA-1.4.4 : Iframe Bloquée

| # | Scénario | Test | Résultat |
|---|----------|------|----------|
| 1 | Embed API | `<iframe src="https://api.serenia.studio">` | Iframe vide |
| 2 | Console error | DevTools | Erreur X-Frame-Options |

**Test HTML :**

```html
<!DOCTYPE html>
<html>
<head><title>Test Clickjacking</title></head>
<body>
    <h1>Test X-Frame-Options</h1>
    <iframe src="https://api.serenia.studio/health" 
            width="600" height="400"></iframe>
    <!-- Devrait être bloqué -->
</body>
</html>
```

### TA-1.4.5 : Pas d'Information Disclosure

| # | Header | Valeur | Risque |
|---|--------|--------|--------|
| 1 | `Server` | Absent/vide | Info serveur masquée |
| 2 | `X-Powered-By` | Absent | Framework masqué |
| 3 | `X-AspNet-Version` | Absent | Version masquée |

```bash
# Vérifier les headers dangereux
curl -s -I "https://api.serenia.studio/health" | grep -iE "server|powered|version"
# Attendu: aucun résultat ou valeurs vides
```

---

## Validation avec Outils de Sécurité

### securityheaders.com

```bash
# Scanner en ligne
# Aller sur https://securityheaders.com/
# Entrer: https://api.serenia.studio
# Score attendu: A+
```

### SSL Labs

```bash
# Aller sur https://www.ssllabs.com/ssltest/
# Entrer: api.serenia.studio
# Vérifier le rating (A+) et la présence de HSTS
```

---

## Critères de Complétion

- [ ] Middleware `security-headers-api` créé dans Traefik
- [ ] Middleware appliqué aux routers backend
- [ ] Header `X-Frame-Options: DENY` présent
- [ ] Header `X-Content-Type-Options: nosniff` présent
- [ ] Header `Strict-Transport-Security` avec preload présent
- [ ] Header `Content-Security-Policy` présent
- [ ] Header `Referrer-Policy` présent
- [ ] Header `Server` absent ou vide
- [ ] Test TA-1.4.1 passe (tous headers présents)
- [ ] Test TA-1.4.2 passe (Mozilla Observatory score >= A)
- [ ] Test TA-1.4.3 passe (HSTS preload eligible)
- [ ] Test TA-1.4.4 passe (iframe bloquée)
- [ ] Test TA-1.4.5 passe (pas d'info disclosure)
- [ ] (Optionnel) Domaine soumis sur hstspreload.org
