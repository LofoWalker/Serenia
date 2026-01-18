# Step 3 : Audit de la Cryptographie et Gestion des Secrets

## Contexte

L'application Serenia manipule des données sensibles (conversations privées avec une IA, données utilisateur, informations de paiement). La protection cryptographique est assurée par plusieurs mécanismes :

| Domaine | Technologie | Fichier Principal |
|---------|-------------|-------------------|
| Chiffrement messages | AES-256-GCM + HKDF-SHA256 | `EncryptionService.java`, `HkdfUtils.java` |
| Hachage mots de passe | bcrypt | (à vérifier dans UserService) |
| Authentification | JWT RS256 | `JwtService.java` |
| Gestion des secrets | Docker Secrets | `compose.yaml` |
| TLS | Let's Encrypt via Traefik | `compose.yaml` |

### État actuel détecté

L'audit préliminaire indique que :
- ✅ AES-256-GCM avec HKDF-SHA256 pour dérivation de clé par utilisateur
- ✅ Docker Secrets pour les 8 secrets critiques
- ✅ Clés RSA pour JWT (séparées private/public)
- ⚠️ À vérifier : rotation des clés, force du master key, logs des secrets

---

## Objectif

1. **Valider l'implémentation cryptographique** : Confirmer la conformité aux standards (NIST, OWASP)
2. **Auditer la gestion des secrets** : Vérifier l'isolation, les permissions, et l'absence de leaks
3. **Vérifier le cycle de vie des clés** : Génération, stockage, rotation, révocation
4. **Identifier les faiblesses potentielles** : Entropie insuffisante, algorithmes obsolètes, timing attacks

---

## Méthode

### 3.1 Audit du Chiffrement AES-256-GCM

#### EncryptionService.java - Analyse détaillée

```java
// Constantes de configuration
private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";  // ✅ Mode authentifié
private static final int GCM_IV_LENGTH_BYTES = 12;  // ✅ 96 bits recommandé par NIST
private static final int GCM_TAG_LENGTH_BITS = 128;  // ✅ Tag maximum

private static final byte PAYLOAD_VERSION_HKDF_V1 = 0x01;  // ✅ Versioning pour migration
private static final String HKDF_CONTEXT = "serenia-user-encryption-v1";  // ✅ Contexte unique
```

**Checklist de conformité :**

| Critère | Attendu | Implémentation | Statut |
|---------|---------|----------------|--------|
| Algorithme | AES-256-GCM | ✅ `AES/GCM/NoPadding` | ✅ |
| Taille clé | 256 bits | ✅ Via HKDF (32 bytes) | ✅ |
| IV/Nonce | 96 bits, unique par message | ✅ 12 bytes, `SecureRandom` | ✅ |
| Tag d'authentification | 128 bits | ✅ `GCM_TAG_LENGTH_BITS = 128` | ✅ |
| Mode | Chiffrement authentifié | ✅ GCM | ✅ |
| Padding | Aucun (GCM stream) | ✅ NoPadding | ✅ |

#### Vérification de la génération d'IV

```java
// EncryptionService.java
private byte[] generateIv() {
    byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
    secureRandom.nextBytes(iv);  // ✅ SecureRandom, pas Random
    return iv;
}
```

**⚠️ Point d'attention :** Avec GCM, réutiliser un IV avec la même clé compromet totalement la sécurité. Vérifier :
- [ ] `SecureRandom` est thread-safe (OK par défaut en Java)
- [ ] Pas de seed manuel de `SecureRandom`
- [ ] IV stocké avec le ciphertext (vérifié dans payload format)

### 3.2 Audit de HKDF (Key Derivation)

#### HkdfUtils.java - Analyse

```java
private static final String HMAC_ALGORITHM = "HmacSHA256";  // ✅ SHA-256
private static final int HASH_LENGTH = 32;  // ✅ 256 bits

public static byte[] derive(byte[] ikm, byte[] salt, byte[] info, int length) {
    if (length > 255 * HASH_LENGTH) {  // ✅ Limite RFC 5869
        throw new IllegalArgumentException("Output length exceeds maximum allowed");
    }
    byte[] prk = hmacSha256(salt, ikm);  // Extract
    return expand(prk, info, length);    // Expand
}

public static byte[] deriveUserKey(byte[] masterKeyBytes, UUID userId, String context) {
    byte[] salt = uuidToBytes(userId);  // Salt = userId (16 bytes)
    byte[] info = context.getBytes(StandardCharsets.UTF_8);  // Info = context string
    return derive(masterKeyBytes, salt, info, 32);  // Output = 256 bits
}
```

**Conformité RFC 5869 :**

