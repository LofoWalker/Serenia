# Feature : Réinitialisation de mot de passe

Ce document décrit comment implémenter la fonctionnalité de réinitialisation de mot de passe côté frontend Angular, en se connectant au backend Quarkus existant.

---

## 1. Vue d'ensemble du workflow

Le flux de réinitialisation de mot de passe se déroule en **2 étapes** :

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ÉTAPE 1 : Demande de réinitialisation                │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. L'utilisateur clique sur "Mot de passe oublié ?" sur la page login      │
│  2. Il est redirigé vers /forgot-password                                   │
│  3. Il saisit son email et soumet le formulaire                             │
│  4. Le backend envoie un email avec un lien contenant un token              │
│  5. Le frontend affiche un message de confirmation (succès dans tous les    │
│     cas pour éviter l'énumération des utilisateurs)                         │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        ÉTAPE 2 : Réinitialisation du mot de passe           │
├─────────────────────────────────────────────────────────────────────────────┤
│  1. L'utilisateur clique sur le lien reçu par email                         │
│  2. Il arrive sur /reset-password?token=<uuid>                              │
│  3. Il saisit son nouveau mot de passe (2 fois pour confirmation)           │
│  4. Le backend valide le token et met à jour le mot de passe                │
│  5. L'utilisateur est redirigé vers /login avec un message de succès        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Endpoints API Backend

### 2.1. Demande de réinitialisation – `POST /api/auth/forgot-password`

**Description**  
Déclenche l'envoi d'un email de réinitialisation si l'utilisateur existe. Retourne toujours `200 OK` pour éviter l'énumération des utilisateurs.

**Requête**

- URL : `/api/auth/forgot-password`
- Méthode : `POST`
- Headers :
  - `Content-Type: application/json`
  - `Accept: application/json`
- Body (JSON) :

```json
{
  "email": "john.doe@example.com"
}
```

**Validation du payload (DTO `ForgotPasswordRequest`)** :
| Champ   | Type   | Contraintes                                      |
|---------|--------|--------------------------------------------------|
| `email` | string | Obligatoire, format email valide                 |

**Réponses**

| Code | Description                                      | Body                                                                                              |
|------|--------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 200  | Requête traitée (email envoyé si compte existe)  | `{ "message": "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé." }` |
| 400  | Payload invalide (email manquant ou mal formé)   | Message d'erreur de validation                                                                    |

---

### 2.2. Réinitialisation du mot de passe – `POST /api/auth/reset-password`

**Description**  
Valide le token et met à jour le mot de passe de l'utilisateur.

**Requête**

- URL : `/api/auth/reset-password`
- Méthode : `POST`
- Headers :
  - `Content-Type: application/json`
  - `Accept: application/json`
- Body (JSON) :

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "MonNouveauMotDePasse123"
}
```

**Validation du payload (DTO `ResetPasswordRequest`)** :
| Champ         | Type   | Contraintes                                |
|---------------|--------|--------------------------------------------|
| `token`       | string | Obligatoire                                |
| `newPassword` | string | Obligatoire, minimum 8 caractères          |

**Réponses**

| Code | Description                                | Body                                                                                       |
|------|--------------------------------------------|--------------------------------------------------------------------------------------------|
| 200  | Mot de passe réinitialisé avec succès      | `{ "message": "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter." }` |
| 400  | Token invalide, expiré ou payload invalide | `{ "message": "Jeton invalide ou expiré" }`                                               |

**Note importante** : Le token expire après **15 minutes**.

---

## 3. Modèles TypeScript à ajouter

Ajouter ces interfaces dans `src/app/core/models/user.model.ts` :

```typescript
export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface PasswordResetResponse {
  message: string;
}
```

---

## 4. Service AuthService - Méthodes à ajouter

Ajouter ces méthodes dans `src/app/core/services/auth.service.ts` :

```typescript
forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetResponse> {
  this.authState.setLoading(true);
  return this.http.post<PasswordResetResponse>(`${this.apiUrl}/forgot-password`, request).pipe(
    finalize(() => this.authState.setLoading(false))
  );
}

resetPassword(request: ResetPasswordRequest): Observable<PasswordResetResponse> {
  this.authState.setLoading(true);
  return this.http.post<PasswordResetResponse>(`${this.apiUrl}/reset-password`, request).pipe(
    finalize(() => this.authState.setLoading(false))
  );
}
```

---

## 5. Composants à créer

### 5.1. ForgotPasswordComponent

**Emplacement** : `src/app/features/auth/forgot-password/`

**Fichiers** :
- `forgot-password.component.ts`
- `forgot-password.component.html`
- `forgot-password.component.css`

**Fonctionnalités** :
- Formulaire avec un champ email
- Validation : email requis et format valide
- Affichage d'un message de succès après soumission (toujours positif)
- Lien de retour vers la page de connexion
- État de chargement pendant la requête

**Structure du formulaire** :
```typescript
form = this.fb.nonNullable.group({
  email: ['', [Validators.required, Validators.email]]
});
```

**UX** :
- Après soumission réussie, afficher un message de confirmation et un bouton pour retourner au login
- Ne pas afficher d'erreur si l'email n'existe pas (sécurité anti-énumération)

---

### 5.2. ResetPasswordComponent

**Emplacement** : `src/app/features/auth/reset-password/`

**Fichiers** :
- `reset-password.component.ts`
- `reset-password.component.html`
- `reset-password.component.css`

**Fonctionnalités** :
- Récupération du token depuis les query params (`/reset-password?token=xxx`)
- Formulaire avec :
  - Nouveau mot de passe
  - Confirmation du mot de passe
- Validations :
  - Mot de passe requis, minimum 8 caractères
  - Les deux mots de passe doivent correspondre
- Gestion des erreurs (token invalide/expiré)
- Redirection vers `/login` après succès avec message

**Structure du formulaire** :
```typescript
form = this.fb.nonNullable.group({
  newPassword: ['', [Validators.required, Validators.minLength(8)]],
  confirmPassword: ['', [Validators.required]]
}, {
  validators: [this.passwordMatchValidator]
});

