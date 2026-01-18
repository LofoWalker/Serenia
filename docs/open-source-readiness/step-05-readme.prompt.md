# Étape 5 : Enrichissement du README

> **Priorité** : 🔴 Critique | **Bloquant** : Oui

## Objectif

Transformer le README existant en une documentation d'accueil complète et professionnelle pour les contributeurs open source.

## Structure Requise du README

Le README doit contenir les sections suivantes :

- [ ] Titre et description
- [ ] Badges (CI, coverage, license)
- [ ] Captures d'écran / Démo
- [ ] Fonctionnalités
- [ ] Prérequis
- [ ] Installation
- [ ] Configuration
- [ ] Usage
- [ ] Architecture (lien vers docs)
- [ ] Comment contribuer
- [ ] Licence

## Actions à Exécuter

### 1. Analyser le README actuel

```bash
cat README.md
```

### 2. Template README recommandé

```markdown
# Serenia 🧠

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Backend CI](https://github.com/username/serenia/actions/workflows/backend.yml/badge.svg)](https://github.com/username/serenia/actions)
[![Frontend CI](https://github.com/username/serenia/actions/workflows/frontend.yml/badge.svg)](https://github.com/username/serenia/actions)

> Application de bien-être mental propulsée par l'IA.

![Serenia Screenshot](docs/screenshot.png)

## ✨ Fonctionnalités

- 🤖 Chat IA empathique et bienveillant
- 📊 Suivi de l'humeur et statistiques
- 🔒 Chiffrement de bout en bout des conversations
- 💳 Abonnements via Stripe

## 🛠 Stack Technique

| Composant | Technologie |
|-----------|-------------|
| Frontend | Angular 21, TailwindCSS |
| Backend | Quarkus 3.29, Java 21 |
| Base de données | PostgreSQL |
| IA | OpenAI API |
| Paiements | Stripe |

## 📋 Prérequis

- Java 21+
- Node.js 20+
- Docker & Docker Compose
- PostgreSQL 15+

## 🚀 Installation

### Avec Docker (recommandé)

```bash
git clone https://github.com/username/serenia.git
cd serenia
docker compose up -d
```

### Installation manuelle

#### Backend
```bash
cd backend
./mvnw quarkus:dev
```

#### Frontend
```bash
cd frontend
npm install
npm start
```

## ⚙️ Configuration

Copier les fichiers d'environnement :

```bash
cp backend/.env.example backend/.env
cp frontend/src/environments/environment.example.ts frontend/src/environments/environment.ts
```

Variables requises :
- `OPENAI_API_KEY` : Clé API OpenAI
- `STRIPE_SECRET_KEY` : Clé secrète Stripe
- `DATABASE_URL` : URL de connexion PostgreSQL

## 📖 Documentation

- [Architecture](docs/architecture.md)
- [PRD](docs/prd.md)
- [API Reference](docs/api.md)

## 🤝 Contribuer

Les contributions sont les bienvenues ! Consultez [CONTRIBUTING.md](CONTRIBUTING.md) pour commencer.

## 📄 Licence

Ce projet est sous licence MIT. Voir [LICENSE](LICENSE) pour plus de détails.

## 🔒 Sécurité

Pour signaler une vulnérabilité, consultez [SECURITY.md](SECURITY.md).
```

### 3. Ajouter des badges dynamiques

```markdown
<!-- Build Status -->
![Build](https://github.com/USER/REPO/workflows/CI/badge.svg)

<!-- Coverage -->
![Coverage](https://codecov.io/gh/USER/REPO/branch/main/graph/badge.svg)

<!-- License -->
![License](https://img.shields.io/github/license/USER/REPO)

<!-- Version -->
![Version](https://img.shields.io/github/v/release/USER/REPO)
```

### 4. Ajouter une capture d'écran

```bash
# Créer le dossier si nécessaire
mkdir -p docs/images

# Ajouter une capture d'écran de l'application
# screenshot.png à placer dans docs/images/
```

## Critères de Validation

- [ ] Toutes les sections requises sont présentes
- [ ] Instructions d'installation testées et fonctionnelles
- [ ] Badges configurés (ou placeholders)
- [ ] Liens vers documentation existante
- [ ] Aucune référence à des éléments privés/internes
- [ ] Orthographe et grammaire vérifiées

## Étape Suivante

→ [Étape 6 : Audit des Vulnérabilités (CVE)](./step-06-cve-audit.prompt.md)