| Étape | RFC 5869 | Implémentation | Statut |
|-------|----------|----------------|--------|
| Extract | PRK = HMAC(salt, IKM) | ✅ `hmacSha256(salt, ikm)` | ✅ |
| Expand | OKM = HKDF-Expand(PRK, info, L) | ✅ `expand(prk, info, length)` | ✅ |
| Salt | Optionnel, améliore sécurité | ✅ userId (16 bytes) | ✅ |
| Info | Context-specific | ✅ `"serenia-user-encryption-v1"` | ✅ |

**Avantage sécurité :** Chaque utilisateur a une clé dérivée unique. Compromission d'une clé user n'affecte pas les autres.

### 3.3 Audit des Mots de Passe (bcrypt)

Rechercher l'implémentation du hachage :

```bash
grep -rn "bcrypt\|BCrypt\|PasswordEncoder\|hashPassword" \
  backend/src/main/java/
```

**Vérifications attendues :**

```java
// Pattern sécurisé attendu
import org.mindrot.jbcrypt.BCrypt;

public String hashPassword(String plaintext) {
    return BCrypt.hashpw(plaintext, BCrypt.gensalt(12));  // ✅ cost factor >= 12
}

public boolean verifyPassword(String plaintext, String hash) {
    return BCrypt.checkpw(plaintext, hash);  // ✅ Constant-time comparison
}
```

| Critère | Minimum | Recommandé | À vérifier |
|---------|---------|------------|------------|
| Cost factor | 10 | 12-14 | `gensalt(N)` |
| Algorithme | bcrypt | bcrypt/Argon2id | Pas MD5/SHA1 |
| Timing-safe compare | Oui | Oui | `checkpw()` |

### 3.4 Audit JWT RSA

#### Configuration JWT

```yaml
# compose.yaml
SMALLRYE_JWT_SIGN_KEY_LOCATION: /run/secrets/jwt_private_key
MP_JWT_VERIFY_PUBLICKEY_LOCATION: /run/secrets/jwt_public_key
```

**Vérifications des clés RSA :**

```bash
# Vérifier la taille de la clé (minimum 2048, recommandé 4096)
openssl rsa -in backend/keys/privateKey.pem -text -noout | head -5
# Attendu: "RSA Private-Key: (4096 bit, 2 primes)"

# Vérifier le format
file backend/keys/privateKey.pem
# Attendu: PEM RSA private key
```

**JwtService.java - Points de contrôle :**

```java
// Vérifier l'algorithme de signature
// ✅ Attendu: RS256 ou mieux (RS384, RS512)
// ❌ Interdit: HS256 avec secret faible, none

// Vérifier les claims obligatoires
// - exp (expiration)
// - iat (issued at)
// - iss (issuer)
// - sub (subject = userId)
```

### 3.5 Audit Docker Secrets

#### compose.yaml - 8 secrets externes

```yaml
secrets:
  db_password:
    file: ./secrets/db_password
  jwt_private_key:
    file: ./secrets/jwt_private_key.pem
  jwt_public_key:
    file: ./secrets/jwt_public_key.pem
  openai_api_key:
    file: ./secrets/openai_api_key
  stripe_secret_key:
    file: ./secrets/stripe_secret_key
  stripe_webhook_secret:
    file: ./secrets/stripe_webhook_secret
  smtp_password:
    file: ./secrets/smtp_password
  security_key:           # ← Master key pour AES
    file: ./secrets/security_key
```

**Vérifications de sécurité :**

```bash
# 1. Permissions des fichiers secrets
ls -la ./secrets/
# Attendu: -rw------- (600) ou -r-------- (400)

# 2. Propriétaire
# Attendu: root:root ou user déployeur spécifique

# 3. Pas dans git
cat .gitignore | grep -i secret
# Attendu: /secrets/ ou *.pem ou similaire

# 4. Pas de secrets dans l'image Docker
docker history ${BACK_IMAGE} | grep -i secret
# Attendu: aucun résultat

# 5. Secrets montés en mémoire (tmpfs dans /run/secrets/)
docker exec serenia-backend ls -la /run/secrets/
# Attendu: fichiers présents, taille cohérente
```

### 3.6 Vérification du Master Key (security_key)

Le master key est critique car il dérive toutes les clés utilisateur.

**Exigences :**

| Critère | Minimum | Recommandé |
|---------|---------|------------|
| Longueur | 32 bytes (256 bits) | 32 bytes |
| Entropie | Cryptographiquement aléatoire | `openssl rand -base64 32` |
| Stockage | Docker Secret | HSM en prod haute sécurité |
| Rotation | Plan documenté | Automatisée avec re-chiffrement |

