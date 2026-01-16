# Étape 1 : Scan des Secrets Exposés

> **Priorité** : 🔴 Critique | **Bloquant** : Oui

## Objectif

Détecter et supprimer tous les secrets (clés API, tokens, credentials, clés cryptographiques) présents dans le code source.

## Problèmes Identifiés

- **Clés cryptographiques présentes** dans `backend/keys/` :
  - `privateKey.pem`
  - `publicKey.pem`
  - `rsaPrivateKey.pem`

## Actions à Exécuter

### 1. Scanner le code avec gitleaks

```bash
gitleaks detect --source . --verbose
```

### 2. Scanner avec truffleHog

```bash
truffleHog git file://. --only-verified
```

### 3. Recherche manuelle par patterns

```bash
grep -rn "password\|secret\|api_key\|token\|credential" --include="*.java" --include="*.ts" --include="*.properties" --include="*.yaml" --include="*.yml" .
```

### 4. Vérifier les fichiers .env

```bash
find . -name ".env*" -type f
cat .gitignore | grep -i "env"
```

### 5. Supprimer les clés PEM du repository

```bash
rm -rf backend/keys/*.pem
```

### 6. Mettre à jour le .gitignore

```bash
echo "backend/keys/*.pem" >> .gitignore
```

### 7. Documenter la génération des clés

Créer un fichier `backend/keys/README.md` expliquant comment générer les clés localement.

## Critères de Validation

- [ ] Aucun secret détecté par gitleaks
- [ ] Aucun secret détecté par truffleHog
- [ ] Fichiers `.pem` supprimés du repository
- [ ] `.gitignore` mis à jour
- [ ] Documentation de génération des clés créée

## Risques si Non-Conformité

- Compromission de l'authentification JWT
- Accès non autorisé aux données utilisateurs
- Réputation du projet compromise

## Étape Suivante

→ [Étape 2 : Anonymisation des Données Personnelles](./step-02-personal-data.prompt.md)
