# Étape 3 : Scan et Nettoyage de l'Historique Git

> **Priorité** : 🔴 Critique | **Bloquant** : Oui

## Objectif

S'assurer que l'historique Git ne contient pas de secrets, données personnelles ou informations sensibles.

## Actions à Exécuter

### 1. Scanner tout l'historique avec gitleaks

```bash
gitleaks detect --source . --verbose --log-opts="--all"
```

### 2. Scanner avec truffleHog sur tout l'historique

```bash
truffleHog git file://. --since-commit HEAD~1000
```

### 3. Vérifier les messages de commits

```bash
git log --oneline | grep -i "internal\|private\|secret\|password\|key\|token"
```

### 4. Rechercher des emails personnels dans l'historique

```bash
git log --all --full-history -- "**/legal*" "**/privacy*" "**/terms*" | head -50
```

### 5. Si secrets trouvés : Nettoyer avec BFG Repo-Cleaner

```bash
# Installer BFG
# brew install bfg (macOS) ou télécharger depuis https://rtyley.github.io/bfg-repo-cleaner/

# Supprimer les fichiers .pem de tout l'historique
bfg --delete-files "*.pem" --no-blob-protection

# Créer un fichier de remplacement pour les secrets
echo "tom1997walker@gmail.com==>contact@serenia.studio" > replacements.txt
bfg --replace-text replacements.txt

# Nettoyer et forcer le garbage collection
git reflog expire --expire=now --all && git gc --prune=now --aggressive
```

### 6. Alternative : git filter-repo (recommandé)

```bash
# Installer git-filter-repo
pip install git-filter-repo

# Supprimer les fichiers sensibles
git filter-repo --invert-paths --path backend/keys/privateKey.pem --path backend/keys/publicKey.pem --path backend/keys/rsaPrivateKey.pem

# Remplacer du texte dans tout l'historique
git filter-repo --replace-text <(echo 'tom1997walker@gmail.com==>contact@serenia.studio')
```

### 7. Lister et supprimer les branches obsolètes

```bash
# Lister les branches fusionnées
git branch --merged main

# Supprimer les branches obsolètes locales
git branch -d <branch-name>

# Supprimer les branches obsolètes distantes
git push origin --delete <branch-name>
```

## ⚠️ Avertissement

Le nettoyage de l'historique Git **réécrit l'historique**. Cela implique :
- Tous les contributeurs devront re-cloner le repository
- Les références aux anciens commits seront invalides
- À faire **avant** la publication, pas après

## Critères de Validation

- [ ] Aucun secret détecté dans l'historique par gitleaks
- [ ] Aucun secret détecté dans l'historique par truffleHog
- [ ] Aucun email personnel dans l'historique
- [ ] Messages de commits propres
- [ ] Branches obsolètes supprimées

## Étape Suivante

→ [Étape 4 : Création des Fichiers Légaux](./step-04-legal-files.prompt.md)