**Génération recommandée :**
```bash
# Générer un nouveau master key
openssl rand -base64 32 > ./secrets/security_key

# Vérifier l'entropie (informel)
cat ./secrets/security_key | wc -c
# Attendu: ~44 caractères (32 bytes en base64)
```

### 3.7 Recherche de Secrets Hardcodés

```bash
# Patterns dangereux à rechercher
grep -rn "password\s*=\s*[\"']" backend/src/
grep -rn "secret\s*=\s*[\"']" backend/src/
grep -rn "apiKey\s*=\s*[\"']" backend/src/
grep -rn "sk_live_\|sk_test_" backend/src/  # Stripe keys
grep -rn "OPENAI_API_KEY\s*=\s*[\"']" backend/src/

# Vérifier les fichiers properties
cat backend/src/main/resources/application.properties | grep -v "^\s*#" | grep -i "password\|secret\|key"
# Attendu: Uniquement des références ${} ou /run/secrets/
```

---

## Architecture

### Flux de chiffrement des messages

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MESSAGE ENCRYPTION FLOW                          │
└────────────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────────┐     ┌─────────────────────────┐
│   User A    │     │  Master Key     │     │    Database             │
│  (plaintext)│     │ (Docker Secret) │     │  (ciphertext)           │
└──────┬──────┘     └────────┬────────┘     └────────────┬────────────┘
       │                     │                           │
       │  "Hello AI"         │                           │
       ▼                     ▼                           │
┌──────────────────────────────────────────────────────┐ │
│                  HKDF Key Derivation                  │ │
│  ┌──────────────────────────────────────────────────┐│ │
│  │  Salt = UUID_UserA (16 bytes)                    ││ │
│  │  IKM  = Master Key (32 bytes)                    ││ │
│  │  Info = "serenia-user-encryption-v1"             ││ │
│  │  ─────────────────────────────────────────────── ││ │
│  │  Output = UserKeyA (32 bytes, unique per user)   ││ │
│  └──────────────────────────────────────────────────┘│ │
└──────────────────────────────────────────────────────┘ │
       │                                                 │
       │  UserKeyA                                       │
       ▼                                                 │
┌──────────────────────────────────────────────────────┐ │
│               AES-256-GCM Encryption                  │ │
│  ┌──────────────────────────────────────────────────┐│ │
│  │  1. Generate IV (12 bytes, SecureRandom)         ││ │
│  │  2. Encrypt: Cipher(UserKeyA, IV, plaintext)     ││ │
│  │  3. Append GCM Tag (16 bytes)                    ││ │
│  └──────────────────────────────────────────────────┘│ │
└──────────────────────────────────────────────────────┘ │
       │                                                 │
       │  Versioned Payload                              │
       │  [0x01][IV:12B][Ciphertext+Tag]                │
       ▼                                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           PostgreSQL                                  │
│   messages table: encrypted_content = 0x01...                        │
│   (données illisibles sans Master Key + userId)                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Hiérarchie des secrets

```
┌─────────────────────────────────────────────────────────────────┐
│                    SECRETS HIERARCHY                             │
└─────────────────────────────────────────────────────────────────┘

Level 0: Infrastructure
├── TLS Certificates (Let's Encrypt)
│   └── Managed by: Traefik
│   └── Storage: ./traefik/acme.json
│
Level 1: Application Secrets (Docker Secrets)
├── security_key          ← Master encryption key (CRITICAL)
│   └── Derives: All user encryption keys
│   └── Compromise = All messages decryptable
│
├── jwt_private_key.pem   ← JWT signing (CRITICAL)
│   └── Usage: Sign all authentication tokens
│   └── Compromise = Token forgery possible
│
├── jwt_public_key.pem    ← JWT verification (PUBLIC)
│   └── Can be shared with external services
│
├── db_password           ← PostgreSQL auth (HIGH)
│   └── Access: Full database
│
├── openai_api_key        ← OpenAI API (HIGH)
│   └── Risk: Cost, data leak to OpenAI
│
├── stripe_secret_key     ← Stripe payments (HIGH)
│   └── Risk: Payment fraud
│
├── stripe_webhook_secret ← Webhook validation (MEDIUM)
│   └── Risk: Fake webhook injection
│
└── smtp_password         ← Email sending (MEDIUM)
    └── Risk: Spam, phishing from domain

Level 2: Derived Keys (Runtime, in-memory)
└── User Encryption Keys
    └── Derived via HKDF from security_key + userId
    └── Never stored, computed on-demand
```

---

## Tests d'Acceptance

### TA-3.1 : Chiffrement AES-256-GCM

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Message chiffré | Créer message via API | BDD contient bytes, pas plaintext |
| 2 | Message déchiffré | GET `/conversations/my-messages` | Plaintext lisible dans réponse |
| 3 | Isolation user | Query BDD avec userId différent | Déchiffrement échoue (clé différente) |
| 4 | IV unique | Chiffrer 2x le même message | Ciphertexts différents |

