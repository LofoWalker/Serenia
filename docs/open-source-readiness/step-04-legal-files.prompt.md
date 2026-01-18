# Étape 4 : Création des Fichiers Légaux Open Source

> **Priorité** : 🔴 Critique | **Bloquant** : Oui

## Objectif

Créer tous les fichiers standards requis pour un projet open source professionnel.

## Fichiers à Créer

| Fichier | Statut | Description |
|---------|--------|-------------|
| `LICENSE` | ❌ À créer | Licence du projet |
| `CONTRIBUTING.md` | ❌ À créer | Guide de contribution |
| `CODE_OF_CONDUCT.md` | ❌ À créer | Code de conduite |
| `SECURITY.md` | ❌ À créer | Politique de sécurité |

## Actions à Exécuter

### 1. Créer le fichier LICENSE

Choisir une licence appropriée :
- **MIT** : Permissive, simple, pas de restrictions
- **Apache 2.0** : Permissive avec protection brevets
- **GPL v3** : Copyleft, oblige à partager les modifications

```bash
# Exemple MIT License
cat > LICENSE << 'EOF'
MIT License

Copyright (c) 2026 Serenia

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
EOF
```

### 2. Créer CONTRIBUTING.md

```bash
cat > CONTRIBUTING.md << 'EOF'
# Contributing to Serenia

Thank you for your interest in contributing to Serenia! 🎉

## How to Contribute

### Reporting Bugs
- Use GitHub Issues
- Include steps to reproduce
- Include expected vs actual behavior

### Suggesting Features
- Open a GitHub Issue with the "enhancement" label
- Describe the feature and its use case

### Pull Requests
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- **Backend (Java)**: Follow Google Java Style Guide
- **Frontend (TypeScript)**: ESLint + Prettier configuration

### Running Tests
```bash
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend && npm test
```

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.
EOF
```

### 3. Créer CODE_OF_CONDUCT.md

Utiliser le Contributor Covenant (standard de l'industrie) :

```bash
cat > CODE_OF_CONDUCT.md << 'EOF'
# Contributor Covenant Code of Conduct

## Our Pledge

We as members, contributors, and leaders pledge to make participation in our
community a harassment-free experience for everyone.

## Our Standards

Examples of behavior that contributes to a positive environment:
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community

Examples of unacceptable behavior:
- Trolling, insulting or derogatory comments
- Public or private harassment
- Publishing others' private information without permission

## Enforcement

Instances of abusive behavior may be reported to contact@serenia.studio.
All complaints will be reviewed and investigated promptly and fairly.

## Attribution

This Code of Conduct is adapted from the [Contributor Covenant](https://www.contributor-covenant.org), version 2.1.
EOF
```

### 4. Créer SECURITY.md

```bash
cat > SECURITY.md << 'EOF'
# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to: security@serenia.studio

You should receive a response within 48 hours. If for some reason you do not,
please follow up via email to ensure we received your original message.

Please include:
- Type of issue (e.g., buffer overflow, SQL injection, XSS)
- Full paths of source file(s) related to the issue
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue

## Preferred Languages

We prefer all communications to be in English or French.
EOF
```

## Critères de Validation

- [ ] Fichier `LICENSE` créé à la racine
- [ ] Fichier `CONTRIBUTING.md` créé à la racine
- [ ] Fichier `CODE_OF_CONDUCT.md` créé à la racine
- [ ] Fichier `SECURITY.md` créé à la racine
- [ ] Emails de contact cohérents avec l'anonymisation (étape 2)

## Ressources

- [Choose a License](https://choosealicense.com/)
- [Contributor Covenant](https://www.contributor-covenant.org/)
- [GitHub Security Policy Template](https://docs.github.com/en/code-security/getting-started/adding-a-security-policy-to-your-repository)

## Étape Suivante

→ [Étape 5 : Enrichissement du README](./step-05-readme.prompt.md)
