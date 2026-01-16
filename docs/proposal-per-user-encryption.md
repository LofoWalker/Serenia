# Proposition : Chiffrement par Utilisateur — Serenia

## 1. Contexte et Problématique

### Situation actuelle

L'application Serenia chiffre actuellement les messages des utilisateurs avec **AES-256-GCM**, un algorithme robuste et moderne. Cependant, l'implémentation actuelle utilise une **clé maître unique** (`SERENIA_SECURITY_KEY`) pour tous les utilisateurs :

```java
// EncryptionService.java - Implémentation actuelle
private final SecretKey masterKey;

public EncryptionService(SereniaConfig sereniaConfig) {
    this.masterKey = initMasterKey(sereniaConfig.securityKey());
}

public byte[] encryptForUser(UUID userId, String plaintext) {
    SecretKey key = getMasterKey(); // ← Même clé pour tous
    // ...
}
```

### Risques identifiés

| Risque | Impact | Probabilité |
|--------|--------|-------------|
| Compromission de la clé maître | **Critique** — Tous les messages de tous les utilisateurs sont exposés | Faible mais catastrophique |
| Accès interne malveillant | **Élevé** — Un admin ou un attaquant interne peut déchiffrer toutes les conversations | Moyenne |
| Conformité RGPD | **Moyen** — Le principe de minimisation des données n'est pas optimal | N/A |
| Rotation de clé | **Élevé** — Impossible sans re-chiffrer l'intégralité de la base | Certaine en cas de compromission |

---

## 2. Solutions Proposées

### Solution A : Key Derivation Function (KDF) — **Recommandée**

#### Principe

Dériver une clé unique par utilisateur à partir de la clé maître et de l'identifiant utilisateur, en utilisant un algorithme de dérivation de clé cryptographique (HKDF).

```
User Key = HKDF(Master Key, User ID, "serenia-user-encryption")
```

#### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Master Key (secret)                       │
│              SERENIA_SECURITY_KEY (AES-256)                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    HKDF-SHA256                               │
│  Input: Master Key + User ID + Context ("serenia-user-enc")  │
└──────────────────────────┬───────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │ User Key A │  │ User Key B │  │ User Key C │
    └────────────┘  └────────────┘  └────────────┘
           │               │               │
           ▼               ▼               ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │ AES-GCM    │  │ AES-GCM    │  │ AES-GCM    │
    │ Encrypt    │  │ Encrypt    │  │ Encrypt    │
    └────────────┘  └────────────┘  └────────────┘
```

#### Avantages

- ✅ **Aucune migration de données nécessaire** — Les clés sont dérivées déterministiquement
- ✅ **Aucun stockage supplémentaire** — Pas de nouvelle table ou colonne
- ✅ **Isolation cryptographique** — Compromission d'une clé utilisateur n'expose pas les autres
- ✅ **Performance** — HKDF est très rapide, dérivation négligeable
- ✅ **Simplicité** — Changements minimes dans le code existant

#### Inconvénients

- ⚠️ La clé maître reste un SPOF (Single Point of Failure)
- ⚠️ Pas de révocation individuelle de clé sans rotation globale

#### Implémentation suggérée

```java
// Pseudo-code - Structure de l'implémentation
public class EncryptionService {
    private static final byte[] HKDF_INFO = "serenia-user-encryption".getBytes(UTF_8);
    
    private SecretKey deriveUserKey(UUID userId) {
        // HKDF-Expand avec Master Key + userId comme salt
        byte[] derivedKey = HKDFUtils.expand(
            masterKey.getEncoded(),
            userId.toString().getBytes(UTF_8),
            HKDF_INFO,
            32 // 256 bits
        );
        return new SecretKeySpec(derivedKey, "AES");
    }
    
