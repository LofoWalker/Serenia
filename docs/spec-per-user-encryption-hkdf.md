# Spécification Technique : Chiffrement par Utilisateur via HKDF

## 1. Objectif

### Problème à résoudre

L'implémentation actuelle de `EncryptionService` utilise une **clé maître unique** pour chiffrer les messages de tous les utilisateurs. Cette approche présente un risque majeur : si la clé est compromise, **toutes les données de tous les utilisateurs** sont exposées.

### Solution retenue

Implémenter un système de **dérivation de clé par utilisateur** basé sur **HKDF (HMAC-based Key Derivation Function)**. Chaque utilisateur aura une clé de chiffrement unique, dérivée de manière déterministe à partir de la clé maître et de son identifiant.

### Bénéfices attendus

| Bénéfice | Description |
|----------|-------------|
| **Isolation cryptographique** | Chaque utilisateur possède sa propre clé de chiffrement |
| **Limitation de l'impact** | Compromission d'une clé dérivée n'expose qu'un seul utilisateur |
| **Compatibilité** | Migration transparente des données existantes |
| **Performance** | Dérivation HKDF négligeable (~1μs par opération) |
| **Simplicité** | Aucune modification de schéma de base de données |

---

## 2. Architecture

### Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────┐
│                         MASTER KEY                                   │
│              (SERENIA_SECURITY_KEY - AES-256)                       │
│              Stockée en secret Docker/env var                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      HKDF-SHA256                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Input Key Material (IKM) : Master Key (32 bytes)            │    │
│  │ Salt                     : User ID (UUID as bytes)          │    │
│  │ Info                     : "serenia-user-encryption-v1"     │    │
│  │ Output Length            : 32 bytes (AES-256)               │    │
│  └─────────────────────────────────────────────────────────────┘    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   User Key A    │  │   User Key B    │  │   User Key C    │
│   (Derived)     │  │   (Derived)     │  │   (Derived)     │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   AES-256-GCM   │  │   AES-256-GCM   │  │   AES-256-GCM   │
│   Encrypt/      │  │   Encrypt/      │  │   Encrypt/      │
│   Decrypt       │  │   Decrypt       │  │   Decrypt       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Flux de chiffrement

```
┌──────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  Message     │     │ EncryptionService │     │    Database       │
│  (plaintext) │     │                  │     │                   │
└──────┬───────┘     └────────┬─────────┘     └─────────┬─────────┘
       │                      │                         │
       │ encryptForUser(      │                         │
       │   userId, message)   │                         │
       │─────────────────────>│                         │
       │                      │                         │
       │                      │ 1. Derive user key      │
       │                      │    via HKDF             │
       │                      │                         │
       │                      │ 2. Generate random IV   │
       │                      │    (12 bytes)           │
       │                      │                         │
       │                      │ 3. AES-GCM encrypt      │
       │                      │    with user key        │
       │                      │                         │
       │                      │ 4. Encode payload:      │
       │                      │    [V][IV][Ciphertext]  │
       │                      │                         │
       │                      │ persist(encrypted)      │
       │                      │────────────────────────>│
       │                      │                         │
       │    encrypted bytes   │                         │
       │<─────────────────────│                         │
```

### Flux de déchiffrement

```
┌──────────────┐     ┌──────────────────┐     ┌───────────────────┐
│   Client     │     │ EncryptionService │     │    Database       │
│              │     │                  │     │                   │
└──────┬───────┘     └────────┬─────────┘     └─────────┬─────────┘
       │                      │                         │
       │ decryptForUser(      │                         │
       │   userId, encrypted) │                         │
       │─────────────────────>│                         │
       │                      │                         │
       │                      │ 1. Parse version byte   │
       │                      │    (determine scheme)   │
       │                      │                         │
       │                      │ 2. Derive user key      │
       │                      │    via HKDF             │
       │                      │                         │
       │                      │ 3. Extract IV from      │
       │                      │    payload              │
       │                      │                         │
       │                      │ 4. AES-GCM decrypt      │
       │                      │    with user key        │
       │                      │                         │
       │    plaintext         │                         │
       │<─────────────────────│                         │
```