private passwordMatchValidator(group: FormGroup): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password === confirm ? null : { passwordMismatch: true };
}
```

**Récupération du token** :
```typescript
private readonly route = inject(ActivatedRoute);
private token = '';

ngOnInit(): void {
  this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
  if (!this.token) {
    // Rediriger vers /forgot-password ou afficher une erreur
  }
}
```

**Gestion des erreurs HTTP** :
```typescript
error: (error: HttpErrorResponse) => {
  if (error.status === 400) {
    this.errorMessage.set('Le lien de réinitialisation est invalide ou a expiré. Veuillez refaire une demande.');
  } else {
    this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
  }
}
```

---

## 6. Routes à ajouter

Modifier `src/app/app.routes.ts` pour ajouter les nouvelles routes :

```typescript
{
  path: '',
  loadComponent: () => import('./shared/layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
  canActivate: [guestGuard],
  children: [
    // ... routes existantes (login, register)
    {
      path: 'forgot-password',
      loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
    }
  ]
},
{
  path: 'reset-password',
  loadComponent: () => import('./shared/layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
  children: [
    {
      path: '',
      loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
    }
  ]
}
```

**Note** : La route `/reset-password` ne doit **PAS** avoir le `guestGuard` car l'utilisateur accède via un lien email et n'a pas besoin d'être connecté.

---

## 7. Modification du LoginComponent

Ajouter un lien "Mot de passe oublié ?" dans `login.component.html` :

```html
<div class="mb-5">
  <app-input
    label="Mot de passe"
    type="password"
    formControlName="password"
    ...
  />
</div>

<!-- Ajouter ce bloc après le champ mot de passe -->
<div class="text-right mb-4">
  <a
    routerLink="/forgot-password"
    class="text-sm text-primary-400 hover:text-primary-200 focus:outline-none focus-visible:underline"
  >
    Mot de passe oublié ?
  </a>
</div>
```

---

## 8. Composants UI à utiliser

Réutiliser les composants existants du projet :
- `ButtonComponent` - pour les boutons de soumission
- `InputComponent` - pour les champs de formulaire
- `AlertComponent` - pour les messages de succès/erreur

---

## 9. Messages utilisateur

### Messages de succès
| Contexte                          | Message                                                                                           |
|-----------------------------------|---------------------------------------------------------------------------------------------------|
| Demande de réinitialisation       | "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."                  |
| Réinitialisation réussie          | "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter."                  |

### Messages d'erreur
| Contexte                          | Message                                                                                           |
|-----------------------------------|---------------------------------------------------------------------------------------------------|
| Email invalide                    | "Veuillez entrer une adresse email valide."                                                       |
| Mot de passe trop court           | "Le mot de passe doit contenir au moins 8 caractères."                                            |
| Mots de passe non identiques      | "Les mots de passe ne correspondent pas."                                                         |
| Token invalide/expiré             | "Le lien de réinitialisation est invalide ou a expiré. Veuillez refaire une demande."            |
| Erreur serveur                    | "Une erreur est survenue. Veuillez réessayer."                                                    |

---

## 10. Checklist d'implémentation

- [ ] Ajouter les interfaces TypeScript dans `user.model.ts`
- [ ] Ajouter les méthodes `forgotPassword()` et `resetPassword()` dans `auth.service.ts`
- [ ] Créer le composant `ForgotPasswordComponent`
- [ ] Créer le composant `ResetPasswordComponent`
- [ ] Ajouter les routes dans `app.routes.ts`
- [ ] Ajouter le lien "Mot de passe oublié ?" dans `LoginComponent`
- [ ] Tester le flux complet :
  - [ ] Demande avec email existant → email reçu
  - [ ] Demande avec email inexistant → même message (pas d'erreur)
  - [ ] Reset avec token valide → succès
  - [ ] Reset avec token expiré → erreur 400
  - [ ] Reset avec token invalide → erreur 400

---

## 11. Structure des fichiers finale

```
src/app/
├── core/
│   ├── models/
│   │   └── user.model.ts  (+ interfaces ajoutées)
│   └── services/
│       └── auth.service.ts  (+ méthodes ajoutées)
├── features/
│   └── auth/
│       ├── forgot-password/
│       │   ├── forgot-password.component.ts
│       │   ├── forgot-password.component.html
│       │   └── forgot-password.component.css
│       ├── reset-password/
│       │   ├── reset-password.component.ts
│       │   ├── reset-password.component.html
│       │   └── reset-password.component.css
│       └── login/
│           └── login.component.html  (+ lien ajouté)
└── app.routes.ts  (+ routes ajoutées)
```

---

## 12. Considérations de sécurité

1. **Anti-énumération** : Le endpoint `/forgot-password` retourne toujours `200 OK`, qu'un compte existe ou non.
2. **Expiration du token** : Le token expire après 15 minutes.
3. **Usage unique** : Le token est supprimé après utilisation.
4. **Validation côté client** : Valider le format email et la longueur du mot de passe avant envoi.
5. **HTTPS** : S'assurer que toutes les communications utilisent HTTPS en production.