    public byte[] encryptForUser(UUID userId, String plaintext) {
        SecretKey userKey = deriveUserKey(userId);
        // ... reste du chiffrement AES-GCM inchangé
    }
}
```

#### Effort estimé

| Composant | Temps estimé |
|-----------|--------------|
| Implémentation HKDF | 2h |
| Adaptation EncryptionService | 1h |
| Tests unitaires | 2h |
| Tests d'intégration | 1h |
| **Total** | **~6h** |

---

### Solution B : Clés Utilisateur Stockées en Base

#### Principe

Générer et stocker une clé AES unique pour chaque utilisateur dans la base de données, elle-même chiffrée avec la clé maître (technique d'enveloppe).

```
┌─────────────────────────────────────────────────────────────┐
│                    Master Key (secret)                       │
│              SERENIA_SECURITY_KEY (AES-256)                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              Chiffrement des User Keys (Envelope)            │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    Table: user_encryption_keys               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ user_id (FK) │ encrypted_key (blob) │ created_at        ││
│  │──────────────│──────────────────────│───────────────────││
│  │ uuid-user-1  │ [AES-GCM encrypted]  │ 2025-01-16        ││
│  │ uuid-user-2  │ [AES-GCM encrypted]  │ 2025-01-16        ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```

#### Avantages

- ✅ **Rotation individuelle** — Possibilité de changer la clé d'un seul utilisateur
- ✅ **Révocation** — Suppression de la clé = inaccessibilité des données
- ✅ **Audit** — Traçabilité de la création/rotation des clés
- ✅ **Flexibilité** — Possibilité d'ajouter des métadonnées (version, algorithme)

#### Inconvénients

- ⚠️ **Migration requise** — Nouvelle table + script Liquibase
- ⚠️ **Complexité accrue** — Gestion du cycle de vie des clés
- ⚠️ **Performance** — Requête BDD pour récupérer la clé (cache recommandé)
- ⚠️ **Données existantes** — Migration des messages existants nécessaire

#### Schéma de base de données

```yaml
# Liquibase changelog
- changeSet:
    id: add-user-encryption-keys
    author: serenia
    changes:
      - createTable:
          tableName: user_encryption_keys
          columns:
            - column:
                name: id
                type: uuid
                constraints:
                  primaryKey: true
            - column:
                name: user_id
                type: uuid
                constraints:
                  nullable: false
                  foreignKeyName: fk_user_enc_key_user
                  references: users(id)
                  unique: true
            - column:
                name: encrypted_key
                type: bytea
                constraints:
                  nullable: false
            - column:
                name: key_version
                type: int
                defaultValue: 1
            - column:
                name: created_at
                type: timestamp with time zone
                constraints:
                  nullable: false
```

#### Effort estimé

| Composant | Temps estimé |
|-----------|--------------|
| Entité JPA + Repository | 2h |
| Service de gestion des clés | 4h |
| Migration Liquibase | 1h |
| Adaptation EncryptionService | 2h |
| Script de migration des données existantes | 4h |
| Tests unitaires + intégration | 4h |
| Cache des clés (optionnel) | 2h |
| **Total** | **~19h** |

---

### Solution C : Chiffrement basé sur le mot de passe (Client-Side Key)

#### Principe

Dériver la clé de chiffrement à partir du mot de passe de l'utilisateur. Les données ne peuvent être déchiffrées qu'avec le mot de passe, inconnu du serveur.

```
┌─────────────────────────────────────────────────────────────┐
│                    User Password                             │
│              (fourni à chaque authentification)              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    PBKDF2 / Argon2                           │
│  Input: Password + User Salt (stocké en BDD)                 │
│  Output: Auth Key + Encryption Key                           │
└──────────────────────────┬───────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
    ┌────────────────┐              ┌────────────────┐
    │ Auth Key       │              │ Encryption Key │
    │ (vérification) │              │ (chiffrement)  │
    └────────────────┘              └────────────────┘