---

## 3. Spécification HKDF

### Algorithme

**HKDF** (RFC 5869) est un algorithme de dérivation de clé en deux étapes :

1. **Extract** : Produit une clé pseudo-aléatoire (PRK) à partir de l'IKM et du salt
2. **Expand** : Dérive une ou plusieurs clés de la longueur souhaitée

```
PRK = HMAC-SHA256(salt, IKM)
OKM = HKDF-Expand(PRK, info, length)
```

### Paramètres choisis

| Paramètre | Valeur | Justification |
|-----------|--------|---------------|
| **Hash** | SHA-256 | Standard, performant, sécurisé |
| **IKM** | Master Key (32 bytes) | Clé AES-256 existante |
| **Salt** | UUID utilisateur (16 bytes) | Unique par utilisateur, immuable |
| **Info** | `"serenia-user-encryption-v1"` | Contexte + versioning |
| **Output** | 32 bytes | Clé AES-256 |

### Pourquoi ces choix ?

- **UUID comme salt** : Chaque utilisateur a un UUID unique et immuable. Utiliser l'UUID garantit que deux utilisateurs n'auront jamais la même clé dérivée.

- **Info avec version** : Le suffixe `-v1` permet d'évoluer vers un nouveau schéma (`-v2`) sans casser la compatibilité.

- **SHA-256** : Algorithme standard, pas de dépendance externe nécessaire (disponible dans JCE).

---

## 4. Format des données chiffrées

### Format actuel (v0 - legacy)

```
┌─────────────────────────────────────────────────────────┐
│ IV (12 bytes) │ Ciphertext + Auth Tag (variable)       │
└─────────────────────────────────────────────────────────┘
```

### Nouveau format (v1 - per-user HKDF)

```
┌───────────────────────────────────────────────────────────────┐
│ Version (1 byte) │ IV (12 bytes) │ Ciphertext + Auth Tag     │
└───────────────────────────────────────────────────────────────┘

Version = 0x01 pour le schéma HKDF per-user
```

### Rétrocompatibilité

Le service détectera automatiquement l'ancien format (sans byte de version) et utilisera la clé maître directement pour le déchiffrement. Les nouveaux messages seront chiffrés avec le nouveau format.

**Règle de détection** :
- Si `encryptedBytes[0] == 0x01` → Nouveau format (HKDF)
- Sinon → Ancien format (Master Key directe)

---

## 5. Implémentation détaillée

### 5.1 Classe utilitaire HKDF

```java
package com.lofo.serenia.service.chat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * HKDF (HMAC-based Key Derivation Function) implementation per RFC 5869.
 * Used to derive unique encryption keys for each user from a master key.
 */
public final class HkdfUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int HASH_LENGTH = 32; // SHA-256 output length

    private HkdfUtils() {
        // Utility class
    }

    /**
     * Derives a key using HKDF-SHA256.
     *
     * @param ikm    Input Key Material (master key bytes)
     * @param salt   Salt value (user ID bytes)
     * @param info   Context and application specific information
     * @param length Desired output key length in bytes
     * @return Derived key bytes
     */
    public static byte[] derive(byte[] ikm, byte[] salt, byte[] info, int length) {
        if (length > 255 * HASH_LENGTH) {
            throw new IllegalArgumentException("Output length exceeds maximum allowed");
        }

        // Extract phase: PRK = HMAC-SHA256(salt, IKM)
        byte[] prk = hmacSha256(salt, ikm);

        // Expand phase
        return expand(prk, info, length);
    }

    /**
     * Convenience method to derive a user-specific key from master key and user ID.
     *
     * @param masterKeyBytes Master key bytes (AES-256 = 32 bytes)
     * @param userId         User UUID
     * @param context        Application context string
     * @return 32-byte derived key suitable for AES-256
     */
    public static byte[] deriveUserKey(byte[] masterKeyBytes, UUID userId, String context) {
        byte[] salt = uuidToBytes(userId);
        byte[] info = context.getBytes(StandardCharsets.UTF_8);
        return derive(masterKeyBytes, salt, info, 32);
    }

    private static byte[] expand(byte[] prk, byte[] info, int length) {
        int iterations = (int) Math.ceil((double) length / HASH_LENGTH);
        byte[] okm = new byte[length];
        byte[] previousBlock = new byte[0];

        int offset = 0;
        for (int i = 1; i <= iterations; i++) {
            // T(i) = HMAC-SHA256(PRK, T(i-1) | info | i)
            byte[] input = ByteBuffer.allocate(previousBlock.length + info.length + 1)
                    .put(previousBlock)
                    .put(info)
                    .put((byte) i)
                    .array();

            previousBlock = hmacSha256(prk, input);

            int copyLength = Math.min(HASH_LENGTH, length - offset);
            System.arraycopy(previousBlock, 0, okm, offset, copyLength);
            offset += copyLength;
        }

        return okm;
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
```

