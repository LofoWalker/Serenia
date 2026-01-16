# Étape 10 : Uniformité et Standards du Code

> **Priorité** : 🟢 Moyenne | **Bloquant** : Non

## Objectif

Assurer une cohérence de style dans tout le codebase pour faciliter les contributions de la communauté.

## Actions à Exécuter

### 1. Vérifier la configuration de formatage existante

```bash
# Frontend
ls -la frontend/.eslintrc* frontend/.prettierrc* frontend/.editorconfig 2>/dev/null

# Backend
ls -la backend/checkstyle.xml backend/.editorconfig 2>/dev/null
```

### 2. Créer/Vérifier .editorconfig (racine)

```bash
cat > .editorconfig << 'EOF'
# EditorConfig helps maintain consistent coding styles
root = true

[*]
indent_style = space
indent_size = 2
end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true

[*.java]
indent_size = 4

[*.md]
trim_trailing_whitespace = false

[Makefile]
indent_style = tab
EOF
```

### 3. Configuration ESLint - Frontend

Vérifier `frontend/.eslintrc.json` ou `frontend/eslint.config.js` :

```bash
cat frontend/eslint.config.js 2>/dev/null || cat frontend/.eslintrc.json 2>/dev/null
```

### 4. Configuration Prettier - Frontend

```bash
cat > frontend/.prettierrc << 'EOF'
{
  "singleQuote": true,
  "trailingComma": "none",
  "printWidth": 100,
  "tabWidth": 2,
  "semi": true
}
EOF
```

### 5. Appliquer le formatage automatique - Frontend

```bash
cd frontend
npx prettier --write "src/**/*.{ts,html,css,scss}"
npx eslint --fix "src/**/*.ts"
```

### 6. Configuration Checkstyle - Backend

Vérifier ou créer `backend/checkstyle.xml` basé sur Google Style :

```bash
# Télécharger Google Style
curl -o backend/checkstyle.xml https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/main/resources/google_checks.xml
```

### 7. Appliquer le formatage - Backend

```bash
cd backend

# Avec le plugin formatter-maven-plugin
./mvnw formatter:format

# Ou avec google-java-format
find src -name "*.java" -exec google-java-format -i {} \;
```

### 8. Vérifier les conventions de nommage

```bash
# Classes Java : PascalCase
find backend/src -name "*.java" -exec basename {} \; | grep -v "^[A-Z]" | head -10

# Fichiers TypeScript : kebab-case ou camelCase
find frontend/src -name "*.ts" -exec basename {} \; | grep "[A-Z]" | grep -v ".spec.ts" | head -10

# Constantes : UPPER_SNAKE_CASE
grep -rn "final.*static.*=" --include="*.java" backend/src/ | grep -v "[A-Z_]\+\s*=" | head -10
```

### 9. Vérifier la structure des packages/modules

```bash
# Backend - Structure des packages
find backend/src/main/java -type d | sed 's|backend/src/main/java/||' | sort

# Frontend - Structure des modules
find frontend/src/app -type d | sed 's|frontend/src/app/||' | sort
```

### 10. Configurer les hooks Git (recommandé)

```bash
# Installer husky pour les pre-commit hooks
cd frontend
npm install --save-dev husky lint-staged

# Configuration dans package.json
cat >> package.json << 'EOF'
{
  "lint-staged": {
    "*.ts": ["eslint --fix", "prettier --write"],
    "*.html": ["prettier --write"],
    "*.css": ["prettier --write"]
  }
}
EOF

npx husky install
npx husky add .husky/pre-commit "npx lint-staged"
```

## Standards par Langage

| Langage | Style Guide | Outil de Vérification |
|---------|-------------|----------------------|
| Java | Google Java Style | Checkstyle |
| TypeScript | Angular Style Guide | ESLint |
| HTML | Prettier | Prettier |
| CSS/SCSS | BEM (optionnel) | Stylelint |
| SQL | Lowercase keywords | - |
| YAML | 2 spaces indent | yamllint |

## Critères de Validation

- [ ] `.editorconfig` présent à la racine
- [ ] ESLint configuré pour le frontend
- [ ] Prettier configuré pour le frontend
- [ ] Checkstyle configuré pour le backend
- [ ] Aucune erreur de formatage après application
- [ ] Conventions de nommage respectées
- [ ] (Optionnel) Hooks pre-commit configurés

## Étape Suivante

→ [Étape 11 : Checklist Finale et Verdict](./step-11-final-checklist.prompt.md)