```

#### Avantages

- ✅ **Sécurité maximale** — Le serveur ne peut pas déchiffrer les données
- ✅ **Zero-knowledge** — Même un admin ne peut accéder aux messages
- ✅ **Conformité RGPD** — Protection maximale des données personnelles

#### Inconvénients

- ❌ **Perte de mot de passe = perte de données** — Récupération impossible
- ❌ **Changement de mot de passe complexe** — Nécessite re-chiffrement de toutes les données
- ❌ **Pas de fonctionnalités serveur** — Recherche full-text impossible
- ❌ **Architecture majeure** — Refonte complète du flux d'authentification
- ❌ **Impact UX** — Le mot de passe doit transiter à chaque session

#### Effort estimé

| Composant | Temps estimé |
|-----------|--------------|
| Refonte du flux d'authentification | 8h |
| Dérivation de clé côté backend | 4h |
| Gestion du changement de mot de passe | 6h |
| Migration des données existantes | 8h |
| Tests complets | 8h |
| **Total** | **~34h+** |

---

## 3. Comparaison des Solutions

| Critère | Solution A (HKDF) | Solution B (Clés stockées) | Solution C (Password-based) |
|---------|-------------------|----------------------------|----------------------------|
| **Sécurité** | ★★★★☆ | ★★★★☆ | ★★★★★ |
| **Complexité** | ★★★★★ | ★★★☆☆ | ★★☆☆☆ |
| **Effort** | ~6h | ~19h | ~34h+ |
| **Migration** | Aucune | Nécessaire | Majeure |
| **Performance** | Excellente | Bonne (avec cache) | Bonne |
| **Rotation clé unitaire** | ❌ | ✅ | ✅ |
| **Zero-knowledge** | ❌ | ❌ | ✅ |
| **Récupération données** | ✅ | ✅ | ❌ |

---

## 4. Recommandation

### Court terme : **Solution A (HKDF)** ✅

La solution HKDF offre le **meilleur rapport sécurité/effort** :

1. **Amélioration immédiate** de la sécurité sans migration de données
2. **Isolation cryptographique** entre utilisateurs
3. **Changements minimes** dans le code existant
4. **Aucun impact** sur les performances ou l'UX

### Moyen terme (optionnel) : **Évolution vers Solution B**

Si des besoins de rotation/révocation individuelle émergent :

1. Implémenter la gestion des clés stockées
2. Utiliser HKDF comme fallback pour les anciens messages
3. Migration progressive vers le nouveau système

---

## 5. Plan d'implémentation — Solution A

### Phase 1 : Préparation

1. Ajouter la dépendance pour HKDF (ou implémenter manuellement)
2. Écrire les tests unitaires pour la dérivation de clé

### Phase 2 : Implémentation

1. Modifier `EncryptionService` pour dériver les clés par utilisateur
2. Ajouter un cache en mémoire pour les clés dérivées (optionnel)
3. Mettre à jour les tests existants

### Phase 3 : Validation

1. Tests d'intégration complets
2. Vérification de la rétrocompatibilité (les messages existants doivent rester déchiffrables avec une migration)
3. Tests de performance

### Phase 4 : Migration des données existantes

1. Script de migration pour re-chiffrer les messages existants avec les nouvelles clés utilisateur
2. Exécution en batch pour éviter les interruptions de service

---

## 6. Considérations Supplémentaires

### Versionnage des clés

Ajouter un préfixe de version au format des données chiffrées :

```
[Version (1 byte)][IV (12 bytes)][Ciphertext][Auth Tag]
```

Cela permettra d'évoluer vers d'autres schémas de chiffrement sans casser la compatibilité.

### Rotation de la clé maître

Même avec HKDF, prévoir un mécanisme de rotation de la clé maître :

1. Stocker la version de la clé maître utilisée pour chaque message
2. Supporter plusieurs clés maîtres en parallèle pendant la transition
3. Re-chiffrer progressivement les messages avec la nouvelle clé

### Monitoring

Ajouter des métriques pour suivre :

- Nombre d'opérations de chiffrement/déchiffrement
- Temps moyen de dérivation de clé
- Erreurs de déchiffrement (indicateur de corruption ou de problème)

---

## 7. Conclusion

La **Solution A (HKDF)** est recommandée pour une implémentation rapide et efficace du chiffrement par utilisateur. Elle résout le problème principal (isolation des données entre utilisateurs) avec un effort minimal et sans impact sur les données existantes.

L'évolution vers la Solution B peut être envisagée ultérieurement si des besoins de gestion fine des clés (rotation, révocation) se manifestent.