### 5.2 EncryptionService modifié

```java
package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.exceptions.EncryptionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

/**
 * Provides AES-GCM encryption/decryption with per-user key derivation via HKDF.
 * 
 * <p>Each user has a unique encryption key derived from the master key using HKDF-SHA256.
 * This ensures cryptographic isolation between users while maintaining a single master secret.</p>
 * 
 * <h2>Payload Format (v1)</h2>
 * <pre>
 * [Version: 1 byte][IV: 12 bytes][Ciphertext + GCM Tag: variable]
 * </pre>
 * 
 * <h2>Key Derivation</h2>
 * <pre>
 * UserKey = HKDF-SHA256(MasterKey, UserID, "serenia-user-encryption-v1")
 * </pre>
 */
@ApplicationScoped
public class EncryptionService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // Version byte for payload format identification
    private static final byte PAYLOAD_VERSION_LEGACY = 0x00;
    private static final byte PAYLOAD_VERSION_HKDF_V1 = 0x01;
    private static final byte CURRENT_PAYLOAD_VERSION = PAYLOAD_VERSION_HKDF_V1;

    // HKDF context for key derivation
    private static final String HKDF_CONTEXT = "serenia-user-encryption-v1";

    private final SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public EncryptionService(SereniaConfig sereniaConfig) {
        Objects.requireNonNull(sereniaConfig, "sereniaConfig must not be null");
        this.masterKey = initMasterKey(sereniaConfig.securityKey());
    }

    /**
     * Encrypts plaintext for a specific user using their derived key.
     *
     * @param userId    The user's unique identifier (used for key derivation)
     * @param plaintext The message to encrypt
     * @return Encrypted payload: [version][iv][ciphertext+tag]
     */
    public byte[] encryptForUser(UUID userId, String plaintext) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(plaintext, "plaintext must not be null");

        SecretKey userKey = deriveUserKey(userId);

        try {
            byte[] iv = generateIv();
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, userKey, iv);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return encodeVersionedPayload(iv, ciphertext);

        } catch (Exception e) {
            throw new EncryptionException("Encryption failed for user " + userId, e);
        }
    }

    /**
     * Decrypts encrypted content for a specific user.
     * Automatically handles both legacy (v0) and new (v1) payload formats.
     *
     * @param userId         The user's unique identifier
     * @param encryptedBytes The encrypted payload
     * @return Decrypted plaintext
     */
    public String decryptForUser(UUID userId, byte[] encryptedBytes) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(encryptedBytes, "encryptedBytes must not be null");

        if (encryptedBytes.length <= GCM_IV_LENGTH_BYTES) {
            throw new EncryptionException("Invalid encrypted data: too short");
        }

        try {
            return decryptWithVersionDetection(userId, encryptedBytes);
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed for user " + userId, e);
        }
    }

    // Derives a unique AES key for the given user via HKDF
    private SecretKey deriveUserKey(UUID userId) {
        byte[] derivedKeyBytes = HkdfUtils.deriveUserKey(
                masterKey.getEncoded(),
                userId,
                HKDF_CONTEXT
        );
        return new SecretKeySpec(derivedKeyBytes, KEY_ALGORITHM);
    }

    // Returns the master key directly (for legacy decryption only)
    private SecretKey getMasterKeyForLegacy() {
        if (masterKey == null) {
            throw new EncryptionException("Master encryption key is not initialized");
        }
        return masterKey;
    }

    private String decryptWithVersionDetection(UUID userId, byte[] encryptedBytes) throws Exception {
        byte versionByte = encryptedBytes[0];

        if (versionByte == PAYLOAD_VERSION_HKDF_V1) {
            // New format: [version(1)][iv(12)][ciphertext]
            return decryptV1Payload(userId, encryptedBytes);
        } else {
            // Legacy format: [iv(12)][ciphertext] - no version byte
            return decryptLegacyPayload(encryptedBytes);
        }
    }

    private String decryptV1Payload(UUID userId, byte[] encryptedBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);

        // Skip version byte
        buffer.get();

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKey userKey = deriveUserKey(userId);
        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, userKey, iv);
        byte[] plaintextBytes = cipher.doFinal(ciphertext);

        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    private String decryptLegacyPayload(byte[] encryptedBytes) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        SecretKey key = getMasterKeyForLegacy();
        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, key, iv);
        byte[] plaintextBytes = cipher.doFinal(ciphertext);

        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private Cipher createCipher(int mode, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(mode, key, spec);
            return cipher;
        } catch (Exception e) {
            throw new EncryptionException("Failed to initialize cipher", e);
        }
    }

    private byte[] encodeVersionedPayload(byte[] iv, byte[] ciphertext) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + iv.length + ciphertext.length);
        buffer.put(CURRENT_PAYLOAD_VERSION);
        buffer.put(iv);
        buffer.put(ciphertext);
        return buffer.array();
    }

    private SecretKey initMasterKey(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new EncryptionException(
                "Configuration property 'serenia.security.key' must not be null or blank"
            );
        }

        String trimmed = configuredKey.trim();
        byte[] keyBytes;
        try {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                keyBytes = hexStringToBytes(trimmed.substring(2));
            } else {
                keyBytes = java.util.Base64.getDecoder().decode(trimmed);
            }
        } catch (IllegalArgumentException e) {
            throw new EncryptionException(
                "Invalid format for 'serenia.security.key' (expected Base64 or 0x-prefixed hex)", e
            );
        }

        int length = keyBytes.length;
        if (length != 16 && length != 24 && length != 32) {
            throw new EncryptionException(
                "Invalid length for 'serenia.security.key': expected 16, 24 or 32 bytes but got " + length
            );
        }

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    private byte[] hexStringToBytes(String hex) {
        String clean = hex.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        int len = clean.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(clean.charAt(i), 16);
            int lo = Character.digit(clean.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex character in security key");
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }
}
```

