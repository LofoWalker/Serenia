# Gestion des Secrets - Guide pour les Contributeurs

## 🔒 Politique de Gestion des Secrets

Ce projet respecte les meilleures pratiques de sécurité en ce qui concerne la gestion des secrets et des données sensibles.

### ✅ Ce qui est Sécurisé

- **`backend/.env`** - ✅ Complètement **ignoré** par git
- **`backend/keys/*.pem`** - ✅ Toutes les clés cryptographiques ignorées
- **Code source** - ✅ Aucun secret en dur (hardcoded)
- **Fichiers de configuration** - ✅ Aucune donnée sensible dans le repository

### 🛡️ Comment Cela Fonctionne

1. **Fichiers ignorés** (voir `.gitignore`)
   ```gitignore
   backend/.env          # Variables d'environnement locales
   backend/.env.*        # Fichiers d'environnement
   backend/keys/*.pem    # Clés cryptographiques
   ```

2. **Template fourni** (`backend/.env.example`)
   - Liste complète des variables requises
   - Instructions pour remplir les placeholders
   - Aucun secret réel contenu

3. **Clés de test isolées**
   - Localisées dans `backend/src/test/resources/keys/`
   - Utilisées uniquement lors des tests
   - Peuvent être publiques (clés faibles de test)

## 📋 Pour Démarrer le Développement

### 1. Cloner le Repository

```bash
git clone <repository-url>
cd Serenia
```

### 2. Configurer Votre Environnement Local

```bash
# Copier le template
cp backend/.env.example backend/.env

# Éditer le fichier et remplir les placeholders
nano backend/.env
```

### 3. Remplir les Secrets Requis

Vous aurez besoin de :

| Variable | Source | Notes |
|----------|--------|-------|
| `OPENAI_API_KEY` | [OpenAI Platform](https://platform.openai.com/api-keys) | Clé API pour le modèle GPT |
| `STRIPE_SECRET_KEY` | [Stripe Dashboard](https://dashboard.stripe.com/test/apikeys) | Clés de **test** pour le développement |
| `STRIPE_WEBHOOK_SECRET` | Stripe Dashboard | Généré après configuration du webhook |
| `QUARKUS_MAILER_PASSWORD` | Votre service SMTP | Optionnel (email en dev peut être désactivé) |

### 4. Générer les Clés JWT Locales

```bash
cd backend/keys

# Générer une paire de clés RSA (2048-bit)
openssl genrsa -out rsaPrivateKey.pem 2048

# Extraire la clé publique
openssl rsa -in rsaPrivateKey.pem -pubout -out publicKey.pem

# Protéger les clés
chmod 600 *.pem

cd ../..
```

### 5. Démarrer le Développement

```bash
# Backend
cd backend
mvn quarkus:dev

# Frontend (dans un autre terminal)
cd frontend
npm install
npm start
```

## 🚫 Important : Ne Pas Committer

**N'OUBLIEZ PAS** : Votre `backend/.env` contient vos secrets locaux et ne doit **JAMAIS** être commité.

```bash
# Vérifier que votre fichier n'a pas été accidentellement ajouté
git status | grep ".env"  # Ne doit rien afficher

# Si accidentellement commité, utilisez:
git rm --cached backend/.env
```

## 🔐 Gestion en Production

Pour **déployer en production**, utilisez un gestionnaire de secrets approprié :

### Docker Secrets (Docker Swarm)

```bash
echo "votre-clé-api" | docker secret create openai_api_key -
echo "votre-clé-secrète" | docker secret create stripe_secret_key -
```

### Variables d'Environnement (Kubernetes)

```yaml
env:
  - name: OPENAI_API_KEY
    valueFrom:
      secretKeyRef:
        name: api-secrets
        key: openai-key
```

### Gestionnaires de Secrets Externes

- **HashiCorp Vault** - Recommandé pour les grandes organisations
- **AWS Secrets Manager** - Pour les déploiements AWS
- **Google Secret Manager** - Pour les déploiements GCP

## 📝 Checklist pour les Contributeurs

Avant de faire un PR, vérifiez que vous :

- [ ] N'avez **pas commité** votre `backend/.env`
- [ ] Avez généré vos propres clés JWT locales
- [ ] Utilisez des **clés de test** pour les services externes
- [ ] N'avez pas modifié le `.gitignore`
- [ ] Avez testé localement avec vos credentials

## 🚨 Incident de Sécurité

Si vous découvrez qu'un secret a été accidentellement commité :

1. **Signalez immédiatement** en créant une issue privée
2. **Révocation** de la clé exposée (changez-la immédiatement)
3. **Suppression** de l'historique git si nécessaire

```bash
# Nettoyer l'historique git (dangereux - force push requis)
git filter-branch --tree-filter 'rm -f backend/.env' -- --all
```

## 📚 Références

- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [12 Factor App - Config](https://12factor.net/config)
- [Quarkus Configuration](https://quarkus.io/guides/config)
- [Docker Secrets Documentation](https://docs.docker.com/engine/swarm/secrets/)

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-01-16
