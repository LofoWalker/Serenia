# Étape 7 : Vérification des Commentaires

> **Priorité** : 🟡 Haute | **Bloquant** : Partiel

## Objectif

S'assurer qu'aucun commentaire ne contient d'informations sensibles, inappropriées ou de TODO/FIXME critiques non résolus.

## Actions à Exécuter

### 1. Rechercher les TODO/FIXME/HACK

```bash
# Backend
grep -rn "TODO\|FIXME\|HACK\|XXX\|BUG" --include="*.java" backend/src/

# Frontend
grep -rn "TODO\|FIXME\|HACK\|XXX\|BUG" --include="*.ts" --include="*.html" frontend/src/
```

### 2. Catégoriser les TODO trouvés

| Type | Fichier | Ligne | Contenu | Action |
|------|---------|-------|---------|--------|
| TODO | | | | Résoudre/Supprimer |
| FIXME | | | | Résoudre |
| HACK | | | | Refactorer |

### 3. Rechercher les commentaires sensibles

```bash
# Rechercher des références à des systèmes internes
grep -rn "internal\|private\|confidential\|proprietary" --include="*.java" --include="*.ts" .

# Rechercher des noms de personnes
grep -rn "@author" --include="*.java" .

# Rechercher des références à des tickets internes
grep -rn "JIRA\|TICKET\|ISSUE-[0-9]\|#[0-9]\{4,\}" --include="*.java" --include="*.ts" .
```

### 4. Rechercher les commentaires non professionnels

```bash
# Mots potentiellement inappropriés
grep -rni "wtf\|fuck\|shit\|damn\|stupid\|idiot\|crap\|hack\|ugly" --include="*.java" --include="*.ts" .

# Commentaires de frustration
grep -rni "why\|don't know\|no idea\|magic\|somehow" --include="*.java" --include="*.ts" .
```

### 5. Vérifier les annotations @author

```bash
# Lister toutes les annotations @author
grep -rn "@author" --include="*.java" backend/src/

# Décider : 
# - Supprimer (recommandé pour l'anonymisation)
# - Garder (si contributeur OK pour être identifié)
```

### 6. Nettoyer les commentaires de code mort

```bash
# Rechercher du code commenté
grep -rn "^[[:space:]]*//.*[a-zA-Z].*[;{}()]" --include="*.java" backend/src/ | head -50
grep -rn "^[[:space:]]*//.*[a-zA-Z].*[;{}()]" --include="*.ts" frontend/src/ | head -50
```

### 7. Vérifier les commentaires de debug

```bash
grep -rn "console.log\|System.out.print\|DEBUG\|TRACE" --include="*.java" --include="*.ts" .
```

## Catégories de Commentaires

### 🔴 À supprimer immédiatement
- Informations personnelles
- Références à des systèmes internes
- Commentaires non professionnels
- Secrets ou credentials commentés

### 🟡 À résoudre avant publication
- TODO critiques pour la fonctionnalité
- FIXME de sécurité
- HACK temporaires

### 🟢 Acceptable
- TODO pour des améliorations futures
- Commentaires explicatifs de logique complexe
- Documentation de code

## Critères de Validation

- [ ] Aucun commentaire contenant des informations personnelles
- [ ] Aucun commentaire référençant des systèmes internes
- [ ] Aucun commentaire non professionnel
- [ ] TODO/FIXME critiques résolus ou documentés
- [ ] Annotations @author revues

## Étape Suivante

→ [Étape 8 : Analyse du Code Mort](./step-08-dead-code.prompt.md)