---

## 6. Stratégie de migration

### 6.1 Migration progressive (recommandée)

La rétrocompatibilité intégrée permet une migration transparente :

1. **Déployer** le nouveau code avec support dual (legacy + HKDF)
2. **Nouveaux messages** : Chiffrés avec le nouveau format (v1)
3. **Anciens messages** : Déchiffrés avec l'ancien format (détection automatique)
4. **Migration en arrière-plan** (optionnel) : Re-chiffrer progressivement les anciens messages

### 6.2 Script de migration batch (optionnel)

Si une migration complète est souhaitée, exécuter en arrière-plan :

```java
/**
 * Migrates legacy encrypted messages to the new per-user HKDF format.
 * Should be run as a background job to avoid service disruption.
 */
@ApplicationScoped
public class EncryptionMigrationService {

    private static final int BATCH_SIZE = 100;
    private static final byte LEGACY_VERSION_INDICATOR = 0x00;

    @Inject
    MessageRepository messageRepository;

    @Inject
    EncryptionService encryptionService;

    @Transactional
    public MigrationResult migrateBatch() {
        List<Message> legacyMessages = messageRepository
            .findMessagesWithLegacyEncryption(BATCH_SIZE);

        int migrated = 0;
        int failed = 0;

        for (Message message : legacyMessages) {
            try {
                migrateMessage(message);
                migrated++;
            } catch (Exception e) {
                log.error("Failed to migrate message {}: {}", message.getId(), e.getMessage());
                failed++;
            }
        }

        return new MigrationResult(migrated, failed, legacyMessages.size() < BATCH_SIZE);
    }

    private void migrateMessage(Message message) {
        // Decrypt with legacy method
        String plaintext = encryptionService.decryptForUser(
            message.getUserId(), 
            message.getEncryptedContent()
        );

        // Re-encrypt with new HKDF method
        byte[] newEncrypted = encryptionService.encryptForUser(
            message.getUserId(), 
            plaintext
        );

        message.setEncryptedContent(newEncrypted);
        messageRepository.persist(message);
    }

    public record MigrationResult(int migrated, int failed, boolean complete) {}
}
```

