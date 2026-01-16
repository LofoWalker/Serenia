# Étape 2 : Anonymisation des Données Personnelles

> **Priorité** : 🔴 Critique | **Bloquant** : Oui

## Objectif

Remplacer toutes les données personnelles (emails, noms, etc.) par des valeurs génériques appropriées pour un projet open source.

## Problèmes Identifiés

Email personnel exposé dans le code source :

| Fichier | Ligne | Valeur |
|---------|-------|--------|
| `frontend/src/app/features/privacy-policy/privacy-policy.component.ts` | 22, 37 | `tom1997walker@gmail.com` |
| `frontend/src/app/features/legal-notices/legal-notices.component.ts` | 23 | `tom1997walker@gmail.com` |
| `frontend/src/app/features/terms-of-service/terms-of-service.component.html` | 192 | `tom1997walker@gmail.com` |

## Actions à Exécuter

### 1. Rechercher toutes les occurrences d'emails personnels

```bash
grep -rn "tom1997walker@gmail.com" .
grep -rn "@gmail.com\|@hotmail.com\|@yahoo.com" --include="*.ts" --include="*.html" --include="*.java" .
```

### 2. Remplacer par un email générique

```bash
find . -type f \( -name "*.ts" -o -name "*.html" -o -name "*.java" \) -exec sed -i 's/tom1997walker@gmail.com/contact@serenia.studio/g' {} +
```

### 3. Rechercher d'autres données personnelles potentielles

```bash
# Numéros de téléphone
grep -rn "[0-9]\{10\}\|+33\|06\|07" --include="*.ts" --include="*.html" .

# Adresses physiques
grep -rn "rue\|avenue\|boulevard\|street" --include="*.ts" --include="*.html" .

# Noms propres dans les composants légaux
grep -rn "Tom\|Walker" --include="*.ts" --include="*.html" .
```

### 4. Mettre à jour les fichiers de tests

```bash
# Les fichiers de tests référençant l'email doivent aussi être mis à jour
grep -rn "tom1997walker@gmail.com" --include="*.spec.ts" .
```

### 5. Vérifier les métadonnées

```bash
# Vérifier les package.json, pom.xml pour les auteurs
grep -rn "author\|maintainer" package.json pom.xml
```

## Critères de Validation

- [ ] Aucune occurrence de `tom1997walker@gmail.com`
- [ ] Aucun email personnel détecté
- [ ] Aucun numéro de téléphone personnel
- [ ] Aucune adresse physique personnelle
- [ ] Tests mis à jour et passants

## Étape Suivante

→ [Étape 3 : Scan de l'Historique Git](./step-03-git-history.prompt.md)
