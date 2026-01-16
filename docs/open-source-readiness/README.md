# Open Source Readiness — Index des Étapes

> **Objectif** : Évaluer et préparer le repository Serenia pour une publication en open source.

## Vue d'Ensemble

| # | Étape | Priorité | Bloquant | Fichier |
|---|-------|----------|----------|---------|
| 1 | Scan des Secrets Exposés | 🔴 Critique | Oui | [step-01](./step-01-secrets-scan.prompt.md) |
| 2 | Anonymisation des Données Personnelles | 🔴 Critique | Oui | [step-02](./step-02-personal-data.prompt.md) |
| 3 | Scan et Nettoyage Historique Git | 🔴 Critique | Oui | [step-03](./step-03-git-history.prompt.md) |
| 4 | Création des Fichiers Légaux | 🔴 Critique | Oui | [step-04](./step-04-legal-files.prompt.md) |
| 5 | Enrichissement du README | 🔴 Critique | Oui | [step-05](./step-05-readme.prompt.md) |
| 6 | Audit des Vulnérabilités (CVE) | 🟡 Haute | Non | [step-06](./step-06-cve-audit.prompt.md) |
| 7 | Vérification des Commentaires | 🟡 Haute | Partiel | [step-07](./step-07-comments-review.prompt.md) |
| 8 | Analyse du Code Mort | 🟢 Moyenne | Non | [step-08](./step-08-dead-code.prompt.md) |
| 9 | Vérification Qualité et Tests | 🟡 Haute | Partiel | [step-09](./step-09-quality-tests.prompt.md) |
| 10 | Uniformité et Standards | 🟢 Moyenne | Non | [step-10](./step-10-code-standards.prompt.md) |
| 11 | Checklist Finale et Verdict | 🔴 Critique | Oui | [step-11](./step-11-final-checklist.prompt.md) |

## Ordre d'Exécution Recommandé

```
┌─────────────────────────────────────────────────────────────┐
│  PHASE 1 : SÉCURITÉ (Bloquants)                             │
├─────────────────────────────────────────────────────────────┤
│  Step 1 → Step 2 → Step 3                                   │
│  Secrets   Données   Historique                             │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 2 : DOCUMENTATION (Bloquants)                        │
├─────────────────────────────────────────────────────────────┤
│  Step 4 → Step 5                                            │
│  Légal     README                                           │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 3 : QUALITÉ (Recommandés)                            │
├─────────────────────────────────────────────────────────────┤
│  Step 6 → Step 7 → Step 9                                   │
│  CVE       Comments  Tests                                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 4 : NETTOYAGE (Optionnels)                           │
├─────────────────────────────────────────────────────────────┤
│  Step 8 → Step 10                                           │
│  Dead Code  Standards                                       │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 5 : VALIDATION                                       │
├─────────────────────────────────────────────────────────────┤
│  Step 11                                                    │
│  Checklist Finale                                           │
└─────────────────────────────────────────────────────────────┘
```

## Problèmes Identifiés (État Initial)

### 🔴 Critiques

| Problème | Étape | Statut |
|----------|-------|--------|
| Clés PEM dans `backend/keys/` | [1](./step-01-REPORT.md) | ✅ Conformes |
| Email `tom1997walker@gmail.com` exposé | [2](./step-02-REPORT.md) | ✅ Remplacé |
| Secrets dans l'historique Git | [3](./step-03-REPORT.md) | ✅ Aucun trouvé |
| Fichier `LICENSE` manquant | [4](./step-04-legal-files.prompt.md) | ⬜ À créer |
| Fichier `CONTRIBUTING.md` manquant | [4](./step-04-legal-files.prompt.md) | ⬜ À créer |
| Fichier `CODE_OF_CONDUCT.md` manquant | [4](./step-04-legal-files.prompt.md) | ⬜ À créer |
| Fichier `SECURITY.md` manquant | [4](./step-04-legal-files.prompt.md) | ⬜ À créer |

## Commencer

Exécuter les étapes dans l'ordre :

```bash
# Étape 1 : Commencer par le scan des secrets
cat docs/open-source-readiness/step-01-secrets-scan.prompt.md
```

## Références

- [Plan complet original](../plan-openSourceReadiness.prompt.md)
- [Architecture](../architecture.md)
- [PRD](../prd.md)

---

*Serenia Open Source Readiness — 11 étapes vers la publication*
