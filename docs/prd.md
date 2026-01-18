# Product Requirements Document (PRD) — Serenia

## 1. Vision Produit

**Serenia** est une application de chat conversationnel propulsée par l'IA, conçue pour offrir un espace personnel, sûr et décontracté où les utilisateurs peuvent discuter, se confier et souffler. Contrairement aux assistants IA classiques, Serenia adopte une personnalité authentique : celle d'un ami proche, naturel, parfois sarcastique, jamais professionnel.

### Proposition de Valeur

- **Espace de bien-être personnel** : Un lieu sûr pour s'exprimer sans jugement
- **Personnalité authentique** : Une IA avec un vrai caractère, pas un assistant aseptisé
- **Confidentialité renforcée** : Chiffrement AES-GCM de bout en bout des conversations
- **Modèle freemium** : Accessibilité gratuite avec options premium

---

## 2. Personas Utilisateurs

### Persona Principal : L'Utilisateur Quotidien

- **Profil** : 18-35 ans, cherche un espace de décompression
- **Besoins** : Parler sans filtre, recevoir des réponses authentiques
- **Douleurs** : Solitude numérique, assistants IA trop formels

### Persona Secondaire : L'Utilisateur Premium

- **Profil** : Utilisateur régulier souhaitant des interactions illimitées
- **Besoins** : Plus de messages quotidiens, plus de tokens mensuels
- **Motivation** : Engagement dans la durée avec Serenia

---

## 3. Fonctionnalités Principales

### 3.1 Authentification & Gestion de Compte

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| Inscription email | Création de compte avec vérification par email | P0 |
| Connexion JWT | Authentification sécurisée par tokens JWT | P0 |
| Réinitialisation mot de passe | Processus sécurisé de récupération | P0 |
| Profil utilisateur | Gestion des informations personnelles | P1 |

### 3.2 Chat Conversationnel

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| Conversations persistantes | Historique des échanges sauvegardé | P0 |
| Personnalité Serenia | Réponses style SMS, max 180 caractères | P0 |
| Chiffrement E2E | AES-GCM pour toutes les conversations | P0 |
| Contexte conversationnel | Mémoire du contexte de la conversation | P1 |

### 3.3 Système d'Abonnement

| Plan | Limite Messages/Jour | Limite Tokens/Mois | Prix |
|------|---------------------|-------------------|------|
| FREE | Limité | Limité | 0€ |
| PLUS | Étendu | Étendu | Premium |
| MAX | Illimité | Maximum | Premium+ |

### 3.4 Intégration Stripe

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| Checkout Sessions | Souscription aux plans payants | P0 |
| Customer Portal | Gestion autonome de l'abonnement | P1 |
| Webhooks | Synchronisation temps réel des événements | P0 |

---

## 4. Exigences Non-Fonctionnelles

### 4.1 Sécurité

- **Chiffrement** : AES-256-GCM pour les messages
- **Authentification** : JWT avec RSA (RS256)
- **Transport** : HTTPS obligatoire (TLS 1.3)
- **Secrets** : Gestion via Docker Secrets

### 4.2 Performance

- **Temps de réponse API** : < 500ms (hors latence OpenAI)
- **Disponibilité** : 99.5% uptime
- **Scalabilité** : Architecture containerisée

### 4.3 Conformité

- **RGPD** : Droit à l'effacement, portabilité des données
- **Sécurité mentale** : Protocole de redirection vers le 3114 en cas de détresse

---

## 5. Contraintes Techniques

- **Backend** : Java 21 + Quarkus 3.29
- **Frontend** : Angular 21 + TailwindCSS
- **Base de données** : PostgreSQL 16
- **IA** : OpenAI API (modèle configurable)
- **Paiements** : Stripe
- **Déploiement** : Docker + Traefik

---

## 6. Métriques de Succès

| KPI | Objectif | Mesure |
|-----|----------|--------|
| Taux de rétention J7 | > 40% | Analytics |
| Conversion Free → Paid | > 5% | Stripe Dashboard |
| NPS | > 50 | Enquêtes utilisateurs |
| Temps moyen par session | > 5 min | Analytics |

---

## 7. Roadmap

### Phase 1 — MVP (Actuel)

- ✅ Authentification complète
- ✅ Chat avec personnalité Serenia
- ✅ Chiffrement des conversations
- ✅ Système d'abonnement Stripe
- ✅ Déploiement containerisé

### Phase 2 — Amélioration

- [ ] Thèmes personnalisables
- [ ] Historique des conversations navigable
- [ ] Statistiques d'utilisation
- [ ] Mode hors-ligne partiel

### Phase 3 — Expansion

- [ ] Application mobile (PWA)
- [ ] Intégrations tierces
- [ ] Multi-langues
- [ ] Personnalisation de la personnalité IA

---

## 8. Risques & Mitigation

| Risque | Impact | Mitigation |
|--------|--------|------------|
| Coûts OpenAI élevés | Élevé | Quotas stricts, caching intelligent |
| Contenu inapproprié | Élevé | Filtrage IA, règles de sécurité |
| Fuite de données | Critique | Chiffrement E2E, audits |
| Dépendance Stripe | Moyen | Architecture découplée |

---

*Document maintenu par l'équipe Serenia — Dernière mise à jour : Janvier 2026*