**Script de validation :**
```bash
# Insérer un message
MSG_ID=$(curl -s -X POST "https://api.serenia.studio/conversations/add-message" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"content":"Test encryption 123"}' | jq -r '.messageId')

# Vérifier en BDD que c'est chiffré
docker exec serenia-db psql -U serenia -d serenia -c \
  "SELECT encode(encrypted_content, 'hex') FROM messages WHERE id = '$MSG_ID';"
# Attendu: Hex bytes commençant par 01 (version), pas de "Test encryption"
```

### TA-3.2 : Sécurité du Master Key

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Permissions fichier | `ls -la ./secrets/security_key` | `-r--------` ou `-rw-------` |
| 2 | Pas dans git | `git ls-files ./secrets/` | Aucun fichier secret listé |
| 3 | Pas dans image | `docker history $IMAGE` | Pas de COPY secrets |
| 4 | Longueur suffisante | `wc -c < ./secrets/security_key` | >= 32 bytes |

### TA-3.3 : JWT RSA

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Taille clé RSA | `openssl rsa -in jwt_private_key.pem -text` | >= 2048 bits |
| 2 | Signature valide | Décoder JWT sur jwt.io | Signature vérifiée avec public key |
| 3 | Claims présents | Inspecter JWT payload | exp, iat, iss, sub présents |
| 4 | Expiration respectée | Attendre expiration, tenter requête | 401 Unauthorized |

**Validation JWT :**
```bash
# Extraire et décoder le JWT
JWT="eyJ..."
echo $JWT | cut -d. -f2 | base64 -d | jq .

# Vérifier les claims
{
  "sub": "uuid-user-id",
  "iss": "https://api.serenia.studio",
  "exp": 1234567890,
  "iat": 1234567800,
  "groups": ["USER"]
}
```

### TA-3.4 : bcrypt Passwords

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Hash format | Query user en BDD | Password commence par `$2a$` ou `$2b$` |
| 2 | Cost factor | Extraire cost du hash | >= 10 (idéalement 12) |
| 3 | Unique salt | 2 users même password | Hashes différents |
| 4 | Timing constant | Mesurer temps login valid/invalid | Écart < 50ms |

```bash
# Vérifier format bcrypt en BDD
docker exec serenia-db psql -U serenia -d serenia -c \
  "SELECT email, substring(password_hash, 1, 30) FROM users LIMIT 5;"

# Format attendu: $2a$12$... (bcrypt, cost 12)
```

### TA-3.5 : Absence de Secrets Hardcodés

| # | Scénario | Commande | Résultat Attendu |
|---|----------|----------|------------------|
| 1 | Pas de password hardcodé | `grep -rn "password.*=" src/` | Uniquement ${} refs |
| 2 | Pas de clé API | `grep -rn "sk_live" src/` | 0 résultats |
| 3 | Properties clean | `grep -v "^#" application.properties` | Pas de valeurs sensibles |
| 4 | Git history clean | `git log -p \| grep -i "secret\|password"` | Aucun secret commité |

---

## Vulnérabilités Potentielles à Corriger

### V-3.1 : Rotation de clé non implémentée

**Risque :** Si le master key est compromis, tous les messages passés et futurs sont exposés.

**Correction recommandée :**
1. Implémenter un système de versioning de clé
2. Lors de rotation : re-chiffrer les messages avec nouvelle clé
3. Garder l'ancienne clé en lecture seule pendant la migration

```java
// Exemple de payload avec version de clé
[KeyVersion: 1 byte][PayloadVersion: 1 byte][IV: 12B][Ciphertext]
```

### V-3.2 : Pas de HSM en production

**Risque :** Master key exposé si le serveur est compromis.

**Recommandation :** Pour environnements haute sécurité, utiliser :
- AWS KMS / GCP Cloud KMS
- HashiCorp Vault
- HSM physique

---

## Critères de Complétion

- [ ] AES-256-GCM vérifié conforme (IV unique, tag 128 bits)
- [ ] HKDF conforme RFC 5869
- [ ] bcrypt avec cost factor >= 12
- [ ] JWT RS256 avec clé RSA >= 2048 bits
- [ ] Tous les 8 Docker Secrets correctement configurés
- [ ] Permissions fichiers secrets = 600 ou 400
- [ ] Aucun secret dans git history
- [ ] Aucun secret hardcodé dans le code
- [ ] Tests TA-3.1 à TA-3.5 passent à 100%
- [ ] Plan de rotation de clé documenté (même si non automatisé)