### 6.3 Requête pour identifier les messages legacy

```sql
-- Messages avec ancien format (premier byte != 0x01)
SELECT COUNT(*) 
FROM messages 
WHERE get_byte(encrypted_content, 0) != 1;
```

---

## 7. Tests

### 7.1 Tests unitaires HkdfUtils

```java
@DisplayName("HkdfUtils Tests")
class HkdfUtilsTest {

    private static final byte[] MASTER_KEY = Base64.getDecoder()
        .decode("qNJYJqH7CdLF0X3R5wZ+bVmK9pL2mN3oQ4rS6tU7vW8=");

    @Test
    @DisplayName("Should derive consistent key for same user")
    void shouldDeriveConsistentKeyForSameUser() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");
        byte[] key2 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");

        assertArrayEquals(key1, key2, "Same inputs should produce same output");
    }

    @Test
    @DisplayName("Should derive different keys for different users")
    void shouldDeriveDifferentKeysForDifferentUsers() {
        UUID user1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID user2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, user1, "test-context");
        byte[] key2 = HkdfUtils.deriveUserKey(MASTER_KEY, user2, "test-context");

        assertFalse(Arrays.equals(key1, key2), "Different users should have different keys");
    }

    @Test
    @DisplayName("Should derive 32-byte key for AES-256")
    void shouldDerive32ByteKey() {
        UUID userId = UUID.randomUUID();

        byte[] key = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test-context");

        assertEquals(32, key.length, "Key should be 32 bytes for AES-256");
    }

    @Test
    @DisplayName("Should derive different keys for different contexts")
    void shouldDeriveDifferentKeysForDifferentContexts() {
        UUID userId = UUID.randomUUID();

        byte[] key1 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "context-v1");
        byte[] key2 = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "context-v2");

        assertFalse(Arrays.equals(key1, key2), "Different contexts should produce different keys");
    }

    @Test
    @DisplayName("Should produce cryptographically strong output")
    void shouldProduceCryptographicallyStrongOutput() {
        UUID userId = UUID.randomUUID();
        byte[] key = HkdfUtils.deriveUserKey(MASTER_KEY, userId, "test");

        // Basic entropy check: no more than 4 consecutive identical bytes
        int maxConsecutive = 0;
        int current = 1;
        for (int i = 1; i < key.length; i++) {
            if (key[i] == key[i - 1]) {
                current++;
                maxConsecutive = Math.max(maxConsecutive, current);
            } else {
                current = 1;
            }
        }

        assertTrue(maxConsecutive < 5, "Key should have good entropy distribution");
    }
}
```

### 7.2 Tests unitaires EncryptionService (nouvelles fonctionnalités)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionService Per-User HKDF Tests")
class EncryptionServiceHkdfTest {

    private static final String VALID_BASE64_KEY_256 = "qNJYJqH7CdLF0X3R5wZ+bVmK9pL2mN3oQ4rS6tU7vW8=";
    private static final UUID USER_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID USER_B = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @Mock
    SereniaConfig sereniaConfig;

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        when(sereniaConfig.securityKey()).thenReturn(VALID_BASE64_KEY_256);
        encryptionService = new EncryptionService(sereniaConfig);
    }

    @Nested
    @DisplayName("Per-User Isolation Tests")
    class PerUserIsolationTests {

        @Test
        @DisplayName("User A cannot decrypt User B's messages")
        void userACannotDecryptUserBMessages() {
            String message = "Secret message for User B";

            byte[] encryptedForB = encryptionService.encryptForUser(USER_B, message);

            assertThrows(EncryptionException.class, () -> {
                encryptionService.decryptForUser(USER_A, encryptedForB);
            }, "Decryption with wrong user key should fail");
        }

        @Test
        @DisplayName("Same message encrypted differently for different users")
        void sameMessageEncryptedDifferentlyForDifferentUsers() {
            String message = "Hello World";

            byte[] encryptedForA = encryptionService.encryptForUser(USER_A, message);
            byte[] encryptedForB = encryptionService.encryptForUser(USER_B, message);

            assertFalse(Arrays.equals(encryptedForA, encryptedForB),
                "Same plaintext should produce different ciphertext for different users");
        }

        @Test
        @DisplayName("User can decrypt their own messages")
        void userCanDecryptOwnMessages() {
            String originalMessage = "My private message";

            byte[] encrypted = encryptionService.encryptForUser(USER_A, originalMessage);
            String decrypted = encryptionService.decryptForUser(USER_A, encrypted);

            assertEquals(originalMessage, decrypted);
        }
    }

    @Nested
    @DisplayName("Versioned Payload Tests")
    class VersionedPayloadTests {

        @Test
        @DisplayName("New encryption produces versioned payload")
        void newEncryptionProducesVersionedPayload() {
            byte[] encrypted = encryptionService.encryptForUser(USER_A, "test");

            assertEquals(0x01, encrypted[0], "First byte should be version 0x01");
        }

        @Test
        @DisplayName("Payload structure is correct: [version][iv][ciphertext]")
        void payloadStructureIsCorrect() {
            byte[] encrypted = encryptionService.encryptForUser(USER_A, "test");

            // Version (1) + IV (12) + at least 1 byte ciphertext + 16 bytes tag
            assertTrue(encrypted.length >= 1 + 12 + 1 + 16,
                "Payload should contain version, IV, ciphertext, and auth tag");
        }
    }

    @Nested
    @DisplayName("Legacy Compatibility Tests")
    class LegacyCompatibilityTests {

        @Test
        @DisplayName("Should decrypt legacy format (no version byte)")
        void shouldDecryptLegacyFormat() {
            // Simulate legacy encrypted data (created with master key, no version byte)
            byte[] legacyEncrypted = createLegacyEncryptedData("Legacy message");

            // Should still be able to decrypt
            String decrypted = encryptionService.decryptForUser(USER_A, legacyEncrypted);

            assertEquals("Legacy message", decrypted);
        }

        private byte[] createLegacyEncryptedData(String plaintext) {
            // Manually create legacy format: [IV][ciphertext]
            // This simulates data encrypted before the HKDF update
            try {
                SecureRandom random = new SecureRandom();
                byte[] iv = new byte[12];
                random.nextBytes(iv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                byte[] masterKeyBytes = Base64.getDecoder().decode(VALID_BASE64_KEY_256);
                SecretKey key = new SecretKeySpec(masterKeyBytes, "AES");
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

                byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

                ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
                buffer.put(iv);
                buffer.put(ciphertext);
                return buffer.array();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            byte[] encrypted = encryptionService.encryptForUser(USER_A, "");
            String decrypted = encryptionService.decryptForUser(USER_A, encrypted);

            assertEquals("", decrypted);
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            String unicode = "Héllo Wörld 你好 🎉";

            byte[] encrypted = encryptionService.encryptForUser(USER_A, unicode);
            String decrypted = encryptionService.decryptForUser(USER_A, encrypted);

            assertEquals(unicode, decrypted);
        }

        @Test
        @DisplayName("Should handle large messages")
        void shouldHandleLargeMessages() {
            String largeMessage = "x".repeat(100_000);

            byte[] encrypted = encryptionService.encryptForUser(USER_A, largeMessage);
            String decrypted = encryptionService.decryptForUser(USER_A, encrypted);

            assertEquals(largeMessage, decrypted);
        }

        @Test
        @DisplayName("Multiple encryptions of same message produce different ciphertexts")
        void multipleEncryptionsProduceDifferentCiphertexts() {
            String message = "Same message";

            byte[] encrypted1 = encryptionService.encryptForUser(USER_A, message);
            byte[] encrypted2 = encryptionService.encryptForUser(USER_A, message);

            assertFalse(Arrays.equals(encrypted1, encrypted2),
                "Different IVs should produce different ciphertexts");

            // But both should decrypt to the same plaintext
            assertEquals(message, encryptionService.decryptForUser(USER_A, encrypted1));
            assertEquals(message, encryptionService.decryptForUser(USER_A, encrypted2));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw on null userId for encryption")
        void shouldThrowOnNullUserIdForEncryption() {
            assertThrows(NullPointerException.class, () -> {
                encryptionService.encryptForUser(null, "test");
            });
        }

        @Test
        @DisplayName("Should throw on null plaintext")
        void shouldThrowOnNullPlaintext() {
            assertThrows(NullPointerException.class, () -> {
                encryptionService.encryptForUser(USER_A, null);
            });
        }

        @Test
        @DisplayName("Should throw on tampered ciphertext")
        void shouldThrowOnTamperedCiphertext() {
            byte[] encrypted = encryptionService.encryptForUser(USER_A, "test");

            // Tamper with the ciphertext (last byte)
            encrypted[encrypted.length - 1] ^= 0xFF;

            assertThrows(EncryptionException.class, () -> {
                encryptionService.decryptForUser(USER_A, encrypted);
            }, "Tampered data should fail GCM authentication");
        }

        @Test
        @DisplayName("Should throw on truncated ciphertext")
        void shouldThrowOnTruncatedCiphertext() {
            byte[] tooShort = new byte[10]; // Less than IV length

            assertThrows(EncryptionException.class, () -> {
                encryptionService.decryptForUser(USER_A, tooShort);
            });
        }
    }
}
```

### 7.3 Tests d'intégration

```java
@QuarkusTest
@DisplayName("Encryption Integration Tests")
class EncryptionIntegrationTest {

    @Inject
    EncryptionService encryptionService;

    @Inject
    MessageService messageService;

    @Inject
    MessageRepository messageRepository;

    @Test
    @DisplayName("End-to-end: persist and retrieve encrypted message")
    @Transactional
    void endToEndPersistAndRetrieve() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        String originalContent = "Integration test message";

        // Persist message (internally encrypts)
        messageService.persistUserMessage(userId, conversationId, originalContent);

        // Retrieve and decrypt
        List<ChatMessage> messages = messageService.decryptConversationMessages(userId, conversationId);

        assertFalse(messages.isEmpty());
        assertEquals(originalContent, messages.get(0).content());
    }

    @Test
    @DisplayName("Encrypted content in database is not plaintext")
    @Transactional
    void encryptedContentIsNotPlaintext() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        String secretMessage = "This should be encrypted";

        messageService.persistUserMessage(userId, conversationId, secretMessage);

        Message storedMessage = messageRepository.findLatestInChronologicalOrder(conversationId).get(0);
        String storedAsString = new String(storedMessage.getEncryptedContent(), StandardCharsets.UTF_8);

        assertFalse(storedAsString.contains(secretMessage),
            "Plaintext should not appear in stored encrypted content");
    }
}
```

---

## 8. Considérations de sécurité

### 8.1 Propriétés cryptographiques garanties

| Propriété | Garantie |
|-----------|----------|
| **Confidentialité** | AES-256-GCM assure le chiffrement des données |
| **Intégrité** | Le tag GCM détecte toute modification |
| **Authenticité** | Le tag GCM prouve que les données n'ont pas été altérées |
| **Isolation utilisateur** | HKDF dérive une clé unique par utilisateur |
| **Non-déterminisme** | IV aléatoire garantit des ciphertexts différents |

### 8.2 Menaces et mitigations

| Menace | Mitigation |
|--------|------------|
| Compromission de la master key | Rotation de clé + re-chiffrement (voir section migration) |
| Compromission d'une clé utilisateur | Impact limité à un seul utilisateur |
| Attaque par rejeu | IV unique par message |
| Altération des données | Détection par GCM auth tag |
| Attaque par canal auxiliaire | Utilisation de SecureRandom pour IV |

### 8.3 Recommandations opérationnelles

1. **Stockage de la master key** : Utiliser Docker Secrets ou un KMS
2. **Rotation périodique** : Planifier une rotation de la master key tous les 12-24 mois
3. **Monitoring** : Alerter sur les erreurs de déchiffrement (potentielle corruption/attaque)
4. **Backup** : Sauvegarder la master key de manière sécurisée et séparée des données

---

## 9. Performance

### 9.1 Benchmarks attendus

| Opération | Temps estimé | Notes |
|-----------|--------------|-------|
| HKDF derive | ~1-5 μs | Négligeable |
| AES-GCM encrypt (100 chars) | ~10-50 μs | Inchangé |
| AES-GCM decrypt (100 chars) | ~10-50 μs | Inchangé |
| Total overhead | ~1-5 μs/message | Impact minimal |

### 9.2 Optimisation optionnelle : Cache des clés dérivées

Si le profiling montre un overhead, implémenter un cache LRU :

```java
private final Map<UUID, SecretKey> keyCache = new ConcurrentHashMap<>();
private static final int MAX_CACHE_SIZE = 1000;

private SecretKey deriveUserKey(UUID userId) {
    return keyCache.computeIfAbsent(userId, id -> {
        if (keyCache.size() >= MAX_CACHE_SIZE) {
            // Evict oldest entry (simplified - use proper LRU in production)
            keyCache.remove(keyCache.keySet().iterator().next());
        }
        byte[] derivedKeyBytes = HkdfUtils.deriveUserKey(
            masterKey.getEncoded(), id, HKDF_CONTEXT
        );
        return new SecretKeySpec(derivedKeyBytes, KEY_ALGORITHM);
    });
}
```

---

## 10. Checklist de déploiement

### Avant déploiement

- [ ] Tests unitaires passent (HkdfUtils, EncryptionService)
- [ ] Tests d'intégration passent
- [ ] Review de code sécurité effectuée
- [ ] Backup de la base de données effectué
- [ ] Documentation mise à jour

### Déploiement

- [ ] Déployer le nouveau code en production
- [ ] Vérifier les logs pour erreurs de déchiffrement
- [ ] Monitorer les métriques de performance

### Post-déploiement

- [ ] Vérifier que les nouveaux messages utilisent le format v1
- [ ] Planifier la migration des anciens messages (optionnel)
- [ ] Mettre à jour la documentation de sécurité

---

## 11. Annexes

### A. Références

- [RFC 5869 - HKDF](https://tools.ietf.org/html/rfc5869)
- [NIST SP 800-38D - GCM](https://csrc.nist.gov/publications/detail/sp/800-38d/final)
- [Java Cryptography Architecture](https://docs.oracle.com/en/java/javase/21/security/java-cryptography-architecture-jca-reference-guide.html)

### B. Glossaire

| Terme | Définition |
|-------|------------|
| **HKDF** | HMAC-based Key Derivation Function |
| **IKM** | Input Key Material (clé source) |
| **PRK** | Pseudorandom Key (sortie de HKDF-Extract) |
| **OKM** | Output Key Material (clé dérivée finale) |
| **GCM** | Galois/Counter Mode (mode de chiffrement authentifié) |
| **IV** | Initialization Vector (vecteur d'initialisation) |
